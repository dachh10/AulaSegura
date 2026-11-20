package com.example.aulasegura.ui.screens.teacher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aulasegura.domain.model.ChatConversation
import com.example.aulasegura.domain.model.ChatMessage
import com.example.aulasegura.domain.model.ChatSendOutcome
import com.example.aulasegura.domain.model.otherParticipant
import com.example.aulasegura.domain.repository.AuthRepository
import com.example.aulasegura.domain.repository.ChatRepository
import com.example.aulasegura.ui.chat.clockLabel
import java.util.TimeZone
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * One conversation: messages from RTDB, header from the Firestore summary.
 *
 * The messages are the store of truth, so the screen renders whatever RTDB has —
 * including a write still queued on the phone, because RTDB emits local events.
 * That is what makes the bubble appear in airplane mode instead of after the
 * round trip.
 *
 * The conversation id arrives with [open] and not with the constructor: the
 * container builds every ViewModel from its class, and navigation arguments are
 * not available there.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TeacherConversationViewModel(
    private val chat: ChatRepository,
    private val auth: AuthRepository,
    private val zone: TimeZone = TimeZone.getDefault(),
) : ViewModel() {

    private val conversationId = MutableStateFlow<String?>(null)
    private val draft = MutableStateFlow("")
    private val sending = MutableStateFlow(false)
    private val notice = MutableStateFlow<String?>(null)
    private var presenceStarted: String? = null

    private val header: Flow<ChatConversation?> = conversationId.flatMapLatest { id ->
        if (id == null) {
            flowOf(null)
        } else {
            chat.observeConversations()
                .map { conversations -> conversations.firstOrNull { it.id == id } }
                .catch { emit(null) }
        }
    }

    private val messages: Flow<List<TeacherChatMessageUi>> = conversationId.flatMapLatest { id ->
        if (id == null) {
            flowOf(emptyList())
        } else {
            val myUid = auth.currentUser?.uid.orEmpty()
            chat.observeMessages(id)
                .map { list -> list.map { it.toUi(myUid) } }
                .catch { emit(emptyList()) }
        }
    }

    private val online: Flow<Boolean> = conversationId.flatMapLatest { id ->
        val myUid = auth.currentUser?.uid.orEmpty()
        val other = id?.let { otherParticipant(it, myUid) }
        if (other == null) {
            flowOf(false)
        } else {
            chat.observeOnline(id, other).catch { emit(false) }
        }
    }

    private data class Composer(
        val draft: String,
        val sending: Boolean,
        val notice: String?,
    )

    private val composer = combine(draft, sending, notice) { text, isSending, message ->
        Composer(draft = text, sending = isSending, notice = message)
    }

    val state: StateFlow<TeacherConversationUi> =
        combine(header, messages, online, composer) { conversation, list, isOnline, input ->
            TeacherConversationUi(
                title = conversation?.title ?: "Conversación",
                subtitle = conversation?.subtitle.orEmpty(),
                online = isOnline,
                messages = list,
                draft = input.draft,
                sending = input.sending,
                notice = input.notice,
                loading = false,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_MILLIS),
            initialValue = TeacherConversationUi(),
        )

    /**
     * Wires the screen to [id]. Idempotent: a recomposition does not re-send the
     * read receipt nor announce presence twice.
     */
    fun open(id: String) {
        if (conversationId.value == id) return
        conversationId.value = id
        presenceStarted = id

        viewModelScope.launch {
            chat.markRead(id)
            chat.goOnline(id)
        }
    }

    fun setDraft(value: String) {
        if (sending.value) return
        draft.value = value.take(MAX_TEXT)
        notice.value = null
    }

    fun send() {
        val id = conversationId.value ?: return
        if (sending.value) return

        val text = draft.value.trim()
        if (text.isEmpty()) return

        sending.value = true
        notice.value = null

        viewModelScope.launch {
            when (val outcome = chat.sendMessage(id, text)) {
                // The bubble comes back from RTDB, not from here, so the screen
                // never shows a message the database refused.
                ChatSendOutcome.Sent -> draft.value = ""
                ChatSendOutcome.Queued -> {
                    draft.value = ""
                    notice.value = "Sin conexión: se enviará al reconectar."
                }
                ChatSendOutcome.Empty -> Unit
                ChatSendOutcome.SignedOut ->
                    notice.value = "Tu sesión expiró. Vuelve a iniciar sesión."
                is ChatSendOutcome.Failed ->
                    notice.value = "No se pudo enviar. Inténtalo de nuevo."
            }
            sending.value = false
        }
    }

    override fun onCleared() {
        // `onDisconnect` covers a dead app; this covers "the teacher left the
        // screen". The scope is detached because the ViewModel's own scope is
        // already cancelled at this point.
        val id = presenceStarted
        if (id != null) {
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch { chat.goOffline(id) }
        }
        super.onCleared()
    }

    private fun ChatMessage.toUi(myUid: String): TeacherChatMessageUi = TeacherChatMessageUi(
        id = id,
        text = text,
        mine = isMine(myUid),
        time = clockLabel(sentAt, zone),
    )

    private companion object {
        const val STOP_MILLIS = 5_000L
        const val MAX_TEXT = 2000
    }
}
