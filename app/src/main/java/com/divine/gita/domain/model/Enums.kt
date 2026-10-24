package com.divine.gita.domain.model

/** The `key` values must match the `category` strings in assets/quotes.json. */
enum class QuoteCategory(val key: String, val hindi: String, val english: String) {
    KARM("Karm", "कर्म", "Karma"),
    JIVAN_GYAN("Jivan Gyan", "जीवन ज्ञान", "Life Wisdom"),
    MAN_AUR_AATMA("Man aur Aatma", "मन और आत्मा", "Mind & Soul"),
    GYAN("Gyan", "ज्ञान", "Knowledge"),
    DHARM("Dharm", "धर्म", "Dharma"),
    BHAKTI("Bhakti", "भक्ति", "Devotion");

    companion object {
        fun fromKey(key: String): QuoteCategory =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: JIVAN_GYAN
    }
}

/** The `key` values must match the `deity` strings in assets/chalisa.json. */
enum class Deity(val key: String, val hindi: String) {
    GANESH("Ganesh", "श्री गणेश"),
    HANUMAN("Hanuman", "श्री हनुमान"),
    AMBA("Amba", "माँ अम्बा"),
    LAKSHMI("Lakshmi", "माँ लक्ष्मी"),
    SAI("Sai", "साईं बाबा"),
    SHIV("Shiv", "भगवान शिव"),
    GAYATRI("Gayatri", "माँ गायत्री"),
    SARASWATI("Saraswati", "माँ सरस्वती"),
    KRISHNA("Krishna", "श्री कृष्ण"),
    RAM("Ram", "श्री राम"),
    SHANI("Shani", "शनि देव"),
    BHAIRAV("Bhairav", "काल भैरव"),
    SHEETLA("Sheetla", "माँ शीतला"),
    VINDHYESHWARI("Vindhyeshwari", "माँ विन्ध्येश्वरी"),
    SURYADEV("Suryadev", "सूर्य देव");

    companion object {
        fun fromKey(key: String): Deity =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: GANESH
    }
}

/** Which translation(s) the reader wants to see. */
enum class TranslationLanguage(val label: String) {
    HINDI("हिन्दी"),
    ENGLISH("English"),
    BOTH("हिन्दी + English"),
}
