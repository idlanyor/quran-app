package com.irengcloud.quran.data.local

import android.content.res.AssetManager
import android.util.Log
import org.json.JSONObject

/** Offline transliteration from the same API used for surah details, indexed by surah and verse. */
internal class QuranLatinSource(private val assets: AssetManager) {
    private val corpus by lazy {
        runCatching {
            assets.open("database/quran_latin.json").bufferedReader(Charsets.UTF_8).use {
                JSONObject(it.readText())
            }
        }.getOrElse { error ->
            Log.e("QuranLatinSource", "Could not read bundled transliterations", error)
            JSONObject()
        }
    }

    fun forSurah(number: Int): List<String> {
        val verses = corpus.optJSONArray(number.toString()) ?: return emptyList()
        return List(verses.length()) { index -> verses.optString(index) }
    }
}
