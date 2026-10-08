package com.example.sync

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import java.io.ByteArrayInputStream
import java.nio.charset.Charset
import java.util.Locale
import java.util.zip.ZipInputStream

data class ParsedWorksheet(
    val name: String,
    val sheetId: String,
    val csvContent: String
)

object SpreadsheetFileParser {

    private const val TAG = "SpreadsheetFileParser"

    fun isDebtCsvContent(text: String): Boolean {
        val nonBlankLines = text.trim().removePrefix("\uFEFF").lines().filter { it.isNotBlank() }.take(15)
        return nonBlankLines.any { line ->
            val l = line.lowercase(Locale.US)
            val hasDebtKeywords = l.contains("debt") || l.contains("apr") ||
                    l.contains("creditor") || l.contains("lender") ||
                    (l.contains("balance") && (l.contains("rate") || l.contains("interest") || l.contains("minimum") || l.contains("min pay")))
            val hasDateKeywords = l.contains("trans date") || l.contains("transaction date") ||
                    l.contains("post date") || l.contains("posting date")
            hasDebtKeywords && !hasDateKeywords
        }
    }

    fun extractContentFromUri(context: Context, uri: Uri): Pair<String?, String> {
        var fileName = "imported_file.csv"
        var docId: String? = null
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIdx >= 0 && cursor.moveToFirst()) {
                    val nameVal = cursor.getString(nameIdx)
                    if (!nameVal.isNullOrBlank()) {
                        fileName = nameVal
                    }
                }
                val docIdx = cursor.getColumnIndex("document_id")
                if (docIdx >= 0 && cursor.moveToPosition(0)) {
                    val idVal = cursor.getString(docIdx)
                    if (!idVal.isNullOrBlank() && !idVal.contains("encoded=") && !idVal.contains("acc=")) {
                        docId = idVal
                    }
                }
            }
        } catch (_: Exception) {}

        // Inspect URI for Drive doc ID if docId not set
        val uriStr = uri.toString()
        if (docId.isNullOrBlank()) {
            val docMatch = Regex("doc:([a-zA-Z0-9-_]{20,})").find(uriStr)
                ?: Regex("/d/([a-zA-Z0-9-_]{20,})").find(uriStr)
                ?: Regex("([a-zA-Z0-9-_]{25,})").find(uriStr)
            if (docMatch != null) {
                val candidate = docMatch.groupValues[1]
                if (!candidate.contains("encoded") && !candidate.contains("acc=")) {
                    docId = candidate
                }
            }
        }

        val lowerName = fileName.lowercase(Locale.US)
        var content: String? = null

        // 1. Direct InputStream read
        try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes != null && bytes.isNotEmpty()) {
                if (isZipOrXlsx(bytes) || lowerName.endsWith(".xlsx") || lowerName.endsWith(".xlsm")) {
                    content = parseXlsxBytes(bytes)
                }
                if (content.isNullOrBlank()) {
                    content = decodeTextBytes(bytes)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed reading input stream for $fileName: ${e.message}")
        }

        // 2. Try getStreamTypes and openTypedAssetFileDescriptor (Essential for Google Drive virtual documents)
        if (content.isNullOrBlank()) {
            val streamTypes = try {
                context.contentResolver.getStreamTypes(uri, "*/*")?.toList() ?: emptyList()
            } catch (_: Exception) {
                emptyList()
            }
            val preferredMimes = listOf(
                "text/csv",
                "text/comma-separated-values",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "application/vnd.ms-excel",
                "text/tab-separated-values",
                "text/plain",
                "application/json",
                "*/*"
            )
            val mimeCandidates = (streamTypes + preferredMimes).distinct()

            for (mime in mimeCandidates) {
                try {
                    val pfd = context.contentResolver.openTypedAssetFileDescriptor(uri, mime, null)
                    if (pfd != null) {
                        val text = pfd.createInputStream().use { stream ->
                            val bytes = stream.readBytes()
                            if (bytes.isNotEmpty()) {
                                if (isZipOrXlsx(bytes) || mime.contains("spreadsheetml") || mime.contains("ms-excel")) {
                                    parseXlsxBytes(bytes)
                                } else {
                                    decodeTextBytes(bytes)
                                }
                            } else null
                        }
                        pfd.close()
                        if (!text.isNullOrBlank()) {
                            content = text
                            break
                        }
                    }
                } catch (_: Exception) {}
            }
        }

        // 3. Fallback ParcelFileDescriptor
        if (content.isNullOrBlank()) {
            try {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    val bytes = java.io.FileInputStream(pfd.fileDescriptor).use { it.readBytes() }
                    if (isZipOrXlsx(bytes)) {
                        content = parseXlsxBytes(bytes)
                    }
                    if (content.isNullOrBlank()) {
                        content = decodeTextBytes(bytes)
                    }
                }
            } catch (_: Exception) {}
        }

        val finalIdentifier = if (!docId.isNullOrBlank() && !docId!!.contains("encoded=") && !docId!!.contains("acc=")) docId!! else fileName
        return Pair(content?.takeIf { it.isNotBlank() }, finalIdentifier)
    }

    fun isZipOrXlsx(bytes: ByteArray): Boolean {
        return bytes.size > 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte() && bytes[2] == 0x03.toByte() && bytes[3] == 0x04.toByte()
    }

    fun decodeTextBytes(bytes: ByteArray): String {
        if (bytes.isEmpty()) return ""
        // Check for UTF-8 with BOM or standard UTF-8
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
            return String(bytes, 3, bytes.size - 3, Charsets.UTF_8).trim()
        }
        // Check UTF-16 LE
        if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
            return String(bytes, 2, bytes.size - 2, Charset.forName("UTF-16LE")).trim()
        }
        // Check UTF-16 BE
        if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
            return String(bytes, 2, bytes.size - 2, Charset.forName("UTF-16BE")).trim()
        }

        val utf8Text = String(bytes, Charsets.UTF_8)
        if (!utf8Text.contains("\u0000")) {
            return utf8Text.removePrefix("\uFEFF").trim()
        }

        // Fallback to ISO-8859-1
        return String(bytes, Charset.forName("ISO-8859-1")).removePrefix("\uFEFF").trim()
    }

    fun parseXlsxSheets(bytes: ByteArray): List<ParsedWorksheet> {
        try {
            val zip = ZipInputStream(ByteArrayInputStream(bytes))
            val sharedStrings = mutableListOf<String>()
            val rawEntries = mutableMapOf<String, ByteArray>()

            var entry = zip.nextEntry
            while (entry != null) {
                val name = entry.name
                val entryBytes = zip.readBytes()
                if (name == "xl/sharedStrings.xml") {
                    parseSharedStringsXml(entryBytes, sharedStrings)
                } else {
                    rawEntries[name] = entryBytes
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }

            // Parse workbook.xml to extract sheet names, sheetIds, and rIds
            val wbBytes = rawEntries["xl/workbook.xml"] ?: return emptyList()
            val wbXml = String(wbBytes, Charsets.UTF_8)
            val sheetRegex = Regex("<sheet\\s+([^>]*?)/>", RegexOption.DOT_MATCHES_ALL)
            val nameRegex = Regex("name=\"([^\"]+)\"")
            val sheetIdRegex = Regex("sheetId=\"([^\"]+)\"")
            val rIdRegex = Regex("r:id=\"([^\"]+)\"")

            val sheetMetaList = mutableListOf<Triple<String, String, String>>() // name, sheetId, rId
            sheetRegex.findAll(wbXml).forEach { match ->
                val attrs = match.groupValues[1]
                val sName = nameRegex.find(attrs)?.groupValues?.get(1) ?: "Sheet"
                val sId = sheetIdRegex.find(attrs)?.groupValues?.get(1) ?: ""
                val rId = rIdRegex.find(attrs)?.groupValues?.get(1) ?: ""
                if (rId.isNotBlank()) {
                    sheetMetaList.add(Triple(unescapeXml(sName), sId, rId))
                }
            }

            // Parse rels to map rId to worksheet XML target file
            val relsBytes = rawEntries["xl/_rels/workbook.xml.rels"]
            val relsMap = mutableMapOf<String, String>()
            if (relsBytes != null) {
                val relsXml = String(relsBytes, Charsets.UTF_8)
                val relRegex = Regex("<Relationship\\s+([^>]*?)/>", RegexOption.DOT_MATCHES_ALL)
                val idAttrRegex = Regex("Id=\"([^\"]+)\"")
                val targetAttrRegex = Regex("Target=\"([^\"]+)\"")
                relRegex.findAll(relsXml).forEach { rMatch ->
                    val attrs = rMatch.groupValues[1]
                    val idVal = idAttrRegex.find(attrs)?.groupValues?.get(1) ?: ""
                    val targetVal = targetAttrRegex.find(attrs)?.groupValues?.get(1) ?: ""
                    if (idVal.isNotBlank() && targetVal.isNotBlank()) {
                        val cleanTarget = when {
                            targetVal.startsWith("/") -> targetVal.substring(1)
                            targetVal.startsWith("xl/") -> targetVal
                            else -> "xl/$targetVal"
                        }
                        relsMap[idVal] = cleanTarget
                    }
                }
            }

            val resultSheets = mutableListOf<ParsedWorksheet>()
            for ((sheetName, sheetId, rId) in sheetMetaList) {
                val targetPath = relsMap[rId] ?: "xl/worksheets/sheet${sheetId}.xml"
                val sheetXmlBytes = rawEntries[targetPath] ?: rawEntries.entries.firstOrNull { it.key.endsWith(targetPath.substringAfterLast("/")) }?.value
                if (sheetXmlBytes != null) {
                    val csvText = parseSheetXmlToCsv(sheetXmlBytes, sharedStrings).trim()
                    if (csvText.isNotBlank()) {
                        resultSheets.add(ParsedWorksheet(name = sheetName, sheetId = sheetId, csvContent = csvText))
                    }
                }
            }

            // Fallback if workbook.xml had no sheets: inspect any sheet*.xml directly
            if (resultSheets.isEmpty()) {
                val sheetEntries = rawEntries.filter { it.key.startsWith("xl/worksheets/sheet") && it.key.endsWith(".xml") }
                sheetEntries.entries.sortedBy { it.key }.forEachIndexed { idx, entryItem ->
                    val csv = parseSheetXmlToCsv(entryItem.value, sharedStrings).trim()
                    if (csv.isNotBlank()) {
                        resultSheets.add(ParsedWorksheet(name = "Sheet${idx + 1}", sheetId = idx.toString(), csvContent = csv))
                    }
                }
            }

            return resultSheets
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting XLSX sheets", e)
            return emptyList()
        }
    }

    fun parseXlsxBytes(bytes: ByteArray, preferredGid: String? = null, isDebtHint: Boolean = false): String? {
        val sheets = parseXlsxSheets(bytes)
        if (sheets.isEmpty()) return null

        // 1. If user passed a specific gid in URL, locate exact worksheet matching sheetId
        if (!preferredGid.isNullOrBlank()) {
            val matchedByGid = sheets.firstOrNull { it.sheetId == preferredGid }
                ?: sheets.firstOrNull { it.sheetId == "0" && (preferredGid == "0" || preferredGid.isBlank()) }
            if (matchedByGid != null && matchedByGid.csvContent.isNotBlank()) {
                return matchedByGid.csvContent
            }
        }

        val nonBlankSheets = sheets.filter { it.csvContent.isNotBlank() }
        if (nonBlankSheets.isEmpty()) return null

        if (nonBlankSheets.size == 1) {
            return nonBlankSheets[0].csvContent
        }

        // If multiple tabs exist, concatenate CSV content from all tabs so no entries are lost
        return nonBlankSheets.joinToString("\n\n") { it.csvContent }
    }

    private fun parseSharedStringsXml(xmlBytes: ByteArray, outStrings: MutableList<String>) {
        try {
            val xml = String(xmlBytes, Charsets.UTF_8)
            // Each <si> tag represents one string item in Excel
            val siRegex = Regex("<si[^>]*>(.*?)</si>", RegexOption.DOT_MATCHES_ALL)
            siRegex.findAll(xml).forEach { siMatch ->
                val siContent = siMatch.groupValues[1]
                val tMatches = Regex("<t(?: [^>]*)?>([^<]*)</t>").findAll(siContent)
                val combinedText = tMatches.joinToString("") { it.groupValues[1] }
                outStrings.add(unescapeXml(combinedText))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing shared strings XML", e)
        }
    }

    private fun parseSheetXmlToCsv(sheetBytes: ByteArray, sharedStrings: List<String>): String {
        val xml = String(sheetBytes, Charsets.UTF_8)
        val sb = StringBuilder()

        val rowRegex = Regex("<row[^>]*>(.*?)</row>", RegexOption.DOT_MATCHES_ALL)
        val cellRegex = Regex("<c([^/>]*)(/>|>(.*?)</c>)", RegexOption.DOT_MATCHES_ALL)
        val cellAttrRefRegex = Regex("r=\"([A-Z]+)(\\d+)\"")
        val cellAttrTypeRegex = Regex("t=\"([^\"]+)\"")
        val valRegex = Regex("<v>([^<]*)</v>")
        val inlineStrRegex = Regex("<is>.*?<t(?: [^>]*)?>([^<]*)</t>.*?</is>", RegexOption.DOT_MATCHES_ALL)

        rowRegex.findAll(xml).forEach { rowMatch ->
            val rowContent = rowMatch.groupValues[1]
            val cellsByCol = mutableMapOf<Int, String>()
            var maxCol = 0

            cellRegex.findAll(rowContent).forEach { cMatch ->
                val attrs = cMatch.groupValues[1]
                val body = cMatch.groupValues[3]

                // Determine column index from cell reference e.g. "A1", "B2", "AA3"
                val refMatch = cellAttrRefRegex.find(attrs)
                val colLetters = refMatch?.groupValues?.get(1) ?: ""
                val colIndex = if (colLetters.isNotBlank()) excelColLettersToIndex(colLetters) else maxCol + 1
                maxCol = maxOf(maxCol, colIndex)

                if (body.isBlank()) {
                    cellsByCol[colIndex] = ""
                    return@forEach
                }

                val typeMatch = cellAttrTypeRegex.find(attrs)
                val type = typeMatch?.groupValues?.get(1) ?: ""

                val rawVal = valRegex.find(body)?.groupValues?.get(1) ?: ""
                val inlineVal = inlineStrRegex.find(body)?.groupValues?.get(1) ?: ""

                val resolvedVal: String = when {
                    inlineVal.isNotEmpty() -> unescapeXml(inlineVal)
                    type == "s" -> {
                        val sIdx = rawVal.toIntOrNull()
                        if (sIdx != null && sIdx in sharedStrings.indices) {
                            sharedStrings[sIdx]
                        } else {
                            unescapeXml(rawVal)
                        }
                    }
                    type == "b" -> if (rawVal == "1") "TRUE" else "FALSE"
                    type == "str" || type == "inlineStr" -> unescapeXml(rawVal.ifEmpty { inlineVal })
                    else -> unescapeXml(rawVal)
                }

                cellsByCol[colIndex] = resolvedVal
            }

            if (maxCol > 0 && cellsByCol.values.any { it.isNotBlank() }) {
                val rowCells = mutableListOf<String>()
                for (col in 1..maxCol) {
                    val cellVal = cellsByCol[col] ?: ""
                    val escaped = escapeCsvValue(cellVal)
                    rowCells.add(escaped)
                }
                sb.append(rowCells.joinToString(",")).append("\n")
            }
        }

        return sb.toString()
    }

    private fun excelColLettersToIndex(letters: String): Int {
        var result = 0
        for (char in letters.uppercase(Locale.US)) {
            if (char in 'A'..'Z') {
                result = result * 26 + (char - 'A' + 1)
            }
        }
        return if (result > 0) result else 1
    }

    private fun escapeCsvValue(value: String): String {
        val trimmed = value.trim()
        if (trimmed.contains(",") || trimmed.contains("\n") || trimmed.contains("\r") || trimmed.contains("\"")) {
            val doubled = trimmed.replace("\"", "\"\"")
            return "\"$doubled\""
        }
        return trimmed
    }

    private fun unescapeXml(text: String): String {
        return text.replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
    }
}
