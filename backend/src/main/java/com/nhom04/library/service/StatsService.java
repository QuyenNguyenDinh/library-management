package com.nhom04.library.service;

import com.nhom04.library.model.*;
import com.nhom04.library.repository.*;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StatsService {
    private final TicketRepository tickets;
    private final BookRepository books;
    public StatsService(TicketRepository tickets, BookRepository books) { this.tickets = tickets; this.books = books; }

    public Map<String, Object> getStats() {
        List<BorrowTicket> all = tickets.findAll();
        long overdueCount = all.stream().filter(t -> t.isOverdue(LocalDate.now())).count();
        double totalFine = all.stream().mapToDouble(BorrowTicket::getFineAmount).sum();
        Map<String, Integer> counts = new HashMap<>();
        for (BorrowTicket t : all) for (BorrowTicketDetail d : t.getDetails()) counts.merge(d.getBookId(), d.getQuantity(), Integer::sum);
        Map<String, Book> bookMap = books.findAll().stream().collect(Collectors.toMap(Book::getId, b -> b, (a,b) -> a));
        List<Map<String,Object>> topBooks = counts.entrySet().stream().sorted(Map.Entry.<String,Integer>comparingByValue().reversed()).limit(5)
            .map(e -> { Book b=bookMap.get(e.getKey()); Map<String,Object> m=new LinkedHashMap<>(); m.put("bookId",e.getKey()); m.put("title",b==null?e.getKey():b.getTitle()); m.put("borrowCount",e.getValue()); return m; }).toList();
        List<Map<String,Object>> lowStockBooks = books.findAll().stream().filter(b -> b.getAvailableQuantity() <= 2)
            .map(b -> { Map<String,Object> m=new LinkedHashMap<>(); m.put("id",b.getId()); m.put("title",b.getTitle()); m.put("availableQuantity",b.getAvailableQuantity()); return m; }).toList();
        Map<String,Object> result=new LinkedHashMap<>(); result.put("overdueCount",overdueCount); result.put("totalFine",totalFine); result.put("topBooks",topBooks); result.put("lowStockBooks",lowStockBooks); return result;
    }
}
