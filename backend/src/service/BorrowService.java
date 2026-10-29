package service;

import exception.*;
import model.*;
import policy.Borrowable;
import policy.FinePolicy;
import repository.TicketRepository;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Service xử lý các nghiệp vụ Mượn - Trả sách và Tính phí phạt quá hạn.
 * Làm việc trực tiếp với các interface Borrowable và FinePolicy do Trưởng nhóm thiết kế.
 * Không phụ thuộc hay làm thay các lớp Book (TV3) và Reader (TV2).
 */
public class BorrowService {

    private final Map<String, Borrowable> itemRepository;          // Danh mục tài nguyên có thể mượn (Borrowable)
    private final Map<String, Integer> readerBorrowLimits;         // Hạn mức mượn tối đa của bạn đọc
    private final Map<String, Integer> readerCurrentBorrowCounts;  // Số lượng sách bạn đọc đang mượn
    private final Map<String, FinePolicy> readerFinePolicies;      // Chính sách phạt của bạn đọc
    private final TicketRepository ticketRepository;
    private final List<ReturnRecord> returnRecordHistory;
    private int ticketIdCounter = 1;

    public BorrowService() {
        this(new TicketRepository());
    }

    public BorrowService(TicketRepository ticketRepository) {
        this.itemRepository = new HashMap<>();
        this.readerBorrowLimits = new HashMap<>();
        this.readerCurrentBorrowCounts = new HashMap<>();
        this.readerFinePolicies = new HashMap<>();
        this.ticketRepository = ticketRepository != null ? ticketRepository : new TicketRepository();
        this.returnRecordHistory = new ArrayList<>();
    }

    /**
     * Đăng ký thông tin bạn đọc và hạn mức mượn để phục vụ kiểm tra 5 điều kiện mượn.
     */
    public void registerReader(String readerId, int maxLimit, FinePolicy policy) {
        if (readerId != null) {
            readerBorrowLimits.put(readerId, maxLimit);
            readerCurrentBorrowCounts.putIfAbsent(readerId, 0);
            readerFinePolicies.put(readerId, policy);
        }
    }

    /**
     * Đăng ký tài nguyên có thể mượn (triển khai interface Borrowable của Trưởng nhóm).
     */
    public void registerBorrowableItem(String itemId, Borrowable item) {
        if (itemId != null && item != null) {
            itemRepository.put(itemId, item);
        }
    }

    public int getCurrentBorrowCount(String readerId) {
        return readerCurrentBorrowCounts.getOrDefault(readerId, 0);
    }

    public int getMaxBorrowLimit(String readerId) {
        return readerBorrowLimits.getOrDefault(readerId, 0);
    }

    // =========================================================================
    // NGHIỆP VỤ MƯỢN SÁCH: KIỂM TRA 5 ĐIỀU KIỆN [ĐỀ] & MƯỢN NHIỀU SÁCH [NÊN]
    // =========================================================================

    public BorrowTicket borrowBook(String ticketId, String readerId, String bookId, int quantity,
                                  LocalDate borrowDate, LocalDate dueDate) {
        List<BorrowTicketDetail> details = new ArrayList<>();
        details.add(new BorrowTicketDetail(bookId, quantity));
        return borrowBooks(ticketId, readerId, details, borrowDate, dueDate);
    }

    public BorrowTicket borrowBooks(String ticketId, String readerId, List<BorrowTicketDetail> details,
                                   LocalDate borrowDate, LocalDate dueDate) {

        // Điều kiện 1: Kiểm tra Bạn đọc có tồn tại trong hệ thống không
        if (readerId == null || !readerBorrowLimits.containsKey(readerId)) {
            throw new ReaderNotFoundException("Không thể mượn sách: Bạn đọc có mã '" + readerId + "' không tồn tại trong hệ thống!");
        }

        if (details == null || details.isEmpty()) {
            throw new InvalidBorrowRequestException("Không thể mượn sách: Danh sách sách yêu cầu mượn không được để trống!");
        }

        int maxLimit = readerBorrowLimits.get(readerId);
        int currentCount = readerCurrentBorrowCounts.getOrDefault(readerId, 0);

        int totalRequestedQuantity = 0;
        List<Borrowable> itemsToBorrow = new ArrayList<>();

        for (BorrowTicketDetail detail : details) {
            String bookId = detail.getBookId();
            int qty = detail.getQuantity();

            // Điều kiện 3: Số lượng mượn phải > 0
            if (qty <= 0) {
                throw new InvalidBorrowRequestException("Số lượng mượn cho mã sách '" + bookId + "' phải lớn hơn 0 (Yêu cầu: " + qty + ")!");
            }

            // Điều kiện 2: Kiểm tra Sách có tồn tại trong hệ thống không
            Borrowable item = itemRepository.get(bookId);
            if (item == null) {
                throw new BookNotFoundException("Không thể mượn sách: Sách có mã '" + bookId + "' không tồn tại trong hệ thống!");
            }

            // Điều kiện 4: Kiểm tra sách hiện có trong kho phải còn đủ số lượng
            if (!item.isAvailable() || item.getAvailableQuantity() < qty) {
                throw new BookOutOfStockException("Sách có mã '" + bookId + "' không đủ tồn kho để mượn! Hiện còn: "
                        + item.getAvailableQuantity() + " cuốn, yêu cầu mượn: " + qty + " cuốn!");
            }

            itemsToBorrow.add(item);
            totalRequestedQuantity += qty;
        }

        // Điều kiện 5: Kiểm tra bạn đọc có mượn vượt quá hạn mức tối đa cho phép không
        if (currentCount + totalRequestedQuantity > maxLimit) {
            throw new BorrowLimitExceededException("Bạn đọc mã '" + readerId + "' đã vượt quá hạn mức mượn cho phép!"
                    + " [Đang mượn: " + currentCount + " cuốn"
                    + ", Muốn mượn thêm: " + totalRequestedQuantity + " cuốn"
                    + ", Hạn mức tối đa: " + maxLimit + " cuốn]");
        }

        // Hợp lệ: Giảm tồn kho từng tài nguyên (Borrowable)
        for (int i = 0; i < details.size(); i++) {
            BorrowTicketDetail detail = details.get(i);
            Borrowable item = itemsToBorrow.get(i);
            item.borrowItem(detail.getQuantity());
        }

        // Tăng số lượng sách đang mượn của bạn đọc
        readerCurrentBorrowCounts.put(readerId, currentCount + totalRequestedQuantity);

        // Sinh mã phiếu nếu chưa có
        String finalTicketId = (ticketId != null && !ticketId.trim().isEmpty())
                ? ticketId : "TK" + String.format("%03d", ticketIdCounter++);

        LocalDate finalBorrowDate = borrowDate != null ? borrowDate : LocalDate.now();
        LocalDate finalDueDate = dueDate != null ? dueDate : finalBorrowDate.plusDays(14);

        BorrowTicket ticket = new BorrowTicket(finalTicketId, readerId, details, finalBorrowDate, finalDueDate);
        ticket.setStatus(TicketStatus.BORROWING);

        // Lưu phiếu mượn vào repository
        this.ticketRepository.save(ticket);
        return ticket;
    }

    // =========================================================================
    // NGHIỆP VỤ TRẢ SÁCH (TĂNG KHO, TÍNH PHẠT QUA FINEPOLICY, LƯU RETURNRECORD)
    // =========================================================================

    public ReturnRecord returnBook(String recordId, String ticketId, LocalDate returnDate) {
        BorrowTicket ticket = this.ticketRepository.findById(ticketId).orElse(null);
        if (ticket == null) {
            throw new TicketNotFoundException("Không thể trả sách: Không tìm thấy phiếu mượn có mã '" + ticketId + "'!");
        }

        // Kiểm tra trạng thái: Tuyệt đối không cho trả lại phiếu đã ở trạng thái "Đã trả"
        if (ticket.getStatus() == TicketStatus.RETURNED) {
            throw new InvalidTicketStateException("Tuyệt đối từ chối trả sách: Phiếu mượn mã '" + ticketId + "' đã ở trạng thái 'Đã trả' trước đó!");
        }
        if (ticket.getStatus() != TicketStatus.BORROWING) {
            throw new InvalidTicketStateException("Không thể trả sách: Trạng thái phiếu mượn không hợp lệ (" + ticket.getStatus() + ")!");
        }

        // Cập nhật ngày trả thực tế (mặc định hôm nay nếu null)
        LocalDate actualReturnDate = returnDate != null ? returnDate : LocalDate.now();
        ticket.setReturnDate(actualReturnDate);

        // Tăng lại số lượng sách trong kho cho tất cả sách trong phiếu
        if (ticket.getDetails() != null && !ticket.getDetails().isEmpty()) {
            for (BorrowTicketDetail detail : ticket.getDetails()) {
                Borrowable item = itemRepository.get(detail.getBookId());
                if (item != null) {
                    item.returnItem(detail.getQuantity());
                }
            }
        } else if (ticket.getBookId() != null) {
            Borrowable item = itemRepository.get(ticket.getBookId());
            if (item != null) {
                item.returnItem(ticket.getQuantity());
            }
        }

        // Giảm số lượng sách đang mượn của bạn đọc
        int currentCount = readerCurrentBorrowCounts.getOrDefault(ticket.getReaderId(), 0);
        readerCurrentBorrowCounts.put(ticket.getReaderId(), Math.max(0, currentCount - ticket.getQuantity()));

        // Kiểm tra quá hạn và tính phí phạt qua FinePolicy
        long overdueDays = 0;
        double fineAmount = 0.0;
        String note;

        FinePolicy policy = readerFinePolicies.get(ticket.getReaderId());

        if (actualReturnDate.isAfter(ticket.getDueDate())) {
            overdueDays = ChronoUnit.DAYS.between(ticket.getDueDate(), actualReturnDate);
            if (policy != null) {
                fineAmount = policy.calculateFine(overdueDays);
                note = "Trả trễ hạn " + overdueDays + " ngày. Áp dụng " + policy.getPolicyName()
                        + " -> Phí phạt: " + String.format("%,.0f VNĐ", fineAmount);
            } else {
                fineAmount = overdueDays * 5000.0;
                note = "Trả trễ hạn " + overdueDays + " ngày. Phí phạt: " + String.format("%,.0f VNĐ", fineAmount);
            }
        } else {
            note = "Trả sách đúng hạn hoặc trước hạn. Không phát sinh phí phạt.";
        }

        ticket.setFineAmount(fineAmount);
        ticket.setStatus(TicketStatus.RETURNED);

        // Lưu cập nhật phiếu mượn vào repository (ghi file JSON)
        this.ticketRepository.save(ticket);

        String finalRecordId = (recordId != null && !recordId.trim().isEmpty())
                ? recordId : "RR" + String.format("%03d", (returnRecordHistory.size() + 1));

        ReturnRecord returnRecord = new ReturnRecord(
                finalRecordId,
                ticket.getId(),
                ticket.getReaderId(),
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

    public List<BorrowTicket> getAllTickets() {
        return this.ticketRepository.findAll();
    }

    public List<ReturnRecord> getReturnRecordHistory() {
        return Collections.unmodifiableList(this.returnRecordHistory);
    }
}
