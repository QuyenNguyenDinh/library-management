package handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import model.Admin;
import service.AuthService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handler tiếp nhận và xử lý API Đăng nhập Thủ thư: POST /api/login.
 */
public class AuthHandler implements HttpHandler {

    private final AuthService authService;

    public AuthHandler(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");

        String method = exchange.getRequestMethod().toUpperCase();
        if ("OPTIONS".equals(method)) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        if ("POST".equals(method)) {
            String body = readRequestBody(exchange);
            String username = extractJsonString(body, "username");
            String password = extractJsonString(body, "password");

            if (authService.authenticate(username, password)) {
                Admin admin = authService.getAdmin(username);
                String resJson = "{\n" +
                        "  \"success\": true,\n" +
                        "  \"message\": \"Đăng nhập thủ thư thành công!\",\n" +
                        "  \"data\": {\n" +
                        "    \"username\": \"" + admin.getUsername() + "\",\n" +
                        "    \"fullName\": \"" + admin.getFullName() + "\"\n" +
                        "  }\n" +
                        "}";
                sendJsonResponse(exchange, 200, resJson);
            } else {
                String errorJson = "{\n" +
                        "  \"success\": false,\n" +
                        "  \"message\": \"Tên đăng nhập hoặc mật khẩu không chính xác!\"\n" +
                        "}";
                sendJsonResponse(exchange, 401, errorJson);
            }
        } else {
            sendJsonResponse(exchange, 405, "{\"success\": false, \"message\": \"Phương thức không hỗ trợ!\"}");
        }
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
}

