package com.nhom04.library.repository;

import com.google.gson.Gson;
import com.nhom04.library.utils.GsonFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public abstract class JsonFileRepository<T> implements Repository<T, String> {

    private final Path filePath;
    private final Type listType;
    private final Gson gson;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    protected JsonFileRepository(String dataDir, String fileName, Type listType) {
        this.filePath = Paths.get(dataDir, fileName);
        this.listType = listType;
        this.gson = createGson();
        ensureFileExists();
    }

    protected abstract String getId(T entity);

    protected Gson createGson() {
        return GsonFactory.builder().create();
    }

    private void ensureFileExists() {
        try {
            if (filePath.getParent() != null) {
                Files.createDirectories(filePath.getParent());
            }
            if (!Files.exists(filePath)) {
                Files.writeString(filePath, "[]", StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new RuntimeException("Không thể khởi tạo file dữ liệu: " + filePath, e);
        }
    }

    protected List<T> readAll() {
        lock.readLock().lock();
        try (Reader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            List<T> items = gson.fromJson(reader, listType);
            return items != null ? new ArrayList<>(items) : new ArrayList<>();
        } catch (IOException e) {
            throw new RuntimeException("Lỗi đọc file JSON: " + filePath, e);
        } finally {
            lock.readLock().unlock();
        }
    }

    protected void writeAll(List<T> items) {
        lock.writeLock().lock();
        try {
            if (filePath.getParent() != null) {
                Files.createDirectories(filePath.getParent());
            }
            Path tempFile = filePath.resolveSibling(filePath.getFileName() + ".tmp");
            try (Writer writer = Files.newBufferedWriter(tempFile, StandardCharsets.UTF_8)) {
                gson.toJson(items, listType, writer);
            }
            Files.move(tempFile, filePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Lỗi ghi file JSON: " + filePath, e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public List<T> findAll() {
        return readAll();
    }

    @Override
    public Optional<T> findById(String id) {
        if (id == null) return Optional.empty();
        return readAll().stream()
                .filter(item -> id.equals(getId(item)))
                .findFirst();
    }

    @Override
    public T save(T entity) {
        lock.writeLock().lock();
        try {
            List<T> items = readAll();
            String id = getId(entity);
            boolean updated = false;
            for (int i = 0; i < items.size(); i++) {
                if (id != null && id.equals(getId(items.get(i)))) {
                    items.set(i, entity);
                    updated = true;
                    break;
                }
            }
            if (!updated) {
                items.add(entity);
            }
            writeAll(items);
            return entity;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean deleteById(String id) {
        if (id == null) return false;
        lock.writeLock().lock();
        try {
            List<T> items = readAll();
            boolean removed = items.removeIf(item -> id.equals(getId(item)));
            if (removed) {
                writeAll(items);
            }
            return removed;
        } finally {
            lock.writeLock().unlock();
        }
    }
}