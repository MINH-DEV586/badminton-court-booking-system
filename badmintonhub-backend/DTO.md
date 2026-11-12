# DTO và Bean Validation

DTO nghiệp vụ nằm trong [dto/request](src/main/java/com/badmintonhub/backend/dto/request) và [dto/response](src/main/java/com/badmintonhub/backend/dto/response). Các DTO xác thực hiện có được giữ trong [AuthDtos](src/main/java/com/badmintonhub/backend/auth/AuthDtos.java), gồm đăng ký, đăng nhập, refresh token và thông tin tài khoản.

## Request

| DTO | Dữ liệu và validation |
| --- | --- |
| `UpdateProfileRequest` | Họ tên bắt buộc, tối đa 120 ký tự; điện thoại tùy chọn, tối đa 30 ký tự. Không nhận quyền, trạng thái hoặc password hash. |
| `VenueRequest` | Tên tối đa 150 ký tự, địa chỉ tối đa 255 ký tự, múi giờ Java hợp lệ tối đa 64 ký tự; cả ba bắt buộc. Dùng cho tạo hoặc thay thế thông tin cụm sân. |
| `CreateCourtRequest` | `venueId`, tên sân tối đa 100 ký tự, giá giờ VND bắt buộc và không âm. |
| `UpdateCourtRequest` | Tên sân, giá không âm và `active` đều bắt buộc; không chuyển sân sang cụm sân khác qua DTO này. |
| `PricingRuleRequest` | `courtId`, thứ ISO từ 1 (thứ Hai) đến 7 (Chủ nhật), giờ bắt đầu/kết thúc, giá không âm và ngày hiệu lực bắt buộc. Giờ kết thúc phải sau giờ bắt đầu trong cùng ngày; ngày kết thúc tùy chọn và phải sau ngày bắt đầu. |
| `CreateBookingRequest` | `courtId`, giờ bắt đầu trong tương lai và giờ kết thúc sau giờ bắt đầu. |
| `CreatePaymentRequest` | `bookingId` và nhà cung cấp thanh toán bắt buộc, tên nhà cung cấp tối đa 32 ký tự. |
| `CreateCourtBlockRequest` | `courtId`, thời điểm bắt đầu/kết thúc, lý do tối đa 255 ký tự đều bắt buộc. Kết thúc phải trong tương lai và sau bắt đầu; hỗ trợ khóa sân ngay hoặc lên lịch trước. |

Các trường số tiền và `active` bắt buộc dùng kiểu wrapper để phân biệt dữ liệu bị thiếu với `0` hoặc `false`. Giá VND là số nguyên. `validUntil` của quy tắc giá là mốc kết thúc loại trừ; `null` nghĩa là không có ngày hết hiệu lực.

Các request thay thế thông tin yêu cầu đầy đủ trường bắt buộc, không mang ngữ nghĩa cập nhật từng phần của PATCH. Với cập nhật hồ sơ, `phone = null` có thể dùng để xóa số điện thoại khi xây dựng service.

## Response

Các response `VenueResponse`, `CourtResponse`, `PricingRuleResponse`, `BookingResponse`, `PaymentResponse`, `CourtBlockResponse` cung cấp `from(entity)` để chuyển đổi dữ liệu. Quan hệ được biểu diễn bằng ID như `ownerId`, `courtId`, `customerId` hoặc `createdById`, không trả entity lồng nhau. Không trả password hash hoặc refresh token hash.

Thực hiện mapping trong transaction của service khi các quan hệ cần thiết còn có thể truy cập. Mốc đặt sân/thanh toán dùng `Instant`; khung giá dùng `LocalDate` / `LocalTime` theo múi giờ của cụm sân.

## Sử dụng trong controller/service

Áp dụng `@Valid` cùng `@RequestBody` khi xây dựng các endpoint nghiệp vụ để kích hoạt validation. [GlobalExceptionHandler](src/main/java/com/badmintonhub/backend/exception/GlobalExceptionHandler.java) đã xử lý lỗi validation thành HTTP `400` và JSON có `errors` theo tên trường. Các kiểm tra chéo thời gian dùng tên lỗi `timeRangeValid` / `dateRangeValid`; các thuộc tính hỗ trợ này không xuất hiện trong JSON nhờ `@JsonIgnore`.

DTO kiểm tra cấu trúc dữ liệu đầu vào. Service cần kiểm tra đối tượng tồn tại, quyền sở hữu sân, lịch trống, quy tắc chuyển trạng thái và tính giá thực tế. Chủ cụm sân, khách đặt sân và người tạo lịch khóa lấy từ tài khoản đã xác thực. Tổng tiền, trạng thái đặt sân/thanh toán, hạn giữ chỗ và mã giao dịch thanh toán do backend hoặc nhà cung cấp thanh toán xác định.

Phần này cung cấp hợp đồng request/response để dùng khi xây dựng service/controller nghiệp vụ; các API xác thực đang dùng DTO hiện có.

## Kiểm thử

```powershell
mvn test
```

Test DTO kiểm tra dữ liệu thiếu, vượt độ dài, số âm, múi giờ không hợp lệ, quan hệ trước/sau của thời gian, mapping và JSON response không chứa entity lồng nhau. Đồng hồ validation trong test được cố định để kiểm tra `@Future` ổn định theo thời gian.
