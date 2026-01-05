package com.zsq.winter.validation.validator;

import com.zsq.winter.validation.annotation.DynamicEnum;
import com.zsq.winter.validation.provider.DictDataProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.*;

/**
 * 动态枚举校验器实现（单 Provider 版本）
 * <p>
 * 仅支持注入一个 DictDataProvider。
 * 如果容器中存在多个 DictDataProvider 实现，启动会报错。
 * </p>
 */
@Slf4j
public class DynamicEnumValidator implements ConstraintValidator<DynamicEnum, Object> {

    // 关键修改：只注入单个 Bean
    // required = false 表示如果没有定义 Bean，则不报错，仅使用 fixedValues
    @Autowired(required = false)
    private DictDataProvider provider;

    private String dictType;
    private Set<String> fixedValues;
    private boolean allowNull;

    @Override
    public void initialize(DynamicEnum constraintAnnotation) {
        this.dictType = constraintAnnotation.dictType();
        this.fixedValues = new HashSet<>(Arrays.asList(constraintAnnotation.fixedValues()));
        this.allowNull = constraintAnnotation.allowNull();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return allowNull;
        }

        String inputVal = String.valueOf(value);

        // 获取允许的值
        Set<String> allowedValues = getAllowedValues();

        boolean isValid = allowedValues.contains(inputVal);

        if (!isValid) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                            String.format("输入值 '%s' 无效，允许的值为: %s", inputVal, allowedValues))
                    .addConstraintViolation();
        }

        return isValid;
    }

    /**
     * 获取最终允许的值
     */
    private Set<String> getAllowedValues() {
        // 1. 基础是固定值
        Set<String> result = new HashSet<>(fixedValues);

        // 2. 如果存在唯一的 Provider，调用它获取动态值
        if (provider != null) {
            try {
                Collection<String> values = provider.getDictValues(dictType);
                if (values != null) {
                    result.addAll(values);
                }
            } catch (Exception e) {
                log.error("获取字典类型 '{}' 失败", dictType, e);
            }
        }

        return result;
    }
}