package com.mystic.guilds.executors;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.mystic.guilds.Defaults;
import com.mystic.guilds.MysticGuilds;
import com.mystic.guilds.controller.GuildController;
import com.mystic.guilds.controller.MC;
import com.mystic.guilds.controller.MoneyController;
import com.mystic.guilds.controller.PermissionController;
import com.mystic.guilds.objects.Guild;
import com.mystic.guilds.objects.GuildPlayer;
import com.mystic.guilds.objects.ReturnValue;
import com.mystic.guilds.objects.permissions.PermissionGroup;

public class GuildExecutor extends CustomCommandExecutor {
	GuildController gc;

	public GuildExecutor(MysticGuilds instance, GuildController guildController){
		super();
		CustomCommandExecutor.setPluginInstance(instance);
		this.gc = guildController;
	}

	@Override
	public void showHelp(CommandSender sender, Command command){
		help(sender,null,command,null,null);
	}

	@MCCommand( cmds = {"help","?"})
	public void help(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		super.help(sender, command, args);
	}

	private boolean validGuildName(CommandSender sender, String name){
		name = name.toLowerCase();
		if(!name.matches("^[a-zA-Z][a-zA-Z0-9]+$")){
			MC.sendMessage(sender, MC.getMessage("main", "guild_name_requires_all_letters"));
			return false;
		}
		if(name.length()>Defaults.MAX_GUILD_NAME_LENGTH){
			MC.sendMessage(sender, MC.getMessage("main", "guild_name_max_length",Defaults.MAX_GUILD_NAME_LENGTH));
			return false;
		}
		Guild guild = MysticGuilds.getGuild(name);
		if(guild!=null){
			MC.sendMessage(sender, MC.getMessage("main", "guild_name_exists", name));
			return false;
		}
		return true;
	}

	@MCCommand( cmds = {"draft"}, notInGuild={SELF}, min=2)
	public void draftGuild(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		String name = (String) args[1];
		if (!validGuildName(sender, name))
			return;
		if(gp.getGuild()!=null){
			MC.sendMessage(sender, MC.getMessage("main", "guild_already_committed", gp.getGuild().getName()));
			return;
		}
		gc.guildDraft(gp, name);
		MC.sendMessage(sender, MC.getMessage("success", "guild_draft_success"));
	}

	@MCCommand( cmds = {"draft"}, notInGuild={2}, min=3,
			usage="/guild draft <guild name> <leader>", op=true, order=1)
	public void draftGuildOther(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		GuildPlayer founder = (GuildPlayer)  args[2];
		gc.guildDraft(founder, (String) args[1]);
		MC.sendMessage(sender, MC.getMessage("success", "guild_draft_success"));
	}

	@MCCommand( cmds = {"found","create"}, guild={1}, min=2, usage="/guild create <guild>", op=true)
	public void foundGuildOther(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		Guild g = (Guild) args[1];
		gc.guildCreate(g);
		plugin.broadcast(g, MC.getMessage("success", "created_guildbroadcast", g.getName()), gp);
		MC.sendMessage(sender, MC.getMessage("success", "created_you_have", g.getName()));
	}

	@MCCommand( cmds = {"found","create"}, inGuild={SELF}, perm="guild.found")
	public void foundGuild(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		Guild guild = gp.getGuild();
		if(!guild.isCharter()){
			MC.sendMessage(sender,ChatColor.RED+"You already have founded");
			return;
		}
		if(guild.getMembers().size()<plugin.getConfig().getInt("minmembers", Defaults.MIN_MEMBERS)){
			MC.sendMessage(sender,ChatColor.RED+"Your charter does not have enough signers !");
			return;
		}
		float guildMoney = guild.getFunds();
		if(guildMoney<Defaults.GUILD_COST){
			MC.sendMessage(sender,ChatColor.RED+"Your charter does not have enough money!");
			return;
		}
		gc.guildWithdraw(guild, Defaults.GUILD_COST);
		gc.guildCreate(guild);

		MC.sendMessage(sender, MC.getMessage("success", "created_you_have", guild.getName()));
		plugin.broadcast(guild, MC.getMessage("success", "created_guildbroadcast", guild.getName()), gp);
		plugin.getServer().broadcastMessage(MC.getMessage("success", "created_serverbroadcast",guild.getName()));
	}

	@MCCommand(cmds = {"money","funds"}, inGuild={SELF}, max=1)
	public void guildMoneySelf(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		sendGuildMoney(sender, gp,gp.getGuild());
	}

	@MCCommand(cmds={"money","funds"}, guild={1}, max=2)
	public void guildMoneyOther(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		sendGuildMoney(sender, gp,(Guild) args[1]);
	}

	private void sendGuildMoney(CommandSender sender,GuildPlayer gp, Guild guild) {
		MC.sendMessage(sender,ChatColor.GREEN+"Funds: "+ChatColor.YELLOW+guild.getFunds()+((guild.isCharter()) ? ChatColor.GREEN+"/"+ChatColor.YELLOW+Defaults.GUILD_COST : ""));
	}

	@MCCommand(cmds = {"info"}, inGuild={SELF}, max=2)
	public void guildInfoSelf(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		sendGuildInfo(sender, gp,gp.getGuild());
	}

	@MCCommand(cmds = {"info"}, guild={1}, max=2, order=1)
	public void guildInfoOther(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		sendGuildInfo(sender, gp,(Guild) args[1]);
	}

	private void sendGuildInfo(CommandSender sender, GuildPlayer gp, Guild guild){
		PermissionGroup group = guild.getRankGroup();
		if (group == null){ /// This really shouldnt happen, they should always be set to some group
			group = PermissionController.getDraftGroup();
		}
		String label = ((!guild.getLabel().equals(guild.getName())) ? ChatColor.GREEN+"("+ChatColor.YELLOW+guild.getLabel()+ChatColor.GREEN+")": guild.getLabel());
		PermissionGroup leader = PermissionController.getLeaderGroup(guild);
		String leaderOrDrafter = ((guild.isCharter()) ? "Drafter" : leader.getLabel());
		try{MC.sendMessage(sender, MC.getMessage("success", "guild_info_header", group.getLabel()));} catch(Exception e){e.printStackTrace();}
		try{MC.sendMessage(sender, MC.getMessage("success", "guild_info_header2", label));} catch(Exception e){e.printStackTrace();}
		try{MC.sendMessage(sender, MC.getMessage("success", "guild_info_leader", leaderOrDrafter,guild.getLeader().getName()));} catch(Exception e){e.printStackTrace();}


		if(guild.getFounder().getName()!=guild.getLeader().getName()){
			MC.sendMessage(sender, MC.getMessage("success", "guild_info_founder", guild.getFounder().getName()));
		}
		/// Make a list of members by rank
		TreeMap<PermissionGroup,List<GuildPlayer>> tree = new TreeMap<PermissionGroup,List<GuildPlayer>>(new Comparator<PermissionGroup>(){
			@Override
			public int compare(PermissionGroup p1, PermissionGroup p2) {
				if (p1.getRank() != p2.getRank())
					return ((Integer)p1.getRank()).compareTo(p2.getRank());
				return p1.getName().compareTo(p2.getName());
			}
		});
		for(GuildPlayer member:guild.getMembers()){
			PermissionGroup pg = PermissionController.getRank(member.getGroups(), true);
			if (pg == null){ /// This really shouldnt happen
				PermissionGroup dg = PermissionController.getMemberGroup(guild);
				gc.playerAddGroup(member, dg);
				System.err.println(member.getName() + "  didnt have the default group!!!");
				pg = dg;
			}
			List<GuildPlayer> membersAtRank = tree.get(pg);
			if (membersAtRank == null){
				membersAtRank = new ArrayList<GuildPlayer>();
				tree.put(pg, membersAtRank);
			}
			membersAtRank.add(member);
		}
		/// Iterate over the members and make the list
		for (PermissionGroup r : tree.keySet()){
			List<GuildPlayer> members = tree.get(r);
			ChatColor rankColor = r.getColor();
			StringBuilder sb = new StringBuilder();
			int count = 0;
			boolean first = true;
			for(GuildPlayer member:members){
				if (!first) sb.append(", ");
				first = false;
				count++;
				/// Deal with titles
				Set<PermissionGroup> titles = member.getTitles();
				StringBuilder sb2 = new StringBuilder();
				if (!titles.isEmpty()){
					sb.append("[");
					boolean first2=true;
					for (PermissionGroup title: titles){
						if (!first2) sb2.append(",");
						first2 = false;
						sb2.append(title.getLabel()+"");
					}
					sb2.append(rankColor+"]");
				}
				/// Append the titles+name
				sb.append(rankColor+sb2.toString()+member.getName());
			}
			String str =null;
			if (guild.isCharter()){
				str= "&2Signers(&e"+count+"&2/&e"+Defaults.MIN_MEMBERS+"&2): "+sb.toString();
			} else {
				str= r.getLabel()+"(&e"+count+r.getColor()+"): &2"+sb.toString();
			}
			MC.sendMessage(sender,str);
		}
		sendGuildMoney(sender,gp, guild);
		RelationshipExecutor.sendGuildRelationships(gc,sender,gp,guild);
	}

	@MCCommand( cmds = {"invite"}, perm="guild.invite", guild={1}, notInGuild={2}, op=true, max=3)
	public void inviteGuildOther(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		invite(sender, gp, (Guild) args[1], (GuildPlayer) args[2]);
	}

	@MCCommand( cmds = {"invite"}, perm="guild.invite", inGuild={SELF}, notInGuild={1})
	public void inviteGuild(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		Guild guild = gp.getGuild();
		GuildPlayer invite = (GuildPlayer) args[1];
		if(MysticGuilds.useFactions){
			String test1 = gp.getGuild().getFaction();
			String test2 = MysticGuilds.factions.getPlayer(invite.getName()).getFaction().toString();
			if(!test1.equals(test2)){
				MC.sendMessage(sender,ChatColor.RED+"You cannot invite players from an opposing faction.");
				return;
			}
		}

		invite(sender, gp, guild, invite);
	}

	private void invite(CommandSender sender, GuildPlayer gp, Guild guild, GuildPlayer invite) {
		gc.guildInvite(guild, invite);

		OfflinePlayer invited = plugin.getServer().getOfflinePlayer(invite.getName());
		MC.sendMessage(sender, MC.getMessage("success", "invite_you_have", invite.getName()));
		if(invited != null && invited.isOnline()){
			if (guild.isCharter()){
				MC.sendMessage(invited, MC.getMessage("success", "invite_charter_you_have_been",guild.getName()));
			} else {
				MC.sendMessage(invited, MC.getMessage("success", "invite_guild_you_have_been",guild.getName()));
			}
		}
	}

	@MCCommand( cmds = {"uninvite"}, perm="guild.uninvite", inGuild={SELF})
	public void uninviteGuild(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		Guild guild = gp.getGuild();
		GuildPlayer uninvite = (GuildPlayer) args[1];
		if(!guild.getInvites().contains(gp)){
			MC.sendMessage(sender,ChatColor.RED+uninvite.getName()+" has not been invited to "+((gp.getGuild().isCharter())? "sign your charter.":"join."));
			return;
		}
		gc.uninvite(guild,uninvite);
		MC.sendMessage(sender, MC.getMessage("success", "uninvite_you_have", uninvite.getName()));
		MC.sendMessage(uninvite, MC.getMessage("success", "uninvite_you_have"));
	}

	@MCCommand( cmds = {"join"}, guild={1}, notInGuild={2}, op=true, min=3)
	public void joinGuildOther(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		join(sender, (GuildPlayer) args[2], (Guild) args[1] );
	}

	@MCCommand( cmds = {"join"}, notInGuild={SELF}, guild={1}, min=2)
	public void joinGuild(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		Guild guild = (Guild) args[1];

		if(!gp.getInvites().contains(guild)){
			MC.sendMessage(sender,ChatColor.RED+"You have not been invited to "+((guild.isCharter()) ? "sign this charter." : "join this guild"));
			return;
		}
		if(MysticGuilds.useFactions){
			String test1 = guild.getFaction();
			String test2 = MysticGuilds.factions.getPlayer(gp.getName()).getFaction().toString();
			if(!test1.equals(test2)){
				MC.sendMessage(sender, ChatColor.RED+"You cannot join a guild in an opposing faction.");
				return;
			}
		}
		join(sender, gp, guild);
	}

	private boolean join(CommandSender sender, GuildPlayer gp, Guild guild) {
		ReturnValue rv = gc.guildJoin(guild, gp);
		if (rv.getMsg() != null)
			MC.sendMultilineMessage(sender, rv.getMsg());
		if (rv.getServerMsg() != null)
			MC.broadcastMultilineMessage(rv.getServerMsg());
		switch (rv.val) {
		case True:
			if (rv.getMsg() == null) {
				if (guild.isCharter()){
					MC.sendMessage(sender, MC.getMessage("success", "join_charter_you_have"));
					plugin.broadcast(guild, MC.getMessage("success", "join_charter_broadcast", gp.getName()));
				} else {
					MC.sendMessage(sender, MC.getMessage("success", "join_guild_you_have"));
					plugin.broadcast(guild, MC.getMessage("success", "join_guild_broadcast", gp.getName()));
				}
			}
			break;
		case False:
			MC.sendMessage(sender, "&4Error joining ");
			break;
		case Cancelled:
			break;
		}

		return true;
	}

	@MCCommand( cmds = {"leave"}, inGuild = {SELF}, perm="guild.leave")
	public void leaveGuild(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		Guild guild = gp.getGuild();
		if(guild.getLeader()==gp){
			MC.sendMessage(sender, ChatColor.RED+"You must assign a new leader before you can leave.");
			return;
		}
		gc.guildLeave(gp);
		if (guild.isCharter()){
			MC.sendMessage(sender, MC.getMessage("success", "leave_charter_you_have"));
			plugin.broadcast(guild, MC.getMessage("success", "leave_charter_broadcast",gp.getName()));

		} else {
			MC.sendMessage(sender, MC.getMessage("success", "leave_guild_you_have"));
			plugin.broadcast(guild, MC.getMessage("success", "leave_guild_broadcast",gp.getName()));
		}
	}

	@MCCommand( cmds = {"kick"}, inGuild = {SELF,1}, perm="guild.kick", sameGuild=true)
	public void kickGuild(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		GuildPlayer kick = (GuildPlayer) args[1];
		if(kick==gp){
			MC.sendMessage(sender,ChatColor.RED+"You cannot kick yourself.");
			return;
		}
		if(!gp.higherRank(kick)){
			MC.sendMessage(sender, ChatColor.RED+"You cannot kick someone of the same or greater rank.");
			return;
		}
		gc.guildKick(kick);
		MC.sendMessage(sender, MC.getMessage("success", "kick_you_have", kick.getName()));
		plugin.broadcast(gp.getGuild(), MC.getMessage("success", "kick_guildbroadcast", kick.getName()));
		MC.sendMessage(kick, MC.getMessage("success", "kick_you_have_been"));
	}

	@MCCommand( cmds = {"kick"}, inGuild = {1}, op=true, order=1)
	public void kickGuildOther(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		GuildPlayer kick = (GuildPlayer) args[1];
		gc.guildKick(kick);
		MC.sendMessage(sender, MC.getMessage("success", "kick_you_have", kick.getName()));
		plugin.broadcast(kick.getGuild(), MC.getMessage("success", "kick_guildbroadcast", kick.getName()));
		MC.sendMessage(kick, MC.getMessage("success", "kick_you_have_been"));
	}

	@MCCommand( cmds = {"deposit"}, inGuild={SELF}, ints={1}, perm="guild.deposit", max=2)
	public void depositGuild(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		Guild guild = gp.getGuild();
		int deposit = (Integer) args[1];
		if (deposit <=0){
			MC.sendMessage(sender,ChatColor.RED+"You can't deposit a negative amount!");
			return;
		}
		if(!MoneyController.hasEnough(gp.getName(), deposit)){
			MC.sendMessage(sender,ChatColor.RED+"You don't have that much money!");
			return;
		}
		MoneyController.subtract(gp.getName(), deposit);
		gc.guildDeposit(guild, deposit);
		MC.sendMessage(sender, MC.getMessage("success", "deposit_you_have", deposit));
		plugin.broadcast(guild, MC.getMessage("success", "deposit_guildbroadcast", gp.getName(),deposit), gp);
	}

	@MCCommand( cmds = {"deposit"}, guild={1}, ints={2}, op=true, max=3)
	public void depositGuildOther(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		Guild g = (Guild) args[1];
		Integer deposit = (Integer) args[2];
		gc.guildDeposit(g,deposit);
		MC.sendMessage(sender, MC.getMessage("success", "deposit_you_have", deposit));
	}

	@MCCommand( cmds = {"withdraw"}, inGuild={SELF}, perm="guild.withdraw", ints={1})
	public void withdrawGuild(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		int withdraw = (Integer) args[1];
		Guild guild = gp.getGuild();
		if (withdraw <=0){
			MC.sendMessage(sender,ChatColor.RED+"You can't withdraw a negative amount!");
			return;
		}

		if(guild.getFunds() < withdraw){
			MC.sendMessage(sender,ChatColor.RED+"Your guild doesn't have that much money!");
			return;
		}
		MoneyController.add(gp.getName(), withdraw);
		gc.guildWithdraw(guild, withdraw);
		MC.sendMessage(sender, MC.getMessage("success", "withdrawn_you_have", withdraw));
		plugin.broadcast(guild, MC.getMessage("success", "withdrawn_guildbroadcast", gp.getName(),withdraw), gp);
	}

	@MCCommand( cmds = {"withdraw"}, guild={1}, ints={2}, op=true, max=3)
	public void withdrawGuildOther(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		Guild guild = (Guild) args[1];
		float withdraw = (Integer) args[2];
		if(guild.getFunds() < withdraw){
			withdraw = guild.getFunds(); /// set them to 0
		}
		gc.guildWithdraw(guild, withdraw);
		MC.sendMessage(sender, MC.getMessage("success", "withdrawn_you_have", withdraw));
	}

	@MCCommand( cmds = {"disband"}, guild={1}, op=true,max=2)
	public void disbandGuildOther(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		disbandGuild(sender, gp,(Guild) args[1]);
	}

	@MCCommand( cmds = {"disband"}, inGuild={SELF}, perm="guild.disband", max=1)
	public void disbandGuildSelf(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		disbandGuild(sender, gp,gp.getGuild());
	}

	private void disbandGuild(CommandSender sender, GuildPlayer gp, Guild guild) {
		String msg = MC.getMessage("success", (guild.isCharter() ? "charter_disbanded": "guild_disbanded"), guild.getName());

		if (gc.guildDisband(guild)){
			plugin.broadcast(guild, msg,gp); /// tell the guild
			MC.sendMessage(sender, msg); /// tell the sender
		} else {
			MC.sendMessage(sender, ChatColor.RED+"An error occurred trying to disband " + guild.getName());
		}
	}

	@MCCommand( cmds = {"label","setlabel"}, inGuild={SELF}, perm="guild.setlabel", min=2)
	public void labelGuild(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		Guild guild = gp.getGuild();
		if(guild.isCharter()){
			MC.sendMessage(sender,ChatColor.RED+"You can't set the label in a charter.");
			return;
		}
		String newlabel = MC.decolorChat((String)args[1]);
		if(!newlabel.equalsIgnoreCase(guild.getName())){
			MC.sendMessage(sender,ChatColor.RED+"Your label must be a variation of your name.");
			return;
		}
		gc.guildSetLabel(guild, (String) args[1]);
		MC.sendMessage(sender,MC.getMessage("success", "guild_relabeled", guild.getLabel()));
	}

	@MCCommand( cmds = {"label","setlabel"}, guild={1}, min=3,op = true,order=1,
			usage="/guild setlabel <guild> <label>")
	public void labelGuildOther(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		Guild guild = (Guild) args[1];
		String oldlabel = guild.getLabel();
		gc.guildSetLabel(guild, args[2].toString());
		MC.sendMessage(sender,MC.getMessage("success", "guild_relabeled_other", oldlabel, guild.getLabel()));
	}

	@MCCommand( cmds = {"createplayer","cp"}, min=2,op = true)
	public void createPlayer(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		plugin.process(args[1].toString());
		MC.sendMessage(sender,ChatColor.GREEN +"GuildPlayer created: " + args[1]);
	}

	@MCCommand( cmds = {"who"}, min=1)
	public void guildWho(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		Player[] players = Bukkit.getServer().getOnlinePlayers();
		HashMap<String,Player> online = new HashMap<String, Player>();
		Map<Guild, HashSet<GuildPlayer>> guilds = new HashMap<Guild, HashSet<GuildPlayer>>();
		for (Player player: players){
			online.put(player.getName(), player);
			GuildPlayer guildp = MysticGuilds.getPlayer(player);

			Guild g = guildp.getGuild();
			HashSet<GuildPlayer> ps = guilds.get(g);
			if (ps == null){
				ps = new HashSet<GuildPlayer>();
				guilds.put(g, ps);
			}
			ps.add(guildp);
		}

		StringBuilder sb1 = new StringBuilder();
		StringBuilder sb2 = new StringBuilder();
		StringBuilder sb;
		sb1.append(ChatColor.GRAY +"Online (" + players.length +"/" + Bukkit.getServer().getMaxPlayers() +"):");
		boolean first = true, first2 = true;
		for (Guild guild: guilds.keySet()){
			sb = guild == null ? sb2 : sb1;
			label = guild == null ? Defaults.DEFAULT_NOGUILD : guild.getLabel();
			if (!first) sb.append(ChatColor.WHITE +", ");
			sb.append("&6("+label+": &f("+guilds.get(guild).size()+")");
			first2 = true;
			for (GuildPlayer s : guilds.get(guild)){
				if (!first2) sb.append(", ");
				sb.append(s.getName());
				first2 = false;
			}
			sb.append("&6)");
		}

		MC.sendMessage(sender, sb1.toString()+sb2.toString());
	}

	@MCCommand( cmds = {"setPVP", "pvp"}, op=true,min=2, order=1)
	public void guildSetPVP(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		String on = (String) args[1];
		Boolean enable = null;
		if (on.equalsIgnoreCase("on") || on.equalsIgnoreCase("enable"))
			enable = true;
		else if (on.equalsIgnoreCase("off") || on.equalsIgnoreCase("disable"))
			enable = false;
		if (enable == null){
			MC.sendMessage(sender, ChatColor.RED + "Valid options are &6[on|off]");
			return;
		}
		boolean oldValue = Defaults.CLAN_PVP_ON;
		Defaults.CLAN_PVP_ON = enable;
		MC.sendMessage(sender, ChatColor.GREEN + "PVP set &6" + enable + " "+ChatColor.GREEN+" was " + oldValue);
	}


	@MCCommand( cmds = {"addPlayer"}, op=true,min=2)
	public void addPlayer(CommandSender sender, GuildPlayer gp, Command command, String label, Object[] args){
		plugin.process((String) args[1]);
		MC.sendMessage(sender, "&ePlayer &c" + args[1] +"&e created");
	}

}
