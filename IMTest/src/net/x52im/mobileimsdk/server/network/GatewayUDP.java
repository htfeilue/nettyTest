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
 * GatewayUDP.java at 2021-6-29 10:24:09, code by Jack Jiang.
 */
package net.x52im.mobileimsdk.server.network;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.timeout.ReadTimeoutHandler;
import net.x52im.mobileimsdk.server.ServerCoreHandler;
import net.x52im.mobileimsdk.server.network.udp.MBUDPClientInboundHandler;
import net.x52im.mobileimsdk.server.network.udp.MBUDPServerChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * UDP协议的网关实现类。
 * 
 * @author Jack Jiang(http://www.52im.net/thread-2792-1-1.html)
 * @since 5.0
 */
public class GatewayUDP extends Gateway
{
	private static Logger logger = LoggerFactory.getLogger(GatewayUDP.class); 
	
	/** 
	 * 服务端UDP监听端口，默认7901。
	 * <p>
	 * 请在 {@link net.x52im.mobileimsdk.server.ServerLauncher#startup()}方法被调用前被设置，否则将不起效.
	 */
    public static int PORT = 7901;
    
    /** 
     * UDP Session的空闲超时时长（单位：秒），默认10秒。
     * 
     * <p>
     * 表示一个用户在非正常退出、网络故障等情况下，服务端判定此用户不在线的超时时间。
     * 此参数应与客户端的KeepAliveDaemon.KEEP_ALIVE_INTERVAL配合调整，为防止因心跳丢包
     * 而产生误判，建议本参数设置为“客户端的KeepAliveDaemon.KEEP_ALIVE_INTERVAL * (2或3)
     * + 典型客户网络延迟时间”，比如默认值10就是等于“3 * 3 + 1”（即服务端允许在最最极端的
     * 情况下即使连丢3个包也不会被误判为掉线！）。
     * 
     * <p>
     * 本值不宜过短，太短则会导致客户端因心跳丢包而误判为掉线，进而触发客户端的重登机制。原则
     * 上设置的长一点更有保证，但设置过长的话会导致服务端判定用户真正的非正常退出过于晚了，带
     * 给用户的体验就是明明好友非正常退出了，过了好长时间才能感知到他已退出。
     * 
     * <p>
     * <b>[补充关于会话超时时间值的设定技巧]：</b><br>
     * <pre>
     *    以客户端的心跳间隔是3秒为例，10秒内如果客户端连丢2个包，通常情况是能保住它的UDP连接的，<br>
     *    但如果设为6秒超时，那么在网络超烂时会导致客户端被判定掉线的几率升高，因为6秒超时下，丢<br>
     *    1个包就有可能被判定掉线了，而网络超烂的情况下丢一个包的可能性普通的很高，也就使得掉线变<br>
     *    的频繁而影响用户体验！
     * </pre>
     *   
	 * <p>
	 * 请在 {@link #startup()}方法被调用前被设置，否则将不起效.
     */
    public static int SESION_RECYCLER_EXPIRE = 10;
    
    /** 
     * <font color="#ff0000">框架专用内部变量：</font>
     * bossGroup用来接收进来的连接 (EventLoopGroup是用来处理IO操作的线程池 ) .
     * <p>
     * 作为全局变量的目的，当前仅用于关闭服务器时来释放此连接池对应的资源，别无它用。
     * 
     * @see #startup()
     */
    protected final EventLoopGroup __bossGroup4Netty = new NioEventLoopGroup();
 	
 	/** 
 	 * <font color="#ff0000">框架专用内部变量：</font>
 	 * workerGroup用来处理已经被接收的连接   (EventLoopGroup是用来处理IO操作的线程池 ) .
 	 * 
     * <p>
     * 作为全局变量的目的，当前仅用于关闭服务器时来释放此连接池对应的资源，别无它用。
     * 
 	 * @see net.x52im.mobileimsdk.server#startup()
 	 */
 	protected final EventLoopGroup __workerGroup4Netty = new DefaultEventLoopGroup();
 	
 	/**
 	 * <font color="#ff0000">框架专用内部变量：</font>服务器Channel引用.
 	 * 
 	 * <p>
 	 * 作为全局变量的目的，当前仅用于关闭服务器时来释放此Channel对应的资源，别无它用。
 	 * 
 	 * @see net.x52im.mobileimsdk.server#shutdown()
 	 */
 	protected Channel __serverChannel4Netty = null;
 	
 	/** 启动器 */
 	protected ServerBootstrap bootstrap = null;
 	
 	/**
     * 初始化 Netty的服务辅助启动类。
     * <p>
     * 因本框架中为了应用层编码的易用性，赋予了Netty中UDP的“会话”（或说“连接”）
     * 的能力，因而本方法中使用了跟TCP一样的ServerBootstrap而非Bootstrap。
     * 有关Bootstrap的官方API说明，请见：http://docs.52im.net/extend/docs/api/netty4_1/io/netty/bootstrap/Bootstrap.html
     */
 	@Override
    public void init(ServerCoreHandler serverCoreHandler)
    {
    	bootstrap = new ServerBootstrap()
    		// 设置并绑定Reactor线程池
    		.group(__bossGroup4Netty, __workerGroup4Netty)
    		// 设置并绑定服务端Channel
    		.channel(MBUDPServerChannel.class)
    		// 初始化针对客户端的handler链
    		.childHandler(initChildChannelHandler(serverCoreHandler));
    }
    
 	@Override
    public void bind() throws Exception
    {
    	//-> 绑定端口，开始接收进来的连接  
		ChannelFuture cf = bootstrap.bind("0.0.0.0", PORT).syncUninterruptibly();
		if (cf.isSuccess()) {
        	logger.info("[IMCORE-udp] 基于MobileIMSDK的UDP服务绑定端口"+PORT+"成功 √");
        }
        else{
        	logger.info("[IMCORE-udp] 基于MobileIMSDK的UDP服务绑定端口"+PORT+"失败 ×");
        }
		//-> 把Netty服务的服务器Channel引用保存起来备用
		__serverChannel4Netty = cf.channel();
		//-> 采用非同步方法监听netty的退出，从而优雅地实现资源释放（异步不会阻塞后面代码的执行）
		//-------------------------------------------------------------------------------
		//  [注意]：.closeFuture().await()或.closeFuture().sync()将导致线程阻塞，这在很多
		//         场景下是不合适的，最优雅的方式，应该是使用.closeFuture().addListener()
		//         来实现netty的优雅退出，请参考：https://blog.csdn.net/a294634473/article/details/89709324
		//-------------------------------------------------------------------------------
		__serverChannel4Netty.closeFuture().addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				// 释放资源退出：优雅地退出netty的线程组
				__bossGroup4Netty.shutdownGracefully();
				__workerGroup4Netty.shutdownGracefully();
			}
		});
		
		logger.info("[IMCORE-udp] .... continue ...");
		logger.info("[IMCORE-udp] 基于MobileIMSDK的UDP服务正在端口" + PORT+"上监听中...");
    }
	
    /**
     * 关闭本UDP网关的监听并释放其所占资源。
     */
 	@Override
	public void shutdown()
	{
    	// 关闭netty的服务端channel（此调用将自动触发上方
    	// __serverChannel4Netty.closeFuture().addListener(..)添加的监听器，从而实现netty的优雅退出）
    	if (__serverChannel4Netty != null) 
    		__serverChannel4Netty.close();

//		// 优雅地退出netty的线程组 - commnet at 20200619
//		__bossGroup4Netty.shutdownGracefully();
//		__workerGroup4Netty.shutdownGracefully();
	}
	
    /**
	 * 初始化针对Netty客户端的handler链。
	 * <p>
	 * 如有需要，子类可以重写此类实现自已的Inbound Hander链接逻辑。
	 * 
	 * @return handler链对象
	 * @see net.x52im.mobileimsdk.server.network.udp.MBUDPClientInboundHandler
	 * @see io.netty.handler.timeout.ReadTimeoutHandler.ReadTimeoutHandler
	 * @see io.netty.channel.ChannelInitializer.ChannelInitializer
	 */
	protected ChannelHandler initChildChannelHandler(final ServerCoreHandler serverCoreHandler)
	{
		// 返回Netty的Inbound Hanndler链
		return new ChannelInitializer<Channel>() {
			@Override
			protected void initChannel(Channel channel) throws Exception {
				channel.pipeline()
					// 设置会话超时处理handler
					.addLast(new ReadTimeoutHandler(SESION_RECYCLER_EXPIRE))
					// 设置客户端的会话逻辑handler
					.addLast(new MBUDPClientInboundHandler(serverCoreHandler));
			}
		};
	}
	
}
