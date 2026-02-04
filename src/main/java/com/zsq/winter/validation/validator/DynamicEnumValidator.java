package com.zsq.winter.validation.validator;

import com.zsq.winter.validation.provider.DictDataProvider;
import com.zsq.winter.validation.annotation.DynamicEnum;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.*;

@Slf4j
public class DynamicEnumValidator implements ConstraintValidator<DynamicEnum, Object> {
    private final DictDataProvider provider;
    private String dictType;
    private boolean reverse;
    private Set<String> fixedValues;
    private boolean allowNull;

    public DynamicEnumValidator(DictDataProvider provider) {
        this.provider = provider;
    }

    @Override
    public void initialize(DynamicEnum constraintAnnotation) {
        this.dictType = constraintAnnotation.dictType();
        this.reverse = constraintAnnotation.reverse();
        this.fixedValues = new HashSet<>(Arrays.asList(constraintAnnotation.fixedValues()));
        this.allowNull = constraintAnnotation.allowNull();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return allowNull;
        }

        String inputVal = String.valueOf(value);
        Set<String> allowedValues = getAllowedValues();

        boolean isValid = allowedValues.contains(inputVal);

        if (!isValid) {
            // 1. 注入动态参数 (Inject dynamic parameters)
            if (context instanceof HibernateConstraintValidatorContext) {
                context.unwrap(HibernateConstraintValidatorContext.class)
                        .addMessageParameter("allowedValues", allowedValues.toString())
                        .addMessageParameter("input", inputVal);
            }

            // 2. 处理默认消息 (Handle default message)
            // 必须与 @DynamicEnum 中的 default message 保持完全一致
            String annotationDefaultMsg = "Value is not in the allowed range";
            String currentTemplate = context.getDefaultConstraintMessageTemplate();

            // 如果用户使用默认消息，则替换为带详情的英文动态消息
            if (annotationDefaultMsg.equals(currentTemplate)) {
                context.disableDefaultConstraintViolation();
                // 修改此处：构建英文的动态详情消息
                context.buildConstraintViolationWithTemplate("Invalid value '{input}', allowed values are: {allowedValues}")
                        .addConstraintViolation();
            }
        }

        return isValid;
    }

    private Set<String> getAllowedValues() {
        Set<String> result = new HashSet<>(fixedValues);
        if (provider != null) {
            try {
                Collection<String> values = provider.getDictValues(dictType, reverse);
                if (values != null) {
                    result.addAll(values);
                }
            } catch (Exception e) {
                log.error("Failed to get dict type '{}'", dictType, e);
            }
        }
        return result;
    }
}