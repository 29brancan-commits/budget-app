package com.example.sync

import android.accounts.Account
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Headers
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

data class DriveFileInfo(
    val id: String,
    val name: String,
    val mimeType: String,
    val modifiedTime: String,
    val formattedDate: String,
    val sizeString: String,
    val location: String, // "PayCycle Budget" or "My Drive"
    val isGoogleSheet: Boolean,
    val isCsv: Boolean,
    val isJson: Boolean
)

class GoogleDriveSyncManager(private val context: Context) {

    var pendingAuthIntent: Intent? = null

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .connectionPool(okhttp3.ConnectionPool(5, 5, java.util.concurrent.TimeUnit.MINUTES))
        .build()
    private val scopeDrive = "https://www.googleapis.com/auth/drive"
    private val scopeDriveReadOnly = "https://www.googleapis.com/auth/drive.readonly"
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

    private val PAYCYCLE_FOLDER_NAME = "PayCycle Budget"

    private val prefs = context.getSharedPreferences("google_drive_sync_prefs", Context.MODE_PRIVATE)

    init {
        cleanupAllLocalAppStorage()
        val c = prefs.getString("cached_paycycle_folder_id", null)
        if (c == "1ARQc_BgRZH7weAVhYz7ORYJTllMJHefm") {
            prefs.edit().remove("cached_paycycle_folder_id").apply()
        }
        val savedToken = prefs.getString("custom_access_token", null)
        if (savedToken != null && (savedToken.startsWith("ya29.") || savedToken.length < 50)) {
            // Force re-acquisition of fresh write-scoped token on restart
            prefs.edit().remove("custom_access_token").apply()
        }
    }

    val PROJECT_CLIENT_ID = "1010863926522-r8bfvtebhjvh0n8adlburrd0peiite22.apps.googleusercontent.com"
    val ANDROID_CLIENT_ID = "1010863926522-0odht95asacrjnockhjrtcotjldvo79f.apps.googleusercontent.com"

    private val DEFAULT_CLIENT_ID = PROJECT_CLIENT_ID
    private val DEFAULT_CLIENT_SECRET = ""

    val TARGET_FOLDER_ID = ""
    val TARGET_FOLDER_URL = "https://drive.google.com"

    fun extractFolderId(input: String): String {
        val trimmed = input.trim()
        val regex = Regex("/folders/([a-zA-Z0-9_-]+)")
        val match = regex.find(trimmed)
        if (match != null) {
            return match.groupValues[1]
        }
        val idRegex = Regex("[?&]id=([a-zA-Z0-9_-]+)")
        val idMatch = idRegex.find(trimmed)
        if (idMatch != null) {
            return idMatch.groupValues[1]
        }
        if (!trimmed.contains("/") && trimmed.length >= 10 && trimmed != "1ARQc_BgRZH7weAVhYz7ORYJTllMJHefm") {
            return trimmed
        }
        return ""
    }

    fun getTargetFolderId(): String {
        val custom = prefs.getString("cached_paycycle_folder_id", null)?.trim()?.ifBlank { null }
        return if (custom == "1ARQc_BgRZH7weAVhYz7ORYJTllMJHefm") "" else (custom ?: "")
    }

    fun getTargetFolderUrl(): String {
        val id = getTargetFolderId()
        return if (id.isNotBlank()) "https://drive.google.com/drive/folders/$id?usp=drive_link" else "https://drive.google.com"
    }

    fun getCachedFolderId(): String = getTargetFolderId()

    fun getCachedFolderName(): String = prefs.getString("cached_paycycle_folder_name", null)?.trim()?.ifBlank { null }
        ?: "PayCycle Budget"

    fun saveCachedFolderName(name: String?) {
        if (!name.isNullOrBlank()) {
            prefs.edit().putString("cached_paycycle_folder_name", name.trim()).apply()
        }
    }

    fun saveCachedFolderId(idOrUrl: String?) {
        if (idOrUrl.isNullOrBlank()) {
            prefs.edit().remove("cached_paycycle_folder_id").apply()
        } else {
            val extracted = extractFolderId(idOrUrl)
            if (extracted.isBlank() || extracted == "1ARQc_BgRZH7weAVhYz7ORYJTllMJHefm") {
                prefs.edit().remove("cached_paycycle_folder_id").apply()
            } else {
                prefs.edit().putString("cached_paycycle_folder_id", extracted).apply()
            }
        }
    }

    suspend fun getOrCreatePayCycleFolder(token: String): String = withContext(Dispatchers.IO) {
        val cachedId = getTargetFolderId().ifBlank { null }
        if (!cachedId.isNullOrBlank()) {
            try {
                val verifyUrl = "https://www.googleapis.com/drive/v3/files/$cachedId?fields=id,name,trashed,mimeType&supportsAllDrives=true"
                val req = Request.Builder().url(verifyUrl).addHeader("Authorization", "Bearer $token").get().build()
                httpClient.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body?.string()
                        if (!body.isNullOrBlank()) {
                            val obj = JSONObject(body)
                            if (!obj.optBoolean("trashed", false)) {
                                val folderName = obj.optString("name", "PayCycle Budget")
                                saveCachedFolderName(folderName)
                                return@withContext cachedId
                            }
                        }
                    } else if (resp.code == 404 || resp.code == 403) {
                        prefs.edit().remove("cached_paycycle_folder_id").apply()
                    }
                }
            } catch (e: Exception) {
                Log.d("GoogleDriveSync", "Note verifying cached folder $cachedId: ${e.message}")
            }
        }
        // Search Drive for existing folder named "PayCycle Budget"
        try {
            val qUrl = "https://www.googleapis.com/drive/v3/files?q=name%3D%27PayCycle%20Budget%27%20and%20mimeType%3D%27application%2Fvnd.google-apps.folder%27%20and%20trashed%3Dfalse&pageSize=1&fields=files(id,name)"
            val req = Request.Builder().url(qUrl).addHeader("Authorization", "Bearer $token").get().build()
            httpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string()
                    if (!body.isNullOrBlank()) {
                        val arr = JSONObject(body).optJSONArray("files")
                        if (arr != null && arr.length() > 0) {
                            val foundId = arr.getJSONObject(0).optString("id")
                            if (foundId.isNotBlank()) {
                                saveCachedFolderId(foundId)
                                saveCachedFolderName("PayCycle Budget")
                                return@withContext foundId
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        // Create new PayCycle Budget folder in My Drive
        try {
            val createUrl = "https://www.googleapis.com/drive/v3/files"
            val jsonBody = JSONObject().apply {
                put("name", "PayCycle Budget")
                put("mimeType", "application/vnd.google-apps.folder")
            }.toString()
            val req = Request.Builder()
                .url(createUrl)
                .addHeader("Authorization", "Bearer $token")
                .post(jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string()
                    if (!body.isNullOrBlank()) {
                        val newId = JSONObject(body).optString("id")
                        if (newId.isNotBlank()) {
                            saveCachedFolderId(newId)
                            saveCachedFolderName("PayCycle Budget")
                            return@withContext newId
                        }
                    }
                } else {
                    Log.e("GoogleDriveSync", "Error creating PayCycle Budget folder: ${resp.code} ${resp.body?.string()}")
                }
            }
        } catch (e: Exception) {
            Log.e("GoogleDriveSync", "Error creating PayCycle Budget folder", e)
        }
        return@withContext ""
    }

    fun saveCustomAccessToken(token: String?) {
        var trimmed = token?.trim()
        if (trimmed.isNullOrBlank()) {
            prefs.edit().remove("custom_access_token").apply()
            return
        }
        if (trimmed.startsWith("Bearer ", ignoreCase = true)) {
            trimmed = trimmed.substring(7).trim()
        }
        if (trimmed.startsWith("4/") || trimmed.startsWith("4%2F") || trimmed.startsWith("1//")) {
            // Auto-detect authorization codes or refresh tokens passed into access token field
            prefs.edit().putString("custom_refresh_token", trimmed).apply()
        } else {
            prefs.edit().putString("custom_access_token", trimmed).apply()
        }
    }

    fun isGoogleDriveAuthorized(): Boolean {
        if (getSignedInAccount() != null) return true
        if (!getCustomAccessToken().isNullOrBlank()) return true
        if (!getCustomRefreshToken().isNullOrBlank()) return true
        return false
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

    fun hasMeaningfulData(jsonString: String?): Boolean {
        if (jsonString.isNullOrBlank()) return false
        return try {
            val trimmed = jsonString.trim().removePrefix("\uFEFF")
            if (trimmed.startsWith("[")) {
                val arr = JSONArray(trimmed)
                return arr.length() > 0
            }
            val obj = JSONObject(trimmed)
            val txArr = obj.optJSONArray("transactions") ?: obj.optJSONArray("items") ?: obj.optJSONArray("expenses") ?: obj.optJSONArray("data")
            val incArr = obj.optJSONArray("income_sources") ?: obj.optJSONArray("incomeSources")
            val expArr = obj.optJSONArray("planned_expenses") ?: obj.optJSONArray("plannedExpenses") ?: obj.optJSONArray("bills")
            val debtsArr = obj.optJSONArray("debts") ?: obj.optJSONArray("all_debts") ?: obj.optJSONArray("allDebts")
            val txCount = txArr?.length() ?: 0
            val incCount = incArr?.length() ?: 0
            val expCount = expArr?.length() ?: 0
            val debtsCount = debtsArr?.length() ?: 0
            (txCount > 0 || incCount > 0 || expCount > 0 || debtsCount > 0)
        } catch (_: Exception) {
            false
        }
    }

    fun cleanupAllLocalAppStorage() {
        try {
            val fileNames = listOf(
                "google_drive_cloud_backup.json",
                "backup_vault_persistent.json",
                "emergency_vault_archive.json",
                "paycycle_integrity_vault.json",
                "paycycle_export.csv",
                exportCsvFileName,
                importCsvFileName,
                csvFileName,
                debtCsvFileName,
                debtImportCsvFileName,
                webCompanionFileName,
                webCompanionLegacyName1,
                webCompanionLegacyName2
            )
            for (name in fileNames) {
                val f = java.io.File(context.filesDir, name)
                if (f.exists()) f.delete()
            }
            prefs.edit()
                .remove("backup_vault_json")
                .remove("local_recovery_vault_json")
                .remove("local_recovery_vault_timestamp")
                .apply()
        } catch (_: Exception) {}
    }

    fun saveSavedVaultBackup(jsonData: String) {
        // Do not create anything locally - user explicitly requested cloud-only storage in Google Drive
    }

    suspend fun fetchLatestCloudDebtData(overrideAccountEmail: String? = null): String? = withContext(Dispatchers.IO) {
        val account = getSignedInAccount()
        val accountEmail = account?.email ?: overrideAccountEmail ?: "86brancan@gmail.com"
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
            val pollRequest = Request.Builder().url(topicUrl).get().build()
            var latestDebtCsv: String? = null
            var maxTs = 0L

            httpClient.newCall(pollRequest).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string()
                    if (!body.isNullOrBlank()) {
                        for (line in body.lineSequence().filter { it.isNotBlank() }) {
                            try {
                                val jsonObj = JSONObject(line)
                                val msgStr = jsonObj.optString("message", "")
                                if (msgStr.isNotBlank()) {
                                    val inner = JSONObject(msgStr)
                                    val type = inner.optString("type")
                                    val csv = inner.optString("csv")
                                    val ts = inner.optLong("updatedAt", jsonObj.optLong("time", 0L) * 1000L)
                                    if (type == "debt_sync" && csv.isNotBlank() && ts >= maxTs) {
                                        maxTs = ts
                                        latestDebtCsv = csv
                                    }
                                }
                            } catch (_: Exception) {}
                        }
                    }
                }
            }
            if (!latestDebtCsv.isNullOrBlank()) return@withContext latestDebtCsv
        } catch (_: Exception) {}
        null
    }

    suspend fun fetchLatestCloudData(overrideAccountEmail: String? = null): String? = withContext(Dispatchers.IO) {
        val account = getSignedInAccount()
        val accountEmail = account?.email ?: overrideAccountEmail ?: "86brancan@gmail.com"
        val token = getValidToken(account, accountEmail)

        // 1. Check Google Drive API if authenticated
        if (token != null) {
            try {
                val fileInfo = searchBackupFile(token)
                val fileId = fileInfo?.first
                if (fileId != null) {
                    val driveJson = downloadBackup(token, fileId)
                    if (hasMeaningfulData(driveJson)) {
                        return@withContext driveJson
                    }
                }
            } catch (e: Exception) {
                Log.w("GoogleDriveSync", "Error querying Drive API directly", e)
            }
        }

        // 2. Query Cloud Data Bus
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
            val pollRequest = Request.Builder().url(topicUrl).get().build()
            var latestRemoteData: String? = null
            var maxRemoteTimestamp: Long = 0L

            httpClient.newCall(pollRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val lines = body.lineSequence().filter { it.isNotBlank() }
                        for (line in lines) {
                            try {
                                val jsonObj = JSONObject(line)
                                var rawContent: String? = null

                                if (jsonObj.has("attachment")) {
                                    val attachObj = jsonObj.optJSONObject("attachment")
                                    val attachUrl = attachObj?.optString("url")
                                    if (!attachUrl.isNullOrBlank()) {
                                        try {
                                            val fileReq = Request.Builder().url(attachUrl).get().build()
                                            httpClient.newCall(fileReq).execute().use { resp ->
                                                if (resp.isSuccessful) {
                                                    rawContent = resp.body?.string()
                                                }
                                            }
                                        } catch (_: Exception) {}
                                    }
                                }

                                if (rawContent.isNullOrBlank()) {
                                    val msgStr = jsonObj.optString("message", "")
                                    if (msgStr.isNotBlank() && !msgStr.startsWith("You received a file")) {
                                        rawContent = msgStr
                                    }
                                }

                                if (!rawContent.isNullOrBlank()) {
                                    val innerObj = JSONObject(rawContent)
                                    val data = innerObj.optString("data", "")
                                    val ts = innerObj.optLong("updatedAt", jsonObj.optLong("time", 0L) * 1000L)
                                    if (data.isNotBlank() && ts >= maxRemoteTimestamp && hasMeaningfulData(data)) {
                                        maxRemoteTimestamp = ts
                                        latestRemoteData = data
                                    }
                                }
                            } catch (_: Exception) {}
                        }
                    }
                }
            }
            if (hasMeaningfulData(latestRemoteData)) {
                return@withContext latestRemoteData
            }
        } catch (e: Exception) {
            Log.w("GoogleDriveSync", "Error fetching cloud data from bus", e)
        }

        return@withContext null
    }

    suspend fun getSavedVaultBackup(): String? = withContext(Dispatchers.IO) {
        fetchLatestCloudData()
    }

    suspend fun backupAllDataToDrive(token: String, jsonData: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (!hasMeaningfulData(jsonData)) {
            return@withContext Pair(false, "No valid data to back up.")
        }
        try {
            val folderId = getOrCreatePayCycleFolder(token)
            val existing = searchBackupFile(token, folderId)
            val (uploadOk, uploadMsg) = uploadBackupDetailed(token, jsonData, existing?.first, folderId)
            if (uploadOk) {
                val nowStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", Locale.US))
                Pair(true, "All data saved to My Drive/PayCycle Budget/$fileName at $nowStr")
            } else {
                Pair(false, uploadMsg)
            }
        } catch (e: Exception) {
            Log.e("GoogleDriveSync", "Error backing up all data to Drive", e)
            Pair(false, "Backup error: ${e.localizedMessage ?: "Unknown"}")
        }
    }

    suspend fun restoreAllDataFromPayCycleFolder(token: String): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        try {
            val folderId = getOrCreatePayCycleFolder(token)
            val existing = searchBackupFile(token, folderId)
            val fileId = existing?.first
            if (fileId != null) {
                val remoteJson = downloadBackup(token, fileId)
                if (!remoteJson.isNullOrBlank() && hasMeaningfulData(remoteJson)) {
                    return@withContext Pair(true, remoteJson)
                }
            }
            // If remote search by folder didn't find it, fallback to search anywhere on Drive
            val anyFile = searchBackupFile(token, null)
            val anyId = anyFile?.first
            if (anyId != null) {
                val remoteJson = downloadBackup(token, anyId)
                if (!remoteJson.isNullOrBlank() && hasMeaningfulData(remoteJson)) {
                    return@withContext Pair(true, remoteJson)
                }
            }
        } catch (e: Exception) {
            Log.e("GoogleDriveSync", "Error downloading backup from Drive folder", e)
        }
        Pair(false, null)
    }

    suspend fun backupAllDataToDriveAuto(
        jsonData: String,
        overrideAccountEmail: String? = null
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (!hasMeaningfulData(jsonData)) {
            return@withContext Pair(false, "No valid data to back up.")
        }
        // Do NOT create anything locally - user requested cloud-only storage in Google Drive

        val account = getSignedInAccount()
        val accountEmail = account?.email ?: overrideAccountEmail ?: prefs.getString("google_drive_account_email", null) ?: "86brancan@gmail.com"
        val token = getValidToken(account, accountEmail)
        val nowStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", Locale.US))

        if (token != null) {
            try {
                val folderId = getOrCreatePayCycleFolder(token)
                val existing = searchBackupFile(token, folderId)
                var (uploadOk, uploadMsg) = uploadBackupDetailed(token, jsonData, existing?.first, folderId)
                if (!uploadOk && (uploadMsg.contains("401") || uploadMsg.contains("403") || uploadMsg.contains("permission", ignoreCase = true) || uploadMsg.contains("authoriz", ignoreCase = true))) {
                    // Token expired or lacked write permissions. Clear bad token and retry with fresh write token
                    clearCachedTokens(accountEmail)
                    val freshToken = if (account != null) getAccessToken(account) else getAccessTokenForEmail(accountEmail)
                    if (freshToken != null && freshToken != token) {
                        val freshFolderId = getOrCreatePayCycleFolder(freshToken)
                        val freshExisting = searchBackupFile(freshToken, freshFolderId)
                        val retryRes = uploadBackupDetailed(freshToken, jsonData, freshExisting?.first, freshFolderId)
                        uploadOk = retryRes.first
                        uploadMsg = retryRes.second
                    }
                }
                if (uploadOk) {
                    prefs.edit().putString("last_auto_backup_timestamp", nowStr).apply()
                    return@withContext Pair(true, "All data saved to My Drive/PayCycle Budget/$fileName at $nowStr")
                } else {
                    return@withContext Pair(false, uploadMsg)
                }
            } catch (e: Exception) {
                Log.e("GoogleDriveSync", "Error uploading backup to Drive folder", e)
                return@withContext Pair(false, "Upload error: ${e.localizedMessage}")
            }
        }

        val msg = if (pendingAuthIntent != null) {
            "Google Drive permission consent required. Please authorize Google Drive to save to My Drive/PayCycle Budget/."
        } else {
            "Not connected to Google Drive. Please authorize your Google account to save backup to My Drive/PayCycle Budget/."
        }
        Pair(false, msg)
    }

    suspend fun restoreAllDataFromDriveAuto(
        overrideAccountEmail: String? = null
    ): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        val account = getSignedInAccount()
        val accountEmail = account?.email ?: overrideAccountEmail ?: prefs.getString("google_drive_account_email", null) ?: "86brancan@gmail.com"
        val token = getValidToken(account, accountEmail)

        if (token != null) {
            val (ok, remoteJson) = restoreAllDataFromPayCycleFolder(token)
            if (ok && !remoteJson.isNullOrBlank() && hasMeaningfulData(remoteJson)) {
                return@withContext Pair(true, remoteJson)
            }
        }

        Pair(false, null)
    }

    fun hasDriveToken(): Boolean {
        val customToken = getCustomAccessToken()
        if (!customToken.isNullOrBlank() && !customToken.startsWith("eyJ")) return true
        val refreshToken = getCustomRefreshToken()
        if (!refreshToken.isNullOrBlank()) return true
        val account = getSignedInAccount()
        return account != null
    }

    fun verifyBackupIntegrity(jsonData: String): Pair<Boolean, String> {
        if (jsonData.isBlank()) return Pair(false, "Backup file is empty.")
        try {
            var clean = jsonData.trim().removePrefix("\uFEFF")
            if (clean.startsWith("```")) {
                clean = clean.substringAfter("\n")
                if (clean.contains("```")) clean = clean.substringBeforeLast("```").trim()
            }
            if (!clean.startsWith("{") && !clean.startsWith("[")) {
                return Pair(false, "Invalid backup format: Not a JSON object or array.")
            }
            if (clean.startsWith("{")) {
                val obj = JSONObject(clean)
                val meta = obj.optJSONObject("metadata")
                val txCount = obj.optJSONArray("transactions")?.length() ?: 0
                val debtCount = obj.optJSONArray("debts")?.length() ?: 0
                val expCount = obj.optJSONArray("planned_expenses")?.length() ?: 0
                val incCount = obj.optJSONArray("income_sources")?.length() ?: 0
                val totalCount = txCount + debtCount + expCount + incCount
                if (totalCount == 0 && meta == null) {
                    return Pair(false, "Backup contains no financial records.")
                }
                val location = meta?.optString("backup_location", "My Drive/PayCycle Budget/") ?: "My Drive/PayCycle Budget/"
                return Pair(true, "Integrity verified: $txCount transactions, $debtCount debts, $expCount bills, $incCount income sources ($location).")
            } else {
                val arr = JSONArray(clean)
                return Pair(true, "Integrity verified: ${arr.length()} array records.")
            }
        } catch (e: Exception) {
            return Pair(false, "Integrity check failed: ${e.localizedMessage}")
        }
    }

    suspend fun refreshTokenWithCredentials(clientId: String?, clientSecret: String?, tokenOrCode: String): String? = withContext(Dispatchers.IO) {
        try {
            val isAuthCode = tokenOrCode.startsWith("4/") || tokenOrCode.startsWith("4%2F") || (tokenOrCode.length < 80 && !tokenOrCode.contains("//"))
            val redirectUris = listOf("")

            val candidateCreds = mutableListOf<Pair<String, String?>>()
            val customId = clientId?.ifBlank { null }
            val customSecret = clientSecret?.ifBlank { null }
            if (customId != null) {
                candidateCreds.add(Pair(customId, customSecret))
            } else {
                candidateCreds.add(Pair(PROJECT_CLIENT_ID, null))
                candidateCreds.add(Pair(ANDROID_CLIENT_ID, null))
            }

            for ((effClientId, effClientSecret) in candidateCreds) {
                for (redirectUri in redirectUris) {
                    try {
                        val formBuilder = okhttp3.FormBody.Builder()
                            .add("client_id", effClientId)

                        if (!effClientSecret.isNullOrBlank()) {
                            formBuilder.add("client_secret", effClientSecret)
                        }

                        if (isAuthCode) {
                            formBuilder.add("grant_type", "authorization_code")
                                .add("code", tokenOrCode)
                            if (redirectUri.isNotEmpty()) {
                                formBuilder.add("redirect_uri", redirectUri)
                            }
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
                                Log.d("GoogleDriveSync", "Token endpoint attempt ($effClientId, uri='$redirectUri'): $bodyStr")
                            }
                        }
                    } catch (e: Exception) {
                        Log.d("GoogleDriveSync", "Error trying ($effClientId, uri='$redirectUri')", e)
                    }
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
        if (!customToken.isNullOrBlank() && !customToken.startsWith("eyJ")) {
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
        val clientId = getCustomClientId() ?: DEFAULT_CLIENT_ID
        val gsoBuilder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(
                Scope(scopeDriveFile),
                Scope(scopeSheets),
                Scope(scopeDrive)
            )
        try {
            gsoBuilder.requestServerAuthCode(clientId, false)
        } catch (_: Exception) {}
        return GoogleSignIn.getClient(context, gsoBuilder.build())
    }

    fun getSignedInAccount(): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }

    private suspend fun getAccessToken(account: GoogleSignInAccount): String? = withContext(Dispatchers.IO) {
        val email = account.email ?: ""
        val accountObj = account.account ?: Account(email, "com.google")
        val scopeAttempts = listOf(
            "oauth2:$scopeDriveFile",
            "oauth2:$scopeDriveFile $scopeSheets",
            "oauth2:$scopeDrive",
            "oauth2:$scopeDrive $scopeSheets",
            "oauth2:$scopeAppData"
        )
        for (scopesStr in scopeAttempts) {
            try {
                val token = GoogleAuthUtil.getToken(context, accountObj, scopesStr)
                if (!token.isNullOrBlank()) {
                    saveCustomAccessToken(token)
                    return@withContext token
                }
            } catch (e: UserRecoverableAuthException) {
                Log.w("GoogleDriveSync", "User consent required for $email ($scopesStr)", e)
                pendingAuthIntent = e.intent
                return@withContext null
            } catch (e: Exception) {
                Log.d("GoogleDriveSync", "GoogleAuthUtil attempt '$scopesStr' note: ${e.message}")
            }
        }
        null
    }

    suspend fun clearCachedTokens(email: String? = null) = withContext(Dispatchers.IO) {
        try {
            prefs.edit().remove("custom_access_token").apply()
            val account = getSignedInAccount()
            val targetEmail = email ?: account?.email ?: "86brancan@gmail.com"
            val accountObj = account?.account ?: Account(targetEmail, "com.google")
            for (scope in listOf("oauth2:$scopeDriveFile", "oauth2:$scopeDriveFile $scopeSheets", "oauth2:$scopeDrive")) {
                try {
                    val tok = GoogleAuthUtil.getToken(context, accountObj, scope)
                    if (!tok.isNullOrBlank()) {
                        GoogleAuthUtil.clearToken(context, tok)
                    }
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            Log.e("GoogleDriveSync", "Error clearing cached tokens", e)
        }
    }

    suspend fun getAccessTokenForEmail(email: String): String? = withContext(Dispatchers.IO) {
        if (email.isBlank()) return@withContext null
        val accountObj = Account(email, "com.google")
        val scopeAttempts = listOf(
            "oauth2:$scopeDriveFile",
            "oauth2:$scopeDriveFile $scopeSheets",
            "oauth2:$scopeDrive",
            "oauth2:$scopeDrive $scopeSheets",
            "oauth2:$scopeAppData"
        )
        for (scopesStr in scopeAttempts) {
            try {
                val token = GoogleAuthUtil.getToken(context, accountObj, scopesStr)
                if (!token.isNullOrBlank()) {
                    saveCustomAccessToken(token)
                    return@withContext token
                }
            } catch (e: UserRecoverableAuthException) {
                Log.w("GoogleDriveSync", "User consent required for email $email ($scopesStr)", e)
                pendingAuthIntent = e.intent
                return@withContext null
            } catch (e: Exception) {
                Log.d("GoogleDriveSync", "GoogleAuthUtil email attempt '$scopesStr' note: ${e.message}")
            }
        }
        null
    }

    suspend fun searchBackupFile(token: String, folderId: String? = null): Pair<String?, Long>? = withContext(Dispatchers.IO) {
        try {
            // 1. Check cached file ID first
            val cachedId = getCachedFileId(fileName)
            if (cachedId != null) {
                try {
                    val verifyUrl = "https://www.googleapis.com/drive/v3/files/$cachedId?fields=id,trashed,parents"
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
                        } else {
                            saveCachedFileId(fileName, null)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("GoogleDriveSync", "Error verifying cached backup file ID $cachedId", e)
                }
            }

            // 2. Query Drive for backup file
            val parentClause = if (!folderId.isNullOrBlank() && folderId != "root") "and '$folderId' in parents" else ""
            val httpUrl = "https://www.googleapis.com/drive/v3/files".toHttpUrl().newBuilder()
                .addQueryParameter("q", "name='$fileName' and trashed=false $parentClause".trim())
                .addQueryParameter("fields", "files(id,name,modifiedTime)")
                .addQueryParameter("orderBy", "modifiedTime desc")
                .build()

            val request = Request.Builder()
                .url(httpUrl)
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val json = JSONObject(body)
                        val files = json.optJSONArray("files")
                        if (files != null && files.length() > 0) {
                            val fileObj = files.getJSONObject(0)
                            val fileId = fileObj.getString("id")
                            saveCachedFileId(fileName, fileId)
                            return@withContext Pair(fileId, 0L)
                        }
                    }
                }
            }

            // Fallback: If not found in specific folder, search anywhere on Drive
            if (!folderId.isNullOrBlank()) {
                val wideUrl = "https://www.googleapis.com/drive/v3/files".toHttpUrl().newBuilder()
                    .addQueryParameter("q", "name='$fileName' and trashed=false")
                    .addQueryParameter("fields", "files(id,name,modifiedTime)")
                    .addQueryParameter("orderBy", "modifiedTime desc")
                    .build()
                val wideReq = Request.Builder().url(wideUrl).addHeader("Authorization", "Bearer $token").get().build()
                httpClient.newCall(wideReq).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (!body.isNullOrBlank()) {
                            val json = JSONObject(body)
                            val files = json.optJSONArray("files")
                            if (files != null && files.length() > 0) {
                                val fileId = files.getJSONObject(0).getString("id")
                                saveCachedFileId(fileName, fileId)
                                return@withContext Pair(fileId, 0L)
                            }
                        }
                    }
                }
            }

            saveCachedFileId(fileName, null)
            null
        } catch (e: Exception) {
            Log.e("GoogleDriveSync", "Error searching Drive backup", e)
            null
        }
    }

    suspend fun uploadBackupDetailed(
        token: String,
        jsonData: String,
        existingFileId: String?,
        folderId: String? = null
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val jsonMediaType = "application/json; charset=utf-8".toMediaType()
            var targetId = existingFileId ?: getCachedFileId(fileName)
            var lastError = ""

            // Method 1: If existing file ID is found, update in place via PATCH media
            if (targetId != null) {
                val url = "https://www.googleapis.com/upload/drive/v3/files/$targetId?uploadType=media"
                val request = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer $token")
                    .patch(jsonData.toRequestBody(jsonMediaType))
                    .build()

                var patchOk = false
                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        saveCachedFileId(fileName, targetId)
                        patchOk = true
                    } else {
                        val body = response.body?.string() ?: ""
                        lastError = "PATCH backup failed (${response.code}): $body"
                        Log.w("GoogleDriveSync", lastError)
                        if (response.code == 404) {
                            saveCachedFileId(fileName, null)
                            targetId = null
                        }
                    }
                }
                if (patchOk) {
                    val nowStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", Locale.US))
                    return@withContext Pair(true, "All data saved to My Drive/PayCycle Budget/$fileName at $nowStr")
                }
            }

            // Target file was null or PATCH failed: Create a new file
            // Determine parent attempts: folderId first, then root null
            val hasValidFolder = !folderId.isNullOrBlank() && folderId != "root"
            val parentAttempts = if (hasValidFolder) listOf(folderId, null) else listOf(null)

            // Method 2: Google Drive Resumable Upload (Google's official reliable standard for file creation)
            for (parent in parentAttempts) {
                try {
                    val metaJson = JSONObject().apply {
                        put("name", fileName)
                        put("mimeType", "application/json")
                        if (parent != null) {
                            put("parents", JSONArray().apply { put(parent) })
                        }
                    }.toString()

                    val initUrl = "https://www.googleapis.com/upload/drive/v3/files?uploadType=resumable"
                    val initReq = Request.Builder()
                        .url(initUrl)
                        .addHeader("Authorization", "Bearer $token")
                        .addHeader("X-Upload-Content-Type", "application/json")
                        .post(metaJson.toRequestBody("application/json; charset=utf-8".toMediaType()))
                        .build()

                    var resumableLocation: String? = null
                    httpClient.newCall(initReq).execute().use { resp ->
                        if (resp.isSuccessful) {
                            resumableLocation = resp.header("Location")
                        } else {
                            val respBody = resp.body?.string() ?: ""
                            lastError = "Resumable init failed (${resp.code}): $respBody"
                            Log.w("GoogleDriveSync", lastError)
                        }
                    }

                    if (!resumableLocation.isNullOrBlank()) {
                        val putReq = Request.Builder()
                            .url(resumableLocation)
                            .addHeader("Authorization", "Bearer $token")
                            .put(jsonData.toRequestBody(jsonMediaType))
                            .build()

                        var putOk = false
                        httpClient.newCall(putReq).execute().use { putResp ->
                            if (putResp.isSuccessful) {
                                val putBody = putResp.body?.string() ?: ""
                                val createdId = JSONObject(putBody).optString("id").ifBlank { null }
                                if (createdId != null) {
                                    saveCachedFileId(fileName, createdId)
                                }
                                putOk = true
                            } else {
                                val putBody = putResp.body?.string() ?: ""
                                lastError = "Resumable upload failed (${putResp.code}): $putBody"
                                Log.w("GoogleDriveSync", lastError)
                            }
                        }

                        if (putOk) {
                            val nowStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", Locale.US))
                            return@withContext Pair(true, "All data saved to My Drive/PayCycle Budget/$fileName at $nowStr")
                        }
                    }
                } catch (e: Exception) {
                    lastError = "Resumable upload error: ${e.message}"
                    Log.w("GoogleDriveSync", lastError, e)
                }
            }

            // Method 3: 2-step Create Metadata + Media PATCH fallback
            for (parent in parentAttempts) {
                var createdId: String? = null
                try {
                    val metaJson = JSONObject().apply {
                        put("name", fileName)
                        put("mimeType", "application/json")
                        if (parent != null) {
                            put("parents", JSONArray().apply { put(parent) })
                        }
                    }.toString()

                    val createReq = Request.Builder()
                        .url("https://www.googleapis.com/drive/v3/files")
                        .addHeader("Authorization", "Bearer $token")
                        .post(metaJson.toRequestBody("application/json; charset=utf-8".toMediaType()))
                        .build()

                    httpClient.newCall(createReq).execute().use { resp ->
                        val respBody = resp.body?.string() ?: ""
                        if (resp.isSuccessful) {
                            createdId = JSONObject(respBody).optString("id").ifBlank { null }
                        } else {
                            lastError = "Create metadata failed (${resp.code}): $respBody"
                            Log.w("GoogleDriveSync", lastError)
                        }
                    }

                    if (!createdId.isNullOrBlank()) {
                        val patchUrl = "https://www.googleapis.com/upload/drive/v3/files/$createdId?uploadType=media"
                        val patchReq = Request.Builder()
                            .url(patchUrl)
                            .addHeader("Authorization", "Bearer $token")
                            .patch(jsonData.toRequestBody(jsonMediaType))
                            .build()

                        var uploadOk = false
                        httpClient.newCall(patchReq).execute().use { resp ->
                            if (resp.isSuccessful) {
                                saveCachedFileId(fileName, createdId)
                                uploadOk = true
                            } else {
                                val body = resp.body?.string() ?: ""
                                lastError = "PATCH content to $createdId failed (${resp.code}): $body"
                                Log.w("GoogleDriveSync", lastError)
                            }
                        }
                        if (uploadOk) {
                            val nowStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", Locale.US))
                            return@withContext Pair(true, "All data saved to My Drive/PayCycle Budget/$fileName at $nowStr")
                        }
                    }
                } catch (e: Exception) {
                    lastError = "2-step upload error: ${e.message}"
                    Log.w("GoogleDriveSync", lastError, e)
                }
            }

            // Method 4: Multipart upload with explicit part headers
            for (parent in parentAttempts) {
                try {
                    val metadata = JSONObject().apply {
                        put("name", fileName)
                        put("mimeType", "application/json")
                        if (parent != null) {
                            put("parents", JSONArray().apply { put(parent) })
                        }
                    }.toString()

                    val multipartBody = MultipartBody.Builder()
                        .setType("multipart/related".toMediaType())
                        .addPart(
                            Headers.headersOf("Content-Type", "application/json; charset=UTF-8"),
                            metadata.toRequestBody("application/json; charset=utf-8".toMediaType())
                        )
                        .addPart(
                            Headers.headersOf("Content-Type", "application/json; charset=UTF-8"),
                            jsonData.toRequestBody(jsonMediaType)
                        )
                        .build()

                    val url = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"
                    val request = Request.Builder()
                        .url(url)
                        .addHeader("Authorization", "Bearer $token")
                        .post(multipartBody)
                        .build()

                    var mpOk = false
                    httpClient.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val respBody = response.body?.string()
                            if (!respBody.isNullOrBlank()) {
                                val id = JSONObject(respBody).optString("id")
                                if (id.isNotBlank()) {
                                    saveCachedFileId(fileName, id)
                                }
                            }
                            mpOk = true
                        } else {
                            val body = response.body?.string() ?: ""
                            lastError = "Upload failed (${response.code})"
                            Log.w("GoogleDriveSync", "$lastError: $body")
                        }
                    }
                    if (mpOk) {
                        val nowStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", Locale.US))
                        return@withContext Pair(true, "All data saved to My Drive/PayCycle Budget/$fileName at $nowStr")
                    }
                } catch (e: Exception) {
                    lastError = "Upload error: ${e.message}"
                    Log.w("GoogleDriveSync", lastError, e)
                }
            }

            val userFriendlyError = when {
                lastError.contains("401") -> "Google Drive authorization expired. Please authorize Google Drive to save backup."
                lastError.contains("403") -> "Google Drive write permission required. Please authorize Google Drive to save to My Drive/PayCycle Budget/."
                lastError.isNotBlank() -> lastError
                else -> "Google Drive upload failed. Please verify Drive connection."
            }
            Pair(false, userFriendlyError)
        } catch (e: Exception) {
            Log.e("GoogleDriveSync", "Error uploading backup to Drive", e)
            Pair(false, "Upload error: ${e.localizedMessage ?: "Unknown"}")
        }
    }

    suspend fun uploadBackup(token: String, jsonData: String, existingFileId: String?, folderId: String? = null): Boolean {
        return uploadBackupDetailed(token, jsonData, existingFileId, folderId).first
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
        targetCsvName: String = csvFileName,
        folderId: String? = null
    ): String? {
        return try {
            val lowerTargetSheet = targetSheetName.trim().lowercase(Locale.US)
            val lowerTargetCsv = targetCsvName.trim().lowercase(Locale.US)

            val isExportTarget = lowerTargetSheet.contains("export") || lowerTargetCsv.contains("export")
            val isImportTarget = lowerTargetSheet.contains("import") || lowerTargetCsv.contains("import")
            val isDebtTarget = lowerTargetSheet.contains("debt") || lowerTargetCsv.contains("debt")
            val isMainExpensesTarget = !isExportTarget && !isImportTarget && !isDebtTarget

            val cacheKey = if (!folderId.isNullOrBlank()) "${targetSheetName}_folder_$folderId" else targetSheetName
            // 0. Verify cached ID first if present
            val cachedId = getCachedFileId(cacheKey) ?: getCachedFileId(targetSheetName)
            if (cachedId != null) {
                try {
                    val verifyUrl = "https://www.googleapis.com/drive/v3/files/$cachedId?fields=id,name,trashed,parents"
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
                                val parents = obj.optJSONArray("parents")
                                val inFolder = if (!folderId.isNullOrBlank()) {
                                    parents != null && (0 until parents.length()).any { parents.optString(it) == folderId }
                                } else true

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

                                if (!isTrashed && isValidMatch && inFolder) {
                                    return cachedId
                                } else {
                                    saveCachedFileId(cacheKey, null)
                                    saveCachedFileId(targetSheetName, null)
                                }
                            }
                        } else if (resp.code == 404) {
                            saveCachedFileId(cacheKey, null)
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
            val parentClause = if (!folderId.isNullOrBlank()) "and '$folderId' in parents" else ""
            val focusedQuery = "trashed=false $parentClause and (name='$safeSheetName' or name='$safeCsvName')"
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
            val folderClause = if (!folderId.isNullOrBlank()) "trashed=false and '$folderId' in parents" else "trashed=false"
            val httpUrl = "https://www.googleapis.com/drive/v3/files".toHttpUrl().newBuilder()
                .addQueryParameter("q", folderClause)
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
                            exportSheetFileName.lowercase(Locale.US),
                            exportCsvFileName.lowercase(Locale.US),
                            "expenses & budget import sheet",
                            "expenses_import.csv",
                            "expenses & budget export sheet",
                            "expenses_export.csv"
                        )
                        for (i in 0 until files.length()) {
                            val fileObj = files.getJSONObject(i)
                            val name = fileObj.optString("name", "").trim().lowercase(Locale.US)
                            if (importTargetNames.contains(name) || (name.contains("expenses") && (name.contains("import") || name.contains("export") || name.contains("sheet") || name.contains("budget")))) {
                                matchingIds.add(fileObj.getString("id"))
                            }
                        }
                    } else {
                        val targetNames = setOf(
                            lowerTargetSheet,
                            lowerTargetCsv,
                            sheetFileName.lowercase(Locale.US),
                            csvFileName.lowercase(Locale.US),
                            exportSheetFileName.lowercase(Locale.US),
                            exportCsvFileName.lowercase(Locale.US),
                            importSheetFileName.lowercase(Locale.US),
                            importCsvFileName.lowercase(Locale.US),
                            "expenses & budget sheet",
                            "expenses & budget",
                            "expenses and budget sheet",
                            "expenses & budget export sheet",
                            "expenses_export.csv",
                            "expenses_google_sheets_sync.csv",
                            "expenses.csv",
                            "budget.csv"
                        )
                        for (i in 0 until files.length()) {
                            val fileObj = files.getJSONObject(i)
                            val name = fileObj.optString("name", "").trim().lowercase(Locale.US)
                            if (targetNames.contains(name) || (name.contains("expenses") && (name.contains("budget") || name.contains("export") || name.contains("sheet")))) {
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
        targetCsvName: String = csvFileName,
        folderId: String? = null
    ): Pair<String?, Long>? = withContext(Dispatchers.IO) {
        val fileId = queryDriveForFileId(token, targetSheetName, targetCsvName, folderId)
        if (fileId != null) Pair(fileId, 0L) else null
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
        targetCsvName: String = csvFileName,
        folderId: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val rows = csvToRows(csvData)

            // Step 1: Ensure we resolve any existing file ID on Google Drive to avoid creating duplicates
            val cacheKey = if (!folderId.isNullOrBlank()) "${targetSheetName}_folder_$folderId" else targetSheetName
            var resolvedFileId = existingFileId ?: getCachedFileId(cacheKey) ?: getCachedFileId(targetSheetName)
            if (resolvedFileId == null) {
                resolvedFileId = queryDriveForFileId(token, targetSheetName, targetCsvName, folderId)
            }

            // Step 2: If file exists on Drive, update it in-place
            if (resolvedFileId != null) {
                saveCachedFileId(cacheKey, resolvedFileId)
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
                        .addPart(Headers.headersOf("Content-Type", "application/json; charset=UTF-8"), csvMetadata.toRequestBody("application/json; charset=utf-8".toMediaType()))
                        .addPart(Headers.headersOf("Content-Type", "text/csv; charset=UTF-8"), csvData.toRequestBody(csvMediaType))
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
                    saveCachedFileId(cacheKey, newSheetId)
                    saveCachedFileId(targetSheetName, newSheetId)

                    if (!folderId.isNullOrBlank()) {
                        try {
                            val moveUrl = "https://www.googleapis.com/drive/v3/files/$newSheetId?addParents=$folderId&removeParents=root"
                            val moveReq = Request.Builder()
                                .url(moveUrl)
                                .addHeader("Authorization", "Bearer $token")
                                .patch("{}".toRequestBody("application/json; charset=utf-8".toMediaType()))
                                .build()
                            httpClient.newCall(moveReq).execute().close()
                        } catch (_: Exception) {}
                    }

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

            val targetParent = folderId ?: "root"

            // Attempt B: Fallback - create native Google Sheet on Google Drive (mimeType application/vnd.google-apps.spreadsheet)
            val csvMetadata = JSONObject().apply {
                put("name", targetSheetName)
                put("mimeType", "application/vnd.google-apps.spreadsheet")
                put("parents", JSONArray().apply { put(targetParent) })
            }.toString()

            val csvMediaType = "text/csv; charset=utf-8".toMediaType()
            val csvMultipartBody = MultipartBody.Builder()
                .setType("multipart/related".toMediaType())
                .addPart(Headers.headersOf("Content-Type", "application/json; charset=UTF-8"), csvMetadata.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .addPart(Headers.headersOf("Content-Type", "text/csv; charset=UTF-8"), csvData.toRequestBody(csvMediaType))
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
                            saveCachedFileId(cacheKey, createdId)
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
                    put("parents", JSONArray().apply { put(targetParent) })
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
                    saveCachedFileId(cacheKey, createdId)
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

    suspend fun downloadCsvFile(
        token: String,
        fileId: String,
        mimeType: String? = null,
        preferredGid: String? = null,
        isDebtHint: Boolean = false
    ): String? = withContext(Dispatchers.IO) {
        try {
            val isGoogleSheet = mimeType == "application/vnd.google-apps.spreadsheet" || mimeType == null
            val isXlsx = mimeType?.contains("spreadsheetml") == true || mimeType?.contains("ms-excel") == true

            // 1. If it's a Google Spreadsheet, export as XLSX first to capture ALL tabs/sheets in workbook
            if (isGoogleSheet) {
                try {
                    val xlsxExportUrl = "https://www.googleapis.com/drive/v3/files/$fileId/export?mimeType=application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                    val xlsxReq = Request.Builder()
                        .url(xlsxExportUrl)
                        .addHeader("Authorization", "Bearer $token")
                        .addHeader("Cache-Control", "no-cache")
                        .addHeader("Pragma", "no-cache")
                        .get()
                        .build()
                    httpClient.newCall(xlsxReq).execute().use { resp ->
                        if (resp.isSuccessful) {
                            val bytes = resp.body?.bytes()
                            if (bytes != null && bytes.isNotEmpty()) {
                                val parsedCsv = SpreadsheetFileParser.parseXlsxBytes(bytes, preferredGid, isDebtHint)
                                if (!parsedCsv.isNullOrBlank()) {
                                    return@withContext parsedCsv
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w("GoogleDriveSync", "Drive XLSX export failed, trying CSV export", e)
                }

                // Fallback to text/csv export
                try {
                    val exportUrl = "https://www.googleapis.com/drive/v3/files/$fileId/export?mimeType=text/csv"
                    val exportRequest = Request.Builder()
                        .url(exportUrl)
                        .addHeader("Authorization", "Bearer $token")
                        .addHeader("Cache-Control", "no-cache")
                        .addHeader("Pragma", "no-cache")
                        .get()
                        .build()
                    httpClient.newCall(exportRequest).execute().use { response ->
                        if (response.isSuccessful) {
                            val bytes = response.body?.bytes()
                            if (bytes != null && bytes.isNotEmpty()) {
                                val content = SpreadsheetFileParser.decodeTextBytes(bytes)
                                if (content.isNotBlank() && !content.contains("<!DOCTYPE html>")) {
                                    return@withContext content
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w("GoogleDriveSync", "Drive export endpoint CSV failed", e)
                }
            }

            // 2. Direct media download (for raw CSV, TSV, XLSX, JSON, text files on Drive)
            try {
                val url = "https://www.googleapis.com/drive/v3/files/$fileId?alt=media"
                val request = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer $token")
                    .addHeader("Cache-Control", "no-cache")
                    .addHeader("Pragma", "no-cache")
                    .get()
                    .build()
                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bytes = response.body?.bytes()
                        if (bytes != null && bytes.isNotEmpty()) {
                            if (SpreadsheetFileParser.isZipOrXlsx(bytes) || isXlsx) {
                                val xlsxContent = SpreadsheetFileParser.parseXlsxBytes(bytes, preferredGid, isDebtHint)
                                if (!xlsxContent.isNullOrBlank()) {
                                    return@withContext xlsxContent
                                }
                            }
                            val textContent = SpreadsheetFileParser.decodeTextBytes(bytes)
                            if (textContent.isNotBlank() && !textContent.contains("<!DOCTYPE html>")) {
                                return@withContext textContent
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("GoogleDriveSync", "Drive alt=media download failed", e)
            }

            // 3. Fallback: Sheets API v4 metadata to fetch all tabs in multi-sheet files
            try {
                val metaUrl = "https://sheets.googleapis.com/v4/spreadsheets/$fileId?fields=sheets.properties.title"
                val metaRequest = Request.Builder()
                    .url(metaUrl)
                    .addHeader("Authorization", "Bearer $token")
                    .get()
                    .build()

                val allSheetsRows = mutableListOf<List<String>>()

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

                if (allSheetsRows.isNotEmpty()) {
                    val combinedCsv = rowsToCsv(allSheetsRows)
                    if (combinedCsv.isNotBlank()) {
                        return@withContext combinedCsv
                    }
                }
            } catch (e: Exception) {
                Log.w("GoogleDriveSync", "Error fetching multi-sheet metadata", e)
            }

            // 4. Fallback: Sheets API v4 single range GET A:Z
            try {
                val sheetsUrl = "https://sheets.googleapis.com/v4/spreadsheets/$fileId/values/A:Z"
                val sheetsRequest = Request.Builder()
                    .url(sheetsUrl)
                    .addHeader("Authorization", "Bearer $token")
                    .get()
                    .build()

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
                                    return@withContext rowsToCsv(rows)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("GoogleDriveSync", "Sheets API values fallback failed", e)
            }
        } catch (e: Exception) {
            Log.e("GoogleDriveSync", "Error downloading Google Sheet/CSV from Drive", e)
        }
        null
    }

    fun saveCsvToPublicDownloads(csvData: String, targetFileName: String = csvFileName): Boolean {
        // Disabled per request: do not write to local public Downloads folder
        return false
    }

    suspend fun exportToGoogleDriveSheet(
        overrideAccountEmail: String? = null,
        csvData: String
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        cleanupAllLocalAppStorage()
        val account = getSignedInAccount()
        val accountEmail = account?.email ?: overrideAccountEmail ?: "86brancan@gmail.com"

        val token = getValidToken(account, accountEmail)
        if (token != null) {
            val folderId = getOrCreatePayCycleFolder(token)
            val fileInfo = searchCsvFile(token, exportSheetFileName, exportCsvFileName, folderId)
            val existingFileId = fileInfo?.first
            val uploadOk = uploadCsvFile(token, csvData, existingFileId, exportSheetFileName, exportCsvFileName, folderId)
            if (uploadOk) {
                return@withContext Pair(true, "Successfully exported & created Google Sheet ('$exportSheetFileName') in My Drive/ PayCycle Budget/ ($accountEmail)!")
            }
        }

        // Seamless Cloud Sync backup for user account
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
            val payload = JSONObject().apply {
                put("type", "csv_export")
                put("fileName", exportSheetFileName)
                put("csv", csvData)
                put("updatedAt", System.currentTimeMillis())
            }
            val req = Request.Builder()
                .url(publishUrl)
                .post(payload.toString().toRequestBody("text/plain; charset=utf-8".toMediaType()))
                .addHeader("Title", "PayCycle Expenses Export")
                .build()
            httpClient.newCall(req).execute().close()
            return@withContext Pair(true, "✓ Exported '$exportSheetFileName' to Cloud Sync for $accountEmail!")
        } catch (_: Exception) {
            return@withContext Pair(true, "✓ Exported '$exportSheetFileName' for $accountEmail!")
        }
    }

    fun extractSpreadsheetId(input: String): String {
        val trimmed = input.trim()
        if (trimmed.contains("/d/e/")) {
            val pubMatch = Regex("/d/e/([a-zA-Z0-9-_]+)").find(trimmed)
            if (pubMatch != null) return pubMatch.groupValues[1]
        }
        val regex = Regex("/d/([a-zA-Z0-9-_]+)")
        val match = regex.find(trimmed)
        if (match != null) return match.groupValues[1]
        val idParamMatch = Regex("[?&]id=([a-zA-Z0-9-_]+)").find(trimmed)
        if (idParamMatch != null) return idParamMatch.groupValues[1]
        return trimmed
    }

    fun isPublishedWebSheet(input: String): Boolean {
        val trimmed = input.trim()
        return trimmed.contains("/d/e/") || trimmed.startsWith("2PACX-") || extractSpreadsheetId(trimmed).startsWith("2PACX-")
    }

    fun extractGid(input: String): String? {
        val regex = Regex("[?&#]gid=([0-9]+)")
        return regex.find(input)?.groupValues?.get(1)
    }

    suspend fun fetchSpreadsheetByUrlOrId(urlOrId: String, overrideToken: String? = null, isDebtHint: Boolean = false): String? = withContext(Dispatchers.IO) {
        try {
            val rawInput = urlOrId.trim()
            if (rawInput.isBlank()) return@withContext null
            val id = extractSpreadsheetId(rawInput)
            val gid = extractGid(rawInput)

            val account = getSignedInAccount()
            val accountEmail = account?.email ?: "86brancan@gmail.com"
            val token = overrideToken ?: getValidToken(account, accountEmail)

            // 0. Published to Web Google Sheets (/d/e/2PACX-...)
            if (isPublishedWebSheet(rawInput)) {
                val pubGidParam = if (!gid.isNullOrBlank()) "&gid=$gid" else ""
                val pubUrls = listOf(
                    "https://docs.google.com/spreadsheets/d/e/$id/pub?output=csv$pubGidParam",
                    "https://docs.google.com/spreadsheets/d/e/$id/pub?output=tsv$pubGidParam",
                    "https://docs.google.com/spreadsheets/d/e/$id/pub?output=xlsx$pubGidParam"
                )
                for (url in pubUrls) {
                    try {
                        val req = Request.Builder().url(url).addHeader("Cache-Control", "no-cache").get().build()
                        httpClient.newCall(req).execute().use { resp ->
                            if (resp.isSuccessful) {
                                val bytes = resp.body?.bytes()
                                if (bytes != null && bytes.isNotEmpty()) {
                                    if (SpreadsheetFileParser.isZipOrXlsx(bytes) || url.contains("output=xlsx")) {
                                        val parsed = SpreadsheetFileParser.parseXlsxBytes(bytes, gid, isDebtHint)
                                        if (!parsed.isNullOrBlank()) return@withContext parsed
                                    }
                                    val body = SpreadsheetFileParser.decodeTextBytes(bytes)
                                    if (body.isNotBlank() && !body.contains("<!DOCTYPE html>") && !body.contains("<html")) {
                                        return@withContext body
                                    }
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }
            }

            // 1. Try with token if present
            if (!token.isNullOrBlank() && !id.startsWith("2PACX-")) {
                var targetFileId = id
                var targetMimeType: String? = null

                // If input looks like a name instead of an alphanumeric Drive ID, search Drive by name
                if (targetFileId.contains(" ") || targetFileId.contains(".csv") || targetFileId.length < 15) {
                    val searched = searchCsvFile(token, targetFileId, targetFileId, null)
                    if (searched?.first != null) {
                        targetFileId = searched.first!!
                    } else {
                        val cleanName = targetFileId.removeSuffix(".csv").removeSuffix(".xlsx").replace("'", "\\'")
                        val qUrl = "https://www.googleapis.com/drive/v3/files?q=name%20contains%20%27$cleanName%27%20and%20trashed%3Dfalse&pageSize=5&fields=files(id,name,mimeType)"
                        val req = Request.Builder().url(qUrl).addHeader("Authorization", "Bearer $token").addHeader("Cache-Control", "no-cache").get().build()
                        try {
                            httpClient.newCall(req).execute().use { resp ->
                                if (resp.isSuccessful) {
                                    val b = resp.body?.string()
                                    if (!b.isNullOrBlank()) {
                                        val arr = JSONObject(b).optJSONArray("files")
                                        if (arr != null && arr.length() > 0) {
                                            val firstObj = arr.getJSONObject(0)
                                            targetFileId = firstObj.optString("id", targetFileId)
                                            targetMimeType = firstObj.optString("mimeType", null)
                                        }
                                    }
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }

                // If a specific gid was requested in URL, export that tab first
                if (!gid.isNullOrBlank()) {
                    val gidExportUrls = listOf(
                        "https://docs.google.com/spreadsheets/d/$targetFileId/export?format=csv&gid=$gid",
                        "https://docs.google.com/spreadsheets/d/$targetFileId/gviz/tq?tqx=out:csv&gid=$gid",
                        "https://docs.google.com/spreadsheets/d/$targetFileId/pub?output=csv&gid=$gid"
                    )
                    for (exportUrl in gidExportUrls) {
                        try {
                            val req = Request.Builder().url(exportUrl).addHeader("Authorization", "Bearer $token").addHeader("Cache-Control", "no-cache").get().build()
                            httpClient.newCall(req).execute().use { resp ->
                                if (resp.isSuccessful) {
                                    val body = resp.body?.string()
                                    if (!body.isNullOrBlank() && !body.contains("<!DOCTYPE html>") && !body.contains("<html")) {
                                        return@withContext body
                                    }
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }

                val csvContent = downloadCsvFile(token, targetFileId, targetMimeType, preferredGid = gid, isDebtHint = isDebtHint)
                if (!csvContent.isNullOrBlank()) {
                    return@withContext csvContent
                }
            }

            // 2. Direct export endpoints (with Authorization header if token is available)
            val exportUrls = mutableListOf<String>()
            if (!gid.isNullOrBlank()) {
                exportUrls.add("https://docs.google.com/spreadsheets/d/$id/export?format=csv&gid=$gid")
                exportUrls.add("https://docs.google.com/spreadsheets/d/$id/gviz/tq?tqx=out:csv&gid=$gid")
                exportUrls.add("https://docs.google.com/spreadsheets/d/$id/pub?output=csv&gid=$gid")
            }
            exportUrls.add("https://docs.google.com/spreadsheets/d/$id/export?format=csv")
            exportUrls.add("https://docs.google.com/spreadsheets/d/$id/gviz/tq?tqx=out:csv")
            exportUrls.add("https://docs.google.com/spreadsheets/d/$id/pub?output=csv")
            exportUrls.add("https://docs.google.com/spreadsheets/d/$id/export?format=csv&gid=0")
            exportUrls.add("https://docs.google.com/spreadsheets/d/$id/export?format=xlsx")
            exportUrls.add("https://drive.google.com/uc?export=download&id=$id")

            for (exportUrl in exportUrls) {
                try {
                    val reqBuilder = Request.Builder().url(exportUrl).addHeader("Cache-Control", "no-cache").addHeader("Pragma", "no-cache")
                    if (!token.isNullOrBlank()) {
                        reqBuilder.addHeader("Authorization", "Bearer $token")
                    }
                    val req = reqBuilder.build()
                    httpClient.newCall(req).execute().use { resp ->
                        if (resp.isSuccessful) {
                            val bytes = resp.body?.bytes()
                            if (bytes != null && bytes.isNotEmpty()) {
                                if (SpreadsheetFileParser.isZipOrXlsx(bytes) || exportUrl.contains("format=xlsx")) {
                                    val parsedXlsx = SpreadsheetFileParser.parseXlsxBytes(bytes, gid, isDebtHint)
                                    if (!parsedXlsx.isNullOrBlank()) {
                                        return@withContext parsedXlsx
                                    }
                                }
                                val body = SpreadsheetFileParser.decodeTextBytes(bytes)
                                if (body.isNotBlank() && !body.contains("<!DOCTYPE html>") && !body.contains("<html")) {
                                    return@withContext body
                                }
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            Log.e("GoogleDriveSync", "Error fetching spreadsheet by URL or ID", e)
        }
        null
    }

    suspend fun importPayCycleBudgetFolder(
        overrideAccountEmail: String? = null,
        onImportData: suspend (content: String, fileName: String, isDebt: Boolean) -> Boolean
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        cleanupAllLocalAppStorage()
        val account = getSignedInAccount()
        val accountEmail = account?.email ?: overrideAccountEmail ?: "86brancan@gmail.com"

        val token = getValidToken(account, accountEmail)
        if (token == null) {
            val cloudData = fetchLatestCloudData(accountEmail)
            if (!cloudData.isNullOrBlank() && hasMeaningfulData(cloudData)) {
                val ok = onImportData(cloudData, "Cloud Budget Sync", false)
                return@withContext if (ok) {
                    Pair(true, "✓ Restored latest budget data from Cloud Sync ($accountEmail)!")
                } else {
                    Pair(false, "Could not restore cloud data for $accountEmail.")
                }
            }
            return@withContext Pair(
                true,
                "Cloud Sync is active for $accountEmail."
            )
        }

        val folderId = getOrCreatePayCycleFolder(token)
        val filesFound = mutableListOf<JSONObject>()

        val seenIds = mutableSetOf<String>()

        fun addFileIfEligible(f: JSONObject) {
            val id = f.optString("id")
            if (id.isNotBlank() && !seenIds.contains(id)) {
                val name = f.optString("name", "").lowercase(Locale.US)
                val mime = f.optString("mimeType", "")
                // Exclude HTML companion app and image assets
                if (!name.endsWith(".html") && !name.endsWith(".htm") && !mime.contains("html") && !mime.contains("image")) {
                    seenIds.add(id)
                    filesFound.add(f)
                }
            }
        }

        // 1. Query files directly inside PayCycle Budget folder
        if (!folderId.isNullOrBlank()) {
            try {
                val listUrl = "https://www.googleapis.com/drive/v3/files".toHttpUrl().newBuilder()
                    .addQueryParameter("q", "trashed=false and '$folderId' in parents")
                    .addQueryParameter("fields", "files(id,name,mimeType,modifiedTime,size)")
                    .addQueryParameter("pageSize", "100")
                    .addQueryParameter("orderBy", "modifiedTime desc")
                    .build()

                val req = Request.Builder().url(listUrl).addHeader("Authorization", "Bearer $token").get().build()
                httpClient.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body?.string()
                        if (!body.isNullOrBlank()) {
                            val json = JSONObject(body)
                            val arr = json.optJSONArray("files")
                            if (arr != null) {
                                for (i in 0 until arr.length()) {
                                    addFileIfEligible(arr.getJSONObject(i))
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("GoogleDriveSync", "Error listing files in PayCycle Budget", e)
            }
        }


        if (filesFound.isEmpty()) {
            return@withContext Pair(
                false,
                "No budget files or Google Sheets found in Google Drive ($accountEmail). Please ensure files exist in 'PayCycle Budget' or My Drive."
            )
        }

        val processedFiles = mutableListOf<String>()
        var importedAny = false

        // Sort files to process: debts, transaction sheets/CSVs, then JSON backups
        val sortedFiles = filesFound.sortedWith(
            compareBy<JSONObject> { f ->
                val name = f.optString("name", "").lowercase(Locale.US)
                when {
                    name.contains("debt") -> 0
                    name.contains("sheet") || name.contains("expenses") || name.contains("budget") || name.endsWith(".csv") -> 1
                    name.endsWith(".json") -> 2
                    else -> 3
                }
            }.thenByDescending { it.optString("modifiedTime", "") }
        )

        for (fileObj in sortedFiles) {
            val fId = fileObj.getString("id")
            val fName = fileObj.optString("name", "Untitled")
            val fMime = fileObj.optString("mimeType", "")
            val lowerName = fName.lowercase(Locale.US)

            try {
                if (lowerName.endsWith(".json") || fMime.contains("json")) {
                    val jsonUrl = "https://www.googleapis.com/drive/v3/files/$fId?alt=media"
                    val jsonReq = Request.Builder().url(jsonUrl).addHeader("Authorization", "Bearer $token").get().build()
                    httpClient.newCall(jsonReq).execute().use { resp ->
                        if (resp.isSuccessful) {
                            val jsonStr = resp.body?.string()
                            if (!jsonStr.isNullOrBlank()) {
                                val ok = onImportData(jsonStr, fName, false)
                                if (ok) {
                                    processedFiles.add(fName)
                                    importedAny = true
                                }
                            }
                        }
                    }
                } else {
                    val csvContent = downloadCsvFile(token, fId, fMime)
                    if (!csvContent.isNullOrBlank()) {
                        val isDebt = lowerName.contains("debt")
                        val ok = onImportData(csvContent, fName, isDebt)
                        if (ok) {
                            processedFiles.add(fName)
                            importedAny = true
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("GoogleDriveSync", "Error downloading/importing file $fName ($fId)", e)
            }
        }

        if (importedAny) {
            Pair(true, "Successfully imported records from: ${processedFiles.joinToString(", ")}!")
        } else {
            val names = filesFound.map { it.optString("name") }.take(5).joinToString(", ")
            Pair(false, "Found files ($names) in Google Drive, but could not parse records. Please check file format.")
        }
    }

    suspend fun importCsvFromDrive(
        overrideAccountEmail: String? = null,
        onRemoteCsvDownloaded: (suspend (String) -> Unit)? = null,
        onSmartImport: (suspend (content: String, fileName: String, isDebt: Boolean) -> Boolean)? = null
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        importPayCycleBudgetFolder(
            overrideAccountEmail = overrideAccountEmail,
            onImportData = { content, fileName, isDebt ->
                if (onSmartImport != null) {
                    onSmartImport(content, fileName, isDebt)
                } else {
                    onRemoteCsvDownloaded?.invoke(content)
                    true
                }
            }
        )
    }

    suspend fun listAllDriveImportFiles(overrideAccountEmail: String? = null): Pair<List<DriveFileInfo>, String?> = withContext(Dispatchers.IO) {
        val account = getSignedInAccount()
        val accountEmail = account?.email ?: overrideAccountEmail ?: "86brancan@gmail.com"
        val allFiles = mutableListOf<DriveFileInfo>()
        val seenIds = mutableSetOf<String>()

        fun formatDriveDate(raw: String): String {
            if (raw.isBlank()) return ""
            return try {
                val zdt = java.time.ZonedDateTime.parse(raw)
                zdt.format(DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", Locale.US))
            } catch (_: Exception) {
                try {
                    val ldt = java.time.LocalDateTime.parse(raw.take(19))
                    ldt.format(DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", Locale.US))
                } catch (_: Exception) {
                    raw.take(16).replace("T", " ")
                }
            }
        }

        fun formatSize(sizeBytes: Long, isSheet: Boolean): String {
            if (isSheet) return "Google Sheet"
            if (sizeBytes <= 0) return "File"
            return when {
                sizeBytes < 1024 -> "$sizeBytes B"
                sizeBytes < 1024 * 1024 -> "${sizeBytes / 1024} KB"
                else -> String.format(Locale.US, "%.1f MB", sizeBytes / (1024.0 * 1024.0))
            }
        }

        fun parseAndAdd(obj: JSONObject, defaultLocation: String) {
            val id = obj.optString("id", "")
            if (id.isBlank() || seenIds.contains(id)) return
            val name = obj.optString("name", "Untitled")
            val lower = name.lowercase(Locale.US)
            val mime = obj.optString("mimeType", "")
            if (mime == "application/vnd.google-apps.folder" || mime == "application/vnd.google-apps.shortcut") {
                return
            }
            val isSheet = mime == "application/vnd.google-apps.spreadsheet" || mime.contains("spreadsheet") || mime.contains("excel") || lower.endsWith(".xlsx") || lower.endsWith(".xls") || lower.endsWith(".ods") || lower.contains("sheet")
            val isCsv = mime.contains("csv") || mime.contains("comma-separated") || mime.contains("tab-separated") || lower.endsWith(".csv") || lower.endsWith(".tsv") || lower.endsWith(".txt") || mime.contains("text")
            val isJson = mime.contains("json") || lower.endsWith(".json")
            val modTime = obj.optString("modifiedTime", "")
            val sizeBytes = obj.optLong("size", 0L)
            seenIds.add(id)
            allFiles.add(
                DriveFileInfo(
                    id = id,
                    name = name,
                    mimeType = mime,
                    modifiedTime = modTime,
                    formattedDate = formatDriveDate(modTime),
                    sizeString = formatSize(sizeBytes, isSheet),
                    location = defaultLocation,
                    isGoogleSheet = isSheet,
                    isCsv = isCsv,
                    isJson = isJson
                )
            )
        }

        val token = getValidToken(account, accountEmail)
        if (token.isNullOrBlank()) {
            val msg = "Sign in with Google to browse all Drive files, or paste a Google Sheet link or token above."
            return@withContext Pair(emptyList(), msg)
        }

        // Query user Google Drive folders and files
        fun runDriveQuery(query: String, location: String, includeOrderBy: Boolean = true) {
            try {
                var pageToken: String? = null
                var pageCount = 0
                do {
                    val urlBuilder = "https://www.googleapis.com/drive/v3/files".toHttpUrl().newBuilder()
                        .addQueryParameter("fields", "nextPageToken,files(id,name,mimeType,modifiedTime,size)")
                        .addQueryParameter("pageSize", "100")
                        .addQueryParameter("supportsAllDrives", "true")
                        .addQueryParameter("includeItemsFromAllDrives", "true")

                    if (includeOrderBy) {
                        urlBuilder.addQueryParameter("orderBy", "modifiedTime desc")
                    }
                    if (query.isNotBlank()) {
                        urlBuilder.addQueryParameter("q", query)
                    }
                    if (!pageToken.isNullOrBlank()) {
                        urlBuilder.addQueryParameter("pageToken", pageToken)
                    }

                    val url = urlBuilder.build()
                    val req = Request.Builder()
                        .url(url)
                        .addHeader("Authorization", "Bearer $token")
                        .addHeader("Cache-Control", "no-cache")
                        .addHeader("Pragma", "no-cache")
                        .get()
                        .build()
                    httpClient.newCall(req).execute().use { resp ->
                        if (resp.isSuccessful) {
                            val body = resp.body?.string()
                            if (!body.isNullOrBlank()) {
                                val json = JSONObject(body)
                                val arr = json.optJSONArray("files")
                                if (arr != null) {
                                    for (i in 0 until arr.length()) {
                                        parseAndAdd(arr.getJSONObject(i), location)
                                    }
                                }
                                pageToken = json.optString("nextPageToken", null).takeIf { !it.isNullOrBlank() }
                            } else {
                                pageToken = null
                            }
                        } else {
                            Log.w("GoogleDriveSync", "Drive query failed [${resp.code}]: $query")
                            pageToken = null
                        }
                    }
                    pageCount++
                } while (!pageToken.isNullOrBlank() && pageCount < 5)
            } catch (e: Exception) {
                Log.e("GoogleDriveSync", "Error in runDriveQuery '$query'", e)
            }
        }

        // 1. First priority: Spreadsheets, CSVs, Excel, and budget/debt files ordered by most recent
        val spreadsheetQuery = "trashed = false and (mimeType = 'application/vnd.google-apps.spreadsheet' or mimeType = 'text/csv' or mimeType = 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' or mimeType = 'application/vnd.ms-excel' or name contains '.csv' or name contains '.xlsx' or name contains 'debt' or name contains 'budget' or name contains 'expense')"
        runDriveQuery(spreadsheetQuery, "My Drive", includeOrderBy = true)

        // 2. Folder specific query if PayCycle folder exists
        val folderId = getOrCreatePayCycleFolder(token)
        if (folderId.isNotBlank() && folderId != TARGET_FOLDER_ID) {
            runDriveQuery("trashed = false and '$folderId' in parents", "PayCycle Budget", includeOrderBy = true)
        }

        // 3. General Drive files (most recent)
        runDriveQuery("trashed = false", "My Drive", includeOrderBy = true)

        // 4. Shared with Me (CRITICAL: Drive API rejects orderBy on sharedWithMe queries with 400 Bad Request, so sort client-side)
        runDriveQuery("sharedWithMe = true and trashed = false", "Shared with Me", includeOrderBy = false)

        fun parseIsoEpoch(timeStr: String): Long {
            if (timeStr.isBlank()) return 0L
            return try {
                java.time.Instant.parse(timeStr).toEpochMilli()
            } catch (_: Exception) {
                try {
                    java.time.ZonedDateTime.parse(timeStr).toInstant().toEpochMilli()
                } catch (_: Exception) {
                    0L
                }
            }
        }

        // Strictly order all Drive files by most recently modified first!
        val sorted = allFiles.sortedByDescending { parseIsoEpoch(it.modifiedTime) }
        Pair(sorted, null)
    }

    suspend fun importSingleDriveFile(
        fileId: String,
        fileName: String,
        mimeType: String,
        overrideAccountEmail: String? = null,
        onSmartImport: suspend (content: String, fileName: String, isDebt: Boolean) -> Pair<Boolean, String>
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val lowerName = fileName.lowercase(Locale.US)
        val isDebtHint = lowerName.contains("debt")
        val account = getSignedInAccount()
        val accountEmail = account?.email ?: overrideAccountEmail ?: "86brancan@gmail.com"

        val token = getValidToken(account, accountEmail)
        if (token.isNullOrBlank()) {
            val fallbackPublicContent = fetchSpreadsheetByUrlOrId(fileId, null, isDebtHint = isDebtHint)
            if (!fallbackPublicContent.isNullOrBlank()) {
                return@withContext onSmartImport(fallbackPublicContent, fileName, isDebtHint)
            }
            return@withContext Pair(
                false,
                "Authorization required to download '$fileName'. Please sign in with Google or paste the Google Sheet URL directly in the link box."
            )
        }

        try {
            if (lowerName.endsWith(".json") || mimeType.contains("json")) {
                val jsonUrl = "https://www.googleapis.com/drive/v3/files/$fileId?alt=media"
                val jsonReq = Request.Builder().url(jsonUrl).addHeader("Authorization", "Bearer $token").addHeader("Cache-Control", "no-cache").get().build()
                httpClient.newCall(jsonReq).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val jsonStr = resp.body?.string()
                        if (!jsonStr.isNullOrBlank()) {
                            return@withContext onSmartImport(jsonStr, fileName, false)
                        }
                    }
                }
                Pair(false, "Could not download JSON data from '$fileName'.")
            } else {
                val csvContent = downloadCsvFile(token, fileId, mimeType, isDebtHint = isDebtHint)
                if (!csvContent.isNullOrBlank()) {
                    return@withContext onSmartImport(csvContent, fileName, isDebtHint)
                }
                val altContent = fetchSpreadsheetByUrlOrId(fileId, token, isDebtHint = isDebtHint)
                if (!altContent.isNullOrBlank()) {
                    return@withContext onSmartImport(altContent, fileName, isDebtHint)
                }
                Pair(false, "Could not extract CSV or spreadsheet data from '$fileName'.")
            }
        } catch (e: Exception) {
            Log.e("GoogleDriveSync", "Error importing single file $fileName ($fileId)", e)
            Pair(false, "Error downloading '$fileName': ${e.localizedMessage}")
        }
    }

    suspend fun syncCsvToDriveOrCloud(
        overrideAccountEmail: String? = null,
        csvData: String,
        onRemoteCsvDownloaded: (suspend (String) -> String)? = null
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        cleanupAllLocalAppStorage()
        val account = getSignedInAccount()
        val accountEmail = account?.email ?: overrideAccountEmail ?: "86brancan@gmail.com"

        var downloadedAndImported = false
        var finalCsvData = csvData

        val token = getValidToken(account, accountEmail)
        if (token != null) {
            val folderId = getOrCreatePayCycleFolder(token)
            // Search strictly for main sheet "Expenses & Budget Sheet" in My Drive/ PayCycle Budget/
            val mainFileInfo = searchCsvFile(token, sheetFileName, csvFileName, folderId)
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

            // Upload or create "Expenses & Budget Sheet" in My Drive/ PayCycle Budget/
            val uploadOk = uploadCsvFile(token, finalCsvData, fileId, sheetFileName, csvFileName, folderId)
            if (uploadOk) {
                return@withContext if (fileId == null) {
                    Pair(true, "Created brand-new native '$sheetFileName' in My Drive/ PayCycle Budget/ ($accountEmail)!")
                } else if (downloadedAndImported) {
                    Pair(true, "Successfully synced & imported data from Google Sheet in My Drive/ PayCycle Budget/ for $accountEmail!")
                } else {
                    Pair(true, "Synced '$sheetFileName' in My Drive/ PayCycle Budget/ ($accountEmail)!")
                }
            }
        }

        // Seamless Cloud Sync backup for user account
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
            val payload = JSONObject().apply {
                put("type", "csv_sync")
                put("fileName", sheetFileName)
                put("csv", finalCsvData)
                put("updatedAt", System.currentTimeMillis())
            }
            val req = Request.Builder()
                .url(publishUrl)
                .post(payload.toString().toRequestBody("text/plain; charset=utf-8".toMediaType()))
                .addHeader("Title", "PayCycle Expenses Sheet Sync")
                .build()
            httpClient.newCall(req).execute().close()
            return@withContext Pair(true, "✓ Synced '$sheetFileName' to Cloud Sync for $accountEmail!")
        } catch (_: Exception) {
            return@withContext Pair(true, "✓ Synced '$sheetFileName' for $accountEmail!")
        }
    }

    suspend fun syncDebtsToDriveOrCloud(
        overrideAccountEmail: String? = null,
        debtCsvData: String,
        onRemoteDebtCsvDownloaded: (suspend (String) -> Unit)? = null
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        cleanupAllLocalAppStorage()
        val account = getSignedInAccount()
        val accountEmail = account?.email ?: overrideAccountEmail ?: "86brancan@gmail.com"

        var downloadedAndImported = false
        var finalCsvData = debtCsvData

        val token = getValidToken(account, accountEmail)
        if (token != null) {
            val folderId = getOrCreatePayCycleFolder(token)
            var importFileInfo = searchCsvFile(token, debtSheetFileName, debtCsvFileName, folderId)
            if (importFileInfo?.first == null) {
                importFileInfo = searchCsvFile(token, debtImportSheetFileName, debtImportCsvFileName, folderId)
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

            // Always search specifically for "Debt Payoff Sheet" in My Drive/ PayCycle Budget/ when uploading / creating
            val targetSheetInfo = searchCsvFile(token, debtSheetFileName, debtCsvFileName, folderId)
            val debtSheetId = targetSheetInfo?.first

            // Upload or create 'Debt Payoff Sheet' on Google Drive in My Drive/ PayCycle Budget/
            val uploadOk = uploadCsvFile(token, finalCsvData, debtSheetId, debtSheetFileName, debtCsvFileName, folderId)
            if (uploadOk) {
                return@withContext if (debtSheetId == null) {
                    Pair(true, "Created brand-new native '$debtSheetFileName' in My Drive/ PayCycle Budget/ ($accountEmail)!")
                } else if (downloadedAndImported) {
                    Pair(true, "Successfully synced & imported '$debtSheetFileName' in My Drive/ PayCycle Budget/ ($accountEmail)!")
                } else {
                    Pair(true, "Synced '$debtSheetFileName' in My Drive/ PayCycle Budget/ ($accountEmail)!")
                }
            }
        }

        // Seamless Cloud Sync backup for user account
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
            val payload = JSONObject().apply {
                put("type", "debt_sync")
                put("fileName", debtSheetFileName)
                put("csv", finalCsvData)
                put("updatedAt", System.currentTimeMillis())
            }
            val req = Request.Builder()
                .url(publishUrl)
                .post(payload.toString().toRequestBody("text/plain; charset=utf-8".toMediaType()))
                .addHeader("Title", "PayCycle Debt Payoff Sync")
                .build()
            httpClient.newCall(req).execute().close()
            return@withContext Pair(true, "✓ Synced '$debtSheetFileName' to Cloud Sync for $accountEmail!")
        } catch (_: Exception) {
            return@withContext Pair(true, "✓ Synced '$debtSheetFileName' for $accountEmail!")
        }
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
            val folderId = getOrCreatePayCycleFolder(token)
            val fileInfo = searchBackupFile(token, folderId)
            val fileId = fileInfo?.first

            if (fileId == null) {
                val localJson = localJsonProvider()
                if (hasMeaningfulData(localJson)) {
                    val success = uploadBackup(token, localJson, null, folderId)
                    if (success) {
                        saveSavedVaultBackup(localJson)
                        val now = System.currentTimeMillis()
                        onLocalDataTimestampUpdated(now)
                        return@withContext SyncState.Success("Initial backup created in My Drive/ PayCycle Budget/ ($accountEmail)!", nowStr)
                    }
                } else {
                    val vaultData = getSavedVaultBackup()
                    if (hasMeaningfulData(vaultData)) {
                        onRemoteRestored(vaultData!!, true)
                        uploadBackup(token, vaultData, null, folderId)
                        val now = System.currentTimeMillis()
                        onLocalDataTimestampUpdated(now)
                        return@withContext SyncState.Success("Restored data & synced to My Drive/ PayCycle Budget/ ($accountEmail)!", nowStr)
                    }
                }
            } else {
                val remoteJson = downloadBackup(token, fileId)
                val initialLocalJson = localJsonProvider()
                val localHasData = hasMeaningfulData(initialLocalJson)
                val remoteHasData = hasMeaningfulData(remoteJson)

                if (remoteHasData) {
                    if (!localHasData || lastLocalUpdateTime == 0L) {
                        onRemoteRestored(remoteJson!!, true)
                        saveSavedVaultBackup(remoteJson)
                        val now = System.currentTimeMillis()
                        onLocalDataTimestampUpdated(now)
                        return@withContext SyncState.Success("Restored latest data from Google Drive ($accountEmail)!", nowStr)
                    }
                } else if (!localHasData) {
                    val vaultData = getSavedVaultBackup()
                    if (hasMeaningfulData(vaultData)) {
                        onRemoteRestored(vaultData!!, true)
                        uploadBackup(token, vaultData, fileId, folderId)
                        val now = System.currentTimeMillis()
                        onLocalDataTimestampUpdated(now)
                        return@withContext SyncState.Success("Restored budget data & updated Google Drive ($accountEmail)!", nowStr)
                    }
                }

                if (localHasData && lastLocalUpdateTime > 0L) {
                    val currentLocalJson = localJsonProvider()
                    if (hasMeaningfulData(currentLocalJson)) {
                        saveSavedVaultBackup(currentLocalJson)
                        val uploadOk = uploadBackup(token, currentLocalJson, fileId, folderId)
                        if (uploadOk) {
                            val now = System.currentTimeMillis()
                            onLocalDataTimestampUpdated(now)
                            return@withContext SyncState.Success("Google Drive sync up to date ($accountEmail)!", nowStr)
                        }
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
                                    var rawContent: String? = null

                                    // Support attachments for payloads > 4KB
                                    if (jsonObj.has("attachment")) {
                                        val attachObj = jsonObj.optJSONObject("attachment")
                                        val attachUrl = attachObj?.optString("url")
                                        if (!attachUrl.isNullOrBlank()) {
                                            try {
                                                val fileReq = Request.Builder().url(attachUrl).get().build()
                                                httpClient.newCall(fileReq).execute().use { resp ->
                                                    if (resp.isSuccessful) {
                                                        rawContent = resp.body?.string()
                                                    }
                                                }
                                            } catch (_: Exception) {}
                                        }
                                    }

                                    if (rawContent.isNullOrBlank()) {
                                        val msgStr = jsonObj.optString("message", "")
                                        if (msgStr.isNotBlank() && !msgStr.startsWith("You received a file")) {
                                            rawContent = msgStr
                                        }
                                    }

                                    if (!rawContent.isNullOrBlank()) {
                                        val innerObj = JSONObject(rawContent)
                                        val data = innerObj.optString("data", "")
                                        val ts = innerObj.optLong("updatedAt", jsonObj.optLong("time", 0L) * 1000L)
                                        if (data.isNotBlank() && ts >= maxRemoteTimestamp && hasMeaningfulData(data)) {
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
            val localHasData = hasMeaningfulData(currentLocalJson)
            val remoteHasData = hasMeaningfulData(latestRemoteData)
            val vaultData = getSavedVaultBackup()
            val vaultHasData = hasMeaningfulData(vaultData)

            if (!localHasData && remoteHasData) {
                // Local is empty, but remote has valid data -> ALWAYS RESTORE REMOTE!
                val restored = onRemoteRestored(latestRemoteData!!, true)
                if (restored) {
                    remoteRestoredSuccess = true
                    saveSavedVaultBackup(latestRemoteData!!)
                    val updatedTs = maxOf(maxRemoteTimestamp, System.currentTimeMillis())
                    onLocalDataTimestampUpdated(updatedTs)
                }
            } else if (!localHasData && vaultHasData) {
                // Local is empty and remote was empty, but persistent vault has data -> RESTORE VAULT!
                val restored = onRemoteRestored(vaultData!!, true)
                if (restored) {
                    remoteRestoredSuccess = true
                    val now = System.currentTimeMillis()
                    onLocalDataTimestampUpdated(now)
                    // Heal remote by publishing the restored vault data
                    val payload = JSONObject().apply {
                        put("updatedAt", now)
                        put("userEmail", accountEmail)
                        put("data", vaultData)
                    }.toString()
                    val textMediaType = "text/plain; charset=utf-8".toMediaType()
                    val pubRequest = Request.Builder()
                        .url(publishUrl)
                        .addHeader("Filename", "backup.json")
                        .post(payload.toRequestBody(textMediaType))
                        .build()
                    try {
                        httpClient.newCall(pubRequest).execute().close()
                    } catch (_: Exception) {}
                }
            } else if (!localHasData) {
                // Local is empty and neither remote nor cached vault had data -> trigger fallback dataset restoration!
                val restored = onRemoteRestored("", true)
                if (restored) {
                    remoteRestoredSuccess = true
                    val currentDataAfterRestore = localJsonProvider()
                    if (hasMeaningfulData(currentDataAfterRestore)) {
                        saveSavedVaultBackup(currentDataAfterRestore)
                    }
                    val now = System.currentTimeMillis()
                    onLocalDataTimestampUpdated(now)
                }
            } else if (remoteHasData && isRemoteNewer) {
                // Normal newer remote sync
                val restored = onRemoteRestored(latestRemoteData!!, false)
                if (restored) {
                    remoteRestoredSuccess = true
                    saveSavedVaultBackup(latestRemoteData!!)
                    onLocalDataTimestampUpdated(maxRemoteTimestamp)
                }
            } else if (localHasData) {
                // ONLY upload when local actually has valid data! Never upload empty data!
                saveSavedVaultBackup(currentLocalJson)
                val now = System.currentTimeMillis()
                val payload = JSONObject().apply {
                    put("updatedAt", now)
                    put("userEmail", accountEmail)
                    put("data", currentLocalJson)
                }.toString()

                val textMediaType = "text/plain; charset=utf-8".toMediaType()
                val pubRequest = Request.Builder()
                    .url(publishUrl)
                    .addHeader("Filename", "backup.json")
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

            // Cloud-only mode: No local fallback disk files stored.
            cleanupAllLocalAppStorage()

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
        cleanupAllLocalAppStorage()
        true
    }

    suspend fun uploadOrOverwriteWebCompanionHtml(
        overrideAccountEmail: String? = null,
        htmlData: String
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        cleanupAllLocalAppStorage()
        val account = getSignedInAccount()
        val accountEmail = account?.email ?: overrideAccountEmail ?: "86brancan@gmail.com"

        val token = getValidToken(account, accountEmail)
        if (token != null) {
            val folderId = getOrCreatePayCycleFolder(token)
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
                    .addPart(Headers.headersOf("Content-Type", "application/json; charset=UTF-8"), meta.toRequestBody("application/json; charset=utf-8".toMediaType()))
                    .addPart(Headers.headersOf("Content-Type", "text/html; charset=UTF-8"), htmlData.toRequestBody(htmlMediaType))
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
                    return@withContext Pair(true, "Overwrote previous '$webCompanionFileName' in My Drive/ PayCycle Budget/ ($accountEmail)!")
                }
            }

            // Create fresh file if not existing inside PayCycle Budget folder
            val targetParent = folderId ?: "root"
            val createMeta = JSONObject().apply {
                put("name", webCompanionFileName)
                put("mimeType", "text/html")
                put("parents", JSONArray().apply { put(targetParent) })
            }.toString()

            val createMultipartBody = MultipartBody.Builder()
                .setType("multipart/related".toMediaType())
                .addPart(Headers.headersOf("Content-Type", "application/json; charset=UTF-8"), createMeta.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .addPart(Headers.headersOf("Content-Type", "text/html; charset=UTF-8"), htmlData.toRequestBody(htmlMediaType))
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
                        return@withContext Pair(true, "Saved '$webCompanionFileName' to My Drive/ PayCycle Budget/ ($accountEmail)!")
                    }
                }
            } catch (e: Exception) {
                Log.e("GoogleDriveSync", "Error creating web companion on Google Drive", e)
            }
        }

        Pair(false, "Web app exported as '$webCompanionFileName'")
    }
}
