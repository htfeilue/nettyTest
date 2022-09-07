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
 * MBUDPChannel.java at 2021-6-29 10:24:09, code by Jack Jiang.
 */
package net.x52im.mobileimsdk.server.network.udp;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.ConcurrentLinkedQueue;

import io.netty.buffer.ByteBuf;
import io.netty.channel.AbstractChannel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultChannelConfig;
import io.netty.channel.DefaultEventLoop;
import io.netty.channel.EventLoop;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.RecyclableArrayList;

/**
 * 仿照TCP协议的AbstractNioChannel实现的专用于UDP的客端Channel实现类。
 * <p>
 * 因Netty4.1.x中对UDP的实现相对于MINA这样的框架来说，回归到了UDP协议的本色状态
 * （即极简设计），从设计上、性能上来说回归到了协议的本质（这也是为什么Netty4中有
 * 关UDP的官方Demo代码几乎没有的原因，因为没太多的东西可演示的）。但对于应用层开发
 * （比如IM、消息推送等场景）来说，失去了很多的便利性。因为为了客户端的身份识别、鉴
 * 权、安全性等等，肯定是要做到“伪连接”的存在（MINA2中称为Session），这样就可以识别
 * 客户端的身份、请求的合法性，并通过超时机制来达到客户端无故退出（比如崩溃等）时
 * 自动判定“连接”的断开，从而即时更新此客户端的在线状态等。而Netty4中的这种“伪连接“、
 * ”伪连接超时检测机制“等，通通无法直接实现，要想达到这一点，那都得自已来。
 * 
 * @author Jack Jiang(http://www.52im.net/thread-2792-1-1.html)
 * @version 1.0
 * @since 3.1
 */
public class MBUDPChannel extends AbstractChannel 
{
	protected final ChannelMetadata metadata = new ChannelMetadata(false);
	protected final DefaultChannelConfig config = new DefaultChannelConfig(this);
	
	/** 接收到的数据缓存队列 */
	private final ConcurrentLinkedQueue<ByteBuf> buffers = new ConcurrentLinkedQueue<ByteBuf>();
	
	/** 对应的服务端Channel实例引用 */
	protected final MBUDPServerChannel serverchannel;
	/** 本Channel对应的客户端IP+端口信息 */
	protected final InetSocketAddress remote;
	
	/** “连接”是否已打开，true表示已打开，否则未打开。默认true。 */
	private volatile boolean open = true;
	
	/** 
	 * 是否“正在”读数据中标识。
	 * @see #doBeginRead() */
	private boolean reading = false;
	
	/**
	 * 构造方法。
	 * 
	 * @param serverchannel 对应的服务端Channel实例引用
	 * @param remote 本Channel对应的客户端IP+端口信息
	 */
	protected MBUDPChannel(MBUDPServerChannel serverchannel, InetSocketAddress remote) 
	{
		super(serverchannel);
		this.serverchannel = serverchannel;
		this.remote = remote;
	}

	@Override
	public ChannelMetadata metadata()
	{
		return metadata;
	}

	@Override
	public ChannelConfig config()
	{
		return config;
	}

	@Override
	public boolean isActive()
	{
		return open;
	}

	@Override
	public boolean isOpen() 
	{
		return isActive();
	}

	@Override
	protected void doClose() throws Exception 
	{
		open = false;
		serverchannel.removeChannel(this);
	}

	@Override
	protected void doDisconnect() throws Exception
	{
		doClose();
	}

	protected void addBuffer(ByteBuf buffer) 
	{
		buffers.add(buffer);
	}

	/**
	 * 对Channel读操作的准备工作。
	 */
	@Override
	protected void doBeginRead() throws Exception
	{
		// is reading check, because the pipeline head context will call read again
		if (reading)
			return;
		
		reading = true;
		
		try 
		{
			ByteBuf buffer = null;
			while ((buffer = buffers.poll()) != null) 
			{
				pipeline().fireChannelRead(buffer);
			}
			pipeline().fireChannelReadComplete();
		} 
		finally 
		{
			reading = false;
		}
	}

	@Override
	protected void doWrite(ChannelOutboundBuffer buffer) throws Exception 
	{
		// transfer all messages that are ready to be written to list
		final RecyclableArrayList list = RecyclableArrayList.newInstance();
		boolean freeList = true;
		
		try 
		{
			ByteBuf buf = null;
			while ((buf = (ByteBuf) buffer.current()) != null) 
			{
				list.add(buf.retain());
				buffer.remove();
			}
			
			freeList = false;
		} 
		finally 
		{
			if (freeList)
			{
				for (Object obj : list) 
				{
					ReferenceCountUtil.safeRelease(obj);
				}
				
				list.recycle();
			}
		}
		
		//schedule a task that will write those entries
		serverchannel.eventLoop().execute(new Runnable() {
			@Override
			public void run() 
			{
				try 
				{
					for (Object buf : list) 
					{
						serverchannel.unsafe()
							.write(new DatagramPacket((ByteBuf) buf, remote), voidPromise());
					}
					
					serverchannel.unsafe().flush();
				} 
				finally 
				{
					list.recycle();
				}
			}
		});
	}

	@Override
	protected boolean isCompatible(EventLoop eventloop) 
	{
		return eventloop instanceof DefaultEventLoop;
	}

	@Override
	protected AbstractUnsafe newUnsafe()
	{
		return new UdpChannelUnsafe();
	}

	@Override
	protected SocketAddress localAddress0()
	{
		return serverchannel.localAddress0();
	}

	@Override
	protected SocketAddress remoteAddress0() 
	{
		return remote;
	}

	@Override
	protected void doBind(SocketAddress addr) throws Exception
	{
		throw new UnsupportedOperationException();
	}

	private class UdpChannelUnsafe extends AbstractUnsafe 
	{
		@Override
		public void connect(SocketAddress addr1, SocketAddress addr2, ChannelPromise pr)
		{
			throw new UnsupportedOperationException();
		}
	}
}
