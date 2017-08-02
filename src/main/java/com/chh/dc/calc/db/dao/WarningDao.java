package com.chh.dc.calc.db.dao;

import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WarningDao extends BaseDao {
	private static final Logger log = LoggerFactory.getLogger(WarningDao.class);
	
	/**
	 * 根据主键更新t_device
	 * @param deviceId
	 * @param params
	 */
	public void updateDeviceById(String deviceId,Map<String, Object> params){
		try {
			String sql = createUpdateSql("t_device", deviceId, params);
			update(sql, params);
		} catch (Exception e) {
			log.error("更新device出错", e);
		}
	}
	private String createUpdateSql(String table,Object key,Map<String, Object> params){
		StringBuilder sb = new StringBuilder();
		sb.append("update t_device set ");
		for (Entry<String, Object> entry : params.entrySet()) {
			sb.append(entry.getKey()).append("=?,");
		}
		sb.deleteCharAt(sb.length() - 1);
		sb.append(" where id='").append(key).append("'");
		return sb.toString();
	}
}
