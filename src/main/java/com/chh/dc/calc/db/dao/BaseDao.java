package com.chh.dc.calc.db.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.chh.dc.calc.util.DBUtil;

public class BaseDao {
    
	/**
     * 日志
     */
    protected static final Logger log = LoggerFactory.getLogger(BaseDao.class);

    protected DataSource dataSource;
    
    /**
     * 更新
     * @param sql
     * @return
     */
    protected int update(String sql,Map<String, Object> data){
    	int rst = 0;
    	Connection con = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
    	try {
    		con = dataSource.getConnection();
            statement = con.prepareStatement(sql);
            int i = 1;
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                statement.setObject(i++, entry.getValue());
            }
            rst = statement.executeUpdate();
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}finally{
        	DBUtil.close(rs, statement, con);
        }
    	return rst;
    }
    /**
     * 
     * query(查询方法)
     * @param resultClass 返回类型 如: Map.class
     * @return
     * @throws SQLException 
     * @exception
     */
	@SuppressWarnings("unchecked")
	public <E> E query (String sql,Class<E> resultClass,Object ... obj){
		Connection con = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
             con = dataSource.getConnection();
             statement = con.prepareStatement(sql);
             if(obj != null && obj.length > 0){
                 for(int j = 0;j < obj.length;j++){
                	 statement.setObject((j+1), obj[j]) ;
                 }
             }
             rs = statement.executeQuery();
            if(resultClass == Map.class){
                if(rs.next()) return (E) getResultMap(rs);
            }else if(resultClass == List.class){
                return (E) getResultList(rs);
            }else if(resultClass == Integer.class){
                return (E) getResultInt(rs);
            }
        } catch (SQLException e) {
            log.error(e.getMessage(),e);
        }finally{
        	DBUtil.close(rs, statement, con);
        }
        return null; 
    }
     
    /**
     * 解析ResultSet 表列数据
     * @param rs
     * @return	Map
     * @throws SQLException
     */
    private Map<String,Object> getResultMap(ResultSet rs) throws SQLException{ 
        Map<String, Object> rawMap = new HashMap<String, Object>();
        ResultSetMetaData  rsmd = rs.getMetaData(); // 表对象信息
        int count = rsmd.getColumnCount();          // 列数
        // 遍历之前需要调用 next()方法
        for (int i = 1; i <= count; i++) {  
            String key = rsmd.getColumnLabel(i);
            Object value = rs.getObject(key);
            rawMap.put(key, value); 
        }
        return rawMap;
    }
    /**
     * 解析ResultSet 表数据
     * @param rs
     * @return	List
     * @throws SQLException
     */
    private List<Map<String,Object>> getResultList(ResultSet rs) throws SQLException{
        List<Map<String,Object>> rawList = new ArrayList<Map<String,Object>>();
        while(rs.next()){
            Map<String, Object> rawMap = getResultMap(rs);
            rawList.add(rawMap); 
        }
        return rawList;
    }
    /**
     * 解析ResultSet 
     * @param rs
     * @return int
     * @throws SQLException
     */
    private Integer getResultInt(ResultSet rs) throws SQLException{
        int rst = 0;
    	if(rs.next()){
            rst = rs.getInt(1);
        }
        return rst;
    }
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}
}
