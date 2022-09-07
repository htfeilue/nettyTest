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
 * MBTCPClientInboundHandler.java at 2021-6-29 10:24:09, code by Jack Jiang.
 */
package net.x52im.mobileimsdk.server.network.tcp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.ReadTimeoutException;
import net.x52im.mobileimsdk.server.ServerCoreHandler;
import net.x52im.mobileimsdk.server.network.Gateway;
import net.x52im.mobileimsdk.server.protocal.Protocal;
import net.x52im.mobileimsdk.server.utils.ServerToolKits;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 用于处理客户端的TCP连接、通信等的ChannelInboundHandler实现类。
 * 
 * @author Jack Jiang(http://www.52im.net/thread-2792-1-1.html)
 * @version 1.0
 * @since 3.1
 * @see ServerCoreHandler
 * @see net.x52im.mobileimsdk.server.ServerLauncher#initGateWays()
 */
public class MBTCPClientInboundHandler extends SimpleChannelInboundHandler<ByteBuf>
{
	private static Logger logger = LoggerFactory.getLogger(MBTCPClientInboundHandler.class); 
	
	private ServerCoreHandler serverCoreHandler = null;
	
	public MBTCPClientInboundHandler(ServerCoreHandler serverCoreHandler)
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
				logger.info("[IMCORE-tcp]客户端{}的会话已超时失效，很可能是对方非正常通出或网络故障" +
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
		Gateway.setSocketType(ctx.channel(), Gateway.SOCKET_TYPE_TCP);
		// 业务处理
		serverCoreHandler.sessionCreated(ctx.channel());
	}

	/**
	 * 客户端与服务端断开“连接”时会调用本方法。
	 * <p>
	 * <b>“断开”的原因有可能多种可能性：</b>
	 * <ul>
	 *   <li>1）客户端因网络原因掉线而导致它的“会话”超时被服务端close掉；</li>
	 *   <li>2）客户端自行退出了“会话”，比如退出APP等；</li>
	 *   <li>3）客户端无故退出（比如APP崩溃）而致服务端“会话”超时；</li>
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
	protected void channelRead0(ChannelHandlerContext ctx, ByteBuf bytebuf) throws Exception {
    	// 读取收到的数据
    	Protocal pFromClient = ServerToolKits.fromIOBuffer(bytebuf);
    	// 进入具体的业务逻辑处理
		serverCoreHandler.messageReceived(ctx.channel(), pFromClient);
	}
	
//	/**
//	 * 客户端事件通知。
//	 * <p>
//	 * 本方法用于捕获客户端会话超时事件，用于及时清理已超时失效的会话。
//	 * <p>
//	 * 关于Netty的超时实现，请见：http://docs.52im.net/extend/docs/src/netty4_1/io/netty/handler/timeout/ReadTimeoutHandler.html
//	 */
//	@Override
//    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
//		
//		logger.info("[IMCORE-tcp]evt="+evt+", evt.class="+evt.getClass().getName());
//		
//		if (evt instanceof IdleStateEvent) {
//            IdleStateEvent event = (IdleStateEvent)evt;
//            if (event.state() == IdleState.READER_IDLE) {
//            	logger.info("[IMCORE-tcp]"+ctx.channel()+"的会话已超时失效，很可能是对方非正常通出或网络故障，即将执行会话关闭流程 ...");
//    			serverCoreHandler.sessionClosed(ctx.channel());
//            }
//        } else {
//            super.userEventTriggered(ctx, evt);
//        }
//    }
}