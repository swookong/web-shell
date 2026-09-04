package com.github.wz.web.shell.utils;

import com.github.wz.web.shell.Constants;
import com.github.wz.web.shell.vo.WebShellData;
import com.jcraft.jsch.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.InputStream;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public final class SftpUtils {

    private ChannelSftp channelSftp;
    private Session session;

    private final String username;

    private String password;

    private String privateKey;

    private final String host;

    private final int port;

    public SftpUtils(WebShellData sshData) {
        this.username = sshData.getUsername();
        this.password = SecretUtils.decrypt(sshData.getPassword(), SecretUtils.AES_KEY);
        this.host = sshData.getHost();
        this.port = sshData.getPort();
    }

    public SftpUtils(String username, String privateKey, int port, String host) {
        this.username = username;
        this.privateKey = privateKey;
        this.host = host;
        this.port = port;
    }

    public boolean login() {
        JSch jsch = new JSch();
        try {
            if (StringUtils.isNotBlank(privateKey)) {
                //设置登陆主机的秘钥
                jsch.addIdentity(privateKey);
            }
            //采用指定的端口连接服务器
            session = jsch.getSession(username, host, port);
            if (StringUtils.isNotBlank(password)) {
                //设置登陆主机的密码
                session.setPassword(password);
            }
            //优先使用 password 验证   注：session.connect()性能低，使用password验证可跳过gssapi认证，提升连接服务器速度
            session.setConfig("PreferredAuthentications", "password");
            //设置第一次登陆的时候提示，可选值：(ask | yes | no)
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();
            //创建sftp通信通道
            Channel channel = session.openChannel("sftp");
            channel.connect();
            channelSftp = (ChannelSftp) channel;
            log.info("sftp server connect success !!");
        } catch (JSchException e) {
            log.error("SFTP服务器连接异常！！", e);
            return false;
        }
        return true;
    }

    public void logout() {
        if (channelSftp != null && channelSftp.isConnected()) {
            channelSftp.disconnect();
            log.debug("sftp closed");
        }
        if (session != null && session.isConnected()) {
            session.disconnect();
            log.debug("session closed");
        }
    }

    public void upload(String directory, String sftpFileName, InputStream input) throws SftpException {
        long start = System.currentTimeMillis();
        // 创建不存在的文件夹，并切换到文件夹
        createDir(directory);
        // 上传文件
        channelSftp.put(input, sftpFileName);
        log.info("文件上传成功！！ 耗时：{}ms", (System.currentTimeMillis() - start));
    }

    public InputStream download(String path) throws SftpException {
        // 文件所在目录
        String directory = path.substring(0, path.lastIndexOf(Constants.SEPARATOR));
        // 文件名
        String fileName = path.substring(path.lastIndexOf(Constants.SEPARATOR) + 1);
        return download(directory, fileName);
    }

    public InputStream download(String directory, String fileName) throws SftpException {
        if (StringUtils.isNotBlank(directory)) {
            channelSftp.cd(directory);
        }
        log.info("下载文件:{}/{}", directory, fileName);
        return channelSftp.get(fileName);
    }

    private boolean delete(String directory, String fileName) {
        String file = directory + Constants.SEPARATOR + fileName;
        try {
            ChannelSftp.LsEntry lsEntry = (ChannelSftp.LsEntry) listFiles(file).get(0);
            // 用户权限处理
            if (!(Constants.USER_ROOT.equals(username)
                    || username.equals(SftpFileUtils.getOwner(lsEntry.getLongname())))) {
                log.warn("用户{}没有权限删除文件：{}", username, file);
                return false;
            }
            channelSftp.cd(directory);
            if (isDirExists(file)) {
                // 删除空文件夹
                channelSftp.rmdir(fileName);
            } else {
                channelSftp.rm(fileName);
            }
            log.info("删除文件：{}成功", file);
        } catch (SftpException e) {
            log.error("删除文件异常：{}", file, e);
            return false;
        }
        return true;
    }

    public boolean delete(String path) {
        AtomicBoolean delFlag = new AtomicBoolean(true);
        Vector<?> vector = listFiles(path);
        // 是文件或者空文件夹
        if (isFileExists(path) || vector.isEmpty()) {
            // 文件所在目录
            String directory = path.substring(0, path.lastIndexOf(Constants.SEPARATOR));
            // 文件名
            String fileName = path.substring(path.lastIndexOf(Constants.SEPARATOR) + 1);
            return delete(directory, fileName);
        } else if (isDirExists(path)) {
            // 1.先循环删除子文件
            vector.forEach(v -> {
                ChannelSftp.LsEntry lsEntry = (ChannelSftp.LsEntry) v;
                // 如果是文件夹，递归删除
                if (FileType.DIRECTORY.getSign().equals(lsEntry.getLongname().substring(0, 1))) {
                    delFlag.set(delete(path + Constants.SEPARATOR + lsEntry.getFilename()));
                } else {
                    // 删除文件
                    delFlag.set(delete(path, lsEntry.getFilename()));
                }
            });
            // 2.再删除空文件夹
            delFlag.set(delete(path));
        }
        return delFlag.get();
    }

    public Vector<?> listFiles(String directory) {
        try {
            if (isDirExists(directory) || isFileExists(directory)) {
                Vector<?> vector = channelSftp.ls(directory);
                //移除上级目录和根目录："." ".."
                Iterator<?> it = vector.iterator();
                while (it.hasNext()) {
                    ChannelSftp.LsEntry lsEntry = (ChannelSftp.LsEntry) it.next();
                    if (Constants.DOT.equals(lsEntry.getFilename())
                            || Constants.PARENT_DIRECTORY.equals(lsEntry.getFilename())) {
                        it.remove();
                    }
                }
                return vector;
            }
        } catch (SftpException e) {
            log.error("获取文件夹信息异常！", e);
        }
        return new Vector<>();
    }

    public boolean createDir(String createPath) {
        try {
            if (isDirExists(createPath)) {
                this.channelSftp.cd(createPath);
                return true;
            }
            String[] pathArray = createPath.split(Constants.SEPARATOR);
            StringBuilder filePath = new StringBuilder(Constants.SEPARATOR);
            for (String path : pathArray) {
                if ("".equals(path)) {
                    continue;
                }
                filePath.append(path);
                // 路径如果是文件，跳过，保存到同级目录
                if (isFileExists(filePath.toString())) {
                    continue;
                }
                filePath.append(Constants.SEPARATOR);
                if (!isDirExists(filePath.toString())) {
                    // 建立目录
                    channelSftp.mkdir(filePath.toString());
                }
                // 并进入目录
                channelSftp.cd(filePath.toString());
            }
        } catch (SftpException e) {
            log.error("目录创建异常！", e);
            return false;
        }
        return true;
    }

    public boolean isDirExists(String directory) {
        try {
            SftpATTRS attrs = this.channelSftp.lstat(directory);
            return null != attrs && attrs.isDir();
        } catch (Exception e) {
            log.error("判断目录是否存在异常：{}", directory, e);
        }
        return false;
    }

    public boolean isFileExists(String filePath) {
        try {
            SftpATTRS attrs = this.channelSftp.lstat(filePath);
            // 存在并且不是文件夹
            return null != attrs && !attrs.isDir();
        } catch (Exception e) {
            log.error("判断文件是否存在异常：{}", filePath, e);
        }
        return false;
    }
}