package com.nhom04.library.repository;

import com.google.gson.reflect.TypeToken;
import com.nhom04.library.model.ReturnRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Chỉ đọc/ghi return_records.json, không có business rule. */
@Repository
public class ReturnRecordRepository extends JsonFileRepository<ReturnRecord> {

    public ReturnRecordRepository(@Value("${library.data-dir}") String dataDir) {
        super(dataDir, "return_records.json", new TypeToken<List<ReturnRecord>>() { }.getType());
    }

    @Override
    protected String getId(ReturnRecord entity) {
        return entity.getId();
    }
}
