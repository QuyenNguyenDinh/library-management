package com.nhom04.library.service;

import com.nhom04.library.exception.BookNotFoundException;
import com.nhom04.library.exception.BookOutOfStockException;
import com.nhom04.library.exception.BorrowLimitExceededException;
import com.nhom04.library.exception.InvalidBorrowRequestException;
import com.nhom04.library.exception.ReaderNotFoundException;
import com.nhom04.library.model.Book;
import com.nhom04.library.model.BorrowTicket;
import com.nhom04.library.model.BorrowTicketDetail;
import com.nhom04.library.model.Reader;
import com.nhom04.library.repository.BookRepository;
import com.nhom04.library.repository.ReaderRepository;
import com.nhom04.library.repository.TicketRepository;
import com.nhom04.library.utils.IdGenerator;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Nghiệp vụ mượn sách: kiểm tra yêu cầu, bạn đọc, hạn mức, tồn kho rồi lập phiếu mượn.
 * <p>
 * Thứ tự kiểm tra (lỗi nào gặp trước thì báo trước):
 * <ol>
 *     <li>Yêu cầu hợp lệ: có mã bạn đọc, có ít nhất một đầu sách, số lượng &gt; 0, ngày hợp lệ (400)</li>
 *     <li>Bạn đọc tồn tại (404)</li>
 *     <li>Từng đầu sách tồn tại (404)</li>
 *     <li>Không vượt hạn mức mượn theo loại bạn đọc (409)</li>
 *     <li>Từng đầu sách còn đủ số lượng (409)</li>
 * </ol>
 * Chỉ khi mọi điều kiện đều đạt mới trừ kho và lưu phiếu, nên một yêu cầu bị từ chối
 * sẽ không làm thay đổi dữ liệu.
 */
@Service
public class BorrowService {

    /** Số ngày mượn mặc định khi yêu cầu không gửi kèm ngày hẹn trả. */
    public static final int DEFAULT_BORROW_DAYS = 14;

    private static final String TICKET_ID_PREFIX = "TK";

    private final BookRepository bookRepository;
    private final ReaderRepository readerRepository;
    private final TicketRepository ticketRepository;

    public BorrowService(BookRepository bookRepository,
                         ReaderRepository readerRepository,
                         TicketRepository ticketRepository) {
        this.bookRepository = bookRepository;
        this.readerRepository = readerRepository;
        this.ticketRepository = ticketRepository;
    }

    /**
     * Lập phiếu mượn cho bạn đọc.
     * <p>
     * Phương thức được đồng bộ hoá để hai yêu cầu mượn cùng lúc không cùng đọc một số lượng tồn kho cũ.
     *
     * @param readerId   mã bạn đọc
     * @param items      danh sách đầu sách và số lượng muốn mượn; cùng một mã sách xuất hiện nhiều lần sẽ được cộng dồn
     * @param borrowDate ngày mượn, {@code null} thì lấy ngày hôm nay
     * @param dueDate    ngày hẹn trả, {@code null} thì lấy ngày mượn + {@value #DEFAULT_BORROW_DAYS} ngày
     * @return phiếu mượn vừa được lưu, trạng thái "Đang mượn"
     * @throws InvalidBorrowRequestException yêu cầu thiếu dữ liệu hoặc số lượng không hợp lệ
     * @throws ReaderNotFoundException       không tìm thấy bạn đọc
     * @throws BookNotFoundException         không tìm thấy một đầu sách trong yêu cầu
     * @throws BorrowLimitExceededException  tổng số sách đang mượn và muốn mượn vượt hạn mức
     * @throws BookOutOfStockException       một đầu sách không còn đủ số lượng
     */
    public synchronized BorrowTicket borrow(String readerId, List<BorrowTicketDetail> items,
                                            LocalDate borrowDate, LocalDate dueDate) {
        // 1. Kiểm tra dữ liệu yêu cầu
        if (readerId == null || readerId.isBlank()) {
            throw new InvalidBorrowRequestException("Mã bạn đọc không được để trống.");
        }
        Map<String, Integer> requested = mergeItems(items);
        LocalDate actualBorrowDate = borrowDate != null ? borrowDate : LocalDate.now();
        LocalDate actualDueDate = dueDate != null ? dueDate : actualBorrowDate.plusDays(DEFAULT_BORROW_DAYS);
        if (actualDueDate.isBefore(actualBorrowDate)) {
            throw new InvalidBorrowRequestException(
                    "Ngày hẹn trả " + actualDueDate + " không được trước ngày mượn " + actualBorrowDate + ".");
        }

        // 2. Bạn đọc phải tồn tại
        String normalizedReaderId = readerId.trim();
        Reader reader = readerRepository.findById(normalizedReaderId)
                .orElseThrow(() -> new ReaderNotFoundException(
                        "Không tìm thấy bạn đọc có mã '" + normalizedReaderId + "'."));

        // 3. Mọi đầu sách phải tồn tại
        Map<String, Book> books = new LinkedHashMap<>();
        for (String bookId : requested.keySet()) {
            Book book = bookRepository.findById(bookId)
                    .orElseThrow(() -> new BookNotFoundException("Không tìm thấy sách có mã '" + bookId + "'."));
            books.put(bookId, book);
        }

        // 4. Không vượt hạn mức mượn
        int requestedTotal = requested.values().stream().mapToInt(Integer::intValue).sum();
        int currentlyBorrowing = countBorrowingBooks(reader.getId());
        int limit = reader.getMaxBorrowLimit();
        if (currentlyBorrowing + requestedTotal > limit) {
            throw new BorrowLimitExceededException(
                    "Bạn đọc '" + reader.getId() + "' đang mượn " + currentlyBorrowing + " cuốn, "
                            + "mượn thêm " + requestedTotal + " cuốn sẽ vượt hạn mức " + limit + " cuốn.");
        }

        // 5. Mọi đầu sách phải còn đủ số lượng (kiểm tra hết trước khi trừ kho)
        for (Map.Entry<String, Integer> entry : requested.entrySet()) {
            Book book = books.get(entry.getKey());
            int quantity = entry.getValue();
            if (!book.hasEnough(quantity)) {
                throw new BookOutOfStockException(
                        "Sách '" + book.getId() + "' chỉ còn " + book.getAvailableQuantity()
                                + " cuốn, không đủ để mượn " + quantity + " cuốn.");
            }
        }

        // Mọi điều kiện đã đạt: trừ kho và lập phiếu
        List<BorrowTicketDetail> details = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : requested.entrySet()) {
            Book book = books.get(entry.getKey());
            int quantity = entry.getValue();
            book.borrowItem(quantity);
            details.add(new BorrowTicketDetail(book.getId(), book.getTitle(), quantity));
        }

        BorrowTicket ticket = new BorrowTicket(nextTicketId(), reader.getId(), details,
                actualBorrowDate, actualDueDate);

        books.values().forEach(bookRepository::save);
        return ticketRepository.save(ticket);
    }

    /**
     * Đếm tổng số cuốn sách bạn đọc đang mượn (cộng mọi phiếu còn trạng thái "Đang mượn").
     */
    public int countBorrowingBooks(String readerId) {
        return ticketRepository.findByReaderId(readerId).stream()
                .filter(BorrowTicket::isBorrowing)
                .mapToInt(BorrowTicket::getTotalQuantity)
                .sum();
    }

    /**
     * Kiểm tra từng dòng yêu cầu và gộp các dòng trùng mã sách, giữ nguyên thứ tự xuất hiện.
     */
    private Map<String, Integer> mergeItems(List<BorrowTicketDetail> items) {
        if (items == null || items.isEmpty()) {
            throw new InvalidBorrowRequestException("Phiếu mượn phải có ít nhất một đầu sách.");
        }
        Map<String, Integer> merged = new LinkedHashMap<>();
        for (BorrowTicketDetail item : items) {
            if (item == null || item.getBookId() == null || item.getBookId().isBlank()) {
                throw new InvalidBorrowRequestException("Mã sách trong yêu cầu mượn không được để trống.");
            }
            String bookId = item.getBookId().trim();
            if (item.getQuantity() <= 0) {
                throw new InvalidBorrowRequestException(
                        "Số lượng mượn sách '" + bookId + "' phải lớn hơn 0 (đang là " + item.getQuantity() + ").");
            }
            merged.merge(bookId, item.getQuantity(), Integer::sum);
        }
        return merged;
    }

    private String nextTicketId() {
        List<String> existingIds = ticketRepository.findAll().stream()
                .map(BorrowTicket::getId)
                .collect(Collectors.toList());
        return IdGenerator.next(TICKET_ID_PREFIX, existingIds);
    }
}
