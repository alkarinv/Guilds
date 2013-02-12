package com.mystic.guilds.executors;

import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import com.mystic.guilds.MysticGuilds;
import com.mystic.guilds.controller.GuildController;
import com.mystic.guilds.controller.MC;
import com.mystic.guilds.objects.Guild;
import com.mystic.guilds.objects.GuildPlayer;
import com.mystic.guilds.objects.Relationship;
import com.mystic.guilds.objects.Relationship.RelationshipType;
import com.mystic.guilds.objects.ReturnValue;

public class RelationshipExecutor {
	protected static final int SELF = CustomCommandExecutor.SELF;

	GuildController gc;

	public RelationshipExecutor(GuildExecutor guildExecutor, GuildController guildController){
		guildExecutor.addMethods(this, this.getClass().getMethods());
		this.gc = guildController;
	}
	
	@MCCommand( cmds = {"setRelationship"}, guild={1,2}, op=true,min=3, order=1)
	public void setRelationship(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		Guild g1 = (Guild) args[1];
		Guild g2 = (Guild) args[2];
		gc.declareRelationship(g1,g2,RelationshipType.ALLY);
	}

	@MCCommand( cmds = {"declareWar","war"}, perm="guild.relationships",inGuild={SELF}, guild={1},min=2)
	public void declareWar(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		Guild g2 = (Guild) args[1];
		if (gp.getGuild().getName().equals(g2.getName())){
			MC.sendMessage(sender, ChatColor.RED+"You declare war on yourself");
			return;
		}
		Guild g1 = gp.getGuild();
		Relationship rel = gc.getRelationship(g1, g2);
		if (rel != null && rel.isAlly()){
			MC.sendMessage(sender, ChatColor.RED+"You can't declare war on an ally!");
			return;			
		}
		declareRelationship(sender,g1,g2,RelationshipType.WAR);
	}

	@MCCommand( cmds = {"declareHostile","hostile"}, perm="guild.relationships",inGuild={SELF}, guild={1},min=2)
	public void declareHostile(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		Guild g2 = (Guild) args[1];
		if (gp.getGuild().getName().equals(g2.getName())){
			MC.sendMessage(sender, ChatColor.RED+"You cant declare war on yourself");
			return;
		}
		Guild g1 = gp.getGuild();
		declareRelationship(sender,g1,g2,RelationshipType.HOSTILE);
	}

	@MCCommand( cmds = {"declareHostile","hostile"}, guild={1,2}, op=true,min=3, order=1)
	public void declareHostileOther(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		declareRelationship(sender,(Guild) args[1],(Guild) args[2],RelationshipType.HOSTILE);
	}

	@MCCommand( cmds = {"declareAlly","ally"}, inGuild={SELF}, perm="guild.relationships",guild={1},min=2)
	public void declareAlly(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		Guild g2 = (Guild) args[1];
		if (gp.getGuild().getName().equals(g2.getName())){
			MC.sendMessage(sender, ChatColor.RED+"You cant be an ally with yourself");
			return;
		}
		Guild g1 = gp.getGuild();
		if (gc.hasAsked(g2,g1,RelationshipType.ALLY)){
			declareRelationship(sender,gp.getGuild(),(Guild) args[1],RelationshipType.ALLY);
		} else {
			MC.sendMessage(sender, ChatColor.GREEN+"You have asked " +g2.getLabel()+" to be &6allied&2 with you ");			
			MC.sendMessage(sender, ChatColor.GREEN+"They will need to accept by declaring you as an ally as well");			
			MC.sendMessage(g2.getLeader(), "&6"+g1.getLabel()+"&2 has asked you to be "+RelationshipType.ALLY);			
			gc.asked(g1,g2,RelationshipType.ALLY);
		}
	}

	@MCCommand( cmds = {"declareAlly","ally"}, guild={1,2}, op=true,min=3, order=1)
	public void declareAllyOther(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		declareRelationship(sender,(Guild) args[1],(Guild) args[2],RelationshipType.ALLY);
	}

	@MCCommand( cmds = {"declareNeutral","neutral"}, perm="guild.relationships",inGuild={SELF}, guild={1},min=2)
	public void declareNeutral(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		Guild g2 = (Guild) args[1];
		if (gp.getGuild().getName().equals(g2.getName())){
			MC.sendMessage(sender, ChatColor.RED+"You cant be neutral with yourself");
			return;
		}
		Guild g1 = gp.getGuild();
		Relationship rel = gc.getRelationship(g1,g2);
		
		if (rel != null && rel.getValue() < RelationshipType.NEUTRAL.value){
			if (gc.hasAsked(g2,g1,RelationshipType.NEUTRAL)){
				declareRelationship(sender,gp.getGuild(),(Guild) args[1],RelationshipType.NEUTRAL);
			} else {
				MC.sendMessage(sender, ChatColor.GREEN+"You have asked " +g2.getLabel()+" to be &6neutral&2 with you ");			
				MC.sendMessage(sender, ChatColor.GREEN+"They will need to accept by declaring you as neutral as well");			
				gc.asked(g1,g2,RelationshipType.NEUTRAL);
			}			
		} else {
			declareRelationship(sender,gp.getGuild(),(Guild) args[1],RelationshipType.NEUTRAL);			
		}
	}

	@MCCommand( cmds = {"declareNeutral","neutral"}, guild={1,2}, op=true,min=3, order=1)
	public void declareNeutralOther(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		declareRelationship(sender,(Guild) args[1],(Guild) args[2],RelationshipType.NEUTRAL);
	}

	private void declareRelationship(CommandSender sender, Guild g1, Guild g2, RelationshipType type){
		Relationship rel = gc.getRelationship(g1,g2);
		if (rel != null){
			if (rel.sameRelationship(type)){
				MC.sendMessage(sender, "&6"+g1.getName()+"&e is already "+type.name+" with &6"+g2.getName());
				return;
			}
		}
		
		ReturnValue rv = gc.declareRelationship(g1,g2,type);
		
		if (rv.getMsg() != null)
			MC.sendMultilineMessage(sender, rv.getMsg());
		if (rv.getServerMsg() != null) 
			MC.broadcastMultilineMessage(rv.getServerMsg());
		switch (rv.val) {
		case True: 
			if (rv.getMsg() == null) 
				MC.sendMessage(sender, "&eYou are now "+type.name +" with &6"+g2.getName());		
			if (rv.getServerMsg()==null) 
				Bukkit.broadcastMessage(MC.colorChat("&4[WAR]&6 "+g1.getName()+"&e is now &6"+type.name+"&e with &6"+g2.getName()));
			break;
		case False: 
			MC.sendMessage(sender, "&4Error declaring "+type.name);
			break;
		case Cancelled:
			break;
		}
	}
	


	public static void sendGuildRelationships(GuildController gc, CommandSender sender, GuildPlayer gp,Guild guild) {
		Set<Relationship> rels = guild.getRelationships(MysticGuilds.getDB());
		//		System.out.println("rels= " +rels);
		if (rels == null || rels.isEmpty()){
			return;}
		StringBuilder allies = new StringBuilder();
		StringBuilder hostile = new StringBuilder();
		StringBuilder war = new StringBuilder();
		boolean firstAlly = true, firstHostile = true, firstWar = true;
		for (Relationship rel: rels){
			System.out.println(rel);
			if (rel.getId() != 0 && rel.getGuild1() == null){ /// why is avaje not loading!!!!!
				rel = gc.getRelationship(rel);}
			if (rel.isAlly()){
				if (!firstAlly) allies.append("&e, ");
				else firstAlly = false;
				allies.append(rel.getGuild2().getLabel());
			} else if (rel.isWarring()){
				if (!firstWar) war.append("&e, ");
				else firstWar = false;
				war.append(rel.getGuild2().getLabel());
			} else if (rel.isHostile()){
				if (!firstHostile) hostile.append("&e, ");
				else firstHostile = false;
				hostile.append(rel.getGuild2().getLabel());
			}
		}
		if (!firstAlly){
			MC.sendMessage(sender, "&2Allies: &e" + allies.toString());}
		if (!firstHostile){
			MC.sendMessage(sender, "&cHostile: &e" + hostile.toString());}
		if (!firstWar){
			MC.sendMessage(sender, "&4Warring: &e" + war.toString());}
	}
}
