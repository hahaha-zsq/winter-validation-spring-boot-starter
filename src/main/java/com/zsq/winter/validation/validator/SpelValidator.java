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
 * SpEL 校验器实现 (已优化性能)
 */
@Slf4j
public class SpelValidator implements ConstraintValidator<SpelValid, Object> {

    // 解析器是线程安全的，静态常量即可
    private static final ExpressionParser PARSER = new SpelExpressionParser();

    // 成员变量：每个 Validator 实例独享一个缓存的表达式
    private Expression cachedExpression;

    @Override
    public void initialize(SpelValid constraintAnnotation) {
        String spelString = constraintAnnotation.value();
        try {
            // 【核心优化】初始化时解析一次，后续只使用对象，不再解析字符串
            this.cachedExpression = PARSER.parseExpression(spelString);
        } catch (Exception e) {
            // 如果表达式写错了，启动时就会报错，方便及时发现
            throw new IllegalArgumentException("无效的 SpEL 表达式: " + spelString, e);
        }
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        // 1. null 值默认跳过 (符合 JSR-303 规范)
        // 如果该字段必须有值，请配合 @NotNull 使用
        if (value == null) {
            return true;
        }

        try {
            // 2. 创建上下文 (StandardEvaluationContext 非线程安全，必须每次创建局部变量)
            StandardEvaluationContext evalContext = new StandardEvaluationContext();
            evalContext.setRootObject(value);
            evalContext.setVariable("this", value);

            // 3. 使用缓存的表达式求值
            Boolean result = cachedExpression.getValue(evalContext, Boolean.class);

            // 结果为 null 视为不通过
            return Boolean.TRUE.equals(result);

        } catch (Exception e) {
            // 4. 运行时异常（如字段不存在）视为校验失败
            log.warn("SpEL 校验执行异常. 值类型: {}", value.getClass().getSimpleName(), e);
            return false;
        }
    }
}