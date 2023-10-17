package com.sky.aspect;

import com.sky.annotation.AutoFill;
import com.sky.constant.AutoFillConstant;
import com.sky.context.BaseContext;
import com.sky.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 自定义切面，实现公共字段统一填充管理
 */
@Aspect
@Component //确保在包扫描时能注入容器
@Slf4j
public class AutoFillAspect {
    /**
     * 切入点
     */
    @Pointcut("execution(* com.sky.mapper.*.*(..)) && @annotation(com.sky.annotation.AutoFill)")
    public void autoFillPointCut(){}

    /**
     * 前置通知，在通知中设置公共字段赋值
     */
    @Before("autoFillPointCut()")
    public void autoFill(JoinPoint joinPoint){
        //获取当前拦截方法的操作类型
        MethodSignature signature = (MethodSignature) joinPoint.getSignature(); //获取当前方法的方法签名
        AutoFill annotation = signature.getMethod().getAnnotation(AutoFill.class);
        OperationType operationType=annotation.value();

        //获取当前拦截方法的参数，实体对象(约定为第一个
        Object[] args=joinPoint.getArgs();
        if(args==null || args.length==0){
            return;
        }
        Object entity = args[0];


        //准备赋值的数据
        Long id= BaseContext.getCurrentId();
        LocalDateTime now = LocalDateTime.now();


        //根据当前不同的类型，通过属性的反射来赋值
        if(operationType==OperationType.INSERT){

            try {
                Method setUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);
                Method setCreateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_TIME, LocalDateTime.class);
                Method setCreateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_USER, Long.class);

                setCreateUser.invoke(entity,id);
                setCreateTime.invoke(entity,now);
                setUpdateUser.invoke(entity,id);
                setUpdateTime.invoke(entity,now);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }else if(operationType==OperationType.UPDATE){
            try {
                Method setUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);

                setUpdateUser.invoke(entity,id);
                setUpdateTime.invoke(entity,now);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


    }
}
