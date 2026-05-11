# Kế hoạch tổng thể: Hệ thống tính Lợi nhuận Thực (Net Profit)

Mục tiêu: Tính toán chính xác số tiền thu về sau khi trừ tất cả chi phí (Giá vốn, Phí ship shop trả, Giảm giá Voucher, Giảm giá Hạng thành viên).

## 1. Database & Model
- **Entity `Order`**: Bổ sung trường `actualShippingCost` (BigDecimal).
- **Mục đích**: Lưu số tiền thực tế Shop phải trả cho đơn vị vận chuyển (ví dụ: GHTK).

## 2. Backend - Repository & Service
- **`OrderRepository`**: Cập nhật query tính lợi nhuận.
    - `Profit = SUM(OrderItem Profit) + Order.shippingFee - Order.actualShippingCost - Order.discountAmount`.
    - *Ghi chú: `discountAmount` trong hệ thống hiện tại đã bao gồm cả Voucher + Giảm giá hạng thành viên.*
- **`OrderService`**: Thêm logic cập nhật `actualShippingCost` khi Admin xử lý đơn hàng.

## 3. Frontend - Quản lý Đơn hàng
- **File:** `admin/orders.html`
- **Thay đổi**: 
    - Thêm ô nhập "Phí ship thực trả" vào Modal cập nhật đơn hàng.
    - Hiển thị Lợi nhuận dự kiến ngay trong chi tiết đơn hàng để Admin theo dõi.

## 4. Frontend - Dashboard Dashboard
- **File:** `admin/index.html`
- **Thay đổi**: Cập nhật biểu đồ và các thẻ thống kê để phản ánh con số "Thực nhận" chính xác nhất (đã trừ phí ship shop trả).

---
*Công thức chốt: Thực nhận = (Giá bán - Giá vốn) + (Ship khách trả - Ship shop trả) - Giảm giá.*
