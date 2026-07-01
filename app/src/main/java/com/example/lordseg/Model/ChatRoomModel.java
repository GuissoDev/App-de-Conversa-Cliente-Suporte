package com.example.lordseg.Model;

import java.util.List;
import java.util.Map;

/**
 * ChatRoomModel: Representa uma sala de conversa.
 * Refatorado para remover dependência do Firebase Timestamp.
 */
public class ChatRoomModel {
    private String chatRoomId;
    private List<String> userIDs;
    private String lastMessageTimestamp; // Mudado para String
    private String lastMessageSenderId;
    private String lastMessage;
    private Map<String, Integer> unreadCount;

    public ChatRoomModel() {
    }

    public ChatRoomModel(String chatRoomId, List<String> userIDs, String lastMessageTimestamp, String lastMessageSenderId) {
        this.chatRoomId = chatRoomId;
        this.userIDs = userIDs;
        this.lastMessageTimestamp = lastMessageTimestamp;
        this.lastMessageSenderId = lastMessageSenderId;
    }

    public String getChatRoomId() {
        return chatRoomId;
    }

    public void setChatRoomId(String chatRoomId) {
        this.chatRoomId = chatRoomId;
    }

    public List<String> getUserIDs() {
        return userIDs;
    }

    public void setUserIDs(List<String> userIDs) {
        this.userIDs = userIDs;
    }

    public String getLastMessageTimestamp() {
        return lastMessageTimestamp;
    }

    public void setLastMessageTimestamp(String lastMessageTimestamp) {
        this.lastMessageTimestamp = lastMessageTimestamp;
    }

    public String getLastMessageSenderId() {
        return lastMessageSenderId;
    }

    public void setLastMessageSenderId(String lastMessageSenderId) {
        this.lastMessageSenderId = lastMessageSenderId;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public Map<String, Integer> getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(Map<String, Integer> unreadCount) {
        this.unreadCount = unreadCount;
    }
}
