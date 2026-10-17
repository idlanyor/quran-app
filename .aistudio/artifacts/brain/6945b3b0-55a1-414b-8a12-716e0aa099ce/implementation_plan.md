# NurQuran — Aplikasi Al-Qur'an Digital & Pendamping Khatam Harian

Aplikasi Al-Qur'an Android modern yang mengintegrasikan API `https://apiku.irengcloud.com` untuk menyajikan daftar 114 surah, bacaan ayat lengkap (Arab, Latin, dan terjemahan Bahasa Indonesia), tafsir resmi Kementerian Agama RI, pencarian ayat terjemahan dengan paginasi, pemutar murottal audio, serta fitur penyimpanan lokal untuk akses offline dan pelacak target Khatam harian.

## User Review & Critical Decisions

> [!IMPORTANT]
> Berikut adalah rangkuman keputusan fitur dan desain yang telah dikonfirmasi beserta arsitektur penyimpanan yang akan dibangun:

- **Fitur Utama & Pendukung (Confirmed)**: Mencakup seluruh 4 endpoint API (`surat`, `surat/{nomor}`, `tafsir/{nomor}`, `search`), dilengkapi dengan **Pemutar Audio Murottal** (per ayat & putar beruntun satu surah), **Bookmark Ayat & Penanda Terakhir Dibaca**, **Pengaturan Ukuran Font Arab & Toggle Terjemahan/Latin**, serta **Khatam Tracker & Target Bacaan Harian**.
- **Identitas Visual & Palet Warna (Confirmed)**: Gaya **Modern Minimalis Bersih** dengan palet warna netral hangat (Warm Alabaster / Slate) dan aksen **Biru Tenang (Serene Ocean Blue)** yang menyejukkan mata serta mendukung mode terang dan gelap otomatis/manual.
- **Penyimpanan Lokal & Cache Offline (Confirmed)**: Menggunakan **Room Database** dan **DataStore Preferences** di perangkat untuk menyimpan daftar surah, cache detail surah & tafsir yang telah dibuka agar dapat dibaca kembali tanpa koneksi internet, daftar bookmark, riwayat terakhir dibaca, dan progres Khatam harian.

---

## 1. Overview & Core Concept

- **What It Does**: Menyediakan pengalaman membaca, mendengarkan, memahami tafsir, dan mencari ayat Al-Qur'an secara khusyuk, responsif, dan bebas gangguan iklan dengan dukungan penyimpanan offline otomatis.
- **Target Audience / Persona**: Pembaca harian Al-Qur'an, pelajar, dan pengguna yang ingin menjaga konsistensi tilawah harian melalui target Khatam yang terukur serta kemudahan merujuk tafsir Kemenag RI kapan saja.
- **Key Value**: Menggabungkan kecepatan akses lokal (offline-first caching) dengan kelengkapan data API `apiku.irengcloud.com` (teks Utsmani, transliterasi Latin, terjemahan Indonesia, tafsir mendalam, pencarian kata kunci, dan audio murottal) dalam antarmuka minimalis yang menenangkan.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Beranda & Ringkasan Tilawah (Daftar Surah)**:
   - Menampilkan kartu hero **"Terakhir Dibaca"** (nama surah, nomor ayat, tombol lanjutkan langsung ke posisi ayat) dan ringkasan progres **"Target Khatam Harian"** di bagian atas.
   - Menyediakan kolom filter cepat untuk menyaring 114 surah berdasarkan nama Latin, arti terjemahan, atau nomor urut, dilengkapi indikator lencana (badge) bagi surah yang sudah tersimpan untuk dibaca offline.
2. **Membaca Surah, Tafsir & Audio Murottal**:
   - Header surah menampilkan nama Arab kaligrafis, arti, tempat turun (Makkiyah/Madaniyah), jumlah ayat, lafaz Bismillah, serta tombol **Putar Murottal Surah** dan tombol **Unduh/Simpan Offline**.
   - Setiap kartu ayat menampilkan nomor ayat dalam bingkai geometris minimalis, teks Arab Utsmani dengan spasi baris lega, teks transliterasi Latin, dan terjemahan Bahasa Indonesia.
   - Baris aksi di setiap ayat menyediakan tombol: **Putar Audio Ayat**, **Lihat Tafsir Kemenag RI** (membuka lembaran bawah *Modal Bottom Sheet* berisi penjelasan tafsir lengkap ayat tersebut), **Simpan Bookmark**, **Tandai Terakhir Dibaca** (sekaligus menambah progres Khatam), dan **Bagikan/Salin Ayat**.
   - Ketika audio murottal diputar, **Mini Audio Player Bar** melayang di bagian bawah layar menampilkan status pemutaran, ayat yang sedang aktif (dengan auto-scroll opsional ke ayat tersebut), tombol jeda/lanjut, dan navigasi ayat sebelumnya/berikutnya.
3. **Pencarian Ayat Terjemahan (`GET /api/v1/quran/search`)**:
   - Layar pencarian khusus yang memungkinkan pengguna memasukkan kata kunci (misal: *"rahmat"*, *"sabar"*, *"ilmu"*).
   - Menampilkan daftar ayat yang relevan lengkap dengan teks Arab, terjemahan yang menyoroti konteks, informasi surah & nomor ayat, serta tombol **"Muat Lebih Banyak" (Paginasi `page` & `size`)** dan navigasi langsung menuju surah terkait.
4. **Bookmark & Khatam Tracker**:
   - Tab khusus untuk mengelola ayat-ayat favorit yang di-bookmark (dapat dikelompokkan atau dicari) serta dasbor **Khatam Tracker** yang menampilkan persentase penyelesaian 6.236 ayat Al-Qur'an, target ayat per hari, rekam jejak bacaan harian, dan tombol catat progres cepat.
5. **Pengaturan Tampilan Bacaan**:
   - Panel kustomisasi real-time (dengan pratinjau ayat langsung) untuk mengatur ukuran font Arab (24sp – 42sp), ukuran font terjemahan, visibilitas teks Latin, visibilitas terjemahan, pilihan Qari (jika tersedia beberapa opsi audio dari respons API), dan mode tema (Terang / Gelap / Sistem).

### Visual Identity & Theme

- **Aesthetic Direction**: *Serene Modern Minimalist* — mengutamakan ruang kosong (*whitespace*) yang bersih, kontras teks yang nyaman untuk pembacaan durasi panjang, dan tata letak terstruktur tanpa ornamen berlebihan.
- **Color Palette & Mood**:
  - `--background`: `#F8FAFC` (Cool Alabaster / Putih Netral Lembut) pada mode terang; `#0F172A` (Deep Slate Night) pada mode gelap.
  - `--surface`: `#FFFFFF` untuk kartu ayat utama dan `#F1F5F9` untuk kartu sekunder/filter; `#1E293B` pada mode gelap.
  - `--primary`: `#0284C7` (Serene Ocean Blue) sebagai warna aksen utama yang menenangkan untuk nomor ayat, tombol aktif, dan indikator audio; `#38BDF8` pada mode gelap.
  - `--secondary`: `#0D9488` (Calm Teal) untuk indikator progres Khatam dan lencana status tersimpan offline.
  - `--accent`: `#D97706` (Warm Amber) untuk penanda bookmark aktif dan sorotan kata kunci pencarian.
  - `--text-primary` & `--text-secondary`: `#0F172A` (Deep Charcoal) untuk teks Arab & judul utama agar tajam dan jelas, serta `#475569` (Muted Slate) untuk transliterasi Latin dan metadata.
- **Typography & Hierarchy**:
  - **Teks Arab Al-Qur'an**: Menggunakan font naskh lokal berkualitas tinggi (`Amiri` yang dibundel secara lokal di perangkat) dengan tinggi baris (*line-height*) 1.9x hingga 2.2x agar harakat tidak saling bertumpuk.
  - **Judul & UI (Display/Sans)**: Menggunakan `Plus Jakarta Sans` untuk judul layar, nama surah Latin, dan terjemahan sehingga tampak modern, bersih, dan sangat mudah dibaca.
- **Component Styling & Layout**:
  - Menggunakan **Material Design 3 (M3)** dengan sudut membulat halus (`16.dp` pada kartu ayat, `24.dp` pada kartu hero Beranda dan Bottom Sheet Tafsir).
  - Mendukung **Adaptive Layout**: Pada layar ponsel (`Compact`) menggunakan *Bottom Navigation Bar* dengan 4 menu utama (*Surah*, *Pencarian*, *Bookmark & Khatam*, *Pengaturan*); pada layar tablet/lipat (`Expanded`) beralih menggunakan *Navigation Rail* dan tata letak dua panel (*List-Detail*) untuk membaca surah dan tafsir secara berdampingan.
- **Interactive Feedback & Motion**:
  - Animasi transisi halus pada kartu ayat yang sedang memutar audio murottal (bingkai berwarna biru tenang lembut).
  - *Skeleton shimmer* saat memuat data surah atau hasil pencarian dari jaringan, disertai tampilan *Empty State* dan tombol coba lagi yang informatif jika koneksi terputus pada surah yang belum ter-cache.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Arsitektur Offline-First dengan Room Database untuk Surah & Tafsir**
  - *Chosen Approach*: Menyimpan daftar seluruh 114 surah serta meng-cache isi ayat dan tafsir setiap kali pengguna membuka suatu surah (atau menekan tombol unduh offline) ke dalam database lokal Room.
  - *Why*: Membaca Al-Qur'an sering dilakukan di masjid, perjalanan, atau area minim sinyal. Dengan menyimpan respons API secara lokal, surah yang pernah dibuka dapat diakses secara instan (0 detik waktu tunggu) dan tetap berfungsi penuh saat offline.
  - *Alternatives Considered*: Mengambil ulang dari jaringan setiap kali membuka surah — ditolak karena boros kuota dan gagal bekerja saat tidak ada internet.
- **Decision 2: Parser JSON Adaptif untuk Respons API `apiku.irengcloud.com`**
  - *Chosen Approach*: Membangun lapisan deserialisasi JSON yang fleksibel dalam membaca struktur properti dari keempat endpoint (`/api/v1/quran/surat`, `/surat/{nomor}`, `/tafsir/{nomor}`, dan `/search`), baik untuk variasi penamaan field standar Indonesia (`nomor`, `nama`, `namaLatin`, `jumlahAyat`, `tempatTurun`, `arti`, `ayat`, `teksArab`, `teksLatin`, `teksIndonesia`, `audio`, `tafsir`) maupun struktur pembungkus (`data`, `pagination`).
  - *Why*: Menjamin aplikasi tetap tangguh, bebas *crash* akibat ketidaksesuaian nama field minor, dan selalu berhasil menampilkan teks Arab, Latin, terjemahan, audio, serta tafsir secara akurat.
- **Decision 3: Pemutar Audio Terintegrasi Berbasis Antrean Ayat (Verse Queue)**
  - *Chosen Approach*: Menggunakan mesin pemutar audio Android yang mendukung pemutaran per ayat maupun pemutaran beruntun (*auto-advance*) dari ayat pertama hingga akhir surah, lengkap dengan kontrol di *Mini Player Bar*.
  - *Why*: Memudahkan pengguna mendengarkan murottal satu surah penuh sambil menyimak bacaan ayat demi ayat di layar tanpa harus menekan tombol putar berulang kali pada setiap ayat.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

### Architecture & Component Diagram

```
┌───────────────────────────────────────────────────────────────────────────┐
│                         UI LAYER (Jetpack Compose)                        │
│                                                                           │
│  ┌─────────────────┐  ┌──────────────────┐  ┌──────────────────────────┐  │
│  │   HomeScreen    │  │ SurahDetailScreen│  │    SearchAyatScreen      │  │
│  │ (114 Surah List,│  │ (Ayat List, Audio│  │ (Keyword Input, Paginated│  │
│  │  Last Read Card,│  │  Highlight,      │  │  Results, Jump to Ayat)  │  │
│  │  Quick Filter)  │  │  Tafsir Sheet)   │  └──────────────────────────┘  │
│  └────────┬────────┘  └────────┬─────────┘  ┌──────────────────────────┐  │
│           │                    │            │  Bookmark & KhatamScreen │  │
│           │                    │            │ (Saved Verses, Daily     │  │
│           ▼                    ▼            │  Target & Progress Ring) │  │
│  ┌──────────────────────────────────────────┴──────────────────────────┐  │
│  │         Shared Components: MiniAudioPlayerBar & SettingsSheet       │  │
│  └──────────────────────────────────┬──────────────────────────────────┘  │
└─────────────────────────────────────┼─────────────────────────────────────┘
                                      │ StateFlow / UI Events
                                      ▼
┌───────────────────────────────────────────────────────────────────────────┐
│                     VIEWMODEL & DOMAIN STATE LAYER                        │
│                                                                           │
│  ┌─────────────────────┐  ┌─────────────────────┐  ┌───────────────────┐  │
│  │   QuranViewModel    │  │   AudioController   │  │ Settings & Khatam │  │
│  │ (Surah, Detail,     │  │ (MediaPlayer Queue, │  │ State Manager     │  │
│  │  Tafsir & Search)   │  │  Verse Auto-Next)   │  │ (Font, Target)    │  │
│  └──────────┬──────────┘  └──────────┬──────────┘  └─────────┬─────────┘  │
└─────────────┼────────────────────────┼───────────────────────┼────────────┘
              │                        │                       │
              ▼                        ▼                       ▼
┌───────────────────────────────────────────────────────────────────────────┐
│                    REPOSITORY & DATA LAYER (Offline-First)                │
│                                                                           │
│                         ┌─────────────────────────┐                       │
│                         │     QuranRepository     │                       │
│                         └─────┬─────────────┬─────┘                       │
│                               │             │                             │
│          ┌────────────────────┘             └───────────────────┐         │
│          ▼                                                      ▼         │
│  ┌──────────────────────────────────┐  ┌────────────────────────────────┐ │
│  │   Remote API (Retrofit + OkHttp) │  │ Local Persistence (Room + DS)  │ │
│  │  https://apiku.irengcloud.com    │  │ - SurahEntity (114 Surahs)     │ │
│  │  - GET /api/v1/quran/surat       │  │ - AyatEntity (Cached Verses)   │ │
│  │  - GET /api/v1/quran/surat/{no}  │  │ - TafsirEntity (Cached Tafsir) │ │
│  │  - GET /api/v1/quran/tafsir/{no} │  │ - BookmarkEntity (Saved Ayat)  │ │
│  │  - GET /api/v1/quran/search      │  │ - DataStore (LastRead, Khatam, │ │
│  └──────────────────────────────────┘  │   Font Size, Theme Prefs)      │ │
│                                        └────────────────────────────────┘ │
└───────────────────────────────────────────────────────────────────────────┘
```

### Data Model & State

1. **Entitas API & Cache Lokal (Room Database)**:
   - **`SurahEntity`**: Menyimpan nomor surah (`1..114`), nama Arab, nama Latin, arti terjemahan, jumlah ayat, tempat turun, deskripsi ringkas, URL audio full, serta status apakah detail ayat sudah di-cache secara lokal (`isCachedOffline`).
   - **`AyatEntity`**: Menyimpan ID komposit (`surahNomor` + `nomorAyat`), teks Arab, teks Latin, terjemahan Bahasa Indonesia, dan URL audio murottal per ayat.
   - **`TafsirEntity`**: Menyimpan ID komposit (`surahNomor` + `nomorAyat`) dan teks penjelasan tafsir Kementerian Agama RI.
   - **`BookmarkEntity`**: Menyimpan daftar ayat yang ditandai pengguna beserta catatan opsional dan waktu penyimpanan.
   - **`KhatamLogEntity`**: Menyimpan catatan jumlah ayat yang diselesaikan per tanggal untuk menghitung statistik harian dan grafik pencapaian Khatam.
2. **Preferensi Pengguna (DataStore Preferences)**:
   - Menyimpan posisi **Terakhir Dibaca** (`lastReadSurahNomor`, `lastReadSurahNama`, `lastReadAyatNomor`, `timestamp`).
   - Menyimpan pengaturan tampilan (`arabicFontSizeSp`, `translationFontSizeSp`, `showLatin`, `showTranslation`, `themeMode`) dan **Target Khatam Harian** (`dailyTargetAyat`, default 20 ayat/hari).

### Interactive Component & State Mapping

- **Pencarian Surah & Filter Cepat (Beranda)**:
  - Ketikan pengguna pada kolom pencarian surah langsung memfilter `StateFlow<List<SurahEntity>>` secara instan berdasarkan nama Latin, arti, maupun nomor surah, ditambah chip filter (*Semua*, *Makkiyah*, *Madaniyah*, *Tersimpan Offline*).
- **Pembacaan Ayat & Integrasi Tafsir Kemenag RI**:
  - Saat pengguna membuka `SurahDetailScreen(nomor)`, aplikasi menampilkan ayat dari cache Room jika tersedia sambil menyinkronkan data terbaru dari `GET /api/v1/quran/surat/{nomor}`.
  - Saat pengguna menekan tombol **"Tafsir"** pada suatu ayat, aplikasi memeriksa cache `TafsirEntity` atau memanggil `GET /api/v1/quran/tafsir/{nomor}`, lalu membuka *Modal Bottom Sheet* yang menampilkan teks Arab ayat terkait beserta uraian tafsir Kemenag RI secara rapi dan mudah digulir.
- **Pencarian Terjemahan Berpaginasi (`SearchAyatScreen`)**:
  - Pengguna mengetik kata kunci dan menekan Cari (atau menggunakan chip kata kunci populer seperti *"rahmat"*, *"taqwa"*, *"sabar"*, *"doa"*).
  - ViewModel memanggil `GET /api/v1/quran/search?q={query}&page={page}&size=10` dengan *debounce* dan penanganan status memuat.
  - Ketika pengguna menggulir ke bawah atau menekan **"Muat Halaman Berikutnya"**, `page` dinaikkan (`page + 1`) dan hasil baru ditambahkan ke daftar tanpa menghilangkan hasil sebelumnya. Menekan salah satu hasil pencarian akan membuka surah terkait dan menggulir langsung ke nomor ayat tersebut.
- **Kontrol Audio Murottal & Penanda Khatam Otomatis**:
  - Menekan tombol putar pada satu ayat akan memutar audio ayat tersebut dan menampilkan `MiniAudioPlayerBar`. Jika mode *Auto-Next* aktif, selesai satu ayat akan otomatis memutar ayat berikutnya dan memperbarui sorotan kartu ayat di layar.
  - Menekan **"Tandai Terakhir Dibaca"** pada sebuah ayat akan memperbarui kartu Beranda, menghitung selisih ayat yang telah dibaca sejak posisi terakhir, dan otomatis menambahkan progres tersebut ke dalam target harian **Khatam Tracker**.
