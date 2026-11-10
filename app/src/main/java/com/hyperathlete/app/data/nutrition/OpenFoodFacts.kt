package com.hyperathlete.app.data.nutrition

import com.hyperathlete.app.BuildConfig
import com.hyperathlete.app.domain.nutrition.CustomFood
import com.hyperathlete.app.domain.nutrition.Macros
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

sealed interface BarcodeResult {
    data class Found(val food: CustomFood) : BarcodeResult
    data object NotFound : BarcodeResult
    data object Offline : BarcodeResult
}

/** Packaged food by barcode from Open Food Facts (free, no key). Found products are saved as the user's own foods. */
@Singleton
class OpenFoodFacts @Inject constructor(private val http: OkHttpClient, private val repo: NutritionRepository) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun lookup(barcode: String): BarcodeResult {
        val code = barcode.filter { it.isDigit() }
        if (code.isEmpty()) return BarcodeResult.NotFound
        repo.byBarcode(code)?.let { return BarcodeResult.Found(it) }
        val body = withContext(Dispatchers.IO) {
            runCatching {
                val req = Request.Builder()
                    .url("https://world.openfoodfacts.org/api/v2/product/$code.json?fields=product_name,product_name_ar,product_name_en,brands,nutriments,serving_quantity,serving_size")
                    .header("User-Agent", "HyperAthlete/${BuildConfig.VERSION_NAME} (Android; personal app)")
                    .build()
                http.newCall(req).execute().use { r -> if (r.code == 404) "" else if (r.isSuccessful) r.body.string() else null }
            }.getOrNull()
        } ?: return BarcodeResult.Offline
        val food = parse(code, body) ?: return BarcodeResult.NotFound
        val id = repo.saveCustom(food)
        return BarcodeResult.Found(food.copy(id = id))
    }

    fun parse(code: String, body: String): CustomFood? {
        if (body.isBlank()) return null
        val root = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull() ?: return null
        val p = root["product"] as? JsonObject ?: return null
        fun str(o: JsonObject, k: String) = o[k]?.jsonPrimitive?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }
        fun num(o: JsonObject, k: String) = o[k]?.jsonPrimitive?.let { it.doubleOrNull ?: it.contentOrNull?.toDoubleOrNull() }
        val n = p["nutriments"] as? JsonObject ?: return null
        val kcal = num(n, "energy-kcal_100g") ?: num(n, "energy_100g")?.let { it / 4.184 } ?: return null
        val per100 = Macros(kcal, num(n, "proteins_100g") ?: 0.0, num(n, "carbohydrates_100g") ?: 0.0, num(n, "fat_100g") ?: 0.0)
        val name = str(p, "product_name_ar") ?: str(p, "product_name") ?: str(p, "product_name_en") ?: return null
        val brand = str(p, "brands")?.substringBefore(',')
        val serving = num(p, "serving_quantity")?.takeIf { it in 1.0..2000.0 } ?: 100.0
        return CustomFood(
            name = if (brand != null && !name.contains(brand, ignoreCase = true)) "$name · $brand" else name,
            servingName = str(p, "serving_size")?.take(24) ?: "${serving.toInt()} g",
            servingGrams = serving, per100 = per100, barcode = code,
        )
    }
}
