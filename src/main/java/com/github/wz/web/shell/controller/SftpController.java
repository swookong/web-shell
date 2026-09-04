package com.github.wz.web.shell.controller;

import com.github.wz.web.shell.Constants;
import com.github.wz.web.shell.utils.EhCacheUtils;
import com.github.wz.web.shell.utils.SftpFileUtils;
import com.github.wz.web.shell.utils.SftpUtils;
import com.github.wz.web.shell.utils.WebShellUtils;
import com.github.wz.web.shell.vo.ApiResult;
import com.github.wz.web.shell.vo.SftpFileTreeVo;
import com.github.wz.web.shell.vo.WebShellData;
import com.jcraft.jsch.SftpException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

@Slf4j
@RequestMapping("/sftp")
@RestController
public class SftpController {

    private static final ApiResult UNAUTHORIZED = ApiResult.builder().error(401, "登录已过期，请重新登录");

    private ResponseEntity unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(UNAUTHORIZED);
    }

    private WebShellData getSshData() {
        String sessionId = WebShellUtils.getSessionId();
        return EhCacheUtils.get(sessionId);
    }

    @GetMapping("getFileTree")
    public ResponseEntity<ApiResult<List<SftpFileTreeVo>>> getFileTree(String path) {
        WebShellData sshData = getSshData();
        if (sshData == null) {
            return unauthorized();
        }
        SftpUtils sftpUtils = new SftpUtils(sshData);
        if (!sftpUtils.login()) {
            return unauthorized();
        }
        try {
            List<SftpFileTreeVo> fileTree = SftpFileUtils.getFileTree(sftpUtils, path);
            return ResponseEntity.ok(ApiResult.<List<SftpFileTreeVo>>builder().data(fileTree));
        } finally {
            sftpUtils.logout();
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<ApiResult<String>> upload(HttpServletRequest request) {
        List<MultipartFile> files = ((MultipartHttpServletRequest) request).getFiles("file");
        String directory = request.getParameter("path");
        WebShellData sshData = getSshData();
        if (sshData == null) {
            return unauthorized();
        }
        SftpUtils sftpUtils = new SftpUtils(sshData);
        if (!sftpUtils.login()) {
            return unauthorized();
        }
        String res = "上传成功！";
        try {
            for (MultipartFile file : files) {
                String fileName = file.getOriginalFilename();
                try {
                    sftpUtils.upload(directory, fileName, file.getInputStream());
                } catch (SftpException | IOException e) {
                    log.error("上传文件失败：{}", fileName, e);
                    res = "上传失败！";
                }
            }
        } finally {
            sftpUtils.logout();
        }
        return ResponseEntity.ok(ApiResult.<String>builder().data(res));
    }

    @GetMapping("/download")
    public void download(String path, HttpServletResponse response) {
        if (StringUtils.isBlank(path)) {
            return;
        }
        WebShellData sshData = getSshData();
        if (sshData == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        String fileName = path.substring(path.lastIndexOf(Constants.SEPARATOR) + 1);
        SftpUtils sftpUtils = new SftpUtils(sshData);
        if (!sftpUtils.login()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        try {
            String type;
            try {
                type = java.nio.file.Files.probeContentType(java.nio.file.Paths.get(path));
            } catch (Exception ex) {
                type = "application/octet-stream";
            }
            if (type == null) type = "application/octet-stream";
            response.setHeader("Content-type", type);
            response.setContentType("application/force-download");
            response.addHeader("Content-Disposition", "attachment;fileName=" + fileName);
            byte[] buffer = new byte[Constants.BUFFER_SIZE];
            try (InputStream fis = sftpUtils.download(path);
                 BufferedInputStream bis = new BufferedInputStream(fis);
                 OutputStream os = response.getOutputStream()) {
                int i;
                while ((i = bis.read(buffer)) != -1) {
                    os.write(buffer, 0, i);
                }
            }
        } catch (Exception e) {
            log.error("下载文件:{}失败", path, e);
        } finally {
            sftpUtils.logout();
        }
    }

    @DeleteMapping
    public ResponseEntity<ApiResult<String>> deleteFile(String path) {
        if (StringUtils.isBlank(path)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResult.<String>builder().error(404, "文件路径为空！"));
        }
        WebShellData sshData = getSshData();
        if (sshData == null) {
            return unauthorized();
        }
        SftpUtils sftpUtils = new SftpUtils(sshData);
        if (!sftpUtils.login()) {
            return unauthorized();
        }
        try {
            if (!sftpUtils.delete(path)) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ApiResult.<String>builder().error(500, "删除文件失败！"));
            }
        } finally {
            sftpUtils.logout();
        }
        return ResponseEntity.ok(ApiResult.<String>builder().data("删除成功!"));
    }
}