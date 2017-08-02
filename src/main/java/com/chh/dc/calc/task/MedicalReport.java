package com.chh.dc.calc.task;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import com.chh.dc.calc.db.dao.DictDao;
import com.chh.dc.calc.db.pojo.TDtc;
import com.chh.dc.calc.exporter.JDBCExporter;
import com.chh.dc.calc.reader.DataReader;
import com.chh.dc.calc.reader.RedisDataReader;
import com.chh.dc.calc.util.EhcacheUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;
import com.chh.dc.calc.reader.DataPackage;
import com.chh.dc.calc.util.SerializeUtil;
import com.chh.dc.calc.util.StringUtil;

/**
 * 体检报告
 * @author wangbin 
 * @date 2016年10月27日 上午10:29:20    
 * @Description: TODO
 */
public class MedicalReport {
	private static final Logger log = LoggerFactory.getLogger(MedicalReport.class);
	private DataReader dataReader;
    private JDBCExporter jdbcExporter;
    private DictDao dictDao;
    private static Map<Integer, String> sysTypeMap = new HashMap<>();
	private Map<String, Map<String, String>> fieldMap;
	
	static{
		//需统计的系统类型
		sysTypeMap.put(1, "Fuel-air");
		sysTypeMap.put(2, "Ignition");
		sysTypeMap.put(3, "Emission-control");
		sysTypeMap.put(4, "Speed-control");
		sysTypeMap.put(5, "Computer-control");
		sysTypeMap.put(6, "Network");
		sysTypeMap.put(7, "Fault-Detection");
	}
	public void init(){
		//初始化获取所有故障码放入缓存
		getDtcListToCache();
	}
    /**
     * 生成报告
     * @param uid
     * @param lastAcconTimeSec
     * @param utctime
     */
	@SuppressWarnings("unchecked")
	public void generateReport(String uid, Long lastAcconTimeSec){
    	try {
    		//保存故障码数据
    		Map<Integer, List<Map<String, Object>>> faultDataMap = new HashMap<>();
    		//数据流数据
    		List<Map<String, Object>> dataFlows = new ArrayList<>();	
    		//总分
            int score = 100;
            //数据流异常次数
            int dataFaultCount = 0;
            //故障码总数
            int faultCount = 0;
            String id = UUID.randomUUID().toString();
            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("id", id);
            dataMap.put("deviceUid", uid);
            //获取故障码
            String key = String.format("htwx_fault_code:%s|%s", uid , lastAcconTimeSec);
            DataPackage faultPack = dataReader.getData(RedisDataReader.OPTION_LRANGE, key, 0 , Integer.MAX_VALUE);
            List<byte[]> dataList = (List<byte[]>) faultPack.getData();
            Iterator<byte[]> iterator = dataList.iterator();
            while(iterator.hasNext()){
            	 byte[] bytes = iterator.next();
                 Map<String, Object> faultData = SerializeUtil.unserialize(bytes, Map.class);
                 //故障标志0:Store 1:Pending
//                 int faultFlag = (int) faultData.get("fault_flag");
                 String faultCode = (String) faultData.get("fault_code");
                 if(StringUtil.isNotNull(faultCode)){
            		Map<String, Object> map = new HashMap<>();
         			map.put("dtcValue", faultCode);
         			//根据故障码获取故码信息
         			TDtc dtc = getDescByCode(faultCode);
         			if(dtc == null)
         				continue;
         			map.put("dtcDescription", dtc.getDescription());
         			/*类型：1：燃油和空气侦查系统，2：点火系统，3：废气控制系统，4：车速怠速控制系统，5：电脑控制系统，
         			6：网络连接系统，7：故障检测，8：车身系统，9：网络系统，10：混合动力驱动系统，
         			11：底盘系统，12：制造商自定义，13：ISO/SAE预留*/
         			int type = dtc.getType();
         			List<Map<String, Object>> curList = faultDataMap.get(type);
         			if(curList == null)
         				curList = new ArrayList<>();
         			curList.add(map);
         			faultDataMap.put(type, curList);
                 }
            }
            //故障码
            List<Map<String, Object>> faultCodes = new ArrayList<>();
            for (Entry<Integer, List<Map<String, Object>>>  entry : faultDataMap.entrySet()) {
            	Map<String, Object> m = new HashMap<>();
            	String sysName = sysTypeMap.get(entry.getKey());
            	if(StringUtil.isNotNull(sysName)){
            		m.put("sys", sysName);
            		m.put("faults", entry.getValue());
                	faultCodes.add(m);
                	faultCount += entry.getValue().size();
            	}
			}
            dataMap.put("faultCodes", faultCodes);
            //取最近一次快照数据
            key = String.format("htwx_snap:%s|%s", uid , lastAcconTimeSec);
            DataPackage snapPack = dataReader.getData(RedisDataReader.OPTION_RPOP,key);
            //遍历每一个字段数据，进行比对，判断(true:正常/false:异常)
            if(snapPack != null && snapPack.getData() != null){
            	byte[] bytes = (byte[]) snapPack.getData();
            	Map<String, Object> canData = SerializeUtil.unserialize(bytes, Map.class);
            	for (Entry<String, Object> entry : canData.entrySet()) {
                	//字段属性
                	Map<String, String> field = fieldMap.get(entry.getKey());
                	Object value = entry.getValue();
                	if(field != null){
                		Map<String, Object> map = new HashMap<>();
                		map.put("name", field.get("desc"));
                		map.put("pid", entry.getKey().replaceAll("f_", "0x"));
                		if(value == null){
                			map.put("value", 0);
                			map.put("abnormalFlag", true);
                			dataFlows.add(map);
                			continue;
                		}
                		boolean flag = checkValue(value, field);
                		//数据流异常减一分
                		if(flag){
                			score--;
                			dataFaultCount++;
                		}
                		map.put("value", value);
                		map.put("abnormalFlag", flag);
                		dataFlows.add(map);
                	}
    			}
            }
            dataMap.put("dataFlow", dataFlows);
            //体检评分规则：1个故障码扣8份，故障码至多扣32分
            score = faultCount >= 4 ? (score - 32) : (score - 8 * faultCount);
            //检查结论
            StringBuilder sb = new StringBuilder();
            if(score == 100){
            	sb.append("您的爱车未检测到故障码，车况良好，请继续保持");
            }else{
            	score = score < 0 ? 0 : score;
            	sb.append("您的爱车检测到");
            	if(faultCount > 0){
            		sb.append(faultCount).append("个故障码");
            	}
            	if(dataFaultCount > 0){
            		if(faultCount > 0)
            			sb.append("/");
            		sb.append(dataFaultCount).append("项数据异常");
            	}
            	sb.append("，建议至4S店或其他专业检测机构进行详细排查和养护");
            }
            Date now = new Date();
            dataMap.put("score", score);
            dataMap.put("conclusion", sb.toString());
            dataMap.put("examinationTime", now);
            //保存
            Map<String, Object> data = new HashMap<>();
            data.put("id", id);
            data.put("device_uid", uid);
            data.put("create_time", now);
            data.put("examination_time", now);
            data.put("report_content", JSONObject.toJSONStringWithDateFormat(dataMap, "yyyy-MM-dd HH:mm:ss"));
            jdbcExporter.export("t_device_medical_report", data);
            log.info("体检报告汇总完毕 {} {}", uid, lastAcconTimeSec);
		} catch (Exception e) {
			log.error("生成体检报告出错",e); 
		}
    }
//	public static void main(String[] args) throws Exception {
//		Map<String, String> field = new HashMap<>();
//		field.put("min", "1");
//		field.put("max", "100");
//		System.out.println(checkValue(11110L, field));
//	}
	/**
	 * 验证值（正常/异常)
	 * @param value
	 * @param field
	 * @return boolean
	 * @throws Exception
	 */
	private static boolean checkValue(Object value, Map<String, String> field) throws Exception{
		boolean flag = false;//true:异常	false:正常   默认正常
		Double min = Double.parseDouble(field.get("min"));
    	Double max = Double.parseDouble(field.get("max"));
		if(value instanceof Integer){
			if(min > (int)value || (int)value > max){
				flag = true;
			}
		}else if(value instanceof Long){
			if(min > (long)value || (long)value > max){
				flag = true;
			}
		}else if(value instanceof Double){
			if(min > (double)value || (double)value > max){
				flag = true;
			}
		}else if(value instanceof String){
			//暂时不确定
		}
		return flag;
	}
    /**
     * 获取所有故障码放入缓存
     */
	private void getDtcListToCache(){
    	try {
    		List<TDtc> dtcList = dictDao.getDtcList();
    		if(dtcList != null && dtcList.size() > 0){
    			Map<String, TDtc> datas = new HashMap<>();
    			for (TDtc d : dtcList) {
    				datas.put(d.getValue(), d);
				}
    			EhcacheUtils.put("dtcDatas", datas);
				log.info("初始化故障码放入缓存，共{}条记录",dtcList.size());
    		}
		} catch (Exception e) {
			log.error("初始化故障码放入缓存错误", e);
		}
    }
    /**
     * 根据故障码获取故障内容
     * @param code
     * @return
     */
	@SuppressWarnings("unchecked")
	public static TDtc getDescByCode(String code){
    	try {
    		Map<String, TDtc> datas = (Map<String, TDtc>) EhcacheUtils.get("dtcDatas");
    		if(datas != null)
    			return datas.get(code);
		} catch (Exception e) {
			log.error("根据故障码获取备注错误", e);
		}
    	return null;
    }
    
	public void setFieldMap(Map<String, Map<String, String>> fieldMap) {
		this.fieldMap = fieldMap;
	}
	public Map<String, Map<String, String>> getFieldMap() {
		return fieldMap;
	}
	public void setDataReader(DataReader dataReader) {
		this.dataReader = dataReader;
	}
	public void setJdbcExporter(JDBCExporter jdbcExporter) {
		this.jdbcExporter = jdbcExporter;
	}
	public DictDao getDictDao() {
		return dictDao;
	}
	public void setDictDao(DictDao dictDao) {
		this.dictDao = dictDao;
	}
}
