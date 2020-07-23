package main;

import database.DatabaseUtil;
import database.Player;
import database.SignUp;
import server.ServerConfig;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class StateStorage {
    public static Map<String, SignUp> signUpMap;
    public static Map<String, Player> playerMap;
    public static Map<String, ServerConfig> serverMap;

    public static Map<String, List<String>> serverAdmins;
    public static Set<String> permittedChannels;

    public static void storeSignUp(SignUp signUp){
        signUpMap.put(signUp.discordMessageId.asString(), signUp);
        DatabaseUtil.storeSignup(signUp);
    }

    public static void addPlayer(Player player){
        playerMap.put(player.discordName, player);
        DatabaseUtil.storePlayer(player);
    }

    public static void addPermittedChannel(String channelId){
        permittedChannels.add(channelId);
        //todo store in database
    }
}
