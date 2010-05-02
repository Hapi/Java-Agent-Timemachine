package com.hapiware.asm.timemachine;


import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.regex.Pattern;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

import com.hapiware.asm.timemachine.TimeMachineAgentDelegate.Config;


/**
 * Initialises {@link TimeMachineAdapter} for class manipulation.
 * 
 * @see TimeMachineAdapter
 * @see TimeMachineTransformer#transform(ClassLoader, String, Class, ProtectionDomain, byte[])
 *
 * @author hapi
 *
 */
public class TimeMachineTransformer
	implements
		ClassFileTransformer
{
	private final Config config;

	public TimeMachineTransformer()
	{
		config = null;
	}
	
	public TimeMachineTransformer(Config config)
	{
		this.config = config;
	}
	
	public byte[] transform(
		ClassLoader loader,
		final String className,
		Class<?> classBeingRedefined,
		ProtectionDomain protectionDomain,
		byte[] classFileBuffer
	)
		throws IllegalClassFormatException
	{
		for(Pattern p : config.getExcludePatterns())
			if(p.matcher(className).matches())
				return null;
		
		for(Pattern p : config.getIncludePatterns()) {
			if(p.matcher(className).matches()) 
			{
				try
				{
					ClassReader cr = new ClassReader(classFileBuffer);
					ClassWriter cw = new ClassWriter(0);
					//ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
					//ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
					cr.accept(
						new ClassAdapter(cw)
						{
							public MethodVisitor visitMethod(
								int access,
								String name,
								String desc,
								String signature,
								String[] exceptions
							)
							{
								MethodVisitor mv =
									super.visitMethod(access, name, desc, signature, exceptions);	
								return new TimeMachineAdapter(config.getMilliseconds(), mv);
							} 
						},
						0
					);
	
					return cw.toByteArray();
				}
				catch(Throwable e)
				{
					throw new Error("Instrumentation of a class " + className + " failed.", e);
				}
			}
		}
		return null;
	}
}