package com.sky.handler;

import com.sky.constant.MessageConstant;
import com.sky.exception.BaseException;
import com.sky.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLIntegrityConstraintViolationException;

/**
 * 全局异常处理器，处理项目中抛出的业务异常
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 捕获业务异常
     * @param ex
     * @return
     */
    @ExceptionHandler
    public Result exceptionHandler(BaseException ex){
        log.error("异常信息：{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }


    /**
     * 处理用户名重复异常
     * @param ex
     * @return
     */
    @ExceptionHandler

    public Result exceptionHandler(SQLIntegrityConstraintViolationException ex){
        //Duplicate entry 'abc' for key 'employee.idx_username'
        String msg=ex.getMessage();
        if(msg.contains("Duplicate entry ")){
            String[] split=msg.split(" ");
            String userName=split[2];
            String showMsg="用户名"+userName+"已存在";
            return Result.error(showMsg);
        }else{
            return Result.error(MessageConstant.UNKNOWN_ERROR);
        }


    }


}
