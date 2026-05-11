# SmartHome Backend

SmartHome API phục vụ hệ thống e-commerce đặt mua thiết bị thông minh. 

## 1. Flow Đặt Hàng (Checkout Flow)
Luồng thanh toán được chuyển hoàn toàn logic về phía Backend để đảm bảo bảo mật và tránh thất thoát doanh thu:
1. Client gửi **địa chỉ** -> Nhận phí Ship dự kiến.
2. Client gửi **Mã giảm giá** -> Trả về kết quả mã và số tiền được trừ.
3. Client nhấp Đặt hàng gửi thông tin -> Backend tự độc lập tính lại `Tiền Hàng (Cart) + Phí Ship - Mã Giảm Giá`. Sinh đơn hàng và gửi `OrderResponseDto` xuống Client.

## 2. Các API Chính Của Checkout
Tất cả endpoint dưới đây đều yêu cầu JWT Token hợp lệ (Bearer Token):
- **Tính cước:** `POST /api/v1/checkout/calculate-shipping` (Body chứa `address`)
- **Kiểm tra Voucher:** `POST /api/v1/checkout/apply-discount` (Body chứa `code`)
- **Đặt hàng:** `POST /api/v1/checkout` (Body `OrderRequestDto` gồm Tên, SDT, Địa chỉ, Mã giảm giá)

## 3. Cách Chạy Project (Run Project)
### A. Cấu Hình
Chỉnh sửa file `src/main/resources/application.properties` (Không Share):
- Import API Key của Google (`google.maps.api-key`)
- Database MySQL (`spring.datasource.password`, `username`)

### B. Biên Dịch & Chạy (IDE/Maven)
Chạy bằng IDE như IntelliJ, Eclipse (Run file `SmartHomeApplication.java`) hoặc:
```bash
./mvnw clean spring-boot:run
```
Giao diện tĩnh có thể truy cập tại `http://localhost:8080/checkout.html`
