package com.mystic.guilds.executors;

import java.util.Formatter;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.mystic.guilds.Defaults;
import com.mystic.guilds.controller.GuildController;
import com.mystic.guilds.controller.MC;
import com.mystic.guilds.controller.PermissionController;
import com.mystic.guilds.objects.Guild;
import com.mystic.guilds.objects.GuildPlayer;
import com.mystic.guilds.objects.permissions.GuildPermission;
import com.mystic.guilds.objects.permissions.PermissionGroup;
import com.mystic.guilds.util.PrintingUtil;

public class GroupExecutor{
	protected static final int SELF = CustomCommandExecutor.SELF;

	GuildController gc;

	public GroupExecutor(GuildExecutor guildExecutor, GuildController guildController){
		super();
		guildExecutor.addMethods(this, this.getClass().getMethods());
		this.gc = guildController;
	}

	@MCCommand( cmds = {"promote"}, inGuild={SELF,1}, perm="guild.promote", sameGuild=true)
	public void promoteGuildMember(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		Guild guild = gp.getGuild();
		if(guild.isCharter()){
			MC.sendMessage(sender,ChatColor.RED+"You can't promote in a charter!.");
			return;
		}
		GuildPlayer target = (GuildPlayer) args[1];
		if(!gp.higherRank(target)){
			MC.sendMessage(sender, "&4You can't promote someone of the same or greater rank!");
			return;
		}
		if(target.getName().equals(gp.getName())){
			MC.sendMessage(sender,ChatColor.RED+"You can't promote yourself.");
			return;
		}

		promote(sender, gp, target);
	}

	@MCCommand( cmds = {"promote"}, op=true, inGuild={1}, order=1)
	public void promoteGuildMemberOther(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		GuildPlayer target = (GuildPlayer) args[1];
		promote(sender, gp, target);
	}

	private boolean promote(CommandSender sender, GuildPlayer gp, GuildPlayer target) {
		PermissionGroup cur = PermissionController.getRank(target.getGroups(),true);
		PermissionGroup next = PermissionController.nextGroup(target.getGuild(),target.getRank(),true,Defaults.PLAYER_RANK_TRACK);
		if (next == null){
			if (cur == null)
				return MC.sendMessage(sender, ChatColor.RED+"There is no valid rank to promote to ");
			else
				return MC.sendMessage(sender, ChatColor.RED+"You cannot promote past " + cur.getLabel());
		} else if (next.isLeader()){
			return MC.sendMessage(sender, ChatColor.RED+"You cannot promote to " + next.getLabel()+". use &6/guild setleader <name>");
		}

		gc.guildPromote(target);
		MC.sendMessage(sender, MC.getMessage("success", "promoted_you_have",target.getName(), next.getLabel()));
		MC.broadcast(target.getGuild(), MC.getMessage("success", "promoted_guildbroadcast",target.getName()), gp, target);
		return MC.sendMessage(target, MC.getMessage("success", "promoted_you_have_been", next.getLabel()));
	}

	@MCCommand( cmds = {"demote"}, inGuild={SELF,1}, perm="guild.demote", sameGuild=true)
	public void demoteGuildPlayer(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		Guild guild = gp.getGuild();
		if(guild.isCharter()){
			MC.sendMessage(sender,ChatColor.RED+"You can't demote officers in a charter.");
			return;
		}
		GuildPlayer target = (GuildPlayer) args[1];
		if(!gp.higherRank(target)){
			MC.sendMessage(sender, "&4You can't demote someone of the same or greater rank!");
			return;
		}
		demote(sender, gp, target);
	}

	@MCCommand( cmds = {"demote"}, op=true, inGuild={1})
	public void demoteGuildMemberOther(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		GuildPlayer target = (GuildPlayer) args[1];
		demote(sender, gp, target);
	}

	private boolean demote(CommandSender sender, GuildPlayer gp, GuildPlayer target) {
		PermissionGroup cur = PermissionController.getRank(target.getGroups(),false);
		PermissionGroup next = PermissionController.nextGroup(target.getGuild(),target.getRank(),false,Defaults.PLAYER_RANK_TRACK);
		if (next == null){
			if (cur == null)
				return MC.sendMessage(sender, ChatColor.RED+"There is no other rank to demote to");
			else
				return MC.sendMessage(sender, ChatColor.RED+"You cannot demote past " + cur.getLabel());
		}
		gc.guildDemote(target,next);
		MC.sendMessage(sender, MC.getMessage("success", "demoted_you_have",target.getName(), next.getLabel()));
		MC.broadcast(target.getGuild(), MC.getMessage("success", "demoted_guildbroadcast",target.getName()), gp, target);
		return MC.sendMessage(target, MC.getMessage("success", "demoted_you_have_been", next.getLabel()));
	}

	@MCCommand( cmds = {"leader","setleader"}, inGuild={SELF,1}, perm="guild.setleader", sameGuild=true)
	public void setLeader(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		Guild guild = gp.getGuild();
		if(guild.isCharter()){
			MC.sendMessage(sender,ChatColor.RED+"You can't promote leaders in a charter.");
			return;
		}
		GuildPlayer target = (GuildPlayer) args[1];
		if(target.getName().equals(gp.getName())){
			MC.sendMessage(sender,ChatColor.RED+"You can't promote yourself.");
			return;
		}
		gc.guildSetLeader(guild, target);
	}

	@MCCommand( cmds = {"leader","setleader"}, inGuild={1}, op=true, order=1)
	public void setLeaderOther(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		GuildPlayer target = (GuildPlayer) args[1];
		gc.guildSetLeader(target.getGuild(), target);
	}

	@MCCommand( cmds = {"rank","title"}, inGuild = {SELF}, max=1)
	public void rankSelf(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		String groupStr = PrintingUtil.groupsToString(gp.getRanks());
		MC.sendMessage(sender, MC.getMessage("success", "rank_you_have", groupStr));
	}

	@MCCommand( cmds = {"rank","title"}, inGuild={1}, max=2)
	public void rankOther(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		GuildPlayer other = (GuildPlayer)args[1];
		String groupStr = PrintingUtil.groupsToString(other.getRanks());
		MC.sendMessage(sender, MC.getMessage("success", "rank_other_has", other.getName(),groupStr) +" guild:"+other.getGuild().getName());
	}

	@MCCommand( cmds = {"perms","permissions","perm","permission"}, inGuild = {SELF}, max=1)
	public void permissionsSelf(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		String groupStr = PrintingUtil.permsToString(gp.getPermissions());
		MC.sendMessage(sender, MC.getMessage("success", "permissions_you_have", groupStr));
	}

	@MCCommand( cmds = {"perms","permissions","perm","permission"}, inGuild={1}, max=2)
	public void permissionsOther(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		GuildPlayer other = (GuildPlayer)args[1];
		String groupStr = PrintingUtil.permsToString(other.getPermissions());
		MC.sendMessage(sender, MC.getMessage("success", "permissions_other_has", other.getName(),groupStr));
	}

	@MCCommand( cmds = {"upgrade"}, guild={1}, min=2,op = true,
			usage="/guild upgrade <guild> <rank> <level>")
	public void upgradeGuildOther(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		Guild target = (Guild) args[1];
		upgrade(sender, gp, target);
	}

	private void changePerm(CommandSender sender, GuildPlayer gp, Guild guild, String group, String perm, boolean rank, boolean add){
		final String rt = rank ? "Rank" : "Title";
		final int track = rank? Defaults.PLAYER_RANK_TRACK : Defaults.PLAYER_TITLE_TRACK;
		final String ad = add ? "add" : "delete";
		final String added = add ? "added" : "deleted";
		PermissionGroup pg = PermissionController.findGroup(group, guild, track);

		if(pg==null){
			MC.sendMessage(sender, rt +" " + group+" doesnt exists");
			return;
		}
		GuildPermission gperm = PermissionController.assignablePermission(perm);
		if (gperm==null){
			MC.sendMessage(sender, "Permission "+perm+" doesnt exist");
			return;
		}
		if (gp != null ){ /// non op
			Set<GuildPermission> perms = gp.getPermissions();
			//			for (GuildPermission permm : perms){
			////				System.out.println("permm=" + permm.getName() +"  " + perms.contains(gperm));
			//			}
			if (!perms.contains(gperm)){
				MC.sendMessage(sender, "You can't "+ad+" a permission you do not have!");
				return;
			}
		}
		gc.verifyGuildGroup(guild);
		if (add){
			if (pg.hasPerm(gperm)){
				MC.sendMessage(sender, pg.getLabel() +" &calready has permission &6"+perm);
				return;
			}
		} else {
			PermissionGroup memgroup = PermissionController.getMemberGroup(null);
			/// They cant delete the basic permissions
			if (memgroup.getPermissions().contains(gperm)){
				MC.sendMessage(sender, "You cant delete the default perm "+perm);
				return;
			}
		}

		boolean success = add? PermissionController.addPermission(pg, gperm) : PermissionController.removePermission(pg, gperm);
		if (success)
			MC.sendMessage(sender, ChatColor.GREEN +rt+" " + pg.getName()+" perms "+added +"!");
		else
			MC.sendMessage(sender, ChatColor.RED +"Error changing "+rt+" " + pg.getName()+". perms werent changed!");
	}

	@MCCommand( cmds = {"addRankPerm"}, inGuild={SELF}, perm="guild.addRankPerm",min=2)
	public void addRankPerms(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		if (!allowUserRanks(sender))
			return;
		String group = ((String) args[1]).toLowerCase();
		String perm = ((String) args[2]).toLowerCase();

		changePerm(sender,gp, gp.getGuild(), group, perm, true, true);
	}

	@MCCommand( cmds = {"addRankPerm"}, guild={1}, op=true,min=4,order=1,
			usage="/guild addRankPerm <guild> <rank> <perm>")
	public void addRankPermsOther(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		Guild guild = (Guild) args[1];
		String group = ((String) args[2]).toLowerCase();
		String perm = ((String) args[3]).toLowerCase();
		changePerm(sender,null, guild, group, perm, true,true);
	}

	@MCCommand( cmds = {"addTitlePerm"}, inGuild={SELF}, perm="guild.addTitlePerm",min=3)
	public void addTitlesPerms(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		if (!allowUserRanks(sender))
			return;
		String group = ((String) args[1]).toLowerCase();
		String perm = ((String) args[2]).toLowerCase();
		changePerm(sender,gp, gp.getGuild(), group, perm, false,true);
	}

	@MCCommand( cmds = {"addTitlePerm"}, guild={1}, op=true,min=4,order=1,
			usage="/guild addRankPerm <guild> <rank> <perm>")
	public void addTitlesPermsOther(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		Guild guild = (Guild) args[1];
		String group = ((String) args[2]).toLowerCase();
		String perm = ((String) args[3]).toLowerCase();
		changePerm(sender,null, guild, group, perm, false,true);
	}

	@MCCommand( cmds = {"deleteRankPerm"}, inGuild={SELF}, perm="guild.deleteRankPerm",min=3)
	public void delRankPerms(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		if (!allowUserRanks(sender))
			return;
		String group = ((String) args[1]).toLowerCase();
		String perm = ((String) args[2]).toLowerCase();
		changePerm(sender,gp, gp.getGuild(), group, perm, true, false);
	}

	@MCCommand( cmds = {"delteRankPerm"}, guild={1}, op=true,min=4,order=1,
			usage="/guild addRankPerm <guild> <rank> <perm>")
	public void delRankPermsOther(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		Guild guild = (Guild) args[1];
		String group = ((String) args[2]).toLowerCase();
		String perm = ((String) args[3]).toLowerCase();
		changePerm(sender,null, guild, group, perm, true,false);
	}

	@MCCommand( cmds = {"deleteTitlePerm"}, inGuild={SELF}, perm="guild.deleteTitlePerm",min=2)
	public void delTitlesPerms(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		if (!allowUserRanks(sender))
			return;
		String group = ((String) args[1]).toLowerCase();
		String perm = ((String) args[2]).toLowerCase();
		changePerm(sender,gp, gp.getGuild(), group, perm, false,false);
	}

	@MCCommand( cmds = {"deleteTitlePerm"}, guild={1}, op=true,min=4,order=1,
			usage="/guild addRankPerm <guild> <rank> <perm>")
	public void delTitlesPermsOther(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		Guild guild = (Guild) args[1];
		String group = ((String) args[2]).toLowerCase();
		String perm = ((String) args[3]).toLowerCase();
		changePerm(sender,null, guild, group, perm, false,false);
	}

	@MCCommand( cmds = {"createrank"}, inGuild={SELF}, ints={2}, perm="guild.createrank",min=3,max=3)
	public void createRank(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		String rankName = (String) args[1];
		Integer rank = (Integer) args[2];
		addGroup(sender,gp,gp.getGuild(),rankName,rank,true);
	}
	@MCCommand( cmds = {"createrank"}, guild={1}, ints={3}, min=4,max=4,op=true,order=1,
			usage="/guild createrank <guild> <rank> <level>")
	public void createRankOther(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		Guild guild = (Guild) args[1];
		String rankName = (String) args[2];
		Integer rank = (Integer) args[3];
		addGroup(sender,gp,guild,rankName,rank,true);
	}


	@MCCommand( cmds = {"createtitle"}, inGuild={SELF}, perm="guild.createtitle",ints={2}, min=3,max=3)
	public void createTitle(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		String rankName = (String) args[1];
		Integer rank = (Integer) args[2];
		addGroup(sender,gp,gp.getGuild(),rankName,rank,false);
	}

	@MCCommand( cmds = {"createtitle"}, guild={1}, ints={3}, min=4,max=4,op=true,order=1,
			usage="/guild createtitle <guild> <title> <level>")
	public void createTitleOther(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		Guild guild = (Guild) args[1];
		String rankName = (String) args[2];
		Integer rank = (Integer) args[3];
		addGroup(sender,gp,guild,rankName,rank,false);
	}


	@MCCommand( cmds = {"deleterank","removerank"}, inGuild={SELF},perm="guild.deleterank", min=2,max=2)
	public void deleteRank(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		removeGroup(sender,gp,gp.getGuild(), (String) args[1], true);
	}

	@MCCommand( cmds = {"deleterank"}, guild={1}, min=3,max=3,op=true,order=1,
			usage="/guild deleterank <guild> <rank>")
	public void deleteRankOther(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		removeGroup(sender,gp,(Guild) args[1], (String) args[2], true);
	}

	@MCCommand( cmds = {"deletetitle","removetitle"}, inGuild={SELF}, perm="guild.deletetitle",min=2,max=2)
	public void deleteTitle(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		removeGroup(sender,gp,gp.getGuild(), (String)args[1], false);
	}
	@MCCommand( cmds = {"deletetitle","removetitle"}, guild={1}, min=3,max=3,op=true,order=1)
	public void deleteTitleOther(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		removeGroup(sender,gp,(Guild) args[1], (String) args[2], false);
	}


	private void addGroup(CommandSender sender, GuildPlayer gp, Guild guild, String group, int ranklvl, boolean rank) {
		String rt = rank ? "Rank" : "Title";
		int track = rank? Defaults.PLAYER_RANK_TRACK : Defaults.PLAYER_TITLE_TRACK;
		gc.verifyGuildGroup(guild);
		String label = new String(group);
		group = MC.decolorChat(group);
		PermissionGroup pg = PermissionController.findGroup(group, guild, track);
		if(pg!=null){
			MC.sendMessage(sender, ChatColor.RED+rt +" already exists");
			return;
		}
		if (rank){
			pg = PermissionController.getGroup(ranklvl, guild, track);
			if(pg!=null){
				MC.sendMessage(sender, ChatColor.RED+pg.getLabel()+" already has that rank level");
				return;
			}
		}
		if (!validGuildPlayerGroup(sender, guild,group, ranklvl,rank))
			return;
		if (gp != null){
			if (!allowUserRanks(sender))
				return;
			if (ranklvl < gp.getRank()){
				MC.sendMessage(sender, ChatColor.RED+"You can't add a "+rt+" with a lower level than yourself");
				return;
			}
		}
		if (rank){
			pg = PermissionController.createGuildPlayerRank(group,label, ranklvl,guild);
		} else {
			pg = PermissionController.createGuildPlayerTitle(group,label, ranklvl,guild);
		}
		MC.sendMessage(sender, ChatColor.GREEN +rt + " " + pg.getLabel()+"&2 created!");

	}
	private void removeGroup(CommandSender sender, GuildPlayer gp, Guild guild, String group, boolean rank) {
		String rt = rank ? "Rank" : "Title";
		int track = rank? Defaults.PLAYER_RANK_TRACK : Defaults.PLAYER_TITLE_TRACK;
		gc.verifyGuildGroup(guild);
		PermissionGroup pg = PermissionController.findGroup(group, guild, track);
		if(pg==null){
			MC.sendMessage(sender, ChatColor.RED+rt +" doesnt exist");
			return;
		}
		if (gp != null){
			if (!allowUserRanks(sender))
				return;
			if (pg.getRank() < gp.getRank()){
				MC.sendMessage(sender, ChatColor.RED+"You can't delete a "+rt+" with lower level than yourself");
				return;
			}
		}
		if (pg.getRank() == 1 || pg.getRank() == 1000){
			MC.sendMessage(sender, ChatColor.RED+"You can't delete this group");
			return;
		}
		if (rank){
			PermissionController.deleteGuildPlayerRank(pg.getName(),guild);
		} else {
			PermissionController.deleteGuildPlayerTitle(pg.getName(),guild);
		}
		MC.sendMessage(sender, ChatColor.GREEN +rt +" &6" +pg.getLabel()+"&2 removed!");
	}

	@MCCommand( cmds = {"titleInfo"}, inGuild={SELF},min=2)
	public void titleInfo(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		String group = ((String) args[1]).toLowerCase();
		Guild guild = gp.getGuild();
		groupInfo(sender,guild, group, false);
	}

	@MCCommand( cmds = {"titleInfo"},guild={1}, min=3, usageNode="titleInfoOther")
	public void titleInfoOther(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		Guild guild = (Guild) args[1];
		String group = ((String) args[2]).toLowerCase();
		groupInfo(sender,guild, group, false);
	}

	@MCCommand( cmds = {"titles"}, inGuild={SELF},min=1)
	public void listTitles(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		Guild guild = gp.getGuild();
		listPlayerGroups(sender,guild, false);
	}

	@MCCommand( cmds = {"titles"}, guild={1}, min=2)
	public void listTitlesOther(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		Guild guild = (Guild) args[1];
		listPlayerGroups(sender,guild, false);
	}

	@MCCommand( cmds = {"ranks"}, inGuild={SELF},min=1)
	public void listRanks(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		Guild guild = gp.getGuild();
		listPlayerGroups(sender,guild, true);
	}

	@MCCommand( cmds = {"ranks"}, guild={1}, min=2)
	public void listRanksOther(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		Guild guild = (Guild) args[1];
		listPlayerGroups(sender,guild, true);
	}

	private void listPlayerGroups(CommandSender sender, Guild guild, boolean rank) {
		List<PermissionGroup> groups  = null;
		String rOrT = rank ? "Rank" : "Title";
		if (rank){
			groups = PermissionController.getGroups(guild, Defaults.PLAYER_RANK_TRACK);
		} else {
			groups = PermissionController.getGroups(guild, Defaults.PLAYER_TITLE_TRACK);
		}

		TreeMap<Double, PermissionGroup> map = new TreeMap<Double,PermissionGroup>();

		for (PermissionGroup group: groups){
			if (map.containsKey(Double.valueOf(group.getRank()))){
				map.put(group.getRank()+0.01D, group);
			} else{
				map.put(Double.valueOf(group.getRank()), group);
			}
		}
		MC.sendMessage(sender, "&4------------- &6Player " + rOrT +" &4-------------");
		for (Double d: map.keySet()){
			StringBuilder sb = new StringBuilder();
			StringBuilder buf = new StringBuilder();
			Formatter formatter = new Formatter(buf);

			boolean first = true;
			PermissionGroup pg = map.get(d);
			for (GuildPermission perm: pg.getPermissions()){
				if (!first) sb.append(", ");
				first = false;
				sb.append(perm.getName());
			}
			formatter.format("&6 [%-4d %s &6]%s: %s",pg.getRank(), pg.getLabel(), pg.getColor(),sb.toString());
			formatter.close();
			MC.sendMessage(sender, buf.toString());
		}
	}

	@MCCommand( cmds = {"rankInfo"}, inGuild={SELF},min=2)
	public void rankInfo(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		String group = ((String) args[1]).toLowerCase();
		Guild guild = gp.getGuild();
		groupInfo(sender,guild, group, true);
	}

	@MCCommand( cmds = {"rankInfo"},guild={1}, min=3, usageNode="rankInfoOther")
	public void rankInfoOther(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		Guild guild = (Guild) args[1];
		String group = ((String) args[2]).toLowerCase();
		groupInfo(sender,guild, group, true);
	}

	@MCCommand( cmds = {"giveTitle"},inGuild={SELF,1}, sameGuild=true,perm="guild.givetitle",min=3)
	public void giveTitle(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		if (!allowUserRanks(sender))
			return;
		GuildPlayer target = (GuildPlayer) args[1];
		String group = ((String) args[2]).toLowerCase();
		giveTitle(sender,gp, target.getGuild(),target,group);
	}


	@MCCommand( cmds = {"giveDefTitle","giveDefaultTitle"},inGuild={1}, op=true,min=3,order=1,
			usage="/guild giveDefaultTitle <player> <title>")
	public void giveDefaultTitleOther(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		GuildPlayer target = (GuildPlayer) args[1];
		String group = ((String) args[2]).toLowerCase();
		giveTitle(sender,null, null, target,group);
	}

	@MCCommand( cmds = {"giveTitle"},inGuild={1}, op=true,min=3,order=1,
			usage="/guild giveTitle <player> <title>")
	public void giveTitleOther(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		GuildPlayer target = (GuildPlayer) args[1];
		String group = ((String) args[2]).toLowerCase();
		giveTitle(sender,null,target.getGuild(), target,group);
	}

	private void giveTitle(CommandSender sender, GuildPlayer gp, Guild guild, GuildPlayer target, String title){
		PermissionGroup pg = PermissionController.findGroup(title, guild, Defaults.PLAYER_TITLE_TRACK);
		if (pg == null){
			MC.sendMessage(sender, "That title doesnt exists.");
			return;
		}
		gc.playerAddGroup(target, pg);
		MC.sendMessage(sender, MC.getMessage("success", "title_you_have_given",target.getName(), pg.getLabel()));
		MC.broadcast(target.getGuild(), MC.getMessage("success", "title_given_guildbroadcast",target.getName(), pg.getLabel()), gp, target);
		MC.sendMessage(target, MC.getMessage("success", "title_you_have_been_given", pg.getLabel()));
	}

	@MCCommand( cmds = {"removeTitle"},inGuild={SELF,1}, sameGuild=true,perm="guild.removetitle",min=2)
	public void removeTitle(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		if (!allowUserRanks(sender))
			return;
		GuildPlayer target = (GuildPlayer) args[1];
		String group = ((String) args[2]).toLowerCase();
		removeTitle(sender,gp, target,group);
	}

	@MCCommand( cmds = {"removeTitle"},inGuild={1}, op=true,min=2, order=1,
			usage="/guild removeTitle <player> <title>")
	public void removeTitleOther(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		GuildPlayer target = (GuildPlayer) args[1];
		String group = ((String) args[2]).toLowerCase();
		removeTitle(sender,null, target,group);
	}

	private void removeTitle(CommandSender sender, GuildPlayer gp, GuildPlayer target, String title){
		PermissionGroup pg = PermissionController.findGroup(title, target.getGuild(), Defaults.PLAYER_TITLE_TRACK);
		if (pg == null){
			MC.sendMessage(sender, "That title doesnt exists.");
			return;
		}
		gc.playerRemoveGroup(target, pg);

		MC.sendMessage(sender, MC.getMessage("success", "title_you_have_removed",target.getName(), pg.getLabel()));
		MC.broadcast(target.getGuild(), MC.getMessage("success", "title_removed_guildbroadcast",target.getName(), pg.getLabel()), gp, target);
		MC.sendMessage(target, MC.getMessage("success", "title_you_have_been_removed", pg.getLabel()));
	}

	@MCCommand( cmds = {"setTitleLabel"},inGuild={SELF}, perm="guild.settitlelabel",min=3)
	public void setTitleLabel(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		gc.verifyGuildGroup(gp.getGuild());

		String group = (String) args[1];
		label = (String) args[2];
		PermissionGroup pg = PermissionController.findGroup(group, gp.getGuild(), Defaults.PLAYER_TITLE_TRACK);
		if(pg==null){
			MC.sendMessage(sender, group+" doesnt exists");
			return;
		}
		if (pg.getRank() < gp.getRank()){
			MC.sendMessage(sender, "You can't modify a Title that has a lower level than you");
			return;
		}
		if (!validGroupLabel(sender, gp.getGuild(), MC.decolorChat(label), false)){
			return;
		}

		setLabel(sender,gp.getGuild(),group,label,false);
	}

	@MCCommand( cmds = {"setTitleLabel"},guild={1}, op=true,min=4,order=1,
			usage="/guild setRankLabel <guild> <rank> <new label>")
	public void setTitleLabelOther(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		setLabel(sender,(Guild) args[1],(String)args[2],(String) args[3],false);
	}


	@MCCommand( cmds = {"setRankLabel"},inGuild={SELF}, perm="guild.setranklabel",min=3)
	public void setRankLabel(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		if (!allowUserRanks(sender))
			return ;
		gc.verifyGuildGroup(gp.getGuild());
		String group = (String) args[1];
		label = (String) args[2];
		PermissionGroup pg = PermissionController.findGroup(group, gp.getGuild(), Defaults.PLAYER_RANK_TRACK);
		if(pg==null){
			MC.sendMessage(sender, group+" doesnt exists");
			return;
		}
		if (pg.getRank() < gp.getRank()){
			MC.sendMessage(sender, "You can't modify a rank that has a lower level than you");
			return;
		}
		if (!validGroupLabel(sender, gp.getGuild(), MC.decolorChat(label), true)){
			return;
		}
		setLabel(sender,gp.getGuild(),group,label,true);
	}

	@MCCommand( cmds = {"setRankLabel"},guild={1}, op=true,min=4, order=1,
			usage="/guild setRankLabel <guild> <rank> <new label>")
	public void setRankLabelOther(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		setLabel(sender,(Guild) args[1],(String)args[2],(String) args[3],true);
	}

	private void setLabel(CommandSender sender, Guild guild, String group, String label, boolean rank) {
		String rt = rank ? "Rank" : "Title";
		int track = rank? Defaults.PLAYER_RANK_TRACK : Defaults.PLAYER_TITLE_TRACK;
		gc.verifyGuildGroup(guild);
		PermissionGroup pg = PermissionController.findGroup(group, guild, track);
		if(pg==null){
			MC.sendMessage(sender, rt +" " + group+" doesnt exists");
			return;
		}
		if (label.isEmpty()){
			MC.sendMessage(sender, "You must specify a valid label. '" +label+"' isnt valid");
			return;
		}
		if (MC.decolorChat(label).length() > Defaults.MAX_GUILD_NAME_LENGTH){
			MC.sendMessage(sender, ChatColor.RED+"That label is too long, &6"+Defaults.MAX_GUILD_NAME_LENGTH+" chars max" );
			return;
		}
		String oldLabel = pg.getLabel();
		PermissionController.relabel(pg,label);
		MC.sendMessage(sender, oldLabel +" has been relabeled to "+ pg.getLabel());
	}

	private void groupInfo(CommandSender sender, Guild guild, String group,boolean rank) {
		String rt = rank ? "Rank" : "Title";
		int track = rank? Defaults.PLAYER_RANK_TRACK : Defaults.PLAYER_TITLE_TRACK;
		PermissionGroup pg = PermissionController.findGroup(group, guild, track);
		if(pg==null){
			MC.sendMessage(sender, rt +" " + group+" doesnt exists");
			return;
		}

		Set<GuildPermission> perms = pg.getPermissions();
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (GuildPermission perm: perms){
			if (!first) sb.append(", ");
			first = false;
			sb.append(perm.getName());
		}
		MC.sendMessage(sender, ChatColor.GREEN +rt+" has perms. &6" + sb.toString());
		if (guild != null){
			sb = new StringBuilder();
			first = true;
			Set<GuildPlayer> players = guild.getMembers();
			for (GuildPlayer player: players){
				if (!player.hasGroup(pg))
					continue;
				if (!first) sb.append(", ");
				first = false;
				sb.append(player.getName());
			}
			MC.sendMessage(sender, ChatColor.GREEN +"Members: &6" + (first ? "None" : sb.toString()));
		}
	}

	private boolean allowUserRanks(CommandSender sender) {
		if (!Defaults.ALLOW_USERMADE_RANKS) {
			MC.sendMessage(sender, ChatColor.RED+"Player made ranks/titles are currently disabled");
			return false;
		}
		return true;
	}

	private boolean upgrade(CommandSender sender,GuildPlayer gp, Guild guild) {
		PermissionGroup cur = PermissionController.getRank(guild.getGroups(),true);
		PermissionGroup next = PermissionController.nextGroup(guild, guild.getRank(),true,Defaults.GUILD_RANK_TRACK);
		if (next == null){
			if (cur == null)
				return MC.sendMessage(sender, ChatColor.RED+"There is no valid rank to upgrade to ");
			else
				return MC.sendMessage(sender, ChatColor.RED+"You cannot upgrade past " + cur.getLabel());
		}

		if (gc.guildUpgrade(guild, next)){
			MC.sendMessage(sender, MC.getMessage("success", "upgraded_you_have", next.getLabel()));
			MC.broadcast(guild, MC.getMessage("success", "upgraded_guildbroadcast", next.getLabel()), gp);
		} else {
			MC.sendMessage(sender, "Upgrade event was cancelled" + next.getLabel());
		}

		return true;
	}

	@MCCommand( cmds = {"checkPerm","cp","hasPerm"}, inGuild={1}, op=true,min=3)
	public void checkPerms(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		GuildPlayer player = (GuildPlayer) args[1];
		String perm = (String) args[2];
		Player p = Bukkit.getPlayer(player.getName());
		if (p == null){
			MC.sendMessage(sender, "Couldnt find Player &6" +player.getName());
			return;
		}


		boolean has = p.hasPermission(perm);
		MC.sendMessage(sender, "Player &6"+player.getName()+"&e " +(has? "has" : "doesnt have") +" perm &6"+perm);
	}

	@MCCommand( cmds = {"copyGuildRanks"},guild={2}, op=true,min=3, order=1,
			usage="/guild copyGuildRanks <guild1> <guild2>")
	public void copyGuildRanks(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		String g1 = (String) args[1];
		Guild guild1 = gc.getGuild(g1);
		if (guild1 == null && !(g1.equalsIgnoreCase("null") || g1.equalsIgnoreCase("default"))){
			MC.sendMessage(sender, "First argument must be a guild name or 'default'");
			return;
		}
		gc.copyGroups(guild1, (Guild) args[2], Defaults.PLAYER_RANK_TRACK);
	}

	@MCCommand( cmds = {"copyRankPerms"},guild={3}, op=true,min=5, order=1,
			usage="/guild copyRankPerms <guild1> <guild rank1> <guild2> <guild rank2>")
	public void copyRankPerms(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		String g1 = (String) args[1];
		Guild guild1 = gc.getGuild(g1);
		String r1 = (String) args[2];
		Guild guild2 = (Guild) args[3];
		String r2 = (String) args[4];

		if (guild1 == null && !(g1.equalsIgnoreCase("null") || g1.equalsIgnoreCase("default"))){
			MC.sendMessage(sender, "First argument must be a guild name or 'default'");
			return;
		}
		PermissionGroup pg1 = PermissionController.getGroup(r1, guild1, Defaults.PLAYER_RANK_TRACK);
		PermissionGroup pg2 = PermissionController.getGroup(r2, guild2, Defaults.PLAYER_RANK_TRACK);
		if (pg1 == null){
			MC.sendMessage(sender, "Couldnt find group " + r1);
			return;
		}
		if (pg2 == null){
			MC.sendMessage(sender, "Couldnt find group " + r2);
			return;
		}
		PermissionController.copyRankPerms(pg1,pg2);
		//		gc.copyGroups(guild1, (Guild) args[2], Defaults.PLAYER_RANK_TRACK);
		MC.sendMessage(sender, "&2Copied perms from &6" + pg1.getName() +"&2 to &6" + pg2.getName());
	}


	private boolean validGroupLabel(CommandSender sender, Guild guild, String name, boolean rank){
		String rOrT = rank ? "Rank" : "Title";
		String modname = MC.decolorChat(name.toLowerCase());
		if(!modname.matches("^[a-zA-Z][a-zA-Z0-9]+$")){
			MC.sendMessage(sender,rOrT+" require all letters and numbers.  Must start with a letter");
			return false;
		}
		if(modname.length()>Defaults.MAX_GUILD_NAME_LENGTH){
			MC.sendMessage(sender, rOrT+" was too long");
			return false;
		}
		PermissionGroup pg = PermissionController.findGroup(modname, guild, Defaults.PLAYER_RANK_TRACK);
		if(pg!=null){
			MC.sendMessage(sender, rOrT+" "+pg.getName() +" already exists");
			return false;
		}
		return true;
	}

	private boolean validGuildPlayerGroup(CommandSender sender, Guild guild, String name, int rank, boolean rankOrTitle){
		if (rankOrTitle){
			if (rank <= Defaults.MIN_RANK || rank > Defaults.MAX_RANK){
				MC.sendMessage(sender,"Ranks need to be within [2-1000]");
				return false;
			}
		}
		return validGroupLabel(sender,guild,name,rankOrTitle);
	}
}
