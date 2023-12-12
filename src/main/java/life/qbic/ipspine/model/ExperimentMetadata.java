package life.qbic.ipspine.model;

import java.util.HashMap;
import java.util.Map;

public class ExperimentMetadata {

  private final String description;
  private final String publication;
  private final String dataProvider;
  public ExperimentMetadata(String description, String publication, String dataProvider) {
    this.description = description;
    this.publication = publication;
    this.dataProvider = dataProvider;
  }

  public String getDescription() {
    return description;
  }

  public String getPublication() {
    return publication;
  }

  public String getDataProvider() {
    return dataProvider;
  }

  Map<String,Object> getExtractionExperimentMetadata() {
    Map<String,Object> metadata = new HashMap<>();
    if(!description.isEmpty()) {
      metadata.put("Q_SECONDARY_NAME", description);
    }
    if(!dataProvider.isEmpty()) {
      metadata.put("Q_CONDUCTED_BY_NAME", dataProvider);
    }
    if(!publication.isEmpty()) {
      metadata.put("Q_ADDITIONAL_INFO", "Publication: "+publication);
    }
    return metadata;
  }

}
