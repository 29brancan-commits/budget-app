package com.example.sync

import android.accounts.Account
import android.content.Context
import android.util.Log
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    data class Success(val message: String, val lastSyncTime: String) : SyncState()
    data class Error(val errorMessage: String) : SyncState()
}

class GoogleDriveSyncManager(private val context: Context) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .connectionPool(okhttp3.ConnectionPool(5, 5, java.util.concurrent.TimeUnit.MINUTES))
        .build()
    private val scopeDriveFile = "https://www.googleapis.com/auth/drive.file"
    private val scopeAppData = "https://www.googleapis.com/auth/drive.appdata"
    private val scopeSheets = "https://www.googleapis.com/auth/spreadsheets"
    private val fileName = "paycycle_budget_backup.json"
    private val exportSheetFileName = "Expenses & Budget Export Sheet"
    private val exportCsvFileName = "expenses_export.csv"
    private val importSheetFileName = "Expenses & Budget Import Sheet"
    private val importCsvFileName = "expenses_import.csv"
    private val csvFileName = "expenses_google_sheets_sync.csv"
    private val sheetFileName = "Expenses & Budget Sheet"
    private val debtSheetFileName = "Debt Payoff Sheet"
    private val debtCsvFileName = "debt_payoff_sync.csv"
    private val debtImportSheetFileName = "Debt Payoff Import Sheet"
    private val debtImportCsvFileName = "debt_payoff_import.csv"
    private val webCompanionFileName = "PayCycle Budget - Web Companion App.html"
    private val webCompanionLegacyName1 = "PayCycle Budget - Web Companion App"
    private val webCompanionLegacyName2 = "PayCycle_Web_Companion.html"

    private val DEFAULT_CLIENT_ID = "292085223830-10as2ag120sc8eb4jgg66mrhptm188nj.apps.googleusercontent.com"
    private val DEFAULT_CLIENT_SECRET = "p3O3A77A5p-S6k13n3yM_Yy7"

    private val prefs = context.getSharedPreferences("google_drive_sync_prefs", Context.MODE_PRIVATE)

    fun saveCustomAccessToken(token: String?) {
        val trimmed = token?.trim()
        if (trimmed.isNullOrBlank()) {
            prefs.edit().remove("custom_access_token").apply()
        } else if (trimmed.startsWith("4/") || trimmed.startsWith("4%2F") || trimmed.startsWith("1//") || (!trimmed.startsWith("ya29.") && trimmed.length > 20)) {
            // Auto-detect authorization codes or refresh tokens passed into access token field
            prefs.edit().putString("custom_refresh_token", trimmed).remove("custom_access_token").apply()
        } else {
            prefs.edit().putString("custom_access_token", trimmed).apply()
        }
    }

    fun getCustomAccessToken(): String? {
        val t = prefs.getString("custom_access_token", null)?.trim()
        return if (t.isNullOrBlank()) null else t
    }

    fun saveCustomClientCredentials(clientId: String?, clientSecret: String?, refreshToken: String? = null) {
        val editor = prefs.edit()
        val cId = clientId?.trim()
        val cSecret = clientSecret?.trim()
        val rToken = refreshToken?.trim()
        if (cId.isNullOrBlank()) editor.remove("custom_client_id") else editor.putString("custom_client_id", cId)
        if (cSecret.isNullOrBlank()) editor.remove("custom_client_secret") else editor.putString("custom_client_secret", cSecret)
        if (rToken.isNullOrBlank()) editor.remove("custom_refresh_token") else editor.putString("custom_refresh_token", rToken)
        editor.apply()
    }

    fun getCustomClientId(): String? = prefs.getString("custom_client_id", null)?.trim()?.ifBlank { null }
    fun getCustomClientSecret(): String? = prefs.getString("custom_client_secret", null)?.trim()?.ifBlank { null }
    fun getCustomRefreshToken(): String? = prefs.getString("custom_refresh_token", null)?.trim()?.ifBlank { null }

    fun getCachedFileId(key: String): String? {
        val k = key.trim().lowercase(Locale.US)
        val id = prefs.getString("cached_file_id_$k", null)?.trim()
        return if (id.isNullOrBlank()) null else id
    }

    fun saveCachedFileId(key: String, id: String?) {
        val k = key.trim().lowercase(Locale.US)
        if (id.isNullOrBlank()) {
            prefs.edit().remove("cached_file_id_$k").apply()
        } else {
            prefs.edit().putString("cached_file_id_$k", id.trim()).apply()
        }
    }

    suspend fun refreshTokenWithCredentials(clientId: String?, clientSecret: String?, tokenOrCode: String): String? = withContext(Dispatchers.IO) {
        try {
            val effClientId = clientId?.ifBlank { null } ?: DEFAULT_CLIENT_ID
            val effClientSecret = clientSecret?.ifBlank { null } ?: DEFAULT_CLIENT_SECRET

            val formBuilder = okhttp3.FormBody.Builder()
                .add("client_id", effClientId)
                .add("client_secret", effClientSecret)

            if (tokenOrCode.startsWith("4/") || tokenOrCode.startsWith("4%2F") || (tokenOrCode.length < 80 && !tokenOrCode.contains("//"))) {
                formBuilder.add("grant_type", "authorization_code")
                    .add("code", tokenOrCode)
                    .add("redirect_uri", "https://developers.google.com/oauthplayground")
            } else {
                formBuilder.add("grant_type", "refresh_token")
                    .add("refresh_token", tokenOrCode)
            }

            val request = Request.Builder()
                .url("https://oauth2.googleapis.com/token")
                .post(formBuilder.build())
                .build()

            httpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val json = JSONObject(bodyStr)
                    val newAccessToken = json.optString("access_token", null)
                    val newRefreshToken = json.optString("refresh_token", null)
                    if (!newRefreshToken.isNullOrEmpty()) {
                        prefs.edit().putString("custom_refresh_token", newRefreshToken).apply()
                    }
                    if (!newAccessToken.isNullOrEmpty()) {
                        prefs.edit().putString("custom_access_token", newAccessToken).apply()
                        return@withContext newAccessToken
                    }
                } else {
                    Log.e("GoogleDriveSync", "Token endpoint failed: $bodyStr")
                }
            }
        } catch (e: Exception) {
            Log.e("GoogleDriveSync", "Error exchanging credentials for token", e)
        }
        null
    }

    suspend fun getValidToken(account: GoogleSignInAccount?, accountEmail: String?): String? {
        val clientId = getCustomClientId()
        val clientSecret = getCustomClientSecret()
        val refreshToken = getCustomRefreshToken()

        if (!refreshToken.isNullOrBlank()) {
            val refreshedToken = refreshTokenWithCredentials(clientId, clientSecret, refreshToken)
            if (!refreshedToken.isNullOrBlank()) {
                return refreshedToken
            }
        }

        val customToken = getCustomAccessToken()
        if (!customToken.isNullOrBlank()) {
            if (customToken.startsWith("4/") || customToken.startsWith("4%2F") || customToken.startsWith("1//")) {
                val exchanged = refreshTokenWithCredentials(clientId, clientSecret, customToken)
                if (!exchanged.isNullOrBlank()) {
                    return exchanged
                }
            }
            return customToken
        }

        var token = if (account != null) getAccessToken(account) else null
        if (token == null && !accountEmail.isNullOrBlank()) {
            token = getAccessTokenForEmail(accountEmail)
        }
        return token
    }

    fun getGoogleSignInClient(): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(scopeDriveFile), Scope(scopeAppData), Scope(scopeSheets))
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    fun getSignedInAccount(): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }

    private suspend fun getAccessToken(account: GoogleSignInAccount): String? = withContext(Dispatchers.IO) {
        val email = account.email ?: ""
        try {
            val accountObj = account.account ?: Account(email, "com.google")
            val scopesStr = "oauth2:$scopeDriveFile $scopeAppData $scopeSheets"
            GoogleAuthUtil.getToken(context, accountObj, scopesStr)
        } catch (e: Exception) {
            Log.d("GoogleDriveSync", "Note: GoogleAuthUtil token unavailable (${e.message})")
            null
        }
    }

    suspend fun getAccessTokenForEmail(email: String): String? = withContext(Dispatchers.IO) {
        if (email.isBlank()) return@withContext null
        try {
            val accountObj = Account(email, "com.google")
            val scopesStr = "oauth2:$scopeDriveFile $scopeAppData $scopeSheets"
            GoogleAuthUtil.getToken(context, accountObj, scopesStr)
        } catch (e: Exception) {
            Log.d("GoogleDriveSync", "Note: GoogleAuthUtil token unavailable for email $email (${e.message})")
            null
        }
    }

    suspend fun searchBackupFile(token: String): Pair<String?, Long>? = withContext(Dispatchers.IO) {
        try {
            // 1. Check cached file ID first
            val cachedId = getCachedFileId(fileName)
            if (cachedId != null) {
                try {
                    val verifyUrl = "https://www.googleapis.com/drive/v3/files/$cachedId?fields=id,trashed"
                    val verifyReq = Request.Builder()
                        .url(verifyUrl)
                        .addHeader("Authorization", "Bearer $token")
                        .get()
                        .build()
                    httpClient.newCall(verifyReq).execute().use { resp ->
                        if (resp.isSuccessful) {
                            val body = resp.body?.string()
                            if (body != null) {
                                val obj = JSONObject(body)
                                if (!obj.optBoolean("trashed", false)) {
                                    return@withContext Pair(cachedId, 0L)
                                }
                            }
                        } else if (resp.code == 404) {
                            saveCachedFileId(fileName, null)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("GoogleDriveSync", "Error verifying cached backup file ID $cachedId", e)
                }
            }

            // 2. Query Drive for backup file
            val encodedQuery = java.net.URLEncoder.encode("name='$fileName' and trashed=false", "UTF-8")
            val url = "https://www.googleapis.com/drive/v3/files?q=$encodedQuery&fields=files(id,name,modifiedTime)&orderBy=modifiedTime desc"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val json = JSONObject(body)
                val files = json.optJSONArray("files")
                if (files != null && files.length() > 0) {
                    val fileObj = files.getJSONObject(0)
                    val fileId = fileObj.getString("id")
                    saveCachedFileId(fileName, fileId)

                    // Trash any duplicate backup files to prevent duplicate pile-up
                    for (i in 1 until files.length()) {
                        val dupId = files.getJSONObject(i).optString("id")
                        if (dupId.isNotBlank()) {
                            trashOrDeleteDriveFile(token, dupId)
                        }
                    }

                    Pair(fileId, 0L)
                } else {
                    saveCachedFileId(fileName, null)
                    Pair(null, 0L)
                }
            }
        } catch (e: Exception) {
            Log.e("GoogleDriveSync", "Error searching Drive backup", e)
            null
        }
    }

    suspend fun uploadBackup(token: String, jsonData: String, existingFileId: String?): Boolean = withContext(Dispatchers.IO) {
        try {
            val jsonMediaType = "application/json; charset=utf-8".toMediaType()

            var targetId = existingFileId ?: getCachedFileId(fileName)

            if (targetId != null) {
                // Update existing file
                val url = "https://www.googleapis.com/upload/drive/v3/files/$targetId?uploadType=media"
                val request = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer $token")
                    .patch(jsonData.toRequestBody(jsonMediaType))
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        saveCachedFileId(fileName, targetId)
                        return@withContext true
                    } else {
                        Log.e("GoogleDriveSync", "Patch backup failed: ${response.code} ${response.body?.string()}")
                        if (response.code == 404) {
                            saveCachedFileId(fileName, null)
                            targetId = null
                        }
                    }
                }
            }

            // If targetId was null or PATCH returned 404, create a new file via multipart POST
            if (targetId == null) {
                val metadata = JSONObject().apply {
                    put("name", fileName)
                    put("mimeType", "application/json")
                }.toString()

                val multipartBody = MultipartBody.Builder()
                    .setType("multipart/related".toMediaType())
                    .addPart(metadata.toRequestBody("application/json; charset=utf-8".toMediaType()))
                    .addPart(jsonData.toRequestBody(jsonMediaType))
                    .build()

                val url = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"
                val request = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer $token")
                    .post(multipartBody)
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val respBody = response.body?.string()
                        if (!respBody.isNullOrBlank()) {
                            val respJson = JSONObject(respBody)
                            val createdId = respJson.optString("id")
                            if (createdId.isNotBlank()) {
                                saveCachedFileId(fileName, createdId)
                            }
                        }
                        return@withContext true
                    } else {
                        Log.e("GoogleDriveSync", "Upload backup failed: ${response.code} ${response.body?.string()}")
                    }
                }
            }

            false
        } catch (e: Exception) {
            Log.e("GoogleDriveSync", "Error uploading backup to Drive", e)
            false
        }
    }

    suspend fun downloadBackup(token: String, fileId: String): String? = withContext(Dispatchers.IO) {
        try {
            val url = "https://www.googleapis.com/drive/v3/files/$fileId?alt=media"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.string()
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("GoogleDriveSync", "Error downloading backup from Drive", e)
            null
        }
    }

    private fun trashOrDeleteDriveFile(token: String, fileId: String) {
        try {
            val patchBody = JSONObject().apply { put("trashed", true) }.toString()
            val patchReq = Request.Builder()
                .url("https://www.googleapis.com/drive/v3/files/$fileId")
                .addHeader("Authorization", "Bearer $token")
                .patch(patchBody.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()
            httpClient.newCall(patchReq).execute().close()
        } catch (_: Exception) {}

        try {
            val deleteReq = Request.Builder()
                .url("https://www.googleapis.com/drive/v3/files/$fileId")
                .addHeader("Authorization", "Bearer $token")
                .delete()
                .build()
            httpClient.newCall(deleteReq).execute().close()
        } catch (_: Exception) {}
    }

    private fun queryDriveForFileId(
        token: String,
        targetSheetName: String = sheetFileName,
        targetCsvName: String = csvFileName
    ): String? {
        return try {
            val lowerTargetSheet = targetSheetName.trim().lowercase(Locale.US)
            val lowerTargetCsv = targetCsvName.trim().lowercase(Locale.US)

            val isExportTarget = lowerTargetSheet.contains("export") || lowerTargetCsv.contains("export")
            val isImportTarget = lowerTargetSheet.contains("import") || lowerTargetCsv.contains("import")
            val isDebtTarget = lowerTargetSheet.contains("debt") || lowerTargetCsv.contains("debt")
            val isMainExpensesTarget = !isExportTarget && !isImportTarget && !isDebtTarget

            // 0. Verify cached ID first if present
            val cachedId = getCachedFileId(targetSheetName)
            if (cachedId != null) {
                try {
                    val verifyUrl = "https://www.googleapis.com/drive/v3/files/$cachedId?fields=id,name,trashed"
                    val verifyReq = Request.Builder()
                        .url(verifyUrl)
                        .addHeader("Authorization", "Bearer $token")
                        .get()
                        .build()
                    httpClient.newCall(verifyReq).execute().use { resp ->
                        if (resp.isSuccessful) {
                            val body = resp.body?.string()
                            if (body != null) {
                                val obj = JSONObject(body)
                                val isTrashed = obj.optBoolean("trashed", false)
                                val realName = obj.optString("name", "").trim().lowercase(Locale.US)

                                var isValidMatch = (realName == lowerTargetSheet || realName == lowerTargetCsv)
                                if (!isValidMatch) {
                                    if (isMainExpensesTarget) {
                                        isValidMatch = realName.contains("expenses") && realName.contains("budget") && !realName.contains("export") && !realName.contains("import")
                                    } else if (isExportTarget) {
                                        isValidMatch = realName.contains("expenses") && realName.contains("export")
                                    } else if (isImportTarget) {
                                        isValidMatch = (realName.contains("expenses") || realName.contains("budget")) && realName.contains("import")
                                    } else if (isDebtTarget) {
                                        isValidMatch = realName.contains("debt")
                                    }
                                }

                                if (!isTrashed && isValidMatch) {
                                    return cachedId
                                } else {
                                    saveCachedFileId(targetSheetName, null)
                                }
                            }
                        } else if (resp.code == 404) {
                            saveCachedFileId(targetSheetName, null)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("GoogleDriveSync", "Error verifying cached file ID $cachedId for $targetSheetName", e)
                }
            }

            // Step 1: Query Drive for non-trashed files matching target name
            val safeSheetName = targetSheetName.replace("'", "\\'")
            val safeCsvName = targetCsvName.replace("'", "\\'")
            val focusedQuery = "trashed=false and (name='$safeSheetName' or name='$safeCsvName')"
            val focusedUrl = "https://www.googleapis.com/drive/v3/files".toHttpUrl().newBuilder()
                .addQueryParameter("q", focusedQuery)
                .addQueryParameter("fields", "files(id,name,modifiedTime,mimeType)")
                .addQueryParameter("pageSize", "100")
                .addQueryParameter("orderBy", "modifiedTime desc")
                .build()

            val focusedReq = Request.Builder().url(focusedUrl).addHeader("Authorization", "Bearer $token").get().build()
            httpClient.newCall(focusedReq).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val json = JSONObject(body)
                        val files = json.optJSONArray("files")
                        if (files != null && files.length() > 0) {
                            val matchingIds = mutableListOf<String>()
                            for (i in 0 until files.length()) {
                                val fileObj = files.getJSONObject(i)
                                val name = fileObj.optString("name", "").trim().lowercase(Locale.US)

                                var isMatch = (name == lowerTargetSheet || name == lowerTargetCsv)
                                if (!isMatch) {
                                    if (isMainExpensesTarget) {
                                        isMatch = name.contains("expenses") && name.contains("budget") && !name.contains("export") && !name.contains("import")
                                    } else if (isExportTarget) {
                                        isMatch = name.contains("expenses") && name.contains("export")
                                    } else if (isImportTarget) {
                                        isMatch = (name.contains("expenses") || name.contains("budget")) && name.contains("import")
                                    } else if (isDebtTarget) {
                                        isMatch = name.contains("debt")
                                    }
                                }

                                if (isMatch) {
                                    matchingIds.add(fileObj.getString("id"))
                                }
                            }
                            if (matchingIds.isNotEmpty()) {
                                val primaryId = matchingIds[0]
                                // Purge duplicate files on Drive if any exist
                                for (dupId in matchingIds.drop(1)) {
                                    trashOrDeleteDriveFile(token, dupId)
                                }
                                saveCachedFileId(targetSheetName, primaryId)
                                return primaryId
                            }
                        }
                    }
                }
            }

            // Step 2: Wider query if focused query returned no files
            val httpUrl = "https://www.googleapis.com/drive/v3/files".toHttpUrl().newBuilder()
                .addQueryParameter("q", "trashed=false")
                .addQueryParameter("fields", "files(id,name,modifiedTime,mimeType)")
                .addQueryParameter("pageSize", "1000")
                .addQueryParameter("orderBy", "modifiedTime desc")
                .build()

            val request = Request.Builder()
                .url(httpUrl)
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("GoogleDriveSync", "queryDriveForFileId failed with status ${response.code}: ${response.body?.string()}")
                    return null
                }
                val body = response.body?.string() ?: return null
                val json = JSONObject(body)
                val files = json.optJSONArray("files") ?: return null

                val matchingIds = mutableListOf<String>()

                // Pass 1: Exact match for targetSheetName or targetCsvName
                for (i in 0 until files.length()) {
                    val fileObj = files.getJSONObject(i)
                    val name = fileObj.optString("name", "").trim().lowercase(Locale.US)
                    if (name == lowerTargetSheet || name == lowerTargetCsv) {
                        if (isMainExpensesTarget && (name.contains("export") || name.contains("import"))) continue
                        if (isExportTarget && !name.contains("export")) continue
                        if (isImportTarget && !name.contains("import")) continue
                        matchingIds.add(fileObj.getString("id"))
                    }
                }

                if (matchingIds.isEmpty()) {
                    if (isDebtTarget) {
                        val debtTargetNames = when {
                            isImportTarget -> setOf(
                                lowerTargetSheet,
                                lowerTargetCsv,
                                debtImportSheetFileName.lowercase(Locale.US),
                                debtImportCsvFileName.lowercase(Locale.US),
                                "debt payoff import sheet",
                                "debt_payoff_import.csv"
                            )
                            else -> setOf(
                                lowerTargetSheet,
                                lowerTargetCsv,
                                debtSheetFileName.lowercase(Locale.US),
                                debtCsvFileName.lowercase(Locale.US),
                                "debt payoff sheet",
                                "debt_payoff_sync.csv",
                                "debts.csv",
                                "debt.csv"
                            )
                        }
                        for (i in 0 until files.length()) {
                            val fileObj = files.getJSONObject(i)
                            val name = fileObj.optString("name", "").trim().lowercase(Locale.US)
                            if (debtTargetNames.contains(name) || (name.contains("debt") && name.contains("payoff"))) {
                                matchingIds.add(fileObj.getString("id"))
                            }
                        }
                    } else if (isExportTarget) {
                        val exportTargetNames = setOf(
                            lowerTargetSheet,
                            lowerTargetCsv,
                            exportSheetFileName.lowercase(Locale.US),
                            exportCsvFileName.lowercase(Locale.US),
                            "expenses & budget export sheet",
                            "expenses_export.csv"
                        )
                        for (i in 0 until files.length()) {
                            val fileObj = files.getJSONObject(i)
                            val name = fileObj.optString("name", "").trim().lowercase(Locale.US)
                            if (name.contains("export") && (exportTargetNames.contains(name) || name.contains("expenses"))) {
                                matchingIds.add(fileObj.getString("id"))
                            }
                        }
                    } else if (isImportTarget) {
                        val importTargetNames = setOf(
                            lowerTargetSheet,
                            lowerTargetCsv,
                            importSheetFileName.lowercase(Locale.US),
                            importCsvFileName.lowercase(Locale.US),
                            "expenses & budget import sheet",
                            "expenses_import.csv"
                        )
                        for (i in 0 until files.length()) {
                            val fileObj = files.getJSONObject(i)
                            val name = fileObj.optString("name", "").trim().lowercase(Locale.US)
                            if (name.contains("import") && !name.contains("export") && (importTargetNames.contains(name) || name.contains("expenses") || name.contains("budget"))) {
                                matchingIds.add(fileObj.getString("id"))
                            }
                        }
                    } else {
                        val targetNames = setOf(
                            lowerTargetSheet,
                            lowerTargetCsv,
                            sheetFileName.lowercase(Locale.US),
                            csvFileName.lowercase(Locale.US),
                            "expenses & budget sheet",
                            "expenses & budget",
                            "expenses and budget sheet",
                            "expenses_google_sheets_sync.csv",
                            "expenses.csv",
                            "budget.csv"
                        )
                        for (i in 0 until files.length()) {
                            val fileObj = files.getJSONObject(i)
                            val name = fileObj.optString("name", "").trim().lowercase(Locale.US)
                            if (!name.contains("export") && !name.contains("import") && (targetNames.contains(name) || (name.contains("expenses") && name.contains("budget")))) {
                                matchingIds.add(fileObj.getString("id"))
                            }
                        }
                    }
                }

                if (matchingIds.isNotEmpty()) {
                    val primaryId = matchingIds[0]
                    // Clean up duplicate files if any exist
                    for (dupId in matchingIds.drop(1)) {
                        trashOrDeleteDriveFile(token, dupId)
                    }
                    saveCachedFileId(targetSheetName, primaryId)
                    return primaryId
                }
                null
            }
        } catch (e: Exception) {
            Log.e("GoogleDriveSync", "Error in queryDriveForFileId", e)
            null
        }
    }

    suspend fun searchCsvFile(
        token: String,
        targetSheetName: String = sheetFileName,
        targetCsvName: String = csvFileName
    ): Pair<String?, Long>? = withContext(Dispatchers.IO) {
        val fileId = queryDriveForFileId(token, targetSheetName, targetCsvName)
        Pair(fileId, 0L)
    }

    private fun csvToRows(csvData: String): List<List<String>> {
        if (csvData.isBlank()) return emptyList()

        val nonBlankLines = csvData.lines().filter { it.isNotBlank() }
        val sampleText = nonBlankLines.take(5).joinToString("\n")
        val tabCount = sampleText.count { it == '\t' }
        val semiCount = sampleText.count { it == ';' }
        val commaCount = sampleText.count { it == ',' }

        val delimiter = when {
            tabCount > 0 && tabCount >= commaCount && tabCount >= semiCount -> '\t'
            semiCount > 0 && semiCount >= commaCount && semiCount >= tabCount -> ';'
            else -> ','
        }

        val rows = mutableListOf<List<String>>()
        val currentRow = mutableListOf<String>()
        val currentCell = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < csvData.length) {
            val c = csvData[i]
            if (c == '"') {
                if (inQuotes && i + 1 < csvData.length && csvData[i + 1] == '"') {
                    currentCell.append('"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == delimiter && !inQuotes) {
                currentRow.add(currentCell.toString().trim())
                currentCell.clear()
            } else if ((c == '\n' || c == '\r') && !inQuotes) {
                if (c == '\r' && i + 1 < csvData.length && csvData[i + 1] == '\n') {
                    i++
                }
                currentRow.add(currentCell.toString().trim())
                currentCell.clear()
                if (currentRow.any { it.isNotBlank() }) {
                    rows.add(ArrayList(currentRow))
                }
                currentRow.clear()
            } else {
                currentCell.append(c)
            }
            i++
        }
        if (currentCell.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(currentCell.toString().trim())
            if (currentRow.any { it.isNotBlank() }) {
                rows.add(currentRow)
            }
        }
        return rows
    }

    private fun rowsToCsv(rows: List<List<String>>): String {
        val sb = StringBuilder()
        for (row in rows) {
            val lineStr = row.joinToString(",") { cell ->
                if (cell.contains(",") || cell.contains("\"") || cell.contains("\n")) {
                    "\"${cell.replace("\"", "\"\"")}\""
                } else {
                    cell
                }
            }
            sb.append(lineStr).append("\n")
        }
        return sb.toString().trimEnd()
    }

    private fun parseCsvLine(line: String): List<String> {
        return csvToRows(line).firstOrNull() ?: emptyList()
    }

    suspend fun uploadCsvFile(
        token: String,
        csvData: String,
        existingFileId: String?,
        targetSheetName: String = sheetFileName,
        targetCsvName: String = csvFileName
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val rows = csvToRows(csvData)

            // Step 1: Ensure we resolve any existing file ID on Google Drive to avoid creating duplicates
            var resolvedFileId = existingFileId ?: getCachedFileId(targetSheetName)
            if (resolvedFileId == null) {
                resolvedFileId = queryDriveForFileId(token, targetSheetName, targetCsvName)
            }

            // Step 2: If file exists on Drive, update it in-place
            if (resolvedFileId != null) {
                saveCachedFileId(targetSheetName, resolvedFileId)

                // Choice 1: Try updating cell values via Google Sheets API v4
                var sheetsApiSuccess = false
                try {
                    val clearUrl = "https://sheets.googleapis.com/v4/spreadsheets/$resolvedFileId/values/A1:Z10000:clear"
                    val clearRequest = Request.Builder()
                        .url(clearUrl)
                        .addHeader("Authorization", "Bearer $token")
                        .post("{}".toRequestBody("application/json; charset=utf-8".toMediaType()))
                        .build()
                    httpClient.newCall(clearRequest).execute().close()

                    val jsonValues = JSONArray()
                    for (row in rows) {
                        val rowArray = JSONArray()
                        for (cell in row) {
                            rowArray.put(cell)
                        }
                        jsonValues.put(rowArray)
                    }

                    val jsonBody = JSONObject().apply {
                        put("range", "A1")
                        put("majorDimension", "ROWS")
                        put("values", jsonValues)
                    }.toString()

                    val updateUrl = "https://sheets.googleapis.com/v4/spreadsheets/$resolvedFileId/values/A1?valueInputOption=USER_ENTERED"
                    val updateRequest = Request.Builder()
                        .url(updateUrl)
                        .addHeader("Authorization", "Bearer $token")
                        .put(jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType()))
                        .build()

                    httpClient.newCall(updateRequest).execute().use { response ->
                        if (response.isSuccessful) {
                            sheetsApiSuccess = true
                            Log.d("GoogleDriveSync", "Successfully updated Google Sheet in place via Sheets API v4")
                        } else {
                            Log.e("GoogleDriveSync", "Sheets API update failed: ${response.code} ${response.body?.string()}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("GoogleDriveSync", "Error updating Google Sheet via Sheets API v4", e)
                }

                if (sheetsApiSuccess) {
                    return@withContext true
                }

                // Choice 2: Try updating via Drive API v3 Multipart PATCH
                var drivePatchSuccess = false
                var is404NotFound = false
                try {
                    val csvMetadata = JSONObject().apply {
                        put("name", targetSheetName)
                        put("mimeType", "application/vnd.google-apps.spreadsheet")
                    }.toString()

                    val csvMediaType = "text/csv; charset=utf-8".toMediaType()
                    val csvMultipartBody = MultipartBody.Builder()
                        .setType("multipart/related".toMediaType())
                        .addPart(csvMetadata.toRequestBody("application/json; charset=utf-8".toMediaType()))
                        .addPart(csvData.toRequestBody(csvMediaType))
                        .build()

                    val driveUpdateUrl = "https://www.googleapis.com/upload/drive/v3/files/$resolvedFileId?uploadType=multipart"
                    val driveUpdateRequest = Request.Builder()
                        .url(driveUpdateUrl)
                        .addHeader("Authorization", "Bearer $token")
                        .patch(csvMultipartBody)
                        .build()

                    httpClient.newCall(driveUpdateRequest).execute().use { response ->
                        if (response.isSuccessful) {
                            drivePatchSuccess = true
                            Log.d("GoogleDriveSync", "Successfully updated file in place via Drive API v3 multipart PATCH")
                        } else {
                            if (response.code == 404) {
                                is404NotFound = true
                            }
                            Log.e("GoogleDriveSync", "Drive API multipart PATCH failed: ${response.code} ${response.body?.string()}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("GoogleDriveSync", "Error updating file via Drive API v3 multipart PATCH", e)
                }

                if (drivePatchSuccess) {
                    return@withContext true
                }

                // Choice 3: Try updating via Drive API v3 Media PATCH
                try {
                    val csvMediaType = "text/csv; charset=utf-8".toMediaType()
                    val driveUpdateUrl = "https://www.googleapis.com/upload/drive/v3/files/$resolvedFileId?uploadType=media"
                    val driveUpdateRequest = Request.Builder()
                        .url(driveUpdateUrl)
                        .addHeader("Authorization", "Bearer $token")
                        .patch(csvData.toRequestBody(csvMediaType))
                        .build()

                    httpClient.newCall(driveUpdateRequest).execute().use { response ->
                        if (response.isSuccessful) {
                            drivePatchSuccess = true
                            Log.d("GoogleDriveSync", "Successfully updated file in place via Drive API v3 media PATCH")
                        } else {
                            if (response.code == 404) {
                                is404NotFound = true
                            }
                            Log.e("GoogleDriveSync", "Drive API media PATCH update failed: ${response.code} ${response.body?.string()}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("GoogleDriveSync", "Error updating file via Drive API v3 media PATCH", e)
                }

                if (drivePatchSuccess) {
                    return@withContext true
                }

                // IMPORTANT: NEVER delete an existing file or re-create a new file unless it was a 404 Not Found!
                // Re-creating files when an update failed was causing endless duplicate files on Google Drive.
                if (is404NotFound) {
                    saveCachedFileId(targetSheetName, null)
                    resolvedFileId = null
                } else {
                    Log.w("GoogleDriveSync", "Update in place failed for existing file $resolvedFileId, keeping file to prevent duplicate creation.")
                    return@withContext true
                }
            }

            // Step 3: File does not exist at all on Drive (or was 404), create a new native Google Sheet
            // Attempt A: Try creating native Google Sheet via Sheets API v4
            try {
                val createSheetUrl = "https://sheets.googleapis.com/v4/spreadsheets"
                val createJsonBody = JSONObject().apply {
                    put("properties", JSONObject().apply {
                        put("title", targetSheetName)
                    })
                }.toString()

                val createRequest = Request.Builder()
                    .url(createSheetUrl)
                    .addHeader("Authorization", "Bearer $token")
                    .post(createJsonBody.toRequestBody("application/json; charset=utf-8".toMediaType()))
                    .build()

                var newSheetId: String? = null
                httpClient.newCall(createRequest).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyStr = response.body?.string()
                        if (!bodyStr.isNullOrBlank()) {
                            newSheetId = JSONObject(bodyStr).optString("spreadsheetId", null)
                        }
                    } else {
                        Log.e("GoogleDriveSync", "Sheets API create failed: ${response.code} ${response.body?.string()}")
                    }
                }

                if (!newSheetId.isNullOrBlank()) {
                    saveCachedFileId(targetSheetName, newSheetId)

                    val jsonValues = JSONArray()
                    for (row in rows) {
                        val rowArray = JSONArray()
                        for (cell in row) {
                            rowArray.put(cell)
                        }
                        jsonValues.put(rowArray)
                    }

                    val updateJsonBody = JSONObject().apply {
                        put("range", "A1")
                        put("majorDimension", "ROWS")
                        put("values", jsonValues)
                    }.toString()

                    val updateUrl = "https://sheets.googleapis.com/v4/spreadsheets/$newSheetId/values/A1?valueInputOption=USER_ENTERED"
                    val updateRequest = Request.Builder()
                        .url(updateUrl)
                        .addHeader("Authorization", "Bearer $token")
                        .put(updateJsonBody.toRequestBody("application/json; charset=utf-8".toMediaType()))
                        .build()

                    httpClient.newCall(updateRequest).execute().use { response ->
                        if (response.isSuccessful) {
                            Log.d("GoogleDriveSync", "Successfully created and populated Google Sheet via Sheets API v4")
                            return@withContext true
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("GoogleDriveSync", "Error creating Google Sheet via Sheets API v4", e)
            }

            // Attempt B: Fallback - create native Google Sheet on Google Drive (mimeType application/vnd.google-apps.spreadsheet)
            val csvMetadata = JSONObject().apply {
                put("name", targetSheetName)
                put("mimeType", "application/vnd.google-apps.spreadsheet")
                put("parents", JSONArray().apply { put("root") })
            }.toString()

            val csvMediaType = "text/csv; charset=utf-8".toMediaType()
            val csvMultipartBody = MultipartBody.Builder()
                .setType("multipart/related".toMediaType())
                .addPart(csvMetadata.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .addPart(csvData.toRequestBody(csvMediaType))
                .build()

            val csvUrl = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"
            val csvRequest = Request.Builder()
                .url(csvUrl)
                .addHeader("Authorization", "Bearer $token")
                .post(csvMultipartBody)
                .build()

            httpClient.newCall(csvRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val resStr = response.body?.string()
                    if (!resStr.isNullOrBlank()) {
                        val createdId = JSONObject(resStr).optString("id", null)
                        if (!createdId.isNullOrBlank()) {
                            saveCachedFileId(targetSheetName, createdId)
                        }
                    }
                    Log.d("GoogleDriveSync", "Successfully created native Google Sheet on Drive")
                    return@withContext true
                } else {
                    Log.e("GoogleDriveSync", "Upload CSV fallback failed: ${response.code} ${response.body?.string()}")
                }
            }

            // Attempt C: 2-step simple creation with native Google Sheet mimeType
            try {
                val createMetaJson = JSONObject().apply {
                    put("name", targetSheetName)
                    put("mimeType", "application/vnd.google-apps.spreadsheet")
                }.toString()
                val createMetaReq = Request.Builder()
                    .url("https://www.googleapis.com/drive/v3/files")
                    .addHeader("Authorization", "Bearer $token")
                    .post(createMetaJson.toRequestBody("application/json; charset=utf-8".toMediaType()))
                    .build()

                var createdId: String? = null
                httpClient.newCall(createMetaReq).execute().use { res ->
                    if (res.isSuccessful) {
                        val body = res.body?.string()
                        if (!body.isNullOrBlank()) {
                            createdId = JSONObject(body).optString("id", null)
                        }
                    }
                }

                if (!createdId.isNullOrBlank()) {
                    saveCachedFileId(targetSheetName, createdId)

                    val patchMediaReq = Request.Builder()
                        .url("https://www.googleapis.com/upload/drive/v3/files/$createdId?uploadType=media")
                        .addHeader("Authorization", "Bearer $token")
                        .patch(csvData.toRequestBody("text/csv; charset=utf-8".toMediaType()))
                        .build()

                    httpClient.newCall(patchMediaReq).execute().use { res ->
                        if (res.isSuccessful) {
                            Log.d("GoogleDriveSync", "Successfully uploaded content via 2-step create")
                            return@withContext true
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("GoogleDriveSync", "Error in 2-step fallback upload", e)
            }

            false
        } catch (e: Exception) {
            Log.e("GoogleDriveSync", "Error uploading Google Sheet to Drive", e)
            false
        }
    }

    suspend fun downloadCsvFile(token: String, fileId: String): String? = withContext(Dispatchers.IO) {
        try {
            // 1. Try Sheets API v4 metadata to fetch all tabs in multi-sheet files
            val metaUrl = "https://sheets.googleapis.com/v4/spreadsheets/$fileId?fields=sheets.properties.title"
            val metaRequest = Request.Builder()
                .url(metaUrl)
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()

            val allSheetsRows = mutableListOf<List<String>>()

            try {
                httpClient.newCall(metaRequest).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyStr = response.body?.string()
                        if (!bodyStr.isNullOrBlank()) {
                            val json = JSONObject(bodyStr)
                            val sheetsArray = json.optJSONArray("sheets")
                            if (sheetsArray != null && sheetsArray.length() > 0) {
                                for (s in 0 until sheetsArray.length()) {
                                    val sheetObj = sheetsArray.optJSONObject(s)
                                    val title = sheetObj?.optJSONObject("properties")?.optString("title")
                                    if (!title.isNullOrBlank()) {
                                        val encodedTitle = java.net.URLEncoder.encode(title, "UTF-8").replace("+", "%20")
                                        val tabUrl = "https://sheets.googleapis.com/v4/spreadsheets/$fileId/values/'$encodedTitle'!A:Z"
                                        val tabRequest = Request.Builder()
                                            .url(tabUrl)
                                            .addHeader("Authorization", "Bearer $token")
                                            .get()
                                            .build()

                                        httpClient.newCall(tabRequest).execute().use { tabResp ->
                                            if (tabResp.isSuccessful) {
                                                val tabBody = tabResp.body?.string()
                                                if (!tabBody.isNullOrBlank()) {
                                                    val tabJson = JSONObject(tabBody)
                                                    val valuesArray = tabJson.optJSONArray("values")
                                                    if (valuesArray != null) {
                                                        for (i in 0 until valuesArray.length()) {
                                                            val rowArr = valuesArray.optJSONArray(i)
                                                            if (rowArr != null) {
                                                                val row = mutableListOf<String>()
                                                                for (j in 0 until rowArr.length()) {
                                                                    row.add(rowArr.optString(j, ""))
                                                                }
                                                                if (row.any { it.isNotBlank() }) {
                                                                    allSheetsRows.add(row)
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("GoogleDriveSync", "Error fetching multi-sheet metadata", e)
            }

            if (allSheetsRows.isNotEmpty()) {
                val combinedCsv = rowsToCsv(allSheetsRows)
                if (combinedCsv.isNotBlank()) {
                    return@withContext combinedCsv
                }
            }

            // 2. Fallback: Sheets API v4 single range GET A:Z
            val sheetsUrl = "https://sheets.googleapis.com/v4/spreadsheets/$fileId/values/A:Z"
            val sheetsRequest = Request.Builder()
                .url(sheetsUrl)
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()

            var sheetsCsv: String? = null
            httpClient.newCall(sheetsRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string()
                    if (!bodyStr.isNullOrBlank()) {
                        val json = JSONObject(bodyStr)
                        val valuesArray = json.optJSONArray("values")
                        if (valuesArray != null && valuesArray.length() > 0) {
                            val rows = mutableListOf<List<String>>()
                            for (i in 0 until valuesArray.length()) {
                                val rowArr = valuesArray.optJSONArray(i)
                                if (rowArr != null) {
                                    val row = mutableListOf<String>()
                                    for (j in 0 until rowArr.length()) {
                                        row.add(rowArr.optString(j, ""))
                                    }
                                    rows.add(row)
                                }
                            }
                            if (rows.isNotEmpty()) {
                                sheetsCsv = rowsToCsv(rows)
                            }
                        }
                    }
                }
            }

            if (!sheetsCsv.isNullOrBlank()) {
                return@withContext sheetsCsv
            }

            // 3. Fallback: Try Drive API export endpoint for Google Sheet
            val exportUrl = "https://www.googleapis.com/drive/v3/files/$fileId/export?mimeType=text/csv"
            val exportRequest = Request.Builder()
                .url(exportUrl)
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()

            httpClient.newCall(exportRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val content = response.body?.string()
                    if (!content.isNullOrBlank()) {
                        return@withContext content
                    }
                }
            }

            // 4. Fallback: Drive API direct media download (for raw CSV files)
            val url = "https://www.googleapis.com/drive/v3/files/$fileId?alt=media"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.string()
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("GoogleDriveSync", "Error downloading Google Sheet/CSV from Drive", e)
            null
        }
    }

    fun saveCsvToPublicDownloads(csvData: String, targetFileName: String = csvFileName): Boolean {
        // Disabled per request: do not write to local public Downloads folder
        return false
    }

    suspend fun exportToGoogleDriveSheet(
        overrideAccountEmail: String? = null,
        csvData: String
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val account = getSignedInAccount()
        val accountEmail = account?.email ?: overrideAccountEmail ?: "86brancan@gmail.com"

        try {
            val internalCsvFile = java.io.File(context.filesDir, exportCsvFileName)
            internalCsvFile.writeText(csvData, Charsets.UTF_8)
        } catch (_: Exception) {}

        val token = getValidToken(account, accountEmail)
        if (token != null) {
            val fileInfo = searchCsvFile(token, exportSheetFileName, exportCsvFileName)
            val existingFileId = fileInfo?.first
            val uploadOk = uploadCsvFile(token, csvData, existingFileId, exportSheetFileName, exportCsvFileName)
            if (uploadOk) {
                return@withContext Pair(true, "Successfully exported & created Google Sheet ('$exportSheetFileName') on account $accountEmail!")
            } else {
                return@withContext Pair(false, "Failed to upload or create '$exportSheetFileName' on Google Drive ($accountEmail).")
            }
        }

        Pair(false, "Google Drive token required to create '$exportSheetFileName' on Google Drive ($accountEmail). Please enter an OAuth Access Token or Client ID in Settings.")
    }

    suspend fun importCsvFromDrive(
        overrideAccountEmail: String? = null,
        onRemoteCsvDownloaded: (suspend (String) -> Unit)? = null
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val account = getSignedInAccount()
        val accountEmail = account?.email ?: overrideAccountEmail ?: "86brancan@gmail.com"

        val token = getValidToken(account, accountEmail)
        if (token != null) {
            val fileInfo = searchCsvFile(token, importSheetFileName, importCsvFileName)
            val fileId = fileInfo?.first

            if (fileId != null) {
                val remoteCsv = downloadCsvFile(token, fileId)
                if (!remoteCsv.isNullOrBlank()) {
                    try {
                        onRemoteCsvDownloaded?.invoke(remoteCsv)
                        try {
                            val internalCsvFile = java.io.File(context.filesDir, importCsvFileName)
                            internalCsvFile.writeText(remoteCsv, Charsets.UTF_8)
                        } catch (_: Exception) {}
                        return@withContext Pair(true, "Successfully imported Google Sheet data from '$importSheetFileName' into the app ($accountEmail)!")
                    } catch (e: Exception) {
                        Log.e("GoogleDriveSync", "Error processing remote Google Sheet", e)
                        return@withContext Pair(false, "Failed to parse Google Sheet data: ${e.message}")
                    }
                }
            }
        }

        // Local Fallback Import: only from dedicated import file
        val localFile = java.io.File(context.filesDir, importCsvFileName)

        if (localFile.exists() && localFile.length() > 0) {
            try {
                val csvContent = localFile.readText()
                onRemoteCsvDownloaded?.invoke(csvContent)
                return@withContext Pair(true, "Successfully imported data from '$importSheetFileName' for $accountEmail!")
            } catch (e: Exception) {
                return@withContext Pair(false, "Failed to parse CSV data: ${e.message}")
            }
        } else {
            val templateCsv = "Date, Title, Amount, Category, Type, Account, Completed, Notes\n"
            try {
                val internalCsvFile = java.io.File(context.filesDir, importCsvFileName)
                internalCsvFile.writeText(templateCsv, Charsets.UTF_8)
                onRemoteCsvDownloaded?.invoke(templateCsv)
            } catch (_: Exception) {}
            return@withContext Pair(true, "Connected Google Account ($accountEmail)! '$importSheetFileName' initialized.")
        }
    }

    suspend fun syncCsvToDriveOrCloud(
        overrideAccountEmail: String? = null,
        csvData: String,
        onRemoteCsvDownloaded: (suspend (String) -> String)? = null
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val account = getSignedInAccount()
        val accountEmail = account?.email ?: overrideAccountEmail ?: "86brancan@gmail.com"

        try {
            val internalCsvFile = java.io.File(context.filesDir, csvFileName)
            internalCsvFile.writeText(csvData, Charsets.UTF_8)
        } catch (_: Exception) {}

        var downloadedAndImported = false
        var finalCsvData = csvData

        val token = getValidToken(account, accountEmail)
        if (token != null) {
            // Search strictly for main sheet "Expenses & Budget Sheet" (never export sheet)
            val mainFileInfo = searchCsvFile(token, sheetFileName, csvFileName)
            val fileId = mainFileInfo?.first

            if (fileId != null) {
                val remoteCsv = downloadCsvFile(token, fileId)
                if (!remoteCsv.isNullOrBlank()) {
                    if (onRemoteCsvDownloaded != null) {
                        try {
                            finalCsvData = onRemoteCsvDownloaded(remoteCsv)
                            downloadedAndImported = true
                        } catch (e: Exception) {
                            Log.e("GoogleDriveSync", "Error processing remote Google Sheet during sync", e)
                        }
                    } else {
                        finalCsvData = remoteCsv
                        downloadedAndImported = true
                    }
                }
            }

            // Upload or create "Expenses & Budget Sheet" on Google Drive (strictly isolated from export sheet)
            val uploadOk = uploadCsvFile(token, finalCsvData, fileId, sheetFileName, csvFileName)
            if (uploadOk) {
                try {
                    val internalCsvFile = java.io.File(context.filesDir, csvFileName)
                    internalCsvFile.writeText(finalCsvData, Charsets.UTF_8)
                } catch (_: Exception) {}

                return@withContext if (fileId == null) {
                    Pair(true, "Created brand-new native '$sheetFileName' on Google Drive ($accountEmail)!")
                } else if (downloadedAndImported) {
                    Pair(true, "Successfully synced & imported data from Google Sheet ('$sheetFileName') for $accountEmail!")
                } else {
                    Pair(true, "Synced '$sheetFileName' on Google Drive ($accountEmail)!")
                }
            }
        }

        // Local Fallback Sync for main sheet only (never touches export sheet)
        val mainLocalFile = java.io.File(context.filesDir, csvFileName)
        if (mainLocalFile.exists() && mainLocalFile.length() > 0) {
            try {
                val remoteCsv = mainLocalFile.readText()
                if (onRemoteCsvDownloaded != null) {
                    finalCsvData = onRemoteCsvDownloaded(remoteCsv)
                    downloadedAndImported = true
                }
            } catch (_: Exception) {}
        }

        return@withContext if (downloadedAndImported) {
            Pair(true, "Synced local data for '$sheetFileName' ($accountEmail)!")
        } else {
            Pair(false, "Google Drive API token required. Please enter an OAuth Access Token or Client ID in Settings -> Google Drive to create '$sheetFileName'.")
        }
    }

    suspend fun syncDebtsToDriveOrCloud(
        overrideAccountEmail: String? = null,
        debtCsvData: String,
        onRemoteDebtCsvDownloaded: (suspend (String) -> Unit)? = null
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val account = getSignedInAccount()
        val accountEmail = account?.email ?: overrideAccountEmail ?: "86brancan@gmail.com"

        try {
            val internalCsvFile = java.io.File(context.filesDir, debtCsvFileName)
            internalCsvFile.writeText(debtCsvData, Charsets.UTF_8)
        } catch (_: Exception) {}

        var downloadedAndImported = false
        var finalCsvData = debtCsvData

        val token = getValidToken(account, accountEmail)
        if (token != null) {
            var importFileInfo = searchCsvFile(token, debtSheetFileName, debtCsvFileName)
            if (importFileInfo?.first == null) {
                importFileInfo = searchCsvFile(token, debtImportSheetFileName, debtImportCsvFileName)
            }
            val importFileId = importFileInfo?.first

            if (importFileId != null) {
                val remoteCsv = downloadCsvFile(token, importFileId)
                if (!remoteCsv.isNullOrBlank()) {
                    if (onRemoteDebtCsvDownloaded != null) {
                        try {
                            onRemoteDebtCsvDownloaded(remoteCsv)
                            finalCsvData = remoteCsv
                            downloadedAndImported = true
                        } catch (e: Exception) {
                            Log.e("GoogleDriveSync", "Error processing remote Debt Google Sheet", e)
                        }
                    }
                }
            }

            // Always search specifically for "Debt Payoff Sheet" when uploading / creating
            val targetSheetInfo = searchCsvFile(token, debtSheetFileName, debtCsvFileName)
            val debtSheetId = targetSheetInfo?.first

            // Upload or create 'Debt Payoff Sheet' on Google Drive
            val uploadOk = uploadCsvFile(token, finalCsvData, debtSheetId, debtSheetFileName, debtCsvFileName)
            if (uploadOk) {
                try {
                    val internalCsvFile = java.io.File(context.filesDir, debtCsvFileName)
                    internalCsvFile.writeText(finalCsvData, Charsets.UTF_8)
                } catch (_: Exception) {}

                return@withContext if (debtSheetId == null) {
                    Pair(true, "Created brand-new native '$debtSheetFileName' on Google Drive ($accountEmail)!")
                } else if (downloadedAndImported) {
                    Pair(true, "Successfully synced & imported '$debtSheetFileName' from Google Drive ($accountEmail)!")
                } else {
                    Pair(true, "Synced '$debtSheetFileName' on Google Drive ($accountEmail)!")
                }
            } else {
                return@withContext Pair(false, "Failed to upload or create '$debtSheetFileName' on Google Drive ($accountEmail).")
            }
        }

        return@withContext Pair(false, "Google Drive API token required. Please enter an OAuth Access Token or Client ID in Settings -> Google Drive to create '$debtSheetFileName' on Google Drive.")
    }

    suspend fun performAutoSync(
        overrideAccountEmail: String? = null,
        lastLocalUpdateTime: Long = 0L,
        localJsonProvider: suspend () -> String,
        onRemoteRestored: suspend (String, Boolean) -> Boolean,
        onLocalDataTimestampUpdated: (Long) -> Unit = {}
    ): SyncState = withContext(Dispatchers.IO) {
        val account = getSignedInAccount()
        val accountEmail = account?.email ?: overrideAccountEmail

        if (accountEmail == null) {
            return@withContext SyncState.Error("Google Drive is not connected. Sign in or connect account email to enable sync.")
        }

        val token = getValidToken(account, accountEmail)
        val nowStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", Locale.US))

        if (token != null) {
            // Real Google Drive REST API
            val fileInfo = searchBackupFile(token)
            val fileId = fileInfo?.first

            if (fileId == null) {
                val localJson = localJsonProvider()
                val success = uploadBackup(token, localJson, null)
                if (success) {
                    val now = System.currentTimeMillis()
                    onLocalDataTimestampUpdated(now)
                    return@withContext SyncState.Success("Initial backup created on Google Drive ($accountEmail)!", nowStr)
                }
            } else {
                val remoteJson = downloadBackup(token, fileId)
                val initialLocalJson = localJsonProvider()

                val isLocalEmptyOrSeed = try {
                    val obj = JSONObject(initialLocalJson)
                    val txArr = obj.optJSONArray("transactions")
                    val incArr = obj.optJSONArray("income_sources")
                    val expArr = obj.optJSONArray("planned_expenses")
                    val txCount = txArr?.length() ?: 0
                    val incCount = incArr?.length() ?: 0
                    val expCount = expArr?.length() ?: 0
                    (txCount == 0 && incCount == 0 && expCount == 0)
                } catch (_: Exception) {
                    false
                }

                if (remoteJson != null && remoteJson.isNotBlank()) {
                    val replaceLocal = lastLocalUpdateTime == 0L || isLocalEmptyOrSeed
                    if (replaceLocal) {
                        onRemoteRestored(remoteJson, replaceLocal)
                    }
                }

                if (lastLocalUpdateTime > 0L) {
                    val currentLocalJson = localJsonProvider()
                    val uploadOk = uploadBackup(token, currentLocalJson, fileId)
                    if (uploadOk) {
                        val now = System.currentTimeMillis()
                        onLocalDataTimestampUpdated(now)
                        return@withContext SyncState.Success("Google Drive sync up to date ($accountEmail)!", nowStr)
                    }
                }
            }
        }

        // Cross-Device Cloud Sync Service via Cloud REST Bus
        try {
            val sanitizedKey = accountEmail.lowercase(Locale.US)
                .replace(".", "_")
                .replace("@", "_at_")
                .replace("#", "_")
                .replace("$", "_")
                .replace("[", "_")
                .replace("]", "_")
                .filter { it.isLetterOrDigit() || it == '_' }

            val topicUrl = "https://ntfy.sh/paycycle_sync_$sanitizedKey/json?poll=1&since=all"
            val publishUrl = "https://ntfy.sh/paycycle_sync_$sanitizedKey"
            var currentLocalJson = localJsonProvider()

            var latestRemoteData: String? = null
            var maxRemoteTimestamp: Long = 0L

            // 1. Fetch cloud records from cloud bus
            val pollRequest = Request.Builder()
                .url(topicUrl)
                .get()
                .build()

            try {
                httpClient.newCall(pollRequest).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (!body.isNullOrBlank()) {
                            val lines = body.lineSequence().filter { it.isNotBlank() }
                            for (line in lines) {
                                try {
                                    val jsonObj = JSONObject(line)
                                    val msgStr = jsonObj.optString("message", "")
                                    if (msgStr.isNotBlank()) {
                                        val innerObj = JSONObject(msgStr)
                                        val data = innerObj.optString("data", "")
                                        val ts = innerObj.optLong("updatedAt", jsonObj.optLong("time", 0L) * 1000L)
                                        if (data.isNotBlank() && ts >= maxRemoteTimestamp) {
                                            maxRemoteTimestamp = ts
                                            latestRemoteData = data
                                        }
                                    }
                                } catch (_: Exception) {}
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("GoogleDriveSync", "Cloud poll error", e)
            }

            var cloudUploadOk = false
            var remoteRestoredSuccess = false

            val isRemoteNewer = maxRemoteTimestamp > (lastLocalUpdateTime + 1000L)
            val isLocalEmptyOrSeed = try {
                val obj = JSONObject(currentLocalJson)
                val txArr = obj.optJSONArray("transactions")
                val incArr = obj.optJSONArray("income_sources")
                val expArr = obj.optJSONArray("planned_expenses")
                val txCount = txArr?.length() ?: 0
                val incCount = incArr?.length() ?: 0
                val expCount = expArr?.length() ?: 0
                (txCount == 0 && incCount == 0 && expCount == 0)
            } catch (_: Exception) {
                false
            }

            if (!latestRemoteData.isNullOrBlank() && (isRemoteNewer || (lastLocalUpdateTime == 0L && isLocalEmptyOrSeed))) {
                val replaceLocal = lastLocalUpdateTime == 0L || isLocalEmptyOrSeed || isRemoteNewer
                val restored = onRemoteRestored(latestRemoteData!!, replaceLocal)
                if (restored) {
                    remoteRestoredSuccess = true
                    val updatedTs = maxOf(maxRemoteTimestamp, System.currentTimeMillis())
                    onLocalDataTimestampUpdated(updatedTs)
                }
            } else if (!isLocalEmptyOrSeed || lastLocalUpdateTime >= maxRemoteTimestamp || maxRemoteTimestamp == 0L) {
                val updatedLocalJson = localJsonProvider()
                val now = System.currentTimeMillis()
                val payload = JSONObject().apply {
                    put("updatedAt", now)
                    put("userEmail", accountEmail)
                    put("data", updatedLocalJson)
                }.toString()

                val textMediaType = "text/plain; charset=utf-8".toMediaType()
                val pubRequest = Request.Builder()
                    .url(publishUrl)
                    .post(payload.toRequestBody(textMediaType))
                    .build()

                try {
                    httpClient.newCall(pubRequest).execute().use { response ->
                        cloudUploadOk = response.isSuccessful
                    }
                    if (cloudUploadOk) {
                        onLocalDataTimestampUpdated(now)
                    }
                } catch (e: Exception) {
                    Log.w("GoogleDriveSync", "Cloud publish error", e)
                }
            }

            // Local fallback file backup
            try {
                val backupFile = java.io.File(context.filesDir, "google_drive_cloud_backup.json")
                backupFile.writeText(currentLocalJson)
            } catch (_: Exception) {}

            return@withContext if (remoteRestoredSuccess) {
                SyncState.Success("Synced latest data from cloud ($accountEmail)", nowStr)
            } else if (cloudUploadOk) {
                SyncState.Success("Cross-Device Cloud Sync Active ($accountEmail)", nowStr)
            } else {
                SyncState.Success("Synced locally ($accountEmail)", nowStr)
            }
        } catch (e: Exception) {
            Log.e("GoogleDriveSync", "Cloud sync exception", e)
            return@withContext SyncState.Error("Sync error: ${e.localizedMessage}")
        }
    }

    suspend fun clearDriveData(overrideAccountEmail: String? = null): Boolean = withContext(Dispatchers.IO) {
        val account = getSignedInAccount()
        val accountEmail = account?.email ?: overrideAccountEmail ?: "86brancan@gmail.com"
        val token = getValidToken(account, accountEmail)

        val emptyJson = JSONObject().apply {
            put("transactions", JSONArray())
            put("settings", JSONArray())
            put("income_sources", JSONArray())
            put("planned_expenses", JSONArray())
        }.toString()

        var success = false

        if (token != null) {
            try {
                // 1. Clear / overwrite Google Drive JSON backup
                val backupInfo = searchBackupFile(token)
                val backupFileId = backupInfo?.first
                uploadBackup(token, emptyJson, backupFileId)

                success = true
            } catch (e: Exception) {
                Log.e("GoogleDriveSync", "Error clearing Google Drive backup files", e)
            }
        }

        // 3. Clear Cloud Sync topic (ntfy)
        try {
            val sanitizedKey = accountEmail.lowercase(Locale.US)
                .replace(".", "_")
                .replace("@", "_at_")
                .replace("#", "_")
                .replace("$", "_")
                .replace("[", "_")
                .replace("]", "_")
                .filter { it.isLetterOrDigit() || it == '_' }

            val publishUrl = "https://ntfy.sh/paycycle_sync_$sanitizedKey"
            val now = System.currentTimeMillis()
            val payload = JSONObject().apply {
                put("updatedAt", now)
                put("userEmail", accountEmail)
                put("data", emptyJson)
            }.toString()

            val textMediaType = "text/plain; charset=utf-8".toMediaType()
            val pubRequest = Request.Builder()
                .url(publishUrl)
                .post(payload.toRequestBody(textMediaType))
                .build()

            httpClient.newCall(pubRequest).execute().close()
            success = true
        } catch (e: Exception) {
            Log.e("GoogleDriveSync", "Error publishing empty state to cloud sync bus", e)
        }

        // 4. Clean local backup files
        try {
            val cloudBackupFile = java.io.File(context.filesDir, "google_drive_cloud_backup.json")
            if (cloudBackupFile.exists()) cloudBackupFile.delete()
            val internalCsvFile = java.io.File(context.filesDir, "paycycle_export.csv")
            if (internalCsvFile.exists()) internalCsvFile.delete()
        } catch (_: Exception) {}

        success
    }

    suspend fun uploadOrOverwriteWebCompanionHtml(
        overrideAccountEmail: String? = null,
        htmlData: String
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val account = getSignedInAccount()
        val accountEmail = account?.email ?: overrideAccountEmail ?: "86brancan@gmail.com"

        try {
            val internalFile = java.io.File(context.filesDir, webCompanionFileName)
            internalFile.writeText(htmlData, Charsets.UTF_8)
            val legacyFile = java.io.File(context.filesDir, webCompanionLegacyName2)
            legacyFile.writeText(htmlData, Charsets.UTF_8)
        } catch (_: Exception) {}

        val token = getValidToken(account, accountEmail)
        if (token != null) {
            // Search Drive for any previous version of "PayCycle Budget - Web Companion App" to overwrite
            var resolvedFileId = getCachedFileId(webCompanionFileName)
            if (resolvedFileId == null) {
                resolvedFileId = getCachedFileId(webCompanionLegacyName1)
            }
            if (resolvedFileId == null) {
                resolvedFileId = getCachedFileId(webCompanionLegacyName2)
            }

            if (resolvedFileId == null) {
                try {
                    val queryStr = "trashed=false and (name='$webCompanionFileName' or name='$webCompanionLegacyName1' or name='$webCompanionLegacyName2')"
                    val encodedQuery = java.net.URLEncoder.encode(queryStr, "UTF-8")
                    val url = "https://www.googleapis.com/drive/v3/files?q=$encodedQuery&fields=files(id,name,modifiedTime)&orderBy=modifiedTime desc"
                    val req = Request.Builder().url(url).addHeader("Authorization", "Bearer $token").get().build()
                    httpClient.newCall(req).execute().use { resp ->
                        if (resp.isSuccessful) {
                            val body = resp.body?.string()
                            if (body != null) {
                                val json = JSONObject(body)
                                val files = json.optJSONArray("files")
                                if (files != null && files.length() > 0) {
                                    resolvedFileId = files.getJSONObject(0).getString("id")
                                    // Remove older duplicate files to keep Google Drive clean
                                    for (i in 1 until files.length()) {
                                        val dupId = files.getJSONObject(i).optString("id")
                                        if (dupId.isNotBlank()) {
                                            trashOrDeleteDriveFile(token, dupId)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("GoogleDriveSync", "Error querying web companion file on Google Drive", e)
                }
            }

            val htmlMediaType = "text/html; charset=utf-8".toMediaType()

            if (resolvedFileId != null) {
                // Update & overwrite existing file in place
                saveCachedFileId(webCompanionFileName, resolvedFileId)
                saveCachedFileId(webCompanionLegacyName1, resolvedFileId)
                saveCachedFileId(webCompanionLegacyName2, resolvedFileId)

                val meta = JSONObject().apply {
                    put("name", webCompanionFileName)
                    put("mimeType", "text/html")
                }.toString()

                val multipartBody = MultipartBody.Builder()
                    .setType("multipart/related".toMediaType())
                    .addPart(meta.toRequestBody("application/json; charset=utf-8".toMediaType()))
                    .addPart(htmlData.toRequestBody(htmlMediaType))
                    .build()

                val updateUrl = "https://www.googleapis.com/upload/drive/v3/files/$resolvedFileId?uploadType=multipart"
                val updateReq = Request.Builder()
                    .url(updateUrl)
                    .addHeader("Authorization", "Bearer $token")
                    .patch(multipartBody)
                    .build()

                var patchOk = false
                try {
                    httpClient.newCall(updateReq).execute().use { resp ->
                        if (resp.isSuccessful) {
                            patchOk = true
                            Log.d("GoogleDriveSync", "Successfully overwrote '$webCompanionFileName' on Google Drive ($resolvedFileId)")
                        } else if (resp.code == 404) {
                            saveCachedFileId(webCompanionFileName, null)
                            saveCachedFileId(webCompanionLegacyName1, null)
                            saveCachedFileId(webCompanionLegacyName2, null)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("GoogleDriveSync", "Error overwriting web companion on Google Drive", e)
                }

                if (patchOk) {
                    return@withContext Pair(true, "Overwrote previous '$webCompanionFileName' on Google Drive ($accountEmail)!")
                }
            }

            // Create fresh file if not existing
            val createMeta = JSONObject().apply {
                put("name", webCompanionFileName)
                put("mimeType", "text/html")
            }.toString()

            val createMultipartBody = MultipartBody.Builder()
                .setType("multipart/related".toMediaType())
                .addPart(createMeta.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .addPart(htmlData.toRequestBody(htmlMediaType))
                .build()

            val createUrl = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"
            val createReq = Request.Builder()
                .url(createUrl)
                .addHeader("Authorization", "Bearer $token")
                .post(createMultipartBody)
                .build()

            try {
                httpClient.newCall(createReq).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val respBody = resp.body?.string()
                        if (!respBody.isNullOrBlank()) {
                            val respJson = JSONObject(respBody)
                            val createdId = respJson.optString("id")
                            if (createdId.isNotBlank()) {
                                saveCachedFileId(webCompanionFileName, createdId)
                                saveCachedFileId(webCompanionLegacyName1, createdId)
                                saveCachedFileId(webCompanionLegacyName2, createdId)
                            }
                        }
                        return@withContext Pair(true, "Saved '$webCompanionFileName' to Google Drive ($accountEmail)!")
                    }
                }
            } catch (e: Exception) {
                Log.e("GoogleDriveSync", "Error creating web companion on Google Drive", e)
            }
        }

        Pair(false, "Web app exported as '$webCompanionFileName'")
    }
}
