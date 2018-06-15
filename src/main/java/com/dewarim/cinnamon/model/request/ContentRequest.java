package com.dewarim.cinnamon.model.request;

public class ContentRequest {

    private String token;
    private Long id;

    public ContentRequest() {
    }

    public ContentRequest(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    
    public boolean validated(){
        return id != null && id > 0 && token != null && token.trim().length() > 0;
    }
}
