# Kế hoạch triển khai: Tích hợp Lợi nhuận (Profit) vào Admin Dashboard

Tài liệu này lưu trữ các bước thực hiện để nâng cấp Dashboard Admin với tính năng theo dõi Thực nhận (Lợi nhuận).

## 1. Thay đổi Database & Entity
- **File:** `com.smarthome.entity.Product`
- **Thay đổi:** Thêm trường `costPrice` (Giá nhập). Hibernate sẽ tự động cập nhật bảng `products` trong MySQL khi khởi động lại.
- **Dữ liệu mẫu:** Cập nhật `DataInitializer` để gán giá vốn mặc định (~70-80% giá bán) cho các sản phẩm hiện có.

## 2. Logic tính toán (Backend)
- **OrderRepository:** Thêm query tính Tổng lợi nhuận (`sumTotalProfit`) và Lợi nhuận theo tháng (`getMonthlyProfit`).
- **OrderItemRepository:** Cập nhật query Top sản phẩm để tính thêm lợi nhuận trên từng món.
- **DashboardController:** Cập nhật API `/stats` để trả về các con số lợi nhuận mới.

## 3. Giao diện (Frontend)
- **File:** `admin/index.html`
- **Card:** Thêm thẻ "Tổng thực nhận" màu xanh lá.
- **Biểu đồ:** Vẽ thêm đường Lợi nhuận màu cam chạy song song với Doanh thu.
- **Bảng:** Thêm cột "Lợi nhuận" vào danh sách Top sản phẩm bán chạy.

## 4. Xác nhận & Kiểm tra
- Chạy ứng dụng và kiểm tra bảng `products` trong DB.
- Truy cập Admin Dashboard để kiểm tra các con số hiển thị.
- Đảm bảo Lợi nhuận luôn thấp hơn hoặc bằng Doanh thu.

---
*Ghi chú: Lợi nhuận được tính bằng công thức: (Giá thực bán - Giá nhập) * Số lượng.*
