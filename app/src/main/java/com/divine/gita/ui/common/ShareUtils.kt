package com.divine.gita.ui.common

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.divine.gita.R
import com.divine.gita.domain.model.Chalisa
import com.divine.gita.domain.model.Quote
import com.divine.gita.domain.model.Sloka
import com.divine.gita.domain.model.TranslationLanguage

object IntentUtils {
    fun shareText(context: Context, text: String, title: String = "Share via") {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(send, title))
    }

    fun shareApp(context: Context) =
        shareText(context, context.getString(R.string.share_app_message, context.packageName), "Share Divine Gita")

    fun rateApp(context: Context) {
        val pkg = context.packageName
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg")))
        } catch (_: ActivityNotFoundException) {
            openUrl(context, "https://play.google.com/store/apps/details?id=$pkg")
        }
    }

    fun openUrl(context: Context, url: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "No browser found", Toast.LENGTH_SHORT).show()
        }
    }
}

/** Plain-text formats used for copy & share. */
object ShareFormatter {
    fun sloka(sloka: Sloka, language: TranslationLanguage): String = buildString {
        appendLine("॥ श्रीमद्भगवद्गीता ${sloka.reference} ॥")
        appendLine()
        appendLine(sloka.sanskrit)
        appendLine()
        if (language != TranslationLanguage.ENGLISH) appendLine(sloka.hindiTranslation)
        if (language == TranslationLanguage.BOTH) appendLine()
        if (language != TranslationLanguage.HINDI) appendLine(sloka.englishTranslation)
        appendLine()
        append("— Divine Gita")
    }

    fun quote(quote: Quote): String = "“${quote.text}”\n\n— ${quote.category.hindi} | Divine Gita"

    fun chalisa(chalisa: Chalisa): String = buildString {
        appendLine("॥ ${chalisa.title} ॥"); appendLine()
        chalisa.verses.forEach { appendLine(it) }
        appendLine(); append("— Divine Gita")
    }
}
