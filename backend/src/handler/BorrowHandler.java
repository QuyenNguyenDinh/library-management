package handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import exception.LibraryException;
import model.BorrowTicket;
import model.BorrowTicketDetail;
import model.ReturnRecord;
import service.BorrowService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HTTP Handler tiếp nhận và xử lý các API Mượn/Trả sách:
 * - POST /api/borrow
 * - POST /api/return/{ticketId} (hoặc POST /api/return)
 * Thuộc phạm vi trách nhiệm của Trưởng nhóm.
 */
public class BorrowHandler implements HttpHandler {

    private final BorrowService borrowService;

    public BorrowHandler(BorrowService borrowService) {
        this.borrowService = borrowService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Cấu hình CORS để Frontend gọi API không bị chặn
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");

        String method = exchange.getRequestMethod().toUpperCase();
        if ("OPTIONS".equals(method)) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        String path = exchange.getRequestURI().getPath();
        try {
            if ("POST".equals(method) && path.endsWith("/api/borrow")) {
                handleBorrow(exchange);
            } else if ("POST".equals(method) && (path.startsWith("/api/return") || path.contains("/return/"))) {
                handleReturn(exchange, path);
            } else {
                sendJsonResponse(exchange, 404, "{\"success\": false, \"message\": \"Endpoint không tồn tại!\"}");
            }
        } catch (LibraryException e) {
            sendJsonResponse(exchange, 400, "{\"success\": false, \"message\": \"" + escapeJson(e.getMessage()) + "\"}");
        } catch (Exception e) {
            e.printStackTrace();
            sendJsonResponse(exchange, 500, "{\"success\": false, \"message\": \"Lỗi máy chủ: " + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private void handleBorrow(HttpExchange exchange) throws IOException {
        String body = readRequestBody(exchange);

        String readerId = extractJsonString(body, "readerId");
        String bookId = extractJsonString(body, "bookId");
        int quantity = extractJsonInt(body, "quantity", 1);
        String borrowDateStr = extractJsonString(body, "borrowDate");
        String dueDateStr = extractJsonString(body, "dueDate");

        LocalDate borrowDate = (borrowDateStr != null && !borrowDateStr.isEmpty())
                ? LocalDate.parse(borrowDateStr) : LocalDate.now();
        LocalDate dueDate = (dueDateStr != null && !dueDateStr.isEmpty())
                ? LocalDate.parse(dueDateStr) : borrowDate.plusDays(14);

        List<BorrowTicketDetail> details = new ArrayList<>();
        if (bookId != null && !bookId.isEmpty()) {
            details.add(new BorrowTicketDetail(bookId, quantity));
        }

        BorrowTicket ticket = borrowService.borrowBooks(null, readerId, details, borrowDate, dueDate);

        String responseJson = "{\n" +
                "  \"success\": true,\n" +
                "  \"message\": \"Tạo phiếu mượn sách thành công!\",\n" +
                "  \"data\": {\n" +
                "    \"ticketId\": \"" + ticket.getId() + "\",\n" +
                "    \"readerId\": \"" + ticket.getReaderId() + "\",\n" +
                "    \"bookId\": \"" + ticket.getBookId() + "\",\n" +
                "    \"quantity\": " + ticket.getQuantity() + ",\n" +
                "    \"borrowDate\": \"" + ticket.getBorrowDate() + "\",\n" +
                "    \"dueDate\": \"" + ticket.getDueDate() + "\",\n" +
                "    \"status\": \"" + ticket.getStatus().name() + "\"\n" +
                "  }\n" +
                "}";

        sendJsonResponse(exchange, 201, responseJson);
    }

    private void handleReturn(HttpExchange exchange, String path) throws IOException {
        String body = readRequestBody(exchange);

        // Lấy ticketId từ path /api/return/{ticketId} hoặc từ body
        String ticketId = null;
        if (path.contains("/api/return/")) {
            ticketId = path.substring(path.indexOf("/api/return/") + "/api/return/".length()).trim();
        }
        if (ticketId == null || ticketId.isEmpty()) {
            ticketId = extractJsonString(body, "ticketId");
        }

        String returnDateStr = extractJsonString(body, "returnDate");
        LocalDate returnDate = (returnDateStr != null && !returnDateStr.isEmpty())
                ? LocalDate.parse(returnDateStr) : LocalDate.now();

        ReturnRecord record = borrowService.returnBook(null, ticketId, returnDate);

        String responseJson = "{\n" +
                "  \"success\": true,\n" +
                "  \"message\": \"Trả sách thành công!\",\n" +
                "  \"data\": {\n" +
                "    \"recordId\": \"" + record.getRecordId() + "\",\n" +
                "    \"ticketId\": \"" + record.getTicketId() + "\",\n" +
                "    \"readerId\": \"" + record.getReaderId() + "\",\n" +
                "    \"returnDate\": \"" + record.getReturnDate() + "\",\n" +
                "    \"overdueDays\": " + record.getOverdueDays() + ",\n" +
                "    \"fineAmount\": " + record.getFineAmount() + ",\n" +
                "    \"note\": \"" + escapeJson(record.getNote()) + "\"\n" +
                "  }\n" +
                "}";

        sendJsonResponse(exchange, 200, responseJson);
    }

    private String readRequestBody(HttpExchange exchange) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    private void sendJsonResponse(HttpExchange exchange, int statusCode, String jsonResponse) throws IOException {
        byte[] bytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String extractJsonString(String json, String key) {
        if (json == null) return null;
        String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]*)\"";
        Matcher m = Pattern.compile(pattern).matcher(json);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    private int extractJsonInt(String json, String key, int defaultVal) {
        if (json == null) return defaultVal;
        String pattern = "\"" + key + "\"\\s*:\\s*(\\d+)";
        Matcher m = Pattern.compile(pattern).matcher(json);
        if (m.find()) {
            return Integer.parseInt(m.group(1));
        }
        return defaultVal;
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", "");
    }
}
