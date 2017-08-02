package com.chh.dc.calc.db.dao;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;

/**
 * Created by Administrator on 2016/12/2.
 */
public class DeviceTripDao extends BaseDao {

    public static final String GET_SQL = "SELECT dt.* FROM t_device_trip dt WHERE dt.device_uid=? and dt.start_time=? LIMIT 1";

    public static final String UPDATE_SQL = "update t_device_trip set id";

    public Map<String,Object> getTrip(String deviceUId, Date startTime) {
        //  初始化设置raCount、adCount、stCount
//        String sql = "SELECT dt.* FROM t_device_trip dt WHERE dt.device_uid=? and dt.start_time=? LIMIT 1";
        Map<String, Object> data = query(GET_SQL, Map.class,deviceUId,new Timestamp(startTime.getTime()));
        return data;
    }

    public void updateTrip(Map<String, Object> curTrip) {

        this.update(createUpdateSql("t_device_trip", curTrip.get("id"),curTrip),curTrip);
    }

    private String createUpdateSql(String table,Object id,Map<String, Object> params){
        StringBuilder sb = new StringBuilder();
        sb.append("update ").append(table).append(" set ");
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            sb.append(entry.getKey()).append("=?,");
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append(" where id='").append(id.toString()).append("'");
        return sb.toString();
    }
}
