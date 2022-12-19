/*******************************************************************************
 * QBiC Project Wizard enables users to create hierarchical experiments including different study
 * conditions using factorial design. Copyright (C) "2016" Andreas Friedrich
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package life.qbic.ipspine.control;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import life.qbic.ipspine.model.SOP;

public class DBManager {
  private DBConfig config;

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
    String sql = "SELECT short_title from projects WHERE openbis_project_identifier = ?";
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

  public int isProjectInDB(String projectIdentifier) {
    logger.info("Looking for project " + projectIdentifier + " in the DB");
    String sql = "SELECT * from projects WHERE openbis_project_identifier = ?";
    int res = -1;
    Connection conn = login();
    try {
      PreparedStatement statement = conn.prepareStatement(sql);
      statement.setString(1, projectIdentifier);
      ResultSet rs = statement.executeQuery();
      if (rs.next()) {
        res = rs.getInt("id");
        logger.info("project found!");
      }
    } catch (SQLException e) {
      logger.error("SQL operation unsuccessful: " + e.getMessage());
      e.printStackTrace();
    }
    logout(conn);
    return res;
  }

  public int addProjectToDB(String projectIdentifier, String projectName) {
    int exists = isProjectInDB(projectIdentifier);
    if (exists < 0) {
      logger.info("Trying to add project " + projectIdentifier + " to the person DB");
      String sql = "INSERT INTO projects (openbis_project_identifier, short_title) VALUES(?, ?)";
      Connection conn = login();
      try (PreparedStatement statement =
          conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
        statement.setString(1, projectIdentifier);
        statement.setString(2, projectName);
        statement.execute();
        ResultSet rs = statement.getGeneratedKeys();
        if (rs.next()) {
          logout(conn);
          logger.info("Successful.");
          return rs.getInt(1);
        }
      } catch (SQLException e) {
        logger.error("SQL operation unsuccessful: " + e.getMessage());
        e.printStackTrace();
      }
      logout(conn);
      return -1;
    }
    return exists;
  }

  public boolean genericInsertIntoTable(String table, Map<String, Object> values) {
    List<String> keys = new ArrayList<String>(values.keySet());
    String key_string = String.join(", ", keys);
    String[] ar = new String[keys.size()];
    for (int i = 0; i < ar.length; i++) {
      ar[i] = "?";
    }
    String val_string = String.join(", ", ar);
    String sql = "INSERT INTO " + table + " (" + key_string + ") VALUES(" + val_string + ")";
    // return false;
    Connection conn = login();
    try (
        PreparedStatement statement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      int i = 0;
      for (String key : keys) {
        i++;
        Object val = values.get(key);
        if (val instanceof String)
          statement.setString(i, (String) val);
        if (val instanceof Integer)
          statement.setInt(i, (int) val);
      }
      boolean res = statement.execute();
      logout(conn);
      return res;
    } catch (SQLException e) {
      logger.error("SQL operation unsuccessful: " + e.getMessage());
    }
    logout(conn);
    return false;
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
    String sql = "SELECT * FROM design INNER JOIN experiments_design ON design_id = design.id WHERE openbis_experiment_identifier = ?";
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
    String sql = "INSERT INTO experiments_design (openbis_experiment_identifier, design_id, active) VALUES(?, ?, ?)";
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
    String sql = "UPDATE experiments_design SET active = ? WHERE openbis_experiment_identifier = ?";
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
    String sql = "SELECT id FROM design WHERE name = ?";
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
    return res;
  }

  public SOP findSOP(String sopName) {
    String sql = "SELECT * from sop WHERE name = ?";
    SOP res = null;
    Connection conn = login();
    try {
      PreparedStatement statement = conn.prepareStatement(sql);
      statement.setString(1, sopName);
      ResultSet rs = statement.executeQuery();
      if (rs.next()) {
        String description = rs.getString("description");
        String category = rs.getString("category");
        String dsCode = rs.getString("dataset_id");
        res = new SOP(sopName, dsCode, category, description);
      }
    } catch (SQLException e) {
      logger.error("SQL operation unsuccessful: " + e.getMessage());
      e.printStackTrace();
    } catch (NullPointerException n) {
      logger.error("Could not reach SQL database, resuming without SOP.");
    }
    logout(conn);
    return res;
  }

  public List<SOP> getSOPsOfType(String type) {
    String sql = "SELECT * from sop WHERE category = ?";
    List<SOP> res = new ArrayList<>();
    Connection conn = login();
    try {
      PreparedStatement statement = conn.prepareStatement(sql);
      statement.setString(1, type);
      ResultSet rs = statement.executeQuery();
      while (rs.next()) {
        String sopName = rs.getString("name");
        String description = rs.getString("description");
        String category = rs.getString("category");
        String dsCode = rs.getString("dataset_id");
        res.add(new SOP(sopName, dsCode, category, description));
      }
    } catch (SQLException e) {
      logger.error("SQL operation unsuccessful: " + e.getMessage());
      e.printStackTrace();
    } catch (NullPointerException n) {
      logger.error("Could not reach SQL database, resuming without SOPs.");
    }
    logout(conn);
    return res;
  }

  public void removeSamples(List<String> samplesToRemove) {
    if (samplesToRemove.size() > 0) {
      String sql1 = "DELETE from samples WHERE id IN (";
      String sql2 = "DELETE from samples_locations WHERE sample_id IN (";

      int max = samplesToRemove.size();

      for (int i = 1; i < max; i++) {
        sql1 += "?, ";
        sql2 += "?, ";
      }
      sql1 += "?)";
      sql2 += "?)";

      System.out.println(sql1);
//      
      Connection conn = login();
      try {
        PreparedStatement statement = conn.prepareStatement(sql1);

        for (int i = 0; i < max; i++) {
          statement.setString(i + 1, samplesToRemove.get(i));
        }

        System.out.println(statement.toString());
        statement.executeQuery();

        statement = conn.prepareStatement(sql2);

        for (int i = 0; i < max; i++) {
          statement.setString(i + 1, samplesToRemove.get(i));
        }
        statement.executeQuery();

      } catch (SQLException e) {
        logger.error("SQL operation unsuccessful: " + e.getMessage());
        e.printStackTrace();
      } catch (NullPointerException n) {
        logger.error("Could not reach SQL database.");
      }
      logout(conn);
    }
  }


}
