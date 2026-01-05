package com.zsq.winter.validation.provider;

import java.util.Collection;

/**
 * 字典数据提供者接口
 * 用户需实现此接口并注册为 Bean，以提供动态字典数据
 */
public interface DictDataProvider {

    /**
     * 根据字典类型获取对应的值集合
     * @param dictType 字典类型/Code
     * @return 允许的值集合
     */
    Collection<String> getDictValues(String dictType);

    /**
     * (可选) 是否支持该字典类型
     * 默认实现：只要 getDictValues 返回不为 null 即视为支持
     */
    default boolean supports(String dictType) {
        return getDictValues(dictType) != null;
    }
}