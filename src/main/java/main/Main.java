package main;

import commands.CreateGroupCommand;
import database.DatabaseUtil;
import database.Player;
import database.SignUp;

import java.util.Map;

public class Main {

    public static Map<String, SignUp> signUpMap;
    public static Map<String, Player> playerMap;

    public static void main(String[] args) {
        DatabaseUtil.connectToDatabase("gw2botdb");
        playerMap = DatabaseUtil.getPlayers();
        signUpMap = DatabaseUtil.getSignUps();
        Bot bot = new Bot()
                .withCommand(
                        event -> event.getMessage().getChannel().flatMap(channel->channel.createMessage("xD").then()),
                        "ping"
                )
                .withCommand(
                        new CreateGroupCommand(),
                        "group"
                )
                .subscribe()
                .build();
    }

    public static void storeSignUp(SignUp signUp){
        signUpMap.put(signUp.discordMessageId.asString(), signUp);
        DatabaseUtil.storeSignup(signUp);
    }

    public static void addPlayer(Player player){
        playerMap.put(player.discordName, player);
        DatabaseUtil.storePlayer(player);
    }

}
