"""
Small DSL for the big food list (tools/food_xl_*.py), shared by gen_foods.py (data) and
gen_food_art.py (pictures).

    X(id, cat, ar, en, kcal, protein, carbs, fat, units, d=(unit_index, qty), add="addon group",
      aka="other spellings", look=("template", ...), dish=True)

Values are per 100 g (or 100 ml for drinks, taken as 100 g). `look` picks a picture template from
food_art_xl.py; ("same", "<existing art id>") reuses an existing picture.
"""

XL = []          # foods
XL_ADDONS = []   # add-ons
XL_GROUPS = {}   # add-on groups
ADDON_OVERRIDES = {}   # existing food id -> add-on group
CAT_OVERRIDES = {}     # existing food id -> new category


def U(id, ar, en, g):
    return {"id": id, "ar": ar, "en": en, "g": g}


# ---- serving units -------------------------------------------------------------
def PL(g): return U("plate", "صحن", "Plate", g)
def HP(g): return U("half_plate", "نص صحن", "Half plate", g)
def PC(g, ar="حبة", en="Piece"): return U("piece", ar, en, g)
def SL(g, ar="شريحة", en="Slice"): return U("slice", ar, en, g)
def TB(g): return U("tbsp", "ملعقة كبيرة", "Tablespoon", g)
def TS(g): return U("tsp", "ملعقة صغيرة", "Teaspoon", g)
def CP(g): return U("cup", "كوب", "Cup", g)
def GL(g): return U("glass", "كاسة", "Glass", g)
def BW(g): return U("bowl", "زبدية", "Bowl", g)
def LF(g): return U("loaf", "رغيف", "Loaf", g)
def HF(g): return U("handful", "حفنة", "Handful", g)
def CN(g): return U("can", "علبة", "Can", g)
def SK(g): return U("skewer", "سيخ", "Skewer", g)
def SC(g): return U("scoop", "سكوب", "Scoop", g)
def BT(g, ar="قنينة", en="Bottle"): return U("bottle", ar, en, g)
def SM(g): return U("small", "صغير", "Small", g)
def MD(g): return U("medium", "وسط", "Medium", g)
def LG(g): return U("large", "كبير", "Large", g)
def SW(g): return U("sandwich", "ساندويش", "Sandwich", g)
def MEAL(g): return U("meal", "وجبة", "Meal", g)
def PORT(g): return U("portion", "حصة", "Portion", g)
def BAR(g): return U("bar", "لوح", "Bar", g)
def PK(g, ar="باكيت", en="Pack"): return U("pack", ar, en, g)
def SH(g): return U("shot", "شوت", "Shot", g)
def CAPS(g=0.6): return U("capsule", "كبسولة", "Capsule", g)
def TAB(g=0.8): return U("tablet", "حبة", "Tablet", g)
def SOFT(g=1.0): return U("softgel", "كبسولة جيلاتين", "Softgel", g)
def GUM(g=3.0): return U("gummy", "حبة جيلي", "Gummy", g)
def SACH(g): return U("sachet", "كيس", "Sachet", g)
def ML5(g=5.0): return U("ml5", "ملعقة دوا (5 مل)", "5 ml spoon", g)
def DROP(g=0.05): return U("drop", "نقطة", "Drop", g)


def X(id, cat, ar, en, kcal, p, c, f, units, d=(0, 1), add=None, aka="", look=None, dish=False):
    XL.append({
        "id": id, "cat": cat, "ar": ar, "en": en, "kcal": kcal, "p": p, "c": c, "f": f,
        "units": units, "d": d, "add": add, "aka": aka, "look": look, "dish": dish,
    })


def A(id, ar, en, kcal, p, c, f, unit_ar, unit_en, g, default=1, look=None):
    XL_ADDONS.append({"id": id, "ar": ar, "en": en, "kcal": kcal, "p": p, "c": c, "f": f,
                      "unit_ar": unit_ar, "unit_en": unit_en, "g": g, "default": default, "look": look})


def G(name, *members):
    XL_GROUPS[name] = list(members)
