package com.github.wz.webshell.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wz.webshell.Constants;
import com.github.wz.webshell.utils.SecretUtils;
import com.github.wz.webshell.utils.ThreadPoolUtils;
import com.github.wz.webshell.utils.WebShellUtils;
import com.github.wz.webshell.vo.ShellConnectInfo;
import com.github.wz.webshell.vo.WebShellData;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class WebShellService {
        private static final Map<String, Object> SSH_MAP = new ConcurrentHashMap<>();

        public void initConnection(WebSocketSession session) {
        JSch jSch = new JSch();
        ShellConnectInfo shellConnectInfo = new ShellConnectInfo();
        shellConnectInfo.setJsch(jSch);
        shellConnectInfo.setWebSocketSession(session);
        String uuid = WebShellUtils.getUuid(session);
        //将这个ssh连接信息放入缓存中
        SSH_MAP.put(uuid, shellConnectInfo);
    }

        public void recvHandle(String buffer, WebSocketSession session) {
        ObjectMapper objectMapper = new ObjectMapper();
        WebShellData shellData;
        try {
            shellData = objectMapper.readValue(buffer, WebShellData.class);
        } catch (IOException e) {
            log.error("Json转换异常:{}", e.getMessage());
            return;
        }
        String userId = WebShellUtils.getUuid(session);
        //找到刚才存储的ssh连接对象
        ShellConnectInfo shellConnectInfo = (ShellConnectInfo) SSH_MAP.get(userId);
        if (shellConnectInfo != null) {
            if (Constants.OPERATE_CONNECT.equals(shellData.getOperate())) {
                //启动线程异步处理
                ThreadPoolUtils.execute(() -> {
                    try {
                        connectToSsh(shellConnectInfo, shellData, session);
                    } catch (JSchException e) {
                        log.error("web shell连接异常:{}", e.getMessage());
                        sendMessage(session, e.getMessage().getBytes());
                        close(session);
                    }
                });
            } else if (Constants.OPERATE_COMMAND.equals(shellData.getOperate())) {
                String command = shellData.getCommand();
                sendToTerminal(shellConnectInfo.getChannel(), command);
            } else {
                log.error("不支持的操作");
                close(session);
            }
        }
    }

        public void close(WebSocketSession session) {
        String userId = WebShellUtils.getUuid(session);
        ShellConnectInfo shellConnectInfo = (ShellConnectInfo) SSH_MAP.get(userId);
        if (shellConnectInfo != null) {
            //断开连接
            if (shellConnectInfo.getChannel() != null) {
                shellConnectInfo.getChannel().disconnect();
            }
            //map中移除
            SSH_MAP.remove(userId);
        }
    }

        private void connectToSsh(ShellConnectInfo shellConnectInfo, WebShellData sshData, WebSocketSession webSocketSession)
            throws JSchException {
        Properties config = new Properties();
        // SSH 连接远程主机时，会检查主机的公钥。如果是第一次该主机，会显示该主机的公钥摘要，提示用户是否信任该主机
        config.put("StrictHostKeyChecking", "no");
        //获取jsch的会话
        Session session = shellConnectInfo.getJsch().getSession(sshData.getUsername(), sshData.getHost(),
                sshData.getPort());
        session.setConfig(config);
        //设置密码
        session.setPassword(SecretUtils.decrypt(sshData.getPassword(), SecretUtils.AES_KEY));
        //连接超时时间30s
        session.connect(30000);

        //开启shell通道
        Channel channel = session.openChannel("shell");
        //通道连接超时时间3s
        channel.connect(3000);
        //设置channel
        shellConnectInfo.setChannel(channel);

        //查询上次登录时间
//		sendToTerminal(channel, "lastlog -u " + sshData.getUsername() + "\r");

        //读取终端返回的信息流
        try (InputStream inputStream = channel.getInputStream()) {
            //循环读取
            byte[] buffer = new byte[Constants.BUFFER_SIZE];
            int i;
            //如果没有数据来，线程会一直阻塞在这个地方等待数据。
            while ((i = inputStream.read(buffer)) != -1) {
                sendMessage(webSocketSession, Arrays.copyOfRange(buffer, 0, i));
            }
        } catch (IOException e) {
            log.error("读取终端返回的信息流异常：", e);
        } finally {
            //断开连接后关闭会话
            session.disconnect();
            channel.disconnect();
        }
    }

        public void sendMessage(WebSocketSession session, byte[] buffer) {
        try {
            session.sendMessage(new TextMessage(buffer));
        } catch (IOException e) {
            log.error("数据写回前端异常：", e);
        }
    }

        private void sendToTerminal(Channel channel, String command) {
        if (channel != null) {
            try {
                OutputStream outputStream = channel.getOutputStream();
                outputStream.write(command.getBytes());
                outputStream.flush();
            } catch (IOException e) {
                log.error("web shell将消息转发到终端异常:{}", e.getMessage());
            }
        }
    }
}
