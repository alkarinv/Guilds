create table mg_guild (
  name                      varchar(16) not null,
  funds                     float not null,
  faction                   varchar(32),
  label                     varchar(48),
  leader                    varchar(16),
  founder                   varchar(16),
  created                   datetime not null,
  constraint pk_mg_guild primary key (name))
;

create table mgp_perms (
  id                        integer auto_increment not null,
  name                      varchar(16),
  node                      varchar(128),
  admin_only                tinyint(1) default 0,
  constraint pk_mgp_perms primary key (id))
;

create table mg_guildplayer (
  name                      varchar(16) not null,
  last_online               datetime,
  guild                     varchar(16),
  constraint pk_mg_guildplayer primary key (name))
;

create table mgp_groups (
  id                        integer auto_increment not null,
  name                      varchar(16) not null,
  label                     varchar(48) not null,
  rank                      integer not null,
  track                     integer not null,
  guild                     varchar(16),
  constraint pk_mgp_groups primary key (id))
;

create table mg_relationship (
  id                        integer auto_increment not null,
  changed                   datetime,
  value                     integer,
  guild1                    varchar(16),
  guild2                    varchar(16),
  constraint pk_mg_relationship primary key (id))
;


create table mg_invites (
  mg_guild_name                  varchar(16) not null,
  mg_guildplayer_name            varchar(16) not null,
  constraint pk_mg_invites primary key (mg_guild_name, mg_guildplayer_name))
;

create table mgp_guilds2groups (
  mg_guild_name                  varchar(16) not null,
  mgp_groups_id                  integer not null,
  constraint pk_mgp_guilds2groups primary key (mg_guild_name, mgp_groups_id))
;

create table mg_guilds2relationships (
  mg_guild_name                  varchar(16) not null,
  mg_relationship_id             integer not null,
  constraint pk_mg_guilds2relationships primary key (mg_guild_name, mg_relationship_id))
;

create table mgp_players2groups (
  mg_guildplayer_name            varchar(16) not null,
  mgp_groups_id                  integer not null,
  constraint pk_mgp_players2groups primary key (mg_guildplayer_name, mgp_groups_id))
;

create table mgp_groups2perms (
  mgp_groups_id                  integer not null,
  mgp_perms_id                   integer not null,
  constraint pk_mgp_groups2perms primary key (mgp_groups_id, mgp_perms_id))
;
alter table mg_guild add constraint fk_mg_guild_leader_1 foreign key (leader) references mg_guildplayer (name) on delete restrict on update restrict;
create index ix_mg_guild_leader_1 on mg_guild (leader);
alter table mg_guild add constraint fk_mg_guild_founder_2 foreign key (founder) references mg_guildplayer (name) on delete restrict on update restrict;
create index ix_mg_guild_founder_2 on mg_guild (founder);
alter table mg_guildplayer add constraint fk_mg_guildplayer_guild_3 foreign key (guild) references mg_guild (name) on delete restrict on update restrict;
create index ix_mg_guildplayer_guild_3 on mg_guildplayer (guild);
alter table mgp_groups add constraint fk_mgp_groups_guild_4 foreign key (guild) references mg_guild (name) on delete restrict on update restrict;
create index ix_mgp_groups_guild_4 on mgp_groups (guild);
alter table mg_relationship add constraint fk_mg_relationship_guild1_5 foreign key (guild1) references mg_guild (name) on delete restrict on update restrict;
create index ix_mg_relationship_guild1_5 on mg_relationship (guild1);
alter table mg_relationship add constraint fk_mg_relationship_guild2_6 foreign key (guild2) references mg_guild (name) on delete restrict on update restrict;
create index ix_mg_relationship_guild2_6 on mg_relationship (guild2);



alter table mg_invites add constraint fk_mg_invites_mg_guild_01 foreign key (mg_guild_name) references mg_guild (name) on delete restrict on update restrict;

alter table mg_invites add constraint fk_mg_invites_mg_guildplayer_02 foreign key (mg_guildplayer_name) references mg_guildplayer (name) on delete restrict on update restrict;

alter table mgp_guilds2groups add constraint fk_mgp_guilds2groups_mg_guild_01 foreign key (mg_guild_name) references mg_guild (name) on delete restrict on update restrict;

alter table mgp_guilds2groups add constraint fk_mgp_guilds2groups_mgp_grou_02 foreign key (mgp_groups_id) references mgp_groups (id) on delete restrict on update restrict;

alter table mg_guilds2relationships add constraint fk_mg_guilds2relationships_mg_01 foreign key (mg_guild_name) references mg_guild (name) on delete restrict on update restrict;

alter table mg_guilds2relationships add constraint fk_mg_guilds2relationships_mg_02 foreign key (mg_relationship_id) references mg_relationship (id) on delete restrict on update restrict;

alter table mgp_players2groups add constraint fk_mgp_players2groups_mg_guil_01 foreign key (mg_guildplayer_name) references mg_guildplayer (name) on delete restrict on update restrict;

alter table mgp_players2groups add constraint fk_mgp_players2groups_mgp_gro_02 foreign key (mgp_groups_id) references mgp_groups (id) on delete restrict on update restrict;

alter table mgp_groups2perms add constraint fk_mgp_groups2perms_mgp_group_01 foreign key (mgp_groups_id) references mgp_groups (id) on delete restrict on update restrict;

alter table mgp_groups2perms add constraint fk_mgp_groups2perms_mgp_perms_02 foreign key (mgp_perms_id) references mgp_perms (id) on delete restrict on update restrict;
