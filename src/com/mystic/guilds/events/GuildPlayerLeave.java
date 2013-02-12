package com.mystic.guilds.events;

import com.mystic.guilds.objects.Guild;
import com.mystic.guilds.objects.GuildPlayer;

public class GuildPlayerLeave extends GuildEvent{
	Guild guild; /// Previous Guild
	GuildPlayer player;
	
	public GuildPlayerLeave(Guild guild, GuildPlayer gp){
		this.guild = guild;
		this.player = gp;
	}
	public Guild getGuild() {
		return guild;
	}
	public GuildPlayer getPlayer() {
		return player;
	}
}
