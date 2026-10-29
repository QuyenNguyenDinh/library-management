# TÀI LIỆU QUY ƯỚC DỮ LIỆU, API CONTRACT & PHÂN CÔNG DỰ ÁN
**Hệ thống Quản lý Thư viện (Library Management System)**  
*Dành cho nhóm phát triển 5 thành viên - Đính kèm Báo cáo cuối kỳ*

---

## 1. DANH MỤC TÍNH NĂNG & THỨ TỰ ƯU TIÊN

> **Nguyên tắc triển khai:** Làm xong toàn bộ các mục **[Đề]** trước, sau đó triển khai các mục **[Nên]** và **[Thêm]**.

| Nhóm | Chức năng chi tiết | Phân loại |
| :--- | :--- | :---: |
| **Sách** | Hiển thị danh sách, tìm kiếm theo mã hoặc tên, kiểm tra còn đủ số lượng trong kho | **[Đề]** |
| | Thêm, sửa, xóa sách (chặn không cho xóa sách đang được mượn) | **[Nên]** |
| **Bạn đọc** | Lưu mã, họ tên, số điện thoại, loại bạn đọc; mỗi loại có mức mượn tối đa riêng | **[Đề]** |
| | Thêm, sửa, xóa, tìm bạn đọc (chặn không cho xóa người đang mượn sách) | **[Nên]** |
| **Mượn sách** | Kiểm tra 5 điều kiện mượn, tạo phiếu mượn, giảm tồn kho, trạng thái "Đang mượn"; lỗi báo cụ thể | **[Đề]** |
| | Một phiếu mượn nhiều sách (`BorrowTicketDetail`); cho phép sửa ngày mượn để demo | **[Nên]** |
| **Trả sách** | Kiểm tra phiếu, tăng lại tồn kho, tính phạt qua `FinePolicy`, cập nhật trạng thái "Đã trả" | **[Đề]** |
| | Cho phép nhập ngày trả thực tế để demo quá hạn; lưu bản ghi `ReturnRecord` | **[Nên]** |
| **Theo dõi phiếu** | Danh sách phiếu, lọc theo trạng thái, số ngày đã mượn, nhãn "Quá hạn", xem chi tiết phiếu | **[Nên]** |
| **Thống kê** | Số phiếu quá hạn, tổng tiền phạt, top sách mượn nhiều nhất, sách sắp hết hàng | **[Thêm]** |
| **Đăng nhập thủ thư** | Tài khoản Admin bảo mật mật khẩu | **[Thêm]** |

---

## 2. BẢNG PHÂN CHIA NHIỆM VỤ 5 THÀNH VIÊN (CHÍNH THỨC)

| STT | Người phụ trách | Phần phụ trách cụ thể | Khối lượng |
| :---: | :--- | :--- | :---: |
| **1** | **Bạn (Trưởng nhóm)** | - Khung dự án (repo, cấu trúc package, bảng API, định dạng JSON).<br>- **Lõi Mượn / Trả / Phạt:** `BorrowTicket`, `BorrowTicketDetail`, `ReturnRecord`, `Borrowable`, `FinePolicy` + 3 class policy, `TicketRepository`, `BorrowService`, `BorrowHandler`.<br>- **Màn hình:** Mượn sách (`borrow.html`), Trả sách (`return.html`).<br>- Xử lý lỗi chung (Exceptions), review và merge Pull Request, ghép hệ thống, báo cáo tổng.<br>- Đăng nhập thủ thư (nếu làm thêm). | **~ 35%** |
| **2** | **Thành viên 2** | - **Backend Bạn đọc:** `User`, `Reader`, 3 lớp bạn đọc (`StudentReader`, `PriorityStudentReader`, `LecturerReader`), `ReaderRepository`, `ReaderService`, `ReaderHandler`.<br>- **Màn hình:** Giao diện Bạn đọc (`readers.html`).<br>- **Thiết kế:** Vẽ sơ đồ Class Diagram (UML) hoàn chỉnh cho báo cáo. | **~ 16%** |
| **3** | **Thành viên 3** | - **Backend Sách:** `Book`, `BookRepository`, `BookService`, `BookHandler`.<br>- **Hạ tầng Utils:** Đọc/ghi JSON, tự động sinh mã ID (`RD...`, `BK...`), validate dữ liệu.<br>- **Dữ liệu mẫu:** Dữ liệu chuẩn ban đầu trong `books.json`, `readers.json`, `tickets.json`.<br>- **Màn hình:** Giao diện Quản lý Sách (`books.html`). | **~ 16%** |
| **4** | **Thành viên 4** | - **Backend Theo dõi & Thống kê:** Xử lý lọc phiếu mượn, tính số ngày đã mượn / quá hạn, `StatsService`, API `/api/tickets`, API `/api/stats`.<br>- **Màn hình:** Giao diện Danh sách phiếu mượn và Giao diện Thống kê báo cáo. | **~ 16%** |
| **5** | **Thành viên 5** | - **QA & Tài liệu:** Viết Unit Test cho mọi chức năng, xây dựng bảng mô tả tình huống test (Test Cases).<br>- Slide báo cáo PowerPoint/Canva, hoàn thiện README dự án, kịch bản thuyết trình và quay video demo. | **~ 16%** |

---

## 3. DANH SÁCH API CONTRACT (RESTFUL SPECIFICATION)

| Method | Endpoint | Nhóm chức năng | Mô tả chi tiết |
| :--- | :--- | :---: | :--- |
| `GET` | `/api/books?keyword=...` | Sách | Hiển thị danh sách hoặc tìm kiếm sách theo tên/mã |
| `POST` | `/api/books` | Sách | Thêm đầu sách mới |
| `PUT` | `/api/books/{id}` | Sách | Sửa thông tin sách |
| `DELETE` | `/api/books/{id}` | Sách | Xóa sách (chỉ xóa khi sách không có ai đang mượn) |
| `GET` | `/api/readers` | Bạn đọc | Danh sách bạn đọc |
| `POST` | `/api/readers` | Bạn đọc | Thêm bạn đọc |
| `PUT` | `/api/readers/{id}` | Bạn đọc | Sửa bạn đọc |
| `DELETE` | `/api/readers/{id}` | Bạn đọc | Xóa bạn đọc (chỉ xóa khi bạn đọc không nợ sách) |
| `POST` | `/api/borrow` | **Mượn sách** | Tạo phiếu mượn, kiểm tra 5 điều kiện, trừ tồn kho |
| `POST` | `/api/return/{ticketId}` | **Trả sách** | Hoàn lại kho, tính phạt theo `FinePolicy`, cập nhật "Đã trả" |
| `GET` | `/api/tickets?status=...` | Theo dõi phiếu | Danh sách phiếu, lọc trạng thái, nhãn quá hạn |
| `GET` | `/api/tickets/{id}` | Theo dõi phiếu | Xem chi tiết phiếu mượn |
| `GET` | `/api/stats` | Thống kê | Thống kê phiếu quá hạn, tổng phạt, top sách, sách sắp hết |

---

## 4. QUY TẮC LÀM VIỆC VỚI GIT

1. **Cấm tuyệt đối push thẳng vào nhánh `main`**.
2. **Quy trình chuẩn cho từng thành viên:**
   * Lấy code mới: `git checkout main && git pull origin main`
   * Tạo nhánh: `git checkout -b feature/<tên-nhiệm-vụ>` (VD: `feature/book-service`, `feature/reader-service`...)
   * Đẩy code và mở Pull Request (PR) trên GitHub.
3. **Trưởng nhóm** là người duy nhất duyệt và merge PR sau khi đã chạy thử kiểm tra.
