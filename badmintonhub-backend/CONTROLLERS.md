# API nghiệp vụ

Tất cả API dưới đây yêu cầu `Authorization: Bearer <access-token>`. Request có body dùng `Content-Type: application/json`. Đăng ký/đăng nhập, refresh và logout tiếp tục dùng [AuthController](src/main/java/com/badmintonhub/backend/auth/AuthController.java), được mô tả trong [SECURITY.md](SECURITY.md).

Controller tiếp nhận và kiểm tra request, chuyển cho service xử lý và trả DTO. Các kiểm tra quyền sở hữu đối tượng nằm ở service hiện có. Endpoint `/api/owner/**` yêu cầu `OWNER` hoặc `ADMIN`; `/api/admin/**` yêu cầu `ADMIN`.

## Quy ước

- Tạo mới trả `201` với header `Location` trỏ tới tài nguyên mới.
- Đọc, cập nhật và các action trả DTO dùng `200`; xóa tài nguyên trả `204`.
- UUID, JSON hoặc request không hợp lệ trả `400`; thiếu xác thực trả `401`; không đủ quyền trả `403`. Các lỗi nghiệp vụ khác do service và Global Exception Handler xác định.
- Endpoint phân trang nhận `page` bắt đầu từ `0` (mặc định `0`) và `size` từ `1` đến `100` (mặc định `20`). Thứ tự sắp xếp được xác định ở controller.
- Response phân trang dùng cấu trúc ổn định `content` và `page` từ Spring Data `PagedModel`, không trả entity JPA.
- Các action thay đổi trạng thái dùng POST/PATCH, không dùng GET.

Ví dụ cấu trúc phân trang:

```json
{
  "content": [],
  "page": { "size": 20, "number": 0, "totalElements": 0, "totalPages": 0 }
}
```

## Cụm sân

[VenueController](src/main/java/com/badmintonhub/backend/controller/VenueController.java)

| Method | Endpoint | Dữ liệu / mục đích |
| --- | --- | --- |
| GET | `/api/venues` | Danh sách cụm sân, phân trang |
| GET | `/api/venues/{id}` | Chi tiết cụm sân |
| GET | `/api/owner/venues` | Cụm sân của tài khoản trong JWT, phân trang |
| GET | `/api/admin/users/{ownerId}/venues` | Cụm sân của chủ sân được chỉ định, phân trang |
| POST | `/api/owner/venues` | Tạo bằng `VenueRequest` |
| PUT | `/api/owner/venues/{id}` | Cập nhật bằng `VenueRequest` |
| DELETE | `/api/owner/venues/{id}` | Xóa cụm sân |

## Sân

[CourtController](src/main/java/com/badmintonhub/backend/controller/CourtController.java)

| Method | Endpoint | Dữ liệu / mục đích |
| --- | --- | --- |
| GET | `/api/venues/{venueId}/courts` | Phân trang; `activeOnly=true` mặc định, `false` để lấy cả sân không hoạt động |
| GET | `/api/courts/{id}` | Chi tiết sân qua cache hiện có |
| POST | `/api/owner/courts` | Tạo bằng `CreateCourtRequest` |
| PUT | `/api/owner/courts/{id}` | Cập nhật bằng `UpdateCourtRequest` |
| DELETE | `/api/owner/courts/{id}` | Xóa sân |

Cache được quản lý trong service theo [REDIS.md](REDIS.md).

## Quy tắc giá

[PricingRuleController](src/main/java/com/badmintonhub/backend/controller/PricingRuleController.java)

| Method | Endpoint | Dữ liệu / mục đích |
| --- | --- | --- |
| GET | `/api/pricing-rules/{id}` | Chi tiết quy tắc giá |
| GET | `/api/courts/{courtId}/pricing-rules` | Danh sách; có thể lọc bằng `dayOfWeek=1..7` theo ISO (thứ Hai đến Chủ nhật) |
| POST | `/api/owner/pricing-rules` | Tạo bằng `PricingRuleRequest` |
| DELETE | `/api/owner/pricing-rules/{id}` | Xóa quy tắc |

Danh sách quy tắc giá dùng kiểu `List` theo hợp đồng của service; không nhận tham số phân trang. Service hiện chưa có thao tác sửa quy tắc giá.

## Khóa lịch sân

[CourtBlockController](src/main/java/com/badmintonhub/backend/controller/CourtBlockController.java)

| Method | Endpoint | Dữ liệu / mục đích |
| --- | --- | --- |
| GET | `/api/court-blocks/{id}` | Chi tiết lịch khóa |
| GET | `/api/courts/{courtId}/blocks` | Lịch khóa chưa hủy, phân trang theo giờ bắt đầu tăng dần |
| POST | `/api/owner/court-blocks` | Tạo bằng `CreateCourtBlockRequest` |
| POST | `/api/owner/court-blocks/{id}/cancel` | Hủy mềm lịch khóa, trả DTO đã cập nhật |

## Đặt sân

[BookingController](src/main/java/com/badmintonhub/backend/controller/BookingController.java)

| Method | Endpoint | Dữ liệu / mục đích |
| --- | --- | --- |
| POST | `/api/bookings` | Tạo bằng `CreateBookingRequest`; khách đặt, giá và trạng thái do service xác định |
| GET | `/api/bookings/me` | Đặt sân của tài khoản trong JWT, phân trang |
| GET | `/api/bookings/{id}` | Chi tiết; service kiểm tra khách đặt/chủ sân/admin |
| POST | `/api/bookings/{id}/cancel` | Hủy theo quyền và điều kiện của service |
| GET | `/api/owner/courts/{courtId}/bookings` | Danh sách đặt sân theo sân, phân trang |
| GET | `/api/admin/users/{customerId}/bookings` | Đặt sân của khách được chỉ định, phân trang |
| POST | `/api/admin/bookings/expire-pending` | Chạy xử lý hết hạn giữ chỗ; trả `{"expiredCount": ...}` |

## Thanh toán

[PaymentController](src/main/java/com/badmintonhub/backend/controller/PaymentController.java)

| Method | Endpoint | Dữ liệu / mục đích |
| --- | --- | --- |
| POST | `/api/payments` | Tạo bằng `CreatePaymentRequest`; số tiền lấy từ đặt sân |
| GET | `/api/payments/{id}` | Chi tiết theo quyền của service |
| GET | `/api/bookings/{bookingId}/payments` | Thanh toán của đặt sân, phân trang |
| POST | `/api/admin/payments/{id}/complete` | Đối soát nội bộ; body `{"providerPaymentId":"reference"}`, bắt buộc và tối đa 128 ký tự |
| POST | `/api/admin/payments/{id}/fail` | Đánh dấu thất bại khi đối soát nội bộ |
| POST | `/api/owner/payments/{id}/refund` | Chuyển trạng thái hoàn tiền theo service |

Các action complete/fail chỉ mở cho ADMIN, không phải webhook công khai. Service thanh toán hiện chỉ quản lý trạng thái trong database, chưa gọi cổng thanh toán hoặc thực hiện chuyển/hoàn tiền thật. Tích hợp callback từ nhà cung cấp cần có xác minh chữ ký, đối chiếu giao dịch và kiểm tra chuyển trạng thái tại tầng nghiệp vụ.

## Hồ sơ và quản trị người dùng

[UserController](src/main/java/com/badmintonhub/backend/controller/UserController.java)

| Method | Endpoint | Dữ liệu / mục đích |
| --- | --- | --- |
| GET | `/api/users/me` | Hồ sơ của tài khoản trong JWT |
| PUT | `/api/users/me` | Cập nhật bằng `UpdateProfileRequest`; không nhận quyền từ client |
| GET | `/api/admin/users` | Danh sách người dùng, phân trang |
| GET | `/api/admin/users/{id}` | Hồ sơ theo khả năng đọc của service |
| PATCH | `/api/admin/users/{id}/role` | Body `{"role":"CUSTOMER"}`, `OWNER` hoặc `ADMIN`; bắt buộc đúng enum |
| PATCH | `/api/admin/users/{id}/active` | Body `{"active":false}` hoặc `true`; không được bỏ trường này |

Response hồ sơ và quản trị người dùng có `Cache-Control: no-store`, không chứa password hash.

## Kiểm thử

```powershell
mvn test
```

Các test controller dùng MockMvc với filter chain thật, method security và mock service: kiểm tra binding, validation, phân trang, danh tính từ JWT, phân quyền và HTTP status/Location. Test controller không thay thế kiểm thử nghiệp vụ của từng service hoặc tích hợp cổng thanh toán. Test Redis thật tiếp tục bật riêng theo [REDIS.md](REDIS.md).
