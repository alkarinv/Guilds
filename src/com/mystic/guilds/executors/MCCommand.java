package com.mystic.guilds.executors;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface MCCommand {
	/// This is required, the cmd and all its aliases
	String[] cmds() default {};

	/// Verify the number of parameters, inGuild and notInGuild imply min if they have an index > number of args
	int min() default 0;
	int max() default Integer.MAX_VALUE;

	boolean inGame() default false;
	int[] inGuild() default {}; /// Implies inGame = true
	int[] notInGuild() default {};
	boolean sameGuild() default false;

	int[] ints() default {};

	int[] guild() default {};
	int[] playerQuery() default {};

	String usage() default "";
	String usageNode() default "";
	
	String perm() default "";
	
	boolean op() default false;
	int order() default -1; /// Specify the order in which a command is checked if same name
}