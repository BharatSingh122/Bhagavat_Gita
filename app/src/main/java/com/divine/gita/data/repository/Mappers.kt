package com.divine.gita.data.repository

import com.divine.gita.data.local.entity.ChalisaEntity
import com.divine.gita.data.local.entity.ChapterEntity
import com.divine.gita.data.local.entity.MoodWithCount
import com.divine.gita.data.local.entity.QuoteEntity
import com.divine.gita.data.local.entity.SlokaEntity
import com.divine.gita.domain.model.Chalisa
import com.divine.gita.domain.model.Chapter
import com.divine.gita.domain.model.Deity
import com.divine.gita.domain.model.Mood
import com.divine.gita.domain.model.Quote
import com.divine.gita.domain.model.QuoteCategory
import com.divine.gita.domain.model.Sloka

fun ChapterEntity.toDomain() = Chapter(
    number = number, name = name, nameTranslation = nameTranslation,
    nameTransliterated = nameTransliterated, nameMeaning = nameMeaning,
    summaryEnglish = summaryEnglish, summaryHindi = summaryHindi, versesCount = versesCount,
)

fun SlokaEntity.toDomain() = Sloka(
    id = id, chapter = chapterNumber, verse = verseNumber, sanskrit = sanskrit,
    transliteration = transliteration, wordMeanings = wordMeanings,
    hindiTranslation = hindiTranslation, englishTranslation = englishTranslation,
    isFavorite = isFavorite,
)

fun QuoteEntity.toDomain() = Quote(
    id = id, category = QuoteCategory.fromKey(category), text = text, isFavorite = isFavorite,
)

fun ChalisaEntity.toDomain() = Chalisa(
    id = id, deity = Deity.fromKey(deity), title = title, verses = verses,
)

fun MoodWithCount.toDomain() = Mood(key = key, title = title, subtitle = subtitle, verseCount = verseCount)
