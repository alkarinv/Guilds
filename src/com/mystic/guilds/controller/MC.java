package com.mystic.guilds.controller;

import java.io.File;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import com.mystic.guilds.MysticGuilds;
import com.mystic.guilds.objects.Guild;
import com.mystic.guilds.objects.GuildPlayer;

/**
 * 
 * @author alkarin
 *
 */
public class MC {

	static YamlConfiguration mc = new YamlConfiguration();
	static File f = new File(MysticGuilds.getSelf().getDataFolder()+"/messages.yml");
	
	/// As getPlayerExact is an O(n) computation, and all guild messages iterate
	/// over every member of a guild,m, this is a O(nm) computation.  lets trade 
	/// space for time and keep our own online list.
	static Map<String,Player> online = new HashMap<String,Player>();

	public static boolean hasMessage(String prefix, String node) {
		return mc.contains(prefix+"."+node);
	}
	public static String getMessage(String prefix,String node, Object... varArgs) {
		if (node.isEmpty())
			return node;
		try{
			ConfigurationSection n = mc.getConfigurationSection(prefix);

			StringBuilder buf = new StringBuilder(n.getString("prefix", "[Guilds]"));
			String msg = n.getString(node, "No translation for " + node);
			Formatter form = new Formatter(buf);

			form.format(msg, varArgs);
			return colorChat(buf.toString());
		} catch(Exception e){
			System.err.println("Error getting message " + prefix + "." + node);
			for (Object o: varArgs){ System.err.println("argument=" + o);}
			e.printStackTrace();
			return "Error getting message " + prefix + "." + node;
		}
	}

	public static String getMessageNP(String prefix,String node, Object... varArgs) {
		if (node.isEmpty())
			return node;
		ConfigurationSection n = mc.getConfigurationSection(prefix);
		StringBuilder buf = new StringBuilder();
		String msg = n.getString(node, "No translation for " + node);
		Formatter form = new Formatter(buf);
		try{
			form.format(msg, varArgs);
		} catch(Exception e){
			System.err.println("Error getting message " + prefix + "." + node);
			for (Object o: varArgs){ System.err.println("argument=" + o);}
			e.printStackTrace();
		}
		return colorChat(buf.toString());
	}

	public static String getMessageAddPrefix(String pprefix, String prefix,String node, Object... varArgs) {
		ConfigurationSection n = mc.getConfigurationSection(prefix);
		StringBuilder buf = new StringBuilder(pprefix);
		String msg = n.getString(node, "No translation for " + node);
		Formatter form = new Formatter(buf);
		try{
			form.format(msg, varArgs);
		} catch(Exception e){
			System.err.println("Error getting message " + prefix + "." + node);
			for (Object o: varArgs){ System.err.println("argument=" + o);}
			e.printStackTrace();
		}
		return colorChat(buf.toString());
	}

	public static String colorChat(String msg) {
		return msg.replaceAll("&", "§");
	}
	public static String decolorChat(String msg){
		return msg.replaceAll("&", "§").replaceAll("\\§[0-9a-zA-Z]", "");	
	}

	public static boolean setConfig(File f){
		MC.f = f;
		return load();
	}


	public static boolean sendMessage(GuildPlayer player, String message) {
		Player p = Bukkit.getPlayerExact(player.getName());
		if (p != null && !p.isOnline())
			return false;
		return MC.sendMessage(p,message);
	}

	public static boolean sendMessage(Player p, String message){
		return MC.sendMessage((OfflinePlayer)p, message);
	}

	public static boolean sendMessage(OfflinePlayer p, String msg){
		if (msg ==null) return true;
		if (p == null){
			System.out.println(MC.colorChat(msg));
		} else {
			if (p.isOnline())
				p.getPlayer().sendMessage(MC.colorChat(msg));
		}			
		return true;
	}
	public static boolean sendMessage(CommandSender p, String msg){
		if (msg ==null) return true;
		if (p == null){
			System.out.println(MC.colorChat(msg));
		} else {
			p.sendMessage(MC.colorChat(msg));			
		}
		return true;
	}

	public static boolean load() {
		try {
			mc.load(f);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	public static boolean sendMultilineMessage(CommandSender sender, String message) {
		if (message ==null) return true;
		String[] msgs = message.split("\n");
		for (String msg: msgs){
			if (sender == null){
				System.out.println(MC.colorChat(msg));
			} else {
				sender.sendMessage(MC.colorChat(msg));			
			}			
		}
		return true;
	}
	public static boolean broadcastMultilineMessage(String message) {
		if (message ==null) return true;
		Server server = Bukkit.getServer();
		String[] msgs = message.split("\n");
		for (String msg: msgs){
			server.broadcastMessage(MC.colorChat(msg));
		}
		return true;
	}

	public static boolean broadcast(Guild guild, String message, GuildPlayer... exempt) {
		if (message ==null) return true;
		message = MC.colorChat(message);
		Set<String> exempted = new HashSet<String>();
		for(GuildPlayer p :exempt){
			if (p==null)
				continue;
			exempted.add(p.getName());
		}

		Server server = Bukkit.getServer();
		for(GuildPlayer gp:guild.getMembers()){
			if (exempted.contains(gp.getName()))
				continue;
			Player p = server.getPlayerExact(gp.getName());
			if(p!=null && p.isOnline()){
				p.sendMessage(message);}
		}
		return true;
	}
	public static void playerJoin(Player p){
		online.put(p.getName(), p);
	}
	public static Player playerLeft(Player p){
		return online.remove(p.getName());
	}
}
