package com.dewarim.cinnamon.application;

/**
 * All API url mappings.
 * <p>
 * Convention: Servlet name + "__" + method name (replacing camelCase with upper-case SNAKE_CASE),
 */
public enum UrlMapping {


    CONTENT__GET_CONTENT("content","getContent" ,"/" ),
    CONTENT__GET_NOTHING("content", "","/");

    private String servlet;
    private String action;
    private String prefix;

    /**
     * @param servlet the servlet handling the url
     * @param action  the action part of the url (the getUser part in /users/getUser?id=1234)
     * @param prefix  a prefix for the servlet - for example, all api servlets are prefixed with /api for
     *                authentication filtering.
     */
    UrlMapping(String servlet, String action, String prefix) {
        this.servlet = servlet;
        this.action = action;
        this.prefix = prefix;
    }

    public String getPath() {
        return prefix + "/" + servlet + "/" + action;
    }

    public String getServlet() {
        return servlet;
    }

    public String getAction() {
        return action;
    }
}
