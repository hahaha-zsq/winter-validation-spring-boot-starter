package com.zsq.winter.validation.aspect;

import com.zsq.winter.validation.validator.SpelValidator;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.validation.Valid;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * 校验切面
 * 用于在校验执行前设置根对象，校验后清理ThreadLocal
 * 
 * @author dadandiaoming
 */
@Slf4j
@Aspect
@Component
@Order(1) // 确保在校验之前执行
public class ValidationAspect {
    
    /**
     * 切入点：匹配所有Controller中的方法
     */
    @Pointcut("@within(org.springframework.web.bind.annotation.RestController) || " +
              "@within(org.springframework.stereotype.Controller)")
    public void controllerMethods() {}
    
    /**
     * 切入点：匹配所有Service中的方法
     */
    @Pointcut("@within(org.springframework.stereotype.Service)")
    public void serviceMethods() {}
    
    /**
     * 切入点：匹配所有带有校验注解参数的方法
     */
    @Pointcut("execution(* *(.., @javax.validation.Valid (*), ..))")
    public void validMethodsWithValid() {}
    
    /**
     * 切入点：匹配所有带有Validated注解参数的方法
     */
    @Pointcut("execution(* *(.., @org.springframework.validation.annotation.Validated (*), ..))")
    public void validMethodsWithValidated() {}
    
    /**
     * 组合切入点：Controller或Service中带有校验注解的方法
     */
    @Pointcut("(controllerMethods() || serviceMethods()) && (validMethodsWithValid() || validMethodsWithValidated())")
    public void validationMethods() {}
    
    /**
     * 在校验执行前设置根对象
     */
    @Before("validationMethods()")
    public void beforeValidation(JoinPoint joinPoint) {
        try {
            Object[] args = joinPoint.getArgs();
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            Parameter[] parameters = method.getParameters();
            
            if (args != null && parameters != null) {
                // 查找带有校验注解的参数
                for (int i = 0; i < Math.min(args.length, parameters.length); i++) {
                    if (hasValidationAnnotation(parameters[i]) && args[i] != null) {
                        log.debug("设置校验根对象: {} 在方法: {}", 
                                args[i].getClass().getSimpleName(), 
                                method.getName());
                        SpelValidator.setRootObject(args[i]);
                        break; // 只设置第一个校验对象作为根对象
                    }
                }
            }
        } catch (Exception e) {
            log.warn("设置校验根对象时发生异常: {}", e.getMessage());
        }
    }
    
    /**
     * 在方法执行后清理ThreadLocal
     */
    @After("validationMethods()")
    public void afterValidation(JoinPoint joinPoint) {
        try {
            log.debug("清理校验根对象，方法: {}", joinPoint.getSignature().getName());
            SpelValidator.clearRootObject();
        } catch (Exception e) {
            log.warn("清理校验根对象时发生异常: {}", e.getMessage());
        }
    }
    
    /**
     * 检查参数是否有校验注解
     */
    private boolean hasValidationAnnotation(Parameter parameter) {
        Annotation[] annotations = parameter.getAnnotations();
        for (Annotation annotation : annotations) {
            String annotationName = annotation.annotationType().getSimpleName();
            if (annotation instanceof Valid || 
                "Valid".equals(annotationName) ||
                "Validated".equals(annotationName)) {
                return true;
            }
        }
        return false;
    }
}