import os
import json
import sqlite3
import re

def main():
    project_root = os.path.dirname(os.path.abspath(__file__))
    json_dir = os.path.join(project_root, "quran-json-temp", "surah")
    output_dir = os.path.join(project_root, "app", "src", "main", "assets", "database")
    os.makedirs(output_dir, exist_ok=True)
    
    db_path = os.path.join(output_dir, "nurquran_offline.db")
    if os.path.exists(db_path):
        os.remove(db_path)

    print(f"Creating SQLite database at: {db_path}")
    conn = sqlite3.connect(db_path)
    cur = conn.cursor()

    # Create tables exactly matching Room's schema in QuranDatabase_Impl.kt
    cur.execute("""
    CREATE TABLE IF NOT EXISTS `surahs` (
        `nomor` INTEGER NOT NULL,
        `namaArab` TEXT NOT NULL,
        `namaLatin` TEXT NOT NULL,
        `arti` TEXT NOT NULL,
        `jumlahAyat` INTEGER NOT NULL,
        `tempatTurun` TEXT NOT NULL,
        `deskripsi` TEXT NOT NULL,
        `audioFullUrl` TEXT NOT NULL,
        `isCachedOffline` INTEGER NOT NULL,
        PRIMARY KEY(`nomor`)
    )
    """)

    cur.execute("""
    CREATE TABLE IF NOT EXISTS `ayats` (
        `surahNomor` INTEGER NOT NULL,
        `nomorAyat` INTEGER NOT NULL,
        `teksArab` TEXT NOT NULL,
        `teksLatin` TEXT NOT NULL,
        `teksIndonesia` TEXT NOT NULL,
        `audioUrl` TEXT NOT NULL,
        PRIMARY KEY(`surahNomor`, `nomorAyat`)
    )
    """)

    cur.execute("""
    CREATE TABLE IF NOT EXISTS `tafsirs` (
        `surahNomor` INTEGER NOT NULL,
        `nomorAyat` INTEGER NOT NULL,
        `teksTafsir` TEXT NOT NULL,
        PRIMARY KEY(`surahNomor`, `nomorAyat`)
    )
    """)

    cur.execute("""
    CREATE TABLE IF NOT EXISTS `bookmarks` (
        `surahNomor` INTEGER NOT NULL,
        `surahNamaLatin` TEXT NOT NULL,
        `surahNamaArab` TEXT NOT NULL,
        `nomorAyat` INTEGER NOT NULL,
        `teksArab` TEXT NOT NULL,
        `teksIndonesia` TEXT NOT NULL,
        `catatan` TEXT NOT NULL,
        `timestamp` INTEGER NOT NULL,
        PRIMARY KEY(`surahNomor`, `nomorAyat`)
    )
    """)

    cur.execute("""
    CREATE TABLE IF NOT EXISTS `khatam_logs` (
        `dateKey` TEXT NOT NULL,
        `ayatCountRead` INTEGER NOT NULL,
        `surahCountRead` INTEGER NOT NULL,
        `lastUpdated` INTEGER NOT NULL,
        PRIMARY KEY(`dateKey`)
    )
    """)

    # Room master table required for Room validation
    identity_hash = "d7a2960a4b42942b6ffeadafa5378f5f"
    cur.execute("""
    CREATE TABLE IF NOT EXISTS room_master_table (
        id INTEGER PRIMARY KEY,
        identity_hash TEXT
    )
    """)
    cur.execute("INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES(42, ?)", (identity_hash,))

    # Parse canonical surah metadata from QuranSeedData.kt to preserve accurate place of revelation & audio
    seed_file = os.path.join(project_root, "app", "src", "main", "java", "com", "example", "data", "local", "QuranSeedData.kt")
    surah_meta = {}
    if os.path.exists(seed_file):
        with open(seed_file, "r", encoding="utf-8") as f:
            content = f.read()
            # Match SurahEntity(1, "الفاتحة", "Al-Fatihah", "Pembukaan", 7, "Makkiyah", ...)
            pattern = re.compile(r'SurahEntity\(\s*(\d+),\s*"([^"]+)",\s*"([^"]+)",\s*"([^"]+)",\s*(\d+),\s*"([^"]+)"')
            for match in pattern.finditer(content):
                num = int(match.group(1))
                surah_meta[num] = {
                    "namaArab": match.group(2),
                    "namaLatin": match.group(3),
                    "arti": match.group(4),
                    "jumlahAyat": int(match.group(5)),
                    "tempatTurun": match.group(6)
                }

    total_surahs = 0
    total_ayats = 0
    total_tafsirs = 0

    for num in range(1, 115):
        json_file = os.path.join(json_dir, f"{num}.json")
        if not os.path.exists(json_file):
            print(f"Warning: {json_file} not found!")
            continue

        with open(json_file, "r", encoding="utf-8") as f:
            data = json.load(f)

        s_obj = data.get(str(num), {})
        meta = surah_meta.get(num, {})

        nama_arab = meta.get("namaArab") or s_obj.get("name", "")
        nama_latin = meta.get("namaLatin") or s_obj.get("name_latin", "")
        arti = meta.get("arti") or s_obj.get("translations", {}).get("id", {}).get("name", "")
        jumlah_ayat = meta.get("jumlahAyat") or int(s_obj.get("number_of_ayah", len(s_obj.get("text", {}))))
        tempat_turun = meta.get("tempatTurun", "Makkiyah")
        deskripsi = ""
        audio_full = f"https://equran.nos.wjv-1.neo.id/audio-full/Misyari-Rasyid-Al-Afasi/{num:03d}.mp3"
        is_cached_offline = 1

        cur.execute("""
        INSERT INTO surahs (nomor, namaArab, namaLatin, arti, jumlahAyat, tempatTurun, deskripsi, audioFullUrl, isCachedOffline)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, (num, nama_arab, nama_latin, arti, jumlah_ayat, tempat_turun, deskripsi, audio_full, is_cached_offline))
        total_surahs += 1

        # Ayats
        texts = s_obj.get("text", {})
        translations_id = s_obj.get("translations", {}).get("id", {}).get("text", {})
        
        # Sort ayat numbers
        sorted_ayat_nums = sorted([int(k) for k in texts.keys()])
        for a_num in sorted_ayat_nums:
            str_a_num = str(a_num)
            t_arab = texts.get(str_a_num, "")
            t_indo = translations_id.get(str_a_num, "")
            t_latin = ""  # Rio Astamal does not separate per-verse latin, clean default
            audio_ayat = f"https://equran.nos.wjv-1.neo.id/audio-partial/Misyari-Rasyid-Al-Afasi/{num:03d}{a_num:03d}.mp3"

            cur.execute("""
            INSERT INTO ayats (surahNomor, nomorAyat, teksArab, teksLatin, teksIndonesia, audioUrl)
            VALUES (?, ?, ?, ?, ?, ?)
            """, (num, a_num, t_arab, t_latin, t_indo, audio_ayat))
            total_ayats += 1

        # Tafsirs (Kemenag)
        tafsir_kemenag = s_obj.get("tafsir", {}).get("id", {}).get("kemenag", {}).get("text", {})
        for a_num, t_text in tafsir_kemenag.items():
            if t_text:
                cur.execute("""
                INSERT INTO tafsirs (surahNomor, nomorAyat, teksTafsir)
                VALUES (?, ?, ?)
                """, (num, int(a_num), t_text))
                total_tafsirs += 1

    conn.commit()
    
    # Run VACUUM and ANALYZE to optimize SQLite database size and query performance
    print("Optimizing database size (VACUUM & ANALYZE)...")
    cur.execute("VACUUM")
    cur.execute("ANALYZE")
    conn.commit()
    conn.close()

    db_size = os.path.getsize(db_path) / (1024 * 1024)
    print(f"Database generation complete!")
    print(f"Summary: {total_surahs} Surahs, {total_ayats} Ayats, {total_tafsirs} Tafsirs.")
    print(f"Final DB Size: {db_size:.2f} MB")

if __name__ == "__main__":
    main()
