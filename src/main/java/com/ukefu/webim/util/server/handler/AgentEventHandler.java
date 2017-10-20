package com.ukefu.webim.util.server.handler;

import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.annotation.OnConnect;
import com.corundumstudio.socketio.annotation.OnDisconnect;
import com.corundumstudio.socketio.annotation.OnEvent;
import com.ukefu.core.UKDataContext;
import com.ukefu.util.UKTools;
import com.ukefu.util.client.NettyClients;
import com.ukefu.webim.service.acd.ServiceQuene;
import com.ukefu.webim.service.cache.CacheHelper;
import com.ukefu.webim.service.repository.AgentUserRepository;
import com.ukefu.webim.service.repository.AgentUserTaskRepository;
import com.ukefu.webim.service.repository.ChatMessageRepository;
import com.ukefu.webim.util.OnlineUserUtils;
import com.ukefu.webim.util.router.OutMessageRouter;
import com.ukefu.webim.util.server.message.AgentServiceMessage;
import com.ukefu.webim.util.server.message.AgentStatusMessage;
import com.ukefu.webim.util.server.message.ChatMessage;
import com.ukefu.webim.web.model.AgentStatus;
import com.ukefu.webim.web.model.AgentUser;
import com.ukefu.webim.web.model.AgentUserTask;
import com.ukefu.webim.web.model.MessageOutContent;
 
/**
 * 坐席（客服）向客户发送消息监听类
 * @author 姜宝俊
 *
 */
public class AgentEventHandler 
{  
	protected SocketIOServer server;
	
    @Autowired  
    public AgentEventHandler(SocketIOServer server)   
    {  
    	this.server = server ;
    }  
    
    @OnConnect  
    public void onConnect(SocketIOClient client)  
    {  
    	String user = client.getHandshakeData().getSingleUrlParam("userid") ;
		if(!StringUtils.isBlank(user)){
			client.set("agentno", user);
			NettyClients.getInstance().putAgentEventClient(user, client);
		}
    }  
      
    //添加@OnDisconnect事件，客户端断开连接时调用，刷新客户端信息  
    @OnDisconnect  
    public void onDisconnect(SocketIOClient client)  
    {  
    	String user = client.getHandshakeData().getSingleUrlParam("userid") ;
		String orgi = client.getHandshakeData().getSingleUrlParam("orgi") ;
		if(!StringUtils.isBlank(user)){
			try {
				OnlineUserUtils.offline(user, orgi);
			} catch (Exception e) {
				e.printStackTrace();
			}
			NettyClients.getInstance().removeAgentEventClient(user , client.getSessionId().toString());
		}
    }  
      
    //消息接收入口，当接收到消息后，查找发送目标客户端，并且向该客户端发送消息，且给自己发送消息  
    @OnEvent(value = "service")  
    public void onEvent(SocketIOClient client, AckRequest request, AgentServiceMessage data)   
    {
    	
    }  
    
	//消息接收入口，当接收到消息后，查找发送目标客户端，并且向该客户端发送消息，且给自己发送消息  
    @OnEvent(value = "status")  
    public void onEvent(SocketIOClient client, AckRequest request, AgentStatusMessage data)   
    {
    	
    }
    
  //消息接收入口，当接收到消息后，查找发送目标客户端，并且向该客户端发送消息，且给自己发送消息  
    @OnEvent(value = "message")  
    public void onEvent(SocketIOClient client, AckRequest request, ChatMessage data)   
    {
    	String user = client.getHandshakeData().getSingleUrlParam("userid") ;
    	AgentUser agentUser = (AgentUser) CacheHelper.getAgentUserCacheBean().getCacheObject(data.getTouser(), data.getOrgi());
    	MessageOutContent outMessage = new MessageOutContent() ;
    	outMessage.setMessage(data.getMessage());
    	if(UKDataContext.MediaTypeEnum.COOPERATION.toString().equals(data.getMsgtype())){
    		outMessage.setMessageType(UKDataContext.MediaTypeEnum.COOPERATION.toString());
		}else{
			outMessage.setMessageType(UKDataContext.MediaTypeEnum.TEXT.toString());
		}
    	
    	outMessage.setAttachmentid(data.getAttachmentid());
    	
    	outMessage.setCalltype(UKDataContext.CallTypeEnum.OUT.toString());
    	outMessage.setAgentUser(agentUser);
    	outMessage.setSnsAccount(null);
    	AgentStatus agentStatus = (AgentStatus) CacheHelper.getAgentStatusCacheBean().getCacheObject(data.getUserid(), data.getOrgi()) ;
    	
    	if(agentUser == null){
    		agentUser = UKDataContext.getContext().getBean(AgentUserRepository.class).findByIdAndOrgi(data.getTouser() , data.getOrgi()) ;
    		try {
				ServiceQuene.serviceFinish(agentUser, data.getOrgi());
			} catch (Exception e) {
				e.printStackTrace();
			}
    	}
    	
    	if(agentUser!=null && user!=null && user.equals(agentUser.getAgentno())){
    		data.setId(UKTools.getUUID());
    		data.setContextid(agentUser.getContextid());
    		
    		data.setAgentserviceid(agentUser.getAgentserviceid());
    		data.setCreater(agentUser.getAgentno());
    		
    		if(UKDataContext.MediaTypeEnum.COOPERATION.toString().equals(data.getMsgtype())){
    			data.setMsgtype(UKDataContext.MediaTypeEnum.COOPERATION.toString());
    		}else{
    			data.setMsgtype(UKDataContext.MediaTypeEnum.TEXT.toString());
    		}
    		
    		data.setCalltype(UKDataContext.CallTypeEnum.OUT.toString());
    		if(!StringUtils.isBlank(agentUser.getAgentno())){
    			data.setTouser(agentUser.getUserid());
    		}
    		data.setChannel(agentUser.getChannel());
    		
    		data.setUsession(agentUser.getUserid());
    		
    		outMessage.setContextid(agentUser.getContextid());
    		outMessage.setFromUser(data.getUserid());
    		outMessage.setToUser(data.getTouser());
    		outMessage.setChannelMessage(data);
    		if(agentStatus!=null){
        		data.setUsername(agentStatus.getUsername());
        		outMessage.setNickName(agentStatus.getUsername());
        	}else{
        		outMessage.setNickName(data.getUsername());
        	}
    		outMessage.setCreatetime(data.getCreatetime());
    		
    		AgentUserTaskRepository agentUserTaskRes = UKDataContext.getContext().getBean(AgentUserTaskRepository.class) ;
    		AgentUserTask agentUserTask = agentUserTaskRes.getOne(agentUser.getId()) ;
    		
    		if(agentUserTask!=null){
	    		if(agentUserTask.getLastgetmessage() != null && agentUserTask.getLastmessage()!=null){
	    			data.setLastagentmsgtime(agentUserTask.getLastgetmessage());
	    			data.setLastmsgtime(agentUserTask.getLastmessage());
	    			data.setAgentreplyinterval((int)((System.currentTimeMillis() - agentUserTask.getLastgetmessage().getTime())/1000));	//坐席上次回复消息的间隔
	    			data.setAgentreplytime((int)((System.currentTimeMillis() - agentUserTask.getLastmessage().getTime())/1000));		//坐席回复消息花费时间
	    		}
	    		
	    		agentUserTask.setAgentreplys(agentUserTask.getAgentreplys()+1);	//总咨询记录数量
    			agentUserTask.setAgentreplyinterval(agentUserTask.getAgentreplyinterval() + data.getAgentreplyinterval());	//总时长
    			if(agentUserTask.getAgentreplys()>0){
    				agentUserTask.setAvgreplyinterval(agentUserTask.getAgentreplyinterval() / agentUserTask.getAgentreplys());
    			}
    			
	    		agentUserTask.setLastgetmessage(new Date());
	    		
	    		agentUserTask.setReptime(null);
	    		agentUserTask.setReptimes("0");
	    		
	    		agentUserTaskRes.save(agentUserTask) ;
    		}
    		
    		/**
    		 * 保存消息
    		 */
    		UKDataContext.getContext().getBean(ChatMessageRepository.class).save(data) ;

    		client.sendEvent(UKDataContext.MessageTypeEnum.MESSAGE.toString(), data);
    		
	    	if(!StringUtils.isBlank(data.getTouser())){
	    		OutMessageRouter router = null ; 
	    		router  = (OutMessageRouter) UKDataContext.getContext().getBean(agentUser.getChannel()) ;
	    		if(router!=null){
	    			router.handler(data.getTouser(), UKDataContext.MessageTypeEnum.MESSAGE.toString(), agentUser.getAppid(), outMessage);
	    		}
	    	}
    	}else if(user!=null && agentUser!=null && !user.equals(agentUser.getAgentno())){
    		client.sendEvent(UKDataContext.MessageTypeEnum.END.toString(), agentUser);
    	}
    }  
}  