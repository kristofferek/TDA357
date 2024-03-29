/* This is the driving engine of the program. It parses the command-line
 * arguments and calls the appropriate methods in the other classes.
 *
 * You should edit this file in three ways:
 * 1) Insert your database username and password in the proper places.
 * 2) Implement the generation of the world by reading the world file.
 * 3) Implement the three functions showPossibleMoves, showPlayerAssets
 *    and showScores.
 */
import javax.xml.transform.Result;
import java.sql.*; // JDBC stuff.
import java.util.Properties;
import java.io.*;  // Reading user input.
import java.util.ArrayList;
import java.util.Random;

public class Game {
    public class Player {
        String playername;
        String personnummer;
        String country;
        private String startingArea;

        public Player(String name, String nr, String cntry, String startingArea) {
            this.playername = name;
            this.personnummer = nr;
            this.country = cntry;
            this.startingArea = startingArea;
        }
    }

    String USERNAME = "tda357_048";
    String PASSWORD = "pWwTuqIB";

    /* Print command optionssetup.
    * /!\ you don't need to change this function! */
    public void optionssetup() {
        System.out.println();
        System.out.println("Setup-Options:");
        System.out.println("		n[ew player] <player name> <personnummer> <country>");
        System.out.println("		d[one]");
        System.out.println();
    }

    /* Print command options.
    * /!\ you don't need to change this function! */
    public void options() {
        System.out.println("\nOptions:");
        System.out.println("    n[ext moves] [area name] [area country]");
        System.out.println("    l[ist properties] [player number] [player country]");
        System.out.println("    s[cores]");
        System.out.println("    r[efund] <area1 name> <area1 country> [area2 name] [area2 country]");
        System.out.println("    b[uy] [name] <area1 name> <area1 country> [area2 name] [area2 country]");
        System.out.println("    m[ove] <area1 name> <area1 country>");
        System.out.println("    p[layers]");
        System.out.println("    q[uit move]");
        System.out.println("    [...] is optional\n");
    }


    void insertCountry(Connection conn, String country) {
        try {
            //Countries insert
            PreparedStatement statement = conn.prepareStatement("INSERT INTO Countries (name) VALUES (?)");
            statement.setString(1, country);
            statement.executeUpdate();
        } catch (SQLException e) {}
    }

    void insertArea(Connection conn, String country, String name, String population) {
        try {
            //Area insert
            PreparedStatement statement = conn.prepareStatement("INSERT INTO Areas (country, name, population) " +
                    "VALUES (?, ?, cast(? as INT))");
            statement.setString(1, country);
            statement.setString(2, name);
            statement.setString(3, population);
            statement.executeUpdate();
        } catch (SQLException e) {}
    }

    void clearDatabase(Connection conn) throws SQLException{
        String query = "DELETE FROM hotels; DELETE FROM roads; DELETE FROM persons; DELETE FROM towns; " +
                "DELETE FROM cities; DELETE FROM areas; DELETE FROM countries;";
        PreparedStatement statement = conn.prepareStatement(query);
        statement.executeUpdate();
    }

    /* Given a town name, country and population, this function
      * should try to insert an area and a town (and possibly also a country)
      * for the given attributes.
      */
    void insertTown(Connection conn, String name, String country, String population) throws SQLException {
        insertCountry(conn, country);
        insertArea(conn, country, name, population);
        //Town insert
        PreparedStatement statement = conn.prepareStatement("INSERT INTO Towns (country, name) VALUES (?, ?)");
        statement.setString(1, country);
        statement.setString(2, name);
        statement.executeUpdate();
    }

    /* Given a city name, country and population, this function
      * should try to insert an area and a city (and possibly also a country)
      * for the given attributes.
      * The city visitbonus should be set to 0.
      */
    void insertCity(Connection conn, String name, String country, String population) throws SQLException {
        insertCountry(conn, country);
        insertArea(conn, country, name, population);
        //City insert
        PreparedStatement statement = conn.prepareStatement("INSERT INTO Cities (country, name, visitbonus) " +
                "VALUES (?, ?, cast(? as NUMERIC))");
        statement.setString(1, country);
        statement.setString(2, name);
        statement.setString(3, "0");
        statement.executeUpdate();
    }

    /* Given two areas, this function
      * should try to insert a government owned road with tax 0
      * between these two areas.
      */
    void insertRoad(Connection conn, String area1, String country1, String area2, String country2) throws SQLException {
        PreparedStatement statement = conn.prepareStatement("INSERT INTO Roads (fromcountry, fromarea, tocountry, " +
                "toarea, ownercountry, ownerpersonnummer, roadtax) VALUES (?, ?, ?, ?, ?, ?, cast(? as NUMERIC))");
        statement.setString(1, country1);
        statement.setString(2, area1);
        statement.setString(3, country2);
        statement.setString(4, area2);
        statement.setString(5, "");
        statement.setString(6, "");
        statement.setString(7, "0");
        statement.executeUpdate();
    }

    /* Given a player, this function
     * should return the area name of the player's current location.
     */
    String getCurrentArea(Connection conn, Player person) throws SQLException {
        PreparedStatement statement = conn.prepareStatement("SELECT locationarea FROM persons WHERE country = ? " +
                "AND personnummer = ?");
        statement.setString(1, person.country);
        statement.setString(2, person.personnummer);
        ResultSet rs = statement.executeQuery();
        String area = "";
        if (rs.next()) {
            area = rs.getString(1);
        }
        rs.close();
        return area;
    }

    /* Given a player, this function
     * should return the country name of the player's current location.
     */
    String getCurrentCountry(Connection conn, Player person) throws SQLException {
        PreparedStatement statement = conn.prepareStatement("SELECT locationcountry FROM persons WHERE country = ? " +
                "AND personnummer = ?");
        statement.setString(1, person.country);
        statement.setString(2, person.personnummer);
        ResultSet rs = statement.executeQuery();
        String country = "";
        if (rs.next()){
            country = rs.getString(1);
        }
        rs.close();
        return country;
    }

    /* Given a player, this function
      * should try to insert a table entry in persons for this player
     * and return 1 in case of a success and 0 otherwise.
      * The location should be random and the budget should be 1000.
     */
    int createPlayer(Connection conn, Player person) throws SQLException {
        ResultSet result;
        PreparedStatement statement;
        String query;
        try
        {
            query = "SELECT COUNT(*) FROM Areas WHERE name <> ?";
            statement = conn.prepareStatement(query);
            statement.setString(1, "");
            result = statement.executeQuery();
            if (result.next())
            {
                Random rg = new Random();
                String offset = rg.nextInt(result.getInt(1)) + "";
                query = "SELECT * FROM Areas WHERE name <> ? OFFSET cast(? as BIGINT)";
                statement = conn.prepareStatement(query);
                statement.setString(1, "");
                statement.setString(2, offset);
                result = statement.executeQuery();
                if (result.next())
                {
                    String locationcountry = result.getString(1);
                    String locationarea = result.getString(2);
                    statement = conn.prepareStatement("INSERT INTO Persons (country, personnummer, name, " +
                            "locationcountry, locationarea, budget) VALUES (?, ?, ?, ?, ?, cast(? AS NUMERIC))");
                    statement.setString(1, person.country);
                    statement.setString(2, person.personnummer);
                    statement.setString(3, person.playername);
                    statement.setString(4, locationcountry);
                    statement.setString(5, locationarea);
                    statement.setString(6, "1000");
                    statement.executeUpdate();
                    result.close();
                    return 1;
                }
            }
            result.close();
        }
        catch (SQLException e) {
            System.out.println(e);
        }
        return 0;
    }

    /* Given a player and an area name and country name, this function
     * sould show all directly-reachable destinations for the player from the
     * area from the arguments.
     * The output should include area names, country names and the associated road-taxes
      */
    void getNextMoves(Connection conn, Player person, String area, String country) throws SQLException {
        try{
            String query =
                    "SELECT toCountry, toArea, Min(cost) as cost FROM " +
                            "(SELECT toCountry, toArea, CASE WHEN ownerCountry = ? AND ownerPersonnummer = ? THEN 0 ELSE roadtax END AS cost FROM roads WHERE fromCountry = ? AND fromArea = ? UNION " +
                            "SELECT fromCountry AS toCountry,fromArea AS toArea,  CASE WHEN ownerCountry = ? AND ownerPersonnummer = ? THEN 0 ELSE roadtax END AS cost FROM roads WHERE toCountry = ? AND toArea = ?) AS staffas" +
                            " GROUP BY toCountry, toArea";
            PreparedStatement statement = conn.prepareStatement(query);
            statement.setString(1,person.country);
            statement.setString(2,person.personnummer);
            statement.setString(3,country);
            statement.setString(4,area);
            statement.setString(5,person.country);
            statement.setString(6,person.personnummer);
            statement.setString(7,country);
            statement.setString(8,area);
            ResultSet result = statement.executeQuery();
            while (result.next())
            {
                System.out.println("Country : " + result.getString("toCountry") + ", Area : " + result.getString("toArea") + ", Cost : " + result.getString("cost") + "\n");
            }
            result.close();
        }
        catch (SQLException e)
        {
            System.out.println(e);
        }
    }

    /* Given a player, this function
       * sould show all directly-reachable destinations for the player from
     * the player's current location.
     * The output should include area names, country names and the associated road-taxes
     */
    void getNextMoves(Connection conn, Player person) throws SQLException {
        try
        {
            int budget;
            PreparedStatement statement = conn.prepareStatement("SELECT * FROM persons " +
                    "WHERE country = ? AND personnummer = ?");
            statement.setString(1,person.country);
            statement.setString(2,person.personnummer);
            ResultSet result = statement.executeQuery();
            System.out.println("All roads: \n");
            if(result.next())
            {
                budget = result.getInt("budget");
                statement = conn.prepareStatement("SELECT * FROM nextmoves " +
                        "WHERE personcountry = ? AND personnummer = ?");
                statement.setString(1, person.country);
                statement.setString(2, person.personnummer);
                result = statement.executeQuery();
                while(result.next())
                {
                    if (result.getInt("cost")<budget){
                    System.out.println("Country name : " + result.getString("destcountry") + ", Area name : " + result.getString("destarea") + ", Cost :" + result.getString("cost") + "\n");
                    }
                }
            }
            result.close();
        }
        catch (SQLException e)
        {
            System.out.println(e);
        }
    }

    /* Given a personnummer and a country, this function
     * should list all properties (roads and hotels) of the person
     * that is identified by the tuple of personnummer and country.
     */
    void listProperties(Connection conn, String personnummer, String country) {
        System.out.println("Properties of " + personnummer+ ", "+ country);
        try{
            PreparedStatement statement = conn.prepareStatement("SELECT * FROM roads " +
                    "WHERE ownerpersonnummer = ? AND ownercountry = ?");
            statement.setString(1, personnummer);
            statement.setString(2, country);
            ResultSet rs = statement.executeQuery();
            System.out.println("Roads:");
            while (rs.next()){
                System.out.println("("+rs.getString(1) + ", "+ rs.getString(2)+ ") <--->"
                        + "("+rs.getString(3) + ", "+ rs.getString(4) + ")");
            }

            statement = conn.prepareStatement("SELECT name, locationcountry, locationname FROM hotels " +
                    "WHERE ownerpersonnummer = ? AND ownercountry = ?");
            statement.setString(1, personnummer);
            statement.setString(2, country);
            rs = statement.executeQuery();
            System.out.println("");
            System.out.println("Hotels:");
            while (rs.next()){
                System.out.println(rs.getString(1) + " in "+ rs.getString(3)+ ", "
                        +rs.getString(2)+ ".");
            }
            rs.close();
        } catch (SQLException e){
            System.out.println(e);
        }
    }

    /* Given a player, this function
     * should list all properties of the player.
     */
    void listProperties(Connection conn, Player person) throws SQLException {
        listProperties(conn, person.personnummer, person.country);
    }

    /* This function should print the budget, assets and refund values for all players.
     */
    void showScores(Connection conn) throws SQLException {
        PreparedStatement statement = conn.prepareStatement("SELECT * FROM assetsummary ORDER BY budget ASC");
        ResultSet rs = statement.executeQuery();
        while (rs.next()){
            System.out.println(rs.getString(3) + " (" + rs.getString(1) + ", "
                    + rs.getString(2)+ ")");
            System.out.println("Budget: " + rs.getString(4));
            System.out.println("Assets: " + rs.getString(5));
            System.out.println("Reclaimable: " + rs.getString(6));
            System.out.println("- - - - -");
        }
        rs.close();
    }

    /* Given a player, a from area and a to area, this function
     * should try to sell the road between these areas owned by the player
     * and return 1 in case of a success and 0 otherwise.
     */
    int sellRoad(Connection conn, Player person, String area1, String country1, String area2, String country2) throws SQLException {
        try{
            PreparedStatement statement = conn.prepareStatement("DELETE FROM Roads " +
                    "WHERE fromcountry = ? AND fromarea = ? AND tocountry = ? AND toarea = ?" +
                    "AND ownercountry = ? AND ownerpersonnummer = ?");
            statement.setString(1, country1);
            statement.setString(2, area1);
            statement.setString(3, country2);
            statement.setString(4, area2);
            statement.setString(5, person.country);
            statement.setString(6, person.personnummer);
            statement.executeUpdate();
        } catch (SQLException e){
            System.out.println(e);
            return 0;
        }
        return 1;
    }

    /* Given a player and a city, this function
     * should try to sell the hotel in this city owned by the player
     * and return 1 in case of a success and 0 otherwise.
     */
    int sellHotel(Connection conn, Player person, String city, String country) throws SQLException {
        try{
            PreparedStatement statement = conn.prepareStatement("DELETE FROM Hotels " +
                    "WHERE locationcountry = ? AND locationname = ? AND ownercountry = ? AND ownerpersonnummer = ?");
            statement.setString(1, country);
            statement.setString(2, city);
            statement.setString(3, person.country);
            statement.setString(4, person.personnummer);
            statement.executeUpdate();
        } catch (SQLException e){
            System.out.println(e);
            return 0;
        }
        return 1;
    }

    /* Given a player, a from area and a to area, this function
     * should try to buy a road between these areas owned by the player
     * and return 1 in case of a success and 0 otherwise.
     */
    int buyRoad(Connection conn, Player person, String area1, String country1, String area2, String country2) throws SQLException {
        try{
            PreparedStatement statement = conn.prepareStatement("INSERT INTO Roads VALUES (?, ?, ?, ?, ?, ?, cast(? as NUMERIC))");
            statement.setString(1, country1);
            statement.setString(2, area1);
            statement.setString(3, country2);
            statement.setString(4, area2);
            statement.setString(5, person.country);
            statement.setString(6, person.personnummer);
            statement.setString(7, "13.5");
            statement.executeUpdate();
        } catch (SQLException e){
            System.out.println(e);
            return 0;
        }
        return 1;
    }

    /* Given a player and a city, this function
     * should try to buy a hotel in this city owned by the player
     * and return 1 in case of a success and 0 otherwise.
     */
    int buyHotel(Connection conn, Player person, String name, String city, String country) throws SQLException {
        try{
            PreparedStatement statement = conn.prepareStatement("INSERT INTO Hotels VALUES (?, ?, ?, ?, ?)");
            statement.setString(1, name);
            statement.setString(2, country);
            statement.setString(3, city);
            statement.setString(4, person.country);
            statement.setString(5, person.personnummer);
            statement.executeUpdate();
        } catch (SQLException e){
            System.out.println(e);
            return 0;
        }
        return 1;
    }

    /* Given a player and a new location, this function
     * should try to update the players location
     * and return 1 in case of a success and 0 otherwise.
     */
    int changeLocation(Connection conn, Player person, String area, String country) throws SQLException {
        try{
            PreparedStatement statement = conn.prepareStatement("UPDATE persons " +
                    "SET locationcountry = ?, locationarea = ? " +
                    "WHERE country = ? AND personnummer = ?");
            statement.setString(1, country);
            statement.setString(2, area);
            statement.setString(3, person.country);
            statement.setString(4, person.personnummer);
            statement.executeUpdate();
        } catch (SQLException e){
            System.out.println(e);
            return 0;
        }
        return 1;
    }

    /* This function should add the visitbonus of 1000 to a random city
      */
    void setVisitingBonus(Connection conn) throws SQLException {
        PreparedStatement statement = conn.prepareStatement("SELECT Count(*) FROM Cities VALUES");
        ResultSet rs = statement.executeQuery();
        int size = 0;
        if (rs.next())
            size = rs.getInt(1);
        statement = conn.prepareStatement("SELECT country,name FROM Cities OFFSET floor(random()*"
                + size +") LIMIT 1");
        rs = statement.executeQuery();
        if (rs.next()){
            statement = conn.prepareStatement("UPDATE Cities SET visitbonus = visitbonus + 1000 " +
                    "WHERE country = ? AND name = ?");
            statement.setString(1, rs.getString(1));
            statement.setString(2, rs.getString(2));
        }
        rs.close();
    }

    /* This function should print the winner of the game based on the currently highest budget.
      */
    void announceWinner(Connection conn) throws SQLException {
        PreparedStatement statement = conn.prepareStatement("SELECT country, personnummer, name, budget FROM persons ORDER BY budget DESC");
        ResultSet rs = statement.executeQuery();
        if (rs.next()){
            System.out.println("The winner is" + rs.getString(3) + " (" + rs.getString(1) + ", "
                    + rs.getString(2)+ ")");
        }
        rs.close();
    }

    void play (String worldfile) throws IOException {

        // Read username and password from config.cfg
        try {
            BufferedReader nf = new BufferedReader(new FileReader("config.cfg"));
            String line;
            if ((line = nf.readLine()) != null) {
                USERNAME = line;
            }
            if ((line = nf.readLine()) != null) {
                PASSWORD = line;
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        if (USERNAME.equals("USERNAME") || PASSWORD.equals("PASSWORD")) {
            System.out.println("CONFIG FILE HAS WRONG FORMAT");
            return;
        }

        try {
            try {
                Class.forName("org.postgresql.Driver");
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
            String url = "jdbc:postgresql://ate.ita.chalmers.se/";
            Properties props = new Properties();
            props.setProperty("user",USERNAME);
            props.setProperty("password",PASSWORD);

            final Connection conn = DriverManager.getConnection(url, props);



			/* This block creates the government entry and the necessary
			 * country and area for that.
			 */
            try {
                clearDatabase(conn); // Clears all old entries

                PreparedStatement statement = conn.prepareStatement("INSERT INTO Countries (name) VALUES (?)");
                statement.setString(1, "");
                statement.executeUpdate();
                statement = conn.prepareStatement("INSERT INTO Areas (country, name, population) VALUES (?, ?, cast(? as INT))");
                statement.setString(1, "");
                statement.setString(2, "");
                statement.setString(3, "1");
                statement.executeUpdate();
                statement = conn.prepareStatement("INSERT INTO Persons (country, personnummer, name, locationcountry, locationarea, budget) VALUES (?, ?, ?, ?, ?, cast(? as NUMERIC))");
                statement.setString(1, "");
                statement.setString(2, "");
                statement.setString(3, "Government");
                statement.setString(4, "");
                statement.setString(5, "");
                statement.setString(6, "0");
                statement.executeUpdate();
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }

            // Initialize the database from the worldfile
            try {
                BufferedReader br = new BufferedReader(new FileReader(worldfile));
                String line;
                while ((line = br.readLine()) != null) {
                    String[] cmd = line.split(" +");
                    if ("ROAD".equals(cmd[0]) && (cmd.length == 5)) {
                        insertRoad(conn, cmd[1], cmd[2], cmd[3], cmd[4]);
                    } else if ("TOWN".equals(cmd[0]) && (cmd.length == 4)) {
						/* Create an area and a town entry in the database */
                        insertTown(conn, cmd[1], cmd[2], cmd[3]);
                    } else if ("CITY".equals(cmd[0]) && (cmd.length == 4)) {
						/* Create an area and a city entry in the database */
                        insertCity(conn, cmd[1], cmd[2], cmd[3]);
                    }
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }

            ArrayList<Player> players = new ArrayList<Player>();

            while(true) {
                optionssetup();
                String mode = readLine("? > ");
                String[] cmd = mode.split(" +");
                cmd[0] = cmd[0].toLowerCase();
                if ("new player".startsWith(cmd[0]) && (cmd.length == 5)) {
                    Player nextplayer = new Player(cmd[1], cmd[2], cmd[3], cmd[4]);
                    if (createPlayer(conn, nextplayer) == 1) {
                        System.out.println("Player was created!");
                        players.add(nextplayer);
                    }
                } else if ("done".startsWith(cmd[0]) && (cmd.length == 1)) {
                    break;
                } else {
                    System.out.println("\nInvalid option.");
                }
            }

            System.out.println("\nGL HF!");
            int roundcounter = 1;
            int maxrounds = 5;
            while(roundcounter <= maxrounds) {
                System.out.println("\nWe are starting the " + roundcounter + ". round!!!");
				/* for each player from the playerlist */
                for (int i = 0; i < players.size(); ++i) {
                    System.out.println("\nIt's your turn " + players.get(i).playername + "!");
                    System.out.println("You are currently located in " + getCurrentArea(conn, players.get(i)) + " (" + getCurrentCountry(conn, players.get(i)) + ")");
                    while (true) {
                        options();
                        String mode = readLine("? > ");
                        String[] cmd = mode.split(" +");
                        cmd[0] = cmd[0].toLowerCase();
                        if ("next moves".startsWith(cmd[0]) && (cmd.length == 1 || cmd.length == 3)) {
							/* Show next moves from a location or current location. Turn continues. */
                            if (cmd.length == 1) {
                                String area = getCurrentArea(conn, players.get(i));
                                String country = getCurrentCountry(conn, players.get(i));
                                getNextMoves(conn, players.get(i));
                            } else {
                                getNextMoves(conn, players.get(i), cmd[1], cmd[2]);
                            }
                        } else if ("list properties".startsWith(cmd[0]) && (cmd.length == 1 || cmd.length == 3)) {
							/* List properties of a player. Can be a specified player
							   or the player himself. Turn continues. */
                            if (cmd.length == 1) {
                                listProperties(conn, players.get(i));
                            } else {
                                listProperties(conn, cmd[1], cmd[2]);
                            }
                        } else if ("scores".startsWith(cmd[0]) && cmd.length == 1) {
							/* Show scores for all players. Turn continues. */
                            showScores(conn);
                        } else if ("players".startsWith(cmd[0]) && cmd.length == 1) {
							/* Show scores for all players. Turn continues. */
                            System.out.println("\nPlayers:");
                            for (int k = 0; k < players.size(); ++k) {
                                System.out.println("\t" + players.get(k).playername + ": " + players.get(k).personnummer + " (" + players.get(k).country + ") ");
                            }
                        } else if ("refund".startsWith(cmd[0]) && (cmd.length == 3 || cmd.length == 5)) {
                            if (cmd.length == 5) {
								/* Sell road from arguments. If no road was sold the turn
								   continues. Otherwise the turn ends. */
                                if (sellRoad(conn, players.get(i), cmd[1], cmd[2], cmd[3], cmd[4]) == 1) {
                                    break;
                                } else {
                                    System.out.println("\nTry something else.");
                                }
                            } else {
								/* Sell hotel from arguments. If no hotel was sold the turn
								   continues. Otherwise the turn ends. */
                                if (sellHotel(conn, players.get(i), cmd[1], cmd[2]) == 1) {
                                    break;
                                } else {
                                    System.out.println("\nTry something else.");
                                }
                            }
                        } else if ("buy".startsWith(cmd[0]) && (cmd.length == 4 || cmd.length == 5)) {
                            if (cmd.length == 5) {
								/* Buy road from arguments. If no road was bought the turn
								   continues. Otherwise the turn ends. */
                                if (buyRoad(conn, players.get(i), cmd[1], cmd[2], cmd[3], cmd[4]) == 1) {
                                    break;
                                } else {
                                    System.out.println("\nTry something else.");
                                }
                            } else {
								/* Buy hotel from arguments. If no hotel was bought the turn
								   continues. Otherwise the turn ends. */
                                if (buyHotel(conn, players.get(i), cmd[1], cmd[2], cmd[3]) == 1) {
                                    break;
                                } else {
                                    System.out.println("\nTry something else.");
                                }
                            }
                        } else if ("move".startsWith(cmd[0]) && cmd.length == 3) {
							/* Change the location of the player to the area from the arguments.
							   If the move was legal the turn ends. Otherwise the turn continues. */
                            if (changeLocation(conn, players.get(i), cmd[1], cmd[2]) == 1) {
                                break;
                            } else {
                                System.out.println("\nTry something else.");
                            }
                        } else if ("quit".startsWith(cmd[0]) && cmd.length == 1) {
							/* End the move of the player without any action */
                            break;
                        } else {
                            System.out.println("\nYou chose an invalid option. Try again.");
                        }
                    }
                }
                setVisitingBonus(conn);
                ++roundcounter;
            }
            announceWinner(conn);
            System.out.println("\nGG!\n");

            conn.close();
        } catch (SQLException e) {
            System.err.println(e);
            System.exit(2);
        }
    }

    private String readLine(String s) throws IOException {
        System.out.print(s);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        char c;
        StringBuilder stringBuilder = new StringBuilder();
        do {
            c = (char) bufferedReader.read();
            stringBuilder.append(c);
        } while(String.valueOf(c).matches(".")); // Without the DOTALL switch, the dot in a java regex matches all characters except newlines

        System.out.println("");
        stringBuilder.deleteCharAt(stringBuilder.length()-1);

        return stringBuilder.toString();
    }

    /* main: parses the input commands.
     * /!\ You don't need to change this function! */
    public static void main(String[] args) throws Exception
    {
        String worldfile = args[0];
        Game g = new Game();
        g.play(worldfile);
    }
}
