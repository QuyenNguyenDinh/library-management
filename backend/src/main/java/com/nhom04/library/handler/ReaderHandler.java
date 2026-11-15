package com.nhom04.library.handler;

import com.nhom04.library.model.*;
import com.nhom04.library.service.ReaderService;
import com.nhom04.library.utils.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller tiếp nhận các yêu cầu quản lý Bạn đọc:
 * GET /api/readers?keyword=
 * GET /api/readers/{id}
 * POST /api/readers
 * PUT /api/readers/{id}
 * DELETE /api/readers/{id}
 */
@RestController
@RequestMapping("/api/readers")
public class ReaderHandler {

    private final ReaderService readerService;

    public ReaderHandler(ReaderService readerService) {
        this.readerService = readerService;
    }

    @GetMapping
    public ApiResponse<List<Reader>> getAll(@RequestParam(required = false) String keyword) {
        List<Reader> readers = readerService.findAll(keyword);
        return ApiResponse.ok(readers);
    }

    @GetMapping("/{id}")
    public ApiResponse<Reader> getById(@PathVariable String id) {
        Reader reader = readerService.findById(id);
        return ApiResponse.ok(reader);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Reader> create(@RequestBody ReaderRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Dữ liệu gửi lên không được để trống.");
        }

        ReaderType type = request.getType() != null ? request.getType() : ReaderType.STUDENT;
        Reader reader = createReaderInstance(request.getId(), request.getFullName(), request.getPhone(), type);

        Reader created = readerService.create(reader);
        return ApiResponse.ok("Thêm bạn đọc thành công.", created);
    }

    @PutMapping("/{id}")
    public ApiResponse<Reader> update(@PathVariable String id, @RequestBody ReaderRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Dữ liệu cập nhật không được để trống.");
        }

        ReaderType type = request.getType() != null ? request.getType() : ReaderType.STUDENT;
        Reader reader = createReaderInstance(id, request.getFullName(), request.getPhone(), type);

        Reader updated = readerService.update(id, reader);
        return ApiResponse.ok("Cập nhật thông tin bạn đọc thành công.", updated);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        readerService.deleteById(id);
        return ApiResponse.ok("Xoá bạn đọc thành công.", null);
    }

    private Reader createReaderInstance(String id, String fullName, String phone, ReaderType type) {
        Reader reader = switch (type) {
            case PRIORITY_STUDENT -> new PriorityStudentReader();
            case LECTURER -> new LecturerReader();
            case STUDENT -> new StudentReader();
        };

        if (id != null && !id.isBlank()) {
            reader.setId(id);
        }
        if (fullName != null && !fullName.isBlank()) {
            reader.setFullName(fullName);
        }
        if (phone != null && !phone.isBlank()) {
            reader.setPhone(phone);
        }

        return reader;
    }

    public static class ReaderRequest {
        private String id;
        private String fullName;
        private String phone;
        private ReaderType type;

        public ReaderRequest() {
        }

        public ReaderRequest(String id, String fullName, String phone, ReaderType type) {
            this.id = id;
            this.fullName = fullName;
            this.phone = phone;
            this.type = type;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public ReaderType getType() {
            return type;
        }

        public void setType(ReaderType type) {
            this.type = type;
        }
    }
}

