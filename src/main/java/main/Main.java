package main;

import commands.CreateGroupCommand;
import commands.HelpCommand;
import database.DatabaseUtil;
import database.Player;
import database.SignUp;
import server.ServerConfig;

import java.util.Map;

public class Main {

    public static Map<String, SignUp> signUpMap;
    public static Map<String, Player> playerMap;
    public static Map<String, ServerConfig> serverMap;

    public static void main(String[] args) {
        DatabaseUtil.connectToDatabase("gw2botdb");
        playerMap = DatabaseUtil.getPlayers();
        signUpMap = DatabaseUtil.getSignUps();
        serverMap = DatabaseUtil.getServers();
        Bot bot = new Bot()
                .withCommand(
                        new HelpCommand(),
                        "help"
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
