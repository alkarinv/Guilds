package com.mystic.guilds.serializer;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import com.avaje.ebean.EbeanServer;
import com.mystic.guilds.Defaults;
import com.mystic.guilds.controller.PermissionController;
import com.mystic.guilds.objects.permissions.GuildPermission;
import com.mystic.guilds.objects.permissions.PermissionGroup;
/**
 * 
 * @author alkarin
 *
 */
public class ConfigSerializer {
	public static YamlConfiguration config;
	static File f = null;

	public ConfigSerializer(){

	}

	public static void setConfig(File f){
		ConfigSerializer.f = f;
		config = new YamlConfiguration();
	}

	public static void loadAll(EbeanServer db){
		try {config.load(f);} catch (Exception e){e.printStackTrace();}
		Defaults.MIN_MEMBERS = config.getInt("guilds.minMembersForCreation", Defaults.MIN_MEMBERS);
		Defaults.GUILD_COST = (float) config.getDouble("guilds.costToCreate", Defaults.GUILD_COST);
		Defaults.MAX_GUILD_NAME_LENGTH = config.getInt("guilds.maxNameLength", Defaults.MAX_GUILD_NAME_LENGTH);
		Defaults.ALLOW_USERMADE_RANKS = config.getBoolean("guilds.allowPerGuildRanks", Defaults.ALLOW_USERMADE_RANKS);
		Defaults.MAX_RANKS_PER_GUILD= config.getInt("guilds.customizableRanksPerGuild", Defaults.MAX_RANKS_PER_GUILD);

		setupPermissions(db, config.getConfigurationSection("permissions"));
		setupRanks(db, config.getConfigurationSection("defaultRanks"), Defaults.PLAYER_RANK_TRACK);
		setupRanks(db, config.getConfigurationSection("defaultGuildRanks"), Defaults.GUILD_RANK_TRACK);
		copyPermsToCustomGroups(db, Defaults.PLAYER_RANK_TRACK);
	}

	private static void copyPermsToCustomGroups(EbeanServer db, int track) {
		PermissionGroup group = db.find(PermissionGroup.class).where().eq("guild", null).
				eq("track",track).eq("rank", Defaults.MIN_RANK).findUnique();
		HashMap<String,GuildPermission> perms = new HashMap<String,GuildPermission>();
		HashSet<GuildPermission> hperms = new HashSet<GuildPermission>();
		for (GuildPermission perm: group.getPermissions()){
			perms.put(perm.getName(),perm);
			hperms.add(perm);
		}
		List<PermissionGroup> groups = db.find(PermissionGroup.class).where().ne("guild", null).
				eq("track",track).eq("rank", Defaults.MIN_RANK).findList();
		boolean changed = false;
		for (PermissionGroup g: groups){
			for (GuildPermission perm: g.getPermissions()){
				if (!hperms.contains(perm)){
//					System.out.println(g + "  Sorry bro, you no longer get the perm " + perm.getName());
				}
			}
			for (GuildPermission perm : hperms){
				if (!g.getPermissions().contains(perm)){
					changed = true;
//					System.out.println(g + " group doenst have perm that they should " + perm.getName());
					g.addPermission(perm);
				}
			}
			
			if (changed){
				db.save(g);
				db.saveManyToManyAssociations(g, "permissions");
			}
			changed = false;
		}
	}

	private static void setupRanks(EbeanServer db,ConfigurationSection cs, int track) {
		Set<String> groupNames = cs.getKeys(false);
		/// Get a set of all perms
		Set<GuildPermission> allPermissions = PermissionController.getAllPerms();
		Set<String> allPerms = new HashSet<String>();
		for (GuildPermission p: allPermissions){
			allPerms.add(p.getName());}

		/// Add groups and perms to groups
		for (String groupName: groupNames){
			ConfigurationSection rs = cs.getConfigurationSection(groupName);
			if (!rs.contains("rank")){
				System.err.println("Rank must be specified for " + groupName);
				continue;
			}
			Integer rank = rs.getInt("rank");
			String label = rs.getString("label", groupName);
			//			System.out.println("rankName = " + groupName +"  " + rank );
			/// Add our group
			PermissionController.addDefaultGroup(groupName.toLowerCase(), label, rank,track);
			/// First remove all permissions
			PermissionController.removeAllPermissions(groupName, null, track);
			PermissionGroup group = PermissionController.findGroup(groupName, null, track);
			/// Add our permissions
			List<String> perms = rs.getStringList("permissions");
			for (String perm: perms){
				//				System.out.println(groupName + " Should have perm " + perm);
				PermissionController.addPermission(group, perm);
			}
			if (rs.contains("customPermissions")){
				ConfigurationSection pcs = rs.getConfigurationSection("customPermissions");
				Set<String> cperms = pcs.getKeys(false);
				for (String perm: cperms){
					//					System.out.println(groupName + " Should have custom perm " + perm +"    "+config.getBoolean("permissions."+perm,false));
					PermissionController.addPerm(perm, pcs.getString(perm), !config.getBoolean("permissions."+perm,false));
				}
			}
		}
	}

	private static void setupPermissions(EbeanServer db, ConfigurationSection cs) {
		PermissionController.addPerm("invite", "guild.invite", !cs.getBoolean("invite", true));
		PermissionController.addPerm("uninvite", "guild.uninvite",!cs.getBoolean("uninvite", true));
		PermissionController.addPerm("deposit", "guild.deposit",!cs.getBoolean("deposit", true));
		PermissionController.addPerm("withdraw", "guild.withdraw",!cs.getBoolean("withdraw", true));
		PermissionController.addPerm("promote", "guild.promote",!cs.getBoolean("promote", true));
		PermissionController.addPerm("demote", "guild.demote",!cs.getBoolean("demote", true));
		PermissionController.addPerm("kick", "guild.kick",!cs.getBoolean("kick", true));
		PermissionController.addPerm("setleader", "guild.setleader",!cs.getBoolean("setleader", true));
		PermissionController.addPerm("setlabel", "guild.setlabel",!cs.getBoolean("setlabel", true));
		PermissionController.addPerm("create", "guild.found",!cs.getBoolean("found", true));
		PermissionController.addPerm("disband", "guild.disband",!cs.getBoolean("disband", true));
		PermissionController.addPerm("leave", "guild.leave",!cs.getBoolean("leave", true));
		PermissionController.addPerm("upgrade", "guild.upgrade",!cs.getBoolean("upgrade", false));
		PermissionController.addPerm("downgrade", "guild.downgrade",!cs.getBoolean("downgrade", false));

		PermissionController.addPerm("createRank","guild.createRank",!cs.getBoolean("createRank",true));
		PermissionController.addPerm("deleteRank","guild.deleteRank",!cs.getBoolean("deleteRank",true));
		PermissionController.addPerm("createTitle","guild.createTitle",!cs.getBoolean("createTitle",true));
		PermissionController.addPerm("deleteTitle","guild.deleteTitle",!cs.getBoolean("deleteTitle",true));
		PermissionController.addPerm("addRankPerm","guild.addRankPerm",!cs.getBoolean("addRankPerm",true));
		PermissionController.addPerm("deleteRankPerm","guild.deleteRankPerm",!cs.getBoolean("deleteRankPerm",true));
		PermissionController.addPerm("addTitlePerm","guild.addTitlePerm",!cs.getBoolean("addTitlePerm",true));
		PermissionController.addPerm("deleteTitlePerm","guild.deleteTitlePerm",!cs.getBoolean("deleteTitlePerm",true));
		PermissionController.addPerm("addRankTitle","guild.addRankTitle",!cs.getBoolean("addRankTitle",true));
		PermissionController.addPerm("deleteRankTitle","guild.deleteRankTitle",!cs.getBoolean("deleteRankTitle",true));
		PermissionController.addPerm("setRankLabel","guild.setRankLabel",!cs.getBoolean("setRankLabel",true));
		PermissionController.addPerm("setTitleLabel","guild.setTitleLabel",!cs.getBoolean("setTitleLabel",true));
		PermissionController.addPerm("setHome","guild.sethome",!cs.getBoolean("sethome",true));
		PermissionController.addPerm("home","guild.home",!cs.getBoolean("home",true));
		PermissionController.addPerm("relationships","guild.relationships",!cs.getBoolean("relationships",true));
		PermissionController.addPerm("alterChest","guild.alterChest",!cs.getBoolean("alterChest",true));
		PermissionController.addPerm("alterChunk","guild.alterChunk",!cs.getBoolean("alterChunk",true));
		PermissionController.addPerm("claim","guild.claim",!cs.getBoolean("claim",true));

	}

}
