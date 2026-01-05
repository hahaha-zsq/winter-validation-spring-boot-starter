package com.zsq.winter.validation.validator;

import com.zsq.winter.validation.annotation.SpelValidation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * SpEL表达式校验器
 * 
 * @author dadandiaoming
 */
@Slf4j
public class SpelValidator implements ConstraintValidator<SpelValidation, Object> {
    
    private String expression;
    private String description;
    private ExpressionParser parser;
    
    /**
     * 用于存储根对象的ThreadLocal
     */
    private static final ThreadLocal<Object> ROOT_OBJECT_HOLDER = new ThreadLocal<>();
    
    @Override
    public void initialize(SpelValidation constraintAnnotation) {
        this.expression = constraintAnnotation.expression();
        this.description = constraintAnnotation.description();
        this.parser = new SpelExpressionParser();
    }
    
    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (expression == null || expression.trim().isEmpty()) {
            log.warn("SpEL表达式为空，校验通过");
            return true;
        }
        
        try {
            Expression exp = parser.parseExpression(expression);
            EvaluationContext evaluationContext = new StandardEvaluationContext();
            
            // 设置变量
            evaluationContext.setVariable("value", value);
            
            // 尝试获取根对象
            Object rootObject = getRootObject(value, context);
            evaluationContext.setVariable("root", rootObject);
            
            // 如果有根对象，也设置为上下文的根对象
            if (rootObject != null) {
                evaluationContext = new StandardEvaluationContext(rootObject);
                evaluationContext.setVariable("value", value);
                evaluationContext.setVariable("root", rootObject);
            }
            
            // 执行表达式
            Boolean result = exp.getValue(evaluationContext, Boolean.class);
            
            if (result == null) {
                log.warn("SpEL表达式执行结果为null，表达式: {}", expression);
                return false;
            }
            
            if (!result && description != null && !description.trim().isEmpty()) {
                // 自定义错误消息
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(description)
                       .addConstraintViolation();
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("SpEL表达式执行异常，表达式: {}, 错误: {}", expression, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 获取根对象（整个被校验的对象）
     * 对于字段级校验，尝试通过反射获取包含该字段的对象
     */
    private Object getRootObject(Object value, ConstraintValidatorContext context) {
        // 首先尝试从ThreadLocal获取
        Object rootObject = ROOT_OBJECT_HOLDER.get();
        if (rootObject != null) {
            return rootObject;
        }
        
        // 对于类级别的校验，value就是根对象
        if (value != null && !isPrimitiveOrWrapper(value.getClass())) {
            return value;
        }
        
        // 对于字段级校验，这里无法直接获取根对象
        // 在实际使用中，可以通过自定义的校验拦截器来设置根对象
        return null;
    }
    
    /**
     * 判断是否为基本类型或包装类型
     */
    private boolean isPrimitiveOrWrapper(Class<?> clazz) {
        return clazz.isPrimitive() || 
               clazz == String.class ||
               clazz == Boolean.class ||
               clazz == Integer.class ||
               clazz == Long.class ||
               clazz == Double.class ||
               clazz == Float.class ||
               clazz == Short.class ||
               clazz == Byte.class ||
               clazz == Character.class;
    }
    
    /**
     * 设置根对象到ThreadLocal（供外部调用）
     */
    public static void setRootObject(Object rootObject) {
        ROOT_OBJECT_HOLDER.set(rootObject);
    }
    
    /**
     * 清除ThreadLocal中的根对象
     */
    public static void clearRootObject() {
        ROOT_OBJECT_HOLDER.remove();
    }
}