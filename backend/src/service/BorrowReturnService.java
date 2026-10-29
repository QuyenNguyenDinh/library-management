package service;

import model.*;
import repository.TicketRepository;

import java.util.Map;

/**
 * Lớp BorrowReturnService mở rộng BorrowService để đảm bảo tương thích ngược hoàn hảo.
 */
public class BorrowReturnService extends BorrowService {

    public BorrowReturnService() {
        super();
    }

    public BorrowReturnService(Map<String, Reader> readerRepository,
                               Map<String, Book> bookRepository,
                               Map<String, BorrowTicket> initialTickets) {
        super(readerRepository, bookRepository, new TicketRepository());
        if (initialTickets != null) {
            for (BorrowTicket t : initialTickets.values()) {
                getTicketRepository().save(t);
            }
        }
    }
}
