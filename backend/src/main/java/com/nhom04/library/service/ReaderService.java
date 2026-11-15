package com.nhom04.library.service;

import com.nhom04.library.exception.DuplicateReaderException;
import com.nhom04.library.exception.ReaderHasActiveBorrowException;
import com.nhom04.library.exception.ReaderNotFoundException;
import com.nhom04.library.model.*;
import com.nhom04.library.repository.ReaderRepository;
import com.nhom04.library.repository.TicketRepository;
import com.nhom04.library.utils.IdGenerator;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service xử lý các nghiệp vụ quản lý Bạn đọc:
 * Tìm kiếm, xem chi tiết, thêm mới, sửa đổi thông tin và xóa bạn đọc.
 * Kiểm tra các ràng buộc nghiệp vụ (hạn mức, đang mượn sách).
 */
@Service
public class ReaderService {

    private final ReaderRepository readerRepository;
    private final TicketRepository ticketRepository;

    public ReaderService(ReaderRepository readerRepository, TicketRepository ticketRepository) {
        this.readerRepository = readerRepository;
        this.ticketRepository = ticketRepository;
    }

    /**
     * Lấy danh sách bạn đọc, tùy chọn lọc theo từ khóa (mã, tên hoặc số điện thoại).
     * Bổ sung thông tin số lượng sách đang mượn (currentBorrowCount) cho từng bạn đọc.
     */
    public List<Reader> findAll(String keyword) {
        List<Reader> allReaders = readerRepository.findAll();

        if (keyword != null && !keyword.isBlank()) {
            String lowerKw = keyword.trim().toLowerCase();
            allReaders = allReaders.stream()
                    .filter(r -> (r.getId() != null && r.getId().toLowerCase().contains(lowerKw))
                            || (r.getFullName() != null && r.getFullName().toLowerCase().contains(lowerKw))
                            || (r.getPhone() != null && r.getPhone().contains(lowerKw)))
                    .collect(Collectors.toList());
        }

        for (Reader reader : allReaders) {
            reader.setCurrentBorrowCount(calculateCurrentBorrowCount(reader.getId()));
        }

        return allReaders;
    }

    /**
     * Tìm chi tiết bạn đọc theo mã.
     *
     * @throws ReaderNotFoundException nếu không tìm thấy
     */
    public Reader findById(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Mã bạn đọc không được để trống.");
        }

        Reader reader = readerRepository.findById(id)
                .orElseThrow(() -> new ReaderNotFoundException("Không tìm thấy bạn đọc có mã: " + id));

        reader.setCurrentBorrowCount(calculateCurrentBorrowCount(reader.getId()));
        return reader;
    }

    /**
     * Thêm mới bạn đọc vào hệ thống.
     * Tự động sinh mã RDxxx nếu chưa có mã.
     */
    public Reader create(Reader reader) {
        if (reader == null) {
            throw new IllegalArgumentException("Thông tin bạn đọc không được để trống.");
        }

        // Tự động sinh mã nếu chưa được cấp
        if (reader.getId() == null || reader.getId().isBlank()) {
            Set<String> existingIds = readerRepository.findAll().stream()
                    .map(Reader::getId)
                    .collect(Collectors.toSet());
            reader.setId(IdGenerator.next("RD", existingIds));
        } else {
            // Kiểm tra trùng mã
            if (readerRepository.findById(reader.getId()).isPresent()) {
                throw new DuplicateReaderException("Mã bạn đọc '" + reader.getId() + "' đã tồn tại trong hệ thống.");
            }
        }

        reader.setCurrentBorrowCount(0);
        return readerRepository.save(reader);
    }

    /**
     * Cập nhật thông tin bạn đọc (họ tên, số điện thoại, loại bạn đọc).
     */
    public Reader update(String id, Reader updatedData) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Mã bạn đọc không được để trống.");
        }
        if (updatedData == null) {
            throw new IllegalArgumentException("Dữ liệu cập nhật bạn đọc không được để trống.");
        }

        Reader existing = findById(id);

        Reader target;
        // Nếu thay đổi loại bạn đọc (subtype)
        if (updatedData.getType() != null && updatedData.getType() != existing.getType()) {
            target = createReaderByType(updatedData.getType());
            target.setId(id);
        } else {
            target = existing;
        }

        target.setFullName(updatedData.getFullName());
        target.setPhone(updatedData.getPhone());
        target.setCurrentBorrowCount(calculateCurrentBorrowCount(id));

        return readerRepository.save(target);
    }

    /**
     * Xóa bạn đọc. Chặn xóa nếu bạn đọc đang có sách mượn chưa trả.
     *
     * @throws ReaderNotFoundException        nếu không tìm thấy
     * @throws ReaderHasActiveBorrowException nếu bạn đọc đang có sách chưa trả (HTTP 409)
     */
    public boolean deleteById(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Mã bạn đọc không được để trống.");
        }

        Reader existing = readerRepository.findById(id)
                .orElseThrow(() -> new ReaderNotFoundException("Không tìm thấy bạn đọc có mã: " + id));

        int activeBorrowCount = calculateCurrentBorrowCount(id);
        if (activeBorrowCount > 0) {
            throw new ReaderHasActiveBorrowException(
                    "Bạn đọc '" + id + "' đang mượn " + activeBorrowCount + " cuốn sách, không thể xoá."
            );
        }

        return readerRepository.deleteById(id);
    }

    /**
     * Đếm tổng số lượng sách bạn đọc đang mượn dựa trên các phiếu mượn có trạng thái BORROWING.
     */
    public int calculateCurrentBorrowCount(String readerId) {
        if (ticketRepository == null || readerId == null || readerId.isBlank()) {
            return 0;
        }
        return ticketRepository.findByReaderId(readerId).stream()
                .filter(BorrowTicket::isBorrowing)
                .mapToInt(BorrowTicket::getTotalQuantity)
                .sum();
    }

    private Reader createReaderByType(ReaderType type) {
        return switch (type) {
            case PRIORITY_STUDENT -> new PriorityStudentReader();
            case LECTURER -> new LecturerReader();
            case STUDENT -> new StudentReader();
        };
    }
}
