package com.example.lordseg.Model;

public class MessageModel {
    public Long id;
    public ConversationModel conversation;
    public Long senderId;
    public String content;
    public String timestamp;
    public boolean viewed; // <-- ADICIONADO: Mapeia se a mensagem foi lida ou não
}