package com.hapiware.asm.timemachine;


import java.lang.instrument.Instrumentation;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;


/**
 * {@code TimeMachineAgentDelegate} can be used to shift system time <i>without touching the system
 * clock</i>. The trick is to catch all the system time calls and manipulate their byte code
 * directly to achieve the desired result (and this is exactly what is done here :-). Time shift is
 * done only for some specific classes which can be defined in the configuration file.
 * <p>
 * 
 * The shift of system time can be relative or absolute. Relative system time shift means that
 * some offset is added to or subtracted from the original system time value. Absolute system
 * time shift means that original system time is always replaced by some absolute value. 
 * <p>
 * 
 * {@code TimeMachineAgentDelegate} is specified in the agent configuration XML file using
 * {@code /agent/delegate} element. For example:
 * <xmp>
 * 	<?xml version="1.0" encoding="UTF-8" ?>
 * 	<agent>
 * 		<delegate>com.hapiware.asm.timemachine.TimeMachineAgentDelegate</delegate>
 * 		...
 * 	</agent>
 * </xmp>
 * 
 * 
 * 
 * <h3>com.hapiware.agent.Agent required</h3>
 * 
 * {@code TimeMachineAgentDelegate} requires {@code com.hapiware.agent.Agent}. For more
 * information see {@code com.hapiware.agent.Agent}.
 * 
 * 
 * 
 * <h3>Configuration elements</h3>
 * 
 * The configuration elements are child elements of {@code /agent/configuration} element and are:
 * <ul>
 * 		<li>{@code <include>}</li>
 * 		<li>{@code <exclude>}</li>
 * 		<li>{@code <time>}</li>
 * </ul>
 * 
 * The configuration elements must always have at least one {@code <include>} rule element,
 * zero or more {@code <exclude>} elements and exactly one valid {@code <time>} element.
 * 
 *
 *
 * <h4>{@code <include>} element</h4>
 * 
 * There must be always at least one {@code <include>} element. If there are more than one they are
 * all used (i.e. ORed) for matching the possible candidates for instrumentation.
 * <p>
 * {@code <include>} element is a regular expression which is used to match all the classes to be
 * instrumented (i.e. those classes which somehow reads the system time).
 * <b>Notice</b> that the class names are presented in the internal form of fully qualified class
 * names as defined in The Java Virtual Machine Specification (e.g. "java/util/List"). So, when
 * you create {@code <include>} and {@code <exclude>} elements, remember that package names are
 * separated with slash (/) instead of period (.). See <a href="#examples">examples</a> in the end
 * of the document.
 *
 * 
 * 
 * <h4>{@code <exclude>} element</h4>
 * 
 * {@code <exclude>} element is optional but there can be several of them. {@code <exclude>} can be
 * used to ensure that the instrumentation is not done for some classes.
 * <p>
 * {@code <exclude>} element is a regular expression which is used to match all the classes
 * <b>not to be</b> instrumented.
 * <b>Notice</b> that the class names are presented in the internal form of fully qualified class
 * names as defined in The Java Virtual Machine Specification (e.g. "java/util/List"). So, when
 * you create {@code <include>} and {@code <exclude>} elements, remember that package names are
 * separated with slash (/) instead of period (.). See <a href="#examples">examples</a> in the end
 * of the document.
 * 
 * 
 * 
 * <h4>{@code <time>} element</h4>
 * 
 * The {@code <time>} element has the following format:<br>
 * <blockquote>
 * 		{@code <time>[+|-]y-M-d@H:m:s</time>}
 * </blockquote>
 * 
 * where:
 * 	<ul>
 * 		<li>
 *			{@code +} or {@code -} <b>Optional</b>. Indicates if the configured time is 
 *			relative or absolute. Relative time has a plus (+) or minus (-) sign preceding
 *			the time signature. If the sign is plus (+) then the time value is added to the
 *			returned system time. If the sign is minus (-) then time subtracted from system time.
 *			Absolute time <b>has no</b> preceding symbol in the front of the time signature.
 * 		</li>
 * 		<li>{@code y} is a year for absolute time or number of years for relative time.</li>
 * 		<li>
 *			{@code M} is a month for absolute time or number of months for relative time.
 *			<b>Notice!</b> The month for absolute time is given from 0 to 11 (from January to
 *			December respectively) and follows the general Java convention of managing months.
 *			Also, month can be anything between 0 and 99. Values bigger than eleven (11) increase
 *			years accordingly.
 *		</li>
 * 		<li>
 *			{@code d} is a days for absolute time or number of days for relative time. Notice
 *			that {@code d} can be anything between 0 and 999999. This can be useful when
 *			exact relative time shift is needed. For absolute time (dates) only correct days
 *			should be used to avoid strange results.
 *		</li>
 * 		<li>{@code H} is hours for absolute and relative time.</li>
 * 		<li>{@code m} is minutes for absolute and relative time.</li>
 * 		<li>{@code s} is seconds for absolute relative time.</li>
 * 	</ul>
 *
 * Exact regular expression match pattern for {@code <time>} element is:
 * <blockquote>
 *  	{@code [+-]?\d{1,4}-\d{1,2}-\d{1,6}@\d{1,2}:\d{1,2}:\d{1,2}}
 * </blockquote>
 *
 *
 * <h3><a name="examples">Example configurations</a></h3>
 * 
 * This example matches all the classes for the system time shift:
 * <xmp>
 * 	<?xml version="1.0" encoding="UTF-8" ?>
 * 	<agent>
 * 		<delegate>com.hapiware.asm.timemachine.TimeMachineAgentDelegate</delegate>
 * 		<classpath>
 * 			<entry>/users/me/agent/target/timemachine-delegate-1.0.0.jar</entry>
 * 			<entry>/usr/local/asm-3.1/lib/all/all-asm-3.1.jar</entry>
 * 		</classpath>
 * 		<configuration unmarshaller="com.hapiware.asm.timemachine.TimeMachineAgentDelegate">
 * 			<!--
 * 				Moves time two years and 5 months backward.
 * 				The time shift is done for every class.
 * 			-->
 * 			<include>.+</include>
 * 			<time>-2-5-0@0:0:0</time>
 * 		</configuration>
 * 	</agent>
 * </xmp>
 * 
 * And here is another example:
 * <xmp>
 * 	<?xml version="1.0" encoding="UTF-8" ?>
 * 	<agent>
 * 		<delegate>com.hapiware.asm.timemachine.TimeMachineAgentDelegate</delegate>
 * 		<classpath>
 * 			<entry>/users/me/agent/target/timemachine-delegate-1.0.0.jar</entry>
 * 			<entry>/usr/local/asm-3.1/lib/all/all-asm-3.1.jar</entry>
 * 		</classpath>
 * 		<configuration unmarshaller="com.hapiware.asm.timemachine.TimeMachineAgentDelegate">
 * 			<!--
 * 				Moves time ten days and eight hours forward.
 * 				The time shift is done for every loaded class
 * 				under com.hapiware.* package.
 * 			-->
 * 			<include>^com/hapiware/.+</include>
 * 			<time>+0-0-10@8:0:0</time>
 * 		</configuration>
 * 	</agent>
 * </xmp>
 * 
 * And yet another example:
 * <xmp>
 * 	<?xml version="1.0" encoding="UTF-8" ?>
 * 	<agent>
 * 		<delegate>com.hapiware.asm.timemachine.TimeMachineAgentDelegate</delegate>
 * 		<classpath>
 * 			<entry>/users/me/agent/target/timemachine-delegate-1.0.0.jar</entry>
 * 			<entry>/usr/local/asm-3.1/lib/all/all-asm-3.1.jar</entry>
 * 		</classpath>
 * 		<configuration unmarshaller="com.hapiware.asm.timemachine.TimeMachineAgentDelegate">
 * 			<!--
 * 				Set time to 13th of April 2010 7:15 AM. Remember that January is zero (0).
 * 			-->
 * 			<include>^com/hapiware/.*f[oi]x/.+</include>
 * 			<include>^com/mysoft/.+</include>
 * 			<exclude>^com/hapiware/.+/CreateCalculationForm</exclude>
 * 			<time>2010-3-13@7:15:0</time>
 * 		</configuration>
 * 	</agent>
 * </xmp>
 *
 *
 * 
 * <h3>Notice! Calculating the relative time value</h3>
 * For calculating the relative time value there are a few assumptions used:
 *  <ol>
 * 		<li>A year has 365 days.</li>
 * 		<li>A month has 30 days.</li>
 *  </ol>
 * 
 * This leads to a little bit inaccurate results, of course, but in practice this doesn't
 * matter. If the exact shift in time is absolutely needed then days should be calculated
 * manually and put the manually calculated result to {@code <time>} element without using 
 * years and months at all.
 * 
 * @see com.hapiware.agent.Agent
 * 
 * @author hapi
 */
public class TimeMachineAgentDelegate
{
	/**
	 * Pattern for matching the time configuration string.
	 * <p>
	 * The pattern in general form is: {@code y-M-d@H:m:s}
	 * <p>
	 * Notice that this pattern is a little bit different than what is given in the documentation
	 * (i.e. the grouping is missing). This is because grouping is not needed for examining 
	 * a match but necessary to get all the items from the given configuration string.
	 */
	private static final String TIME_CONFIGURATION_MATCH_PATTERN =
		"[+-]?(\\d{1,4})-(\\d{1,2})-(\\d{1,6})@(\\d{1,2}):(\\d{1,2}):(\\d{1,2})";
	

	/**
	 * This method is called by the general agent {@code com.hapiware.agent.Agent} and
	 * is done before the main method call right after the JVM initialisation. 
	 * <p>
	 * <b>Notice</b> the difference between this method and 
	 * the {@code public static void premain(String, Instrumentation} method described in
	 * {@code java.lang.instrument} package. 
	 * 
	 * @param config
	 * 		Configuration object ({@link Config}) to configure {@link TimeMachineTransformer}.
	 * 		This is the object returned by {@link #unmarshall(Element)}.
	 * 
	 * @param instrumentation
	 * 		See {@link java.lang.instrument.Instrumentation}
	 * 
	 * @throws IllegalArgumentException
	 * 		If there is something wrong with the configuration file.
	 *
	 * @see #unmarshall(Element)
	 * @see java.lang.instrument
	 */
	public static void premain(Object config, Instrumentation instrumentation)
	{
		try 
		{
			if(config != null)
				instrumentation.addTransformer(new TimeMachineTransformer((Config)config));
			else {
				String ex = "A configuration object is missing.";
				throw new IllegalArgumentException(ex);
			}
		} 
		catch(Exception e) 
		{
			System.err.println(
				"Couldn't start the TimeMachine agent delegate due to an exception. "
					+ e.getMessage()
			);
			e.printStackTrace();
		}
	}

	/**
	 * Parses the configuration node and creates the include and exclude regular expression
	 * pattern compilations for class matching as well as a proper time shift pattern compilation.
	 * 
	 * @param configElement
	 *		/agent/configuration node from the java agent configuration file.
	 * 
	 * @return
	 * 		Configuration elements ({@link Config}) parsed from the configuration node.
	 */
	public static Object unmarshall(Element configElement)
	{
		XPath xpath = XPathFactory.newInstance().newXPath();
		Config config = null;
		try {
			// /agent/configuration/include entries.
			NodeList includeEntries = 
				(NodeList)xpath.evaluate("./include", configElement, XPathConstants.NODESET);
			if(includeEntries.getLength() == 0) {
				String ex = "There are no include rules in TimeMachine configuration file.";
				throw new IllegalArgumentException(ex);
			}
			
			List<Pattern> includes = new ArrayList<Pattern>();
			for(int i = 0; i < includeEntries.getLength(); i++) {
				Node includeEntry = includeEntries.item(i).getFirstChild();
				if(includeEntry != null)
					includes.add(Pattern.compile(((Text)includeEntry).getData()));
			}
			
			// /agent/configuration/exclude entries.
			NodeList excludeEntries = 
				(NodeList)xpath.evaluate("./exclude", configElement, XPathConstants.NODESET);
			List<Pattern> excludes = new ArrayList<Pattern>();
			for(int i = 0; i < excludeEntries.getLength(); i++) {
				Node excludeEntry = excludeEntries.item(i).getFirstChild();
				if(excludeEntry != null)
					excludes.add(Pattern.compile(((Text)excludeEntry).getData()));
			}
			
			// /agent/configuration/time entry.
			NodeList timeEntries = 
				(NodeList)xpath.evaluate("./time", configElement, XPathConstants.NODESET);
			if(timeEntries.getLength() < 1) {
				String ex =
					"/agent/configuration/time element is missing from "
						+ "TimeMachine configuration file."; 
				throw new IllegalArgumentException(ex);
			}
			if(timeEntries.getLength() > 1) {
				String ex =
					"Wrong number of /agent/configuration/time elements in TimeMachine "
						+ "configuration file.";
				ex += "\n\t-> Only one (1) \"/agent/configuration/time\" element was expected"
					+ " but " + timeEntries.getLength() + " elements were found.";
				throw new IllegalArgumentException(ex);
			}
			
			config =
				new Config(
					excludes,
					includes,
					parseTime(((Text)timeEntries.item(0).getFirstChild()).getData())
				);
		}
		catch(XPathExpressionException e) {
			e.printStackTrace();
		}

		return config;
	}

	
	/**
	 * Parses the configuration string as documented in class description
	 *  
	 * @param timeString
	 * 		A string to be parsed.
	 * 
	 * @return
	 * 		Time ({@link Milliseconds}) parsed from string.
	 */
	static Milliseconds parseTime(String timeString)
	{
		timeString = timeString.trim();

		Pattern p = Pattern.compile(TIME_CONFIGURATION_MATCH_PATTERN);
		Matcher m = p.matcher(timeString);
		if(!m.matches()) {
			String ex = "TimeMachine time configuration string is incorrect.";
			ex += "\n\t-> This RegEx pattern \"" + TIME_CONFIGURATION_MATCH_PATTERN + "\" was expected"
				+ " but \"" + timeString + "\" was received.";
			throw new IllegalArgumentException(ex);
		}

		int year = Integer.parseInt(m.group(1));
		int month = Integer.parseInt(m.group(2));
		int date = Integer.parseInt(m.group(3));
		int hour = Integer.parseInt(m.group(4));
		int minute = Integer.parseInt(m.group(5));
		int second = Integer.parseInt(m.group(6));
		
		String firstChar = timeString.substring(0, 1);
		boolean isRelative = firstChar.equals("+") || firstChar.equals("-");
		long millis = 0;
		if(isRelative) {
			millis = ((long)year * 365 + (long)month * 30 + (long)date) * 24 * 60 * 60 * 1000;
			millis += ((long)hour * 60 * 60 + (long)minute * 60 + (long)second) * 1000;
			if(firstChar.equals("-"))
				millis = -millis;
		}
		else {
			Calendar c = Calendar.getInstance();
			c.clear();
			c.set(year, month, date, hour, minute, second);
			millis = c.getTimeInMillis();
		}
		
		return new Milliseconds(isRelative, millis);
	}

	/**
	 * {@code Config} is just used to manage configuration file data.
	 * <p>
	 * {@code Config} class is <b>immutable</b>.
	 * 
	 * @author hapi
	 *
	 */
	static class Config
	{
		private final Milliseconds milliseconds;
		private final List<Pattern> includePatterns;
		private final List<Pattern> excludePatterns;

		public Config(
			List<Pattern> excludePatterns,
			List<Pattern> includePatterns,
			Milliseconds milliseconds)
		{
			this.excludePatterns = Collections.unmodifiableList(excludePatterns);
			this.includePatterns = Collections.unmodifiableList(includePatterns);
			this.milliseconds = milliseconds;
		}

		public Milliseconds getMilliseconds()
		{
			return milliseconds;
		}

		public List<Pattern> getIncludePatterns()
		{
			return includePatterns;
		}

		public List<Pattern> getExcludePatterns()
		{
			return excludePatterns;
		}
	}
	
	
	/**
	 * {@code Milliseconds} is used to hold relative {@literal (i.e. offset)} or absolute time in
	 * milliseconds. If time is a relative value then it is supposed to be added to a returned 
	 * system time value. In the case of absolute value then the returned system value is to
	 * be replaced with time hold in {@code Milliseconds}. 
	 * <p>
	 * {@code Milliseconds} class is <b>immutable</b>.
	 * 
	 * @author hapi
	 *
	 */
	static public class Milliseconds
	{
		/**
		 * {@code true}, if {@link Milliseconds#time} property has relative time.</br>
		 * {@code false}, if {@link Milliseconds#time} property has absolute time.
		 * 
		 * @see Milliseconds#time
		 */
		private final boolean isRelative;
		
		/**
		 *  Time in milliseconds. This value is either relative or absolute value
		 *  depending on {@link Milliseconds#isRelative} member.
		 *  
		 *  @see Milliseconds#isRelative
		 */
		private final Long time;
		
		
		/**
		 * Constructs a {@code Milliseconds} object with either absolute or relative time value.
		 * 
		 * @param isRelative
		 * 		{@code true}, if {@code time} is relative time.
		 * 		{@code false}, if {@code time} is absolute time.
		 * 
		 * @param time
		 * 		Relative or absolute time in milliseconds.
		 */
		public Milliseconds(boolean isRelative, long time)
		{
			this.isRelative = isRelative;
			this.time = time;
		}

		/**
		 * Indicates if the time value is absolute or relative time.
		 *  
		 * @return
		 * 		{@code true}, if {@code time} is relative time.
		 * 		{@code false}, if {@code time} is absolute time.
		 * 
		 * @see Milliseconds#getTime()
		 */
		public boolean isRelative()
		{
			return isRelative;
		}

		/**
		 * Returns a time value. 
		 * 
		 * @return
		 * 		Time in milliseconds.
		 * 
		 * @see Milliseconds
		 * @see Milliseconds#isRelative()
		 */
		public Long getTime()
		{
			return time;
		}
		
		/**
		 * Returns a string representing {@code Milliseconds}. The returned string has one of
		 * the following formats depending on {@link Milliseconds#isRelative} property:
		 * 	<ol>
		 * 		<li>{@code {date[DATE], ms[XX]}}</li> 
		 * 		<li>{@code {y[YY], m[MM], d[DD], h[HH], ms[XX]}}</li>
		 * 	</ol>
		 * where:
		 * 	<ul>
		 *     <li>{@code XX} is time in milliseconds.</li>
		 *     <li>{@code DATE} is date as {@link Date#toString()}.</li>
		 *     <li>{@code YY} is offset in years.</li>
		 *     <li>{@code MM} is offset in months.</li>
		 *     <li>{@code DD} is offset in days.</li>
		 *     <li>{@code HH} is offset in hours.</li>
		 *	</ul>
		 * <p>
		 * The first form is for the absolute value and the second form for the relative value.
		 * <b>Notice</b> that hours, days, months and years all represent the same value but
		 * with different units just to make it a little bit easier for user to examine the value.
		 * 
		 * @see Milliseconds#isRelative()
		 */
		@Override
		public String toString()
		{
			String retVal = "{";
			DecimalFormatSymbols symbols = new DecimalFormatSymbols();
			symbols.setDecimalSeparator('.');
			DecimalFormat df = new DecimalFormat("0.00", symbols);
			if(isRelative) {
				retVal +=
					"y[" + df.format((double)time/(double)(365L * 24L * 60L * 60L * 1000L)) + "], ";
				retVal +=
					"m[" + df.format((double)time/(double)(30L * 24L * 60L * 60L * 1000L)) + "], ";
				retVal +=
					"d[" + df.format((double)time/(double)(24L * 60L * 60L * 1000L)) + "], ";
				retVal +=
					"h[" + df.format((double)time/(double)(60L * 60L * 1000L)) + "], ";
			}
			else {
				Date d = new Date(time);
				retVal += "date[" + d.toString() + "], ";
			}
			retVal += "ms[" + time + "]";
			retVal += "}";
			return retVal;
		}
	}
}
