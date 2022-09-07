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
 * OnlineProcessor.java at 2021-6-29 10:24:09, code by Jack Jiang.
 */
package net.x52im.mobileimsdk.server.processor;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.x52im.mobileimsdk.server.network.Gateway;
import net.x52im.mobileimsdk.server.protocal.s.PKickoutInfo;
import net.x52im.mobileimsdk.server.utils.LocalSendHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MibileIMSDK的服务端用户列表管理类。
 * <p>
 * 根据全局算法约定，当user_id=0时即表示是服务器。
 * <p>
 * <b>注意：</b>由于历史原因，MobileIMSDK中的“会话”当前等同于Netty中的“Channel”，你可以认为代码中提到这两个名词时指的是同一个东西 。
 * 
 * @author Jack Jiang(http://www.52im.net/thread-2792-1-1.html)
 * @version 3.1
 */
public class OnlineProcessor
{
	/** 用于用户会话（即Netty中的“Channel”）中存放user_id的属性key */
	public final static String USER_ID_ATTRIBUTE = "__user_id__";
	/** 用于用户会话（即Netty中的“Channel”）中存放首次登陆时间的属性key */
	public final static String FIRST_LOGIN_TIME_ATTRIBUTE = "__first_login_time__";
	
	/** 用于用户会话（即Netty中的“Channel”）中存取属性 {@link #USER_ID_ATTRIBUTE} 用的AttributeKey */
	public static final AttributeKey<String> USER_ID_ATTRIBUTE_ATTR = AttributeKey.newInstance(USER_ID_ATTRIBUTE);
	/** 用于用户会话（即Netty中的“Channel”）中存取属性 {@link #FIRST_LOGIN_TIME_ATTRIBUTE} 用的AttributeKey */
	public static final AttributeKey<Long> FIRST_LOGIN_TIME_ATTRIBUTE_ATTR = AttributeKey.newInstance(FIRST_LOGIN_TIME_ATTRIBUTE);
	
	public static boolean DEBUG = false;

	private static Logger logger = LoggerFactory.getLogger(OnlineProcessor.class); 
	private static OnlineProcessor instance = null;
	
	/** 用户在线列表：key=user_id、value=会话实例引用 */
	private ConcurrentMap<String, Channel> onlineSessions = new ConcurrentHashMap<String, Channel>();
	
	/**
	 * 为了简化API调用，本方法将以单例的形式存活。
	 * 
	 * @return 本类的全局单例
	 */
	public static OnlineProcessor getInstance()
	{
		if (instance == null) {
			synchronized (OnlineProcessor.class) {
				if (instance == null) {
					instance = new OnlineProcessor();
				}
			}
		}
		return instance;
	}
	
	private OnlineProcessor()
	{
	}
	
	/**
	 * 将用户放入在线列表。
	 * <p>
	 * 从 v6.0开始，本方法增加了同一账号多端登陆互踢的逻辑，且互踢逻辑考虑了复杂网络变动下的多端互踢判定方法，防止误判误踢）。
	 * 本次互踢思路，请见我在此帖中的回复：<a href="http://www.52im.net/thread-2879-1-1.html">http://www.52im.net/thread-2879-1-1.html</a>
	 * <p>
     * <b><font color="#ff0000">本方法由MobileIMSDK内部决定如何调用，不建议开发者调用此方法！</font></b>
	 * 
	 * @param user_id 用户的user_id
	 * @param firstLoginTime 用户的首次登陆时间（如果是首次登陆则此值是<=0，否则为标准java时间戳）
	 * @param newSession 该用户对应的 Netty Channel 对象
	 * @param loginName 用户登陆账号
	 * @return true表示该会话正常加入在线列表，否则未被加入在线列表（这种情况下的会话应该是已被踢出的会话(此次是重连时过来的)，具体原因请见本方法中的代码逻辑实现）
	 */
	public boolean putUser(String user_id, long firstLoginTime, Channel newSession)
	{
		boolean putOk = true;
		final Channel oldSession = onlineSessions.get(user_id);
		
		// 在线列表中已经存在“会话”了，进入会话踢出判定逻辑
		if(oldSession != null)
		{
			
			// 将要加入的新会话对象跟已存在于列表中的会话对象是同一个
			boolean isTheSame = (oldSession.compareTo(newSession) == 0);

			logger.debug("[IMCORE-{}]【注意】用户id={}已经在在线列表中了，session也是同一个吗？{}", Gateway.$(newSession), user_id, isTheSame);

			/************* 以下将展开同一账号重复登陆情况的处理逻辑 *************/

			// 不是同一个会话
			if(!isTheSame)
			{
				//** 新会话的“首次登陆时间”字段没有被设置（应该是真的首次登陆），无条件踢出“老会话”
				//  【场景模拟】：此种情况，是“新会话”正常登陆时，发现此前已在别的端登陆过，此种情况属于最典型的互踢场景
				if(firstLoginTime <= 0)
				{
					logger.debug("[IMCORE-{}]【注意】用户id={}提交过来的firstLoginTime未设置(值={}, 应该是真的首次登陆？！)，将无条件踢出前面的会话！"
							, Gateway.$(newSession), user_id, firstLoginTime);

					// 向"老"会话发出被踢指令
					sendKickoutDuplicateLogin(oldSession, user_id);	
					// 将"新"用户加入到在线列表中
					onlineSessions.put(user_id, newSession);
				}
				// 新会话的“首次登陆时间”字段已被设置，则进入登陆时间判定逻辑，用于区分出新会话是否是掉线重连这种情况
				else
				{
					long firstLoginTimeForOld = OnlineProcessor.getFirstLoginTimeFromChannel(oldSession);

					//** 新的“首次登陆时间”晚于列表中的“老的”（这是正常登陆），踢出老的即可
					//  【场景模拟】：此种情况比较罕见，即“老会话”因客户端断线时“新会话”恰好登陆，而“新会话”恰好又掉线时“老会话”的客户
					//               端网络恢复被服务端正常放入在线列表，稍后“新会话”的网络又恢复，此时就发生了本次逻辑中出现的情况了！
					if(firstLoginTime >= firstLoginTimeForOld)
					{
						logger.debug("[IMCORE-{}]【提示】用户id={}提交过来的firstLoginTime为{}、firstLoginTimeForOld为{}，新的“首次登陆时间”【晚于】列表中的“老的”、正常踢出老的即可！"
								, Gateway.$(newSession), user_id, firstLoginTime, firstLoginTimeForOld);

						// 向"老"会话发出被踢指令
						sendKickoutDuplicateLogin(oldSession, user_id);		
						// 将"新"用户加入到在线列表中
						onlineSessions.put(user_id, newSession);
					}
					//** 新的“首次登陆时间”早于列表中的“老的”（这是不正常的掉线重连），强行常踢出所谓的“新”的会话
					//  【场景模拟】：此种情况相对来说不罕见，即这个“新会话”实际上是之前已登陆的端但恰好因网络原因掉线，后来登陆的“老会话”
					//               就正常登陆并被服务端放入在线列表，等于之前这个会话因网络恢复而重连时，就发生了本次逻辑中出现的情况了！
					else
					{
						logger.debug("[IMCORE-{}]【注意】用户id={}提交过来的firstLoginTime为{}、firstLoginTimeForOld为{}，新的“首次登陆时间”【早于】列表中的“老的”，表示“新”的会话应该是未被正常通知的“已踢”会话，应再次向“新”会话发出被踢通知！！"
								, Gateway.$(newSession), user_id, firstLoginTime, firstLoginTimeForOld);

						// 向"新"会话发出被踢指令
						sendKickoutDuplicateLogin(newSession, user_id);	
						
						//*** "老会话"已在列表中，啥也不用干了。。
						
						// 该会话已被踢出，所以不会被加入在线列表
						putOk = false;
					}
				}
			}
			else
			{
				// 将用户加入到在线列表中（覆盖已在列表中的会话）
				onlineSessions.put(user_id, newSession);
			}
		}
		// 在线列表是空的，正常加入此会话（如您无需多端互踢逻辑，请仅保留本else分支持代码即可！）
		else
		{
			// 将用户加入到在线列表中
			onlineSessions.put(user_id, newSession);
		}

		__printOnline();// just for debug
		
		return putOk;
	}
	
	/**
	 * 向客户端发出"重复登陆被踢"指令。
	 * 
	 * @param sessionBeKick 被踢会话
	 * @param to_user_id 接收方
	 * @since 6.0
	 */
	private void sendKickoutDuplicateLogin(final Channel sessionBeKick, String to_user_id)
	{
		try{
			LocalSendHelper.sendKickout(sessionBeKick, to_user_id, PKickoutInfo.KICKOUT_FOR_DUPLICATE_LOGIN, null);
		}
		catch (Exception e){
			logger.warn("[IMCORE-"+Gateway.$(sessionBeKick)+"] sendKickoutDuplicate的过程中发生了异常：", e);
		}
	}
	
	/**
	 * 打印在线用户列。
	 * <p>
	 * 本方法仅应用于DEBUG时，当在线用户数量众多时，本方法会影响性能。
	 */
	public void __printOnline()
	{
		logger.debug("【@】当前在线用户共("+onlineSessions.size()+")人------------------->");
		if(DEBUG)
		{
			for(String key : onlineSessions.keySet())
				logger.debug("      > user_id="+key+",session="+onlineSessions.get(key).remoteAddress());
		}
	}
	
	/**
	 * 将用户从在线列表中移除.
	 * <p>
     * <b><font color="#ff0000">本方法由MobileIMSDK内部决定如
     * 何调用，不建议开发者调用此方法！</font></b>
     * 
	 * @param user_id 用户的user_id
	 * @return true表示已成功remove，否则表示没有此user_id对应的在线信息
	 */
	public boolean removeUser(String user_id)
	{
		synchronized(onlineSessions)
		{
			if(!onlineSessions.containsKey(user_id))
			{
				logger.warn("[IMCORE]！用户id={}不存在在线列表中，本次removeUser没有继续.", user_id);
				__printOnline();// just for debug
				return false;
			}
			else
				return (onlineSessions.remove(user_id) != null);
		}
	}
	
	/**
	 * 根据user_id获得该在线用户对应的 Netty UDP Channel 会话实例句柄。
	 * 
	 * @param user_id 用户的user_id
	 * @return 存在该在线用户则成功返回，否则返回null
	 */
	public Channel getOnlineSession(String user_id)
	{
//		logger.debug("======>user_id="+user_id+"在列表中吗？"+usersBySession.containsKey(user_id));
//		__printOnline();
		
		if(user_id == null)
		{
			logger.warn("[IMCORE][CAUTION] getOnlineSession时，作为key的user_id== null.");
			return null;
		}
		
		return onlineSessions.get(user_id);
	}
	
	/**
	 * 返回用户在线列表：key=user_id、value=会话实例引用。
	 * 
	 * @return 在线列表实例引用
	 */
	public ConcurrentMap<String, Channel> getOnlineSessions()
	{
		return onlineSessions;
	}

	//------------------------------------------------------------------ 实用方法
	/**
	 * 该用户会话是否是合法的（已登陆认证）。
	 * <p>
	 * 根据MINA的原理，任何请求都会建立会话，但会话是否是合法的，则需根据
	 * 存放于其会话中的登陆属性来校验（MobileIMSDK中，已经过登陆认证的会话
	 * ，会在其session中存放user_id，判断是否已设置user_id即可认定是否是
	 * 合法的会话）。
	 * 
	 * @param session 用户会话引用
	 * @return true表示已经成功登陆认证过，否则表示未登陆过（非法请求）
	 */
	public static boolean isLogined(Channel session)
	{
		return session != null && getUserIdFromChannel(session) != null;
	}
	
	/**
	 * 指定用户ID的用户是否在线。
	 * 
	 * @param userId 用户ID
	 * @return true表示该用户在线，否则不在线
	 * @since 3.0
	 */
	public static boolean isOnline(String userId)
	{
		return OnlineProcessor.getInstance().getOnlineSession(userId) != null;
	}
	
	/**
	 * 为指定的“会话”设置它对应的用户ID（后绪代码可据用户ID此判定此Channel的身份）。
	 * 
	 * @param session 用户会话引用
	 * @param userId 用户ID
	 * @since 6.0
	 */
	public static void setUserIdForChannel(Channel session, String userId)
	{
		session.attr(OnlineProcessor.USER_ID_ATTRIBUTE_ATTR).set(userId);
	}
	
	/**
	 * 为指定的“会话”设置首次登陆时间（后绪代码可据此时间更准确地处理多端互踢逻辑）。
	 * 
	 * @param session 用户会话引用
	 * @param firstLoginTime 首次登陆时间（“首次登陆”区分于断线重连，指的是第1次真正的“登陆认证”时）
	 * @since 6.0
	 */
	public static void setFirstLoginTimeForChannel(Channel session, long firstLoginTime)
	{
		session.attr(OnlineProcessor.FIRST_LOGIN_TIME_ATTRIBUTE_ATTR).set(firstLoginTime);
	}
	
	/**
	 * 尝试取出存放于用户会话中的user_id.
	 * <p>
	 * 通常只有已成功登陆验证后的用户会话中才会存放它对应的user_id.
	 * 
	 * @param session 用户会话引用
	 * @return 如果找到该属性则返回指定session的user_id，否则返回null
	 */
	public static String getUserIdFromChannel(Channel session)
	{
		return (session != null ? session.attr(USER_ID_ATTRIBUTE_ATTR).get() : null);
	}
	
	/**
	 * 尝试取出存放于用户会话中的首次登陆时间.
	 * 
	 * @param session 用户会话引用
	 * @return 如果找到该属性则返回正常值，否则返回-1
	 */
	public static long getFirstLoginTimeFromChannel(Channel session)
	{
		if(session != null){
			Long attr = session.attr(FIRST_LOGIN_TIME_ATTRIBUTE_ATTR).get();
			return attr != null ? attr : -1;
		}
		return -1;
	}
	
	/**
	 * 从指定的“会话”中清除之前保存在Channel中的Attribute属性（这一般用于会话注销等情况）。
	 * 
	 * @param session 用户会话引用
	 */
	
	public static void removeAttributesForChannel(Channel session)
	{
		session.attr(OnlineProcessor.USER_ID_ATTRIBUTE_ATTR).set(null);
		session.attr(OnlineProcessor.FIRST_LOGIN_TIME_ATTRIBUTE_ATTR).set(null);
	}
}
