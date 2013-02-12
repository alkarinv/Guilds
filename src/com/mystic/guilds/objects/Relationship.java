package com.mystic.guilds.objects;

import java.sql.Timestamp;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity()
@Table(name = "mg_relationship")
public class Relationship {
	static final int VAL = 100;
	public static enum RelationshipType{
		WAR(-9*VAL,"Warring"), HOSTILE(-VAL,"Hostile"), NEUTRAL(0,"Neutral"), ALLY(VAL,"Allied");
		public int value;
		public String name;
		RelationshipType(int v,String name){value = v;this.name = name;}
		public int value(){return value;}
	}
	
	@Id
	int id;

	Timestamp changed; /// when was the relationship last modified
	
	int value; /// The strength of the relationship

	@ManyToOne
	@JoinColumn(name="guild1")
	private Guild guild1;

	@ManyToOne
	@JoinColumn(name="guild2")
	private Guild guild2;

	public Relationship() {}
	public Relationship(Guild g1, Guild g2, int value) {
		guild1 = g1; guild2 = g2; this.value = value;
	}

	public Guild getGuild1() {
		return guild1;
	}

	public void setGuild1(Guild guild1) {
		this.guild1 = guild1;
	}

	public Guild getGuild2() {
		return guild2;
	}

	public void setGuild2(Guild guild2) {
		this.guild2 = guild2;
	}


	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Timestamp getChanged() {
		return changed;
	}

	public void setChanged(Timestamp changed) {
		this.changed = changed;
	}

	public int getValue() {
		return value;
	}

	public void setValue(int value) {
		this.value = value;
	}
	public void setType(RelationshipType type) {
		this.value = type.value();
	}
	public boolean sameRelationship(RelationshipType type){
		switch(type){
		case ALLY: return isAlly();
		case NEUTRAL: return isNeutral();
		case WAR: return isWarring();
		case HOSTILE: return isHostile();
		}
		return false;
	}
	public boolean isAlly(){return value >= RelationshipType.ALLY.value();}
	public boolean isNeutral(){return value < RelationshipType.ALLY.value() && value > RelationshipType.HOSTILE.value() ;}
	public boolean isHostile(){return value <= RelationshipType.HOSTILE.value() && value > RelationshipType.WAR.value();}
	public boolean isWarring(){return value <= RelationshipType.WAR.value();}
	public String toString(){
		return id +"  " + guild1 +" : " + guild2 +"  rel= " + value; 
	}
	public long getChangedMilliseconds() {
		return changed != null ? changed.getTime() : 0;
	}
}
