package com.chh.dc.calc.exporter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.chh.dc.calc.util.DBUtil;
import com.chh.dc.calc.util.EhcacheUtils;


/**
 * 使用JDBC批量提交的数据库输出器
 *
 * @version 1.0
 * @since 3.0
 */
public class JDBCExporter {

    /**
     * 日志
     */
    protected static final Logger log = LoggerFactory.getLogger(JDBCExporter.class);


    /**
     * 数据库链接 在DBExporter初始化的时候创建 并且在异常或者数据分发完成后关闭
     */
    protected Connection con;

    protected PreparedStatement statement;

    protected DataSource dataSource;

    /**
     * 执行SQL语句
     */
    protected Map<String, String> sqlMap;

    private ReentrantLock lock = new ReentrantLock();



    /**
     * 构造函数
     */
    public JDBCExporter() {
        System.out.println("创建"+this.getClass().getSimpleName()+ System.currentTimeMillis());
        sqlMap = new ConcurrentHashMap<>(5);
        Listener listener = new Listener();
    	listener.start();
    }


    public void export(String table, Map<String, Object> data) throws Exception {
        String sql = getSql(table);
        Connection con = null;
        PreparedStatement statement = null;
        try {
            con = dataSource.getConnection();
            System.out.println("sql:"+sql);
            if (sql == null) {
                sql = createInsertSql(table, data);
            }
            statement = con.prepareStatement(sql);
            int i = 1;
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                statement.setObject(i++, entry.getValue());
            }
            statement.execute();
        } catch (Exception e) {
//            if (con!=null) {
//                try {
//                    con.rollback();
//                } catch (Exception ee) {
//
//                }
//            }
            if (e.getMessage()!=null&&e.getMessage().indexOf("Duplicate")!=-1) {
                // 数据已经存在数据库，重复入库直接返回
                return;
            }
            log.error("jdbc输出{" + table + "}出现错误,加入异步输出队列。", e);
            setErrorDatas(table, data);
        } finally {
            DBUtil.close(null, statement, con);
        }
    }


    /**
     * 初始化输出SQL
     *
     * @return
     */
    protected  String createInsertSql(String table, Map<String, Object> data) {
        StringBuilder sb = new StringBuilder();
        sb.append("insert into ").append(table).append(" (");

        StringBuilder sbValue = new StringBuilder();

        for (String column : data.keySet()) {
            sb.append(column);
            sb.append(",");
            sbValue.append("?");
            sbValue.append(",");
        }
        sb.deleteCharAt(sb.length() - 1);
        sbValue.deleteCharAt(sbValue.length() - 1);
        sb.append(") values (").append(sbValue.toString()).append(")");
        String sql = sb.toString();
        if (log.isDebugEnabled()) {
            log.debug("[{}]生成输出SQL:{}", table, sql);
        }
        sqlMap.put(table, sql);
        return sql;
    }

    protected String getSql(String table) {
        lock.lock();
        String sql = sqlMap.get(table);
        lock.unlock();
        return sql;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    /**
     * 将失败数据保存至缓存
     * @param table
     * @param data
     */
	private void setErrorDatas(String table, Map<String, Object> data){
    	try {
    		lock.lock();
    		Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("table", table);
            errorMap.put("data", data);
            EhcacheUtils.put("jdbcError" + System.currentTimeMillis(), errorMap);
		} catch (Exception e) {
			log.error("将失败数据保存至缓存失败！", e);
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
							try {
								if(key.startsWith("jdbcError")){
									Map<String, Object> map = (Map<String, Object>) EhcacheUtils.get(key);
									String table = map.get("table").toString();
									Map<String, Object> data = (Map<String, Object>) map.get("data");
									try {
										export(table, data);
									}finally{
										EhcacheUtils.remove(key);
									}
								}
							} catch (Exception e) {
								e.printStackTrace();
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
