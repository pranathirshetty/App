package to.kuudere.anisuge.data.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class WebSocketMessage(
    val event: String,
    val data: JsonElement? = null
)

@Serializable
data class JoinRoomData(
    val room: String,
    val user: UserInfoData
)

@Serializable
data class UserInfoData(
    val userId: String,
    val username: String,
    val avatar: String? = null
)
