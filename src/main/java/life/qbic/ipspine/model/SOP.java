package life.qbic.ipspine.model;

import com.vaadin.server.Resource;
@Deprecated
public class SOP {

  private String name;
  private String dsCode;
  private Resource resource;
  private String category;
  private String description;

  public SOP(String name, String dsCode, String category, String description) {
    super();
    this.name = name;
    this.dsCode = dsCode;
    this.category = category;
    this.description = description;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDsCode() {
    return dsCode;
  }

  public void setDsCode(String dsCode) {
    this.dsCode = dsCode;
  }

  public Resource getResource() {
    return resource;
  }

  public void setResource(Resource resource) {
    this.resource = resource;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @Override
  public String toString() {
    return name + " (" + category + "): " + description + " Dataset: " + dsCode;
  }

}
