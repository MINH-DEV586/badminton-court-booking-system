# Redis cache cho thông tin sân

Redis được dùng cho cache `courtDetails`. MySQL vẫn là nguồn dữ liệu chính; entity JPA và dữ liệu đặt sân/thanh toán không được đưa vào cache này.

## Khởi động Redis cục bộ

Nếu chưa có container, chạy khi Docker Desktop đang hoạt động:

```powershell
docker run --detach --name badmintonhub-redis --publish 127.0.0.1:6379:6379 redis:8-alpine
```

Nếu container đã tồn tại và đang dừng:

```powershell
docker start badmintonhub-redis
```

Kiểm tra kết nối, kết quả mong đợi là `PONG`:

```powershell
docker exec badmintonhub-redis redis-cli PING
```

## Cấu hình

[redis-cache.properties](src/main/resources/redis-cache.properties) cung cấp mặc định được [CacheConfig](src/main/java/com/badmintonhub/backend/config/CacheConfig.java) tự nạp. Không cần chuyển thông tin Redis vào profile test.

| Thuộc tính | Mặc định |
| --- | --- |
| `spring.cache.type` | `redis` |
| `spring.cache.redis.time-to-live` | `10m` |
| `spring.data.redis.host` | `localhost`, hoặc biến môi trường `REDIS_HOST` |
| `spring.data.redis.port` | `6379`, hoặc biến môi trường `REDIS_PORT` |
| `spring.data.redis.connect-timeout` | `2s` |
| `spring.data.redis.timeout` | `2s` |

Có thể ghi đè bằng cấu hình ứng dụng hoặc các biến môi trường chuẩn như `SPRING_DATA_REDIS_HOST`, `SPRING_DATA_REDIS_PORT`, `SPRING_CACHE_REDIS_TIME_TO_LIVE`. Với Redis có xác thực, dùng `SPRING_DATA_REDIS_USERNAME` / `SPRING_DATA_REDIS_PASSWORD`; không commit thông tin đăng nhập.

TTL phải lớn hơn 0. Cache lưu JSON theo kiểu cụ thể `CourtResponse`, có UUID và thời điểm tạo sân; không yêu cầu DTO triển khai `Serializable`. Key có dạng:

```text
badmintonhub:v1:courtDetails::<court-id>
```

Cache không lưu giá trị null, không tạo thêm nhóm cache do gõ sai tên. Redis cần sẵn sàng khi gọi chức năng sử dụng cache; lỗi kết nối được xử lý bởi cơ chế lỗi chung của backend.

## API và hành vi cache

Tất cả API dưới đây yêu cầu `Authorization: Bearer <access-token>`. Các endpoint cập nhật/xóa yêu cầu `OWNER` sở hữu cụm sân chứa sân đó hoặc `ADMIN`.

| Method | Endpoint | Kết quả |
| --- | --- | --- |
| GET | `/api/courts/{id}` | `200` với `CourtResponse`; đọc cache trước, truy vấn database và nạp cache nếu chưa có |
| PUT | `/api/owner/courts/{id}` | `200` với dữ liệu đã cập nhật; xóa cache của sân sau commit |
| DELETE | `/api/owner/courts/{id}` | `204`; xóa cache của sân sau commit |

Body cập nhật dùng [UpdateCourtRequest](src/main/java/com/badmintonhub/backend/dto/request/UpdateCourtRequest.java):

```json
{
  "name": "San A",
  "baseHourlyRateVnd": 150000,
  "active": true
}
```

Tên không được để trống và tối đa 100 ký tự; giá không âm; `active` bắt buộc. UUID sai định dạng hoặc request không hợp lệ trả `400`; thiếu xác thực trả `401`; không đủ quyền trả `403`; sân không tồn tại trả `404`. Xóa sân còn dữ liệu tham chiếu như đơn đặt sân có thể trả `409`; dữ liệu và cache không bị xóa trong trường hợp rollback này.

[CourtService](src/main/java/com/badmintonhub/backend/service/CourtService.java) dùng cùng một cache lấy từ `CacheManager` cho các thao tác `get`, `put` và `evict`. Cache manager hỗ trợ transaction sẽ chỉ thực hiện `put`/`evict` đang chờ sau commit; nếu transaction rollback, các thao tác này không được áp dụng. Khi bổ sung thao tác ghi sân ở service khác, cần thực hiện invalidation tương ứng. Gọi service thông qua Spring bean proxy, ví dụ từ controller sang service, để transaction và kiểm tra quyền được áp dụng.

Đây là cache-aside có TTL, không cung cấp tính nhất quán tuyệt đối giữa các request đồng thời. Cập nhật trực tiếp bằng SQL không tự xóa cache, và request đọc đang xử lý đồng thời với cập nhật có thể đưa lại bản cũ vào cache; TTL giới hạn thời gian bản sao được giữ. Quyết định đặt sân, chống trùng lịch và tính số tiền thanh toán phải dựa vào dữ liệu chính và transaction của nghiệp vụ.

## Quan sát cache

Sau khi gọi GET với một sân tồn tại, xem các key:

```powershell
docker exec badmintonhub-redis redis-cli --scan --pattern 'badmintonhub:v1:courtDetails::*'
```

Thay `<court-id>` bằng UUID sân để xem nội dung và số giây còn lại:

```powershell
docker exec badmintonhub-redis redis-cli GET 'badmintonhub:v1:courtDetails::<court-id>'
docker exec badmintonhub-redis redis-cli TTL 'badmintonhub:v1:courtDetails::<court-id>'
```

Sau PUT/DELETE thành công, key cũ biến mất. GET tiếp theo nạp lại dữ liệu mới nếu sân vẫn tồn tại.

## Kiểm thử

Bộ test thông thường dùng H2 và cache trong bộ nhớ, không cần Redis đang chạy:

```powershell
mvn test
```

Để chạy cả test tích hợp với Redis thật tại địa chỉ đã cấu hình:

```powershell
mvn '-Dredis.integration.enabled=true' test
```

Test tích hợp dùng database H2 riêng, UUID ngẫu nhiên và chỉ xóa key của sân test, không dùng `FLUSHDB`. Các trường hợp được kiểm tra gồm JSON round-trip, cache hit/miss, TTL, xác thực, phân quyền chủ sân/admin, validation, cập nhật/xóa sau commit và giữ nguyên cache khi rollback. Database MySQL thực tế không được dùng trong các test này.

Tham khảo: [Spring Data Redis Cache](https://docs.spring.io/spring-data/redis/reference/redis/redis-cache.html).
