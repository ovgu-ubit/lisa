package retrieve;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Properties;

import javax.naming.InitialContext;

public class DatabaseConnection {

	private Connection connection;

	public DatabaseConnection() throws ClassNotFoundException, SQLException {
		this(false, false);
	}

	public DatabaseConnection(boolean test) throws ClassNotFoundException, SQLException {
		this(test, false);
	}
	
	public DatabaseConnection(Connection c) {
		this.connection = c;
	}

	/**
	 * 
	 * @param test if the test database should be used
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	@SuppressWarnings("unchecked")
	public DatabaseConnection(boolean test, boolean local) throws ClassNotFoundException, SQLException {
		if (local && test)
			throw new IllegalArgumentException();
		try {
			String con, driver;
			InitialContext initialContext = new InitialContext();
			if (initialContext != null) {
				Properties props = new Properties();
				if (!local) {
					Map<String, String> database_lbs = (Map<String, String>) initialContext
							.lookup("java:/comp/env/database/lbs");
					con = database_lbs.get("lbsdb_connection_string" + (test ? "_test" : ""));
					driver = database_lbs.get("lbsdb_driver");
				} else {
					Map<String, String> database_local = (Map<String, String>) initialContext
							.lookup("java:/comp/env/database/local");
					con = database_local.get("localdb_connection_string");
					driver = database_local.get("localdb_driver");
					/*
					 * int timeout = 5 * 60 * 1000; props.setProperty("tcpKeepAlive", "true");
					 * props.setProperty("idle_session_timeout", ""+timeout);
					 * props.setProperty("options", "-c statement_timeout=5min");
					 */
				}
				this.dbConnection(driver, con, props);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (this.connection == null)
			throw new SQLException("connection null");
	}

	public DatabaseConnection(String driverName, String connectionName) throws ClassNotFoundException, SQLException {
		this.dbConnection(driverName, connectionName);
	}

	@Override
	public void finalize() throws SQLException {
		connection.close();
	}

	public boolean isValid() {
		// not supported by jdts driver
		// return this.connection.isValid(timeout);
		ResultSet rs = null;
		try {
			// rs = this.sqlQuery("SELECT TOP 1 * from borrower");
			rs = this.sqlQuery("sp_who");
		} catch (SQLException e) {
		}
		return rs != null;
	}

	/**
	 * loads drivers and establishes connection
	 * 
	 * @param driverName     fully qualified class name of driver
	 * @param connectionName URL of DB connection
	 * @throws ClassNotFoundException if the driver could not be identified
	 * @throws SQLException           if the DB connection fails
	 */
	void dbConnection(String driverName, String connectionName) throws ClassNotFoundException, SQLException {
		Class.forName(driverName);
		connection = DriverManager.getConnection(connectionName);
		if (connection == null)
			throw new SQLException("connection null");
	}

	void dbConnection(String driverName, String connectionName, Properties props)
			throws ClassNotFoundException, SQLException {
		Class.forName(driverName);
		connection = DriverManager.getConnection(connectionName, props);
		if (connection == null)
			throw new SQLException("connection null");
	}

	/**
	 * executes a select query on the DB and retrieves result set
	 * 
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
	 * 
	 * @param query the SQL update statement
	 * @return if the update has been successful (true) or not (false)
	 * @throws SQLException
	 */
	public boolean updateStatement(String query) throws SQLException {
		PreparedStatement statement = connection.prepareStatement(query);
		statement.executeUpdate();
		return true;
	}

	public PreparedStatement initStatement(String query) throws SQLException {
		return connection.prepareStatement(query);
	}

}
