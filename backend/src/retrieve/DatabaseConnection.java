package retrieve;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {
	
	private Connection connection;
	private static final String driverSybase = "net.sourceforge.jtds.jdbc.Driver";
	
	//Test DB
	private static final String connectionXMDB = "jdbc:jtds:sybase://test-db.gbv.de:2025/xmdb;user=db-user;password=db-password;";
	//Prod DB
	private static final String connectionLHMDB = "jdbc:jtds:sybase://prod-db.gbv.de:2025/lbsdb;user=db-user;password=db-password;";
	
	public DatabaseConnection() throws ClassNotFoundException, SQLException {
		this(driverSybase, connectionLHMDB);
	}
	
	/**
	 * 
	 * @param test if the test database should be used
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	public DatabaseConnection(boolean test) throws ClassNotFoundException, SQLException {
		if (test) this.dbConnection(driverSybase,connectionXMDB);
		else this.dbConnection(driverSybase,connectionLHMDB);
	}
	
	DatabaseConnection(String driverName, String connectionName) throws ClassNotFoundException, SQLException {
		this.dbConnection(driverName, connectionName);
	}
	
	@Override
	public void finalize() throws SQLException {
		this.dbDisconnect();
	}
	
	
	/**
	 * loads drivers and establishes connection
	 * @param driverName fully qualified class name of driver
	 * @param connectionName URL of DB connection
	 * @throws ClassNotFoundException if the driver could not be identified
	 * @throws SQLException  if the DB connection fails
	 */
	void dbConnection(String driverName, String connectionName) throws ClassNotFoundException, SQLException {
		Class.forName(driverName);
		connection = DriverManager.getConnection(connectionName);
	}
	
	/**
	 * closes connection to DB, should be called after queries have been sent
	 * @throws SQLException 
	 */
	void dbDisconnect() throws SQLException {
		connection.close();
	}
	
	/**
	 * executes a select query on the DB and retrieves result set
	 * @param query SQL query
	 * @return SQL ResultSet of query
	 * @throws SQLException 
	 */
	public ResultSet sqlQuery(String query) throws SQLException {
		ResultSet rs = null;

		Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
		rs = statement.executeQuery(query);

		return rs;
	}

	/**
	 * executes an update query
	 * @param query the SQL update statement
	 * @return if the update has been successful (true) or not (false)
	 * @throws SQLException 
	 */
	public boolean updateStatement(String query) throws SQLException {
		PreparedStatement statement = connection.prepareStatement(query);
		statement.executeUpdate();
		return true;
	}

}
