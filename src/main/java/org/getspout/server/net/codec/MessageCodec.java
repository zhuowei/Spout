package org.getspout.server.net.codec;

import java.io.IOException;

import org.jboss.netty.buffer.ChannelBuffer;

import org.getspout.server.msg.Message;

public abstract class MessageCodec<T extends Message> {
	private final Class<T> clazz;
	private final int opcode;

	public MessageCodec(Class<T> clazz, int opcode) {
		this.clazz = clazz;
		this.opcode = opcode;
	}

	public final Class<T> getType() {
		return clazz;
	}

	public final int getOpcode() {
		return opcode;
	}

	public abstract ChannelBuffer encode(T message) throws IOException;

	public abstract T decode(ChannelBuffer buffer) throws IOException;
}