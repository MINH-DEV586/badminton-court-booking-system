# Xác thực và phân quyền

Backend dùng Spring Security Resource Server để xác minh JWT HS256. Mật khẩu được băm BCrypt với cost 12. API không dùng session, HTTP Basic hay cookie để xác thực.

## Cấu hình

Giữ cấu hình MySQL trong `src/main/resources/application.properties` ở máy của bạn. File này đã được Git bỏ qua. Không đưa mật khẩu database hoặc khóa JWT vào Git.

Khóa ký bắt buộc là Base64 của ít nhất 32 byte ngẫu nhiên. Có thể tạo và đặt biến môi trường cho terminal hiện tại bằng PowerShell:

```powershell
$jwtKeyBytes = New-Object byte[] 32
$jwtRandom = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$jwtRandom.GetBytes($jwtKeyBytes)
$jwtRandom.Dispose()
$env:APP_SECURITY_JWT_SECRET = [Convert]::ToBase64String($jwtKeyBytes)
```

Dùng một khóa cố định được lưu trong hệ thống quản lý secret khi triển khai. Đổi khóa sẽ làm access token cũ không còn hợp lệ. Ứng dụng từ chối khởi động nếu khóa thiếu hoặc quá ngắn.

| Thuộc tính | Biến môi trường | Mặc định |
| --- | --- | --- |
| `app.security.jwt-secret` | `APP_SECURITY_JWT_SECRET` | Bắt buộc cấu hình |
| `app.security.issuer` | `APP_SECURITY_ISSUER` | `badmintonhub` |
| `app.security.access-token-ttl` | `APP_SECURITY_ACCESS_TOKEN_TTL` | `15m` |
| `app.security.refresh-token-ttl` | `APP_SECURITY_REFRESH_TOKEN_TTL` | `7d` |
| `app.security.allowed-origins` | `APP_SECURITY_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:3000` |

Access token phải có thời hạn ngắn hơn refresh token. Cấu hình CORS chỉ chấp nhận các origin trong danh sách, không gửi cookie xác thực.

Nếu database đã có các bảng entity, chạy [security-schema.sql](src/main/resources/db/security-schema.sql) để thêm bảng `refresh_tokens`. Script không tự chạy và không sửa/xóa dữ liệu hiện có. Với database phát triển mới, có thể đặt `spring.jpa.hibernate.ddl-auto=update` trong cấu hình cục bộ để Hibernate tạo bảng từ entity. Khi triển khai, quản lý schema bằng migration.

## API

Tất cả request có body dùng `Content-Type: application/json`.

| Method | Endpoint | Nội dung / quyền |
| --- | --- | --- |
| POST | `/api/auth/register` | `email`, `password`, `fullName`, `phone` tùy chọn; công khai |
| POST | `/api/auth/login` | `email`, `password`; công khai |
| POST | `/api/auth/refresh` | `refreshToken`; công khai, cần refresh token hợp lệ |
| POST | `/api/auth/logout` | `refreshToken`; thu hồi refresh token, trả `204` |
| GET | `/api/auth/me` | Cần access token |
| * | `/api/owner/**` | `OWNER`, `ADMIN` |
| * | `/api/admin/**` | `ADMIN` |
| * | Các endpoint còn lại | Cần access token |

Đăng ký luôn tạo tài khoản `CUSTOMER`. Client không được tự chọn `OWNER` hoặc `ADMIN`; việc cấp các quyền này phải qua quy trình quản trị riêng. Tiền tố `/api/owner/**` và `/api/admin/**` là quy tắc bảo vệ cho các API tương ứng; chưa tạo API nghiệp vụ sân/quản trị trong phần này. Có thể dùng `@PreAuthorize` cho kiểm tra quyền ở cấp method.

Ví dụ đăng ký:

```json
{
  "email": "customer@example.com",
  "password": "your-password",
  "fullName": "Nguyen Van A",
  "phone": "0901234567"
}
```

Đăng ký trả `201`; đăng nhập và refresh trả `200`, cùng định dạng:

```json
{
  "accessToken": "<JWT>",
  "refreshToken": "<opaque-token>",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "user": {
    "id": "<UUID>",
    "email": "customer@example.com",
    "fullName": "Nguyen Van A",
    "phone": "0901234567",
    "role": "CUSTOMER"
  }
}
```

Gửi `Authorization: Bearer <accessToken>` khi gọi API được bảo vệ. Khi access token hết hạn, gọi `/api/auth/refresh` với body `{"refreshToken":"<refreshToken>"}` mà không gửi access token đã hết hạn. Lưu cả hai token mới sau mỗi lần refresh.

Refresh token có 256 bit ngẫu nhiên; database chỉ lưu SHA-256 của token. Refresh token hết hạn, đã dùng hoặc bị thu hồi trả `401`. Việc đổi token chạy trong transaction có khóa dòng để hai request đồng thời không cùng đổi thành công. Mỗi lần đổi tạo một refresh token mới có thời hạn 7 ngày tính từ lúc đổi. Mỗi lần đăng nhập tạo một phiên refresh độc lập.

Logout thu hồi refresh token được gửi lên; client cần xóa cả hai token. Access token đã phát hành vẫn có hiệu lực đến khi hết hạn, có độ lệch đồng hồ cho phép mặc định của Spring Security. Khóa hoặc xóa tài khoản chặn các request tiếp theo; thay đổi vai trò được đọc lại từ database trên mỗi request.

CSRF được tắt vì API chỉ nhận bearer token trong header và refresh token trong JSON, không xác thực tự động bằng cookie. Frontend nên giữ token trong bộ nhớ và dùng HTTPS khi triển khai. Các response chứa token có `Cache-Control: no-store`.

## Validation và lỗi

Email hợp lệ và tối đa 254 ký tự; email lưu dạng chữ thường. Mật khẩu đăng ký từ 8 đến 72 ký tự và không quá 72 byte UTF-8 để phù hợp với BCrypt. Họ tên bắt buộc, tối đa 120 ký tự; số điện thoại tối đa 30 ký tự. Refresh token phải đúng định dạng Base64 URL không padding gồm 43 ký tự.

Global Exception Handler trả lỗi JSON, không đưa mật khẩu, hash, token hoặc stack trace vào response:

```json
{
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "Invalid request",
  "errors": { "email": "must be a well-formed email address" }
}
```

Các mã chính: `VALIDATION_ERROR` / `INVALID_REQUEST` (`400`), `INVALID_CREDENTIALS` / `INVALID_REFRESH_TOKEN` / `UNAUTHORIZED` (`401`), `FORBIDDEN` (`403`), `EMAIL_ALREADY_EXISTS` / `CONFLICT` (`409`), `INTERNAL_ERROR` (`500`). Lỗi từ security filter cũng dùng cấu trúc `status`, `code`, `message`, `errors`.

## Kiểm thử

```powershell
mvn test
```

Test profile dùng H2 với chế độ tương thích MySQL và khóa JWT chỉ dành cho test. Không cần MySQL hoặc secret thật để chạy test; không ghi dữ liệu vào database phát triển. Test bao gồm xác minh JWT, BCrypt, ba vai trò, đăng ký/đăng nhập, validation, xoay/thu hồi refresh token và tranh chấp refresh đồng thời. Kiểm thử H2 không thay thế kiểm thử triển khai trên MySQL.

Tham khảo: [Spring Security JWT Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html).
