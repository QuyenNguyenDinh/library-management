package com.nhom04.library.service;

import com.nhom04.library.exception.BookNotFoundException;
import com.nhom04.library.exception.BookOutOfStockException;
import com.nhom04.library.exception.BorrowLimitExceededException;
import com.nhom04.library.exception.InvalidBorrowRequestException;
import com.nhom04.library.exception.ReaderNotFoundException;
import com.nhom04.library.model.Book;
import com.nhom04.library.model.BorrowTicket;
import com.nhom04.library.model.BorrowTicketDetail;
import com.nhom04.library.model.LecturerReader;
import com.nhom04.library.model.StudentReader;
import com.nhom04.library.model.TicketStatus;
import com.nhom04.library.repository.BookRepository;
import com.nhom04.library.repository.ReaderRepository;
import com.nhom04.library.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Kiểm thử nghiệp vụ mượn sách (tình huống test 1–5 của TV1) trên dữ liệu JSON trong thư mục tạm.
 */
class BorrowServiceTest {

    private static final LocalDate BORROW_DATE = LocalDate.of(2026, 11, 13);

    @TempDir
    Path dataDir;

    private BookRepository bookRepository;
    private ReaderRepository readerRepository;
    private TicketRepository ticketRepository;
    private BorrowService borrowService;

    @BeforeEach
    void setUp() {
        String dir = dataDir.toString();
        bookRepository = new BookRepository(dir);
        readerRepository = new ReaderRepository(dir);
        ticketRepository = new TicketRepository(dir);
        borrowService = new BorrowService(bookRepository, readerRepository, ticketRepository);

        bookRepository.save(new Book("BK001", "Lập trình hướng đối tượng với Java", "Nguyễn Văn A", "CNTT", 5, 120000));
        bookRepository.save(new Book("BK002", "Cấu trúc dữ liệu và giải thuật", "Trần Văn B", "CNTT", 0, 95000));
        bookRepository.save(new Book("BK003", "Mạng máy tính", "Lê Thị C", "CNTT", 10, 110000));

        readerRepository.save(new StudentReader("RD001", "Trần Thị B", "0901234567"));
        readerRepository.save(new LecturerReader("RD002", "Phạm Văn D", "0907654321"));
    }

    private static List<BorrowTicketDetail> items(Object... bookIdAndQuantity) {
        BorrowTicketDetail[] details = new BorrowTicketDetail[bookIdAndQuantity.length / 2];
        for (int i = 0; i < details.length; i++) {
            BorrowTicketDetail detail = new BorrowTicketDetail();
            detail.setBookId((String) bookIdAndQuantity[2 * i]);
            detail.setQuantity((Integer) bookIdAndQuantity[2 * i + 1]);
            details[i] = detail;
        }
        return List.of(details);
    }

    private int stockOf(String bookId) {
        return bookRepository.findById(bookId).orElseThrow().getAvailableQuantity();
    }

    /** Lưu sẵn một phiếu "Đang mượn" để giả lập bạn đọc đã mượn trước đó. */
    private void givenBorrowingTicket(String ticketId, String readerId, int quantity) {
        BorrowTicket ticket = new BorrowTicket(ticketId, readerId,
                List.of(new BorrowTicketDetail("BK003", "Mạng máy tính", quantity)),
                BORROW_DATE.minusDays(3), BORROW_DATE.plusDays(11));
        ticketRepository.save(ticket);
    }

    // ===================== Tình huống 1: mượn thành công =====================

    @Test
    @DisplayName("TH1: RD001 mượn 1 cuốn BK001 (còn 5) -> lập phiếu Đang mượn, BK001 còn 4")
    void borrow_success_createsTicketAndDecreasesStock() {
        BorrowTicket ticket = borrowService.borrow("RD001", items("BK001", 1), BORROW_DATE, null);

        assertEquals("TK001", ticket.getId());
        assertEquals("RD001", ticket.getReaderId());
        assertEquals(TicketStatus.BORROWING, ticket.getStatus());
        assertEquals(BORROW_DATE, ticket.getBorrowDate());
        assertEquals(BORROW_DATE.plusDays(BorrowService.DEFAULT_BORROW_DAYS), ticket.getDueDate());
        assertNull(ticket.getReturnDate());
        assertEquals(1, ticket.getDetails().size());
        assertEquals("Lập trình hướng đối tượng với Java", ticket.getDetails().get(0).getBookTitle());

        assertEquals(4, stockOf("BK001"));
        assertTrue(ticketRepository.findById("TK001").isPresent(), "Phiếu phải được ghi vào tickets.json");
    }

    @Test
    @DisplayName("Không gửi ngày mượn -> lấy ngày hôm nay, hạn trả +14 ngày")
    void borrow_withoutDates_usesTodayAndDefaultDueDate() {
        BorrowTicket ticket = borrowService.borrow("RD001", items("BK001", 1), null, null);

        assertEquals(LocalDate.now(), ticket.getBorrowDate());
        assertEquals(LocalDate.now().plusDays(14), ticket.getDueDate());
    }

    @Test
    @DisplayName("Mượn nhiều đầu sách, mã sách trùng được cộng dồn, mã phiếu tăng dần")
    void borrow_multipleBooks_mergesDuplicatesAndIncrementsId() {
        borrowService.borrow("RD002", items("BK001", 1), BORROW_DATE, null);
        BorrowTicket second = borrowService.borrow("RD002", items("BK003", 2, "BK001", 1, "BK003", 1), BORROW_DATE, null);

        assertEquals("TK002", second.getId());
        assertEquals(2, second.getDetails().size(), "BK003 xuất hiện 2 lần phải gộp thành 1 dòng");
        assertEquals(4, second.getTotalQuantity());
        assertEquals(3, stockOf("BK001"));
        assertEquals(7, stockOf("BK003"));
    }

    // ===================== Tình huống 2: hết sách =====================

    @Test
    @DisplayName("TH2: mượn sách có số lượng hiện có = 0 -> BookOutOfStockException (409)")
    void borrow_outOfStock_throws() {
        BookOutOfStockException e = assertThrows(BookOutOfStockException.class,
                () -> borrowService.borrow("RD001", items("BK002", 1), BORROW_DATE, null));

        assertTrue(e.getMessage().contains("BK002"));
        assertTrue(ticketRepository.findAll().isEmpty());
    }

    @Test
    @DisplayName("Một đầu sách không đủ thì cả phiếu bị từ chối, không đầu sách nào bị trừ kho")
    void borrow_oneBookOutOfStock_changesNothing() {
        assertThrows(BookOutOfStockException.class,
                () -> borrowService.borrow("RD002", items("BK001", 2, "BK002", 1), BORROW_DATE, null));

        assertEquals(5, stockOf("BK001"));
        assertTrue(ticketRepository.findAll().isEmpty());
    }

    // ===================== Tình huống 3: số lượng không hợp lệ =====================

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    @DisplayName("TH3: mượn với quantity = 0 hoặc âm -> InvalidBorrowRequestException (400)")
    void borrow_nonPositiveQuantity_throws(int quantity) {
        assertThrows(InvalidBorrowRequestException.class,
                () -> borrowService.borrow("RD001", items("BK001", quantity), BORROW_DATE, null));

        assertEquals(5, stockOf("BK001"));
    }

    @Test
    @DisplayName("Thiếu mã bạn đọc, danh sách sách rỗng hoặc hạn trả trước ngày mượn -> 400")
    void borrow_invalidRequest_throws() {
        assertThrows(InvalidBorrowRequestException.class,
                () -> borrowService.borrow(" ", items("BK001", 1), BORROW_DATE, null));
        assertThrows(InvalidBorrowRequestException.class,
                () -> borrowService.borrow("RD001", List.of(), BORROW_DATE, null));
        assertThrows(InvalidBorrowRequestException.class,
                () -> borrowService.borrow("RD001", null, BORROW_DATE, null));
        assertThrows(InvalidBorrowRequestException.class,
                () -> borrowService.borrow("RD001", items("BK001", 1), BORROW_DATE, BORROW_DATE.minusDays(1)));
    }

    // ===================== Tình huống 4: không tồn tại =====================

    @Test
    @DisplayName("TH4a: mượn sách không tồn tại -> BookNotFoundException (404)")
    void borrow_bookNotFound_throws() {
        assertThrows(BookNotFoundException.class,
                () -> borrowService.borrow("RD001", items("BK999", 1), BORROW_DATE, null));
    }

    @Test
    @DisplayName("TH4b: bạn đọc không tồn tại -> ReaderNotFoundException (404)")
    void borrow_readerNotFound_throws() {
        assertThrows(ReaderNotFoundException.class,
                () -> borrowService.borrow("RD999", items("BK001", 1), BORROW_DATE, null));
    }

    // ===================== Tình huống 5: vượt hạn mức =====================

    @Test
    @DisplayName("TH5: SV thường đang mượn 3, mượn thêm 1 -> BorrowLimitExceededException (409)")
    void borrow_studentOverLimit_throws() {
        givenBorrowingTicket("TK001", "RD001", 3);

        BorrowLimitExceededException e = assertThrows(BorrowLimitExceededException.class,
                () -> borrowService.borrow("RD001", items("BK001", 1), BORROW_DATE, null));

        assertTrue(e.getMessage().contains("3"));
        assertEquals(5, stockOf("BK001"));
        assertEquals(1, ticketRepository.findAll().size());
    }

    @Test
    @DisplayName("SV thường mượn một lần 4 cuốn cũng vượt hạn mức 3")
    void borrow_studentRequestsMoreThanLimitAtOnce_throws() {
        assertThrows(BorrowLimitExceededException.class,
                () -> borrowService.borrow("RD001", items("BK001", 4), BORROW_DATE, null));
    }

    @Test
    @DisplayName("Giảng viên (hạn mức 10) đang mượn 3 vẫn mượn thêm được")
    void borrow_lecturerHasHigherLimit() {
        givenBorrowingTicket("TK001", "RD002", 3);

        BorrowTicket ticket = borrowService.borrow("RD002", items("BK001", 4), BORROW_DATE, null);

        assertEquals("TK002", ticket.getId());
        assertEquals(1, stockOf("BK001"));
    }

    @Test
    @DisplayName("Phiếu đã trả không tính vào số sách đang mượn")
    void borrow_returnedTicketsDoNotCountTowardsLimit() {
        givenBorrowingTicket("TK001", "RD001", 3);
        BorrowTicket returned = ticketRepository.findById("TK001").orElseThrow();
        returned.setReturnDate(BORROW_DATE);
        returned.setStatus(TicketStatus.RETURNED);
        ticketRepository.save(returned);

        assertEquals(0, borrowService.countBorrowingBooks("RD001"));
        borrowService.borrow("RD001", items("BK001", 3), BORROW_DATE, null);
        assertEquals(3, borrowService.countBorrowingBooks("RD001"));
    }
}
