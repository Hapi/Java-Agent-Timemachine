package com.hapiware.asm.timemachine;

import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.LADD;
import static org.objectweb.asm.Opcodes.POP2;

import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;

import com.hapiware.asm.timemachine.TimeMachineAgentDelegate.Milliseconds;


/**
 * TimeMachineAdapter is used to catch all the system time queries and manipulate the result
 * to effectively get a shift in time.
 *
 * @see TimeMachineAgentDelegate
 * 
 * @author <a href="http://www.hapiware.com" target="_blank">hapi</a>
 *
 */
public class TimeMachineAdapter extends MethodAdapter 
{
	private final Milliseconds milliseconds;
	

	public TimeMachineAdapter(Milliseconds milliseconds, MethodVisitor mv)
	{
		super(mv);
		this.milliseconds = milliseconds;
	}
	
	/**
	 * Catches all the system time queries and manipulates the result either by adding
	 * (or subtracting) time from it or replacing the system time value altogether with
	 * absolute time. 
	 */
	@Override
	public void visitMethodInsn(
		int opcode,
		String owner,
		String name,
		String desc
	) {
		mv.visitMethodInsn(opcode, owner, name, desc);
		switch(opcode) {
			case INVOKESPECIAL :
				if(
					owner.equals("java/util/Date") && 
					name.equals("<init>") &&
					desc.equals("()V")
				) {
					mv.visitInsn(DUP);
					if(milliseconds.isRelative()) {
						mv.visitInsn(DUP);
						mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/Date", "getTime", "()J");
						mv.visitLdcInsn(milliseconds.getTime());
						mv.visitInsn(LADD);
					}
					else
						mv.visitLdcInsn(milliseconds.getTime());
					mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/Date", "setTime", "(J)V");
				}
				break;
				
			case INVOKESTATIC :
				if(
					owner.equals("java/lang/System") && 
					name.equals("currentTimeMillis") &&
					desc.equals("()J")
				) {
					if(milliseconds.isRelative()) {
						mv.visitLdcInsn(milliseconds.getTime());
						mv.visitInsn(LADD);
					}
					else {
						mv.visitInsn(POP2);
						mv.visitLdcInsn(milliseconds.getTime());
					}
				}
				else
					if(
						owner.equals("java/util/Calendar") && 
						name.equals("getInstance") &&
						desc.equals("()Ljava/util/Calendar;")
					) {
						mv.visitInsn(DUP);
						if(milliseconds.isRelative()) {
							mv.visitInsn(DUP);
							mv.visitMethodInsn(
								INVOKEVIRTUAL, "java/util/Calendar", "getTimeInMillis", "()J"
							);
							mv.visitLdcInsn(milliseconds.getTime());
							mv.visitInsn(LADD);
						}
						else
							mv.visitLdcInsn(milliseconds.getTime());
						mv.visitMethodInsn(
							INVOKEVIRTUAL, "java/util/Calendar", "setTimeInMillis", "(J)V"
						);
					}
				break;
		}
	}

	@Override
	public void visitMaxs(int maxStack, int maxLocals)
	{
		mv.visitMaxs(maxStack + 6, maxLocals);
	}
}
