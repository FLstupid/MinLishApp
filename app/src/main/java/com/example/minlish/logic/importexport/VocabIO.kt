package com.example.minlish.logic.importexport

import com.example.minlish.data.model.Word
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.InputStream
import java.io.ByteArrayOutputStream

object VocabIO {
    data class VocabRow(
        val word: String,
        val pronunciation: String?,
        val meaning: String,
        val descriptionEn: String?,
        val example: String?,
        val collocation: String?,
        val relatedWords: String?,
        val note: String?,
        val cefrLevel: String? = null,
        val easeFactor: Double? = null,
        val interval: Int? = null,
        val repetitions: Int? = null,
        val nextReviewDate: Long? = null,
        val lastReviewed: Long? = null,
    )

    fun rowHasSrsFields(row: VocabRow): Boolean =
        row.easeFactor != null ||
            row.interval != null ||
            row.repetitions != null ||
            row.nextReviewDate != null ||
            row.lastReviewed != null

    fun wordFromRow(row: VocabRow, setId: Long, existingId: Long = 0L): Word {
        val trimmedWord = row.word.trim()
        return Word(
            id = existingId,
            setId = setId,
            word = trimmedWord,
            wordNorm = trimmedWord.lowercase(),
            pronunciation = row.pronunciation?.trim()?.ifBlank { null },
            meaning = row.meaning.trim(),
            descriptionEn = row.descriptionEn?.trim()?.ifBlank { null },
            example = row.example?.trim()?.ifBlank { null },
            collocation = row.collocation?.trim()?.ifBlank { null },
            relatedWords = row.relatedWords?.trim()?.ifBlank { null },
            note = row.note?.trim()?.ifBlank { null },
            cefrLevel = row.cefrLevel?.trim()?.uppercase()?.ifBlank { null },
            easeFactor = row.easeFactor ?: 2.5,
            interval = row.interval ?: 0,
            repetitions = row.repetitions ?: 0,
            nextReviewDate = row.nextReviewDate ?: System.currentTimeMillis(),
            lastReviewed = row.lastReviewed,
        )
    }

    fun mergeRowSrs(existing: Word, row: VocabRow): Word =
        existing.copy(
            pronunciation = row.pronunciation?.trim()?.ifBlank { null } ?: existing.pronunciation,
            meaning = row.meaning.trim().ifBlank { existing.meaning },
            descriptionEn = row.descriptionEn?.trim()?.ifBlank { null } ?: existing.descriptionEn,
            example = row.example?.trim()?.ifBlank { null } ?: existing.example,
            collocation = row.collocation?.trim()?.ifBlank { null } ?: existing.collocation,
            relatedWords = row.relatedWords?.trim()?.ifBlank { null } ?: existing.relatedWords,
            note = row.note?.trim()?.ifBlank { null } ?: existing.note,
            cefrLevel = row.cefrLevel?.trim()?.uppercase()?.ifBlank { null } ?: existing.cefrLevel,
            easeFactor = row.easeFactor ?: existing.easeFactor,
            interval = row.interval ?: existing.interval,
            repetitions = row.repetitions ?: existing.repetitions,
            nextReviewDate = row.nextReviewDate ?: existing.nextReviewDate,
            lastReviewed = row.lastReviewed ?: existing.lastReviewed,
        )

    private val exportHeaders = listOf(
        "word",
        "meaning",
        "pronunciation",
        "descriptionEn",
        "example",
        "collocation",
        "relatedWords",
        "note",
        "cefrLevel",
        "easeFactor",
        "interval",
        "repetitions",
        "nextReviewDate",
        "lastReviewed",
    )

    private val knownHeaders = setOf(
        "word",
        "meaning",
        "pronunciation",
        "descriptionen",
        "description_en",
        "example",
        "collocation",
        "relatedwords",
        "related_words",
        "note",
        "cefr",
        "cefrlevel",
        "easefactor",
        "interval",
        "repetitions",
        "nextreviewdate",
        "lastreviewed",
    )

    fun parseCsv(csvText: String): List<VocabRow> {
        val lines = csvText
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .lines()
            .map { it.trimEnd() }
            .filter { it.isNotBlank() }

        if (lines.isEmpty()) return emptyList()

        var startIndex = 0
        val firstCells = parseCsvLine(lines.first()).map { it.trim().lowercase().replace(" ", "") }
        val isHeader = firstCells.any { knownHeaders.contains(it) }

        if (isHeader) startIndex = 1

        // If no header, assume fixed column order from spec.
        val fixedOrder = listOf(
            "word", // 0
            "meaning", // 1
            "pronunciation", // 2
            "descriptionEn", // 3
            "example", // 4
            "collocation", // 5
            "relatedWords", // 6
            "note", // 7
        )

        val headerIndexByKey = if (isHeader) {
            val map = mutableMapOf<String, Int>()
            parseCsvLine(lines.first()).forEachIndexed { idx, cell ->
                val key = cell.trim().lowercase().replace(" ", "")
                map[key] = idx
            }
            map
        } else {
            emptyMap()
        }

        fun readByFixedOrder(cells: List<String>, key: String): String? {
            val idx = fixedOrder.indexOf(key)
            if (idx < 0 || idx >= cells.size) return null
            return cells[idx].trim().ifBlank { null }
        }

        fun readByHeader(cells: List<String>, key: String): String? {
            val normalizedKey = key.lowercase().replace(" ", "")
            val idx = headerIndexByKey[normalizedKey] ?: return null
            if (idx < 0 || idx >= cells.size) return null
            return cells[idx].trim().ifBlank { null }
        }

        return lines.drop(startIndex).mapNotNull { line ->
            val cells = parseCsvLine(line)
            val word = if (isHeader) readByHeader(cells, "word") else readByFixedOrder(cells, "word")
            val meaning = if (isHeader) readByHeader(cells, "meaning") else readByFixedOrder(cells, "meaning")

            val wordValue = word?.trim().orEmpty()
            val meaningValue = meaning?.trim().orEmpty()

            if (wordValue.isBlank() || meaningValue.isBlank()) return@mapNotNull null

            val pronunciation = if (isHeader) readByHeader(cells, "pronunciation") else readByFixedOrder(cells, "pronunciation")
            val descriptionEn =
                if (isHeader) readByHeader(cells, "descriptionen") else readByFixedOrder(cells, "descriptionEn")
            val example = if (isHeader) readByHeader(cells, "example") else readByFixedOrder(cells, "example")
            val collocation = if (isHeader) readByHeader(cells, "collocation") else readByFixedOrder(cells, "collocation")
            val relatedWords =
                if (isHeader) readByHeader(cells, "relatedwords") else readByFixedOrder(cells, "relatedWords")
            val note = if (isHeader) readByHeader(cells, "note") else readByFixedOrder(cells, "note")
            val cefrRaw = if (isHeader) {
                readByHeader(cells, "cefr") ?: readByHeader(cells, "cefrlevel")
            } else {
                null
            }
            val easeFactor = if (isHeader) readByHeader(cells, "easefactor") else null
            val interval = if (isHeader) readByHeader(cells, "interval") else null
            val repetitions = if (isHeader) readByHeader(cells, "repetitions") else null
            val nextReviewDate = if (isHeader) readByHeader(cells, "nextreviewdate") else null
            val lastReviewed = if (isHeader) readByHeader(cells, "lastreviewed") else null

            VocabRow(
                word = wordValue,
                pronunciation = pronunciation,
                meaning = meaningValue,
                descriptionEn = descriptionEn,
                example = example,
                collocation = collocation,
                relatedWords = relatedWords,
                note = note,
                cefrLevel = cefrRaw?.trim()?.uppercase()?.ifBlank { null },
                easeFactor = easeFactor?.toDoubleOrNull(),
                interval = interval?.toIntOrNull(),
                repetitions = repetitions?.toIntOrNull(),
                nextReviewDate = nextReviewDate?.toLongOrNull(),
                lastReviewed = lastReviewed?.toLongOrNull(),
            )
        }
    }

    fun parseXlsx(input: InputStream): List<VocabRow> {
        input.use {
            val workbook = XSSFWorkbook(it)
            val sheet = workbook.getSheetAt(0) ?: return emptyList()
            val firstRow = sheet.getRow(0) ?: return emptyList()

            val headerMap = parseHeaderRow(firstRow)
            val hasHeader = headerMap.isNotEmpty()

            val startRow = if (hasHeader) 1 else 0
            val rows = mutableListOf<VocabRow>()

            for (r in startRow..sheet.lastRowNum) {
                val row = sheet.getRow(r) ?: continue
                if (row.isBlank()) continue

                val rowData = VocabRowBuilder()

                if (hasHeader) {
                    headerMap.forEach { (key, idx) ->
                        val cellValue = row.cellValue(idx)
                        when (key) {
                            "word" -> rowData.word = cellValue.orEmpty()
                            "pronunciation" -> rowData.pronunciation = cellValue.nullIfBlank()
                            "meaning" -> rowData.meaning = cellValue.orEmpty()
                            "descriptionen" -> rowData.descriptionEn = cellValue.nullIfBlank()
                            "example" -> rowData.example = cellValue.nullIfBlank()
                            "collocation" -> rowData.collocation = cellValue.nullIfBlank()
                            "relatedwords" -> rowData.relatedWords = cellValue.nullIfBlank()
                            "note" -> rowData.note = cellValue.nullIfBlank()
                            "cefr", "cefrlevel" -> rowData.cefrLevel = cellValue.nullIfBlank()
                            "easefactor" -> rowData.easeFactor = cellValue?.toDoubleOrNull()
                            "interval" -> rowData.interval = cellValue?.toIntOrNull()
                            "repetitions" -> rowData.repetitions = cellValue?.toIntOrNull()
                            "nextreviewdate" -> rowData.nextReviewDate = cellValue?.toLongOrNull()
                            "lastreviewed" -> rowData.lastReviewed = cellValue?.toLongOrNull()
                        }
                    }
                } else {
                    // Fixed order (spec default):
                    rowData.word = row.cellValue(0).orEmpty()
                    rowData.meaning = row.cellValue(1).orEmpty()
                    rowData.pronunciation = row.cellValue(2).nullIfBlank()
                    rowData.descriptionEn = row.cellValue(3).nullIfBlank()
                    rowData.example = row.cellValue(4).nullIfBlank()
                    rowData.collocation = row.cellValue(5).nullIfBlank()
                    rowData.relatedWords = row.cellValue(6).nullIfBlank()
                    rowData.note = row.cellValue(7).nullIfBlank()
                }

                if (rowData.word.isBlank() || rowData.meaning.isBlank()) continue
                    rows.add(rowData.build())
            }

            return rows
        }
    }

    private fun parseHeaderRow(row: Row): Map<String, Int> {
        val map = mutableMapOf<String, Int>()
        val lastCell = row.lastCellNum
        for (c in 0 until lastCell) {
            val header = row.cellValue(c) ?: continue
            val normalized = header.trim().lowercase().replace(" ", "")
            if (normalized.isBlank()) continue

            when (normalized) {
                "word" -> map["word"] = c
                "pronunciation" -> map["pronunciation"] = c
                "meaning" -> map["meaning"] = c
                "descriptionen", "description_en" -> map["descriptionen"] = c
                "example" -> map["example"] = c
                "collocation" -> map["collocation"] = c
                "relatedwords", "related_words" -> map["relatedwords"] = c
                "note" -> map["note"] = c
                "cefr", "cefrlevel" -> map["cefr"] = c
                "easefactor" -> map["easefactor"] = c
                "interval" -> map["interval"] = c
                "repetitions" -> map["repetitions"] = c
                "nextreviewdate" -> map["nextreviewdate"] = c
                "lastreviewed" -> map["lastreviewed"] = c
            }
        }
        return map
    }

    fun buildCsv(words: List<Word>): String {
        val sb = StringBuilder()
        sb.append(exportHeaders.joinToString(",") { escapeCsvCell(it) }).append('\n')
        for (w in words) {
            val cells = listOf(
                w.word,
                w.meaning,
                w.pronunciation,
                w.descriptionEn,
                w.example,
                w.collocation,
                w.relatedWords,
                w.note,
                w.cefrLevel,
                w.easeFactor,
                w.interval,
                w.repetitions,
                w.nextReviewDate,
                w.lastReviewed,
            )
            sb.append(cells.joinToString(",") { escapeCsvCell(it) }).append('\n')
        }
        return sb.toString()
    }

    fun buildXlsx(words: List<Word>): ByteArray {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Vocabulary")

        val headerRow = sheet.createRow(0)
        for (i in exportHeaders.indices) {
            headerRow.createCell(i).setCellValue(exportHeaders[i])
        }

        for (r in words.indices) {
            val row = sheet.createRow(r + 1)
            val w = words[r]
            row.createCell(0).setCellValue(w.word)
            row.createCell(1).setCellValue(w.meaning)
            row.createCell(2).setCellValue(w.pronunciation.orEmpty())
            row.createCell(3).setCellValue(w.descriptionEn.orEmpty())
            row.createCell(4).setCellValue(w.example.orEmpty())
            row.createCell(5).setCellValue(w.collocation.orEmpty())
            row.createCell(6).setCellValue(w.relatedWords.orEmpty())
            row.createCell(7).setCellValue(w.note.orEmpty())
            row.createCell(8).setCellValue(w.cefrLevel.orEmpty())
            row.createCell(9).setCellValue(w.easeFactor)
            row.createCell(10).setCellValue(w.interval.toDouble())
            row.createCell(11).setCellValue(w.repetitions.toDouble())
            row.createCell(12).setCellValue(w.nextReviewDate.toDouble())
            row.createCell(13).setCellValue((w.lastReviewed ?: 0L).toDouble())
        }

        val out = ByteArrayOutputStream()
        workbook.use { it.write(out) }
        return out.toByteArray()
    }

    private class VocabRowBuilder {
        var word: String = ""
        var pronunciation: String? = null
        var meaning: String = ""
        var descriptionEn: String? = null
        var example: String? = null
        var collocation: String? = null
        var relatedWords: String? = null
        var note: String? = null
        var cefrLevel: String? = null
        var easeFactor: Double? = null
        var interval: Int? = null
        var repetitions: Int? = null
        var nextReviewDate: Long? = null
        var lastReviewed: Long? = null

        fun build() = VocabRow(
            word = word.trim(),
            pronunciation = pronunciation,
            meaning = meaning.trim(),
            descriptionEn = descriptionEn,
            example = example,
            collocation = collocation,
            relatedWords = relatedWords,
            note = note,
            cefrLevel = cefrLevel?.trim()?.uppercase()?.ifBlank { null },
            easeFactor = easeFactor,
            interval = interval,
            repetitions = repetitions,
            nextReviewDate = nextReviewDate,
            lastReviewed = lastReviewed,
        )
    }
}

private fun String?.nullIfBlank(): String? = this?.trim()?.takeIf { it.isNotBlank() }
private fun String?.orEmpty(): String = this ?: ""

private fun Row.isBlank(): Boolean {
    val lastCell = this.lastCellNum
    for (c in 0 until lastCell) {
        val v = cellValue(c)
        if (!v.isNullOrBlank()) return false
    }
    return true
}

private fun Row.cellValue(index: Int): String? {
    val cell = getCell(index) ?: return null
    return when (cell.cellType) {
        org.apache.poi.ss.usermodel.CellType.STRING -> cell.stringCellValue
        org.apache.poi.ss.usermodel.CellType.NUMERIC -> cell.numericCellValue.toString()
        org.apache.poi.ss.usermodel.CellType.BOOLEAN -> cell.booleanCellValue.toString()
        else -> cell.toString()
    }
}

private fun escapeCsvCell(value: Any?): String {
    val s = value?.toString() ?: ""
    val needsQuotes = s.contains(',') || s.contains('"') || s.contains('\n') || s.contains('\r')
    if (!needsQuotes) return s
    val escaped = s.replace("\"", "\"\"")
    return "\"$escaped\""
}

private fun parseCsvLine(line: String): List<String> {
    val out = mutableListOf<String>()
    val sb = StringBuilder()
    var inQuotes = false
    var i = 0
    while (i < line.length) {
        val c = line[i]
        when {
            c == '"' -> {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                    // Escaped quote.
                    sb.append('"')
                    i += 2
                    continue
                }
                inQuotes = !inQuotes
            }
            c == ',' && !inQuotes -> {
                out.add(sb.toString())
                sb.setLength(0)
            }
            else -> sb.append(c)
        }
        i++
    }
    out.add(sb.toString())
    return out
}

