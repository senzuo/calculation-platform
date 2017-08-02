package com.chh.dc.calc.trigger.condition;

import com.chh.dc.calc.exporter.JDBCExporter;
import com.chh.dc.calc.exporter.RedisExporter;
import com.chh.dc.calc.reader.DataPackage;
import com.chh.dc.calc.util.OBDAlarmCodeConverter;
import com.chh.dc.calc.util.SerializeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by niow on 16/10/24.
 */
public class HtwxAlarmCondition implements Condition {

    private static final Logger log = LoggerFactory.getLogger(HtwxAlarmCondition.class);

    private JDBCExporter jdbcExporter;

    private RedisExporter redisExporter;

    private byte[] keys;

    public HtwxAlarmCondition(String pushKey) {
        this.keys = pushKey.getBytes();
    }


    private String deviceType = "3";

    @Override
    public Map<String, Object> check(DataPackage dataPackage) {
        Map<String, Object> alarmData = SerializeUtil.unserialize((byte[]) dataPackage.getData(), Map.class);
        String deviceId = (String) alarmData.get("device_id");
        String alarmType = (String) alarmData.get("alarm_type");
        int alarmId = OBDAlarmCodeConverter.htwxAlarm(alarmType);
        if (alarmId == 0) {
            log.warn("告警类型不匹配{}",alarmType);
            return null;
        }
        String uid = deviceType + deviceId;
        Map<String, Object> alarmMap = new HashMap<>();
        alarmMap.put("id", UUID.randomUUID().toString());
        alarmMap.put("latitude", 38.13156);
        alarmMap.put("longitude", 162.46545);
        alarmMap.put("device_uid", uid);
        alarmMap.put("device_uid", uid);
        alarmMap.put("warning_type", alarmId);
        alarmMap.put("warning_time", alarmData.get("utctime"));
        alarmMap.put("warning_value", alarmData.get("alarm_desc"));
        alarmMap.put("create_time", new Date());
        alarmMap.put("warning_desc", OBDAlarmCodeConverter.getHtwxAlarmDesc(alarmId));
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
        return null;
    }

    public JDBCExporter getJdbcExporter() {
        return jdbcExporter;
    }

    public void setJdbcExporter(JDBCExporter jdbcExporter) {
        this.jdbcExporter = jdbcExporter;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public RedisExporter getRedisExporter() {
        return redisExporter;
    }

    public void setRedisExporter(RedisExporter redisExporter) {
        this.redisExporter = redisExporter;
    }
}
