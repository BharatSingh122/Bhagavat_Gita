package com.divine.gita.data.seed

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.divine.gita.data.local.GitaDatabase
import com.divine.gita.data.local.entity.ChalisaEntity
import com.divine.gita.data.local.entity.ChapterEntity
import com.divine.gita.data.local.entity.MoodEntity
import com.divine.gita.data.local.entity.MoodSlokaCrossRef
import com.divine.gita.data.local.entity.QuoteEntity
import com.divine.gita.data.local.entity.SlokaEntity
import com.divine.gita.domain.repository.DataSeeder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Reads the JSON files in assets/ and pre-populates Room on first launch.
 *
 * Each table is seeded only when it is empty, inside one transaction, so a crash mid-seed is
 * rolled back and retried on the next launch. Existing rows (and favourites) are never overwritten.
 */
class DatabaseSeeder(
    private val context: Context,
    private val db: GitaDatabase,
) : DataSeeder {

    private val mutex = Mutex()

    override suspend fun seedIfNeeded() {
        mutex.withLock {
            withContext(Dispatchers.IO) {
                val started = System.currentTimeMillis()
                db.withTransaction {
                    if (db.chapterDao().count() == 0) seedChapters()
                    if (db.slokaDao().count() == 0) seedSlokas()
                    if (db.quoteDao().count() == 0) seedQuotes()
                    if (db.chalisaDao().count() == 0) seedChalisa()
                    // Moods reference slokas (foreign key), so they are seeded after them.
                    if (db.moodDao().count() == 0) seedMoods()
                }
                Log.d(TAG, "Seed check finished in ${System.currentTimeMillis() - started} ms")
            }
        }
    }

    private suspend fun seedChapters() {
        db.chapterDao().insertAll(readArray(ASSET_CHAPTERS).objects().map {
            ChapterEntity(
                number = it.getInt("number"),
                name = it.optString("name"),
                nameTranslation = it.optString("nameTranslation"),
                nameTransliterated = it.optString("nameTransliterated"),
                nameMeaning = it.optString("nameMeaning"),
                summaryEnglish = it.optString("summaryEnglish"),
                summaryHindi = it.optString("summaryHindi"),
                versesCount = it.optInt("versesCount"),
            )
        })
    }

    private suspend fun seedSlokas() {
        db.slokaDao().insertAll(readArray(ASSET_SLOKAS).objects().map {
            SlokaEntity(
                id = it.getInt("id"),
                chapterNumber = it.getInt("chapter"),
                verseNumber = it.getInt("verse"),
                sanskrit = it.optString("sanskrit"),
                transliteration = it.optString("transliteration"),
                wordMeanings = it.optString("wordMeanings"),
                hindiTranslation = it.optString("hindi"),
                englishTranslation = it.optString("english"),
            )
        })
    }

    private suspend fun seedQuotes() {
        val rows = readArray(ASSET_QUOTES).objects().flatMapIndexed { order, group ->
            val category = group.getString("category")
            group.getJSONArray("quotes").objects().map { q ->
                QuoteEntity(sourceId = q.getInt("id"), category = category, categoryOrder = order, text = q.getString("text").trim())
            }
        }
        db.quoteDao().insertAll(rows)
    }

    private suspend fun seedChalisa() {
        db.chalisaDao().insertAll(readArray(ASSET_CHALISA).objects().map {
            ChalisaEntity(
                id = it.getInt("id"),
                deity = it.getString("deity"),
                title = it.getString("title"),
                verses = it.getJSONArray("verses").strings(),
            )
        })
    }

    private suspend fun seedMoods() {
        val moods = readArray(ASSET_MOODS).objects()
        db.moodDao().insertMoods(moods.map {
            MoodEntity(it.getString("key"), it.getString("title"), it.getString("subtitle"), it.getInt("order"))
        })
        db.moodDao().insertCrossRefs(moods.flatMap { mood ->
            val ids = mood.getJSONArray("slokaIds")
            List(ids.length()) { position -> MoodSlokaCrossRef(mood.getString("key"), ids.getInt(position), position) }
        })
    }

    private fun readArray(asset: String): JSONArray =
        context.assets.open(asset).bufferedReader(Charsets.UTF_8).use { JSONArray(it.readText()) }

    private fun JSONArray.objects(): List<JSONObject> = List(length()) { getJSONObject(it) }
    private fun JSONArray.strings(): List<String> = List(length()) { getString(it) }

    private companion object {
        const val TAG = "DatabaseSeeder"
        const val ASSET_CHAPTERS = "chapters.json"
        const val ASSET_SLOKAS = "slokas.json"
        const val ASSET_QUOTES = "quotes.json"
        const val ASSET_CHALISA = "chalisa.json"
        const val ASSET_MOODS = "moods.json"
    }
}
