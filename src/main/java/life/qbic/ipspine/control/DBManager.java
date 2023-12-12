package life.qbic.ipspine.control;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DBManager {
  private DBConfig config;

  final String PROJECT_TABLE = "projects";
  final String DESIGN_TABLE = "ipspine_designs";
  final String EXPERIMENTS_DESIGN_TABLE = "ipspine_experiments_design";

  Logger logger = LogManager.getLogger(DBManager.class);

  public DBManager(DBConfig config) {
    this.config = config;
  }

  private void logout(Connection conn) {
    try {
      if (conn != null)
        conn.close();
    } catch (SQLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  private Connection login() {
    String DB_URL = "jdbc:mariadb://" + config.getHostname() + ":" + config.getPort() + "/"
        + config.getSql_database();

    Connection conn = null;

    try {
      Class.forName("org.mariadb.jdbc.Driver");
      conn = DriverManager.getConnection(DB_URL, config.getUsername(), config.getPassword());
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return conn;
  }

  public String getProjectName(String projectIdentifier) {
    String sql = "SELECT short_title from "+PROJECT_TABLE+" WHERE openbis_project_identifier = ?";
    String res = "";
    Connection conn = login();
    try {
      PreparedStatement statement = conn.prepareStatement(sql);
      statement.setString(1, projectIdentifier);
      ResultSet rs = statement.executeQuery();
      if (rs.next()) {
        res = rs.getString(1);
      }
    } catch (SQLException e) {
      logger.error("SQL operation unsuccessful: " + e.getMessage());
      e.printStackTrace();
    } catch (NullPointerException n) {
      logger.error("Could not reach SQL database, resuming without project names.");
    }
    logout(conn);
    return res;
  }

  private void endQuery(Connection c, PreparedStatement p) {
    if (p != null)
      try {
        p.close();
      } catch (Exception e) {
        logger.error("PreparedStatement close problem");
      }
    if (c != null)
      try {
        logout(c);
      } catch (Exception e) {
        logger.error("Database Connection close problem");
      }
  }

  public String getDesignNameOfExperiment(String experimentID) {
    String sql = "SELECT * FROM "+DESIGN_TABLE+" INNER JOIN "+EXPERIMENTS_DESIGN_TABLE+" "
        + "ON design_id = "+DESIGN_TABLE+".id WHERE openbis_experiment_identifier = ?";
    String res = "";
    Connection conn = login();
    try {
      PreparedStatement statement = conn.prepareStatement(sql);
      statement.setString(1, experimentID);
      ResultSet rs = statement.executeQuery();
      if (rs.next()) {
        String name = rs.getString("name");
        String shortname = rs.getString("shortname");
        if(shortname.isEmpty()) {
          res = name;
        } else {
          res = shortname;
        }
      }
    } catch (SQLException e) {
      logger.error("SQL operation unsuccessful: " + e.getMessage());
      e.printStackTrace();
    } catch (NullPointerException n) {
      logger.error("Could not reach SQL database, resuming without SOP.");
    } finally {
      logout(conn);
    }
    return res;
  }

  public boolean addExperiment(String experimentID, String designName) {
    int designID = getDesignID(designName);
    setExperimentsInactive(experimentID);
    logger.info("Trying to add experiment " + experimentID + " to the DB");
    String sql = "INSERT INTO "+EXPERIMENTS_DESIGN_TABLE+" (openbis_experiment_identifier, design_id, active) VALUES(?, ?, ?)";
    Connection conn = login();
    try (PreparedStatement statement =
        conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      statement.setString(1, experimentID);
      statement.setInt(2, designID);
      statement.setInt(3, 1);
      statement.execute();
      ResultSet rs = statement.getGeneratedKeys();
      if (rs.next()) {
        logout(conn);
        logger.info("Successful.");
        return true;
      }
    } catch (SQLException e) {
      logger.error("SQL operation unsuccessful: " + e.getMessage());
      e.printStackTrace();
    } finally {
      logout(conn);
    }
    return false;
  }

  private void setExperimentsInactive(String experimentID) {
    logger.info("Trying to set old experiments with id " + experimentID + " inactive in the DB");
    String sql = "UPDATE "+EXPERIMENTS_DESIGN_TABLE+" SET active = ? WHERE openbis_experiment_identifier = ?";
    Connection conn = login();
    try {
      PreparedStatement statement = conn.prepareStatement(sql);
      statement.setInt(1, 0);
      statement.setString(2, experimentID);
      statement.execute();
    } catch (SQLException e) {
      logger.error("SQL operation unsuccessful: " + e.getMessage());
      e.printStackTrace();
    } finally {
      logout(conn);
    }
  }

  private int getDesignID(String designName) {
    String sql = "SELECT id FROM "+DESIGN_TABLE+" WHERE name = ?";
    int res = -1;
    Connection conn = login();
    try {
      PreparedStatement statement = conn.prepareStatement(sql);
      statement.setString(1, designName);
      ResultSet rs = statement.executeQuery();
      if (rs.next()) {
        int id = rs.getInt("id");
        res = id;
      }
    } catch (SQLException e) {
      logger.error("SQL operation unsuccessful: " + e.getMessage());
      e.printStackTrace();
    } catch (NullPointerException n) {
      logger.error("Could not reach SQL database.");
    } finally {
      logout(conn);
    }
    if(res < 0) {
      logger.error("Could not find design with name "+designName+" in the database.");
    }
    return res;
  }

}
