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

import javax.xml.bind.JAXBException;
import life.qbic.datamodel.identifiers.ExperimentCodeFunctions;
import life.qbic.ipspine.model.*;
import life.qbic.xml.study.TechnologyType;
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

  private final OpenbisV3ReadController v3API;
  private final OpenBisClient openbis;
  private final DBManager mainDB;
  private final MainView view;
  private SOPToOpenbisTranslater sopToOpenbisTranslator;
  private List<JSONSOP> jsonDesigns;
  private final Set<String> BLACKLIST = new HashSet<>(Arrays.asList("QSOPS"));
  private final OpenbisV3CreationController creationController;
  final String SETUP_PROPERTY_CODE = "Q_EXPERIMENTAL_SETUP";
  protected MeasurementExperimentStructure currentMeasurementExperimentStructure;
  private Experiment currentDesignExperiment;

  private static final Logger logger = LogManager.getLogger(MainController.class);


  private final List<String> SPACES = Arrays.asList("IPSPINE_TESTING", "IPSPINE",
      "IPSPINE_BIOMATERIAL_DEVELOPMENT", "IPSPINE_IPSC_DEVELOPMENT");

  private final List<String> ATMP_SPACES = Arrays.asList("IPSPINE_BIOMATERIAL_DEVELOPMENT",
      "IPSPINE_IPSC_DEVELOPMENT");
  private Map<String, String> projectToSpace;

  public MainController(OpenBisClient openbis, OpenbisV3ReadController v3API,
      OpenbisV3CreationController openBIScreationController, DBManager mainDB, MainView view) {
    this.openbis = openbis;
    this.v3API = v3API;
    this.creationController = openBIScreationController;
    this.mainDB = mainDB;
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
    Map<ProjectInfo,Boolean> projectInfosToATMP = new HashMap<>();
    projectToSpace = new HashMap<>();

    for (String space : SPACES) {
      projects = v3API.getProjectsOfSpace(space);
      for (Project p : projects) {
        if (!BLACKLIST.contains(p.getCode())) {
          String name = mainDB.getProjectName(p.getIdentifier().toString());
          ProjectInfo info = new ProjectInfo(space, p.getCode(), p.getDescription(), name, -1);
          projectInfosToATMP.put(info, ATMP_SPACES.contains(space));
          projectToSpace.put(p.getCode(), space);
        }
      }
    }
    view.setAvailableProjects(projectInfosToATMP);
    //view.setSamplePrepSOPs(getSOPsOfType("Sample Preparation"));
    view.setJSONSamplePrepSOPs(jsonDesigns);
  }

  private JSONSOP fetchDesignForExperiment(String SOPName) {
    String name = mainDB.getDesignNameOfExperiment(SOPName);
    for(JSONSOP design : jsonDesigns) {
      if(design.getName().equals(name)) {
        return design;
      }
    }
    return null;
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
    currentDesignExperiment = null;
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
        if(ExperimentCodeFunctions.getInfoExperimentID(space, project).equals(e.getIdentifier().getIdentifier())) {
          currentDesignExperiment = e;
        }
        //TODO cleanup
        //String sopName = e.getProperties().get("Q_ADDITIONAL_INFO");
        //SOP sop = getSOPFromName(sopName);
        JSONSOP design = fetchDesignForExperiment(e.getIdentifier().getIdentifier());
        if (design != null) {
          experimentsToSOP.put(e, design);
        }
      }
      if(currentDesignExperiment==null) {
        logger.error("No experimental design experiment found for project "+project);
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
      Result<Void, String> validationResult = view.validateDesignInputs();
      if(validationResult.isError()) {
        String errorMessage = validationResult.getError();
        displayError(errorMessage);
      } else {
        List<Map<String, Object>> sampleInfo = view.getComplexSampleInformation();
        JSONSOP sop = view.getSelectedSOP();

        PreparationExperimentStructure design = sopToOpenbisTranslator.getDesignForSOP(sop,
            sampleInfo);

        String project = view.getProjectCode();

        List<RegisterableExperiment> experiments = prepareRegistration(design, project,
            view.getExperimentLevelMetadata());

        prepDesignXML(new ArrayList<>(), design.getExperimentalDesignProperties());

        view.startedRegistrationProcess(view.getDesignRegistrationButton());
        creationController.registerExperimentsWithSamples(projectToSpace.get(project), project,
            experiments,
            new RegisteredSamplesReadyRunnable(controller, experiments, sop.getName(), false));
      }
    });

    view.getMeasurementRegistrationButton().addClickListener((Button.ClickListener) event -> {
      Result<Void, String> validationResult = view.validateMeasurementInputs();
      if(validationResult.isError()) {
        String errorMessage = validationResult.getError();
        displayError(errorMessage);
      } else {
      currentMeasurementExperimentStructure = view.getMeasurementExperimentStructure();
      String project = view.getProjectCode();

      List<RegisterableExperiment> experiments = prepareRegistration(currentMeasurementExperimentStructure, project, null);//TODO

      view.startedRegistrationProcess(view.getMeasurementRegistrationButton());
      creationController.registerExperimentsWithSamples(projectToSpace.get(project), project,
          experiments, new RegisteredSamplesReadyRunnable(controller, experiments, null,
              true));
      }
    });
  }

  private List<RegisterableExperiment> prepareRegistration(IExperimentStructure experimentCreator,
      String project, ExperimentMetadata experimentLevelMetadata) {
    String space = projectToSpace.get(project);

    List<Sample> oldSamples = loadSamplesOfProject(space, project);
    List<Experiment> oldExperiments = loadExperimentsOfProject(space, project);
    SampleCounter counter = new SampleCounter(oldSamples, oldExperiments, project);
    return experimentCreator.createExperimentsForRegistration(counter, space, project, experimentLevelMetadata);
  }

  private void prepDesignXML(List<TechnologyType> techTypes, ExperimentalDesignPropertyWrapper newDesignProperties) {
    String experimentalDesignXML = null;
    Set<String> designs = new HashSet<String>();
    if (currentDesignExperiment != null) {
      Map<String, String> currentProps = currentDesignExperiment.getProperties();
      String oldXML = currentProps.get(SETUP_PROPERTY_CODE);
      experimentalDesignXML = ParserHelpers.mergeExperimentalDesignXMLs(oldXML,
          newDesignProperties, techTypes, designs);
      if (!experimentalDesignXML.equals(oldXML)) {
        logger.info("update of experimental design needed");
        currentProps.put(SETUP_PROPERTY_CODE, experimentalDesignXML);
        currentDesignExperiment.setProperties(currentProps);
      } else {
        logger.info("no update of existing experimental design needed");
      }
    } else {

      //TODO this should never be necessary
      try {
        logger.info("creating new experimental design");
        experimentalDesignXML =
            ParserHelpers.createDesignXML(newDesignProperties, techTypes, designs);
      } catch (JAXBException e) {
        logger.error("JAXB Error while creating experimental design XML");
        e.printStackTrace();
      }
    }
  }

  private void endProgressVisualization(boolean withMeasurements) {
    view.endProgressVisualization(true, withMeasurements);
  }

  public void sampleRegistrationFailed(String error, boolean withMeasurements) {
    view.endProgressVisualization(false, withMeasurements);
    displayError(error);
  }

  private void displayError(String error) {
    Styles.notification("Failed to register sample information.", error, NotificationType.ERROR);
  }

  public void sampleRegistrationDone(boolean withMeasurements, String designName,
      List<String> experimentCodes) {
    if (withMeasurements) {
      Button download = view.getTSVDownloadButton();
      view.setTSVDownload(currentMeasurementExperimentStructure.getTSVContent());
      download.setEnabled(true);
    } else {
      for(String code : experimentCodes) {
        String project = view.getProjectCode();
        String id = ExperimentCodeFunctions.getExperimentIdentifier(projectToSpace.get(project), project, code);
      mainDB.addExperiment(id, designName);
      updateExperimentalDesign();
      }
    }
    endProgressVisualization(withMeasurements);
    Styles.notification("Registration complete.",
        "All experiment information has been successfully added to the system.",
        NotificationType.SUCCESS);

  }

  private void updateExperimentalDesign() {
    v3API.updateExperiment(currentDesignExperiment);
  }
}
