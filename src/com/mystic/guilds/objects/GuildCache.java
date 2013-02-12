package com.mystic.guilds.objects;

import java.util.List;

import org.apache.commons.lang.mutable.MutableBoolean;

import com.avaje.ebean.EbeanServer;
import com.mystic.guilds.Defaults;
import com.mystic.guilds.util.Cache;
import com.mystic.guilds.util.Cache.CacheSerializer;

public class GuildCache implements CacheSerializer<String,Guild>{
	EbeanServer db;
	public Cache<String,Guild> cache = new Cache<String,Guild>(this);
//	GuildPlayerCache gpCache =null;
	
	public GuildCache(EbeanServer db){
		this.db = db;
		cache.setSaveEvery(Defaults.SAVE_EVERY_X_SECONDS *1000);
	}

	public Guild get(String g) {
		return cache.get(g);
	}

	public Guild match(String g) {
		return cache.get(g,new Boolean(true));
	}

	@Override
	public Guild load(String key, MutableBoolean dirty, Object... varArgs) {
		Guild guild = null;
		if (varArgs.length > 0){
			if (varArgs[0] instanceof Boolean){ /// try and match
				System.out.println("guild attemping match =" + key);
				return db.find(Guild.class).where().ilike("name", key).findUnique();				
			} else if (varArgs[0] instanceof Guild){ /// attempt to get from the guild name, otherwise return the object itself
				guild = db.find(Guild.class).where().ieq("name", ((Guild)varArgs[0]).getName()).findUnique();
				if (guild != null){
					return guild;
				} else {
					return (Guild) varArgs[0];
				}
			}
		} 
		System.out.println("loading =" + key);
		return db.find(Guild.class).where().ieq("name", key).findUnique();
	}

	@Override
	public void save(List<Guild> guilds) {
		for (Guild guild: guilds){
			db.update(guild);
		}
	}
	
	@SuppressWarnings("unchecked")
	public void update(Guild guild) {
		db.update(guild);
		cache.setClean(guild);
	}

	public void save(Guild guild) {
		cache.save(guild);
	}

	public Guild remove(Guild guild) {
		return cache.remove(guild);
	}

	@SuppressWarnings("unchecked")
	public void setDirty(Guild guild) {
		cache.setDirty(guild);
	}



}
