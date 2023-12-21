package life.qbic.ipspine.control;

public class DBConfig {

  private final String hostname;
  private final String port;
  private final String sql_database;
  private final String username;
  private final String password;

  public DBConfig(String hostname, String port, String sql_database, String username,
      String password) {
    this.hostname = hostname;
    this.port = port;
    this.sql_database = sql_database;
    this.username = username;
    this.password = password;
  }

  public String getHostname() {
    return hostname;
  }

  public String getPort() {
    return port;
  }

  public String getSql_database() {
    return sql_database;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }



}
