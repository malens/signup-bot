package commands;

import com.beust.jcommander.JCommander;
import discord4j.core.event.domain.message.MessageCreateEvent;
import reactor.core.publisher.Mono;


public class HelpCommand extends BaseCommand implements Command {

    @Override
    public Command newInstance() {
        return new HelpCommand();
    }

    @Override
    public Mono<Void> execute(MessageCreateEvent event) {
        StringBuilder sb = new StringBuilder();
        JCommander.newBuilder().programName("!group")
                .addObject(new CreateGroupCommand())
                .build().getUsageFormatter().usage(sb);
        JCommander.newBuilder().programName("!role")
                .addObject(new RoleAssignCommand())
                .build().getUsageFormatter().usage(sb);
        JCommander.newBuilder().programName("!permit")
                .addObject(new AddChannelCommand())
                .build().getUsageFormatter().usage(sb);

        return Mono.just(event)
                .flatMap(evt -> evt.getMessage().getChannel())
                .flatMap(messageChannel -> messageChannel.createMessage(sb.toString()))
                .doOnSuccess(message -> this.confirm(event))
                .doOnError(message -> this.fail(event))
                .then();

    }
}
