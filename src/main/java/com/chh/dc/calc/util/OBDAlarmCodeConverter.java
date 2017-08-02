package com.chh.dc.calc.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.chh.dc.calc.db.dao.DictDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by niow on 16/10/12.
 */
public class OBDAlarmCodeConverter {
	private static final Logger log = LoggerFactory.getLogger(OBDAlarmCodeConverter.class);
    private static Map<String, Integer> htwxMap = new HashMap<>();
    private static Map<Integer, String> htwxAlarmMap = new HashMap<>();
    private DictDao dictDao;
    public static final int HTWX_ALARM_FAULT_CODE = 100;
    
    public void init(){
    	getWarningToCache();
    }
    static {
        htwxMap.put("0x01",27);
        htwxMap.put("0x02",1);
        htwxMap.put("0x03",2);
        htwxMap.put("0x04",66);
        htwxMap.put("0x05",67);
        htwxMap.put("0x06",22);
        htwxMap.put("0x07",7);
        htwxMap.put("0x08",8);
        htwxMap.put("0x09",69);
        htwxMap.put("0x0a",9);
        htwxMap.put("0x0b",10);
        htwxMap.put("0x0c",21);
        htwxMap.put("0x0d",11);
        htwxMap.put("0x0e",70);
        htwxMap.put("0x0f",12);
        htwxMap.put("0x10",13);
        htwxMap.put("0x11",14);
        htwxMap.put("0x12",15);
        htwxMap.put("0x13",16);
        htwxMap.put("0x14",17);
        htwxMap.put("0x15",18);
        htwxMap.put("0x16",80);
        htwxMap.put("0x17",81);
        htwxMap.put("0x18",100);
        htwxMap.put("lost",200);
        htwxMap.put("missing",65);
    }



    public static int htwxAlarm(String alarmType) {
        Integer i = htwxMap.get(alarmType);
        if (i == null) {
            return 0;
        }
        return i;
    }
    /**
     * 根据告警类型获取告警描述
     * @param type
     * @return
     */
    public static String getHtwxAlarmDesc(int type){
    	if(type <= 0)
    		return "";
    	return htwxAlarmMap.get(type);
    }
    /**
     * 获取告警字典表数据放入缓存
     */
	private void getWarningToCache(){
    	try {
    		List<Map<String, Object>> list = dictDao.getDictWarningList();
    		if(list != null && list.size() > 0){
    			for (Map<String, Object> map : list) {
    				htwxAlarmMap.put((int)map.get("value"), (String) map.get("description"));
				}
				log.info("初始化告警字典表数据，共{}条记录",list.size());
    		}
		} catch (Exception e) {
			log.error("获取告警字典表数据放入缓存异常", e);
		}
    }
	public DictDao getDictDao() {
		return dictDao;
	}
	public void setDictDao(DictDao dictDao) {
		this.dictDao = dictDao;
	}
    
}
