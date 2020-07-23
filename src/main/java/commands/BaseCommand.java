package commands;

import com.beust.jcommander.JCommander;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.reaction.ReactionEmoji;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import secret.SECRETS;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BaseCommand implements Command{

    protected Logger logger = LoggerFactory.getLogger("command " + this.getClass().getName());

    @Override
    public Mono<Void> execute(MessageCreateEvent event) {
        return null;
    }

    protected void parseArguments(Object source, MessageCreateEvent event) throws Exception {
        try {
            String messageContent = event.getMessage().getContent().split(" ", 2)[1];
            Pattern p = Pattern.compile("(\"[^\"]*\")|[^ ]+");
            Matcher matcher = p.matcher(messageContent);
            List<String> matches = new ArrayList<>();
            while (matcher.find()) {
                String match = matcher.group(0).replaceAll("\"", "");
                matches.add(match);
            }
            String[] arrMatches = new String[matches.size()];
            JCommander.newBuilder()
                    .addObject(source)
                    .build()
                    .parse(matches.toArray(arrMatches));
        } catch(Exception e){
            logger.error(e.getMessage());
            throw e;
        }

    }

    protected Mono<Void> confirm(MessageCreateEvent event) {
        return event
                .getMessage()
                .addReaction(ReactionEmoji.unicode(SECRETS.EMOTE_SUCCESS))
                .onErrorResume(error -> event.getMessage().addReaction(ReactionEmoji.unicode(SECRETS.EMOTE_ERROR)));
    }

    protected Mono<Void> fail(MessageCreateEvent event) {
        return event
                .getMessage()
                .addReaction(ReactionEmoji.unicode(SECRETS.EMOTE_ERROR));
    }
}
