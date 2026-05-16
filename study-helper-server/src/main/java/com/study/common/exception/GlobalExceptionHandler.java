package com.study.common.exception;

import com.study.common.result.R;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public R<Void> handleException(Exception e) {
        return R.fail(e.getMessage());
    }
}
