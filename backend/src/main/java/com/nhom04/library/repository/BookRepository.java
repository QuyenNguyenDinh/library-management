package com.nhom04.library.repository;

import com.google.gson.reflect.TypeToken;
import com.nhom04.library.model.Book;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Repository
public class BookRepository extends JsonFileRepository<Book> {

    public BookRepository(@Value("${library.data-dir}") String dataDir) {
        super(dataDir, "books.json", new TypeToken<List<Book>>() {}.getType());
    }

    @Override
    protected String getId(Book entity) {
        return entity.getId();
    }

    public List<Book> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return findAll();
        }
        String kw = keyword.trim().toLowerCase(Locale.ROOT);
        return findAll().stream()
                .filter(b -> containsIgnoreCase(b.getId(), kw)
                        || containsIgnoreCase(b.getTitle(), kw))
                .collect(Collectors.toList());
    }

    private boolean containsIgnoreCase(String source, String kw) {
        return source != null && source.toLowerCase(Locale.ROOT).contains(kw);
    }
}
