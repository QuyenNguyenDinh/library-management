package com.nhom04.library.repository;

import com.google.gson.reflect.TypeToken;
import com.nhom04.library.model.Admin;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository quản lý đọc/ghi danh sách Tài khoản Thủ thư từ file admins.json.
 */
@Repository
public class AdminRepository extends JsonFileRepository<Admin> {

    public AdminRepository(@Value("${library.data-dir}") String dataDir) {
        super(dataDir, "admins.json", new TypeToken<List<Admin>>() {}.getType());
    }

    @Override
    protected String getId(Admin entity) {
        return entity.getId();
    }

    /**
     * Tìm tài khoản thủ thư theo tên đăng nhập (không phân biệt chữ hoa, chữ thường).
     */
    public Optional<Admin> findByUsername(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        return findAll().stream()
                .filter(admin -> username.trim().equalsIgnoreCase(admin.getUsername()))
                .findFirst();
    }
}

