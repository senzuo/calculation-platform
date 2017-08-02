package com.chh.dc.calc.exporter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import com.chh.dc.calc.util.EhcacheUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.chh.dc.calc.util.DBUtil;


/**
 * 使用JDBC批量提交的数据库输出器
 *
 * @version 1.0
 * @since 3.0
 */
public class JDBCBatchExporter extends JDBCExporter {

    /**
     * 日志
     */
    protected static final Logger log = LoggerFactory.getLogger(JDBCBatchExporter.class);
    /**
     * 执行SQL语句
     */
    protected Map<String, String> sqlMap ;

    private int batchNum = 100;
    private ReentrantLock lock = new ReentrantLock();

    /**
     * 构造函数
     */
    public JDBCBatchExporter() {
        sqlMap = new ConcurrentHashMap<>(5);
    	Listener listener = new Listener();
    	listener.start();
    }


    public void export(String table, List<Map<String, Object>> datas) throws Exception {
        if (datas == null || datas.isEmpty()) {
            return;
        }
        String sql = sqlMap.get(table);
        Connection con = null;
        PreparedStatement statement = null;
        try {
            con = dataSource.getConnection();
            if (sql == null) {
                Map<String, Object> data = datas.get(0);
                sql = createInsertSql(table, data);
            }
            statement = con.prepareStatement(sql);
            int i = 1;
            int j = 0;
            for (Map<String, Object> data : datas) {
                for (Map.Entry<String, Object> entry : data.entrySet()) {
                    statement.setObject(i++, entry.getValue());
                }
                i = 1;
                statement.addBatch();
                j++;
                if (j >= batchNum) {
                    statement.executeBatch();
                    j = 1;
                }
            }
            statement.executeBatch();
            log.debug("输出数据:{}共{}条", table, datas.size());
        } catch (Exception e) {
            log.error("批量输出到数据库出错" + table, e);
            setErrorDatas(table, datas);
        } finally {
            DBUtil.close(null, statement, con);
        }
    }

    public int getBatchNum() {
        return batchNum;
    }

    public void setBatchNum(int batchNum) {
        this.batchNum = batchNum;
    }
    /**
     * 将失败数据保存至缓存
     * @param table
     * @param datas
     */
	private void setErrorDatas(String table, List<Map<String, Object>> datas){
    	try {
    		lock.lock();
             Map<String, Object> errorMap = new HashMap<>();
             errorMap.put("table", table);
             errorMap.put("datas", datas);
             EhcacheUtils.put("jdbcBathError" + System.currentTimeMillis(), errorMap);
             log.error("批量输出到数据库出错" + table + ",将失败数据保存至缓存。");
		} catch (Exception e) {
			e.printStackTrace();
		}finally {
			lock.unlock();
		}
    }
    /**
     * 数据处理失败监听器<br>
     */
    class Listener extends Thread{

		@SuppressWarnings("unchecked")
		@Override
		public void run() {
			// TODO Auto-generated method stub
			try {
				while(true){
					Thread.sleep(3000);
					//重缓存中获取提交失败数据
					List<String> keys = EhcacheUtils.getKeys();
					if(keys != null && keys.size() > 0){
						for (String key : keys) {
							if(key.startsWith("jdbcBathError")){
								try {
									Map<String, Object> map = (Map<String, Object>) EhcacheUtils.get(key);
									String table = map.get("table").toString();
									List<Map<String, Object>> datas = (List<Map<String, Object>>) map.get("datas");
									try {
										export(table, datas);
									} catch (Exception e) {}
									EhcacheUtils.remove(key);
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						}
					}
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
    }
}
