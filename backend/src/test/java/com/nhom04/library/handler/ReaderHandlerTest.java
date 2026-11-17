package com.nhom04.library.handler;

import com.nhom04.library.model.*;
import com.nhom04.library.repository.AdminRepository;
import com.nhom04.library.repository.ReaderRepository;
import com.nhom04.library.repository.TicketRepository;
import com.nhom04.library.service.AuthService;
import com.nhom04.library.service.ReaderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Kiểm thử API Bạn đọc và Xác thực qua MockMvc:
 * Đúng mã HTTP, format ApiResponse theo contract và tình huống test 11.
 */
class ReaderHandlerTest {

    @TempDir
    Path dataDir;

    private MockMvc mockMvc;
    private ReaderRepository readerRepository;
    private TicketRepository ticketRepository;
    private AdminRepository adminRepository;

    @BeforeEach
    void setUp() {
        String dir = dataDir.toString();
        readerRepository = new ReaderRepository(dir);
        ticketRepository = new TicketRepository(dir);
        adminRepository = new AdminRepository(dir);

        ReaderService readerService = new ReaderService(readerRepository, ticketRepository);
        AuthService authService = new AuthService(adminRepository);

        // Khởi tạo tài khoản mẫu
        readerRepository.save(new StudentReader("RD001", "Nguyễn Văn A", "0901234567"));
        String hashedPass = authService.hashPassword("admin123");
        adminRepository.save(new Admin("AD001", "Thủ thư", "0900000000", "admin", hashedPass));

        mockMvc = MockMvcBuilders.standaloneSetup(new ReaderHandler(readerService), new AuthHandler(authService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/readers với type LECTURER trả về 201, data.type là LECTURER, maxBorrowLimit là 10")
    void createReader_Lecturer_returns201() throws Exception {
        mockMvc.perform(post("/api/readers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Thầy Hoàng",
                                  "phone": "0983334455",
                                  "type": "LECTURER"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.type").value("LECTURER"))
                .andExpect(jsonPath("$.data.maxBorrowLimit").value(10))
                .andExpect(jsonPath("$.data.fullName").value("Thầy Hoàng"));
    }

    @Test
    @DisplayName("POST /api/readers với fullName rỗng trả về 400")
    void createReader_emptyFullName_returns400() throws Exception {
        mockMvc.perform(post("/api/readers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "",
                                  "phone": "0901234567",
                                  "type": "STUDENT"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Họ và tên không được để trống."));
    }

    @Test
    @DisplayName("PUT /api/readers/{id} đổi type sang PRIORITY_STUDENT trả về 200, maxBorrowLimit là 5")
    void updateReader_changeTypeToPriorityStudent_returns200() throws Exception {
        mockMvc.perform(put("/api/readers/RD001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Nguyễn Văn A",
                                  "phone": "0901234567",
                                  "type": "PRIORITY_STUDENT"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.type").value("PRIORITY_STUDENT"))
                .andExpect(jsonPath("$.data.maxBorrowLimit").value(5));
    }

    @Test
    @DisplayName("DELETE bạn đọc đang mượn sách trả về 409 Conflict")
    void deleteReader_withActiveBorrows_returns409() throws Exception {
        // Tạo phiếu mượn đang active cho RD001
        BorrowTicketDetail detail = new BorrowTicketDetail("BK001", "Java Core", 1);
        BorrowTicket activeTicket = new BorrowTicket(
                "TK001",
                "RD001",
                List.of(detail),
                LocalDate.now().minusDays(2),
                LocalDate.now().plusDays(12)
        );
        ticketRepository.save(activeTicket);

        mockMvc.perform(delete("/api/readers/RD001"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/auth/login đúng mật khẩu trả về 200, sai mật khẩu trả về 401 (Tình huống test 11)")
    void authLogin_successAndFailure() throws Exception {
        // Đăng nhập đúng mật khẩu -> 200
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "admin",
                                  "password": "admin123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("admin"));

        // Đăng nhập sai mật khẩu -> 401
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "admin",
                                  "password": "wrongpassword"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Tên đăng nhập hoặc mật khẩu không chính xác."));
    }
}
