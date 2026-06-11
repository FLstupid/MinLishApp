# MinLish - Ứng dụng Học Từ vựng Tiếng Anh

> Ứng dụng Android học và ôn từ vựng tiếng Anh dành cho người Việt, sử dụng thuật toán lặp lại spaced (SM-2) giúp nhớ từ hiệu quả.

| | |
|---|---|
| **Package** | `com.example.minlish` |
| **Min SDK** | 33 (Android 13+) |
| **Target SDK** | 35 |
| **Language** | Kotlin |
| **UI** | Jetpack Compose + Material 3 |
| **Database** | Room (SQLite) |
| **Cloud** | Firebase (Auth, Firestore, FCM) |

---

## Mục lục

1. [Tổng quan tính năng](#tổng-quan-tính-năng)
2. [Kiến trúc ứng dụng (Architecture)](#kiến-trúc-ứng-dụng)
3. [Cấu trúc thư mục](#cấu-trúc-thư-mục)
4. [Luồng dữ liệu chính](#luồng-dữ-liệu-chính)
5. [Thuật toán SM-2 (Spaced Repetition)](#thuật-toán-sm-2)
6. [Database Schema](#database-schema)
7. [Các màn hình (Screens)](#các-màn-hình)
8. [Các ViewModel và nhiệm vụ](#các-viewmodel)
9. [Hệ thống thông báo](#hệ-thống-thông-báo)
10. [Import / Export từ vựng](#import--export)
11. [Yêu cầu & Cài đặt](#yêu-cầu--cài-đặt)
12. [Câu hỏi vấn đáp dự án](#câu-hỏi-vấn-đáp-dự-án)

---

## Tổng quan tính năng

MinLish là ứng dụng giúp người Việt học từ vựng tiếng Anh theo phương pháp **Spaced Repetition** (lặp lại cách quãng). Ứng dụng bao gồm 5 tab chính:

| Tab | Chức năng |
|-----|-----------|
| **Trang chủ (Dashboard)** | Xem tổng quan tiến độ, chuỗi ngày học, thống kê |
| **Thư viện (Library)** | Quản lý bộ từ, tạo/sửa/xóa bộ, import CSV/XLSX, cài gói từ có sẵn |
| **Học (Learn)** | Học từ mới + ôn từ cũ theo hàng đợi thông minh |
| **Luyện tập (Practice)** | Flashcard, Trắc nghiệm (Quiz), Gõ từ (Typing) |
| **Hồ sơ (Profile)** | Quản lý thông tin cá nhân, cài đặt nhắc học |

### Tính năng chi tiết

- **Quản lý bộ từ**: Tạo, sửa, xóa bộ từ vựng. Mỗi bộ chứa nhiều từ.
- **Học thông minh (Learn)**: Hàng đợi ưu tiên từ cần ôn trước, rồi từ mới theo mục tiêu ngày.
- **3 chế độ luyện tập**:
  - *Flashcard*: Lật thẻ xem nghĩa, đánh giá mức nhớ (Lại/Khó/Ổn/Dễ)
  - *Trắc nghiệm (Quiz):* Chọn nghĩa đúng từ 4 đáp án
  - *Gõ từ (Typing):* Gõ lại từ tiếng Anh đúng nghĩa
- **Kiểm tra nhanh (Checkpoint)**: Quiz 5 câu ngẫu nhiên sau mỗi phiên học
- **Thống kê (Analytics)**: Biểu đồ hoạt động, tỷ lệ nhớ 7 ngày, trình độ ước tính
- **Gói từ có sẵn (Starter Packs)**: Các bộ từ đã biên soạn sẵn theo mục tiêu
- **Import/Export**: Nhập/xuất từ vựng qua file CSV hoặc XLSX
- **Nhắc học hàng ngày**: Thông báo nhắc ôn tập qua AlarmManager
- **Đăng nhập**: Email/Password + Google Sign-In qua Firebase
- **Đồng bộ đám mây**: Từ vựng và hồ sơ đồng bộ lên Firestore

---

## Kiến trúc ứng dụng

### Mô hình MVVM (Model-View-ViewModel)

MinLish sử dụng kiến trúc **MVVM** kết hợp **Repository Pattern**:

```
┌─────────────────────────────────────────────────────────┐
│                        VIEW LAYER                       │
│                                                         │
│   Composable Screens (Jetpack Compose)                  │
│   ├── DashboardScreen    ── collects ── DashboardVM     │
│   ├── LibraryScreen      ── collects ── SetViewModel    │
│   ├── LearnScreen        ── collects ── WordViewModel   │
│   ├── PracticeScreen     ── collects ── PracticeVM      │
│   ├── ProfileScreen      ── collects ── ProfileVM       │
│   ├── QuizScreen         ── collects ── PracticeVM      │
│   ├── TypingScreen       ── collects ── PracticeVM      │
│   ├── AnalyticsScreen    ── collects ── AnalyticsVM      │
│   └── CheckpointScreen   ── collects ── CheckpointVM    │
│                                                         │
├─────────────────────────────────────────────────────────┤
│                     VIEWMODEL LAYER                     │
│                                                         │
│   ┌──────────────┐  ┌──────────────┐  ┌──────────────┐ │
│   │ WordViewModel│  │PracticeVM    │  │SetViewModel  │ │
│   │ (Learn tab)  │  │(Practice tab)│  │(Library tab) │ │
│   └──────┬───────┘  └──────┬───────┘  └──────┬───────┘ │
│          │                 │                  │         │
│   ┌──────┴───────┐  ┌──────┴───────┐  ┌──────┴───────┐ │
│   │DashboardVM   │  │AnalyticsVM   │  │AuthViewModel │ │
│   │(Home tab)    │  │(Stats)       │  │(Login)       │ │
│   └──────────────┘  └──────────────┘  └──────────────┘ │
│                                                         │
├─────────────────────────────────────────────────────────┤
│                    REPOSITORY LAYER                     │
│                                                         │
│   WordRepository ────── WordDao (Room)                  │
│   VocabSetRepository ── VocabularySetDao (Room)         │
│   StudySessionRepository ── StudySessionDao (Room)      │
│   UserRepository ──────── Firestore                     │
│   UserPreferencesRepository ── DataStore                │
│                                                         │
├─────────────────────────────────────────────────────────┤
│                       DATA LAYER                        │
│                                                         │
│   Room Database (SQLite)   │   Firebase (Firestore)     │
│   ├── words                │   ├── users/{uid}          │
│   ├── vocabulary_sets      │   ├── users/{uid}/words    │
│   └── study_sessions       │   └── users/{uid}/sets     │
│                                                         │
│   DataStore Preferences                                    │
│   ├── active_set_id, notifications, reminder time       │
│   └── daily_new_studied_count, profile cache             │
└─────────────────────────────────────────────────────────┘
```

### Tại sao chọn MVVM + Compose?

- **MVVM** tách biệt UI và logic → dễ test, dễ bảo trì
- **Compose** là UI toolkit mới nhất của Google → declarative, reactive
- **Room** + **Flow** → dữ liệu tự cập nhật khi thay đổi (reactive)
- **Repository Pattern** → tách logic data ra khỏi ViewModel, dễ đổi nguồn data (local/cloud)

---

## Cấu trúc thư mục

```
app/src/main/java/com/example/minlish/
│
├── MainActivity.kt                  # Entry point, tạo ViewModels, điều hướng tabs
├── MinLishApplication.kt            # Application class, khởi tạo DB, schedule nhắc học
│
├── data/                            # Layer dữ liệu
│   ├── AppDatabase.kt               # Room Database (3 bảng, 6 migration)
│   ├── dao/                         # Data Access Objects
│   │   ├── WordDao.kt               # Truy vấn từ vựng (due words, practice, introduce...)
│   │   ├── VocabularySetDao.kt      # CRUD bộ từ
│   │   └── StudySessionDao.kt       # Thống kê học tập
│   ├── model/                       # Data classes (Room Entities)
│   │   ├── Word.kt                  # Từ vựng + SM-2 fields
│   │   ├── VocabularySet.kt         # Bộ từ vựng
│   │   ├── StudySession.kt          # Phiên học
│   │   └── User.kt                  # Hồ sơ người dùng
│   ├── repository/                  # Repository layer
│   │   ├── WordRepository.kt        # Word CRUD + sync Firebase
│   │   ├── VocabSetRepository.kt    # Set CRUD + sync
│   │   ├── StudySessionRepository.kt
│   │   └── UserRepository.kt        # Firestore user profile
│   └── preferences/
│       └── UserPreferencesRepository.kt  # DataStore preferences
│
├── logic/                           # Business logic
│   ├── SrsEngine.kt                 # Thuật toán SM-2 tính ngày ôn tiếp theo
│   ├── StudyQueueBuilder.kt         # Xây hàng đợi học hàng ngày
│   ├── StudySessionRecorder.kt      # Ghi lại số câu đúng/sai mỗi phiên
│   ├── WordNorm.kt                  # Chuẩn hóa từ (lowercase, trim)
│   ├── CefrLevels.kt                # Trình độ CEFR (A1-C2)
│   ├── TimeUtils.kt                 # Utility thời gian
│   ├── notification/                # Hệ thống thông báo
│   │   ├── ReminderReceiver.kt      # BroadcastReceiver + AlarmManager nhắc học
│   │   ├── NotificationHelper.kt    # Tạo notification
│   │   └── MinLishFirebaseMessagingService.kt  # FCM push notification
│   ├── importexport/                # Import/Export
│   │   ├── VocabIO.kt               # Parse/build CSV, XLSX
│   │   └── WordImportHelper.kt      # Logic import từ vào bộ
│   ├── starter/                     # Gói từ có sẵn
│   │   ├── StarterPackCatalog.kt    # Danh sách gói từ
│   │   └── StarterPackInstaller.kt  # Cài gói từ vào DB
│   └── auth/
│       └── GoogleIdTokenProvider.kt  # Google Sign-In helper
│
├── ui/                              # Layer giao diện
│   ├── navigation/
│   │   └── AppDestinations.kt       # Enum 5 tab
│   ├── screen/                      # Composable screens
│   │   ├── DashboardScreen.kt       # Trang chủ
│   │   ├── LibraryScreen.kt         # Quản lý bộ từ
│   │   ├── SetDetailScreen.kt       # Chi tiết 1 bộ từ
│   │   ├── LearnScreen.kt           # Học flashcard SRS
│   │   ├── PracticeScreen.kt        # Luyện tập
│   │   ├── QuizScreen.kt            # Trắc nghiệm
│   │   ├── TypingScreen.kt          # Gõ từ
│   │   ├── AnalyticsScreen.kt       # Thống kê
│   │   ├── ProfileScreen.kt         # Hồ sơ
│   │   ├── AuthScreen.kt            # Đăng nhập
│   │   └── AddWordSheet.kt          # Bottom sheet thêm từ
│   ├── viewmodel/                   # ViewModels
│   │   ├── WordViewModel.kt         # Hàng đợi Learn
│   │   ├── PracticeViewModel.kt     # Practice (Flashcard/Quiz/Typing)
│   │   ├── SetViewModel.kt          # Library
│   │   ├── DashboardViewModel.kt    # Dashboard stats
│   │   ├── AnalyticsViewModel.kt    # Thống kê + ước tính trình độ
│   │   ├── CheckpointViewModel.kt   # Quiz kiểm tra nhanh
│   │   ├── AuthViewModel.kt         # Đăng nhập + hồ sơ
│   │   └── ProfileViewModel.kt      # Cài đặt thông báo
│   ├── components/                  # UI components tái sử dụng
│   │   └── Flashcard.kt
│   └── theme/                       # Theme Material 3
│       ├── Color.kt, Theme.kt, Type.kt
│
└── res/
    ├── values/strings.xml
    └── assets/starter/              # Starter pack CSV files
```

---

## Luồng dữ liệu chính

### 1. Luồng Học (Learn Flow)

```
User bấm "Bắt đầu học"
        │
        ▼
WordViewModel.loadQueue()
        │
        ├─── 1. dueWords = getDueReviewWords(setId, now)
        │       → Từ đã học, hết hạn ôn (nextReviewDate <= now, repetitions > 0)
        │
        ├─── 2. pendingRetries = getPendingIntroductionRetries(...)
        │       → Từ mới lần trước học sai, cần ôn lại
        │
        ├─── 3. introduceWords = getIntroduceWordsForLevel(...)
        │       → Từ mới (repetitions=0, lastReviewed=NULL), giới hạn theo dailyGoal
        │
        └─── 4. StudyQueueBuilder.buildDailyQueue()
                    → dueWords + pendingRetries + introduceWords
                    → Deduplicate bằng wordNorm
                    │
                    ▼
              Học từng từ → User đánh giá (Lại/Khó/Ổn/Dễ)
                    │
                    ▼
              SrsEngine.calculateNextReview(word, quality)
                    │
                    ▼
              Cập nhật: nextReviewDate, interval, easeFactor, repetitions
                    │
                    ▼
              updateWordLocal() → Room DB → Flow auto-update
              syncWordToCloud() → Firestore (background)
```

### 2. Luồng Luyện tập (Practice Flow)

```
User mở tab Practice
        │
        ▼
PracticeViewModel đọc activeSetId từ DataStore
        │
        ▼
WordDao.getPracticeWords(setId, now)
  → SELECT * FROM words
    WHERE setId = :setId AND lastReviewed IS NOT NULL
    ORDER BY CASE WHEN due THEN 0 ELSE 1 END, nextReviewDate ASC
        │
        ├─── Flashcard: Duyệt tuần tự (flashcardIndex++)
        │    → User tự đánh giá → SrsEngine.update()
        │
        ├─── Quiz: pickNextDueWordWithRandom(now, excludeId)
        │    → Filter due words → random → exclude từ vừa trả lời
        │    → Hiển thị 4 đáp án (1 đúng + 3 random distractors)
        │    → User chọn → SrsEngine.update(quality=4 or 0)
        │
        └─── Typing: pickNextDueWordWithRandom(now, excludeId)
             → User gõ từ tiếng Anh
             → 3 lần thử: đúng → quality=4, sai hết → quality=0
```

### 3. Luồng Import từ vựng

```
User dán CSV hoặc chọn file XLSX
        │
        ▼
VocabIO.parseCsv(text) hoặc VocabIO.parseXlsx(inputStream)
        │ → List<VocabRow>
        ▼
WordImportHelper.importRowsIntoSet(wordRepository, setId, rows)
        │
        ├─── Với mỗi row:
        │    ├── existsInSet(setId, word)? → skip (skipped++)
        │    ├── Mới → insertWord() → inserted++
        │    └── Exception → errors++
        │
        └─── Gửi SetUiEvent.ImportCompleted(stats)
             → Hiển thị: "Đã thêm X, bỏ qua Y, lỗi Z"
```

---

## Thuật toán SM-2

MinLish implement thuật toán **SM-2** (SuperMemo 2) trong `SrsEngine.kt`.

### Cách hoạt động

Mỗi từ có 3 thông số SM-2:

| Field | Ý nghĩa | Giá trị mặc định |
|-------|---------|-------------------|
| `easeFactor` (EF) | Độ khó của từ (càng thấp = càng khó) | 2.5 |
| `interval` | Số ngày giữa các lần ôn | 0 |
| `repetitions` | Số lần ôn liên tiếp đúng | 0 |

### Công thức tính

**Khi trả lời đúng** (quality >= 3: Hard, Good, Easy):

```
repetitions += 1

Nếu repetitions == 1: interval = 1 ngày
Nếu repetitions == 2: interval = 6 ngày
Nếu repetitions >= 3: interval = interval × EF

EF mới = EF + (0.1 - (5 - quality) × (0.08 + (5 - quality) × 0.02))
```

**Khi trả lời sai** (quality < 3: Again):

```
repetitions = 0
interval = 1 ngày
EF mới = max(1.3, EF - 0.2)
```

### Ví dụ thực tế

Từ "abandon" (EF=2.5, interval=0, reps=0):

| Lần | User đánh giá | Quality | interval mới | reps mới | EF mới | Ngày ôn tiếp |
|-----|--------------|---------|--------------|----------|--------|-------------|
| 1 | Ổn | 4 | 1 | 1 | 2.6 | Ngày mai |
| 2 | Dễ | 5 | 6 | 2 | 2.8 | 6 ngày nữa |
| 3 | Khó | 3 | 16 | 3 | 2.64 | 16 ngày nữa |
| 4 | Lại | 0 | 1 | 0 | 2.44 | Ngày mai (bắt đầu lại) |

---

## Database Schema

### Bảng `words` (từ vựng)

```sql
CREATE TABLE words (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    setId       INTEGER NOT NULL,       -- FK → vocabulary_sets.id
    word        TEXT NOT NULL,          -- Từ tiếng Anh
    wordNorm    TEXT NOT NULL,          -- Lowercase, trim (unique trong 1 set)
    pronunciation TEXT,                 -- Phiên âm (IPA)
    meaning     TEXT NOT NULL,          -- Nghĩa tiếng Việt
    descriptionEn TEXT,                 -- Mô tả tiếng Anh
    collocation TEXT,                   -- Cụm từ đi kèm
    example     TEXT,                   -- Ví dụ
    relatedWords TEXT,                  -- Từ liên quan
    note        TEXT,                   -- Ghi chú
    cefrLevel   TEXT,                   -- Trình độ CEFR (A1-C2)
    -- SM-2 fields
    easeFactor  REAL DEFAULT 2.5,       -- Độ khó
    interval    INTEGER DEFAULT 0,      -- Số ngày interval
    repetitions INTEGER DEFAULT 0,      -- Số lần ôn liên tiếp đúng
    nextReviewDate INTEGER,             -- Ngày ôn tiếp theo (epoch ms)
    lastReviewed INTEGER                -- Lần học cuối (epoch ms)
);
CREATE UNIQUE INDEX index_words_setId_wordNorm ON words(setId, wordNorm);
```

### Bảng `vocabulary_sets` (bộ từ)

```sql
CREATE TABLE vocabulary_sets (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    title       TEXT NOT NULL,          -- Tên bộ từ
    description TEXT,                   -- Mô tả
    tags        TEXT,                   -- Thẻ (CSV)
    wordCount   INTEGER DEFAULT 0,      -- Số từ (denormalized)
    createdAt   INTEGER,                -- Thời gian tạo
    userId      TEXT                    -- UID chủ sở hữu
);
```

### Bảng `study_sessions` (phiên học)

```sql
CREATE TABLE study_sessions (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    date              INTEGER NOT NULL,  -- Ngày học (epoch ms, local midnight)
    wordsReviewed     INTEGER DEFAULT 0, -- Số từ tự đánh giá (Learn/Flashcard)
    correctCount      INTEGER DEFAULT 0, -- Số câu đúng (tự đánh giá)
    objectiveReviewed INTEGER DEFAULT 0, -- Số câu quiz/typing/checkpoint
    objectiveCorrect  INTEGER DEFAULT 0, -- Số câu quiz/typing/checkpoint đúng
    studyTimeMs       INTEGER DEFAULT 0  -- Thời gian học (ms)
);
```

---

## Các màn hình

### 1. Dashboard (Trang chủ)
- Hiển thị: tổng quan hôm nay (ôns + từ mới), chuỗi ngày, độ chính xác, trình độ
- Nút CTA: "Bắt đầu học" / "Học thêm" / "Thêm từ"
- Link đến Analytics chi tiết

### 2. Library (Thư viện)
- Hiển thị gói từ có sẵn (Starter Packs) theo mục tiêu
- Hiển thị bộ từ của user, cho tạo/sửa/xóa
- Chọn bộ từ → mở SetDetailScreen → xem/tThêm/xóa/import/export từ

### 3. Learn (Học)
- Flashcard SRS với 4 mức đánh giá: Lại(0), Khó(3), Ổn(4), Dễ(5)
- Hiển thị progress: "Từ X/Y", "Thẻ A/B (gồm ôn lại)"
- Sau khi xong hàng đợi → đề xuất Checkpoint quiz

### 4. Practice (Luyện tập)
- Dropdown chọn bộ từ
- 3 chế độ: Flashcard (xem lại), Quiz (trắc nghiệm), Typing (gõ từ)
- Priority: từ đến hạn ôn trước → random trong nhóm

### 5. Profile (Hồ sơ)
- Thông tin cá nhân: tên, mục tiêu (IELTS/Business/Travel...), trình độ (A1-C2)
- Mục tiêu từ mới/ngày
- Cài đặt nhắc học: bật/tắt, giờ/phút
- Kiểm tra quyền runtime: `POST_NOTIFICATIONS` (Android 13+), `SCHEDULE_EXACT_ALARM` (Android 12+)
- Warning card hướng dẫn user cấp quyền nếu chưa được

### 6. Auth (Đăng nhập)
- Đăng nhập Email/Password hoặc Google Sign-In
- Firebase Authentication

---

## Các ViewModel

| ViewModel | Tabs | Nhiệm vụ chính |
|-----------|------|----------------|
| **WordViewModel** | Learn | Xây hàng đợi học, quản lý phiên học, đếm progress, đề xuất checkpoint |
| **PracticeViewModel** | Practice | Flashcard + Quiz + Typing, chọn từ priority+random, dropdown chọn bộ từ |
| **SetViewModel** | Library | CRUD bộ từ, import/export, cài gói starter |
| **DashboardViewModel** | Dashboard | Tổng hợp stats từ WordVM + AnalyticsVM |
| **AnalyticsViewModel** | Analytics | Thống kê 7 ngày, biểu đồ, ước tính trình độ |
| **CheckpointViewModel** | Checkpoint | Quiz 5 câu ngẫu nhiên kiểm tra nhanh |
| **AuthViewModel** | Auth, Profile | Đăng nhập, đăng ký, quản lý profile Firebase |
| **ProfileViewModel** | Profile | Cài đặt thông báo nhắc học |

---

## Hệ thống thông báo

### Nhắc học hàng ngày (AlarmManager + BroadcastReceiver)

```
MinLishApplication.onCreate()
    │
    ▼
observePreferencesAndSchedule()
    │ (collect notificationsEnabled, reminderHour, reminderMinute)
    ▼
ReminderReceiver.scheduleExactAlarm() / cancelAlarm()
    │
    ├── enabled = false → cancel alarm
    │
    └── enabled = true
         │
         ▼
    AlarmManager.setExactAndAllowWhileIdle(RTC_WAKEUP, triggerTime, pendingIntent)
         │
         ▼
    BroadcastReceiver.onReceive(ACTION_STUDY_REMINDER)
         │
         ├── Kiểm tra notifications enabled
         ├── Query due words (getDueReviewWordsAnySet)
         ├── NotificationHelper.showReviewNotification()
         └── rescheduleIfNeeded() → lên lịch ngày tiếp theo
```

**Quyền lợi (Android 12+):**
- `SCHEDULE_EXACT_ALARM`: Kiểm tra `canScheduleExactAlarms()` trước khi gọi `setExactAndAllowWhileIdle()`. Nếu chưa cấp → fallback sang `setAndAllowWhileIdle()` (inexact) để tránh crash.
- `POST_NOTIFICATIONS` (Android 13+): Yêu cầu runtime permission khi user bật nhắc học.
- `RECEIVE_BOOT_COMPLETED`: Tự động lên lịch lại alarm sau khi thiết bị khởi động lại.

**ProfileScreen** hiển thị warning card nếu quyền chưa được cấp, kèm nút mở Settings.

### FCM Push Notification
- `MinLishFirebaseMessagingService` xử lý push từ server
- Có thể gửi thông báo nhắc học từ Firebase Console

---

## Import / Export

### Hỗ trợ định dạng
- **CSV**: Dán text trực tiếp hoặc import file
- **XLSX**: Import/Export qua Apache POI

### Cấu trúc CSV
```csv
word,meaning,pronunciation,descriptionEn,example,collocation,relatedWords,note
abandon,vứt bỏ,əˈbændən,to leave permanently,"He abandoned the project.",give up,desert,retain
```

### Starter Packs
- `basic.csv`: Từ vựng cơ bản (~100 từ)
- `ielts.csv`: Từ vựng IELTS (~200 từ)
- `communication.csv`: Từ giao tiếp (~150 từ)
- Cài đặt qua `StarterPackInstaller` → insert vào DB + set active

---

## Yêu cầu & Cài đặt

### Yêu cầu
- [Android Studio](https://developer.android.com/studio) Ladybug trở lên
- JDK 21
- Thiết bị Android API 33+ (Android 13+)
- Tài khoản [Firebase Console](https://console.firebase.google.com/)

### Cài đặt Firebase (BẮT BUỘC)

File `app/google-services.json` không nằm trên Git (bảo mật).

1. Tạo app trên Firebase Console với package `com.example.minlish`
2. Bật Authentication (Email/Password + Google)
3. Bật Firestore Database
4. Tải `google-services.json` → đặt vào `app/google-services.json`

### Build & Chạy

```bash
# Windows
gradlew.bat assembleDebug
gradlew.bat installDebug

# macOS / Linux
./gradlew assembleDebug
./gradlew installDebug
```

### Kiểm thử

```bash
gradlew.bat test                    # Unit tests
gradlew.bat connectedAndroidTest    # Instrumented tests
```

---
