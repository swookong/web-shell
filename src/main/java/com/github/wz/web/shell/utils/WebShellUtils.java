package com.github.wz.web.shell.utils;

import com.github.wz.web.shell.Constants;
import org.springframework.web.socket.WebSocketSession;

public final class WebShellUtils {
    private WebShellUtils() {
    }

    public static String getUuid(WebSocketSession webSocketSession) {
        return String.valueOf(webSocketSession.getAttributes().get(Constants.USER_UUID_KEY));
    }

    public static String getSessionId() {
        return SpringUtils.getSession().getId();
    }

    public static String convertFileSize(long size) {
        long kb = Constants.KB;
        long mb = kb * Constants.KB;
        long gb = mb * Constants.KB;
        String fileSize;
        if (size >= gb) {
            fileSize = String.format("%.1fGB", (float) size / (float) gb);
        } else {
            float f;
            if (size >= mb) {
                f = (float) size / (float) mb;
                fileSize = String.format(f > 100.0F ? "%.0fMB" : "%.1fMB", f);
            } else if (size >= kb) {
                f = (float) size / (float) kb;
                fileSize = String.format(f > 100.0F ? "%.0fKB" : "%.1fKB", f);
            } else {
                fileSize = String.format("%dB", size);
            }
        }
        return fileSize;
    }
}
