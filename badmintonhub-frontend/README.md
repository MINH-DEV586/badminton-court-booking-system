# BadmintonHub frontend

Giao diện React tiếng Việt gọi API Spring Boot bằng JSON và Bearer token. Dữ liệu sân, người dùng, giá, lịch đặt và thanh toán được lấy từ database qua backend. Database trống sẽ hiển thị trạng thái chưa có dữ liệu.

## Chạy backend với MySQL

Cấu hình trong `badmintonhub-backend/src/main/resources/application.properties` dùng MySQL. Có thể ghi đè thông qua các biến môi trường sau.

- `DB_URL` là JDBC URL của database hiện có.
- `DB_USERNAME` và `DB_PASSWORD` là thông tin đăng nhập MySQL.
- `APP_SECURITY_JWT_SECRET` là khóa JWT dạng Base64, tối thiểu 32 byte.
- `SPRING_CACHE_TYPE` chọn loại cache. Cấu hình local hiện dùng `none` để đọc trực tiếp database. Có thể chọn `redis` khi Redis đã chạy.

```powershell
cd D:\BadmintonHub\badmintonhub-backend
mvn spring-boot:run
```

Hibernate cập nhật cấu trúc bảng từ entity với `ddl-auto=update`. Dữ liệu nghiệp vụ được tạo khi người dùng thực hiện đăng ký, thêm sân, đặt lịch hoặc các thao tác tương ứng trên giao diện.

## Chạy frontend

Yêu cầu Node.js 22.12 trở lên và backend ở cổng 8080.

```powershell
cd D:\BadmintonHub\badmintonhub-frontend
npm ci
npm run dev
```

Mở http://127.0.0.1:5173/. Vite chuyển tiếp `/api` tới `http://localhost:8080`. Để đổi máy chủ, tạo `.env.development.local` và đặt `VITE_API_TARGET`. Khi triển khai riêng frontend, đặt `VITE_API_BASE_URL` thành địa chỉ API có hậu tố `/api` khi build, đồng thời cấu hình `app.security.allowed-origins` ở backend.

Đăng nhập bằng tài khoản có trong database hoặc đăng ký tài khoản của bạn. Khi đăng ký, chọn Khách hàng (CUSTOMER) để đặt sân hoặc Chủ sân (OWNER) để đăng sân và quản lý lịch đặt. Backend lưu vai trò đã chọn và cấp token tương ứng. Vai trò ADMIN do quy trình quản trị hiện có cấp. Yêu cầu đăng ký từ client cũ bỏ qua trường `role` tiếp tục mặc định là CUSTOMER.

## Các luồng đã nối API

- Đăng ký, đăng nhập, đăng xuất, làm mới access token và sửa hồ sơ.
- Tìm cụm sân, xem sân, chọn lịch theo múi giờ cụm sân, xem giá dự kiến, tạo giữ chỗ, xem và hủy lịch đặt.
- Tạo yêu cầu thanh toán và xem trạng thái thanh toán.
- Chủ sân quản lý các sân của mình, số sân, số khách và lượt đặt theo ngày, giá theo khung giờ, lịch khóa và ghi nhận hoàn tiền đã đối soát.
- Admin quản lý vai trò, truy cập tài khoản, đối soát thanh toán và xử lý giữ chỗ hết hạn.

Giá cuối cùng và xung đột lịch do backend xác nhận khi tạo đơn. Thanh toán hiện hỗ trợ ghi nhận và đối soát nội bộ. Access token và refresh token được giữ trong bộ nhớ tab, nên tải lại trang cần đăng nhập lại.

Lịch trống của chủ sân xét ngày 00:00–24:00 theo múi giờ cụm sân và bước 15 phút. Thời gian đã qua, sân tạm ngưng, lịch khóa và giữ chỗ còn hiệu lực được loại khỏi giờ trống. Dự án hiện chưa có cấu hình giờ mở cửa theo từng cụm sân.

## Kiểm tra và build

```powershell
npm test
npm run build
# Cần frontend, backend đang chạy và Microsoft Edge.
npm run test:e2e
```

Unit test dùng dữ liệu kiểm thử trong bộ nhớ. Kiểm thử trình duyệt kiểm tra đăng nhập và bảo vệ trang. Có thể đặt `E2E_EMAIL`, `E2E_PASSWORD` cho một tài khoản hiện có để chạy thêm kiểm tra đọc dữ liệu. Kiểm thử trình duyệt hiện chỉ thực hiện đăng nhập và đọc dữ liệu.

Thư mục `dist` là kết quả build. Khi triển khai, cấu hình reverse proxy cho `/api` hoặc đặt `VITE_API_BASE_URL` trước khi build.
