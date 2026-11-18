package com.nhom04.library.service;

import com.nhom04.library.exception.BookNotFoundException;
import com.nhom04.library.exception.InvalidReturnRequestException;
import com.nhom04.library.exception.InvalidTicketStateException;
import com.nhom04.library.exception.ReaderNotFoundException;
import com.nhom04.library.exception.TicketNotFoundException;
import com.nhom04.library.model.Book;
import com.nhom04.library.model.BorrowTicket;
import com.nhom04.library.model.BorrowTicketDetail;
import com.nhom04.library.model.Reader;
import com.nhom04.library.model.ReturnRecord;
import com.nhom04.library.model.TicketStatus;
import com.nhom04.library.policy.FinePolicy;
import com.nhom04.library.repository.BookRepository;
import com.nhom04.library.repository.ReaderRepository;
import com.nhom04.library.repository.ReturnRecordRepository;
import com.nhom04.library.repository.TicketRepository;
import com.nhom04.library.utils.IdGenerator;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Nghiệp vụ trả sách: kiểm tra phiếu, tính tiền phạt theo chính sách của bạn đọc,
 * cộng lại sách vào kho, đóng phiếu và ghi lịch sử trả.
 * <p>
 * Thứ tự kiểm tra (lỗi nào gặp trước thì báo trước):
 * <ol>
 *     <li>Có mã phiếu (400)</li>
 *     <li>Phiếu tồn tại (404)</li>
 *     <li>Phiếu đang ở trạng thái "Đang mượn", chưa trả (409)</li>
 *     <li>Ngày trả không trước ngày mượn (400)</li>
 *     <li>Bạn đọc và từng đầu sách trong phiếu còn tồn tại (404)</li>
 * </ol>
 * Chỉ khi mọi điều kiện đều đạt mới cộng kho, đóng phiếu và ghi lịch sử,
 * nên một yêu cầu bị từ chối sẽ không làm thay đổi dữ liệu.
 * <p>
 * Tiền phạt tính một lần cho cả phiếu: số ngày trễ x đơn giá của nhóm bạn đọc
 * (đa hình qua {@link FinePolicy}, service không cần biết bạn đọc thuộc nhóm nào).
 */
@Service
public class ReturnService {

    private static final String RECORD_ID_PREFIX = "RR";

    private final TicketRepository ticketRepository;
    private final ReaderRepository readerRepository;
    private final BookRepository bookRepository;
    private final ReturnRecordRepository returnRecordRepository;

    public ReturnService(TicketRepository ticketRepository,
                         ReaderRepository readerRepository,
                         BookRepository bookRepository,
                         ReturnRecordRepository returnRecordRepository) {
        this.ticketRepository = ticketRepository;
        this.readerRepository = readerRepository;
        this.bookRepository = bookRepository;
        this.returnRecordRepository = returnRecordRepository;
    }

    /**
     * Ghi nhận bạn đọc trả sách theo phiếu mượn.
     * <p>
     * Đồng bộ hoá để hai yêu cầu trả cùng một phiếu không cùng cộng kho hai lần.
     *
     * @param ticketId   mã phiếu mượn
     * @param returnDate ngày trả, {@code null} thì lấy ngày hôm nay
     * @return bản ghi lịch sử trả vừa được lưu (có số ngày trễ và tiền phạt)
     * @throws InvalidReturnRequestException thiếu mã phiếu hoặc ngày trả trước ngày mượn
     * @throws TicketNotFoundException       không tìm thấy phiếu mượn
     * @throws InvalidTicketStateException   phiếu đã được trả trước đó
     * @throws ReaderNotFoundException       không tìm thấy bạn đọc của phiếu
     * @throws BookNotFoundException         không tìm thấy một đầu sách trong phiếu
     */
    public synchronized ReturnRecord returnBook(String ticketId, LocalDate returnDate) {
        // 1-3. Phiếu hợp lệ và đang mượn
        if (ticketId == null || ticketId.isBlank()) {
            throw new InvalidReturnRequestException("Mã phiếu mượn không được để trống.");
        }
        String normalizedTicketId = ticketId.trim();
        BorrowTicket ticket = ticketRepository.findById(normalizedTicketId)
                .orElseThrow(() -> new TicketNotFoundException(
                        "Không tìm thấy phiếu mượn có mã '" + normalizedTicketId + "'."));
        if (!ticket.isBorrowing()) {
            throw new InvalidTicketStateException(
                    "Phiếu '" + ticket.getId() + "' đã được trả, không thể trả lại lần nữa.");
        }

        // 4. Ngày trả hợp lệ
        LocalDate actualReturnDate = returnDate != null ? returnDate : LocalDate.now();
        if (actualReturnDate.isBefore(ticket.getBorrowDate())) {
            throw new InvalidReturnRequestException("Ngày trả " + actualReturnDate
                    + " không được trước ngày mượn " + ticket.getBorrowDate() + ".");
        }

        // 5. Bạn đọc và mọi đầu sách còn tồn tại
        Reader reader = readerRepository.findById(ticket.getReaderId())
                .orElseThrow(() -> new ReaderNotFoundException(
                        "Không tìm thấy bạn đọc có mã '" + ticket.getReaderId() + "' của phiếu '" + ticket.getId() + "'."));
        List<Book> booksToRestock = new ArrayList<>();
        for (BorrowTicketDetail detail : ticket.getDetails()) {
            Book book = bookRepository.findById(detail.getBookId())
                    .orElseThrow(() -> new BookNotFoundException(
                            "Không tìm thấy sách có mã '" + detail.getBookId() + "' của phiếu '" + ticket.getId() + "'."));
            book.returnItem(detail.getQuantity());
            booksToRestock.add(book);
        }

        // Tính phạt theo chính sách của nhóm bạn đọc
        FinePolicy policy = reader.getFinePolicy();
        long overdueDays = Math.max(0, ChronoUnit.DAYS.between(ticket.getDueDate(), actualReturnDate));
        double fine = policy.calculateFine(overdueDays);

        // Mọi điều kiện đã đạt: cộng kho, đóng phiếu, ghi lịch sử
        booksToRestock.forEach(bookRepository::save);

        ticket.setReturnDate(actualReturnDate);
        ticket.setStatus(TicketStatus.RETURNED);
        ticket.setFineAmount(fine);
        ticketRepository.save(ticket);

        ReturnRecord record = new ReturnRecord(nextRecordId(), ticket.getId(), reader.getId(),
                actualReturnDate, overdueDays, fine, policy.getPolicyName());
        return returnRecordRepository.save(record);
    }

    /** @return toàn bộ lịch sử trả sách, theo thứ tự đã lưu (cũ trước, mới sau) */
    public List<ReturnRecord> getAllReturnRecords() {
        return returnRecordRepository.findAll();
    }

    private String nextRecordId() {
        List<String> existingIds = returnRecordRepository.findAll().stream()
                .map(ReturnRecord::getId)
                .collect(Collectors.toList());
        return IdGenerator.next(RECORD_ID_PREFIX, existingIds);
    }
}
