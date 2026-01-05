package com.zsq.winter.validation.validator;

import com.zsq.winter.validation.annotation.DynamicEnum;
import com.zsq.winter.validation.provider.DictDataProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.*;

/**
 * 动态枚举值校验器
 * 
 * @author dadandiaoming
 */
@Slf4j
public class DynamicEnumValidator implements ConstraintValidator<DynamicEnum, Object> {
    
    @Autowired(required = false)
    private List<DictDataProvider> dictDataProviders;
    
    private String dictType;
    private String[] fixedValues;
    private boolean allowNull;
    private boolean ignoreCase;
    
    @Override
    public void initialize(DynamicEnum constraintAnnotation) {
        this.dictType = constraintAnnotation.dictType();
        this.fixedValues = constraintAnnotation.values();
        this.allowNull = constraintAnnotation.allowNull();
        this.ignoreCase = constraintAnnotation.ignoreCase();
    }
    
    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        // 空值处理
        if (value == null) {
            return allowNull;
        }
        
        String stringValue = value.toString();
        if (!StringUtils.hasText(stringValue)) {
            return allowNull;
        }
        
        // 获取最终的允许值列表（动态值 + 固定值的并集）
        List<String> allowedValues = getAllowedValues();
        
        // 如果没有任何允许的值，则校验失败
        if (allowedValues.isEmpty()) {
            log.warn("字典类型 {} 没有配置任何允许的值", dictType);
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    String.format("字典类型 '%s' 没有配置任何允许的值", dictType))
                   .addConstraintViolation();
            return false;
        }
        
        // 执行校验
        boolean isValid = allowedValues.stream()
                .anyMatch(allowedValue -> compareValues(stringValue, allowedValue));
        
        if (!isValid) {
            // 自定义错误消息，包含允许的值
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    String.format("值 '%s' 不在允许的范围内，允许的值: %s", stringValue, allowedValues))
                   .addConstraintViolation();
        }
        
        return isValid;
    }
    
    /**
     * 获取所有允许的值（动态值 + 固定值的并集）
     */
    private List<String> getAllowedValues() {
        Set<String> allValues = new LinkedHashSet<>(); // 使用LinkedHashSet保持顺序并去重
        
        // 1. 获取动态字典值
        List<String> dynamicValues = getDynamicValues();
        if (dynamicValues != null && !dynamicValues.isEmpty()) {
            allValues.addAll(dynamicValues);
            log.debug("添加动态字典值: {}", dynamicValues);
        }
        
        // 2. 获取固定枚举值
        if (fixedValues != null && fixedValues.length > 0) {
            List<String> fixedList = Arrays.asList(fixedValues);
            allValues.addAll(fixedList);
            log.debug("添加固定枚举值: {}", fixedList);
        }
        
        List<String> result = new ArrayList<>(allValues);
        log.debug("字典类型 {} 的最终允许值: {}", dictType, result);
        
        return result;
    }
    
    /**
     * 获取动态字典值
     */
    private List<String> getDynamicValues() {
        if (dictDataProviders == null || dictDataProviders.isEmpty()) {
            log.debug("没有找到字典数据提供者");
            return null;
        }
        
        for (DictDataProvider provider : dictDataProviders) {
            if (provider.supports(dictType)) {
                List<String> values = provider.getDictValues(dictType);
                log.debug("从提供者 {} 获取字典类型 {} 的值: {}", 
                         provider.getClass().getSimpleName(), dictType, values);
                return values;
            }
        }
        
        log.debug("没有找到支持字典类型 {} 的提供者", dictType);
        return null;
    }
    
    /**
     * 比较值
     */
    private boolean compareValues(String value, String allowedValue) {
        if (ignoreCase) {
            return value.equalsIgnoreCase(allowedValue);
        } else {
            return value.equals(allowedValue);
        }
    }
}