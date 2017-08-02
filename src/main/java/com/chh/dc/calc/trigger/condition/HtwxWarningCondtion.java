package com.chh.dc.calc.trigger.condition;

import com.chh.dc.calc.util.SerializeUtil;
import com.chh.dc.calc.reader.DataPackage;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by niow on 16/10/8.
 */
public class HtwxWarningCondtion implements Condition {

    private int warningType;

    @Override
    public Map<String, Object> check(DataPackage dataPackage) {
        Map<String,Object> data = SerializeUtil.unserialize((byte[])dataPackage.getData(),Map.class);
        Integer wt = (Integer) data.get("warning_type");
        if (wt!=null&&warningType==wt) {
            Map<String, Object> rs = new HashMap<String, Object>();
            rs.putAll(data);
            return rs;
        }
        return null;
    }


    public int getWarningType() {
        return warningType;
    }

    public void setWarningType(int warningType) {
        this.warningType = warningType;
    }
}
