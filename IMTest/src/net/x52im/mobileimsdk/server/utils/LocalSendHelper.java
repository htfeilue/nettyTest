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
 * LocalSendHelper.java at 2021-6-29 10:24:09, code by Jack Jiang.
 */
package net.x52im.mobileimsdk.server.utils;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import net.x52im.mobileimsdk.server.ServerCoreHandler;
import net.x52im.mobileimsdk.server.ServerLauncher;
import net.x52im.mobileimsdk.server.network.Gateway;
import net.x52im.mobileimsdk.server.network.GatewayUDP;
import net.x52im.mobileimsdk.server.network.MBObserver;
import net.x52im.mobileimsdk.server.processor.OnlineProcessor;
import net.x52im.mobileimsdk.server.protocal.ErrorCode;
import net.x52im.mobileimsdk.server.protocal.Protocal;
import net.x52im.mobileimsdk.server.protocal.ProtocalFactory;
import net.x52im.mobileimsdk.server.protocal.s.PKickoutInfo;
import net.x52im.mobileimsdk.server.qos.QoS4SendDaemonS2C;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 本类提供的是公开的数据发送方法（数据接收者仅限于本机在线用户）。
 * <p>
 * <b>注意：</b>如果未开通与Web版的互通（参见： {@link net.x52im.mobileimsdk.server.ServerLauncher#bridgeEnabled} ）
 * ，则服务端消息发送建议使用本类，否则请使用 {@link GlobalSendHelper} 。
 * 
 * @author Jack Jiang(http://www.52im.net/thread-2792-1-1.html)
 * @version 1.0
 * @since 3.1
 * @see net.x52im.mobileimsdk.server.ServerLauncher#bridgeEnabled
 */
public class LocalSendHelper
{
	private static Logger logger = LoggerFactory.getLogger(ServerCoreHandler.class);  
	
	/**
     * 向目标发送一条数据（默认QoS=true、typeu=-1、from_user_id="0"）。
     * <p>
     * <b>注：</b>用户id为"0"是MobileIMSDK框架中的保留值，表示是服务端。
     * <p>
     * 典型的消息发送代码示例，请见：{@link #sendData(Channel, Protocal, MBObserver)} 方法中的详细说明。
     * 
     * @param to_user_id 接收方的user_id
     * @param dataContent 发送的文本内容
     * @param resultObserver 因netty的异步化特征，发送数据在API层也是异步的，本参数用于获得数据发送的结果通
     * 知（这是与MINA的区别之一）。服务端为了获得高并发、高性能，失去传统网络编程同步调用时编码的便利也是在所
     * 难免(再也不是直接的函数返回值了)，开发者需适应之
     * @throws Exception 发送过程中出现错误则抛出本异常
     * @see #sendData(String, String, String, boolean, String, int, MBObserver)
     * @since 2.1.4
     */
	public static void sendData(String to_user_id, String dataContent, MBObserver resultObserver) throws Exception 
    {
    	sendData(to_user_id, dataContent, true, null, -1, resultObserver);
    }
	
	/**
     * 向目标发送一条数据（默认QoS=true、from_user_id="0"）。
     * <p>
     * <b>注：</b>用户id为"0"是MobileIMSDK框架中的保留值，表示是服务端。
     * <p>
     * 典型的消息发送代码示例，请见：{@link #sendData(Channel, Protocal, MBObserver)} 方法中的详细说明。
     * 
     * @param to_user_id 接收方的user_id
     * @param dataContent 发送的文本内容
     * @param typeu 应用层专用字段——用于应用层存放聊天、推送等场景下的消息类型。注意：此值为-1时表示未定义。
	 *              MobileIMSDK框架中，本字段为保留字段，不参与框架的核心算法，专留作应用层自行定义和使用。
     * @param resultObserver 因netty的异步化特征，发送数据在API层也是异步的，本参数用于获得数据发送的结果通
     * 知（这是与MINA的区别之一）。服务端为了获得高并发、高性能，失去传统网络编程同步调用时编码的便利也是在所
     * 难免(再也不是直接的函数返回值了)，开发者需适应之
     * @throws Exception 发送过程中出现错误则抛出本异常
     * @see #sendData(String, String, String, boolean, String, int, MBObserver)
     * @since 2.1.4
     */
	public static void sendData(String to_user_id, String dataContent, int typeu, MBObserver resultObserver) throws Exception 
    {
    	sendData(to_user_id, dataContent, true, null, typeu, resultObserver);
    }
	
	/**
     * 向目标发送一条数据（默认from_user_id="0"）。
     * <p>
     * <b>注：</b>用户id为"0"是MobileIMSDK框架中的保留值，表示是服务端。
     * <p>
     * 典型的消息发送代码示例，请见： {@link #sendData(Channel, Protocal, MBObserver)} 方法中的详细说明。
     * 
     * @param to_user_id 接收方的user_id
     * @param dataContent 发送的文本内容
     * @param QoS 是否需要QoS支持，true表示需要，否则不需要。当为true时系统将自动生成指纹码
     * @param typeu 应用层专用字段——用于应用层存放聊天、推送等场景下的消息类型。注意：此值为-1时表示未定义。
	 *              MobileIMSDK框架中，本字段为保留字段，不参与框架的核心算法，专留作应用层自行定义和使用。
     * @param resultObserver 因netty的异步化特征，发送数据在API层也是异步的，本参数用于获得数据发送的结果通
     * 知（这是与MINA的区别之一）。服务端为了获得高并发、高性能，失去传统网络编程同步调用时编码的便利也是在所
     * 难免(再也不是直接的函数返回值了)，开发者需适应之
     * @throws Exception 发送过程中出现错误则抛出本异常
     * @see #sendData(String, String, String, boolean, String, int, MBObserver)
     * @since 2.1.4
     */
	public static void sendData(String to_user_id, String dataContent
			, boolean QoS, int typeu, MBObserver resultObserver) throws Exception 
    {
    	sendData(to_user_id, dataContent, QoS, null, typeu, resultObserver);
    }
	
	/**
     * 向目标发送一条数据（typeu=-1、from_user_id="0"）。
     * <p>
     * <b>注：</b>用户id为"0"是MobileIMSDK框架中的保留值，表示是服务端。
     * <p>
     * 典型的消息发送代码示例，请见：{@link #sendData(Channel, Protocal, MBObserver)} 方法中的详细说明。
     * 
     * @param to_user_id 接收方的user_id
     * @param dataContent 发送的文本内容
     * @param QoS 是否需要QoS支持，true表示需要，否则不需要。当为false时fingerPrint字段值将无意义
	 * @param fingerPrint 消息指纹特征码，当QoS=true且本参数为null则表示由系统自动生成指纹码，否则使用本参数指明的指纹码
     * @param resultObserver 因netty的异步化特征，发送数据在API层也是异步的，本参数用于获得数据发送的结果通
     * 知（这是与MINA的区别之一）。服务端为了获得高并发、高性能，失去传统网络编程同步调用时编码的便利也是在所
     * 难免(再也不是直接的函数返回值了)，开发者需适应之
     * @throws Exception 发送过程中出现错误则抛出本异常
     * @see #sendData(String, String, String, boolean, String, int, MBObserver)
     * @since 2.1.4
     */
	public static void sendData(String to_user_id, String dataContent
			, boolean QoS, String fingerPrint, MBObserver resultObserver) throws Exception 
    {
    	sendData(to_user_id, dataContent, QoS, fingerPrint, -1, resultObserver);
    }
	
	/**
     * 向目标发送一条数据（默认from_user_id="0"）。
     * <p>
     * <b>注：</b>用户id为"0"是MobileIMSDK框架中的保留值，表示是服务端。
     * <p>
     * 典型的消息发送代码示例，请见： {@link {@link #sendData(Channel, Protocal, MBObserver)} 方法中的详细说明。
     * 
     * @param to_user_id 接收方的user_id
     * @param dataContent 发送的文本内容
     * @param QoS 是否需要QoS支持，true表示需要，否则不需要。当为false时fingerPrint字段值将无意义
	 * @param fingerPrint 消息指纹特征码，当QoS=true且本参数为null则表示由系统自动生成指纹码，否则使用本参数指明的指纹码
     * @param typeu 应用层专用字段——用于应用层存放聊天、推送等场景下的消息类型。注意：此值为-1时表示未定义。
	 *              MobileIMSDK框架中，本字段为保留字段，不参与框架的核心算法，专留作应用层自行定义和使用。
     * @param resultObserver 因netty的异步化特征，发送数据在API层也是异步的，本参数用于获得数据发送的结果通
     * 知（这是与MINA的区别之一）。服务端为了获得高并发、高性能，失去传统网络编程同步调用时编码的便利也是在所
     * 难免(再也不是直接的函数返回值了)，开发者需适应之
     * @throws Exception 发送过程中出现错误则抛出本异常
     * @see #sendData(Protocal, MBObserver)
     * @since 3.0
     */
	public static void sendData(String to_user_id, String dataContent
			, boolean QoS, String fingerPrint, int typeu, MBObserver resultObserver) throws Exception 
    {
		// 服务端发出的消息，from)user_id填“0”即可，user_id为“0”是保留给服务端的
    	sendData(ProtocalFactory.createCommonData(dataContent, "0", to_user_id, QoS, fingerPrint, typeu), resultObserver);
    }
    
	/**
	 * 向目标发送一条数据。
	 * <p>
     * <b>注：</b>用户id为"0"是MobileIMSDK框架中的保留值，表示是服务端。
     * <p>
     * 典型的消息发送代码示例，请见： {@link #sendData(Channel, Protocal, MBObserver)} 方法中的详细说明。
	 * 
	 * @param p 要发送的内容（此对象封装了发送方user_id、接收方user_id、消息内容等）
     * @param resultObserver 因netty的异步化特征，发送数据在API层也是异步的，本参数
     * 用于获得数据发送的结果通知（这是与MINA的区别之一）。服务端为了获得高并发、高性
     * 能，失去传统网络编程同步调用时编码的便利也是在所难免(再也不是直接的函数返回值了
     * )，开发者需适应之
	 * @throws Exception 发送过程中出现错误则抛出本异常
	 * @see #sendData(io.netty.channel.Channel, Protocal, MBObserver)
	 */
    public static void sendData(Protocal p, MBObserver resultObserver) throws Exception 
    {
    	if(p != null)
    	{
    		if(!"0".equals(p.getTo()))
    			sendData(OnlineProcessor.getInstance().getOnlineSession(p.getTo()), p, resultObserver);
    		else
    		{
    			logger.warn("[IMCORE]【注意】此Protocal对象中的接收方是服务器(user_id==0)（而此方法本来就是由Server调用，自已发自已不可能！），数据发送没有继续！"+p.toGsonString());

//    			return false;
    			// 通知观察者，数据发送失败
    			if(resultObserver != null)
    				resultObserver.update(false, null);
    		}
    	}
    	else
    	{
    		// 通知观察者，数据发送失败
    		if(resultObserver != null)
    			resultObserver.update(false, null);
//    		return false;
    	}
    }
    
    /**
     * 向目标发送一条数据。
     * 
     * <p>
     * <b>注：</b>用户id为"0"是MobileIMSDK框架中的保留值，表示是服务端。
     * 
     * <p>
	 * <b>举个例子：以下是一段典型的服务端消息/指令发送代码：</b>
	 * <pre style="border: 1px solid #eaeaea;background-color: #fff6ea;border-radius: 6px;">
	 * // 消息接收者的id（这个id由你自已定义，对于MobileIMSDK来说只要保证唯一性即可）
	 * String destinationUserId = "400069";
	 * 
	 * // 这是要发送的消息（"你好"是消息内容、“0”是消息发送者）
	 * final Protocal p = ProtocalFactory.createCommonData("你好", "0", destinationUserId, true, null, -1);
	 * 
	 * // 对方在线的情况下，才需要实时发送，否则走离线处理逻辑
	 * if(OnlineProcessor.isOnline(destinationUserId)) {
	 *     // netty是异步通知数据发送结果的
	 *     MBObserver＜Object＞ resultObserver = new MBObserver＜Object＞(){
	 *         public void update(boolean sucess, Object extraObj) {
	 *              if(sucess){
	 *                 // 你的消息/指令实时发送成功，不需要额外处理了
	 *              }
	 *              else{
	 *                // TODO: 你的消息/指令实时发送失败，在这里实现离线消息处理逻辑！
	 *              }
	 *          }
	 *      };
	 * 		
	 *      // 开始实时消息/指令的发送
	 *      LocalSendHelper.sendData(p, resultObserver);
	 *  }
	 *  else{
	 *      // TODO: 你的离线消息处理逻辑！
	 *  }
	 * </pre>
     * 
     * @param session 接收者的会话对象引用
     * @param p 要发送的内容（此对象封装了发送方user_id、接收方user_id、消息内容等）
     * @param resultObserver 因netty的异步化特征，发送数据在API层也是异步的，本参数
     * 用于获得数据发送的结果通知（这是与MINA的区别之一）。服务端为了获得高并发、高性
     * 能，失去传统网络编程同步调用时编码的便利也是在所难免(再也不是直接的函数返回值了
     * )，开发者需适应之
     * @throws Exception 发送过程中出现错误则抛出本异常
     * @see io.netty.channel.ChannelFuture#writeAndFlush(Object)
     * @see MBObserver
     */
    public static void sendData(final Channel session, final Protocal p, final MBObserver resultObserver) throws Exception 
    {
    	// 要发送的目标用户的session已经不存在了(也就是他不在线，因为只有在线的用户才有这个会话引用)
		if(session == null)
		{
			logger.info("[IMCORE-{}]toSession==null >> id={}的用户尝试发给客户端{}的消息：str={}因接收方的id已不在线，此次实时发送没有继续(此消息应考虑作离线处理哦)."
					, Gateway.$(session), p.getFrom(), p.getTo(), p.getDataContent());
		}
		else
		{
			// 要发送到的对方会话是正常状态(可以理解为对方正常在线)
			if(session.isActive())
			{
		    	if(p != null)
		    	{
		    		// 为消息报文打上服务端的发送时间戳（此时间戳可以辅助用于应用层的消息顺序处理逻辑中）
		    		if(ServerLauncher.serverTimestamp)
		    			p.setSm(Protocal.genServerTimestamp());
		    		
		    		Object to = null;
		    		
		    		// WebSocket通信时，收发的是TextWebSocketFrame帧
		    		if(Gateway.isWebSocketChannel(session)){
		    			final String res = p.toGsonString();
		    			to = new TextWebSocketFrame(res);
		    		}
		    		// 其它协议时，直接2进制发送
		    		else{
		    			final byte[] res = p.toBytes();
		    			to = Unpooled.copiedBuffer(res);
		    		}
		    		
		    		ChannelFuture cf = session.writeAndFlush(to);//.sync();
		    		
		    		// 通过异步监听来实现结果的判定：使用ChannelFutureListener是
		    		// netty的最优化方法，因为await()虽简单但它是一个阻塞的操作而且可能会发生死锁，
		    		// 而ChannelFutureListener会利于最佳的性能和资源的利用，因为它一点阻塞都没有
		    		cf.addListener(new ChannelFutureListener() {
		    			 // Perform post-closure operation
		    	         public void operationComplete(ChannelFuture future) 
		    	         {
		    	        	// The message has been written successfully
		 		    		if( future.isSuccess())
		 		    		{
//		 		    			logger.info("[IMCORE-tcp] >> 给客户端："+ServerToolKits.clientInfoToString(session)
//		 		    					+"的数据->"+p.toGsonString()+",已成功发出["+res.length+"].");
		 		    			
		 		    			//## 自MobileIMSDK 5.0开始，所有消息都统一由发送者到服务端进行QoS重发保障，取消了
		 		    			//## 老版本中c2c模式下，QoS重发保障由发出客户端跟接受客户直接处理（跳过了服务端），
		 		    			//## 从而简单了新版本中QoS的整体算法、统一了代逻辑、提升了极端网络情况下的体验。
		 		    			
		 		    			// 【【S2C模式下的QoS机制1/4步：将包加入到发送QoS队列中】】
		 		    			// 如果需要进行QoS质量保证，则把它放入质量保证队列中供处理(已在存在于列
		 		    			// 表中就不用再加了，已经存在则意味当前发送的这个是重传包哦)
		 		    			if(p.isQoS() && !QoS4SendDaemonS2C.getInstance().exist(p.getFp()))
		 		    				QoS4SendDaemonS2C.getInstance().put(p);
		 		    		}
		 		    		// The messsage couldn't be written out completely for some reason. (e.g. Connection is closed)
		 		    		else
		 		    		{
		 		    			logger.warn("[IMCORE-{}]给客户端：{}的数据->{},发送失败！(此消息应考虑作离线处理哦)."
		 		    					, Gateway.$(session), ServerToolKits.clientInfoToString(session), p.toGsonString());
		 		    		}
		 		    		
		 		    		// 通知观察者，数据发送结果
		 		    		if(resultObserver != null)
	 		    				resultObserver.update(future.isSuccess(), null);
		    	         }
		    	    });
		    		
		    		// ## Bug FIX: 20171226 by JS, 上述数据的发送结果直接通过ChannelFutureListener就能知道，
		    		//            如果此处不return，则会走到最后的resultObserver.update(false, null);，就会
		    		//            出现一个发送方法的结果回调先是失败（错误地走到下面去了），一个是成功（真正的listener结果）
		    		return;
		    		// ## Bug FIX: 20171226 by JS END
		    	}
		    	else
		    	{
		    		logger.warn("[IMCORE-{}]客户端id={}要发给客户端{}的实时消息：str={}没有继续(此消息应考虑作离线处理哦)."
							, Gateway.$(session), p.getFrom(), p.getTo(), p.getDataContent());
		    	}
			}
		}
		
		// 通知观察者，数据发送失败
		if(resultObserver != null)
			resultObserver.update(false, null);
    }
    
	/**
	 * 当服务端检测到用户尚未登陆（或登陆会话已失效时）由服务端回复给
	 * 客户端的消息。
	 * <p>
	 * <font color="red">本方法将由MobileIMSDK框架内部算法按需调用，目前不建议也不需要开发者调用。</font>
	 * 
	 * @param session 被回复的会话
	 * @param p 回复的数据包
     * @param resultObserver 因netty的异步化特征，发送数据在API层也是异步的，本参数
     * 用于获得数据发送的结果通知（这是与MINA的区别之一）。服务端为了获得高并发、高性
     * 能，失去传统网络编程同步调用时编码的便利也是在所难免(再也不是直接的函数返回值了
     * )，开发者需适应之
	 * @throws Exception 发生时出错将抛出本异常
	 * @see #sendData(io.netty.channel.Channel, Protocal, MBObserver)
	 */
	public static void replyDataForUnlogined(final Channel session, Protocal p, MBObserver resultObserver) throws Exception
	{
		logger.warn("[IMCORE-{}]>> 客户端{}尚未登陆，{}处理未继续."
				, Gateway.$(session), ServerToolKits.clientInfoToString(session), p.getDataContent());
		
		if(resultObserver == null)
		{
			// Netty的数据发送结果观察者：netty的数据发送结果是通过异步通知来实现的（这就
			// 是异步编程模型，跟Nodejs的promise、Androi里的RxJava、iOS的block道理一样）
			resultObserver = new MBObserver(){
				@Override
				public void update(boolean sendOK, Object extraObj)
				{
					logger.warn("[IMCORE-{}]>> 客户端{}未登陆，服务端反馈发送成功？{}（会话即将关闭）"
							, Gateway.$(session), ServerToolKits.clientInfoToString(session), sendOK);
					
					if(!GatewayUDP.isUDPChannel(session))
						session.close();
				}
			};
		}
		
		// 把“未登陆”错误信息反馈给客户端，并把本次发送的数据内容回给客户端！
		Protocal perror = ProtocalFactory.createPErrorResponse(
				// 将原Protocal包的JSON作为错误内容原样返回（给客户端）
				ErrorCode.ForS.RESPONSE_FOR_UNLOGIN, p.toGsonString(), "-1"); // 尚未登陆则user_id就不存在了,用-1表示吧，目前此情形下该参数无意义
		
		// 发送数据
		sendData(session, perror, resultObserver);
	}

	/**
	 * 服务端回复应答包（给客户端发送方）。前提是客户发送的消息包中的字段QoS=true，否则本方法什么也不做。
	 * <p>
	 * <b>何为应答包？</b><br>
	 * 应答包是MobileIMSDK框架中的QoS消息送达保证机制的一部分，即为了保证消息必达（QoS=true时），由客户端发出的
	 * 消息，只要到达服务端（c2c或c2s路径的消息），服务端就会返回ACK应答包，表示告诉客户端“消息已被收到”（实际上
	 * 这条消息的下半程，将由服务端来继续处理，但客户端就不需要具体知道后续的环节了。）
	 * <p>
	 * <font color="red">本方法将由MobileIMSDK框架内部算法按需调用，目前不建议也不需要开发者调用。</font>
	 * 
	 * @param session 被回复的会话
	 * @param pFromClient 客户端发过来的数据包，本方法将据此包中的from、to、fp属性进行回复
     * @param resultObserver 因netty的异步化特征，发送数据在API层也是异步的
	 * @throws Exception 当发送数据出错时将抛出本异常
	 * @see #sendData(io.netty.channel.Channel, Protocal, MBObserver)
	 */
	public static void replyRecievedBack(Channel session, Protocal pFromClient, MBObserver resultObserver) throws Exception
	{
		if(pFromClient.isQoS() && pFromClient.getFp() != null)
		{
			Protocal receivedBackP = ProtocalFactory.createRecivedBack(pFromClient.getTo(), pFromClient.getFrom(), pFromClient.getFp());
			sendData(session, receivedBackP, resultObserver);
		}
		else
		{
			logger.warn("[IMCORE-{}]收到{}发过来需要QoS的包，但它的指纹码却为null！无法发伪应答包哦！", Gateway.$(session), pFromClient.getFrom());
//			return false;
			
			// 将失败的结果通知观察者
			if(resultObserver != null)
				resultObserver.update(false, null);
		}
	}
	
	/**
	 * 向客户端发出被踢指令。<b>本方法可供开发者自行调用，可用于实现自定义的踢出逻辑</b>。
	 * 
	 * @param sessionBeKick 被踢会话
	 * @param to_user_id 接收方
	 * @param code 被踢原因编码（本参数不可为空），see {@link PKickoutInfo} 中的常量定义，自定义被踢原因请使用>100的值
	 * @param reason 被踢原因描述（本参数可为空） 
	 * @since 6.0
	 */
	public static void sendKickout(final Channel sessionBeKick, String to_user_id, int code, String reason) throws Exception
	{
		// Netty的数据发送结果观察者
		MBObserver sendResultObserver = new MBObserver(){
			@Override
			public void update(boolean sendOK, Object extraObj)
			{
				logger.warn("[IMCORE-{}]>> 客户端{}的被踢指令发送成功？{}（会话即将关闭）", Gateway.$(sessionBeKick), ServerToolKits.clientInfoToString(sessionBeKick), sendOK);
				if(!GatewayUDP.isUDPChannel(sessionBeKick))
					sessionBeKick.close();
			}
		};
		// 给用户发出“重复登陆被踢出”指令
		LocalSendHelper.sendData(sessionBeKick, ProtocalFactory.createPKickout(to_user_id, code, reason), sendResultObserver);
	}
}
