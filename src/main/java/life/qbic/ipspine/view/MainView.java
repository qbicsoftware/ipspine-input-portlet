package life.qbic.ipspine.view;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.ui.CheckBox;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.stream.Collectors;
import life.qbic.ipspine.control.Result;
import life.qbic.ipspine.model.ExperimentMetadata;
import life.qbic.ipspine.model.JSONSOP;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.VerticalLayout;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import life.qbic.datamodel.projects.ProjectInfo;
import life.qbic.ipspine.components.ExperimentCreationComponent;
import life.qbic.ipspine.components.MeasurementCreationComponent;
import life.qbic.ipspine.model.MeasurementExperimentStructure;
import life.qbic.ipspine.model.omics.AOmicsComponent;

public class MainView extends VerticalLayout implements View {

  /**
   * 
   */
  private static final long serialVersionUID = -2015647177613573942L;

  private static final Logger logger = LogManager.getLogger(MainView.class);
  private final Map<String, String> taxonomyMap;
  private final Map<String, String> tissueMap;
  private final String designsFolder;

  private final TabSheet tabs;

  private final ComboBox availableProjects;
  private final CheckBox showATMPProjects;
  private final Map<String, String> projectCodeToDisplay;
  private Map<ProjectInfo, Boolean> projectsToATMPStatus;

  private final ExperimentCreationComponent experimentCreationComponent;

  private final MeasurementCreationComponent addMeasurementsComponent;

  public final static String navigateToLabel = "project";

  private ProgressBar bar;

  // private String resourceUrl;

  public MainView(Map<String, String> taxMap, Map<String, String> tissueMap, String designsFolder,
      Map<String, AOmicsComponent> typesToComponents) {
    this.taxonomyMap = taxMap;
    this.tissueMap = tissueMap;
    this.designsFolder = designsFolder;

    setMargin(true);
    setSpacing(true);

    // set base context
    availableProjects = new ComboBox("Sub-projects");
    availableProjects.setNullSelectionAllowed(false);
    addComponent(availableProjects);
    showATMPProjects = new CheckBox("Include ATMP Sub-projects");
    addComponent(showATMPProjects);

    showATMPProjects.addValueChangeListener(new ValueChangeListener() {
      @Override
      public void valueChange(ValueChangeEvent event) {
        filterAvailableProjects(projectsToATMPStatus, showATMPProjects.getValue());
      }
    });

    projectCodeToDisplay = new HashMap<>();

    tabs = new TabSheet();

    // component to add new experiments from known SOPs
    experimentCreationComponent = new ExperimentCreationComponent();
    tabs.addTab(experimentCreationComponent, "New Samples");

    Map<String,String> reverseTissueMap = tissueMap.entrySet().stream().collect(
        Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

    Map<String,String> reverseTaxMap = taxonomyMap.entrySet().stream().collect(
        Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
    addMeasurementsComponent =
        new MeasurementCreationComponent(reverseTaxMap, reverseTissueMap, typesToComponents);
    tabs.addTab(addMeasurementsComponent, "New Measurements");

    addComponent(tabs);
  }

  public void setAvailableProjects(Map<ProjectInfo, Boolean> projectsToATMPStatus) {
    this.projectsToATMPStatus = projectsToATMPStatus;

    filterAvailableProjects(projectsToATMPStatus, showATMPProjects.getValue());

    availableProjects.setWidth("400");
  }

  private void filterAvailableProjects(Map<ProjectInfo, Boolean> projectsToATMPStatus, boolean showATMPProjects) {
    availableProjects.removeAllItems();
    for (ProjectInfo info : projectsToATMPStatus.keySet()) {
      if ((!showATMPProjects && !projectsToATMPStatus.get(info)) || showATMPProjects) {
        String code = info.getProjectCode();
        String displayName = code;
        if (info.getSecondaryName() != null && info.getSecondaryName().length() > 0) {
          displayName = code + ": " + info.getSecondaryName();
        }
        projectCodeToDisplay.put(code, displayName);

        availableProjects.addItem(displayName);
      }
    }
  }

  public String getProjectCode() {
    if (availableProjects.getValue() != null) {
      return availableProjects.getValue().toString().substring(0, 5);
    }
    return null;
  }

  public ComboBox getProjectBox() {
    return availableProjects;
  }

  public void setExistingExperiments(Map<Experiment, JSONSOP> experimentsToSOP) {
    addMeasurementsComponent.setExistingExperiments(experimentsToSOP);
  }

  public void setJSONSamplePrepSOPs(List<JSONSOP> sops) {
    experimentCreationComponent.setJSONSOPs(sops);
  }

  public Button getDesignRegistrationButton() {
    return experimentCreationComponent.getRegistrationButton();
  }

  public TabSheet getTabsheet() {
    return tabs;
  }

  public Map<String, String> getSampleInformation() {
    return experimentCreationComponent.getSampleInformation();
  }

  public List<Map<String, Object>> getComplexSampleInformation() {
    return experimentCreationComponent.getComplexSampleInformation();
  }

  public ExperimentMetadata getExperimentLevelMetadata() {
    String publication = experimentCreationComponent.getPublicationReference();
    String dataProvider = experimentCreationComponent.getDataProvider();
    String description = experimentCreationComponent.getExperimentDescription();
    return new ExperimentMetadata(description, publication, dataProvider);
  }

  public JSONSOP getSelectedSOP() {
    return experimentCreationComponent.getJSONSOP();
  }

  public Button getMeasurementRegistrationButton() {
    return addMeasurementsComponent.getRegisterButton();
  }

  public Button getTSVDownloadButton() {
    return addMeasurementsComponent.getDownloadButton();
  }

  public MeasurementExperimentStructure getMeasurementExperimentStructure() {
    return addMeasurementsComponent.getMeasurementExperimentStructure();
  }

  public void setTSVDownload(String tsvContent) {
    addMeasurementsComponent.setTSVDownload(tsvContent);
  }

  @Override
  public void enter(ViewChangeEvent event) {
    String project = event.getParameters();
    boolean showAllSetting = showATMPProjects.getValue();
    if(!showAllSetting) {
      showATMPProjects.setValue(true);
    }
    if (projectCodeToDisplay.containsKey(project)) {
      logger.info("Navigating to project " + project + " provided via URL.");
      updateContent(project);
      return;
    } else if (project.isEmpty()) {
      logger.info("No project provided. Dropdown selection of projects possible.");
    } else {
      logger.warn("Unable to load project '" + project
          + "' provided via URL. Dropdown selection of projects possible.");
    }
    showATMPProjects.setValue(showAllSetting);
    availableProjects.setNullSelectionAllowed(true);
    availableProjects.setValue(null);
    availableProjects.setNullSelectionAllowed(false);
  }

  public void updateContent(String project) {
    availableProjects.setValue(projectCodeToDisplay.get(project));
  }

  public void startedRegistrationProcess(Button button) {
    button.setEnabled(false);
    bar = new ProgressBar();
    bar.setIndeterminate(true);
    addComponent(bar);
  }

  public void endProgressVisualization(boolean success, boolean withMeasurements) {
    if (withMeasurements) {
      getMeasurementRegistrationButton().setEnabled(true);
      if (success) {
        addMeasurementsComponent.resetView();
      }
    } else {
      getDesignRegistrationButton().setEnabled(true);
      if (success) {
        experimentCreationComponent.resetView();
      }
    }
    removeComponent(bar);
  }

  public Map<String, String> getTaxonomyMap() {
    return taxonomyMap;
  }
  public Map<String, String> getTissueMap() {
    return tissueMap;
  }
  public String getDesignsFolder() { return designsFolder; }

  public Result<Void, String> validateDesignInputs() {
    return experimentCreationComponent.validateInputs();
  }

  public Result<Void, String> validateMeasurementInputs() {
    return addMeasurementsComponent.validateInputs();
  }
}
