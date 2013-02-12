package com.mystic.guilds.controller;

import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.milkbowl.vault.permission.Permission;

import org.bukkit.entity.Player;

import com.avaje.ebean.CallableSql;
import com.avaje.ebean.EbeanServer;
import com.mystic.guilds.Defaults;
import com.mystic.guilds.MysticGuilds;
import com.mystic.guilds.events.GuildChangeRelationshipEvent;
import com.mystic.guilds.events.GuildCreate;
import com.mystic.guilds.events.GuildDisband;
import com.mystic.guilds.events.GuildEvent;
import com.mystic.guilds.events.GuildPlayerJoin;
import com.mystic.guilds.events.GuildPlayerKick;
import com.mystic.guilds.events.GuildPlayerLeave;
import com.mystic.guilds.objects.Guild;
import com.mystic.guilds.objects.GuildPlayer;
import com.mystic.guilds.objects.Relationship;
import com.mystic.guilds.objects.Relationship.RelationshipType;
import com.mystic.guilds.objects.ReturnValue;
import com.mystic.guilds.objects.ReturnValue.Value;
import com.mystic.guilds.objects.permissions.GuildPermission;
import com.mystic.guilds.objects.permissions.PermissionGroup;

public class GuildController {
	EbeanServer db;

	HashMap<String, Map<String, Relationship>> relationshipRequests = new HashMap<String, Map<String, Relationship>>();

	public GuildController(EbeanServer db) {
		this.db = db;
	}

	public GuildPlayer getPlayer(Player p) {
		return getPlayer(p.getName());
	}

	public GuildPlayer getPlayer(String p) {
		return db.find(GuildPlayer.class, p);
	}

	public GuildPlayer matchPlayer(String p) {
		GuildPlayer gp = getPlayer(p);
		if (gp != null)
			return gp;
		List<GuildPlayer> list = db.find(GuildPlayer.class).where().ilike("name", p + "%").findList();
		return list != null && list.size()==1 ? list.get(0) : null;
	}

	public Guild getGuild(String name) {
		return db.find(Guild.class).where().ilike("name", name).findUnique();
	}

	public Guild getGuild(Player p) {
		GuildPlayer gp = getPlayer(p);
		if (gp == null || gp.getGuild() == null)
			return null;
		return gp.getGuild();
	}

	public Guild guildDraft(GuildPlayer gp, String name) {
		Guild guild = new Guild(gp, name);
		if (MysticGuilds.useFactions) {
			guild.setFaction(MysticGuilds.factions.getPlayer(gp.getName())
					.getFaction().toString());
		} else {
			guild.setFaction("");
		}
		PermissionGroup charter = PermissionController.getDraftGroup();
		PermissionGroup leader = PermissionController.getLeaderGroup(null);
		guild.setLabel("");
		guild.setGroups(charter);
		gp.setGuild(guild);
		gp.addGroup(leader);

		db.beginTransaction();
		try {
			db.save(guild);
			db.save(gp);
			db.saveAssociation(gp, "groups");
			db.saveAssociation(guild, "groups");
			db.commitTransaction();
			PermissionController.updatePermissions(gp);
			return guild;
		} catch (Exception e) {
			return null;
		} finally {
			db.endTransaction();
		}
	}

	public boolean guildCreate(Guild guild) {
		return guildUpgrade(guild, PermissionController.findGroup(
				Defaults.GUILD, null, Defaults.GUILD_RANK_TRACK));
	}

	public static Permission permission = null;

	public boolean guildUpgrade(Guild guild, PermissionGroup next) {
		GuildEvent event = new GuildCreate(guild);
		event.callEvent();
		if (event.isCancelled())
			return false;
		MysticGuilds.getSelf();
		/// It seems like I need to reload the BeanObject everytime I do a
		/// previous save on it
		/// in this case it was drafting right before this guildCreate
		guild = MysticGuilds.getGuild(guild.getName());
		if (guild.isCharter() || guild.getLabel() == null || guild.getLabel().isEmpty()) {
			guild.setLabel(MC.colorChat("&a" + guild.getName()));
		}
		PermissionGroup group = PermissionController.findGroup(Defaults.GUILD,
				null, Defaults.GUILD_RANK_TRACK);
		guild.setGroups(group);

		db.beginTransaction();
		try {
			db.update(guild);
			db.deleteManyToManyAssociations(guild, "groups");
			db.saveAssociation(guild, "groups");
			db.commitTransaction();
			for (GuildPlayer mem : guild.getMembers()) {
				PermissionController.updatePermissions(mem);
			}
			return true;
		} catch (Exception e) {
			return false;
		} finally {
			db.endTransaction();
		}
	}

	public boolean guildDisband(Guild guild) {
		GuildEvent event = new GuildDisband(guild);
		event.callEvent();
		if (event.isCancelled())
			return false;

		try {
			PermissionController.removeAllGroups(guild, Defaults.PLAYER_RANK_TRACK);
			PermissionController.removeAllGroups(guild, Defaults.PLAYER_TITLE_TRACK);
			for (GuildPlayer mem : guild.getMembers()) {
				mem.setGuild(null);
				mem.setGroups(null);
				db.update(mem);
			}
			db.beginTransaction();
			Set<Relationship> rels = guild.getRelationships();
			for (Relationship rel : rels) {
				Guild g1 = rel.getGuild1();
				Guild g2 = rel.getGuild2();
				db.deleteManyToManyAssociations(g1, "relationships");
				db.deleteManyToManyAssociations(g2, "relationships");
				db.delete(rel);
			}
			db.deleteManyToManyAssociations(guild, "groups");
			db.deleteManyToManyAssociations(guild, "invites");
			db.delete(guild);
			db.commitTransaction();
			for (GuildPlayer mem : guild.getMembers()) {
				PermissionController.updatePermissions(mem);
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		} finally {
			db.endTransaction();
		}
	}

	public void guildInvite(Guild guild, GuildPlayer gp) {
		if (guild.getInvites().contains(gp)) {
			return;
		}
		gp.addInvite(guild);
		db.saveManyToManyAssociations(gp, "invites");
	}

	public void uninvite(Guild guild, GuildPlayer gp) {
		if (!guild.getInvites().contains(gp)) {
			return;
		}
		gp.removeInvite(guild);
		db.deleteManyToManyAssociations(gp, "invites");
	}

	public ReturnValue guildJoin(Guild guild, GuildPlayer gp) {
		GuildEvent event = new GuildPlayerJoin(guild, gp);
		event.callEvent();
		if (event.isCancelled())
			return new ReturnValue(Value.Cancelled, event);
		gp.setGuild(guild);
		gp.setInvites(null);
		PermissionGroup group = PermissionController.getMemberGroup(guild);
		gp.setGroup(group);
		// System.out.println("Member group = " + group.getName());
		db.save(gp);
		db.saveAssociation(gp, "groups");
		db.deleteManyToManyAssociations(guild, "invites");
		PermissionController.updatePermissions(gp);
		return new ReturnValue(Value.True, event);
	}

	public void guildLeave(GuildPlayer gp) {
		Guild guild = gp.getGuild();
		if (guild == null)
			return;
		gp.setGuild(null);
		gp.setGroups(null);
		db.save(gp);
		db.deleteManyToManyAssociations(gp, "groups");
		PermissionController.updatePermissions(gp);
		new GuildPlayerLeave(guild, gp).callEvent();
	}

	public void guildKick(GuildPlayer gp) {
		Guild guild = gp.getGuild();
		if (guild == null)
			return;
		gp.setGuild(null);
		gp.setGroups(null);
		db.save(gp);
		db.deleteManyToManyAssociations(gp, "groups");
		PermissionController.updatePermissions(gp);
		new GuildPlayerKick(guild, gp).callEvent();
	}

	public void guildDeposit(Guild guild, int deposit) {
		guild.setFunds(guild.getFunds() + deposit);
		db.update(guild);
	}

	public void guildWithdraw(Guild guild, float withdraw) {
		float currentFunds = guild.getFunds();
		if (withdraw > currentFunds)
			withdraw = currentFunds;
		guild.setFunds(guild.getFunds() - withdraw);
		db.update(guild);
	}

	public boolean guildDemote(GuildPlayer gp) {
		return guildDemote(gp, PermissionController.nextGroup(gp.getGuild(),
				gp.getRank(), false, Defaults.PLAYER_RANK_TRACK));
	}

	public boolean guildDemote(GuildPlayer gp, PermissionGroup next) {
		if (gp.getGuild() == null) // / Cant demote someone not in the guild
			return false;
		db.beginTransaction();
		try {
			gp.demote(next);
			db.save(gp);
			/// Why do I have to delete then save?? shouldnt I just have to save
			db.deleteManyToManyAssociations(gp, "groups");
			db.saveAssociation(gp, "groups");
			db.commitTransaction();
			PermissionController.updatePermissions(gp);
		} catch (Exception e) {
			return false;
		} finally {
			db.endTransaction();
		}
		return true;
	}

	public boolean guildPromote(GuildPlayer gp) {
		return guildPromote(gp, PermissionController.nextGroup(gp.getGuild(),
				gp.getRank(), true, Defaults.PLAYER_RANK_TRACK));
	}

	public boolean guildPromote(GuildPlayer gp, PermissionGroup next) {
		if (gp.getGuild() == null || next == null || next.isLeader())
			return false;
		db.beginTransaction();
		try {
			gp.promote(next);
			db.save(gp);
			db.deleteManyToManyAssociations(gp, "groups");
			db.saveAssociation(gp, "groups");
			db.commitTransaction();
			PermissionController.updatePermissions(gp);
		} catch (Exception e) {
			return false;
		} finally {
			db.endTransaction();
		}
		return true;
	}

	public boolean guildSetLeader(Guild guild, GuildPlayer newLeader) {
		GuildPlayer oldLeader = guild.getLeader();
		if (oldLeader != null) {
			PermissionGroup newLeaderGroup = PermissionController.nextGroup(
					guild, oldLeader.getRank(), false,
					Defaults.PLAYER_RANK_TRACK);
			oldLeader.demote(newLeaderGroup);
		}
		db.beginTransaction();
		try {
			guild.setLeader(newLeader);
			newLeader.setGroup((PermissionController.getLeaderGroup(guild)));
			db.save(guild);
			if (oldLeader != null) {
				db.save(oldLeader);
				db.deleteManyToManyAssociations(oldLeader, "groups");
				db.saveAssociation(oldLeader, "groups");
				PermissionController.updatePermissions(oldLeader);
			}
			db.save(newLeader);
			db.deleteManyToManyAssociations(newLeader, "groups");
			db.saveAssociation(newLeader, "groups");
			PermissionController.updatePermissions(newLeader);
			db.commitTransaction();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		} finally {
			db.endTransaction();
		}
	}

	public void guildSetLabel(Guild guild, String label) {
		guild.setLabel(MC.colorChat(label));
		db.save(guild);
	}

	public void playerAddGroup(GuildPlayer gp, PermissionGroup group) {
		verifyGuildGroup(gp.getGuild());
		if (gp.addGroup(group)) {
			db.save(gp);
			db.saveManyToManyAssociations(gp, "groups");
			PermissionController.updatePermissions(gp);
		}
	}

	public void playerRemoveGroup(GuildPlayer gp, PermissionGroup group) {
		verifyGuildGroup(gp.getGuild());
		if (gp.removeGroup(group)) {
			db.save(gp);
			db.deleteManyToManyAssociations(gp, "groups");
			PermissionController.updatePermissions(gp);
		}
	}

	/**
	 * This is called when we might be switching from strictly default groups,
	 * to user made groups we need to copy over the default groups so that they
	 * are now editable
	 * 
	 * @param guild
	 */
	public void verifyGuildGroup(Guild guild) {
		if (guild == null)
			return;
		int count = db.find(PermissionGroup.class).where().eq("guild", guild)
				.eq("track", Defaults.PLAYER_RANK_TRACK).findRowCount();

		// System.out.println("Guild and Groups = " + count);
		// / If this is the first time a guild has modified a custom rank.. move
		// the entire guild over to custom groups
		// / This involves copying all default groups into guild groups
		// / Then moving all members to these new groups
		// / Titles are different.. titles do not get copied or deleted
		if (count <= 0) { // / Yep we need to copy over the default ranks into
			// this guild
			copyGroups(null, guild, Defaults.PLAYER_RANK_TRACK);
		}
	}

	public void copyGroups(Guild guild1, Guild guild2, int track) {
		List<PermissionGroup> groups = PermissionController.getGroups(guild1,track);
		HashMap<String, PermissionGroup> newGroups = new HashMap<String, PermissionGroup>();
		if (groups != null) {
			// guild2 = db.find(Guild.class,guild2.getName());
			for (PermissionGroup pg : groups) {
				if (pg.getTrack() == track) {
					Set<GuildPermission> perms = pg.getPermissions();
					PermissionGroup newGroup = db.find(PermissionGroup.class)
							.where().ieq("name", pg.getName())
							.eq("guild", guild2).eq("track", track)
							.findUnique();
					if (newGroup == null) {
						newGroup = new PermissionGroup(pg.getName(),
								pg.getLabel(), guild2, pg.getRank(),
								pg.getTrack());
						db.save(newGroup);
					}

					newGroups.put(pg.getName(), newGroup);
					// System.out.println(" rankname=" + pg.getName());
					for (GuildPermission perm : perms) {
						newGroup.addPermission(perm);
					}
					db.saveManyToManyAssociations(newGroup, "permissions");
					PermissionGroup oldGroup = PermissionController.getGroup(
							pg.getName(), guild2, track);
					if (oldGroup != null)
						guild2.removeGroup(oldGroup);
					guild2.addGroup(newGroup);
				}
			}
			db.save(guild2);
			db.saveManyToManyAssociations(guild2, "groups");
		}
		for (GuildPlayer gp : guild2.getMembers()) {
			PermissionGroup cur = PermissionController.getRank(gp.getGroups(),
					true);
			if (cur == null)
				cur = newGroups.get(Defaults.MEMBER_GROUP);
			if (newGroups.containsKey(cur.getName())) {
				PermissionGroup newGroup = newGroups.get(cur.getName());
				try {
					gp.removeGroup(cur);
					gp.addGroup(newGroup);
					db.save(gp);
					db.deleteManyToManyAssociations(gp, "groups");
					db.saveManyToManyAssociations(gp, "groups");
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	public List<Guild> getGuilds() {
		List<Guild> guilds = db.find(Guild.class).findList();
		Date last2Week = new Date();
		// last2Week.setTime(last2Week.getTime() - 2*Defaults.WEEK*1000); //
		// convert week to milliseconds
		// for (Guild guild: guilds){
		// db.find(GuildPlayer.class).where().eq("guild", guild).gt(arg0, arg1);
		// int count=0;
		// for (GuildPlayer gp : guild.getMembers()){
		// Date d = gp.getLastOnline();
		// if (d.after(last2Week)){
		// count++;}
		//
		// }
		// }
		return null;
	}

	public ReturnValue declareWar(Guild g1, Guild g2) {
		return declareRelationship(g1, g2, RelationshipType.HOSTILE);
	}

	public ReturnValue declareAlly(Guild g1, Guild g2) {
		return declareRelationship(g1, g2, RelationshipType.ALLY);
	}

	public ReturnValue declareNeutral(Guild g1, Guild g2) {
		return declareRelationship(g1, g2, RelationshipType.NEUTRAL);
	}

	public ReturnValue declareRelationship(Guild g1, Guild g2, RelationshipType type) {
		return declareRelationship(g1,g2,type,false);
	}

	static final String REL_SQL = "update mg_relationship SET value=?,changed=? where id=?";
	public ReturnValue declareRelationship(Guild g1, Guild g2, RelationshipType type,boolean skipEvent) {
		Relationship rel = getRelationship(g1, g2);
		// / do we have an old relationship
		boolean newRel = false;
		if (rel != null) {
			System.out.println(" old rel " + rel);
			if (rel.sameRelationship(type)) { // / cant change to a relationship
				// that we already are
				return new ReturnValue(Value.False);
			}
			rel.setType(type);
			rel.setValue(type.value());
		} else {
			newRel = true;
			rel = new Relationship(g1, g2, type.value());
		}
		GuildEvent event = new GuildChangeRelationshipEvent(g1, g2, rel);
		if (!skipEvent){
			/// Call our event
			event.callEvent();
			if (event.isCancelled())
				return new ReturnValue(Value.Cancelled, event);
		}
		rel.setChanged(new Timestamp(System.currentTimeMillis()));			

		//		System.out.println(" newest rel " + rel);
		if (newRel) {
			//			System.out.println(" rel id=" + rel.getId() + "   rel=" + rel);
			db.save(rel);
			g1.setRelationship(rel);
			g2.setRelationship(rel);
			db.save(g1);
			db.save(g2);
			db.saveManyToManyAssociations(g1, "relationships");
			db.saveManyToManyAssociations(g2, "relationships");
		} else{ /// Seriously, totally serious. save
			CallableSql cs = db.createCallableSql(REL_SQL);
			cs.setParameter(1, rel.getValue());
			cs.setParameter(2, rel.getChanged());
			cs.setParameter(3, rel.getId());
			cs.addModification("mg_relationship", true, true, false);
			db.beginTransaction();
			//			System.out.println("here saving " + cs.getSql());
			int v = db.execute(cs);
			System.out.println(" v = " + v);
			db.commitTransaction();
		}
		return new ReturnValue(Value.True, event);
	}

	public void save() {
		// guildCache.cache.save();
		// gpCache.cache.save();
	}

	public static void save(Guild guild) {
		// guildCache.save(guild);
	}

	public boolean hasAsked(Guild g1, Guild g2, RelationshipType type) {
		Map<String, Relationship> requests = relationshipRequests.get(g1
				.getName());
		System.out.println("  checking to see if " + g1.getName()
				+ "  hasasked " + g2.getName());
		if (requests == null || !requests.containsKey(g2.getName())) {
			return false;
		}
		Relationship rel = requests.get(g2.getName());
		return rel.sameRelationship(type);
	}

	public void asked(Guild g1, Guild g2, RelationshipType type) {
		Map<String, Relationship> requests = relationshipRequests.get(g1
				.getName());
		if (requests == null) {
			requests = new HashMap<String, Relationship>();
			relationshipRequests.put(g1.getName(), requests);
		}
		Relationship rel = new Relationship(g1, g2, type.value);
		requests.put(g2.getName(), rel);
		System.out.println("  adding " + g1.getName() + "  asked "
				+ g2.getName());
	}

	public Relationship getRelationship(Relationship rel) {
		return db.find(Relationship.class, rel.getId());
	}

	public Relationship getRelationship(Guild g1, Guild g2) {
		Relationship rel = db.find(Relationship.class).where()
				.ilike("guild1", g1.getName()).ilike("guild2", g2.getName())
				.findUnique();
		if (rel == null) {
			rel = db.find(Relationship.class).where()
					.ilike("guild1", g2.getName())
					.ilike("guild2", g1.getName()).findUnique();
		}
		return rel;
	}
}
