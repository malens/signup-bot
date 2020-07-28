package database;

import discord4j.common.util.Snowflake;
import discord4j.core.object.Embed;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.EmbedData;
import discord4j.rest.util.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import secret.SECRETS;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.DoubleAccumulator;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SignUp {
    public Map<String, RaidRole> roles;
    public Snowflake discordMessageId;
    public String message;
    private final Logger logger = LoggerFactory.getLogger("signup");
    private Boolean exclusive;

    public SignUp(Snowflake discordMessageId, String message) {
        this.discordMessageId = discordMessageId;
        this.roles = new LinkedHashMap<>();
        this.message = message;
    }
    public SignUp(Snowflake discordMessageId, String message, Boolean exclusive) {
        this(discordMessageId, message);
        this.exclusive = exclusive;
    }

    public static <T> Collector<T, ?, Stream<T>> reverse() {
        return Collectors.collectingAndThen(Collectors.toList(), list -> {
            Collections.reverse(list);
            return list.stream();
        });
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
            String regular = role.signups.values().stream().limit(role.amount)
                    .map(Player::getAsMention).collect(Collectors.joining("\n"));
            String backups = role.signups.values().stream().skip(role.amount)
                    .map(Player::getAsMention).collect(Collectors.joining("\n"));
            tmp.append(role.name).append(" ").append(role.signups.size()).append("/")
                    .append(role.amount).append(role.getEmote()).append("\n").append(regular)
                    .append("\nBackups:\n").append(backups);
            toReturn.append(tmp).append("\n").append(SECRETS.DIVIDER);
        }
        toReturn.append("To sign up as a role react with corresponding emote.");
        logger.debug(toReturn.toString());
        return toReturn.toString();
    }

    public EmbedCreateSpec getAsEmbed(EmbedCreateSpec spec){
        Set<Player> distinctPlayers = new LinkedHashSet<>();
        roles.values().forEach(role -> distinctPlayers.addAll(role.signups.values()));
        Integer total = roles.values().parallelStream().reduce(0, (accumulator, role) -> accumulator + role.amount, Integer::sum);
        spec.setFooter("Made with ♡ for the peepos.", "https://cdn.betterttv.net/emote/5ed4456d924aa35e32a67db4/3x")
                .setTitle(this.message)
                .setColor(Color.of(226, 118, 197))
                .setDescription("All peepos signed up: " + distinctPlayers.size() + "/" + total + "\nTo sign up as a role react with corresponding emote.");
        roles.values().forEach(role -> {
            String regular = role.signups.values().stream().limit(role.amount)
                    .map(Player::getAsMention).collect(Collectors.joining("\n"));
            String backups = role.signups.values().stream().skip(role.amount)
                    .map(Player::getAsMention).collect(Collectors.joining("\n"));
            spec.addField(role.getEmote() + ' ' + role.name + " (" + role.signups.size() + "/" + role.amount + ")",regular.isEmpty() && backups.isEmpty() ? "This place is waiting for you!" : regular + "\nBackups:\n" + backups , false);

        });
        spec.setTimestamp(Instant.now());
        return spec;
    }

    public Boolean notContains(String userId){
        long count = this.roles.values().parallelStream().filter(role -> role.hasPlayer(userId)).count();
        logger.debug(String.valueOf(count));
        return count == 0;
    }

    public Boolean isExclusive(){
        return this.exclusive;
    }
}
