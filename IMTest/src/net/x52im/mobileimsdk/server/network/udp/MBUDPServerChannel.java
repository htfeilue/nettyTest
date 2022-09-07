/*
 * Copyright (C) 2021  即时通讯网(52im.net) & Jack Jiang.
 * The MobileIMSDK v6.x Project. 
 * All rights reserved.
 * 
 * > Github地址：https://github.com/JackJiang2011/MobileIMSDK
 * > 文档地址：  http://www.52im.net/forum-89-1.html
 * > 技术社区：  http://www.52im.net/
 * > 技术交流群：320837163 (http://www.52im.net/topic-qqgroup.html)
 * > 作者公众号：“【即时通讯技术圈】”，欢迎关注！
 * > 联系作者：  http://www.52im.net/thread-2792-1-1.html
 *  
 * "即时通讯网(52im.net) - 即时通讯开发者社区!" 推荐开源工程。
 * 
 * MBUDPServerChannel.java at 2021-6-29 10:24:09, code by Jack Jiang.
 */
package net.x52im.mobileimsdk.server.network.udp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.StandardProtocolFamily;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.spi.SelectorProvider;
import java.util.LinkedHashMap;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.RecvByteBufAllocator;
import io.netty.channel.nio.AbstractNioMessageChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.ServerSocketChannelConfig;
import io.netty.util.internal.PlatformDependent;

/**
 * 仿照TCP协议的NioServerSocketChannel实现的专用于UDP的服务端Channel实现类。
 * <p>
 * 因Netty4.1.x中对UDP的实现相对于MINA这样的框架来说，回归到了UDP协议的本色状态
 * （即极简设计），从设计上、性能上来说回归到了协议的本质（这也是为什么Netty4中有
 * 关UDP的官方Demo代码几乎没有的原因，因为没太多的东西可演示的）。但对于应用层开发
 * （比如IM、消息推送等场景）来说，失去了很多的便利性。因为为了客户端的身份识别、鉴
 * 权、安全性等等，肯定是要做到“伪连接”的存在（MINA2中称为Session），这样就可以识别
 * 客户端的身份、请求的合法性，并通过超时机制来达到客户端无故退出（比如崩溃等）时
 * 自动判定“连接”的断开，从而即时更新此客户端的在线状态等。而Netty4中的这种“伪连接“、
 * ”伪连接超时检测机制“等，通通无法直接实现，要想达到这一点，那都得自已来。
 * <p>
 * NioServerSocketChannel的官方源码请见：
 * <a href="http://docs.52im.net/extend/docs/src/netty4_1/io/netty/channel/socket/nio/NioServerSocketChannel.html">NioServerSocketChannel</a>
 * 
 * @author Jack Jiang(http://www.52im.net/thread-2792-1-1.html)
 * @version 1.0
 * @since 3.1
 */
public class MBUDPServerChannel extends AbstractNioMessageChannel implements ServerSocketChannel 
{
	/** 
	 * Channel属性描述对象。
	 * <p>
	 * 参数用true的原因是：官方文档原文“if and only if the channel has the disconnect() 
	 * operation that allows a user to disconnect and then call ChannelOutboundInvoker
	 * .connect(SocketAddress) again, such as UDP/IP.”，即UDP协议时hasDisconnect设为true即可。
	 */
	private final ChannelMetadata METADATA = new ChannelMetadata(true);
	
	private final MBUDPServerChannelConfig config;
	
	protected final LinkedHashMap<InetSocketAddress, MBUDPChannel> channels = new LinkedHashMap<InetSocketAddress, MBUDPChannel>();

	public MBUDPServerChannel() throws IOException
	{
		this(SelectorProvider.provider().openDatagramChannel(StandardProtocolFamily.INET));
	}

	protected MBUDPServerChannel(DatagramChannel datagramChannel)
	{
		// 对于UDP来说，服务端链路注册成功之后，Channel一直处于OP_READ就可以了
		// ，因为UDP本身就是无连接的，我们只是借用TCP的概念来封装UDP而已，并非
		// 真正的需要继续监听客户端的网络连接操作
		super(null, datagramChannel, SelectionKey.OP_READ);
		this.config = new MBUDPServerChannelConfig(this, datagramChannel);
	}
	
	@Override
	public InetSocketAddress localAddress()
	{
		return (InetSocketAddress) super.localAddress();
	}
	
	@Override
	protected SocketAddress localAddress0() 
	{
		return this.javaChannel().socket().getLocalSocketAddress();
	}
	
	@Override
	public InetSocketAddress remoteAddress()
	{
		return null;
	}

	@Override
	protected SocketAddress remoteAddress0()
	{
		return null;
	}
	
	@Override
	public ChannelMetadata metadata()
	{
		return METADATA;
	}
	
	@Override
	public ServerSocketChannelConfig config()
	{
		return config;
	}
	
	/**
	 * 判断监听是否已启动。
	 * 
	 * @return true表示已已启动监听，否则未启动
	 * @see DatagramChannel#isOpen()
	 * @see DatagramSocket#isBound()
	 */
	@Override
	public boolean isActive() 
	{
		return this.javaChannel().isOpen() && this.javaChannel().socket().isBound();
	}
	
	@Override
	protected DatagramChannel javaChannel() 
	{
		return (DatagramChannel) super.javaChannel();
	}

	@Override
	protected void doBind(SocketAddress localAddress) throws Exception 
	{
		javaChannel().socket().bind(localAddress);
	}

	@Override
	protected void doClose() throws Exception
	{
		// “关闭”所有客户端的伪连接Channel
		for (MBUDPChannel channel : channels.values())
			channel.close();
		
		javaChannel().close();
	}

	/**
	 * 将一个客户端的Channel实例从服务端管理的列表中移除。
	 * 
	 * @param channel 要移除的Channel引用
	 */
	public void removeChannel(final Channel channel) 
	{
		eventLoop().submit(new Runnable() {
			@Override
			public void run() {
				InetSocketAddress remote = (InetSocketAddress) channel.remoteAddress();
				if (channels.get(remote) == channel) 
				{
					channels.remove(remote);
				}
			}
		});
	}

	@Override
	protected int doReadMessages(List<Object> list) throws Exception
	{
		DatagramChannel javaChannel = javaChannel();
		RecvByteBufAllocator.Handle allocatorHandle = unsafe().recvBufAllocHandle();
		ByteBuf buffer = allocatorHandle.allocate(config.getAllocator());
		allocatorHandle.attemptedBytesRead(buffer.writableBytes());
		
		boolean freeBuffer = true;
		try 
		{
			// read message
			ByteBuffer nioBuffer = buffer.internalNioBuffer(buffer.writerIndex(), buffer.writableBytes());
			int nioPos = nioBuffer.position();
			
			InetSocketAddress inetSocketAddress = (InetSocketAddress) javaChannel.receive(nioBuffer);
			if (inetSocketAddress == null) 
				return 0;
			
			allocatorHandle.lastBytesRead(nioBuffer.position() - nioPos);
			buffer.writerIndex(buffer.writerIndex() + allocatorHandle.lastBytesRead());
			
			// allocate new channel or use existing one and push message to it
			MBUDPChannel udpchannel = channels.get(inetSocketAddress);
			if ((udpchannel == null) || !udpchannel.isOpen()) 
			{
				udpchannel = new MBUDPChannel(this, inetSocketAddress);
				channels.put(inetSocketAddress, udpchannel);
				list.add(udpchannel);
				
				udpchannel.addBuffer(buffer);
				freeBuffer = false;
				
				return 1;
			} 
			else
			{
				udpchannel.addBuffer(buffer);
				freeBuffer = false;
				
				if (udpchannel.isRegistered()) 
					udpchannel.read();
				
				return 0;
			}
		} 
		catch (Throwable t) 
		{
			PlatformDependent.throwException(t);
			return -1;
		} 
		finally
		{
			if (freeBuffer)
				buffer.release();
		}
	}

	@Override
	protected boolean doWriteMessage(Object msg, ChannelOutboundBuffer buffer) throws Exception
	{
		DatagramPacket dpacket = (DatagramPacket) msg;
		InetSocketAddress recipient = dpacket.recipient();
		ByteBuf byteBuf = dpacket.content();
		int readableBytes = byteBuf.readableBytes();
		if (readableBytes == 0) 
			return true;
		
		ByteBuffer internalNioBuffer = byteBuf.internalNioBuffer(
				byteBuf.readerIndex(), readableBytes);
		
		return javaChannel().send(internalNioBuffer, recipient) > 0;
	}

	@Override
	protected boolean doConnect(SocketAddress addr1, SocketAddress addr2) throws Exception 
	{
		throw new UnsupportedOperationException();
	}

	@Override
	protected void doFinishConnect() throws Exception
	{
		throw new UnsupportedOperationException();
	}

	@Override
	protected void doDisconnect() throws Exception 
	{
		throw new UnsupportedOperationException();
	}
}
