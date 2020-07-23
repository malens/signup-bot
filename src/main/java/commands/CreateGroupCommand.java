package commands;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import database.RaidRole;
import database.SignUp;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.GuildEmoji;
import discord4j.core.object.reaction.ReactionEmoji;
import main.Bot;
import main.StateStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import secret.SECRETS;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CreateGroupCommand extends BaseCommand implements Command {
    @Parameter(names = "-role", converter = RoleConverter.class, description = "List of roles to use in signup - format is \"role name_number\" or rolename_number")
    private List<RaidRole> roles;
    @Parameter(description = "message to display at the top of the signup")
    private List<String> message;
    @Parameter(names = "-excl", description = "Sets the group to exclusive mode")
    private Boolean exclusive = false;

    private SignUp signUp;

    public Command newInstance(){
        return new CreateGroupCommand();
    }

    @Override
    public Mono<Void> execute(MessageCreateEvent event) {
        this.message = new ArrayList<>();
        this.roles = new ArrayList<>();
        try {
            this.parseArguments(this, event);
            this.signUp = new SignUp(event.getMessage().getId(), message == null ? "" : String.join(" ", message), exclusive == null ? true : exclusive);
            if (this.roles.isEmpty()) {
                this.roles.add(new RaidRole("All", 10));
            }
            this.roles.forEach(role -> this.signUp.roles.put(role.name, role));
        } catch (Exception e) {
            return event.getMessage().addReaction(ReactionEmoji.unicode(SECRETS.EMOTE_ERROR));
        }

        event.getGuildId().ifPresent(guildId -> this.signUp.roles.values().forEach(role -> {
            role.setEmote(Bot.getRandomEmote(guildId));
            logger.debug(role.emojiName);
        }));

        return event.getMessage()
                .getChannel()
                .flatMap(channel -> channel.createEmbed(this.signUp::getAsEmbed))
                .flatMapMany(message -> {
                            this.signUp.discordMessageId = message.getId();
                            return Flux.fromIterable(this.signUp.roles.values())
                                    .flatMap(role -> message.addReaction(ReactionEmoji.custom(Snowflake.of(role.emojiId), role.emojiName, false)));
                        }
                )
                .then(this.confirm(event))
                .doOnSuccess(success -> StateStorage.storeSignUp(this.signUp))
                .onErrorResume(error -> event.getMessage().addReaction(ReactionEmoji.unicode(SECRETS.EMOTE_ERROR)))
                .then();
    }


    public static class RoleConverter implements IStringConverter<RaidRole> {
        @Override
        public RaidRole convert(String s) {
            return new RaidRole(s);
        }
    }
}



