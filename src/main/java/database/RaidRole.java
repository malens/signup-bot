package database;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.GuildEmoji;
import discord4j.core.object.entity.User;
import discord4j.core.object.reaction.ReactionEmoji;
import main.StateStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.xml.crypto.Data;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RaidRole extends ReactionEvent {
    public String name;
    public Integer amount;
    Map<String, Player> signups;

    public Integer dbId;

    private final Logger logger = LoggerFactory.getLogger("Raid Role");

    public RaidRole(String s, Integer am) {
        this.name = s;
        this.amount = am;
        this.signups = new LinkedHashMap<>();
    }

    public RaidRole(String s) {
        this(s.split("_")[0], Integer.valueOf(s.split("_")[1]));
    }


    public Flux<RaidRole> addMissingUsers(List<User> users) {
        return Flux.fromStream(users.stream()
                .filter(user -> this.signups.values().stream().anyMatch(player -> player.discordName.equals(user.getId().asString())))
                .map(this::addPlayer));
    }

    public Flux<RaidRole> removeUnsignedUsers(List<User> users) {
        return Flux.fromStream(this.signups.values().parallelStream()
                .filter(player -> users.stream().noneMatch(u -> u.getId().asString().equals(player.discordName)))
                .map(this::removePlayer));
    }

    public Integer getSignupNumber() {
        return this.signups.size();
    }



    public RaidRole addPlayer(Player player) {
        if (!this.signups.containsKey(player.discordName)) {
            logger.debug("committing player");
            this.signups.put(player.discordName, player);
            DatabaseUtil.addSignup(player, this);
        }
        return this;
    }

    public RaidRole addPlayer(String playerId) {
        if (StateStorage.hasPlayer(playerId)) {
            this.addPlayer(StateStorage.getPlayer(playerId));
        } else {
            this.addPlayer(new Player(playerId, ""));
        }
        return this;
    }
    public Mono<RaidRole> addPlayerMono(String playerId) {
        return Mono.just(this.addPlayer(playerId));
    }

    public RaidRole addPlayer(User user) {
        logger.debug("adding player");
        this.addPlayer(user.getId().asString());
        return this;
    }

    public RaidRole removePlayer(String id) {
        DatabaseUtil.deleteSignup(this.signups.get(id), this);
        this.signups.remove(id);
        return this;
    }

    public RaidRole removePlayer(Player player) {
        DatabaseUtil.deleteSignup(this.signups.get(player.discordName), this);
        this.signups.remove(player.discordName);
        return this;
    }

    public Boolean hasPlayer(String id) {
        return this.signups.containsKey(id);
    }


}
