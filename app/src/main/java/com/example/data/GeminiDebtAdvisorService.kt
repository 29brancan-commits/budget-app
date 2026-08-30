package com.example.data

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit

object GeminiDebtAdvisorService {

    private const val TAG = "GeminiDebtAdvisor"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    suspend fun getDebtPayoffSuggestions(
        debts: List<DebtEntity>,
        selectedStrategy: PayoffStrategy,
        extraMonthlyBudget: Double,
        targetMonthsGoal: Int,
        isTargetTimelineMode: Boolean,
        customPrompt: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }

        if (debts.isEmpty()) {
            return@withContext Result.success("🎉 You currently have zero active debts listed! Add your debts to generate customized AI payoff suggestions.")
        }

        val totalBalance = debts.sumOf { it.balance }
        val totalMinPayment = debts.sumOf { it.minimumPayment }
        val avgApr = debts.map { it.interestRate }.average()
        val highestAprDebt = debts.maxByOrNull { it.interestRate }
        val lowestBalDebt = debts.minByOrNull { it.balance }

        val strategyResults = DebtPayoffCalculator.calculatePayoff(debts, extraMonthlyBudget)
        val snowballRes = strategyResults[PayoffStrategy.SNOWBALL]
        val avalancheRes = strategyResults[PayoffStrategy.AVALANCHE]
        val minimumRes = strategyResults[PayoffStrategy.MINIMUM_ONLY]

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.success(
                generateLocalDebtAdvice(
                    debts, selectedStrategy, extraMonthlyBudget,
                    snowballRes, avalancheRes, minimumRes,
                    highestAprDebt, lowestBalDebt, totalBalance
                )
            )
        }

        val debtSummarySb = StringBuilder()
        debts.forEachIndexed { idx, d ->
            debtSummarySb.append("- ${idx + 1}. ${d.name} (${d.category}): $${String.format(Locale.US, "%.2f", d.balance)} balance @ ${d.interestRate}% APR, min payment $${String.format(Locale.US, "%.2f", d.minimumPayment)}/mo\n")
        }

        val promptText = buildString {
            append("Analyze this user's debt portfolio and provide 3 to 4 concise, highly practical, actionable debt payoff suggestions:\n\n")
            append("PORTFOLIO SUMMARY:\n")
            append("- Total Debts: ${debts.size}\n")
            append("- Total Debt Balance: $${String.format(Locale.US, "%.2f", totalBalance)}\n")
            append("- Total Minimum Monthly Payment: $${String.format(Locale.US, "%.2f", totalMinPayment)}/mo\n")
            append("- Average APR: ${String.format(Locale.US, "%.1f", avgApr)}%\n")
            if (highestAprDebt != null) {
                append("- Highest Interest Debt: ${highestAprDebt.name} at ${highestAprDebt.interestRate}% APR\n")
            }
            if (lowestBalDebt != null) {
                append("- Smallest Balance Debt: ${lowestBalDebt.name} ($${String.format(Locale.US, "%.2f", lowestBalDebt.balance)})\n")
            }
            append("\nCURRENT PLANNING CONFIGURATION:\n")
            append("- Selected Strategy: ${selectedStrategy.name}\n")
            if (isTargetTimelineMode) {
                append("- Goal Timeline: Debt-free in $targetMonthsGoal months\n")
            } else {
                append("- Extra Monthly Payment Budget: $${String.format(Locale.US, "%.2f", extraMonthlyBudget)}/mo\n")
            }
            append("\nSIMULATION COMPARISON:\n")
            if (snowballRes != null) {
                append("• Snowball: ${snowballRes.totalMonths} months, $${String.format(Locale.US, "%.2f", snowballRes.totalInterestPaid)} interest\n")
            }
            if (avalancheRes != null) {
                append("• Avalanche: ${avalancheRes.totalMonths} months, $${String.format(Locale.US, "%.2f", avalancheRes.totalInterestPaid)} interest\n")
            }
            if (minimumRes != null) {
                append("• Minimum Payments Only: ${minimumRes.totalMonths} months, $${String.format(Locale.US, "%.2f", minimumRes.totalInterestPaid)} interest\n")
            }
            append("\nDEBT LIST:\n")
            append(debtSummarySb.toString())

            if (!customPrompt.isNullOrBlank()) {
                append("\nUSER SPECIFIC QUESTION:\n")
                append(customPrompt)
                append("\n")
            } else {
                append("\nPLEASE COVER:\n")
                append("1. **Optimal Payoff Strategy**: Snowball vs Avalanche recommendation for this specific portfolio.\n")
                append("2. **Interest Savings & Speedup**: Specific impact of adding extra monthly payments or refinancing high-APR debts.\n")
                append("3. **Immediate First Action**: The exact debt to target right now and why.\n")
                append("4. **Pro Tip**: A smart budgeting or debt reduction tip tailored to their debts.\n")
            }
        }

        try {
            val jsonPayload = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", promptText) })
                        })
                    })
                })
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", "You are an expert AI Debt & Financial Advisor. Provide direct, encouraging, well-structured, clear markdown advice for debt reduction. Keep bullet points brief and easy to read.")
                        })
                    })
                })
            }

            val requestUrl = "$BASE_URL?key=$apiKey"
            val body = jsonPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(requestUrl)
                .post(body)
                .build()

            val response = httpClient.newCall(request).execute()
            val respString = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                Log.w(TAG, "Gemini API error ${response.code}: $respString, using local advisor engine")
                return@withContext Result.success(
                    generateLocalDebtAdvice(
                        debts, selectedStrategy, extraMonthlyBudget,
                        snowballRes, avalancheRes, minimumRes,
                        highestAprDebt, lowestBalDebt, totalBalance
                    )
                )
            }

            val respJson = JSONObject(respString)
            val candidates = respJson.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val answerText = parts?.optJSONObject(0)?.optString("text")

            if (answerText.isNullOrBlank()) {
                return@withContext Result.success(
                    generateLocalDebtAdvice(
                        debts, selectedStrategy, extraMonthlyBudget,
                        snowballRes, avalancheRes, minimumRes,
                        highestAprDebt, lowestBalDebt, totalBalance
                    )
                )
            }

            Result.success(answerText.trim())
        } catch (e: Exception) {
            Log.w(TAG, "Failed to call Gemini API, falling back to local advisor", e)
            Result.success(
                generateLocalDebtAdvice(
                    debts, selectedStrategy, extraMonthlyBudget,
                    snowballRes, avalancheRes, minimumRes,
                    highestAprDebt, lowestBalDebt, totalBalance
                )
            )
        }
    }

    private fun generateLocalDebtAdvice(
        debts: List<DebtEntity>,
        selectedStrategy: PayoffStrategy,
        extraMonthlyBudget: Double,
        snowballRes: StrategyPayoffResult?,
        avalancheRes: StrategyPayoffResult?,
        minimumRes: StrategyPayoffResult?,
        highestAprDebt: DebtEntity?,
        lowestBalDebt: DebtEntity?,
        totalBalance: Double
    ): String = buildString {
        append("### 🎯 Strategic Debt Payoff Analysis\n\n")
        
        if (avalancheRes != null && snowballRes != null) {
            val interestDiff = snowballRes.totalInterestPaid - avalancheRes.totalInterestPaid
            if (interestDiff > 50.0) {
                append("• **Optimal Strategy**: **Debt Avalanche** will save you **$${String.format(Locale.US, "%,.2f", interestDiff)}** in total interest charges compared to Snowball by eliminating high APR balances first.\n")
            } else {
                append("• **Optimal Strategy**: **Debt Snowball** is ideal here! The interest difference is minimal ($${String.format(Locale.US, "%,.2f", Math.abs(interestDiff))}), but Snowball will give you quick psychological momentum by knocking out accounts faster.\n")
            }
        }

        if (highestAprDebt != null && highestAprDebt.interestRate >= 18.0) {
            append("• **Priority Target**: **${highestAprDebt.name}** is charging **${highestAprDebt.interestRate}% APR**. Focus all extra monthly contributions ($${String.format(Locale.US, "%,.2f", extraMonthlyBudget)}/mo) here to stop compounding interest.\n")
        } else if (lowestBalDebt != null) {
            append("• **Quick Win Target**: **${lowestBalDebt.name}** has the lowest balance ($${String.format(Locale.US, "%,.2f", lowestBalDebt.balance)}). Eliminating it first frees up $${String.format(Locale.US, "%,.2f", lowestBalDebt.minimumPayment)}/mo in cash flow.\n")
        }

        if (minimumRes != null && (avalancheRes != null || snowballRes != null)) {
            val bestRes = if (selectedStrategy == PayoffStrategy.AVALANCHE) {
                avalancheRes ?: snowballRes
            } else {
                snowballRes ?: avalancheRes
            }
            if (bestRes != null) {
                val monthsSaved = Math.max(0, minimumRes.totalMonths - bestRes.totalMonths)
                val interestSaved = Math.max(0.0, minimumRes.totalInterestPaid - bestRes.totalInterestPaid)
                append("• **Accelerated Progress**: Adding $${String.format(Locale.US, "%,.0f", extraMonthlyBudget)}/mo cuts your debt-free timeline by **$monthsSaved months** and saves **$${String.format(Locale.US, "%,.2f", interestSaved)}** in interest!\n")
            }
        }

        append("• **Pro Tip**: Set up automatic transfers on the day your paycheck arrives to lock in your extra payments before discretionary spending.")
    }
}
