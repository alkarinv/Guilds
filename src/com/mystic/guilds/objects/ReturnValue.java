package com.mystic.guilds.objects;

import com.mystic.guilds.events.GuildEvent;

public class ReturnValue {
	public static enum Value{
		True, False, Cancelled;
	}
	public Value val; 
	GuildEvent event;
	
	public ReturnValue(Value val){
		this.val = val;
	}
	public ReturnValue(Value val, GuildEvent event){
		this.val = val;
		this.event = event;
	}

	public Value getVal() {
		return val;
	}
	public void setVal(Value val) {
		this.val = val;
	}
	
	public String getMsg() {
		return event != null ? event.getMsg() : null;
	}

	public void setMsg(String message) {
		event.setMsg(message);
	}
	public String getGuildMsg() {
		return event != null ? event.getGuildMsg() : null;
	}
	public void setGuildMsg(String message) {
		this.event.setGuildMsg(message);
	}

	public String getServerMsg() {
		return event != null ? event.getServerMsg() : null;
	}
	public void setServerMsg(String message) {
		this.event.setServerMsg(message);
	}
}
