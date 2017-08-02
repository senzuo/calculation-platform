package com.chh.dc.calc.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

/**
 * 数据库操作公共类/静态方法
 *
 */
public final class DBUtil {

	/**
	 * 向数据库批量添加新数据
	 * 
	 * @return 返回批量提交受影响的行数
	 * @throws Exception
	 */
	public static int[] executeBatch(Connection con, List<String> sqlList) throws Exception {
		int[] result = null;
		if(con == null){
			return result;
		}
		Statement stm = null;
		boolean autoCommit = con.getAutoCommit();
		con.setAutoCommit(false);

		try {
			if (sqlList != null && !sqlList.isEmpty()) {
				stm = con.createStatement();
				for (String sql : sqlList)
					stm.addBatch(sql);
				result = stm.executeBatch();
				if (con != null)
					con.commit();
			}
		} finally {
			close(null, stm, con);
			con.setAutoCommit(autoCommit);
		}
		return result;
	}

	/**
	 * 关闭所有连接
	 */
	public static void close(ResultSet rs, Statement stm, Connection conn) {
		// liangww modify 2012-04-17 增加关闭 rs的所属的statement
		if (rs != null) {
			Statement tmpStm = null;
			try {
				tmpStm = rs.getStatement();
				rs.close();
			} catch (Exception e) {
			} finally {
				try {
					if (tmpStm != null)
						tmpStm.close();
				} catch (Exception e2) {
				}
			}// try
		}

		if (stm != null) {
			try {
				stm.close();
			} catch (Exception e2) {
			}
		}

		if (conn != null) {
			try {
				conn.close();
			} catch (Exception e2) {
			}
		}
	}

	/**
	 * 更新数据库
	 * 
	 * @param conn
	 *            数据库连接
	 * @param sql
	 *            要执行的语句
	 * @return 受影响的条数
	 */
	public static int executeUpdate(Connection conn, String sql) throws Exception {
		int count = -1;
		PreparedStatement ps = null;
		if(conn == null){
			return count;
		}
		boolean autoCommit = conn.getAutoCommit();
		conn.setAutoCommit(false);

		try {
			ps = conn.prepareStatement(sql);
			count = ps.executeUpdate();
			if (conn != null)
				conn.commit();
		} catch (Exception e) {
			try {
				if (conn != null)
					conn.rollback();
			} catch (Exception ex) {
			}
			throw e;
		} finally {
			conn.setAutoCommit(autoCommit);
			close(null, ps, conn);
		}

		return count;
	}

	/**
	 * 更新数据库,不用关闭连接
	 * 
	 * @param conn
	 *            数据库连接
	 * @param sql
	 *            要执行的语句
	 * @return 受影响的条数
	 */
	public static int executeUpdateO(Connection conn, String sql) throws Exception {
		int count = -1;
		PreparedStatement ps = null;
		if(conn == null){
			return count;
		}
		boolean autoCommit = conn.getAutoCommit();
		conn.setAutoCommit(false);

		try {
			ps = conn.prepareStatement(sql);
			count = ps.executeUpdate();
			if (conn != null)
				conn.commit();
		} catch (Exception e) {
			try {
				if (conn != null)
					conn.rollback();
			} catch (Exception ex) {
			}
			throw e;
		} finally {
			conn.setAutoCommit(autoCommit);
			close(null, ps, null);
		}

		return count;
	}

	/**
	 * 获取数据库连接
	 * 
	 * @param driver
	 *            驱动类
	 * @param url
	 *            数据库路径
	 * @param username
	 *            用户名
	 * @param password
	 *            密码
	 * @return 数据库连接
	 */
	public static Connection getConnection(String driver, String url, String username, String password) throws Exception {
		Connection conn = null;
		Class.forName(driver);
		conn = DriverManager.getConnection(url, username, password);
		return conn;
	}

	/**
	 * 执行select数据
	 * 
	 * @param sql
	 * @return
	 * @throws Exception
	 */
	public static ResultSet queryForResultSet(Connection connection, String sql) throws Exception {
		ResultSet resultSet = null;
		PreparedStatement preparedStatement = null;

		preparedStatement = connection.prepareStatement(sql);
		resultSet = preparedStatement.executeQuery();

		return resultSet;
	}

	public static boolean exist(Connection connection, String sql) {
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			ps = connection.prepareStatement(sql);
			rs = ps.executeQuery();
			if (rs.next()) {
				return true;
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			DBUtil.close(rs, ps, connection);
		}

		return false;
	}

}