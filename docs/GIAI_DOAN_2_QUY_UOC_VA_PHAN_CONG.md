# TÀI LIỆU QUY ƯỚC DỮ LIỆU, API CONTRACT & PHÂN CÔNG DỰ ÁN
**Hệ thống Quản lý Thư viện (Library Management System)**  
*Dành cho nhóm phát triển 5 thành viên - Đính kèm Báo cáo cuối kỳ*

---

## 1. QUY ƯỚC DỮ LIỆU (JSON DATA SCHEMA)

Tất cả các tệp dữ liệu JSON được lưu trữ trong thư mục `backend/data/` gồm 3 tệp chính: `readers.json`, `books.json`, `tickets.json` (và mở rộng `return_records.json`).

### 1.1. Bạn đọc (Reader) - `backend/data/readers.json`
| Tên trường (Field) | Kiểu dữ liệu | Bắt buộc | Mô tả |
| :--- | :--- | :---: | :--- |
| `id` | `String` | Có | Mã định danh bạn đọc (VD: `"RD001"`, `"RD002"`...) |
| `name` | `String` | Có | Họ và tên đầy đủ của bạn đọc |
| `phone` | `String` | Có | Số điện thoại liên hệ (10 chữ số, bắt đầu bằng 0) |
| `readerType` | `String` | Có | Loại bạn đọc: `"NORMAL_STUDENT"`, `"PRIORITY_STUDENT"`, `"LECTURER"` |
| `maxBorrowLimit` | `int` | Có | Hạn mức số lượng sách được mượn tối đa (SV: 3, SV ưu tiên: 5, GV: 10) |
| `currentBorrowCount` | `int` | Có | Số lượng sách bạn đọc hiện đang mượn (mặc định 0) |

**Ví dụ JSON:**
```json
[
  {
    "id": "RD001",
    "name": "Nguyễn Văn An",
    "phone": "0912345678",
    "readerType": "NORMAL_STUDENT",
    "maxBorrowLimit": 3,
    "currentBorrowCount": 1
  },
  {
    "id": "RD002",
    "name": "Trần Thị Bích",
    "phone": "0987654321",
    "readerType": "PRIORITY_STUDENT",
    "maxBorrowLimit": 5,
    "currentBorrowCount": 0
  },
  {
    "id": "RD003",
    "name": "TS. Lê Hoàng Nam",
    "phone": "0901234567",
    "readerType": "LECTURER",
    "maxBorrowLimit": 10,
    "currentBorrowCount": 2
  }
]
```

---

### 1.2. Sách (Book) - `backend/data/books.json`
| Tên trường (Field) | Kiểu dữ liệu | Bắt buộc | Mô tả |
| :--- | :--- | :---: | :--- |
| `id` | `String` | Có | Mã định danh sách (VD: `"BK001"`, `"BK002"`...) |
| `title` | `String` | Có | Tên sách / Nhan đề |
| `author` | `String` | Có | Tên tác giả |
| `category` | `String` | Có | Thể loại sách (VD: "Công nghệ thông tin", "Kinh tế", "Văn học"...) |
| `quantity` | `int` | Có | Số lượng sách hiện có trong kho sẵn sàng cho mượn |
| `price` | `double` | Có | Giá trị sách (VNĐ) |

**Ví dụ JSON:**
```json
[
  {
    "id": "BK001",
    "title": "Lập trình Hướng đối tượng với Java",
    "author": "Đoàn Văn Ban",
    "category": "Công nghệ thông tin",
    "quantity": 10,
    "price": 125000.0
  },
  {
    "id": "BK002",
    "title": "Cấu trúc Dữ liệu và Giải thuật",
    "author": "Nguyễn Đức Nghĩa",
    "category": "Công nghệ thông tin",
    "quantity": 5,
    "price": 95000.0
  },
  {
    "id": "BK003",
    "title": "Kinh tế vi mô ứng dụng",
    "author": "Trần Văn Cường",
    "category": "Kinh tế",
    "quantity": 8,
    "price": 110000.0
  }
]
```

---

### 1.3. Phiếu mượn (BorrowTicket) - `backend/data/tickets.json`
| Tên trường (Field) | Kiểu dữ liệu | Bắt buộc | Mô tả |
| :--- | :--- | :---: | :--- |
| `id` | `String` | Có | Mã phiếu mượn (VD: `"TK001"`, `"TK002"`...) |
| `readerId` | `String` | Có | Mã bạn đọc thực hiện mượn sách |
| `bookId` | `String` | Có | Mã sách được mượn |
| `quantity` | `int` | Có | Số lượng mượn (thường là 1 hoặc nhiều cuốn) |
| `borrowDate` | `String` | Có | Ngày mượn định dạng chuẩn ISO-8601 `YYYY-MM-DD` |
| `dueDate` | `String` | Có | Ngày hẹn trả định dạng chuẩn ISO-8601 `YYYY-MM-DD` |
| `returnDate` | `String` | Không | Ngày trả thực tế (null nếu đang mượn) |
| `status` | `String` | Có | Trạng thái phiếu: `"BORROWING"` (Đang mượn), `"RETURNED"` (Đã trả) |
| `fineAmount` | `double` | Có | Tiền phạt phát sinh nếu trễ hạn (mặc định 0.0) |

**Ví dụ JSON:**
```json
[
  {
    "id": "TK001",
    "readerId": "RD001",
    "bookId": "BK001",
    "quantity": 1,
    "borrowDate": "2026-10-15",
    "dueDate": "2026-10-25",
    "returnDate": "2026-10-28",
    "status": "RETURNED",
    "fineAmount": 15000.0
  },
  {
    "id": "TK002",
    "readerId": "RD003",
    "bookId": "BK002",
    "quantity": 2,
    "borrowDate": "2026-10-20",
    "dueDate": "2026-11-03",
    "returnDate": null,
    "status": "BORROWING",
    "fineAmount": 0.0
  }
]
```

---

## 2. API CONTRACT (RESTFUL SPECIFICATION)

Quy ước chuẩn kết nối giữa Backend và Frontend để 2 nhóm code độc lập song song.  
**Mã phản hồi chuẩn:**
- `200 OK`: Thành công với payload trả về.
- `201 Created`: Tạo mới thành công.
- `400 Bad Request`: Lỗi dữ liệu gửi lên không hợp lệ hoặc vi phạm Business Rules (sách hết kho, vượt hạn mức...).
- `404 Not Found`: Không tìm thấy tài nguyên (không thấy Sách, Bạn đọc hoặc Phiếu mượn).
- `500 Internal Server Error`: Lỗi hệ thống máy chủ.

### 2.1. Quản lý Bạn đọc (Reader APIs)
| Method | Endpoint | Request Body | Response (Success) | Mô tả |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/readers` | Không | `200 OK` + List các Reader | Lấy toàn bộ danh sách bạn đọc |
| `GET` | `/api/readers/{id}` | Không | `200 OK` + Reader object | Lấy thông tin chi tiết một bạn đọc theo ID |
| `POST` | `/api/readers` | JSON thông tin bạn đọc mới | `201 Created` + Reader vừa tạo | Thêm mới bạn đọc (tự động phân loại & hạn mức) |
| `PUT` | `/api/readers/{id}` | JSON cập nhật thông tin | `200 OK` + Reader sau khi sửa | Cập nhật thông tin họ tên, SĐT của bạn đọc |
| `DELETE` | `/api/readers/{id}` | Không | `200 OK` (`{"message": "Xóa thành công"}`) | Xóa bạn đọc (chỉ cho phép khi currentBorrowCount == 0) |

---

### 2.2. Quản lý Sách (Book APIs)
| Method | Endpoint | Request Body | Response (Success) | Mô tả |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/books` | Tham số query `?search=...` (tùy chọn) | `200 OK` + List các Book | Lấy danh sách sách hoặc tìm kiếm theo tên/tác giả |
| `GET` | `/api/books/{id}` | Không | `200 OK` + Book object | Lấy chi tiết thông tin sách theo mã sách |
| `POST` | `/api/books` | JSON thông tin sách | `201 Created` + Book vừa tạo | Thêm mới đầu sách vào thư viện |
| `PUT` | `/api/books/{id}` | JSON thông tin cập nhật | `200 OK` + Book đã cập nhật | Cập nhật tên sách, số lượng, đơn giá |
| `DELETE` | `/api/books/{id}` | Không | `200 OK` (`{"message": "Xóa thành công"}`) | Xóa sách khỏi hệ thống |

---

### 2.3. Nghiệp vụ Mượn - Trả - Phạt (Borrow & Return APIs)
| Method | Endpoint | Request Body | Response (Success) | Mô tả |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/tickets` | Tham số `?status=BORROWING` hoặc `?readerId=...` | `200 OK` + List BorrowTicket | Lấy danh sách phiếu mượn (lọc theo trạng thái/bạn đọc) |
| `GET` | `/api/tickets/{id}` | Không | `200 OK` + BorrowTicket chi tiết | Lấy chi tiết phiếu mượn theo mã phiếu |
| `POST` | `/api/tickets/borrow` | `{"readerId": "RD001", "bookId": "BK001", "quantity": 1, "borrowDays": 14}` | `201 Created` + BorrowTicket | Mượn sách: Kiểm tra 7 rules, sinh phiếu mượn, trừ kho |
| `POST` | `/api/tickets/return` | `{"ticketId": "TK001", "returnDate": "2026-10-28"}` | `200 OK` + ReturnRecord & updated ticket | Trả sách: Kiểm tra phiếu, tăng lại kho, tính tiền phạt nếu trễ hạn |

---

## 3. BẢNG PHÂN CHIA NHIỆM VỤ 5 THÀNH VIÊN (CHO BÁO CÁO CUỐI KỲ)

| STT | Thành viên | Vai trò | Trách nhiệm chính | Sản phẩm bàn giao (Deliverables) |
| :---: | :--- | :--- | :--- | :--- |
| **1** | **Bạn (Trưởng nhóm)** | Lead Developer & Core Architect | - Thiết kế kiến trúc OOP gốc (FinePolicy, Borrowable, User, Reader và các lớp kế thừa).<br>- Lập quy ước dữ liệu (JSON & API Contract) và phân chia công việc.<br>- Cài đặt `BorrowReturnService` xử lý nghiêm ngặt 7 Business Rules mượn/trả/phạt.<br>- Quản lý kho mã nguồn Git, review Pull Request và merge code vào `main`. | - Bộ interface và class OOP lõi (`policy`, `model`).<br>- `BorrowReturnService.java` hoàn chỉnh.<br>- Tài liệu quy ước API Contract & phân công nhiệm vụ. |
| **2** | **Thành viên 2** | Backend Developer (Data & Storage) | - Xây dựng file JSON mẫu chuẩn tại `backend/data/` (`readers.json`, `books.json`, `tickets.json`).<br>- Lập trình package `repository` phụ trách đọc/ghi dữ liệu từ file JSON vào bộ nhớ và ngược lại.<br>- Lập trình package `utils`: validate dữ liệu (SĐT, số lượng, ngày tháng), tự động sinh ID tăng dần (`RD001`, `BK001`, `TK001`). | - `ReaderRepository.java`, `BookRepository.java`, `TicketRepository.java`.<br>- `ValidationUtils.java`, `IdGeneratorUtils.java`.<br>- Các file dữ liệu JSON mẫu ban đầu. |
| **3** | **Thành viên 3** | Backend Developer (Models & APIs) | - Hoàn thiện đầy đủ các model còn lại (`Book`, `BorrowTicket`, `ReturnRecord`...).<br>- Lập trình Service quản lý danh mục: `BookService`, `ReaderService` (thêm, sửa, xóa, tìm kiếm, hiển thị danh sách).<br>- Xây dựng tầng `handler` tiếp nhận các HTTP request, gọi Service và trả về response JSON theo đúng API Contract. | - `BookService.java`, `ReaderService.java`.<br>- Các class Handler trong `backend/src/handler/` (ReaderHandler, BookHandler, TicketHandler).<br>- Khung server HTTP điều phối request. |
| **4** | **Thành viên 4** | Frontend Developer (Catalog UI) | - Thiết kế và xây dựng giao diện Quản lý Sách: Bảng danh sách sách, form thêm sách mới, modal sửa sách, chức năng tìm kiếm/lọc.<br>- Thiết kế và xây dựng giao diện Quản lý Bạn đọc: Danh sách bạn đọc, phân loại sinh viên/giảng viên, form thêm/sửa bạn đọc.<br>- Kết nối gọi API từ Backend theo đúng API Contract. | - Giao diện Quản lý Sách (`index.html`, `books.html` hoặc React/Vue component).<br>- Giao diện Quản lý Bạn đọc (`readers.html`).<br>- File script xử lý gọi fetch API. |
| **5** | **Thành viên 5** | Frontend Developer & QA / Reporter | - Thiết kế và xây dựng giao diện Mượn/Trả sách: Form mượn sách, bảng tra cứu phiếu mượn, nút xác nhận trả sách và hiển thị phí phạt tự động.<br>- Vẽ sơ đồ kiến trúc Class Diagram (UML) thể hiện các mối quan hệ Kế thừa, Đa hình, Trừu tượng và Đóng gói.<br>- Soạn thảo kịch bản kiểm thử (Test Cases), biên soạn tài liệu Báo cáo cuối kỳ và thiết kế Slide báo cáo bảo vệ. | - Giao diện Mượn/Trả sách (`borrow-return.html`).<br>- File sơ đồ Class Diagram (ảnh PNG + file vẽ).<br>- Bộ kịch bản Test Case & Slide thuyết trình PowerPoint/Canva. |

---

## 4. QUY TẮC LÀM VIỆC VỚI GIT (GIT WORKFLOW & COLLABORATION)

Để đảm bảo mã nguồn của nhóm không bị xung đột (conflict) và mã nguồn trên nhánh chính luôn luôn hoạt động ổn định:

1. **QUY TẮC BẤT DI BẤT DỊCH: CẤM PUSH TRỰC TIẾP VÀO NHÁNH `main`**
   - Nhánh `main` chỉ chứa phiên bản mã nguồn đã qua kiểm thử, đã được review và merge bởi Trưởng nhóm.

2. **QUY ƯỚC ĐẶT TÊN NHÁNH:**
   Mỗi thành viên khi làm nhiệm vụ được giao phải tạo nhánh riêng tách từ `main`:
   - Làm tính năng mới: `feature/<tên-chức-năng>`  
     *Ví dụ:* `feature/json-repository`, `feature/book-management-service`, `feature/frontend-book-ui`, `feature/borrow-return-service`.
   - Sửa lỗi phát sinh: `fix/<tên-lỗi>`

3. **QUY TRÌNH LÀM VIỆC TỪNG BƯỚC CHO THÀNH VIÊN:**
   - **Bước 1: Cập nhật code mới nhất từ main trước khi code:**
     ```bash
     git checkout main
     git pull origin main
     ```
   - **Bước 2: Tạo nhánh mới để làm việc:**
     ```bash
     git checkout -b feature/<ten-nhiem-vu>
     ```
   - **Bước 3: Lập trình, kiểm thử cục bộ và commit rõ ràng:**
     ```bash
     git add .
     git commit -m "feat: mo ta ngan gon cong viec da lam"
     ```
   - **Bước 4: Đẩy nhánh lên GitHub:**
     ```bash
     git push -u origin feature/<ten-nhiem-vu>
     ```
   - **Bước 5: Tạo Pull Request (PR) trên GitHub:**
     - Truy cập GitHub repository, tạo Pull Request từ nhánh `feature/...` vào nhánh `main`.
     - Điền mô tả chi tiết: Đã làm gì, các hàm/file nào mới, cách kiểm tra.
     - Tag **Trưởng nhóm** vào mục Reviewers.
   - **Bước 6: Trưởng nhóm review và merge:**
     - Trưởng nhóm kiểm tra code, chạy thử. Nếu đạt tiêu chuẩn mới thực hiện `Merge Pull Request`.
     - Nếu có lỗi hoặc chưa chuẩn OOP/quy ước, Trưởng nhóm ghi comment yêu cầu thành viên sửa lại trên nhánh đó.

