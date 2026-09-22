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
        customPrompt: String? = null,
        windfallAmount: Double = 0.0,
        consolidationApr: Double? = null
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
        val pinnedDebts = debts.filter { it.customPriorityRank > 0 }.sortedBy { it.customPriorityRank }
        val promoDebts = debts.filter { it.promoApr != null }

        val strategyResults = DebtPayoffCalculator.calculatePayoff(debts, extraMonthlyBudget)
        val snowballRes = strategyResults[PayoffStrategy.SNOWBALL]
        val avalancheRes = strategyResults[PayoffStrategy.AVALANCHE]
        val minimumRes = strategyResults[PayoffStrategy.MINIMUM_ONLY]

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.success(
                generateLocalDebtAdvice(
                    debts, selectedStrategy, extraMonthlyBudget,
                    snowballRes, avalancheRes, minimumRes,
                    highestAprDebt, lowestBalDebt, totalBalance,
                    pinnedDebts, promoDebts, windfallAmount, consolidationApr
                )
            )
        }

        val debtSummarySb = StringBuilder()
        debts.forEachIndexed { idx, d ->
            val promoInfo = if (d.promoApr != null) " [PROMO: ${d.promoApr}% until ${d.promoExpirationDate ?: "N/A"}]" else ""
            val pinInfo = if (d.customPriorityRank > 0) " [PINNED PRIORITY #${d.customPriorityRank}]" else ""
            debtSummarySb.append("- ${idx + 1}. ${d.name} (${d.category}): $${String.format(Locale.US, "%.2f", d.balance)} balance @ ${d.interestRate}% APR, min payment $${String.format(Locale.US, "%.2f", d.minimumPayment)}/mo$promoInfo$pinInfo\n")
        }

        val promptText = buildString {
            append("Analyze this user's debt portfolio and provide concise, highly practical, actionable debt payoff suggestions with modeling scenarios:\n\n")
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
            if (promoDebts.isNotEmpty()) {
                append("- Promotional / 0% APR Debts: ${promoDebts.joinToString { "${it.name} (${it.promoApr}% exp: ${it.promoExpirationDate ?: "N/A"})" }}\n")
            }
            if (pinnedDebts.isNotEmpty()) {
                append("- User-Pinned Custom Priorities: ${pinnedDebts.joinToString { "#${it.customPriorityRank}: ${it.name}" }}\n")
            }
            if (windfallAmount > 0.0) {
                append("- One-Time Windfall Available: $${String.format(Locale.US, "%.2f", windfallAmount)}\n")
            }
            if (consolidationApr != null) {
                append("- Consolidation Loan / Balance Transfer Option APR: ${String.format(Locale.US, "%.2f", consolidationApr)}%\n")
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
                append("\nUSER SPECIFIC QUESTION / SCENARIO:\n")
                append(customPrompt)
                append("\n")
            } else {
                append("\nPLEASE COVER:\n")
                append("1. **Optimal Payoff Strategy**: Snowball vs Avalanche recommendation, factoring in pinned priorities and promo APR expiration dates.\n")
                append("2. **Promo APR Alert**: Specific payoff pace required before promo expiration to avoid deferred interest cliffs.\n")
                if (windfallAmount > 0.0) {
                    append("3. **Windfall Impact**: Exactly where applying the $${String.format(Locale.US, "%,.0f", windfallAmount)} lump sum yields the greatest interest savings or quickest payoff.\n")
                }
                if (consolidationApr != null) {
                    append("4. **Consolidation Analysis**: Whether rolling high-rate cards into a ${consolidationApr}% consolidation loan is mathematically beneficial.\n")
                }
                append("5. **Immediate Next Step**: The single most impactful action the user should take right now.\n")
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
                        highestAprDebt, lowestBalDebt, totalBalance,
                        pinnedDebts, promoDebts, windfallAmount, consolidationApr
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
                        highestAprDebt, lowestBalDebt, totalBalance,
                        pinnedDebts, promoDebts, windfallAmount, consolidationApr
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
                    highestAprDebt, lowestBalDebt, totalBalance,
                    pinnedDebts, promoDebts, windfallAmount, consolidationApr
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
        totalBalance: Double,
        pinnedDebts: List<DebtEntity> = emptyList(),
        promoDebts: List<DebtEntity> = emptyList(),
        windfallAmount: Double = 0.0,
        consolidationApr: Double? = null
    ): String = buildString {
        append("### 🎯 Strategic Debt Payoff Analysis\n\n")

        if (pinnedDebts.isNotEmpty()) {
            val topPinned = pinnedDebts.first()
            append("• **Pinned Priority Overrides Active**: **${topPinned.name}** is pinned as #1 Priority. All accelerated budget ($${String.format(Locale.US, "%,.2f", extraMonthlyBudget)}/mo) will route here first before reverting to ${selectedStrategy.name}.\n")
        }

        if (promoDebts.isNotEmpty()) {
            val promo = promoDebts.first()
            val expInfo = promo.promoExpirationDate?.let { "expires on $it" } ?: "active promo"
            append("• **Promo APR Alert**: **${promo.name}** is currently at ${promo.promoApr ?: 0.0}% ($expInfo). Pay off its balance ($${String.format(Locale.US, "%,.2f", promo.balance)}) before expiration to prevent retroactive standard interest charges (${promo.interestRate}% APR)!\n")
        }
        
        if (avalancheRes != null && snowballRes != null) {
            val interestDiff = snowballRes.totalInterestPaid - avalancheRes.totalInterestPaid
            if (interestDiff > 50.0) {
                append("• **Optimal Strategy**: **Debt Avalanche** will save you **$${String.format(Locale.US, "%,.2f", interestDiff)}** in total interest charges compared to Snowball by eliminating high APR balances first.\n")
            } else {
                append("• **Optimal Strategy**: **Debt Snowball** is ideal here! The interest difference is minimal ($${String.format(Locale.US, "%,.2f", Math.abs(interestDiff))}), but Snowball will give you quick psychological momentum by knocking out accounts faster.\n")
            }
        }

        if (pinnedDebts.isEmpty()) {
            if (highestAprDebt != null && highestAprDebt.interestRate >= 18.0) {
                append("• **Priority Target**: **${highestAprDebt.name}** is charging **${highestAprDebt.interestRate}% APR**. Focus all extra monthly contributions ($${String.format(Locale.US, "%,.2f", extraMonthlyBudget)}/mo) here to stop compounding interest.\n")
            } else if (lowestBalDebt != null) {
                append("• **Quick Win Target**: **${lowestBalDebt.name}** has the lowest balance ($${String.format(Locale.US, "%,.2f", lowestBalDebt.balance)}). Eliminating it first frees up $${String.format(Locale.US, "%,.2f", lowestBalDebt.minimumPayment)}/mo in cash flow.\n")
            }
        }

        if (windfallAmount > 0.0) {
            val targetForWindfall = pinnedDebts.firstOrNull() ?: highestAprDebt ?: lowestBalDebt
            if (targetForWindfall != null) {
                val remainingAfter = maxOf(0.0, targetForWindfall.balance - windfallAmount)
                if (remainingAfter <= 0.0) {
                    append("• **Windfall Impact**: Applying your $${String.format(Locale.US, "%,.0f", windfallAmount)} windfall instantly wipes out **${targetForWindfall.name}**, immediately freeing up $${String.format(Locale.US, "%,.2f", targetForWindfall.minimumPayment)}/mo!\n")
                } else {
                    append("• **Windfall Impact**: Applying $${String.format(Locale.US, "%,.0f", windfallAmount)} to **${targetForWindfall.name}** slashes its principal from $${String.format(Locale.US, "%,.2f", targetForWindfall.balance)} down to $${String.format(Locale.US, "%,.2f", remainingAfter)}, eliminating significant interest accrual!\n")
                }
            }
        }

        if (consolidationApr != null) {
            val avgRate = debts.map { it.interestRate }.average()
            if (consolidationApr < avgRate) {
                val rateSavings = avgRate - consolidationApr
                append("• **Consolidation Simulation**: Consolidating at ${String.format(Locale.US, "%.1f", consolidationApr)}% APR beats your average ${String.format(Locale.US, "%.1f", avgRate)}% APR by **${String.format(Locale.US, "%.1f", rateSavings)} percentage points**, which could significantly lower your monthly payments and interest.\n")
            } else {
                append("• **Consolidation Note**: The ${String.format(Locale.US, "%.1f", consolidationApr)}% APR is higher than or equal to your current weighted rate (${String.format(Locale.US, "%.1f", avgRate)}%); keeping your current targeted payoff plan is likely better.\n")
            }
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

    suspend fun simulateBalanceTransfer(
        debts: List<DebtEntity>,
        transferBalance: Double,
        transferFeePercent: Double,
        promoApr: Double,
        promoDurationMonths: Int,
        targetMonthlyPayment: Double,
        currentApr: Double
    ): Result<String> = withContext(Dispatchers.IO) {
        val upfrontFee = transferBalance * (transferFeePercent / 100.0)
        val newBalance = transferBalance + upfrontFee
        val monthlyRateCurrent = (currentApr / 100.0) / 12.0
        val estimatedCurrentInterest = transferBalance * monthlyRateCurrent * promoDurationMonths
        val monthlyRatePromo = (promoApr / 100.0) / 12.0
        val estimatedPromoInterest = newBalance * monthlyRatePromo * promoDurationMonths
        val netSavings = estimatedCurrentInterest - (upfrontFee + estimatedPromoInterest)
        val requiredMonthlyPayment = newBalance / maxOf(1, promoDurationMonths)

        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Throwable) {
            ""
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            val localAnalysis = buildString {
                append("### 🔄 Balance Transfer & Consolidation Analysis\n\n")
                append("• **Transfer Fee**: $${String.format(Locale.US, "%,.2f", upfrontFee)} (${transferFeePercent}% on $${String.format(Locale.US, "%,.2f", transferBalance)})\n")
                append("• **Total Starting Balance**: $${String.format(Locale.US, "%,.2f", newBalance)}\n")
                append("• **Interest on Current Card (${currentApr}% APR)**: ~$${String.format(Locale.US, "%,.2f", estimatedCurrentInterest)} over $promoDurationMonths months\n")
                append("• **Promo Interest (${promoApr}% APR)**: ~$${String.format(Locale.US, "%,.2f", estimatedPromoInterest)}\n")
                if (netSavings > 0) {
                    append("• **Net Savings (After Fee)**: **+$${String.format(Locale.US, "%,.2f", netSavings)}** 🎉\n")
                    append("• **Verdict**: **Worth it!** You save substantially even after paying the $${String.format(Locale.US, "%,.2f", upfrontFee)} transfer fee.\n")
                } else {
                    append("• **Net Savings (After Fee)**: **-$${String.format(Locale.US, "%,.2f", Math.abs(netSavings))}** ⚠️\n")
                    append("• **Verdict**: **Proceed with caution.** The upfront transfer fee outweighs interest savings over this duration.\n")
                }
                append("• **Required Payoff Pace**: Pay **$${String.format(Locale.US, "%,.2f", requiredMonthlyPayment)}/mo** to completely wipe out the balance before the $promoDurationMonths-month promo ends and standard rates kick in.\n")
                append("• **Key Risk**: Ensure you never miss a minimum payment deadline, as many card issuers revoke 0% promotional rates and apply retroactive default APR (up to 29.99%) upon a late payment.")
            }
            return@withContext Result.success(localAnalysis)
        }

        try {
            val promptText = """
                Analyze this credit card balance transfer scenario:
                - Transfer Amount: $${String.format(Locale.US, "%.2f", transferBalance)}
                - Transfer Fee: ${transferFeePercent}% ($${String.format(Locale.US, "%.2f", upfrontFee)})
                - Promotional APR: ${promoApr}% for $promoDurationMonths months
                - Current Debt APR: ${currentApr}%
                - User's Planned Monthly Payment: $${String.format(Locale.US, "%.2f", targetMonthlyPayment)}
                - Required Monthly Payment to clear before promo ends: $${String.format(Locale.US, "%.2f", requiredMonthlyPayment)}

                Provide an objective, concise assessment answering:
                1. Math verdict: Is it mathematically worth transferring considering the upfront fee vs interest saved?
                2. Risk factors (retroactive interest, missed payment penalties, credit utilization impacts).
                3. Clear action steps to execute successfully.
            """.trimIndent()

            val requestBodyJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", promptText)
                            })
                        })
                    })
                })
            }

            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBodyJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                throw Exception("HTTP ${response.code}: ${response.message}")
            }
            val responseBody = response.body?.string() ?: ""
            val json = JSONObject(responseBody)
            val candidates = json.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val answer = parts?.optJSONObject(0)?.optString("text", "") ?: ""
            Result.success(answer.ifBlank { "Transfer analysis complete." })
        } catch (e: Exception) {
            Log.w(TAG, "Balance transfer query failed, using local math", e)
            val fallback = buildString {
                append("### 🔄 Balance Transfer & Consolidation Analysis\n\n")
                append("• **Transfer Fee**: $${String.format(Locale.US, "%,.2f", upfrontFee)} (${transferFeePercent}% fee)\n")
                append("• **Total New Balance**: $${String.format(Locale.US, "%,.2f", newBalance)}\n")
                append("• **Estimated Net Savings**: **$${String.format(Locale.US, "%,.2f", netSavings)}** over $promoDurationMonths months.\n")
                append("• **Clear Before Expiry**: Pay **$${String.format(Locale.US, "%,.2f", requiredMonthlyPayment)}/mo** to eliminate the balance before the promo rate ends.")
            }
            Result.success(fallback)
        }
    }
}
