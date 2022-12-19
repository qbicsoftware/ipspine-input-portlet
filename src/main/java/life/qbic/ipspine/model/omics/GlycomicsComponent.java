package life.qbic.ipspine.model.omics;

import java.util.HashMap;
import java.util.Map;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.ui.ComboBox;

public class GlycomicsComponent extends AOmicsComponent {

  public GlycomicsComponent(Map<String, String> devices) {
    super(devices);
    multiplexing.setVisible(true);
    // TextField test = new TextField("Test");
    // addComponent(test);
  }

  public Map<String, String> getExperimentMetadata() {
    Map<String, String> metadata = new HashMap<>();
    metadata.put("Q_MS_DEVICE", devices.get(measurementDevice.getValue().toString()));
    metadata.put("EXPERIMENT_TYPE", "Q_MS_MEASUREMENT");
    return metadata;
  }

  public boolean isValid() {
    return measurementDevice.getValue() != null;
  }

  @Override
  public String getInfoText() {
    return "Glycomics measurement";
  }

  @Override
  public void addListenerToSubcomponents(ValueChangeListener omicsInputChangeListener) {
    measurementDevice.addValueChangeListener(omicsInputChangeListener);
  }
}
