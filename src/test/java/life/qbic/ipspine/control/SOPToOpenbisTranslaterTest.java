package life.qbic.ipspine.control;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import life.qbic.datamodel.samples.ISampleBean;
import life.qbic.datamodel.samples.SampleType;
import life.qbic.datamodel.samples.TSVSampleBean;
import life.qbic.ipspine.model.JSONSOP;
import life.qbic.ipspine.model.PreparationExperimentStructure;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class SOPToOpenbisTranslaterTest {

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
  public void prepareExamples() throws FileNotFoundException {
    prepareEasyExample();
    prepareComplexExample();
  }

  private void prepareComplexExample() throws FileNotFoundException {
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
    sampleInfo.put("selected_entity_types", new HashSet<>(Arrays.asList("source h")));
    sampleInfo.put("description", "bla3");
    sampleInfo.put("selected_measured_type", "cell 2");
    sampleInfosComplex.add(sampleInfo);

    JSONSOP complexSOP = translator.parseJSON(new File(folderPath + "difficult.json"));
    complexResult = translator.getDesignForSOP(complexSOP, sampleInfosComplex);
  }

  private void prepareEasyExample() throws FileNotFoundException {
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
  public void testCorrectSourcesAreCreated() {
    List<String> expectedNames = new ArrayList<>(Arrays.asList("donor 1", "donor 2"));
    List<String> expectedSpecies = new ArrayList<>(Arrays.asList("human", "human"));
    for(ISampleBean s : easyResult.getSpeciesSamples()) {
      String name = s.getSecondaryName();
      expectedNames.remove(name);
      String species = (String) s.getMetadata().get("Q_NCBI_ORGANISM");
      expectedSpecies.remove(species);
    }
    assert(easyResult.getSpeciesSamples().size()==2);
    assert(expectedNames.isEmpty());
    assert(expectedSpecies.isEmpty());

    expectedNames = new ArrayList<>(Arrays.asList("donor 1", "donor 2", "donor 1"));
    expectedSpecies = new ArrayList<>(Arrays.asList("Sus scrofa domesticus", "Homo sapiens", "Homo sapiens"));
    for(ISampleBean s : complexResult.getSpeciesSamples()) {
      String name = s.getSecondaryName();
      expectedNames.remove(name);
      String species = (String) s.getMetadata().get("Q_NCBI_ORGANISM");
      expectedSpecies.remove(species);
    }
    assert(complexResult.getSpeciesSamples().size()==3);
    assert(expectedNames.isEmpty());
    assert(expectedSpecies.isEmpty());

  }

  @Test
  public void testSamplesAreCreated() {
    System.err.println(easyResult.getExtracts().size());
    assert(easyResult.getExtracts().size()==(2+2+2));
    assert(complexResult.getExtracts().size()==(5+2+2));
  }

  @Test
  public void samplesAreConnected() {
    Map<String, String> idsToNames = fetchSampleIDsToSampleNames(easyResult);
    for(TSVSampleBean t : easyResult.getSpeciesSamples()) {
      assert t.getParentIDs().isEmpty();
    }
    for(TSVSampleBean t : easyResult.getExtracts()) {
      List<String> parentNames = t.getParentIDs().stream().map(idsToNames::get).collect(Collectors.toList());

      String uniqueName = t.getSecondaryName()+ " "+t.getMetadata().get("Q_PRIMARY_TISSUE");
      switch (uniqueName) {
        case "sample 1 source tissue":
        case "sample 2 source tissue":
          assert parentNames.size()==1;
          assert parentNames.get(0).equals("donor 1");
          break;
        case "sample 3 source tissue":
          assert parentNames.size()==1;
          assert parentNames.get(0).equals("donor 2");
          break;
        case "sample 1 measured":
          assert parentNames.size()==1;
          assert parentNames.get(0).equals("sample 1");
          break;
        case "sample 2 measured":
          assert parentNames.size()==1;
          assert parentNames.get(0).equals("sample 2");
          break;
        case "sample 3 measured":
          assert parentNames.size()==1;
          assert parentNames.get(0).equals("sample 3");
          break;
        default:
          assert false;
      }
    }

    idsToNames = fetchSampleIDsToSampleNames(complexResult);
    for(TSVSampleBean t : complexResult.getSpeciesSamples()) {
      assert t.getParentIDs().isEmpty();
    }
    for(TSVSampleBean t : complexResult.getExtracts()) {
      List<String> parentNames = t.getParentIDs().stream().map(idsToNames::get).collect(Collectors.toList());

      String uniqueName = t.getSecondaryName()+ " "+t.getMetadata().get("Q_PRIMARY_TISSUE");

      switch (uniqueName) {
        case "sample 1 Co-culture":
          assert parentNames.size()==2;
          for(String name : parentNames) {
            assert name.equals("sample 1");
          }
          break;
        case "sample 1 cell 1":
          assert parentNames.size()==1;
          assert parentNames.contains("sample 1");
          break;
        case "sample 1 cell 2":
          assert parentNames.size()==1;
          assert parentNames.contains("sample 1");
          break;
        case "sample 1 source h":
          assert parentNames.size()==1;
          assert parentNames.contains("donor 1");
          break;
        case "sample 1 source a":
          assert parentNames.size()==1;
          assert parentNames.contains("donor 1");
          break;
        case "sample 2 cell 1":
          assert parentNames.size()==1;
          assert parentNames.contains("sample 2");
          break;
        case "sample 3 cell 2":
          assert parentNames.size()==1;
          assert parentNames.contains("sample 3");
          break;
        case "sample 2 source a":
          assert parentNames.size()==1;
          assert parentNames.contains("donor 1");
          break;
        case "sample 3 source h":
          assert parentNames.size()==1;
          assert parentNames.contains("donor 2");
          break;
        default:
          System.err.println(uniqueName+" should not exist");
          assert false;
      }
    }
  }

  @Test
  public void testSamplesContainCorrectMetadata() {
    for(TSVSampleBean s : easyResult.getSpeciesSamples()) {
      String species = (String) s.getMetadata().get("Q_NCBI_ORGANISM");
      SampleType type = s.getType();
      assert type.equals(SampleType.Q_BIOLOGICAL_ENTITY);
      switch (s.getSecondaryName()) {
        case "donor 1":
        case "donor 2":
          assert species.equals("human");
          break;
        default:
          assert false;
      }
    }

    for(TSVSampleBean s : easyResult.getExtracts()) {
      String uniqueName = s.getSecondaryName()+ " "+s.getMetadata().get("Q_PRIMARY_TISSUE");
      String info = (String) s.getMetadata().get("Q_ADDITIONAL_INFO");
      SampleType type = s.getType();
      assert type.equals(SampleType.Q_BIOLOGICAL_SAMPLE);
      switch (uniqueName) {
        case "sample 1 source tissue":
          assert info.equals("bla1");
          break;
        case "sample 2 source tissue":
          assert info.equals("bla2");
          break;
        case "sample 3 source tissue":
          assert info.equals("bla3");
          break;
        case "sample 1 measured":
          assert info.equals("bla1");
          break;
        case "sample 2 measured":
          assert info.equals("bla2");
          break;
        case "sample 3 measured":
          assert info.equals("bla3");
          break;
        default:
          assert false;
      }
    }

  }

  private Map<String, String> fetchSampleIDsToSampleNames(PreparationExperimentStructure experimentStructure) {
    Map<String, String> res = new HashMap<>();
    for(ISampleBean s : experimentStructure.getSpeciesSamples()) {
      res.put(s.getCode(), s.getSecondaryName());
    }
    for(ISampleBean s : experimentStructure.getExtracts()) {
      res.put(s.getCode(), s.getSecondaryName());
    }
    return res;
  }

}
