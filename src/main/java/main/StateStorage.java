package main;

import database.DatabaseUtil;
import database.Player;
import database.RoleAssignment;
import database.SignUp;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.GuildEmoji;
import discord4j.core.object.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.ServerConfig;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


public class StateStorage {
    public static Map<Snowflake, Map<Snowflake, GuildEmoji>> guildEmojis;
    private static Logger logger = LoggerFactory.getLogger("StateStorage");

    public static Map<String, SignUp> signUpMap;
    public static Map<String, Player> playerMap;
    public static Map<String, ServerConfig> serverMap;
    public static Map<String, RoleAssignment> assignmentMap;
    static Map<Snowflake, Integer> counters;

    public static void storeAssignment(RoleAssignment assignment){
        logger.debug(assignment.getMessageId());
        assignment.getRoleIds().forEach(roleAssignmentInstance -> logger.debug(roleAssignmentInstance.getRoleId()));
        assignmentMap.put(assignment.getMessageId(), assignment);
        DatabaseUtil.storeAssignment(assignment);
    }

    public static void storeSignUp(SignUp signUp) {
        signUpMap.put(signUp.discordMessageId.asString(), signUp);
        DatabaseUtil.storeSignup(signUp);
    }

    public static Boolean hasGuild(Optional<Snowflake> guildId) {
        return guildId.filter(snowflake -> serverMap.containsKey(snowflake.asString())).isPresent();
    }

    public static Optional<ServerConfig> getServerConfig(Optional<Snowflake> guildId){
        return guildId.map(snowflake -> serverMap.get(snowflake.asString()));
    }

    public static ServerConfig getServerConfig(Snowflake guildId){
        return serverMap.get(guildId.asString());
    }

    public static Boolean isAdminForGuild(Optional<Snowflake> guildId, Optional<User> user) {
        if (guildId.isPresent() && user.isPresent()){
            return getServerConfig(guildId.get()).isAdmin(user.get().getId().asString());
        } else {
            return false;
        }

    }

    public static Boolean isPermittedChannel(Optional<Snowflake> guildId, Optional<Snowflake> channelId) {
        return guildId
                .filter(guild ->
                        channelId.filter(channel ->
                                serverMap.containsKey(guild.asString()) &&
                                        serverMap.get(guild.asString()).hasChannel(channel.asString()))
                                .isPresent())
                .isPresent();
    }

    public static Boolean isFromPermittedChannel(MessageCreateEvent event){
        return isPermittedChannel(event.getGuildId(), Optional.of(event.getMessage().getChannelId()));
    }

    public static void addPlayer(Player player) {
        playerMap.put(player.discordName, player);
        DatabaseUtil.storePlayer(player);
    }

    public static void addPermittedChannel(String channelId, String guildId) {
        logger.debug(channelId);
        logger.debug(guildId);
        if (serverMap.containsKey(guildId)) {
            serverMap.get(guildId).addChannel(channelId);
            DatabaseUtil.storePermittedChannel(channelId, guildId);
        }
    }

    public static void removeChannel(String channelId, String guildId) {
        if (serverMap.containsKey(guildId)) {
            serverMap.get(guildId).removeChannel(channelId);
            DatabaseUtil.removePermittedChannel(channelId, guildId);
        }
    }

    public static Boolean hasPlayer(String playerId) {
        return playerMap.containsKey(playerId);
    }

    public static Player getPlayer(String playerId) {
        return playerMap.get(playerId);
    }

    public static GuildEmoji getEmoteById(Snowflake guildId, Snowflake emoteId) {
        return guildEmojis.get(guildId).get(emoteId);
    }

    public static GuildEmoji getRandomEmote(Snowflake guildId) {
        counters.put(guildId, (counters.get(guildId) + 1) % guildEmojis.get(guildId).size());
        return guildEmojis.get(guildId).values().parallelStream().collect(Collectors.toList()).get(counters.get(guildId));
    }
}
