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
 * Gateway.java at 2021-6-29 10:24:09, code by Jack Jiang.
 */
package net.x52im.mobileimsdk.server.network;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import net.x52im.mobileimsdk.server.ServerCoreHandler;

/**
 * 网关（用于服务端支持的各种网络通信类型的父类）。
 * 
 * @author Jack Jiang(http://www.52im.net/thread-2792-1-1.html)
 * @since 5.0
 */
public abstract class Gateway
{
	/** 用于用户会话（即Netty中的“Channel”）中存放Socket类型标识的属性key */
	public final static String SOCKET_TYPE_IN_CHANNEL_ATTRIBUTE = "__socket_type__";
	
	/** 用于用户会话（即Netty中的“Channel”）中存取属性 {@link SOCKET_TYPE_IN_CHANNEL_ATTRIBUTE} 用的AttributeKey */
	public static final AttributeKey<Integer> SOCKET_TYPE_IN_CHANNEL_ATTRIBUTE_ATTR = AttributeKey.newInstance(SOCKET_TYPE_IN_CHANNEL_ATTRIBUTE);
	
//	/** 网络通信类型常量：UDP */
//	public static final int SOCKET_TYPE_UDP       = 0;
//	/** 网络通信类型常量：TCP */
//	public static final int SOCKET_TYPE_TCP       = 1;
//	/** 网络通信类型常量：WebSocket */
//	public static final int SOCKET_TYPE_WEBSOCKET = 2;
	
	/** 网络通信类型常量：UDP */
	public static final int SOCKET_TYPE_UDP       = 0x0001; // 即2进制：0000 0001
	/** 网络通信类型常量：TCP */
	public static final int SOCKET_TYPE_TCP       = 0x0002; // 即2进制：0000 0010
	/** 网络通信类型常量：WebSocket */
	public static final int SOCKET_TYPE_WEBSOCKET = 0x0004; // 即2进制：0000 0100
	
	/**
	 * 初始化。
	 * 
	 * @param serverCoreHandler
	 */
	public abstract void init(ServerCoreHandler serverCoreHandler);
	
	/**
	 * 绑定端口、启动服务。
	 * 
	 * @throws Exception
	 */
	public abstract void bind() throws Exception;
	
	/**
	 * 关闭服务并释放资源。
	 */
	public abstract void shutdown();
	
	/**
	 * 为该Channel设置网络类型标识（因为MobileIMSDK支持多种网络通信类型）。
	 * 
	 * @param c 通信会话Channel对象引用
	 * @param socketType 网络类型标识
	 * @see Gateway#SOCKET_TYPE_UDP
	 * @see Gateway#SOCKET_TYPE_TCP
	 * @see Gateway#SOCKET_TYPE_WEBSOCKET
	 */
	public static void setSocketType(Channel c, int socketType)
	{
		c.attr(SOCKET_TYPE_IN_CHANNEL_ATTRIBUTE_ATTR).set(socketType);
	}
	
	/**
	 * 清除该Channel网络类型标识。
	 * 
	 * @param c 通信会话Channel对象引用
	 */
	public static void removeSocketType(Channel c)
	{
		c.attr(SOCKET_TYPE_IN_CHANNEL_ATTRIBUTE_ATTR).set(null);
	}
	
	/**
	 * 获取该Channel中存放的网络类型标识。
	 * 
	 * @param c 通信会话Channel对象引用
	 * @return 如果成功取到则正常返回网络类型标识常量，否则返回-1
	 * @see Gateway#SOCKET_TYPE_UDP
	 * @see Gateway#SOCKET_TYPE_TCP
	 * @see Gateway#SOCKET_TYPE_WEBSOCKET
	 */
	public static int getSocketType(Channel c)
	{
		Integer socketType = c.attr(SOCKET_TYPE_IN_CHANNEL_ATTRIBUTE_ATTR).get();
		if(socketType != null)
			return socketType.intValue();
		return -1;
	}
	
	/**
	 * 是否支持UDP。
	 * 
	 * @param support
	 * @return
	 */
	public static boolean isSupportUDP(int support)
	{
		// 位运算
		return (support & SOCKET_TYPE_UDP) == SOCKET_TYPE_UDP;
	}
	
	/**
	 * 是否支持TCP。
	 * 
	 * @param support
	 * @return
	 */
	public static boolean isSupportTCP(int support)
	{
		// 位运算
		return (support & SOCKET_TYPE_TCP) == SOCKET_TYPE_TCP;
	}
	
	/**
	 * 是否支持WebSocket。
	 * 
	 * @param support
	 * @return
	 * @since 6.0
	 */
	public static boolean isSupportWebSocket(int support)
	{
		// 位运算
		return (support & SOCKET_TYPE_WEBSOCKET) == SOCKET_TYPE_WEBSOCKET;
	}

	/**
	 * 客户端连接是否是TCP。
	 * 
	 * @param c
	 * @return
	 */
	public static boolean isTCPChannel(Channel c)
	{
//		return (c != null && c instanceof NioSocketChannel);
		return (c != null && getSocketType(c) == SOCKET_TYPE_TCP);
	}

	/**
	 * 客户端连接是否是UDP。
	 * 
	 * @param c
	 * @return
	 */
	public static boolean isUDPChannel(Channel c)
	{
//		return (c != null && c instanceof MBUDPChannel);
		return (c != null && getSocketType(c) == SOCKET_TYPE_UDP);
	}
	
	/**
	 * 客户端连接是否是WebSocket。
	 * 
	 * @param c
	 * @return
	 */
	public static boolean isWebSocketChannel(Channel c)
	{
		return (c != null && getSocketType(c) == SOCKET_TYPE_WEBSOCKET);
	}
	
	/**
	 * 返回客户端连接的网络通信类型的字符串描述，主要用于Debug中。
	 * 
	 * @param c
	 * @return
	 */
	public static String $(Channel c)
	{
		return getGatewayFlag(c);
	}
	
	/**
	 * 返回客户端连接的网络通信类型的字符串描述，主要用于Debug中。
	 * 
	 * @param c
	 * @return
	 */
	public static String getGatewayFlag(Channel c)
	{
//		logger.info(">>>>>> c.class="+c.getClass().getName());
		if(Gateway.isUDPChannel(c))
			return "udp";
		else if(Gateway.isTCPChannel(c))
			return "tcp";
		else if(Gateway.isWebSocketChannel(c))
			return "websocket";
		else 
			return "unknow";
	}
}
