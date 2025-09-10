package com.example.animelib;

import java.util.List;

public class ToastData {
    private String type;
    private String message;
    private List<ButtonData> buttons;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<ButtonData> getButtons() {
        return buttons;
    }

    public void setButtons(List<ButtonData> buttons) {
        this.buttons = buttons;
    }
}
