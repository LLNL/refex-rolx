/**
 * Version: 1.0
 * Author: Keith Henderson
 * Contact: keith@llnl.gov
 */

import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * Utility class for converting times between long and String.
 * @author hendersk
 *
 */
public class TimeUtils {

	/*
	 * Conversion utilities for timestamps. There are 3 involved: string based
	 * for humans, Java epoch style (milliseconds since 1/1/1970) and
	 * java.sql.Timsestamp
	 */
	public static long dateAsMS(String rawDate, String format) {
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		long startTime = 0;
		try {
			startTime = (sdf.parse(rawDate).getTime());
		} catch (ParseException e1) {
			return -1;
		}
		return startTime;
	}
	
	public static String dateAsString(long msDate) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		return sdf.format(new java.util.Date(msDate));
	}
	
	public static String dateAsString(long msDate, String format) {
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		return sdf.format(new java.util.Date(msDate));
	}
	
	public static void main(String[] args) {
		System.out.println(dateAsMS("2007-11-1302:19:53.550", "yyyy-MM-ddHH:mm:ss.SSS"));
		System.out.println(dateAsMS("2007-11-1302:19:53.717", "yyyy-MM-ddHH:mm:ss.SSS"));
	}
}