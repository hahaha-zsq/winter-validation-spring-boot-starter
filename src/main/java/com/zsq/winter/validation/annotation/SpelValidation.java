package com.zsq.winter.validation.annotation;

import com.zsq.winter.validation.validator.SpelValidator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

/**
 * 基于SpEL表达式的自定义校验注解
 * 
 * @author dadandiaoming
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = SpelValidator.class)
public @interface SpelValidation {
    
    /**
     * 错误消息
     */
    String message() default "SpEL表达式校验失败";
    
    /**
     * 分组
     */
    Class<?>[] groups() default {};
    
    /**
     * 负载
     */
    Class<? extends Payload>[] payload() default {};
    
    /**
     * SpEL表达式，返回true表示校验通过
     * 可以使用 #value 引用当前字段值
     * 可以使用 #root 引用整个对象
     */
    String expression();
    
    /**
     * 表达式描述，用于错误提示
     */
    String description() default "";
}