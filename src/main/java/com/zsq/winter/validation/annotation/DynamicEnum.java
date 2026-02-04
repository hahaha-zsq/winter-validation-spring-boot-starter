package com.zsq.winter.validation.annotation;

import com.zsq.winter.validation.validator.DynamicEnumValidator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

/**
 * 动态枚举值校验注解
 * 支持从 Bean 中动态获取值，也支持配置固定值
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = DynamicEnumValidator.class)
public @interface DynamicEnum {

    String message() default "Value is not in the allowed range";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * 字典类型标识
     * 将传递给 DictDataProvider.getDictValues(dictType)
     */
    String dictType();

    /**
     * 是否反转枚举值
     * 默认情况下，枚举值是作为字典的 value 来使用的
     * 如果设置为 true，则将枚举值作为字典的 key 来使用
     */
    boolean reverse() default false; //

    /**
     * 固定的枚举值（作为补充）
     * 最终允许的值 = 动态获取的值 + fixedValues
     */
    String[] fixedValues() default {};

    /**
     * 是否允许为 null
     */
    boolean allowNull() default false;
}