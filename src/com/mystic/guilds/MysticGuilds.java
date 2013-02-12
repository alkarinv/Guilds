package com.mystic.guilds;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.PersistenceException;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.EbeanServerFactory;
import com.avaje.ebean.SqlRow;
import com.avaje.ebean.config.ServerConfig;
import com.avaje.ebeaninternal.api.SpiEbeanServer;
import com.avaje.ebeaninternal.server.ddl.DdlGenerator;
import com.mystic.guilds.controller.GuildController;
import com.mystic.guilds.controller.MC;
import com.mystic.guilds.controller.MoneyController;
import com.mystic.guilds.controller.PermissionController;
import com.mystic.guilds.executors.GroupExecutor;
import com.mystic.guilds.executors.GuildExecutor;
import com.mystic.guilds.executors.RelationshipExecutor;
import com.mystic.guilds.listeners.MysticGuildsPlayerListener;
import com.mystic.guilds.objects.Guild;
import com.mystic.guilds.objects.GuildPlayer;
import com.mystic.guilds.objects.Relationship;
import com.mystic.guilds.objects.permissions.GuildPermission;
import com.mystic.guilds.objects.permissions.PermissionGroup;
import com.mystic.guilds.serializer.ConfigSerializer;
import com.mystic.zonedabone.mysticfactions.MysticFactions;

public class MysticGuilds extends JavaPlugin {

	public FileConfiguration config;
	public static MysticFactions factions;

	public static MysticGuilds plugin;

	public static boolean useFactions = true;
	private static GuildController guildController = null;
	static EbeanServer ebeanServer = null;
	private static GuildExecutor guildExecutor;
	private static RelationshipExecutor relationshipExecutor;
	private static GroupExecutor groupExecutor;

	@Override
	public void onDisable() {
		guildController.save();
	}

	@Override
	public void onEnable() {
		plugin = this;
		setupDatabase();
		ConfigSerializer.setConfig(load("/default_files/config.yml",getDataFolder().getPath() +"/config.yml"));
		ConfigSerializer.loadAll(getDatabase());
		guildController = new GuildController(getDatabase());

		this.config=this.getConfig();
		PluginManager pm = this.getServer().getPluginManager();
		factions = (MysticFactions) pm.getPlugin("MysticFactions");
		if(factions == null){
			useFactions = false;
		}

		MC.setConfig(load("/default_files/messages.yml", getDataFolder().getPath()+"/messages.yml"));
		MC.load();

		guildExecutor = new GuildExecutor(this,guildController);
		pm.registerEvents(new MysticGuildsPlayerListener(this), this);
		this.getCommand("guild").setExecutor(guildExecutor);
		/// These executors also refer to guild, but have been separated out for more clarity
		groupExecutor = new GroupExecutor(guildExecutor, guildController); /// Handle promote/demote/setLeader, ranks and titles
		relationshipExecutor = new RelationshipExecutor(guildExecutor, guildController); /// Handle relationships beetween guilds

		MoneyController.setup();

		reloadPlayerPermissions();
		Player[] online = Bukkit.getOnlinePlayers();
		for (Player p: online)
			MC.playerJoin(p);
	}

	public static void main(String[] args) {
		setupDatabase2();
    }

	private void reloadPlayerPermissions() {
		for (Player p: Bukkit.getOnlinePlayers()){
			PermissionController.updatePermissions(p);
		}
	}
	private static void setupDatabase2() {
		ServerConfig c = new ServerConfig();
		 c.setName("guilds");

		 // read the ebean.properties and load
		 // those settings into this serverConfig object
		 c.loadFromProperties();

		 // generate DDL and run it
		 c.setDdlGenerate(true);
		 c.setDdlRun(true);

		 // add any classes found in the app.data package
		 c.addPackage("app.data");

		 // add the names of Jars that contain entities
		 c.addJar("Guilds.jar");

		 c.setDefaultServer(false);

		 EbeanServer server = EbeanServerFactory.create(c);

		try{

		} catch (final PersistenceException e){
	        String sql = "create database guilds";
	        DdlGenerator gen = ((SpiEbeanServer) server).getDdlGenerator();

	        gen.runScript(false, gen.generateCreateDdl());
		}
        String sql = "select count(*) as count from mg_guild";
        SqlRow row =
            server.createSqlQuery(sql)
            .findUnique();

        Integer i = row.getInteger("count");

        System.out.println("Got "+i+"  - DataSource good.");
        PermissionController.db = server;
	}

	private void setupDatabase() {
		try {
			getDatabase().find(Guild.class).findRowCount();
		} catch (final PersistenceException ex) {
			System.out.println("Installing database for "
					+ this.getDescription().getName() + " due to first time usage");
			installDDL();
		}
		PermissionController.db = getDatabase();
	}

	public void process(String name){
		GuildPlayer gp = getPlayer(name);
		if(gp == null){
			gp = new GuildPlayer(name);
			getLogger().info("Guild data created for '"+name+"'.");
		}

		gp.updateTime();
		this.getDatabase().save(gp);
	}
	public void process(Player p){
		process(p.getName());
	}

	public void broadcast(Guild guild, String message, GuildPlayer... exempt){
		MC.broadcast(guild, message, exempt);
	}

	public void send(GuildPlayer player, String message){
		Player p = this.getServer().getPlayer(player.getName());
		if(p!=null){
			p.sendMessage(message);
		}
	}

	public static GuildPlayer getPlayer(Player p){
		return guildController == null ? null : guildController.getPlayer(p);
	}
	public static GuildPlayer getPlayer(String p){
		return guildController == null ? null :guildController.getPlayer(p);
	}
	public static GuildPlayer matchPlayer(String p){
		return guildController == null ? null :guildController.matchPlayer(p);
	}
	public static Guild getGuild(String name){
		return guildController == null ? null :guildController.getGuild(name);
	}
	public static Guild getGuild(Player p){
		return guildController == null ? null : guildController.getGuild(p);
	}

	@Override
	public List<Class<?>> getDatabaseClasses() {
		final List<Class<?>> list = new ArrayList<Class<?>>();
		list.add(Guild.class);
		list.add(GuildPlayer.class);
		list.add(PermissionGroup.class);
		list.add(GuildPermission.class);
		list.add(Relationship.class);
		return list;
	}

	public static String getName(Player p){
		GuildPlayer gp = MysticGuilds.getPlayer(p);
		Guild g = gp.getGuild();
		if(g==null||g.isCharter()){
			return "No-Guild";
		}
		return g.getLabel();
	}

	public static MysticGuilds getSelf(){
		return plugin;
	}

	public static void addGuildMethods(Object obj, Method[] methods){
		guildExecutor.addMethods(obj, methods);
	}

	public static GuildController getGuildController(){
		return guildController;
	}
	public static RelationshipExecutor getRelationshipExecutor() {
		return relationshipExecutor;
	}
	public static GroupExecutor getGroupExecutor() {
		return groupExecutor;
	}
	public File load(String default_file, String config_file) {
		File file = new File(config_file);
		if (!file.exists()){ /// Create a new file from our default example
			try{
				InputStream inputStream = getClass().getResourceAsStream(default_file);
				OutputStream out=new FileOutputStream(config_file);
				byte buf[]=new byte[1024];
				int len;
				while((len=inputStream.read(buf))>0){
					out.write(buf,0,len);}
				out.close();
				inputStream.close();
			} catch (Exception e){
			}
		}
		return file;
	}

	public static EbeanServer getDB() {
		return ebeanServer;
	}


}
