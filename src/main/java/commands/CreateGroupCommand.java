package commands;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import database.RaidRole;
import database.SignUp;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.GuildEmoji;
import discord4j.core.object.entity.Message;
import discord4j.core.object.reaction.ReactionEmoji;
import main.Bot;
import main.Command;
import main.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import secret.SECRETS;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CreateGroupCommand implements Command {
    @Parameter(names = "-role", converter = RoleConverter.class)
    private List<RaidRole> roles;
    @Parameter(names = "-msg")
    private String message;

    private SignUp signUp;

    private Map<Snowflake, List<GuildEmoji>> emojis;

    private Snowflake guildId;

    @Override
    public Mono<Void> execute(MessageCreateEvent event) {
        this.roles = new ArrayList<>();
        String messageContent = event.getMessage().getContent().split(" ", 2)[1];
        JCommander.newBuilder()
                .addObject(this)
                .build()
                .parse(messageContent.split(" "));
        this.signUp = new SignUp(event.getMessage().getId());
        this.roles.parallelStream().forEach(role -> this.signUp.roles.put(role.name, role));
        Logger logger = LoggerFactory.getLogger("test");
        logger.debug("xD");
        return event.getGuild().doOnSuccess(guild -> {
            this.signUp.roles.values().forEach(role -> {
                role.setEmote(Bot.getRandomEmote(guild.getId()));
                logger.debug(role.emoji.getName());
            });
            logger.debug("xD");
        }).then(
            event.getMessage()
                .getChannel()
                .flatMap(channel -> channel.createMessage(this.signUp.getAsMessage())
                                           .doOnSuccess(success -> {
                                               this.signUp.discordMessageId = success.getId();
                                               this.signUp.roles.values().parallelStream().forEach(role -> {
                                                   logger.debug("sending reaction");
                                                   logger.debug(role.emoji.getName());
                                                   success.addReaction(ReactionEmoji.custom(role.emoji)).subscribe();
                                               });
                                           }))
                .then(event.getMessage().addReaction(ReactionEmoji.unicode(SECRETS.EMOTE_SUCCESS)))
                .doOnSuccess(success -> Main.storeSignUp(this.signUp))
                .onErrorResume(error -> event.getMessage().addReaction(ReactionEmoji.unicode(SECRETS.EMOTE_ERROR)))
        ).then();


    }


    public static class RoleConverter implements IStringConverter<RaidRole> {
        @Override
        public RaidRole convert(String s) {
            return new RaidRole(s);
        }
    }
}



