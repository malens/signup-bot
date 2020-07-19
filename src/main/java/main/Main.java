package main;

import commands.CreateGroupCommand;
import database.SignUp;
import discord4j.common.util.Snowflake;
import main.Bot;

import java.util.HashMap;
import java.util.Map;

public class Main {

    public static Map<String, SignUp> signUpMap;

    public static void main(String[] args) {
        signUpMap = new HashMap<>();
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
    }

}
