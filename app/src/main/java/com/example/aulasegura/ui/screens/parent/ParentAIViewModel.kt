package com.example.aulasegura.ui.screens.parent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aulasegura.domain.model.AssistantContext
import com.example.aulasegura.domain.repository.AiAnswer
import com.example.aulasegura.domain.repository.AiFailure
import com.example.aulasegura.domain.usecase.AssistantContextResult
import com.example.aulasegura.ui.chat.clockLabel
import java.util.TimeZone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The tutor's assistant: real children data, in-memory history, typed failures.
 *
 * Three rules from `CONTEXT.md` §9.2 shape this class:
 * - the history lives here and nothing is persisted;
 * - the button is blocked while a question is in flight;
 * - a rate limit is a message, never an automatic retry loop.
 *
 * The data load and the question are lambdas instead of concrete use cases so a
 * JVM test can pin the behaviour with three lines and no Firebase: what matters
 * here is the state machine, and the use cases already have their own tests.
 */
class ParentAIViewModel(
    private val currentUserName: () -> String,
    private val loadContext: suspend () -> AssistantContextResult,
    private val askAssistant: suspend (String, AssistantContext?) -> AiAnswer,
    private val isAvailable: () -> Boolean,
    private val now: () -> Long = System::currentTimeMillis,
    private val zone: TimeZone = TimeZone.getDefault(),
) : ViewModel() {

    private val _state = MutableStateFlow(ParentAIUi(welcome = welcomeFor(null)))
    val state: StateFlow<ParentAIUi> = _state.asStateFlow()

    private var context: AssistantContext? = null
    private var nextId = 1L

    init {
        refresh()
    }

    /**
     * Reads the children's data again.
     *
     * Called on open and from the retry the screen offers after a failure. It does
     * not clear the conversation: the questions already asked stay, and only the
     * data under them is refreshed.
     */
    fun refresh() {
        _state.update { it.copy(loading = true, contextError = null) }

        viewModelScope.launch {
            val available = isAvailable()
            when (val result = loadContext()) {
                is AssistantContextResult.Loaded -> {
                    context = result.context
                    _state.update {
                        it.copy(
                            loading = false,
                            faqOnly = !available,
                            welcome = welcomeFor(result.context),
                            contextError = null,
                        )
                    }
                }

                AssistantContextResult.NoChildren -> {
                    context = null
                    _state.update {
                        it.copy(
                            loading = false,
                            faqOnly = !available,
                            welcome = welcomeFor(null),
                            contextError = "Tu cuenta todavía no tiene alumnos vinculados.",
                        )
                    }
                }

                AssistantContextResult.SignedOut -> _state.update {
                    it.copy(
                        loading = false,
                        contextError = "Vuelve a iniciar sesión para usar el asistente.",
                    )
                }

                is AssistantContextResult.Failed -> _state.update {
                    it.copy(
                        loading = false,
                        contextError = homeFailureMessage(result.reason),
                    )
                }
            }
        }
    }

    fun setDraft(value: String) {
        if (_state.value.sending) return
        _state.update { it.copy(draft = value.take(MAX_TEXT), notice = null) }
    }

    /** Sends what the tutor typed. */
    fun send() {
        val text = _state.value.draft.trim()
        if (text.isEmpty()) return
        submit(question = null, text = text)
    }

    /** The three suggestions: the same sentence goes to the AI or to the FAQ. */
    fun ask(question: ParentAIQuestion) = submit(question = question, text = question.prompt)

    private fun submit(question: ParentAIQuestion?, text: String) {
        val current = _state.value
        if (current.sending) return

        if (current.loading) {
            _state.update { it.copy(notice = "Espera a que termine de cargar los datos.") }
            return
        }

        val data = context
        if (!current.faqOnly && data == null) {
            // Asking without data would get "sin registro" for everything, which
            // reads as "your child has nothing" instead of "I could not read it".
            _state.update {
                it.copy(
                    notice = current.contextError
                        ?: "No pude leer los datos de tus hijos. Intenta de nuevo.",
                )
            }
            return
        }

        if (current.faqOnly && question == null) {
            // A question with no predefined answer would be answered by nobody:
            // better to say it and leave the three suggestions as the way out.
            _state.update {
                it.copy(notice = "El asistente no está disponible. Puedes usar las sugerencias.")
            }
            return
        }

        appendQuestion(text)

        if (current.faqOnly || data == null) {
            answerLocally(question)
            return
        }

        _state.update { it.copy(sending = true, notice = null) }

        viewModelScope.launch {
            when (val answer = askAssistant(text, data)) {
                is AiAnswer.Answer -> {
                    appendAnswer(
                        text = answer.text,
                        tasks = assistantTasksFor(question, data, now(), zone),
                    )
                    _state.update { it.copy(sending = false) }
                }

                is AiAnswer.Failure -> {
                    // The question stays on screen and goes back to the composer:
                    // retrying is one tap, and the tutor does not lose what they
                    // wrote to a five-second hiccup.
                    _state.update {
                        it.copy(
                            sending = false,
                            notice = answer.reason.userMessage,
                            draft = text,
                            faqOnly = it.faqOnly || answer.reason.isPermanent(),
                        )
                    }
                }
            }
        }
    }

    /**
     * The offline path `CONTEXT.md` §9.2 requires: the same data, printed without
     * a model. Only a known suggestion has a predefined answer, so free text asks
     * for the suggestions instead of inventing one.
     */
    private fun answerLocally(question: ParentAIQuestion?) {
        if (question == null) {
            _state.update {
                it.copy(notice = "El asistente no está disponible. Puedes usar las sugerencias.")
            }
            return
        }

        appendAnswer(
            text = assistantFaqAnswer(question, context, now(), zone),
            tasks = assistantTasksFor(question, context, now(), zone),
        )
    }

    private fun appendQuestion(text: String) {
        val message = ParentAIMessageUi(
            id = nextId++,
            text = text,
            mine = true,
            time = clockLabel(now(), zone),
        )
        _state.update { it.copy(messages = it.messages + message, draft = "", notice = null) }
    }

    private fun appendAnswer(text: String, tasks: List<ParentAITaskUi>) {
        val message = ParentAIMessageUi(
            id = nextId++,
            text = text,
            mine = false,
            time = clockLabel(now(), zone),
            tasks = tasks,
        )
        _state.update { it.copy(messages = it.messages + message) }
    }

    private fun welcomeFor(data: AssistantContext?): String = assistantWelcome(
        fullName = currentUserName(),
        childNames = data?.children?.map { it.firstName }.orEmpty(),
    )

    private fun AiFailure.isPermanent(): Boolean =
        this == AiFailure.DISABLED || this == AiFailure.NOT_CONFIGURED

    private companion object {
        const val MAX_TEXT = 500
    }
}
