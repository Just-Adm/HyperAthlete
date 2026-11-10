"""Big food list, add-ons: many more extras, the groups that offer them, and new groups for existing foods."""
from xlkit import *

# id, ar, en, kcal, p, c, f per 100 g, unit, grams per unit, default qty
# ---- coffee & tea
A("milk_shot", "حليب كامل (إضافة)", "Whole milk splash", 61, 3.2, 4.8, 3.3, "رشة (50 مل)", "Splash (50 ml)", 50, look=("glass", "#FFFFFF", None))
A("milk_skim_shot", "حليب خالي الدسم", "Skim milk splash", 34, 3.4, 5, 0.1, "رشة (50 مل)", "Splash (50 ml)", 50, look=("glass", "#F6F8FA", None))
A("milk_oat_shot", "حليب شوفان", "Oat milk splash", 46, 1, 6.5, 1.5, "رشة (50 مل)", "Splash (50 ml)", 50, look=("glass", "#F3E8D2", None))
A("milk_almond_shot", "حليب لوز", "Almond milk splash", 15, 0.5, 0.3, 1.1, "رشة (50 مل)", "Splash (50 ml)", 50, look=("glass", "#F5EFE3", None))
A("coffee_cream", "كريمة قهوة", "Coffee creamer", 450, 2, 55, 25, "ملعقة", "Spoon", 5, 2, look=("tub", "#FFFFFF", "#8B5E3C"))
A("syrup_vanilla", "سيرب فانيلا", "Vanilla syrup", 320, 0, 80, 0, "بمب (10 مل)", "Pump (10 ml)", 10, 2, look=("bottle", "#F3E2C6", "#2E2E2E"))
A("syrup_caramel", "سيرب كراميل", "Caramel syrup", 320, 0, 80, 0, "بمب (10 مل)", "Pump (10 ml)", 10, 2, look=("bottle", "#C9853A", "#2E2E2E"))
A("syrup_hazelnut", "سيرب بندق", "Hazelnut syrup", 320, 0, 80, 0, "بمب (10 مل)", "Pump (10 ml)", 10, 2, look=("bottle", "#8B5E34", "#2E2E2E"))
A("syrup_sugarfree", "سيرب بدون سكر", "Sugar-free syrup", 0, 0, 0, 0, "بمب", "Pump", 10, 2, look=("bottle", "#FFFFFF", "#4E8A3A"))
A("extra_shot", "شوت إسبريسو إضافي", "Extra espresso shot", 9, 0.1, 1.7, 0.2, "شوت", "Shot", 30, look=("mug", "#3E2216", None))
A("whipped_top", "كريمة مخفوقة (فوق)", "Whipped cream topping", 257, 3.2, 12.5, 22, "حصة", "Portion", 20, look=("bowl", "#FFFFFF", "#F6F2E6"))
A("caramel_drizzle", "صوص كراميل", "Caramel drizzle", 380, 1, 70, 10, "رشة", "Drizzle", 10, look=("bottle", "#C9853A", "#8B4A2E"))
A("choc_sauce", "صوص شوكولاتة", "Chocolate sauce", 350, 2.5, 62, 9, "ملعقة", "Spoon", 20, look=("bottle", "#5B3321", "#3E2216"))
A("sweetener", "محلّي (صفر سعرات)", "Zero-calorie sweetener", 0, 0, 0, 0, "كيس", "Packet", 1, look=("sachet", "#4E8A3A", "#FFFFFF"))
A("condensed_add", "حليب مكثف", "Condensed milk", 321, 8, 54, 8.7, "ملعقة", "Spoon", 20, look=("can", "#2E86DE", "#FFFFFF"))
A("lemon_slice", "ليمون", "Lemon", 29, 1.1, 9.3, 0.3, "شريحة", "Slice", 8, look=("fruit", "#F7DC3A", None))
A("mint_leaves", "نعنع", "Mint", 44, 3.3, 8.4, 0.7, "أوراق", "Leaves", 2, look=("leafy", "#3E9A3A"))
A("cinnamon_nuts", "قرفة ومكسرات (للسحلب)", "Cinnamon & nuts", 600, 15, 25, 50, "رشة", "Sprinkle", 8, look=("pile", "#B8865B", "#C99A55"))
A("coconut_add", "جوز هند مبشور", "Shredded coconut", 660, 6.9, 24, 64.5, "ملعقة", "Spoon", 5, look=("pile", "#FAFAF5", "#F3EEE0"))
# ---- eggs, bread & spreads
A("sausage_add", "نقانق / سجق", "Sausage / sujuk", 300, 15, 3, 25, "حبة", "Piece", 40, look=("meat", "#9C3B26"))
A("tomato_add", "بندورة", "Tomato", 18, 0.9, 3.9, 0.2, "حبة", "Tomato", 120, look=("fruit", "#E8453C", "#4E8A3A"))
A("onion_add", "بصل", "Onion", 40, 1.1, 9.3, 0.1, "حصة", "Portion", 40, look=("fruit", "#C9A0C8", None))
A("mushrooms_add", "فطر", "Mushrooms", 30, 3, 4, 1, "حصة", "Portion", 50, look=("pile", "#C8B8A6", "#A89580"))
A("avocado_add", "أفوكادو", "Avocado", 160, 2, 8.5, 14.7, "نص حبة", "Half", 70, look=("fruit", "#6B8E3A", None))
A("toast_add", "توست", "Toast", 265, 9, 49, 3.2, "شريحة", "Slice", 30, look=("bread", "#E2B36A"))
A("butter_add", "زبدة", "Butter", 717, 0.9, 0.1, 81, "ملعقة صغيرة", "Teaspoon", 5, look=("cheese", "#F7E07A"))
A("cream_cheese_add", "جبنة كريمي", "Cream cheese", 342, 6, 4, 34, "ملعقة", "Spoon", 15, look=("jar", "#FBF8F0", "#4A6FD6"))
A("labneh_add", "لبنة", "Labneh", 170, 7, 4.5, 14, "ملعقة", "Spoon", 20, look=("bowl", "#FAF7EF", "#EFE6CE"))
A("zaatar_add", "زعتر", "Za'atar", 340, 10, 40, 16, "ملعقة", "Spoon", 7, look=("pile", "#6B7F2A", "#8E9A5A"))
A("almond_butter_add", "زبدة لوز", "Almond butter", 614, 21, 19, 56, "ملعقة", "Spoon", 16, look=("jar", "#C49A6C", "#6B3420"))
A("olives_add", "زيتون", "Olives", 145, 1, 4, 15, "5 حبات", "5 olives", 20, look=("pile", "#4E5A2E", "#2E3A1E"))
A("falafel_add", "فلافل", "Falafel", 333, 13.3, 31.8, 17.8, "قرص", "Ball", 17, 3, look=("pile", "#8B5E34", "#6B4422"))
A("kiri_add", "جبنة مثلثات / كيري", "Cheese triangle", 300, 7, 3, 29, "حبة", "Piece", 17, look=("cheese", "#FFFDF5"))
A("lotus_spread", "لوتس سبريد", "Lotus spread", 584, 3, 57, 38, "ملعقة", "Spoon", 15, look=("jar", "#C9853A", "#E8453C"))
A("maple_add", "ميبل سيرب", "Maple syrup", 260, 0, 67, 0.1, "ملعقة", "Spoon", 20, look=("bottle", "#C9853A", "#8B4A2E"))
A("powdered_sugar", "سكر بودرة", "Powdered sugar", 389, 0, 100, 0, "رشة", "Dusting", 5, look=("pile", "#FFFFFF", "#F3F0E0"))
# ---- sandwiches, grills & sauces
A("hot_sauce_add", "صوص حار", "Hot sauce", 30, 1, 5, 0.5, "رشة", "Dash", 5, 2, look=("bottle", "#C0392B", "#2E2E2E"))
A("mustard_add", "خردل", "Mustard", 66, 4.4, 5.3, 4, "ملعقة صغيرة", "Teaspoon", 5, look=("bottle", "#F2C33A", "#E8453C"))
A("bbq_add", "صوص باربكيو", "BBQ sauce", 172, 0.8, 41, 0.6, "ملعقة", "Spoon", 17, look=("bottle", "#6B2C1E", "#2E2E2E"))
A("honey_mustard_add", "عسل وخردل", "Honey mustard", 270, 1.5, 24, 19, "ملعقة", "Spoon", 16, look=("bottle", "#E8B83A", "#F2C33A"))
A("ranch_add", "رانش", "Ranch", 480, 1.3, 6, 50, "ملعقة", "Spoon", 15, look=("bottle", "#FFFFFF", "#4E8A3A"))
A("cheese_sauce_add", "صوص جبنة", "Cheese sauce", 180, 5.5, 8, 14, "حصة", "Portion", 30, look=("bowl", "#F2C33A", "#E2A93A"))
A("sweet_chili_add", "سويت تشيلي", "Sweet chili", 220, 0.5, 52, 0.5, "ملعقة", "Spoon", 17, look=("bottle", "#E8453C", "#F2C33A"))
A("extra_meat_sandwich", "لحمة/دجاج إضافي", "Extra meat", 220, 22, 2, 14, "حصة", "Portion", 60, look=("meat", "#C9853A"))
A("extra_shawarma", "شاورما إضافية", "Extra shawarma", 210, 20, 3, 13, "حصة", "Portion", 70, look=("meat", "#C9853A"))
A("bread_side", "خبز عربي", "Pita bread", 275, 9, 55, 1.2, "رغيف", "Loaf", 70, look=("bread", "#E6B565"))
A("hummus_side", "حمص", "Hummus", 177, 8, 14, 10, "صحن صغير", "Small plate", 100, look=("bowl", "#EBCB8F", "#C9A14E"))
A("salad_side", "سلطة خضراء", "Side salad", 35, 1.2, 5, 1.5, "صحن صغير", "Small plate", 120, look=("bowl", "#7DC04B", "#E8453C"))
A("grilled_veg_side", "خضار مشوية", "Grilled vegetables", 60, 1.5, 7, 3, "حصة", "Portion", 100, look=("pile", "#E8453C", "#6DBE45"))
A("rice_vermicelli_side", "رز بالشعيرية", "Vermicelli rice", 160, 3, 28, 4, "صحن", "Plate", 200, look=("bowl", "#FFF4DC", "#C9944A"))
A("fried_onions", "بصل مقلي", "Fried onions", 450, 4, 40, 30, "ملعقة", "Spoon", 10, 2, look=("pile", "#8A4B1E", "#C9853A"))
A("raisins_add", "زبيب", "Raisins", 299, 3.1, 79, 0.5, "ملعقة", "Spoon", 15, look=("pile", "#5B2E3E", "#3E1F2E"))
A("cucumber_yogurt_add", "سلطة خيار ولبن", "Cucumber yogurt", 55, 3, 4, 3, "زبدية صغيرة", "Small bowl", 150, look=("bowl", "#FAF7EF", "#6DBE45"))
A("extra_meat_mansaf", "لحمة إضافية", "Extra lamb", 294, 25, 0, 21, "قطعة", "Piece", 100, look=("meat", "#8E4B2E"))
A("shrak_add", "خبز شراك", "Shrak bread", 270, 9, 56, 1, "رغيف", "Loaf", 60, look=("bread", "#F0D9A8"))
# ---- burgers, pizza, mexican, pasta
A("extra_patty", "قرص لحمة إضافي", "Extra beef patty", 250, 20, 0, 18, "قرص", "Patty", 110, look=("meat", "#7A3E2A"))
A("beef_bacon_add", "بيكون بقري", "Beef bacon", 330, 25, 1, 25, "شريحة", "Slice", 10, 2, look=("meat", "#B4513A"))
A("onion_rings_add", "حلقات بصل", "Onion rings", 330, 4.5, 38, 18, "حصة", "Portion", 60, look=("pile", "#E0A04A", "#F3D9A0"))
A("jalapeno_add", "هالابينو", "Jalapeño", 30, 1, 6, 0.4, "حصة", "Portion", 15, look=("long", "#4E8A3A"))
A("pepperoni_add", "بيبروني", "Pepperoni", 494, 23, 0, 44, "حصة", "Portion", 20, look=("pile", "#B5473A", "#8E2C24"))
A("stuffed_crust", "أطراف محشية جبنة", "Cheese-stuffed crust", 300, 12, 25, 17, "للقطعة", "Per slice", 25, look=("bread", "#F2C33A"))
A("guac_add", "غواكامولي", "Guacamole", 160, 2, 8.5, 14.5, "حصة", "Portion", 40, look=("bowl", "#9CC069", "#7DA04B"))
A("sour_cream_add", "كريمة حامضة", "Sour cream", 198, 2.4, 4.6, 19, "ملعقة", "Spoon", 20, look=("bowl", "#FFFFFF", "#F6F2E6"))
A("salsa_add", "سالسا", "Salsa", 36, 1.5, 7, 0.2, "ملعقة", "Spoon", 30, look=("bowl", "#E8453C", "#6DBE45"))
A("beans_side", "فاصولياء حمراء", "Red beans", 127, 8.7, 22.8, 0.5, "حصة", "Portion", 80, look=("pile", "#8E1F2E", "#6B1F2E"))
A("parmesan_add", "بارميزان", "Parmesan", 431, 38, 4, 29, "ملعقة", "Spoon", 5, 2, look=("cheese", "#F6E7B0"))
A("shrimp_add", "جمبري", "Shrimp", 99, 24, 0.2, 0.3, "حصة", "Portion", 80, look=("pile", "#F3A35B", "#E8853A"))
A("meatballs_add", "كرات لحمة", "Meatballs", 230, 16, 8, 15, "كرة", "Ball", 25, 3, look=("pile", "#8E4B2E", "#6B3420"))
A("pesto_add", "بيستو", "Pesto", 450, 5, 6, 45, "ملعقة", "Spoon", 16, look=("jar", "#4E8A3A", "#2E6A3A"))
A("garlic_bread_add", "خبز بالثوم", "Garlic bread", 350, 8, 42, 16, "قطعة", "Piece", 30, 2, look=("bread", "#E8C88A"))
A("soy_sauce_add", "صويا صوص", "Soy sauce", 53, 8, 4.9, 0.6, "ملعقة", "Spoon", 16, look=("bottle", "#2E1F14", "#E8453C"))
A("peanuts_add", "فستق سوداني", "Peanuts", 567, 26, 16, 49, "ملعقة", "Spoon", 9, look=("pile", "#D9A14E", "#B8865B"))
# ---- soups & salads
A("croutons", "خبز محمص (كروتون)", "Croutons", 407, 12, 74, 6.6, "حفنة", "Handful", 15, look=("pile", "#E0A04A", "#C9853A"))
A("tuna_add", "تونة", "Tuna", 116, 26, 0, 1, "علبة", "Can", 80, look=("can", "#C9D0D8", "#2E86DE"))
A("seeds_mix", "بذور مشكلة", "Mixed seeds", 560, 22, 18, 46, "ملعقة", "Spoon", 10, look=("pile", "#8B6B4E", "#5B5B5B"))
A("balsamic_add", "بلسميك", "Balsamic", 88, 0.5, 17, 0, "ملعقة", "Spoon", 16, look=("bottle", "#2E1F14", "#C9853A"))
A("quinoa_add", "كينوا", "Quinoa", 120, 4.4, 21.3, 1.9, "حصة", "Portion", 80, look=("pile", "#EADCB8", "#C9B58A"))
A("corn_add", "ذرة", "Corn", 86, 3.3, 19, 1.4, "حصة", "Portion", 40, look=("pile", "#F2C33A", "#E2A93A"))
A("egg_boiled_add", "بيضة مسلوقة", "Boiled egg", 155, 12.6, 1.1, 10.6, "بيضة", "Egg", 50, look=("fruit", "#FFFFFF", None))
# ---- sweet, fruit, milk, yogurt & oats
A("chia_add", "شيا", "Chia seeds", 486, 17, 42, 31, "ملعقة", "Spoon", 12, look=("pile", "#5B5B5B", "#3E3E3E"))
A("flax_add", "بذر كتان", "Flaxseed", 534, 18, 29, 42, "ملعقة", "Spoon", 10, look=("pile", "#8B5E34", "#6B4422"))
A("berries_add", "توت مشكل", "Mixed berries", 50, 0.8, 12, 0.3, "حفنة", "Handful", 60, look=("pile", "#3E3B8E", "#D6264E"))
A("nuts_mix_add", "مكسرات", "Nuts", 607, 20, 21, 54, "ملعقة", "Spoon", 15, look=("pile", "#B8865B", "#E8C88A"))
A("dates_add", "تمر", "Dates", 282, 2.5, 75, 0.4, "حبة", "Date", 10, 2, look=("pile", "#6B3420", "#4E2216"))
A("oats_add", "شوفان", "Oats", 379, 13, 68, 6.5, "ملعقة", "Spoon", 10, 3, look=("pile", "#E8D7B3", "#C9B58A"))
A("cocoa_add", "كاكاو", "Cocoa", 228, 19.6, 58, 13.7, "ملعقة", "Spoon", 5, look=("tub", "#5B3321", "#C9853A"))
A("milk_cup", "حليب (كوب)", "Milk (cup)", 61, 3.2, 4.8, 3.3, "كوب", "Cup", 250, look=("glass", "#FFFFFF", None))
A("ice_cream_scoop", "سكوب بوظة", "Ice cream scoop", 207, 3.5, 24, 11, "سكوب", "Scoop", 66, look=("icecream", "#FFF4DC", False))
A("pistachio_add", "فستق حلبي مطحون", "Ground pistachio", 560, 20, 28, 45, "ملعقة", "Spoon", 8, look=("pile", "#8DB255", "#6B8E3A"))
A("qater_add", "قطر إضافي", "Extra syrup", 310, 0, 78, 0, "ملعقة", "Spoon", 20, look=("jar", "#F2D27E", "#E2A93A"))
A("sprinkles", "سبرنكلز", "Sprinkles", 450, 2, 90, 10, "رشة", "Sprinkle", 5, look=("pile", "#E05A8A", "#2E86DE"))
A("oreo_crumbs", "أوريو مطحون", "Oreo crumbs", 480, 4.5, 69, 20, "ملعقة", "Spoon", 10, look=("pile", "#2E2E2E", "#FFFFFF"))
A("cone_add", "كون (بسكويت)", "Wafer cone", 417, 8, 80, 7, "كون", "Cone", 12, look=("icecream", "#F3D9A0", True))
# ---- shakes
A("creatine_add", "كرياتين", "Creatine", 0, 0, 0, 0, "سكوب", "Scoop", 5, look=("tub", "#FFFFFF", "#2E86DE"))
A("ice_none", "ثلج", "Ice", 0, 0, 0, 0, "حفنة", "Handful", 50, look=("pile", "#DCEAF5", "#BFE3F5"))

def keep(*ids): return list(ids)

# existing groups, extended (old members first so nothing disappears)
G("rice", "chicken", "meat", "kofta", "beans", "peas", "laban", "tomato_sauce", "nuts_pine", "salad_side", "cucumber_yogurt_add", "fried_onions", "raisins_add", "hummus_side")
G("sweet_toppings", "honey", "jam", "nutella", "peanut_butter", "banana", "strawberry", "qashta", "maple_add", "butter_add", "whipped_top", "choc_sauce", "ice_cream_scoop", "nuts_mix_add", "lotus_spread", "powdered_sugar", "berries_add")
G("oats", "milk", "honey", "banana", "peanut_butter", "whey", "chia_add", "flax_add", "berries_add", "dates_add", "nuts_mix_add", "cocoa_add", "raisins_add", "almond_butter_add")
G("manakish", "extra_cheese", "vegetables", "egg", "olives_add", "labneh_add", "kiri_add", "sausage_add", "hot_sauce_add")
G("sandwich", "toum", "tahini_sauce", "fries", "pickles", "cheese", "ketchup", "mayo", "hot_sauce_add", "extra_meat_sandwich", "vegetables")
G("pasta", "red_sauce", "white_sauce", "chicken", "ground_meat", "extra_cheese", "parmesan_add", "mushrooms_add", "shrimp_add", "meatballs_add", "pesto_add", "garlic_bread_add")
G("salad", "olive_oil", "caesar_dressing", "feta", "chicken", "tuna_add", "egg_boiled_add", "avocado_add", "croutons", "seeds_mix", "nuts_mix_add", "balsamic_add", "ranch_add", "quinoa_add", "corn_add")
G("hummus_ful", "olive_oil", "meat_topping", "pine_nuts", "bread_side", "pickles", "vegetables", "falafel_add", "egg_boiled_add", "hot_sauce_add")
G("burger", "cheese", "egg", "burger_sauce", "fries", "extra_patty", "beef_bacon_add", "onion_rings_add", "jalapeno_add", "avocado_add", "mushrooms_add", "ketchup", "mayo", "bbq_add")
G("potato", "ketchup", "mayo", "cheese", "cheese_sauce_add", "bbq_add", "hot_sauce_add", "toum", "ranch_add")
G("yogurt", "honey", "granola", "fruits", "chia_add", "nuts_mix_add", "dates_add", "peanut_butter", "whey", "berries_add", "flax_add")
G("hot_drink", "sugar", "milk", "sweetener", "honey")
G("mansaf", "jameed", "nuts_pine", "extra_meat_mansaf", "shrak_add", "laban")
G("stew", "rice_side", "rice_vermicelli_side", "bread_side", "laban", "lemon_slice")
# new groups
G("eggs", "sausage_add", "cheese", "tomato_add", "onion_add", "mushrooms_add", "avocado_add", "toast_add", "bread_side", "butter_add", "olive_oil", "hot_sauce_add")
G("spreads", "butter_add", "cream_cheese_add", "labneh_add", "honey", "jam", "nutella", "peanut_butter", "almond_butter_add", "zaatar_add", "olive_oil", "cheese", "avocado_add")
G("labneh_extra", "olive_oil", "zaatar_add", "vegetables", "olives_add", "mint_leaves")
G("kaak", "falafel_add", "egg", "zaatar_add", "cheese", "labneh_add", "kiri_add", "olive_oil")
G("cereal", "milk_cup", "banana", "strawberry", "honey", "berries_add", "nuts_mix_add")
G("noodles", "egg", "chicken", "shrimp_add", "vegetables", "soy_sauce_add", "peanuts_add", "hot_sauce_add")
G("maqluba", "laban", "salad_side", "nuts_pine", "cucumber_yogurt_add")
G("grill_sides", "rice_side", "bread_side", "fries", "hummus_side", "toum", "salad_side", "grilled_veg_side", "tahini_sauce", "pickles", "hot_sauce_add")
G("fish_sides", "rice_side", "fries", "tahini_sauce", "salad_side", "lemon_slice", "bread_side", "toum")
G("shawarma", "toum", "fries", "pickles", "extra_cheese", "hot_sauce_add", "tahini_sauce", "extra_shawarma", "mayo", "ketchup")
G("sandwich_extra", "toum", "tahini_sauce", "fries", "pickles", "cheese", "ketchup", "mayo", "hot_sauce_add", "mustard_add", "extra_meat_sandwich", "vegetables", "avocado_add")
G("dips", "ketchup", "mayo", "bbq_add", "honey_mustard_add", "ranch_add", "cheese_sauce_add", "sweet_chili_add", "toum", "fries")
G("mexican", "guac_add", "sour_cream_add", "salsa_add", "cheese", "jalapeno_add", "rice_side", "beans_side", "hot_sauce_add")
G("pizza", "extra_cheese", "pepperoni_add", "mushrooms_add", "olives_add", "jalapeno_add", "chicken", "ranch_add", "toum", "stuffed_crust", "hot_sauce_add")
G("soup_extra", "croutons", "lemon_slice", "bread_side", "toast_add", "parmesan_add")
G("fruit_extra", "honey", "laban", "granola", "nuts_mix_add", "qashta", "ice_cream_scoop")
G("milk_extra", "honey", "sugar", "cocoa_add", "banana", "dates_add", "oats_add", "whey")
G("dessert_extra", "qashta", "ice_cream_scoop", "pistachio_add", "qater_add", "nuts_mix_add", "whipped_top", "choc_sauce", "lotus_spread")
G("icecream_extra", "choc_sauce", "caramel_drizzle", "sprinkles", "nuts_mix_add", "lotus_spread", "oreo_crumbs", "fruits", "whipped_top", "cone_add")
G("coffee", "milk_shot", "milk_skim_shot", "milk_oat_shot", "milk_almond_shot", "coffee_cream", "sugar", "sweetener", "syrup_vanilla", "syrup_caramel", "syrup_hazelnut", "syrup_sugarfree", "extra_shot", "whipped_top", "caramel_drizzle", "choc_sauce", "condensed_add")
G("tea", "sugar", "sweetener", "honey", "milk", "lemon_slice", "mint_leaves")
G("juice", "sugar", "honey", "ice_cream_scoop", "mint_leaves")
G("jallab", "pine_nuts", "raisins_add")
G("smoothie", "whey", "peanut_butter", "oats_add", "honey", "chia_add", "banana", "dates_add", "milk")
G("cocktail_extra", "qashta", "honey", "nuts_mix_add", "ice_cream_scoop", "fruits")
G("protein_shake", "milk_cup", "banana", "oats_add", "peanut_butter", "honey", "creatine_add", "dates_add", "berries_add", "cocoa_add")
G("sahlab", "cinnamon_nuts", "coconut_add", "pistachio_add", "raisins_add")

# existing foods: categories that moved and add-ons they now offer
CAT_OVERRIDES.update({
    "tea": "hot_drinks", "coffee_arabic": "hot_drinks", "coffee_turkish": "hot_drinks", "espresso": "hot_drinks",
    "cappuccino": "hot_drinks", "latte": "hot_drinks", "sahlab": "hot_drinks",
    "orange_juice": "juices", "lemon_mint": "juices", "soda": "soft_drinks", "energy_drink": "soft_drinks",
    "olive_oil": "basics", "mayo": "sauces", "ketchup": "sauces",
})
ADDON_OVERRIDES.update({
    "tea": "tea", "coffee_turkish": "hot_drink", "coffee_arabic": "hot_drink", "espresso": "coffee", "cappuccino": "coffee",
    "latte": "coffee", "sahlab": "sahlab", "orange_juice": "juice", "lemon_mint": "juice",
    "egg_boiled": "eggs", "egg_fried": "eggs", "ijjeh": "eggs", "shakshuka": "eggs",
    "labneh": "labneh_extra", "kaak": "kaak", "falafel": "sandwich_extra",
    "bread_arabic": "spreads", "bread_taboon": "spreads", "bread_shrak": "spreads", "bread_brown": "spreads",
    "toast_white": "spreads", "toast_brown": "spreads", "croissant": "spreads",
    "corn_flakes": "cereal",
    "maqluba": "maqluba", "musakhan": "grill_sides", "mandi": "rice", "kabsa": "rice", "zarb": "rice", "qidreh": "rice",
    "shish_tawook": "grill_sides", "kebab": "grill_sides", "kofta_grilled": "grill_sides", "chicken_grilled": "grill_sides",
    "chicken_breast": "grill_sides", "chicken_thigh": "grill_sides", "steak": "grill_sides", "lamb": "grill_sides",
    "fish_grilled": "fish_sides", "fish_fried": "fish_sides", "shrimp": "fish_sides",
    "shawarma_chicken": "shawarma", "shawarma_meat": "shawarma", "shawarma_chicken_meat": "shawarma", "shawarma_meat_only": "shawarma",
    "falafel_sandwich": "sandwich_extra", "zinger": "burger", "broasted": "dips", "pizza": "pizza",
    "soup_lentil": "soup_extra", "soup_veg": "soup_extra", "soup_freekeh": "soup_extra", "soup_chicken": "soup_extra",
    "milk_full": "milk_extra", "milk_low": "milk_extra", "laban": "yogurt", "greek_yogurt": "yogurt",
    "knafeh": "dessert_extra", "qatayef": "dessert_extra", "um_ali": "dessert_extra", "harissa": "dessert_extra",
    "baklava": "dessert_extra", "muhallabia": "dessert_extra", "rice_pudding": "dessert_extra", "ice_cream": "icecream_extra",
    "pancakes": "sweet_toppings", "waffle": "sweet_toppings", "crepe": "sweet_toppings",
    "whey": "protein_shake", "mass_gainer": "protein_shake",
    "salad_green": "salad", "salad_arabic": "salad", "fattoush": "salad", "salad_greek": "salad",
    "fries": "potato", "potato_boiled": "potato", "sweet_potato": "potato",
})
