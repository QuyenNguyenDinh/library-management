package com.nhom04.library.utils;

import com.google.gson.*;
import com.nhom04.library.model.*;

import java.lang.reflect.Type;

/**
 * TypeAdapter cho Gson giúp serialize và deserialize đa hình đối tượng {@link Reader}
 * thành đúng các lớp con ({@link StudentReader}, {@link PriorityStudentReader}, {@link LecturerReader})
 * dựa theo trường dữ liệu {@code type}.
 */
public class ReaderTypeAdapter implements JsonSerializer<Reader>, JsonDeserializer<Reader> {

    @Override
    public JsonElement serialize(Reader src, Type typeOfSrc, JsonSerializationContext context) {
        if (src == null) {
            return JsonNull.INSTANCE;
        }

        JsonObject obj = new JsonObject();
        if (src.getId() != null) {
            obj.addProperty("id", src.getId());
        }
        if (src.getFullName() != null) {
            obj.addProperty("fullName", src.getFullName());
        }
        if (src.getPhone() != null) {
            obj.addProperty("phone", src.getPhone());
        }
        if (src.getType() != null) {
            obj.addProperty("type", src.getType().name());
        }
        obj.addProperty("maxBorrowLimit", src.getMaxBorrowLimit());
        obj.addProperty("currentBorrowCount", src.getCurrentBorrowCount());

        return obj;
    }

    @Override
    public Reader deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if (json == null || !json.isJsonObject()) {
            return null;
        }

        JsonObject obj = json.getAsJsonObject();

        String id = obj.has("id") && !obj.get("id").isJsonNull() ? obj.get("id").getAsString() : null;
        String fullName = obj.has("fullName") && !obj.get("fullName").isJsonNull() ? obj.get("fullName").getAsString() : null;
        String phone = obj.has("phone") && !obj.get("phone").isJsonNull() ? obj.get("phone").getAsString() : null;
        int currentBorrowCount = obj.has("currentBorrowCount") && !obj.get("currentBorrowCount").isJsonNull()
                ? obj.get("currentBorrowCount").getAsInt() : 0;

        String typeStr = obj.has("type") && !obj.get("type").isJsonNull()
                ? obj.get("type").getAsString() : "STUDENT";

        ReaderType readerType;
        try {
            readerType = ReaderType.valueOf(typeStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            readerType = ReaderType.STUDENT;
        }

        Reader reader = switch (readerType) {
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
        reader.setCurrentBorrowCount(currentBorrowCount);

        return reader;
    }
}

