package life.qbic.ipspine.components;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import life.qbic.ipspine.control.Result;
import life.qbic.ipspine.model.JSONSOP;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.vaadin.data.Item;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.server.FileDownloader;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Table;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import life.qbic.ipspine.control.Functions;
import life.qbic.ipspine.control.MeasurementExperimentCreator;
import life.qbic.ipspine.model.MeasurementExperimentStructure;
import life.qbic.ipspine.model.omics.AOmicsComponent;
import life.qbic.portal.Styles;

public class MeasurementCreationComponent extends VerticalLayout {

  Logger logger = LogManager.getLogger(MeasurementCreationComponent.class);

  private ExperimentsComponent experimentsComponent;
  private ComboBox omicsSelection;
  private AOmicsComponent currentOmicsComponent;

  private Button registerSamples;
  private Button downloadBarcodes;

  private Table measuredSamples;
  private List<Object> tableItems;
  private VerticalLayout omicsComponents;
  private Map<String, AOmicsComponent> typesToComponents;

  public MeasurementCreationComponent(Map<String, String> taxMap, Map<String, String> tissueMap,
      Map<String, AOmicsComponent> typesToComponents) {
    
    setSpacing(true);

    tableItems = new ArrayList<>();

    this.typesToComponents = typesToComponents;
    experimentsComponent = new ExperimentsComponent(taxMap, tissueMap);
    addComponent(experimentsComponent);
    ValueChangeListener parentSelectionListener = new ValueChangeListener() {

      @Override
      public void valueChange(ValueChangeEvent event) {
        List<Sample> samples = experimentsComponent.getSelectedSamplesSortedByCode();
        showOmicsSelection(samples.size() > 0);
        reactToSelectionChange();
      }
    };
    experimentsComponent.getSampleTable().addValueChangeListener(parentSelectionListener);
    omicsSelection = new ComboBox("Measurement Type", typesToComponents.keySet());
    omicsSelection.setVisible(false);
    omicsSelection.setNullSelectionAllowed(false);
    addComponent(omicsSelection);

    omicsComponents = new VerticalLayout();
    omicsComponents.setVisible(false);
    addComponent(omicsComponents);

    ValueChangeListener omicsInputChangeListener = new ValueChangeListener() {

      @Override
      public void valueChange(ValueChangeEvent event) {
        reactToSelectionChange();
      }
    };
    omicsSelection.addValueChangeListener(omicsInputChangeListener);

    for (AOmicsComponent component : typesToComponents.values()) {
      component.addListenerToSubcomponents(omicsInputChangeListener);
      component.getMultiplexingCheckBox().addValueChangeListener(parentSelectionListener);
    }

    measuredSamples = new Table("Measurements");

    measuredSamples.setStyleName(Styles.tableTheme);
    measuredSamples.addContainerProperty("Name", TextField.class, null);
    measuredSamples.addContainerProperty("Description", TextField.class, null);
    measuredSamples.addContainerProperty("Info", String.class, "");
    measuredSamples.addValueChangeListener(new ValueChangeListener() {

      @Override
      public void valueChange(ValueChangeEvent event) {
        reactToMetadataInput();
      }
    });

    addComponent(measuredSamples);

    measuredSamples.setVisible(false);
    measuredSamples.setSelectable(true);

    registerSamples = new Button("Register Measurements");
    registerSamples.setVisible(false);
    downloadBarcodes = new Button("Download Identifiers");
    downloadBarcodes.setVisible(false);
    addComponent(registerSamples);
    addComponent(downloadBarcodes);
  }

  protected void reactToMetadataInput() {
    registerSamples.setVisible(sampleInfoValid() && currentOmicsComponent.isValid());
  }

  private void reactToSelectionChange() {
    boolean omicsSelected = omicsSelection.getValue() != null;
    omicsComponents.removeAllComponents();
    if (omicsSelected) {
      currentOmicsComponent = typesToComponents.get(omicsSelection.getValue());
      omicsComponents.addComponent(currentOmicsComponent);

      initMeasuredSamplesTable(currentOmicsComponent.hasMultiplexing());
    }
    omicsComponents.setVisible(omicsSelected);
    measuredSamples.setVisible(omicsSelected && currentOmicsComponent.isValid());
  }

  private void initMeasuredSamplesTable(boolean pooling) {
    measuredSamples.removeAllItems();
    if (currentOmicsComponent.isValid()) {
      List<Sample> samples = experimentsComponent.getSelectedSamplesSortedByCode();

      if (pooling) {
        addPooledRow(samples);
      } else {
        int i = 0;
        while (measuredSamples.size() < samples.size()) {
          addSampleRow(samples.get(i));
          i++;
        }
      }
    }
  }

  private void addPooledRow(List<Sample> samples) {
    Object newItem = measuredSamples.addItem();
    tableItems.add(newItem);

    Item row = measuredSamples.getItem(newItem);

    TextField t1 = new TextField();
    TextField t2 = new TextField();
    t1.setSizeFull();
    t2.setSizeFull();

    ValueChangeListener tfListener = new ValueChangeListener() {

      @Override
      public void valueChange(ValueChangeEvent event) {
        reactToMetadataInput();
      }
    };

    t1.addValueChangeListener(tfListener);
    t2.addValueChangeListener(tfListener);

    row.getItemProperty("Name").setValue(t1);
    row.getItemProperty("Description").setValue(t2);

    // String parentType = experimentsComponent.getType(sample);

    String info = currentOmicsComponent.getInfoText() + " of pooled samples";

    row.getItemProperty("Info").setValue(info);

    measuredSamples.setPageLength(measuredSamples.size() + 1);
  }

  protected void addSampleRow(Sample sample) {
    Object last = measuredSamples.addItem();
    tableItems.add(last);

    Item row = measuredSamples.getItem(last);

    TextField t1 = new TextField();
    TextField t2 = new TextField();

    ValueChangeListener tfListener = new ValueChangeListener() {

      @Override
      public void valueChange(ValueChangeEvent event) {
        reactToMetadataInput();
      }
    };

    t1.addValueChangeListener(tfListener);
    t2.addValueChangeListener(tfListener);

    row.getItemProperty("Name").setValue(t1);
    row.getItemProperty("Description").setValue(t2);

    String parentType = experimentsComponent.getType(sample);

    String info = currentOmicsComponent.getInfoText() + " of " + parentType;

    row.getItemProperty("Info").setValue(info);

    measuredSamples.setPageLength(measuredSamples.size() + 1);
  }

  protected void showOmicsSelection(boolean enable) {
    omicsSelection.setVisible(enable);
  }

  public void setExistingExperiments(Map<Experiment, JSONSOP> experimentsToSOP) {
    experimentsComponent.setExistingExperiments(experimentsToSOP);
  }

  public Button getRegisterButton() {
    return registerSamples;
  }

  public Button getDownloadButton() {
    return downloadBarcodes;
  }

  public MeasurementExperimentStructure getMeasurementExperimentStructure() {
    if (currentOmicsComponent.isValid()) {

      MeasurementExperimentCreator creator = new MeasurementExperimentCreator();
      return creator.createStructure(currentOmicsComponent.getExperimentMetadata(),
          getBasicSampleMetadata());
    }
    logger.error("metadata incomplete, could not create experiment structure");
    // TODO Auto-generated method stub
    return null;
  }


  private String parseTextField(String colname, Object id) {
    return ((TextField) measuredSamples.getItem(id).getItemProperty(colname).getValue()).getValue();
  }

  public boolean sampleInfoValid() {
    if (!measuredSamples.isVisible()) {
      return false;
    }
    for (Object id : measuredSamples.getItemIds()) {
      String name = parseTextField("Name", id);
      String description = parseTextField("Description", id);
      if (name.isEmpty() || description.isEmpty()) {
        return false;
      }
    }
    return true;
  }

  /**
   * 
   * @return Map: sample name to description
   */
  public List<Map<String, Object>> getBasicSampleMetadata() {
    List<Map<String, Object>> res = new ArrayList<>();
    List<Sample> parentSamples = experimentsComponent.getSelectedSamplesSortedByCode();
    int i = -1;
    for (Object id : measuredSamples.getItemIds()) {
      Map<String, Object> map = new HashMap<>();
      i++;
      String name = parseTextField("Name", id);
      String description = parseTextField("Description", id);
      String info = (String) measuredSamples.getItem(id).getItemProperty("Info").getValue();
      map.put("Name", name);
      map.put("Description", description);
      List<String> parentIDs = new ArrayList<>();
      if (currentOmicsComponent.hasMultiplexing()) {
        for (Sample parent : parentSamples) {
          parentIDs.add(parent.getCode());
        }
      } else {
        parentIDs.add(parentSamples.get(i).getCode());
      }
      map.put("Parents", parentIDs);
      map.put("Info", info);
      res.add(map);
    }
    return res;
  }

  public void setTSVDownload(String tsvContent) {
    String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());

    if (downloadBarcodes != null)
      removeComponent(downloadBarcodes);
    downloadBarcodes = new Button("Download Spreadsheet");
    addComponent(downloadBarcodes);
    FileDownloader tsvDL =
        new FileDownloader(Functions.getFileStream(tsvContent, timeStamp+"_samples", "tsv"));
    tsvDL.extend(downloadBarcodes);
  }

  public void resetView() {
    experimentsComponent.getSampleTable().setValue(experimentsComponent.getSampleTable().getNullSelectionItemId());
    omicsSelection.setValue(omicsSelection.getNullSelectionItemId());
  }

  public Result<Void, String> validateInputs() {
    //TODO
    /*
    Set<String> uniqueSampleNames = new HashSet<>();
    for (Object id : samples.getItemIds()) {
      TextField nameField = (TextField) samples.getItem(id).getItemProperty("Name").getValue();
      TextField donorField = (TextField) samples.getItem(id).getItemProperty("Donor").getValue();
      if (!nameField.isValid()) {
        return Result.fromError("Missing sample name.");
      }
      if (!donorField.isValid()) {
        return Result.fromError("Missing donor name.");
      }
      String name = nameField.getValue();
      String donor = donorField.getValue();
      String uniqueName = donor + name;
      if (uniqueSampleNames.contains(uniqueName)) {
        return Result.fromError(
            "Sample name " + name + " from donor " + donor + " must be unique.");
      } else {
        uniqueSampleNames.add(uniqueName);
        ComboBox sampleTypeBox = (ComboBox) samples.getItem(id).getItemProperty("Sample Type")
            .getValue();
        if (!sampleTypeBox.isValid()) {
          return Result.fromError(
              "Please select a Sample Type from the dropdown for each sample you wish to add.");
        }
      }
    }
     */
    return Result.fromValue(null);
  }
}
