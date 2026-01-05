package com.zsq.winter.validation.validator;

import com.zsq.winter.validation.annotation.SpelValid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * SpEL 校验器实现
 */
@Slf4j
public class SpelValidator implements ConstraintValidator<SpelValid, Object> {

    // 线程安全的 Parser
    private static final ExpressionParser PARSER = new SpelExpressionParser();

    private String spelExpression;

    @Override
    public void initialize(SpelValid constraintAnnotation) {
        this.spelExpression = constraintAnnotation.value();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        // JSR 303 惯例：null 值通常跳过校验（除非配合 @NotNull）
        if (value == null) {
            return true;
        }

        try {
            StandardEvaluationContext evalContext = new StandardEvaluationContext();

            // 设置根对象
            // 1. 如果注解在类上，value 就是整个 Bean，#root 可访问所有字段
            // 2. 如果注解在字段上，value 就是该字段的值
            evalContext.setRootObject(value);

            // 提供别名方便使用
            evalContext.setVariable("this", value);

            Expression exp = PARSER.parseExpression(spelExpression);
            Boolean result = exp.getValue(evalContext, Boolean.class);

            if (result == null) {
                log.warn("SpEL表达式结果为null: {}", spelExpression);
                return false;
            }

            return result;

        } catch (Exception e) {
            // 发生异常（如字段不存在、语法错误）视为校验失败
            log.error("SpEL校验执行异常. 表达式: '{}', 值类型: '{}'",
                    spelExpression, value.getClass().getSimpleName(), e);
            return false;
        }
    }
}