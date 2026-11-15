package com.example.aulasegura.domain.model

/**
 * What the teacher reports, in the vocabulary Firestore accepts.
 *
 * The `wire` values are the exact strings of `CONTEXT.md` §6.2 and the Firestore
 * rules; the labels are only for the UI. The mockup prints "Leve / Moderado /
 * Grave", which is why the Spanish label lives next to the wire value instead of
 * being formatted at the call site.
 */
enum class IncidentSeverity(val wire: String, val label: String) {
    LEVE("leve", "Leve"),
    MODERADO("moderado", "Moderado"),
    GRAVE("grave", "Grave"),
}

/**
 * Incident category, picked by the teacher.
 *
 * [OTRO] is the default because "no sé en qué cajón entra" is a real answer and
 * the tutor still needs the incident; the other four match the admin filters.
 */
enum class IncidentCategory(val wire: String, val label: String) {
    DISCUSION("discusion", "Discusión"),
    CAIDA("caida", "Caída"),
    DANIO("danio", "Daño"),
    DESACATO("desacato", "Desacato"),
    OTRO("otro", "Otro"),
}

/** Where the incident happened, as the teacher placed the pin. */
data class IncidentLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Double? = null,
    val distanceMeters: Double? = null,
    val verified: Boolean = false,
)

/**
 * A new incident, before it has an id.
 *
 * [studentName] and [reportedByName] are denormalised on purpose: the tutor can
 * read their own incidents but not the school roster, so the name has to travel
 * inside the document (`FASE1.md`, bloque 6).
 */
data class NewIncident(
    val groupId: String,
    val studentId: String,
    val studentName: String,
    val title: String,
    val description: String,
    val severity: IncidentSeverity,
    val occurredAt: Long,
    val location: IncidentLocation? = null,
    val category: IncidentCategory = IncidentCategory.OTRO,
)

/** Outcome of queueing an incident. */
sealed interface IncidentSaveOutcome {

    /** Written to Room and queued; [incidentId] is the Firestore document id. */
    data class Queued(val incidentId: String) : IncidentSaveOutcome

    data object SignedOut : IncidentSaveOutcome

    /** The student was not chosen; the schema has no anonymous incident. */
    data object NoStudent : IncidentSaveOutcome

    /** The description is the only thing that explains the incident. */
    data object EmptyDescription : IncidentSaveOutcome

    /** The group is not in the local cache, so its `schoolId` cannot be trusted. */
    data class NoGroup(val groupId: String) : IncidentSaveOutcome

    /** The local write failed; nothing was queued. */
    data class Failed(val groupId: String) : IncidentSaveOutcome
}
