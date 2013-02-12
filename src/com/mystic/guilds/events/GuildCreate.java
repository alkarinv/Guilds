package com.mystic.guilds.events;

import com.mystic.guilds.objects.Guild;

public class GuildCreate extends GuildEvent{
	Guild guild;
	
	public GuildCreate(Guild guild){
		this.guild = guild;
	}

	public Guild getGuild() {
		return guild;
	}
}
