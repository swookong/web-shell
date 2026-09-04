package com.github.wz.web.shell.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SftpFileTreeVo implements Comparable<SftpFileTreeVo> {
    private String id;
    private String parent;
    private String text;
    private String icon;

    private String fileType;
    private String fileAttr;
    private String numberOfDir;
    private String owner;
    private String group;
    private String size;
    private String modifiedDate;

    @Override
    public int compareTo(SftpFileTreeVo vo) {
        // 先根据文件类型排序，再根据文件名排序
        int ic = vo.getIcon().compareTo(icon);
        if (ic == 0) {
            return text.compareTo(vo.getText());
        }
        return ic;
    }
}
