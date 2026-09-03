package com.github.wz.webshell.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class WebShellData implements Serializable {
        private static final long serialVersionUID = -2326528171211907216L;
        private String operate;
        private String host;
        private Integer port = 22;
        private String username;
        private String password;
        private String command = "";
}
