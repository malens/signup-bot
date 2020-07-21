package commands;

import com.beust.jcommander.JCommander;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.reaction.ReactionEmoji;
import main.Main;
import reactor.core.publisher.Mono;
import secret.SECRETS;

public class HelpCommand implements Command {

    @Override
    public Mono<Void> execute(MessageCreateEvent event) {
        StringBuilder sb = new StringBuilder();
        JCommander.newBuilder().programName("!group")
                .addObject(new CreateGroupCommand())
                .build().getUsageFormatter().usage(sb);

        return event.getMessage()
                .getChannel()
                .flatMap(channel -> channel.createMessage(sb.toString())
                        .then(event.getMessage().addReaction(ReactionEmoji.unicode(SECRETS.EMOTE_SUCCESS)))
                        .onErrorResume(error -> event.getMessage().addReaction(ReactionEmoji.unicode(SECRETS.EMOTE_ERROR)))
                );

    }
}
