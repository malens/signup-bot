package main;

import database.Player;
import database.SignUp;
import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.guild.GuildEvent;
import discord4j.core.event.domain.lifecycle.ConnectEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.GuildEmoji;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.discordjson.json.gateway.GuildCreate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import secret.SECRETS;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class Bot {

    public static Map<Snowflake, List<GuildEmoji>> guildEmojis;
    private static Map<Snowflake, Integer> counters;

    private final GatewayDiscordClient client;
    private final Map<String, Command> commandMap = new HashMap<>();

    public Bot() {
        this.client = DiscordClientBuilder.create(SECRETS.BOT_TOKEN)
                .build()
                .login()
                .block();

    }

    public Bot withCommand(Command command, String name) {
        this.commandMap.put(name, command);
        return this;
    }

    public Bot subscribe() {
        this.client.getEventDispatcher()
                .on(ReactionAddEvent.class)
                .flatMap(event -> Flux.fromIterable(Main.signUpMap.entrySet())
                        .filter(entry -> entry.getKey().equals(event.getMessageId().asString()))
                        .flatMap(entry -> signUpReact(entry.getValue(), event)))
                .next()
                .subscribe();
        this.client.getEventDispatcher()
                .on(GuildCreateEvent.class)
                .flatMap(event -> Mono.just(event.getGuild()))
                .flatMap(Guild::getEmojis)
                .doOnNext(emoji -> {
                    if (counters == null) {
                        counters = new HashMap<>();
                    }
                    if (guildEmojis == null) {
                        guildEmojis = new HashMap<>();
                    }
                    if (!guildEmojis.containsKey(emoji.getGuildId())) {
                        guildEmojis.put(emoji.getGuildId(), new ArrayList<>());
                    }
                    counters.put(emoji.getGuildId(), 0);
                    guildEmojis.get(emoji.getGuildId()).add(emoji);
                    Logger logger = LoggerFactory.getLogger("test");
                    logger.debug(emoji.getName());
                })
                .subscribe();
        this.client.getEventDispatcher()
                .on(MessageCreateEvent.class)
                .flatMap(event -> Mono.just(event.getMessage().getContent())
                        .flatMap(content -> Flux.fromIterable(commandMap.entrySet())
                                .filter(entry -> content.startsWith(SECRETS.COMMAND_CHAR + entry.getKey()))
                                .flatMap(entry -> entry.getValue().execute(event))
                                .next()))
                .subscribe();

        return this;
    }

    public Bot build() {
        client.onDisconnect().block();
        return this;
    }

    public static GuildEmoji getRandomEmote(Snowflake guildId) {
        counters.put(guildId, (counters.get(guildId) + 1) % guildEmojis.get(guildId).size());
        return guildEmojis.get(guildId).get(counters.get(guildId));
    }

    private Mono<Void> signUpReact(SignUp signUp, ReactionAddEvent event) {
        return event.getMessage()
                .doOnSuccess(x -> signUp.roles.values().parallelStream()
                    .filter(value -> ReactionEmoji.custom(value.emoji).equals(event.getEmoji()))
                    .forEach(val -> val.addPlayer(new Player(event.getUserId().asString(), ""))))
                .flatMap(msg -> msg.edit(messageEditSpec -> {
                    messageEditSpec.setContent(signUp.getAsMessage());
                })).then();

    }

}
