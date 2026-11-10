package com.hyperathlete.app.domain.progress

/**
 * Muscles on the body map (brief §12.6). [upper]/[lower] feed the upper-to-lower balance; core and
 * accessory muscles count for neither. [front] = the view it is mainly drawn on (several appear on both).
 *
 * The names are stable ids: the first 19 are the ones exercises and stats have always used. 3.3.0 appended
 * smaller groups. A muscle with a [parent] is a detail of that group (forearm flexors of FOREARMS, soleus of
 * CALVES): it shows the parent's weekly sets on the map, so stats keyed by the old ids stay exactly the same.
 */
enum class Muscle(val region: Region, val front: Boolean, val parent: Muscle? = null) {
    CHEST(Region.UPPER, true),
    FRONT_DELTS(Region.UPPER, true),
    SIDE_DELTS(Region.UPPER, true),
    BICEPS(Region.UPPER, true),
    FOREARMS(Region.UPPER, true),
    ABS(Region.CORE, true),
    OBLIQUES(Region.CORE, true),
    QUADS(Region.LOWER, true),
    ADDUCTORS(Region.LOWER, true),
    TIBIALIS(Region.LOWER, true),
    TRAPS(Region.UPPER, false),
    REAR_DELTS(Region.UPPER, false),
    LATS(Region.UPPER, false),
    MID_BACK(Region.UPPER, false),
    TRICEPS(Region.UPPER, false),
    LOWER_BACK(Region.CORE, false),
    GLUTES(Region.LOWER, false),
    HAMSTRINGS(Region.LOWER, false),
    CALVES(Region.LOWER, false),

    // 3.3.0: smaller groups (appended so the ids above never move).
    FOREARM_FLEXORS(Region.UPPER, true, parent = FOREARMS),
    FOREARM_EXTENSORS(Region.UPPER, false, parent = FOREARMS),
    GASTROCNEMIUS(Region.LOWER, false, parent = CALVES),
    SOLEUS(Region.LOWER, false, parent = CALVES),
    SERRATUS(Region.ACCESSORY, true),
    ROTATOR_CUFF(Region.ACCESSORY, false),
    HIP_FLEXORS(Region.ACCESSORY, true),
    NECK(Region.ACCESSORY, true),
    ;

    /** Accessory muscles are tracked and shown but stay out of the upper-to-lower ratio. */
    enum class Region { UPPER, LOWER, CORE, ACCESSORY }

    /** The group this muscle's sets are counted under ([parent] for a detail, itself otherwise). */
    val group: Muscle get() = parent ?: this

    /** Big groups that make sense in "weakest muscles" (no details, no core or accessory). */
    val major: Boolean get() = parent == null && (region == Region.UPPER || region == Region.LOWER)

    companion object {
        /** Details drawn for a group (FOREARMS → flexors and extensors). */
        fun detailsOf(m: Muscle): List<Muscle> = entries.filter { it.parent == m }
    }
}

/** Which muscles each program exercise trains: primary = 1 set, secondary = ½ set. Stretches count for none. */
object MuscleMap {
    private const val P = 1.0
    private const val S = 0.5

    val byExercise: Map<String, List<Pair<Muscle, Double>>> = mapOf(
        "bench_press" to listOf(Muscle.CHEST to P, Muscle.TRICEPS to S, Muscle.FRONT_DELTS to S),
        "incline_db_press" to listOf(Muscle.CHEST to P, Muscle.FRONT_DELTS to S),
        "cable_fly" to listOf(Muscle.CHEST to P),
        "pec_deck" to listOf(Muscle.CHEST to P),
        "seated_db_curl" to listOf(Muscle.BICEPS to P),
        "hammer_curl" to listOf(Muscle.FOREARMS to P, Muscle.BICEPS to S),
        "rope_hammer_curl" to listOf(Muscle.FOREARMS to P, Muscle.BICEPS to S),
        "lat_pulldown" to listOf(Muscle.LATS to P, Muscle.BICEPS to S),
        "chest_supported_row" to listOf(Muscle.MID_BACK to P, Muscle.LATS to S, Muscle.REAR_DELTS to S),
        "seated_cable_row" to listOf(Muscle.MID_BACK to P, Muscle.LATS to S),
        "back_extension" to listOf(Muscle.LOWER_BACK to P, Muscle.GLUTES to S, Muscle.HAMSTRINGS to S),
        "weighted_dips" to listOf(Muscle.TRICEPS to P, Muscle.CHEST to S),
        "overhead_cable_ext" to listOf(Muscle.TRICEPS to P),
        "seated_db_shoulder_press" to listOf(Muscle.FRONT_DELTS to P, Muscle.SIDE_DELTS to S, Muscle.TRICEPS to S, Muscle.SERRATUS to S),
        "db_lateral_raise" to listOf(Muscle.SIDE_DELTS to P),
        "face_pull" to listOf(Muscle.REAR_DELTS to P, Muscle.TRAPS to S, Muscle.ROTATOR_CUFF to S),
        "db_shrug" to listOf(Muscle.TRAPS to P, Muscle.NECK to S),
        "barbell_shrug" to listOf(Muscle.TRAPS to P, Muscle.NECK to S),
        "farmers_walk" to listOf(Muscle.FOREARMS to P, Muscle.TRAPS to S, Muscle.ABS to S, Muscle.NECK to S),
        "snap_down" to listOf(Muscle.QUADS to S),
        "box_jump" to listOf(Muscle.QUADS to S, Muscle.GLUTES to S, Muscle.CALVES to S),
        "broad_jump" to listOf(Muscle.QUADS to S, Muscle.GLUTES to S, Muscle.CALVES to S),
        "backward_sled_drag" to listOf(Muscle.QUADS to P, Muscle.TIBIALIS to S),
        "sled_push" to listOf(Muscle.QUADS to P, Muscle.GLUTES to S, Muscle.CALVES to S),
        "kb_deadlift" to listOf(Muscle.GLUTES to P, Muscle.HAMSTRINGS to S, Muscle.LOWER_BACK to S),
        "tire_flip" to listOf(Muscle.GLUTES to P, Muscle.QUADS to S, Muscle.LOWER_BACK to S),
        "wall_ball" to listOf(Muscle.QUADS to P, Muscle.FRONT_DELTS to S, Muscle.SERRATUS to S),
        "kb_swing" to listOf(Muscle.GLUTES to P, Muscle.HAMSTRINGS to S),
        "pull_up" to listOf(Muscle.LATS to P, Muscle.BICEPS to S),
        "sandbag_carry" to listOf(Muscle.ABS to P, Muscle.MID_BACK to S),
        "monkey_bars" to listOf(Muscle.FOREARMS to P, Muscle.LATS to S),
        "hanging_knee_raise" to listOf(Muscle.ABS to P, Muscle.HIP_FLEXORS to S),
        "hanging_leg_raise" to listOf(Muscle.ABS to P, Muscle.HIP_FLEXORS to S),
        "dead_bug" to listOf(Muscle.ABS to P, Muscle.HIP_FLEXORS to S),
        "copenhagen" to listOf(Muscle.ADDUCTORS to P, Muscle.OBLIQUES to S),
        "suitcase_carry" to listOf(Muscle.OBLIQUES to P, Muscle.FOREARMS to S),
        "goblet_squat" to listOf(Muscle.QUADS to P, Muscle.GLUTES to S),
        "back_squat" to listOf(Muscle.QUADS to P, Muscle.GLUTES to S, Muscle.ADDUCTORS to S),
        "rdl" to listOf(Muscle.HAMSTRINGS to P, Muscle.GLUTES to S, Muscle.LOWER_BACK to S),
        "db_rdl" to listOf(Muscle.HAMSTRINGS to P, Muscle.GLUTES to S, Muscle.LOWER_BACK to S),
        "bulgarian_split_squat" to listOf(Muscle.QUADS to P, Muscle.GLUTES to P, Muscle.ADDUCTORS to S),
        "hip_adduction" to listOf(Muscle.ADDUCTORS to P),
        "seated_leg_curl" to listOf(Muscle.HAMSTRINGS to P),
        "lying_leg_curl" to listOf(Muscle.HAMSTRINGS to P),
        "wall_tib_raise" to listOf(Muscle.TIBIALIS to P),
        "cossack_squat" to listOf(Muscle.ADDUCTORS to S),
    )
}

/** Lifts on the strength box (brief §12.5). Several ids = the phase variants of one movement. */
enum class KeyLift(val exerciseIds: List<String>) {
    BENCH(listOf("bench_press")),
    SQUAT(listOf("back_squat", "goblet_squat")),
    RDL(listOf("rdl", "db_rdl")),
    LAT_PULLDOWN(listOf("lat_pulldown")),
    SHOULDER_PRESS(listOf("seated_db_shoulder_press")),
    DIPS(listOf("weighted_dips")),
    CURL(listOf("seated_db_curl")),
}
