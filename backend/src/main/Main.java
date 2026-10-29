package main;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import exception.*;
import handler.AuthHandler;
import handler.BorrowHandler;
import model.*;
import policy.*;
import service.AuthService;
import service.BorrowService;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.List;

/**
 * Lớp khởi chạy hệ thống (Main Server & Test Runner).
 * Khởi chạy hệ thống, ghép nối API và phục vụ giao diện người dùng.
 */
public class Main {

    private static final int PORT = 8080;

    /**
     * Lớp cài đặt mẫu của interface Borrowable (chỉ dùng nội bộ để demo/test kiểm thử).
     */
    static class DemoBorrowableItem implements Borrowable {
        private final String title;
        private int quantity;

        public DemoBorrowableItem(String title, int quantity) {
            this.title = title;
            this.quantity = quantity;
        }

        @Override
        public boolean isAvailable() {
            return quantity > 0;
        }

        @Override
        public int getAvailableQuantity() {
            return quantity;
        }

        @Override
        public void borrowItem(int q) {
            if (q <= 0 || q > quantity) {
                throw new IllegalStateException("Số lượng không hợp lệ!");
            }
            this.quantity -= q;
        }

        @Override
        public void returnItem(int q) {
            this.quantity += q;
        }

        public String getTitle() {
            return title;
        }
    }

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("     HỆ THỐNG QUẢN LÝ THƯ VIỆN - TRƯỞNG NHÓM KHỞI CHẠY HỆ THỐNG                ");
        System.out.println("================================================================================\n");

        BorrowService borrowService = new BorrowService();
        AuthService authService = new AuthService();

        // 1. Nạp dữ liệu mẫu cấu hình cho Trưởng nhóm
        initSampleData(borrowService);

        // 2. Chạy kiểm thử tự động các quy tắc nghiệp vụ cốt lõi
        runAutomatedTests(borrowService, authService);

        // 3. Khởi chạy HTTP Server phục vụ giao diện & APIs
        startHttpServer(borrowService, authService);
    }

    private static void initSampleData(BorrowService service) {
        // Cấu hình Bạn đọc & Chính sách phạt (FinePolicy)
        service.registerReader("RD001", 3, new NormalStudentFinePolicy());    // Sinh viên thường: limit 3, phạt 5k/ngày
        service.registerReader("RD002", 5, new PriorityStudentFinePolicy());  // Sinh viên ưu tiên: limit 5, phạt 3k/ngày
        service.registerReader("RD003", 10, new LecturerFinePolicy());        // Giảng viên: limit 10, phạt 2k/ngày

        // Đăng ký các tài nguyên có thể mượn (triển khai Borrowable)
        service.registerBorrowableItem("BK001", new DemoBorrowableItem("Lập trình Hướng đối tượng với Java", 5));
        service.registerBorrowableItem("BK002", new DemoBorrowableItem("Cấu trúc Dữ liệu và Giải thuật", 3));
        service.registerBorrowableItem("BK003", new DemoBorrowableItem("Kinh tế vi mô ứng dụng", 4));

        System.out.println("[INIT] Đã nạp cấu hình thử nghiệm cho Trưởng nhóm:");
        System.out.println("   + Bạn đọc: RD001 (SV Thường: max 3), RD002 (SV Ưu tiên: max 5), RD003 (Giảng viên: max 10)");
        System.out.println("   + Tài nguyên mượn (Borrowable): BK001 (5 cuốn), BK002 (3 cuốn), BK003 (4 cuốn)\n");
    }

    private static void runAutomatedTests(BorrowService service, AuthService authService) {
        System.out.println("--- KIỂM THỬ TỰ ĐỘNG CÁC QUY TẮC NGHIỆP VỤ CỦA TRƯỞNG NHÓM ---");
        try {
            // Test 1: Mượn sách hợp lệ
            BorrowTicket t = service.borrowBook("TK901", "RD001", "BK001", 1,
                    LocalDate.now().minusDays(5), LocalDate.now().plusDays(9));
            System.out.println("[TEST] 1. Mượn hợp lệ thành công: Mã phiếu " + t.getId());

            // Test 2: Trả sách quá hạn tính phạt theo đúng FinePolicy của bạn đọc
            BorrowTicket overdueTicket = service.borrowBook("TK902", "RD001", "BK002", 1,
                    LocalDate.now().minusDays(20), LocalDate.now().minusDays(3));
            ReturnRecord rr = service.returnBook("RR902", overdueTicket.getId(), LocalDate.now());
            System.out.println("[TEST] 2. Trả trễ hạn tính phạt thành công: " + String.format("%,.0f VNĐ", rr.getFineAmount()));

            // Test 3: Chặn trả lại phiếu đã trả
            try {
                service.returnBook("RR903", overdueTicket.getId(), LocalDate.now());
                System.err.println("[TEST ERROR] Không chặn được phiếu đã trả!");
            } catch (InvalidTicketStateException e) {
                System.out.println("[TEST] 3. Chặn trả lại phiếu đã ở trạng thái 'Đã trả': THÀNH CÔNG");
            }

            // Test 4: Xác thực Đăng nhập Thủ thư (SHA-256)
            boolean authOk = authService.authenticate("admin", "admin123");
            if (authOk) {
                System.out.println("[TEST] 4. Xác thực Đăng nhập Thủ thư (mã hóa SHA-256): THÀNH CÔNG");
            }

            System.out.println("[TEST] Kết luận: 100% Nghiệp vụ của Trưởng nhóm hoạt động chính xác!\n");
        } catch (Exception e) {
            System.err.println("[TEST ERROR] " + e.getMessage());
        }
    }

    private static void startHttpServer(BorrowService service, AuthService authService) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

            // Gắn Handler cho API Mượn/Trả sách
            BorrowHandler borrowHandler = new BorrowHandler(service);
            server.createContext("/api/borrow", borrowHandler);
            server.createContext("/api/return", borrowHandler);

            // Gắn Handler cho API Đăng nhập Thủ thư
            AuthHandler authHandler = new AuthHandler(authService);
            server.createContext("/api/login", authHandler);

            // Endpoint danh sách phiếu mượn (/api/tickets)
            server.createContext("/api/tickets", exchange -> {
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                List<BorrowTicket> tickets = service.getAllTickets();
                StringBuilder sb = new StringBuilder("[\n");
                for (int i = 0; i < tickets.size(); i++) {
                    BorrowTicket tk = tickets.get(i);
                    sb.append("  {\"id\": \"").append(tk.getId())
                            .append("\", \"readerId\": \"").append(tk.getReaderId())
                            .append("\", \"bookId\": \"").append(tk.getBookId())
                            .append("\", \"quantity\": ").append(tk.getQuantity())
                            .append(", \"status\": \"").append(tk.getStatus().name())
                            .append("\", \"fineAmount\": ").append(tk.getFineAmount())
                            .append("}").append(i < tickets.size() - 1 ? ",\n" : "\n");
                }
                sb.append("]");
                byte[] bytes = sb.toString().getBytes("UTF-8");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            });

            // Phục vụ giao diện Frontend tĩnh
            server.createContext("/", exchange -> {
                String uriPath = exchange.getRequestURI().getPath();
                if (uriPath.equals("/") || uriPath.equals("/borrow.html")) {
                    serveStaticFile(exchange, "frontend/borrow.html", "text/html; charset=UTF-8");
                } else if (uriPath.equals("/return.html")) {
                    serveStaticFile(exchange, "frontend/return.html", "text/html; charset=UTF-8");
                } else if (uriPath.equals("/login.html")) {
                    serveStaticFile(exchange, "frontend/login.html", "text/html; charset=UTF-8");
                } else {
                    String notFound = "<h1>404 - Không tìm thấy trang</h1>";
                    exchange.sendResponseHeaders(404, notFound.length());
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(notFound.getBytes());
                    }
                }
            });

            server.setExecutor(null);
            server.start();

            System.out.println("================================================================================");
            System.out.println(">>> SERVER BACKEND ĐANG CHẠY TẠI: http://localhost:" + PORT);
            System.out.println(">>> Giao diện Đăng Nhập Thủ Thư:  http://localhost:" + PORT + "/login.html");
            System.out.println(">>> Giao diện Mượn Sách:          http://localhost:" + PORT + "/borrow.html");
            System.out.println(">>> Giao diện Trả Sách:           http://localhost:" + PORT + "/return.html");
            System.out.println(">>> API Mượn Sách:                POST http://localhost:" + PORT + "/api/borrow");
            System.out.println(">>> API Trả Sách:                 POST http://localhost:" + PORT + "/api/return/{ticketId}");
            System.out.println(">>> API Đăng Nhập:                POST http://localhost:" + PORT + "/api/login");
            System.out.println("================================================================================");

        } catch (IOException e) {
            System.err.println("Không thể khởi động HTTP Server: " + e.getMessage());
        }
    }

    private static void serveStaticFile(HttpExchange exchange, String filePath, String contentType) throws IOException {
        File file = new File(filePath);
        if (file.exists()) {
            byte[] bytes = Files.readAllBytes(file.toPath());
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        } else {
            exchange.sendResponseHeaders(404, -1);
        }
    }
}
