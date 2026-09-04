package com.github.wz.web.shell.utils;

import com.github.wz.web.shell.Constants;
import com.github.wz.web.shell.vo.SftpFileTreeVo;
import com.jcraft.jsch.ChannelSftp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SftpFileUtils {
    private static final Pattern FILE_PATTERN = Pattern.compile(
            "^([-dlpbcsrwx]{10})\\s+([0-9]+)\\s+([0-9a-zA-Z]+)\\s+([0-9a-zA-Z]+)\\s+([0-9]+)\\s+([0-9a-zA-Z:\\s]+)\\s+");

    public static List<SftpFileTreeVo> getFileTree(SftpUtils sftpUtils, String path) {
        List<SftpFileTreeVo> fileTree = new ArrayList<>();
        String parentPath = path;
        if (!parentPath.endsWith(Constants.SEPARATOR)) {
            parentPath = path + Constants.SEPARATOR;
        }
        Vector<?> files = sftpUtils.listFiles(parentPath);
        String finalParentPath = parentPath;
        files.forEach(file -> {
            ChannelSftp.LsEntry lsEntry = (ChannelSftp.LsEntry) file;
            String fileType = lsEntry.getLongname().substring(0, 1);
            // 文件类型图标
            String icon = FileType.getFileTypeIcon(fileType);
            SftpFileTreeVo vo = SftpFileTreeVo.builder()
                    .id(finalParentPath + lsEntry.getFilename())
                    .parent(finalParentPath)
                    .text(lsEntry.getFilename())
                    .icon(icon)
                    .build();
            // 匹配文件详情
            Matcher m = FILE_PATTERN.matcher(lsEntry.getLongname());
            if (m.find()) {
                vo.setFileType(FileType.getZhName(m.group(1).substring(0, 1)));
                vo.setFileAttr(m.group(1).substring(1));
                vo.setNumberOfDir(m.group(2));
                vo.setOwner(m.group(3));
                vo.setGroup(m.group(4));
                vo.setSize(WebShellUtils.convertFileSize(Long.parseLong(m.group(5))));
                vo.setModifiedDate(m.group(6));
            }
            fileTree.add(vo);
        });
        // 排序
        Collections.sort(fileTree);
        return fileTree;
    }

    public static String getOwner(String longName) {
        // 正则匹配长文件详情
        Matcher m = FILE_PATTERN.matcher(longName);
        if (m.find()) {
            return m.group(3);
        }
        return "";
    }

    private SftpFileUtils() {
    }
}
