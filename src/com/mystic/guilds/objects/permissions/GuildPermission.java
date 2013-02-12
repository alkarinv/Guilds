package com.mystic.guilds.objects.permissions;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.avaje.ebean.validation.Length;

@Entity
@Table(name="mgp_perms")
public class GuildPermission {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private int id;
	
	@Length(max=16)
	private String name;

	@Length(max=128)
	private String node;

	private boolean adminOnly = false;

	public GuildPermission(){}
	public GuildPermission(String name, String node, boolean adminOnly){
		this.name = name;
		this.node = node;
		this.adminOnly = adminOnly;
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getNode() {
		return node;
	}
	public void setNode(String node) {
		this.node = node;
	}
	public boolean isAdminOnly() {
		return adminOnly;
	}
	public void setAdminOnly(boolean adminOnly) {
		this.adminOnly = adminOnly;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	@Override
	public int hashCode(){
		return Integer.valueOf(id).hashCode();
	}
	@Override
	public boolean equals(Object obj){
		return (obj instanceof GuildPermission && id == ((GuildPermission) obj).id);
	}

}
