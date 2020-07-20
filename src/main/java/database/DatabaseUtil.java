package database;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.GuildEmoji;
import main.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class DatabaseUtil {

    private static String url;
    public static void connectToDatabase(String fileName){
        url = "jdbc:sqlite:./" + fileName;
        try {
            Connection connection = DriverManager.getConnection(url);
            if (connection != null){
                DatabaseMetaData meta = connection.getMetaData();
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        createTable(url);
    }

    private static void createTable(String url){
        String sql1 = "create table if not exists players\n" +
                "(\n" +
                "    discordId     varchar(50) not null\n" +
                "        primary key,\n" +
                "    guildwarsname text\n" +
                ");\n" +
                "\n" +
                "create table if not exists signups\n" +
                "(\n" +
                "    signup_id INT not null\n" +
                "        primary key,\n" +
                "    message   text\n" +
                ");\n" +
                "\n" +
                "create table if not exists roles\n" +
                "(\n" +
                "    role_id   INTEGER\n" +
                "        primary key autoincrement,\n" +
                "    name      text not null,\n" +
                "    signup_id INT  not null\n" +
                "        references signups\n" +
                "            on update cascade on delete cascade,\n" +
                "    amount    int,\n" +
                "    emojiName text,\n" +
                "    emojiId   text\n" +
                ");\n" +
                "\n" +
                "create table if not exists player_roles\n" +
                "(\n" +
                "    role_id        varchar(50) not null\n" +
                "        references roles,\n" +
                "    player_role_id INTEGER     not null\n" +
                "        constraint player_roles_pk\n" +
                "            primary key autoincrement,\n" +
                "    player_id      varchar(50) not null\n" +
                "        references players\n" +
                ");\n" +
                "\n" +
                "create unique index if not exists player_roles_player_role_id_uindex\n" +
                "    on player_roles (player_role_id);\n" +
                "\n";

        try (
                Connection conn = DriverManager.getConnection(url);
                Statement statement = conn.createStatement()
        ){
            statement.execute(sql1);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public static void storePlayer(Player player){
        String sql = "INSERT INTO players(discordId, guildwarsname) VALUES(?,?)";
        try (
                Connection conn = DriverManager.getConnection(url);
                PreparedStatement statement = conn.prepareStatement(sql)
        ){
            statement.setString(1, player.discordName);
            statement.setString(2, player.gw2Name);
            statement.executeUpdate();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public static void storeSignup(SignUp signUp){
        Logger logger = LoggerFactory.getLogger("signup db");
        Map<String, Integer> insertedRolesIds = new HashMap<>();
        String sql = "INSERT INTO signups(signup_id, message) VALUES(?,?)";
        try (
                Connection conn = DriverManager.getConnection(url);
                PreparedStatement statement = conn.prepareStatement(sql)
        ){
            statement.setString(1, signUp.discordMessageId.asString());
            statement.setString(2, signUp.message);
            statement.executeUpdate();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        String sqlRoles = "INSERT INTO roles(signup_id, name, amount, emojiName, emojiId) VALUES(?,?,?,?,?)";
        try (
                Connection conn = DriverManager.getConnection(url);
                PreparedStatement statement = conn.prepareStatement(sqlRoles, Statement.RETURN_GENERATED_KEYS)
        ){
            List<String> names = new ArrayList<>();
            signUp.roles.values().forEach(role ->{
                try {
                    names.add(role.name);
                    statement.setString(1, signUp.discordMessageId.asString());
                    statement.setString(2, role.name);
                    statement.setInt(3, role.amount);
                    statement.setString(4, role.emojiName);
                    statement.setString(5, role.emojiId);
                    statement.addBatch();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            });
            int[] updates = statement.executeBatch();
            String insertedsql = "SELECT * FROM roles WHERE name IN (?) AND signup_id = '" + signUp.discordMessageId.asString() + "'";
            logger.debug(insertedsql);
            insertedsql = any(insertedsql, names.size());
            logger.debug(insertedsql);

            PreparedStatement selectStatement = conn.prepareStatement(insertedsql);
            for (int i = 1; i<=names.size(); i++){
                selectStatement.setString(i, names.get(i-1));
            }

            ResultSet rsIns = selectStatement.executeQuery();
            logger.debug("select");
            while (rsIns.next()){
                insertedRolesIds.put(rsIns.getString("name"), rsIns.getInt("role_id"));
            }
            signUp.roles.values().forEach(role -> role.dbId = insertedRolesIds.get(role.name));

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        String sqlPlayerSignups = "INSERT INTO player_roles(role_id, player_id) VALUES(?,?)";
        try (
                Connection conn = DriverManager.getConnection(url);
                PreparedStatement statement = conn.prepareStatement(sqlPlayerSignups)
        ){
            AtomicReference<Boolean> execute = new AtomicReference<>(false);
            signUp.roles.values().forEach(role -> role.signups.values().forEach(playerSignup -> {
                try {
                    execute.set(true);
                    logger.debug(playerSignup.discordName);
                    logger.debug(String.valueOf(insertedRolesIds.get(role.name)));
                    statement.setInt(1, insertedRolesIds.get(role.name));
                    statement.setString(2, playerSignup.discordName);
                    statement.addBatch();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }));
            if (execute.get()){
                statement.executeUpdate();
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public static String any(String sql, final int params) {
        // Create a comma-delimited list based on the number of parameters.
        final StringBuilder sb = new StringBuilder(
                new String(new char[params]).replace("\0", "?,")
        );

        // Remove trailing comma.
        sb.setLength(Math.max(sb.length() - 1, 0));

        // For more than 1 parameter, replace the single parameter with
        // multiple parameter placeholders.
        if (sb.length() > 1) {
            sql = sql.replace("(?)", "(" + sb + ")");
        }

        // Return the modified comma-delimited list of parameters.
        return sql;
    }


    public static Map<String, SignUp> getSignUps(){
        String sql = "SELECT discordId, r.role_id, s.signup_id, s.message, r.amount, r.emojiName, r.name, pr.player_id, pr.role_id, r.emojiId\n" +
                "FROM signups s\n" +
                "    INNER JOIN roles r on s.signup_id = r.signup_id\n" +
                "    LEFT OUTER JOIN player_roles pr on r.role_id = pr.role_id\n" +
                "    LEFT OUTER JOIN players p on p.discordId = pr.player_id";
        Map<String, SignUp> signUps = new HashMap<>();
        try (
                Connection conn = DriverManager.getConnection(url);
                PreparedStatement statement = conn.prepareStatement(sql)
        ){
            ResultSet rs = statement.executeQuery();
            Logger logger = LoggerFactory.getLogger("database");
            logger.debug("retrieve signups");
            while (rs.next()){
                SignUp signUp;
                String roleName = rs.getString("name");
                String signUpId = rs.getString("signup_id");
                if (!signUps.containsKey(signUpId)){
                    signUp = new SignUp(Snowflake.of(signUpId), rs.getString("message"));
                    signUps.put(signUp.discordMessageId.asString(), signUp);
                }
                signUp = signUps.get(signUpId);
                if (!signUp.roles.containsKey(roleName)){
                    RaidRole role = new RaidRole(roleName, rs.getInt("amount"));
                    role.emojiName = rs.getString("emojiName");
                    role.emojiId = rs.getString("emojiId");
                    role.dbId = rs.getInt("role_id");
                    signUp.roles.put(roleName, role);
                }
                String playerId = rs.getString("discordId");
                if(playerId != null && !signUp.roles.get(roleName).signups.containsKey(playerId)){
                    signUp.roles.get(roleName).signups.put(playerId, (Main.playerMap.get(playerId)));
                }
                logger.debug(signUp.getAsMessage());
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return signUps;
    }

    public static Map<String, Player> getPlayers(){
        String sql = "SELECT discordId, guildwarsname FROM players";
        Map<String, Player> players = new HashMap<>();
        try (
                Connection conn = DriverManager.getConnection(url);
                PreparedStatement statement = conn.prepareStatement(sql)
        ){
            ResultSet rs = statement.executeQuery();
            Logger logger = LoggerFactory.getLogger("database");
            while (rs.next()){
                logger.debug(rs.toString());
                Player pl = new Player(rs.getString("discordId"), rs.getString("guildwarsname"));
                logger.debug(pl.discordName);
                logger.debug(pl.gw2Name);
                players.put(pl.discordName, pl);
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return players;
    }

    public static void addSignup(Player player, RaidRole role){
        String sqlPlayerSignups = "INSERT INTO player_roles(role_id, player_id) VALUES(?,?)";
        try (
                Connection conn = DriverManager.getConnection(url);
                PreparedStatement statement = conn.prepareStatement(sqlPlayerSignups)
        ){
            statement.setInt(1, role.dbId);
            statement.setString(2, player.discordName);
            statement.execute();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public static void deleteSignup(Player player, RaidRole role){
        String sqlPlayerDelete = "DELETE FROM player_roles WHERE player_id = (?) AND role_id = (?)";
        try (
                Connection conn = DriverManager.getConnection(url);
                PreparedStatement statement = conn.prepareStatement(sqlPlayerDelete)
        ){
            statement.setString(1, player.discordName);
            statement.setInt(2, role.dbId);
            statement.execute();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }


}
