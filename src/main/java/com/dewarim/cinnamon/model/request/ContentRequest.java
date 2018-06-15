package com.dewarim.cinnamon.model.request;

public class ContentRequest {

    private String ticket;
    private Long id;

    public ContentRequest() {
    }

    public ContentRequest(String ticket, Long id) {
        this.ticket = ticket;
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTicket() {
        return ticket;
    }

    public void setTicket(String ticket) {
        this.ticket = ticket;
    }

    public boolean validated(){
        return id != null && id > 0 && ticket != null && ticket.trim().length() > 0;
    }


}
