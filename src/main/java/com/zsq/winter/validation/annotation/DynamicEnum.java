package com.zsq.winter.validation.annotation;

import com.zsq.winter.validation.validator.DynamicEnumValidator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

/**
 * 动态枚举值校验注解
 * 
 * @author dadandiaoming
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = DynamicEnumValidator.class)
public @interface DynamicEnum {
    
    /**
     * 错误消息
     */
    String message() default "枚举值校验失败，不在允许的值范围内";
    
    /**
     * 分组
     */
    Class<?>[] groups() default {};
    
    /**
     * 负载
     */
    Class<? extends Payload>[] payload() default {};
    
    /**
     * 字典类型/枚举类型标识
     */
    String dictType();
    
    /**
     * 固定的枚举值（当没有自定义字典提供者时使用）
     */
    String[] values() default {};
    
    /**
     * 是否允许空值
     */
    boolean allowNull() default false;
    
    /**
     * 是否忽略大小写
     */
    boolean ignoreCase() default false;
}