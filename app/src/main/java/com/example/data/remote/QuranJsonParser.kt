package com.example.data.remote

import com.example.data.local.AyatEntity
import com.example.data.local.PaginatedSearchResult
import com.example.data.local.SearchAyatItem
import com.example.data.local.SurahEntity
import com.example.data.local.TafsirEntity
import org.json.JSONArray
import org.json.JSONObject

/**
 * Adaptive JSON Parser for https://apiku.irengcloud.com/api/v1/quran endpoints.
 * Handles standard and nested JSON structures cleanly without crashing on minor field name variations.
 */
object QuranJsonParser {

    fun parseSurahList(rawJson: String): List<SurahEntity> {
        val trimmed = rawJson.trim()
        if (trimmed.isEmpty()) return emptyList()

        val array: JSONArray = when {
            trimmed.startsWith("[") -> JSONArray(trimmed)
            trimmed.startsWith("{") -> {
                val root = JSONObject(trimmed)
                findJsonArray(root, listOf("data", "surat", "surah", "results", "items", "list"))
                    ?: root.optJSONObject("data")?.let {
                        findJsonArray(it, listOf("surat", "surah", "items", "results", "list"))
                    }
                    ?: return emptyList()
            }
            else -> return emptyList()
        }

        val result = mutableListOf<SurahEntity>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            parseSingleSurahObject(obj, fallbackNomor = i + 1)?.let { result.add(it) }
        }
        return result.sortedBy { it.nomor }
    }

    fun parseSurahDetail(
        rawJson: String,
        expectedNomor: Int
    ): Pair<SurahEntity?, List<AyatEntity>> {
        val trimmed = rawJson.trim()
        if (trimmed.isEmpty() || !trimmed.startsWith("{")) return null to emptyList()

        val root = JSONObject(trimmed)
        val dataObj = when {
            root.has("ayat") || root.has("verses") || root.has("namaLatin") || root.has("nama_latin") -> root
            root.optJSONObject("data") != null -> root.getJSONObject("data")
            root.optJSONObject("surat") != null -> root.getJSONObject("surat")
            root.optJSONObject("surah") != null -> root.getJSONObject("surah")
            else -> root
        }

        val surahEntity = parseSingleSurahObject(dataObj, fallbackNomor = expectedNomor)
        val surahNomor = surahEntity?.nomor ?: expectedNomor

        val ayatArray = findJsonArray(dataObj, listOf("ayat", "verses", "ayahs", "data", "list"))
            ?: findJsonArray(root, listOf("ayat", "verses", "ayahs"))

        val ayats = mutableListOf<AyatEntity>()
        if (ayatArray != null) {
            for (i in 0 until ayatArray.length()) {
                val item = ayatArray.optJSONObject(i) ?: continue
                val nomorAyat = optIntAny(
                    item,
                    listOf("nomorAyat", "nomor_ayat", "nomor", "numberInSurah", "verse_number", "aya", "id"),
                    default = i + 1
                )
                val teksArab = optStringAny(
                    item,
                    listOf("teksArab", "teks_arab", "arab", "arabic", "ar", "text_uthmani", "text")
                )
                val teksLatin = stripHtmlTags(
                    optStringAny(
                        item,
                        listOf("teksLatin", "teks_latin", "latin", "transliteration", "tr", "bacaan")
                    )
                )
                val teksIndo = stripHtmlTags(
                    optStringAny(
                        item,
                        listOf(
                            "teksIndonesia",
                            "teks_indonesia",
                            "terjemahan",
                            "terjemah",
                            "translation",
                            "idn",
                            "arti",
                            "meaning"
                        )
                    )
                )
                val audioUrl = extractAudioUrl(item, surahNomor, nomorAyat)

                if (teksArab.isNotBlank() || teksIndo.isNotBlank()) {
                    ayats.add(
                        AyatEntity(
                            surahNomor = surahNomor,
                            nomorAyat = nomorAyat,
                            teksArab = teksArab,
                            teksLatin = teksLatin,
                            teksIndonesia = teksIndo,
                            audioUrl = audioUrl
                        )
                    )
                }
            }
        }
        return surahEntity to ayats.sortedBy { it.nomorAyat }
    }

    fun parseTafsirList(rawJson: String, expectedSurahNomor: Int): List<TafsirEntity> {
        val trimmed = rawJson.trim()
        if (trimmed.isEmpty()) return emptyList()

        val array: JSONArray? = when {
            trimmed.startsWith("[") -> JSONArray(trimmed)
            trimmed.startsWith("{") -> {
                val root = JSONObject(trimmed)
                val dataObj = root.optJSONObject("data") ?: root
                findJsonArray(dataObj, listOf("tafsir", "tafsirs", "ayat", "data", "items", "list"))
                    ?: findJsonArray(root, listOf("tafsir", "tafsirs", "data"))
            }
            else -> null
        }

        if (array == null) return emptyList()

        val result = mutableListOf<TafsirEntity>()
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val nomorAyat = optIntAny(
                item,
                listOf("ayat", "nomorAyat", "nomor_ayat", "nomor", "verse", "id"),
                default = i + 1
            )
            var teksTafsir = optStringAny(
                item,
                listOf("teks", "tafsir", "uraian", "penjelasan", "text", "content", "kemenag", "isi")
            )
            if (teksTafsir.isBlank()) {
                val nestedTafsir = item.optJSONObject("tafsir") ?: item.optJSONObject("kemenag")
                if (nestedTafsir != null) {
                    teksTafsir = optStringAny(nestedTafsir, listOf("panjang", "pendek", "teks", "uraian", "id"))
                }
            }
            if (teksTafsir.isNotBlank()) {
                result.add(
                    TafsirEntity(
                        surahNomor = expectedSurahNomor,
                        nomorAyat = nomorAyat,
                        teksTafsir = stripHtmlTags(teksTafsir)
                    )
                )
            }
        }
        return result.sortedBy { it.nomorAyat }
    }

    fun parseSearchResults(
        rawJson: String,
        requestedPage: Int,
        requestedSize: Int
    ): PaginatedSearchResult {
        val trimmed = rawJson.trim()
        if (trimmed.isEmpty()) {
            return PaginatedSearchResult(emptyList(), requestedPage, 1, 0, false)
        }

        var array: JSONArray? = null
        var currentPage = requestedPage
        var totalPages = requestedPage
        var totalItems = 0
        var hasMore = false

        if (trimmed.startsWith("[")) {
            array = JSONArray(trimmed)
            totalItems = array.length()
            hasMore = array.length() >= requestedSize
        } else if (trimmed.startsWith("{")) {
            val root = JSONObject(trimmed)
            val dataNode = root.opt("data")
            if (dataNode is JSONArray) {
                array = dataNode
            } else if (dataNode is JSONObject) {
                array = findJsonArray(
                    dataNode,
                    listOf("results", "items", "ayat", "data", "content", "hits", "matches", "list")
                )
                parsePaginationMeta(dataNode)?.let { meta ->
                    currentPage = meta.first
                    totalPages = meta.second
                    totalItems = meta.third
                }
            }

            if (array == null) {
                array = findJsonArray(
                    root,
                    listOf("results", "items", "ayat", "data", "content", "hits", "matches", "list")
                )
            }

            val paginationObj = root.optJSONObject("pagination")
                ?: root.optJSONObject("meta")
                ?: root.optJSONObject("page")
                ?: (dataNode as? JSONObject)?.optJSONObject("pagination")
                ?: (dataNode as? JSONObject)?.optJSONObject("meta")

            if (paginationObj != null) {
                parsePaginationMeta(paginationObj)?.let { meta ->
                    currentPage = meta.first
                    totalPages = meta.second
                    totalItems = meta.third
                }
            } else {
                parsePaginationMeta(root)?.let { meta ->
                    currentPage = meta.first
                    totalPages = meta.second
                    totalItems = meta.third
                }
            }
        }

        val items = mutableListOf<SearchAyatItem>()
        if (array != null) {
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val suratObj = obj.optJSONObject("surat") ?: obj.optJSONObject("surah")

                // Extract surah and ayat number from "ayat_key" / "verse_key" (e.g. "41:12" => surah 41, ayat 12)
                val ayatKey = optStringAny(
                    obj,
                    listOf("ayat_key", "ayatKey", "verse_key", "verseKey", "key")
                )
                val (keySurah, keyAyat) = if (ayatKey.contains(":")) {
                    val parts = ayatKey.split(":")
                    Pair(parts.getOrNull(0)?.trim()?.toIntOrNull(), parts.getOrNull(1)?.trim()?.toIntOrNull())
                } else {
                    Pair(null, null)
                }

                val surahNomor = keySurah ?: optIntAny(
                    obj,
                    listOf("nomorSurat", "nomor_surat", "surahNomor", "surah_id", "surat_id", "surah", "sura", "suratId", "surahId"),
                    default = suratObj?.let { optIntAny(it, listOf("nomor", "id", "number"), 1) } ?: 1
                )
                val surahNamaLatin = optStringAny(
                    obj,
                    listOf("namaSurat", "nama_surat", "namaLatin", "nama_latin", "surahName", "surah_nama")
                ).ifBlank {
                    suratObj?.let {
                        optStringAny(it, listOf("namaLatin", "nama_latin", "nama", "latin", "name"))
                    }.orEmpty()
                }.ifBlank { "Surah ke-$surahNomor" }

                val surahNamaArab = optStringAny(
                    obj,
                    listOf("namaArab", "nama_arab")
                ).ifBlank {
                    suratObj?.let { optStringAny(it, listOf("nama", "namaArab", "arab")) }.orEmpty()
                }

                val nomorAyat = keyAyat ?: optIntAny(
                    obj,
                    listOf("nomorAyat", "nomor_ayat", "ayat", "nomor", "verse", "ayah", "numberInSurah", "verse_id", "verseId"),
                    default = i + 1
                )

                val teksArab = optStringAny(
                    obj,
                    listOf("teksArab", "teks_arab", "arab", "arabic", "ar", "text_uthmani", "text")
                )
                val teksLatin = stripHtmlTags(
                    optStringAny(
                        obj,
                        listOf("teksLatin", "teks_latin", "latin", "transliteration", "tr")
                    )
                )
                val teksIndonesia = stripHtmlTags(
                    optStringAny(
                        obj,
                        listOf(
                            "teksIndonesia",
                            "teks_indonesia",
                            "terjemahan",
                            "terjemah",
                            "translation",
                            "arti",
                            "idn",
                            "text"
                        )
                    )
                )
                val audioUrl = extractAudioUrl(obj, surahNomor, nomorAyat)

                items.add(
                    SearchAyatItem(
                        surahNomor = surahNomor,
                        surahNamaLatin = surahNamaLatin,
                        surahNamaArab = surahNamaArab,
                        nomorAyat = nomorAyat,
                        teksArab = teksArab,
                        teksLatin = teksLatin,
                        teksIndonesia = teksIndonesia,
                        audioUrl = audioUrl
                    )
                )
            }
        }

        if (totalItems == 0) totalItems = items.size
        hasMore = (currentPage < totalPages) || (items.size >= requestedSize && requestedSize > 0)

        return PaginatedSearchResult(
            items = items,
            currentPage = currentPage,
            totalPages = maxOf(totalPages, currentPage),
            totalItems = totalItems,
            hasMore = hasMore
        )
    }

    private fun parseSingleSurahObject(obj: JSONObject, fallbackNomor: Int): SurahEntity? {
        val nomor = optIntAny(
            obj,
            listOf("nomor", "number", "id", "nomorSurat", "surah_number"),
            default = fallbackNomor
        )
        val namaArab = optStringAny(
            obj,
            listOf("nama", "namaArab", "nama_arab", "arab", "arabic", "asma")
        )
        val namaLatin = optStringAny(
            obj,
            listOf("namaLatin", "nama_latin", "latin", "transliteration", "name", "englishName")
        ).ifBlank {
            // If `nama` held Latin text and `asma` held Arabic
            if (namaArab.any { it in 'A'..'Z' || it in 'a'..'z' }) namaArab else ""
        }
        val arti = optStringAny(
            obj,
            listOf("arti", "terjemahan", "terjemah", "translation", "meaning", "idn")
        )
        val jumlahAyat = optIntAny(
            obj,
            listOf("jumlahAyat", "jumlah_ayat", "ayatCount", "numberOfAyahs", "total_ayat", "banyak_ayat"),
            default = obj.optJSONArray("ayat")?.length() ?: 0
        )
        val tempatTurunRaw = optStringAny(
            obj,
            listOf("tempatTurun", "tempat_turun", "revelationType", "type", "kategori", "golongan")
        )
        val tempatTurun = when {
            tempatTurunRaw.lowercase().contains("mad") || tempatTurunRaw.lowercase().contains("med") -> "Madaniyah"
            tempatTurunRaw.lowercase().contains("mak") || tempatTurunRaw.lowercase().contains("mec") -> "Makkiyah"
            tempatTurunRaw.isNotBlank() -> tempatTurunRaw
            else -> "Makkiyah"
        }
        val deskripsi = stripHtmlTags(
            optStringAny(obj, listOf("deskripsi", "description", "keterangan", "info"))
        )
        val audioFullUrl = extractSurahAudioUrl(obj, nomor)

        if (namaLatin.isBlank() && namaArab.isBlank()) return null

        return SurahEntity(
            nomor = nomor,
            namaArab = namaArab.ifBlank { "سورة" },
            namaLatin = namaLatin.ifBlank { "Surah $nomor" },
            arti = arti.ifBlank { "Surah Al-Qur'an" },
            jumlahAyat = jumlahAyat,
            tempatTurun = tempatTurun,
            deskripsi = deskripsi,
            audioFullUrl = audioFullUrl
        )
    }

    private fun extractAudioUrl(obj: JSONObject, surahNomor: Int, nomorAyat: Int): String {
        val direct = obj.opt("audio") ?: obj.opt("audioUrl") ?: obj.opt("audio_url")
        if (direct is String && direct.startsWith("http")) return direct
        if (direct is JSONObject) {
            for (key in listOf("05", "01", "02", "03", "04", "primary", "default", "url", "misyari", "alafasy")) {
                val url = direct.optString(key)
                if (url.startsWith("http")) return url
            }
            val keys = direct.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val v = direct.optString(k)
                if (v.startsWith("http")) return v
            }
        }
        // Fallback standard CDN audio URL (Mishary Rashid Alafasy per-verse MP3) if API returns empty audio field
        val sPad = surahNomor.toString().padStart(3, '0')
        val aPad = nomorAyat.toString().padStart(3, '0')
        return "https://everyayah.com/data/Alafasy_128kbps/$sPad$aPad.mp3"
    }

    private fun extractSurahAudioUrl(obj: JSONObject, surahNomor: Int): String {
        val direct = obj.opt("audioFull") ?: obj.opt("audio_full") ?: obj.opt("audio")
        if (direct is String && direct.startsWith("http")) return direct
        if (direct is JSONObject) {
            for (key in listOf("05", "01", "02", "03", "04", "primary", "default", "url")) {
                val url = direct.optString(key)
                if (url.startsWith("http")) return url
            }
            val keys = direct.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val v = direct.optString(k)
                if (v.startsWith("http")) return v
            }
        }
        return ""
    }

    private fun parsePaginationMeta(obj: JSONObject): Triple<Int, Int, Int>? {
        val page = optIntAny(obj, listOf("page", "currentPage", "current_page", "number"), -1)
        val totalPages = optIntAny(obj, listOf("totalPages", "total_pages", "lastPage", "last_page", "pages"), -1)
        val totalItems = optIntAny(obj, listOf("total", "totalItems", "total_items", "totalElements", "count"), -1)
        if (page == -1 && totalPages == -1 && totalItems == -1) return null
        return Triple(
            if (page > 0) page else 1,
            if (totalPages > 0) totalPages else 1,
            if (totalItems >= 0) totalItems else 0
        )
    }

    private fun findJsonArray(obj: JSONObject, keys: List<String>): JSONArray? {
        for (key in keys) {
            val arr = obj.optJSONArray(key)
            if (arr != null) return arr
        }
        return null
    }

    private fun optStringAny(obj: JSONObject, keys: List<String>): String {
        for (key in keys) {
            if (obj.has(key) && !obj.isNull(key)) {
                val v = obj.opt(key)
                if (v is String && v.isNotBlank()) return v.trim()
                if (v is JSONObject) {
                    val sub = optStringAny(v, listOf("id", "teks", "arab", "latin", "short", "long", "text"))
                    if (sub.isNotBlank()) return sub
                }
            }
        }
        return ""
    }

    private fun optIntAny(obj: JSONObject, keys: List<String>, default: Int): Int {
        for (key in keys) {
            if (obj.has(key) && !obj.isNull(key)) {
                val v = obj.opt(key)
                when (v) {
                    is Number -> return v.toInt()
                    is String -> v.trim().toIntOrNull()?.let { return it }
                    is JSONObject -> {
                        val sub = optIntAny(v, listOf("inSurah", "nomor", "id", "number"), -1)
                        if (sub != -1) return sub
                    }
                }
            }
        }
        return default
    }

    fun stripHtmlTags(input: String): String {
        if (input.isBlank()) return ""
        val brRegex = Regex("(?i)<br\\s*/?>")
        val tagRegex = Regex("<[^>]+>")
        return input
            .replace(brRegex, "\n")
            .replace(tagRegex, "")
            .replace("&quot;", "\"")
            .replace("&amp;", "&")
            .replace("&#39;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .trim()
    }
}
