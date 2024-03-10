package com.example.data

import io.ktor.websocket.*
import kotlinx.serialization.Serializable
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId


@Serializable
data class UserData(
    @BsonId val userId : String ,//google id
    val userName : String,
    val displayName : String? = null,
    val profileUri : String? = null,

)
@Serializable
data class ChatInfo(
    @BsonId val userId : String,//google id
    var chatList : List<ChatDetail> = emptyList()

)
@Serializable
data class ChatDetail(
    @BsonId val chatId : String, //user chat id
    val sender : String,
    val receiver : String,
    val receiverName: String? = null,
    val receiverPic: String? = null,
    val lastMessage : String ? = null,
    val timeStamp : Long? = null
)

@Serializable
data class Member(
    val id : String,
    val userName : String,
    val sessionId : String,
    val socket : WebSocketSession
)
@Serializable
data class PeerRoom(
    val userId: String,
    val sessionId : String,
    val socket : WebSocketSession
)
@Serializable
data class PeerRes(
    val userId: String,
    val userId2: String = "Noting",
)


@Serializable
data class UserMessage(
    @BsonId
    val id: String = ObjectId().toString(),
    val senderId : String,
    val recipientId: String,
    val message : String ,
    val timeStamp : Long
)

@Serializable
data class Chats(
     @BsonId val chatId : String,
    val message: List<UserMessage>
)

@Serializable
data class RtcObject(
    val type: String,
    val sender: String? = null,
    val target: String? = null,
    val sdp: String? = null,
    val offer: HashMap<String, String>? = null,
    val iceCandidateModel: IceCandidateModel? = null
)

@Serializable
data class IceCandidateModel(
    val sdpMid:String ? = null,
    val sdpMLineIndex:Double  ? = null,
    val sdpCandidate:String ? = null
)
