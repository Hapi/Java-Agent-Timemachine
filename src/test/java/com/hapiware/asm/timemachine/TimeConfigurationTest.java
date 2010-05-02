package com.hapiware.asm.timemachine;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.hapiware.asm.timemachine.TimeMachineAgentDelegate;


public class TimeConfigurationTest
{
	@Test
	public void correctTimes()
	{
		assertEquals(998787723000L, (long)TimeMachineAgentDelegate.parseTime("+30-20-10@1:2:3").getTime());
		assertEquals(864000000L, (long)TimeMachineAgentDelegate.parseTime("+0-0-10@0:0:0").getTime());
		assertEquals(-864000000L, (long)TimeMachineAgentDelegate.parseTime("-0-0-10@0:0:0").getTime());
		assertEquals(1228082400000L, (long)TimeMachineAgentDelegate.parseTime("2008-11-1@0:0:0").getTime());

		assertEquals(998787723000L, (long)TimeMachineAgentDelegate.parseTime("  +30-20-10@1:2:3").getTime());
		assertEquals(864000000L, (long)TimeMachineAgentDelegate.parseTime("+0-0-10@0:0:0 ").getTime());
		assertEquals(-864000000L, (long)TimeMachineAgentDelegate.parseTime(" -0-0-10@0:0:0 ").getTime());
		assertEquals(1228082400000L, (long)TimeMachineAgentDelegate.parseTime("   2008-11-1@0:0:0  ").getTime());
	}
	
	@Test(expected=IllegalArgumentException.class)
	public final void incorrectTimes1()
	{
		TimeMachineAgentDelegate.parseTime("");
	}
	
	@Test(expected=IllegalArgumentException.class)
	public final void incorrectTimes2()
	{
		TimeMachineAgentDelegate.parseTime("a");
	}
	
	@Test(expected=IllegalArgumentException.class)
	public final void incorrectTimes3()
	{
		TimeMachineAgentDelegate.parseTime("+ 0-0-10@0:0:0");
	}
	
	@Test(expected=IllegalArgumentException.class)
	public final void incorrectTimes4()
	{
		TimeMachineAgentDelegate.parseTime("+0-0- 10@0:0:0");
	}
}
