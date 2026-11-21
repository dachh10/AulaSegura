package com.example.aulasegura.ui.screens.admin

import com.example.aulasegura.domain.model.AdminReportRangeSummary

/** Radius of the hero progress ring in the Stitch mockup (viewBox 88). */
private const val RING_RADIUS = 36.0

/** dashoffset for the ring: full circumference at 0%, none at 100%. */
fun ringOffset(percentage: Int): Double {
    val circumference = 2 * Math.PI * RING_RADIUS
    val clamped = percentage.coerceIn(0, 100)
    return circumference - circumference * (clamped / 100.0)
}

/** Legend rows from the real day summary, in the mockup's order and colors. */
fun attendanceLegend(summary: AdminReportRangeSummary): List<String> = listOf(
    "${summary.presentCount} presentes",
    "${summary.absentCount} faltas",
    "${summary.lateCount} retardos",
)
