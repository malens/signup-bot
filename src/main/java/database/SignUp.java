package database;

import discord4j.common.util.Snowflake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import secret.SECRETS;

import java.util.*;
import java.util.concurrent.atomic.DoubleAccumulator;
import java.util.stream.Collectors;

public class SignUp {
    public Map<String, RaidRole> roles;
    public Snowflake discordMessageId;
    public String message;
    private Logger logger = LoggerFactory.getLogger("signup");
    private Boolean exclusive;

    public SignUp(Snowflake discordMessageId, String message) {
        this.discordMessageId = discordMessageId;
        this.roles = new HashMap<>();
        this.message = message;
    }
    public SignUp(Snowflake discordMessageId, String message, Boolean exclusive) {
        this(discordMessageId, message);
        this.exclusive = exclusive;
    }

    public String getAsMessage() {
        StringBuilder toReturn = new StringBuilder();
        toReturn.append(this.message).append("\n");
        toReturn.append(SECRETS.DIVIDER);
        Set<Player> distinctPlayers = new LinkedHashSet<>();
        Integer total = roles.values().parallelStream().reduce(0, (accumulator, role) -> accumulator + role.amount, Integer::sum);
        roles.values().forEach(role -> distinctPlayers.addAll(role.signups.values()));
        toReturn.append("Distinct peepos signed up: ").append(distinctPlayers.size()).append("/").append(total).append("\n");
        for (RaidRole role : roles.values()) {
            StringBuilder tmp = new StringBuilder();
            tmp.append(role.name).append(" ").append(role.signups.size()).append("/")
                    .append(role.amount).append("<:")
                    .append(role.emojiName).append(":")
                    .append(role.emojiId)
                    .append(">").append("\n").append(
                    role.signups.values().parallelStream().limit(role.amount)
                            .map(player -> "<@" + player.discordName + "> " + player.gw2Name).collect(Collectors.joining("\n")))
                    .append("\nBackups:\n").append(
                    role.signups.values().parallelStream().skip(role.amount)
                            .map(player -> "<@" + player.discordName + "> " + player.gw2Name).collect(Collectors.joining("\n"))
            );
            toReturn.append(tmp).append("\n").append(SECRETS.DIVIDER);
        }
        toReturn.append("To sign up as a role react with corresponding emote.");
        logger.debug(toReturn.toString());
        return toReturn.toString();
    }

    public Boolean notContains(String userId){
        long count = this.roles.values().parallelStream().filter(role -> role.hasPlayer(userId)).count();
        logger.debug(String.valueOf(count));
        return count == 0;
    }

    public Boolean isExclusive(){
        return this.exclusive == true;
    }
}
