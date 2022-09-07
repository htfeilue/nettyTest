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
 * PLoginInfo.java at 2021-6-29 10:24:09, code by Jack Jiang.
 */
package net.x52im.mobileimsdk.server.protocal.c;

/**
 * 登陆信息DTO类.
 * 
 * @author Jack Jiang(http://www.52im.net/thread-2792-1-1.html)
 * @version 1.0
 * @since 3.0
 */
public class PLoginInfo
{
	/** 登陆时提交到服务端的准一身份id，可能是登陆用户名、任意不重复的id等，具体意义由业务层决定。*/
	protected String loginUserId = null;
	
	/** 登陆时提交到服务端用于身份鉴别和合法性检查的token，它可能是登陆密码、也可能是通过前置http单点登陆接口拿到的token等 ，具体意义由业务层决定。*/
	protected String loginToken = null;
	
	/** 用户登陆时要提交的额外信息。本字段目前为保留字段，供上层应用自行放置需要的内容。*/
	protected String extra = null;
	
	/** 
	 * 客户端首次登陆时间（此时间由服务端在客户端首次登陆时返回的登陆信息中提供，客户端后绪在
	 * 掉重连时带上本字段，以便服务端用于多端互踢判定逻辑中使用）。此值不设置则默认应置为0。
	 * <p>
	 * 此时间由服务端提供，且直到客户端主动登陆，此时间不会被更新或重置（重连时也不会重置）。
	 * <p>
	 * 此时间目前的唯一用途：用于多端登陆时互踢的依据，防止在客户端未收到服务端“踢出”指令的
	 * 情况下，再次自动重连过来（通过此时间就可以判断出此客户端登陆时间之后又有新的端登陆，从
	 * 而拒绝此次重连，防止后登陆的端被之前这个“老”的端在它的网络恢复后错误地挤出“新”登陆的）。
	 * <p>
	 * 本次互踢思路，请见我在此帖中的回复：<a href="http://www.52im.net/thread-2879-1-1.html">http://www.52im.net/thread-2879-1-1.html</a>
	 * 
	 * @since 6.0
	 */
	protected long firstLoginTime = 0;
	
	/**
	 * 构造方法。
	 * 
	 * @param loginUserId 传递过来的准一id，保证唯一就可以通信，可能是登陆用户名、也可能是任意不重复的id等，具体意义由业务层决定
	 * @param loginToken 用于身份鉴别和合法性检查的token，它可能是登陆密码，也可能是通过前置单点登陆接口拿到的token等，具体意义由业务层决定
	 */
	public PLoginInfo(String loginUserId, String loginToken)
	{
		this(loginUserId, loginToken, null);
	}
	
	/**
	 * 构造方法。
	 * 
	 * @param loginUserId 传递过来的准一id，保证唯一就可以通信，可能是登陆用户名、也可能是任意不重复的id等，具体意义由业务层决定
	 * @param loginToken 用于身份鉴别和合法性检查的token，它可能是登陆密码，也可能是通过前置单点登陆接口拿到的token等，具体意义由业务层决定
	 * @param extra 额外信息字符串。本字段目前为保留字段，供上层应用自行放置需要的内容
	 */
	public PLoginInfo(String loginUserId, String loginToken, String extra)
	{
		this.loginUserId = loginUserId;
		this.loginToken = loginToken;
		this.extra = extra;
	}
	
	
	
	/**
	 * 返回登陆时提交的准一id，保证唯一就可以通信，可能是登陆用户名、也可
	 * 能是任意不重复的id等，具体意义由业务层决定。
	 * 
	 * @return
	 */
	public String getLoginUserId()
	{
		return loginUserId;
	}

	/**
	 * 设置登陆时提交的准一id，保证唯一就可以通信，可能是登陆用户名、也可
	 * 能是任意不重复的id等，具体意义由业务层决定。
	 * 
	 * @param loginUserId
	 */
	public void setLoginUserId(String loginUserId)
	{
		this.loginUserId = loginUserId;
	}

	/**
	 * 返回登陆时提交的用于身份鉴别和合法性检查的token，它可能是登陆密码，也可能是
	 * 通过前置单点登陆接口拿到的token等，具体意义由业务层决定。
	 * 
	 * @return
	 */
	public String getLoginToken()
	{
		return loginToken;
	}

	/**
	 * 设置登陆时提交的用于身份鉴别和合法性检查的token，它可能是登陆密码，也可能是
	 * 通过前置单点登陆接口拿到的token等，具体意义由业务层决定。
	 * 
	 * @param loginToken
	 */
	public void setLoginToken(String loginToken)
	{
		this.loginToken = loginToken;
	}

	/**
	 * 返回额外信息字符串。本字段目前为保留字段，供上层应用自行放置需要的内容。
	 * 
	 * @return
	 * @since 2.1.6
	 */
	public String getExtra()
	{
		return extra;
	}
	
	/**
	 * 设置额外信息字符串。本字段目前为保留字段，供上层应用自行放置需要的内容。
	 * 
	 * @param extra
	 * @since 2.1.6
	 */
	public void setExtra(String extra)
	{
		this.extra = extra;
	}

	public long getFirstLoginTime()
	{
		return firstLoginTime;
	}

	public void setFirstLoginTime(long firstLoginTime)
	{
		this.firstLoginTime = firstLoginTime;
	}
	
	public static boolean isFirstLogin(long firstLoginTime)
	{
		return firstLoginTime <= 0;
	}
}
