package life.qbic.ipspine.control;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import life.qbic.datamodel.identifiers.ExperimentCodeFunctions;
import life.qbic.ipspine.model.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.server.ExternalResource;
import com.vaadin.server.Resource;
import com.vaadin.ui.Button;
import com.vaadin.ui.TabSheet.SelectedTabChangeListener;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.Project;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.DataSetFile;
import ch.systemsx.cisd.openbis.generic.shared.dto.identifier.ProjectIdentifier;
import ch.systemsx.cisd.openbis.generic.shared.dto.identifier.SpaceIdentifier;
import life.qbic.datamodel.projects.ProjectInfo;
import life.qbic.ipspine.registration.OpenbisV3CreationController;
import life.qbic.ipspine.registration.OpenbisV3ReadController;
import life.qbic.ipspine.registration.RegisterableExperiment;
import life.qbic.ipspine.registration.RegisteredSamplesReadyRunnable;
import life.qbic.ipspine.registration.SampleCounter;
import life.qbic.ipspine.view.MainView;
import life.qbic.openbis.openbisclient.OpenBisClient;
import life.qbic.portal.Styles;
import life.qbic.portal.Styles.NotificationType;

public class MainController {

  private OpenbisV3ReadController v3API;
  private OpenBisClient openbis;
  private DBManager mainDB;
  private DBManager ipspineDB;
  private MainView view;
  private SOPToOpenbisTranslater sopToOpenbisTranslator;
  private List<JSONSOP> jsonDesigns;
  private final Set<String> BLACKLIST = new HashSet<>(Arrays.asList("QSOPS"));
  private OpenbisV3CreationController creationController;
  protected MeasurementExperimentStructure currentDesign;

  private static final Logger logger = LogManager.getLogger(MainController.class);


  private final List<String> SPACES = Arrays.asList("IPSPINE_TESTING",
      "IPSPINE_BIOMATERIAL_DEVELOPMENT", "IPSPINE_IPSC_DEVELOPMENT", "IPSPINE_OTHER");

  private Map<String, String> projectToSpace;


  public MainController(OpenBisClient openbis, OpenbisV3ReadController v3API,
      OpenbisV3CreationController openBIScreationController, DBManager mainDB, DBManager ipspineDB,
      MainView view) {
    this.openbis = openbis;
    this.v3API = v3API;
    this.creationController = openBIScreationController;
    this.mainDB = mainDB;
    this.ipspineDB = ipspineDB;
    this.view = view;
    initMetadata(view.getTaxonomyMap(), view.getTissueMap(), view.getDesignsFolder());
    initListeners();
    listProjects();
  }

  private void initMetadata(Map<String,String> taxonomy, Map<String, String> tissues, String designsFolder) {
    this.sopToOpenbisTranslator = new SOPToOpenbisTranslater(taxonomy, tissues, designsFolder);
    this.jsonDesigns = sopToOpenbisTranslator.getSOPs();
  }

  private void listProjects() {
    List<Project> projects;
    List<ProjectInfo> projectInfos = new ArrayList<>();
    projectToSpace = new HashMap<>();

    for (String space : SPACES) {
      projects = v3API.getProjectsOfSpace(space);
      for (Project p : projects) {
        if (!BLACKLIST.contains(p.getCode())) {
          String name = mainDB.getProjectName(p.getIdentifier().toString());
          projectInfos.add(new ProjectInfo(space, p.getCode(), p.getDescription(), name, -1));
          projectToSpace.put(p.getCode(), space);
        }
      }
    }
    view.setAvailableProjects(projectInfos);
    //view.setSamplePrepSOPs(getSOPsOfType("Sample Preparation"));
    view.setJSONSamplePrepSOPs(jsonDesigns);
  }

  private List<SOP> getSOPsOfType(String type) {
    return ipspineDB.getSOPsOfType(type);
  }

  private JSONSOP fetchDesignForExperiment(String SOPName) {
    String name = ipspineDB.getDesignNameOfExperiment(SOPName);
    for(JSONSOP design : jsonDesigns) {
      if(design.getName().equals(name)) {
        return design;
      }
    }
    return null;
  }

  private SOP getSOPFromName(String SOPName) {
    SOP sop = ipspineDB.findSOP(SOPName);
    if (sop != null) {
      sop.setResource(createResourceForDataset(sop.getDsCode()));
    }
    return sop;
  }

  private Resource createResourceForDataset(String datasetCode) {
    Resource resource = null;
    List<DataSetFile> files = openbis.getFilesOfDataSetWithID(datasetCode);
    String dssPath = "";

    try {
      files.get(0).getPath();
      for (DataSetFile file : files) {
        if (file.getPath().length() > dssPath.length()) {
          dssPath = file.getPath();
        }
      }
    } catch (Exception e) {
      logger.error("dataset for sop not found");
    }

    try {
      URL url = openbis.getDataStoreDownloadURLLessGeneric(datasetCode, dssPath);
      url = new URL(url.toString().replace(":444", ""));
      resource = new ExternalResource(url);

    } catch (MalformedURLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return resource;
  }

  /**
   * Removes all samples from the sample tracking database that are of one or several user-defined
   * types. Goes through every openBIS project that is found and removes samples project by project.
   * 
   * @param types the sample type codes belonging to samples that should be removed
   */
  public void removeSampleTypesFromTrackingDB(Set<String> types) {
    for (Project project : openbis.listProjects()) {
      List<String> samplesToRemove = new ArrayList<>();
      String projectID = project.getIdentifier().toString();
      List<Sample> samples = openbis.getSamplesOfProject(projectID);
      for (Sample sample : samples) {
        String type = sample.getType().getCode();
        if (types.contains(type)) {
          samplesToRemove.add(sample.getCode());
        }
      }
      System.out.println("trying to remove " + samplesToRemove.size());
      mainDB.removeSamples(samplesToRemove);
    }
  }

  private List<Sample> loadSamplesOfProject(String space, String project) {
    String projectID = new ProjectIdentifier(new SpaceIdentifier(space), project).toString();
    return openbis.getSamplesOfProject(projectID);
  }

  private List<Experiment> loadExperimentsOfProject(String space, String project) {
    String projectID = new ProjectIdentifier(new SpaceIdentifier(space), project).toString();
    return v3API.getExperimentsForProject(projectID);
  }

  public void loadProjectInformation() {
    logger.info("load project info");
    String project = view.getProjectCode();
    String space = projectToSpace.get(project);
    Map<Experiment, JSONSOP> experimentsToSOP = new HashMap<>();
    if (project != null) {
      List<Experiment> experiments = loadExperimentsOfProject(space, project);
      // List<Sample> samples = loadSamplesOfProject(space, project);
      // Map<String, Sample> samplesPerCode = new HashMap<>();
      // for (Sample s : samples) {
      // System.out.println(s);
      // System.out.println(s.getCode());
      // samplesPerCode.put(s.getCode(), s);
      // }
      for (Experiment e : experiments) {
        //TODO cleanup
        //String sopName = e.getProperties().get("Q_ADDITIONAL_INFO");
        //SOP sop = getSOPFromName(sopName);
        JSONSOP design = fetchDesignForExperiment(e.getIdentifier().getIdentifier());
        if (design != null) {
          experimentsToSOP.put(e, design);
        }
      }
      // view.setSamplesPerCode(samplesPerCode);
    }
    view.setExistingExperiments(experimentsToSOP);

  }

  private void initListeners() {

    MainController controller = this;

    view.getTabsheet().addSelectedTabChangeListener((SelectedTabChangeListener) event -> loadProjectInformation());

    view.getProjectBox().addValueChangeListener((ValueChangeListener) event -> loadProjectInformation());

    view.getDesignRegistrationButton().addClickListener((Button.ClickListener) event -> {
      List<Map<String, Object>> sampleInfo = view.getComplexSampleInformation();
      JSONSOP sop = view.getSelectedSOP();

      PreparationExperimentStructure design = sopToOpenbisTranslator.getDesignForSOP(sop, sampleInfo);

      String project = view.getProjectCode();

      List<RegisterableExperiment> experiments = prepareRegistration(design, project, view.getExperimentDescription());

      view.startedRegistrationProcess(view.getDesignRegistrationButton());
      creationController.registerExperimentsWithSamples(projectToSpace.get(project), project,
          experiments, new RegisteredSamplesReadyRunnable(controller, experiments, sop.getName(), false));
    });

    view.getMeasurementRegistrationButton().addClickListener((Button.ClickListener) event -> {

      currentDesign = view.getMeasurementExperimentStructure();
      String project = view.getProjectCode();

      List<RegisterableExperiment> experiments = prepareRegistration(currentDesign, project,
          "");//TODO

      view.startedRegistrationProcess(view.getMeasurementRegistrationButton());
      creationController.registerExperimentsWithSamples(projectToSpace.get(project), project,
          experiments, new RegisteredSamplesReadyRunnable(controller, experiments, null,
              true));
    });
  }

  private List<RegisterableExperiment> prepareRegistration(IExperimentStructure experimentCreator,
      String project, String experimentDescription) {
    String space = projectToSpace.get(project);

    List<Sample> oldSamples = loadSamplesOfProject(space, project);
    List<Experiment> oldExperiments = loadExperimentsOfProject(space, project);
    SampleCounter counter = new SampleCounter(oldSamples, oldExperiments, project);
    return experimentCreator.createExperimentsForRegistration(counter, space, project, experimentDescription);
  }

  private void endProgressVisualization(boolean withMeasurements) {
    view.endProgressVisualization(true, withMeasurements);
  }

  public void sampleRegistrationFailed(String error, boolean withMeasurements) {
    view.endProgressVisualization(false, withMeasurements);
    Styles.notification("Failed to register sample information.", error, NotificationType.ERROR);
  }

  public void sampleRegistrationDone(boolean withMeasurements, String designName,
      List<String> experimentCodes) {
    if (withMeasurements) {
      Button download = view.getTSVDownloadButton();
      view.setTSVDownload(currentDesign.getTSVContent());
      download.setEnabled(true);
    } else {
      for(String code : experimentCodes) {
        String project = view.getProjectCode();
        String id = ExperimentCodeFunctions.getExperimentIdentifier(projectToSpace.get(project), project, code);
      ipspineDB.addExperiment(id, designName);
      }
    }
    endProgressVisualization(withMeasurements);
    Styles.notification("Registration complete.",
        "All experiment information has been successfully added to the system.",
        NotificationType.SUCCESS);

  }
}
