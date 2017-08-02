package com.chh.dc.calc.db.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.chh.dc.calc.db.pojo.TDtc;
import com.chh.dc.calc.util.DBUtil;

public class DictDao extends BaseDao{
	
	/**
	 * 获取所有故障码字典
	 * @return
	 * @throws Exception
	 */
	public List<TDtc> getDtcList() throws Exception {
        List<TDtc> dtcList = new ArrayList<TDtc>();
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            connection = dataSource.getConnection();
            String sql = "SELECT id,`value`,type,description FROM t_dictionary_dtc";
        	ps = connection.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()) {
            	TDtc d = new TDtc();
            	d.setId(rs.getInt("id"));
            	d.setValue(rs.getString("value"));
            	d.setType(rs.getInt("type"));
            	d.setDescription(rs.getString("description"));
            	dtcList.add(d);
            }
        } finally {
            DBUtil.close(rs, ps, connection);
        }
        return dtcList;
    }
	
	/**
	 * 获取所有告警字典
	 * @return
	 * @throws Exception
	 */
	public List<Map<String, Object>> getDictWarningList() throws Exception {
        List<Map<String, Object>> wList = new ArrayList<Map<String, Object>>();
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            connection = dataSource.getConnection();
            String sql = "SELECT `value`,description FROM t_dictionary_warning";
        	ps = connection.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()) {
            	Map<String, Object> m = new HashMap<String, Object>();
            	m.put("value", rs.getObject("value"));
            	m.put("description", rs.getObject("description"));
            	wList.add(m);
            }
        } finally {
            DBUtil.close(rs, ps, connection);
        }
        return wList;
    }
}
