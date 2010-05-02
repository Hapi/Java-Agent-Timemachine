package com.hapiware.asm.timemachine;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.hapiware.asm.timemachine.TimeMachineAgentDelegate;
import com.hapiware.asm.timemachine.TimeMachineAgentDelegate.Config;


public class PatternMatchTest
{
	private Document configDoc;
	private Element configuration;
	
	
	@Before
	public void setup() throws ParserConfigurationException
	{
		DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		configDoc = builder.newDocument();

		configuration = configDoc.createElement("configuration");
		configuration.setAttribute("unmarshaller", this.getClass().getName());
		configDoc.appendChild(configuration);
		
		// /agent/configuration/item
		Element item = configDoc.createElement("include");
		item.appendChild(configDoc.createTextNode("^com/hapiware/.+"));
		configuration.appendChild(item);

		item = configDoc.createElement("include");
		item.appendChild(configDoc.createTextNode("^com/mysoft/.+"));
		configuration.appendChild(item);

		item = configDoc.createElement("exclude");
		item.appendChild(configDoc.createTextNode("^com/bea/.+"));
		configuration.appendChild(item);

		item = configDoc.createElement("time");
		item.appendChild(configDoc.createTextNode("+0-0-10@1:2:3"));
		configuration.appendChild(item);
	}
	
	@Test
	public void normalConfiguration() throws IOException
	{
		Config config = (Config)TimeMachineAgentDelegate.unmarshall(configuration);
		assertEquals(2, config.getIncludePatterns().size());
		assertEquals("^com/hapiware/.+", config.getIncludePatterns().get(0).toString());
		assertEquals("^com/mysoft/.+", config.getIncludePatterns().get(1).toString());
		
		assertEquals(1, config.getExcludePatterns().size());
		assertEquals("^com/bea/.+", config.getExcludePatterns().get(0).toString());

		assertEquals(867723000L, config.getMilliseconds().getTime().longValue());
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void noIncludePatterns() throws IOException
	{
		final String tagName = "include";
		int len = configuration.getElementsByTagName(tagName).getLength();
		for(int i = 0; i < len; i++) {
			Element element = (Element)configuration.getElementsByTagName(tagName).item(0);
			element.getParentNode().removeChild(element);
		}

		TimeMachineAgentDelegate.unmarshall(configuration);
	}

	@Test
	public void noExcludePatterns() throws IOException
	{
		final String tagName = "exclude";
		int len = configuration.getElementsByTagName(tagName).getLength();
		for(int i = 0; i < len; i++) {
			Element element = (Element)configuration.getElementsByTagName(tagName).item(0);
			element.getParentNode().removeChild(element);
		}

		TimeMachineAgentDelegate.unmarshall(configuration);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void noTimePatterns() throws IOException
	{
		final String tagName = "time";
		Element element = (Element)configuration.getElementsByTagName(tagName).item(0);
		element.getParentNode().removeChild(element);

		TimeMachineAgentDelegate.unmarshall(configuration);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void moreThanOneTimePatterns() throws IOException
	{
		Element item = configDoc.createElement("time");
		item.appendChild(configDoc.createTextNode("2008-6-23@0:0:0"));
		configuration.appendChild(item);

		TimeMachineAgentDelegate.unmarshall(configuration);
	}
}
