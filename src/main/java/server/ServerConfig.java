package server;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public class ServerConfig {
    public String discordId;
    public Set<String> allowedChannelIds;
    public Set<String> admins;

    public ServerConfig(String discordId){
        this.discordId = discordId;
        this.allowedChannelIds = new LinkedHashSet<>();
        this.admins = new LinkedHashSet<>();
    }

    public ServerConfig(String discordId, Collection<String> allowedChannelNames){
        this(discordId);
        this.allowedChannelIds.addAll(allowedChannelNames);
    }

    public ServerConfig addChannel(String channelId){
        this.allowedChannelIds.add(channelId);
        return this;
    }
    public ServerConfig removeChannel(String channelId){
        this.allowedChannelIds.remove(channelId);
        return this;
    }

    public Boolean isAdmin(String adminId){
        return admins.contains(adminId);
    }

    public Boolean hasChannel(String channelId){
        return this.allowedChannelIds.contains(channelId);
    }
}
