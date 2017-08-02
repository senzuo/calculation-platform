package com.chh.dc.calc.trigger.condition;

import com.chh.dc.calc.util.SerializeUtil;
import com.chh.dc.calc.reader.DataPackage;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by niow on 16/10/12.
 */
public class HtwxLostCondition implements Condition{

    @Override
    public Map<String, Object> check(DataPackage dataPackage) {
        Map<String,Object> data = SerializeUtil.unserialize((byte[])dataPackage.getData(),Map.class);
        String alarmType = (String) data.get("alarm_type");
        if ("lost".equals(alarmType)) {
            Map<String, Object> rs = new HashMap<String, Object>();
            rs.putAll(data);
            return rs;
        }
        return null;
    }
}
