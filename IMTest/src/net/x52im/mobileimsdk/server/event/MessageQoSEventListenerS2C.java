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
 * MessageQoSEventListenerS2C.java at 2021-6-29 10:24:09, code by Jack Jiang.
 */
package net.x52im.mobileimsdk.server.event;

import java.util.ArrayList;

import net.x52im.mobileimsdk.server.protocal.Protocal;

/**
 * MobileIMSDK的服务端QoS消息送达保证机制的事件监听器.
 * <p>
 * <b>当前QoS机制支持全部的C2C、C2S、S2C共3种消息交互场景下的消息送达质量保证：</b>
 * <ul>
 * <li>1) Client to Server(C2S)：即由某客户端主动发起，消息最终接收者是服务端，此模式下：重发由C保证、ACK应答由S发回；</li>
 * <li>2) Server to Client(S2C)：即由服务端主动发起，消息最终接收者是某客户端，此模式下：重发由S保证、ACK应答由C发回；</li>
 * <li>2) Client to Client(C2C)：即由客户端主动发起，消息最终接收者是另一客户端。此模式对于QoS机制来说，相当于C2S+S2C两程路径。</li>
 * </ul>
 * <p>
 * MobileIMSDK QoS机制的目标是：尽全力送达消息，即使无法送达也会通过回调告之应用层，尽最大可能
 * 避免因TCP协议在应用层不可靠性而发生消息黑洞情况（何为黑洞消息？即消息发出后发送方完全不知道到
 * 底送达了还是没有送达，而MobileIMSDK的QoS机制将会即时准确地告之发送方：“已送达”或者“没有送达”
 * ，没有第3种可能）。
 * <p>
 * TCP理论上能从底层保证数据的可靠性，但应用层的代码和场景中存在网络本身和网络之外的各种不可靠性，
 * MobileIMSDK中的QoS送达保证机制，将加强TCP的可靠性，确保消息，无法从哪一个层面和维度，都会给
 * 开发者提供两种结果：要么明确被送达（即收到ACK应答包，见
 *  {@link MessageQoSEventListenerS2C#messagesBeReceived(String)}）、要行明确未被送达（见
 *   {@link MessageQoSEventListenerS2C#messagesLost(ArrayList)}）。从理论上，保证消息的百分百送达率。
 * <p>
 * <b>一个有趣的问题：TCP协议为什么还需要消息送达保证机制？它不是可靠的吗？</b><br>
 * 是的，TCP是可靠的，但那是在底层协议这一层。但对于应用层来说，TCP并不能解决消息的百分百可靠性。<br>
 * 原因有可能是：
 * <pre>
 * 1）客户端意外崩溃导致TCP缓冲区消息丢失；
 * 2）网络拥堵，导致TCP反复重传并指数退避，导致长时间无法送达的也应在送达超时时间内被判定为无法送
 * 	     达（对于应用层来说tcp传的太慢，用户不可能等的了这么久，否则体验会很差）；
 * 3）中间路由故障，tcp本身是无法感知的，这种情况下tcp做传输重试也会出现2）中的情况，这也应算是事
 *    实上的无法送达；
 * 4）其它更多情况。</pre>
 * 
 * @author Jack Jiang(http://www.52im.net/thread-2792-1-1.html)
 * @version 1.0
 * @since 2.1
 */
public interface MessageQoSEventListenerS2C
{
	/**
	 * 消息未送达的回调事件通知.
	 * 
	 * @param lostMessages 由MobileIMSDK QoS算法判定出来的未送达消息列表（此列表
	 * 中的Protocal对象是原对象的clone（即原对象的深拷贝），请放心使用哦），应用层
	 * 可通过指纹特征码找到原消息并可以UI上将其标记为”发送失败“以便即时告之用户
	 */
	void messagesLost(ArrayList<Protocal> lostMessages);

	/**
	 * 消息已被对方收到的回调事件通知.
	 * <p>
	 * <b>目前，判定消息被对方收到是有两种可能：</b><br>
	 * 1) 对方确实是在线并且实时收到了；<br>
	 * 2) 对方不在线或者服务端转发过程中出错了，由服务端进行离线存储成功后的反馈
	 * （此种情况严格来讲不能算是“已被收到”，但对于应用层来说，离线存储了的消息
	 * 原则上就是已送达了的消息：因为用户下次登陆时肯定能通过HTTP协议取到）。
	 * 
	 * @param theFingerPrint 已被收到的消息的指纹特征码（唯一ID），应用层可据此ID
	 * 来找到原先已发生的消息并可在UI是将其标记为”已送达“或”已读“以便提升用户体验
	 */
	void messagesBeReceived(String theFingerPrint);
}
