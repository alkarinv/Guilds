package com.mystic.guilds.objects;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.annotation.CreatedTimestamp;
import com.avaje.ebean.validation.Length;
import com.avaje.ebean.validation.NotNull;
import com.mystic.guilds.Defaults;
import com.mystic.guilds.MysticGuilds;
import com.mystic.guilds.objects.permissions.PermissionGroup;
import com.mystic.guilds.util.Cache.UniqueKey;

@Entity()
@Table(name = "mg_guild")
public class Guild implements UniqueKey<String>{

	private static final int MAX_GUILDNAME_LENGTH = 16;
	private static final int MAX_NAMEFORMAT_LENGTH = 48;

	@Id
	@Length(max=MAX_GUILDNAME_LENGTH)
	private String name;

	@NotNull
	private float funds = 0;

	@CreatedTimestamp
	Timestamp created;

	@Length(max=32)
	private String faction = null;

	@Length(max=MAX_NAMEFORMAT_LENGTH)
	private String label = null;

	@OneToMany(cascade=CascadeType.ALL, mappedBy="guild")
	private Set<GuildPlayer> members = new HashSet<GuildPlayer>();

	@OneToOne(optional=true)
	@JoinColumn(name="leader", unique=true, nullable=true, updatable=true)
	private GuildPlayer leader = null;

	@OneToOne(optional=true)
	@JoinColumn(name="founder", unique=true, nullable=true, updatable=true)
	private GuildPlayer founder = null;

	@ManyToMany
	@JoinTable(name="mg_invites")
	private Set<GuildPlayer> invites = new HashSet<GuildPlayer>();

	@ManyToMany
	@JoinTable(name="mgp_guilds2groups")
	private Set<PermissionGroup> groups = new HashSet<PermissionGroup>();

	@ManyToMany
	@JoinTable(name="mg_guilds2relationships")
	private Set<Relationship> relationships = new HashSet<Relationship>();

	public Guild(){}

	public Guild(GuildPlayer leader, String name){
		this.setLeader(leader);
		this.setFounder(leader);
		this.name = name;
		members.add(leader);
	}

	private void setDirty() {
//		GuildCache cache = GuildController.getGuildCache();
//		if (cache != null)
//			cache.setDirty(this);
	}

	public Timestamp getCreated() {
		return created;
	}

	public void setCreated(Timestamp created) {
		this.created = created;
		setDirty();
	}

	@Override
	public String getKey() {
		return name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
		this.label = name;
		setDirty();
	}

	public float getFunds() {
		return funds;
	}

	public void setFunds(float funds) {
		this.funds = funds;
	}

	public String getFaction() {
		return faction;
	}

	public void setFaction(String faction) {
		this.faction = faction;
		setDirty();
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
		setDirty();
	}

	public Set<GuildPlayer> getMembers() {
		return members;
	}

	public void setMembers(Set<GuildPlayer> members) {
		this.members = members;
		setDirty();
	}

	public GuildPlayer getLeader() {
		return leader;
	}

	public void setLeader(GuildPlayer leader) {
		this.leader = leader;		
		setDirty();
	}

	public Set<GuildPlayer> getInvites() {
		return invites;
	}

	public void setInvites(Set<GuildPlayer> invites) {
		this.invites = invites;
		setDirty();
	}

	public GuildPlayer getFounder() {
		return founder;
	}

	public void setFounder(GuildPlayer founder) {
		this.founder = founder;
		setDirty();
	}

	public boolean isCharter() {
		return hasGroup(Defaults.CHARTER);
	}

	public boolean hasGroup(String groupName){
		/// Another quirk.. if Guild is gotten from player.getGuild() none of the invites/groups are here
		Guild self = this;
//		System.out.println("this=" + name +"   self.groups="+ groups);
		if (self.groups == null || self.groups.isEmpty())/// try and get it again from db
			self = MysticGuilds.getGuild(this.getName());
		if (self == null || self.groups == null || self.groups.isEmpty())
			return false;
		for (PermissionGroup group: self.groups){
//			System.out.println("looking at group = " + group);
			if (group.getName().equalsIgnoreCase(groupName))
				return true;
		}
		return false;
	}
	public boolean hasGroup(PermissionGroup group){
		if (groups == null || groups.isEmpty())
			return false;
		for (PermissionGroup g: groups){
			if (g.getId() == group.getId())
				return true;
		}
		return false;

	}
	public Set<PermissionGroup> getGroups() {
		return groups;
	}

	public void setGroups(Set<PermissionGroup> groups) {
		this.groups = groups;
		setDirty();
	}
	public void setGroups(PermissionGroup group) {
		Set<PermissionGroup> groups = new HashSet<PermissionGroup>();
		if (group != null)
			groups.add(group);
		setGroups(groups);
		setDirty();
	}

	public String toString(){
		return "[Guild "+name+":"+isCharter()+":"+leader+"]";
	}

	public int getRank() {
		PermissionGroup group = getRankGroup();
		if (group == null || groups.isEmpty()){
			return Defaults.MAX_RANK;
		}
		return group.getRank();
	}

	public PermissionGroup getRankGroup() {
		Guild self = this;
		if (self.groups == null || self.groups.isEmpty())/// try and get it again from db
			self = MysticGuilds.getGuild(this.getName());

		PermissionGroup group = null;
		if (self.groups == null || self.groups.isEmpty()){
			return group;
		}
		int rank = Defaults.MAX_RANK;
		for (PermissionGroup g : self.groups){
			if (g.getTrack() == Defaults.GUILD_RANK_TRACK && g.getRank() < rank){
				rank = g.getRank();
				group = g;
			}
		}
		return group;
	}

	public void removeGroup(PermissionGroup group) {
		if (hasGroup(group))
			groups.remove(group);
	}

	public void addGroup(PermissionGroup group) {
		if (!hasGroup(group))
			groups.add(group);
	}

	public Set<Relationship> getRelationships() {
		return relationships;
	}

	public Set<Relationship> getRelationships(EbeanServer db) {
		Set<Relationship> rels = new HashSet<Relationship>();
		for (Relationship rel: getRelationships()){
			if (rel.id != 0 && rel.getGuild1()==null){
				rel = db.find(Relationship.class,rel.id);}
			rels.add(rel);
		}
		return rels;
	}

	public void setRelationships(Set<Relationship> relationships) {
		this.relationships = relationships;
		setDirty();
	}
	public Relationship getRelationship(EbeanServer db, Guild other){
		String oname = other.getName();
		for (Relationship rel: getRelationships(db)){
			if (rel.getGuild1().getName().compareTo(oname)==0 || rel.getGuild2().getName().compareTo(oname)==0)
				return rel;
		}
		return null;
	}

	public Relationship getRelationship(Guild other){
		String oname = other.getName();
		for (Relationship rel: relationships){
			if (rel.getGuild1().getName().compareTo(oname)==0 || rel.getGuild2().getName().compareTo(oname)==0)
				return rel;
		}
		return null;
	}

	public void addRelationship(Relationship rel) {
		relationships.add(rel);
		setDirty();
	}

	public void addInvite(GuildPlayer gp) {
		invites.add(gp);
		setInvites(invites);
		setDirty();
	}

	public void removeInvite(GuildPlayer gp) {
		invites.remove(gp);
		setDirty();
	}
	@Override
	public int hashCode(){
		return name.hashCode();
	}
	@Override
	public boolean equals(Object obj){
		return (obj instanceof Guild && name.equals(((Guild) obj).name));
	}

	public void setRelationship(Relationship rel) {
		relationships.remove(rel);
		relationships.add(rel);
	}

}
