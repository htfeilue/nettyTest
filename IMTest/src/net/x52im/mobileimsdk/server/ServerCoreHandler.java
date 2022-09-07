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
 * ServerCoreHandler.java at 2021-6-29 10:24:09, code by Jack Jiang.
 */
package net.x52im.mobileimsdk.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import net.x52im.mobileimsdk.server.event.MessageQoSEventListenerS2C;
import net.x52im.mobileimsdk.server.event.ServerEventListener;
import net.x52im.mobileimsdk.server.network.Gateway;
import net.x52im.mobileimsdk.server.processor.BridgeProcessor;
import net.x52im.mobileimsdk.server.processor.LogicProcessor;
import net.x52im.mobileimsdk.server.processor.OnlineProcessor;
import net.x52im.mobileimsdk.server.protocal.Protocal;
import net.x52im.mobileimsdk.server.protocal.ProtocalType;
import net.x52im.mobileimsdk.server.utils.LocalSendHelper;
import net.x52im.mobileimsdk.server.utils.ServerToolKits;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MobileIMSDK的核心通信逻辑实现类。
 * <p>
 * 本类是MobileIMSDK的服务端网络调度算法核心所在，其性能将决定整个
 * 即时通讯架构的数据交换性能、负载能力、吞吐效率等。
 * 
 * @author Jack Jiang(http://www.52im.net/thread-2792-1-1.html)
 * @version 1.0
 * @since 3.1
 * @see net.x52im.mobileimsdk.server.processor.LogicProcessor
 * @see net.x52im.mobileimsdk.server.processor.BridgeProcessor
 * @see net.x52im.mobileimsdk.server.processor.OnlineProcessor
 */
public class ServerCoreHandler
{
	private static Logger logger = LoggerFactory.getLogger(ServerCoreHandler.class);  
	
	/** 服务端事件回调实现 */
	protected ServerEventListener serverEventListener = null;
	/** QoS机制下的S2C模式中，由服务端主动发起消息的QoS事件回调实现 */
	protected MessageQoSEventListenerS2C serverMessageQoSEventListener = null;
	
	/** IM消息逻辑处理器（真正的算法实现都在此类中处理）. 
	 * @since 3.0 */
	protected LogicProcessor logicProcessor = null;
	
	/** 跨机器通信中间件服务提供者.
	 * @since 3.0 */
	protected BridgeProcessor bridgeProcessor = null;
	
	/**
	 * 构造方法。
	 * 
	 * @see #createLogicProcessor()
	 * @see ServerLauncher#bridgeEnabled
	 * @see #createBridgeProcessor()
	 */
    public ServerCoreHandler()
    {
    	logicProcessor = this.createLogicProcessor();
    	
    	// 需要与Web版互通
    	if(ServerLauncher.bridgeEnabled)
    		bridgeProcessor = this.createBridgeProcessor();
    }
    
    /**
     * 创建MobileIMSDK框架核心层的数据交互处理逻辑封装对象。
     * <p>
     * 子类可继承本方法实现自已的逻辑，但在您没有完全掌握MobileIMSDK算法前不建议这样做。
     * 
     * @return 返回新的LogicProcessor对象
     * @since 3.0
     * @see net.x52im.mobileimsdk.server.processor.LogicProcessor
     */
    protected LogicProcessor createLogicProcessor()
    {
    	return new LogicProcessor(this);
    }
    
    /**
     * 新建跨机器通信消息中间件服务提供者。
     * <p>
     * 子类可继承本方法实现自已的逻辑，但在您没有完全掌握MobileIMSDK算法前不建议这样做。
     * 
     * @return  返回新的BridgeProcessor对象
     * @since 3.0
     * @see net.x52im.mobileimsdk.server.processor.BridgeProcessor
     */
    protected BridgeProcessor createBridgeProcessor()
    {
    	BridgeProcessor bp = new BridgeProcessor(){
			protected void realtimeC2CSuccessCallback(Protocal p){
				serverEventListener.onTransferMessage4C2C(p);
			}

			@Override
			protected boolean offlineC2CProcessCallback(Protocal p){
				return serverEventListener.onTransferMessage_RealTimeSendFaild(p);
			}
    	};
    	return bp;
    }
    
    /**
     * 单独的方法启动跨服务器通信桥接处理器，防止出现交叉引用而发生不可预知的错误。
     * <p>
     * 本方法的目前在 {@link ServerLauncher#startup()}中被调用，从而启动与Web的消息互通桥接器。
     * <p>
     * 本方法只在 {@link ServerLauncher#bridgeEnabled} == true时才会真实启动桥接处理器。
     * 
     * @see ServerLauncher#bridgeEnabled
     * @see net.x52im.mobileimsdk.server.processor.BridgeProcessor#start()
     */
    public void lazyStartupBridgeProcessor()
    {
    	if(ServerLauncher.bridgeEnabled && bridgeProcessor != null)
    	{
    		// 必须start
    		bridgeProcessor.start();
    	}
    }

    /**
     * 异常回调方法。
     * <p>
     * 本类中将在异常发生时，立即close当前会话。
     * <p>
     * 本方法将被 {@link net.x52im.mobileimsdk.server.network.tcp.MBTCPClientInboundHandler#exceptionCaught(
     * io.netty.channel.ChannelHandlerContext, Throwable)}调用，以便接受Netty中客户端
     * “会话”处理过程中发生的异常。
     * 
     * @param session 发生异常的会话
     * @param cause 异常内容
     * @see io.netty.channel.Channel#close(boolean)
     * @see net.x52im.mobileimsdk.server.network.tcp.MBTCPClientInboundHandler#exceptionCaught(io.netty.channel.ChannelHandlerContext, Throwable)
     */
    public void exceptionCaught(Channel session, Throwable cause) throws Exception 
    {
        logger.debug("[IMCORE-"+Gateway.$(session)+"]此客户端的Channel抛出了exceptionCaught，原因是："
        		+cause.getMessage()+"，可以提前close掉了哦！", cause);
        session.close();
    }

    /**
     * 框架中收到客户端消息的回调方法。
     * <p>
     * 本类将在此方法中实现完整的即时通讯数据交互和处理策略，<font color="#ff0000">
     * 本方法是整个MobileIMSDK服务端的核心所在</font>。
     * <p>
     * 本方法将被 {@link net.x52im.mobileimsdk.server.network.tcp.MBTCPClientInboundHandler#channelRead0(
     * io.netty.channel.ChannelHandlerContext, ByteBuf)}调用，以便接受Netty中客户端
     * 发送的数据。
     * 
     * @param session 收到消息对应的会话引用
     * @param pFromClient 读取到的数据内容（已被转换成Protocal对象）
     * @throws Exception 当有错误发生时将抛出异常
     * @see net.x52im.mobileimsdk.server.processor.LogicProcessor
     * @see net.x52im.mobileimsdk.server.network.tcp.MBTCPClientInboundHandler#channelRead0(io.netty.channel.ChannelHandlerContext, ByteBuf)
     */
//    public void messageReceived(Channel session, ByteBuf bytebuf) throws Exception 
    public void messageReceived(Channel session, Protocal pFromClient) throws Exception 
    {
//    	// 读取收到的数据
//    	Protocal pFromClient = ServerToolKits.fromIOBuffer(bytebuf);

    	String remoteAddress = ServerToolKits.clientInfoToString(session);
    	
    	// TODO: 以下代码仅用于开发调试时，建议即时注释掉！！
    	logger.info("---------------------------------------------------------");
    	logger.info("[IMCORE-"+Gateway.getGatewayFlag(session)+"] << 收到客户端"+remoteAddress+"的消息:::"+pFromClient.toGsonString());

    	switch(pFromClient.getType())
    	{
	    	// 【MobileIMSDK框架层协议：客户端与客户端、客户端与服务端之间的ACK应答包】
	    	case ProtocalType.C.FROM_CLIENT_TYPE_OF_RECIVED:
	    	{
	    		logger.info("[IMCORE-{}]<< 收到客户端{}的ACK应答包发送请求.", Gateway.$(session), remoteAddress);
	
	    		if(!OnlineProcessor.isLogined(session))
	    		{
	    			LocalSendHelper.replyDataForUnlogined(session, pFromClient, null);
	    			return;
	    		}
	
	    		logicProcessor.processACK(pFromClient, remoteAddress);
	    		break;
	    	}
	    	// 【MobileIMSDK框架层协议：用户通用数据转发请求】
	    	case ProtocalType.C.FROM_CLIENT_TYPE_OF_COMMON$DATA:
	    	{
	    		logger.info("[IMCORE-{}]<< 收到客户端{}的通用数据发送请求.", Gateway.$(session), remoteAddress);
	
	    		// 开始回调
	    		if(serverEventListener != null)
	    		{
	    			if(!OnlineProcessor.isLogined(session))
	    			{
	    				LocalSendHelper.replyDataForUnlogined(session, pFromClient, null);
	    				return;
	    			}
	
	    			// 【C2S数据】客户端发给服务端的消息
	    			if("0".equals(pFromClient.getTo()))
	    				logicProcessor.processC2SMessage(session, pFromClient, remoteAddress);
	    			// 【C2C数据】客户端发给客户端的消息
	    			else
	    				logicProcessor.processC2CMessage(bridgeProcessor, session, pFromClient, remoteAddress);
	    		}
	    		else
	    		{
	    			logger.warn("[IMCORE-{}]<< 收到客户端{}的通用数据传输消息，但回调对象是null，回调无法继续.", Gateway.$(session), remoteAddress);
	    		}
	    		break;
	    	}
	    	// 【MobileIMSDK框架层协议：用户KeepAlive心跳包】
	    	case ProtocalType.C.FROM_CLIENT_TYPE_OF_KEEP$ALIVE:
	    	{
	    		// TODO: 以下代码仅用于开发调试时，建议即时注释掉！！
		        logger.debug("[IMCORE-{}]>> 【1收心跳{}】收到客户端{}的心跳包."
		        		, Gateway.$(session), OnlineProcessor.getUserIdFromChannel(session), remoteAddress);
	    		
		        if(!OnlineProcessor.isLogined(session))
	    		{
	    			LocalSendHelper.replyDataForUnlogined(session, pFromClient, null);
	    			return;
	    		}
	    		else
	    			logicProcessor.processKeepAlive(session, pFromClient, remoteAddress);
	
	    		break;
	    	}
	    	// 【MobileIMSDK框架层协议：用户登陆/连接IM服务器的请求】
	    	case ProtocalType.C.FROM_CLIENT_TYPE_OF_LOGIN:
	    	{
	    		logicProcessor.processLogin(session, pFromClient, remoteAddress);
	    		break;
	    	}
	    	// 【MobileIMSDK框架层协议：用户注销与IM服务器的连接】
	    	// 目前的逻辑是：用户退出登陆时不需要反馈给客户端
	    	case ProtocalType.C.FROM_CLIENT_TYPE_OF_LOGOUT:
	    	{
	    		logger.info("[IMCORE-{}]<< 收到客户端{}的退出登陆请求.", Gateway.$(session), remoteAddress);
	    		// 立即注销用户会话
	    		session.close();
	    		break;
	    	}
	    	// 【MobileIMSDK框架层协议：收到客户端发过来的ECHO指令（目前回显指令仅用于C2S时开发人员的网络测试，别无他用】
	    	case ProtocalType.C.FROM_CLIENT_TYPE_OF_ECHO:
	    	{
	    		pFromClient.setType(ProtocalType.S.FROM_SERVER_TYPE_OF_RESPONSE$ECHO);
	    		// 将客户端发过来的Protocal包原样返回（谓之ECHO哦）
	    		LocalSendHelper.sendData(session, pFromClient, null);
	    		break;
	    	}
	    	default:
	    	{
	    		logger.warn("[IMCORE-{}]【注意】收到的客户端{}消息类型：{}，但目前该类型服务端不支持解析和处理！"
	    				, Gateway.$(session), remoteAddress, pFromClient.getType());
	    		break;
	    	}
    	}
    }
    
    /**
     * 框架中，当用户的会话被关闭时将调本本方法。
     * <p>
     * 本方法中会将此用户从在线列表及相关队列中移除，将通过回调通知上层代码（由
     * 上层代码接力实现自定义的其它业务处理）。
     * <p>
     * <b>会话被关闭的可能性有3种：</b>
     * <ul>
     *   <li>1）当客户端显式地退出网络连接时（即正常退出时）；</li>
     *   <li>2）客户端非正常关闭，但服务端的会话超时到来时；</li>
     *   <li>3）与客户端的会话发生其它错误或异常时。</li>
     * </ul>
     * <p>
     * 本方法将被 {@link net.x52im.mobileimsdk.server.network.tcp.MBTCPClientInboundHandler#channelInactive(
     * io.netty.channel.ChannelHandlerContext)}调用，以便接受Netty中客户端“会话”断开通知。
     * 
     * @param session 被关闭的会话Channel引用
     * @throws Exception 任何错误发生时将抛出本异常
     * @see net.x52im.mobileimsdk.server.processor.OnlineProcessor#removeUser(int)
     * @see net.x52im.mobileimsdk.server.event.ServerEventListener#onUserLogoutAction_CallBack(int, Object)
     * @see net.x52im.mobileimsdk.server.network.tcp.MBTCPClientInboundHandler#channelInactive(io.netty.channel.ChannelHandlerContext)
     */
    public void sessionClosed(Channel session) throws Exception 
    {
    	// 取出在连接认证时放入会话中的user_id
    	String user_id = OnlineProcessor.getUserIdFromChannel(session);
    	
    	if(user_id != null)
    	{
	    	// 从在线列表中取出会话引用
	    	Channel sessionInOnlinelist = OnlineProcessor.getInstance().getOnlineSession(user_id);
	    	
	    	logger.info("[IMCORE-{}]{}的会话已关闭(user_id={}, firstLoginTime={})了..."
	    			, Gateway.$(session), ServerToolKits.clientInfoToString(session), user_id, OnlineProcessor.getFirstLoginTimeFromChannel(session));
	    	
	    	// TODO just for DEBUG：以下代码仅作Debug之用，您随时可删除之！
	    	{// DEBUG Start
	    		
	    		logger.info(".......... 【0】[当前正在被关闭的session] session.hashCode={}, session.ip+port={}"
	    				, session.hashCode(), session.remoteAddress());
	    		
	    		if(sessionInOnlinelist != null)
	    		{
	    			logger.info(".......... 【1】[处于在线列表中的session] session.hashCode={}, session.ip+port="
	    					, sessionInOnlinelist.hashCode(), sessionInOnlinelist.remoteAddress());
	    		}
	    	}// DEBUG END
    	
    		//## Bug FIX: 20171211 START
    		// [此bug的现象是]：客户端在某种几小几率下出现每隔21秒的周期性掉线问题。原因是它的当前会话在
    		// 它前一个被废弃的会话（可能是客户端网络原因）超时后错误地将当前会话从在线列表移除而导致
    		// 的。加上此判断后，意味着被close的会话要从在线列表中移除时必须保证移除的是该会话本身，这
    		// 样就排除了客户端在极端网络情况下发起的新会话不会因此废弃的会话超时时而把新会话错误地从在
    		// 线列表中移除的问题，从而解决此bug。
    		//
    		// [产生此bug的根本原因是]：在线列表中的key是user_id，而同一个user_id发起的新会话，在在线列表
    		// 中被放入时的逻辑时只覆盖之前的会话，而未close掉（之所以这么做是为了方便以后做多点登陆功能），
    		// 而且在线列表就这么实现的话，当前来说并没有什么问题。
    		if(sessionInOnlinelist != null && session != null && session == sessionInOnlinelist)
    		//## Bug FIX: 20171211 END
    		{
    			// 尽最大可能移除用户登陆成功后暂存到会话对象中的user_id
				OnlineProcessor.removeAttributesForChannel(session);
    			
    			// 从在线列表中移除
    			// 【理论上：】因为每个session只在用户登陆成功后才会放入列表中，那么每
    			//         一个存放在在线列表中的session肯定都对应了user_id。所以
    			//		    此处先取出session中之前存放的id再把这个session从在线列表中删除
    			//        的算法是可以保证session被关闭的同时肯定能同步将它从在线列表中移除，
    			//        从而保证在列表的准确性！
    			OnlineProcessor.getInstance().removeUser(user_id);

    			// 开始回调
    			if(serverEventListener != null)
    				// 通知回调：用户退出登陆了
    				serverEventListener.onUserLogout(user_id, null, session);
    			else
    				logger.debug("[IMCORE-{}]>> 会话{}被系统close了，但回调对象是null，没有进行回调通知."
    						, Gateway.$(session), ServerToolKits.clientInfoToString(session));
    		}
    		else
    		{
    			logger.warn("[IMCORE-{}]【2】【注意】会话{}不在在线列表中，意味着它是被客户端弃用/或被服务端强踢，本次忽略这条关闭事件即可！"
    					, Gateway.$(session), ServerToolKits.clientInfoToString(session));
    		}
    	}
    	else
    	{
    		logger.warn("[IMCORE-{}]【注意】会话{}被系统close了，但它里面没有存放user_id，它很可能是没有成功合法认证而被提前关闭，从而正常释放资源。"
    				, Gateway.$(session), ServerToolKits.clientInfoToString(session));
    	}
    }

    /**
     * 当与客户的会话建立时本方法回调用。
     * <p>
     * 默认情况下，本方法什么也没做。
     * <p>
     * 本方法将被 {@link net.x52im.mobileimsdk.server.network.tcp.MBTCPClientInboundHandler#channelActive(
     * io.netty.channel.ChannelHandlerContext)}调用，以便接受Netty中客户端“会话”建立通知。
     * 
     * @param session 建立的Netty"会话"对象引用
     * @see net.x52im.mobileimsdk.server.network.tcp.MBTCPClientInboundHandler#channelActive(io.netty.channel.ChannelHandlerContext)
     */
    public void sessionCreated(Channel session) throws Exception 
    {
    	logger.info("[IMCORE-{}]与{}的会话建立(channelActive)了...", Gateway.$(session), ServerToolKits.clientInfoToString(session));
    }

    public ServerEventListener getServerEventListener()
	{
		return serverEventListener;
	}
	void setServerEventListener(ServerEventListener serverEventListener)
	{
		this.serverEventListener = serverEventListener;
	}
	
	public MessageQoSEventListenerS2C getServerMessageQoSEventListener()
	{
		return serverMessageQoSEventListener;
	}

	void setServerMessageQoSEventListener(MessageQoSEventListenerS2C serverMessageQoSEventListener)
	{
		this.serverMessageQoSEventListener = serverMessageQoSEventListener;
	}

	/**
	 * 获得本类对应的BridgeProcessor对象引用。
	 * 
	 * @return BridgeProcessor对象引用
	 * @since 3.0
	 * @see net.x52im.mobileimsdk.server.processor.BridgeProcessor
	 */
	public BridgeProcessor getBridgeProcessor()
	{
		return bridgeProcessor;
	}
}
