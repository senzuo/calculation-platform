package com.chh.dc.calc.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 * @ClassName: StringUtil
 * @author Niow
 * @date: 2014-1-21
 */
public class StringUtil {

	/**
	 * 判断两个字符串是否相等
	 * 
	 * @param string
	 * @param another
	 * @return 如果相等则返回true 否则false
	 */
	public static boolean equals(String string, String another) {
		return string == null ? another == null : string.equals(another);
	}

	/**
	 * 字符串是否为空
	 * @param str
	 * @return
	 */
	public static boolean isNull(String str) {
		return str == null || str.length() == 0;
	}

	/**
	 * 字符串是否为非空
	 * @param str
	 * @return
	 */
	public static boolean isNotNull(String str) {
		return !isNull(str);
	}


	/**
	 * 字符串截取方法<br>
	 * 从原始string中截图begin和end之间的部分
	 * 
	 * @param string
	 * @param begin
	 * @param end
	 * @return begin和end之间字符串
	 */
	public static String substring(String string, String begin, String end) {
		if (isNull(string))
			return "";
		int beginIndex = string.indexOf(begin);
		if (beginIndex == -1)
			return "";
		int endIndex = string.indexOf(end, beginIndex + begin.length());
		if (endIndex == -1)
			return string.substring(beginIndex + begin.length());
		return string.substring(beginIndex + begin.length(), endIndex);
	}

	/**
	 * 字符串截取方法<br>
	 * 带有偏移量<br>
	 * 从原始string中截图begin和end之间的部分
	 * 
	 * @param string
	 * @param begin
	 * @param offset
	 *            偏移量，从截取开始处为begin末尾+offset位
	 * @param end
	 * @return begin和end之间字符串
	 */
	public static String substring(String string, String begin, int offset, String end) {
		if (isNull(string))
			return "";
		int beginIndex = string.indexOf(begin);
		if (beginIndex == -1)
			return "";
		int endIndex = string.indexOf(end, beginIndex + begin.length() + offset);
		if (endIndex == -1)
			return string.substring(beginIndex + begin.length());
		return string.substring(beginIndex + begin.length() + offset, endIndex);
	}

	/**
	 * 字符串截取方法<br>
	 * 从原始string中截取begin到字符串末尾
	 * 
	 * @param string
	 * @param begin
	 * @return
	 */
	public static String substring(String string, String begin) {
		if (isNull(string))
			return "";
		int beginIndex = string.indexOf(begin);
		if (beginIndex == -1)
			return "";
		return string.substring(beginIndex + begin.length());
	}

	/**
	 * @param str
	 * @param beginStr
	 * @param endStr
	 * @return
	 */
	public static String getSubStrFromLast(String str, String beginStr, String endStr) {
		if (isNull(str)) {
			return "";
		}

		int begin = str.lastIndexOf(beginStr);
		if (begin == -1) {
			return "";
		}

		int end = str.lastIndexOf(endStr, begin - beginStr.length());
		if (end == -1) {
			return str.substring(0, begin);
		}

		return str.substring(end + endStr.length(), begin);
	}

	/** 
	 * 获取本地计算机名 
	 * 
	 * @return
	 */
	public static String getHostName() {
		String strHostName = null;
		try {
			strHostName = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			strHostName = (e != null && e.getMessage() != null ? e.getMessage() : e.getMessage().trim());
			try {
				strHostName = strHostName.split(":")[0].trim();
			} catch (Exception exx) {
			}
		}

		return strHostName;
	}

	/**
	 * 分割字符串
	 * 
	 * @param srcStr
	 *            源字符串
	 * @param splitStr
	 *            分割符
	 * @return
	 */
	public static String[] split(String srcStr, String splitStr) {
		if (StringUtil.isNull(srcStr)) {
			return new String[0];
		}

		List<String> list = new ArrayList<String>();
		int begin = 0;
		int index = 0;

		while ((index = srcStr.indexOf(splitStr, begin)) != -1) {
			list.add(srcStr.substring(begin, index));
			begin = index + splitStr.length();
		}

		list.add(srcStr.substring(begin));
		return list.toArray(new String[0]);
	}

	/**
	 * 转换采集路径，将“%%”占位符转换为实际值。
	 * 
	 * @param raw
	 *            原始路径。
	 * @param date
	 *            时间点。
	 * @return 转换后的值。
	 */
	public static String convertCollectPath(String raw, Date date) {
		if (raw == null || date == null)
			return raw;
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		String s = raw;
		s = s.replaceAll("%%Y", String.format("%04d", cal.get(Calendar.YEAR)));
		s = s.replaceAll("%%y", String.format("%04d", cal.get(Calendar.YEAR)));
		s = s.replaceAll("%%M", String.format("%02d", cal.get(Calendar.MONTH) + 1));
		s = s.replaceAll("%%D", String.format("%02d", cal.get(Calendar.DAY_OF_MONTH)));
		s = s.replaceAll("%%d", String.format("%02d", cal.get(Calendar.DAY_OF_MONTH)));
		s = s.replaceAll("%%H", String.format("%02d", cal.get(Calendar.HOUR_OF_DAY)));
		s = s.replaceAll("%%h", String.format("%02d", cal.get(Calendar.HOUR_OF_DAY)));
		s = s.replaceAll("%%m", String.format("%02d", cal.get(Calendar.MINUTE)));
		s = s.replaceAll("%%S", String.format("%02d", cal.get(Calendar.SECOND)));
		s = s.replaceAll("%%s", String.format("%02d", cal.get(Calendar.SECOND)));
		String em = new SimpleDateFormat("MMM", Locale.ENGLISH).format(date);
		s = s.replaceAll("%%EM", em);
		return s;
	}
}
