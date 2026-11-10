#!/usr/bin/env python3
"""Generates app/src/main/assets/program/program.json — the training program seed (brief §14–15).
Edit here, re-run, commit both. Text: ar = Arabic, en = English, mx = mixed mode (Arabic with English terms)."""
import json, os

import re
LRI, PDI = "\u2066", "\u2069"
_MEASURE = re.compile(r"(\d+(?:[.,]\d+)?(?:\s?[-–]\s?\d+(?:[.,]\d+)?)?(?:\s?×\s?\d+(?:[-–]\d+)?)?\s?(?:cm|kg|km|m)\b|\d+\s?×\s?\d+(?:[-–]\d+)?|\d+(?:[-–]\d+)?°)")


def iso(text):
    """Keeps numbers with units ("30-40 cm", "3 × 10", "30°") left-to-right inside Arabic text."""
    return _MEASURE.sub(lambda m: LRI + m.group(0) + PDI, text)


def T(ar, en, mx=None):
    d = {"ar": iso(ar), "en": en}
    if mx: d["mx"] = iso(mx)
    return d

EX = []
def ex(id, en, ar, muscles, cls="OTHER", measure="REPS", load="NONE", gear="BODY", art=None, per_side=False, knee=False, lower=False, cue=None):
    e = {"id": id, "name": T(ar, en), "muscles": muscles, "cls": cls, "measure": measure, "load": load,
         "gear": gear, "art": art or id.upper(), "perSide": per_side, "knee": knee, "lower": lower}
    if cue: e["cue"] = cue
    EX.append(e)

# ---------- Sunday: Chest + Biceps ----------
ex("bench_press", "Barbell Bench Press", "بنش برس بالبار", T("صدر · تراي · كتف أمامي", "Chest · Triceps · Front delts", "صدر · Triceps · كتف أمامي"),
   "COMPOUND", load="WEIGHTED", gear="BARBELL", art="BENCH",
   cue=T("لوحين الكتف لورا ولتحت ورجليك ثابتة عالأرض. نزّل البار بتحكم لنص الصدر وادفع.", "Shoulder blades back and down, feet planted. Lower the bar under control to mid-chest, then press."))
ex("incline_db_press", "Incline Dumbbell Press (30°)", "ضغط دامبلز مائل (30°)", T("صدر علوي · كتف أمامي", "Upper chest · Front delts"),
   "COMPOUND", load="WEIGHTED", gear="DUMBBELL", art="INCLINE",
   cue=T("المقعد على 30°. نزّل لحد ما تحس بشد بالصدر بدون ما يتدحرج كتفك لقدّام.", "Bench at 30°. Lower until the chest stretches without your shoulders rolling forward."))
ex("cable_fly", "Cable Fly", "كيبل فلاي", T("صدر", "Chest"), "ISOLATION", load="WEIGHTED", gear="CABLE", art="FLY",
   cue=T("كوعك مثني شوي وثابت. لم إيديك قدام صدرك بقوس واسع وارجع بتحكم.", "Soft, fixed elbows. Bring the hands together in a wide arc and return under control."))
ex("pec_deck", "Pec Deck", "جهاز الفراشة", T("صدر", "Chest"), "ISOLATION", load="WEIGHTED", gear="MACHINE", art="FLY",
   cue=T("ظهرك لازق بالمسند، ولم الذراعين بدون ما تطلع كتافك لفوق.", "Back flat on the pad; squeeze the arms together without shrugging."))
ex("seated_db_curl", "Seated Dumbbell Curl", "بايسبس دامبلز جالس", T("باي", "Biceps", "Biceps"), "ISOLATION", load="WEIGHTED", gear="DUMBBELL", art="CURL",
   cue=T("ظهرك عالمسند وكوعك جنبك. اطلع بدون مرجحة ونزّل بثانيتين.", "Back on the pad, elbows at your sides. Curl without swinging, lower in two seconds."))
ex("hammer_curl", "Hammer Curl", "هامر كيرل", T("عضلة الساعد · باي", "Brachioradialis · Biceps", "Brachioradialis · Biceps"), "ISOLATION", load="WEIGHTED", gear="DUMBBELL", art="HAMMER",
   cue=T("قبضة محايدة (الإبهام لفوق). كوعك ثابت والساعد هو اللي بيشتغل.", "Neutral grip, thumbs up. Elbows fixed; let the forearm do the work."))
ex("rope_hammer_curl", "Rope Hammer Curl", "هامر كيرل بالحبل", T("عضلة الساعد · باي", "Brachioradialis · Biceps", "Brachioradialis · Biceps"), "ISOLATION", load="WEIGHTED", gear="CABLE", art="HAMMER",
   cue=T("الحبل من تحت، كوعك ثابت، وشد لفوق بقبضة محايدة.", "Rope on the low pulley, elbows fixed, curl with a neutral grip."))

# ---------- Monday: Back + Triceps ----------
ex("lat_pulldown", "Lat Pulldown", "سحب أمامي", T("الظهر العريض · باي", "Lats · Biceps", "Lats · Biceps"), "COMPOUND", load="WEIGHTED", gear="CABLE", art="PULLDOWN",
   cue=T("صدرك لفوق، اسحب البار لأعلى صدرك بكوعك مش بإيدك، وارجع لفوق للآخر.", "Chest up, pull the bar to your upper chest with the elbows, then return to a full stretch."))
ex("chest_supported_row", "Chest-Supported T-Bar Row", "تي بار رو بسند صدر", T("وسط الظهر · الظهر العريض", "Mid back · Lats", "وسط الظهر · Lats"), "COMPOUND", load="WEIGHTED", gear="PLATE", art="ROW",
   cue=T("صدرك عالمسند طول الوقت. اسحب لبطنك وشد لوحين الكتف لبعض.", "Keep your chest on the pad. Row to the belly and squeeze the shoulder blades together."))
ex("seated_cable_row", "Seated Cable Row", "سحب كيبل جالس", T("وسط الظهر · الظهر العريض", "Mid back · Lats", "وسط الظهر · Lats"), "COMPOUND", load="WEIGHTED", gear="CABLE", art="ROW",
   cue=T("ظهرك مستقيم وما تمرجح لورا. اسحب لبطنك وارجع بتحكم.", "Torso upright, no rocking back. Row to the belly and return under control."))
ex("back_extension", "45° Back Extension", "تمديد ظهر 45°", T("أسفل الظهر · الجلوتس · الفخد الخلفي", "Lower back · Glutes · Hamstrings", "أسفل الظهر · Glutes · Hamstrings"), "COMPOUND", load="WEIGHTED", gear="PLATE", art="BACK_EXT",
   cue=T("انطوي من الورك مش من الظهر، واطلع لحد ما يصير جسمك خط مستقيم بس — بدون تقويس.", "Hinge at the hips, not the spine. Come up to a straight line only — no arching."))
ex("weighted_dips", "Weighted Dips", "ديبس بالحزام", T("تراي · صدر", "Triceps · Chest", "Triceps · صدر"), "COMPOUND", load="BODYWEIGHT", gear="BELT", art="DIPS",
   cue=T("جسمك مستقيم عشان التراي ياخد الشغل. انزل لحد ما يصير العضد أفقي تقريباً.", "Stay upright so the triceps do the work. Lower until the upper arm is about level."))
ex("overhead_cable_ext", "Overhead Cable Triceps Extension", "تراي كيبل فوق الراس", T("تراي (الرأس الطويل)", "Triceps (long head)", "Triceps (long head)"), "ISOLATION", load="WEIGHTED", gear="CABLE", art="OVERHEAD_EXT",
   cue=T("الحبل ورا راسك وكوعك جنب راسك وثابت. افرد للآخر وارجع بتحكم.", "Rope behind the head, elbows by your ears and still. Extend fully, return slowly."))

# ---------- Wednesday: Shoulders + Traps ----------
ex("seated_db_shoulder_press", "Seated Dumbbell Shoulder Press", "ضغط أكتاف دامبلز جالس", T("كتف أمامي وجانبي · تراي", "Front & side delts · Triceps", "كتف أمامي وجانبي · Triceps"), "COMPOUND", load="WEIGHTED", gear="DUMBBELL", art="SHOULDER_PRESS",
   cue=T("ظهرك عالمسند وبطنك مشدود. ادفع لفوق بدون ما تقوّس أسفل ظهرك.", "Back on the pad, core braced. Press up without arching your lower back."))
ex("db_lateral_raise", "Dumbbell Lateral Raise", "رفرفة جانبي دامبلز", T("كتف جانبي", "Side delts"), "ISOLATION", load="WEIGHTED", gear="DUMBBELL", art="LATERAL",
   cue=T("ارفع لمستوى الكتف بس، والكوع قايد الحركة. نزول 3 ثواني وبدون مرجحة.", "Raise to shoulder height only, elbows leading. Three-second lowering, no swinging."))
ex("face_pull", "Face Pull", "فيس بول", T("كتف خلفي · ترابيس وسطى", "Rear delts · Mid traps", "كتف خلفي · Mid traps"), "ISOLATION", load="WEIGHTED", gear="CABLE", art="FACE_PULL",
   cue=T("الحبل بمستوى وجهك. اسحب لجبينك وافتح إيديك لبرّا مع تدوير الكتف لورا.", "Rope at face height. Pull to the forehead, hands apart, rotating the shoulders back."))
ex("db_shrug", "Dumbbell Shrug", "شرَغ دامبلز", T("ترابيس", "Traps", "Traps"), "ISOLATION", load="WEIGHTED", gear="DUMBBELL", art="SHRUG",
   cue=T("ارفع كتافك لأذنيك مستقيم (بدون تدوير) وثبّت ثانية فوق.", "Lift the shoulders straight toward the ears (no rolling) and hold a second on top."))
ex("barbell_shrug", "Barbell Shrug", "شرَغ بالبار", T("ترابيس", "Traps", "Traps"), "ISOLATION", load="WEIGHTED", gear="BARBELL", art="SHRUG",
   cue=T("البار قدامك وإيديك بعرض الكتف. ارفع مستقيم وثبّت ثانية.", "Bar in front, hands shoulder-width. Lift straight up and hold a second."))
ex("farmers_walk", "Farmer's Walk", "مشي المزارع", T("ترابيس · الساعد · الكور", "Traps · Forearms · Core", "Traps · الساعد · Core"), "OTHER", "DISTANCE", "WEIGHTED", "DUMBBELL", "FARMER",
   cue=T("دامبلز ثقيلة، كتافك لورا وبطنك مشدود. خطوات قصيرة وسريعة.", "Heavy dumbbells, shoulders back, core tight. Short, quick steps."))

# ---------- Thursday: Outdoor ----------
ex("snap_down", "Snap Down", "سناب داون", T("الركبة · الهبوط", "Knees · Landing", "الركبة · Landing"), "OTHER", art="SNAP_DOWN", knee=True,
   cue=T("اطلع على أصابعك، انزل بسرعة لنص سكوات وثبّت الهبوط — ركبك بخط رجليك.", "Rise onto your toes, drop fast into a half squat and freeze — knees track over the toes."))
ex("box_jump", "Box Jump", "القفز على الصندوق", T("انفجارية الرجلين", "Leg power"), "OTHER", art="BOX_JUMP", knee=True,
   cue=T("اقفز وانزل ناعم عالصندوق، ثبّت ثانيتين، وانزل مشي — ما تقفز لتحت.", "Jump, land softly on the box, hold two seconds and step down — never jump down."))
ex("broad_jump", "Broad Jump", "القفز العريض", T("انفجارية الرجلين", "Leg power"), "OTHER", art="BROAD_JUMP", knee=True,
   cue=T("مرجح إيديك واقفز لقدّام، وثبّت الهبوط بدون ما تتحرك.", "Swing the arms, jump forward and stick the landing without moving."))
ex("backward_sled_drag", "Backward Sled Drag", "سحب السليد لورا", T("الفخد الأمامي · الركبة · التيبياليس", "Quads · Knees · Tibialis", "Quads · الركبة · Tibialis"), "OTHER", "DISTANCE", "WEIGHTED", "SLED", "SLED_DRAG", knee=True,
   cue=T("امشي لورا على مقدمة قدمك وركبك متقدمة، خطوات قصيرة ومتواصلة.", "Walk backwards on the balls of your feet, knees forward, short continuous steps."))
ex("sled_push", "Sled Push", "دفع السليد", T("الفخد · الجلوتس · القوة", "Quads · Glutes · Power", "الفخد · Glutes · القوة"), "OTHER", "DISTANCE", "WEIGHTED", "SLED", "SLED_PUSH", knee=True,
   cue=T("جسمك مايل لقدّام وإيديك ممدودة. ادفع الأرض لورا بخطوات قوية.", "Lean in with long arms. Drive the floor back with strong steps."))
ex("kb_deadlift", "Kettlebell Deadlift", "ديدلفت كتلبيل", T("الورك · الجلوتس · الفخد الخلفي", "Hips · Glutes · Hamstrings", "الورك · Glutes · Hamstrings"), "OTHER", load="WEIGHTED", gear="KETTLEBELL", art="KB_DEADLIFT",
   cue=T("ادفع وركك لورا وظهرك مستقيم، ووقف بدفع الورك لقدّام.", "Push the hips back with a flat back, stand up by driving the hips forward."))
ex("tire_flip", "Tire Flip", "قلب الإطار", T("الجسم كامل · الورك", "Full body · Hips"), "OTHER", art="TIRE_FLIP",
   cue=T("صدرك عالإطار وأصابعك تحته. ادفع بالورك والرجلين مش بظهرك، وبعدين ادفع بإيديك.", "Chest to the tire, fingers under. Drive with hips and legs, not your back, then push it over."))
ex("wall_ball", "Wall Ball", "وول بول", T("الرجلين · الكتاف · التحمل", "Legs · Shoulders · Conditioning"), "OTHER", load="WEIGHTED", gear="MEDBALL", art="WALL_BALL",
   cue=T("سكوات كامل والكرة على صدرك، واطلع ارمي على الهدف بنفس الحركة.", "Full squat with the ball at your chest, stand and throw to the target in one motion."))
ex("kb_swing", "Kettlebell Swing", "كتلبيل سوينغ", T("الجلوتس · الفخد الخلفي · الكور", "Glutes · Hamstrings · Core", "Glutes · Hamstrings · Core"), "OTHER", load="WEIGHTED", gear="KETTLEBELL", art="KB_SWING",
   cue=T("الحركة من الورك مش من الكتف. شد الجلوتس فوق والكتلبيل لمستوى صدرك.", "Hinge, don't squat. Snap the hips, squeeze the glutes, bell to chest height."))
ex("pull_up", "Pull-up", "عقلة", T("الظهر العريض · باي", "Lats · Biceps", "Lats · Biceps"), "COMPOUND", load="BODYWEIGHT", gear="BELT", art="PULL_UP",
   cue=T("ابدأ من تعليق كامل، واطلع لحد ما تعدي دقنك البار بدون مرجحة.", "Start from a dead hang and pull until your chin clears the bar, no kipping."))
ex("sandbag_carry", "Sandbag Bear-Hug Carry", "حمل كيس الرمل", T("الكور · الظهر العلوي · التحمل", "Core · Upper back · Conditioning", "Core · الظهر العلوي · التحمل"), "OTHER", "DISTANCE", "WEIGHTED", "SANDBAG", "SANDBAG",
   cue=T("احضن الكيس قريب من صدرك، ظهرك مستقيم، وامشي بخطوات ثابتة.", "Hug the bag tight to your chest, back tall, steady steps."))
ex("monkey_bars", "Monkey Bars", "بارات القرد", T("القبضة · الظهر · الكتاف", "Grip · Back · Shoulders"), "OTHER", "LENGTH", art="MONKEY_BARS",
   cue=T("كتافك مشدودة مش مرخية، وخلّي الحركة إيقاعية من بار لبار.", "Keep the shoulders engaged, move rhythmically bar to bar."))
ex("hanging_knee_raise", "Hanging Knee Raise", "رفع ركب معلّق", T("بطن سفلي", "Lower abs"), "OTHER", art="KNEE_RAISE",
   cue=T("علّق بدون مرجحة، ارفع ركبك لصدرك بلف الحوض، ونزّل بتحكم.", "Hang still, curl the knees to your chest by tilting the pelvis, lower with control."))
ex("hanging_leg_raise", "Hanging Leg Raise", "رفع رجلين معلّق", T("بطن سفلي", "Lower abs"), "OTHER", art="KNEE_RAISE",
   cue=T("رجليك شبه مفرودة، ارفعها لمستوى الورك أو أعلى بدون مرجحة.", "Legs nearly straight, raise to hip height or higher with no swing."))
ex("dead_bug", "Dead Bug (Med Ball)", "ديد بَغ مع ميديسن بول", T("الكور العميق · أسفل الظهر", "Deep core · Lower back", "Core العميق · أسفل الظهر"), "OTHER", art="DEAD_BUG", per_side=True,
   cue=T("أسفل ظهرك لازق بالأرض طول الوقت. امسك الكرة بين إيدك وركبتك المقابلة ومد الثانيين ببطء.", "Lower back glued down. Squeeze the ball between one hand and the opposite knee; extend the other pair slowly."))
ex("copenhagen", "Copenhagen Side Plank", "كوبنهاغن بلانك جانبي", T("الأدكترز · الجوانب", "Adductors · Obliques", "Adductors · الجوانب"), "OTHER", "TIME", art="COPENHAGEN", per_side=True,
   cue=T("الرجل اللي فوق على الستيب، جسمك خط مستقيم من الراس للقدم، وما تنزّل الورك.", "Top leg on the step, body in a straight line, don't let the hips sag."))
ex("suitcase_carry", "Suitcase Carry", "حمل الحقيبة", T("الجوانب · الثبات · القبضة", "Obliques · Stability · Grip"), "OTHER", "DISTANCE", "WEIGHTED", "KETTLEBELL", "SUITCASE", per_side=True,
   cue=T("كتلبيل بإيد وحدة وجسمك مستقيم — لا تميل للجهة الثقيلة.", "Kettlebell in one hand, stand tall — don't lean toward the weight."))

# ---------- Friday: Legs + Knees + Lower back ----------
ex("goblet_squat", "Heel-Elevated Goblet Squat", "جوبلت سكوات والكعب مرفوع", T("الفخد الأمامي · الركبة", "Quads · Knees", "Quads · الركبة"), "COMPOUND", load="WEIGHTED", gear="DUMBBELL", art="GOBLET", knee=True, lower=True,
   cue=T("كعبك على صحن 5-10 kg، الدامبل على صدرك. انزل 3 ثواني ووقفة ثانية تحت.", "Heels on a 5–10 kg plate, dumbbell at the chest. Three seconds down, one-second pause."))
ex("back_squat", "Heel-Elevated Back Squat", "باك سكوات والكعب مرفوع", T("الفخد الأمامي · الركبة · الجلوتس", "Quads · Knees · Glutes", "Quads · الركبة · Glutes"), "COMPOUND", load="WEIGHTED", gear="BARBELL", art="BACK_SQUAT", knee=True, lower=True,
   cue=T("البار على الترابيس، كعبك مرفوع، صدرك لفوق وركبك بخط أصابعك.", "Bar on the traps, heels raised, chest up, knees tracking the toes."))
ex("rdl", "Romanian Deadlift", "ديدلفت روماني", T("الفخد الخلفي · الجلوتس · أسفل الظهر", "Hamstrings · Glutes · Lower back", "Hamstrings · Glutes · أسفل الظهر"), "COMPOUND", load="WEIGHTED", gear="BARBELL", art="RDL", lower=True,
   cue=T("ظهر مستقيم، ادفع الورك لورا، وانزل لحد ما تحس شد بالفخد الخلفي بس.", "Flat back, push the hips back, go down only until the hamstrings stretch."))
ex("db_rdl", "Dumbbell Romanian Deadlift", "ديدلفت روماني دامبلز", T("الفخد الخلفي · الجلوتس · أسفل الظهر", "Hamstrings · Glutes · Lower back", "Hamstrings · Glutes · أسفل الظهر"), "COMPOUND", load="WEIGHTED", gear="DUMBBELL", art="RDL", lower=True,
   cue=T("الدامبلز قريبة من رجليك، ظهر مستقيم، وادفع الورك لورا.", "Dumbbells close to the legs, flat back, hips back."))
ex("bulgarian_split_squat", "Bulgarian Split Squat", "بلغاري سبليت سكوات", T("الجلوتس · الفخد · الأدكترز", "Glutes · Quads · Adductors", "Glutes · Quads · Adductors"), "COMPOUND", load="WEIGHTED", gear="DUMBBELL", art="BULGARIAN", per_side=True, knee=True, lower=True,
   cue=T("الرجل الخلفية على البنش، انزل مستقيم لتحت وركبتك الأمامية بخط أصابعك.", "Rear foot on the bench, drop straight down, front knee over the toes."))
ex("hip_adduction", "Hip Adduction Machine", "جهاز الأدكترز", T("الأدكترز", "Adductors", "Adductors"), "ISOLATION", load="WEIGHTED", gear="MACHINE", art="ADDUCTION", lower=True,
   cue=T("ضم رجليك بتحكم وثبّت ثانية، وارجع ببطء لحد ما تحس بشد.", "Squeeze the legs together, pause, return slowly to a light stretch."))
ex("seated_leg_curl", "Seated Leg Curl", "ثني ركبة جالس", T("الفخد الخلفي", "Hamstrings", "Hamstrings"), "ISOLATION", load="WEIGHTED", gear="MACHINE", art="LEG_CURL", lower=True,
   cue=T("ثبّت فخدك تحت المسند، اثني للآخر وارجع بثلاث ثواني.", "Thighs locked under the pad, curl fully, return in three seconds."))
ex("lying_leg_curl", "Lying Leg Curl", "ثني ركبة نايم", T("الفخد الخلفي", "Hamstrings", "Hamstrings"), "ISOLATION", load="WEIGHTED", gear="MACHINE", art="LEG_CURL", lower=True,
   cue=T("وركك لازق بالمسند، اثني بدون ما ترفع الورك.", "Hips pinned to the pad, curl without lifting them."))

# ---------- Mobility & prevention (Tue/Sat) ----------
ex("knee_to_wall", "Knee-to-Wall Ankle Mobilization", "تحريك الكاحل للحيط", T("مرونة الكاحل", "Ankle mobility"), art="KNEE_TO_WALL", per_side=True,
   cue=T("قدمك قدام الحيط، دز ركبتك للحيط والكعب ثابت على الأرض.", "Foot facing the wall, drive the knee to the wall with the heel down."))
ex("wall_tib_raise", "Wall Tibialis Raise", "رفع التيبياليس عالحيط", T("التيبياليس", "Tibialis", "Tibialis"), art="TIB_RAISE",
   cue=T("ظهرك عالحيط ورجليك لقدّام شوي، ارفع أصابع قدمك لفوق للآخر.", "Back to the wall, feet slightly forward, lift your toes as high as you can."))
ex("calf_stretch", "Calf Stretch", "إطالة السمانة", T("الكاحل · السمانة", "Ankles · Calves"), "OTHER", "TIME", art="CALF_STRETCH", per_side=True,
   cue=T("مرة الركبة مستقيمة ومرة مثنية، والكعب على الأرض.", "Once with a straight knee, once bent, heel down."))
ex("couch_stretch", "Couch Stretch", "إطالة الكنبة", T("قابض الورك · الفخد الأمامي", "Hip flexors · Quads", "Hip flexors · Quads"), "OTHER", "TIME", art="COUCH", per_side=True,
   cue=T("ركبتك عند الحيط والقدم عليه، شد الجلوتس وطلّع صدرك لفوق.", "Knee at the wall, foot up on it, squeeze the glute and lift your chest."))
ex("hamstring_stretch", "Supine Hamstring Stretch", "إطالة الفخد الخلفي", T("الفخد الخلفي", "Hamstrings", "Hamstrings"), "OTHER", "TIME", art="HAM_STRETCH", per_side=True,
   cue=T("نايم على ظهرك، الحبل أو المنشفة حول قدمك، ارفع الرجل مفرودة لحد الشد.", "On your back, strap around the foot, raise the straight leg to a stretch."))
ex("cossack_squat", "Cossack Squat", "كوزاك سكوات", T("الأدكترز · المرونة · التوازن", "Adductors · Mobility · Balance", "Adductors · المرونة · التوازن"), art="COSSACK", per_side=True,
   cue=T("رجليك مفتوحة، انزل على جهة والرجل الثانية مفرودة وأصابعها لفوق.", "Wide stance, sit to one side with the other leg straight, toes up."))
ex("deep_squat_hold", "Assisted Deep Squat Hold", "ثبات سكوات عميق", T("عمق السكوات", "Squat depth"), "OTHER", "TIME", art="DEEP_SQUAT",
   cue=T("امسك عمود، انزل لتحت والكعب على الأرض، وخلّي صدرك لفوق.", "Hold a post, sink down with heels flat, chest up."))
ex("wall_slides", "Wall Slides + Thread the Needle", "انزلاق عالحيط + لفّة الصدر", T("الكتف · الظهر العلوي", "Shoulders · Upper back"), art="WALL_SLIDE",
   cue=T("ظهرك وإيديك عالحيط وطلّعهم لفوق بدون ما يتركوا الحيط، وبعدها لف صدرك لكل جهة.", "Back and arms on the wall, slide up without losing contact, then rotate through each side."))
ex("single_leg_balance", "Single-Leg Balance", "التوازن على رجل وحدة", T("التوازن · الكاحل", "Balance · Ankle"), "OTHER", "TIME", art="BALANCE", per_side=True,
   cue=T("وقفة على رجل وحدة والركبة مرنة شوي. لما يصير سهل غمّض عيونك.", "Stand on one leg, knee soft. Close your eyes once it gets easy."))

def slot(ex, sets, mn, mx=None, rest=None, note=None, alts=None, weeks=None, load=None):
    s = {"ex": ex, "sets": sets, "min": mn, "max": mx if mx is not None else mn}
    if rest: s["rest"] = rest
    if note: s["note"] = note
    if alts: s["alts"] = alts
    if weeks: s["weeks"] = weeks
    if load: s["load"] = load
    return s

def block(id, slots, type="STRAIGHT", rounds=1, rest=(60, 90), title=None, minutes=None, note=None):
    b = {"id": id, "type": type, "rounds": rounds, "rest": list(rest), "slots": slots}
    if title: b["title"] = title
    if minutes: b["minutes"] = minutes
    if note: b["note"] = note
    return b

MOBILITY = [block("mobility", [
    slot("knee_to_wall", 2, 10, rest=[15, 15]),
    slot("wall_tib_raise", 3, 15, 25, rest=[20, 20]),
    slot("calf_stretch", 2, 45, rest=[10, 10], note=T("جولة الركبة مستقيمة وجولة مثنية", "One round straight knee, one bent")),
    slot("couch_stretch", 1, 45, rest=[10, 10]),
    slot("hamstring_stretch", 2, 45, rest=[10, 10], note=T("بحبل أو منشفة", "With a strap or towel")),
    slot("cossack_squat", 2, 6, rest=[20, 20]),
    slot("deep_squat_hold", 3, 30, 45, rest=[20, 20], note=T("مسك عمود", "Hold a post")),
    slot("wall_slides", 2, 10, rest=[15, 15], note=T("10 من كل واحد", "10 of each")),
    slot("single_leg_balance", 3, 30, rest=[10, 10], note=T("عيون مغمضة لما يصير سهل", "Eyes closed once it gets easy")),
], rest=(15, 15), minutes=20, note=T("بدون أوزان. بتقدر تعمله بعد أي حصة ثانية.", "No weights. You can also do it after any other session."))]

DAYS = [
 {"day": "SUNDAY", "blocks": [block("main", [
    slot("bench_press", 4, 6, 8, rest=[120, 180]),
    slot("incline_db_press", 3, 8, 10, rest=[120, 120]),
    slot("cable_fly", 3, 12, 15, rest=[60, 90], note=T("آخر جولة للفشل", "Last set to failure"), alts=["pec_deck"]),
    slot("seated_db_curl", 3, 8, 12, rest=[90, 90], note=T("كوعك ثابت ونزول ثانيتين. إذا 20 kg بتخليك تمرجح، نزّل لـ 16-18 kg.", "Elbows still, two seconds down. If 20 kg makes you swing, use 16–18 kg.")),
    slot("hammer_curl", 3, 10, 12, rest=[60, 90], note=T("للساعد (Brachioradialis)", "For the forearm (brachioradialis)"), alts=["rope_hammer_curl"]),
 ])]},
 {"day": "MONDAY", "notes": [T("السحب بدون Straps عشان القبضة والساعد.", "Pull without straps to build grip and forearms.")],
  "blocks": [block("main", [
    slot("lat_pulldown", 4, 8, 10, rest=[120, 120], note=T("قبضة عريضة أو محايدة · عرض الظهر", "Wide or neutral grip · back width")),
    slot("chest_supported_row", 3, 8, 10, rest=[120, 120], note=T("سماكة الظهر، والصدر مسنود بيحمي أسفل الظهر", "Back thickness; the chest pad protects your lower back"), alts=["seated_cable_row"]),
    slot("back_extension", 3, 12, 15, rest=[90, 90], weeks=[1, 4], load="BODYWEIGHT", note=T("بوزن الجسم بمرحلة التأسيس", "Body weight during the foundation phase")),
    slot("back_extension", 3, 12, 15, rest=[90, 90], weeks=[5, 9], note=T("صحن على الصدر", "Hold a plate on your chest")),
    slot("weighted_dips", 3, 6, 10, rest=[120, 120], note=T("جسمك مستقيم للتراي. إذا أقل من 10 بوزن الجسم، بدون حزام لحد 3 × 10.", "Upright for the triceps. Under 10 body-weight reps? No belt until 3 × 10.")),
    slot("overhead_cable_ext", 3, 10, 12, rest=[60, 90], note=T("الرأس الطويل · بالحبل", "Long head · rope")),
 ])]},
 {"day": "TUESDAY", "blocks": MOBILITY},
 {"day": "WEDNESDAY", "notes": [T("Shrug و Farmer's Walk بدون Straps.", "Shrugs and farmer's walks without straps."),
                                 T("الرفرفة: 15 kg × 10 ثقيل على وزنك وغالباً فيه مرجحة والترابيس عم تاخد الشغل.", "Lateral raises: 15 kg × 10 is heavy for your size — usually swinging, with the traps taking over.")],
  "blocks": [block("main", [
    slot("seated_db_shoulder_press", 4, 6, 10, rest=[120, 120]),
    slot("db_lateral_raise", 4, 12, 15, rest=[60, 60], note=T("8-10 kg بفورم صارم، لمستوى الكتف، نزول 3 ثواني", "8–10 kg strict, to shoulder height, three seconds down")),
    slot("face_pull", 3, 15, rest=[60, 60], note=T("الكتف الخلفي، نص الترابيس، وصحة الكتف", "Rear delts, mid traps, shoulder health")),
    slot("db_shrug", 4, 10, 12, rest=[90, 90], note=T("ثانية تثبيت فوق", "One-second hold on top"), alts=["barbell_shrug"]),
    slot("farmers_walk", 3, 40, rest=[90, 90], note=T("دامبلز ثقيلة", "Heavy dumbbells")),
 ])]},
 {"day": "THURSDAY", "notes": [T("المجهود بالدائرة 7 من 10 بس، لأن بكرا يوم أرجل.", "Keep the circuit at 7/10 effort — tomorrow is leg day.")],
  "blocks": [
    block("jumps", [
        slot("snap_down", 3, 5, weeks=[1, 4]),
        slot("box_jump", 4, 4, weeks=[1, 4], note=T("Step Risers بارتفاع 30-40 cm: هبوط ناعم، ثبّت ثانيتين، انزل مشي", "Step risers at 30–40 cm: soft landing, hold two seconds, step down")),
        slot("box_jump", 4, 3, weeks=[5, 9], note=T("أعلى: 45-60 cm", "Higher: 45–60 cm")),
        slot("broad_jump", 4, 3, weeks=[5, 9], note=T("ثبّت الهبوط", "Stick the landing")),
    ], rest=(60, 90), title=T("A — القفز", "A — Jumps"), minutes=10, note=T("الجودة أهم من التعب", "Quality over fatigue")),
    block("sled", [
        slot("backward_sled_drag", 4, 20, 30, note=T("مشي لورا على مقدمة القدم", "Walk backwards on the balls of your feet")),
        slot("sled_push", 4, 20, note=T("ثقيل", "Heavy")),
    ], type="SUPERSET", rounds=4, rest=(90, 90), title=T("B — Sled", "B — Sled", "B — Sled"), minutes=15, note=T("التمرينين ورا بعض، وبعدهم راحة", "Both back to back, then rest")),
    block("circuit", [
        slot("kb_deadlift", 4, 10, weeks=[1, 2], note=T("بدل Tire Flip لحد ما تتقن حركة الورك", "Instead of tire flips until the hip hinge is solid")),
        slot("tire_flip", 4, 4, weeks=[3, 9]),
        slot("wall_ball", 4, 12, note=T("على الهدف", "To the target")),
        slot("kb_swing", 4, 15),
        slot("pull_up", 4, 6, 8, note=T("أو Weighted بالحزام × 5", "Or weighted with the belt × 5")),
        slot("sandbag_carry", 4, 40),
        slot("monkey_bars", 4, 1, note=T("طول واحد", "One length")),
    ], type="CIRCUIT", rounds=4, rest=(90, 120), title=T("C — دائرة التحمل", "C — Endurance circuit"), minutes=25, note=T("ورا بعض بدون راحة، والراحة بين الجولات", "Back to back; rest between rounds")),
    block("core", [
        slot("hanging_knee_raise", 3, 8, 12, note=T("بدون مرجحة. لما يصير سهل: Hanging Leg Raise", "No swinging. When it gets easy: hanging leg raise"), alts=["hanging_leg_raise"]),
        slot("dead_bug", 3, 8, note=T("لكل جهة · الكور العميق وحماية أسفل الظهر", "Per side · deep core, lower-back protection")),
        slot("copenhagen", 3, 15, 30, weeks=[1, 4], note=T("الركبة على الستيب (نسخة قصيرة)", "Knee on the step (short lever)")),
        slot("copenhagen", 3, 15, 30, weeks=[5, 9], note=T("القدم على الستيب (النسخة الكاملة)", "Foot on the step (full lever)")),
        slot("suitcase_carry", 3, 30, note=T("كتلبيل بإيد وحدة، لكل جهة", "Kettlebell in one hand, per side")),
    ], rest=(45, 60), title=T("D — البطن والكور", "D — Abs & core", "D — البطن والـ Core"), minutes=15),
 ]},
 {"day": "FRIDAY", "notes": [T("إذا رجليك لسا ثقيلة من الخميس: شيل جولة من أول تمرينين بدل ما تنزّل الوزن كثير.", "Legs still heavy from Thursday? Drop a set from the first two exercises instead of cutting the weight a lot."),
                              T("Bulgarian: إذا التوازن ما ساعدك بالأسبوع الأول، بوزن الجسم ومسك شي بإيد وحدة.", "Bulgarian: if balance is shaky in week one, use body weight and hold something with one hand.")],
  "blocks": [block("main", [
    slot("goblet_squat", 4, 10, 12, rest=[120, 120], weeks=[1, 4], note=T("الكعب على صحن 5-10 kg، نزول 3 ثواني ووقفة ثانية تحت", "Heels on a 5–10 kg plate, three seconds down, one-second pause")),
    slot("back_squat", 4, 6, 8, rest=[120, 180], weeks=[5, 9], note=T("نفس الهدف بوزن أثقل", "Same goal, heavier load")),
    slot("rdl", 3, 8, 10, rest=[120, 120], note=T("ابدأ بدامبلز خفيفة", "Start with light dumbbells"), alts=["db_rdl"]),
    slot("bulgarian_split_squat", 3, 8, 10, rest=[90, 90], note=T("لكل رجل", "Per leg")),
    slot("hip_adduction", 3, 12, 15, rest=[60, 90]),
    slot("seated_leg_curl", 3, 10, 12, rest=[60, 90], note=T("حماية الركبة", "Knee protection"), alts=["lying_leg_curl"]),
 ])]},
 {"day": "SATURDAY", "blocks": MOBILITY},
]

out = {"version": 1, "exercises": EX, "days": DAYS}
path = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "assets", "program", "program.json")
with open(path, "w", encoding="utf-8") as f:
    json.dump(out, f, ensure_ascii=False, indent=1)
ids = {e["id"] for e in EX}
for d in DAYS:
    for b in d["blocks"]:
        for s in b["slots"]:
            assert s["ex"] in ids, s["ex"]
            for a in s.get("alts", []): assert a in ids, a
print("exercises", len(EX), "→", os.path.normpath(path), os.path.getsize(path), "bytes")
