package com.ukefu.webim.util;

import java.util.Date;

import org.apache.commons.lang.StringUtils;

import com.ukefu.core.UKDataContext;
import com.ukefu.util.client.NettyClients;
import com.ukefu.webim.service.cache.CacheHelper;
import com.ukefu.webim.service.repository.AgentUserTaskRepository;
import com.ukefu.webim.service.repository.ChatMessageRepository;
import com.ukefu.webim.util.server.message.ChatMessage;
import com.ukefu.webim.web.model.AgentUser;
import com.ukefu.webim.web.model.AgentUserTask;
import com.ukefu.webim.web.model.MessageOutContent;

public class MessageUtils {
	/**
	 * 
	 * @param image
	 * @param userid
	 */
	public static ChatMessage uploadImage(String image ,String attachid, int size , String name  , String userid){
		return createMessage(image , size , name , UKDataContext.MediaTypeEnum.IMAGE.toString(), userid , attachid);
	}
	public static ChatMessage uploadImage(String image  , String attachid, int size , String name , String channel , String userid , String username , String appid , String orgi){
		return createMessage(image , size , name , channel , UKDataContext.MediaTypeEnum.IMAGE.toString(), userid , username, appid , orgi , attachid);
	}
	
	/**
	 * 
	 * @param image
	 * @param userid
	 */
	public static ChatMessage uploadFile(String url , int size , String name , String userid , String attachid){
		return createMessage(url , size , name , UKDataContext.MediaTypeEnum.FILE.toString(), userid , attachid);
	}
	public static ChatMessage uploadFile(String url , int size , String name, String channel , String userid , String username , String appid , String orgi , String attachid){
		return createMessage(url , size , name , channel , UKDataContext.MediaTypeEnum.FILE.toString(), userid , username, appid , orgi , attachid);
	}
	public static ChatMessage createMessage(String message , int length , String name , String msgtype , String userid , String attachid){
		AgentUser agentUser = (AgentUser) CacheHelper.getAgentUserCacheBean().getCacheObject(userid, UKDataContext.SYSTEM_ORGI);
		ChatMessage data = new ChatMessage() ;
		if(agentUser != null){
			data.setUserid(agentUser.getUserid());
			data.setUsername(agentUser.getUsername());
			
			data.setFilesize(length);
			data.setFilename(name);
			data.setAttachmentid(attachid);
			
			data.setTouser(agentUser.getAgentno());
			data.setAppid(agentUser.getAppid());
			data.setOrgi(agentUser.getOrgi());
			data.setMessage(message);
			
			data.setMsgtype(msgtype);
			
			data.setType(UKDataContext.MessageTypeEnum.MESSAGE.toString());
			createMessage(data, msgtype, userid);
		}
		return data ;
	}
	
	public static ChatMessage createMessage(String message , int length , String name ,String channel ,String msgtype , String userid , String username , String appid , String orgi , String attachid){
		ChatMessage data = new ChatMessage() ;
		if(!StringUtils.isBlank(userid)){
			data.setUserid(userid);
			data.setUsername(username);
			data.setTouser(userid);
			data.setAppid(appid);
			data.setOrgi(orgi);
			data.setChannel(channel);
			data.setMessage(message);
			
			data.setFilesize(length);
			data.setFilename(name);
			data.setAttachmentid(attachid);
			
			data.setMsgtype(msgtype);
			
			data.setType(UKDataContext.MessageTypeEnum.MESSAGE.toString());
			createAiMessage(data , appid , channel, UKDataContext.CallTypeEnum.IN.toString() , UKDataContext.AiItemType.USERINPUT.toString() , msgtype, data.getUserid());
		}
		return data ;
	}

	/**
	 * @Description:	 客户向坐席发送消息
	 * @Title:			createMessage  
	 * @param data		消息内容
	 * @param msgtype	消息类型
	 * @param userid	客户id   
	 * @return:			void    
	 * @throws： 
	 * @author：			 姜宝俊
	 * @version：		V0.1  
	 * @Date：			2017-10-19 下午4:33:11
	 */
	public static void createMessage(ChatMessage data , String msgtype , String userid){
		AgentUser agentUser = (AgentUser) CacheHelper.getAgentUserCacheBean().getCacheObject(userid, UKDataContext.SYSTEM_ORGI);
    	MessageOutContent outMessage = new MessageOutContent() ;
    	
    	outMessage.setMessage(data.getMessage());
    	outMessage.setFilename(data.getFilename());
    	outMessage.setFilesize(data.getFilesize());
    	
    	
    	outMessage.setMessageType(msgtype);
    	outMessage.setCalltype(UKDataContext.CallTypeEnum.IN.toString());
    	outMessage.setAgentUser(agentUser);
    	outMessage.setSnsAccount(null);
    	
    	MessageOutContent statusMessage = null ;
    	if(agentUser==null){
    		statusMessage = new MessageOutContent() ;
    		statusMessage.setMessage(data.getMessage());
    		statusMessage.setMessageType(UKDataContext.MessageTypeEnum.STATUS.toString());
    		statusMessage.setCalltype(UKDataContext.CallTypeEnum.OUT.toString());
    		statusMessage.setMessage("当前坐席全忙，请稍候");
    	}else{
    		data.setUserid(agentUser.getUserid());
    		data.setUsername(agentUser.getUsername());
    		data.setTouser(agentUser.getAgentno());
    		
    		data.setAgentuser(agentUser.getId());
    		
    		data.setAgentserviceid(agentUser.getAgentserviceid());
    		
    		data.setAppid(agentUser.getAppid());
    		data.setOrgi(agentUser.getOrgi());
    		
    		data.setMsgtype(msgtype);
    		
    		
    		data.setUsername(agentUser.getUsername());
    		data.setUsession(agentUser.getUserid());				//agentUser作为 session id
    		data.setContextid(agentUser.getContextid());
    		data.setCalltype(UKDataContext.CallTypeEnum.IN.toString());
    		if(!StringUtils.isBlank(agentUser.getAgentno())){
    			data.setTouser(agentUser.getAgentno());
    		}
    		data.setChannel(agentUser.getChannel());
    		
    		outMessage.setContextid(agentUser.getContextid());
    		outMessage.setFromUser(data.getUserid());
    		outMessage.setToUser(data.getTouser());
    		outMessage.setChannelMessage(data);
    		outMessage.setNickName(agentUser.getUsername());
    		outMessage.setCreatetime(data.getCreatetime());
    		
    		if(data.getType()!=null && data.getType().equals(UKDataContext.MessageTypeEnum.MESSAGE.toString())){
	    		AgentUserTaskRepository agentUserTaskRes = UKDataContext.getContext().getBean(AgentUserTaskRepository.class) ;
	    		AgentUserTask agentUserTask = agentUserTaskRes.getOne(agentUser.getId()) ;
	    		if(agentUserTask!=null){
	    			if(agentUserTask.getLastgetmessage() != null && agentUserTask.getLastmessage()!=null){
		    			data.setLastagentmsgtime(agentUserTask.getLastgetmessage());
		    			data.setLastmsgtime(agentUserTask.getLastmessage());
		    			data.setAgentreplyinterval((int)((System.currentTimeMillis() - agentUserTask.getLastgetmessage().getTime())/1000));	//坐席上次回复消息的间隔
		    			data.setAgentreplytime((int)((System.currentTimeMillis() - agentUserTask.getLastmessage().getTime())/1000));		//坐席回复消息花费时间
	    			}
	    			agentUserTask.setUserasks(agentUserTask.getUserasks()+1);	//总咨询记录数量
	    			agentUserTask.setAgentreplytime(agentUserTask.getAgentreplytime() + data.getAgentreplyinterval());	//总时长
	    			if(agentUserTask.getUserasks()>0){
	    				agentUserTask.setAvgreplytime(agentUserTask.getAgentreplytime() / agentUserTask.getUserasks());
	    			}
	    			
		    		agentUserTask.setLastmessage(new Date());
		    		agentUserTask.setWarnings("0");
		    		agentUserTask.setWarningtime(null);
		    		agentUserTask.setLastmsg(data.getMessage().length() > 100 ? data.getMessage().substring(0 , 100) : data.getMessage());
		    		agentUserTask.setTokenum(agentUserTask.getTokenum()+1);
		    		data.setTokenum(agentUserTask.getTokenum());
		    		agentUserTaskRes.save(agentUserTask) ;
	    		}
    		}
    		
    		/**
    		 * 保存消息
    		 */
    		if(UKDataContext.MessageTypeEnum.MESSAGE.toString().equals(data.getType())){
    			UKDataContext.getContext().getBean(ChatMessageRepository.class).save(data) ;
    		}
    	}
    	if(!StringUtils.isBlank(data.getUserid()) && UKDataContext.MessageTypeEnum.MESSAGE.toString().equals(data.getType())){
    		//回显自己发送的消息
    		NettyClients.getInstance().sendIMEventMessage(data.getUserid(), UKDataContext.MessageTypeEnum.MESSAGE.toString(), outMessage);
    		if(statusMessage!=null){
    			//发送客服（坐席）全忙的消息
    			NettyClients.getInstance().sendIMEventMessage(data.getUserid(), UKDataContext.MessageTypeEnum.STATUS.toString(), statusMessage);
    		}
    	}
    	if(agentUser!=null && !StringUtils.isBlank(agentUser.getAgentno())){
    		//将消息发送给 坐席
    		NettyClients.getInstance().sendAgentEventMessage(agentUser.getAgentno(), UKDataContext.MessageTypeEnum.MESSAGE.toString(), data);
    	}
	}
	
	public static MessageOutContent createAiMessage(ChatMessage data , String appid,String channel , String direction , String chatype, String msgtype , String userid){
    	MessageOutContent outMessage = new MessageOutContent() ;
    	
    	outMessage.setMessage(data.getMessage());
    	outMessage.setMessageType(msgtype);
    	outMessage.setCalltype(direction);
    	outMessage.setAgentUser(null);
    	outMessage.setSnsAccount(null);
    	
    	{
    		data.setUserid(userid);
    		data.setUsername(data.getUsername());
    		data.setTouser(userid);
    		
    		data.setAgentuser(userid);
    		
    		
    		data.setAgentserviceid(data.getContextid());
    		data.setChatype(chatype);
    		
    		data.setChannel(channel);
    		
    		data.setAppid(data.getAppid());
    		data.setOrgi(data.getOrgi());
    		
    		data.setMsgtype(msgtype);
    		
    		data.setUsername(data.getUsername());
    		data.setUsession(data.getUserid());				//agentUser作为 session id
    		data.setContextid(data.getContextid());
    		data.setCalltype(direction);
    		
    		outMessage.setContextid(data.getContextid());
    		outMessage.setFromUser(data.getUserid());
    		outMessage.setToUser(data.getTouser());
    		outMessage.setChannelMessage(data);
    		outMessage.setNickName(data.getUsername());
    		outMessage.setCreatetime(data.getCreatetime());
    		
    		data.setUpdatetime(System.currentTimeMillis());
    		
    		
    		/**
    		 * 保存消息
    		 */
    		if(UKDataContext.MessageTypeEnum.MESSAGE.toString().equals(data.getType())){
    			UKDataContext.getContext().getBean(ChatMessageRepository.class).save(data) ;
    		}
    	}
    	return outMessage ;
	}
	
}
