package com.zsq.winter.validation.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 校验功能配置属性
 * 
 * @author dadandiaoming
 */
@Data
@ConfigurationProperties(prefix = "winter.validation")
public class ValidationProperties {
    
    /**
     * 是否启用校验功能
     */
    private boolean enabled = true;
    
    /**
     * 是否启用SpEL校验
     */
    private boolean spelEnabled = true;
    
    /**
     * 是否启用动态枚举校验
     */
    private boolean dynamicEnumEnabled = true;
    
    /**
     * 是否启用AOP切面
     */
    private boolean aopEnabled = true;
    
    /**
     * 是否打印详细日志
     */
    private boolean verbose = false;
}