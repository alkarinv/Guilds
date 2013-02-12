package com.mystic.guilds.events;

import com.mystic.guilds.objects.Guild;

public class GuildDisband extends GuildEvent{
	Guild guild;
	
	public GuildDisband(Guild guild){
		this.guild = guild;
	}

	public Guild getGuild() {
		return guild;
	}
}
