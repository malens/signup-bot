package database;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.GuildEmoji;
import discord4j.core.object.reaction.ReactionEmoji;
import main.StateStorage;

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

    public RaidRole addPlayer(String playerId){
        if (StateStorage.hasPlayer(playerId)){
            this.addPlayer(StateStorage.getPlayer(playerId));
        } else {
            this.addPlayer(new Player(playerId, ""));
        }
        return this;
    }

    public RaidRole removePlayer(String id){
        DatabaseUtil.deleteSignup(this.signups.get(id), this);
        this.signups.remove(id);
        return this;
    }

    public Boolean hasPlayer(String id){
        return this.signups.containsKey(id);
    }

    public Boolean equalsEmoji(ReactionEmoji emoji){
        return ReactionEmoji.custom(Snowflake.of(this.emojiId), this.emojiName, false).equals(emoji);
    }
}
