package com.mystic.guilds.objects;

import java.util.List;

import org.apache.commons.lang.mutable.MutableBoolean;

import com.avaje.ebean.EbeanServer;
import com.mystic.guilds.Defaults;
import com.mystic.guilds.util.Cache;
import com.mystic.guilds.util.Cache.CacheSerializer;

public class GuildPlayerCache implements CacheSerializer<String,GuildPlayer>{
	EbeanServer db;
	public Cache<String,GuildPlayer> cache = new Cache<String,GuildPlayer>(this);
	GuildCache guildCache;
	public GuildPlayerCache(EbeanServer db, GuildCache guildCache){
		this.db = db;
		cache.setSaveEvery(Defaults.SAVE_EVERY_X_SECONDS *1000);
		this.guildCache = guildCache;
	}

	public GuildPlayer get(String p) {
		return cache.get(p);
	}

	public GuildPlayer match(String p) {
		return cache.get(p,new Boolean(true));
	}

	@Override
	public GuildPlayer load(String key, MutableBoolean dirty, Object... varArgs) {
		GuildPlayer gp = null;
		if (varArgs.length > 0 && varArgs[0] instanceof Boolean){
			System.out.println("attemping match =" + key);
			gp = db.find(GuildPlayer.class).where().ilike("name", key+"%").findUnique();
		} else {
			System.out.println("loading key =" + key);
			gp = db.find(GuildPlayer.class).where().ieq("name", key).findUnique();
		}
		if (gp !=null){
			
		}
		return gp; 
	}

	@Override
	public void save(List<GuildPlayer> types) {
		for (GuildPlayer gp: types){
			db.save(gp);
		}
	}
	public GuildPlayer remove(GuildPlayer player) {
		return cache.remove(player);
	}

	@SuppressWarnings("unchecked")
	public void setDirty(GuildPlayer guildPlayer) {
		cache.setDirty(guildPlayer);
	}

	@SuppressWarnings("unchecked")
	public void update(GuildPlayer gp) {
		db.save(gp);
		cache.setClean(gp);
	}


}
