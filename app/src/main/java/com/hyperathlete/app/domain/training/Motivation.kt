package com.hyperathlete.app.domain.training

import com.hyperathlete.app.domain.program.Txt
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * The daily line under "today's workout" (brief §9, box 2). Changes every day, sometimes uses the
 * user's name ("%s"), and fits the day's focus. Deterministic per date so it doesn't flicker.
 */
object Motivation {

    private val byDay: Map<DayOfWeek, List<Txt>> = mapOf(
        DayOfWeek.SUNDAY to listOf(
            Txt("اليوم صدر وباي — كل جولة نظيفة بتنحسب.", "Chest and biceps today — every clean rep counts."),
            Txt("%s، البنش اليوم بالتحكم مش بس بالوزن.", "%s, today's bench is about control, not just load."),
            Txt("صدر أقوى وذراع أعرض. يلا نبلّش.", "A stronger chest, bigger arms. Let's go."),
            Txt("الضخامة بتيجي من الجولات الصح، مش من المرجحة.", "Size comes from honest sets, not swinging."),
            Txt("أسبوع جديد، بداية قوية.", "New week, strong start."),
        ),
        DayOfWeek.MONDAY to listOf(
            Txt("ظهر عريض بيبلّش من سحبة مظبوطة.", "A wide back starts with one good pull."),
            Txt("%s، بدون Straps اليوم — القبضة بتكبر معك.", "%s, no straps today — let your grip grow with you."),
            Txt("سماكة الظهر اليوم، والتراي بيكمّل الصورة.", "Back thickness today; the triceps finish the picture."),
            Txt("اسحب بكوعك مش بإيدك.", "Pull with your elbows, not your hands."),
        ),
        DayOfWeek.TUESDAY to listOf(
            Txt("يوم راحة — العضلة بتكبر هلأ.", "Rest day — this is when muscle grows."),
            Txt("20 دقيقة مرونة بتفرق بالسكوات الجاي.", "Twenty minutes of mobility shows up in your next squat."),
            Txt("%s، الكاحل المرن هو مفتاح الـ Deep Squat.", "%s, mobile ankles are the key to the deep squat."),
        ),
        DayOfWeek.WEDNESDAY to listOf(
            Txt("كتاف عريضة بفورم صارم — الوزن الخفيف مش عيب.", "Wide shoulders come from strict form — light weight is fine."),
            Txt("%s، الرفرفة لمستوى الكتف بس، وخلّي الترابيس ترتاح.", "%s, raises to shoulder height only — let the traps rest."),
            Txt("اليوم بنبني الإطار.", "Today we build the frame."),
            Txt("مشي المزارع آخر الحصة — قبضتك ضد الدامبلز.", "Farmer's walk at the end — your grip against the dumbbells."),
        ),
        DayOfWeek.THURSDAY to listOf(
            Txt("يوم الـ Outdoor! جودة القفز أهم من التعب.", "Outdoor day! Jump quality beats fatigue."),
            Txt("%s، الدائرة على 7 من 10 بس — بكرا أرجل.", "%s, keep the circuit at 7 out of 10 — legs tomorrow."),
            Txt("Sled وإطار وMonkey Bars — اليوم بنبني الرياضي الهجين.", "Sled, tire and monkey bars — today builds the hybrid athlete."),
            Txt("نَفَسك اليوم هو تمرينك.", "Your lungs are the workout today."),
        ),
        DayOfWeek.FRIDAY to listOf(
            Txt("اليوم بنبني الأساس اللي ناقصك.", "Today we build the foundation you're missing."),
            Txt("%s، ركبة قوية يعني قفز أعلى وسكوات أعمق.", "%s, strong knees mean higher jumps and deeper squats."),
            Txt("انزل بتحكم، واطلع بقوة.", "Down with control, up with power."),
            Txt("الجسم السفلي عم يلحق العلوي — حصة ورا حصة.", "Your lower body is catching up — one session at a time."),
        ),
        DayOfWeek.SATURDAY to listOf(
            Txt("ريّح جسمك، بس لا تنسى روتين المرونة.", "Rest up — but don't skip the mobility routine."),
            Txt("توازن ومرونة اليوم، قوة الأسبوع الجاي.", "Balance and mobility today, strength next week."),
            Txt("%s، أسبوع كامل وراك. افتخر فيه.", "%s, a full week behind you. Be proud of it."),
        ),
    )

    private val deload = listOf(
        Txt("أسبوع تخفيف: نص الجولات، وزن أخف، وجسم مرتاح.", "Deload week: half the sets, lighter weights, a rested body."),
        Txt("%s، التخفيف مش كسل — هون بيترسّخ كل اللي بنيته.", "%s, deloading isn't slacking — it locks in what you built."),
    )

    fun lineFor(date: LocalDate, deloadWeek: Boolean): Txt {
        val pool = if (deloadWeek && date.dayOfWeek !in setOf(DayOfWeek.TUESDAY, DayOfWeek.SATURDAY)) deload + byDay.getValue(date.dayOfWeek)
        else byDay.getValue(date.dayOfWeek)
        val week = date.toEpochDay() / 7
        return pool[((week * 3 + date.dayOfMonth) % pool.size).toInt()]
    }

    /** Fills in the name, or drops it cleanly ("%s، " → "") when there's none. */
    fun render(line: String, name: String): String =
        if (name.isBlank()) line.replace("%s، ", "").replace("%s, ", "").replace("%s", "").replaceFirstChar { it.uppercase() }
        else line.replace("%s", name)
}
