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
 * ServerLauncher.java at 2021-6-29 10:24:10, code by Jack Jiang.
 */
package net.x52im.mobileimsdk.server;

import java.io.IOException;

import net.x52im.mobileimsdk.server.event.MessageQoSEventListenerS2C;
import net.x52im.mobileimsdk.server.event.ServerEventListener;
import net.x52im.mobileimsdk.server.network.Gateway;
import net.x52im.mobileimsdk.server.network.GatewayTCP;
import net.x52im.mobileimsdk.server.network.GatewayUDP;
import net.x52im.mobileimsdk.server.network.GatewayWebsocket;
import net.x52im.mobileimsdk.server.qos.QoS4ReciveDaemonC2S;
import net.x52im.mobileimsdk.server.qos.QoS4SendDaemonS2C;
import net.x52im.mobileimsdk.server.protocal.Protocal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MobileIMSDK的服务端入口主类。
 * <p>
 * 为了简化API的调用，理论上使用者使用本类公开的方法即可实现MobileIMSDK
 * 的所有能力。
 * <p>
 * 本类主要实现服务端的初始化、基本服务的启停以及一些提供给开发者的公开API。
 * <p>
 * 服务端默认不会自动开启，请调用 {@link #startup()}以便启动MobileIMSDK的服务端, {@link #shutdown()}关闭
 * 服务端，直到再次调用 {@link #startup()} 。
 * <p>
 * <b>提示1：</b>
 * 请重写 {@link #initListeners()}方法，以实现应用层的回调监听器以便实现自定义业务逻辑。
 * <p>
 * <b>提示2：</b>
 * 如要开启与MobileIMSDK-Web版的消息互通，请设置 {@link ServerLauncher#bridgeEnabled} == true
 * ，默认为false.
 * 
 * @author Jack Jiang(http://www.52im.net/thread-2792-1-1.html)
 * @version 5.0
 * @since 3.1
 * @see ServerCoreHandler
 */
public abstract class ServerLauncher 
{
	private static Logger logger = LoggerFactory.getLogger(ServerLauncher.class); 
	
//	/**
//	 * 全局设置：服务端Debug日志输出总开关（建议压力测试和生产环境下设为false）。
//	 * <p>
//	 * 默认值：true。
//	 * <p>
//	 * <b>特别注意：</b>为了减少过多地使用本标识而增加服务器不必要的if判断开销，自2017年
//	 * 12月11日v3.1版起，已废除本标识的作用。<font color="red">为了保证性能，在压力测试或
//	 * 生产环境下请直接关闭log4j的日志输出（设置log4j的日志输出级别即可(比如设为WARN级别等
//	 * )，请查阅log4j文档），<b>切记！！</b></font>
//	 * 
//	 * @since 3.0
//	 * @deprecated 于2017年12月11日v3.1版废除本字段，为了性能请使用log4j的日志输出级别来控制日志输出
//	 */
//	public static boolean debug = true;
//	
//	/**
//	 * 全局设置：AppKey。
//	 * <p>
//	 * <b>友情提示：</b>本字段目前为保留字段，使用时无需独立设置或随意设置均不影响使用。
//	 * 
//	 * @deprecated
//	 */
//	public static String appKey = null;
	
	/**
	 * 是否为消息/指令打上服务端时间戳。true表示需要，否则不需要，默认false。
	 * <p>
	 * 此时间戳可以辅助用于应用层处理消息的顺序问题。
	 * 
	 * @see Protocal#getSm()
	 * @see Protocal#genServerTimestamp()
	 */
	public static boolean serverTimestamp = false;
	
    /**
     * 是否允许与MobileIMSDK Web版进行互通。true表示需要互通，否则不互通，默认false。
     * <p>
	 * <b>注意：</b>请在 {@link #startup()}方法被调用前被设置，否则将不起效.
	 * 
     * @since 3.0
     */
    public static boolean bridgeEnabled = false;
    
    /**
     * 当前支持的接入网关类型（即支持的网络传输协议类型）。
     * <p>
	 * <b>注意：</b>请在 {@link #startup()}方法被调用前被设置，否则将不起效.
     * <p>
     * <b>使用举例：</b><br>
     * <pre>
     * 1）仅支持UDP时，设置本参数为：Gateway.SOCKET_TYPE_UDP;
     * 2）仅支持TCP时，设置本参数：Gateway.SOCKET_TYPE_TCP;
     * 3）仅支持WebSocket时，设置本参数：Gateway.SOCKET_TYPE_WEBSOCKET;
     * 3）需支持UDP+TCP+WebSocket时，设置本参数：Gateway.SOCKET_TYPE_UDP | Gateway.SOCKET_TYPE_TCP | Gateway.SOCKET_TYPE_WEBSOCKET;
     * </pre>
     * 
     * @see Gateway#SOCKET_TYPE_UDP
     * @see Gateway#SOCKET_TYPE_TCP
     * @see Gateway.SOCKET_TYPE_WEBSOCKET
     */
    public static int supportedGateways = 0;
    
    /**
     * MobileIMSDK框架的核心通信逻辑实现类（实现的是MobileIMSDK服务端的通信处理核心算法）。
     */
    protected ServerCoreHandler serverCoreHandler = null; 
    
    /** 服务端是否启动并运行中 */
    private boolean running = false;
    
    /** UDP网关实现类 */
    private Gateway udp = null;
    /** TCP网关实现类 */
    private Gateway tcp = null;
    /** WebSocket网关实现类 */
    private Gateway ws = null;
    
    public ServerLauncher() throws IOException 
    {
    	// default do nothing
    }
    
    /**
     * 初始化MobileIMSDK的ServerCoreHandler实现类。
     * <p>
     * 本类是MobileIMSDK的服务端网络调度算法核心所在，其性能将决定整个
     * 即时通讯架构的数据交换性能、负载能力、吞吐效率等。
     * 
     * @return 初始化完成后的ServerCoreHandler实现对象
     * @since 2.1.3
     */
    protected ServerCoreHandler initServerCoreHandler()
    {
    	return new ServerCoreHandler();
    }
    
    /**
     * 初始化回调处理事件监听器。
     * <p>
     * 请重写 {@link #initListeners()}方法，以实现应用层的回调监听器以便实现自定义业务
     * 逻辑，可以设置的回调监听器有： {@link #setServerEventListener(ServerEventListener)}
     * 和 {@link #setServerMessageQoSEventListener(MessageQoSEventListenerS2C)}。
     */
    protected abstract void initListeners();
    
    /**
     * 初始化 网关（一个网关实例对应一种网络通信类型）。
     */
    protected void initGateways()
    {
    	if(Gateway.isSupportUDP(supportedGateways))
    	{
	    	udp = new GatewayUDP();
	    	udp.init(this.serverCoreHandler);
    	}
    	
    	if(Gateway.isSupportTCP(supportedGateways))
    	{
	    	tcp = new GatewayTCP();
	    	tcp.init(this.serverCoreHandler);
    	}
    	
    	if(Gateway.isSupportWebSocket(supportedGateways))
    	{
    		ws = new GatewayWebsocket();
    		ws.init(this.serverCoreHandler);
    	}
    }
    
    /**
     * 开启服务端。
     * 
     * @throws IOException 端口绑定失败将抛出本异常
     * @see #initServerCoreHandler()
     * @see #initListeners()
     * @see #initGateways()
     * @see net.x52im.mobileimsdk.server.qos.QoS4ReciveDaemonC2S#startup()
     * @see net.x52im.mobileimsdk.server.qos.QoS4SendDaemonS2C#startup(boolean)
     * @see ServerCoreHandler#lazyStartupBridgeProcessor()
     * @see ServerLauncher#bridgeEnabled
     * @see io.netty.bootstrap.ServerBootstrap#bind(String, int)
     */
    public void startup() throws Exception
    {	
    	if(!this.running)
    	{
    		// ** 【1】初始化MobileIMSDK的核心通信逻辑实现类
    		serverCoreHandler = initServerCoreHandler();

    		// ** 【2】初始化消息处理事件监听者
    		initListeners();

    		// ** 【3】初始化各通信协议的网关接入服务（就是初始化网络监听啦）  
    		initGateways();

    		// ** 【4】启动服务端对C2S模式的QoS机制下的防重复检查线程
    		QoS4ReciveDaemonC2S.getInstance().startup();
    		// ** 【5】启动服务端对S2C模式下QoS机制的丢包重传和离线通知线程
    		QoS4SendDaemonS2C.getInstance().startup(true).setServerLauncher(this);

    		// 如果需要与Web版互通
    		if(ServerLauncher.bridgeEnabled){

//    			// ** 【6】启动桥接模式下服务端的QoS机制下的防重复检查线程(since 3.0)
//    			QoS4ReciveDaemonC2B.getInstance().startup();
//    			// ** 【7】启动桥接模式下服务端的QoS机制的丢包重传和离线通知线程(since 3.0)
//    			QoS4SendDaemonB2C.getInstance().startup(true).setServerLauncher(this);

    			// ** 【8】上面initServerCoreHandler不立即启动跨服桥接处理器而放在此处在
    			//    所有初始化完成后（放置于initListeners后，是防止交叉引用产生不可预知的错误）
    			serverCoreHandler.lazyStartupBridgeProcessor();

    			logger.info("[IMCORE] 配置项：已开启与MobileIMSDK Web的互通.");
    		}
    		else{
    			logger.info("[IMCORE] 配置项：未开启与MobileIMSDK Web的互通.");
    		}
    		
    		// ** 【9】服务端开始各通信类型的网络侦听
    		bind();

    		// ** 【10】设置启动标识
    		this.running = true;
    	}
    	else
    	{
    		logger.warn("[IMCORE] 基于MobileIMSDK的通信服务正在运行中，本次startup()失败，请先调用shutdown()后再试！");
    	}
    }
    
    /**
     * 绑定端口，启动网络接入服务。
     * 
     * @throws Exception
     */
    protected void bind() throws Exception
    {
    	if(udp != null)
    		udp.bind();
    	if(tcp != null)
    		tcp.bind();
    	if(ws != null)
    		ws.bind();
    }

	/**
     * 关闭服务端。
     * 
     * @see io.netty.channel.Channel#close()
     * @see net.x52im.mobileimsdk.server.qos.QoS4ReciveDaemonC2S#stop()
     * @see net.x52im.mobileimsdk.server.qos.QoS4SendDaemonS2C#stop()
     * @see net.x52im.mobileimsdk.server.bridge.QoS4ReciveDaemonC2B#stop()
     * @see net.x52im.mobileimsdk.server.bridge.QoS4SendDaemonB2C#stop()
     */
    public void shutdown()
    {
    	//** 【1】释放网络接入资源 START
    	if(udp != null)
    		udp.shutdown();
    	if(tcp != null)
    		tcp.shutdown();
    	if(ws != null)
    		ws.shutdown();
    	//** 【1】END
    	
		//** 【2】释放MobileIMSDK框架的资源 START
    	// 停止QoS机制（目前服务端只支持C2S模式的QoS）下的防重复检查线程
    	QoS4ReciveDaemonC2S.getInstance().stop();
    	// 停止服务端对S2C模式下QoS机制的丢包重传和离线通知线程
    	QoS4SendDaemonS2C.getInstance().stop();
//    	// 需要与Web版互通时
//    	if(ServerLauncher.bridgeEnabled){
//    		QoS4ReciveDaemonC2B.getInstance().stop();
//    		QoS4SendDaemonB2C.getInstance().stop();
//    	}
    	//** 【2】 END
    	
    	// ** 设置启动标识
    	this.running = false;
    }
    
    /**
     * 返回服务端通用事件回调监听器对象引用。
     * 
     * @return ServerEventListener对象
     * @see ServerCoreHandler#getServerEventListener()
     */
    public ServerEventListener getServerEventListener()
	{
		return serverCoreHandler.getServerEventListener();
	}
    /**
     * 设置服务端通用事件回调监听器。
     * 
     * @param serverEventListener ServerEventListener对象
     * @see ServerCoreHandler#setServerEventListener(ServerEventListener)
     */
	public void setServerEventListener(ServerEventListener serverEventListener)
	{
		this.serverCoreHandler.setServerEventListener(serverEventListener);
	}
	
	/**
	 * 返回QoS机制的Server主动消息发送之质量保证事件监听器对象。
	 * 
	 * @return MessageQoSEventListenerS2C对象
	 * @see ServerCoreHandler#getServerMessageQoSEventListener()
	 */
	public MessageQoSEventListenerS2C getServerMessageQoSEventListener()
	{
		return serverCoreHandler.getServerMessageQoSEventListener();
	}
	/**
	 * 设置QoS机制的Server主动消息发送之质量保证事件监听器。
	 * 
	 * @param serverMessageQoSEventListener MessageQoSEventListenerS2C对象
	 * @see ServerCoreHandler#setServerMessageQoSEventListener(MessageQoSEventListenerS2C)
	 */
	public void setServerMessageQoSEventListener(MessageQoSEventListenerS2C serverMessageQoSEventListener)
	{
		this.serverCoreHandler.setServerMessageQoSEventListener(serverMessageQoSEventListener);
	}

	/**
	 * 获取 ServerCoreHandler 对象引用。
	 * 
	 * @return 返回对象引用
	 * @since 3.0
	 */
	public ServerCoreHandler getServerCoreHandler()
	{
		return serverCoreHandler;
	}
	
	/**
     * 服务端是否启动并运行中。
     * 
     * @return true表示已正常启动并运行，否则没有启动
     */
    public boolean isRunning()
	{
		return running;
	}
	
//	public static void main(String[] args) throws IOException 
//    {
//        new ServerLauncher().startup();
//    }
}
