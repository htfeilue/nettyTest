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
 * GatewayWebsocket.java at 2021-6-29 10:24:09, code by Jack Jiang.
 */
package net.x52im.mobileimsdk.server.network;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.handler.timeout.ReadTimeoutHandler;
import net.x52im.mobileimsdk.server.ServerCoreHandler;
import net.x52im.mobileimsdk.server.network.websocket.MBWebsocketClientInboundHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GatewayWebsocket extends Gateway
{
	private static Logger logger = LoggerFactory.getLogger(GatewayWebsocket.class); 
	
	public static String WEBSOCKET_PATH = "/websocket";
	
	public static boolean SSL = false;
	
	/** 
	 * 服务端WebSocket监听端口，默认3000。
	 * <p>
	 * 请在 {@link net.x52im.mobileimsdk.server.ServerLauncher#startup()}方法被调用前被设置，否则将不起效.
	 */
    public static int PORT = 3000;
    
    /** 
     * TCP Session的空闲超时时长（单位：秒），默认20秒。
     * 
     * <p>
     * 表示一个用户在非正常退出、网络故障等情况下，服务端判定此用户不在线的超时时间。
     * 此参数应与客户端的KeepAliveDaemon.KEEP_ALIVE_INTERVAL配合调整，为防止产生误判，
     * 建议本参数设置为“客户端的KeepAliveDaemon.KEEP_ALIVE_INTERVAL + 典型客户网络延迟时间”，
     * 比如默认值20就是等于“15 + 5”（即服务端允许在最最极端的情况下连续心跳两次超时后将被误判
     * 为掉线！）。
     * 
     * <p>
     * 本值不宜过短，太短则会导致客户端因心跳丢包而误判为掉线，进而触发客户端的重登机制。原则
     * 上设置的长一点更有保证，但设置过长的话会导致服务端判定用户真正的非正常退出过于晚了，带
     * 给用户的体验就是明明好友非正常退出了，过了好长时间才能感知到他已退出。
     *   
	 * <p>
	 * 请在 {@link net.x52im.mobileimsdk.server.ServerLauncher#startup()}方法被调用前被设置，否则将不起效.
	 * 
	 * @see net.x52im.mobileimsdk.server.utils.ServerToolKits#setSenseModeTCP(net.x52im.mobileimsdk.server.utils.ServerToolKits.SenseModeTCP)
     */
    public static int SESION_RECYCLER_EXPIRE = 20;//10;
    
    /** 
     * <font color="#ff0000">框架专用内部变量：</font>
     * bossGroup用来接收进来的连接 (EventLoopGroup是用来处理IO操作的线程池 ) .
     * <p>
     * 作为全局变量的目的，当前仅用于关闭服务器时来释放此连接池对应的资源，别无它用。
     * 
     * @see net.x52im.mobileimsdk.server.ServerLauncher#startup()
     */
	protected final EventLoopGroup __bossGroup4Netty = new NioEventLoopGroup(1);
 	
 	/** 
 	 * <font color="#ff0000">框架专用内部变量：</font>
 	 * workerGroup用来处理已经被接收的连接   (EventLoopGroup是用来处理IO操作的线程池 ) .
 	 * 
     * <p>
     * 作为全局变量的目的，当前仅用于关闭服务器时来释放此连接池对应的资源，别无它用。
     * 
 	 * @see net.x52im.mobileimsdk.server.ServerLauncher#startup()
 	 */
 	protected final EventLoopGroup __workerGroup4Netty = new NioEventLoopGroup();
 	
 	/**
 	 * <font color="#ff0000">框架专用内部变量：</font>
 	 * Netty服务的服务器Channel引用.
 	 * 
 	 * <p>
 	 * 作为全局变量的目的，当前仅用于关闭服务器时来释放此Channel对应的资源，别无它用。
 	 * 
 	 * @see net.x52im.mobileimsdk.server.ServerLauncher#shutdown()
 	 */
 	protected Channel __serverChannel4Netty = null;
 	
 	/** 启动器 */
 	protected ServerBootstrap bootstrap = null;
 	
 	/**
     * 初始化 Netty的服务辅助启动类。
     * 
     * @see net.x52im.mobileimsdk.server.ServerLauncher#initGateways()
     */
 	@Override
 	public void init(ServerCoreHandler serverCoreHandler)
    {
 		// 配置SSL
        SslContext sslCtx = null;
        try{
        	if (SSL) {
                SelfSignedCertificate ssc = new SelfSignedCertificate();
                sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
            } else {
                sslCtx = null;
            }
		}
		catch (Exception e){
			logger.error("[IMCORE-ws] SSL证书准备失败：", e);
		}
        
    	//** 新建启动器
        bootstrap = new ServerBootstrap()
			// 设置并绑定Reactor线程池
			.group(__bossGroup4Netty, __workerGroup4Netty)
			// 设置并绑定服务端Channel
			.channel(NioServerSocketChannel.class)
			// 初始化针对客户端的handler链
			.childHandler(initChildChannelHandler(sslCtx, serverCoreHandler));
    }
	
 	@Override
 	public void bind() throws Exception
    {
		//-> 绑定端口，启动服务
        ChannelFuture cf = bootstrap.bind(PORT).sync();
        if (cf.isSuccess()) {
        	logger.info("[IMCORE-ws] 基于MobileIMSDK的WebSocket服务绑定端口"+PORT+"成功 √");
        }
        else{
        	logger.info("[IMCORE-ws] 基于MobileIMSDK的WebSocket服务绑定端口"+PORT+"失败 ×");
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
		
		logger.info("[IMCORE-ws] .... continue ...");
		logger.info("[IMCORE-ws] 基于MobileIMSDK的WebSocket服务正在端口"+ PORT +"上监听中"+(SSL?"(已开启SSL)":"")+"...");
    }
	
 	/**
     * 关闭本TCP网关的监听并释放其所占资源。
     */
 	@Override
	public void shutdown()
	{
    	// 关闭netty的服务端channel（此调用将自动触发上方
    	// __serverChannel4Netty.closeFuture().addListener(..)添加的监听器，从而实现netty的优雅退出）
    	if (__serverChannel4Netty != null) 
    		__serverChannel4Netty.close();
	}
 	
	/**
	 * 初始化针对Netty客户端的handler链。
	 * <p>
	 * 如有需要，子类可以重写此类实现自已的Inbound Hander链接逻辑。
	 * 
	 * @return handler链对象
	 * @see #init()
	 * @see net.x52im.mobileimsdk.server.network.tcp.MBTCPClientInboundHandler
	 * @see io.netty.handler.timeout.ReadTimeoutHandler.ReadTimeoutHandler
	 * @see io.netty.channel.ChannelInitializer.ChannelInitializer
	 */
    protected ChannelHandler initChildChannelHandler(final SslContext sslCtx, final ServerCoreHandler serverCoreHandler)
	{
		// 返回Netty的Inbound Hanndler链
		return new ChannelInitializer<Channel>() {
			@Override
			protected void initChannel(Channel channel) throws Exception {
				
				ChannelPipeline pipeline = channel.pipeline();   
				
				// SSL支持
				if (sslCtx != null) {
		            pipeline.addLast(sslCtx.newHandler(channel.alloc()));
		        }
				
				// ====================== 以下是用于支持http协议    ======================
		        // http编解码器（WebSocket的握手是基于http协议）
		        pipeline.addLast(new HttpServerCodec());
		        // 对httpMessage进行聚合，聚合成FullHttpRequest或FullHttpRespons
		        pipeline.addLast(new HttpObjectAggregator(65536));
		        
		        // ====================== 以下是支持httpWebsocket ======================
		        // WebSocket协议处理
		        pipeline.addLast(new WebSocketServerProtocolHandler(WEBSOCKET_PATH, null, true));
		        // 设置会话超时处理handler（提示：此handler不能放最后，否则读超时异常将无法被InboundHandler处理）
				pipeline.addLast(new ReadTimeoutHandler(SESION_RECYCLER_EXPIRE));
		        // 自定义的handler
		        pipeline.addLast(new MBWebsocketClientInboundHandler(serverCoreHandler));
			}
		};
	}
}
