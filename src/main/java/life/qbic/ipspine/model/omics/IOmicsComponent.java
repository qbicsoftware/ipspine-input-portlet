package life.qbic.ipspine.model.omics;

import java.util.Map;
import com.vaadin.data.Property.ValueChangeListener;

public interface IOmicsComponent {

  public Map<String, String> getExperimentMetadata();

  public boolean isValid();

  public String getInfoText();

  public void addListenerToSubcomponents(ValueChangeListener omicsInputChangeListener);

}
