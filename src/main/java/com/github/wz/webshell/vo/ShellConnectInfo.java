package com.github.wz.webshell.vo;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import lombok.Data;
import org.springframework.web.socket.WebSocketSession;

import java.io.Serializable;

@Data
public class ShellConnectInfo implements Serializable {
        private static final long serialVersionUID = 1555506471798748444L;
        private WebSocketSession webSocketSession;
        private JSch jsch;
        private Channel channel;
}
