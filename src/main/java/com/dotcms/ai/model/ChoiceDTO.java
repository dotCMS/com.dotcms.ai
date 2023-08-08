package com.dotcms.ai.model;

public class ChoiceDTO {

    private int index;
    private AssistantMessageDTO message;
    private String finish_reason;

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public AssistantMessageDTO getMessage() {
        return message;
    }

    public void setMessage(AssistantMessageDTO message) {
        this.message = message;
    }

    public String getFinish_reason() {
        return finish_reason;
    }

    public void setFinish_reason(String finish_reason) {
        this.finish_reason = finish_reason;
    }
}
