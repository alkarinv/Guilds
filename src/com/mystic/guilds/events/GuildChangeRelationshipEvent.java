package com.mystic.guilds.events;

import com.mystic.guilds.objects.Guild;
import com.mystic.guilds.objects.Relationship;

public class GuildChangeRelationshipEvent extends GuildEvent{
	Guild g1;
	Guild g2;
	Relationship rel;
	
	public GuildChangeRelationshipEvent(Guild g1,Guild g2, Relationship rel){
		this.g1 = g1; this.g2 = g2;
		this.rel = rel;
	}

	public Guild getGuild1() {
		return g1;
	}
	public Guild getGuild2() {
		return g2;
	}
	public Relationship getRelationship(){
		return rel;
	}
}
