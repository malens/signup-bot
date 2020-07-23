package Utils;


import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.MessageChannel;

public class EventUtils {

    public static Boolean isFromDm(MessageChannel messageChannel){
        return messageChannel.getType().equals(Channel.Type.DM);
    }

}
