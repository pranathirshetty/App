package to.kuudere.anisuge.data.services

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.url
import io.ktor.client.request.header
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import to.kuudere.anisuge.data.models.JoinRoomData
import to.kuudere.anisuge.data.models.UserInfoData
import to.kuudere.anisuge.data.models.WebSocketMessage

class RealtimeService(
    private val httpClient: HttpClient,
    private val authService: AuthService
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var session: WebSocketSession? = null
    private var currentRoom: String? = null
    private var currentUser: UserInfoData? = null

    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }

    private val wsUrl = "wss://realtime.kuudere.to/ws"

    fun connect(user: UserInfoData) {
        if (currentUser?.userId == user.userId && _isConnected.value) return
        
        currentUser = user
        
        scope.launch {
            try {
                val token = authService.getWsToken()
                if (token == null) {
                    println("[RealtimeService] No WS token available, skipping connection")
                    _isConnected.value = false
                    return@launch
                }
                
                session = httpClient.webSocketSession {
                    url(wsUrl)
                    url {
                        parameters.append("token", token)
                        parameters.append("userId", user.userId)
                        parameters.append("username", user.username)
                        user.avatar?.let { parameters.append("avatar", it) }
                    }
                    header("Origin", "https://kuudere.to")
                }
                
                _isConnected.value = true
                println("[RealtimeService] Connected to WebSocket")

                // Re-join current room if we had one
                currentRoom?.let { room ->
                    sendJoinRoom(room, user)
                }

                listen()
            } catch (e: Exception) {
                val msg = e.message ?: ""
                println("[RealtimeService] Connection failed: $msg")
                _isConnected.value = false
                
                // If unauthorized or forbidden, don't retry immediately
                if (msg.contains("403") || msg.contains("401")) {
                    println("[RealtimeService] Unauthorized (403/401), stopping auto-retry")
                    return@launch
                }
                
                delay(10000) // Longer delay before retry
                connect(user)
            }
        }
    }

    private suspend fun listen() {
        session?.incoming?.receiveAsFlow()?.collect { frame ->
            if (frame is Frame.Text) {
                val text = frame.readText()
                // Handle messages if needed
                // println("[RealtimeService] Received: $text")
            }
        }
        _isConnected.value = false
        println("[RealtimeService] Disconnected")
    }

    fun joinRoom(room: String) {
        if (currentRoom == room) return
        
        currentRoom = room
        val user = currentUser ?: return
        
        if (_isConnected.value) {
            scope.launch {
                sendJoinRoom(room, user)
            }
        }
    }

    private suspend fun sendJoinRoom(room: String, user: UserInfoData) {
        val message = WebSocketMessage(
            event = "join_room",
            data = json.encodeToJsonElement(JoinRoomData(room, user))
        )
        val text = json.encodeToString(message)
        session?.send(Frame.Text(text))
        println("[RealtimeService] Joined room: $room")
    }

    fun disconnect() {
        scope.launch {
            session?.close()
            session = null
            _isConnected.value = false
            currentRoom = null
            currentUser = null
        }
    }
}
