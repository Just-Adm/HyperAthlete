package com.hyperathlete.app.domain.nutrition

/** Arabic-aware text folding for search: "أرز" finds "ارز", "شوربة" finds "شوربه", "مُعجّنات" finds "معجنات". */
object ArabicText {
    private val tashkeel = Regex("[\\u0610-\\u061A\\u064B-\\u065F\\u0670\\u06D6-\\u06ED\\u0640]")
    private val spaces = Regex("[\\s\\p{Punct}،؛؟]+")

    fun normalize(s: String): String {
        val noMarks = tashkeel.replace(s.lowercase(), "")
        val sb = StringBuilder(noMarks.length)
        for (ch in noMarks) {
            sb.append(
                when (ch) {
                    'أ', 'إ', 'آ', 'ٱ' -> 'ا'
                    'ة' -> 'ه'
                    'ى' -> 'ي'
                    'ؤ' -> 'و'
                    'ئ' -> 'ي'
                    '٠' -> '0'; '١' -> '1'; '٢' -> '2'; '٣' -> '3'; '٤' -> '4'
                    '٥' -> '5'; '٦' -> '6'; '٧' -> '7'; '٨' -> '8'; '٩' -> '9'
                    else -> ch
                },
            )
        }
        return spaces.replace(sb, " ").trim()
    }

    /** Drops the Arabic definite article so "الحمص" matches "حمص". */
    fun stem(word: String): String = if (word.length > 3 && word.startsWith("ال")) word.substring(2) else word
}

/**
 * Ranked search over foods (built-in + the user's own). Every query word must appear somewhere in the
 * Arabic name, the English name or the extra spellings; exact and prefix matches come first.
 */
class FoodIndex(foods: List<Food>) {
    private class Entry(val food: Food, val ar: String, val en: String, val hay: String, val words: List<String>)

    private val entries = foods.map { f ->
        val ar = ArabicText.normalize(f.ar)
        val en = ArabicText.normalize(f.en)
        val hay = ArabicText.normalize("${f.ar} ${f.en} ${f.aka.orEmpty()}")
        Entry(f, ar, en, hay, hay.split(' ').map(ArabicText::stem))
    }

    fun search(query: String, limit: Int = 60): List<Food> {
        val q = ArabicText.normalize(query)
        if (q.isEmpty()) return emptyList()
        val tokens = q.split(' ').map(ArabicText::stem).filter { it.isNotEmpty() }
        return entries.asSequence()
            .filter { e -> tokens.all { t -> e.hay.contains(t) || e.words.any { w -> w.contains(t) } } }
            .map { e -> e to score(e, q, tokens) }
            .sortedWith(compareBy<Pair<Entry, Int>> { it.second }.thenBy { it.first.ar.length })
            .take(limit)
            .map { it.first.food }
            .toList()
    }

    private fun score(e: Entry, q: String, tokens: List<String>): Int = when {
        e.ar == q || e.en == q -> 0
        e.ar.startsWith(q) || e.en.startsWith(q) -> 1
        e.words.any { it.startsWith(tokens.first()) } -> 2
        else -> 3
    }
}
