package com.acesse.youchat;

import java.text.SimpleDateFormat;


import android.widget.ProgressBar;


public class MessageBean {

    protected int id;  
    protected String from, to, text, externalBodyUrl, localPath, duration;
    protected long time;
    protected int state;
    protected boolean isOutgoing;
    protected boolean animateFlag;
    protected boolean isDownloading;
    protected boolean isUploading;
    protected long deliveryDuration;
    protected long downloadDuration;
    protected long uploadDuration;
    protected int downloadSize;
    protected int uploadSize;

    protected ProgressBar progressBar;


    public MessageBean() {
    }

    /*
    public MessageBean(LinphoneChatMessage msg, boolean animateFlag) {
        this(msg.getStorageId(), msg.getFrom().getUserName(), msg.getTo().getUserName(), msg.getText(), msg.getTime(), msg.getExternalBodyUrl(), msg.getStatus(), msg.isOutgoing(), animateFlag);
    }
    */

    public MessageBean(int id, String from, String to, String text, long time, String externalBodyUrl, int state, boolean isOutgoing, boolean animateFlag) {
        this.id = id;
        this.from = from;
        this.to = to;
        this.text = text;
        this.time = time;
        this.externalBodyUrl = externalBodyUrl;
        this.state = state;
        this.isOutgoing = isOutgoing;
        this.animateFlag = animateFlag;
        /*
        if (state == LinphoneChatMessage.State.UploadFailed) {
            this.localPath = externalBodyUrl;
            this.externalBodyUrl = null;
        }
        */
    }

    @Override
    public String toString() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        StringBuilder sb = new StringBuilder();
        sb.append("ID: ").append(id);
        sb.append(" TIME: ").append(sdf.format(time));
        sb.append(" CURRENT TIME: ").append(sdf.format(System.currentTimeMillis()));
        sb.append(" FROM: ").append(from);
        sb.append(" TO: ").append(to);
        if (text != null && !text.isEmpty()) {
            sb.append(" TEXT: ").append(text);
        }
        if (externalBodyUrl != null) {
            sb.append( " URL: ").append(externalBodyUrl.substring(externalBodyUrl.lastIndexOf("/")+1));
        }
        if (localPath != null) {
            sb.append( " LOCAL: ").append(localPath.substring(localPath.lastIndexOf("/")+1));
        }
        sb.append( " STATE: ").append(state);
        return sb.toString();
    }
    
    
    @Override
    public int hashCode() {
        return 13 * Long.valueOf(time).hashCode() * Integer.valueOf(id).hashCode();
    }


    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MessageBean))
            return false;
        MessageBean b = (MessageBean) obj;
        return this.time == b.time && this.id == b.id;
    }
}