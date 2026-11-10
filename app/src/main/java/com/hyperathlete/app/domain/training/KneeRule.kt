package com.hyperathlete.app.domain.training

/** Brief §15.5. */
enum class KneeAdvice {
    /** 0–3/10 during training and not worse the next morning: carry on. */
    NORMAL,
    /** Above 3, or worse the next morning: lighter weight or shorter range next session. */
    EASE_OFF,
    /** Sharp pain or swelling: see a physiotherapist. */
    SEE_PHYSIO,
}

data class KneeReport(
    /** Highest pain reported during the session (0–10), null if not asked. */
    val during: Int?,
    /** Next-morning answer to "how's your knee?", null until answered. */
    val morning: Int?,
    val sharpOrSwollen: Boolean = false,
)

object KneeRule {
    const val LIMIT = 3

    fun evaluate(r: KneeReport): KneeAdvice = when {
        r.sharpOrSwollen -> KneeAdvice.SEE_PHYSIO
        (r.during ?: 0) > LIMIT -> KneeAdvice.EASE_OFF
        r.morning != null && r.morning > LIMIT -> KneeAdvice.EASE_OFF
        r.morning != null && r.during != null && r.morning > r.during -> KneeAdvice.EASE_OFF
        else -> KneeAdvice.NORMAL
    }
}
