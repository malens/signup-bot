package database;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.GuildEmoji;
import discord4j.core.object.reaction.ReactionEmoji;

public class ReactionEvent {

    public String emojiName;
    public String emojiId;

    public ReactionEvent setEmote(GuildEmoji emoji) {
        this.emojiId = emoji.getId().asString();
        this.emojiName = emoji.getName();
        return this;
    }

    public ReactionEmoji getEmoji() {
        return ReactionEmoji.custom(Snowflake.of(this.emojiId), this.emojiName, false);
    }

    public String getEmote() {
        return "<:" + emojiName + ":" + emojiId + ">";
    }

    public Boolean equalsEmoji(ReactionEmoji emoji) {
        return ReactionEmoji.custom(Snowflake.of(this.emojiId), this.emojiName, false).equals(emoji);
    }
}
