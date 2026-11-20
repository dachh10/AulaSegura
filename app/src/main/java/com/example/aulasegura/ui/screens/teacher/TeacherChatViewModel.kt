package com.example.aulasegura.ui.screens.teacher

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
 * The chat list: conversations from Firestore, the "new message" sheet from the
 * roster.
 *
 * Search and the group chip are filtered here and not in the screen so the same
 * rules can be pinned by a test. Nothing is cached in Room: RTDB brings its own
 * disk cache and the conversation summaries are cheap to re-read.
 */
class TeacherChatViewModel(
    private val chat: ChatRepository,
    private val now: () -> Long = System::currentTimeMillis,
    private val zone: TimeZone = TimeZone.getDefault(),
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(CHAT_ALL_FILTER)
    private val contacts = MutableStateFlow<List<ChatContact>>(emptyList())
    private val contactsOpen = MutableStateFlow(false)
    private val contactsLoading = MutableStateFlow(false)
    private val loading = MutableStateFlow(true)
    private val notice = MutableStateFlow<String?>(null)

    /** Set when a conversation is ready to open; the screen consumes and clears it. */
    private val _openedConversation = MutableStateFlow<String?>(null)
    val openedConversation: StateFlow<String?> = _openedConversation

    private val rows: StateFlow<List<TeacherChatRowUi>> = chat.observeConversations()
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

    private val filters: StateFlow<List<String>> = rows
        .map { chatFiltersFor(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MILLIS), listOf(CHAT_ALL_FILTER))

    private data class Sheet(
        val contacts: List<TeacherContactUi>,
        val open: Boolean,
        val loadingContacts: Boolean,
        val notice: String?,
        val loading: Boolean,
    )

    private val sheet = combine(contacts, contactsOpen, contactsLoading, notice, loading) {
            loadedContacts,
            isOpen,
            isLoadingContacts,
            message,
            isLoading,
        ->
        Sheet(
            contacts = loadedContacts.map { contactRowOf(it) },
            open = isOpen,
            loadingContacts = isLoadingContacts,
            notice = message,
            loading = isLoading,
        )
    }

    val state: StateFlow<TeacherChatUi> =
        combine(rows, filters, query, filter, sheet) { allRows, chips, term, chip, panel ->
            val effectiveChip = if (chip in chips) chip else CHAT_ALL_FILTER
            TeacherChatUi(
                rows = filterChatRows(allRows, term, effectiveChip),
                filters = chips,
                activeFilter = effectiveChip,
                query = term,
                loading = panel.loading,
                contacts = panel.contacts,
                contactsOpen = panel.open,
                contactsLoading = panel.loadingContacts,
                notice = panel.notice,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_MILLIS),
            initialValue = TeacherChatUi(),
        )

    fun setQuery(value: String) {
        query.value = value
    }

    fun setFilter(value: String) {
        filter.value = value
    }

    fun openContacts() {
        if (contacts.value.isNotEmpty()) {
            contactsOpen.value = true
            return
        }

        contactsOpen.value = true
        contactsLoading.value = true
        notice.value = null

        viewModelScope.launch {
            val loaded = runCatching { chat.contacts() }.getOrDefault(emptyList())
            contacts.value = loaded
            contactsLoading.value = false
            if (loaded.isEmpty()) {
                notice.value = "No hay tutores vinculados en tus grupos."
            }
        }
    }

    fun dismissContacts() {
        contactsOpen.value = false
    }

    fun startConversation(otherUid: String) {
        val contact = contacts.value.firstOrNull { it.otherUid == otherUid }
        if (contact == null) {
            notice.value = "Ese tutor ya no aparece en tus grupos."
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

    private fun ChatConversation.toRow(): TeacherChatRowUi = chatRowOf(this, now(), zone)

    private companion object {
        const val STOP_MILLIS = 5_000L
    }
}
