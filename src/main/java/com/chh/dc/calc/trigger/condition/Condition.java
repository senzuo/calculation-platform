package com.chh.dc.calc.trigger.condition;

import com.chh.dc.calc.reader.DataPackage;

import java.util.Map;

/**
 * Created by niow on 16/10/7.
 */
public interface Condition {

    /**
     * 判断是否满足条件
     * @param dataPackage
     * @return 如果判断满足条件,则返回对应参数,否则返回null
     */
    public Map<String,Object> check(DataPackage dataPackage);
}
