import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.JCommander;
import commands.CreateGroupCommand;
import commands.HelpCommand;
import database.DatabaseUtil;
import main.Args;
import main.Bot;
import main.StateStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.plaf.nimbus.State;
import java.io.*;



public class Main {

    private static String apikey;

    public static void main(String[] args) {
        Args argss = new Args();
        JCommander.newBuilder()
                .addObject(argss)
                .build()
                .parse(args);

        try {
            InputStream reader = new FileInputStream(argss.apikey);
            BufferedReader br = new BufferedReader(new InputStreamReader(reader));
            Main.apikey = br.readLine();
            br.close();
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


        DatabaseUtil.connectToDatabase("gw2botdb");
        StateStorage.playerMap = DatabaseUtil.getPlayers();
        StateStorage.signUpMap = DatabaseUtil.getSignUps();
        StateStorage.serverMap = DatabaseUtil.getServers();

        Bot bot = new Bot(Main.apikey)
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
