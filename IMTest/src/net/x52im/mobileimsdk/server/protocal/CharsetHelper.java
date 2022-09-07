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
 * CharsetHelper.java at 2021-6-29 10:24:09, code by Jack Jiang.
 */
package net.x52im.mobileimsdk.server.protocal;

import java.io.UnsupportedEncodingException;

/**
 * 数据交互的编解码实现类。
 * 
 * @author Jack Jiang(http://www.52im.net/thread-2792-1-1.html)
 * @version 1.0
 */
public class CharsetHelper
{
//	public static final CharsetDecoder decoder = (Charset.forName(CharsetHelper.DECODE_CHARSET)).newDecoder();
	
	/** 字符编码格式。默认utf-8。 */
	public final static String ENCODE_CHARSET = "UTF-8";
	/** 字符解码格式。默认utf-8。 */
	public final static String DECODE_CHARSET = "UTF-8";
	
	/**
	 * 将byte数据使用服务端设定的解码方式组织成字符串。
	 * 
	 * @param b byte数据
	 * @param len 从0开始的总长度
	 * @return 解码后的字符串结果
	 */
	public static String getString(byte[] b, int len)
	{
		try
		{
			return new String(b, 0 , len, DECODE_CHARSET);
		}
		// 如果是不支持的字符类型则按默认字符集进行解码
		catch (UnsupportedEncodingException e)
		{
			return new String(b, 0 , len);
		}
	}
	/**
	 * 将byte数据使用服务端设定的解码方式组织成字符串。
	 * 
	 * @param b byte数据
	 * @param start 起始索引
	 * @param len 从0开始的总长度
	 * @return 解码后的字符串结果
	 */
	public static String getString(byte[] b, int start,int len)
	{
		try
		{
			return new String(b, start , len, DECODE_CHARSET);
		}
		// 如果是不支持的字符类型则按默认字符集进行解码
		catch (UnsupportedEncodingException e)
		{
			return new String(b, start , len);
		}
	}
	
	/**
	 * 将字符串按设置的方式编码成byte数组。
	 * 
	 * @param str 字符串
	 * @return 编码后的byte数组结果
	 */
	public static byte[] getBytes(String str)
	{
		if(str != null)
		{
			try
			{
				return str.getBytes(ENCODE_CHARSET);
			}
			// 如果是不支持的字符类型则按默认字符集进行编码
			catch (UnsupportedEncodingException e)
			{
				return str.getBytes();
			}
		}
		else
			return new byte[0];
	}
}
