package com.mystic.guilds.listeners;

import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.mystic.guilds.Defaults;
import com.mystic.guilds.MysticGuilds;
import com.mystic.guilds.controller.MC;
import com.mystic.guilds.controller.PermissionController;
import com.mystic.guilds.objects.GuildPlayer;
import com.mystic.guilds.objects.permissions.PermissionGroup;

public class MysticGuildsPlayerListener implements Listener {

	private MysticGuilds plugin;

	public MysticGuildsPlayerListener(MysticGuilds instance){
		plugin = instance;
	}

	static final String JOIN = " &ehas joined the game";
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerJoin(PlayerJoinEvent event){
		Player p = event.getPlayer();
		MC.playerJoin(p);
		try{
			plugin.process(p);
			GuildPlayer pg = MysticGuilds.getPlayer(p);
			if (pg.getGuild()!= null){
				PermissionController.updatePermissions(p,pg);
				MC.broadcast(pg.getGuild(), getLongName(pg)+JOIN, pg);
			}
		} catch(Exception e){
			e.printStackTrace();
		}
		if (MysticGuilds.getSelf().getConfig().getBoolean("showBukkitJoinMessages", false))
			event.setJoinMessage(null);
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerQuit(PlayerQuitEvent e){
		onLeave(e.getPlayer(), e.getQuitMessage());
		if (MysticGuilds.getSelf().getConfig().getBoolean("showBukkitQuitMessages", false))
			e.setQuitMessage(null);
	}

	@EventHandler
	public void onPlayerKick(PlayerKickEvent e){
		onLeave(e.getPlayer(), e.getLeaveMessage());
		if (MysticGuilds.getSelf().getConfig().getBoolean("showBukkitQuitMessages", false))
			e.setLeaveMessage(null);
	}
	
	static final String LEFT = " &ehas left the game";
	private void onLeave(Player player, String msg){
		MC.playerLeft(player);
		GuildPlayer pg = MysticGuilds.getPlayer(player);
		if (pg==null || pg.getGuild()==null)
			return;
		MC.broadcast(pg.getGuild(), getLongName(pg)+LEFT, pg);
	}

	private static String getLongName(GuildPlayer member){
		if (member.getGuild()==null)
			return member.getName();
		PermissionGroup rank = member.getRankGroup();
		if (rank == null)
			return member.getName();
		ChatColor rankColor = rank.getColor();

		StringBuilder sb = new StringBuilder();
		/// Deal with titles
		Set<PermissionGroup> titles = member.getTitles();
		StringBuilder sb2 = new StringBuilder();
		if (!titles.isEmpty()){
			sb.append("[");
			boolean first2=true;
			for (PermissionGroup title: titles){
				if (!first2) sb2.append(",");
				first2 = false;
				sb2.append(title.getLabel()+"");
			}					
			sb2.append(rankColor+"]");
		}
		/// Append the titles+name
		sb.append(rank.getColor()+"["+rank.getLabel()+"]"+sb2.toString()+rank.getColor()+member.getName());
		return sb.toString();
	}


	@EventHandler(priority = EventPriority.HIGHEST)
	public void onEntityDamage(EntityDamageEvent event) {
		if (event.isCancelled() || event.getEntityType() != EntityType.PLAYER || Defaults.CLAN_PVP_ON)
			return;
		Player target = (Player) event.getEntity();
		Player damager = getCause(event.getEntity().getLastDamageCause()); 
		if (damager == null)
			return;
		GuildPlayer gpTarget = MysticGuilds.getPlayer(target);
		if (gpTarget.getGuild() == null)
			return;
		GuildPlayer gpDamager = MysticGuilds.getPlayer(damager);
		if (gpDamager.getGuild() == null)
			return;
		if (gpTarget.getGuild().getName().equals(gpDamager.getGuild().getName())){
			event.setCancelled(true);
			MC.sendMessage(damager, "&eGuild PvP is &4off");
		}
	}

	private Player getCause(EntityDamageEvent lastDamageCause) {
		if (!(lastDamageCause instanceof EntityDamageByEntityEvent))
			return null;
		EntityDamageByEntityEvent edbee = (EntityDamageByEntityEvent) lastDamageCause;

		if (edbee.getDamager() instanceof Projectile) { /// we have some sort of projectile
			Projectile proj = (Projectile) edbee.getDamager();
			if (proj.getShooter() instanceof Player){ /// projectile was shot by a player
				return (Player) proj.getShooter();
			} else { /// projectile shot by some mob, or other source, get out of here
				return null;
			}
		} else if (! (edbee.getDamager() instanceof Player)) { /// killer not player
			return null;
		} else { /// Killer is a player
			return (Player) edbee.getDamager();
		}
	}
}
