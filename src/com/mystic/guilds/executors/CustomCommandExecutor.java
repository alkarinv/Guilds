package com.mystic.guilds.executors;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.TreeMap;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.mystic.guilds.MysticGuilds;
import com.mystic.guilds.controller.MC;
import com.mystic.guilds.objects.Guild;
import com.mystic.guilds.objects.GuildPlayer;
import com.mystic.zonedabone.mysticfactions.Faction;
import com.mystic.zonedabone.mysticfactions.FactionPlayer;


public abstract class CustomCommandExecutor implements CommandExecutor{
	static final boolean DEBUG = false;
	private HashMap<String,TreeMap<Integer,MethodWrapper>> methods = new HashMap<String,TreeMap<Integer,MethodWrapper>>();
	public static final int SELF = -2; /// Which index defines the sender
	///Use a linked hashMap so that help messages are displayed in roughly the order of the methods added
	protected LinkedHashMap<MCCommand, String> usage = new LinkedHashMap<MCCommand, String>();

	/**
	 * Custom arguments class so that we can return a modified sender and args
	 */
	public static class Arguments{
		GuildPlayer sender;
		Object[] args;
	}
	protected static class MethodWrapper{
		public MethodWrapper(Object obj, Method method){this.obj = obj; this.method = method;}
		Object obj; /// Object the method belongs to
		Method method; /// Method
	}

	/**
	 * When no arguments are supplied, no method is found
	 * What to display when this happens
	 * @param sender
	 */
	protected abstract void showHelp(CommandSender sender, Command command);

	static MysticGuilds plugin;
	protected CustomCommandExecutor(){
		addMethods(this, getClass().getMethods());
	}
	public static void setPluginInstance(MysticGuilds plugin){
		CustomCommandExecutor.plugin = plugin;
	}

	public void addMethods(Object obj, Method[] methodArray){
		for (Method method : methodArray){
			//			System.out.println("adding method " + method);
			MCCommand mc = method.getAnnotation(MCCommand.class);
			if (mc == null)
				continue;
			/// For each of the cmds, store them with the method
			for (String cmd : mc.cmds()){
				cmd = cmd.toLowerCase();
				TreeMap<Integer,MethodWrapper> mthds = methods.get(cmd);
				if (mthds == null){
					mthds = new TreeMap<Integer,MethodWrapper>();
				}
				int order = mc.order() != -1? mc.order() : Integer.MAX_VALUE-mthds.size();
				mthds.put(order, new MethodWrapper(obj,method));
				methods.put(cmd, mthds);
			}
			/// Add in the usages, for showing help messages
			if (MC.hasMessage("usage", mc.cmds()[0])){
				usage.put(mc,MC.getMessage("usage", mc.cmds()[0]));
			} else if (!mc.usageNode().isEmpty()){
				usage.put(mc, MC.getMessage("usage",mc.usageNode()));
			} else if (!mc.usage().isEmpty()){
				usage.put(mc, "&6"+mc.usage());
			} else {
				/// Maybe auto generate a usage?
			}

		}
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(MysticGuilds.useFactions && sender instanceof Player){
			FactionPlayer p = MysticGuilds.factions.getPlayer((Player)sender);
			if(p.getFaction() == Faction.NO_FACTION){
				sender.sendMessage(ChatColor.RED+"You are not in a faction yet.");
				return true;
			}
		}
		/// No method to handle, show some help
		if (args.length == 0){
			showHelp(sender, command);
			return true;
		}
		/// Find our method, and verify all the annotations
		TreeMap<Integer,MethodWrapper> methodmap = methods.get(args[0].toLowerCase());
		if (methodmap == null || methodmap.isEmpty()){
			return MC.sendMessage(sender, "That command does not exist!");
		}

		MCCommand mccmd = null;
		List<InvalidArgumentException> errs =null;
		boolean success = false;
		for (MethodWrapper mwrapper : methodmap.values()){
			mccmd = mwrapper.method.getAnnotation(MCCommand.class);
			if (mccmd.op() && !sender.isOp()) /// no op, no pass
				continue;
			try {
				Arguments newArgs= verifyArgs(mccmd,sender,command, label, args);
				mwrapper.method.invoke(mwrapper.obj,sender,newArgs.sender,command,label, newArgs.args);
				success = true;
				break; /// success on one
			} catch (InvalidArgumentException e){
				if (errs == null)
					errs = new ArrayList<InvalidArgumentException>();
				errs.add(e);
			} catch (Exception e) { /// Just all around bad
				e.printStackTrace();
			}
		}
		/// and handle all errors
		if (!success && errs != null && !errs.isEmpty()){
			if (errs.size() == 1){
				MC.sendMessage(sender, errs.get(0).getMessage());
				return true;
			}
			HashSet<String> errstrings = new HashSet<String>();
			for (InvalidArgumentException e: errs){
				errstrings.add(e.getMessage());
			}
			for (String msg : errstrings){
				MC.sendMessage(sender, msg);
			}
		}
		return true;
	}

	private Arguments verifyArgs(MCCommand cmd, CommandSender sender, Command command, String label, String[] args) 
			throws InvalidArgumentException{
		if (DEBUG)System.out.println("verifyArgs " + cmd +" sender=" +sender+", label=" + label+" args="+args);
		Arguments newArgs = new Arguments(); /// Our return value
		Object[] objs = new Object[args.length]; /// Our new array of castable arguments
		System.arraycopy( args, 0, objs, 0, args.length );
		newArgs.args = objs; /// Set our return object with the new castable arguments

		/// Verify min number of arguments
		if (args.length < cmd.min()){
			throw new InvalidArgumentException(MC.getMessage("main", "too_few_arguments") +getUsage(cmd));
		}
		/// Verfiy max number of arguments
		if (args.length > cmd.max()){
			throw new InvalidArgumentException(MC.getMessage("main", "too_many_arguments") +getUsage(cmd));
		}

		/// First convert our sender to either a GuildPlayer or an op (null)
		GuildPlayer player = null;
		if (sender instanceof Player){
			player = MysticGuilds.getPlayer((Player) sender);
			if (player == null) /// this should never happen... hopefully
				throw new InvalidArgumentException(MC.getMessage("main", "not_a_player", sender.getName()));
		} else {
			player = null;
		}
		if (cmd.op() && (player != null && !sender.isOp()))
			throw new InvalidArgumentException(ChatColor.RED +"You need to be an Admin to use this command");

		newArgs.sender = player; /// Put in our player to our return arguments

		/// Does this command need the same guild, implies only in game
		final boolean needsSameGuild = cmd.sameGuild();
		if (needsSameGuild){
			if (player == null)
				throw new InvalidArgumentException(MC.getMessage("main", "only_in_game"));				
			if (player.getGuild() == null)
				throw new InvalidArgumentException(MC.getMessage("main", "only_in_guild"));
		}			

		/// Verify guild arguments
		if (cmd.guild().length > 0){
			if (DEBUG)System.out.println("guild " + cmd.guild());
			for (int guildIndex : cmd.guild()){
				if (guildIndex >= args.length)
					throw new InvalidArgumentException("Index out of range. " + getUsage(cmd));

				Guild guild = MysticGuilds.getGuild(args[guildIndex]);
				if (guild == null)
					throw new InvalidArgumentException(MC.getMessage("main", "guild_not_exist", args[guildIndex]));

				/// Change over our string to a guild
				objs[guildIndex] = guild;
			}
		}

		/// In game check
		if (cmd.inGame() && player == null){
			throw new InvalidArgumentException(MC.getMessage("main", "only_in_game"));			
		}

		/// Check to see if the players are in guilds
		if (cmd.inGuild().length > 0){
			if (DEBUG)System.out.println("inGuild " + cmd.inGuild());

			for (int playerIndex : cmd.inGuild()){
				if (playerIndex == SELF){
					if (player == null)
						throw new InvalidArgumentException(MC.getMessage("main", "only_in_game"));			
					if (player.getGuild() == null)
						throw new InvalidArgumentException(MC.getMessage("main", "only_in_guild"));			
				} else {
					if (playerIndex >= args.length)
						throw new InvalidArgumentException("PlayerIndex out of range. " +getUsage(cmd));
					GuildPlayer gp = MysticGuilds.matchPlayer(args[playerIndex]);
					if (gp == null)
						throw new InvalidArgumentException(MC.getMessage("main", "not_a_player", args[playerIndex]));
					if (gp.getGuild() == null)
						throw new InvalidArgumentException(MC.getMessage("main", "other_only_in_guild", args[playerIndex]));
					if (needsSameGuild && !gp.getGuild().getName().equals(player.getGuild().getName())){
						throw new InvalidArgumentException(MC.getMessage("main", "other_not_in_your_guild", args[playerIndex]));				 
					}
					/// Change over our string to a guild player
					objs[playerIndex] = gp;
				}
			}
		}
		/// Check to see if the players are not in guilds
		if (cmd.notInGuild().length > 0){
			if (DEBUG)System.out.println("notInGuild " + cmd.notInGuild());
			for (int playerIndex : cmd.notInGuild()){
				if (playerIndex == SELF){
					if (player == null)
						throw new InvalidArgumentException(MC.getMessage("main", "only_in_game"));			
					if (player.getGuild() != null)
						throw new InvalidArgumentException(MC.getMessage("main", "only_not_in_guild", player.getGuild().getName()));			
				} else {
					if (playerIndex >= args.length)
						throw new InvalidArgumentException("PlayerIndex out of range. " + getUsage(cmd));
					GuildPlayer gp = MysticGuilds.matchPlayer(args[playerIndex]);
					if (gp == null)
						throw new InvalidArgumentException(MC.getMessage("main", "not_a_player", args[playerIndex]));

					if (gp.getGuild() != null)
						throw new InvalidArgumentException(MC.getMessage("main", "other_only_not_in_guild", args[playerIndex], 
								gp.getGuild().getName()));
					/// Change over our string to a guild player
					objs[playerIndex] = gp;
				}
			}
		}
		/// Verify ints
		if (cmd.ints().length > 0){
			for (int intIndex: cmd.ints()){
				if (intIndex >= args.length)
					throw new InvalidArgumentException("IntegerIndex out of range. " + getUsage(cmd));
				try {
					objs[intIndex] = Integer.parseInt(args[intIndex]);
				}catch (NumberFormatException e){
					throw new InvalidArgumentException(ChatColor.RED+(String)args[1]+" is not a valid integer.");
				}
			}
		}

		/// Check our permissions
		if (!cmd.perm().isEmpty() && !sender.hasPermission(cmd.perm()))
			throw new InvalidArgumentException(MC.getMessage("main", "no_permission"));

		return newArgs; /// Success
	}

	private String getUsage(MCCommand cmd) {
		if (!cmd.usageNode().isEmpty())
			return MC.getMessage("usage",cmd.usageNode());
		if (!cmd.usage().isEmpty())
			return cmd.usage();
		/// By Default try to return the message under this commands name in "usage.cmd"
		return MC.getMessage("usage", cmd.cmds()[0]);
	}


	public class InvalidArgumentException extends Exception {
		private static final long serialVersionUID = 1L;

		public InvalidArgumentException(String string) {
			super(string);
		}
	}

	static final int LINES_PER_PAGE = 8;
	public void help(CommandSender sender, Command command, Object[] args){
		Integer page = 1;

		if (args != null && args.length > 1){
			try{
				page = Integer.valueOf((String) args[1]);
			} catch (Exception e){
				MC.sendMessage(sender, ChatColor.RED+" " + args[1] +" is not a number, showing help for page 1.");
			}
		}

		LinkedHashSet<String> available = new LinkedHashSet<String>();
		LinkedHashSet<String> unavailable = new LinkedHashSet<String>();
		LinkedHashSet<String> onlyop = new LinkedHashSet<String>();
		for (MCCommand cmd : usage.keySet()){
			final String use = usage.get(cmd);
			if (cmd.op() && !sender.isOp())
				onlyop.add(use);
			else if (!cmd.perm().isEmpty() && !sender.hasPermission(cmd.perm()))
				unavailable.add(use);
			else 
				available.add(use);
		}
		int ncommands = available.size()+unavailable.size();
		if (sender.isOp())
			ncommands += onlyop.size();
		final int npages = (int) Math.ceil( (float)ncommands/LINES_PER_PAGE);
		if (page > npages || page <= 0){
			MC.sendMessage(sender, "&4That page doesnt exist, try help 1-"+npages);
			return;
		}
		if (command != null)
			MC.sendMessage(sender, "&eShowing page &6"+page +"/"+npages +"&6 : /"+command.getName()+" help <page number>");
		else 
			MC.sendMessage(sender, "&eShowing page &6"+page +"/"+npages +"&6 : /cmd help <page number>");
		int i=0;
		for (String use : available){
			i++;
			if (i < (page-1) *LINES_PER_PAGE || i >= page*LINES_PER_PAGE)
				continue;
			MC.sendMessage(sender, use);
		}
		for (String use : unavailable){
			i++;
			if (i < (page-1) *LINES_PER_PAGE || i >= page *LINES_PER_PAGE)
				continue;
			MC.sendMessage(sender, ChatColor.RED+"[Insufficient Perms] " + use);
		}
		if (sender.isOp()){
			for (String use : onlyop){
				i++;
				if (i < (page-1) *LINES_PER_PAGE || i >= page *LINES_PER_PAGE)
					continue;
				MC.sendMessage(sender, ChatColor.AQUA+"[OP only] &6"+use);
			}			
		}
	}

}
