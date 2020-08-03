package database;

import commands.MessageCreateCommand;
import discord4j.common.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import main.StateStorage;

import java.time.Instant;
import java.util.ArrayList;

import java.util.List;


public class RoleAssignment extends MessageCreateCommand {



    private String messageId;
    private List<RoleAssignmentInstance> roleIds;
    private String message;

    public String getMessageId() {
        return messageId;
    }
    public String getMessageText() {
        return message;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public List<RoleAssignmentInstance> getRoleIds() {
        return roleIds;
    }

    public void setRoleIds(List<RoleAssignmentInstance> roleIds) {
        this.roleIds = roleIds;
    }

    public RoleAssignment(String messageId, List<String> roleIds, Snowflake guildId, String message) {
        this.setGetAsText(false);
        this.messageId = messageId;
        this.roleIds = new ArrayList<>();
        this.message = message;
        for (String roleId : roleIds){
            this.roleIds.add((RoleAssignmentInstance) new RoleAssignmentInstance(roleId.replace("<@&", "").replace(">", "")).setEmote(StateStorage.getRandomEmote(guildId)));
        }
        logger.debug(this.message);
    }
    public RoleAssignment(String messageId, String message) {
        this.setGetAsText(false);
        this.messageId = messageId;
        this.roleIds = new ArrayList<>();
        this.message = message;
    }

    @Override
    public String getAsMessage() {
        StringBuilder msg = new StringBuilder();
        msg.append(this.message).append("\n");
        for (RoleAssignmentInstance rai : this.roleIds){
            msg.append(rai.getEmote()).append(" ").append(rai.getRoleMention()).append("\n");
        }
        return msg.toString();
    }

    @Override
    public EmbedCreateSpec getAsEmbed(EmbedCreateSpec spec) {
        spec.setFooter("Made with ♡ for the peepos.", "https://cdn.betterttv.net/emote/5ed4456d924aa35e32a67db4/3x")
                .setTitle(this.message)
                .setColor(Color.of(226, 118, 197))
                .setDescription("To self-assign a role react with the corresponding emote.");
        List<String> roleStrings = new ArrayList<>();
        roleIds.forEach(role -> roleStrings.add(role.getEmote() + ' ' + role.getRoleMention()));
        spec.addField("Available roles",  String.join("\n", roleStrings), false);
        spec.setTimestamp(Instant.now());
        return spec;
    }

    public void addRoleId(RoleAssignmentInstance instance){
        this.roleIds.add(instance);
    }
}
