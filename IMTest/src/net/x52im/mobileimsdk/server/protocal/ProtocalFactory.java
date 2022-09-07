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
 * ProtocalFactory.java at 2021-6-29 10:24:09, code by Jack Jiang.
 */
package net.x52im.mobileimsdk.server.protocal;

import net.x52im.mobileimsdk.server.protocal.c.PKeepAlive;
import net.x52im.mobileimsdk.server.protocal.c.PLoginInfo;
import net.x52im.mobileimsdk.server.protocal.s.PErrorResponse;
import net.x52im.mobileimsdk.server.protocal.s.PKeepAliveResponse;
import net.x52im.mobileimsdk.server.protocal.s.PKickoutInfo;
import net.x52im.mobileimsdk.server.protocal.s.PLoginInfoResponse;

import com.google.gson.Gson;

/**
 * MibileIMSDK框架的协议工厂类。
 * <p>
 * 理论上这些协议都是框架内部要用到的，应用上层可以无需理解和理会之。
 * 
 * @author Jack Jiang(http://www.52im.net/thread-2792-1-1.html)
 * @version 1.0
 */
public class ProtocalFactory
{
	private static String create(Object c)
	{
		return new Gson().toJson(c);
	}
	
	/**
	 * 将JSON文本反射成Java对象。
	 * <p>
	 * <b>本方法主要由MobileIMSDK框架内部使用。</b>
	 * 
	 * @param fullProtocalJSONBytes 对象的json（byte数组组织形式）
	 * @param len byte数组有效数据长度
	 * @param clazz 要反射成的对象
	 * @return 反射完成的{@link Protocal}报文对象
	 * @see #parse(String, Class)
	 */
	public static <T> T parse(byte[] fullProtocalJSONBytes, int len, Class<T> clazz)
	{
		return parse(CharsetHelper.getString(fullProtocalJSONBytes, len), clazz);
	}
	
	/**
	 * 将JSON文本反射成Java对象。
	 * <p>
	 * <b>本方法主要由MobileIMSDK框架内部使用。</b>
	 * 
	 * @param dataContentOfProtocal 对象json文本
	 * @param clazz 要反射成的对象
	 * @return 反射完成的{@link Protocal}报文对象
	 * @see Gson#fromJson(String, Class)
	 */
	public static <T> T parse(String dataContentOfProtocal, Class<T> clazz)
	{
		return new Gson().fromJson(dataContentOfProtocal, clazz);
	}
	
	/**
	 * 将JSON文本反射成Java对象。
	 * 
	 * @param fullProtocalJSONBytes 对象的json（byte数组组织形式）
	 * @param len bye数组有效数据长度
	 * @return 反射完成的{@link Protocal}报文对象
	 * @see #parse(byte[], int, Class)
	 */
	public static Protocal parse(byte[] fullProtocalJSONBytes, int len)
	{
		return parse(fullProtocalJSONBytes, len, Protocal.class);
	}
	
	/**
	 * 创建响应客户端的心跳消息报文对象（该对象由服务端发出）.
	 * <p>
	 * <b>本方法主要由MobileIMSDK框架内部使用。</b>
	 * 
	 * @param to_user_id
	 * @return 新建的{@link Protocal}报文对象
	 */
	public static Protocal createPKeepAliveResponse(String to_user_id)
	{
		return new Protocal(ProtocalType.S.FROM_SERVER_TYPE_OF_RESPONSE$KEEP$ALIVE, create(new PKeepAliveResponse()), "0", to_user_id);
	}
	
	/**
	 * <b>本方法主要由MobileIMSDK框架内部使用。</b>
	 * 
	 * @param dataContentOfProtocal
	 * @return
	 */
	public static PKeepAliveResponse parsePKeepAliveResponse(String dataContentOfProtocal)
	{
		return parse(dataContentOfProtocal, PKeepAliveResponse.class);
	}
	
	/**
	 * 创建用户心跳包报文对象（该对象由客户端发出）.
	 * <p>
	 * <b>本方法主要由MobileIMSDK框架内部使用。</b>
	 * 
	 * @param from_user_id
	 * @return 新建的{@link Protocal}报文对象
	 */
	public static Protocal createPKeepAlive(String from_user_id)
	{
		return new Protocal(ProtocalType.C.FROM_CLIENT_TYPE_OF_KEEP$ALIVE, create(new PKeepAlive()), from_user_id, "0");
	}
	
	/**
	 * <b>本方法主要由MobileIMSDK框架内部使用。</b>
	 * 
	 * @param dataContentOfProtocal
	 * @return
	 */
	public static PKeepAlive parsePKeepAlive(String dataContentOfProtocal)
	{
		return parse(dataContentOfProtocal, PKeepAlive.class);
	}
	
	/**
	 * 创建错误响应消息报文对象（该对象由服务端发出）.
	 * <p>
	 * <b>本方法主要由MobileIMSDK框架内部使用。</b>
	 * 
	 * @param errorCode 错误码
	 * @param errorMsg 错误消息文本内容（本参数非必须的）
	 * @return 新建的{@link Protocal}报文对象
	 * @see ErrorCode
	 */
	public static Protocal createPErrorResponse(int errorCode, String errorMsg, String user_id)
	{
		return new Protocal(ProtocalType.S.FROM_SERVER_TYPE_OF_RESPONSE$FOR$ERROR, create(new PErrorResponse(errorCode, errorMsg)), "0", user_id);
	}
	
	/**
	 * 解析错误响应消息报文对象（该对象由客户端接收）.
	 * <p>
	 * <b>本方法主要由MobileIMSDK框架内部使用。</b>
	 * 
	 * @param dataContentOfProtocal
	 * @return
	 */
	public static PErrorResponse parsePErrorResponse(String dataContentOfProtocal)
	{
		return parse(dataContentOfProtocal, PErrorResponse.class);
	}
	
	/**
	 * 创建用户注消登陆消息报文对象（该对象由客户端发出）.
	 * <p>
	 * <b>本方法主要由MobileIMSDK框架内部使用。</b>
	 * 
	 * @param user_id
	 * @return 新建的Protocal报文对象
	 */
	public static Protocal createPLoginoutInfo(String user_id)
	{
		return new Protocal(ProtocalType.C.FROM_CLIENT_TYPE_OF_LOGOUT, null, user_id, "0");
	}
	
	/**
	 * 创建用户登陆消息报文对象（该对象由客户端发出）.
	 * <p>
	 * <b>本方法主要由MobileIMSDK框架内部使用。</b>
	 * 
	 * @param loginInfo 登陆信息对象，详见 {@link PLoginInfo}
	 * @return 新建的{@link Protocal}报文对象
	 */
	public static Protocal createPLoginInfo(PLoginInfo loginInfo)
	{
		// 因登陆额外处理丢包逻辑，所以此包也无需QoS支持。不能支持QoS的原因
		// 是：登陆时QoS机制都还没启用呢（只在登陆成功后启用），所以此处无需设置且设置了也没有用的哦
		return new Protocal(ProtocalType.C.FROM_CLIENT_TYPE_OF_LOGIN, create(loginInfo), loginInfo.getLoginUserId(), "0");
	}
	
	/**
	 * 解析用户登陆消息报文对象（该对象由服务端接收）.
	 * <p>
	 * <b>本方法主要由MobileIMSDK框架内部使用。</b>
	 * 
	 * @param dataContentOfProtocal
	 * @return
	 */
	public static PLoginInfo parsePLoginInfo(String dataContentOfProtocal)
	{
		return parse(dataContentOfProtocal, PLoginInfo.class);
	}
	
	/**
	 * 创建用户登陆响应消息报文对象（该对象由服务端发出）.
	 * <p>
	 * <b>本方法主要由MobileIMSDK框架内部使用。</b>
	 * 
	 * @param code 服务端返回的错误码
	 * @param firstLoginTime 客户端首次登陆时间（此时间由服务端在客户端首次登陆时返回的登陆信息中提供，客户端后绪在掉重连时带上本字段，以便服务端用于多端互踢判定逻辑中使用）。此值不设置则默认应置为0。
     * @param user_id 服务端返因的错误信息
	 * @return 新建的{@link Protocal}报文对象
	 */
	public static Protocal createPLoginInfoResponse(int code, long firstLoginTime, String user_id)
	{
		return new Protocal(ProtocalType.S.FROM_SERVER_TYPE_OF_RESPONSE$LOGIN
				, create(new PLoginInfoResponse(code, firstLoginTime))
				, "0"
				, user_id // changed -1 to user_id: modified by Jack Jiang 20150911 -> 目的是让登陆响应包能正常支持QoS机制
				
				// [1]【20210711日，让登陆响应包不再支持QoS机制】：
				// 目的是防止在多端互踢逻辑的情况下，重连情况下的被踢者已被服务端注销会话后，
				// 客户端才发回登陆响应ACK应答，导致服务端错误地向未被踢者发出已登陆者重复登
				// 陆响应的问题。
				// 注1：以上情况发生于UDP协议时，TCP和WebSocket中不会发生此情况。
				// 注2：登陆响应包在不支持QoS的情况下，如客户端在极端烂网中发生丢包，则依赖客
				//      户端应用层的“登陆超时重试机制”(Demo中已提供代码演示实现)，提示用户重
				//      试即可，体验上没有影响！
				, false
				, null
				
				// [2]【20210711日前，登陆响应包支持QoS机制的代码】：
//				, true
//				, Protocal.genFingerPrint()// add QoS support by Jack Jiang 20150911
				
				); 
	}
	
	/**
	 * 解析用户登陆响应消息报文对象（该对象由客户端接收）.
	 * <p>
	 * <b>本方法主要由MobileIMSDK框架内部使用。</b>
	 * 
	 * @param dataContentOfProtocal
	 * @return 
	 */
	public static PLoginInfoResponse parsePLoginInfoResponse(String dataContentOfProtocal)
	{
		return parse(dataContentOfProtocal, PLoginInfoResponse.class);
	}
	
	/**
	 * 通用消息的Protocal报文对象新建方法（typeu字段默认-1）。
	 * <p>
	 * <font color="#0000ff"><b>友情提示：</b></font>为了您能定义更优雅的IM协议，
	 * 建议优先使用typeu定义您的协议类型，而不是使用默认的-1。
	 * 
	 * @param dataContent 要发送的消息内容
	 * @param from_user_id 发送人的user_id
	 * @param to_user_id 接收人的user_id
	 * @param QoS 是否需要QoS支持，true表示需要，否则不需要
	 * @param fingerPrint 消息指纹特征码，为null则表示由系统自动生成指纹码，否则使用本参数指明的指纹码
	 * @return 新建的{@link Protocal}报文对象
	 */
	public static Protocal createCommonData(String dataContent, String from_user_id, String to_user_id, boolean QoS, String fingerPrint)
	{
		return createCommonData(dataContent, from_user_id, to_user_id, QoS, fingerPrint, -1);
	}
	
	/**
	 * 通用消息的Protocal报文对象新建方法。
	 * 
	 * @param dataContent 要发送的消息内容
	 * @param from_user_id 发送人的user_id
	 * @param to_user_id 接收人的user_id
	 * @param QoS 是否需要QoS支持，true表示需要，否则不需要
	 * @param fingerPrint 消息指纹特征码，为null则表示由系统自动生成指纹码，否则使用本参数指明的指纹码
	 * @param typeu 应用层专用字段——用于应用层存放聊天、推送等场景下的消息类型，不需要设置时请填-1即可
	 * @return 新建的{@link Protocal}报文对象
	 */
	public static Protocal createCommonData(String dataContent, String from_user_id, String to_user_id, boolean QoS, String fingerPrint, int typeu)
	{
		return new Protocal(ProtocalType.C.FROM_CLIENT_TYPE_OF_COMMON$DATA, dataContent, from_user_id, to_user_id, QoS, fingerPrint, typeu);
	}
	
	/**
	 * 客户端from_user_id向to_user_id发送一个QoS机制中需要的“收到消息应答包”(默认bridge标认为false).
	 * <p>
	 * <b>本方法主要由MobileIMSDK框架内部使用。</b>
	 * 
	 * @param from_user_id 发起方
	 * @param to_user_id 接收方
	 * @param recievedMessageFingerPrint 已收到的消息包指纹码
	 * @return 新建的{@link Protocal}报文对象
	 */
	public static Protocal createRecivedBack(String from_user_id, String to_user_id, String recievedMessageFingerPrint)
	{
		return createRecivedBack(from_user_id, to_user_id, recievedMessageFingerPrint, false);
	}
	
	/**
	 * 客户端from_user_id向to_user_id发送一个QoS机制中需要的“收到消息应答包”.
	 * <p>
	 * <b>本方法主要由MobileIMSDK框架内部使用。</b>
	 * 
	 * @param from_user_id 发起方
	 * @param to_user_id 接收方
	 * @param recievedMessageFingerPrint 已收到的消息包指纹码
	 * @param bridge true表示是跨机器的桥接消息ACK，否则是本机消息ACK，默认请填false
	 * @return 新建的{@link Protocal}报文对象
	 */
	public static Protocal createRecivedBack(String from_user_id, String to_user_id, String recievedMessageFingerPrint, boolean bridge)
	{
		Protocal p = new Protocal(ProtocalType.C.FROM_CLIENT_TYPE_OF_RECIVED, recievedMessageFingerPrint, from_user_id, to_user_id);// 该包当然不需要QoS支持！
		p.setBridge(bridge);
		return p;
	}
	
	/**
	 * 创建用户被踢包报文对象（该对象由服务端发出）.
	 * <p>
	 * <b>本方法主要由MobileIMSDK框架内部使用。</b>
	 * 
	 * @param to_user_id 接收方
	 * @param code 被踢原因编码（本参数不可为空），see {@link PKickoutInfo} 中的常量定义，自定义被踢原因请使用>100的值
	 * @param reason 被踢原因描述（本参数可为空） 
	 * @return 新建的{@link Protocal}报文对象
	 * @since 6.0
	 */
	public static Protocal createPKickout(String to_user_id, int code, String reason)
	{
		return new Protocal(ProtocalType.S.FROM_SERVER_TYPE_OF_KICKOUT, create(new PKickoutInfo(code, reason)), "0", to_user_id);
	}
	
	/**
	 * 解析用户被踢消息报文对象（该对象由客户端接收）.
	 * <p>
	 * <b>本方法主要由MobileIMSDK框架内部使用。</b>
	 * 
	 * @param dataContentOfProtocal
	 * @return 
	 */
	public static PKickoutInfo parsePKickoutInfo(String dataContentOfProtocal)
	{
		return parse(dataContentOfProtocal, PKickoutInfo.class);
	}
}
