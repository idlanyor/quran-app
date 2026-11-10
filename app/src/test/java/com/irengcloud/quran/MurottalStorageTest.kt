package com.irengcloud.quran

import com.irengcloud.quran.data.MurottalStorage
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class MurottalStorageTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun incompleteDownloadResumesWithoutFetchingSavedAyat() = runBlocking {
        var failSecondVerse = true
        val requests = mutableListOf<String>()
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            val filename = request.url.pathSegments.last()
            requests += filename
            val status = if (failSecondVerse && filename == "001002.mp3") 503 else 200
            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(status)
                .message("Audio")
                .body(byteArrayOf(1, 2, 3).toResponseBody("audio/mpeg".toMediaType()))
                .build()
        }.build()
        val root = folder.newFolder()
        val storage = MurottalStorage(root, client)

        assertTrue(runCatching { storage.saveSurah(1, 2) {} }.isFailure)
        assertNotNull(storage.localAudioPath(1, 1))
        assertNull(storage.localAudioPath(1, 2))

        failSecondVerse = false
        val progress = mutableListOf<Int>()
        storage.saveSurah(1, 2) { progress += it }
        assertEquals(listOf("001001.mp3", "001002.mp3", "001002.mp3"), requests)
        assertEquals(listOf(1, 2), progress)

        val reopened = MurottalStorage(root, client)
        assertEquals(2, reopened.savedSurahs().getValue(1).verseCount)
        assertEquals(6L, reopened.savedSurahs().getValue(1).bytes)
        assertTrue(reopened.removeSurah(1))
        assertNull(reopened.localAudioPath(1, 1))
        assertTrue(reopened.savedSurahs().isEmpty())
    }
}
