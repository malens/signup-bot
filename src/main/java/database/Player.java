package database;

import java.util.Objects;

public class Player {
    public String discordName;
    public String gw2Name;

    public Player(String discordName, String gw2Name) {
        this.discordName = discordName;
        this.gw2Name = gw2Name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Player player = (Player) o;
        return discordName.equals(player.discordName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(discordName);
    }

    public String getAsMention(){
        return "<@" + this.discordName + "> " + this.gw2Name;
    }
}
