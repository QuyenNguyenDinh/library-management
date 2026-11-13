package com.nhom04.library.service;

import com.google.gson.reflect.TypeToken;
import com.nhom04.library.exception.BookInUseException;
import com.nhom04.library.exception.BookNotFoundException;
import com.nhom04.library.model.Book;
import com.nhom04.library.model.BorrowTicket;
import com.nhom04.library.model.BorrowTicketDetail;
import com.nhom04.library.model.TicketStatus;
import com.nhom04.library.repository.BookRepository;
import com.nhom04.library.utils.GsonFactory;
import com.nhom04.library.utils.IdGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BookService {

    private final BookRepository bookRepository;
    private final String dataDir;

    public BookService(BookRepository bookRepository,
                       @Value("${library.data-dir:data}") String dataDir) {
        this.bookRepository = bookRepository;
        this.dataDir = dataDir;
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
        Path ticketsPath = Paths.get(dataDir, "tickets.json");
        if (!Files.exists(ticketsPath)) {
            return false;
        }
        try (Reader reader = Files.newBufferedReader(ticketsPath, StandardCharsets.UTF_8)) {
            List<BorrowTicket> tickets = GsonFactory.create()
                    .fromJson(reader, new TypeToken<List<BorrowTicket>>() {}.getType());
            if (tickets == null) return false;

            for (BorrowTicket ticket : tickets) {
                if (ticket.getStatus() == TicketStatus.BORROWING && ticket.getDetails() != null) {
                    for (BorrowTicketDetail detail : ticket.getDetails()) {
                        if (bookId.equals(detail.getBookId())) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }
}