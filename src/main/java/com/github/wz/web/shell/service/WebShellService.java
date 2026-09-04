package com.github.wz.web.shell.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wz.web.shell.Constants;
import com.github.wz.web.shell.utils.EhCacheUtils;
import com.github.wz.web.shell.utils.SecretUtils;
import com.github.wz.web.shell.utils.ThreadPoolUtils;
import com.github.wz.web.shell.utils.WebShellUtils;
import com.github.wz.web.shell.vo.ShellConnectInfo;
import com.github.wz.web.shell.vo.WebShellData;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.Resource;
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
    private static final Map<String, ShellConnectInfo> SSH_MAP = new ConcurrentHashMap<>();

    @Resource
    private ObjectMapper objectMapper;

    public void initConnection(WebSocketSession session) {
        JSch jSch = new JSch();
        ShellConnectInfo shellConnectInfo = new ShellConnectInfo();
        shellConnectInfo.setJsch(jSch);
        shellConnectInfo.setWebSocketSession(session);
        String uuid = WebShellUtils.getUuid(session);
        SSH_MAP.put(uuid, shellConnectInfo);
        log.info("WebSocket initConnection uuid={}, sessionId={}, SSH_MAP size={}", uuid, session.getId(), SSH_MAP.size());
    }

    public void recvHandle(String buffer, WebSocketSession session) {
        WebShellData shellData;
        try {
            shellData = objectMapper.readValue(buffer, WebShellData.class);
        } catch (IOException e) {
            log.error("Json转换异常:{}", e.getMessage());
            return;
        }
        String userId = WebShellUtils.getUuid(session);
        log.info("recvHandle uuid={}, operate={}, sessionOpen={}", userId, shellData.getOperate(), session.isOpen());
        ShellConnectInfo shellConnectInfo = SSH_MAP.get(userId);
        if (shellConnectInfo != null) {
            if (Constants.OPERATE_CONNECT.equals(shellData.getOperate())) {
                log.info("开始连接SSH {}:{} as {}", shellData.getHost(), shellData.getPort(), shellData.getUsername());
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
            } else if (Constants.OPERATE_RESIZE.equals(shellData.getOperate())) {
                log.debug("resize terminal: cols={}, rows={}", shellData.getCols(), shellData.getRows());
            } else {
                log.warn("忽略不支持的操作: {}", shellData.getOperate());
            }
        } else {
            log.warn("recvHandle: 找不到 uuid={} 对应的 SSH 连接信息，SSH_MAP size={}", userId, SSH_MAP.size());
        }
    }

    public void close(WebSocketSession session) {
        String userId = WebShellUtils.getUuid(session);
        ShellConnectInfo shellConnectInfo = SSH_MAP.get(userId);
        log.info("close uuid={}, sessionOpen={}", userId, session != null ? session.isOpen() : "null");
        if (shellConnectInfo != null) {
            if (shellConnectInfo.getChannel() != null) {
                shellConnectInfo.getChannel().disconnect();
            }
            SSH_MAP.remove(userId);
            log.info("close完成, SSH_MAP size={}", SSH_MAP.size());
        }
        String httpSessionId = WebShellUtils.getHttpSessionId(session);
        if (httpSessionId != null) {
            EhCacheUtils.delete(httpSessionId);
            log.info("清除 EhCache sessionId={}", httpSessionId);
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
            session.sendMessage(new BinaryMessage(buffer));
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