package com.mystic.guilds.controller;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.ExpressionList;
import com.mystic.guilds.Defaults;
import com.mystic.guilds.MysticGuilds;
import com.mystic.guilds.objects.Guild;
import com.mystic.guilds.objects.GuildPlayer;
import com.mystic.guilds.objects.permissions.GuildPermission;
import com.mystic.guilds.objects.permissions.PermissionGroup;
import com.mystic.guilds.util.FileLogger;

public class PermissionController {

	/// Our permissions for logged in players
	private static HashMap<String,PermissionAttachment> attachments = new HashMap<String,PermissionAttachment>();
	/// Valid permissions
	private static HashMap<String,GuildPermission> assignablePermissions = new HashMap<String,GuildPermission>();

	/// Our database connection
	public static EbeanServer db = null;

	public static PermissionGroup getRank(Set<PermissionGroup> groups, boolean highest) {
		if (groups == null || groups.isEmpty())
			return null;
		PermissionGroup res = null;
		for (PermissionGroup group : groups){
			if (group.getTrack() != Defaults.GUILD_RANK_TRACK && group.getTrack() != Defaults.PLAYER_RANK_TRACK)
				continue;
			if (res == null){
				res = group;
			} else if (highest){
				if (group.getRank() < res.getRank())
					res = group;
			} else {
				if (group.getRank() > res.getRank())
					res = group;
			}
		}
		return res;
	}

	public static List<PermissionGroup> getGroups(Guild guild, int track) {
		ExpressionList<PermissionGroup> el = null;
		if (Defaults.ALLOW_USERMADE_RANKS && guild != null){
			el = db.find(PermissionGroup.class).where().eq("guild",guild).eq("track", track);
		} 
		if (el == null || el.findRowCount() == 0) /// no user defined ranks, use the defaults
			el = db.find(PermissionGroup.class).where().eq("guild",null).eq("track", track);
		return el.findList();
	}
	public static List<PermissionGroup> getGroups(Guild guild) {
		ExpressionList<PermissionGroup> el = null;
		if (Defaults.ALLOW_USERMADE_RANKS && guild != null){
			el = db.find(PermissionGroup.class).where().eq("guild",guild);
		}
		if (el == null || el.findRowCount() == 0) /// no user defined ranks, use the defaults
			el = db.find(PermissionGroup.class).where().eq("guild",null);
		return el.findList();
	}

	public static PermissionGroup nextGroup(Guild guild, int rank, boolean above, int track) {
		ExpressionList<PermissionGroup> el =  null;
		TreeMap<Integer, PermissionGroup> tmap;
		//		System.out.println("cur rank = " + rank +"  guild = "+(guild != null ?guild.getName():"null") +"  track=" + track);
		PermissionGroup possibleGroup = null;
		if (!Defaults.ALLOW_USERMADE_RANKS){
			guild = null; /// Set them to the default ranks
		} else if (guild != null) {
			possibleGroup = nextGroup(null,rank,above,track);
		}

		if (above){
			el = db.find(PermissionGroup.class).where().lt("rank", rank)
					.eq("guild",guild).eq("track", track);
			tmap = new TreeMap<Integer,PermissionGroup>(Collections.reverseOrder());
		} else { 
			el = db.find(PermissionGroup.class).where().gt("rank", rank).
					eq("guild",guild).eq("track", track);
			tmap = new TreeMap<Integer,PermissionGroup>();
		}
		if (possibleGroup != null && el != null && el.findRowCount()==0)
			tmap.put(possibleGroup.getRank(), possibleGroup);
		for (PermissionGroup pg: el.findList()){
			//			System.out.println("treemap  " + pg);
			tmap.put(pg.getRank(), pg);
		}

		return tmap.isEmpty() ? null : tmap.firstEntry().getValue();
	}


	public static PermissionGroup getDraftGroup() {
		return getGroup(Defaults.CHARTER, null, Defaults.GUILD_RANK_TRACK);
	}
	public static PermissionGroup getMemberGroup(Guild guild) {
		if (Defaults.ALLOW_USERMADE_RANKS && guild != null){ /// Try and get a custom member group first
			PermissionGroup pg = getGroup(Defaults.MAX_RANK, guild, Defaults.PLAYER_RANK_TRACK);
			if (pg != null)
				return pg;
		} 			
		/// Return the Default Member group
		return getGroup(Defaults.MEMBER_GROUP, null, Defaults.PLAYER_RANK_TRACK);
	}

	public static PermissionGroup getLeaderGroup(Guild guild) {
		if (Defaults.ALLOW_USERMADE_RANKS && guild != null){ /// Try and get a custom Leader group first
			PermissionGroup pg = getGroup(Defaults.MIN_RANK,guild,Defaults.PLAYER_RANK_TRACK);
			if (pg != null)
				return pg;
		} 
		/// Return the Default Leader group
		return getGroup(Defaults.MIN_RANK,null,Defaults.PLAYER_RANK_TRACK);
	}

	public static PermissionGroup getGroup(int rankLvl,Guild guild, int track) {
		return db.find(PermissionGroup.class).where().
				eq("rank",rankLvl).eq("guild",guild).eq("track", track).findUnique();
	}

	public static PermissionGroup getGroup(String groupName,Guild guild, int track) {
		return db.find(PermissionGroup.class).where().
				ieq("name",groupName).eq("guild",guild).eq("track", track).findUnique();
	}


	public static PermissionGroup addPermissionsGroup(String name, String label, Guild guild, int rank, int track) {
		PermissionGroup group = db.find(PermissionGroup.class).where().
				ieq("name", name).eq("guild",guild).eq("track", track).
				findUnique();
		if (rank < Defaults.MIN_RANK || rank > Defaults.MAX_RANK){
			return null;}
		if (group == null){
			group = new PermissionGroup(name,label, guild, rank,track);
			group.setRank(rank);
			db.save(group);
		} else {
			group.setRank(rank);
			group.setLabel(label);
			db.save(group);
		}
		if (guild != null){
			if (!guild.hasGroup(group)){
				Guild newGuild = db.find(Guild.class,guild.getName());
				newGuild.addGroup(group);
				db.save(newGuild);
				db.saveManyToManyAssociations(newGuild, "groups");
			}
		}
		return group;
	}

	public static PermissionGroup addDefaultGroup(String name, String label, int rank, int track) {
		return addPermissionsGroup(name,label,null,rank,track);
	}
	public static PermissionGroup createGuildPlayerRank(String name, String label, int rank, Guild guild) {
		PermissionGroup pg = addPermissionsGroup(name,label,guild,rank, Defaults.PLAYER_RANK_TRACK);
		if (pg != null && guild != null){ /// Whatever rank they are custom creating.. dont let them take away less than min perms
			PermissionGroup def = getMemberGroup(null);
			//			System.out.println(" default member group = " + def.getName());
			for (GuildPermission p: def.getPermissions()){
				pg.addPermission(p);
			}
			/// By default the created rank has all the permissions of the previous rank
			PermissionGroup next = PermissionController.nextGroup(guild, rank, false, Defaults.PLAYER_RANK_TRACK);
			if (next != null && next != def){
				Set<GuildPermission> perms = pg.getPermissions();
				for (GuildPermission perm: next.getPermissions()){
					if (!perms.contains(perm))
						pg.addPermission(perm);
				}				
			}
			db.saveAssociation(pg, "permissions");
		}
		return pg;
	}
	public static PermissionGroup createGuildPlayerTitle(String name,String label, int rank, Guild guild) {
		return addPermissionsGroup(name,label,guild,rank, Defaults.PLAYER_TITLE_TRACK);		
	}

	public static void deleteGuildPlayerRank(String name, Guild guild){
		removeGroup(name,guild,Defaults.PLAYER_RANK_TRACK);
	}
	public static void deleteGuildPlayerTitle(String name, Guild guild){
		removeGroup(name,guild,Defaults.PLAYER_TITLE_TRACK);
	}
	public static void removeDefaultGroup(String name, int track){
		removeGroup(name,null,track);
	}
	public static void removeGroup(String name, Guild guild, int track){
		PermissionGroup group = db.find(PermissionGroup.class).where().
				ieq("name", name).eq("track", track).eq("guild",guild).findUnique();
		if (group == null || group.isLeader()){ /// cant remove the "leader" group
			return;
		}
		db.beginTransaction(); 
		try { 
			List<GuildPlayer> gps = db.find(GuildPlayer.class).where().eq("guild",guild).findList();
			PermissionGroup def = getMemberGroup(guild);
			for (GuildPlayer gp: gps){
				if (gp.hasGroup(group)){
					gp.removeGroup(group);
					gp.addGroup(def);
					db.update(gp);
					db.saveAssociation(gp, "groups");
				}
			}
			db.deleteManyToManyAssociations(group, "permissions");
			db.delete(group);
			db.commitTransaction();
		} finally {
			db.endTransaction(); 			
		}
	}
	public static void removeAllGroups(Guild guild, int track){
		List<PermissionGroup> groups = db.find(PermissionGroup.class).where().eq("track", track).eq("guild",guild).findList();

		db.beginTransaction(); 
		try { 
			for (PermissionGroup group: groups){
				guild.removeGroup(group);
				db.deleteManyToManyAssociations(guild, "groups");
				List<GuildPlayer> gps = db.find(GuildPlayer.class).where().eq("guild",guild).findList();
				for (GuildPlayer gp: gps){
					if (gp.hasGroup(group)){
						gp.removeGroup(group);
						db.update(gp);
						db.saveAssociation(gp, "groups");
					}
				}
				db.deleteManyToManyAssociations(group, "permissions");
				db.delete(group);
			}
			db.commitTransaction();
		} finally {
			db.endTransaction(); 			
		}
	}
	public static PermissionGroup findGroup(String name, Guild guild, int track){
		return db.find(PermissionGroup.class).where().ieq("name", name).eq("guild", guild).eq("track",track).findUnique();
	}

	public static void addPerm(String name, String node, boolean adminOnly) {
		GuildPermission perm = db.find(GuildPermission.class).where().ieq("name", name).findUnique();
		/// 
		//		System.out.println("------------------");
		//		System.out.println(name +"  " + node +"  " + adminOnly);
		if (perm == null){
			perm = new GuildPermission(name, node, adminOnly);
			db.save(perm);
		} else if (perm.isAdminOnly() != adminOnly){
			perm.setAdminOnly(adminOnly);
			db.save(perm);
		}
		if (!adminOnly){
			assignablePermissions.put(perm.getName().toLowerCase(), perm);			
		}
	}

	public static boolean addPermission(PermissionGroup group, String name) {
		GuildPermission perm = db.find(GuildPermission.class).where().ieq("name", name).findUnique();
		return addPermission(group, perm);
	}

	public static boolean addPermission(PermissionGroup group, GuildPermission perm) {
		if (group == null || perm == null)
			return false;

		group.addPermission(perm);
		db.save(group);
		db.saveManyToManyAssociations(group, "permissions");
		/// Update everyones permissions who had this group
		List<GuildPlayer> list = db.find(GuildPlayer.class).where().eq("groups", group).findList();
		for (GuildPlayer gp: list){
			updatePermissions(gp);
		}
		return true;
	}

	public static boolean removePermission(PermissionGroup group, String name) {
		GuildPermission perm = db.find(GuildPermission.class).where().ieq("name", name).findUnique();
		return removePermission(group, perm);
	}
	public static boolean removePermission(PermissionGroup group, GuildPermission perm) {
		if (group == null || perm == null)
			return false;
		group.removePermission(perm);
		db.save(group);
		db.saveAssociation(group, "permissions");
		/// Update everyones permissions who had this group
		List<GuildPlayer> list = db.find(GuildPlayer.class).where().eq("groups", group).findList();
		for (GuildPlayer gp: list){
			updatePermissions(gp);
		}
		return true;
	}

	public static boolean setLabel(PermissionGroup group, String label) {
		group.setLabel(label);
		db.save(group);
		return true;
	}


	/// We can't let someone set a rank higher or equal to a leader, or less than the default
	/// We also wont allow a leaders group rank to be changed(since that takes additional steps)
	public static boolean setRank(PermissionGroup group, Integer rank) {
		if (	rank <= Defaults.MIN_RANK || rank > Defaults.MAX_RANK ||  group.isLeader())
			return false;
		group = db.find(PermissionGroup.class).where().ieq("name", group.getName()).eq("guild", group.getGuild()).findUnique();
		if (group == null)
			return false;
		group.setRank(rank);
		db.save(group);
		return true;
	}

	public static Set<GuildPermission> getAllPerms() {
		return db.find(GuildPermission.class).findSet();
	}

	public static void removeAllPermissions(String groupName, Guild guild, int track) {
		PermissionGroup group = findGroup(groupName, guild, track);
		if (group!=null){
			group.setPermissions(null);
			db.save(group);
			db.deleteManyToManyAssociations(group, "permissions");
		}
	}

	public static void updatePermissions(GuildPlayer gp) {
		Player p = Bukkit.getServer().getPlayer(gp.getName());
		if (p == null){
			return;}
		updatePermissions(p,gp);
	}

	public static void updatePermissions(Player player) {
		GuildPlayer gp = MysticGuilds.getPlayer(player.getName());
		if (gp == null)
			return;
		updatePermissions(player, gp);
	}

	public static void updatePermissions(Player player, GuildPlayer gp){
		Set<GuildPermission> newPerms = gp.getPermissions();
		if (newPerms == null || newPerms.isEmpty()){
			unsetPermissions(player);
			return;
		}
		PermissionAttachment attachment = attachments.get(gp.getName());
		FileLogger.log("  getAttach" + player.getName()+" pa="+attachment);		
		if (attachment != null){
			try{
				player.removeAttachment(attachment);
			} catch(Exception e){
//				e.printStackTrace();
			}
		}
		attachment = player.addAttachment(MysticGuilds.getSelf());
		attachments.put(player.getName(), attachment);
		FileLogger.log("  puttingAttach" + player.getName()+" pa="+attachment);

		Map<String, Boolean> oldperms = attachment.getPermissions();
		HashSet<String> newStrPerm = new HashSet<String>();
		/// Give them the new perms
		for (GuildPermission perm : newPerms){
			attachment.setPermission(perm.getNode(), true);
			newStrPerm.add(perm.getNode());
		}
		FileLogger.log("updating for player " + player.getName() +"   a="+attachment +"  " +oldperms.size()+" " + newStrPerm.size());
		//		System.out.println("Old perms size  =" + oldperms.size() + attachment.getPermissions().size());
		/// Remove any oldperms that we dont current have
		//		for (String oldperm : oldperms.keySet()){
		//			if (!newStrPerm.contains(oldperm)){	
		//				attachment.unsetPermission(oldperm);
		//			} 
		//		}
	}

	public static PermissionAttachment getAttachment(Player p, GuildPlayer gp){
		PermissionAttachment pa = attachments.get(gp.getName());
		FileLogger.log("  getAttach" + p.getName()+" pa="+pa);
		if (pa == null){
			pa = p.addAttachment(MysticGuilds.getSelf());

			attachments.put(p.getName(), pa);
			FileLogger.log("  puttingAttach" + p.getName()+" pa="+pa);
		}
		return pa;
	}

	public static void unsetPermissions(Player p) {
		PermissionAttachment pa = attachments.get(p);
		FileLogger.log("  unsetting perms for player " + p.getName() +"   a="+pa);
		if (pa != null){
			p.removeAttachment(pa);
		}
	}

	public static int getGuildPlayerGroupCount(Guild guild) {
		int count =0;
		ExpressionList<PermissionGroup> el =db.find(PermissionGroup.class).where().eq("guild", guild).eq("track",Defaults.PLAYER_RANK_TRACK);
		if (el != null)
			count += el.findRowCount();
		el =db.find(PermissionGroup.class).where().eq("guild", guild).eq("track",Defaults.PLAYER_TITLE_TRACK);
		if (el != null)
			count += el.findRowCount();
		return count;
	}

	public static GuildPermission assignablePermission(String perm) {
		//		System.out.println(" perms =" + assignablePermissions.size());
		//		for (String s : assignablePermissions.keySet()){
		//			System.out.println(" perms =" + s);
		//		}
		GuildPermission gperm = assignablePermissions.get(perm);
		if (gperm == null)
			return null;
		return db.find(GuildPermission.class,gperm.getId());
	}

	public static void relabel(PermissionGroup pg, String label) {
		pg.setLabel(MC.colorChat(label));
		pg.setName(MC.decolorChat(label));
		db.save(pg);
	}

	public static void copyRankPerms(PermissionGroup pg1, PermissionGroup pg2) {
		for (GuildPermission perm : pg1.getPermissions()){
			pg2.addPermission(perm);
		}
		db.save(pg2);
		db.saveManyToManyAssociations(pg2, "permissions");
	}



}
