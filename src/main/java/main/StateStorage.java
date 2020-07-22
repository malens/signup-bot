package main;

import database.DatabaseUtil;
import database.Player;
import database.SignUp;
import server.ServerConfig;

import java.util.Map;

public class StateStorage {
    public static Map<String, SignUp> signUpMap;
    public static Map<String, Player> playerMap;
    public static Map<String, ServerConfig> serverMap;

    public static void storeSignUp(SignUp signUp){
        signUpMap.put(signUp.discordMessageId.asString(), signUp);
        DatabaseUtil.storeSignup(signUp);
    }

    public static void addPlayer(Player player){
        playerMap.put(player.discordName, player);
        DatabaseUtil.storePlayer(player);
    }
}
