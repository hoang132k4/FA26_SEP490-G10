# Hướng dẫn chạy SePay Sandbox trên local

## 1. Cấu hình local

Mở PowerShell tại thư mục dự án:

```powershell
Set-Location 'C:\Users\tripl\Downloads\JEPS\FA26_SEP490-G10'
notepad .env.local
```

Trong `.env.local`, đặt các giá trị riêng cho local:

```properties
LOCAL_BANK_ACCOUNT_NUMBER=<so-tai-khoan-nhan-tien-6-den-20-so>
LOCAL_SEPAY_WEBHOOK_SECRET=<Shared-Secret-cua-SePay-Sandbox>
```

Thay các phần trong dấu `<...>` bằng giá trị của bạn và bỏ dấu ngoặc. Không dùng secret production cho Sandbox; không gửi các giá trị này qua chat và không commit `.env.local`.

Nếu cần ghi đè kết nối Supabase, thêm các dòng sau và thay bằng thông tin từ Supabase Dashboard > Database > Connect:

```properties
spring.datasource.url=jdbc:postgresql://<host>:<port>/postgres?sslmode=require&prepareThreshold=0&tcpKeepAlive=true
spring.datasource.username=<database-user>
spring.datasource.password=<database-password>
```

Nếu không đặt các dòng kết nối này, profile `local` sẽ kế thừa datasource đang cấu hình trong ứng dụng. Dự án hiện dùng Supabase; do đó, webhook Sandbox có thể tạo/cập nhật dữ liệu trên database production. Hãy dùng tài khoản test riêng. Profile `local` đã tắt Hibernate tự động cập nhật schema và bỏ qua việc tạo thông báo mẫu lúc khởi động.

Không lưu giá trị thật của database hoặc secret trong `application.yml`; giữ chúng trong `.env.local`, vốn đã được Git bỏ qua.

## 2. Chạy ứng dụng

Trong PowerShell tại thư mục có `pom.xml`:

```powershell
$env:SPRING_PROFILES_ACTIVE = 'local'
mvn spring-boot:run
```

Giữ terminal này mở. Chỉ truy cập trang web sau khi log hiện `Started JepsApplication`:

```text
http://localhost:8081
```

## 3. Chạy SePay CLI

Mở terminal PowerShell thứ hai và chạy:

```powershell
Set-Location 'C:\Users\tripl\Downloads\JEPS\FA26_SEP490-G10'
sepay webhook listen --forward http://localhost:8081/api/webhooks/sepay --print
```

Endpoint đúng là `/api/webhooks/sepay` (có chữ `s`). CLI phải được đăng nhập/cấu hình để nghe webhook SePay Sandbox.

## 4. Kiểm tra và xử lý lỗi thường gặp

- Trang checkout báo thiếu `BANK_ACCOUNT_NUMBER` hoặc `SEPAY_WEBHOOK_SECRET`: kiểm tra hai dòng `LOCAL_*` trong `.env.local`, sau đó khởi động lại app.
- `ERR_CONNECTION_REFUSED`: app chưa lắng nghe cổng 8081; xem lỗi trong terminal Maven và đảm bảo có log `Started JepsApplication`.
- `NoClassDefFoundError: org/springframework/boot/SpringApplication`: không chạy `debugjava -cp target\classes`; lệnh đó thiếu các thư viện Maven. Dùng `mvn spring-boot:run` ở thư mục có `pom.xml`.
- Webhook trả `401`: kiểm tra đúng Shared Secret Sandbox, timestamp và chữ ký do SePay CLI gửi.

Dừng app và CLI bằng `Ctrl+C` trong terminal tương ứng.

> Nếu mật khẩu database hoặc secret đã từng được lưu trong file được Git theo dõi hoặc chia sẻ qua chat, hãy đổi secret đó và cập nhật lại Railway cùng `.env.local`. Không commit secret mới.
