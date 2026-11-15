package com.nhom04.library.service;

import com.nhom04.library.exception.DuplicateReaderException;
import com.nhom04.library.exception.ReaderHasActiveBorrowException;
import com.nhom04.library.exception.ReaderNotFoundException;
import com.nhom04.library.model.*;
import com.nhom04.library.repository.ReaderRepository;
import com.nhom04.library.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử đơn vị cho {@link ReaderService}.
 * Tuân thủ quy định: sử dụng @TempDir để không ghi vào thư mục data thật.
 */
class ReaderServiceTest {

    @TempDir
    Path tempDir;

    private ReaderRepository readerRepository;
    private TicketRepository ticketRepository;
    private ReaderService readerService;

    @BeforeEach
    void setUp() {
        String dataDir = tempDir.toString();
        readerRepository = new ReaderRepository(dataDir);
        ticketRepository = new TicketRepository(dataDir);
        readerService = new ReaderService(readerRepository, ticketRepository);
    }

    @Test
    @DisplayName("Thêm mới bạn đọc SV thường, tự động sinh mã RD001 và có hạn mức 3 cuốn")
    void testCreateStudentReader_AutoGenerateId() {
        StudentReader student = new StudentReader(null, "Nguyễn Văn A", "0901112233");
        Reader created = readerService.create(student);

        assertNotNull(created);
        assertEquals("RD001", created.getId());
        assertEquals("Nguyễn Văn A", created.getFullName());
        assertEquals(3, created.getMaxBorrowLimit());
        assertEquals(ReaderType.STUDENT, created.getType());
        assertEquals(5000.0, created.getFinePolicy().getDailyRate());
    }

    @Test
    @DisplayName("Thêm bạn đọc SV ưu tiên và Giảng viên, kiểm tra đa hình hạn mức và chính sách phạt")
    void testCreatePriorityStudentAndLecturer() {
        PriorityStudentReader priorityStudent = new PriorityStudentReader(null, "Trần Thị B", "0912223344");
        LecturerReader lecturer = new LecturerReader(null, "Thầy Hoàng", "0983334455");

        Reader created1 = readerService.create(priorityStudent);
        Reader created2 = readerService.create(lecturer);

        assertEquals("RD001", created1.getId());
        assertEquals(5, created1.getMaxBorrowLimit());
        assertEquals(ReaderType.PRIORITY_STUDENT, created1.getType());
        assertEquals(3000.0, created1.getFinePolicy().getDailyRate());
        assertEquals(12000.0, created1.calculateFine(4)); // 4 ngày trễ = 12.000đ

        assertEquals("RD002", created2.getId());
        assertEquals(10, created2.getMaxBorrowLimit());
        assertEquals(ReaderType.LECTURER, created2.getType());
        assertEquals(2000.0, created2.getFinePolicy().getDailyRate());
        assertEquals(8000.0, created2.calculateFine(4)); // 4 ngày trễ = 8.000đ (Tình huống 08)
    }

    @Test
    @DisplayName("Thêm bạn đọc với mã đã tồn tại sẽ ném DuplicateReaderException (HTTP 409)")
    void testCreateReader_DuplicateIdThrowsException() {
        StudentReader r1 = new StudentReader("RD999", "Người thứ nhất", "0900000001");
        readerService.create(r1);

        StudentReader r2 = new StudentReader("RD999", "Người thứ hai", "0900000002");
        assertThrows(DuplicateReaderException.class, () -> readerService.create(r2));
    }

    @Test
    @DisplayName("Tìm kiếm bạn đọc theo từ khóa không phân biệt hoa thường (mã, tên, số điện thoại)")
    void testFindAllWithKeyword() {
        readerService.create(new StudentReader("RD001", "Nguyễn Văn An", "0901111111"));
        readerService.create(new PriorityStudentReader("RD002", "Trần Bình Trọng", "0902222222"));
        readerService.create(new LecturerReader("RD003", "Lê Văn An", "0903333333"));

        List<Reader> byName = readerService.findAll("an");
        assertEquals(2, byName.size());

        List<Reader> byId = readerService.findAll("RD002");
        assertEquals(1, byId.size());
        assertEquals("Trần Bình Trọng", byId.get(0).getFullName());

        List<Reader> byPhone = readerService.findAll("333333");
        assertEquals(1, byPhone.size());
        assertEquals("Lê Văn An", byPhone.get(0).getFullName());
    }

    @Test
    @DisplayName("Tìm chi tiết bạn đọc theo mã: thành công và ném ReaderNotFoundException khi không thấy")
    void testFindById() {
        readerService.create(new StudentReader("RD001", "Phạm Văn D", "0904445555"));

        Reader reader = readerService.findById("RD001");
        assertNotNull(reader);
        assertEquals("Phạm Văn D", reader.getFullName());

        assertThrows(ReaderNotFoundException.class, () -> readerService.findById("RD999"));
        assertThrows(IllegalArgumentException.class, () -> readerService.findById("  "));
    }

    @Test
    @DisplayName("Cập nhật thông tin bạn đọc thành công")
    void testUpdateReader() {
        readerService.create(new StudentReader("RD001", "Tên Cũ", "0901234567"));

        StudentReader updateInfo = new StudentReader("RD001", "Tên Mới Đã Sửa", "0987654321");
        Reader updated = readerService.update("RD001", updateInfo);

        assertEquals("Tên Mới Đã Sửa", updated.getFullName());
        assertEquals("0987654321", updated.getPhone());

        // Kiểm tra đọc lại từ repository
        Reader reloaded = readerService.findById("RD001");
        assertEquals("Tên Mới Đã Sửa", reloaded.getFullName());
    }

    @Test
    @DisplayName("Xóa bạn đọc không có phiếu mượn nào -> thành công")
    void testDeleteReader_SuccessWhenNoActiveBorrows() {
        readerService.create(new StudentReader("RD001", "Bạn Đọc Xóa Được", "0901234567"));
        assertTrue(readerService.deleteById("RD001"));
        assertThrows(ReaderNotFoundException.class, () -> readerService.findById("RD001"));
    }

    @Test
    @DisplayName("Chặn xóa bạn đọc đang có sách mượn chưa trả (ném ReaderHasActiveBorrowException - HTTP 409)")
    void testDeleteReader_ThrowsExceptionWhenHavingActiveBorrows() {
        readerService.create(new StudentReader("RD001", "Bạn Đọc Đang Mượn", "0901234567"));

        // Tạo 1 phiếu mượn đang active cho bạn đọc RD001
        BorrowTicketDetail detail = new BorrowTicketDetail("BK001", "Java Core", 2);
        BorrowTicket activeTicket = new BorrowTicket(
                "TK001",
                "RD001",
                List.of(detail),
                LocalDate.now().minusDays(3),
                LocalDate.now().plusDays(11)
        );
        ticketRepository.save(activeTicket);

        // Kiểm tra số sách đang mượn được cập nhật đúng là 2
        Reader reader = readerService.findById("RD001");
        assertEquals(2, reader.getCurrentBorrowCount());
        assertFalse(reader.canBorrow(2)); // SV thường max 3, đã mượn 2, muốn mượn thêm 2 -> false

        // Xóa bạn đọc phải ném ngoại lệ ReaderHasActiveBorrowException
        ReaderHasActiveBorrowException ex = assertThrows(
                ReaderHasActiveBorrowException.class,
                () -> readerService.deleteById("RD001")
        );
        assertTrue(ex.getMessage().contains("đang mượn 2 cuốn sách"));
    }
}

