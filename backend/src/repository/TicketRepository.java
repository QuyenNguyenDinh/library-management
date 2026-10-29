package repository;

import model.BorrowTicket;
import model.BorrowTicketDetail;
import model.ReturnRecord;
import model.TicketStatus;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;

/**
 * Repository phụ trách lưu trữ và truy xuất Phiếu mượn (tickets.json) và Lịch sử trả sách.
 * Thuộc phạm vi phụ trách của Trưởng nhóm theo phân công kiến trúc.
 */
public class TicketRepository {

    private final String filePath;
    private final Map<String, BorrowTicket> ticketMap = new LinkedHashMap<>();
    private final List<ReturnRecord> returnRecords = new ArrayList<>();

    public TicketRepository() {
        this("backend/data/tickets.json");
    }

    public TicketRepository(String filePath) {
        this.filePath = filePath;
        loadFromFile();
    }

    public List<BorrowTicket> findAll() {
        return new ArrayList<>(ticketMap.values());
    }

    public Optional<BorrowTicket> findById(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(ticketMap.get(id));
    }

    public BorrowTicket save(BorrowTicket ticket) {
        if (ticket == null || ticket.getId() == null) {
            throw new IllegalArgumentException("Phiếu mượn không thể rỗng hoặc thiếu ID!");
        }
        ticketMap.put(ticket.getId(), ticket);
        saveToFile();
        return ticket;
    }

    public List<BorrowTicket> findByStatus(TicketStatus status) {
        List<BorrowTicket> result = new ArrayList<>();
        for (BorrowTicket t : ticketMap.values()) {
            if (t.getStatus() == status) {
                result.add(t);
            }
        }
        return result;
    }

    public List<BorrowTicket> findByReaderId(String readerId) {
        List<BorrowTicket> result = new ArrayList<>();
        for (BorrowTicket t : ticketMap.values()) {
            if (t.getReaderId().equalsIgnoreCase(readerId)) {
                result.add(t);
            }
        }
        return result;
    }

    public void saveReturnRecord(ReturnRecord record) {
        if (record != null) {
            returnRecords.add(record);
        }
    }

    public List<ReturnRecord> findAllReturnRecords() {
        return Collections.unmodifiableList(returnRecords);
    }

    // =========================================================================
    // ĐỌC / GHI FILE JSON (TỰ CHUẨN HÓA BẰNG THƯ VIỆN THUẦN JAVA)
    // =========================================================================

    public synchronized void loadFromFile() {
        File file = new File(filePath);
        if (!file.exists() || file.length() == 0) {
            return;
        }

        try {
            String content = new String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8).trim();
            if (content.isEmpty() || content.equals("[]")) {
                return;
            }

            // Phân tích cú pháp các object JSON trong mảng [...]
            int startIndex = content.indexOf('{');
            while (startIndex != -1) {
                int braceCount = 0;
                int endIndex = -1;
                for (int i = startIndex; i < content.length(); i++) {
                    char c = content.charAt(i);
                    if (c == '{') braceCount++;
                    else if (c == '}') {
                        braceCount--;
                        if (braceCount == 0) {
                            endIndex = i;
                            break;
                        }
                    }
                }

                if (endIndex != -1) {
                    String objJson = content.substring(startIndex, endIndex + 1);
                    BorrowTicket ticket = parseTicketJson(objJson);
                    if (ticket != null && ticket.getId() != null) {
                        ticketMap.put(ticket.getId(), ticket);
                    }
                    startIndex = content.indexOf('{', endIndex + 1);
                } else {
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("[TicketRepository] Cảnh báo khi đọc file tickets.json: " + e.getMessage());
        }
    }

    public synchronized void saveToFile() {
        try {
            File file = new File(filePath);
            if (file.getParentFile() != null && !file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }

            StringBuilder sb = new StringBuilder();
            sb.append("[\n");
            int count = 0;
            int total = ticketMap.size();
            for (BorrowTicket t : ticketMap.values()) {
                count++;
                sb.append("  {\n");
                sb.append("    \"id\": \"").append(escapeJson(t.getId())).append("\",\n");
                sb.append("    \"readerId\": \"").append(escapeJson(t.getReaderId())).append("\",\n");
                sb.append("    \"bookId\": \"").append(escapeJson(t.getBookId() != null ? t.getBookId() : "")).append("\",\n");
                sb.append("    \"quantity\": ").append(t.getQuantity()).append(",\n");
                sb.append("    \"borrowDate\": \"").append(t.getBorrowDate() != null ? t.getBorrowDate().toString() : "").append("\",\n");
                sb.append("    \"dueDate\": \"").append(t.getDueDate() != null ? t.getDueDate().toString() : "").append("\",\n");
                if (t.getReturnDate() != null) {
                    sb.append("    \"returnDate\": \"").append(t.getReturnDate().toString()).append("\",\n");
                } else {
                    sb.append("    \"returnDate\": null,\n");
                }
                sb.append("    \"status\": \"").append(t.getStatus() != null ? t.getStatus().name() : "BORROWING").append("\",\n");
                sb.append("    \"fineAmount\": ").append(t.getFineAmount()).append("\n");
                sb.append("  }").append(count < total ? ",\n" : "\n");
            }
            sb.append("]\n");

            Files.write(Paths.get(filePath), sb.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            System.err.println("[TicketRepository] Lỗi khi ghi file tickets.json: " + e.getMessage());
        }
    }

    private BorrowTicket parseTicketJson(String json) {
        BorrowTicket t = new BorrowTicket();
        t.setId(extractJsonString(json, "id"));
        t.setReaderId(extractJsonString(json, "readerId"));
        t.setBookId(extractJsonString(json, "bookId"));
        t.setQuantity(extractJsonInt(json, "quantity", 1));

        String bDate = extractJsonString(json, "borrowDate");
        if (bDate != null && !bDate.isEmpty()) {
            t.setBorrowDate(LocalDate.parse(bDate));
        }

        String dDate = extractJsonString(json, "dueDate");
        if (dDate != null && !dDate.isEmpty()) {
            t.setDueDate(LocalDate.parse(dDate));
        }

        String rDate = extractJsonString(json, "returnDate");
        if (rDate != null && !rDate.isEmpty() && !rDate.equals("null")) {
            t.setReturnDate(LocalDate.parse(rDate));
        }

        String statusStr = extractJsonString(json, "status");
        if (statusStr != null) {
            if (statusStr.equalsIgnoreCase("RETURNED") || statusStr.equalsIgnoreCase("Đã trả")) {
                t.setStatus(TicketStatus.RETURNED);
            } else {
                t.setStatus(TicketStatus.BORROWING);
            }
        }

        t.setFineAmount(extractJsonDouble(json, "fineAmount", 0.0));
        return t;
    }

    private String extractJsonString(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]*)\"";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(pattern).matcher(json);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    private int extractJsonInt(String json, String key, int defaultVal) {
        String pattern = "\"" + key + "\"\\s*:\\s*(\\d+)";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(pattern).matcher(json);
        if (m.find()) {
            return Integer.parseInt(m.group(1));
        }
        return defaultVal;
    }

    private double extractJsonDouble(String json, String key, double defaultVal) {
        String pattern = "\"" + key + "\"\\s*:\\s*([0-9.]+)";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(pattern).matcher(json);
        if (m.find()) {
            return Double.parseDouble(m.group(1));
        }
        return defaultVal;
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

