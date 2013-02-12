package com.mystic.guilds.events;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class GuildEvent extends Event{
	private static final HandlerList handlers = new HandlerList();

	/** Cancel the event */
	boolean cancelled = false;
	
	/** Message for sender, Msg for guild, Msg for server*/
	String msg, gmsg, smsg;

	public void callEvent(){
		Bukkit.getServer().getPluginManager().callEvent(this);
	}
	
	public HandlerList getHandlers() {
        return handlers;
    }
 
    public static HandlerList getHandlerList() {
        return handlers;
    }
    
    public void setCancelled(boolean cancel){
    	cancelled = cancel;
    }
	public boolean isCancelled() {
		return cancelled;
	}
	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}
	
    public String getGuildMsg() {
		return gmsg;
	}

	public void setGuildMsg(String gmsg) {
		this.gmsg = gmsg;
	}
	
	public void setServerMsg(String msg) {
		this.smsg = msg;
	}
	public String getServerMsg() {
		return smsg;
	}

}
