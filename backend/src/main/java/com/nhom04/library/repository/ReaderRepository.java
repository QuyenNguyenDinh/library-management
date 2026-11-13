package com.nhom04.library.repository;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nhom04.library.model.Reader;
import com.nhom04.library.utils.GsonFactory;
import com.nhom04.library.utils.ReaderTypeAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository quản lý đọc/ghi danh sách Bạn đọc từ file readers.json.
 * Đăng ký {@link ReaderTypeAdapter} để hỗ trợ đa hình các lớp con của {@link Reader}.
 */
@Repository
public class ReaderRepository extends JsonFileRepository<Reader> {

    public ReaderRepository(@Value("${library.data-dir}") String dataDir) {
        super(dataDir, "readers.json", new TypeToken<List<Reader>>() {}.getType());
    }

    @Override
    protected String getId(Reader entity) {
        return entity.getId();
    }

    @Override
    protected Gson createGson() {
        return GsonFactory.builder()
                .registerTypeAdapter(Reader.class, new ReaderTypeAdapter())
                .create();
    }
}

