package database;

import discord4j.core.object.entity.GuildEmoji;

import javax.xml.crypto.Data;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class RaidRole {
    public String name;
    public Integer amount;
    Map<String, Player> signups;
    public String emojiName;
    public String emojiId;
    public Integer dbId;

    public RaidRole(String s, Integer am){
        this.name = s;
        this.amount = am;
        this.signups = new LinkedHashMap<>();
    }

    public RaidRole(String s){
        this(s.split("_")[0], Integer.valueOf(s.split("_")[1]));
    }


    public RaidRole setEmote(GuildEmoji emoji){
        this.emojiId = emoji.getId().asString();
        this.emojiName = emoji.getName();
        return this;
    }

    public String getEmote(){
        return "<:" + emojiName + ":" + emojiId + ">";
    }

    public RaidRole addPlayer(Player player){
        this.signups.put(player.discordName, player);
        DatabaseUtil.addSignup(player, this);
        return this;
    }

    public void removePlayer(String id){
        DatabaseUtil.deleteSignup(this.signups.get(id), this);
        this.signups.remove(id);
    }

    public Boolean hasPlayer(String id){
        return this.signups.containsKey(id);
    }
}
