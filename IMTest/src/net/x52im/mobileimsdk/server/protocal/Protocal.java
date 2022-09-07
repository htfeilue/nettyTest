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
 * Protocal.java at 2021-6-29 10:24:09, code by Jack Jiang.
 */
package net.x52im.mobileimsdk.server.protocal;

import java.util.UUID;

import net.x52im.mobileimsdk.server.ServerLauncher;

import com.google.gson.Gson;

/**
 * 协议报文对象.
 * 
 * @author Jack Jiang(http://www.52im.net/thread-2792-1-1.html)
 * @version 1.0
 * @see ProtocalType
 */
public class Protocal
{
	/** 
	 * 意义：是否来自跨服务器的消息，true表示是、否则不是。本字段是为跨服务器或集群准备的。<br>
	 * 默认：false
	 * 
	 * @since 3.0 */
	protected boolean bridge = false;
	
	/** 
	 * 意义：协议类型。<br>
     * 注意：本字段为框架专用字段，本字段的使用涉及IM核心层算法的表现，如无必要请避免应用层使用此字段。<br>
     * 补充：理论上应用层不参与本字段的定义，可将其视为透明，如需定义应用层的消息类型，请使用 {@link typeu} 字
     *      段并配合dataContent一起使用。<br>
     * 默认：0
	 * 
	 * @see ProtocalType */
	protected int type = 0;
	 /**
     * 意义：协议数据内容。<br>
     * 说明：本字段用于MobileIMSDK框架中时，可能会存放一些指令内容。当本字段用于应用层时，由用户自行
     *      定义和使用其内容。 */
	protected String dataContent = null;
	
	/**
     * 意义：消息发出方的id<br>
     * 说明：为“-1”表示未设定、为“0”表示来自Server。<br>
     * 默认："-1"
     * */
	protected String from = "-1";
	/**
     * 意义：消息接收方的id（当用户退出时，此值可不设置）<br>
     * 说明：为“-1”表示未设定、为“0”表示发给Server。<br>
     * 默认："-1"
     * */
	protected String to = "-1";
	
	/** 
	 * 意义：用于QoS消息包的质量保证时作为消息的指纹特征码（理论上全局唯一）。<br>
	 * 注意：本字段为框架专用字段，请勿用作其它用途。*/
	protected String fp = null;
	/** 
	 * 意义：true表示本包需要进行QoS质量保证，否则不需要.<br>
	 * 默认：false */
	protected boolean QoS = false;
	
	/**
     * 意义：应用层专用字段——用于应用层存放聊天、推送等场景下的消息类型。<br>
     * 注意：此值为-1时表示未定义。MobileIMSDK框架中，本字段为保留字段，不参与框架的核心算法，专留用应用
     *      层自行定义和使用。<br>
     * 默认：-1
     *
     * @since 3.0, at 20161021 */
	protected int typeu = -1;
	
	/** 本字段仅用于客户端QoS时：表示丢包重试次数 */
	protected transient int retryCount = 0;
	
//	/** 
//	 * 客户端发出此消息时的时间（此字段仅在 <code>{@link ClintCoreSDK#clientTimestamp} == true</code> 时被客户端自动设置）。
//	 * <p>
//	 * 此时间为形如“1624333553769”的Java时间戳，此值不设置则默认为-1。
//	 * <p>
//	 * <b>本段的用途</b>：<font color="red">本字段不参与MobileIMSDK核心框架的任何算法</font>，
//	 * 本字段的存与否与框架算法无任何关系，仅仅提供给应用层供开发者使用，开发者可以利用此字段来
//	 * 构造消息的排序逻辑等，别无他用。
//	 * 
//	 * @since 6.0 */
//	protected long cm = -1;
	
	/** 
	 * 服务端发出或转发此消息时的时间（此字段仅在 <code>{@link ServerLauncher#serverTimestamp} == true</code> 时被服务端自动设置）。
	 * <p>
	 * 此时间为形如“1624333553769”的Java时间戳，此值不设置则默认为-1。
	 * <p>
	 * <b>本段的用途</b>：<font color="red">本字段不参与MobileIMSDK核心框架的任何算法</font>，
	 * 本字段的存与否与框架算法无任何关系，仅仅提供给应用层供开发者使用，开发者可以利用此字段来
	 * 构造消息的排序逻辑等，别无他用。
	 * 
	 * @since 6.0 */
	protected long sm = -1;
	
	/**
	 * 构造方法（QoS标记默认为false、typeu字段默认为-1）。
	 * 
	 * @param type 协议类型
	 * @param dataContent 协议数据内容
	 * @param from 消息发出方的id（当用户登陆时，此值可不设置）
	 * @param to 消息接收方的id（当用户退出时，此值可不设备）
	 * @see #Protocal(int, String, String, String, int)
	 */
	public Protocal(int type, String dataContent, String from, String to)
	{
		this(type, dataContent, from, to, -1);
	}
	
	/**
	 * 构造方法（QoS标记默认为false）。
	 * 
	 * @param type 协议类型
	 * @param dataContent 协议数据内容
	 * @param from 消息发出方的id（当用户登陆时，此值可不设置）
	 * @param to 消息接收方的id（当用户退出时，此值可不设备）
	 * @param typeu 应用层专用字段——用于应用层存放聊天、推送等场景下的消息类型，不需要设置时请填-1即可
	 * @see #Protocal(int, String, String, String, boolean, String, int)
	 */
	public Protocal(int type, String dataContent, String from, String to, int typeu)
	{
		this(type, dataContent, from, to, false, null, typeu);
	}
	
	/**
	 * 构造方法（typeu字段默认为-1）。
	 * 
	 * @param type 协议类型
	 * @param dataContent 协议数据内容
	 * @param from 消息发出方的id（当用户登陆时，此值可不设置）
	 * @param to 消息接收方的id（当用户退出时，此值可不设置）
	 * @param QoS 是否需要QoS支持，true表示是，否则不需要
	 * @param fingerPrint 协议包的指纹特征码，当 QoS字段=true时且本字段为null时方法中
	 * 将自动生成指纹码否则使用本参数指定的指纹码
	 * @see #Protocal(int, String, String, String, boolean, String, int)
	 */
	public Protocal(int type, String dataContent, String from, String to
			, boolean QoS, String fingerPrint)
	{
		this(type, dataContent, from, to, QoS, fingerPrint, -1);
	}
	
	/**
	 * 构造方法。
	 * 
	 * @param type 协议类型
	 * @param dataContent 协议数据内容
	 * @param from 消息发出方的id（当用户登陆时，此值可不设置）
	 * @param to 消息接收方的id（当用户退出时，此值可不设置）
	 * @param QoS 是否需要QoS支持，true表示是，否则不需要
	 * @param fingerPrint 协议包的指纹特征码，当 QoS字段=true时且本字段为null时方法中
	 * 将自动生成指纹码否则使用本参数指定的指纹码
	 * @param typeu 应用层专用字段——用于应用层存放聊天、推送等场景下的消息类型，不需要设置时请填-1即可
	 */
	public Protocal(int type, String dataContent, String from, String to
			, boolean QoS, String fingerPrint, int typeu)
	{
		this.type = type;
		this.dataContent = dataContent;
		this.from = from;
		this.to = to;
		this.QoS = QoS;
		this.typeu = typeu;
		
		// 只有在需要QoS支持时才生成指纹，否则浪费数据传输流量
		// 目前一个包的指纹只在对象建立时创建哦
		if(QoS && fingerPrint == null)
			fp = Protocal.genFingerPrint();
		else
			fp = fingerPrint;
	}
	
	/**
	 * 返回协议类型.
	 * 
	 * @return
	 * @see ProtocalType
	 */
	public int getType()
	{
		return this.type;
	}
	/**
	 * 设置协议类型.
	 * 
	 * @param type 协议类型
	 * @see ProtocalType
	 */
	public void setType(int type)
	{
		this.type = type;
	}
	
	/**
	 * 返回协议数据内容。
	 * 
	 * @return
	 */
	public String getDataContent()
	{
		return this.dataContent;
	}
	/**
	 * 设置协议数据内容。
	 * 
	 * @param dataContent
	 */
	public void setDataContent(String dataContent)
	{
		this.dataContent = dataContent;
	}
	
	/**
	 * 返回消息发出方的id。
	 * 
	 * @return
	 */
	public String getFrom()
	{
		return this.from;
	}
	/**
	 * 设置消息发出方的id（当用户登陆时，此值可不设置）。
	 * 
	 * @param from
	 */
	public void setFrom(String from)
	{
		this.from = from;
	}

	/**
	 * 返回消息接收方的id。
	 * 
	 * @return
	 */
	public String getTo()
	{
		return this.to;
	}
	/**
	 * 消息接收方的id（当用户退出时，此值可不设置）。 
	 * 
	 * @param to
	 */
	public void setTo(String to)
	{
		this.to = to;
	}
	
	/**
	 * 返回QoS质量保证时的指统特征码.
	 * 
	 * @return
	 * @see Protocal#genFingerPrint()
	 */
	public String getFp()
	{
		return this.fp;
	}

	/**
	 * 本字段仅用于QoS时：表示丢包重试次数。
	 * <p>
	 * 本值是<code>transient</code>类型，对象序列化时将会忽略此字段。
	 * 
	 * @return
	 */
	public int getRetryCount()
	{
		return this.retryCount;
	}
	/**
	 * 本方法仅用于QoS时：选出包重试次数+1。
	 * <p>
	 * <b>本方法理论上由MobileIMSDK内部调用，应用层无需额外调用。</b>
	 */
	public void increaseRetryCount()
	{
		this.retryCount += 1;
	}
	
	/**
	 * 是否需QoS支持。
	 * 
	 * @return true表示是，否则不是
	 */
	public boolean isQoS()
	{
		return QoS;
	}
	public void setQoS(boolean qoS)
	{
		this.QoS = qoS;
	}
	
	/**
	 * 是否来自跨服务器的消息，true表示是、否则不是。本字段是为跨服务器或集群准备的。
	 * 默认：false。
	 * 
	 * @return
	 * @since 3.0
	 */
	public boolean isBridge()
	{
		return bridge;
	}
	public void setBridge(boolean bridge)
	{
		this.bridge = bridge;
	}
	
	/**
	 * 返回应用层专用字段——用于应用层存放聊天、推送等场景下的消息类型。
	 * <p>
     * 注意：此值为-1时表示未定义。MobileIMSDK框架中，本字段为保留字段，
     *      不参与框架的核心算法，专留用应用层自行定义和使用。
     * <p>
     * 默认：-1。
	 * 
	 * @return 任意应用层决定的整数
	 * @since 3.0
	 */
	public int getTypeu()
	{
		return typeu;
	}
	/**
	 * 设置应用层专用字段——用于应用层存放聊天、推送等场景下的消息类型。
	 * <p>
     * 注意：此值为-1时表示未定义。MobileIMSDK框架中，本字段为保留字段，
     *      不参与框架的核心算法，专留用应用层自行定义和使用。
     * <p>
     * 默认：-1。
	 * 
	 * @param typeu 任意应用层决定的整数，如不使用请填-1
	 * @since 3.0
	 */
	public void setTypeu(int typeu)
	{
		this.typeu = typeu;
	}

	public long getSm()
	{
		return sm;
	}

	public void setSm(long sm)
	{
		this.sm = sm;
	}

	/**
	 * 将本对象转换成JSON字符串.
	 * 
	 * @return
	 */
	public String toGsonString()
	{
		return new Gson().toJson(this);
	}
	
	/**
	 * 将本对象转换成JSON后再转换成byte数组.
	 * 
	 * @return
	 * @see #toGsonString()
	 */
	public byte[] toBytes()
	{
		return CharsetHelper.getBytes(toGsonString());
	}
	
	/**
	 * Protocal深度对象克隆方法实现。
	 * 
	 * @return 克隆完成后的新对象
	 */
	@Override
	public Object clone()
	{
		// 克隆一个Protocal对象（该对象已重置retryCount数值为0）
		Protocal cloneP = new Protocal(this.getType()
				, this.getDataContent(), this.getFrom(), this.getTo(), this.isQoS(), this.getFp());
		cloneP.setBridge(this.bridge); // since 3.0
		cloneP.setTypeu(this.typeu);   // since 3.0
//		cloneP.setCm(this.cm);         // since 6.0
		cloneP.setSm(this.sm);         // since 6.0
		return cloneP;
	}
	
	/**
	 * 返回服务端时间戳。
	 * 
	 * @return
	 */
	public static long genServerTimestamp()
	{
		return System.currentTimeMillis();
	}
	
	/**
	 * 返回QoS需要的消息包的指纹特征码.
	 * <p>
	 * <b>重要说明：</b>使用系统时间戳作为指纹码，则意味着只在Protocal生成的环境中可能唯一.
	 * 它存在重复的可能性有2种：
	 * <ul>
	 * 		<li>1) 比如在客户端生成时如果生成过快的话（时间戳最小单位是1毫秒，如1毫秒内生成
	 * 		多个指纹码），理论上是有重复可能性；</li>
	 * 		<li>2) 不同的客户端因为系统时间并不完全一致，理论上也是可能存在重复的，所以唯一性应是：好友+指纹码才对.</li>
	 * </ul>
	 * 
	 * <p>
	 * 目前使用的UUID能保证全局唯一，但它有36位长（加上分隔符32+4），在流量敏
	 * 感的场景下，建议可考虑适当优化此生成算法。
	 * 
	 * @return 指纹特征码实际上就是系统的当时时间戳
	 * @see UUID.randomUUID().toString()
	 */
	public static String genFingerPrint()
	{
		return UUID.randomUUID().toString();//System.currentTimeMillis();
	}
}
