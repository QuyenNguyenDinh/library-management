package com.nhom04.library.service;

import com.nhom04.library.exception.InvalidReturnRequestException;
import com.nhom04.library.exception.InvalidTicketStateException;
import com.nhom04.library.exception.TicketNotFoundException;
import com.nhom04.library.model.Book;
import com.nhom04.library.model.BorrowTicket;
import com.nhom04.library.model.BorrowTicketDetail;
import com.nhom04.library.model.LecturerReader;
import com.nhom04.library.model.PriorityStudentReader;
import com.nhom04.library.model.ReturnRecord;
import com.nhom04.library.model.StudentReader;
import com.nhom04.library.model.TicketStatus;
import com.nhom04.library.repository.BookRepository;
import com.nhom04.library.repository.ReaderRepository;
import com.nhom04.library.repository.ReturnRecordRepository;
import com.nhom04.library.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Kiểm thử nghiệp vụ trả sách (tình huống test 6–9 của TV4) trên dữ liệu JSON trong thư mục tạm,
 * không đụng tới backend/data/ thật.
 */
class ReturnServiceTest {

    private static final LocalDate BORROW_DATE = LocalDate.of(2026, 11, 1);
    private static final LocalDate DUE_DATE = LocalDate.of(2026, 11, 15);

    @TempDir
    Path dataDir;

    private BookRepository bookRepository;
    private TicketRepository ticketRepository;
    private ReturnRecordRepository returnRecordRepository;
    private ReturnService returnService;

    @BeforeEach
    void setUp() {
        String dir = dataDir.toString();
        bookRepository = new BookRepository(dir);
        ReaderRepository readerRepository = new ReaderRepository(dir);
        ticketRepository = new TicketRepository(dir);
        returnRecordRepository = new ReturnRecordRepository(dir);
        returnService = new ReturnService(ticketRepository, readerRepository, bookRepository, returnRecordRepository);

        // Kho chỉ còn 4 cuốn vì 1 cuốn đang được mượn
        bookRepository.save(new Book("BK001", "Lập trình hướng đối tượng với Java", "Nguyễn Văn A", "CNTT", 4, 120000));

        readerRepository.save(new StudentReader("RD001", "Trần Thị B", "0901234567"));
        readerRepository.save(new PriorityStudentReader("RD002", "Lê Văn C", "0912345678"));
        readerRepository.save(new LecturerReader("RD003", "Nguyễn Văn D", "0987654321"));

        givenBorrowingTicket("TK001", "RD001");
        givenBorrowingTicket("TK002", "RD002");
        givenBorrowingTicket("TK003", "RD003");
    }

    /** Lưu sẵn một phiếu "Đang mượn" 1 cuốn BK001 (mượn 01/11, hẹn trả 15/11). */
    private void givenBorrowingTicket(String ticketId, String readerId) {
        BorrowTicketDetail detail = new BorrowTicketDetail("BK001", "Lập trình hướng đối tượng với Java", 1);
        ticketRepository.save(new BorrowTicket(ticketId, readerId, List.of(detail), BORROW_DATE, DUE_DATE));
    }

    private int stockOfBook() {
        return bookRepository.findById("BK001").orElseThrow().getAvailableQuantity();
    }

    // ===================== Tình huống 6: trả đúng hạn =====================

    @Test
    @DisplayName("Test 6: trả đúng hạn -> không phạt, cộng lại kho, phiếu chuyển sang Đã trả")
    void returnOnTime_noFine_stockRestored_ticketReturned() {
        ReturnRecord record = returnService.returnBook("TK001", DUE_DATE);

        assertEquals("RR001", record.getId());
        assertEquals("TK001", record.getTicketId());
        assertEquals("RD001", record.getReaderId());
        assertEquals(0, record.getOverdueDays());
        assertEquals(0, record.getFineAmount());
        assertEquals(5, stockOfBook());

        BorrowTicket ticket = ticketRepository.findById("TK001").orElseThrow();
        assertEquals(TicketStatus.RETURNED, ticket.getStatus());
        assertEquals(DUE_DATE, ticket.getReturnDate());
        assertEquals(0, ticket.getFineAmount());
    }

    @Test
    @DisplayName("Trả sớm hơn hạn cũng không bị phạt")
    void returnEarly_noFine() {
        ReturnRecord record = returnService.returnBook("TK001", LocalDate.of(2026, 11, 5));

        assertEquals(0, record.getOverdueDays());
        assertEquals(0, record.getFineAmount());
    }

    @Test
    @DisplayName("Không gửi ngày trả thì lấy ngày hôm nay")
    void returnWithoutDate_usesToday() {
        // Phiếu mượn 01/11/2026, hôm nay chắc chắn không trước ngày đó
        ReturnRecord record = returnService.returnBook("TK001", null);

        assertEquals(LocalDate.now(), record.getReturnDate());
    }

    // ===================== Tình huống 7, 8: phạt theo nhóm bạn đọc =====================

    @Test
    @DisplayName("Test 7: sinh viên thường trả trễ 4 ngày -> phạt 20.000đ")
    void normalStudent_late4Days_fine20000() {
        ReturnRecord record = returnService.returnBook("TK001", DUE_DATE.plusDays(4));

        assertEquals(4, record.getOverdueDays());
        assertEquals(20000, record.getFineAmount());
        assertEquals("Sinh viên thường (5.000đ/ngày)", record.getPolicyName());
        assertEquals(20000, ticketRepository.findById("TK001").orElseThrow().getFineAmount());
    }

    @Test
    @DisplayName("Test 8: giảng viên trả trễ 4 ngày -> phạt 8.000đ")
    void lecturer_late4Days_fine8000() {
        ReturnRecord record = returnService.returnBook("TK003", DUE_DATE.plusDays(4));

        assertEquals(4, record.getOverdueDays());
        assertEquals(8000, record.getFineAmount());
        assertEquals("Giảng viên (2.000đ/ngày)", record.getPolicyName());
    }

    @Test
    @DisplayName("Sinh viên ưu tiên trả trễ 4 ngày -> phạt 12.000đ")
    void priorityStudent_late4Days_fine12000() {
        ReturnRecord record = returnService.returnBook("TK002", DUE_DATE.plusDays(4));

        assertEquals(12000, record.getFineAmount());
        assertEquals("Sinh viên ưu tiên (3.000đ/ngày)", record.getPolicyName());
    }

    // ===================== Tình huống 9: phiếu không hợp lệ =====================

    @Test
    @DisplayName("Test 9: trả lại phiếu đã trả -> InvalidTicketStateException (409), kho không cộng lần hai")
    void returnTwice_throwsInvalidTicketState() {
        returnService.returnBook("TK001", DUE_DATE);

        assertThrows(InvalidTicketStateException.class, () -> returnService.returnBook("TK001", DUE_DATE));
        assertEquals(5, stockOfBook());
        assertEquals(1, returnRecordRepository.findAll().size());
    }

    @Test
    @DisplayName("Phiếu không tồn tại -> TicketNotFoundException (404)")
    void unknownTicket_throwsNotFound() {
        assertThrows(TicketNotFoundException.class, () -> returnService.returnBook("TK999", DUE_DATE));
    }

    @Test
    @DisplayName("Thiếu mã phiếu -> InvalidReturnRequestException (400)")
    void blankTicketId_throwsInvalidRequest() {
        assertThrows(InvalidReturnRequestException.class, () -> returnService.returnBook("  ", DUE_DATE));
        assertThrows(InvalidReturnRequestException.class, () -> returnService.returnBook(null, DUE_DATE));
    }

    @Test
    @DisplayName("Ngày trả trước ngày mượn -> 400, dữ liệu không thay đổi")
    void returnBeforeBorrowDate_throwsInvalidRequest_andKeepsData() {
        assertThrows(InvalidReturnRequestException.class,
                () -> returnService.returnBook("TK001", BORROW_DATE.minusDays(1)));

        assertEquals(4, stockOfBook());
        assertEquals(TicketStatus.BORROWING, ticketRepository.findById("TK001").orElseThrow().getStatus());
        assertEquals(0, returnRecordRepository.findAll().size());
    }

    // ===================== Lịch sử trả =====================

    @Test
    @DisplayName("Mỗi lần trả ghi một bản ghi lịch sử, mã tăng dần RR001, RR002")
    void eachReturn_addsHistoryRecordWithIncreasingId() {
        returnService.returnBook("TK001", DUE_DATE);
        ReturnRecord second = returnService.returnBook("TK003", DUE_DATE.plusDays(1));

        assertEquals("RR002", second.getId());
        List<ReturnRecord> records = returnService.getAllReturnRecords();
        assertEquals(2, records.size());
        assertNotNull(records.get(0).getPolicyName());
    }
}
