package com.chh.dc.calc.task;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.chh.dc.calc.exporter.JDBCExporter;
import com.chh.dc.calc.exporter.RedisExporter;
import com.chh.dc.calc.reader.DataReader;
import com.chh.dc.calc.reader.RedisDataReader;
import com.chh.dc.calc.util.SerializeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.chh.dc.calc.reader.DataPackage;

/**
 * 故障码告警任务
 * @author wangbin 
 * @date 2016年11月26日 下午4:46:56    
 * @Description: TODO
 */
public class HtwxFaultTask extends Task {
	private static final Logger log = LoggerFactory.getLogger(HtwxLostTask.class);
	
	private DataReader dataReader;
    private JDBCExporter jdbcExporter;
    private RedisExporter redisExporter;
    private String deviceType = "3";
    private Long interval;//当前GPS信息utctime与故障码告警utctime间隔,最大值（单位/秒）
    private String keys;
    private static final String  LAST_GPS_KEY = "lastGpsCache";//当前设备gps key
	
	/**
	 * 1.故障码告警+当前GPS信息；
	 * 2.告警入库；	
	 * 3.告警推送业务系统		
	 */
	@Override
	public TaskFuture call() throws Exception {
		try {
			Map<String, Object> params = getEvent().getParams();
            String deviceId = (String) params.get("device_id");
            String uid = deviceType + deviceId;
            Date utctime = (Date) params.get("utctime");

            Map<String, Object> warningMap = new HashMap<>();
			//1.通过device_uid从当前GPS缓存(lastGpsCache)获取设备的当前GPS信息；
            DataPackage gpsPack = dataReader.getData(RedisDataReader.OPTION_HGET, LAST_GPS_KEY, uid);
            if(gpsPack != null && gpsPack.getData() != null){
            	byte[] gpsBytes = (byte[]) gpsPack.getData();
                Map<String, Object> gpsData = SerializeUtil.unserialize(gpsBytes, Map.class);
                //2.判断gps数据是否有效，加入当前gps数据
    			if(gpsData != null && isTimeout(utctime, gpsData.get("utctime"))){
    				warningMap.put("longitude", params.get("lon"));
    				warningMap.put("latitude", params.get("lat"));
    				warningMap.put("gps_locate_model", params.get("gps_locate_model"));
    			}
            }
			warningMap.put("id", UUID.randomUUID().toString());
			warningMap.put("device_uid", uid);
			warningMap.put("warning_type", params.get("warning_type"));
			warningMap.put("warning_desc", params.get("warning_desc"));
			warningMap.put("warning_time", params.get("utctime"));
			warningMap.put("warning_value", params.get("warning_value"));
			warningMap.put("create_time", new Date());
			//3.告警入库
            try {
           	 	jdbcExporter.export("t_device_warning", warningMap);
			} catch (Exception e) {
				log.error("JDBC故障告警转换输出异常", e);
			}
            //4.故障告警写入告警推送队列缓存
            try {
            	//modify by fulr 缓存obd_data_warning_queue字段名与汇总表t_device_warning保持一致
//            	warningMap.put("utctime", params.get("utctime"));
//            	warningMap.put("lat", params.get("lat"));
//            	warningMap.put("lon", params.get("lon"));
//            	warningMap.put("gps_locate_model", params.get("gps_locate_model"));
                redisExporter.export(RedisExporter.OP_LPUSH, keys.getBytes(), warningMap, null, null);
            } catch (Exception e) {
                log.error("Redis故障告警转换输出异常", e);
            }
		} catch (Exception e) {
			log.error("htwx故障码告警任务出错", e);
		}
		TaskFuture future = new TaskFuture(TaskFuture.TASK_CODE_SUCCESS);
        return future;
	}
	/**
	 * 判断时间间隔，是否大于指定时间
	 * @param start
	 * @param end
	 * @return
	 */
	public boolean isTimeout(Object start,Object end){
		try {
			if(start instanceof Date && end instanceof Date){
				Date s = (Date) start;
				Date e = (Date) end;
				long time = Math.abs(e.getTime() - s.getTime()) / 1000;
				if(0 <= time && time <= interval)
					return true;
			}
		} catch (Exception e) {
			log.error("故障码告警任务,判断gps时间是否超时错误", e);
		}
		return false;
	}
	
	

	public void setDataReader(DataReader dataReader) {
		this.dataReader = dataReader;
	}


	public void setJdbcExporter(JDBCExporter jdbcExporter) {
		this.jdbcExporter = jdbcExporter;
	}


	public void setRedisExporter(RedisExporter redisExporter) {
		this.redisExporter = redisExporter;
	}


	public void setKeys(String keys) {
		this.keys = keys;
	}


	public void setInterval(Long interval) {
		this.interval = interval;
	}

}
