package com.github.wz.webshell.vo;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;

import java.io.Serializable;

@Data
public class ApiResult<T> implements Serializable {
        private static final long serialVersionUID = 1L;
        private int code = HttpStatus.OK.value();
        private String msg = HttpStatus.OK.getReasonPhrase();
        private transient T data;

        public ApiResult() {
        // 构造器
    }

        public static <T> ApiResult<T> badRequest() {
        ApiResult<T> result = new ApiResult<>();
        result.setCode(HttpStatus.BAD_REQUEST.value());
        result.setMsg(HttpStatus.BAD_REQUEST.getReasonPhrase());
        return result;
    }

    public static <T> ApiResult<T> builder() {
        return new ApiResult<>();
    }

        public ApiResult<T> error(int code, String errorMsg) {
        setCode(code);
        if (StringUtils.isNotBlank(errorMsg)) {
            setMsg(errorMsg);
        }
        return this;
    }

        public ApiResult<T> info(String msg) {
        if (StringUtils.isNotBlank(msg)) {
            setMsg(msg);
        }
        return this;
    }

        public ApiResult<T> data(T data) {
        if (null != data) {
            setData(data);
        }
        return this;
    }
}
