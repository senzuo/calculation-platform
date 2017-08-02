package com.chh.dc.calc.trigger.condition;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.chh.dc.calc.exporter.JDBCExporter;
import com.chh.dc.calc.exporter.RedisExporter;
import com.chh.dc.calc.reader.DataPackage;
import com.chh.dc.calc.util.OBDAlarmCodeConverter;
import com.chh.dc.calc.util.SerializeUtil;

/**
 * Created by niow on 16/10/24.
 */
public class HtwxFaultCodeCondition implements Condition {

    private static final Logger log = LoggerFactory.getLogger(HtwxFaultCodeCondition.class);

    private RedisExporter redisExporter;

    private JDBCExporter jdbcExporter;

    private String deviceType = "3";

    private byte[] keys;
    private static Map<Integer, String> dtcMap = new HashMap<>(); 
    public HtwxFaultCodeCondition(String key) {
        this.keys = key.getBytes();
    }
    static{
    	dtcMap.put(1, "燃油和空气侦查系统");
    	dtcMap.put(2, "点火系统");
    	dtcMap.put(3, "废气控制系统");
    	dtcMap.put(4, "车速怠速控制系统");
    	dtcMap.put(5, "电脑控制系统");
    	dtcMap.put(6, "网络连接系统");
    	dtcMap.put(7, "故障检测");
    	dtcMap.put(8, "车身系统");
    	dtcMap.put(9, "网络系统");
    	dtcMap.put(10, "混合动力驱动系统");
    	dtcMap.put(11, "底盘系统");
    	dtcMap.put(12, "制造商自定义");
    	dtcMap.put(13, "ISO/SAE预留");
    }
    @Override
    public Map<String, Object> check(DataPackage dataPackage) {
        Map<String, Object> faultData = SerializeUtil.unserialize((byte[]) dataPackage.getData(), Map.class);
        String deviceId = (String) faultData.get("device_id");
        String uid = deviceType + deviceId;
        String faults = (String) faultData.get("fault_code");
        if (faults == null || "".equals(faults)) {
            return null;
        }
        String[] faultCodes = faults.split(",");
        for (String faultCode : faultCodes) {
            Map<String, Object> alarmMap = new HashMap<>();
            Map<String, Object> tcMap = new HashMap<>();
            alarmMap.put("id", UUID.randomUUID().toString());
            alarmMap.put("latitude", 38.13156);
            alarmMap.put("longitude", 162.46545);
            alarmMap.put("device_uid", uid);
            alarmMap.put("warning_type", OBDAlarmCodeConverter.HTWX_ALARM_FAULT_CODE);
            alarmMap.put("warning_time", faultData.get("utctime"));
            alarmMap.put("warning_value", faultCode);
            alarmMap.put("create_time", new Date());
            String warningDesc = "";
            String description = "";
//            Map<String, Object> codeInfo = MedicalReport.getDescByCode(faultCode);
//            if(codeInfo != null){
//            	description = (String) codeInfo.get("description");
//            	warningDesc = codeInfo.get("value") + 
//            			" " + dtcMap.get(codeInfo.get("type")) +
//            			" " + description;
//            }
            alarmMap.put("warning_desc", warningDesc);
            tcMap.put("device_uid", uid);
            tcMap.put("dtc_vaule", faultCode);
            tcMap.put("gps_time", faultData.get("utctime"));
            tcMap.put("dtc_description", description);
            tcMap.put("create_time", new Date());
            try {
                jdbcExporter.export("t_device_trouble_code", tcMap);
            } catch (Exception e) {
                log.error("JDBC告警转换输出异常", e);
            }
            try {
                jdbcExporter.export("t_device_warning", alarmMap);
            } catch (Exception e) {
                log.error("JDBC告警转换输出异常", e);
            }
            try {
            	redisExporter.export(RedisExporter.OP_LPUSH, keys, alarmMap, null, null);
            } catch (Exception e) {
                log.error("Redis告警转换输出异常", e);
            }
        }
        return null;
    }

    public RedisExporter getRedisExporter() {
        return redisExporter;
    }

    public void setRedisExporter(RedisExporter redisExporter) {
        this.redisExporter = redisExporter;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public JDBCExporter getJdbcExporter() {
        return jdbcExporter;
    }

    public void setJdbcExporter(JDBCExporter jdbcExporter) {
        this.jdbcExporter = jdbcExporter;
    }
}
