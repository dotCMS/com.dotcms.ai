package com.dotcms.ai.model;

import java.net.URL;
import java.util.List;

public class ChatGptImageResponseDTO {

    private long created;
    private List<ImageData> data;

    public ChatGptImageResponseDTO() {
    }

    public ChatGptImageResponseDTO(long created, List<ImageData> data) {
        this.created = created;
        this.data = data;
    }

    public long getCreated() {
        return created;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    public List<ImageData> getData() {
        return data;
    }

    public void setData(List<ImageData> data) {
        this.data = data;
    }

    // Inner class for the nested "data" structure
    public static class ImageData {
        private URL url;

        public URL getUrl() {
            return url;
        }

        public void setUrl(URL url) {
            this.url = url;
        }
    }

}
