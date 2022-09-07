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
 * PKickoutInfo.java at 2021-6-29 10:24:09, code by Jack Jiang.
 */
package net.x52im.mobileimsdk.server.protocal.s;

/**
 * 向客户端发出的“被踢”指令包内容的DTO类。
 * 
 * @author Jack Jiang(http://www.52im.net/thread-2792-1-1.html)
 * @since 6.0
 */
public class PKickoutInfo
{
	/** 被踢原因编码：因重复登陆被踢 */
	public final static int KICKOUT_FOR_DUPLICATE_LOGIN = 1;
	/** 被踢原因编码：被管理员强行踢出 */
	public final static int KICKOUT_FOR_ADMIN = 2;
	
	/** 被踢原因编码 */
	protected int code = -1;
	/** 被踢原因描述 */
	protected String reason = null;
	
	/**
	 * 构造方法。
	 * 
	 * @param code 被踢原因编码（本参数不可为空）
	 * @param reason 被踢原因描述（本参数可为空） 
	 */
	public PKickoutInfo(int code, String reason)
	{
		this.code = code;
		this.reason = reason;
	}

	public int getCode()
	{
		return code;
	}

	public void setCode(int code)
	{
		this.code = code;
	}

	public String getReason()
	{
		return reason;
	}

	public void setReason(String reason)
	{
		this.reason = reason;
	}
}
