package database;

import discord4j.core.object.entity.GuildEmoji;

import java.util.HashMap;
import java.util.Map;

public class RaidRole {
    public String name;
    public Integer amount;
    Map<String, Player> signups;
    public String emote;
    public GuildEmoji emoji;

    public RaidRole(String s){
        String[] split = s.split("_");
        this.name = split[0];
        this.amount = Integer.valueOf(split[1]);
        this.signups = new HashMap<>();
    }

    public RaidRole setEmote(GuildEmoji emoji){
        this.emoji = emoji;
        return this;
    }

    public RaidRole addPlayer(Player player){
        this.signups.put(player.discordName, player);
        return this;
    }
}
