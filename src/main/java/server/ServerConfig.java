package server;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public class ServerConfig {
    public String discordId;
    public Set<String> allowedChannelNames;

    public ServerConfig(String discordId){
        this.discordId = discordId;
        this.allowedChannelNames = new LinkedHashSet<>();
    }

    public ServerConfig(String discordId, Collection<String> allowedChannelNames){
        this(discordId);
        this.allowedChannelNames.addAll(allowedChannelNames);
    }
}
