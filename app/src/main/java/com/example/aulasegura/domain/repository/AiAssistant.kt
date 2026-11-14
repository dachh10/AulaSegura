package com.example.aulasegura.domain.repository

/**
 * The tutor-facing assistant, as the rest of the app sees it.
 *
 * The contract is a question and an answer, not a chat session: the history
 * lives in the ViewModel only (CONTEXT §9.2), so nothing here needs to remember
 * previous turns and no conversation is ever persisted.
 *
 * Failures are typed because they are not equivalent to the user. "You hit the
 * free-tier limit" and "the school turned the assistant off" both end in no
 * answer, but only one is worth retrying, so the UI must be able to tell them
 * apart without parsing an English error message from the SDK.
 */
interface AiAssistant {

    /**
     * Whether the assistant may answer right now.
     *
     * Backed by `ai_assistant_enabled` in Remote Config. It is checked before
     * every query and not cached: switching the assistant off must take effect
     * on the next question, not on the next app start.
     */
    fun isAvailable(): Boolean

    /** Sends [prompt] and waits for the answer. Never throws for an expected failure. */
    suspend fun ask(prompt: String): AiAnswer
}

sealed interface AiAnswer {

    /** [text] is already trimmed and guaranteed non-empty. */
    data class Answer(val text: String) : AiAnswer

    data class Failure(val reason: AiFailure) : AiAnswer
}

/**
 * Why the assistant could not answer.
 *
 * [userMessage] is the copy the UI shows. It lives here, next to the reason, so
 * a new failure cannot be added without deciding what the tutor reads.
 */
enum class AiFailure(val userMessage: String) {

    /** The `ai_assistant_enabled` flag is off. */
    DISABLED("El asistente no está disponible en este momento."),

    /** Free-tier quota exhausted (HTTP 429 `RESOURCE_EXHAUSTED`). */
    RATE_LIMITED("El asistente alcanzó su límite de consultas. Intenta de nuevo en unos minutos."),

    /** No API key, API disabled in the project, or App Check rejected the request. */
    NOT_CONFIGURED("El asistente no está configurado. Avisa a la escuela."),

    /** Safety filters refused the prompt or the answer. */
    BLOCKED("No puedo responder a esa pregunta. Intenta reformularla."),

    /** Empty question after trimming. */
    EMPTY_PROMPT("Escribe una pregunta para el asistente."),

    /**
     * The request outlived the answer budget.
     *
     * Distinct from [NETWORK]: the device had a connection and the request was
     * accepted, but no answer came back in time. Telling the user to check their
     * internet would send them to look for a problem that is not there.
     */
    TIMEOUT("El asistente tardó demasiado en responder. Intenta de nuevo."),

    /** The model answered with no usable text. */
    EMPTY_RESPONSE("El asistente no devolvió una respuesta. Intenta de nuevo."),

    NETWORK("Sin conexión. Revisa tu internet e intenta de nuevo."),

    UNKNOWN("No pudimos contactar al asistente. Intenta de nuevo en un rato."),
}
