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
 * MBWebsocketClientInboundHandler.java at 2021-6-29 10:24:09, code by Jack Jiang.
 */
package net.x52im.mobileimsdk.server.network.websocket;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.timeout.ReadTimeoutException;
import net.x52im.mobileimsdk.server.ServerCoreHandler;
import net.x52im.mobileimsdk.server.network.Gateway;
import net.x52im.mobileimsdk.server.network.tcp.MBTCPClientInboundHandler;
import net.x52im.mobileimsdk.server.protocal.Protocal;
import net.x52im.mobileimsdk.server.utils.ServerToolKits;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 用于处理客户端的WebSocket连接、通信等的ChannelInboundHandler实现类。
 * 
 * @author Jack Jiang(http://www.52im.net/thread-2792-1-1.html)
 * @since 6.0
 * @see ServerCoreHandler
 * @see net.x52im.mobileimsdk.server.ServerLauncher#initGateWays()
 */
public class MBWebsocketClientInboundHandler  extends SimpleChannelInboundHandler<WebSocketFrame>
{
	private static Logger logger = LoggerFactory.getLogger(MBTCPClientInboundHandler.class); 
	
	private ServerCoreHandler serverCoreHandler = null;
	
	public MBWebsocketClientInboundHandler(ServerCoreHandler serverCoreHandler)
	{
		this.serverCoreHandler = serverCoreHandler;
	}
	
	/**
	 * “会话”处理过程中出现异步时会调用本方法。
	 */
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) {
		try{
			if(e instanceof ReadTimeoutException){
				logger.info("[IMCORE-ws]客户端{}的会话已超时失效，很可能是对方非正常通出或网络故障" +
						"，即将以会话异常的方式执行关闭流程 ...", ServerToolKits.clientInfoToString(ctx.channel()));
			}
			
			serverCoreHandler.exceptionCaught(ctx.channel(), e);
		}catch (Exception e2){
			logger.warn(e2.getMessage(), e);
		}
	}
	
	/**
	 * 客户端与服务端建立“连接”时会调用本方法。
	 */
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		super.channelActive(ctx);
		
		// 设置好网络类型标志（因为MobileIMSDK支持多种网络通信类型）
		Gateway.setSocketType(ctx.channel(), Gateway.SOCKET_TYPE_WEBSOCKET);
		// 业务处理
		serverCoreHandler.sessionCreated(ctx.channel());
	}
	
	/**
	 * 客户端与服务端断开“连接”时会调用本方法。
	 * <p>
	 * <b>“断开”的原因有可能多种可能性：</b>
	 * <ul>
	 *   <li>1）客户端因网络原因掉线而导致它的“会话”超时被服务端close掉；</li>
	 *   <li>2）客户端自行退出了“会话”，比如退出浏览器等；</li>
	 *   <li>3）客户端无故退出（比如浏览器强制关闭）而致服务端“会话”超时；</li>
	 *   <li>4）服务端因某种原因主动关闭了此“会话”等。</li>
	 * </ul>
	 */
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		super.channelInactive(ctx);
		
		// 清除好网络类型标志（因为MobileIMSDK支持多种网络通信类型）
		Gateway.removeSocketType(ctx.channel());
		// 业务处理
		serverCoreHandler.sessionClosed(ctx.channel());
	}
	
	/**
	 * 客户端向服务端发送数据时会调用本方法。
	 * <p>
	 * 本方法是"客户端 to 服务端"这个方向的数据通信唯一处理方法。
	 */
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
		// TextWebSocketFrame 在netty中，是用于为websocket处理文本的对象
        if (frame instanceof TextWebSocketFrame) {
        	String frameContent = ((TextWebSocketFrame) frame).text();
        	if(frameContent != null){
        		// 读取收到的数据
            	Protocal pFromClient = ServerToolKits.toProtocal(frameContent);
            	// 进入具体的业务逻辑处理
        		serverCoreHandler.messageReceived(ctx.channel(), pFromClient);
        	}
        	else
        		throw new UnsupportedOperationException("不支持的 frame content (is null!!)");
        }
        else 
        	throw new UnsupportedOperationException("不支持的 frame type: " + frame.getClass().getName());
	}
}
