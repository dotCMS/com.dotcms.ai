package com.dotcms.ai.model;

import java.util.ArrayList;
import java.util.List;

public class ChatGptRequestDTO {

    private String model;
    private List<Message> messages;

    public ChatGptRequestDTO(String model, String role, String prompt, String promptTextStyle, String promptImage, String promptInput, boolean rawPrompt) {
        String promptText = rawPrompt ? promptInput : (prompt + " " + promptTextStyle + " " + promptImage + " " + promptInput);
        this.model = model;
        this.messages = new ArrayList<>();
        this.messages.add(new Message(role, promptText));
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }
    // Constructors, getters, and setters (if needed)

    public static class Message {

        private String role;
        private String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

    }

}
