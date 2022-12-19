package life.qbic.ipspine.model;

import java.util.List;

public class SamplePrepSOP {

  private String name;
  private String description;
  private List<String> species;
  private List<String> tissue;
  private String analyte;
  private String url;

  public SamplePrepSOP(String name, String description, List<String> species, List<String> tissues,
      String analyte, String url) {
    super();
    this.name = name;
    this.description = description;
    this.species = species;
    this.tissue = tissues;
    this.analyte = analyte;
    this.url = url;
  }

  public String getName() {
    return name;
  }
  
  public String getSOPUrl() {
    return url;
  }

  public String getDescription() {
    return description;
  }

  public List<String> getSpecies() {
    return species;
  }

  public List<String> getTissues() {
    return tissue;
  }

  public String getAnalyte() {
    return analyte;
  }



}
