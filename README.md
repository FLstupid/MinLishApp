# MinLish

Ứng dụng Android học và ôn **từ vựng tiếng Anh**: quản lý bộ từ, học flashcard SRS (SM-2), luyện quiz/gõ từ, thống kê tiến độ, đăng nhập Firebase, import/export CSV/XLSX.

| | |
|---|---|
| **Package** | `com.example.minlish` |
| **Min SDK** | 33 (Android 13+) |
| **Stack** | Kotlin · Jetpack Compose · Room · Firebase · WorkManager |

## Tính năng chính

- Tạo / quản lý bộ từ, thêm từ (CEFR, ví dụ, collocation…)
- Tab **Học**: hàng đợi ôn thông minh (due → retry → từ mới theo mục tiêu/ngày)
- Tab **Luyện tập**: flashcard, trắc nghiệm, gõ từ
- Dashboard, analytics, checkpoint quiz sau phiên học
- Gói từ khởi đầu trong `app/src/main/assets/starter/`
- Nhắc ôn local (WorkManager) và FCM

## Yêu cầu

- [Android Studio](https://developer.android.com/studio) (Ladybug trở lên khuyến nghị)
- JDK **21** (theo cấu hình Gradle module `app`)
- Tài khoản [Firebase](https://console.firebase.google.com/) (Auth, Firestore, Cloud Messaging)

## Cấu hình Firebase (bắt buộc để build)

File `app/google-services.json` **không** nằm trên Git (bảo mật).

1. Tạo app Android trên Firebase Console, package `com.example.minlish`.
2. Tải `google-services.json` và đặt tại `app/google-services.json`.

Hoặc:

```text
copy app\google-services.json.example app\google-services.json
```

rồi thay nội dung bằng file tải từ Console.

Bật **Authentication** (Email/Password, Google), **Firestore**, **Cloud Messaging** theo nhu cầu app.

## Build & chạy

```bash
# Windows
gradlew.bat assembleDebug
gradlew.bat installDebug

# macOS / Linux
./gradlew assembleDebug
./gradlew installDebug
```

Mở project bằng Android Studio → **Run** trên emulator hoặc thiết bị (API 33+).

## Kiểm thử

```bash
gradlew.bat test
gradlew.bat connectedAndroidTest
```

## Cloud Functions (tùy chọn)

Thư mục `functions/` — email digest MVP (cần cấu hình SendGrid, deploy riêng):

```bash
cd functions
npm install
```

## Cấu trúc mã (tóm tắt)

```text
app/src/main/java/com/example/minlish/
  data/       Room, Repository, DataStore
  logic/      SRS, import/export, notification, starter packs
  ui/         Compose screens & ViewModels
```

## Bảo mật & repo

- Không commit: `local.properties`, `google-services.json`, keystore, `.idea/`, `.cursor/`.
- Mẫu: `app/google-services.json.example`.

## Giấy phép

Phần mềm **chỉ cho sử dụng cá nhân / học tập** — xem [LICENSE](LICENSE).

---

© 2026 Nhutltm
