# BadmintonHub frontend

Giao diện React tiếng Việt dựa trên hai mẫu đăng nhập và chi tiết cụm sân. Frontend gọi API Spring Boot bằng JSON và Bearer token.

## Chạy với backend hiện có

Yêu cầu Node.js 22.12 trở lên và backend đang chạy ở cổng 8080.

```powershell
cd D:\BadmintonHub\badmintonhub-frontend
npm ci
npm run dev
```

Mở http://127.0.0.1:5173/. Vite chuyển tiếp `/api` tới `http://localhost:8080`. Để đổi máy chủ, tạo `.env.development.local` và đặt `VITE_API_TARGET`. Nếu frontend được triển khai riêng, đặt `VITE_API_BASE_URL` thành địa chỉ API có hậu tố `/api` khi build, đồng thời cấu hình `app.security.allowed-origins` ở backend cho tên miền frontend.

## Chạy bản trải nghiệm có dữ liệu mẫu

Profile `frontend-preview` khởi tạo H2 trong bộ nhớ và dùng cache trong tiến trình. Mỗi lần khởi động lại backend sẽ tạo lại dữ liệu. Profile này cần được bật tường minh.

Mở terminal backend, chọn JDK của dự án rồi chạy.

```powershell
cd D:\BadmintonHub\badmintonhub-backend
$previewSecretBytes = New-Object byte[] 32
[System.Security.Cryptography.RandomNumberGenerator]::Fill($previewSecretBytes)
$env:APP_SECURITY_JWT_SECRET = [Convert]::ToBase64String($previewSecretBytes)
$env:BADMINTON_PREVIEW_PASSWORD = 'Preview123!'
mvn -Pfrontend-preview spring-boot:run '-Dspring-boot.run.profiles=frontend-preview'
```

Tạo `.env.development.local` trong thư mục frontend.

```dotenv
VITE_API_TARGET=http://localhost:8080
VITE_PREVIEW_MODE=true
VITE_PREVIEW_PASSWORD=Preview123!
```

Khởi động `npm run dev`. Các nút Người chơi, Chủ sân và Admin trên trang đăng nhập điền tài khoản mẫu. Nhấn Đăng nhập để gửi yêu cầu xác thực tới backend.

| Vai trò | Email |
| --- | --- |
| CUSTOMER | minhanh@badmintonhub.test |
| OWNER | chusan@badmintonhub.test |
| ADMIN | admin@badmintonhub.test |

Các tài khoản dùng mật khẩu lấy từ `BADMINTON_PREVIEW_PASSWORD`. Các biến `VITE_*` được trình duyệt đọc, vì vậy chỉ đưa mật khẩu dữ liệu trải nghiệm vào biến này. Nút tài khoản mẫu chỉ xuất hiện khi chạy development và bật `VITE_PREVIEW_MODE`.

## Các luồng đã nối API

- Đăng ký, đăng nhập, đăng xuất, tự làm mới access token khi API trả 401, sửa hồ sơ.
- Tìm cụm sân, xem sân hoạt động, chọn ngày giờ theo múi giờ cụm sân, xem giá ước tính và lịch khóa, tạo giữ chỗ, xem và hủy lịch đặt.
- Tạo yêu cầu thanh toán, xem trạng thái thanh toán.
- Chủ sân đăng nhập vào khu quản lý với tổng số cụm sân, số sân, lượt đặt và số khách theo ngày. Lịch trống tính từ các lịch đặt hiệu lực và lịch khóa của các sân thuộc chủ sân, tự cập nhật mỗi phút. Chủ sân quản lý giá, lịch khóa, các sân đã đăng và ghi nhận hoàn tiền đã đối soát trong chi tiết lịch đặt.
- Admin quản lý vai trò, khóa/mở tài khoản, tra cứu thanh toán và xử lý giữ chỗ hết hạn.

Giá cuối cùng và xung đột lịch được backend xác nhận khi tạo đơn. Giao diện ghi rõ giá ước tính trước bước đó. Backend hiện cung cấp thao tác ghi nhận và đối soát thanh toán nội bộ. Việc chuyển tiền cần tích hợp nhà cung cấp thanh toán.

Access token và refresh token được giữ trong bộ nhớ của tab. Tải lại trang yêu cầu đăng nhập lại. Bộ lọc ở bảng phân trang có ghi rõ phạm vi trang đang xem.

Lịch trống của chủ sân xét ngày 00:00–24:00 theo múi giờ cụm sân và bước 15 phút. Thời gian đã qua, sân tạm ngưng, lịch khóa và giữ chỗ còn hiệu lực được loại khỏi giờ trống. Dự án hiện chưa có cấu hình giờ mở cửa theo từng cụm sân.

## Kiểm tra và build

```powershell
npm test
npm run build
# Cần frontend và backend trải nghiệm đang chạy, máy có Microsoft Edge.
npm run test:e2e
```

Unit test kiểm tra xử lý API, refresh token đồng thời, đăng xuất, múi giờ và tính giá. Playwright kiểm tra đăng nhập trên desktop/mobile, đặt và hủy lịch qua API, đăng ký/sửa hồ sơ, thao tác chủ sân và quản trị.

Thư mục `dist` là kết quả build. Khi triển khai, cấu hình reverse proxy cho `/api` hoặc đặt `VITE_API_BASE_URL` trước khi build.
