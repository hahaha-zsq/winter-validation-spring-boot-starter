package com.zsq.winter.validation.util;

import com.zsq.winter.validation.validator.SpelValidator;
import lombok.extern.slf4j.Slf4j;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Set;

/**
 * 校验工具类
 * 提供便捷的校验方法
 * 
 * @author dadandiaoming
 */
@Slf4j
public class ValidationUtils {
    
    private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = VALIDATOR_FACTORY.getValidator();
    
    /**
     * 校验对象
     * 
     * @param object 要校验的对象
     * @param <T> 对象类型
     * @return 校验结果集合
     */
    public static <T> Set<ConstraintViolation<T>> validate(T object) {
        return validate(object, (Class<?>[]) null);
    }
    
    /**
     * 校验对象（指定分组）
     * 
     * @param object 要校验的对象
     * @param groups 校验分组
     * @param <T> 对象类型
     * @return 校验结果集合
     */
    public static <T> Set<ConstraintViolation<T>> validate(T object, Class<?>... groups) {
        // 设置根对象到ThreadLocal，供SpEL校验器使用
        SpelValidator.setRootObject(object);
        
        try {
            if (groups == null || groups.length == 0) {
                return VALIDATOR.validate(object);
            } else {
                return VALIDATOR.validate(object, groups);
            }
        } finally {
            // 清除ThreadLocal
            SpelValidator.clearRootObject();
        }
    }
    
    /**
     * 校验对象属性
     * 
     * @param object 要校验的对象
     * @param propertyName 属性名
     * @param <T> 对象类型
     * @return 校验结果集合
     */
    public static <T> Set<ConstraintViolation<T>> validateProperty(T object, String propertyName) {
        return validateProperty(object, propertyName, (Class<?>[]) null);
    }
    
    /**
     * 校验对象属性（指定分组）
     * 
     * @param object 要校验的对象
     * @param propertyName 属性名
     * @param groups 校验分组
     * @param <T> 对象类型
     * @return 校验结果集合
     */
    public static <T> Set<ConstraintViolation<T>> validateProperty(T object, String propertyName, Class<?>... groups) {
        // 设置根对象到ThreadLocal
        SpelValidator.setRootObject(object);
        
        try {
            if (groups == null || groups.length == 0) {
                return VALIDATOR.validateProperty(object, propertyName);
            } else {
                return VALIDATOR.validateProperty(object, propertyName, groups);
            }
        } finally {
            // 清除ThreadLocal
            SpelValidator.clearRootObject();
        }
    }
    
    /**
     * 检查对象是否校验通过
     * 
     * @param object 要校验的对象
     * @param <T> 对象类型
     * @return 是否校验通过
     */
    public static <T> boolean isValid(T object) {
        Set<ConstraintViolation<T>> violations = validate(object);
        return violations.isEmpty();
    }
    
    /**
     * 获取校验错误信息
     * 
     * @param object 要校验的对象
     * @param <T> 对象类型
     * @return 错误信息字符串
     */
    public static <T> String getValidationMessage(T object) {
        Set<ConstraintViolation<T>> violations = validate(object);
        if (violations.isEmpty()) {
            return null;
        }
        
        StringBuilder sb = new StringBuilder();
        for (ConstraintViolation<T> violation : violations) {
            if (sb.length() > 0) {
                sb.append("; ");
            }
            sb.append(violation.getPropertyPath()).append(": ").append(violation.getMessage());
        }
        
        return sb.toString();
    }
}