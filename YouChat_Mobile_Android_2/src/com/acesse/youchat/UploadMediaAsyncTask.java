package com.acesse.youchat;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONObject;

import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.crittercism.app.Crittercism;


/**
 * Upload to the media server audio, photo or video. 
 */
public class UploadMediaAsyncTask extends AsyncTask<MessageBean, Integer, Object> {

    private static final String TAG = "YOUC";
    private final String lineEnd = "\r\n";
    private final String twoHyphens = "--";
    private final String boundary = "---------------------------14737809831466499882746641449";
    private MessageBean mbean;
    protected long mUploaded;
    int numOfRetries = 0;


    /*
    public UploadMediaAsyncTask(LinphoneChatMessage.StateListener listener) {
        this.listener = listener;
    }
    */


    @Override
    protected Object doInBackground(MessageBean... args) {

        mbean = args[0];
        MainApplication.addTask(mbean);

        String fileName = mbean.localPath.substring(mbean.localPath.lastIndexOf("/")+1);

        File file = new File(mbean.localPath);
        int size = (int) file.length();
        if (Config.DEBUG) {
            Log.d(TAG, "UPLOADING FILE: " + file);
            Log.d(TAG, "UPLOADING NUM BYTES: " + size);
        }
        mbean.uploadSize = size;

        return doHttpUrlConnectionUpload(file, fileName, size);
        //return doSocketUpload(file, fileName, size);
    }



    private Object doHttpUrlConnectionUpload(File file, String fileName, int size) {
        HttpURLConnection connection = null;
        try {               

            URL url = new URL("http://" + Config.DOMAIN_NODEJS + ":7000/api/tools/upload");
            if (Config.DEBUG) {
                Log.d(TAG, "UPLOADING TO: " + url);
            }
            long startTime = System.currentTimeMillis();
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(30000);
            connection.setDoInput(true); 
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("ENCTYPE", "multipart/form-data");
            connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
            connection.setRequestProperty("Connection", "close");


            StringBuilder sb = new StringBuilder();
            sb.append(lineEnd + twoHyphens + boundary + lineEnd); 
            sb.append("Content-Disposition: form-data; name=\"displayImage\"; filename=\""+ fileName + "\"" + lineEnd);
            sb.append("Content-Type: application/octet-stream" + lineEnd);
            sb.append(lineEnd);
            byte[] headerData = sb.toString().getBytes();

            sb = new StringBuilder();
            sb.append(lineEnd);
            sb.append(twoHyphens + boundary + twoHyphens + lineEnd);
            byte[] footerData = sb.toString().getBytes();

            connection.setFixedLengthStreamingMode(headerData.length + size + footerData.length);

            DataOutputStream dos = new DataOutputStream(connection.getOutputStream());

            dos.write(headerData);

            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
            byte[] data = new byte[1024 * 8];
            int bytesRead = 0;
            int bytesSent = 0;
            int prevProgress = 0;
            while ((bytesRead = bis.read(data)) > 0 && !isCancelled()) {
                dos.write(data, 0, bytesRead);
                bytesSent += bytesRead;
                int progress = (int) ((float) bytesSent / (float) size * 100f);
                if (progress != prevProgress) {
                    if (Config.DEBUG) {
                        Log.d(TAG, "PROGRESS: " + progress + " SENT: " + bytesSent + " SIZE: " + size);
                    }
                    mUploaded = bytesSent;
                    publishProgress(prevProgress = progress);
                    try {
                        // allow ui to update and don't starve network pipe...
                        Thread.sleep(2);
                    } catch (Exception ex) {
                    }
                }
            }
            dos.write(footerData);

            dos.flush();
            dos.close();
            bis.close();

            mbean.uploadDuration = System.currentTimeMillis()-startTime;
            if (Config.DEBUG) {
                Log.d(TAG, "SENT TIME: " + mbean.uploadDuration + "ms");
            }

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new Exception("Request Failed with " + connection.getResponseCode() + ": " + connection.getResponseMessage());
            }

            startTime = System.currentTimeMillis();

            InputStream is = connection.getInputStream();

            data = new byte[2048];
            bytesRead = is.read(data);
            is.close();

            if (bytesRead == -1) {
                throw new Exception("No response data");
            }

            String json = new String(data, 0, bytesRead);
            if (Config.DEBUG) {
                Log.d(TAG, "RCV: " + json);
            }

            JSONObject jobj = new JSONObject(json);

            String resultUrl = jobj.getString("return_url");
            if (Config.DEBUG) {
                Log.d(TAG, "UPLOAD RESULT: " + resultUrl);
            }
            mbean.externalBodyUrl = resultUrl;

            //
            // rename the local content to match the server name...
            //
            String serverName = resultUrl.substring(resultUrl.lastIndexOf("/")+1);
            File oldFile = new File(mbean.localPath);
            File newFile = new File(MainApplication.getChatStorageDirectory(), serverName);
            if (Config.DEBUG) {
                Log.d(TAG, "RENAME CONTENT FROM: " + oldFile + " TO " + serverName);
            }
            if (!oldFile.renameTo(newFile)) {
                Log.w(TAG, "OH NO, RENAME FAILED");
            }
            mbean.localPath = newFile.getAbsolutePath();
            if (mbean.localPath.endsWith(".mp4") || mbean.localPath.endsWith(".aac")) {
                mbean.duration = MainApplication.getMediaDuration(mbean.localPath);
            }

            //
            // Upload successful now send our chat message with the externalBodyUrl.  Recreate
            // the chat room in the case that the user changed chat sessions.
            //
            /*
            LinphoneCore lc = LinphoneManager.getLcIfManagerNotDestroyedOrNull();
            if (listener != null && lc != null) {
                String to = "sip:" + mbean.to + "@" + Config.DOMAIN_XMPP;
                LinphoneChatRoom room = lc.getOrCreateChatRoom(to);
                LinphoneChatMessage msg = room.createLinphoneChatMessage("");
                msg.addCustomHeader("size", String.valueOf(size));
                msg.setExternalBodyUrl(mbean.externalBodyUrl);
                room.markAsRead();
                room.sendMessage(msg, listener);
                mbean.deliveryDuration = System.currentTimeMillis();
                mbean.time = msg.getTime();
                mbean.id = msg.getStorageId();
            }
            */

            return resultUrl;
        } catch (Exception ex) {

            Crittercism.logHandledException(ex);

            String message = ex.getMessage();
            Log.w(TAG, "FAILED TO UPLOAD: " + (message != null ? message : ""), ex);

            //
            // Simple retry logic... retry one time, delay 3 seconds before trying again...
            //
            if (++numOfRetries < 2) {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                }
                Log.v(TAG, "Retrying uploag again.");
                return doHttpUrlConnectionUpload(file, fileName, size);
            }

            /*
            mbean.state = LinphoneChatMessage.State.UploadFailed;
            //
            // Preserve this in the chat storage...
            //
            LinphoneCore lc = LinphoneManager.getLcIfManagerNotDestroyedOrNull();
            if (listener != null && lc != null) {
                String to = "sip:" + mbean.to + "@" + Config.DOMAIN_XMPP;
                LinphoneChatRoom room = lc.getOrCreateChatRoom(to);
                LinphoneChatMessage msg = room.createLinphoneChatMessage("", mbean.localPath, mbean.state, System.currentTimeMillis(), true, false);
                msg.store();
                mbean.id = msg.getStorageId();
            }
            */

            return ex;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            mbean.isUploading = false;
            MainApplication.removeTask(mbean);
            MainApplication.sendLocalBroadcast(new Intent("MESSAGE.UPDATE").putExtra("cmd", "upload").putExtra("time", mbean.time).putExtra("id", mbean.id));
        }
    }



    /*
    private Object doSocketUpload(File file, String fileName, int size) {
        Socket socket = null;
        OutputStream os = null;
        InputStream is = null;
        try {               
            long startTime = System.currentTimeMillis();
            if (Config.DEBUG) {
                Log.d(TAG, "CONNECTING TO: " + Config.DOMAIN_NODEJS + ":7000");
            }

            socket = new Socket();
            socket.setTcpNoDelay(true);
            socket.setKeepAlive(false);
            socket.setSoTimeout(30000); 
            //socket.setSendBufferSize(1024 * 8);
            socket.connect(new InetSocketAddress(Config.DOMAIN_NODEJS, 7000), 30000);
            if (Config.DEBUG) {
                Log.d(TAG, "CONNECT TIME: " + (System.currentTimeMillis()-startTime) + "ms");
            }
            os = socket.getOutputStream();
            is = socket.getInputStream();

            startTime = System.currentTimeMillis();

            StringBuffer buf = new StringBuffer();
            buf.append(lineEnd).append(twoHyphens).append(boundary).append(lineEnd);
            buf.append("Content-Disposition: form-data; name=\"displayImage\"; filename=\"").append(fileName).append("\"").append(lineEnd);
            buf.append("Content-Type: application/octet-stream").append(lineEnd).append(lineEnd);

            String openingPart = buf.toString();
            String closingPart = lineEnd + twoHyphens + boundary + twoHyphens + lineEnd;
            int totalLength = openingPart.length() + closingPart.length() + size;

            String userAgent = System.getProperty("http.agent");

            String header = "POST /api/tools/upload HTTP/1.1\n" +
                    //"Connection: keep-alive\n" +
                    "ENCTYPE: multipart/form-data\n" +
                    "Content-Type: multipart/form-data;boundary=" + boundary + "\n" +
                    "uploaded_file: " + fileName + "\n" +
                    "User-Agent: " + userAgent + "\n" +
                    "Host: " + Config.DOMAIN_NODEJS + ":7000\n" +
                    "Accept-Encoding: gzip\n" +
                    "Content-Length: " + totalLength + "\n";

            if (Config.DEBUG) {
                Log.d(TAG, "HEADER:\n" + header);
            }

            os.write(header.getBytes());
            os.write(openingPart.getBytes());

            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
            byte[] data = new byte[1024 * 8];
            int bytesRead = 0;
            int bytesSent = 0;
            int prevProgress = 0;
            while ((bytesRead = bis.read(data)) > 0) {
                os.write(data, 0, bytesRead);
                bytesSent += bytesRead;
                int progress = (int) ((float) bytesSent / (float) size * 100f);
                if (progress != prevProgress) {
                    if (Config.DEBUG) {
                        Log.d(TAG, "PROGRESS: " + progress + " SENT: " + bytesSent + " SIZE: " + size);
                    }
                    publishProgress(prevProgress = progress);
                    try {
                        // allow ui to update and don't starve network pipe...
                        Thread.sleep(2);
                    } catch (Exception ex) {
                    }
                }
            }
            os.write(closingPart.getBytes());
            os.flush();
            bis.close();
            if (Config.DEBUG) {
                Log.d(TAG, "SENT TIME: " + (System.currentTimeMillis()-startTime) + "ms");
            }
            startTime = System.currentTimeMillis();

            bytesRead = is.read(data);
            if (bytesRead == -1) {
                throw new Exception("No response data");
            }
            String rsp = new String(data, 0, bytesRead);
            if (Config.DEBUG) {
                Log.d(TAG, "RCV:\n" + rsp);
            }
            String statusLine = rsp.substring(0, rsp.indexOf("\n"));
            if (statusLine.indexOf("200") == -1) {
                throw new Exception(statusLine);
            }

            if (Config.DEBUG) {
                Log.d(TAG, "RCV TIME: " + (System.currentTimeMillis()-startTime) + "ms");
            }

            int idx = rsp.indexOf("{");
            if (idx == -1) {
                throw new Exception("Unable to find JSON:\n" + rsp);
            }
            rsp = rsp.substring(idx);

            JSONObject jobj = new JSONObject(rsp);

            String resultUrl = jobj.getString("return_url");
            if (Config.DEBUG) {
                Log.d(TAG, "UPLOAD RESULT: " + resultUrl);
            }
            mbean.externalBodyUrl = resultUrl;

            //
            // rename the local content to match the server name...
            //
            String serverName = resultUrl.substring(resultUrl.lastIndexOf("/")+1);
            File oldFile = new File(mbean.localPath);
            File newFile = new File(MainApplication.getChatStorageDirectory(), serverName);
            if (Config.DEBUG) {
                Log.d(TAG, "RENAME CONTENT FROM: " + oldFile + " TO " + serverName);
            }
            if (!oldFile.renameTo(newFile)) {
                Log.w(TAG, "OH NO, RENAME FAILED");
            }
            mbean.localPath = newFile.getAbsolutePath();

            //
            // Upload successful now send our chat message with the externalBodyUrl.  Recreate
            // the chat room in the case that the user changed chat sessions.
            //
            String to = "sip:" + mbean.to + "@" + Config.DOMAIN_XMPP;
            LinphoneChatRoom room = LinphoneManager.getLc().getOrCreateChatRoom(to);
            LinphoneChatMessage msg = room.createLinphoneChatMessage("");
            msg.addCustomHeader("size", String.valueOf(size));
            msg.setExternalBodyUrl(mbean.externalBodyUrl);
            room.markAsRead();
            room.sendMessage(msg, listener);
            mbean.time = msg.getTime();
            mbean.id = msg.getStorageId();

            return resultUrl;
        } catch (Exception ex) {

            String message = ex.getMessage();
            Log.w(TAG, "FAILED TO UPLOAD: " + (message != null ? message : ""), ex);

            //
            // Simple retry logic... retry one time, delay 3 seconds before trying again...
            //
            if (++numOfRetries < 2) {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                }
                return doSocketUpload(file, fileName, size);
            }

            mbean.state = LinphoneChatMessage.State.UploadFailed;
            //
            // Preserve this in the chat storage...
            //
            LinphoneCore lc = LinphoneManager.getLcIfManagerNotDestroyedOrNull();
            if (listener != null && lc != null) {
                String to = "sip:" + mbean.to + "@" + Config.DOMAIN_XMPP;
                LinphoneChatRoom room = lc.getOrCreateChatRoom(to);
                LinphoneChatMessage msg = room.createLinphoneChatMessage("", mbean.localPath, mbean.state, System.currentTimeMillis(), true, false);
                msg.store();
                mbean.id = msg.getStorageId();
            }

            return ex;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception ex) {
                }
            }
            if (os != null) {
                try {
                    os.close();
                } catch (Exception ex) {
                }
            }
            if (socket != null) {
                try {
                    socket.close();
                } catch (Exception ex) {
                }
            }
            mbean.isUploading = false;
            MainApplication.removeTask(mbean);
            MainApplication.sendLocalBroadcast(new Intent("MESSAGE.UPDATE").putExtra("cmd", "upload").putExtra("time", mbean.time).putExtra("id", mbean.id));
        }
    }
    */


    @Override
    protected void onPostExecute(Object result) {
        if (result instanceof Exception) {
            Toast.makeText(MainApplication.getInstance(), R.string.upload_failed, Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    protected void onProgressUpdate(Integer... progress) {
        //Log.d(TAG, "PROGRESS: " + progress[0]);
        if (mbean.progressBar != null) {
            mbean.progressBar.setProgress(progress[0]);
        }
    }
}
