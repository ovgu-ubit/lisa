package retrieve;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LBSConnectionPool {
	private List<DatabaseConnection> connectionPool;
	private List<DatabaseConnection> usedConnections = new ArrayList<DatabaseConnection>();
	private Map<DatabaseConnection, Thread> idle_threads;
	private int MAX_POOL_SIZE = 10;
	private int MAX_IDLE_SECONDS = 5 * 60;
	private boolean test;

	private boolean debug = false;

	public LBSConnectionPool(boolean test) throws SQLException, ClassNotFoundException {
		this.test = test;
		connectionPool = new ArrayList<DatabaseConnection>();
		idle_threads = new HashMap<DatabaseConnection, Thread>();
	}

	public DatabaseConnection getConnection() throws ClassNotFoundException, SQLException, TooManyConnectionsException {
		if (debug)
			System.out.println(new Date() + ": Connection requested, current pool size: " + this.connectionPool.size());
		// too much connection handling
		if (connectionPool.isEmpty()) {
			if (usedConnections.size() < MAX_POOL_SIZE) {
				if (debug)
					System.out.println(new Date() + ": Connection is tried to be established");
				connectionPool.add(createConnection());
			} else {
				throw new TooManyConnectionsException();
			}
		}
		DatabaseConnection connection = connectionPool.remove(connectionPool.size() - 1);
		if (idle_threads.get(connection) != null) {
			idle_threads.get(connection).interrupt();
			idle_threads.remove(connection);
		}
		// possible reconnect
		if (connection == null || !connection.isValid()) {
			if (debug)
				System.out.println(
						new Date() + ": Connection " + connection.hashCode() + " is not valid, try to reconnect");
			connection = createConnection();
		}
		usedConnections.add(connection);
		return connection;
	}

	public boolean releaseConnection(DatabaseConnection connection) {
		if (debug)
			System.out.println(new Date() + ": Connection is tried to be released to pool with size "
					+ this.connectionPool.size());
		if (connection == null || !connection.isValid()) {
			if (debug)
				System.out.println(new Date() + ": Connection is not valid, is discarded");
			return usedConnections.remove(connection);
		}
		connectionPool.add(connection);
		Thread t1 = new Thread(new Runnable() {
			public void run() {
				try {
					Thread.sleep(MAX_IDLE_SECONDS * 1000);
					if (debug)
						System.out.println(new Date() + ": Idle connection " + connection.hashCode() + " is finalized");
					connection.finalize();
					connectionPool.remove(connection);
					idle_threads.remove(connection);
				} catch (InterruptedException e) {
					if (debug)
						System.out.println(new Date() + ": Idle thread of connection " + connection.hashCode()
								+ " is interrupted");
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		});
		idle_threads.put(connection, t1);
		t1.start();
		return usedConnections.remove(connection);
	}

	private DatabaseConnection createConnection() throws SQLException, ClassNotFoundException {
		DatabaseConnection conn = new DatabaseConnection(test);
		if (debug)
			System.out.println(new Date() + ": new connection " + conn.hashCode() + " established");
		return conn;
	}

	public int getSize() {
		return connectionPool.size() + usedConnections.size();
	}

	public class TooManyConnectionsException extends RuntimeException {

		private static final long serialVersionUID = 3266237014989604427L;

		public TooManyConnectionsException() {
			super();
		}
	}
}
