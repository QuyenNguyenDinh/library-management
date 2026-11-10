package com.nhom04.library.main;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Điểm khởi chạy hệ thống quản lý mượn sách thư viện.
 * <p>
 * Lớp này nằm trong package {@code main} theo yêu cầu tổ chức project, nên phải khai báo
 * {@code scanBasePackages} để Spring quét được các package anh em
 * (handler, service, repository, ...).
 */
@SpringBootApplication(scanBasePackages = "com.nhom04.library")
public class LibraryApplication {

    public static void main(String[] args) {
        SpringApplication.run(LibraryApplication.class, args);
    }
}
