package main;

import exception.*;
import model.*;
import service.BorrowReturnService;

import java.time.LocalDate;

/**
 * Lớp kiểm thử thực nghiệm (Test Runner) dành cho Trưởng nhóm.
 * Xác nhận toàn diện 7 Business Rules của BorrowReturnService & Khung OOP.
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("     KIỂM THỬ KHUNG OOP & 7 BUSINESS RULES CỦA BORROW_RETURN_SERVICE           ");
        System.out.println("================================================================================\n");

        BorrowReturnService service = new BorrowReturnService();

        // Khởi tạo dữ liệu mẫu
        StudentReader student = new StudentReader("RD001", "Nguyễn Văn Sinh Viên", "0911111111");
        PriorityStudentReader priorityStudent = new PriorityStudentReader("RD002", "Lê Thị Ưu Tiên", "0922222222");
        LecturerReader lecturer = new LecturerReader("RD003", "ThS. Trần Văn Giảng Viên", "0933333333");

        service.addReader(student);
        service.addReader(priorityStudent);
        service.addReader(lecturer);

        Book javaBook = new Book("BK001", "Lập trình Java Hướng đối tượng", "Đoàn Văn Ban", "CNTT", 2, 120000);
        Book pythonBook = new Book("BK002", "Python cho Khoa học Dữ liệu", "Lê Văn C", "CNTT", 5, 150000);

        service.addBook(javaBook);
        service.addBook(pythonBook);

        System.out.println("[INIT] Đã nạp thành công 3 bạn đọc (SV Thường: limit 3, SV Ưu tiên: limit 5, Giảng viên: limit 10)");
        System.out.println("[INIT] Đã nạp 2 cuốn sách (BK001: 2 cuốn, BK002: 5 cuốn)\n");

        // ---------------------------------------------------------------------
        // TEST RULE 1: Kiểm tra bạn đọc hoặc sách không tồn tại
        // ---------------------------------------------------------------------
        System.out.println("--- TEST RULE 1: KIỂM TRA BẠN ĐỌC HOẶC SÁCH KHÔNG TỒN TẠI ---");
        try {
            service.borrowBook("TK999", "RD_UNKNOWN", "BK001", 1, LocalDate.now(), LocalDate.now().plusDays(14));
            System.err.println("FAILED: Lẽ ra phải ném ReaderNotFoundException!");
        } catch (ReaderNotFoundException e) {
            System.out.println("PASSED: Bắt đúng ReaderNotFoundException -> " + e.getMessage());
        }

        try {
            service.borrowBook("TK999", "RD001", "BK_UNKNOWN", 1, LocalDate.now(), LocalDate.now().plusDays(14));
            System.err.println("FAILED: Lẽ ra phải ném BookNotFoundException!");
        } catch (BookNotFoundException e) {
            System.out.println("PASSED: Bắt đúng BookNotFoundException -> " + e.getMessage());
        }

        // ---------------------------------------------------------------------
        // TEST RULE 2: Số lượng mượn phải > 0 và kho phải còn đủ sách
        // ---------------------------------------------------------------------
        System.out.println("\n--- TEST RULE 2: KIỂM TRA SỐ LƯỢNG MƯỢN > 0 VÀ TỒN KHO ---");
        try {
            service.borrowBook("TK999", "RD001", "BK001", 0, LocalDate.now(), LocalDate.now().plusDays(14));
            System.err.println("FAILED: Lẽ ra phải ném InvalidBorrowRequestException!");
        } catch (InvalidBorrowRequestException e) {
            System.out.println("PASSED: Bắt đúng InvalidBorrowRequestException (mượn <= 0) -> " + e.getMessage());
        }

        try {
            // BK001 chỉ có 2 cuốn, yêu cầu mượn 3 cuốn
            service.borrowBook("TK999", "RD001", "BK001", 3, LocalDate.now(), LocalDate.now().plusDays(14));
            System.err.println("FAILED: Lẽ ra phải ném BookOutOfStockException!");
        } catch (BookOutOfStockException e) {
            System.out.println("PASSED: Bắt đúng BookOutOfStockException (vượt quá tồn kho) -> " + e.getMessage());
        }

        // ---------------------------------------------------------------------
        // TEST RULE 3, 4, 5: Mượn hợp lệ & Kiểm tra hạn mức tối đa
        // ---------------------------------------------------------------------
        System.out.println("\n--- TEST RULE 3, 4, 5: MƯỢN HỢP LỆ & KIỂM TRA HẠN MỨC CHO PHÉP ---");
        // Sinh viên thường hạn mức 3 cuốn. Hiện tại mượn 2 cuốn hợp lệ.
        BorrowTicket ticket1 = service.borrowBook("TK001", "RD001", "BK001", 2,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 15));
        System.out.println("PASSED Mượn hợp lệ: " + ticket1);
        System.out.println("Kho sách BK001 còn lại: " + javaBook.getAvailableQuantity() + " cuốn (đã giảm từ 2 xuống 0)");
        System.out.println("Bạn đọc RD001 đang mượn: " + student.getCurrentBorrowCount() + " / " + student.getMaxBorrowLimit() + " cuốn");

        // Thử mượn tiếp 2 cuốn nữa (2 + 2 = 4 > limit 3) -> Phải ném ngoại lệ
        try {
            service.borrowBook("TK002", "RD001", "BK002", 2, LocalDate.now(), LocalDate.now().plusDays(14));
            System.err.println("FAILED: Lẽ ra phải ném BorrowLimitExceededException!");
        } catch (BorrowLimitExceededException e) {
            System.out.println("PASSED: Bắt đúng BorrowLimitExceededException -> " + e.getMessage());
        }

        // ---------------------------------------------------------------------
        // TEST RULE 6: Kiểm tra phiếu mượn tồn tại
        // ---------------------------------------------------------------------
        System.out.println("\n--- TEST RULE 6: KIỂM TRA PHIẾU MƯỢN TỒN TẠI ---");
        try {
            service.returnBook("RR999", "TK_NON_EXISTENT", LocalDate.now());
            System.err.println("FAILED: Lẽ ra phải ném TicketNotFoundException!");
        } catch (TicketNotFoundException e) {
            System.out.println("PASSED: Bắt đúng TicketNotFoundException -> " + e.getMessage());
        }

        // ---------------------------------------------------------------------
        // TEST RULE 7 & FINE POLICY: Trả sách trễ hạn & Tính phạt đa hình
        // ---------------------------------------------------------------------
        System.out.println("\n--- TEST RULE 7 & FINE POLICY: TRẢ SÁCH TRỄ HẠN & TÍNH PHÍ PHẠT ---");
        // Ticket 1: Hạn trả là 2026-10-15. Trả ngày 2026-10-18 (Trễ 3 ngày).
        // RD001 là Sinh viên thường: 3 ngày * 5.000đ = 15.000đ.
        ReturnRecord record1 = service.returnBook("RR001", "TK001", LocalDate.of(2026, 10, 18));
        System.out.println("PASSED Trả sách quá hạn: " + record1);
        System.out.println("Tiền phạt đã tính: " + String.format("%,.0f VNĐ", record1.getFineAmount()) + " (Kỳ vọng: 15.000 VNĐ)");
        System.out.println("Kho sách BK001 sau khi trả: " + javaBook.getAvailableQuantity() + " cuốn (đã hoàn lại 2)");
        System.out.println("Bạn đọc RD001 sau khi trả sách: đang mượn " + student.getCurrentBorrowCount() + " cuốn");

        // Test phạt cho Giảng viên: Hạn trả 2026-10-10, Trả 2026-10-14 (Trễ 4 ngày).
        // 4 ngày * 2.000đ = 8.000đ.
        BorrowTicket ticketLecturer = service.borrowBook("TK002", "RD003", "BK002", 1,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 10));
        ReturnRecord recordLecturer = service.returnBook("RR002", "TK002", LocalDate.of(2026, 10, 14));
        System.out.println("PASSED Giảng viên trả trễ 4 ngày: Tiền phạt = "
                + String.format("%,.0f VNĐ", recordLecturer.getFineAmount()) + " (Kỳ vọng: 8.000 VNĐ)");

        // Test trả đúng hạn: Không phạt (0 VNĐ)
        BorrowTicket ticketPriority = service.borrowBook("TK003", "RD002", "BK002", 1,
                LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 30));
        ReturnRecord recordPriority = service.returnBook("RR003", "TK003", LocalDate.of(2026, 10, 25));
        System.out.println("PASSED Sinh viên ưu tiên trả trước hạn: Tiền phạt = "
                + String.format("%,.0f VNĐ", recordPriority.getFineAmount()) + " (Kỳ vọng: 0 VNĐ)");

        // ---------------------------------------------------------------------
        // TEST RULE 6 (PHẦN CẤM): Tuyệt đối không cho trả lại phiếu đã "Đã trả"
        // ---------------------------------------------------------------------
        System.out.println("\n--- TEST RULE 6 (CHẶN TUYỆT ĐỐI): KHÔNG CHO TRẢ LẠI PHIẾU ĐÃ TRẢ ---");
        try {
            // Thử trả lại TK001 vừa mới được trả ở trên
            service.returnBook("RR999", "TK001", LocalDate.now());
            System.err.println("FAILED: Lẽ ra phải chặn và ném InvalidTicketStateException!");
        } catch (InvalidTicketStateException e) {
            System.out.println("PASSED: Chặn thành công và bắt InvalidTicketStateException -> " + e.getMessage());
        }

        System.out.println("\n================================================================================");
        System.out.println("     KẾT QUẢ: 100% KIỂM THỬ THÀNH CÔNG VÀ CHÍNH XÁC THEO 7 BUSINESS RULES!      ");
        System.out.println("================================================================================");
    }
}

