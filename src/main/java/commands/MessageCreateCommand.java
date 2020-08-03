package commands;

import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.EmbedCreateSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

public abstract class MessageCreateCommand {

    protected Logger logger = LoggerFactory.getLogger("command " + this.getClass().getName());

    protected Boolean getAsText = false;

    public Mono<Message> createMessage(MessageChannel channel){
        if (getAsText){
            return channel.createMessage(this.getAsMessage());
        } else {
            return channel.createEmbed(this::getAsEmbed);
        }
    }

    public Mono<Message> editMessage(Message message){
        return message.edit(messageEditSpec -> {
            if (this.getAsText){
                messageEditSpec.setContent(this.getAsMessage());
            } else {
                messageEditSpec.setContent("").setEmbed(this::getAsEmbed);
            }
        });
    }

    public abstract String getAsMessage();

    public abstract EmbedCreateSpec getAsEmbed(EmbedCreateSpec spec);

    public void setGetAsText(Boolean getAsText){
        this.getAsText = getAsText;
    }

    public Boolean isText(){
        return this.getAsText;
    }

}
