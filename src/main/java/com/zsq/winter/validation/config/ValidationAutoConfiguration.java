package com.zsq.winter.validation.config;

import com.zsq.winter.validation.aspect.ValidationAspect;
import com.zsq.winter.validation.provider.DefaultDictDataProvider;
import com.zsq.winter.validation.provider.DictDataProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * 校验功能自动配置类
 * 
 * @author dadandiaoming
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(ValidationProperties.class)
@ConditionalOnProperty(prefix = "winter.validation", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ValidationAutoConfiguration {
    
    /**
     * 默认字典数据提供者
     * 只有在用户没有自定义实现时才会创建
     */
    @Bean
    @ConditionalOnMissingBean(DictDataProvider.class)
    public DefaultDictDataProvider defaultDictDataProvider() {
        log.info("创建默认字典数据提供者");
        return new DefaultDictDataProvider();
    }
    
    /**
     * AOP配置
     */
    @Configuration
    @ConditionalOnClass(name = "org.aspectj.lang.annotation.Aspect")
    @ConditionalOnProperty(prefix = "winter.validation", name = "aop-enabled", havingValue = "true", matchIfMissing = true)
    @EnableAspectJAutoProxy
    static class AopConfiguration {
        
        @Bean
        @ConditionalOnMissingBean(ValidationAspect.class)
        public ValidationAspect validationAspect() {
            log.info("创建校验AOP切面");
            return new ValidationAspect();
        }
    }
}