package com.example.server.domain;

import com.example.server.dto.UserDto;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
public class ChatMessage {
    private MessageType messageType;
    private String content;
    private String sender;
    private String roomId;
    public enum MessageType{
        CHAT,
        JOIN,
        LEAVE
    }
}
