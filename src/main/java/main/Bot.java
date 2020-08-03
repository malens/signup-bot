package main;

import Utils.EventUtils;
import commands.Command;
import database.Player;
import database.RoleAssignment;
import database.SignUp;
import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.event.domain.message.ReactionRemoveEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import secret.SECRETS;

import java.util.*;


public class Bot {

    private final GatewayDiscordClient client;
    private final Map<String, Command> commandMap = new LinkedHashMap<>();

    private final Logger logger = LoggerFactory.getLogger("Bot");

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
                .filter(event -> !event.getUserId().equals(this.client.getSelfId()))
                .flatMap(event -> Flux.fromIterable(StateStorage.signUpMap.entrySet())
                        .filter(entry -> entry.getKey().equals(event.getMessageId().asString()))
                        .flatMap(entry -> signUpReact(entry.getValue(), event)))
                .next()
                .subscribe();
        this.client.getEventDispatcher()
                .on(ReactionAddEvent.class)
                .filter(event -> !event.getUserId().equals(this.client.getSelfId()))
                .flatMap(event -> Flux.fromIterable(StateStorage.assignmentMap.entrySet())
                        .filter(entry -> entry.getKey().equals(event.getMessageId().asString()))
                        .flatMap(entry -> assignReact(entry.getValue(), event)))
                .next()
                .subscribe();
        this.client.getEventDispatcher()
                .on(ReactionRemoveEvent.class)
                .filter(event -> !event.getUserId().equals(this.client.getSelfId()))
                .flatMap(event -> Flux.fromIterable(StateStorage.assignmentMap.entrySet())
                        .filter(entry -> entry.getKey().equals(event.getMessageId().asString()))
                        .flatMap(entry -> assignUnReact(entry.getValue(), event)))
                .next()
                .subscribe();
        this.client.getEventDispatcher()
                .on(ReactionRemoveEvent.class)
                .filter(event -> !event.getUserId().equals(this.client.getSelfId()))
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
                    if (StateStorage.counters == null) {
                        StateStorage.counters = new LinkedHashMap<>();
                    }
                    if (StateStorage.guildEmojis == null) {
                        StateStorage.guildEmojis = new LinkedHashMap<>();
                    }
                    if (!StateStorage.guildEmojis.containsKey(emoji.getGuildId())) {
                        StateStorage.guildEmojis.put(emoji.getGuildId(), new LinkedHashMap<>());
                    }
                    StateStorage.counters.put(emoji.getGuildId(), 0);
                    StateStorage.guildEmojis.get(emoji.getGuildId()).put(emoji.getId(), emoji);
                    Logger logger = LoggerFactory.getLogger("test");
                    logger.debug(emoji.getName());
                })
                .subscribe();
        this.client.getEventDispatcher()
                .on(MessageCreateEvent.class)
                .filter(StateStorage::isFromPermittedChannel)
                .flatMap(event -> Mono.just(event.getMessage().getContent())
                        .flatMap(content -> Flux.fromIterable(commandMap.entrySet())
                                .filter(entry -> content.startsWith(SECRETS.COMMAND_CHAR + entry.getKey()))
                                .flatMap(entry -> entry.getValue().newInstance().execute(event))
                                .next()))
                .subscribe();
        this.client.getEventDispatcher()
                .on(MessageCreateEvent.class)
                .flatMap(event ->
                        event.getMessage()
                                .getChannel()
                                .filter(EventUtils::isFromDm)
                                .flatMap(channel -> whisperGW2Name(event))
                )
                .subscribe();

        return this;
    }

    public Bot build() {
        client.onDisconnect().block();
        return this;
    }

    private Mono<Void> signUpReact(SignUp signUp, ReactionAddEvent reactionAddEvent) {
        return Mono.just(reactionAddEvent)
                .filter(event -> !signUp.isExclusive() || signUp.notContains(event.getUserId().asString()))
                .flatMapMany(event ->
                        event.getMessage()
                                .flatMap(message -> Flux.fromIterable(signUp.roles.values())
                                        .filter(raidRole -> raidRole.equalsEmoji(event.getEmoji()))
                                        .flatMap(raidRole -> raidRole.addPlayerMono(event.getUserId().asString()))
                                        .filter(raidRole -> raidRole.getSignupNumber() + 1 != message.getReactions().stream().filter(reaction -> raidRole.equalsEmoji(reaction.getEmoji())).findFirst().get().getCount())
                                        .flatMap(raidRole -> {
                                            Mono<List<User>> reactors = message.getReactors(event.getEmoji()).collectList();
                                            return Flux.zip(
                                                    reactors.flatMapMany(raidRole::addMissingUsers),
                                                    reactors.flatMapMany(raidRole::removeUnsignedUsers)
                                            );
                                        })
                                        .then()

                                )
                )
                .then(reactionAddEvent.getMessage().flatMap(signUp::editMessage))
                .then();

    }

    private Mono<Void> assignReact(RoleAssignment roleAssignment, ReactionAddEvent reactionAddEvent) {
        logger.debug("assign react");
        return Mono.just(reactionAddEvent)
                .flatMapMany(event -> event.getMessage()
                        .flatMap(message -> Flux.fromIterable(roleAssignment.getRoleIds())
                                .filter(assignment -> assignment.equalsEmoji(event.getEmoji()))
                                .filter(roleAssignmentInstance -> reactionAddEvent.getMember().isPresent())
                                .flatMap(assignment ->
                                        reactionAddEvent.getMember().get().addRole(Snowflake.of(assignment.getRoleId()))
                                ).collectList()
                        )
                ).then();
    }
    private Mono<Void> assignUnReact(RoleAssignment roleAssignment, ReactionRemoveEvent reactionRemoveEvent) {
        logger.debug("assign react");
        return Mono.just(reactionRemoveEvent)
                .filter(event -> event.getGuildId().isPresent())
                .flatMapMany(event -> event.getMessage()
                        .flatMap(message -> Flux.fromIterable(roleAssignment.getRoleIds())
                                .filter(assignment -> assignment.equalsEmoji(event.getEmoji()))
                                .flatMap(assignment ->
                                        reactionRemoveEvent.getUser()
                                                .flatMap(user -> user.asMember(event.getGuildId().get())
                                                        .flatMap(member -> member.removeRole(Snowflake.of(assignment.getRoleId()))))
                                ).collectList()
                        )
                ).then();
    }

    private Mono<Void> signUpUnReact(SignUp signUp, ReactionRemoveEvent reactionRemoveEvent) {
        return Mono.just(reactionRemoveEvent)
                .flatMapMany(event ->
                        Flux.fromIterable(signUp.roles.values())
                                .filter(raidRole -> raidRole.equalsEmoji(event.getEmoji()))
                                .map(raidRole -> raidRole.removePlayer(event.getUserId().asString()))
                )
                .then(reactionRemoveEvent
                        .getMessage()
                        .flatMap(signUp::editMessage))
                .then();
    }

    private Mono<Void> whisperGW2Name(MessageCreateEvent event) {
        return Mono.just(event.getMessage().getContent())
                .filter(content -> content.matches("^[A-z ]+\\.\\d{4}$"))
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
