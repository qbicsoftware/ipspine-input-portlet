package life.qbic.ipspine.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import life.qbic.datamodel.experiments.ExperimentType;
import life.qbic.ipspine.control.SOPToOpenbisTranslater;
import life.qbic.ipspine.control.SOPToOpenbisTranslaterTest;
import life.qbic.ipspine.registration.RegisterableExperiment;
import life.qbic.ipspine.registration.SampleCounter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class PreparationExperimentStructureTest {

  private static final Logger log = LogManager.getLogger(SOPToOpenbisTranslaterTest.class);
  static Map<String, String> mockTax = new HashMap<>();
  static Map<String, String> mockTis = new HashMap<>();
  static String folderPath = "src/test/resources/designs/";

  PreparationExperimentStructure complexResult;
  PreparationExperimentStructure easyResult;

  static SOPToOpenbisTranslater translator;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    mockTax.put("Sus scrofa domesticus", "Sus scrofa domesticus");
    mockTax.put("Homo sapiens", "Homo sapiens");
    mockTax.put("human", "human");

    mockTis.put("source a", "source a");
    mockTis.put("source h", "source h");
    mockTis.put("source b", "source b");
    mockTis.put("Co-culture", "Co-culture");
    mockTis.put("cell 1", "cell 1");
    mockTis.put("cell 2", "cell 2");
    mockTis.put("source tissue", "source tissue");
    mockTis.put("measured", "measured");

    translator = new SOPToOpenbisTranslater(mockTax, mockTis, folderPath);
  }

  @Before
  public void prepareComplexExample() throws FileNotFoundException {
    ArrayList<Map<String, Object>> sampleInfosComplex = new ArrayList<>();
    Map<String, Object> sampleInfo = new HashMap<>();
    sampleInfo.put("name", "sample 1");
    sampleInfo.put("donor", "donor 1");
    sampleInfo.put("selected_entity_types", new HashSet<>(Arrays.asList("source a")));
    sampleInfo.put("description", "bla1");
    sampleInfo.put("selected_measured_type", "Co-culture");
    sampleInfosComplex.add(sampleInfo);

    sampleInfo = new HashMap<>();
    sampleInfo.put("name", "sample 2");
    sampleInfo.put("donor", "donor 1");
    sampleInfo.put("selected_entity_types", new HashSet<>(Arrays.asList("source a")));
    sampleInfo.put("description", "bla2");
    sampleInfo.put("selected_measured_type", "cell 1");
    sampleInfosComplex.add(sampleInfo);

    sampleInfo = new HashMap<>();
    sampleInfo.put("name", "sample 3");
    sampleInfo.put("donor", "donor 2");
    sampleInfo.put("selected_entity_types", new HashSet<>(Arrays.asList("source a")));
    sampleInfo.put("description", "bla3");
    sampleInfo.put("selected_measured_type", "cell 2");
    sampleInfosComplex.add(sampleInfo);

    JSONSOP complexSOP = translator.parseJSON(new File(folderPath + "difficult.json"));
    complexResult = translator.getDesignForSOP(complexSOP, sampleInfosComplex);
  }

  @Before
  public void prepareEasyExample() throws FileNotFoundException {
    ArrayList<Map<String, Object>> sampleInfosEasy = new ArrayList<>();
    Map<String, Object> sampleInfo = new HashMap<>();
    sampleInfo.put("name", "sample 1");
    sampleInfo.put("donor", "donor 1");
    sampleInfo.put("selected_entity_types", new HashSet<>(Arrays.asList("source tissue")));
    sampleInfo.put("description", "bla1");
    sampleInfo.put("selected_measured_type", "measured");
    sampleInfosEasy.add(sampleInfo);

    sampleInfo = new HashMap<>();
    sampleInfo.put("name", "sample 2");
    sampleInfo.put("donor", "donor 1");
    sampleInfo.put("selected_entity_types", new HashSet<>(Arrays.asList("source tissue")));
    sampleInfo.put("description", "bla2");
    sampleInfo.put("selected_measured_type", "measured");
    sampleInfosEasy.add(sampleInfo);

    sampleInfo = new HashMap<>();
    sampleInfo.put("name", "sample 3");
    sampleInfo.put("donor", "donor 2");
    sampleInfo.put("selected_entity_types", new HashSet<>(Arrays.asList("source tissue")));
    sampleInfo.put("description", "bla3");
    sampleInfo.put("selected_measured_type", "measured");
    sampleInfosEasy.add(sampleInfo);

    JSONSOP easySOP = translator.parseJSON(new File(folderPath + "easy.json"));
    easyResult = translator.getDesignForSOP(easySOP, sampleInfosEasy);
  }

  @Test
  public void testExperimentMetadata() {
    String project = "QABCD";
    String publication = "a publication";
    String description = "the experiment description";
    String dataPerson = "John Deer";
    ExperimentMetadata expData = new ExperimentMetadata(description, publication, dataPerson);
    List<RegisterableExperiment> experiments = easyResult.createExperimentsForRegistration(new SampleCounter(project),"TESTSPACE", project,  expData);
    assert experiments.size() == 2;
    for(RegisterableExperiment e : experiments) {
      if(e.getType().equals(ExperimentType.Q_SAMPLE_EXTRACTION.toString())) {
        Map<String, Object> metadata = e.getProperties();
        assert metadata.get("Q_ADDITIONAL_INFO").equals("Publication: "+publication);
        assert description.equals(metadata.get("Q_SECONDARY_NAME"));
        String sopDescription;
        assert dataPerson.equals(metadata.get("Q_CONDUCTED_BY_NAME"));
      }
    }

    experiments = complexResult.createExperimentsForRegistration(new SampleCounter(project),"TESTSPACE", project,  expData);
    assert experiments.size() == 2;
    for(RegisterableExperiment e : experiments) {
      if(e.getType().equals(ExperimentType.Q_SAMPLE_EXTRACTION.toString())) {
        Map<String, Object> metadata = e.getProperties();
        assert metadata.get("Q_ADDITIONAL_INFO").equals("Publication: "+publication);
        assert description.equals(metadata.get("Q_SECONDARY_NAME"));
        String sopDescription;
        assert dataPerson.equals(metadata.get("Q_CONDUCTED_BY_NAME"));
      }
    }
  }

}
