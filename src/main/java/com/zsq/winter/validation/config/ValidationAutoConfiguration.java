package com.zsq.winter.validation.config;

import com.zsq.winter.validation.provider.DictDataProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 校验功能自动配置类
 * 基于Bean Validation标准实现，无需复杂的AOP和ThreadLocal
 * 
 * @author dadandiaoming
 */
@Configuration
// 只需要确保能扫描到 com.zsq.winter.validation 下的 Validator 和切面（如果有）
@ComponentScan(basePackages = "com.zsq.winter.validation")
public class ValidationAutoConfiguration {
}