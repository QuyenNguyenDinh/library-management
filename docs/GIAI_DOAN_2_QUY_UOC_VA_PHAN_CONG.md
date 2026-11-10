# QUY ƯỚC CODE, API CONTRACT & PHÂN CÔNG — NHÓM 04
**Hệ thống quản lý mượn sách thư viện sinh viên**

> Đây là "hợp đồng" chung của cả nhóm. Mọi người code **đúng tên class, tên method và định dạng JSON** ở đây thì ghép lại sẽ chạy được ngay.
> Muốn đổi bất kỳ điều gì trong file này → nhắn trưởng nhóm, **không tự sửa**.

---

## 1. Công nghệ & cách chạy

| Phần | Công nghệ | Thư mục |
| :--- | :--- | :--- |
| Backend | Java 21, Spring Boot 4, Gson | `backend/` |
| Mock database | File JSON | `backend/data/` |
| Frontend | React + Vite | `frontend/` (TV4 tạo) |

```bash
# Backend (không cần cài Maven, dùng wrapper có sẵn)
cd backend
mvnw.cmd spring-boot:run      # Windows  →  http://localhost:8080
./mvnw spring-boot:run        # macOS/Linux
mvnw.cmd test                 # chạy toàn bộ test

# Frontend
cd frontend
npm install
npm run dev                   # http://localhost:5173 (proxy /api sang 8080)
```

---

## 2. Phân công (5 phần, mỗi người ~1.100–1.200 dòng)

Mỗi người làm **trọn một mảng từ model → repository → service → handler → giao diện → test**.

### TV1 — Trưởng nhóm: Khung backend + Mượn sách
| Tầng | File |
| :--- | :--- |
| Khung | `pom.xml`, `main/LibraryApplication`, `main/CorsConfig` ✅ |
| Exception | `exception/*` (cây ngoại lệ, xem mục 5) ✅ |
| Handler chung | `handler/GlobalExceptionHandler`, `utils/ApiResponse` ✅ |
| Policy | `policy/Borrowable` ✅ |
| Model | `model/BorrowTicket`, `model/BorrowTicketDetail`, `model/TicketStatus` ✅ |
| Service | `service/BorrowService` |
| Handler | `handler/BorrowHandler` |
| Frontend | Trang **Mượn sách** |
| Test | `BorrowTicketTest` ✅, `BorrowServiceTest` |

### TV2: Bạn đọc + Tài khoản thủ thư
| Tầng | File |
| :--- | :--- |
| Model | `User` (abstract), `Reader` (abstract), `StudentReader`, `PriorityStudentReader`, `LecturerReader`, `ReaderType`, `Admin` |
| Utils | `ReaderTypeAdapter` (Gson tạo đúng lớp con theo trường `type`) |
| Repository | `ReaderRepository`, `AdminRepository` |
| Service | `ReaderService`, `AuthService` (băm SHA-256) |
| Handler | `ReaderHandler`, `AuthHandler` |
| Data | `readers.json`, `admins.json` |
| Frontend | Trang **Bạn đọc** |
| Test | `ReaderServiceTest`, `AuthServiceTest` |

### TV3: Sách + Hạ tầng JSON
| Tầng | File |
| :--- | :--- |
| Model | `Book implements Borrowable` |
| Repository | `Repository<T, ID>` (interface), `JsonFileRepository<T>` (abstract, **cả nhóm dùng**), `BookRepository` |
| Utils | `GsonFactory`, `LocalDateAdapter`, `IdGenerator`, `Validator` |
| Service | `BookService` |
| Handler | `BookHandler` |
| Data | `books.json` (~10 sách) |
| Frontend | Trang **Sách** |
| Test | `BookServiceTest`, `JsonFileRepositoryTest` |

### TV4: Trả sách + Chính sách phạt + Khung frontend
| Tầng | File |
| :--- | :--- |
| Policy | `FinePolicy`, `DailyRateFinePolicy` (abstract), `NormalStudentFinePolicy`, `PriorityStudentFinePolicy`, `LecturerFinePolicy` |
| Model | `ReturnRecord` |
| Repository | `TicketRepository`, `ReturnRecordRepository` |
| Service | `ReturnService` |
| Handler | `ReturnHandler` |
| Frontend khung | Tạo project Vite, `App.jsx` (routing), `Layout`/`Sidebar`, `api/client.js`, `Toast`, `ConfirmModal` |
| Frontend | Trang **Trả sách** |
| Test | `ReturnServiceTest`, `FinePolicyTest` |

### TV5: Theo dõi phiếu + Thống kê + Đăng nhập (giao diện)
| Tầng | File |
| :--- | :--- |
| Service | `TicketQueryService`, `StatsService` |
| Handler | `TicketHandler`, `StatsHandler` |
| Data | `tickets.json` mẫu (có cả phiếu quá hạn để demo) |
| Frontend | `DataTable` (bảng dùng chung), trang **Phiếu mượn**, **Thống kê**, **Đăng nhập** (+ route guard) |
| Test | `TicketQueryServiceTest`, `StatsServiceTest` |

### Báo cáo — mỗi người tự làm phần của mình
- Vẽ class diagram cho mảng của mình (TV1 ghép thành sơ đồ tổng).
- Viết 2–3 tình huống test (mục 8) và 2–3 slide.

---

## 3. Cấu trúc package

Package gốc: `com.nhom04.library` — đặt file **đúng package** dưới đây.

```
backend/src/main/java/com/nhom04/library/
├── main/        LibraryApplication, CorsConfig
├── model/       User, Reader, StudentReader, PriorityStudentReader, LecturerReader, ReaderType,
│                Admin, Book, BorrowTicket, BorrowTicketDetail, ReturnRecord, TicketStatus
├── policy/      Borrowable, FinePolicy, DailyRateFinePolicy, 3 chính sách phạt
├── repository/  Repository, JsonFileRepository, BookRepository, ReaderRepository,
│                TicketRepository, ReturnRecordRepository, AdminRepository
├── service/     BookService, ReaderService, BorrowService, ReturnService,
│                TicketQueryService, StatsService, AuthService
├── handler/     GlobalExceptionHandler, BookHandler, ReaderHandler, BorrowHandler,
│                ReturnHandler, TicketHandler, StatsHandler, AuthHandler
├── exception/   cây ngoại lệ (mục 5)
└── utils/       ApiResponse, GsonFactory, LocalDateAdapter, ReaderTypeAdapter, IdGenerator, Validator
backend/src/test/java/com/nhom04/library/   (test đặt cùng package với class được test)
```

**Quy tắc tầng** (giảng viên sẽ hỏi):
- `handler` chỉ nhận request → gọi `service` → trả `ApiResponse`. **Không try-catch, không viết business rule.**
- `service` kiểm tra business rule, ném exception. **Không biết HTTP, không đọc file.**
- `repository` chỉ đọc/ghi JSON. **Không kiểm tra business rule.**
- `model`: thuộc tính `private`, setter kiểm tra dữ liệu và ném `IllegalArgumentException`, có constructor rỗng cho Gson.
- Class Spring: `@RestController` (handler), `@Service` (service), `@Repository` (repository); tiêm phụ thuộc qua **constructor**.

---

## 4. Chữ ký method dùng chung (bắt buộc giữ đúng)

```java
// ===== TV3 — repository/Repository.java
public interface Repository<T, ID> {
    List<T> findAll();
    Optional<T> findById(ID id);
    T save(T entity);            // chưa có id đó thì thêm, có rồi thì thay thế; ghi lại toàn bộ file
    boolean deleteById(ID id);   // true nếu có xoá
}

// ===== TV3 — repository/JsonFileRepository.java
public abstract class JsonFileRepository<T> implements Repository<T, String> {
    protected JsonFileRepository(String dataDir, String fileName, Type listType);
    protected abstract String getId(T entity);
    protected Gson createGson();  // mặc định GsonFactory.builder().create(); lớp con override nếu cần thêm adapter
}
// Lớp con nhận dataDir qua: @Value("${library.data-dir}") String dataDir

// ===== TV3 — utils
GsonFactory.builder()                         // GsonBuilder đã đăng ký LocalDateAdapter (yyyy-MM-dd), pretty printing
IdGenerator.next(String prefix, Collection<String> existingIds)   // ("TK", [TK001, TK007]) -> "TK008"

// ===== TV3 — model/Book.java
public class Book implements Borrowable {
    // id, title, author, category, availableQuantity, price (+ getter/setter có validate)
    // borrowItem(q) ném BookOutOfStockException nếu không đủ; returnItem(q) ném IllegalArgumentException nếu q <= 0
}

// ===== TV2 — model/Reader.java
public abstract class Reader extends User {          // User: id, fullName, phone
    public abstract int getMaxBorrowLimit();          // 3 / 5 / 10
    public abstract FinePolicy getFinePolicy();       // policy tương ứng của TV4
    public abstract ReaderType getType();             // STUDENT / PRIORITY_STUDENT / LECTURER
}
// ReaderRepository override createGson() để đăng ký ReaderTypeAdapter

// ===== TV4 — policy/FinePolicy.java
public interface FinePolicy {
    double calculateFine(long overdueDays);   // overdueDays <= 0 -> 0
    double getDailyRate();
    String getPolicyName();
}

// ===== TV4 — repository/TicketRepository.java
public class TicketRepository extends JsonFileRepository<BorrowTicket> {
    List<BorrowTicket> findByStatus(TicketStatus status);
    List<BorrowTicket> findByReaderId(String readerId);
}

// ===== TV1 — đã có sẵn: policy/Borrowable, model/BorrowTicket, model/BorrowTicketDetail,
//              model/TicketStatus, utils/ApiResponse (xem code)
```

---

## 5. Exception — chọn đúng nhóm cha

`GlobalExceptionHandler` tự chuyển exception thành mã HTTP **theo nhóm cha**. Cần exception mới thì tạo lớp kế thừa đúng nhóm, **không sửa `GlobalExceptionHandler`**.

| Nhóm cha | HTTP | Đã có sẵn |
| :--- | :---: | :--- |
| `NotFoundException` | 404 | `BookNotFoundException`, `ReaderNotFoundException`, `TicketNotFoundException` |
| `BusinessRuleException` | 409 | `BookOutOfStockException`, `BorrowLimitExceededException`, `InvalidTicketStateException` |
| `InvalidRequestException` | 400 | `InvalidBorrowRequestException` |
| `IllegalArgumentException` (setter model) | 400 | — |
| Lỗi khác | 500 | — |

Ví dụ: chặn xoá sách đang được mượn → TV3 tạo `BookInUseException extends BusinessRuleException`.
**Message phải nói rõ nguyên nhân** (đề yêu cầu), ví dụ: `"Sách 'BK002' chỉ còn 0 cuốn, không đủ để mượn 1 cuốn."`
Đăng nhập sai: `AuthHandler` tự trả `ResponseEntity.status(401)`.

---

## 6. Cấu trúc file JSON (`backend/data/`)

Ngày luôn có dạng `"yyyy-MM-dd"`. Mã tự sinh: `BK001`, `RD001`, `TK001`, `RR001`.

```jsonc
// books.json (TV3)
[{ "id": "BK001", "title": "Lập trình hướng đối tượng với Java", "author": "Nguyễn Văn A",
   "category": "Công nghệ thông tin", "availableQuantity": 5, "price": 120000 }]

// readers.json (TV2) — "type": STUDENT | PRIORITY_STUDENT | LECTURER
[{ "id": "RD001", "fullName": "Trần Thị B", "phone": "0901234567", "type": "STUDENT" }]

// admins.json (TV2) — passwordHash = SHA-256 hex của "admin123"
[{ "id": "AD001", "fullName": "Thủ thư", "phone": "0900000000", "username": "admin",
   "passwordHash": "240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9" }]

// tickets.json (TV5 tạo dữ liệu mẫu, TV4 viết repository) — status: BORROWING | RETURNED
[{ "id": "TK001", "readerId": "RD001", "borrowDate": "2026-11-01", "dueDate": "2026-11-15",
   "returnDate": null, "status": "BORROWING", "fineAmount": 0,
   "details": [{ "bookId": "BK001", "bookTitle": "Lập trình hướng đối tượng với Java", "quantity": 1 }] }]

// return_records.json (TV4)
[{ "id": "RR001", "ticketId": "TK001", "readerId": "RD001", "returnDate": "2026-11-19",
   "overdueDays": 4, "fineAmount": 20000, "policyName": "Sinh viên thường (5.000đ/ngày)" }]
```

---

## 7. API contract

Mọi response: `{ "success": true|false, "message": "...", "data": ... }`

| Method | Endpoint | Người làm | Body / Ghi chú | Mã trả về |
| :--- | :--- | :---: | :--- | :--- |
| GET | `/api/books?keyword=` | TV3 | tìm theo mã hoặc tên (không phân biệt hoa thường) | 200 |
| GET | `/api/books/{id}` | TV3 | | 200 / 404 |
| GET | `/api/books/{id}/availability?quantity=` | TV3 | `data: { available: true, availableQuantity: 5 }` | 200 / 404 |
| POST / PUT / DELETE | `/api/books`, `/api/books/{id}` | TV3 | DELETE bị chặn nếu sách đang được mượn | 201 / 200 / 409 |
| GET | `/api/readers?keyword=` · `/api/readers/{id}` | TV2 | `data` có thêm `maxBorrowLimit`, `currentBorrowCount` | 200 / 404 |
| POST / PUT / DELETE | `/api/readers`, `/api/readers/{id}` | TV2 | DELETE bị chặn nếu đang mượn | 201 / 200 / 409 |
| POST | `/api/auth/login` | TV2 | `{ username, password }` | 200 / 401 |
| POST | `/api/borrow` | TV1 | `{ readerId, items: [{ bookId, quantity }], borrowDate?, dueDate? }` (mặc định hôm nay, +14 ngày) | 201 / 400 / 404 / 409 |
| POST | `/api/return/{ticketId}` | TV4 | `{ returnDate? }` (mặc định hôm nay) → `data`: ReturnRecord | 200 / 404 / 409 |
| GET | `/api/return-records` | TV4 | | 200 |
| GET | `/api/tickets?status=&readerId=` | TV5 | mỗi phiếu thêm `daysBorrowed`, `overdue` | 200 |
| GET | `/api/tickets/{id}` | TV5 | | 200 / 404 |
| GET | `/api/stats` | TV5 | `{ overdueCount, totalFine, topBooks: [...], lowStockBooks: [...] }` | 200 |

---

## 8. Tình huống test bắt buộc

| # | Tình huống | Kỳ vọng | Người viết |
| :---: | :--- | :--- | :---: |
| 1 | RD001 mượn 1 cuốn BK001 (còn 5) | 201, BK001 còn 4, phiếu "Đang mượn" | TV1 |
| 2 | Mượn sách có số lượng hiện có = 0 | 409 | TV1 |
| 3 | Mượn với quantity = 0 hoặc âm | 400 | TV1 |
| 4 | Mượn sách / bạn đọc không tồn tại | 404 | TV1 |
| 5 | SV thường đang mượn 3, mượn thêm 1 | 409 vượt hạn mức | TV1 |
| 6 | Trả đúng hạn | Phạt 0đ, kho tăng lại, "Đã trả" | TV4 |
| 7 | SV thường trả trễ 4 ngày | 20.000đ | TV4 |
| 8 | Giảng viên trả trễ 4 ngày | 8.000đ (đa hình FinePolicy) | TV4 |
| 9 | Trả lại phiếu đã trả / phiếu không tồn tại | 409 / 404 | TV4 |
| 10 | Tìm sách theo tên, thêm/xoá sách | | TV3 |
| 11 | Thêm/sửa/xoá bạn đọc, đăng nhập đúng/sai | | TV2 |
| 12 | Lọc phiếu quá hạn, thống kê tổng phạt | | TV5 |

Test **không được ghi vào `backend/data/` thật** — dùng thư mục tạm (`@TempDir` của JUnit).

---

## 9. Lịch làm việc

| Mốc | Việc |
| :--- | :--- |
| **Tuần 1** | Làm và merge **phần nền** trước: TV3 `Repository`/`JsonFileRepository`/`GsonFactory`/`Book` · TV2 `User`/`Reader` + 3 lớp con · TV4 `FinePolicy` + 3 policy, `TicketRepository`, khung React · TV5 `DataTable`, `tickets.json` mẫu |
| **Tuần 2–3** | Service, handler, giao diện, test của từng mảng |
| **Tuần 4** | Ghép hệ thống, chạy đủ tình huống test, class diagram, slide, video demo |

---

## 10. Quy tắc Git

1. **Không push thẳng vào `main`.** Mỗi việc một nhánh `feature/<mảng>` (vd `feature/book`, `feature/reader`), mở Pull Request, trưởng nhóm review rồi merge.
2. Trước khi code: `git checkout main && git pull` rồi tạo nhánh mới từ `main`.
3. **Commit bằng tài khoản của chính mình** — kiểm tra `git config user.name` và `git config user.email` trùng với GitHub, nếu không số dòng không được tính cho bạn.
4. **Chỉ sửa file của mình.** Thấy lỗi ở file người khác → báo người đó sửa.
5. PR phải build và test pass (`mvnw.cmd test`) trước khi gửi review.
6. Message commit: `feat(book): them BookService tim kiem theo ten`, `fix(borrow): ...`, `test(return): ...`.
