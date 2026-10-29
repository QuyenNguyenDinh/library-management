package model;

/**
 * Định nghĩa các loại Bạn đọc trong thư viện.
 */
public enum ReaderType {
    NORMAL_STUDENT("Sinh viên thường"),
    PRIORITY_STUDENT("Sinh viên ưu tiên"),
    LECTURER("Giảng viên");

    private final String description;

    ReaderType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return description;
    }
}

