# Website Digital Qur’an — GitHub Pages

Domain produksi: `https://quran.irengcloud.com`.

## Sumber dan build

- `website/`: landing page, CSS, font lokal, ikon, screenshot aplikasi, dukungan, dan pengelolaan data.
- `docs/playstore/privacy-policy.md`: satu sumber kebijakan privasi untuk aplikasi dan website.
- `python3 scripts/build-website.py`: membangun HTML statis ke `build/site/` tanpa dependensi Python tambahan.
- `.github/workflows/pages.yml`: build dan deployment GitHub Pages saat sumber website/privasi berubah di `main`; juga dapat dijalankan manual.
- Hanya `build/site/` yang diterbitkan. Dokumen internal rilis, file signing, AAB/APK, dan source aplikasi tidak menjadi bagian website.

Halaman publik:

- `/`: pengenalan, fitur, screenshot, FAQ, status ketersediaan.
- `/privasi/`: kebijakan privasi.
- `/dukungan/`: kontak, pelaporan masalah, dan bantuan.
- `/hapus-data/`: izin, data lokal, backup, dan batasan penghapusan log layanan.

Link Google Play belum ditampilkan karena listing publik belum dikonfirmasi. Setelah tersedia, ubah bagian `#rilis` dalam `website/index.html` menggunakan URL listing yang benar. Jangan menambahkan formulir pengumpulan data atau analitik tanpa memperbarui kebijakan privasi.

## Pengaturan GitHub dan DNS

GitHub repository **Settings → Pages → Build and deployment → Source: GitHub Actions**. Custom domain harus ditetapkan ke `quran.irengcloud.com` di pengaturan Pages; file `CNAME` dalam artefak saja tidak mengatur domain pada mode Actions.

Di penyedia DNS, tambahkan:

| Type | Name | Target |
| --- | --- | --- |
| CNAME | `quran` | `idlanyor.github.io` |

Target tidak menyertakan `https://`, nama repo, atau path. Hapus record A/AAAA/CNAME yang konflik **hanya pada hostname `quran`** jika ada. Jika memakai Cloudflare, gunakan DNS only saat memvalidasi domain dan menerbitkan sertifikat GitHub. TTL Auto/default cukup.

Setelah DNS terbaca GitHub, tunggu sertifikat HTTPS tersedia dan aktifkan **Enforce HTTPS**. Bila GitHub meminta verifikasi kepemilikan domain melalui TXT, gunakan nilai yang diberikan akun GitHub; nilainya tidak boleh ditebak. Jangan mengubah DNS domain lain.

Setelah aktif, gunakan `https://quran.irengcloud.com/privasi/` untuk URL kebijakan privasi Play Console dan `https://quran.irengcloud.com/` sebagai website pengembang. Sebelum itu, status file lokal/deployment tidak membuktikan domain sudah aktif.

## Pemeriksaan

Jalankan build lalu server preview, misalnya `python3 -m http.server 4173 --directory build/site`. Periksa semua halaman, tautan footer, FAQ, gambar, dan lebar 360–1440 px. Halaman tidak memakai JavaScript, cookie, font eksternal, atau analitik. Kunjungan tetap diproses oleh infrastruktur hosting; kebijakan aplikasi tidak mengklaim bahwa server hosting tidak memiliki log.

Rujukan: [Custom domain GitHub Pages](https://docs.github.com/en/pages/configuring-a-custom-domain-for-your-github-pages-site/managing-a-custom-domain-for-your-github-pages-site), [publishing source](https://docs.github.com/en/pages/getting-started-with-github-pages/configuring-a-publishing-source-for-your-github-pages-site).

## Status deployment — 27 November 2026

Commit website `6c123b0` sudah dipush ke `main`. [Workflow Pages](https://github.com/idlanyor/quran-app/actions/runs/45950506289) selesai sukses. Halaman utama dan `/privasi/` diuji HTTP 200 di URL GitHub Pages sebelum domain dipasang. Custom domain `quran.irengcloud.com` sudah disimpan lewat pengaturan Pages. DNS di penyedia domain dikerjakan pemilik; akses HTTPS pada custom domain masih harus diverifikasi setelah DNS diarahkan. Pemeriksaan browser pada empat halaman di lebar 360, 390, 768, dan 1440 px lulus, termasuk gambar, FAQ, serta tidak ada teks terpotong.
