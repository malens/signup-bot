package commands;

import com.beust.jcommander.Parameter;
import database.RaidRole;
import database.RoleAssignment;
import database.SignUp;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.reaction.ReactionEmoji;
import main.StateStorage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import secret.SECRETS;

import javax.management.relation.Role;
import java.util.ArrayList;
import java.util.List;

public class RoleAssignCommand extends BaseCommand implements Command {

    @Parameter(names = "-role")
    private List<String> assignableRoles;
    @Parameter(description = "message to display")
    private List<String> message;
    private RoleAssignment roleAssignment;

    @Override
    public Command newInstance() {
        return new RoleAssignCommand();
    }

    @Override
    public Mono<Void> execute(MessageCreateEvent event) {
        try {
            this.parseArguments(this, event);
            this.roleAssignment = new RoleAssignment(
                    event.getMessage().getId().asString(),
                    this.assignableRoles,
                    event.getGuildId().get(), //TODO: this should only be called from a guild, but a check should still be implemented
                    message == null ? "" : String.join(" ", message)
            );
            this.message.forEach(message->logger.debug(message));

        } catch (Exception e) {
            return fail(event);
        }
        return event.getMessage()
                .getChannel()
                .flatMap(this.roleAssignment::createMessage)
                .flatMapMany(message -> {
                            this.roleAssignment.setMessageId(message.getId().asString());
                            StateStorage.storeAssignment(this.roleAssignment);
                            return Flux.fromIterable(this.roleAssignment.getRoleIds())
                                    .flatMap(role -> message.addReaction(ReactionEmoji.custom(Snowflake.of(role.emojiId), role.emojiName, false)));
                        }
                )
                .collectList()
                .then(this.confirm(event))
                .onErrorResume(error -> this.fail(event))
                .then();
    }

}
