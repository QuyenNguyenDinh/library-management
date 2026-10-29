package service;

import exception.*;
import model.*;
import repository.TicketRepository;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Service xử lý các nghiệp vụ Mượn - Trả sách và Tính phí phạt quá hạn.
 * Thực thi đầy đủ các tiêu chuẩn Đề (5 điều kiện) và tiêu chuẩn Nên (mượn nhiều sách, tùy chỉnh ngày demo).
 */
public class BorrowService {

    private final Map<String, Reader> readerRepository;
    private final Map<String, Book> bookRepository;
    private final TicketRepository ticketRepository;
    private final List<ReturnRecord> returnRecordHistory;
    private int ticketIdCounter = 1;

    public BorrowService() {
        this.readerRepository = new HashMap<>();
        this.bookRepository = new HashMap<>();
        this.ticketRepository = new TicketRepository();
        this.returnRecordHistory = new ArrayList<>();
    }

    public BorrowService(Map<String, Reader> readerRepository,
                         Map<String, Book> bookRepository,
                         TicketRepository ticketRepository) {
        this.readerRepository = readerRepository != null ? readerRepository : new HashMap<>();
        this.bookRepository = bookRepository != null ? bookRepository : new HashMap<>();
        this.ticketRepository = ticketRepository != null ? ticketRepository : new TicketRepository();
        this.returnRecordHistory = new ArrayList<>();
    }

    // =========================================================================
    // HELPER METHODS QUẢN LÝ DỮ LIỆU
    // =========================================================================

    public void addReader(Reader reader) {
        if (reader != null) {
            this.readerRepository.put(reader.getId(), reader);
        }
    }

    public void addBook(Book book) {
        if (book != null) {
            this.bookRepository.put(book.getId(), book);
        }
    }

    public Reader getReader(String readerId) {
        return this.readerRepository.get(readerId);
    }

    public Book getBook(String bookId) {
        return this.bookRepository.get(bookId);
    }

    public BorrowTicket getTicket(String ticketId) {
        return this.ticketRepository.findById(ticketId).orElse(null);
    }

    public List<BorrowTicket> getAllTickets() {
        return this.ticketRepository.findAll();
    }

    public List<ReturnRecord> getReturnRecordHistory() {
        return Collections.unmodifiableList(this.returnRecordHistory);
    }

    public TicketRepository getTicketRepository() {
        return ticketRepository;
    }

    // =========================================================================
    // NGHIỆP VỤ MƯỢN SÁCH (HỖ TRỢ 1 SÁCH HOẶC NHIỀU SÁCH TRÊN 1 PHIẾU)
    // =========================================================================

    /**
     * Mượn 1 cuốn sách (tương thích đơn giản).
     */
    public BorrowTicket borrowBook(String ticketId, String readerId, String bookId, int quantity,
                                  LocalDate borrowDate, LocalDate dueDate) {
        List<BorrowTicketDetail> details = new ArrayList<>();
        details.add(new BorrowTicketDetail(bookId, quantity));
        return borrowBooks(ticketId, readerId, details, borrowDate, dueDate);
    }

    /**
     * Mượn nhiều sách trong một phiếu (Tính năng Nên).
     * Kiểm tra nghiêm ngặt 5 điều kiện mượn sách của đề bài.
     */
    public BorrowTicket borrowBooks(String ticketId, String readerId, List<BorrowTicketDetail> details,
                                   LocalDate borrowDate, LocalDate dueDate) {

        // Điều kiện 1: Kiểm tra Bạn đọc có tồn tại không
        Reader reader = this.readerRepository.get(readerId);
        if (reader == null) {
            throw new ReaderNotFoundException("Không thể mượn sách: Bạn đọc có mã '" + readerId + "' không tồn tại trong hệ thống!");
        }

        if (details == null || details.isEmpty()) {
            throw new InvalidBorrowRequestException("Không thể mượn sách: Danh sách sách yêu cầu mượn không được để trống!");
        }

        // Tính tổng số lượng sách yêu cầu mượn
        int totalRequestedQuantity = 0;
        List<Book> booksToBorrow = new ArrayList<>();

        for (BorrowTicketDetail detail : details) {
            String bookId = detail.getBookId();
            int qty = detail.getQuantity();

            // Điều kiện 3: Kiểm tra số lượng muốn mượn phải > 0
            if (qty <= 0) {
                throw new InvalidBorrowRequestException("Số lượng mượn cho sách mã '" + bookId + "' phải lớn hơn 0 (Yêu cầu: " + qty + ")!");
            }

            // Điều kiện 2: Kiểm tra Sách có tồn tại không
            Book book = this.bookRepository.get(bookId);
            if (book == null) {
                throw new BookNotFoundException("Không thể mượn sách: Sách có mã '" + bookId + "' không tồn tại trong hệ thống!");
            }

            // Điều kiện 4: Kiểm tra sách hiện có trong kho phải còn đủ số lượng
            if (!book.isAvailable() || book.getAvailableQuantity() < qty) {
                throw new BookOutOfStockException("Sách '" + book.getTitle() + "' (Mã: " + bookId + ") không đủ tồn kho! Hiện còn: "
                        + book.getAvailableQuantity() + " cuốn, yêu cầu: " + qty + " cuốn!");
            }

            detail.setBookTitle(book.getTitle());
            detail.setPrice(book.getPrice());
            booksToBorrow.add(book);
            totalRequestedQuantity += qty;
        }

        // Điều kiện 5: Kiểm tra bạn đọc có mượn vượt quá hạn mức tối đa cho phép không
        if (!reader.canBorrow(totalRequestedQuantity)) {
            throw new BorrowLimitExceededException("Bạn đọc '" + reader.getName() + "' (" + reader.getReaderType().getDescription() + ") đã vượt quá hạn mức!"
                    + " [Đang mượn: " + reader.getCurrentBorrowCount() + " cuốn"
                    + ", Muốn mượn thêm: " + totalRequestedQuantity + " cuốn"
                    + ", Hạn mức tối đa: " + reader.getMaxBorrowLimit() + " cuốn]");
        }

        // Hợp lệ: Giảm tồn kho từng cuốn sách
        for (int i = 0; i < details.size(); i++) {
            BorrowTicketDetail detail = details.get(i);
            Book book = booksToBorrow.get(i);
            book.borrowItem(detail.getQuantity());
        }

        // Tăng số lượng sách đang mượn của bạn đọc
        reader.increaseBorrowCount(totalRequestedQuantity);

        // Sinh mã phiếu nếu chưa có
        String finalTicketId = (ticketId != null && !ticketId.trim().isEmpty())
                ? ticketId : "TK" + String.format("%03d", ticketIdCounter++);

        LocalDate finalBorrowDate = borrowDate != null ? borrowDate : LocalDate.now();
        LocalDate finalDueDate = dueDate != null ? dueDate : finalBorrowDate.plusDays(14);

        BorrowTicket ticket = new BorrowTicket(finalTicketId, readerId, details, finalBorrowDate, finalDueDate);
        ticket.setStatus(TicketStatus.BORROWING);

        // Lưu vào repository
        this.ticketRepository.save(ticket);
        return ticket;
    }

    // =========================================================================
    // NGHIỆP VỤ TRẢ SÁCH (HỖ TRỢ NHẬP NGÀY TRẢ ĐỂ DEMO QUÁ HẠN & TÍNH PHẠT)
    // =========================================================================

    public ReturnRecord returnBook(String recordId, String ticketId, LocalDate returnDate) {
        BorrowTicket ticket = this.ticketRepository.findById(ticketId).orElse(null);
        if (ticket == null) {
            throw new TicketNotFoundException("Không thể trả sách: Không tìm thấy phiếu mượn có mã '" + ticketId + "'!");
        }

        // Tuyệt đối không cho trả lại phiếu đã ở trạng thái "Đã trả"
        if (ticket.getStatus() == TicketStatus.RETURNED) {
            throw new InvalidTicketStateException("Tuyệt đối từ chối trả sách: Phiếu mượn mã '" + ticketId + "' đã được trả trước đó!");
        }

        Reader reader = this.readerRepository.get(ticket.getReaderId());
        if (reader == null) {
            throw new ReaderNotFoundException("Không tìm thấy thông tin bạn đọc sở hữu phiếu mượn (Mã: " + ticket.getReaderId() + ")!");
        }

        // Cập nhật ngày trả thực tế (mặc định hôm nay nếu null)
        LocalDate actualReturnDate = returnDate != null ? returnDate : LocalDate.now();
        ticket.setReturnDate(actualReturnDate);

        // Tăng lại số lượng sách trong kho cho tất cả sách trong phiếu
        if (ticket.getDetails() != null && !ticket.getDetails().isEmpty()) {
            for (BorrowTicketDetail detail : ticket.getDetails()) {
                Book book = this.bookRepository.get(detail.getBookId());
                if (book != null) {
                    book.returnItem(detail.getQuantity());
                }
            }
        } else if (ticket.getBookId() != null) {
            Book book = this.bookRepository.get(ticket.getBookId());
            if (book != null) {
                book.returnItem(ticket.getQuantity());
            }
        }

        // Giảm số lượng sách đang mượn của bạn đọc
        reader.decreaseBorrowCount(ticket.getQuantity());

        // Kiểm tra quá hạn và tính phí phạt qua FinePolicy
        long overdueDays = 0;
        double fineAmount = 0.0;
        String note;

        if (actualReturnDate.isAfter(ticket.getDueDate())) {
            overdueDays = ChronoUnit.DAYS.between(ticket.getDueDate(), actualReturnDate);
            fineAmount = reader.calculateFine(overdueDays);
            note = "Trả trễ hạn " + overdueDays + " ngày. Áp dụng "
                    + (reader.getFinePolicy() != null ? reader.getFinePolicy().getPolicyName() : "phạt")
                    + " -> Phí phạt: " + String.format("%,.0f VNĐ", fineAmount);
        } else {
            note = "Trả sách đúng hạn hoặc trước hạn. Không phát sinh phí phạt.";
        }

        ticket.setFineAmount(fineAmount);
        ticket.setStatus(TicketStatus.RETURNED);

        // Lưu cập nhật phiếu mượn vào repository (ghi file)
        this.ticketRepository.save(ticket);

        String finalRecordId = (recordId != null && !recordId.trim().isEmpty())
                ? recordId : "RR" + String.format("%03d", (returnRecordHistory.size() + 1));

        ReturnRecord returnRecord = new ReturnRecord(
                finalRecordId,
                ticket.getId(),
                reader.getId(),
                ticket.getBookId(),
                ticket.getQuantity(),
                actualReturnDate,
                overdueDays,
                fineAmount,
                note
        );

        this.returnRecordHistory.add(returnRecord);
        this.ticketRepository.saveReturnRecord(returnRecord);

        return returnRecord;
    }
}
