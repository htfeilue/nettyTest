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
 * LogicProcessor.java at 2021-6-29 10:24:09, code by Jack Jiang.
 */
package net.x52im.mobileimsdk.server.processor;

import io.netty.channel.Channel;
import net.x52im.mobileimsdk.server.ServerCoreHandler;
import net.x52im.mobileimsdk.server.network.Gateway;
import net.x52im.mobileimsdk.server.network.GatewayUDP;
import net.x52im.mobileimsdk.server.network.MBObserver;
import net.x52im.mobileimsdk.server.protocal.Protocal;
import net.x52im.mobileimsdk.server.protocal.ProtocalFactory;
import net.x52im.mobileimsdk.server.protocal.c.PLoginInfo;
import net.x52im.mobileimsdk.server.qos.QoS4ReciveDaemonC2S;
import net.x52im.mobileimsdk.server.qos.QoS4SendDaemonS2C;
import net.x52im.mobileimsdk.server.utils.GlobalSendHelper;
import net.x52im.mobileimsdk.server.utils.LocalSendHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MobileIMSDK框架的IM消息逻辑处理器。
 * <p>
 * <font color="red">本类是MobileIMSDK的服务端消息处理逻辑代码集中类，由服务端算法自行
 * 调用，开发者原则上不需要关注本类中的任何方法，请勿随意调用！</font>
 * 
  * @author Jack Jiang(http://www.52im.net/thread-2792-1-1.html)
 * @version 1.0
 * @since 3.0
 */
public class LogicProcessor
{
	private static Logger logger = LoggerFactory.getLogger(LogicProcessor.class);  
	
	private ServerCoreHandler serverCoreHandler = null;

	public LogicProcessor(ServerCoreHandler serverCoreHandler)
	{
		this.serverCoreHandler = serverCoreHandler;
	}
	
	/**
	 * 处理C2C（client to client）类消息（即客户端发给客户端的普通聊天消息）。
	 * 
	 * @param bridgeProcessor
	 * @param session
	 * @param pFromClient
	 * @param remoteAddress
	 * @throws Exception
	 */
	public void processC2CMessage(BridgeProcessor bridgeProcessor,
			Channel session, Protocal pFromClient, String remoteAddress) throws Exception
	{
		GlobalSendHelper.sendDataC2C(bridgeProcessor, session, pFromClient, remoteAddress, this.serverCoreHandler);
	}
	
	/**
	 * 处理C2S（client to server）类消息（即客户端发给服务端的指点令类消息）。
	 * 
	 * @param session
	 * @param pFromClient
	 * @param remoteAddress
	 * @throws Exception
	 */
	public void processC2SMessage(Channel session, final Protocal pFromClient, String remoteAddress) throws Exception
	{
		// 客户端直发服务端（而不是发给另一客户端）的正常数据包则
		// 回一个C2S模式的质量保证模式下的应答包
		if(pFromClient.isQoS())// && processedOK)
		{
			boolean hasRecieved = QoS4ReciveDaemonC2S.getInstance().hasRecieved(pFromClient.getFp());
			
			// ## Bug FIX: 20170215 by jackjiang - 解决了不能触发回调onTransBuffer_CallBack的问题
			//------------------------------------------------------------------------------ [2]代码与[1]处相同的哦 S
			// 【【QoS机制2/4步：将收到的包存入QoS接收方暂存队列中（用于防QoS消息重复）】】
			// @see 客户端LocalUDPDataReciever中的第1/4和第4/4步相关处理
			QoS4ReciveDaemonC2S.getInstance().addRecieved(pFromClient);
			// 【【QoS机制3/4步：回应答包】】
			// @see 客户端LocalUDPDataReciever中的第1/4和第4/4步相关处理
			// 给发送者回一个“收到”应答包(发送C2S模式的应答包)
			LocalSendHelper.replyRecievedBack(session
					, pFromClient
					// Netty的数据发送结果观察者：netty的数据发送结果是通过异步通知来实现的（这就
					// 是异步编程模型，跟Nodejs的promise、Androi里的RxJava、iOS的block道理一样）
					, new MBObserver(){
						@Override
						public void update(boolean receivedBackSendSucess, Object extraObj)
						{
							if(receivedBackSendSucess)
								logger.debug("[IMCORE-本机QoS！]【QoS_应答_C2S】向"+pFromClient.getFrom()+"发送"+pFromClient.getFp()
										+"的应答包成功了,from="+pFromClient.getTo()+".");
						}
					}
			);
			//------------------------------------------------------------------------------ [2]代码与[1]处相同的哦 E
		
			// 已经存在于已接收列表中（及意味着可能是之前发给对方的应答包因网络或其它情况丢了，对方又因QoS机制重新发过来了）
			if(hasRecieved)
			{
				if(QoS4ReciveDaemonC2S.getInstance().isDebugable())
					logger.debug("[IMCORE-本机QoS！]【QoS机制】"+pFromClient.getFp()+"因已经存在于发送列表中，这是重复包，本次忽略通知业务处理层（只需要回复ACK就行了）！");
				
				return;
			}	
		}

		// 进入业务处理回调（processedOK返回值目前尚未有用到，目前作为保留参数以后试情况再行定义和使用）
		boolean processedOK = this.serverCoreHandler.getServerEventListener().onTransferMessage4C2S(pFromClient, session);
	}
	
	/**
	 * 处理来自客户端的各类ACK消息应答包。
	 * 
	 * @param pFromClient
	 * @param remoteAddress
	 * @throws Exception
	 */
	public void processACK(final Protocal pFromClient, final String remoteAddress) throws Exception
	{
		//## 自MobileIMSDK 5.0开始，所有由服务端直发（s2c模式）或转发（c2c模式）的消息，客户端发
		//## 回来的应答包只需要由服务端接收并处理，取消了之前c2c模式下，客户端的ACK应答包需要回给
		//## 客户端发送者（这个逻辑在极端情况下，会受发送端网络的影响，而导致体验不佳。）

		// 应答包的消息内容即为之前收到包的指纹id
		String theFingerPrint = pFromClient.getDataContent();
		logger.debug("[IMCORE-本机QoS！]【QoS机制_S2C】收到接收者"+pFromClient.getFrom()+"回过来的指纹为"+theFingerPrint+"的应答包.");

		// 将收到的应答事件通知事件处理者
		if(this.serverCoreHandler.getServerMessageQoSEventListener() != null)
			this.serverCoreHandler.getServerMessageQoSEventListener().messagesBeReceived(theFingerPrint);

		// 【【S2C模式下的QoS机制4/4步：收到应答包时将包从发送QoS队列中删除】】
		QoS4SendDaemonS2C.getInstance().remove(theFingerPrint);
	}
	
	/**
	 * 处理来自客户端的登陆请求。
	 * 
	 * @param session
	 * @param pFromClient
	 * @param remoteAddress
	 * @throws Exception
	 */
	public void processLogin(final Channel session, final Protocal pFromClient, final String remoteAddress) throws Exception
	{
		final PLoginInfo loginInfo = ProtocalFactory.parsePLoginInfo(pFromClient.getDataContent());
		logger.info("[IMCORE-{}]>> 客户端"+remoteAddress+"发过来的登陆信息内容是：uid={}、token={}、firstLoginTime={}"
				, Gateway.$(session), loginInfo.getLoginUserId(), loginInfo.getLoginToken(), loginInfo.getFirstLoginTime());
		
		//## Bug FIX: 20170603 by Jack Jiang
		//##          解决在某些极端情况下由于Java PC客户端程序的不合法数据提交而导致登陆数据处理流程发生异常。
		if(loginInfo == null || loginInfo.getLoginUserId() == null)
		{
			logger.warn("[IMCORE-{}]>> 收到客户端{}登陆信息，但loginInfo或loginInfo.getLoginUserId()是null，登陆无法继续[uid={}、token={}、firstLoginTime={}]！"
					, Gateway.$(session), remoteAddress, loginInfo, loginInfo.getLoginUserId(), loginInfo.getFirstLoginTime());
			
			if(!GatewayUDP.isUDPChannel(session))
				session.close();
			
			return;
		}
		
		// 开始回调
		if(serverCoreHandler.getServerEventListener() != null)
		{
//			final long firstLoginTimeFromClient = loginInfo.getFirstLoginTime();
//			final boolean firstLogin = PLoginInfo.isFirstLogin(firstLoginTimeFromClient);//(firstLoginTimeFromClient <= 0);
//			final long firstLoginTimeToClient = (firstLogin? System.currentTimeMillis() : firstLoginTimeFromClient);
			
			// ** 先检查看看该会话的用户是否已经登陆
			// 是否已经登陆（目前只要会话中存放有user_id就被认为该用户已经登陆：会话
			// 还在在线列表中即意味着与客户端的session是处活性状态，所以借user_id来
			// 判定在线更严谨也确实是合理的）
			boolean alreadyLogined = OnlineProcessor.isLogined(session);//(_try_user_id != -1);
			// 该会话对应的用户已经登陆：此种情况目前还是让它再次走登陆流程吧，测试期观察它会不会导致bug即可
			// 【理论上出现这种情况的可能是】：当用户在会话有效期内程序非正常退出（如崩溃等））后，
			//								又在很短的时间内再次登陆！
			if(alreadyLogined)
			{
				logger.debug("[IMCORE-{}]>> 【注意】客户端{}的会话正常且已经登陆过，而此时又重新登陆：uid={}、token={}、firstLoginTime={}"
        				, Gateway.$(session), remoteAddress, loginInfo.getLoginUserId(), loginInfo.getLoginToken(), loginInfo.getFirstLoginTime());
				
//				// Netty的数据发送结果观察者：netty的数据发送结果是通过异步通知来实现的（这就
//				// 是异步编程模型，跟Nodejs的promise、Androi里的RxJava、iOS的block道理一样）
//				MBObserver retObserver = new MBObserver(){
//					@Override
//					public void update(boolean _sendOK, Object extraObj)
//					{
//						if(_sendOK)
//						{
//							//----------------------------------------------------------------------- [1] 代码同[2] START
//							// 将用户信息放入到在线列表中（理论上：每一个存放在在线列表中的session都对应了user_id）
//							OnlineProcessor.getInstance().putUser(loginInfo.getLoginUserId(), firstLoginTimeFromClient, session);
//							// 将用户登陆成功后的id暂存到会话对象中备用
//							OnlineProcessor.setUserIdForChannel(session, loginInfo.getLoginUserId());
//							// 将用户登陆成功后的首次登陆时间暂存到会话对象中备用
//							OnlineProcessor.setFirstLoginTimeForChannel(session, firstLoginTimeToClient);
//							
//							// 重复登陆则至少回调：成功登陆了（保证通知给在线好友我的在线状态，之前基于性能考虑，想
//							// 让重复登陆就不用再通知好友了，但实际情况是因丢包等因素的存在，极端情况下好友可能永远
//							// 也收不到上线通知了，目前在没有质量保证的前提下，还是损失点性能至少保证正确性吧！）
//							serverCoreHandler.getServerEventListener().onUserLoginSucess(loginInfo.getLoginUserId(), loginInfo.getExtra(), session);
//							//----------------------------------------------------------------------- [1] 代码同[2] END
//						}
//						else
//						{
//							logger.warn("[IMCORE-{}]>> 发给客户端{}的登陆成功信息发送失败了！", Gateway.$(session), remoteAddress);
//						}
//					}
//				};
//				
//				// 【1】直接将登陆反馈信息回馈给客户端而不用再走完整的登陆流程（包括通知好友上线等），
//				// 之所以将登陆反馈信息返回的目的是让客户端即时更新上线状态，因为重复登陆的原因
//				// 可能是在于客户端之前确实是因某种原因短时断线了（而服务端的会话在如此短的时间内还没在
//				// 关闭），那么此登陆反馈消息的返回有助于纠正此时间段内可能的好友状态的更新（上、下线等）
//				// 因为此时间虽短，但理论上可以发生任何事情哦！
//				// 【2】为何不干脆再走一遍登陆流程呢？这样启不是在保证该用户登陆数据一致性
//				//      上更保险，而不是像现在这样直接利用上次登陆的数据（理论上如果客户端
//				//      在此时间段内改了loginName的话则就真的不一致了，理论上可能发生，现
//				//      现实不太可能，即使出现也无太大问题）。总的一句话，就是为了避免完整
//				//      登陆过程中需要产生的一些数据查询、网络交互等，从而在大并发的情况下
//				//      能尽可能地提升性能
//				LocalSendHelper.sendData(session, ProtocalFactory.createPLoginInfoResponse(0, firstLoginTimeToClient, loginInfo.getLoginUserId()), retObserver);
			
				// 【1】直接将登陆反馈信息回馈给客户端而不用再走完整的登陆流程（包括通知好友上线等），
				// 之所以将登陆反馈信息返回的目的是让客户端即时更新上线状态，因为重复登陆的原因
				// 可能是在于客户端之前确实是因某种原因短时断线了（而服务端的会话在如此短的时间内还没在
				// 关闭），那么此登陆反馈消息的返回有助于纠正此时间段内可能的好友状态的更新（上、下线等）
				// 因为此时间虽短，但理论上可以发生任何事情哦！
				// 【2】为何不干脆再走一遍登陆流程呢？这样启不是在保证该用户登陆数据一致性
				//      上更保险，而不是像现在这样直接利用上次登陆的数据（理论上如果客户端
				//      在此时间段内改了loginName的话则就真的不一致了，理论上可能发生，现
				//      现实不太可能，即使出现也无太大问题）。总的一句话，就是为了避免完整
				//      登陆过程中需要产生的一些数据查询、网络交互等，从而在大并发的情况下
				//      能尽可能地提升性能
				processLoginSucessSend(session, loginInfo, remoteAddress);
			}
			// 新登陆的用户
			else
			{
				int code = serverCoreHandler.getServerEventListener().onUserLoginVerify(
						loginInfo.getLoginUserId(), loginInfo.getLoginToken(), loginInfo.getExtra(), session);
				// 登陆验证成功
				if(code == 0)
				{
//					final long firstLoginTimeFromClient = loginInfo.getFirstLoginTime();
//					final boolean firstLogin = (firstLoginTimeFromClient <= 0);
//					final long firstLoginTimeToClient = (firstLogin? System.currentTimeMillis() : firstLoginTimeFromClient);
					
//					// Netty的数据发送结果观察者：netty的数据发送结果是通过异步通知来实现的（这就
//					// 是异步编程模型，跟Nodejs的promise、Androi里的RxJava、iOS的block道理一样）
//					MBObserver sendResultObserver = new MBObserver(){
//						@Override
//						public void update(boolean __sendOK, Object extraObj)
//						{
//							if(__sendOK)
//							{
//								//----------------------------------------------------------------------- [2] 代码同[1] START
//								// 将用户信息放入到在线列表中（理论上：每一个存放在在线列表中的session都对应了user_id）
//								OnlineProcessor.getInstance().putUser(loginInfo.getLoginUserId(), firstLoginTimeFromClient, session);
//								// 将用户登陆成功后的id暂存到会话对象中备用
//								OnlineProcessor.setUserIdForChannel(session, loginInfo.getLoginUserId());
//								// 将用户登陆成功后的首次登陆时间暂存到会话对象中备用
//								OnlineProcessor.setFirstLoginTimeForChannel(session, firstLoginTimeToClient);
//
//								// 回调：成功登陆了
//								serverCoreHandler.getServerEventListener().onUserLoginSucess(loginInfo.getLoginUserId(), loginInfo.getExtra(), session);
//								//----------------------------------------------------------------------- [2] 代码同[1] START
//							}
//							else
//								logger.warn("[IMCORE-{}]>> 发给客户端{}的登陆成功信息发送失败了【no】！", Gateway.$(session), remoteAddress);
//							
//						}
//					};
//					// 将登陆反馈信息回馈给客户端
//					LocalSendHelper.sendData(session, ProtocalFactory.createPLoginInfoResponse(code, firstLoginTimeToClient, loginInfo.getLoginUserId()), sendResultObserver);
				
					// 将登陆反馈信息回馈给客户端
					processLoginSucessSend(session, loginInfo, remoteAddress);
				}
				// 登陆验证失败！
				else
				{
					logger.warn("[IMCORE-{}]>> 客户端{}登陆失败【no】，马上返回失败信息，并关闭其会话。。。", Gateway.$(session), remoteAddress);
					
					// Netty的数据发送结果观察者：netty的数据发送结果是通过异步通知来实现的（这就
					// 是异步编程模型，跟Nodejs的promise、Androi里的RxJava、iOS的block道理一样）
					MBObserver sendResultObserver = new MBObserver(){
						@Override
						public void update(boolean sendOK, Object extraObj)
						{
							logger.warn("[IMCORE-{}]>> 客户端{}登陆失败信息返回成功？{}（会话即将关闭）", Gateway.$(session), remoteAddress, sendOK);
							session.close();
						}
					};
					
					// 将登陆错误信息回馈给客户端
					LocalSendHelper.sendData(session, ProtocalFactory.createPLoginInfoResponse(code, -1, "-1"), GatewayUDP.isUDPChannel(session)?null:sendResultObserver);
				}
			}
		}
		else
		{
			logger.warn("[IMCORE-{}]>> 收到客户端{}登陆信息，但回调对象是null，没有进行回调.", Gateway.$(session), remoteAddress);
		}
	}
	
	private void processLoginSucessSend(final Channel session, final PLoginInfo loginInfo, final String remoteAddress) throws Exception
	{
		final long firstLoginTimeFromClient = loginInfo.getFirstLoginTime();
		final boolean firstLogin = PLoginInfo.isFirstLogin(firstLoginTimeFromClient);//(firstLoginTimeFromClient <= 0);
		final long firstLoginTimeToClient = (firstLogin? System.currentTimeMillis() : firstLoginTimeFromClient);
		
		// Netty的数据发送结果观察者：netty的数据发送结果是通过异步通知来实现的（这就
		// 是异步编程模型，跟Nodejs的promise、Androi里的RxJava、iOS的block道理一样）
		MBObserver sendResultObserver = new MBObserver(){
			@Override
			public void update(boolean __sendOK, Object extraObj)
			{
				if(__sendOK)
				{
					// 将用户信息放入到在线列表中（理论上：每一个存放在在线列表中的session都对应了user_id）
					boolean putOK = OnlineProcessor.getInstance().putUser(loginInfo.getLoginUserId(), firstLoginTimeFromClient, session);
					
					// 如果该会话已正式放入在线列表成功，则走正常的用户上线处理逻辑
					if(putOK)
					{
						// 将用户登陆成功后的id暂存到会话对象中备用
						OnlineProcessor.setUserIdForChannel(session, loginInfo.getLoginUserId());
						// 将用户登陆成功后的首次登陆时间暂存到会话对象中备用
						OnlineProcessor.setFirstLoginTimeForChannel(session, firstLoginTimeToClient);
	
						// 回调：成功登陆了
						//-------------------------------------------------------------------------------
						// 【对于重复登陆请求】：
						// 重复登陆则至少回调：成功登陆了（保证通知给在线好友我的在线状态，之前基于性能考虑，想
						// 让重复登陆就不用再通知好友了，但实际情况是因丢包等因素的存在，极端情况下好友可能永远
						// 也收不到上线通知了，目前在没有质量保证的前提下，还是损失点性能至少保证正确性吧！）
						serverCoreHandler.getServerEventListener().onUserLoginSucess(loginInfo.getLoginUserId(), loginInfo.getExtra(), session);
					}
				}
				else
					logger.warn("[IMCORE-{}]>> 发给客户端{}的登陆成功信息发送失败了【no】！", Gateway.$(session), remoteAddress);
				
			}
		};
		// 将登陆反馈信息回馈给客户端
		LocalSendHelper.sendData(session, ProtocalFactory.createPLoginInfoResponse(0, firstLoginTimeToClient, loginInfo.getLoginUserId()), sendResultObserver);
	}

	/**
	 * 处理来自客户端的心跳包。
	 * 
	 * @param session
	 * @param pFromClient
	 * @param remoteAddress
	 * @throws Exception
	 */
	public void processKeepAlive(Channel session, Protocal pFromClient, String remoteAddress) throws Exception
	{
		String userId = OnlineProcessor.getUserIdFromChannel(session);
		if(userId != null)
		{
			// 给用户发回心跳响应包
			LocalSendHelper.sendData(ProtocalFactory.createPKeepAliveResponse(userId), null);
		}
		else
		{
			logger.warn("[IMCORE-{}]>> Server在回客户端{}的响应包时，调用getUserIdFromSession返回null，用户在这一瞬间掉线了？！", Gateway.$(session), remoteAddress);
		}
	}
}
