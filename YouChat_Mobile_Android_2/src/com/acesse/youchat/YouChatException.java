/**
 * 
 */
package com.acesse.youchat;

public class YouChatException extends Exception {
    
    private int code;

    public YouChatException() {
        super();
    }

    public YouChatException(int code, String msg) {
        super(msg);
        this.code = code;
    }

    public YouChatException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public YouChatException(String detailMessage) {
        super(detailMessage);
    }

    public YouChatException(Throwable throwable) {
        super(throwable);
    }

    public int getCode() {
        return code;
    }
}
