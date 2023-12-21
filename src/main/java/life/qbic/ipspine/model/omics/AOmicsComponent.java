package life.qbic.ipspine.model.omics;

import java.util.Map;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.VerticalLayout;

public abstract class AOmicsComponent extends VerticalLayout implements IOmicsComponent {

  protected Map<String, String> devices;
  protected ComboBox measurementDevice;
  protected CheckBox multiplexing;

  public AOmicsComponent(Map<String, String> devices) {
    this();
    this.devices = devices;
    measurementDevice = new ComboBox("Measurement Device", devices.keySet());
    measurementDevice.setNullSelectionAllowed(false);
    measurementDevice.setWidth("300");
    addComponent(measurementDevice);
  }

  public AOmicsComponent() {
    setSpacing(true);
    setMargin(false);
    multiplexing = new CheckBox("Pool samples (Multiplexing)");
    multiplexing.setVisible(false);
    addComponent(multiplexing);
  }

  public boolean hasMultiplexing() {
    return multiplexing.getValue();
  }

  public CheckBox getMultiplexingCheckBox() {
    return multiplexing;
  }

}
