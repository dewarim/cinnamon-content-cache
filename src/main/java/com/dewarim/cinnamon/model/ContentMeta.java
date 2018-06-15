package com.dewarim.cinnamon.model;

public class ContentMeta {

    private Long id;
    private String name;
    private String contentHash;
    private String contentPath;
    private Long contentSize;
    private String contentType;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    public String getContentPath() {
        return contentPath;
    }

    public void setContentPath(String contentPath) {
        this.contentPath = contentPath;
    }

    public Long getContentSize() {
        return contentSize;
    }

    public void setContentSize(Long contentSize) {
        this.contentSize = contentSize;
    }

    @Override
    public String toString() {
        return "ContentMeta{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", contentHash='" + contentHash + '\'' +
                ", contentPath='" + contentPath + '\'' +
                ", contentSize=" + contentSize +
                ", contentType='" + contentType + '\'' +
                '}';
    }
}
