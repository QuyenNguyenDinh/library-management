package com.nhom04.library;

import com.nhom04.library.main.LibraryApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Kiểm tra ứng dụng Spring khởi động được (mọi bean được tạo thành công).
 */
@SpringBootTest(classes = LibraryApplication.class)
class LibraryApplicationTests {

    @Test
    void contextLoads() {
    }
}
