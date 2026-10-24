package com.divine.gita.domain.model

data class Chapter(
    val number: Int,
    val name: String,
    val nameTranslation: String,
    val nameTransliterated: String,
    val nameMeaning: String,
    val summaryEnglish: String,
    val summaryHindi: String,
    val versesCount: Int,
)

data class Sloka(
    val id: Int,
    val chapter: Int,
    val verse: Int,
    val sanskrit: String,
    val transliteration: String,
    val wordMeanings: String,
    val hindiTranslation: String,
    val englishTranslation: String,
    val isFavorite: Boolean,
) {
    val reference: String get() = "$chapter.$verse"
}

data class Quote(
    val id: Long,
    val category: QuoteCategory,
    val text: String,
    val isFavorite: Boolean,
)

data class Chalisa(
    val id: Int,
    val deity: Deity,
    val title: String,
    val verses: List<String>,
)

data class Mood(
    val key: String,
    val title: String,
    val subtitle: String,
    val verseCount: Int,
)

/** Sloka of the Day bundled with the date it was computed for. */
data class DailySloka(
    val sloka: Sloka,
    val dayOfYear: Int,
    val dateMillis: Long,
)
