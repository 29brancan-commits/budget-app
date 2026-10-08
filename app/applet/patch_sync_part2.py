import sys

with open('app/src/main/java/com/example/sync/GoogleDriveSyncManager.kt', 'r') as f:
    content = f.read()

# 1. Update extractSpreadsheetId and fetchSpreadsheetByUrlOrId
old_fetch = """    fun extractSpreadsheetId(input: String): String {
        val trimmed = input.trim()
        val regex = Regex("/d/([a-zA-Z0-9-_]+)")
        val match = regex.find(trimmed)
        return match?.groupValues?.get(1) ?: trimmed
    }

    suspend fun fetchSpreadsheetByUrlOrId(urlOrId: String, overrideToken: String? = null): String? = withContext(Dispatchers.IO) {
        try {
            val rawInput = urlOrId.trim()
            if (rawInput.isBlank()) return@withContext null
            val id = extractSpreadsheetId(rawInput)
            val account = getSignedInAccount()
            val accountEmail = account?.email ?: "86brancan@gmail.com"
            val token = overrideToken ?: getValidToken(account, accountEmail)

            // 1. Try with token if present
            if (!token.isNullOrBlank()) {
                var targetFileId = id
                var targetMimeType: String? = null
                // If input looks like a name instead of an alphanumeric Drive ID, search Drive by name
                if (targetFileId.contains(" ") || targetFileId.contains(".csv") || targetFileId.length < 15) {
                    val searched = searchCsvFile(token, targetFileId, targetFileId, null)
                    if (searched?.first != null) {
                        targetFileId = searched.first!!
                    } else {
                        val cleanName = targetFileId.removeSuffix(".csv").removeSuffix(".xlsx").replace("'", "\\\\'")
                        val qUrl = "https://www.googleapis.com/drive/v3/files?q=name%20contains%20%27$cleanName%27%20and%20trashed%3Dfalse&pageSize=5&fields=files(id,name,mimeType)"
                        val req = Request.Builder().url(qUrl).addHeader("Authorization", "Bearer $token").get().build()
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
                val csvContent = downloadCsvFile(token, targetFileId, targetMimeType)
                if (!csvContent.isNullOrBlank()) {
                    return@withContext csvContent
                }
            }

            // 2. Check Cloud Sync data for Debt / Budget if name matches
            if (rawInput.contains("debt", ignoreCase = true) || rawInput.contains("Debt Payoff", ignoreCase = true)) {
                val cloudDebt = fetchLatestCloudDebtData(accountEmail)
                if (!cloudDebt.isNullOrBlank()) {
                    return@withContext cloudDebt
                }
            }

            // 3. Try direct and public export URLs for Google Sheet / CSV / Drive
            val exportUrls = listOf(
                "https://docs.google.com/spreadsheets/d/$id/export?format=csv",
                "https://docs.google.com/spreadsheets/d/$id/gviz/tq?tqx=out:csv",
                "https://docs.google.com/spreadsheets/d/$id/pub?output=csv",
                "https://docs.google.com/spreadsheets/d/$id/export?format=csv&gid=0",
                "https://drive.google.com/uc?export=download&id=$id"
            )
            for (exportUrl in exportUrls) {
                try {
                    val req = Request.Builder().url(exportUrl).get().build()
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
        } catch (e: Exception) {
            Log.e("GoogleDriveSync", "Error fetching spreadsheet by URL or ID", e)
        }
        null
    }"""

new_fetch = """    fun extractSpreadsheetId(input: String): String {
        val trimmed = input.trim()
        val regex = Regex("/d/([a-zA-Z0-9-_]+)")
        val match = regex.find(trimmed)
        return match?.groupValues?.get(1) ?: trimmed
    }

    fun extractGid(input: String): String? {
        val regex = Regex("[?&#]gid=([0-9]+)")
        return regex.find(input)?.groupValues?.get(1)
    }

    suspend fun fetchSpreadsheetByUrlOrId(urlOrId: String, overrideToken: String? = null): String? = withContext(Dispatchers.IO) {
        try {
            val rawInput = urlOrId.trim()
            if (rawInput.isBlank()) return@withContext null
            val id = extractSpreadsheetId(rawInput)
            val gid = extractGid(rawInput)
            val account = getSignedInAccount()
            val accountEmail = account?.email ?: "86brancan@gmail.com"
            val token = overrideToken ?: getValidToken(account, accountEmail)

            // 1. Try with token if present
            if (!token.isNullOrBlank()) {
                var targetFileId = id
                var targetMimeType: String? = null
                // If input looks like a name instead of an alphanumeric Drive ID, search Drive by name
                if (targetFileId.contains(" ") || targetFileId.contains(".csv") || targetFileId.length < 15) {
                    val searched = searchCsvFile(token, targetFileId, targetFileId, null)
                    if (searched?.first != null) {
                        targetFileId = searched.first!!
                    } else {
                        val cleanName = targetFileId.removeSuffix(".csv").removeSuffix(".xlsx").replace("'", "\\\\'")
                        val qUrl = "https://www.googleapis.com/drive/v3/files?q=name%20contains%20%27$cleanName%27%20and%20trashed%3Dfalse&pageSize=5&fields=files(id,name,mimeType)"
                        val req = Request.Builder().url(qUrl).addHeader("Authorization", "Bearer $token").get().build()
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

                // If specific gid was requested in URL, fetch that exact tab first
                if (!gid.isNullOrBlank()) {
                    val gidExportUrls = listOf(
                        "https://docs.google.com/spreadsheets/d/$targetFileId/export?format=csv&gid=$gid",
                        "https://docs.google.com/spreadsheets/d/$targetFileId/gviz/tq?tqx=out:csv&gid=$gid",
                        "https://docs.google.com/spreadsheets/d/$targetFileId/pub?output=csv&gid=$gid"
                    )
                    for (exportUrl in gidExportUrls) {
                        try {
                            val req = Request.Builder().url(exportUrl).addHeader("Authorization", "Bearer $token").get().build()
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

                val csvContent = downloadCsvFile(token, targetFileId, targetMimeType)
                if (!csvContent.isNullOrBlank()) {
                    return@withContext csvContent
                }
            }

            // 2. Direct export endpoints (with token header if available)
            val exportUrls = mutableListOf<String>()
            if (!gid.isNullOrBlank()) {
                exportUrls.add("https://docs.google.com/spreadsheets/d/$id/export?format=csv&gid=$gid")
                exportUrls.add("https://docs.google.com/spreadsheets/d/$id/gviz/tq?tqx=out:csv&gid=$gid")
                exportUrls.add("https://docs.google.com/spreadsheets/d/$id/pub?output=csv&gid=$gid")
            }
            exportUrls.add("https://docs.google.com/spreadsheets/d/$id/export?format=xlsx")
            exportUrls.add("https://docs.google.com/spreadsheets/d/$id/export?format=csv")
            exportUrls.add("https://docs.google.com/spreadsheets/d/$id/gviz/tq?tqx=out:csv")
            exportUrls.add("https://docs.google.com/spreadsheets/d/$id/pub?output=csv")
            exportUrls.add("https://docs.google.com/spreadsheets/d/$id/export?format=csv&gid=0")
            exportUrls.add("https://drive.google.com/uc?export=download&id=$id")

            for (exportUrl in exportUrls) {
                try {
                    val reqBuilder = Request.Builder().url(exportUrl)
                    if (!token.isNullOrBlank()) {
                        reqBuilder.addHeader("Authorization", "Bearer $token")
                    }
                    val req = reqBuilder.build()
                    httpClient.newCall(req).execute().use { resp ->
                        if (resp.isSuccessful) {
                            val bytes = resp.body?.bytes()
                            if (bytes != null && bytes.isNotEmpty()) {
                                if (SpreadsheetFileParser.isZipOrXlsx(bytes) || exportUrl.contains("format=xlsx")) {
                                    val parsedXlsx = SpreadsheetFileParser.parseXlsxBytes(bytes)
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
    }"""

if old_fetch in content:
    content = content.replace(old_fetch, new_fetch, 1)
    print("Replaced fetchSpreadsheetByUrlOrId successfully")
else:
    print("Could not find old_fetch")
    sys.exit(1)

# 2. Update listAllDriveImportFiles and importSingleDriveFile
old_list_and_import = """    suspend fun listAllDriveImportFiles(overrideAccountEmail: String? = null): Pair<List<DriveFileInfo>, String?> = withContext(Dispatchers.IO) {
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

        // 1. Always include Active Cloud Sync files if available
        try {
            val cloudData = fetchLatestCloudData(accountEmail)
            if (hasMeaningfulData(cloudData)) {
                allFiles.add(
                    DriveFileInfo(
                        id = "cloud_sync_latest",
                        name = "PayCycle Cloud Budget Sync ($accountEmail)",
                        mimeType = "application/json",
                        modifiedTime = "",
                        formattedDate = "Active Cloud Sync",
                        sizeString = "Cloud Sync",
                        location = "PayCycle Budget",
                        isGoogleSheet = false,
                        isCsv = false,
                        isJson = true
                    )
                )
                seenIds.add("cloud_sync_latest")
            }
        } catch (_: Exception) {}

        try {
            val debtCloudData = fetchLatestCloudDebtData(accountEmail)
            if (!debtCloudData.isNullOrBlank() && hasMeaningfulData(debtCloudData)) {
                allFiles.add(
                    DriveFileInfo(
                        id = "debt_cloud_sync_latest",
                        name = "PayCycle Debt Payoff Cloud Sync ($accountEmail)",
                        mimeType = "text/csv",
                        modifiedTime = "",
                        formattedDate = "Active Cloud Sync",
                        sizeString = "Debt Sync",
                        location = "PayCycle Budget",
                        isGoogleSheet = true,
                        isCsv = true,
                        isJson = false
                    )
                )
                seenIds.add("debt_cloud_sync_latest")
            }
        } catch (_: Exception) {}

        val token = getValidToken(account, accountEmail)
        if (token.isNullOrBlank()) {
            val msg = "Sign in with Google to browse all Drive files, or paste a Google Sheet link or token above."
            return@withContext Pair(allFiles, msg)
        }

        // 3. User is authorized with token: query Google Drive folders and files
        fun runDriveQuery(query: String, location: String) {
            try {
                var pageToken: String? = null
                var pageCount = 0
                do {
                    val urlBuilder = "https://www.googleapis.com/drive/v3/files".toHttpUrl().newBuilder()
                        .addQueryParameter("fields", "nextPageToken,files(id,name,mimeType,modifiedTime,size)")
                        .addQueryParameter("pageSize", "1000")
                        .addQueryParameter("supportsAllDrives", "true")
                        .addQueryParameter("includeItemsFromAllDrives", "true")
                        .addQueryParameter("orderBy", "modifiedTime desc")

                    if (query.isNotBlank()) {
                        urlBuilder.addQueryParameter("q", query)
                    }
                    if (!pageToken.isNullOrBlank()) {
                        urlBuilder.addQueryParameter("pageToken", pageToken)
                    }

                    val url = urlBuilder.build()
                    val req = Request.Builder().url(url).addHeader("Authorization", "Bearer $token").get().build()
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
                } while (!pageToken.isNullOrBlank() && pageCount < 10)
            } catch (e: Exception) {
                Log.e("GoogleDriveSync", "Error in runDriveQuery '$query'", e)
            }
        }

        val folderId = getOrCreatePayCycleFolder(token)
        val targetFolder = folderId.ifBlank { TARGET_FOLDER_ID }

        // Primary: designated folder
        runDriveQuery("trashed=false and '" + targetFolder + "' in parents", "PayCycle Budget")
        runDriveQuery("'" + targetFolder + "' in parents", "PayCycle Budget")

        // Secondary: all spreadsheets and relevant finance files across user Drive
        runDriveQuery("mimeType='application/vnd.google-apps.spreadsheet' and trashed=false", "My Drive")
        runDriveQuery("trashed=false and (mimeType='application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' or mimeType='application/vnd.ms-excel' or name contains '.csv' or name contains '.xlsx' or name contains '.xls')", "My Drive")
        runDriveQuery("trashed=false and (name contains 'PayCycle' or name contains 'Budget' or name contains 'Expense' or name contains 'Debt' or name contains 'Import' or name contains 'Export')", "My Drive")
        runDriveQuery("trashed=false and mimeType != 'application/vnd.google-apps.folder'", "My Drive")
        runDriveQuery("sharedWithMe=true and trashed=false", "Shared with Me")

        val sorted = allFiles.sortedWith(
            compareBy<DriveFileInfo> {
                when {
                    it.id.startsWith("cloud_sync") || it.id.startsWith("debt_cloud") -> 0
                    it.isGoogleSheet -> 1
                    it.location == "PayCycle Budget" -> 1
                    it.isCsv -> 2
                    else -> 2
                }
            }.thenByDescending { it.modifiedTime }
        )
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

        if (fileId == "cloud_sync_latest" || fileId.startsWith("cloud_sync")) {
            val cloudData = fetchLatestCloudData(accountEmail)
            if (!cloudData.isNullOrBlank() && hasMeaningfulData(cloudData)) {
                return@withContext onSmartImport(cloudData, fileName, false)
            }
        }
        if (fileId == "debt_cloud_sync_latest" || fileId.startsWith("debt_cloud")) {
            val debtCloudData = fetchLatestCloudDebtData(accountEmail)
            if (!debtCloudData.isNullOrBlank() && hasMeaningfulData(debtCloudData)) {
                return@withContext onSmartImport(debtCloudData, fileName, true)
            }
        }

        // Try direct fetch with fetchSpreadsheetByUrlOrId
        val fallbackPublicContent = fetchSpreadsheetByUrlOrId(fileId, null)
        if (!fallbackPublicContent.isNullOrBlank()) {
            return@withContext onSmartImport(fallbackPublicContent, fileName, isDebtHint)
        }

        val token = getValidToken(account, accountEmail)
        if (token.isNullOrBlank()) {
            val cloudData = fetchLatestCloudData(accountEmail)
            if (!cloudData.isNullOrBlank() && hasMeaningfulData(cloudData)) {
                return@withContext onSmartImport(cloudData, fileName, false)
            }
            return@withContext Pair(
                false,
                "Authorization required to download '$fileName'. Please sign in with Google or paste the Google Sheet URL directly in the link box."
            )
        }

        try {
            if (lowerName.endsWith(".json") || mimeType.contains("json")) {
                val jsonUrl = "https://www.googleapis.com/drive/v3/files/$fileId?alt=media"
                val jsonReq = Request.Builder().url(jsonUrl).addHeader("Authorization", "Bearer $token").get().build()
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
                val csvContent = downloadCsvFile(token, fileId, mimeType)
                if (!csvContent.isNullOrBlank()) {
                    return@withContext onSmartImport(csvContent, fileName, isDebtHint)
                }
                Pair(false, "Could not extract CSV or spreadsheet data from '$fileName'.")
            }
        } catch (e: Exception) {
            Log.e("GoogleDriveSync", "Error importing single file $fileName ($fileId)", e)
            Pair(false, "Error downloading '$fileName': ${e.localizedMessage}")
        }
    }"""

new_list_and_import = """    suspend fun listAllDriveImportFiles(overrideAccountEmail: String? = null): Pair<List<DriveFileInfo>, String?> = withContext(Dispatchers.IO) {
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
        fun runDriveQuery(query: String, location: String) {
            try {
                var pageToken: String? = null
                var pageCount = 0
                do {
                    val urlBuilder = "https://www.googleapis.com/drive/v3/files".toHttpUrl().newBuilder()
                        .addQueryParameter("fields", "nextPageToken,files(id,name,mimeType,modifiedTime,size)")
                        .addQueryParameter("pageSize", "100")
                        .addQueryParameter("supportsAllDrives", "true")
                        .addQueryParameter("includeItemsFromAllDrives", "true")
                        .addQueryParameter("orderBy", "modifiedTime desc")

                    if (query.isNotBlank()) {
                        urlBuilder.addQueryParameter("q", query)
                    }
                    if (!pageToken.isNullOrBlank()) {
                        urlBuilder.addQueryParameter("pageToken", pageToken)
                    }

                    val url = urlBuilder.build()
                    val req = Request.Builder().url(url).addHeader("Authorization", "Bearer $token").get().build()
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

        val folderId = getOrCreatePayCycleFolder(token)
        val targetFolder = folderId.ifBlank { TARGET_FOLDER_ID }

        // Primary: files in designated PayCycle folder
        if (targetFolder.isNotBlank()) {
            runDriveQuery("trashed = false and '$targetFolder' in parents", "PayCycle Budget")
        }

        // Secondary: all non-trashed files across user Google Drive sorted by modified time
        runDriveQuery("trashed = false", "My Drive")

        // Tertiary: files shared with the user
        runDriveQuery("sharedWithMe = true and trashed = false", "Shared with Me")

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
            // Direct public fetch with fetchSpreadsheetByUrlOrId
            val fallbackPublicContent = fetchSpreadsheetByUrlOrId(fileId, null)
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
                val jsonReq = Request.Builder().url(jsonUrl).addHeader("Authorization", "Bearer $token").get().build()
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
                val csvContent = downloadCsvFile(token, fileId, mimeType)
                if (!csvContent.isNullOrBlank()) {
                    return@withContext onSmartImport(csvContent, fileName, isDebtHint)
                }
                val altContent = fetchSpreadsheetByUrlOrId(fileId, token)
                if (!altContent.isNullOrBlank()) {
                    return@withContext onSmartImport(altContent, fileName, isDebtHint)
                }
                Pair(false, "Could not extract CSV or spreadsheet data from '$fileName'.")
            }
        } catch (e: Exception) {
            Log.e("GoogleDriveSync", "Error importing single file $fileName ($fileId)", e)
            Pair(false, "Error downloading '$fileName': ${e.localizedMessage}")
        }
    }"""

if old_list_and_import in content:
    content = content.replace(old_list_and_import, new_list_and_import, 1)
    print("Replaced listAllDriveImportFiles and importSingleDriveFile successfully")
else:
    print("Could not find old_list_and_import")
    sys.exit(1)

with open('app/src/main/java/com/example/sync/GoogleDriveSyncManager.kt', 'w') as f:
    f.write(content)
print("Updated GoogleDriveSyncManager successfully!")
