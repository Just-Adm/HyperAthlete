package com.hyperathlete.app.domain.model

import java.time.LocalDate
import java.time.Period

data class UserProfile(
    val name: String = "",
    val birthDate: LocalDate? = null,
    val weightKg: Double = 67.0,
    val heightCm: Double = 176.0,
    val photoUri: String? = null,
) {
    fun ageOn(today: LocalDate): Int? = birthDate?.let { Age.years(it, today) }
}

object Age {
    /** Whole years between [birth] and [today]; never negative. */
    fun years(birth: LocalDate, today: LocalDate): Int =
        if (today.isBefore(birth)) 0 else Period.between(birth, today).years
}
