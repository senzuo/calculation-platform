package com.chh.dc.calc.task;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.chh.dc.calc.db.dao.DeviceTripDao;
import com.chh.dc.calc.exporter.JDBCExporter;
import com.chh.dc.calc.exporter.RedisExporter;
import com.chh.dc.calc.reader.DataPackage;
import com.chh.dc.calc.util.OBDAlarmCodeConverter;
import com.chh.dc.calc.util.SerializeUtil;
import com.chh.dc.calc.util.redis.RedisCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import DataReader;
//import RedisDataReader;


/**
 * Created by fulr on 16/11/28.
 */
public class HtwxTripTask extends Task {

    private static final Logger log = LoggerFactory.getLogger(HtwxTripTask.class);

    /**
     * 5分钟断链告警
     */
    public static final int WARNING_TYPE_CONNECTION_BROKEN = 17;

    /**
     * 熄火告警
     */
    public static final int WARNING_TYPE_STALL = 81;

    /**
     * 行程结束告警：新行程
     */
    public static final int WARNING_TYPE_TRIP_END = 200;
    /**
     * 行程更新告警：旧行程，之前汇总过的行程
     */
    public static final int WARNING_TYPE_TRIP_UPDATE = 201;

    /**
     * 行程未输出DB标志
     */
    public static final String TRIP_UN_OUTPUT_KEY = "unOutput";

    private RedisCache redisCache;

    private JDBCExporter jdbcExporter;

//    private JDBCBatchExporter jdbcBatchExporter;

    private RedisExporter redisExporter;

//    private byte[] keys = "obd_data_warning_queue".getBytes();

//    private String deviceType = "3";

    private DeviceTripDao deviceTripDao;
    
//    private BaseDao baseDao;
    
    private MedicalReport medicalReport;

    /*
     * 1.从event中获取到设备id和熄火告警时间
     * 2.读取告警数据,找到点火时间,并且统计急加\急减\急转弯次数
     * 3.根据开始和结束时间,读取stat数据,找到数据区间,然后
     *
     * */
    @Override
    public TaskFuture call() throws Exception {
        try{
            Map<String, Object> params = getEvent().getParams();
            String deviceUId = (String) params.get("device_uid");
            Integer warningType = (Integer) params.get("warning_type");

            //告警类型为5分钟断链告警(warning_type==17)，遍历待汇总行程缓存(htwx_cur_trip:{device_uid})
            if (warningType==WARNING_TYPE_CONNECTION_BROKEN) {
                log.debug("deviceUid:{}断开连接超过5分钟，开始汇总该盒子全部待汇总行程",deviceUId);
                DataPackage data = redisCache.getData(RedisCache.OPTION_ZRANGE_BY_SCORES,"htwx_cur_trip:"+deviceUId,Double.MIN_VALUE,Double.MAX_VALUE);
                if (data!=null&&data.getData()!=null) {
                    Set<byte[]> list = (Set<byte[]>) data.getData();
                    for (byte[] item : list) {
                        Long acconTimeSec = SerializeUtil.unserialize(item, Long.class);
                        processTask(deviceUId,acconTimeSec,null);
                    }
                }
            } else {
//                告警类型为熄火告警（warning_type==81），取出device_uid和last_accon_time_sec

                Long acconTimeSec = (Long) params.get("last_accon_time_sec");
                Date endTime = (Date) params.get("utctime");
                log.debug("deviceUid:{}收到熄火告警:{}，开始汇总单个行程",deviceUId,acconTimeSec);
                processTask(deviceUId,acconTimeSec,endTime);
            }
        } catch (Exception e) {
            log.error("htwx汇总任务出错", e);
        }




        TaskFuture future = new TaskFuture(TaskFuture.TASK_CODE_SUCCESS);
        return future;
    }

    private void processTask(String deviceUId, Long acconTimeSec,Date endTime) throws Exception {

        //1.从缓存移除 acconTimeSec
        //判断移除返回结果是否1：是否移除成功
        Long res = redisCache.zrem("htwx_cur_trip:"+deviceUId,acconTimeSec);
        if (res<=0) {
            //1.熄火没有GPS；2.多任务汇总，别的任务取走了
            log.warn("deviceUid:{}，行程点火时间:{}，不在待汇总缓存中，直接返回",deviceUId,acconTimeSec);
            return;
        }
        log.debug("deviceUid:{}，行程点火时间:{}，行程熄火时间:{}，从待汇总队列移出开始汇总",deviceUId,acconTimeSec,endTime);

        Map<String, Object> curTrip = null;
        Date startTime = new Date(acconTimeSec*1000);//点火时间
       
        int raCount = 0;           //急加 66
        int adCount = 0;           //急减 67
        int stCount = 0;           //急转 21
		// 2016 12 26 add 添加其他 告警统计
		int door_unclosed_cnt = 0; // 车门异常次数 60
		int idle_cnt = 0;          // 怠速时间过长次数 22
		int temp_barrier_cnt = 0;  // 水温异常次数 2
		int fault_cnt = 0;         // 故障告警次数 100
		int overspeed_cnt = 0;     // 超速告警次数 27
		int over_revs_cnt = 0;     // 转速过高次数 8
		int overdo_cnt = 0;        // 疲劳驾驶次数 11
		
//		int score = 0;             // 行程得分
//		int rank_cnt = 0;          // 行程排名
//		double rank_per = 0;       // 行程排名按百分比计算

        int gpsCount = 0;//行程缓存中GPS点数
        
        int warningType = WARNING_TYPE_TRIP_END;//告警类型，可以判断行程是否输出过
        boolean isCached = false;//当前行程是否缓存过

        //查询已汇总行程缓存，看是否存在score》=last_accon_time_sec的记录，如果存在==则汇总，如果存在>则从数据库查询，否则新行程
        DataPackage pack = redisCache.getData(RedisCache.OPTION_ZRANGE_BY_SCORES,"htwx_processed_trip:"+deviceUId,acconTimeSec,Double.MAX_VALUE);
        if (pack!=null) {
            Set<byte[]> tripDataSet = (Set<byte[]>) pack.getData();
            for (Iterator<byte[]> it=tripDataSet.iterator();it.hasNext();) {
                Map trip = SerializeUtil.unserialize(it.next(),Map.class);
                long accon_time_sec = ((Date)trip.get("start_time")).getTime()/1000;
                if (accon_time_sec==acconTimeSec) {
                    curTrip = trip;
                    isCached = true;
                    log.debug("deviceUid:{}，行程点火时间:{}，在已汇总缓存中",deviceUId,acconTimeSec);
                    break;
                } else if(accon_time_sec>acconTimeSec) {
                    //从DB加载 curTrip
                    curTrip = deviceTripDao.getTrip(deviceUId,startTime);
                    if (curTrip!=null) {
                        log.debug("deviceUid:{}，行程点火时间:{}，从DB中提取",deviceUId,acconTimeSec);

                        //历史告警数据redis缓存已被清除，需要从DB初始化
                        raCount = (int) curTrip.get("ra_count");
                        adCount = (int) curTrip.get("ad_count");
                        stCount = (int) curTrip.get("st_count");

                        door_unclosed_cnt = (int) curTrip.get("door_unclosed_cnt");
                        idle_cnt = (int) curTrip.get("idle_cnt");
                        temp_barrier_cnt = (int) curTrip.get("temp_barrier_cnt");
                        fault_cnt = (int) curTrip.get("fault_cnt");
                        overspeed_cnt = (int) curTrip.get("overspeed_cnt");
                        over_revs_cnt = (int) curTrip.get("over_revs_cnt");
                        overdo_cnt = (int) curTrip.get("overdo_cnt");
                    } else {
                        //

                    }
                    
//					//得分、行程排名、排名百分比
//					score = (int) curTrip.get("score");
//					rank_cnt = (int) curTrip.get("rank_cnt");
//					rank_per = (double) curTrip.get("rank_per");
//                    isCacheCurTrip = false;
                    break;
                }
            }
        }

        if (curTrip!=null) {

            if (curTrip.containsKey(TRIP_UN_OUTPUT_KEY)) {// 在缓存中未输出到DB和业务系统
                curTrip.remove(TRIP_UN_OUTPUT_KEY);
                warningType = WARNING_TYPE_TRIP_END;
            } else {
                warningType = WARNING_TYPE_TRIP_UPDATE;
            }

        } else {
            //初始化新行程
            curTrip = new HashMap<String, Object>();
            curTrip.put("id",UUID.randomUUID().toString());
            curTrip.put("device_uid", deviceUId);
            curTrip.put("create_time", new Date());
            curTrip.put("start_time", startTime);
            curTrip.put("trip_mileage", 0);
            curTrip.put("fuel_consumption", 0);
//            curTrip.put("start_longitude", 0);
//            curTrip.put("start_latitude", 0);
//            curTrip.put("end_longitude", 0);
//            curTrip.put("end_latitude", 0);
            //地球坐标1
            curTrip.put("gps_locate_model", 1);
            curTrip.put("ra_count", 0);
            curTrip.put("st_count", 0);
            curTrip.put("ad_count", 0);
    		
            curTrip.put("door_unclosed_cnt", 0);
            curTrip.put("idle_cnt", 0);
            curTrip.put("temp_barrier_cnt", 0);
            curTrip.put("fault_cnt", 0);
            curTrip.put("overspeed_cnt", 0);
            curTrip.put("over_revs_cnt", 0);
            curTrip.put("overdo_cnt", 0);
    		
//            curTrip.put("end_time", new Date());
            log.debug("deviceUid:{}，行程点火时间:{}，初始化新行程ID：{}",deviceUId,acconTimeSec,curTrip.get("id"));
        }

        if (curTrip!=null) {
            //4.从GPS里获取油耗、里程、起点和终点坐标
            Map endGps = null;
            Map statGps = null;
            //通过device_uid、last_accon_time_sec读取行程gps数据缓存（htwx_gps:{device_uid}|{last_accon_time_sec}）、行程告警数据缓存(htwx_alarm:{device_uid}|{last_accon_time_sec})
            DataPackage gpsPack = redisCache.getData(RedisCache.OPTION_ZREVRANGE_BY_SCORES,"htwx_gps:"+deviceUId+"|"+acconTimeSec,Double.MIN_VALUE,Double.MAX_VALUE);
            if (gpsPack!=null&&gpsPack.getData()!=null) {
                Set<byte[]> gpsDataSet = (Set<byte[]>) gpsPack.getData();
                gpsCount = gpsDataSet.size();
                for (Iterator<byte[]> iterator=gpsDataSet.iterator();iterator.hasNext();) {
                    Map tempGps = SerializeUtil.unserialize(iterator.next(),Map.class);
                    if (tempGps.get("current_trip_milea")!=null) {
                        statGps = tempGps;
                        break;
                    }
                }
                if (statGps!=null) {
                    long milea = (Long) statGps.get("current_trip_milea");
                    double currentFuel = (double) statGps.get("current_fuel");
                    curTrip.put("trip_mileage", milea / 1000.0);//里程，单位：M转KM
                    curTrip.put("fuel_consumption", currentFuel);//油耗
                }


                Object[] gpsData = (gpsDataSet).toArray();
                endGps = SerializeUtil.unserialize((byte[])gpsData[0],Map.class);
                //新行程才更新行程起点
                if (curTrip.get("start_longitude")==null) {
                    Map startGps = SerializeUtil.unserialize((byte[])gpsData[gpsData.length-1],Map.class);
                    curTrip.put("start_longitude", startGps.get("lon"));
                    curTrip.put("start_latitude", startGps.get("lat"));
                }
                curTrip.put("end_longitude", endGps.get("lon"));
                curTrip.put("end_latitude", endGps.get("lat"));
                if (endTime==null) {
                    endTime = (Date) endGps.get("utctime");
                }
            }

            //5. end_time
            Date curEndTime = (Date) curTrip.get("end_time");
            //endTime为空||已经汇总过，结束时间不为空&&结束时间大于endTime
            if (endTime==null||(curEndTime!=null&&curEndTime.getTime()>endTime.getTime())) {
                endTime = curEndTime;
            }
            if (endTime==null) {
                endTime = new Date();
            }
            curTrip.put("end_time", endTime);



            //6.统计
            //raCount、adCount、stCount
            DataPackage alarmPack = redisCache.getData(RedisCache.OPTION_LRANGE,"htwx_alarm:"+deviceUId+"|"+acconTimeSec,0,-1);
            if (alarmPack!=null) {
                List<byte[]> alarmSet = (List<byte[]>) alarmPack.getData();
                Iterator<byte[]> iterator = alarmSet.iterator();
                while (iterator.hasNext()) {
                    byte[] bytes = iterator.next();
                    Map<String, Object> alarmData = SerializeUtil.unserialize(bytes, Map.class);
                    Integer wt = (Integer) alarmData.get("warning_type");
                    switch (wt) {
                        case 66:
                            raCount++;
                            break;
                        case 67:
                            adCount++;
                            break;
                        case 21:
                            stCount++;
                            break;
                        case 60:
                        	door_unclosed_cnt++;
                            break;
                        case 22:
                        	idle_cnt++;
                            break;
                        case 2:
                        	temp_barrier_cnt++;
                            break;
                        case 100:
                        	fault_cnt++;
                            break;
                        case 27:
                        	overspeed_cnt++;
                            break;
                        case 8:
                        	over_revs_cnt++;
                            break;
                        case 11:
                        	overdo_cnt++;
                            break;
                    }

                }
                curTrip.put("ra_count", raCount);
                curTrip.put("st_count", stCount);
                curTrip.put("ad_count", adCount);
                
                curTrip.put("door_unclosed_cnt", door_unclosed_cnt);
                curTrip.put("idle_cnt", idle_cnt);
                curTrip.put("temp_barrier_cnt", temp_barrier_cnt);
                curTrip.put("fault_cnt", fault_cnt);
                curTrip.put("overspeed_cnt", overspeed_cnt);
                curTrip.put("over_revs_cnt", over_revs_cnt);
                curTrip.put("overdo_cnt", overdo_cnt);

            }
            curTrip.put("update_time",new Date());

            if (isActualTrip(curTrip,warningType,gpsCount)) {
            	//真实的行程，才进行得分计算
            	dealTripScore(deviceUId, curTrip, overdo_cnt, raCount, adCount, stCount);
                if (warningType == WARNING_TYPE_TRIP_END) {
                    //7.输出行程到DB
                    log.debug("deviceUid:{}，新行程ID：{}，输出数据库并生成体检报告",deviceUId,curTrip.get("id"));
                    jdbcExporter.export("t_device_trip", curTrip);

                    medicalReport.generateReport(deviceUId,acconTimeSec);
                } else {
                    log.debug("deviceUid:{}，旧行程ID：{}，更新数据库",deviceUId,curTrip.get("id"));
                    deviceTripDao.updateTrip(curTrip);
                }



                //8.告警输出到业务系统
                Map<String, Object> warningMap = new HashMap<>();
                warningMap.put("id",UUID.randomUUID().toString());
                warningMap.put("device_uid", deviceUId);
                warningMap.put("warning_type", warningType);
                warningMap.put("warning_time", new Date());
                warningMap.put("warning_value", curTrip.get("id"));
                warningMap.put("create_time", new Date());
                warningMap.put("warning_desc", OBDAlarmCodeConverter.getHtwxAlarmDesc(warningType));
                try {
                    jdbcExporter.export("t_device_warning", warningMap);
                } catch (Exception e) {
                    log.error("JDBC告警转换输出异常，行程ID：{},告警ID：{}", deviceUId,curTrip.get("id"),warningMap.get("id"),e);
                }
                try {
                    redisExporter.export(RedisExporter.OP_LPUSH, "obd_data_warning_queue".getBytes(), warningMap, null, null);
                } catch (Exception e) {
                    log.error("Redis告警转换输出异常，行程ID：{},告警ID：{}", deviceUId,curTrip.get("id"),warningMap.get("id"), e);
                }
            } else {
                curTrip.put(TRIP_UN_OUTPUT_KEY,true);
                log.debug("deviceUid:{}，行程ID：{}，汇总完毕，行程过短不予输出！",deviceUId,curTrip.get("id"));
            }

            if (isCached) {
                //先清除htwx_processed_trip中旧记录
                redisCache.zremrangeByScore("htwx_processed_trip:"+deviceUId,acconTimeSec,acconTimeSec);
            }
            redisCache.zadd("htwx_processed_trip:"+deviceUId,acconTimeSec,curTrip);

            log.debug("deviceUid:{}，行程ID：{}，汇总完毕",deviceUId,curTrip.get("id"));
        } else {
            log.debug("deviceUid:{}，行程点火时间:{}，初始化行程数据失败！",deviceUId,acconTimeSec);
        }

    }

    /**
     * 判断是否真实的行程
     * @param curTrip
     * @return
     */
    private boolean isActualTrip(Map<String, Object> curTrip,int warningType,int gpsCount) {
        //行程更新，旧行程
        if (warningType==WARNING_TYPE_TRIP_UPDATE) {
            return true;
        }
        //行程过短不输出（行程只有一个点、行程总时长小于1分钟、总里程小于等于100米）
        if (gpsCount<=1) {
            return false;
        }
        Date startTime = (Date) curTrip.get("start_time");
        Date endTime = (Date) curTrip.get("end_time");
//        行程总时长小于1分钟
        if (endTime.getTime()-startTime.getTime()<1*60*1000){
            return false;
        }
        Double tripMileage = (Double) curTrip.get("trip_mileage");
//        总里程小于等于100米
        if (tripMileage<0.1){
            return false;
        }

        return true;
    }


    private void dealTripScore(String deviceUId,Map<String, Object> curTrip,int overdo_cnt,int raCount,int adCount,int stCount){
        //疲劳驾驶得分(出现一次疲劳驾驶就全部扣减) 19
        int overdoCntScore = overdo_cnt == 0 ? 19 : 0;
        //总里程
        Double tripMileage = (Double) curTrip.get("trip_mileage");
        Double raScore = getScore(raCount, tripMileage);//急加 得分
        Double adScore = getScore(adCount, tripMileage);//急减 得分
        Double stScore = getScore(stCount, tripMileage);//急转 得分
        //发动机高转速
        Double zsScore = 9d;
        //总分
        Double totalScore = raScore + adScore + stScore + overdoCntScore + zsScore;
        redisCache.zadd("htwx_trip_ranking", totalScore, deviceUId);
        //获取当前设备在所有设备中的排名
        Long rank = redisCache.zrevrank("htwx_trip_ranking", deviceUId);
        int rank_cnt = rank == null ? 0 : rank.intValue() + 1;
        //获取所有排行总数
        Long rankTotal = redisCache.zcount("htwx_trip_ranking", 0d, 100d);
        //行程排名按百分比计算
        double rank_per = rankTotal == 0 ? 0 : new BigDecimal(rank_cnt).divide(new BigDecimal(rankTotal),10,BigDecimal.ROUND_HALF_DOWN).doubleValue();
        curTrip.put("score", totalScore);
        curTrip.put("rank_cnt", rank_cnt);
        curTrip.put("rank_per", rank_per);
    }

    /**
     * 根据异常次数和里程计算得分
     * @param count
     * @param tripMileage
     * @return
     */
    private static Double getScore(int count,Double tripMileage){
    	Double rstScore = 0d;
    	Double totalScore = 24d;
    	try {
    		//异常次数为零、总里程为零 返回总分数
			if(count == 0 || tripMileage == null || tripMileage == 0)
				return totalScore;
			
			double rate = count / tripMileage;
        	if(rate == 0){
        		rstScore = totalScore;
        	}else if(rate > 0 && rate < 0.1){
        		rstScore = (1 - (rate / 0.1)) * 24;
        	}
		} catch (Exception e) {
			log.error("计算得分错误", e);
		}
    	return rstScore;
    }
    
    public RedisCache getRedisCache() {
        return redisCache;
    }

    public void setRedisCache(RedisCache redisCache) {
        this.redisCache = redisCache;
    }

    public JDBCExporter getJdbcExporter() {
        return jdbcExporter;
    }

    public void setJdbcExporter(JDBCExporter jdbcExporter) {
        this.jdbcExporter = jdbcExporter;
    }

//    public JDBCBatchExporter getJdbcBatchExporter() {
//        return jdbcBatchExporter;
//    }
//
//    public void setJdbcBatchExporter(JDBCBatchExporter jdbcBatchExporter) {
//        this.jdbcBatchExporter = jdbcBatchExporter;
//    }

    public RedisExporter getRedisExporter() {
        return redisExporter;
    }

    public void setRedisExporter(RedisExporter redisExporter) {
        this.redisExporter = redisExporter;
    }

//    public String getDeviceType() {
//        return deviceType;
//    }
//
//    public void setDeviceType(String deviceType) {
//        this.deviceType = deviceType;
//    }

	public void setMedicalReport(MedicalReport medicalReport) {
		this.medicalReport = medicalReport;
	}

	public MedicalReport getMedicalReport() {
		return medicalReport;
	}

    public DeviceTripDao getDeviceTripDao() {
        return deviceTripDao;
    }

    public void setDeviceTripDao(DeviceTripDao deviceTripDao) {
        this.deviceTripDao = deviceTripDao;
    }
}
