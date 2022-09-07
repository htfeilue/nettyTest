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
 * PLoginInfoResponse.java at 2021-6-29 10:24:09, code by Jack Jiang.
 */
package net.x52im.mobileimsdk.server.protocal.s;

/**
 * 登陆结果响应信息DTO类。
 * 
 * @author Jack Jiang(http://www.52im.net/thread-2792-1-1.html)
 * @version 1.0
 */
public class PLoginInfoResponse
{
	/** 错误码：0表示认证成功，否则是用户自定的错误码（该码应该是>1024的整数） */
	protected int code = 0;
	
	/** 
	 * 客户端首次登陆时间（此时间为形如“1624333553769”的Java时间戳），此值不设置则默认应置为0。
	 * <p>
	 * 此时间由服务端设置，且直到客户端主动登陆，此时间不会被更新或重置（重连时也不会重置）。
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
	
	public PLoginInfoResponse(int code, long firstLoginTime)
	{
		this.code = code;
		this.firstLoginTime = firstLoginTime;
	}

	public int getCode()
	{
		return code;
	}
	public void setCode(int code)
	{
		this.code = code;
	}

	public long getFirstLoginTime()
	{
		return firstLoginTime;
	}

	public void setFirstLoginTime(long firstLoginTime)
	{
		this.firstLoginTime = firstLoginTime;
	}
}
