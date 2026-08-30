package com.example.data

import android.graphics.Bitmap
import android.util.Base64
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
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

data class ParsedTransactionResult(
    val title: String,
    val amount: Double,
    val date: LocalDate,
    val category: String,
    val isIncome: Boolean,
    val isRecurring: Boolean,
    val accountName: String,
    val isAutoPay: Boolean,
    val reminderDaysBefore: Int,
    val explanation: String
)

data class ReceiptScanResult(
    val merchantName: String,
    val totalAmount: Double,
    val date: LocalDate,
    val category: String,
    val itemsSummary: List<String>,
    val taxAmount: Double?,
    val isPaid: Boolean,
    val notes: String
)

data class CashFlowForecastResult(
    val safeToSpendPerDay: Double,
    val safeToSpendPerWeek: Double,
    val projectedEndingBalance: Double,
    val riskLevel: String, // "LOW", "MODERATE", "HIGH"
    val summary: String,
    val recommendations: List<String>,
    val criticalDates: List<String>
)

data class WhatIfSimulationResult(
    val scenario: String,
    val monthlyImpact: Double,
    val threeMonthImpact: Double,
    val annualSavings: Double,
    val feasibilityAssessment: String,
    val keyTakeaways: List<String>,
    val strategicAdvice: String
)

data class SubscriptionAuditItem(
    val name: String,
    val currentAmount: Double,
    val previousAmount: Double?,
    val priceChangePct: Double?,
    val frequency: String,
    val annualCost: Double,
    val isCreeping: Boolean,
    val recommendation: String
)

data class SubscriptionAuditResult(
    val totalMonthlySubscriptionSpend: Double,
    val totalAnnualSubscriptionSpend: Double,
    val items: List<SubscriptionAuditItem>,
    val overallInsights: String
)

object GeminiFinancialService {

    private const val TAG = "GeminiFinancialService"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private fun getApiKey(): String {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }
        return if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") "" else apiKey
    }

    private fun Bitmap.toBase64(): String {
        val maxDim = 1280
        val ratio = Math.min(maxDim.toFloat() / width, maxDim.toFloat() / height)
        val targetWidth = if (ratio < 1.0f) (width * ratio).toInt() else width
        val targetHeight = if (ratio < 1.0f) (height * ratio).toInt() else height
        
        val scaled = if (ratio < 1.0f) Bitmap.createScaledBitmap(this, targetWidth, targetHeight, true) else this
        val outputStream = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    // 1. Natural Language Quick-Entry
    suspend fun parseNaturalLanguageTransaction(
        userInput: String,
        availableAccounts: List<String>,
        activeAccount: String
    ): Result<ParsedTransactionResult> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            return@withContext Result.success(
                parseLocalNaturalLanguageTransaction(userInput, availableAccounts, activeAccount)
            )
        }

        val today = LocalDate.now()
        val prompt = buildString {
            append("You are an intelligent budget parser. Convert this user's natural language financial entry into structured transaction JSON:\n\n")
            append("Input text: \"$userInput\"\n\n")
            append("Context:\n")
            append("- Today's date: ${today.format(DateTimeFormatter.ISO_LOCAL_DATE)} (${today.dayOfWeek})\n")
            append("- Active account: $activeAccount\n")
            append("- Available accounts: ${availableAccounts.joinToString(", ")}\n\n")
            append("Standard categories for expenses: Groceries, Dining Out, Utilities, Rent/Mortgage, Transportation, Subscriptions, Health, Shopping, Entertainment, Debt, Other.\n")
            append("Standard categories for income: Salary, Freelance, Gift, Investment, Refund, Other.\n\n")
            append("Output MUST be a strict JSON object with these exact keys:\n")
            append("{\n")
            append("  \"title\": \"Clear short title (e.g. Trader Joe's, Electric Bill, Paycheck)\",\n")
            append("  \"amount\": 45.50,\n")
            append("  \"date\": \"YYYY-MM-DD\",\n")
            append("  \"category\": \"Category name\",\n")
            append("  \"isIncome\": false,\n")
            append("  \"isRecurring\": false,\n")
            append("  \"accountName\": \"Account matching available accounts or Primary Account\",\n")
            append("  \"isAutoPay\": false,\n")
            append("  \"reminderDaysBefore\": 0,\n")
            append("  \"explanation\": \"Brief confirmation note\"\n")
            append("}\n")
            append("Do not include markdown code fence formatting or surrounding text, just valid JSON.")
        }

        try {
            val jsonPayload = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.1)
                })
            }

            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(jsonPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            val respString = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                Log.w(TAG, "Gemini API error: ${response.code} $respString, using smart local parser")
                return@withContext Result.success(
                    parseLocalNaturalLanguageTransaction(userInput, availableAccounts, activeAccount)
                )
            }

            val rawText = extractTextFromGeminiResponse(respString)
            val cleanJson = cleanJsonString(rawText)
            if (cleanJson.isBlank()) {
                return@withContext Result.success(
                    parseLocalNaturalLanguageTransaction(userInput, availableAccounts, activeAccount)
                )
            }
            val json = JSONObject(cleanJson)

            val title = json.optString("title", userInput).ifBlank { "Quick Entry" }
            val amount = Math.abs(json.optDouble("amount", 0.0))
            val dateStr = json.optString("date", today.format(DateTimeFormatter.ISO_LOCAL_DATE))
            val date = try {
                LocalDate.parse(dateStr)
            } catch (e: Exception) {
                today
            }
            val isIncome = json.optBoolean("isIncome", false)
            val category = json.optString("category", if (isIncome) "Salary" else "Other")
            val isRecurring = json.optBoolean("isRecurring", false)
            val accountName = json.optString("accountName", activeAccount).ifBlank { activeAccount }
            val isAutoPay = json.optBoolean("isAutoPay", false)
            val reminderDaysBefore = json.optInt("reminderDaysBefore", 0)
            val explanation = json.optString("explanation", "Parsed from: \"$userInput\"")

            Result.success(
                ParsedTransactionResult(
                    title = title,
                    amount = if (amount > 0) amount else 20.0,
                    date = date,
                    category = category,
                    isIncome = isIncome,
                    isRecurring = isRecurring,
                    accountName = if (accountName == "All Accounts") "Primary Account" else accountName,
                    isAutoPay = isAutoPay,
                    reminderDaysBefore = reminderDaysBefore,
                    explanation = explanation
                )
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse natural language transaction via Gemini, fallback to local", e)
            Result.success(parseLocalNaturalLanguageTransaction(userInput, availableAccounts, activeAccount))
        }
    }

    // 2. Smart Receipt & Bill Scanner (Vision AI)
    suspend fun scanReceiptOrBill(
        bitmap: Bitmap,
        defaultAccount: String
    ): Result<ReceiptScanResult> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("Gemini API Key is not configured. Please add GEMINI_API_KEY in the Secrets panel in AI Studio.")
            )
        }

        val base64Image = bitmap.toBase64()
        val today = LocalDate.now()
        val prompt = buildString {
            append("You are an expert OCR receipt and bill extraction engine. Analyze this image of a receipt, invoice, or utility bill.\n")
            append("Extract all key transaction details and return ONLY a JSON object:\n")
            append("{\n")
            append("  \"merchantName\": \"Name of store/vendor/biller\",\n")
            append("  \"totalAmount\": 0.00,\n")
            append("  \"date\": \"YYYY-MM-DD (or ${today.format(DateTimeFormatter.ISO_LOCAL_DATE)} if unreadable)\",\n")
            append("  \"category\": \"Groceries | Dining Out | Utilities | Transportation | Shopping | Health | Subscriptions | Other\",\n")
            append("  \"itemsSummary\": [\"Item 1 \\\$0.00\", \"Item 2 \\\$0.00\"],\n")
            append("  \"taxAmount\": 0.00,\n")
            append("  \"isPaid\": true,\n")
            append("  \"notes\": \"Short summary of items\"\n")
            append("}\n")
            append("Output strict JSON only with no markdown wrapping.")
        }

        try {
            val jsonPayload = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                            put(JSONObject().apply {
                                put("inlineData", JSONObject().apply {
                                    put("mimeType", "image/jpeg")
                                    put("data", base64Image)
                                })
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.1)
                })
            }

            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(jsonPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            val respString = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                val err = extractErrorMessage(respString) ?: "Vision API error (${response.code})"
                Log.e(TAG, "Gemini vision error: $err")
                return@withContext Result.failure(Exception(err))
            }

            val rawText = extractTextFromGeminiResponse(respString)
            val cleanJson = cleanJsonString(rawText)
            if (cleanJson.isBlank()) {
                return@withContext Result.failure(Exception("Unable to read text from image. Please ensure good lighting."))
            }
            val json = JSONObject(cleanJson)

            val merchantName = json.optString("merchantName", "Scanned Receipt").ifBlank { "Scanned Receipt" }
            val totalAmount = Math.abs(json.optDouble("totalAmount", 0.0))
            val dateStr = json.optString("date", today.format(DateTimeFormatter.ISO_LOCAL_DATE))
            val date = try {
                LocalDate.parse(dateStr)
            } catch (e: Exception) {
                today
            }
            val category = json.optString("category", "Groceries")
            val tax = if (json.has("taxAmount") && !json.isNull("taxAmount")) json.optDouble("taxAmount") else null
            val isPaid = json.optBoolean("isPaid", true)
            val notes = json.optString("notes", "")

            val itemsList = mutableListOf<String>()
            val itemsJson = json.optJSONArray("itemsSummary")
            if (itemsJson != null) {
                for (i in 0 until itemsJson.length()) {
                    itemsList.add(itemsJson.optString(i))
                }
            }

            Result.success(
                ReceiptScanResult(
                    merchantName = merchantName,
                    totalAmount = totalAmount,
                    date = date,
                    category = category,
                    itemsSummary = itemsList,
                    taxAmount = tax,
                    isPaid = isPaid,
                    notes = notes
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to scan receipt", e)
            Result.failure(Exception(e.message ?: "Failed to parse receipt image."))
        }
    }

    // 3. Pay Cycle Cash Flow Forecasting & "Safe-to-Spend" Advisor
    suspend fun generateCashFlowForecast(
        payPeriod: PayPeriod,
        recentTransactions: List<TransactionEntity>
    ): Result<CashFlowForecastResult> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            return@withContext Result.success(calculateLocalCashFlowForecast(payPeriod, recentTransactions))
        }

        val today = LocalDate.now()
        val daysTotal = Math.max(1, java.time.temporal.ChronoUnit.DAYS.between(payPeriod.startDate, payPeriod.endDate).toInt() + 1)
        val daysRemaining = Math.max(1, java.time.temporal.ChronoUnit.DAYS.between(if (today.isAfter(payPeriod.startDate)) today else payPeriod.startDate, payPeriod.endDate).toInt() + 1)

        val completedExpenses = payPeriod.transactions.filter { !it.isIncome && it.isCompleted }.sumOf { it.amount }
        val pendingExpenses = payPeriod.transactions.filter { !it.isIncome && !it.isCompleted }.sumOf { it.amount }
        val totalIncome = payPeriod.totalIncomeWithCarryover

        val prompt = buildString {
            append("You are an expert financial cash flow forecaster and safe-to-spend advisor for pay cycle budgeting.\n")
            append("CURRENT PAY CYCLE METRICS:\n")
            append("- Cycle Range: ${payPeriod.startDate} to ${payPeriod.endDate} ($daysTotal days total, $daysRemaining days remaining)\n")
            append("- Total Income (Paycheck + Carryover + Other): $${String.format(Locale.US, "%.2f", totalIncome)}\n")
            append("- Expenses Paid / Cleared so far: $${String.format(Locale.US, "%.2f", completedExpenses)}\n")
            append("- Pending / Upcoming scheduled bills: $${String.format(Locale.US, "%.2f", pendingExpenses)}\n")
            append("- Net Remaining Balance: $${String.format(Locale.US, "%.2f", payPeriod.remainingBalance)}\n\n")
            append("SCHEDULED TRANSACTIONS IN THIS CYCLE:\n")
            payPeriod.transactions.forEach { tx ->
                val status = if (tx.isCompleted) "PAID" else "PENDING"
                val date = DateUtils.millisToLocalDate(tx.dateMillis)
                append("- [$status] ${tx.title} ($${String.format(Locale.US, "%.2f", tx.amount)}) on $date [${tx.category}]\n")
            }
            append("\nAnalyze cash flow risk and output ONLY strict JSON:\n")
            append("{\n")
            append("  \"safeToSpendPerDay\": 0.00,\n")
            append("  \"safeToSpendPerWeek\": 0.00,\n")
            append("  \"projectedEndingBalance\": 0.00,\n")
            append("  \"riskLevel\": \"LOW | MODERATE | HIGH\",\n")
            append("  \"summary\": \"2 sentence executive diagnosis\",\n")
            append("  \"recommendations\": [\"Tip 1\", \"Tip 2\", \"Tip 3\"],\n")
            append("  \"criticalDates\": [\"High-impact expense date or danger zone\"]\n")
            append("}\n")
            append("Strict JSON only.")
        }

        try {
            val jsonPayload = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.2)
                })
            }

            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(jsonPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            val respString = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                Log.w(TAG, "Gemini forecast API error: ${response.code} $respString, falling back to local calculation")
                return@withContext Result.success(calculateLocalCashFlowForecast(payPeriod, recentTransactions))
            }

            val rawText = extractTextFromGeminiResponse(respString)
            val cleanJson = cleanJsonString(rawText)
            if (cleanJson.isBlank()) {
                return@withContext Result.success(calculateLocalCashFlowForecast(payPeriod, recentTransactions))
            }
            val json = JSONObject(cleanJson)

            val recs = mutableListOf<String>()
            val recsArray = json.optJSONArray("recommendations")
            if (recsArray != null) {
                for (i in 0 until recsArray.length()) recs.add(recsArray.optString(i))
            }

            val dates = mutableListOf<String>()
            val datesArray = json.optJSONArray("criticalDates")
            if (datesArray != null) {
                for (i in 0 until datesArray.length()) dates.add(datesArray.optString(i))
            }

            val defaultDaily = Math.max(0.0, payPeriod.remainingBalance / daysRemaining)
            Result.success(
                CashFlowForecastResult(
                    safeToSpendPerDay = json.optDouble("safeToSpendPerDay", defaultDaily),
                    safeToSpendPerWeek = json.optDouble("safeToSpendPerWeek", defaultDaily * 7.0),
                    projectedEndingBalance = json.optDouble("projectedEndingBalance", payPeriod.remainingBalance),
                    riskLevel = json.optString("riskLevel", if (payPeriod.remainingBalance > 100) "LOW" else if (payPeriod.remainingBalance >= 0) "MODERATE" else "HIGH"),
                    summary = json.optString("summary", "Your pay cycle forecast indicates safe discretionary pacing through ${payPeriod.endDate}."),
                    recommendations = recs.ifEmpty { listOf("Track upcoming bills to ensure you maintain a safety buffer.") },
                    criticalDates = dates
                )
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to generate cash flow forecast via AI, falling back to local computation", e)
            Result.success(calculateLocalCashFlowForecast(payPeriod, recentTransactions))
        }
    }

    // 4. AI "What-If" Scenario Simulator
    suspend fun simulateWhatIfScenario(
        scenarioQuery: String,
        payPeriods: List<PayPeriod>,
        debts: List<DebtEntity>,
        transactions: List<TransactionEntity>
    ): Result<WhatIfSimulationResult> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            return@withContext Result.success(calculateLocalWhatIfSimulation(scenarioQuery, payPeriods, debts, transactions))
        }

        val totalDebt = debts.sumOf { it.balance }
        val avgCycleRemaining = if (payPeriods.isNotEmpty()) payPeriods.map { it.remainingBalance }.average() else 0.0
        val recentMonthlyExpense = transactions.filter { !it.isIncome }.sumOf { it.amount }

        val prompt = buildString {
            append("You are a financial planning simulation engine. Evaluate this hypothetical financial scenario:\n\n")
            append("USER SCENARIO: \"$scenarioQuery\"\n\n")
            append("FINANCIAL CONTEXT:\n")
            append("- Current Active Debts: ${debts.size} debts, Total Balance: $${String.format(Locale.US, "%.2f", totalDebt)}\n")
            append("- Average Pay Cycle Remaining Surplus: $${String.format(Locale.US, "%.2f", avgCycleRemaining)}\n")
            append("- Total Expenses Recorded: $${String.format(Locale.US, "%.2f", recentMonthlyExpense)}\n\n")
            append("Calculate the concrete financial impact and return ONLY strict JSON:\n")
            append("{\n")
            append("  \"scenario\": \"Concise title of scenario\",\n")
            append("  \"monthlyImpact\": 150.00,\n")
            append("  \"threeMonthImpact\": 450.00,\n")
            append("  \"annualSavings\": 1800.00,\n")
            append("  \"feasibilityAssessment\": \"Highly Achievable | Moderate | Aggressive\",\n")
            append("  \"keyTakeaways\": [\"Point 1\", \"Point 2\", \"Point 3\"],\n")
            append("  \"strategicAdvice\": \"Direct recommendations on how to implement this smoothly\"\n")
            append("}\n")
            append("Strict JSON only.")
        }

        try {
            val jsonPayload = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.2)
                })
            }

            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(jsonPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            val respString = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                Log.w(TAG, "Gemini simulation API error: ${response.code} $respString, falling back to local simulation")
                return@withContext Result.success(calculateLocalWhatIfSimulation(scenarioQuery, payPeriods, debts, transactions))
            }

            val rawText = extractTextFromGeminiResponse(respString)
            val cleanJson = cleanJsonString(rawText)
            if (cleanJson.isBlank()) {
                return@withContext Result.success(calculateLocalWhatIfSimulation(scenarioQuery, payPeriods, debts, transactions))
            }
            val json = JSONObject(cleanJson)

            val takeaways = mutableListOf<String>()
            val tkArray = json.optJSONArray("keyTakeaways")
            if (tkArray != null) {
                for (i in 0 until tkArray.length()) takeaways.add(tkArray.optString(i))
            }

            Result.success(
                WhatIfSimulationResult(
                    scenario = json.optString("scenario", scenarioQuery),
                    monthlyImpact = json.optDouble("monthlyImpact", 0.0),
                    threeMonthImpact = json.optDouble("threeMonthImpact", 0.0),
                    annualSavings = json.optDouble("annualSavings", 0.0),
                    feasibilityAssessment = json.optString("feasibilityAssessment", "Achievable"),
                    keyTakeaways = takeaways.ifEmpty { listOf("Provides noticeable budget flexibility over time.") },
                    strategicAdvice = json.optString("strategicAdvice", "Apply the extra savings directly towards high-priority debts or emergency funds.")
                )
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to simulate what-if scenario via AI, falling back to local model", e)
            Result.success(calculateLocalWhatIfSimulation(scenarioQuery, payPeriods, debts, transactions))
        }
    }

    // 5. Subscription & Price Creep Auditor
    suspend fun auditSubscriptionsAndPriceCreep(
        transactions: List<TransactionEntity>
    ): Result<SubscriptionAuditResult> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            return@withContext Result.success(calculateLocalSubscriptionAudit(transactions))
        }

        val expenses = transactions.filter { !it.isIncome && it.amount > 0.0 }
        if (expenses.isEmpty()) {
            return@withContext Result.success(
                SubscriptionAuditResult(
                    totalMonthlySubscriptionSpend = 0.0,
                    totalAnnualSubscriptionSpend = 0.0,
                    items = emptyList(),
                    overallInsights = "No expense records found to audit yet. Add recurring bills or subscriptions to run an audit."
                )
            )
        }

        val txSb = StringBuilder()
        expenses.forEach { tx ->
            val d = DateUtils.millisToLocalDate(tx.dateMillis)
            txSb.append("- ${tx.title}: $${String.format(Locale.US, "%.2f", tx.amount)} on $d (${tx.category}, recurring=${tx.isRecurring})\n")
        }

        val prompt = buildString {
            append("You are an expert Subscription & Price Creep Auditor. Audit these expenses to identify all recurring subscriptions, memberships, software licenses, streaming services, and utility bills.\n")
            append("Detect any price creep (e.g. price increasing between dates) or redundant services.\n\n")
            append("TRANSACTION HISTORY:\n")
            append(txSb.toString())
            append("\nReturn ONLY strict JSON in this structure:\n")
            append("{\n")
            append("  \"totalMonthlySubscriptionSpend\": 0.00,\n")
            append("  \"totalAnnualSubscriptionSpend\": 0.00,\n")
            append("  \"items\": [\n")
            append("    {\n")
            append("      \"name\": \"Netflix\",\n")
            append("      \"currentAmount\": 17.99,\n")
            append("      \"previousAmount\": 15.49,\n")
            append("      \"priceChangePct\": 16.1,\n")
            append("      \"frequency\": \"Monthly\",\n")
            append("      \"annualCost\": 215.88,\n")
            append("      \"isCreeping\": true,\n")
            append("      \"recommendation\": \"Review plan or share family subscription\"\n")
            append("    }\n")
            append("  ],\n")
            append("  \"overallInsights\": \"Executive summary of subscription health and annual savings potential.\"\n")
            append("}\n")
            append("Strict JSON only.")
        }

        try {
            val jsonPayload = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.2)
                })
            }

            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(jsonPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            val respString = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                Log.w(TAG, "Gemini audit API error: ${response.code} $respString, falling back to local audit")
                return@withContext Result.success(calculateLocalSubscriptionAudit(transactions))
            }

            val rawText = extractTextFromGeminiResponse(respString)
            val cleanJson = cleanJsonString(rawText)
            if (cleanJson.isBlank()) {
                return@withContext Result.success(calculateLocalSubscriptionAudit(transactions))
            }
            val json = JSONObject(cleanJson)

            val items = mutableListOf<SubscriptionAuditItem>()
            val itemsArr = json.optJSONArray("items")
            if (itemsArr != null) {
                for (i in 0 until itemsArr.length()) {
                    val obj = itemsArr.optJSONObject(i) ?: continue
                    val curAmt = obj.optDouble("currentAmount", 0.0)
                    val prevAmt = if (obj.has("previousAmount") && !obj.isNull("previousAmount")) obj.optDouble("previousAmount") else null
                    val pct = if (obj.has("priceChangePct") && !obj.isNull("priceChangePct")) obj.optDouble("priceChangePct") else null

                    items.add(
                        SubscriptionAuditItem(
                            name = obj.optString("name", "Subscription"),
                            currentAmount = curAmt,
                            previousAmount = prevAmt,
                            priceChangePct = pct,
                            frequency = obj.optString("frequency", "Monthly"),
                            annualCost = obj.optDouble("annualCost", curAmt * 12),
                            isCreeping = obj.optBoolean("isCreeping", false),
                            recommendation = obj.optString("recommendation", "Active subscription")
                        )
                    )
                }
            }

            Result.success(
                SubscriptionAuditResult(
                    totalMonthlySubscriptionSpend = json.optDouble("totalMonthlySubscriptionSpend", items.sumOf { it.currentAmount }),
                    totalAnnualSubscriptionSpend = json.optDouble("totalAnnualSubscriptionSpend", items.sumOf { it.annualCost }),
                    items = items.ifEmpty { calculateLocalSubscriptionAudit(transactions).items },
                    overallInsights = json.optString("overallInsights", "Audited subscriptions across your account history.")
                )
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to audit subscriptions via AI, falling back to local audit", e)
            Result.success(calculateLocalSubscriptionAudit(transactions))
        }
    }

    private fun extractErrorMessage(respJsonString: String): String? {
        return try {
            val json = JSONObject(respJsonString)
            json.optJSONObject("error")?.optString("message")
        } catch (e: Exception) {
            null
        }
    }

    private fun extractTextFromGeminiResponse(respJsonString: String): String {
        if (respJsonString.isBlank()) return ""
        val respJson = JSONObject(respJsonString)
        if (respJson.has("error")) {
            val errObj = respJson.optJSONObject("error")
            val errMsg = errObj?.optString("message") ?: "Gemini API returned an error."
            throw IllegalStateException(errMsg)
        }
        val candidates = respJson.optJSONArray("candidates") ?: return ""
        if (candidates.length() == 0) return ""
        val firstCandidate = candidates.optJSONObject(0) ?: return ""
        val content = firstCandidate.optJSONObject("content") ?: return ""
        val parts = content.optJSONArray("parts") ?: return ""
        return parts.optJSONObject(0)?.optString("text") ?: ""
    }

    private fun cleanJsonString(raw: String): String {
        var str = raw.trim()
        val firstBrace = str.indexOf('{')
        val lastBrace = str.lastIndexOf('}')
        if (firstBrace != -1 && lastBrace != -1 && lastBrace >= firstBrace) {
            return str.substring(firstBrace, lastBrace + 1).trim()
        }
        if (str.startsWith("```json", ignoreCase = true)) {
            str = str.substring(7)
        } else if (str.startsWith("```")) {
            str = str.substring(3)
        }
        if (str.endsWith("```")) {
            str = str.substring(0, str.length - 3)
        }
        return str.trim()
    }

    // --------------------------------------------------------------------------------------------
    // Local Resilient Financial Engines (Rule-based Fallbacks)
    // --------------------------------------------------------------------------------------------

    private fun calculateLocalCashFlowForecast(
        payPeriod: PayPeriod,
        recentTransactions: List<TransactionEntity>
    ): CashFlowForecastResult {
        val today = LocalDate.now()
        val daysTotal = Math.max(1, java.time.temporal.ChronoUnit.DAYS.between(payPeriod.startDate, payPeriod.endDate).toInt() + 1)
        val daysRemaining = Math.max(1, java.time.temporal.ChronoUnit.DAYS.between(if (today.isAfter(payPeriod.startDate)) today else payPeriod.startDate, payPeriod.endDate).toInt() + 1)

        val completedExpenses = payPeriod.transactions.filter { !it.isIncome && it.isCompleted }.sumOf { it.amount }
        val pendingExpenses = payPeriod.transactions.filter { !it.isIncome && !it.isCompleted }.sumOf { it.amount }
        val remaining = payPeriod.remainingBalance

        val safePerDay = Math.max(0.0, remaining / daysRemaining)
        val safePerWeek = safePerDay * 7.0
        val riskLevel = when {
            remaining > 200 -> "LOW"
            remaining >= 0 -> "MODERATE"
            else -> "HIGH"
        }

        val summary = if (remaining >= 0) {
            "You have $${String.format(Locale.US, "%,.2f", remaining)} projected buffer remaining across $daysRemaining days, giving you $${String.format(Locale.US, "%,.2f", safePerDay)}/day in safe discretionary spending."
        } else {
            "Caution: Pending commitments exceed available cycle funds by $${String.format(Locale.US, "%,.2f", Math.abs(remaining))}. Consider deferring optional expenses."
        }

        val recs = mutableListOf<String>()
        recs.add("Pace discretionary spending to around $${String.format(Locale.US, "%,.2f", safePerDay)}/day to finish the cycle with buffer.")
        if (pendingExpenses > 0) {
            recs.add("Hold $${String.format(Locale.US, "%,.2f", pendingExpenses)} in checking to ensure upcoming scheduled bills clear safely.")
        }
        if (remaining > 300) {
            recs.add("Consider transferring $${String.format(Locale.US, "%,.0f", remaining * 0.4)} towards debt reduction or high-yield savings.")
        }

        val criticalDates = payPeriod.transactions
            .filter { !it.isIncome && !it.isCompleted && it.amount >= 40.0 }
            .sortedBy { it.dateMillis }
            .map { "${DateUtils.millisToLocalDate(it.dateMillis)}: ${it.title} ($${String.format(Locale.US, "%,.2f", it.amount)})" }

        return CashFlowForecastResult(
            safeToSpendPerDay = safePerDay,
            safeToSpendPerWeek = safePerWeek,
            projectedEndingBalance = remaining,
            riskLevel = riskLevel,
            summary = summary,
            recommendations = recs,
            criticalDates = criticalDates
        )
    }

    private fun calculateLocalWhatIfSimulation(
        scenarioQuery: String,
        payPeriods: List<PayPeriod>,
        debts: List<DebtEntity>,
        transactions: List<TransactionEntity>
    ): WhatIfSimulationResult {
        val numMatch = Regex("""\$?([0-9]+(?:\.[0-9]{1,2})?)""").find(scenarioQuery)
        val parsedNum = numMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 100.0

        val isReductionOrIncome = scenarioQuery.contains("cut", ignoreCase = true) ||
                scenarioQuery.contains("save", ignoreCase = true) ||
                scenarioQuery.contains("cancel", ignoreCase = true) ||
                scenarioQuery.contains("refund", ignoreCase = true) ||
                scenarioQuery.contains("bonus", ignoreCase = true) ||
                scenarioQuery.contains("raise", ignoreCase = true) ||
                scenarioQuery.contains("reduce", ignoreCase = true)

        val monthly = if (isReductionOrIncome) parsedNum else -parsedNum
        val threeMonth = monthly * 3.0
        val annual = monthly * 12.0

        val feasibility = when {
            Math.abs(monthly) <= 150 -> "Highly Achievable"
            Math.abs(monthly) <= 400 -> "Moderate"
            else -> "Aggressive"
        }

        val takeaways = listOf(
            "Monthly net impact on budget: ${if (monthly >= 0) "+$" else "-$"}${String.format(Locale.US, "%,.2f", Math.abs(monthly))}/mo",
            "Compounded 3-month variance: ${if (threeMonth >= 0) "+$" else "-$"}${String.format(Locale.US, "%,.2f", Math.abs(threeMonth))}",
            "Annual total cash differential: ${if (annual >= 0) "+$" else "-$"}${String.format(Locale.US, "%,.2f", Math.abs(annual))}"
        )

        val advice = if (monthly > 0) {
            "Channeling this extra $${String.format(Locale.US, "%,.0f", monthly)}/mo towards your debts could shorten payoff by several months and save hundreds in interest."
        } else {
            "To absorb this $${String.format(Locale.US, "%,.0f", Math.abs(monthly))}/mo cost without debt, consider tightening grocery or dining categories by $${String.format(Locale.US, "%,.0f", Math.abs(monthly / 4.0))}/week."
        }

        return WhatIfSimulationResult(
            scenario = scenarioQuery,
            monthlyImpact = monthly,
            threeMonthImpact = threeMonth,
            annualSavings = annual,
            feasibilityAssessment = feasibility,
            keyTakeaways = takeaways,
            strategicAdvice = advice
        )
    }

    private fun calculateLocalSubscriptionAudit(
        transactions: List<TransactionEntity>
    ): SubscriptionAuditResult {
        val knownSubKeywords = listOf("netflix", "spotify", "hulu", "disney", "apple", "icloud", "amazon", "prime", "gym", "fitness", "youtube", "patreon", "sub", "membership", "internet", "wifi", "electric", "water", "gas", "insurance", "phone", "verizon", "t-mobile", "att")
        val candidateTx = transactions.filter { tx ->
            !tx.isIncome && (tx.isRecurring || knownSubKeywords.any { kw -> tx.title.contains(kw, ignoreCase = true) })
        }

        val grouped = candidateTx.groupBy { it.title.trim().lowercase(Locale.ROOT) }
        val items = mutableListOf<SubscriptionAuditItem>()

        for ((_, txs) in grouped) {
            val sorted = txs.sortedBy { it.dateMillis }
            val latest = sorted.last()
            val earliest = sorted.first()
            val hasPriceCreep = sorted.size > 1 && latest.amount > earliest.amount
            val priceChangePct = if (hasPriceCreep && earliest.amount > 0) {
                ((latest.amount - earliest.amount) / earliest.amount) * 100.0
            } else null

            items.add(
                SubscriptionAuditItem(
                    name = latest.title,
                    currentAmount = latest.amount,
                    previousAmount = if (hasPriceCreep) earliest.amount else null,
                    priceChangePct = priceChangePct,
                    frequency = "Monthly",
                    annualCost = latest.amount * 12.0,
                    isCreeping = hasPriceCreep,
                    recommendation = if (hasPriceCreep) "Price increased recently. Consider auditing your current plan." else "Active recurring subscription."
                )
            )
        }

        // If no candidates found, extract recurring flags or highest frequency expenses
        if (items.isEmpty()) {
            transactions.filter { !it.isIncome && it.isRecurring }.forEach { r ->
                items.add(
                    SubscriptionAuditItem(
                        name = r.title,
                        currentAmount = r.amount,
                        previousAmount = null,
                        priceChangePct = null,
                        frequency = "Monthly",
                        annualCost = r.amount * 12.0,
                        isCreeping = false,
                        recommendation = "Marked as recurring expense."
                    )
                )
            }
        }

        val totalMonthly = items.sumOf { it.currentAmount }
        val totalAnnual = items.sumOf { it.annualCost }

        return SubscriptionAuditResult(
            totalMonthlySubscriptionSpend = totalMonthly,
            totalAnnualSubscriptionSpend = totalAnnual,
            items = items,
            overallInsights = if (items.isNotEmpty()) {
                "Found ${items.size} recurring items totaling $${String.format(Locale.US, "%,.2f", totalMonthly)}/mo ($${String.format(Locale.US, "%,.2f", totalAnnual)}/yr)."
            } else {
                "No recurring subscriptions detected yet. Mark transactions as recurring or add utility bills to track price creep."
            }
        )
    }

    private fun parseLocalNaturalLanguageTransaction(
        userInput: String,
        availableAccounts: List<String>,
        activeAccount: String
    ): ParsedTransactionResult {
        val numMatch = Regex("""\$?([0-9]+(?:\.[0-9]{1,2})?)""").find(userInput)
        val amount = numMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 25.0

        val lower = userInput.lowercase(Locale.ROOT)
        val isIncome = lower.contains("paycheck") || lower.contains("salary") || lower.contains("income") || lower.contains("refund") || lower.contains("deposit") || lower.contains("freelance")
        val isRecurring = lower.contains("monthly") || lower.contains("bill") || lower.contains("subscription") || lower.contains("every month") || lower.contains("recurring")

        val category = when {
            isIncome -> "Salary"
            lower.contains("grocer") || lower.contains("food") || lower.contains("trader") || lower.contains("safeway") || lower.contains("kroger") || lower.contains("walmart") || lower.contains("costco") -> "Groceries"
            lower.contains("coffee") || lower.contains("dinner") || lower.contains("lunch") || lower.contains("starbucks") || lower.contains("mcdonald") || lower.contains("restaurant") || lower.contains("chipotle") -> "Dining Out"
            lower.contains("electric") || lower.contains("water") || lower.contains("gas") || lower.contains("power") || lower.contains("utility") || lower.contains("internet") || lower.contains("wifi") -> "Utilities"
            lower.contains("uber") || lower.contains("lyft") || lower.contains("gasoline") || lower.contains("fuel") || lower.contains("transit") || lower.contains("metro") || lower.contains("parking") -> "Transportation"
            lower.contains("rent") || lower.contains("mortgage") || lower.contains("lease") -> "Rent/Mortgage"
            lower.contains("netflix") || lower.contains("spotify") || lower.contains("gym") || lower.contains("hulu") || lower.contains("sub") -> "Subscriptions"
            lower.contains("doctor") || lower.contains("pharmacy") || lower.contains("med") || lower.contains("dentist") || lower.contains("cvs") -> "Health"
            lower.contains("target") || lower.contains("amazon") || lower.contains("clothes") || lower.contains("shoes") -> "Shopping"
            lower.contains("card") || lower.contains("debt") || lower.contains("loan") -> "Debt"
            else -> "Other"
        }

        // Clean title
        var title = userInput
            .replace(Regex("""\$?[0-9]+(?:\.[0-9]{1,2})?"""), "")
            .replace(Regex("""(?i)\b(spent|paid|bought|for|at|on|dollars?|bucks?)\b"""), "")
            .trim()
            .replace(Regex("""\s+"""), " ")
        if (title.isBlank()) title = category

        return ParsedTransactionResult(
            title = title.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() },
            amount = amount,
            date = LocalDate.now(),
            category = category,
            isIncome = isIncome,
            isRecurring = isRecurring,
            accountName = if (activeAccount == "All Accounts") "Primary Account" else activeAccount,
            isAutoPay = isRecurring,
            reminderDaysBefore = if (isRecurring) 3 else 0,
            explanation = "Parsed: $title • $${String.format(Locale.US, "%.2f", amount)} ($category)"
        )
    }
}

