package com.example.aulasegura.ui.screens.admin

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * Turns the reports screen state into a real, paginated PDF.
 *
 * The content builder is pure so a unit test can pin the numbers without an
 * emulator; the Canvas rendering and the Android share intent stay in the object
 * below. No third-party PDF library is needed: [PdfDocument] ships with the
 * platform (API 19+, this app's minSdk is 24).
 */

private const val PAGE_WIDTH = 595
private const val PAGE_HEIGHT = 842
private const val MARGIN = 48f
private const val LINE_HEIGHT = 20f
private const val TITLE_TEXT_SIZE = 18f
private const val BODY_TEXT_SIZE = 11f

/** Plain lines of the report, in the order they are drawn. Blank lines separate sections. */
fun reportPdfLines(state: AdminReportsUiState): List<String> {
    val lines = mutableListOf<String>()
    lines += "Reporte de asistencia"
    lines += "Periodo: ${rangeLabel(state.fromDate, state.toDate)}"
    lines += ""

    val summary = state.summary
    if (summary == null) {
        lines += "Sin datos de asistencia para el periodo seleccionado."
        return lines
    }

    lines += "Resumen"
    lines += "Asistencia: ${summary.attendancePercentage}%"
    lines += "Sesiones: ${summary.sessionCount}"
    lines += "Presentes: ${summary.presentCount} · Faltas: ${summary.absentCount} · " +
        "Retardos: ${summary.lateCount} · Justificadas: ${summary.justifiedCount}"
    lines += "Total de registros: ${summary.totalMarks}"
    lines += ""

    lines += "Por grupo"
    val groups = sortedGroups(state.groups)
    if (groups.isEmpty()) {
        lines += "Sin desglose por grupo."
    } else {
        groups.forEach { group ->
            val label = group.groupLabel.ifBlank { group.groupId }
            lines += "$label — ${group.attendancePercentage}% · ${group.absentCount} faltas · " +
                "${group.sessionCount} sesiones"
        }
    }
    lines += ""

    lines += "Días con más faltas"
    val days = sortedTopDays(state.topDays).take(5)
    if (days.isEmpty()) {
        lines += "Sin días con faltas registradas."
    } else {
        days.forEach { day ->
            val sessions = if (day.sessionCount == 1) "sesión" else "sesiones"
            lines += "${formatIsoDate(day.date)} — ${day.absentCount} faltas en " +
                "${day.sessionCount} $sessions"
        }
    }
    return lines
}

object ReportPdfExporter {

    /** Writes the PDF and hands it to a viewer. Any failure is a silent no-op. */
    fun export(context: Context, state: AdminReportsUiState) {
        val file = write(context, state) ?: return
        open(context, file)
    }

    /** Renders the lines into `cacheDir/reportes/reporte-<periodo>.pdf`. */
    fun write(context: Context, state: AdminReportsUiState): File? = try {
        val directory = File(context.cacheDir, "reportes").apply { mkdirs() }
        // One export at a time: old files would otherwise accumulate in the cache.
        directory.listFiles { file ->
            file.name.startsWith("reporte-") && file.extension == "pdf"
        }?.forEach { it.delete() }

        val file = File(directory, "reporte-${periodSuffix(state)}.pdf")
        val document = PdfDocument()
        try {
            val titlePaint = Paint().apply {
                color = Color.BLACK
                textSize = TITLE_TEXT_SIZE
                isFakeBoldText = true
            }
            val bodyPaint = Paint().apply {
                color = Color.BLACK
                textSize = BODY_TEXT_SIZE
            }

            var pageNumber = 1
            var page = document.startPage(
                PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create(),
            )
            var canvas: Canvas = page.canvas
            var y = MARGIN

            reportPdfLines(state).forEachIndexed { index, line ->
                val paint = if (index == 0) titlePaint else bodyPaint
                if (y + LINE_HEIGHT > PAGE_HEIGHT - MARGIN) {
                    document.finishPage(page)
                    pageNumber += 1
                    page = document.startPage(
                        PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create(),
                    )
                    canvas = page.canvas
                    y = MARGIN
                }
                canvas.drawText(line, MARGIN, y, paint)
                y += LINE_HEIGHT
            }
            document.finishPage(page)

            FileOutputStream(file).use { document.writeTo(it) }
        } finally {
            document.close()
        }
        file
    } catch (error: Exception) {
        null
    }

    private fun periodSuffix(state: AdminReportsUiState): String {
        val from = state.fromDate.ifBlank { "sin-desde" }
        val to = state.toDate.ifBlank { "sin-hasta" }
        return "${from}_al_$to".replace(Regex("[^A-Za-z0-9_-]"), "-")
    }

    private fun open(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        // An explicit grant covers viewers that do not honour the intent flag.
        intent.resolveActivity(context.packageManager)?.packageName?.let { target ->
            context.grantUriPermission(target, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { context.startActivity(Intent.createChooser(intent, "Abrir reporte")) }
    }
}
