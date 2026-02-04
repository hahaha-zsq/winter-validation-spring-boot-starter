package com.zsq.winter.validation.config;

import com.zsq.winter.validation.provider.DictDataProvider;
import com.zsq.winter.validation.validator.DynamicEnumValidator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.*;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.Collection;
import java.util.Collections;
import java.util.Properties;

/**
 * 校验功能自动配置类
 * <p>
 * 功能说明：
 * 1. 替代传统的静态工具类（如 ValidatorUtil），完全基于 Spring 容器管理。
 * 2. 提供两种不同策略的校验器实例：全量校验（默认）和 快速失败校验。
 * 3. 完美集成国际化（i18n）和 Spring 的依赖注入（DI）功能。
 * </p>
 *
 * @author dadandiaoming
 */
@Configuration
public class ValidationAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(DictDataProvider.class)
    public DictDataProvider defaultDictDataProvider() {
        return new DictDataProvider() {
            @Override
            public Collection<String> getDictValues(String dictType, boolean reverse) {
                return Collections.emptyList();
            }
        };
    }
    @Bean
    public DynamicEnumValidator dynamicEnumValidator(DictDataProvider dictDataProvider) {
        return new DynamicEnumValidator(dictDataProvider);
    }
    /**
     * Bean 1: 全量校验器 (ValidatorAll) —— 默认校验器
     * <p>
     * <b>配置策略：</b> failFast = false <br>
     * <b>行为描述：</b> 校验实体类的所有字段，收集所有违反约束的错误信息并统一返回。<br>
     * <b>适用场景：</b> 前端表单提交，用户希望一次性看到所有填写错误的字段，而不是改一个错一个。<br>
     * </p>
     *
     * @param messageSource 国际化消息源，用于解析验证注解中的 {key}
     * @return 默认的 Spring 校验器工厂 Bean
     */
    @Bean("fastFalseValidator")
    public LocalValidatorFactoryBean fastFalseValidator(@Lazy MessageSource messageSource) {
        LocalValidatorFactoryBean validatorFactoryBean = new LocalValidatorFactoryBean();

        // 1. 注入国际化消息源
        // 【关键点】：使用 @Lazy 注解。
        // 原因：MessageSource 通常依赖数据库（Repository），而 Repository 可能又依赖 Validator 进行数据校验。
        // 不加 @Lazy 会导致 "MessageSource -> Repository -> Validator -> MessageSource" 的死循环依赖导致启动报错。
        // 加上 @Lazy 后，Spring 会注入一个代理对象，直到真正进行校验时才去获取 MessageSource 实例。
        validatorFactoryBean.setValidationMessageSource(messageSource);

        // 2. 配置 Hibernate Validator 特有属性
        Properties properties = new Properties();
        // hibernate.validator.fail_fast = false : 关闭快速失败模式，进行全量校验
        properties.setProperty("hibernate.validator.fail_fast", "false");

        validatorFactoryBean.setValidationProperties(properties);

        // 3. 关于依赖注入支持
        // LocalValidatorFactoryBean 默认会自动设置 SpringConstraintValidatorFactory。
        // 这意味着：在你自定义的校验器（如 @CheckPhone）实现类中，可以直接使用 @Autowired 注入 Service 或 Mapper。
        return validatorFactoryBean;
    }

    /**
     * Bean 2: 快速校验器 (ValidatorFast)
     * <p>
     * <b>配置策略：</b> failFast = true <br>
     * <b>行为描述：</b> 只要发现第一个违反约束的字段，立即停止校验并抛出异常/返回结果。<br>
     * <b>适用场景：</b>
     * 1. 批量数据导入（如 Excel 导入），为了性能考虑，或者一行数据只要有一个错就认为该行无效。
     * 2. 内部微服务调用参数检查，追求极致性能。
     * </p>
     * <b>使用方式：</b> 需要显式指定名称注入：<br>
     * {@code @Autowired @Qualifier("fastTrueValidator") Validator validator;}
     *
     * @param messageSource 国际化消息源
     * @return 快速失败模式的校验器工厂 Bean
     */
    @Bean("fastTrueValidator")
    public LocalValidatorFactoryBean fastTrueValidator(@Lazy MessageSource messageSource) {
        LocalValidatorFactoryBean validatorFactoryBean = new LocalValidatorFactoryBean();

        // 同样注入国际化消息源，支持返回国际化的错误提示
        validatorFactoryBean.setValidationMessageSource(messageSource);

        Properties properties = new Properties();
        // hibernate.validator.fail_fast = true : 开启快速失败模式
        properties.setProperty("hibernate.validator.fail_fast", "true");

        validatorFactoryBean.setValidationProperties(properties);

        return validatorFactoryBean;
    }
}