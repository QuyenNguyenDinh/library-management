package com.nhom04.library.handler;

import com.nhom04.library.model.Book;
import com.nhom04.library.service.BookService;
import com.nhom04.library.utils.ApiResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/books")
public class BookHandler {

    private final BookService bookService;

    public BookHandler(BookService bookService) {
        this.bookService = bookService;
    }

    // Lấy danh sách sách hoặc tìm kiếm sách
    @GetMapping
    public ResponseEntity<ApiResponse<List<Book>>> getBooks(
            @RequestParam(required = false) String keyword) {

        List<Book> books = (keyword != null && !keyword.isBlank())
                ? bookService.searchBooks(keyword)
                : bookService.getAllBooks();

        return ResponseEntity.ok(
                ApiResponse.ok("Lấy danh sách sách thành công", books)
        );
    }

    // Lấy thông tin sách theo ID
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Book>> getBookById(
            @PathVariable String id) {

        return ResponseEntity.ok(
                ApiResponse.ok(bookService.getBookById(id))
        );
    }

    // Kiểm tra số lượng sách còn lại
    @GetMapping("/{id}/availability")
    public ResponseEntity<ApiResponse<?>> checkAvailability(
            @PathVariable String id,
            @RequestParam(defaultValue = "1") int quantity) {

        return ResponseEntity.ok(
                ApiResponse.ok(bookService.checkAvailability(id, quantity))
        );
    }

    // Thêm sách mới
    @PostMapping
    public ResponseEntity<ApiResponse<Book>> createBook(
            @RequestBody BookRequest book) {

        Book created = bookService.createBook(book.toBook());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Thêm sách mới thành công", created));
    }

    // Cập nhật sách
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Book>> updateBook(
            @PathVariable String id,
            @RequestBody BookRequest book) {

        Book updated = bookService.updateBook(id, book.toBook());

        return ResponseEntity.ok(
                ApiResponse.ok("Cập nhật sách thành công", updated)
        );
    }

    // Xóa sách
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBook(
            @PathVariable String id) {

        bookService.deleteBook(id);

        return ResponseEntity.ok(
                ApiResponse.ok("Xóa sách thành công", null)
        );
    }

    public record BookRequest(String title, String author, String category, int availableQuantity, double price) {
        Book toBook() {
            Book b = new Book();
            b.setTitle(title);
            b.setAuthor(author);
            b.setCategory(category);
            b.setAvailableQuantity(availableQuantity);
            b.setPrice(price);
            return b;
        }
    }
}