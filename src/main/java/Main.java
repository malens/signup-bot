import com.beust.jcommander.JCommander;
import commands.CreateGroupCommand;
import commands.HelpCommand;
import database.DatabaseUtil;
import main.Args;
import main.Bot;
import main.StateStorage;


public class Main {

    public static void main(String[] args) {
        Args argss = new Args();
        JCommander.newBuilder()
                .addObject(argss)
                .build()
                .parse(args);

        DatabaseUtil.connectToDatabase("gw2botdb");
        StateStorage.playerMap = DatabaseUtil.getPlayers();
        StateStorage.signUpMap = DatabaseUtil.getSignUps();
        StateStorage.serverMap = DatabaseUtil.getServers();
        Bot bot = new Bot(argss.apikey)
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



}
