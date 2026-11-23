# Library Management

Hệ thống quản lý thư viện cho phép thủ thư quản lý sách, bạn đọc, phiếu mượn, trả sách và thống kê hoạt động thư viện.

Ứng dụng gồm hai phần:

- **Backend:** Spring Boot, Java 21, cung cấp REST API.
- **Frontend:** React 18 và Vite, giao diện desktop cho thủ thư.
- **Lưu trữ:** các tệp JSON trong `backend/data`, không cần cài cơ sở dữ liệu.

## 1. Chức năng chính

### Quản lý sách

- Danh sách sách, tìm kiếm theo mã hoặc tên.
- Lọc theo thể loại và tình trạng tồn kho.
- Sắp xếp theo mã, tên, tồn kho hoặc giá bìa.
- Thêm, sửa, xóa và xem chi tiết sách.
- Kiểm tra số lượng có thể đáp ứng trước khi lập phiếu.
- Xuất danh mục sách ra CSV.

### Quản lý bạn đọc

- Thêm, sửa, xóa và tìm kiếm bạn đọc.
- Phân loại sinh viên thường, sinh viên ưu tiên và giảng viên.
- Hiển thị hạn mức và số sách đang mượn.
- Xem phiếu đang mượn, lịch sử trả và tiền phạt.
- Lọc theo loại bạn đọc, chưa mượn, đang mượn hoặc có phiếu quá hạn.

### Mượn sách

- Chọn bạn đọc và kiểm tra hạn mức còn lại.
- Tìm sách, xem tồn kho và thêm nhiều đầu sách vào cùng một phiếu.
- Điều chỉnh số lượng từng đầu sách.
- Chọn ngày mượn, hạn trả nhanh 7/14/30 ngày hoặc ngày tùy chọn.
- Kiểm tra trước hạn mức và tồn kho trước khi gửi.
- Tự động cập nhật tồn kho sau khi lập phiếu.

### Trả sách

- Lọc phiếu đang mượn theo trong hạn, đến hạn hôm nay và quá hạn.
- Tìm theo mã phiếu, bạn đọc hoặc tên sách.
- Xem trước số ngày trễ và tiền phạt dự kiến.
- Trả toàn bộ sách trong một phiếu.
- Tự động cộng sách trở lại kho, đóng phiếu và ghi lịch sử trả.
- Tra cứu lịch sử trả theo ngày và tình trạng tiền phạt.

### Phiếu mượn và thống kê

- Tra cứu phiếu theo trạng thái, bạn đọc, tên sách và khoảng ngày.
- Xem chi tiết bạn đọc, sách, số lượng, thời gian và tiền phạt.
- Dashboard gồm tổng sách, tồn kho, sách đang được mượn, bạn đọc, phiếu quá hạn và tiền phạt.
- Biểu đồ số phiếu theo tháng, trạng thái phiếu và sách được mượn nhiều nhất.
- Danh sách phiếu quá hạn và sách hết hoặc sắp hết.

## 2. Kiến trúc hệ thống

```text
┌──────────────────────────────┐
│ React + Vite                 │
│ pages / components / api     │
└──────────────┬───────────────┘
               │ HTTP JSON /api/**
┌──────────────▼───────────────┐
│ Spring Boot                  │
│ handler → service → repo     │
│ model / policy / exception   │
└──────────────┬───────────────┘
               │ Gson + file lock
┌──────────────▼───────────────┐
│ backend/data/*.json          │
└──────────────────────────────┘
```

### Backend

- `handler`: nhận HTTP request, chuyển dữ liệu vào service và trả response.
- `service`: chứa nghiệp vụ mượn, trả, quản lý sách, bạn đọc, phiếu và thống kê.
- `repository`: đọc/ghi dữ liệu JSON thông qua `JsonFileRepository`.
- `model`: các đối tượng `Book`, `Reader`, `BorrowTicket`, `ReturnRecord` và `Admin`.
- `policy`: chính sách tính phạt theo loại bạn đọc.
- `exception`: lỗi không tìm thấy, sai dữ liệu và vi phạm nghiệp vụ.
- `utils`: response thống nhất, sinh mã, validate và adapter Gson.

### Frontend

- `src/pages`: màn hình Login, Books, Readers, Borrow, Return, Tickets và Stats.
- `src/components`: Layout, Sidebar, modal, toast, phân trang và chi tiết phiếu.
- `src/api`: các API client dùng chung `fetch`.
- `src/utils/libraryView.js`: hàm hiển thị trạng thái phiếu, tìm kiếm không dấu và tính tổng số cuốn.
- `src/styles.css`: biến màu, bố cục, bảng, form, modal và trạng thái.

## 3. Luồng hoạt động

### Đăng nhập

1. Người dùng gửi username và password tới `POST /api/auth/login`.
2. Backend tìm tài khoản trong `backend/data/admins.json`.
3. Mật khẩu gửi lên được băm SHA-256 để so sánh với `passwordHash`.
4. Frontend lưu thông tin thủ thư trong `sessionStorage`.
5. `RequireAuth` chặn các route giao diện nếu chưa có phiên đăng nhập.

### Lập phiếu mượn

1. Frontend tải danh sách bạn đọc và sách.
2. Người dùng chọn bạn đọc và các đầu sách.
3. Frontend kiểm tra sơ bộ số lượng, hạn mức và ngày.
4. `BorrowService` kiểm tra lại toàn bộ dữ liệu ở backend.
5. Backend gộp các dòng trùng mã sách.
6. Backend kiểm tra bạn đọc, sách, hạn mức và tồn kho.
7. Nếu hợp lệ, backend trừ tồn kho và lưu phiếu trạng thái `BORROWING`.
8. Frontend tải lại dữ liệu và hiển thị mã phiếu mới.

### Trả sách

1. Frontend tải các phiếu có trạng thái `BORROWING`.
2. Người dùng chọn phiếu và ngày trả thực tế.
3. Frontend hiển thị số ngày trễ và tiền phạt dự kiến.
4. `ReturnService` kiểm tra phiếu, bạn đọc, sách và ngày trả.
5. Backend cộng số lượng sách vào kho.
6. Phiếu chuyển sang `RETURNED`, lưu ngày trả và tiền phạt.
7. Backend tạo `ReturnRecord` để lưu lịch sử.
8. Frontend cập nhật lại danh sách và hiển thị kết quả.

## 4. Quy tắc nghiệp vụ

- Hạn trả mặc định là ngày mượn cộng 14 ngày.
- Hạn trả không được trước ngày mượn.
- Ngày trả không được trước ngày mượn.
- Mỗi phiếu phải có ít nhất một đầu sách.
- Số lượng mượn phải là số nguyên dương.
- Không được mượn quá số lượng còn trong kho.
- Không được vượt hạn mức theo loại bạn đọc:

| Loại bạn đọc | Hạn mức | Phạt trễ |
| --- | ---: | ---: |
| Sinh viên thường | 3 cuốn | 5.000đ/ngày |
| Sinh viên ưu tiên | 5 cuốn | 3.000đ/ngày |
| Giảng viên | 10 cuốn | 2.000đ/ngày |

- Không thể trả lại phiếu đã ở trạng thái `RETURNED`.
- Không thể xóa sách đang xuất hiện trong phiếu mượn chưa trả.
- Không thể xóa bạn đọc đang còn sách chưa trả.
- Tiền phạt được tính theo công thức `số ngày trễ × đơn giá của loại bạn đọc`.
- Tồn kho chỉ thay đổi khi nghiệp vụ mượn hoặc trả được backend chấp nhận.

## 5. Mô hình dữ liệu

### Book

```json
{
  "id": "BK001",
  "title": "Lập trình hướng đối tượng với Java",
  "author": "Phạm Văn Ất",
  "category": "Công nghệ thông tin",
  "availableQuantity": 5,
  "price": 120000
}
```

### Reader

```json
{
  "id": "RD001",
  "fullName": "Trần Thị B",
  "phone": "0901234567",
  "type": "STUDENT",
  "currentBorrowCount": 0
}
```

`maxBorrowLimit` và `finePolicy` được tính theo `type` khi trả về API, không cần lưu cố định trong JSON.

### BorrowTicket

```json
{
  "id": "TK001",
  "readerId": "RD001",
  "details": [
    {
      "bookId": "BK001",
      "bookTitle": "Lập trình hướng đối tượng với Java",
      "quantity": 1
    }
  ],
  "borrowDate": "2026-11-24",
  "dueDate": "2026-12-08",
  "returnDate": null,
  "status": "BORROWING",
  "fineAmount": 0
}
```

### ReturnRecord

```json
{
  "id": "RR001",
  "ticketId": "TK001",
  "readerId": "RD001",
  "returnDate": "2026-12-08",
  "overdueDays": 0,
  "fineAmount": 0,
  "policyName": "Sinh viên thường (5.000đ/ngày)"
}
```

### Admin

```json
{
  "id": "AD001",
  "fullName": "Thủ thư",
  "phone": "0900000000",
  "username": "admin",
  "passwordHash": "..."
}
```

## 6. API và định dạng response

Mọi API trả về cùng cấu trúc:

```json
{
  "success": true,
  "message": "Thành công",
  "data": {}
}
```

Khi lỗi:

```json
{
  "success": false,
  "message": "Nội dung lỗi",
  "data": null
}
```

### Xác thực

```http
POST /api/auth/login
Content-Type: application/json
```

```json
{
  "username": "admin",
  "password": "admin"
}
```

### Sách

| Method | Endpoint | Mục đích |
| --- | --- | --- |
| `GET` | `/api/books` | Lấy toàn bộ sách |
| `GET` | `/api/books?keyword=java` | Tìm theo mã hoặc tên |
| `GET` | `/api/books/{id}` | Xem chi tiết sách |
| `GET` | `/api/books/{id}/availability?quantity=2` | Kiểm tra tồn kho |
| `POST` | `/api/books` | Thêm sách |
| `PUT` | `/api/books/{id}` | Cập nhật sách |
| `DELETE` | `/api/books/{id}` | Xóa sách |

Body thêm/cập nhật sách:

```json
{
  "title": "Tên sách",
  "author": "Tác giả",
  "category": "Thể loại",
  "availableQuantity": 5,
  "price": 120000
}
```

### Bạn đọc

| Method | Endpoint | Mục đích |
| --- | --- | --- |
| `GET` | `/api/readers` | Lấy toàn bộ bạn đọc |
| `GET` | `/api/readers?keyword=RD001` | Tìm theo mã, tên hoặc số điện thoại |
| `GET` | `/api/readers/{id}` | Xem chi tiết bạn đọc |
| `POST` | `/api/readers` | Thêm bạn đọc |
| `PUT` | `/api/readers/{id}` | Cập nhật bạn đọc |
| `DELETE` | `/api/readers/{id}` | Xóa bạn đọc |

Body thêm/cập nhật bạn đọc:

```json
{
  "fullName": "Nguyễn Văn A",
  "phone": "0901234567",
  "type": "STUDENT"
}
```

### Mượn sách

```http
POST /api/borrow
Content-Type: application/json
```

```json
{
  "readerId": "RD001",
  "items": [
    { "bookId": "BK001", "quantity": 1 },
    { "bookId": "BK002", "quantity": 2 }
  ],
  "borrowDate": "2026-11-24",
  "dueDate": "2026-12-08"
}
```

`borrowDate` và `dueDate` có thể bỏ trống; backend sẽ dùng ngày hiện tại và hạn trả mặc định 14 ngày.

### Phiếu mượn

| Method | Endpoint | Mục đích |
| --- | --- | --- |
| `GET` | `/api/tickets` | Lấy toàn bộ phiếu |
| `GET` | `/api/tickets?status=BORROWING` | Lọc phiếu đang mượn |
| `GET` | `/api/tickets?status=OVERDUE` | Lọc phiếu quá hạn |
| `GET` | `/api/tickets?status=RETURNED` | Lọc phiếu đã trả |
| `GET` | `/api/tickets?readerId=RD001` | Lọc theo bạn đọc |
| `GET` | `/api/tickets/{id}` | Xem chi tiết phiếu |

Trạng thái hợp lệ: `BORROWING`, `OVERDUE`, `RETURNED`.

### Trả sách

```http
POST /api/return/TK001
Content-Type: application/json
```

```json
{
  "returnDate": "2026-12-08"
}
```

Nếu bỏ body hoặc bỏ `returnDate`, backend sử dụng ngày hiện tại.

```http
GET /api/return-records
```

### Thống kê

```http
GET /api/stats
```

API trả về số phiếu quá hạn, tổng tiền phạt, top sách được mượn và sách có tồn kho từ 2 cuốn trở xuống. Dashboard frontend còn tổng hợp thêm dữ liệu từ `/api/books`, `/api/readers` và `/api/tickets` để hiển thị các biểu đồ và bộ lọc thời gian.

## 7. Mã lỗi HTTP

| Mã | Ý nghĩa |
| ---: | --- |
| `200` | Xử lý thành công |
| `201` | Tạo mới thành công |
| `400` | JSON, ngày hoặc dữ liệu đầu vào không hợp lệ |
| `401` | Đăng nhập thất bại |
| `404` | Không tìm thấy tài nguyên hoặc endpoint |
| `409` | Vi phạm nghiệp vụ: hết sách, vượt hạn mức, phiếu đã trả |
| `405` | Sai HTTP method |
| `415` | Body không phải JSON |
| `500` | Lỗi máy chủ hoặc lỗi đọc/ghi dữ liệu |

## 8. Cài đặt và chạy

### Yêu cầu

- Java 21 trở lên.
- Node.js 20 trở lên.
- Maven Wrapper có sẵn trong `backend`.

### Backend

Windows:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

macOS/Linux:

```bash
cd backend
./mvnw spring-boot:run
```

Backend chạy tại `http://localhost:8080`.

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend chạy tại `http://localhost:5173`.

Mở `http://localhost:5173` sau khi cả hai service đã khởi động.

## 9. Cấu hình

File cấu hình: `backend/src/main/resources/application.properties`.

```properties
spring.application.name=library-management
server.port=8080
library.data-dir=data
library.cors.allowed-origin=http://localhost:5173
```

- `server.port`: cổng backend.
- `library.data-dir`: thư mục JSON, tính tương đối từ thư mục `backend`.
- `library.cors.allowed-origin`: địa chỉ frontend được phép gọi API.

Tài khoản demo được lưu trong `backend/data/admins.json`:

```text
Tên đăng nhập: admin
Mật khẩu: admin
```

Không sửa các file JSON bằng nhiều tiến trình cùng lúc. Đây là dữ liệu mẫu của ứng dụng, không phải cơ chế lưu trữ production.

## 10. Kiểm thử và build

Kiểm thử backend:

```powershell
cd backend
.\mvnw.cmd test
```

Build backend:

```powershell
.\mvnw.cmd clean package
```

Build frontend:

```bash
cd frontend
npm run build
npm run preview
```

## 11. Cấu trúc thư mục

```text
backend/
├── data/                         # admins, books, readers, tickets, return_records
├── src/main/java/com/nhom04/library/
│   ├── handler/                  # REST controller và global exception handler
│   ├── main/                     # Spring Boot application và CORS
│   ├── model/                    # Entity/model và enum
│   ├── policy/                   # FinePolicy theo loại bạn đọc
│   ├── repository/               # Repository JSON và khóa đọc/ghi
│   ├── service/                  # Nghiệp vụ ứng dụng
│   ├── exception/                # Exception nghiệp vụ
│   └── utils/                    # ApiResponse, Gson, validator, ID
└── src/test/                     # Unit test và MockMvc test

frontend/
├── src/
│   ├── api/                      # API client và request helper
│   ├── components/               # Layout, sidebar, modal, toast, pagination
│   ├── pages/                    # Login, Books, Readers, Borrow, Return, Tickets, Stats
│   ├── utils/                    # Hàm tìm kiếm và định dạng dữ liệu phiếu
│   ├── App.jsx                   # Route ứng dụng
│   ├── main.jsx                  # Điểm mount React
│   └── styles.css                # CSS dùng chung
├── index.html
├── package.json
└── vite.config.js
```

## 12. Giới hạn hiện tại

- Dữ liệu lưu bằng JSON, phù hợp cho đồ án/demo và dữ liệu nhỏ.
- Frontend lưu trạng thái đăng nhập trong `sessionStorage`.
- Chưa có phân quyền nhiều vai trò.
- Chưa có cơ chế đặt chỗ, lưu phiếu nháp hoặc quét mã vạch.
- Chưa có phân trang ở backend; các màn hình lọc và phân trang phần lớn xử lý ở frontend.
- Không nên dùng dữ liệu mẫu hoặc cơ chế xác thực hiện tại cho môi trường production nếu chưa bổ sung database, session/token phía server, giao dịch ghi dữ liệu và phân quyền.
