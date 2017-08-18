package com.chh.dc.calc.task;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.chh.dc.calc.db.dao.WarningDao;
import com.chh.dc.calc.exporter.JDBCExporter;
import com.chh.dc.calc.exporter.RedisExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 取消失联告警任务
 * @author wangbin 
 * @date 2016年11月26日 下午2:01:30    
 * @Description: TODO
 */
public class HtwxCancelLostTask extends Task{
	private static final Logger log = LoggerFactory.getLogger(HtwxLostTask.class);
	
    private JDBCExporter jdbcExporter;
    private RedisExporter redisExporter;
    private WarningDao warningDao;
    private String keys;
    
    /**
     * 1.修改盒子状态；	
     * 2.取消失联告警入库；		
     * 3.取消失联告警推写入告警推送队列缓存
     * 4.取消失联告警无gps信息
     */
	@Override
	public TaskFuture call() throws Exception {
		try {
			 Map<String, Object> params = getEvent().getParams();
             String deviceUid = (String) params.get("device_uid");
             Date collectionTime = (Date) params.get("collection_time");
             //1.更新盒子状态为在线状态
             Map<String, Object> deviceMap = new HashMap<>();
             deviceMap.put("status", 1);
             warningDao.updateDeviceById(deviceUid, deviceMap);
             //2.取消失联告警入库
             Map<String, Object> lostMap = new HashMap<>();
             lostMap.put("id", UUID.randomUUID().toString());
             lostMap.put("device_uid", deviceUid);
             lostMap.put("warning_type", params.get("warning_type"));
             lostMap.put("warning_desc", params.get("warning_desc"));
             lostMap.put("warning_time", collectionTime);
             lostMap.put("warning_value", params.get("warning_value"));
             lostMap.put("create_time", new Date());
             try {
            	 jdbcExporter.export("t_device_warning", lostMap);
			 } catch (Exception e) {
				log.error("JDBC失联告警转换输出异常", e);
			 }
             //3.取消失联告警 写入告警推送队列缓存
             try {
                 redisExporter.export(RedisExporter.OP_LPUSH, keys.getBytes(), lostMap, null, null);
             } catch (Exception e) {
                 log.error("Redis告警转换输出异常", e);
             }
		} catch (Exception e) {
			log.error("htwx失联告警任务出错", e);
		}
		TaskFuture future = new TaskFuture(TaskFuture.TASK_CODE_SUCCESS);
        return future;
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

	public void setWarningDao(WarningDao warningDao) {
		this.warningDao = warningDao;
	}
}
