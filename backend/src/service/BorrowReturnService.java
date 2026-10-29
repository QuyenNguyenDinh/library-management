package service;

import exception.*;
import model.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Service xử lý các nghiệp vụ cốt lõi Mượn - Trả sách và Tính phí phạt quá hạn.
 * Tuân thủ nghiêm ngặt 7 Business Rules bắt buộc của đề bài.
 */
public class BorrowReturnService {

    private final Map<String, Reader> readerRepository;
    private final Map<String, Book> bookRepository;
    private final Map<String, BorrowTicket> ticketRepository;
    private final List<ReturnRecord> returnRecordHistory;

    public BorrowReturnService() {
        this.readerRepository = new HashMap<>();
        this.bookRepository = new HashMap<>();
        this.ticketRepository = new HashMap<>();
        this.returnRecordHistory = new ArrayList<>();
    }

    public BorrowReturnService(Map<String, Reader> readerRepository,
                               Map<String, Book> bookRepository,
                               Map<String, BorrowTicket> ticketRepository) {
        this.readerRepository = readerRepository != null ? readerRepository : new HashMap<>();
        this.bookRepository = bookRepository != null ? bookRepository : new HashMap<>();
        this.ticketRepository = ticketRepository != null ? ticketRepository : new HashMap<>();
        this.returnRecordHistory = new ArrayList<>();
    }

    // =========================================================================
    // HELPER METHODS ĐĂNG KÝ VÀ TRA CỨU DỮ LIỆU
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

    public void addTicket(BorrowTicket ticket) {
        if (ticket != null) {
            this.ticketRepository.put(ticket.getId(), ticket);
        }
    }

    public Reader getReader(String readerId) {
        return this.readerRepository.get(readerId);
    }

    public Book getBook(String bookId) {
        return this.bookRepository.get(bookId);
    }

    public BorrowTicket getTicket(String ticketId) {
        return this.ticketRepository.get(ticketId);
    }

    public List<BorrowTicket> getAllTickets() {
        return new ArrayList<>(this.ticketRepository.values());
    }

    public List<ReturnRecord> getReturnRecordHistory() {
        return Collections.unmodifiableList(this.returnRecordHistory);
    }

    // =========================================================================
    // NGHIỆP VỤ MƯỢN SÁCH (BORROW BOOK) - TUÂN THỦ STRICT RULES 1, 2, 3, 4, 5
    // =========================================================================

    /**
     * Thực hiện nghiệp vụ Mượn sách theo mã bạn đọc và mã sách.
     *
     * @param ticketId   Mã phiếu mượn được cấp
     * @param readerId   Mã bạn đọc
     * @param bookId     Mã sách
     * @param quantity   Số lượng muốn mượn
     * @param borrowDate Ngày mượn
     * @param dueDate    Ngày hẹn trả
     * @return Đối tượng BorrowTicket vừa tạo
     */
    public BorrowTicket borrowBook(String ticketId, String readerId, String bookId, int quantity,
                                  LocalDate borrowDate, LocalDate dueDate) {
        // Business Rule 1: Kiểm tra bạn đọc có tồn tại không và sách có tồn tại không
        Reader reader = this.readerRepository.get(readerId);
        if (reader == null) {
            throw new ReaderNotFoundException("Không thể mượn sách: Bạn đọc có mã '" + readerId + "' không tồn tại trong hệ thống!");
        }

        Book book = this.bookRepository.get(bookId);
        if (book == null) {
            throw new BookNotFoundException("Không thể mượn sách: Sách có mã '" + bookId + "' không tồn tại trong hệ thống!");
        }

        return borrowBook(ticketId, reader, book, quantity, borrowDate, dueDate);
    }

    /**
     * Overload phương thức Mượn sách nhận trực tiếp đối tượng Reader và Book.
     */
    public BorrowTicket borrowBook(String ticketId, Reader reader, Book book, int quantity,
                                  LocalDate borrowDate, LocalDate dueDate) {
        // Business Rule 1: Kiểm tra bạn đọc và sách có null không
        if (reader == null) {
            throw new ReaderNotFoundException("Không thể mượn sách: Đối tượng Bạn đọc là null!");
        }
        if (book == null) {
            throw new BookNotFoundException("Không thể mượn sách: Đối tượng Sách là null!");
        }

        // Business Rule 2: Kiểm tra số lượng muốn mượn phải > 0 và sách trong kho phải > 0 (còn đủ số lượng)
        if (quantity <= 0) {
            throw new InvalidBorrowRequestException("Không thể mượn sách: Số lượng muốn mượn phải > 0 (Yêu cầu: " + quantity + ")!");
        }
        if (!book.isAvailable() || book.getAvailableQuantity() <= 0) {
            throw new BookOutOfStockException("Không thể mượn sách: Sách '" + book.getTitle() + "' (Mã: " + book.getId() + ") đã hết hàng trong kho (Số lượng còn: 0)!");
        }
        if (book.getAvailableQuantity() < quantity) {
            throw new BookOutOfStockException("Không thể mượn sách: Sách '" + book.getTitle() + "' không đủ số lượng trong kho! Hiện còn: "
                    + book.getAvailableQuantity() + " cuốn, yêu cầu mượn: " + quantity + " cuốn!");
        }

        // Business Rule 3: Kiểm tra bạn đọc có đang mượn vượt quá số lượng sách tối đa cho phép không
        if (!reader.canBorrow(quantity)) {
            throw new BorrowLimitExceededException("Không thể mượn sách: Bạn đọc '" + reader.getName()
                    + "' (" + reader.getReaderType().getDescription() + ") đã vượt quá hạn mức cho phép!"
                    + " [Đang mượn: " + reader.getCurrentBorrowCount() + " cuốn"
                    + ", Muốn mượn thêm: " + quantity + " cuốn"
                    + ", Hạn mức tối đa: " + reader.getMaxBorrowLimit() + " cuốn]");
        }

        // Business Rule 4: Nếu hợp lệ:
        // - Tạo phiếu mượn (BorrowTicket)
        // - Lưu ngày mượn, ngày hẹn trả
        // - Giảm số lượng sách hiện có trong kho
        // - Tăng số sách đang mượn của bạn đọc
        // - Cập nhật trạng thái phiếu là "Đang mượn"
        book.borrowItem(quantity);
        reader.increaseBorrowCount(quantity);

        BorrowTicket ticket = new BorrowTicket(
                ticketId,
                reader.getId(),
                book.getId(),
                quantity,
                borrowDate,
                dueDate
        );
        ticket.setStatus(TicketStatus.BORROWING);

        this.ticketRepository.put(ticket.getId(), ticket);
        return ticket;
        // Business Rule 5: Nếu không hợp lệ -> Đã ném Exception tương ứng cụ thể ở trên
    }

    // =========================================================================
    // NGHIỆP VỤ TRẢ SÁCH & TÍNH PHÍ PHẠT - TUÂN THỦ STRICT RULES 6, 7
    // =========================================================================

    /**
     * Thực hiện nghiệp vụ Trả sách theo mã phiếu mượn.
     *
     * @param recordId   Mã bản ghi trả sách được cấp
     * @param ticketId   Mã phiếu mượn cần trả
     * @param returnDate Ngày trả thực tế
     * @return Bản ghi ReturnRecord chứa chi tiết trả sách và phí phạt
     */
    public ReturnRecord returnBook(String recordId, String ticketId, LocalDate returnDate) {
        // Business Rule 6 (Phần 1): Kiểm tra phiếu mượn có tồn tại không
        BorrowTicket ticket = this.ticketRepository.get(ticketId);
        if (ticket == null) {
            throw new TicketNotFoundException("Không thể trả sách: Phiếu mượn có mã '" + ticketId + "' không tồn tại trong hệ thống!");
        }

        Reader reader = this.readerRepository.get(ticket.getReaderId());
        if (reader == null) {
            throw new ReaderNotFoundException("Không tìm thấy thông tin Bạn đọc tương ứng với phiếu mượn (Mã BĐ: " + ticket.getReaderId() + ")!");
        }

        Book book = this.bookRepository.get(ticket.getBookId());
        if (book == null) {
            throw new BookNotFoundException("Không tìm thấy thông tin Sách tương ứng với phiếu mượn (Mã Sách: " + ticket.getBookId() + ")!");
        }

        return returnBook(recordId, ticket, reader, book, returnDate);
    }

    /**
     * Overload phương thức Trả sách nhận trực tiếp đối tượng BorrowTicket, Reader, Book.
     */
    public ReturnRecord returnBook(String recordId, BorrowTicket ticket, Reader reader, Book book, LocalDate returnDate) {
        // Business Rule 6 (Phần 1): Kiểm tra phiếu mượn có null không
        if (ticket == null) {
            throw new TicketNotFoundException("Không thể trả sách: Đối tượng phiếu mượn là null!");
        }

        // Business Rule 6 (Phần 2): Kiểm tra có đang ở trạng thái "Đang mượn" không
        // Tuyệt đối không cho trả lại phiếu đã ở trạng thái "Đã trả"
        if (ticket.getStatus() == TicketStatus.RETURNED) {
            throw new InvalidTicketStateException("Tuyệt đối từ chối trả sách: Phiếu mượn mã '"
                    + ticket.getId() + "' đã ở trạng thái 'Đã trả' trước đó vào ngày " + ticket.getReturnDate() + "!");
        }
        if (ticket.getStatus() != TicketStatus.BORROWING) {
            throw new InvalidTicketStateException("Không thể trả sách: Trạng thái phiếu mượn không hợp lệ (" + ticket.getStatus() + ")!");
        }

        // Business Rule 7.1: Cập nhật ngày trả thực tế và tăng lại số lượng sách hiện có trong kho
        ticket.setReturnDate(returnDate);
        book.returnItem(ticket.getQuantity());
        reader.decreaseBorrowCount(ticket.getQuantity());

        // Business Rule 7.2: Kiểm tra nếu trả sau ngày hẹn trả thì bắt buộc gọi FinePolicy để tính phí phạt theo đúng loại bạn đọc
        long overdueDays = 0;
        double fineAmount = 0.0;
        String note;

        if (returnDate.isAfter(ticket.getDueDate())) {
            overdueDays = ChronoUnit.DAYS.between(ticket.getDueDate(), returnDate);
            fineAmount = reader.calculateFine(overdueDays);
            note = "Trả trễ hạn " + overdueDays + " ngày. Áp dụng " + (reader.getFinePolicy() != null ? reader.getFinePolicy().getPolicyName() : "phạt")
                    + " -> Phí phạt: " + String.format("%,.0f VNĐ", fineAmount);
        } else {
            note = "Trả sách đúng hạn hoặc trước hạn. Không phát sinh phí phạt.";
        }

        ticket.setFineAmount(fineAmount);

        // Business Rule 7.3: Cập nhật trạng thái phiếu mượn thành "Đã trả" và lưu thông tin trả sách (ReturnRecord)
        ticket.setStatus(TicketStatus.RETURNED);

        ReturnRecord returnRecord = new ReturnRecord(
                recordId,
                ticket.getId(),
                reader.getId(),
                book.getId(),
                ticket.getQuantity(),
                returnDate,
                overdueDays,
                fineAmount,
                note
        );

        this.returnRecordHistory.add(returnRecord);
        return returnRecord;
    }
}

