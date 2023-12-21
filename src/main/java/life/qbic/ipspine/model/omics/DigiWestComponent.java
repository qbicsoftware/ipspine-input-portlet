package life.qbic.ipspine.model.omics;

import java.util.HashMap;
import java.util.Map;
import com.vaadin.data.Property.ValueChangeListener;

public class DigiWestComponent extends AOmicsComponent {

  public DigiWestComponent() {
    super();
    multiplexing.setVisible(true);
  }

  public Map<String, String> getExperimentMetadata() {
    Map<String, String> metadata = new HashMap<>();
    // metadata.put("Q_MS_DEVICE", devices.get(measurementDevice.getValue().toString()));
    metadata.put("EXPERIMENT_TYPE", "Q_DIGIWEST_MEASUREMENT");
    return metadata;
  }

  public boolean isValid() {
    return true;
  }

  @Override
  public String getInfoText() {
    return "DigiWest measurement";
  }

  @Override
  public void addListenerToSubcomponents(ValueChangeListener omicsInputChangeListener) {}

}
