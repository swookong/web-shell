package com.github.wz.webshell.utils;

public enum FileType {
        NORMAL_FILE("-", "普通文件"),
        DIRECTORY("d", "目录"),
        LINK_FILE("l", "链接文件"),
        MANAGE_FILE("p", "管理文件"),
        BLOCK_DEVICE_FILE("b", "块设备文件"),
        CHARACTER_DEVICE_FILE("c", "字符设备文件"),
        SOCKET_FILE("s", "套接字文件");

        private final String sign;
        private final String zhName;

    FileType(String sign, String zhName) {
        this.sign = sign;
        this.zhName = zhName;
    }

        public static String getZhName(String sign) {
        FileType[] types = FileType.values();
        for (FileType type : types) {
            if (type.getSign().equals(sign)) {
                return type.getZhName();
            }
        }
        return NORMAL_FILE.zhName;
    }

        public static String getFileTypeIcon(String fileType) {
        String icon;
        if (fileType.equals(NORMAL_FILE.getSign())) {
            // 文件图标
            icon = "jstree-file";
        } else if (fileType.equals(DIRECTORY.getSign())) {
            // 文件夹图标
            icon = "jstree-folder";
        } else {
            icon = "/static/img/" + fileType + ".png";
        }
        return icon;
    }

        public String getSign() {
        return sign;
    }

        public String getZhName() {
        return zhName;
    }

}
