package commands;

import com.beust.jcommander.Parameter;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.reaction.ReactionEmoji;
import main.StateStorage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import secret.SECRETS;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AddChannelCommand extends BaseCommand implements Command {
    @Parameter(names = "-add")
    private List<String> permittedChannels;
    @Parameter(names = "-remove")
    private List<String> removedChannels;

    private Boolean isAdmin(MessageCreateEvent event) {
        logger.debug("checkadmin");
        return StateStorage.isAdminForGuild(event.getGuildId(), event.getMessage().getAuthor());
    }

    @Override
    public Command newInstance() {
        return new AddChannelCommand();
    }

    @Override
    public Mono<Void> execute(MessageCreateEvent event) {
        permittedChannels = new ArrayList<>();
        removedChannels = new ArrayList<>();
        return Mono.just(event)
                .filter(this::isAdmin)
                .flatMap(ev -> {
                    logger.debug("permitted channels");
                    try {
                        this.parseArguments(this, ev);
                    } catch (Exception e) {
                        return this.fail(ev);
                    }
                    logger.debug("permitted channels");
                    permittedChannels.forEach(channel -> logger.debug(channel));
                    permittedChannels.parallelStream().forEach(channel ->
                            event.getGuildId().ifPresent(guildId ->
                                    StateStorage.addPermittedChannel(channel, guildId.asString())
                            )
                    );
                    removedChannels.parallelStream().forEach(channel ->
                            event.getGuildId().ifPresent(guildId ->
                                    StateStorage.removeChannel(channel, guildId.asString())
                            )
                    );
                    return this.confirm(event);
                })
                .then();
    }
}
