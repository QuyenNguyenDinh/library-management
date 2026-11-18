package com.nhom04.library.service;

import com.nhom04.library.exception.BookInUseException;
import com.nhom04.library.exception.BookNotFoundException;
import com.nhom04.library.model.Book;
import com.nhom04.library.model.TicketStatus;
import com.nhom04.library.repository.BookRepository;
import com.nhom04.library.repository.TicketRepository;
import com.nhom04.library.utils.IdGenerator;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BookService {

    private final BookRepository bookRepository;
    private final TicketRepository ticketRepository;

    public BookService(BookRepository bookRepository, TicketRepository ticketRepository) {
        this.bookRepository = bookRepository;
        this.ticketRepository = ticketRepository;
    }

    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    public Book getBookById(String id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new BookNotFoundException("Không tìm thấy sách có mã: " + id));
    }

    public List<Book> searchBooks(String keyword) {
        return bookRepository.search(keyword);
    }

    public Map<String, Object> checkAvailability(String id, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Số lượng kiểm tra phải lớn hơn 0.");
        }
        Book book = getBookById(id);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("available", book.hasEnough(quantity));
        result.put("availableQuantity", book.getAvailableQuantity());
        return result;
    }

    public Book createBook(Book request) {
        if (request == null) {
            throw new IllegalArgumentException("Dữ liệu sách không được để trống.");
        }
        List<String> existingIds = bookRepository.findAll().stream()
                .map(Book::getId)
                .collect(Collectors.toList());

        String newId = IdGenerator.next("BK", existingIds);
        Book book = new Book(
                newId,
                request.getTitle(),
                request.getAuthor(),
                request.getCategory(),
                request.getAvailableQuantity(),
                request.getPrice()
        );
        return bookRepository.save(book);
    }

    public Book updateBook(String id, Book request) {
        Book existing = getBookById(id);
        if (request == null) {
            throw new IllegalArgumentException("Dữ liệu cập nhật không được để trống.");
        }
        existing.setTitle(request.getTitle());
        existing.setAuthor(request.getAuthor());
        existing.setCategory(request.getCategory());
        existing.setAvailableQuantity(request.getAvailableQuantity());
        existing.setPrice(request.getPrice());
        return bookRepository.save(existing);
    }

    public void deleteBook(String id) {
        Book existing = getBookById(id);
        if (isBookCurrentlyBorrowed(id)) {
            throw new BookInUseException(
                    "Không thể xóa sách '" + existing.getTitle() + "' (" + id + ") vì đang có phiếu mượn chưa trả.");
        }
        bookRepository.deleteById(id);
    }

    private boolean isBookCurrentlyBorrowed(String bookId) {
        return ticketRepository.findByStatus(TicketStatus.BORROWING).stream()
                .filter(ticket -> ticket.getDetails() != null)
                .flatMap(ticket -> ticket.getDetails().stream())
                .anyMatch(detail -> bookId.equals(detail.getBookId()));
    }
}