package commands;

import com.beust.jcommander.Parameter;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.reaction.ReactionEmoji;
import main.StateStorage;
import reactor.core.publisher.Mono;
import secret.SECRETS;

import java.util.List;

public class AddChannelCommand extends BaseCommand implements Command {
    @Parameter(names = "-channel")
    private List<String> permittedChannels;

    private Boolean isAdmin(MessageCreateEvent event){
        return event.getMessage().getAuthor().isPresent() &&
                event.getGuildId().isPresent() &&
                StateStorage.serverAdmins.get(event.getGuildId().get()).contains(event.getMessage().getAuthor().get());
    }

    @Override
    public Mono<Void> execute(MessageCreateEvent event) {
        return Mono.just(event)
                .filter(this::isAdmin)
                .flatMap(ev -> {
                    try {
                        this.parseArguments(this, ev);
                    } catch (Exception e) {
                        return this.fail(ev);
                    }
                    this.permittedChannels.forEach(StateStorage::addPermittedChannel);
                    return this.confirm(ev);
                });
    }
}
