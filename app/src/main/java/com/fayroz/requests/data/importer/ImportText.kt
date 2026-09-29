package com.fayroz.requests.data.importer

import java.text.Normalizer
import java.util.Locale

object ImportText {
    private val arabicDigits = mapOf(
        '٠' to '0', '١' to '1', '٢' to '2', '٣' to '3', '٤' to '4',
        '٥' to '5', '٦' to '6', '٧' to '7', '٨' to '8', '٩' to '9',
        '۰' to '0', '۱' to '1', '۲' to '2', '۳' to '3', '۴' to '4',
        '۵' to '5', '۶' to '6', '۷' to '7', '۸' to '8', '۹' to '9',
    )

    fun normalizeItemName(value: String): String {
        val digitsNormalized = buildString(value.length) {
            value.forEach { append(arabicDigits[it] ?: it) }
        }
        val punctuationNormalized = digitsNormalized.replace('٫', '.').replace("٬", "")
        val noDiacritics = Normalizer.normalize(punctuationNormalized.trim().lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
        return noDiacritics
            .replace("[أإآٱ]".toRegex(), "ا")
            .replace("ة", "ه")
            .replace("ى", "ي")
            .replace("ؤ", "و")
            .replace("ئ", "ي")
            .replace("[×xX]".toRegex(), "x")
            .replace("[^\\p{L}\\p{N}.]+".toRegex(), " ")
            .replace("\\s+".toRegex(), " ")
            .trim()
    }

    fun normalizeCode(value: String): String = buildString(value.length) {
        value.trim().uppercase(Locale.ROOT).forEach { ch ->
            val mapped = arabicDigits[ch] ?: ch
            if (mapped.isLetterOrDigit()) append(mapped)
        }
    }

    fun parseNumber(value: String): Double? {
        var clean = buildString(value.length) {
            value.trim().forEach { append(arabicDigits[it] ?: it) }
        }
            .replace("٫", ".")
            .replace("٬", "")
            .replace("%", "")
            .replace("جنيه", "", ignoreCase = true)
            .replace("EGP", "", ignoreCase = true)
            .replace(" ", "")
            .replace("[^0-9,.-]".toRegex(), "")

        if (clean.isBlank()) return null
        if (clean.contains(',') && clean.contains('.')) {
            clean = clean.replace(",", "")
        } else if (clean.contains(',')) {
            val last = clean.substringAfterLast(',')
            clean = if (last.length in 1..2) clean.replace(',', '.') else clean.replace(",", "")
        }
        return clean.toDoubleOrNull()
    }
}
