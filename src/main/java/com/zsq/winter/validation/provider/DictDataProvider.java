package com.zsq.winter.validation.provider;

import java.util.List;

/**
 * 字典数据提供者接口
 * 用户可以实现此接口来提供动态的字典数据
 * 
 * @author dadandiaoming
 */
public interface DictDataProvider {
    
    /**
     * 根据字典类型获取字典值列表
     * 
     * @param dictType 字典类型
     * @return 字典值列表，如果没有找到返回null或空列表
     */
    List<String> getDictValues(String dictType);
    
    /**
     * 检查指定字典类型是否支持
     * 
     * @param dictType 字典类型
     * @return 是否支持
     */
    default boolean supports(String dictType) {
        List<String> values = getDictValues(dictType);
        return values != null && !values.isEmpty();
    }
}