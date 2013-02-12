package com.mystic.guilds.objects;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.avaje.ebean.validation.Length;
import com.mystic.guilds.Defaults;
import com.mystic.guilds.objects.permissions.GuildPermission;
import com.mystic.guilds.objects.permissions.PermissionGroup;
import com.mystic.guilds.util.FileLogger;
import com.mystic.guilds.util.Cache.UniqueKey;

@Entity
@Table(name="mg_guildplayer")
public class GuildPlayer implements UniqueKey<String> {

	@Id
	@Length(max=16)
	private String name = null;

	private Date lastOnline = new Date();

	@ManyToOne
	@JoinColumn(name="guild", nullable=true)
	private Guild guild;

	@ManyToMany
	@JoinTable(name="mg_invites")
	private Set<Guild> invites = new HashSet<Guild>();

	@ManyToMany
	@JoinTable(name="mgp_players2groups")
	private Set<PermissionGroup> groups = new HashSet<PermissionGroup>();

	@Transient
	int rank = Defaults.MAX_RANK;

	public GuildPlayer(String name){
		this.setName(name);
	}

	public GuildPlayer(){}

	private void calculateRank(){
		if (groups == null || groups.isEmpty()){
			rank = Defaults.MAX_RANK;
			return;
		}
		for (PermissionGroup g : groups){
			if (g.getTrack() == Defaults.PLAYER_RANK_TRACK)
				rank = Math.min(g.getRank(), rank);
		}
	}

	@Override
	public String getKey() {
		return name;
	}

	public String getName(){
		return name;
	}

	public void updateTime(){
		setLastOnline(new Date());
		setDirty();
	}

	public Date getLastOnline(){
		return lastOnline;
	}

	public Guild getGuild(){
		return guild;
	}

	public void setGuild(Guild guild){
		this.guild = guild;
		setDirty();
	}

	public void setName(String name) {
		this.name = name;
		setDirty();
	}

	public void setLastOnline(Date lastOnline) {
		this.lastOnline = lastOnline;
		setDirty();
	}

	public Set<Guild> getInvites() {
		return invites;
	}

	public void setInvites(Set<Guild> invites) {
		this.invites = invites;
		setDirty();
	}

	public Set<PermissionGroup> getGroups() {
		return groups;
	}

	public Set<PermissionGroup> getRanks() {
		HashSet<PermissionGroup> ranks = new HashSet<PermissionGroup>();
		for (PermissionGroup pg: groups){
			if (pg.getTrack() == Defaults.PLAYER_RANK_TRACK)
				ranks.add(pg);
		}
		return ranks;
	}
	public PermissionGroup getRankGroup() {
		if (groups == null || groups.isEmpty()){
			return null;
		}
		int min = Defaults.MAX_RANK;
		PermissionGroup group = null;
		for (PermissionGroup g : groups){
			if (g.getTrack() == Defaults.PLAYER_RANK_TRACK)
				if (g.getRank() <= min){
					group = g;}
		}
		return group;
	}

	public Set<PermissionGroup> getTitles() {
		HashSet<PermissionGroup> titles = new HashSet<PermissionGroup>();
		for (PermissionGroup pg: groups){
			if (pg.getTrack() == Defaults.PLAYER_TITLE_TRACK)
				titles.add(pg);
		}
		return titles;
	}

	public void setGroups(Set<PermissionGroup> groups) {
		this.groups = groups;
		calculateRank();
		setDirty();
	}

	public boolean higherRank(GuildPlayer gp) {
		return rank < gp.rank;
	}

	public void promote(PermissionGroup group) {
		Set<PermissionGroup> gs = new HashSet<PermissionGroup>();
		if (group != null)
			gs.add(group);
		setGroups(gs);
	}

	public void demote(PermissionGroup group) {
		Set<PermissionGroup> gs = new HashSet<PermissionGroup>();
		if (group != null)
			gs.add(group);
		setGroups(gs);
	}

	public String toString(){
		StringBuilder sb = new StringBuilder("[GPlayer "+name+":" +((guild != null) ? guild.getName(): "null")+":" +
				rank +" groups=");
		if (groups!=null){
			boolean first = true;
			for (PermissionGroup g: groups){
				if (!first) sb.append(",");
				first = false;
				sb.append(g.getName());
			}
		}
		sb.append("]");
		return sb.toString();
	}

	public boolean setGroup(PermissionGroup group) {
		if (groups == null){
			groups = new HashSet<PermissionGroup>();}
		groups.clear();
		groups.add(group);
		calculateRank();
		return true;
	}

	public boolean addGroup(PermissionGroup group) {
		if (groups == null){
			groups = new HashSet<PermissionGroup>();}
		if (groups.add(group)){
			calculateRank();
			return true;
		}
		return false;
	}

	public int getRank() {
		return rank;
	}

	public boolean hasGroup(String name) {
		if (groups == null || groups.isEmpty())
			return false;
		for (PermissionGroup g : groups){
			if (g.getName().equals(name))
				return true;
		}
		return false;
	}

	public boolean hasGroup(PermissionGroup group) {
		return groups != null && groups.contains(group);
	}

	public boolean removeGroup(PermissionGroup group) {
		return groups == null ? false : groups.remove(group);
	}

	public Set<GuildPermission> getPermissions() {
		FileLogger.log(" getPermissions for player " + name +"   " + (groups == null || groups.isEmpty()));
		if (groups == null || groups.isEmpty())
			return null;
		Set<GuildPermission> perms = new HashSet<GuildPermission>();
		for (PermissionGroup g: groups){
			FileLogger.log("  getPermissions group  " + g +"   permSize="+g.getPermissions().size());

			//			System.out.println(" group g=" + g.getLabel());
			perms.addAll(g.getPermissions());
		}
		if (guild != null && guild.getGroups() != null){
			PermissionGroup guildRank =guild.getRankGroup();
			if (guildRank != null){
				//				System.out.println("  - perm g=" + guildRank.getLabel());
				perms.addAll(guildRank.getPermissions());				
			}
		}
		return perms;
	}

	private void setDirty() {
//		GuildPlayerCache cache = GuildController.getPlayerCache();
//		if (cache != null)
//			cache.setDirty(this);
	}

	public void addInvite(Guild guild) {
		invites.add(guild);
		setDirty();
	}
	public void removeInvite(Guild guild) {
		invites.remove(guild);
		setDirty();
	}
}
