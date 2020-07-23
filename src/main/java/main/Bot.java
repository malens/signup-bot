package main;

import commands.Command;
import database.Player;
import database.SignUp;
import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.event.domain.message.ReactionRemoveEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.GuildEmoji;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.EmbedCreateSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import secret.SECRETS;

import javax.swing.plaf.nimbus.State;
import java.util.*;
import java.util.stream.Collectors;


public class Bot {

    public static Map<Snowflake, Map<Snowflake, GuildEmoji>> guildEmojis;
    private static Map<Snowflake, Integer> counters;

    private final GatewayDiscordClient client;
    private final Map<String, Command> commandMap = new LinkedHashMap<>();

    public Bot(String token) {
        this.client = DiscordClientBuilder.create(token)
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
                .filter(event -> event.getUserId() != this.client.getSelfId())
                .flatMap(event -> Flux.fromIterable(StateStorage.signUpMap.entrySet())
                        .filter(entry -> entry.getKey().equals(event.getMessageId().asString()))
                        .flatMap(entry -> signUpReact(entry.getValue(), event)))
                .next()
                .subscribe();
        this.client.getEventDispatcher()
                .on(ReactionRemoveEvent.class)
                .filter(event -> event.getUserId() != this.client.getSelfId())
                .flatMap(event -> Flux.fromIterable(StateStorage.signUpMap.entrySet())
                        .filter(entry -> entry.getKey().equals(event.getMessageId().asString()))
                        .flatMap(entry -> signUpUnReact(entry.getValue(), event)))
                .next()
                .subscribe();
        this.client.getEventDispatcher()
                .on(GuildCreateEvent.class)
                .flatMap(event -> Mono.just(event.getGuild()))
                .flatMap(Guild::getEmojis)
                .doOnNext(emoji -> {
                    if (counters == null) {
                        counters = new LinkedHashMap<>();
                    }
                    if (guildEmojis == null) {
                        guildEmojis = new LinkedHashMap<>();
                    }
                    if (!guildEmojis.containsKey(emoji.getGuildId())) {
                        guildEmojis.put(emoji.getGuildId(), new LinkedHashMap<>());
                    }
                    counters.put(emoji.getGuildId(), 0);
                    guildEmojis.get(emoji.getGuildId()).put(emoji.getId(), emoji);
                    Logger logger = LoggerFactory.getLogger("test");
                    logger.debug(emoji.getName());
                })
                .subscribe();
        this.client.getEventDispatcher()
                .on(MessageCreateEvent.class)
                .filter(event -> event.getGuildId().isPresent() &&
                                StateStorage.serverMap.containsKey(event.getGuildId().get().asString()) &&
                                StateStorage.serverMap.get(event.getGuildId().get().asString())
                                        .allowedChannelNames.contains(event.getMessage().getChannelId().asString()))
                .flatMap(event -> Mono.just(event.getMessage().getContent())
                        .flatMap(content -> Flux.fromIterable(commandMap.entrySet())
                                .filter(entry -> content.startsWith(SECRETS.COMMAND_CHAR + entry.getKey()))
                                .flatMap(entry -> entry.getValue().execute(event))
                                .next()))
                .subscribe();
        this.client.getEventDispatcher()
                .on(MessageCreateEvent.class)
                .flatMap(event -> event.getMessage().getChannel().filter(channel -> channel.getType() == Channel.Type.DM)
                        .flatMap(channel -> whisperGW2Name(event))
                )
                .subscribe();

        return this;
    }

    public Bot build() {
        client.onDisconnect().block();
        return this;
    }

    public static GuildEmoji getEmoteById(Snowflake guildId, Snowflake emoteId){
        return guildEmojis.get(guildId).get(emoteId);
    }

    public static GuildEmoji getRandomEmote(Snowflake guildId) {
        counters.put(guildId, (counters.get(guildId) + 1) % guildEmojis.get(guildId).size());
        return guildEmojis.get(guildId).values().parallelStream().collect(Collectors.toList()).get(counters.get(guildId));
    }

    private Mono<Void> signUpReact(SignUp signUp, ReactionAddEvent event) {
        return event.getMessage()
                .filter(message -> !signUp.isExclusive() || signUp.notContains(event.getUserId().asString()))
                .flatMap(x -> {signUp.roles.values().parallelStream()
                    .filter(value -> ReactionEmoji.custom(Snowflake.of(value.emojiId), value.emojiName, false).equals(event.getEmoji()))
                    .forEach(val -> {
                        if (StateStorage.playerMap.containsKey(event.getUserId().asString())){
                            val.addPlayer(StateStorage.playerMap.get(event.getUserId().asString()));
                        } else {
                            val.addPlayer(new Player(event.getUserId().asString(), ""));
                        }
                    });
                    return Mono.just(x);
                })
                .flatMap(msg -> msg.edit(messageEditSpec -> {
                    messageEditSpec.setEmbed(signUp::getAsEmbed);
                })).then();
    }

    private Mono<Void> signUpUnReact(SignUp signUp, ReactionRemoveEvent event){
        return event.getMessage()
                .doOnSuccess(x -> signUp.roles.values().parallelStream()
                .filter(value -> ReactionEmoji.custom(Snowflake.of(value.emojiId), value.emojiName, false).equals(event.getEmoji()))
                .forEach(val -> val.removePlayer(event.getUserId().asString())))
                .flatMap(msg -> msg.edit(messageEditSpec -> {
                    messageEditSpec.setEmbed(signUp::getAsEmbed);
                })).then();
    }

    private Mono<Void> whisperGW2Name(MessageCreateEvent event){
        return Mono.just(event.getMessage().getContent())
                .filter(content -> content.matches("^[A-z]+\\.\\d{4}$"))
                .flatMap(msg ->
                    Mono.justOrEmpty(event.getMessage().getAuthor()).doOnSuccess(author ->
                            StateStorage.addPlayer(new Player(
                                            author.getId().asString(),
                                            event.getMessage().getContent()
                                    )
                            ))
                    ).then();
    }
}
