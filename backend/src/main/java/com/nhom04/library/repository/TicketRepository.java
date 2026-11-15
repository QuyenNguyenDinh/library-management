package com.nhom04.library.repository;

import com.google.gson.reflect.TypeToken;
import com.nhom04.library.model.BorrowTicket;
import com.nhom04.library.model.TicketStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Chỉ đọc/ghi tickets.json, không có business rule. */
@Repository
public class TicketRepository extends JsonFileRepository<BorrowTicket> {

    public TicketRepository(@Value("${library.data-dir}") String dataDir) {
        super(dataDir, "tickets.json", new TypeToken<List<BorrowTicket>>() { }.getType());
    }

    @Override
    protected String getId(BorrowTicket entity) {
        return entity.getId();
    }

    public List<BorrowTicket> findByStatus(TicketStatus status) {
        return findAll().stream()
                .filter(t -> t.getStatus() == status)
                .toList();
    }

    public List<BorrowTicket> findByReaderId(String readerId) {
        return findAll().stream()
                .filter(t -> readerId != null && readerId.equals(t.getReaderId()))
                .toList();
    }
}
