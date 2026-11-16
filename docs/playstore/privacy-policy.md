# Kebijakan Privasi Digital Qur’an

Berlaku untuk aplikasi Android Digital Qur’an (`com.irengcloud.quran`). Diperbarui 16 November 2026.

## Data di perangkat

Pengaturan, posisi baca terakhir, penanda ayat, progres khatam, pilihan wilayah, dan lokasi terakhir untuk kiblat disimpan di ruang penyimpanan aplikasi. Teks bacaan, jadwal Sholat, doa, dan audio yang diunduh juga dapat disimpan di perangkat. Aplikasi tidak memerlukan akun, tidak menampilkan iklan, dan pada versi ini tidak menyertakan SDK analitik atau pelaporan crash pihak ketiga.

## Lokasi dan sensor

Lokasi digunakan saat pengguna meminta penentuan wilayah atau kiblat. Izin lokasi bersifat opsional; pengguna dapat memilih wilayah secara manual. Aplikasi tidak meminta izin lokasi latar belakang. Penghitungan arah kiblat dan pemrosesan sensor kompas berlangsung di perangkat.

Saat alamat wilayah ditentukan dari koordinat, atau koordinat wilayah dicari dari nama wilayah, aplikasi menggunakan layanan Geocoder Android. Penyedia layanan sistem dapat memproses koordinat atau nama wilayah tersebut sesuai kebijakannya. Jadwal Sholat diambil menggunakan nama provinsi dan kabupaten/kota yang dipilih.

## Sambungan internet

Digital Qur’an menghubungi layanan berikut untuk menyediakan konten:

- `apiku.irengcloud.com` untuk data Al-Qur’an, tafsir, pencarian, dan metadata audio.
- `equran.id` untuk jadwal Sholat, daftar wilayah, serta doa dan dzikir.
- `everyayah.com` serta alamat penyedia audio yang tercantum dalam respons API untuk pemutaran dan pengunduhan murottal.
- Layanan Geocoder yang tersedia pada perangkat untuk pencarian alamat atau titik wilayah.

Permintaan konten dapat mencakup kata kunci pencarian, nomor surah/ayat, pilihan wilayah, atau alamat berkas audio. Seperti koneksi internet pada umumnya, penyedia layanan dapat menerima alamat IP dan informasi teknis permintaan. Pengelolaan log serta masa penyimpanannya oleh penyedia tersebut mengikuti praktik masing-masing penyedia; aplikasi tidak mengendalikan server pihak ketiga. Permintaan API yang ditetapkan aplikasi menggunakan HTTPS.

## Notifikasi dan pencadangan

Notifikasi digunakan untuk pengingat membaca yang diaktifkan pengguna. Pengguna dapat menonaktifkannya melalui aplikasi atau pengaturan Android. Android dapat mencadangkan atau mentransfer preferensi aplikasi, termasuk pilihan wilayah dan lokasi kiblat terakhir, sesuai pengaturan pencadangan perangkat. Berkas audio unduhan tidak termasuk dalam aturan pencadangan aplikasi.

## Pengendalian dan penghapusan

Pengguna dapat mencabut izin lokasi dan notifikasi melalui pengaturan Android, menghapus penanda, serta mengelola audio unduhan dari aplikasi. Menghapus data aplikasi atau mencopot aplikasi menghapus data lokal aplikasi pada perangkat tersebut. Salinan cadangan yang dikelola Android mengikuti pengaturan akun dan perangkat pengguna. Tidak ada akun Digital Qur’an yang perlu dihapus.

## Kontak dan perubahan

Pertanyaan mengenai privasi dapat disampaikan kepada pengelola Digital Qur’an melalui [support@irengcloud.com](mailto:support@irengcloud.com) atau [Issues Digital Qur’an](https://github.com/idlanyor/quran-app/issues). Kebijakan ini akan diperbarui bila pemrosesan data berubah.
