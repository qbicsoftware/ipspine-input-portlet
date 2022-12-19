package life.qbic.ipspine.model.omics;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.ui.ComboBox;

public class SequencingComponent extends AOmicsComponent {

//  private ComboBox measurementDevice;
  private ComboBox analyteType;

  public SequencingComponent(Map<String, String> devices) {
    super(devices);
//    measurementDevice = new ComboBox("Sequencer Device", devices.keySet());
//    measurementDevice.setNullSelectionAllowed(false);
//    addComponent(measurementDevice);

    analyteType = new ComboBox("Sequencing Type", Arrays.asList("DNA", "RNA"));
    analyteType.setNullSelectionAllowed(false);
    analyteType.setValue("DNA");
    addComponent(analyteType);

    multiplexing.setVisible(true);
    // TextField test = new TextField("Test");
    // addComponent(test);
  }

  public Map<String, String> getExperimentMetadata() {
    Map<String, String> metadata = new HashMap<>();
    metadata.put("Q_SEQUENCER_DEVICE", devices.get(measurementDevice.getValue().toString()));
    metadata.put("EXPERIMENT_TYPE", "Q_NGS_MEASUREMENT");
    metadata.put("ANALYTE_TYPE", analyteType.getValue().toString());
    return metadata;
  }

  public boolean isValid() {
    return measurementDevice.getValue() != null && analyteType.getValue() != null;
  }

  @Override
  public String getInfoText() {
    return analyteType.getValue().toString() + "-Seq measurement";
  }
  
  @Override
  public void addListenerToSubcomponents(ValueChangeListener omicsInputChangeListener) {
    measurementDevice.addValueChangeListener(omicsInputChangeListener);
    analyteType.addValueChangeListener(omicsInputChangeListener);
  }
}
