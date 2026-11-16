![Digital Qur’an — Baca, pahami, dan dengarkan](docs/promosi/readme-cover.png)

# Digital Qur’an

Aplikasi Al-Qur’an untuk Android dengan teks Arab, latin, terjemahan Indonesia, tafsir, dan murottal.

## Fitur

- Jelajahi 114 surah dan cari ayat.
- Baca teks Arab, latin, dan terjemahan bahasa Indonesia.
- Pahami makna ayat melalui tafsir Kemenag.
- Dengarkan murottal dari qari pilihan.
- Simpan penanda dan pantau progres khatam.
- Lihat jadwal Sholat dan arah kiblat.
- Buka kalender Hijriyah offline beserta tanggal Masehi, pasaran, penanggalan Jawa Asapon (1936–2052), dan hari besar Islam.
- Baca doa harian dan dzikir dari EQuran.id, cari berdasarkan kata kunci atau kategori, dan akses offline setelah dimuat.
- Atur pengingat membaca, ukuran huruf, dan tema tampilan.

## Materi promosi

- [Poster portrait](docs/promosi/poster-digital-quran-portrait.png)
- [Cover README](docs/promosi/readme-cover.png)
- [Mockup antarmuka](docs/mockups/)

## Persiapan rilis 2.0.1

Jalankan `powershell -NoProfile -File scripts/build-release.ps1` untuk lint, seluruh tes, serta build APK dan AAB bertanda tangan. Hasil distribusi dan checksum disimpan di `build/releases/2.0.1/`. Keystore dan kredensial tetap berada di folder lokal yang diabaikan Git.

- [Catatan rilis](docs/releases/v2.0.1.md)
- [Persiapan Play Store](docs/playstore/release-2.0.1.md)
- [Kebijakan privasi](docs/playstore/privacy-policy.md)

Workflow GitHub memeriksa branch dan menyiapkan **draft** release pada tag versi atau pemicu manual. Rilis bertanda tangan memerlukan secret kunci yang sudah ada; workflow tidak membuat kunci pengganti.
