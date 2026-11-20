package com.example.aulasegura.ui.screens.parent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aulasegura.domain.model.ChatContact
import com.example.aulasegura.domain.model.ChatConversation
import com.example.aulasegura.domain.model.ChatOpenOutcome
import com.example.aulasegura.domain.repository.ChatRepository
import java.util.TimeZone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The tutor's chat list: conversations from Firestore, the "new message" sheet
 * from the teachers of their children's groups.
 *
 * Search is filtered here and not in the screen so the same rule can be pinned by
 * a test. Nothing is cached in Room: RTDB brings its own disk cache and the
 * summaries are cheap to re-read.
 */
class ParentChatViewModel(
    private val chat: ChatRepository,
    private val now: () -> Long = System::currentTimeMillis,
    private val zone: TimeZone = TimeZone.getDefault(),
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val contacts = MutableStateFlow<List<ChatContact>>(emptyList())
    private val contactsOpen = MutableStateFlow(false)
    private val contactsLoading = MutableStateFlow(false)
    private val loading = MutableStateFlow(true)
    private val notice = MutableStateFlow<String?>(null)

    /** Set when a conversation is ready to open; the screen consumes and clears it. */
    private val _openedConversation = MutableStateFlow<String?>(null)
    val openedConversation: StateFlow<String?> = _openedConversation

    private val rows: StateFlow<List<ParentChatRowUi>> = chat.observeConversations()
        .onEach { loading.value = false }
        .map { conversations -> conversations.map { it.toRow() } }
        .catch {
            // A denied query (deploying indexes) or no network shows the empty
            // state with a sentence, not a crash and not a spinner forever.
            loading.value = false
            notice.value = "No se pudo cargar el chat. Revisa tu conexión."
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MILLIS), emptyList())

    private data class Sheet(
        val contacts: List<ParentTeacherUi>,
        val open: Boolean,
        val loadingContacts: Boolean,
    )

    private val sheet = combine(contacts, contactsOpen, contactsLoading) { loaded, open, isLoading ->
        Sheet(
            contacts = loaded.map { parentTeacherRowOf(it) },
            open = open,
            loadingContacts = isLoading,
        )
    }

    val state: StateFlow<ParentChatUi> =
        combine(rows, query, sheet, loading, notice) { allRows, term, panel, isLoading, message ->
            ParentChatUi(
                rows = filterParentChatRows(allRows, term),
                query = term,
                loading = isLoading,
                contacts = panel.contacts,
                contactsOpen = panel.open,
                contactsLoading = panel.loadingContacts,
                notice = message,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_MILLIS),
            initialValue = ParentChatUi(),
        )

    fun setQuery(value: String) {
        query.value = value
    }

    fun openContacts() {
        contactsOpen.value = true
        if (contacts.value.isNotEmpty()) return

        contactsLoading.value = true
        notice.value = null

        viewModelScope.launch {
            val loaded = runCatching { chat.tutorContacts() }.getOrDefault(emptyList())
            contacts.value = loaded
            contactsLoading.value = false
            if (loaded.isEmpty()) {
                notice.value = "No hay docentes asignados al grupo de tus hijos."
            }
        }
    }

    fun dismissContacts() {
        contactsOpen.value = false
    }

    fun startConversation(otherUid: String) {
        val contact = contacts.value.firstOrNull { it.otherUid == otherUid }
        if (contact == null) {
            notice.value = "Ese docente ya no aparece en el grupo."
            return
        }

        viewModelScope.launch {
            when (val outcome = chat.ensureConversation(contact)) {
                is ChatOpenOutcome.Ready -> {
                    contactsOpen.value = false
                    _openedConversation.value = outcome.conversationId
                }
                ChatOpenOutcome.SignedOut ->
                    notice.value = "Tu sesión expiró. Vuelve a iniciar sesión."
                is ChatOpenOutcome.Failed ->
                    notice.value = "No se pudo abrir el chat. Inténtalo de nuevo."
            }
        }
    }

    /** The screen calls this once it navigated, so a rotation does not reopen it. */
    fun openedHandled() {
        _openedConversation.value = null
    }

    private fun ChatConversation.toRow(): ParentChatRowUi = parentChatRowOf(this, now(), zone)

    private companion object {
        const val STOP_MILLIS = 5_000L
    }
}
