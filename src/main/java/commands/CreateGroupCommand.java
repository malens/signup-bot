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
import main.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import secret.SECRETS;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CreateGroupCommand implements Command {
    @Parameter(names = "-role", converter = RoleConverter.class, description = "List of roles to use in signup - format is \"role name_number\" or rolename_number")
    private List<RaidRole> roles;
    @Parameter(description = "message to display at the top of the signup")
    private List<String> message;
    @Parameter(names = "-excl", description = "Sets the group to exclusive mode")
    private Boolean exclusive = false;

    private SignUp signUp;

    private Map<Snowflake, List<GuildEmoji>> emojis;

    private Snowflake guildId;

    @Override
    public Mono<Void> execute(MessageCreateEvent event) {
        this.message = new ArrayList<>();
        this.roles = new ArrayList<>();
        String messageContent = event.getMessage().getContent().split(" ", 2)[1];
        Pattern p = Pattern.compile("(\"[^\"]*\")|[^ ]+");
        Matcher matcher = p.matcher(messageContent);
        Logger logger = LoggerFactory.getLogger("test");
        List<String> matches = new ArrayList<>();
        while (matcher.find()){
            String match = matcher.group(0).replaceAll("\"", "");
            logger.debug(match);
            matches.add(match);
        }
        String[]arrMatches = new String[matches.size()];
        JCommander.newBuilder()
                .addObject(this)
                .build()
                .parse(matches.toArray(arrMatches));
        this.signUp = new SignUp(event.getMessage().getId(), message == null ? "" :String.join(" ",message), exclusive == null ? true : exclusive);
        this.roles.parallelStream().forEach(role -> this.signUp.roles.put(role.name, role));

        return event.getGuild().doOnSuccess(guild -> {
            this.signUp.roles.values().forEach(role -> {
                role.setEmote(Bot.getRandomEmote(guild.getId()));
                logger.debug(role.emojiName);
            });
        }).then(
            event.getMessage()
                .getChannel()
                .flatMap(channel -> channel.createMessage(this.signUp.getAsMessage())
                                           .doOnSuccess(success -> {
                                               this.signUp.discordMessageId = success.getId();
                                               this.signUp.roles.values().parallelStream().forEach(role -> {
                                                   success.addReaction(ReactionEmoji.custom(Snowflake.of(role.emojiId), role.emojiName, false)).subscribe();
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



