#!/usr/bin/env python3
"""
Food database for Hyper Athlete (brief §11) → app/src/main/assets/food/foods.json

Values are per 100 g (kcal, protein, carbs, fat). Plain foods follow USDA-style reference values;
home-cooked Arabic dishes are typical-recipe estimates (marked "dish": true so the app can say so).
Units are realistic servings with their weight in grams (صحن، نص صحن، رغيف، حبة، ملعقة، كوب …);
grams are always available as a unit too.
"""
import json, os, re

CATS = [
    ("breakfast", "فطور", "Breakfast"),
    ("bread", "خبز ونشويات", "Bread & starches"),
    ("dishes", "طبخات", "Home dishes"),
    ("grill", "مشاوي ولحوم وسمك", "Grills, meat & fish"),
    ("fastfood", "سندويشات وأكل سريع", "Sandwiches & fast food"),
    ("soups", "شوربات", "Soups"),
    ("salads", "سلطات ومقبلات", "Salads & mezze"),
    ("dairy", "ألبان وأجبان", "Dairy"),
    ("fruits", "فواكه", "Fruit"),
    ("vegetables", "خضار", "Vegetables"),
    ("nuts", "مكسرات وبذور", "Nuts & seeds"),
    ("sweets", "حلويات", "Sweets"),
    ("drinks", "مشروبات", "Drinks"),
    ("supplements", "مكملات", "Supplements"),
    ("basics", "أساسيات", "Basics"),
]

# unit helper: (id, ar, en, grams)
def U(id, ar, en, g):
    return {"id": id, "ar": ar, "en": en, "g": g}

PLATE = lambda g: U("plate", "صحن", "Plate", g)
HALF_PLATE = lambda g: U("half_plate", "نص صحن", "Half plate", g)
PIECE = lambda g, ar="حبة", en="Piece": U("piece", ar, en, g)
SLICE = lambda g, ar="شريحة", en="Slice": U("slice", ar, en, g)
TBSP = lambda g: U("tbsp", "ملعقة كبيرة", "Tablespoon", g)
TSP = lambda g: U("tsp", "ملعقة صغيرة", "Teaspoon", g)
CUP = lambda g: U("cup", "كوب", "Cup", g)
GLASS = lambda g: U("glass", "كاسة", "Glass", g)
BOWL = lambda g: U("bowl", "زبدية", "Bowl", g)
LOAF = lambda g: U("loaf", "رغيف", "Loaf", g)
HANDFUL = lambda g: U("handful", "حفنة", "Handful", g)
CAN = lambda g: U("can", "علبة", "Can", g)
SKEWER = lambda g: U("skewer", "سيخ", "Skewer", g)
SCOOP = lambda g: U("scoop", "سكوب", "Scoop", g)

FOODS = []


def F(id, cat, ar, en, kcal, p, c, f, units, default=(0, 1), addons=None, aliases="", dish=False):
    FOODS.append({
        "id": id, "cat": cat, "ar": ar, "en": en, "art": id,
        "per100": [kcal, p, c, f], "units": units,
        "defUnit": units[default[0]]["id"], "defQty": default[1],
        **({"addons": addons} if addons else {}),
        **({"aka": aliases} if aliases else {}),
        **({"dish": True} if dish else {}),
    })


# ---------------------------------------------------------------- breakfast
F("hummus", "breakfast", "حمص", "Hummus", 177, 8, 14, 10, [PLATE(200), HALF_PLATE(100), TBSP(15)], (1, 1), "hummus_ful", "حمص بطحينة hummus houmous", True)
F("ful", "breakfast", "فول مدمس", "Ful medames", 110, 6.5, 13, 3.5, [PLATE(250), HALF_PLATE(125), TBSP(15)], (1, 1), "hummus_ful", "فول foul ful medames", True)
F("falafel", "breakfast", "فلافل", "Falafel", 333, 13.3, 31.8, 17.8, [PIECE(17, "قرص", "Ball"), PLATE(150)], (0, 5), None, "فلافل طعمية falafel taameya")
F("msabbaha", "breakfast", "مسبّحة", "Msabbaha", 190, 8, 16, 11, [PLATE(250), HALF_PLATE(125)], (1, 1), "hummus_ful", "مسبحة مشط msabbaha", True)
F("fatteh_hummus", "breakfast", "فتة حمص", "Hummus fatteh", 200, 7, 20, 10, [PLATE(350), HALF_PLATE(175)], (1, 1), None, "فتة fatteh fatet hummus", True)
F("shakshuka", "breakfast", "شكشوكة", "Shakshuka", 110, 6, 6, 7, [PLATE(250), HALF_PLATE(125)], (0, 1), None, "شكشوكة بيض بالبندورة shakshouka", True)
F("egg_boiled", "breakfast", "بيض مسلوق", "Boiled egg", 155, 12.6, 1.1, 10.6, [PIECE(50, "بيضة", "Egg")], (0, 2), None, "بيضة مسلوقة egg boiled")
F("egg_fried", "breakfast", "بيض مقلي", "Fried egg", 200, 12, 0.8, 16, [PIECE(55, "بيضة", "Egg")], (0, 2), None, "بيضة مقلية egg fried")
F("ijjeh", "breakfast", "عجة", "Ijjeh omelette", 190, 9, 7, 14, [PIECE(60, "قرص", "Patty"), PLATE(180)], (0, 2), None, "عجه omelette omelet ijje", True)
F("labneh", "breakfast", "لبنة", "Labneh", 170, 7, 4.5, 14, [TBSP(20), PLATE(100)], (0, 2), None, "لبنه labneh labne")
F("white_cheese", "breakfast", "جبنة بيضاء (نابلسية / عكاوي)", "White cheese (Nabulsi / Akkawi)", 290, 20, 2, 22, [SLICE(30), PIECE(50, "قطعة", "Piece")], (0, 2), None, "جبنة بيضا نابلسية عكاوي akkawi nabulsi cheese")
F("halloumi", "breakfast", "حلوم", "Halloumi", 321, 21, 2.2, 25, [SLICE(30)], (0, 2), None, "حلومي halloumi")
F("olives", "breakfast", "زيتون", "Olives", 145, 1, 4, 15, [PIECE(4), PLATE(50)], (0, 10), None, "زيتون olives")
F("zaatar_oil", "breakfast", "زعتر وزيت", "Za'atar & olive oil", 700, 2, 10, 72, [U("dip", "تغميسة (ملعقة زعتر + ملعقة زيت)", "Dip (zaatar + oil spoon)", 18)], (0, 2), None, "زعتر وزيت zaatar zaatar w zeit")
F("manakish_zaatar", "breakfast", "مناقيش زعتر", "Manakish za'atar", 300, 7, 38, 13, [PIECE(130, "قطعة", "Piece"), U("half", "نص قطعة", "Half", 65)], (0, 1), "manakish", "مناقيش زعتر منقوشة manakish manaeesh manakeesh zaatar", True)
F("manakish_cheese", "breakfast", "مناقيش جبنة", "Cheese manakish", 300, 12, 33, 13, [PIECE(150, "قطعة", "Piece"), U("half", "نص قطعة", "Half", 75)], (0, 1), "manakish", "مناقيش جبنة منقوشة جبنة manakish cheese", True)
F("manakish_muhammara", "breakfast", "مناقيش محمرة", "Muhammara manakish", 310, 7, 38, 14, [PIECE(130, "قطعة", "Piece"), U("half", "نص قطعة", "Half", 65)], (0, 1), "manakish", "محمره منقوشة محمرة muhammara", True)
F("fatayer_spinach", "breakfast", "فطاير سبانخ", "Spinach fatayer", 260, 6, 32, 12, [PIECE(70)], (0, 2), None, "فطاير فطيرة سبانخ fatayer spinach", True)
F("sfiha", "breakfast", "صفيحة", "Sfiha", 280, 11, 28, 14, [PIECE(60)], (0, 3), None, "صفيحه لحم بعجين sfiha sfeeha", True)
F("kaak", "breakfast", "كعك بالسمسم", "Sesame ka'ak", 330, 11, 55, 7, [PIECE(120, "كعكة", "Ka'ak"), U("half", "نص كعكة", "Half", 60)], (0, 1), None, "كعك سمسم كعكة kaak kaek")
F("halawa", "breakfast", "حلاوة طحينية", "Halawa (tahini halva)", 520, 12, 56, 28, [TBSP(20), PIECE(40, "قطعة", "Piece")], (0, 2), None, "حلاوه halawa halva")
F("qashta", "breakfast", "قشطة", "Qashta (clotted cream)", 260, 4, 6, 24, [TBSP(20)], (0, 2), None, "قشطه ashta qashta cream")
F("oats", "breakfast", "شوفان", "Oats", 379, 13, 68, 6.5, [CUP(80), U("half_cup", "نص كوب", "Half cup", 40), TBSP(10)], (1, 1), "oats", "شوفان oats oatmeal")
F("corn_flakes", "breakfast", "كورن فليكس", "Corn flakes", 357, 7.5, 84, 0.4, [CUP(30), PLATE(45)], (1, 1), "oats", "كورن فلكس حبوب cornflakes cereal")
F("croissant", "breakfast", "كرواسون", "Croissant", 406, 8.2, 45.8, 21, [PIECE(60)], (0, 1), None, "كرواسان croissant")
F("croissant_choc", "breakfast", "كرواسون شوكولاتة", "Chocolate croissant", 420, 7, 45, 23, [PIECE(65)], (0, 1), None, "كرواسان شوكولا pain au chocolat")
F("qalayet_bandora", "breakfast", "قلاية بندورة", "Tomato qalayet", 90, 1.5, 7, 6.5, [PLATE(200), HALF_PLATE(100)], (0, 1), None, "قلاية قلايه بندورة galayet bandora", True)
F("cheese_triangles", "breakfast", "جبنة مثلثات", "Cheese triangles", 240, 9, 6, 20, [PIECE(17, "مثلث", "Triangle")], (0, 2), None, "مثلثات جبنة كيري triangles")
F("mortadella", "breakfast", "مرتديلا", "Mortadella", 311, 15, 3, 26, [SLICE(15)], (0, 3), None, "مرتديلا مورتديلا mortadella")

# ---------------------------------------------------------------- bread & starches
F("bread_arabic", "bread", "خبز عربي", "Arabic bread (pita)", 275, 9.1, 55.7, 1.2, [LOAF(90), U("small_loaf", "رغيف صغير", "Small loaf", 60), U("half_loaf", "نص رغيف", "Half loaf", 45)], (0, 1), None, "خبز كماج عربي pita bread khubz")
F("bread_taboon", "bread", "خبز طابون", "Taboon bread", 270, 9, 54, 1.8, [LOAF(220), U("half_loaf", "نص رغيف", "Half loaf", 110), U("quarter_loaf", "ربع رغيف", "Quarter loaf", 55)], (1, 1), None, "طابون taboon")
F("bread_shrak", "bread", "خبز شراك", "Shrak bread", 275, 9, 56, 1.5, [LOAF(120), U("half_loaf", "نص رغيف", "Half loaf", 60)], (0, 1), None, "شراك مرقوق صاج shrak saj")
F("bread_brown", "bread", "خبز أسمر", "Brown bread", 250, 10, 47, 2.5, [LOAF(80), U("half_loaf", "نص رغيف", "Half loaf", 40)], (0, 1), None, "اسمر نخالة بر brown bread wholemeal")
F("toast_white", "bread", "توست أبيض", "White toast", 265, 9, 49, 3.2, [SLICE(28)], (0, 2), None, "توست ابيض toast")
F("toast_brown", "bread", "توست أسمر", "Brown toast", 247, 13, 41, 3.4, [SLICE(30)], (0, 2), None, "توست اسمر toast brown")
F("rice_white", "bread", "رز أبيض", "White rice", 150, 2.7, 28, 3, [PLATE(250), HALF_PLATE(125), CUP(160), TBSP(20)], (0, 1), "rice", "رز ارز ابيض rice")
F("rice_basmati", "bread", "رز بسمتي", "Basmati rice", 145, 3.4, 26, 2.8, [PLATE(250), HALF_PLATE(125), CUP(160)], (0, 1), "rice", "بسمتي basmati rice")
F("rice_vermicelli", "bread", "رز بالشعيرية", "Rice with vermicelli", 165, 3, 28, 4.5, [PLATE(250), HALF_PLATE(125), CUP(160)], (0, 1), "rice", "شعيرية رز vermicelli rice", True)
F("bulgur", "bread", "برغل", "Bulgur", 110, 3.1, 19, 2.4, [PLATE(250), HALF_PLATE(125), CUP(180)], (0, 1), "rice", "برغل bulgur burghul")
F("freekeh", "bread", "فريكة", "Freekeh", 135, 4.2, 23, 3, [PLATE(250), HALF_PLATE(125)], (0, 1), "rice", "فريكه freekeh", True)
F("pasta", "bread", "معكرونة", "Pasta", 158, 5.8, 31, 0.9, [PLATE(250), HALF_PLATE(125), CUP(140)], (0, 1), "pasta", "معكرونه مكرونة باستا pasta macaroni spaghetti")
F("potato_boiled", "bread", "بطاطا مسلوقة", "Boiled potato", 87, 1.9, 20, 0.1, [PIECE(150), PLATE(200)], (0, 1), "potato", "بطاطا مسلوقه potato boiled")
F("fries", "bread", "بطاطا مقلية", "French fries", 312, 3.4, 41, 15, [PLATE(150), HALF_PLATE(75)], (0, 1), "potato", "بطاطا مقلية شيبس فرايز fries chips")
F("sweet_potato", "bread", "بطاطا حلوة", "Sweet potato", 90, 2, 20.7, 0.2, [PIECE(150)], (0, 1), None, "بطاطا حلوه sweet potato")
F("corn", "bread", "ذرة", "Sweet corn", 96, 3.4, 21, 1.5, [U("cob", "كوز", "Cob", 100), CUP(150)], (0, 1), None, "ذره عرنوس corn")

# ---------------------------------------------------------------- home dishes
F("mansaf", "dishes", "منسف", "Mansaf", 200, 10, 15, 11, [PLATE(450), HALF_PLATE(225)], (0, 1), "mansaf", "منسف mansaf", True)
F("maqluba", "dishes", "مقلوبة", "Maqluba", 170, 8, 18, 7.5, [PLATE(400), HALF_PLATE(200)], (0, 1), None, "مقلوبه maqluba maqloubeh", True)
F("musakhan", "dishes", "مسخن", "Musakhan", 260, 12, 20, 15, [PIECE(400, "قطعة (رغيف + ربع دجاجة)", "Piece (loaf + ¼ chicken)"), U("half", "نص قطعة", "Half", 200), U("roll", "رول مسخن", "Musakhan roll", 90)], (0, 1), None, "مسخن مسخّن musakhan", True)
F("kabsa", "dishes", "كبسة", "Kabsa", 165, 9, 20, 5.5, [PLATE(400), HALF_PLATE(200)], (0, 1), None, "كبسه kabsa kabseh", True)
F("mandi", "dishes", "مندي", "Mandi", 175, 10, 19, 6.5, [PLATE(400), HALF_PLATE(200)], (0, 1), None, "مندي mandi", True)
F("zarb", "dishes", "زرب", "Zarb", 180, 15, 8, 10, [PLATE(350), HALF_PLATE(175)], (0, 1), None, "زرب zarb", True)
F("qidreh", "dishes", "قدرة", "Qidreh", 190, 9, 20, 8.5, [PLATE(400), HALF_PLATE(200)], (0, 1), None, "قدره قدرة خليلية qidreh", True)
F("dawali_meat", "dishes", "ورق عنب بلحمة", "Stuffed vine leaves (meat)", 180, 7, 18, 9, [PIECE(25), PLATE(300)], (0, 10), None, "ورق عنب دوالي يبرق warak enab dawali", True)
F("dawali_veg", "dishes", "ورق عنب صيامي", "Stuffed vine leaves (veg)", 160, 2.5, 20, 8, [PIECE(20), PLATE(250)], (0, 10), None, "ورق عنب دوالي صيامي warak enab vegetarian", True)
F("kousa_mahshi", "dishes", "كوسا محشي", "Stuffed zucchini", 105, 5, 10, 5, [PIECE(120), PLATE(360)], (0, 3), None, "كوسا محشي kousa mahshi", True)
F("batinjan_mahshi", "dishes", "باذنجان محشي", "Stuffed eggplant", 115, 5, 10, 6, [PIECE(130), PLATE(390)], (0, 2), None, "باذنجان محشي بتنجان eggplant stuffed", True)
F("filfil_mahshi", "dishes", "فلفل محشي", "Stuffed peppers", 110, 5, 11, 5, [PIECE(150), PLATE(300)], (0, 2), None, "فلفل محشي stuffed peppers", True)
F("malfouf_mahshi", "dishes", "ملفوف محشي", "Stuffed cabbage", 120, 5, 15, 4.5, [PIECE(40), PLATE(300)], (0, 6), None, "ملفوف محشي cabbage rolls malfouf", True)
F("mulukhiyah", "dishes", "ملوخية", "Mulukhiyah", 70, 6, 4, 3.5, [PLATE(300), HALF_PLATE(150)], (0, 1), "stew", "ملوخيه molokhia mulukhiyah", True)
F("bamia", "dishes", "بامية", "Okra stew", 90, 6, 6, 5, [PLATE(300), HALF_PLATE(150)], (0, 1), "stew", "باميه bamia okra", True)
F("fasolia", "dishes", "فاصولياء خضراء", "Green bean stew", 85, 5, 7, 4.5, [PLATE(300), HALF_PLATE(150)], (0, 1), "stew", "فاصوليا خضرا fasolia green beans", True)
F("bazella", "dishes", "بازيلاء", "Peas & carrot stew", 105, 6, 10, 4.5, [PLATE(300), HALF_PLATE(150)], (0, 1), "stew", "بازيلا بازلاء peas bazella", True)
F("mujaddara", "dishes", "مجدرة", "Mujaddara", 150, 6, 22, 4.5, [PLATE(300), HALF_PLATE(150)], (0, 1), None, "مجدره مجدّرة mujaddara mujadara", True)
F("sayadieh", "dishes", "صيادية", "Sayadieh", 165, 10, 18, 5.5, [PLATE(400), HALF_PLATE(200)], (0, 1), None, "صياديه sayadieh fish rice", True)
F("fatteh_chicken", "dishes", "فتة دجاج", "Chicken fatteh", 185, 10, 18, 8, [PLATE(400), HALF_PLATE(200)], (0, 1), None, "فتة دجاج fatteh chicken", True)
F("kibbeh_fried", "dishes", "كبة مقلية", "Fried kibbeh", 265, 12, 22, 14, [PIECE(60)], (0, 3), None, "كبه مقليه kibbeh kubbeh", True)
F("kibbeh_tray", "dishes", "كبة بالصينية", "Baked kibbeh", 230, 13, 18, 12, [PIECE(150, "قطعة", "Piece")], (0, 1), None, "كبة صينية kibbeh tray", True)
F("kibbeh_labaniyeh", "dishes", "كبة لبنية", "Kibbeh in yogurt", 140, 7, 10, 8, [PLATE(300), HALF_PLATE(150)], (0, 1), None, "كبه لبنيه kibbeh labanieh", True)
F("kofta_tray", "dishes", "كفتة بالصينية", "Kofta tray", 145, 8, 9, 9, [PLATE(350), HALF_PLATE(175)], (0, 1), None, "كفته صينية kofta tray", True)

# ---------------------------------------------------------------- grills, meat & fish
F("shish_tawook", "grill", "شيش طاووق", "Shish tawook", 175, 25, 2.5, 7, [SKEWER(100), PLATE(250)], (0, 2), None, "شيش طاووق طاووق shish tawook")
F("kebab", "grill", "كباب", "Kebab", 250, 18, 2, 19, [SKEWER(90)], (0, 2), None, "كباب kebab kabab")
F("kofta_grilled", "grill", "كفتة مشوية", "Grilled kofta", 240, 18, 3, 17, [SKEWER(80)], (0, 2), None, "كفته مشويه kofta kufta")
F("chicken_grilled", "grill", "دجاج مشوي", "Grilled chicken", 215, 26, 0, 12, [U("quarter", "ربع دجاجة", "Quarter chicken", 180), U("half", "نص دجاجة", "Half chicken", 360)], (0, 1), None, "جاج مشوي فروج chicken grilled")
F("chicken_breast", "grill", "صدر دجاج مشوي", "Grilled chicken breast", 165, 31, 0, 3.6, [PIECE(150, "صدر", "Breast"), U("100g", "100 غ", "100 g", 100)], (0, 1), None, "صدر جاج دجاج chicken breast")
F("chicken_thigh", "grill", "فخذ دجاج", "Chicken thigh", 209, 26, 0, 10.9, [PIECE(100, "فخذ", "Thigh")], (0, 1), None, "فخذ جاج chicken thigh")
F("steak", "grill", "ستيك", "Steak", 250, 27, 0, 15, [PIECE(200, "قطعة", "Piece")], (0, 1), None, "ستيك steak beef")
F("beef_mince", "grill", "لحمة مفرومة", "Ground beef", 250, 26, 0, 15.5, [U("100g", "100 غ", "100 g", 100)], (0, 1), None, "لحمة مفرومة ground beef mince")
F("lamb", "grill", "لحم غنم", "Lamb", 294, 25, 0, 21, [PIECE(100, "قطعة", "Piece")], (0, 1), None, "لحم خروف غنم lamb")
F("fish_grilled", "grill", "سمك مشوي", "Grilled fish", 130, 22, 0, 4.5, [PIECE(150, "قطعة", "Piece"), U("fish", "سمكة", "Whole fish", 300)], (0, 1), None, "سمك مشوي fish grilled")
F("fish_fried", "grill", "سمك مقلي", "Fried fish", 230, 20, 8, 13, [PIECE(150, "قطعة", "Piece")], (0, 1), None, "سمك مقلي fish fried")
F("shrimp", "grill", "جمبري", "Shrimp", 140, 22, 1, 5, [PLATE(150), U("100g", "100 غ", "100 g", 100)], (0, 1), None, "جمبري قريدس روبيان shrimp prawns")
F("tuna_water", "grill", "تونة بالماء", "Tuna in water", 116, 25.5, 0, 0.8, [CAN(120)], (0, 1), None, "تونه بالماء tuna")
F("tuna_oil", "grill", "تونة بالزيت", "Tuna in oil", 198, 29, 0, 8.2, [CAN(120)], (0, 1), None, "تونه بالزيت tuna oil")
F("sardines", "grill", "سردين", "Sardines", 208, 24.6, 0, 11.5, [CAN(90)], (0, 1), None, "سردين sardines")

# ---------------------------------------------------------------- sandwiches & fast food
F("shawarma_chicken", "fastfood", "شاورما دجاج (ساندويش)", "Chicken shawarma sandwich", 225, 12, 23, 9.5, [U("small", "ساندويش صغير", "Small sandwich", 180), U("large", "ساندويش كبير", "Large sandwich", 300)], (1, 1), "sandwich", "شاورما شورما دجاج جاج shawarma chicken", True)
F("shawarma_chicken_meat", "fastfood", "شاورما دجاج (لحمة فقط)", "Chicken shawarma (meat only)", 210, 22, 3, 12, [PLATE(200), U("100g", "100 غ", "100 g", 100)], (0, 1), None, "صحن شاورما دجاج shawarma plate", True)
F("shawarma_meat", "fastfood", "شاورما لحمة (ساندويش)", "Beef shawarma sandwich", 245, 13, 22, 11.5, [U("small", "ساندويش صغير", "Small sandwich", 180), U("large", "ساندويش كبير", "Large sandwich", 300)], (1, 1), "sandwich", "شاورما لحمة لحم shawarma meat beef", True)
F("shawarma_meat_only", "fastfood", "شاورما لحمة (لحمة فقط)", "Beef shawarma (meat only)", 245, 20, 3, 17, [PLATE(200), U("100g", "100 غ", "100 g", 100)], (0, 1), None, "صحن شاورما لحمة shawarma beef plate", True)
F("falafel_sandwich", "fastfood", "ساندويش فلافل", "Falafel sandwich", 230, 7, 30, 9, [U("sandwich", "ساندويش", "Sandwich", 200)], (0, 1), "sandwich", "ساندويش فلافل falafel wrap", True)
F("zinger", "fastfood", "زنجر", "Zinger sandwich", 260, 12, 25, 12.5, [U("sandwich", "ساندويش", "Sandwich", 220)], (0, 1), "sandwich", "زنجر zinger", True)
F("broasted", "fastfood", "بروستد", "Broasted chicken", 270, 20, 10, 17, [PIECE(110, "قطعة", "Piece"), U("meal", "وجبة 4 قطع", "4-piece meal", 440)], (0, 2), None, "بروستد بروست broasted fried chicken", True)
F("burger", "fastfood", "برغر", "Burger", 250, 13, 22, 12, [U("burger", "برغر", "Burger", 220), U("double", "دبل برغر", "Double burger", 330)], (0, 1), "burger", "برجر همبرغر burger", True)
F("pizza", "fastfood", "بيتزا", "Pizza", 266, 11, 33, 10, [SLICE(110), U("medium", "بيتزا وسط", "Medium pizza", 660)], (0, 2), None, "بيتزا pizza", True)

# ---------------------------------------------------------------- soups
F("soup_lentil", "soups", "شوربة عدس", "Lentil soup", 70, 3.8, 10, 1.8, [BOWL(300)], (0, 1), None, "شوربه عدس lentil soup", True)
F("soup_veg", "soups", "شوربة خضار", "Vegetable soup", 40, 1.5, 6, 1.2, [BOWL(300)], (0, 1), None, "شوربه خضار vegetable soup", True)
F("soup_freekeh", "soups", "شوربة فريكة", "Freekeh soup", 65, 4.5, 8, 1.8, [BOWL(300)], (0, 1), None, "شوربه فريكه freekeh soup", True)
F("soup_chicken", "soups", "شوربة دجاج", "Chicken soup", 45, 3.5, 4, 1.5, [BOWL(300)], (0, 1), None, "شوربه جاج دجاج chicken soup", True)

# ---------------------------------------------------------------- salads & mezze
F("fattoush", "salads", "فتوش", "Fattoush", 110, 2, 11, 7, [PLATE(250), HALF_PLATE(125)], (1, 1), None, "فتوش fattoush", True)
F("tabbouleh", "salads", "تبولة", "Tabbouleh", 120, 2.5, 10, 8, [PLATE(200), HALF_PLATE(100)], (1, 1), None, "تبوله tabbouleh tabouli", True)
F("salad_arabic", "salads", "سلطة عربية", "Arabic salad", 60, 1, 5, 4, [PLATE(200), HALF_PLATE(100)], (1, 1), "salad", "سلطه عربيه salad", True)
F("salad_green", "salads", "سلطة خضراء", "Green salad", 20, 1.3, 3.5, 0.2, [PLATE(150)], (0, 1), "salad", "سلطه خضرا green salad")
F("salad_caesar", "salads", "سلطة سيزر", "Caesar salad", 160, 6, 8, 12, [PLATE(250)], (0, 1), "salad", "سيزر caesar salad", True)
F("salad_greek", "salads", "سلطة يونانية", "Greek salad", 110, 3.5, 5, 9, [PLATE(250)], (0, 1), "salad", "يونانيه greek salad", True)
F("baba_ghanoush", "salads", "بابا غنوج", "Baba ghanoush", 120, 2.5, 8, 9, [PLATE(120), TBSP(15)], (0, 1), None, "بابا غنوج baba ghanouj", True)
F("mutabbal", "salads", "متبل", "Mutabbal", 130, 3, 7, 10, [PLATE(120), TBSP(15)], (0, 1), None, "متبّل متبل mutabal moutabal", True)
F("salad_tahini", "salads", "سلطة طحينة", "Tahini salad", 250, 7, 9, 21, [TBSP(20), PLATE(100)], (0, 2), None, "سلطة طحينة طراطور tahini salad", True)
F("pickles", "salads", "مخللات", "Pickles", 15, 0.5, 2.5, 0.2, [PLATE(50), PIECE(30)], (0, 1), None, "مخلل كبيس pickles")

# ---------------------------------------------------------------- dairy
F("milk_full", "dairy", "حليب كامل الدسم", "Whole milk", 61, 3.2, 4.8, 3.3, [CUP(250)], (0, 1), None, "حليب milk")
F("milk_low", "dairy", "حليب قليل الدسم", "Low-fat milk", 46, 3.4, 4.9, 1.5, [CUP(250)], (0, 1), None, "حليب قليل الدسم خالي milk low fat")
F("laban", "dairy", "لبن رائب", "Yogurt (laban)", 61, 3.5, 4.7, 3.3, [CUP(200), PLATE(250)], (0, 1), "yogurt", "لبن زبادي رائب laban yogurt")
F("ayran", "dairy", "عيران", "Ayran", 37, 1.7, 2.7, 2, [GLASS(250)], (0, 1), None, "عيران شنينة ayran")
F("greek_yogurt", "dairy", "زبادي يوناني", "Greek yogurt", 73, 9.9, 3.9, 2, [PIECE(170, "علبة", "Tub")], (0, 1), "yogurt", "يوناني زبادي greek yogurt")
F("cheddar", "dairy", "جبنة شيدر", "Cheddar", 403, 25, 1.3, 33, [SLICE(20)], (0, 2), None, "شيدر cheddar")
F("mozzarella", "dairy", "موزاريلا", "Mozzarella", 280, 28, 3.1, 17, [SLICE(28, "قطعة", "Piece")], (0, 2), None, "موزريلا mozzarella")

# ---------------------------------------------------------------- fruit
F("banana", "fruits", "موز", "Banana", 89, 1.1, 22.8, 0.3, [PIECE(118)], (0, 1), None, "موزة banana")
F("apple", "fruits", "تفاح", "Apple", 52, 0.3, 13.8, 0.2, [PIECE(182)], (0, 1), None, "تفاحة apple")
F("orange", "fruits", "برتقال", "Orange", 47, 0.9, 11.8, 0.1, [PIECE(130)], (0, 1), None, "برتقانة برتقالة orange")
F("strawberry", "fruits", "فراولة", "Strawberries", 32, 0.7, 7.7, 0.3, [PIECE(12), CUP(150)], (1, 1), None, "فراوله توت strawberry")
F("grapes", "fruits", "عنب", "Grapes", 69, 0.7, 18, 0.2, [CUP(150), U("bunch", "عنقود صغير", "Small bunch", 150)], (0, 1), None, "عنب grapes")
F("watermelon", "fruits", "بطيخ", "Watermelon", 30, 0.6, 7.6, 0.2, [SLICE(280), CUP(150)], (0, 1), None, "بطيخ احمر watermelon")
F("melon", "fruits", "شمام", "Cantaloupe", 34, 0.8, 8.2, 0.2, [SLICE(100), CUP(160)], (0, 2), None, "شمام بطيخ اصفر cantaloupe melon")
F("mango", "fruits", "مانجا", "Mango", 60, 0.8, 15, 0.4, [PIECE(200)], (0, 1), None, "مانجو منجا mango")
F("dates", "fruits", "تمر", "Dates", 282, 2.5, 75, 0.4, [PIECE(10), U("medjool", "تمرة مجهول", "Medjool date", 24)], (0, 3), None, "تمر تمرات بلح dates")
F("figs", "fruits", "تين", "Figs", 74, 0.8, 19, 0.3, [PIECE(50)], (0, 2), None, "تين figs")
F("pomegranate", "fruits", "رمان", "Pomegranate", 83, 1.7, 18.7, 1.2, [CUP(175)], (0, 1), None, "رمانة رمان pomegranate")
F("pineapple", "fruits", "أناناس", "Pineapple", 50, 0.5, 13, 0.1, [SLICE(85), CUP(165)], (0, 2), None, "اناناس pineapple")
F("peach", "fruits", "خوخ", "Peach", 39, 0.9, 9.5, 0.3, [PIECE(150)], (0, 1), None, "خوخ دراق peach")
F("kiwi", "fruits", "كيوي", "Kiwi", 61, 1.1, 14.7, 0.5, [PIECE(75)], (0, 1), None, "كيوي kiwi")
F("apricot", "fruits", "مشمش", "Apricot", 48, 1.4, 11, 0.4, [PIECE(35)], (0, 3), None, "مشمش apricot")
F("avocado", "fruits", "أفوكادو", "Avocado", 160, 2, 8.5, 14.7, [U("half", "نص حبة", "Half", 100), PIECE(200)], (0, 1), None, "افوكادو avocado")

# ---------------------------------------------------------------- vegetables
F("cucumber", "vegetables", "خيار", "Cucumber", 15, 0.7, 3.6, 0.1, [PIECE(100)], (0, 1), None, "خيار خيارة cucumber")
F("tomato", "vegetables", "بندورة", "Tomato", 18, 0.9, 3.9, 0.2, [PIECE(120)], (0, 1), None, "بندوره طماطم tomato")
F("lettuce", "vegetables", "خس", "Lettuce", 15, 1.4, 2.9, 0.2, [CUP(50)], (0, 1), None, "خس lettuce")
F("carrot", "vegetables", "جزر", "Carrot", 41, 0.9, 9.6, 0.2, [PIECE(60)], (0, 1), None, "جزرة جزر carrot")
F("broccoli", "vegetables", "بروكلي", "Broccoli", 34, 2.8, 6.6, 0.4, [CUP(90)], (0, 1), None, "بروكلي brokoli broccoli")
F("spinach", "vegetables", "سبانخ", "Spinach", 23, 2.9, 3.6, 0.4, [CUP(30)], (0, 1), None, "سبانخ spinach")

# ---------------------------------------------------------------- nuts & seeds
F("almonds", "nuts", "لوز", "Almonds", 579, 21, 22, 50, [HANDFUL(28), PIECE(1.2)], (0, 1), None, "لوز almonds")
F("walnuts", "nuts", "جوز", "Walnuts", 654, 15, 14, 65, [HANDFUL(28)], (0, 1), None, "جوز عين جمل walnuts")
F("cashews", "nuts", "كاجو", "Cashews", 553, 18, 30, 44, [HANDFUL(28)], (0, 1), None, "كاجو cashew")
F("pistachio", "nuts", "فستق حلبي", "Pistachios", 560, 20, 28, 45, [HANDFUL(28)], (0, 1), None, "فستق حلبي pistachio")
F("peanuts", "nuts", "فستق سوداني", "Peanuts", 585, 24, 21, 50, [HANDFUL(28)], (0, 1), None, "فستق سوداني فول سوداني peanuts")
F("seeds", "nuts", "بزر", "Seeds (bizr)", 580, 22, 18, 50, [U("handful_shell", "حفنة بالقشر", "Handful (in shell)", 20)], (0, 1), None, "بزر قضامة بذور seeds bizr")
F("peanut_butter", "nuts", "زبدة فول سوداني", "Peanut butter", 588, 25, 20, 50, [TBSP(16)], (0, 2), None, "زبدة فستق peanut butter")
F("tahini", "nuts", "طحينة", "Tahini", 595, 17, 21, 54, [TBSP(15)], (0, 1), None, "طحينه tahini")

# ---------------------------------------------------------------- sweets
F("knafeh", "sweets", "كنافة نابلسية", "Knafeh", 330, 8, 40, 16, [PIECE(150, "قطعة", "Piece"), PLATE(200)], (0, 1), None, "كنافه نابلسية knafeh kunafa", True)
F("qatayef", "sweets", "قطايف", "Qatayef", 330, 6, 45, 14, [PIECE(50)], (0, 3), None, "قطايف قطائف qatayef atayef", True)
F("baklava", "sweets", "بقلاوة", "Baklava", 428, 6.7, 37.6, 29, [PIECE(30, "قطعة", "Piece")], (0, 3), None, "بقلاوه baklava", True)
F("harissa", "sweets", "هريسة", "Harissa (basbousa)", 370, 5, 60, 12, [PIECE(60, "قطعة", "Piece")], (0, 2), None, "هريسه بسبوسة harissa basbousa", True)
F("awameh", "sweets", "عوامة", "Awameh", 370, 4, 55, 15, [PIECE(12), PLATE(120)], (0, 6), None, "عوامه لقيمات awameh", True)
F("maamoul", "sweets", "معمول", "Maamoul", 450, 6, 63, 19, [PIECE(35)], (0, 2), None, "معمول maamoul", True)
F("um_ali", "sweets", "أم علي", "Um Ali", 230, 6, 26, 11, [PLATE(250)], (0, 1), None, "ام علي um ali", True)
F("muhallabia", "sweets", "مهلبية", "Muhallabia", 120, 3.5, 18, 3.5, [GLASS(150)], (0, 1), None, "مهلبيه muhallabia", True)
F("rice_pudding", "sweets", "رز بحليب", "Rice pudding", 130, 3.5, 22, 3, [GLASS(180)], (0, 1), None, "رز بحليب rice pudding", True)
F("cake", "sweets", "كيك", "Cake", 380, 5.5, 52, 16, [SLICE(70, "قطعة", "Piece")], (0, 1), None, "كيكة كيك cake")
F("pancakes", "sweets", "بان كيك", "Pancakes", 227, 6.4, 28, 9.7, [PIECE(40)], (0, 3), "sweet_toppings", "بانكيك pancake pancakes")
F("crepe", "sweets", "كريب", "Crepe", 200, 6.5, 25, 8, [PIECE(60)], (0, 1), "sweet_toppings", "كريب crepe")
F("waffle", "sweets", "وافل", "Waffle", 291, 7.9, 32.9, 14, [PIECE(75)], (0, 1), "sweet_toppings", "وافل waffle")
F("donut", "sweets", "دونات", "Donut", 421, 4.6, 51, 22.8, [PIECE(60)], (0, 1), None, "دونتس دونات donut doughnut")
F("chocolate", "sweets", "شوكولاتة", "Chocolate", 535, 7.7, 59, 30, [U("bar", "لوح صغير", "Small bar", 40), PIECE(10, "مربع", "Square")], (0, 1), None, "شوكولاته شوكلت chocolate")
F("ice_cream", "sweets", "آيس كريم", "Ice cream", 207, 3.5, 23.6, 11, [SCOOP(65), GLASS(130)], (0, 2), None, "بوظة ايس كريم ice cream")
F("biscuits", "sweets", "بسكويت", "Biscuits", 470, 7, 66, 20, [PIECE(12)], (0, 3), None, "بسكوت بسكويت biscuits cookies")

# ---------------------------------------------------------------- drinks
F("tea", "drinks", "شاي", "Tea", 1, 0, 0.2, 0, [GLASS(200)], (0, 1), "hot_drink", "شاي tea")
F("coffee_arabic", "drinks", "قهوة عربية", "Arabic coffee", 2, 0.1, 0.3, 0, [U("finjan", "فنجان", "Small cup", 50)], (0, 2), None, "قهوه عربيه سادة arabic coffee qahwa")
F("coffee_turkish", "drinks", "قهوة تركية", "Turkish coffee", 7, 0.3, 1, 0.2, [U("finjan", "فنجان", "Cup", 60)], (0, 1), "hot_drink", "قهوه تركي turkish coffee")
F("espresso", "drinks", "إسبريسو", "Espresso", 9, 0.1, 1.7, 0.2, [U("shot", "شوت", "Shot", 30)], (0, 1), "hot_drink", "اسبريسو espresso")
F("cappuccino", "drinks", "كابتشينو", "Cappuccino", 45, 2.5, 3.8, 2.3, [CUP(240)], (0, 1), "hot_drink", "كابتشينو كابوتشينو cappuccino")
F("latte", "drinks", "لاتيه", "Latte", 55, 3, 4.5, 2.8, [CUP(350)], (0, 1), "hot_drink", "لاتيه لاتي latte")
F("orange_juice", "drinks", "عصير برتقال", "Orange juice", 45, 0.7, 10.4, 0.2, [GLASS(250)], (0, 1), None, "عصير برتقال orange juice")
F("lemon_mint", "drinks", "ليمون بالنعناع", "Lemon mint", 45, 0.2, 11.5, 0, [GLASS(300)], (0, 1), None, "ليمون نعنع lemonade mint")
F("soda", "drinks", "مشروب غازي", "Soft drink", 42, 0, 10.6, 0, [CAN(330)], (0, 1), None, "بيبسي كولا غازي soda cola pepsi")
F("energy_drink", "drinks", "مشروب طاقة", "Energy drink", 45, 0, 11, 0, [CAN(250)], (0, 1), None, "مشروب طاقة ريد بول energy drink")
F("sahlab", "drinks", "سحلب", "Sahlab", 110, 3.5, 17, 3.3, [CUP(250)], (0, 1), None, "سحلب sahlab salep")

# ---------------------------------------------------------------- supplements
F("whey", "supplements", "Whey Protein", "Whey protein", 400, 78, 8, 6, [SCOOP(30)], (0, 1), None, "واي بروتين whey protein")
F("creatine", "supplements", "Creatine", "Creatine", 0, 0, 0, 0, [TSP(5)], (0, 1), None, "كرياتين creatine")
F("mass_gainer", "supplements", "Mass Gainer", "Mass gainer", 380, 15, 75, 2.5, [SCOOP(50), U("serving", "حصة (3 سكوبات)", "Serving (3 scoops)", 150)], (1, 1), None, "ماس جينر gainer")
F("protein_bar", "supplements", "Protein Bar", "Protein bar", 350, 33, 35, 12, [PIECE(60)], (0, 1), None, "بروتين بار protein bar")

# ---------------------------------------------------------------- basics
F("olive_oil", "basics", "زيت زيتون", "Olive oil", 884, 0, 0, 100, [TBSP(13.5), TSP(4.5)], (0, 1), None, "زيت زيتون olive oil")
F("ghee", "basics", "سمنة / زبدة", "Ghee / butter", 717, 0.9, 0.1, 81, [TBSP(14)], (0, 1), None, "سمنه زبدة ghee butter")
F("honey", "basics", "عسل", "Honey", 304, 0.3, 82.4, 0, [TBSP(21), TSP(7)], (0, 1), None, "عسل honey")
F("sugar", "basics", "سكر", "Sugar", 387, 0, 100, 0, [TSP(4)], (0, 2), None, "سكر sugar")
F("jam", "basics", "مربى", "Jam", 278, 0.4, 69, 0.1, [TBSP(20)], (0, 1), None, "مربى jam")
F("mayo", "basics", "مايونيز", "Mayonnaise", 680, 1, 0.6, 75, [TBSP(14)], (0, 1), None, "مايونيز mayonnaise")
F("ketchup", "basics", "كاتشب", "Ketchup", 112, 1.2, 26, 0.1, [TBSP(17)], (0, 1), None, "كاتشب كتشب ketchup")
F("egg_white", "basics", "بياض بيض", "Egg white", 52, 10.9, 0.7, 0.2, [PIECE(33, "بياض بيضة", "Egg white")], (0, 3), None, "بياض بيض egg white")
F("lentils", "basics", "عدس مطبوخ", "Cooked lentils", 116, 9, 20, 0.4, [CUP(200)], (0, 1), None, "عدس lentils")
F("chickpeas", "basics", "حمص حب", "Cooked chickpeas", 164, 8.9, 27.4, 2.6, [CUP(160)], (0, 1), None, "حمص حب مسلوق chickpeas")
F("white_beans", "basics", "فاصولياء بيضاء", "White beans", 139, 9.7, 25, 0.4, [CUP(180)], (0, 1), None, "فاصوليا بيضاء white beans")

# ---------------------------------------------------------------- add-ons
ADDONS = []


def A(id, ar, en, kcal, p, c, f, unit_ar, unit_en, g, default=1):
    ADDONS.append({"id": id, "ar": ar, "en": en, "art": "addon_" + id, "per100": [kcal, p, c, f],
                   "unit": {"ar": unit_ar, "en": unit_en, "g": g}, "defQty": default})


A("chicken", "دجاج", "Chicken", 190, 27, 0, 9, "حصة", "Portion", 100)
A("meat", "لحمة", "Meat", 294, 25, 0, 21, "حصة", "Portion", 100)
A("kofta", "كفتة", "Kofta", 240, 18, 3, 17, "قطعة", "Piece", 80)
A("beans", "فاصولياء", "Beans", 85, 5, 7, 4.5, "صحن صغير", "Small plate", 150)
A("peas", "بازيلاء", "Peas", 105, 6, 10, 4.5, "صحن صغير", "Small plate", 150)
A("laban", "لبن", "Yogurt", 61, 3.5, 4.7, 3.3, "كوب", "Cup", 200)
A("tomato_sauce", "صلصة بندورة", "Tomato sauce", 40, 1.5, 7, 0.7, "حصة", "Portion", 100)
A("nuts_pine", "لوز وصنوبر", "Almonds & pine nuts", 610, 17, 12, 56, "ملعقة", "Spoon", 10, 2)
A("honey", "عسل", "Honey", 304, 0.3, 82.4, 0, "ملعقة", "Spoon", 21)
A("jam", "مربى", "Jam", 278, 0.4, 69, 0.1, "ملعقة", "Spoon", 20)
A("nutella", "شوكولاتة / نوتيلا", "Chocolate spread", 539, 6.3, 57.5, 31, "ملعقة", "Spoon", 19)
A("peanut_butter", "زبدة فول سوداني", "Peanut butter", 588, 25, 20, 50, "ملعقة", "Spoon", 16)
A("banana", "موز", "Banana", 89, 1.1, 22.8, 0.3, "حبة", "Piece", 118)
A("strawberry", "فراولة", "Strawberries", 32, 0.7, 7.7, 0.3, "5 حبات", "5 berries", 60)
A("qashta", "قشطة", "Qashta", 260, 4, 6, 24, "ملعقة", "Spoon", 20)
A("milk", "حليب", "Milk", 61, 3.2, 4.8, 3.3, "نص كوب", "Half cup", 125)
A("whey", "Whey", "Whey", 400, 78, 8, 6, "سكوب", "Scoop", 30)
A("extra_cheese", "جبنة إضافية", "Extra cheese", 300, 22, 2, 23, "حصة", "Portion", 30)
A("vegetables", "خضار", "Vegetables", 25, 1, 5, 0.2, "حصة", "Portion", 50)
A("egg", "بيض", "Egg", 155, 12.6, 1.1, 10.6, "بيضة", "Egg", 50)
A("toum", "ثومية", "Garlic sauce", 650, 1, 6, 70, "ملعقة", "Spoon", 15)
A("tahini_sauce", "طحينة", "Tahini sauce", 250, 7, 9, 21, "ملعقة", "Spoon", 15)
A("fries", "بطاطا", "Fries", 312, 3.4, 41, 15, "حصة", "Portion", 80)
A("pickles", "مخلل", "Pickles", 15, 0.5, 2.5, 0.2, "حصة", "Portion", 30)
A("cheese", "جبنة", "Cheese", 403, 25, 1.3, 33, "شريحة", "Slice", 20)
A("red_sauce", "صوص أحمر", "Red sauce", 50, 1.6, 8, 1.5, "حصة", "Portion", 100)
A("white_sauce", "صوص أبيض", "White sauce", 160, 3, 6, 14, "حصة", "Portion", 100)
A("ground_meat", "لحمة مفرومة", "Ground meat", 250, 26, 0, 15.5, "حصة", "Portion", 100)
A("olive_oil", "زيت زيتون", "Olive oil", 884, 0, 0, 100, "ملعقة", "Spoon", 13.5)
A("caesar_dressing", "صوص سيزر", "Caesar dressing", 540, 2.5, 4, 57, "ملعقة", "Spoon", 15)
A("feta", "فيتا", "Feta", 264, 14, 4, 21, "حصة", "Portion", 30)
A("burger_sauce", "صوص", "Sauce", 400, 1, 10, 40, "ملعقة", "Spoon", 15)
A("ketchup", "كاتشب", "Ketchup", 112, 1.2, 26, 0.1, "ملعقة", "Spoon", 17)
A("mayo", "مايونيز", "Mayonnaise", 680, 1, 0.6, 75, "ملعقة", "Spoon", 14)
A("granola", "جرانولا", "Granola", 471, 10, 64, 20, "حفنة", "Handful", 30)
A("fruits", "فواكه", "Fruit", 50, 0.7, 12, 0.2, "حصة", "Portion", 80)
A("sugar", "سكر", "Sugar", 387, 0, 100, 0, "ملعقة صغيرة", "Teaspoon", 4, 2)
A("jameed", "جميد إضافي", "Extra jameed", 100, 6, 4, 7, "مغرفة", "Ladle", 100)
A("meat_topping", "لحمة", "Meat topping", 280, 20, 2, 21, "حصة", "Portion", 50)
A("pine_nuts", "صنوبر", "Pine nuts", 673, 14, 13, 68, "ملعقة", "Spoon", 10)
A("rice_side", "رز", "Rice", 150, 2.7, 28, 3, "صحن", "Plate", 200)

GROUPS = {
    "rice": ["chicken", "meat", "kofta", "beans", "peas", "laban", "tomato_sauce", "nuts_pine"],
    "sweet_toppings": ["honey", "jam", "nutella", "peanut_butter", "banana", "strawberry", "qashta"],
    "oats": ["milk", "honey", "banana", "peanut_butter", "whey"],
    "manakish": ["extra_cheese", "vegetables", "egg"],
    "sandwich": ["toum", "tahini_sauce", "fries", "pickles", "cheese"],
    "pasta": ["red_sauce", "white_sauce", "chicken", "ground_meat", "extra_cheese"],
    "salad": ["olive_oil", "caesar_dressing", "feta", "chicken"],
    "hummus_ful": ["olive_oil", "meat_topping", "pine_nuts"],
    "burger": ["cheese", "egg", "burger_sauce", "fries"],
    "potato": ["ketchup", "mayo", "cheese"],
    "yogurt": ["honey", "granola", "fruits"],
    "hot_drink": ["sugar", "milk"],
    "mansaf": ["jameed", "nuts_pine"],
    "stew": ["rice_side"],
}


# ---------------------------------------------------------------- the big list (tools/food_xl_*.py)
import sys as _sys
_sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import xlkit
import food_xl_1, food_xl_2, food_xl_3, food_xl_4, food_xl_addons  # noqa: F401  (they fill xlkit lists)

CATS += [
    ("pastries", "معجنات وفطائر", "Pastries & pies"),
    ("restaurants", "مطاعم عالمية", "World restaurants"),
    ("grains", "حبوب وبقوليات", "Grains & legumes"),
    ("snacks", "سناكات وشيبس", "Snacks & chips"),
    ("sauces", "صوصات وتتبيلات", "Sauces & dressings"),
    ("hot_drinks", "قهوة وشاي", "Coffee & tea"),
    ("juices", "عصائر وسموذي", "Juices & smoothies"),
    ("soft_drinks", "غازيات وطاقة ومي", "Soft drinks, energy & water"),
    ("vitamins", "فيتامينات وأدوية", "Vitamins & medicines"),
]
for x in xlkit.XL:
    look = x["look"]
    art = look[1] if look and look[0] == "same" else x["id"]
    F(x["id"], x["cat"], x["ar"], x["en"], x["kcal"], x["p"], x["c"], x["f"], x["units"], x["d"], x["add"], x["aka"], x["dish"])
    FOODS[-1]["art"] = art
for a in xlkit.XL_ADDONS:
    A(a["id"], a["ar"], a["en"], a["kcal"], a["p"], a["c"], a["f"], a["unit_ar"], a["unit_en"], a["g"], a["default"])
GROUPS.update(xlkit.XL_GROUPS)
_by_id = {f["id"]: f for f in FOODS}
for fid, g in xlkit.ADDON_OVERRIDES.items():
    _by_id[fid]["addons"] = g
for fid, c in xlkit.CAT_OVERRIDES.items():
    _by_id[fid]["cat"] = c


def check():
    cats = {c[0] for c in CATS}
    aid_list = [a["id"] for a in ADDONS]
    assert len(aid_list) == len(set(aid_list)), "duplicate add-on id"
    for f in FOODS:
        assert f["cat"] in cats, ("unknown category", f["id"], f["cat"])
        uids = [u["id"] for u in f["units"]]
        assert len(uids) == len(set(uids)), ("duplicate unit id", f["id"], uids)
        assert f["defUnit"] in uids, f["id"]
        assert all(u["g"] > 0 for u in f["units"]), f["id"]
    ids = [f["id"] for f in FOODS]
    assert len(ids) == len(set(ids)), "duplicate food id"
    aids = {a["id"] for a in ADDONS}
    for g, members in GROUPS.items():
        for m in members:
            assert m in aids, (g, m)
    for f in FOODS:
        if "addons" in f:
            assert f["addons"] in GROUPS, f["id"]
        kcal, p, c, fat = f["per100"]
        est = 4 * p + 4 * c + 9 * fat
        # energy should roughly match the macros (alcohol/fibre/rounding aside)
        if kcal > 20 and abs(est - kcal) / kcal > 0.15:
            print("  check energy:", f["id"], kcal, "vs macros", round(est))


if __name__ == "__main__":
    check()
    out = {"version": 1, "categories": [{"id": i, "ar": a, "en": e} for i, a, e in CATS],
           "foods": FOODS, "addons": ADDONS, "groups": GROUPS}
    path = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "assets", "food", "foods.json")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as fh:
        json.dump(out, fh, ensure_ascii=False, separators=(",", ":"))
    print(len(FOODS), "foods,", len(ADDONS), "add-ons →", os.path.normpath(path), os.path.getsize(path), "bytes")
