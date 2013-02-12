package com.mystic.guilds.util;

import java.util.Set;

import com.mystic.guilds.objects.permissions.GuildPermission;
import com.mystic.guilds.objects.permissions.PermissionGroup;

public class PrintingUtil {

	public static String groupsToString(Set<PermissionGroup> groups) {
		if (groups == null || groups.isEmpty())
			return "None";
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (PermissionGroup group: groups){
			if (!first) sb.append(", ");
			first = false;
			sb.append(group.getLabel());
		}
		return sb.toString();
	}

	public static String permsToString(Set<GuildPermission> perms) {
		if (perms == null || perms.isEmpty())
			return "None";
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (GuildPermission perm: perms){
			if (!first) sb.append(", ");
			first = false;
			sb.append(perm.getName());
		}
		return sb.toString();
	}

}
