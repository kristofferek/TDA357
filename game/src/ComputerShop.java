import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.io.*;

public class ComputerShop
{
    /* TODO Here you should put your database name, username and password */
    static final String USERNAME = "tda357_XXX";
    static final String PASSWORD = "secret";

    /* Print command usage. */
    public static void usage () {
	System.out.println("Usage: Select a letter to test the corresponding exercise:");
	System.out.println("    (a) Find the PC whose price is closest to the one desired");
	System.out.println("    (b) Find laptop that satisfies user requirements");
	System.out.println("    (c) Print spec of all products by manufacturer");
	System.out.println("    (d) Find the cheapest \"system\" (PC + printer)");
	System.out.println("    (e) Check there is no PC with specified model number");
	System.out.println("    (q) Quit");
    }

    /* main: parses the input commands. */
    public static void main(String[] args) throws Exception
    {
	try {
	    Class.forName("org.postgresql.Driver");
	    String url = "jdbc:postgresql://localhost/computer-shop";
	    Properties props = new Properties();
	    props.setProperty("user",USERNAME);
	    props.setProperty("password",PASSWORD);
	    Connection conn = DriverManager.getConnection(url, props);

	    Console console = System.console();
	    // In Eclipse. System.console() returns null due to a bug (https://bugs.eclipse.org/bugs/show_bug.cgi?id=122429)
	    // In that case, use the following line instead:
	    // BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
	    usage();
	    System.out.println("Welcome!");
	    while(true) {
		String exercise = console.readLine("? > ");
		if (exercise.equalsIgnoreCase("a")) {
		    int price = Integer.parseInt(console.readLine("Enter a price: "));
		    exerciseA(conn, price);
		}
		else if (exercise.equalsIgnoreCase("b")) {
		    float speed = Float.parseFloat(console.readLine("Enter minimum speed: "));
		    float ram = Float.parseFloat(console.readLine("Enter minimum RAM: "));
		    float hd = Float.parseFloat(console.readLine("Enter minimum hard-disk size: "));
		    float screen = Float.parseFloat(console.readLine("Enter minimum screen size: "));
		    exerciseB(conn, speed, ram, hd, screen);
		}
		else if (exercise.equalsIgnoreCase("c")) {
		    String manufacturer = console.readLine("Enter a manufacturer: ");
		    exerciseC(conn, manufacturer);
		}
		else if (exercise.equalsIgnoreCase("d")) {
		    int budget = Integer.parseInt(console.readLine("Enter the budget (PC price + printer price): "));
		    float speed = Float.parseFloat(console.readLine("Enter the minimum speed: "));
		    exerciseD(conn, budget, speed);
		}
		else if (exercise.equalsIgnoreCase("e")) {
		    String manufacturer = console.readLine("Enter the manufacturer: ");
		    int model = Integer.parseInt(console.readLine("Enter the model: "));
		    float speed = Float.parseFloat(console.readLine("Enter the speed: "));
		    float ram = Float.parseFloat(console.readLine("Enter the RAM: "));
		    float hd = Float.parseFloat(console.readLine("Enter the hard-disk size: "));
		    float price = Float.parseFloat(console.readLine("Enter the price: "));
		    exerciseE(conn,
			      manufacturer,
			      model,
			      speed,
			      ram,
			      hd,
			      price);
		}
		else if (exercise.equalsIgnoreCase("q")) {
			       break;
		} else
		      usage();
	    }
	    System.out.println("Goodbye!");
	    conn.close();
	} catch (SQLException e) {
	    System.err.println(e);
	    System.exit(2);
	}
    }

    private static void printTable(String[] attributeNames, ResultSet rs) throws SQLException {
	List<String> placeholders = Collections.nCopies(attributeNames.length, "%8s");
	String format = String.join(" | ", placeholders);
	String header = String.format(format, (Object[]) attributeNames);
	String border = String.join("-+-",
				    Collections.nCopies(attributeNames.length, "--------"));
	System.out.println(header);
	System.out.println(border);
	
	while (rs.next()) {
	    List<String> values = new ArrayList<String>();
	    for (int i = 1; i <= attributeNames.length; i++)
		values.add(rs.getString(i));
	    String row = String.format(format, values.toArray(new Object[values.size()]));
	    System.out.println(row);
	}
    }

    private static void exerciseA(Connection conn, int price) throws SQLException {
	String query = "WITH R1 AS (" +
	    "     SELECT model," +
	    "            speed," +
	    "            abs(price - ?) AS diff" +
	    "     FROM pcs" +
	    "     )," +
	    "     R2  AS (" +
	    "     SELECT MIN(diff) AS minimum" +
	    "     FROM R1" +
	    "     )," +
	    "     R3 AS (" +
	    "     SELECT model," +
	    "            speed" +
	    "     FROM R1" +
	    "     WHERE diff = (SELECT minimum from R2)" +
	    "     )" +
	    "SELECT maker," +
	    "       model," +
	    "       speed " +
	    " FROM R3" +
	    "      NATURAL JOIN products";
	PreparedStatement st = conn.prepareStatement(query);
	st.setInt(1, price);
	ResultSet rs = st.executeQuery();
	while (rs.next()) {
	    System.out.print("Maker: ");
	    System.out.println(rs.getString(1));
	    System.out.print("Model: ");
	    System.out.println(rs.getString(2));
	    System.out.print("Speed : ");
	    System.out.println(rs.getString(3));
	}
	rs.close();
	st.close();
    }

    private static void exerciseB(Connection conn,
				  float speed,
				  float ram,
				  float hd,
				  float screen) throws SQLException {
	String[] attrs = {"maker", "speed", "ram", "hd", "screen"};
	String query = "WITH R1 AS (" +
	    " SELECT * " +
	    "    FROM laptops " +
	    " WHERE speed >= ?" +
	    "    AND ram >= ?" +
	    "    AND hd >= ?" +
	    "    AND screen >= ?" +
	    " )," +
	    "     R2 AS (" +
	    " SELECT model," +
	    "        maker " +
	    "    FROM products" +
	    " ) " +
	    "SELECT * " +
	    "FROM R1 " +
	    "NATURAL JOIN R2 ";
	PreparedStatement st = conn.prepareStatement(query);
	st.setFloat(1, speed);
	st.setFloat(2, ram);
	st.setFloat(3, hd);
	st.setFloat(4, screen);
	ResultSet rs = st.executeQuery();

	if(rs.isBeforeFirst())
	    printTable(attrs, rs);
	else
	    System.out.println("No laptop found");

	rs.close();
	st.close();
    }

    private static void exerciseC(Connection conn, String manufacturer) throws SQLException {
	Map<String, String[]> tables = new HashMap<String, String[]>();
	tables.put("PCs", new String[] {"model", "speed", "ram", "hd", "price"});
	tables.put("Laptops", new String[] {"model", "speed", "ram", "hd", "screen", "price"});
	tables.put("Printers", new String[] {"model", "color", "type", "price"});

        final int NOTHING_FOUND = 3;
	int notFoundCount = 0;
	for (Map.Entry<String, String[]> entry : tables.entrySet()) {
		String query = "SELECT * " +
		    "FROM " + entry.getKey() + " " +
		    "NATURAL JOIN products " +
		    "WHERE products.maker = ?";
		PreparedStatement st = conn.prepareStatement(query);
		st.setString(1, manufacturer);

		ResultSet rs = st.executeQuery();

		if (rs.isBeforeFirst()) {
		    System.out.format("%20s", entry.getKey());
		    System.out.println();
		    printTable(entry.getValue(), rs);
		    System.out.println();
		} else
		    notFoundCount++;

		rs.close();
		st.close();
	}

	if (notFoundCount == NOTHING_FOUND)
	    System.out.println("No products found for manufacturer: " + manufacturer);
    }

    private static void exerciseD(Connection conn, int budget, float speed) throws SQLException {
	String query = "WITH R1 AS (" +
	    "SELECT pcs.model AS pc, " +
	    "       printers.model AS printer, " +
	    "       pcs.price + printers.price AS system_price, " +
	    "       printers.color AS printer_color " +
	    "FROM pcs, " +
	    "     printers " +
	    "WHERE pcs.price + printers.price <= ? " +
	    "      AND pcs.speed >= ? " +
	    ") " +
	    "SELECT pc, " +
	    "       printer " +
	    "FROM R1 " +
	    "ORDER BY system_price, " +
	    "         printer_color DESC LIMIT 1";
	PreparedStatement st = conn.prepareStatement(query);
	st.setInt(1, budget);
	st.setFloat(2, speed);
	ResultSet rs = st.executeQuery();
	if (rs.next()) {
	    System.out.println("PC model: " + rs.getString(1));
	    System.out.println("Printer model: " + rs.getString(2));
	} else
	    System.out.println("No system matches the requirements.");
	rs.close();
	st.close();
    }

    private static void exerciseE(Connection conn,
				  String maker,
				  int model,
				  float speed,
				  float ram,
				  float hd,
				  float price) throws SQLException {
	String query = "SELECT model FROM products WHERE model = ?";
	PreparedStatement st = conn.prepareStatement(query);
	st.setInt(1, model);
	ResultSet rs = st.executeQuery();
	if (rs.next()) {
	    System.out.println("A product with model no. " + model + " already exists.");
	} else {
	    String insertPc = "INSERT INTO pcs VALUES (?,?,?,?,?)";
	    String insertProduct = "INSERT INTO products VALUES (?,?,'pc')";
	    conn.setAutoCommit(false);

	    PreparedStatement stPc = conn.prepareStatement(insertPc);
	    stPc.setInt(1, model);
	    stPc.setFloat(2, speed);
	    stPc.setFloat(3, ram);
	    stPc.setFloat(4, hd);
	    stPc.setFloat(5, price);

	    PreparedStatement stProduct = conn.prepareStatement(insertProduct);
	    stProduct.setString(1, maker);
	    stProduct.setInt(2, model);

	    stPc.executeUpdate();
	    stProduct.executeUpdate();
	    conn.commit();
	    conn.setAutoCommit(true);
	}
	rs.close();
	st.close();
    }
}
