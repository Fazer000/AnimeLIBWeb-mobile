package com.example.animelib.models;

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

    /**
     * Вложенный класс для данных кнопки
     */
    public static class ButtonData {
        private String text;
        private String tag;
        private String target;
        private String rel;
        private String href;

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public String getTag() {
            return tag;
        }

        public void setTag(String tag) {
            this.tag = tag;
        }

        public String getTarget() {
            return target;
        }

        public void setTarget(String target) {
            this.target = target;
        }

        public String getRel() {
            return rel;
        }

        public void setRel(String rel) {
            this.rel = rel;
        }

        public String getHref() {
            return href;
        }

        public void setHref(String href) {
            this.href = href;
        }
    }
}
