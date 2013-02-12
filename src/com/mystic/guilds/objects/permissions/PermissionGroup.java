package com.mystic.guilds.objects.permissions;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.bukkit.ChatColor;

import com.avaje.ebean.validation.Length;
import com.avaje.ebean.validation.NotNull;
import com.mystic.guilds.Defaults;
import com.mystic.guilds.objects.Guild;

@Entity
@Table(name="mgp_groups")
public class PermissionGroup {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Integer id;
	
	@NotNull
	@Length(max=16)
	String name;
	
	@NotNull
	@Length(max=48)
	String label;
	
	@NotNull
	private int rank;
	
	@NotNull
	private int track;
	
	@ManyToOne
    @JoinColumn(name="guild", nullable=true)
	Guild guild;

	@ManyToMany
    @JoinTable(name="mgp_groups2perms")
	private Set<GuildPermission> permissions = new HashSet<GuildPermission>();
	
	public PermissionGroup(){	
	}

	public PermissionGroup(String name, String label, Guild guild, int rank, int track) {
		this.name = name;
		this.label = label;
		this.rank = rank;
		this.track = track;
		this.guild = guild;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public Guild getGuild() {
		return guild;
	}

	public void setGuild(Guild guild) {
		this.guild = guild;
	}
	
	public Set<GuildPermission> getPermissions() {
		return permissions;
	}

	public void setPermissions(Set<GuildPermission> permissions) {
		this.permissions = permissions;
	}
	public void addPermission(GuildPermission perm){
		if (!permissions.contains(perm))
			permissions.add(perm);
	}

	public void removePermission(GuildPermission perm) {
		if (permissions.contains(perm))
			permissions.remove(perm);
	}
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public int getRank() {
		return rank;
	}

	public void setRank(int rank) {
		this.rank = rank;
	}
	public String toString(){
		return "[Group "+id+" " + name +" rank=" + rank+"]";
	}

	public boolean isLeader() {
		return rank == Defaults.MIN_RANK || name.equals(Defaults.LEADER_GROUP);
	}

	public ChatColor getColor() {
		String lbl = label.replaceAll("&", "§");
		int index = lbl.indexOf("§");
		if (index != -1 && lbl.length() > index+1){
			ChatColor cc = ChatColor.getByChar(lbl.charAt(index+1));
			if (cc != null)	
				return cc;
		} 		
		return ChatColor.GREEN;
	}

	public int getTrack() {
		return track;
	}

	public void setTrack(int track) {
		this.track = track;
	}

	public boolean hasPerm(GuildPermission perm) {
		return permissions.contains(perm);
	}

}
