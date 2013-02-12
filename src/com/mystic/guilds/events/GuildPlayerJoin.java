package com.mystic.guilds.events;

import com.mystic.guilds.objects.Guild;
import com.mystic.guilds.objects.GuildPlayer;

public class GuildPlayerJoin extends GuildEvent{
	Guild guild; /// Joined guild
	GuildPlayer player;
	public GuildPlayerJoin(Guild guild, GuildPlayer gp){
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
