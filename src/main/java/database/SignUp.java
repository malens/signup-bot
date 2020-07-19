package database;

import discord4j.common.util.Snowflake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import secret.SECRETS;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SignUp {
    public Map<String, RaidRole> roles;
    public Snowflake discordMessageId;

    public SignUp(Snowflake discordMessageId) {
        this.discordMessageId = discordMessageId;
        this.roles = new HashMap<>();
    }

    public String getAsMessage() {
        Logger logger = LoggerFactory.getLogger("mylog");
        StringBuilder toReturn = new StringBuilder();
        toReturn.append("Group sign up\n");
        toReturn.append(SECRETS.DIVIDER);
        for (RaidRole role : roles.values()) {
            StringBuilder tmp = new StringBuilder();
            tmp.append(role.name).append(" ").append(role.signups.size()).append("/")
                    .append(role.amount).append("\n").append(
                    role.signups.values().parallelStream()
                            .map(player -> "<@" + player.discordName + "> " + player.gw2Name).collect(Collectors.joining("\n"))
            );
            toReturn.append(tmp).append("\n").append(SECRETS.DIVIDER);
        }
        roles.values().forEach(
                role -> toReturn
                        .append("To sign up as ").append(role.name)
                        .append(" react with ").append("<:")
                        .append(role.emoji.getName()).append(":")
                        .append(role.emoji.getId().asString())
                        .append(">"));
        logger.debug(toReturn.toString());
        return toReturn.toString();
    }
}
