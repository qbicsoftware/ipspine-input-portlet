package life.qbic.ipspine.components;

import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import java.util.*;

import com.vaadin.data.Item;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import life.qbic.datamodel.samples.SampleType;
import life.qbic.ipspine.control.Result;
import life.qbic.ipspine.model.JSONSOP;
import life.qbic.portal.Styles;
import org.apache.commons.lang3.tuple.Pair;

public class ExperimentCreationComponent extends VerticalLayout {

  private final String DONOR_NAME_TITLE = "Donor Name";
  private final String SAMPLE_NAME_TITLE = "Sample Name";
  private final String SAMPLE_DESCRIPTION_TITLE = "Description";
  private final String SAMPLE_TYPE_NAME = "Sample Type";
  private final String DESIGN_FACTOR1_TITLE = "Medium";
  private final String DESIGN_FACTOR2_TITLE = "Treatment";

  private final String DESIGN_FACTOR3_TITLE = "Timepoint";
  private final ComboBox sampleSOPs;
  private final Label sopDescription;
  private TextField experimentDescription;
  private TextField publicationReference;

  private TextField dataProvider;
  private final VerticalLayout sopOptions;
  private final Table samples;
  private List<JSONSOP> JSONSops;
  private final Button addSampleRow;
  private final Button registerSamples;
  private final HorizontalLayout buttons;
  private final List<Object> tableItems;

  public ExperimentCreationComponent() {
    setSpacing(true);
    addComponent(new Label("Create a new experiment"));

    tableItems = new ArrayList<>();

    sampleSOPs = new ComboBox("Sample Preparation (SOPs)");
    sampleSOPs.setWidth("550");
    addComponent(sampleSOPs);
    sopDescription = new Label();
    addComponent(sopDescription);

    sopOptions = new VerticalLayout();
    addComponent(sopOptions);

    buttons = new HorizontalLayout();
    buttons.setSpacing(true);
    buttons.setVisible(false);
    addSampleRow = new Button();
    addSampleRow.setIcon(FontAwesome.PLUS_CIRCLE);

    Button removeSampleRow = new Button();
    removeSampleRow.setIcon(FontAwesome.MINUS_CIRCLE);

    removeSampleRow.addClickListener(new Button.ClickListener() {

      @Override
      public void buttonClick(ClickEvent event) {
        removeSampleRow();
      }
    });

    registerSamples = new Button("Register new samples");
    buttons.addComponent(addSampleRow);
    buttons.addComponent(removeSampleRow);
    buttons.addComponent(registerSamples);

    samples = new Table("New Samples");
    samples.setWidth("1000");

    samples.setStyleName(Styles.tableTheme);
    samples.addContainerProperty(DONOR_NAME_TITLE, TextField.class, null);
    samples.addContainerProperty(SAMPLE_NAME_TITLE, TextField.class, null);
    samples.addContainerProperty(SAMPLE_DESCRIPTION_TITLE, TextField.class, null);
    samples.addContainerProperty(SAMPLE_TYPE_NAME, ComboBox.class, null);
    samples.addContainerProperty(DESIGN_FACTOR1_TITLE, TextField.class, null);
    samples.addContainerProperty(DESIGN_FACTOR2_TITLE, TextField.class, null);
    samples.addContainerProperty(DESIGN_FACTOR3_TITLE, TextField.class, null);

    samples.setColumnWidth(DONOR_NAME_TITLE, 100);
    samples.setColumnWidth(SAMPLE_NAME_TITLE, 110);
    samples.setColumnWidth(SAMPLE_DESCRIPTION_TITLE, 150);
    samples.setColumnWidth(DESIGN_FACTOR1_TITLE, 100);
    samples.setColumnWidth(DESIGN_FACTOR2_TITLE, 110);
    samples.setColumnWidth(DESIGN_FACTOR3_TITLE, 110);

    addComponent(samples);
    addComponent(buttons);

    samples.setVisible(false);
    samples.setSelectable(true);

    sampleSOPs.addValueChangeListener(new ValueChangeListener() {

      @Override
      public void valueChange(ValueChangeEvent event) {
        sopDescription.setValue("");
        sopOptions.removeAllComponents();
        Object selected = sampleSOPs.getValue();
        tableItems.clear();
        samples.removeAllItems();
        samples.setVisible(false);
        if (selected != null) {
          JSONSOP sop = getJSONSOP();
          sopDescription.setValue(sop.getDescription());

          listSOPOptions(sop.getEntitiesNotMeasured());

          Set<String> measuredTypes = sop.getMeasuredEntities();

          initAddRowListener(measuredTypes);

          experimentDescription = new TextField("Additional Description");
          experimentDescription.setWidth("400");
          sopOptions.addComponent(experimentDescription);

          publicationReference = new TextField("Reference to Publication");
          publicationReference.setWidth("400");
          sopOptions.addComponent(publicationReference);

          dataProvider = new TextField("Name of Data Provider");
          dataProvider.setWidth("400");
          sopOptions.addComponent(dataProvider);

          if (samples.isEmpty()) {
            addSampleRow(measuredTypes);
          }
        }
        buttons.setVisible(selected != null);
      }

      private void initAddRowListener(Set<String> measuredTypes) {
        for (Object listener : addSampleRow.getListeners(ClickEvent.class)) {
          addSampleRow.removeClickListener((ClickListener) listener);
        }
        addSampleRow.addClickListener(new Button.ClickListener() {
          @Override
          public void buttonClick(ClickEvent event) {
            addSampleRow(measuredTypes);
          }
        });
      }
    });
  }

  private void listSOPOptions(List<Pair<SampleType, Set<String>>> availableEntities) {
    for(Pair<SampleType, Set<String>> sample : availableEntities) {
      ComboBox optionBox = new ComboBox();
      optionBox.setWidth("230");
      Set<String> values = sample.getRight();
      optionBox.setNullSelectionAllowed(false);
      optionBox.addItems(values);
      if(values.size() == 1) {
        optionBox.setValue(values.iterator().next());
        optionBox.setEnabled(false);
      } else {
        optionBox.setRequired(true);
        optionBox.setValidationVisible(true);
      }
      sopOptions.addComponent(optionBox);
    }
  }

  private Set<String> getSOPEntitySelections() {
    Set<String> res = new HashSet<>();
    for(int i = 0; i < sopOptions.getComponentCount(); i++) {
      Component c = sopOptions.getComponent(i);
      if (c instanceof ComboBox) {
        ComboBox box = (ComboBox) c;
        Object val = box.getValue();
        if(val==null) {
          res.add("");
        } else {
          res.add(val.toString());
        }
      }
    }
    return res;
  }

  private TextField newTextFieldUnsized(boolean required) {
    TextField res = new TextField();
    res.setRequired(required);
    res.setValidationVisible(required);
    res.setSizeFull();
    return res;
  }

  private TextField newTextFieldUnsized() {
    return newTextFieldUnsized(false);
  }

  protected void addSampleRow(Set<String> measuredTypes) {
    Object last = samples.addItem();
    tableItems.add(last);

    Item row = samples.getItem(last);
    row.getItemProperty(DONOR_NAME_TITLE).setValue(newTextFieldUnsized(true));
    row.getItemProperty(SAMPLE_NAME_TITLE).setValue(newTextFieldUnsized(true));
    row.getItemProperty(SAMPLE_DESCRIPTION_TITLE).setValue(newTextFieldUnsized());
    row.getItemProperty(DESIGN_FACTOR1_TITLE).setValue(newTextFieldUnsized());
    row.getItemProperty(DESIGN_FACTOR2_TITLE).setValue(newTextFieldUnsized());
    row.getItemProperty(DESIGN_FACTOR3_TITLE).setValue(newTextFieldUnsized());
    ComboBox measuredSelection = new ComboBox();
    measuredSelection.setNullSelectionAllowed(false);
    measuredSelection.addItems(measuredTypes);
    measuredSelection.setSizeFull();
    if(measuredTypes.size() == 1) {
      measuredSelection.setValue(measuredTypes.iterator().next());
      measuredSelection.setEnabled(false);
    } else {
      measuredSelection.setRequired(true);
      measuredSelection.setValidationVisible(true);
    }
    row.getItemProperty(SAMPLE_TYPE_NAME).setValue(measuredSelection);

    samples.setVisible(!samples.isEmpty());
    samples.setPageLength(samples.size() + 1);
  }

  protected void removeSampleRow() {
    if (!tableItems.isEmpty()) {
      Object last = tableItems.get(tableItems.size() - 1);
      samples.removeItem(last);
      tableItems.remove(last);
    }
    samples.setVisible(!samples.isEmpty());
    samples.setPageLength(samples.size() + 1);
  }

  public void setJSONSOPs(List<JSONSOP> sops) {
    this.JSONSops = sops;
    for (JSONSOP sop : sops) {
      sampleSOPs.addItem(sop.getName());
    }
  }

  private JSONSOP JSONSopNameToSOP(String name) {
    for (JSONSOP sop : JSONSops) {
      if (sop.getName().equals(name)) {
        return sop;
      }
    }
    return null;
  }
  public JSONSOP getJSONSOP() {
    String name = sampleSOPs.getValue().toString();
    return JSONSopNameToSOP(name);
  }

  private String parseTextField(String colname, Object id) {
    return ((TextField) samples.getItem(id).getItemProperty(colname).getValue()).getValue();
  }

  private String parseComboBox(String colname, Object id) {
    return (String) ((ComboBox) samples.getItem(id).getItemProperty(colname).getValue()).getValue();
  }
  /**
   * 
   * @return Map: sample name to description
   */
  public Map<String, String> getSampleInformation() {
    Map<String, String> res = new HashMap<>();
    for (Object id : samples.getItemIds()) {
      String name = parseTextField("Name", id);
      String description = parseTextField("Description", id);
      res.put(name, description);
    }
    return res;
  }

  public String getExperimentDescription() {
    return experimentDescription.getValue();
  }

  public String getPublicationReference() {
    return publicationReference.getValue();
  }

  public String getDataProvider() {
    return dataProvider.getValue();
  }

  public List<Map<String, Object>> getComplexSampleInformation() {
    List<Map<String, Object>> res = new ArrayList<>();
    Set<String> selectedEntityTypes = getSOPEntitySelections();
    for (Object id : samples.getItemIds()) {
      String name = parseTextField(SAMPLE_NAME_TITLE, id);
      String donor = parseTextField(DONOR_NAME_TITLE, id);
      String description = parseTextField(SAMPLE_DESCRIPTION_TITLE, id);
      String measuredSample = parseComboBox(SAMPLE_TYPE_NAME, id);
      String medium = parseTextField(DESIGN_FACTOR1_TITLE, id);
      String treatment = parseTextField(DESIGN_FACTOR2_TITLE, id);
      String timepoint = parseTextField(DESIGN_FACTOR3_TITLE, id);
      Map<String, Object> sampleProperties = new HashMap<>();
      sampleProperties.put("name", name);
      sampleProperties.put("donor", donor);
      sampleProperties.put("description", description);
      sampleProperties.put("selected_entity_types", selectedEntityTypes);
      sampleProperties.put("selected_measured_type", measuredSample);
      if(medium!=null) {
        sampleProperties.put("medium", medium);
      }
      if(treatment!=null) {
        sampleProperties.put("treatment", treatment);
      }
      if(timepoint!=null) {
        sampleProperties.put("timepoint", timepoint);
      }
      res.add(sampleProperties);
    }
    return res;
  }

  public Button getRegistrationButton() {
    return registerSamples;
  }

  public void resetView() {
    sampleSOPs.setValue(sampleSOPs.getNullSelectionItemId());
  }

  public Result<Void, String> validateInputs() {
    Set<String> selections = getSOPEntitySelections();
    if(selections.contains(null) || selections.contains("")) {
      return Result.fromError("Please select all missing tissue or cell types.");
    }

    Set<String> uniqueSampleNames = new HashSet<>();
    for (Object id : samples.getItemIds()) {
      TextField nameField = (TextField) samples.getItem(id).getItemProperty("Sample Name").getValue();
      TextField donorField = (TextField) samples.getItem(id).getItemProperty("Donor Name").getValue();
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
      return Result.fromValue(null);
  }

}
