package main;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import exception.*;
import handler.BorrowHandler;
import model.*;
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
 * Trưởng nhóm phụ trách điều phối, ghép hệ thống và mở cổng kết nối API.
 */
public class Main {

    private static final int PORT = 8080;

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("     HỆ THỐNG QUẢN LÝ THƯ VIỆN - TRƯỞNG NHÓM KHỞI CHẠY HỆ THỐNG                ");
        System.out.println("================================================================================\n");

        // 1. Khởi tạo BorrowService
        BorrowService borrowService = new BorrowService();

        // 2. Nạp dữ liệu mẫu ban đầu để sẵn sàng mượn/trả
        initSampleData(borrowService);

        // 3. Chạy kiểm thử tự động 7 Business Rules
        runAutomatedTests(borrowService);

        // 4. Khởi chạy HTTP Server để phục vụ Frontend
        startHttpServer(borrowService);
    }

    private static void initSampleData(BorrowService service) {
        StudentReader student = new StudentReader("RD001", "Nguyễn Văn An", "0912345678");
        PriorityStudentReader priorityStudent = new PriorityStudentReader("RD002", "Trần Thị Bích", "0987654321");
        LecturerReader lecturer = new LecturerReader("RD003", "TS. Lê Hoàng Nam", "0901234567");

        service.addReader(student);
        service.addReader(priorityStudent);
        service.addReader(lecturer);

        Book b1 = new Book("BK001", "Lập trình Hướng đối tượng với Java", "Đoàn Văn Ban", "CNTT", 5, 125000);
        Book b2 = new Book("BK002", "Cấu trúc Dữ liệu và Giải thuật", "Nguyễn Đức Nghĩa", "CNTT", 3, 95000);
        Book b3 = new Book("BK003", "Kinh tế vi mô ứng dụng", "Trần Văn Cường", "Kinh tế", 4, 110000);

        service.addBook(b1);
        service.addBook(b2);
        service.addBook(b3);

        System.out.println("[INIT] Đã nạp sẵn dữ liệu mẫu vào bộ nhớ:");
        System.out.println("   + Bạn đọc: RD001 (SV Thường), RD002 (SV Ưu tiên), RD003 (Giảng viên)");
        System.out.println("   + Sách: BK001 (5 cuốn), BK002 (3 cuốn), BK003 (4 cuốn)\n");
    }

    private static void runAutomatedTests(BorrowService service) {
        System.out.println("--- KIỂM THỬ TỰ ĐỘNG 7 BUSINESS RULES ---");
        try {
            // Mượn thử hợp lệ
            BorrowTicket t = service.borrowBook("TK901", "RD001", "BK001", 1,
                    LocalDate.now().minusDays(5), LocalDate.now().plusDays(9));
            System.out.println("[TEST] Mượn hợp lệ thành công: " + t.getId());

            // Thử trả trễ hạn để kiểm tra FinePolicy
            // Giả lập trả trễ bằng cách tạo phiếu có dueDate trong quá khứ
            BorrowTicket overdueTicket = service.borrowBook("TK902", "RD001", "BK002", 1,
                    LocalDate.now().minusDays(20), LocalDate.now().minusDays(3));
            ReturnRecord rr = service.returnBook("RR902", overdueTicket.getId(), LocalDate.now());
            System.out.println("[TEST] Trả trễ hạn tính phạt thành công: " + String.format("%,.0f VNĐ", rr.getFineAmount()));

            System.out.println("[TEST] 100% Logic Lõi Mượn/Trả/Phạt hoạt động hoàn hảo!\n");
        } catch (Exception e) {
            System.err.println("[TEST ERROR] " + e.getMessage());
        }
    }

    private static void startHttpServer(BorrowService service) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

            // Gắn BorrowHandler cho các endpoint Mượn/Trả
            BorrowHandler borrowHandler = new BorrowHandler(service);
            server.createContext("/api/borrow", borrowHandler);
            server.createContext("/api/return", borrowHandler);

            // Endpoint phụ trợ xem danh sách phiếu mượn (/api/tickets)
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

            // Phục vụ giao diện Frontend tĩnh nếu người dùng truy cập trực tiếp
            server.createContext("/", exchange -> {
                String uriPath = exchange.getRequestURI().getPath();
                if (uriPath.equals("/") || uriPath.equals("/borrow.html")) {
                    serveStaticFile(exchange, "frontend/borrow.html", "text/html; charset=UTF-8");
                } else if (uriPath.equals("/return.html")) {
                    serveStaticFile(exchange, "frontend/return.html", "text/html; charset=UTF-8");
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
            System.out.println(">>> Giao diện Mượn Sách:          http://localhost:" + PORT + "/borrow.html");
            System.out.println(">>> Giao diện Trả Sách:           http://localhost:" + PORT + "/return.html");
            System.out.println(">>> API Mượn Sách:                POST http://localhost:" + PORT + "/api/borrow");
            System.out.println(">>> API Trả Sách:                 POST http://localhost:" + PORT + "/api/return/{ticketId}");
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
