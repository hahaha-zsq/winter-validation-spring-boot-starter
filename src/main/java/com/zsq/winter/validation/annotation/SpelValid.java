package com.zsq.winter.validation.annotation;

import com.zsq.winter.validation.validator.SpelValidator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

/**
 * SpEL 表达式校验注解
 * * 最佳实践：
 * 1. 放在类（Type）上：#root 代表当前对象，可进行跨字段校验（如 start < end）
 * 2. 放在字段（Field）上：#root/#this 代表当前字段值
 */
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = SpelValidator.class)
@Repeatable(SpelValid.List.class)
public @interface SpelValid {

    String message() default "SpEL表达式校验失败";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * SpEL 表达式
     * 例：#root.age > 18
     */
    String value();

    /**
     * 描述，用于文档或日志
     */
    String description() default "";

    @Target({ElementType.TYPE, ElementType.FIELD, ElementType.PARAMETER})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @interface List {
        SpelValid[] value();
    }
}