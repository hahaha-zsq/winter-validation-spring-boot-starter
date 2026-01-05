package com.zsq.winter.validation.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 默认字典数据提供者实现
 * 当用户没有自定义实现时使用此默认实现
 * 
 * @author dadandiaoming
 */
@Slf4j
@Component
public class DefaultDictDataProvider implements DictDataProvider {
    
    @Override
    public List<String> getDictValues(String dictType) {
        log.debug("使用默认字典数据提供者，字典类型: {}", dictType);
        // 默认实现返回空列表，实际的值将从注解的values属性中获取
        return Collections.emptyList();
    }
    
    @Override
    public boolean supports(String dictType) {
        // 默认实现不支持任何字典类型，将使用注解中的固定值
        return false;
    }
}