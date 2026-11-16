# Persiapan Play Store 2.0.1

- Package produksi: `com.irengcloud.quran` (tanpa akhiran `.preview`).
- Version name: `2.0.1`; version code: `4`.
- Target SDK: `36`; minimum SDK: `24`.
- Pengajuan Play Store pertama, dikonfirmasi pemilik pada 16 November 2026. Tidak ada versi Play Store yang perlu diperbarui dari rilis sebelumnya.
- Build lokal: `powershell -NoProfile -File scripts/build-release.ps1`.
- AAB dan APK menggunakan keystore rilis yang sudah tersedia di `release-signing/`; folder ini tidak masuk Git.

## Materi yang disiapkan

- [Catatan perubahan singkat](whats-new-id-ID.txt).
- [Nama dan deskripsi listing](listing-id-ID.md).
- [Kebijakan privasi](privacy-policy.md); tautan juga tersedia di menu Lainnya aplikasi.
- [Catatan rilis GitHub](../releases/v2.0.1.md).

## Pemeriksaan di Play Console sebelum pengajuan

1. Pastikan version code `4` lebih tinggi daripada semua versi yang pernah diunggah, termasuk track pengujian. Versi di Console belum dapat diverifikasi dari workspace ini.
2. Cocokkan SHA-256 upload certificate AAB dengan **App integrity → Upload key certificate**. Sertifikat **App signing key** Google Play dapat berbeda; jangan mengganti keystore hanya karena keduanya berbeda. Jika upload key yang terdaftar berbeda, gunakan kunci asli atau prosedur reset resmi.
3. Unggah AAB ke internal testing, periksa hasil App Bundle Explorer dan pre-launch report, lalu uji pembaruan dari aplikasi Play Store yang sudah ada. APK GitHub hanya dapat menimpa instalasi dengan sertifikat yang sama.
4. Tinjau deklarasi Data safety sesuai implementasi dan penyedia layanan. Lokasi opsional diproses lewat layanan Geocoder Android; koordinat terakhir juga disimpan dalam preferensi lokal. API menerima informasi jaringan dan parameter permintaan, termasuk kata kunci dan pilihan wilayah. Jangan menyatakan “tidak ada pengumpulan data” tanpa menilai pemrosesan di luar perangkat dan log penyedia.
5. App access: tidak membutuhkan akun. Ads: tidak terdapat SDK/penempatan iklan pada build ini. Lengkapi target usia, rating konten, email dukungan, dan informasi pengembang dengan data pemilik akun.
6. Gunakan URL kebijakan privasi publik yang aktif. Tambahkan ikon 512×512, feature graphic 1024×500, dan screenshot build produksi yang sesuai; jangan gunakan screenshot aplikasi Uji atau gambar mockup sebagai bukti tampilan aplikasi.
7. Publikasi Play Store dan publikasi draft GitHub dilakukan setelah pemeriksaan akun dan penandatanganan di atas selesai.

## Hasil pemeriksaan lokal

- Build rilis APK dan AAB berhasil; 62 unit/UI tests lulus, tanpa kegagalan.
- Lint release: 0 error, 60 warning (pembaruan dependensi, sumber daya tidak terpakai, dan saran pemakaian API).
- APK: `com.irengcloud.quran`, `versionName=2.0.1`, `versionCode=4`, min SDK 24, target SDK 36.
- Verifikasi tanda tangan APK dan tanda tangan JAR AAB berhasil. Sertifikat Android bersifat self-signed; peringatan trust-chain dari `jarsigner` tidak berarti integritas tanda tangan gagal.
- ZIP alignment 16 KB APK berhasil. Semua segmen LOAD pada empat pustaka native arm64-v8a/x86_64 memiliki alignment 16384 byte. Pengujian runtime pada perangkat/emulator 16 KB belum dilakukan.
- SHA-256 upload certificate lokal: `822ca5a83eb7635f321008d34a838517fcf18225e68d45a79af0476f1b33ebae`.
- SHA-256 APK GitHub 2.0.0: `ceeccef5df667e027aa8d5b9de65c3bc0038b082562ab175a82170dd13d7c2f0`. Kunci aslinya belum ditemukan, sehingga pembaruan langsung dari APK tersebut belum didukung. Jangan menghapus data pengguna sebagai bagian pengujian.
- GitHub repository belum memiliki signing secrets pada saat pemeriksaan. Job rilis otomatis berhenti jika secrets belum dilengkapi; paket lokal tetap dapat dipakai untuk persiapan pengajuan pertama.

Rujukan resmi:

- [Menyiapkan aplikasi untuk ditinjau](https://support.google.com/googleplay/android-developer/answer/9859455).
- [Play App Signing](https://support.google.com/googleplay/android-developer/answer/9842756).
- [Memperbarui aplikasi](https://support.google.com/googleplay/android-developer/answer/9859350).
- [Dukungan ukuran halaman 16 KB](https://developer.android.com/guide/practices/page-sizes).

## GitHub Actions

Push branch menjalankan lint, tes, dan build debug. Tag `v2.0.1` atau workflow manual dengan versi yang cocok menyiapkan APK/AAB bertanda tangan dan **draft** GitHub Release. Job rilis memerlukan repository secrets `KEYSTORE_BASE64`, `STORE_PASSWORD`, dan `KEY_PASSWORD` dari keystore yang sama. Tanpa secret, job berhenti dan tidak membuat kunci pengganti. Jangan menaruh kunci atau password dalam commit, log, atau lampiran rilis.
