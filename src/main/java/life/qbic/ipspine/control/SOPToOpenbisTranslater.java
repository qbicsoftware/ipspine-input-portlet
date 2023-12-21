package life.qbic.ipspine.control;

import life.qbic.datamodel.experiments.ExperimentType;
import life.qbic.datamodel.experiments.OpenbisExperiment;
import life.qbic.datamodel.samples.SampleType;
import life.qbic.datamodel.samples.TSVSampleBean;
import life.qbic.ipspine.model.BasicSampleDTO;
import life.qbic.ipspine.model.JSONSOP;
import life.qbic.ipspine.model.PreparationExperimentStructure;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import groovy.json.JsonSlurper;

public class SOPToOpenbisTranslater {

  private static final Logger log = LogManager.getLogger(SOPToOpenbisTranslater.class);
  private final Map<String, String> taxonomyMap;
  private final Map<String, String> tissueMap;
  private final String designsFolder;

  public SOPToOpenbisTranslater(Map<String, String> taxonomyMap, Map<String, String> tissueMap, String designsFolder) {
    this.taxonomyMap = taxonomyMap;
    this.tissueMap = tissueMap;
    this.designsFolder = designsFolder;
  }

  private List<File> getAllFilesFromFolder(String folder) throws IOException {

    List<File> collect = Files.walk(Paths.get(folder))
            .filter(Files::isRegularFile)
            .map(x -> x.toFile())
            .collect(Collectors.toList());

    return collect;
  }

  public JSONSOP parseJSON(File file) throws FileNotFoundException {

    LinkedHashMap<BasicSampleDTO, List<BasicSampleDTO>> samplesToSources = new LinkedHashMap<>();
    InputStream targetStream = new FileInputStream(file);

    Set<String> measuredEntities = new HashSet<>();

    Map res = (Map) new JsonSlurper().parse(targetStream);
    String name = (String) res.get("experiment_name");
    String description = (String) res.get("description");
    List samples = (List) res.get("samples");

    for(Object s : samples) {
      Map sample = (Map) s;
      BasicSampleDTO leaf = mapToSampleDTO(sample);
      if(leaf.isMeasured()) {
        measuredEntities.addAll(leaf.getValidEntityTypeNames());
      }
      sampleSourceRecursionHelper(leaf, (List) sample.get("sources"), samplesToSources);
    }
    List<Pair<SampleType,Set<String>>> availableEntities = new ArrayList<>();
    for(BasicSampleDTO s : samplesToSources.keySet()) {
      Pair<SampleType, Set<String>> typeToOptions = Pair.of(s.getSampleType(), s.getValidEntityTypeNames());
      availableEntities.add(typeToOptions);
    }
    // collect all measured samples on higher levels
    for(BasicSampleDTO sample : samplesToSources.values().stream()
        .flatMap(List::stream)
        .collect(Collectors.toList())) {
      if(sample.isMeasured()) {
        measuredEntities.addAll(sample.getValidEntityTypeNames());
      }
    }
    Collections.reverse(availableEntities); // sources first, cells last etc.
    return new JSONSOP(name, description, measuredEntities, samplesToSources, availableEntities);
  }

  private void sampleSourceRecursionHelper(BasicSampleDTO sample, List<Object> sources, LinkedHashMap<BasicSampleDTO, List<BasicSampleDTO>> samplesToSources) {
    for(Object s : sources) {
      Map sourceMap = (Map) s;
      BasicSampleDTO source = mapToSampleDTO(sourceMap);
      if(samplesToSources.containsKey(sample)) {
        samplesToSources.get(sample).add(source);
      } else {
        List<BasicSampleDTO> newList = new ArrayList<>(Arrays.asList(source));
        samplesToSources.put(sample, newList);
      }
      List samples = (List) sourceMap.get("sources");
      sampleSourceRecursionHelper(source, samples, samplesToSources);
    }
    if(sources.isEmpty()) {
      samplesToSources.put(sample, Arrays.asList());
    }
  }

  private BasicSampleDTO mapToSampleDTO(Map jsonMap) {
    SampleType entityType = SampleType.valueOf(jsonMap.get("entity_type").toString());
    Set<String> validNames = new HashSet((List) jsonMap.get("valid_entity_names"));
    boolean isMeasured = false;
    if(jsonMap.containsKey("measured")) {
      isMeasured = (boolean) jsonMap.get("measured");
    }
    BasicSampleDTO source = new BasicSampleDTO(entityType, validNames, isMeasured);
    return source;
  }

  private boolean isParentOfSelectionHelper(BasicSampleDTO parentToFind, BasicSampleDTO currentSample, Map<BasicSampleDTO, List<BasicSampleDTO>> sampleMap) {
    if(parentToFind.equals(currentSample)) {
      return true;
    }
    boolean res = false;
    for(BasicSampleDTO parent : sampleMap.get(currentSample)) {
      res |= isParentOfSelectionHelper(parentToFind, parent, sampleMap);
    }
    return res;
  }

  private boolean isParentOfSelection(BasicSampleDTO sample, String selectedMeasuredType, Map<BasicSampleDTO, List<BasicSampleDTO>> sampleMap) {
    BasicSampleDTO measured = null;
    for(BasicSampleDTO key : sampleMap.keySet()) {
      if(key.getValidEntityTypeNames().contains(selectedMeasuredType)) {
        measured = key;
        break;
      }
    }
    boolean res = isParentOfSelectionHelper(sample, measured, sampleMap);
    System.err.println("is "+sample+" parent of "+measured+"?");
    System.err.println(res);
    return res;
  }

  public PreparationExperimentStructure getDesignForSOP(JSONSOP sop, List<Map<String, Object>> sampleInfos) {
    String sopName = sop.getName();

    Map<String, TSVSampleBean> knownDonors = new HashMap<>();
    Map<String, TSVSampleBean> knownSamples = new HashMap<>();
    Map<String, Object> designProps = new HashMap<>();
    Map<String, Object> extractProps = new HashMap<>();
    // SOP name is always set to the lowest level of sample preparation in order to be able to fetch
    // relevant samples later
    extractProps.put("Q_ADDITIONAL_INFO", sopName);

    List<TSVSampleBean> speciesSamples = new ArrayList<>();
    List<TSVSampleBean> extractSamples = new ArrayList<>();

    int uniqueSampleCode = 0;

    for (Map<String,Object> sampleInfo : sampleInfos) {
      log.error("handling: "+sampleInfo);
      String name = (String) sampleInfo.get("name");
      String donor = (String) sampleInfo.get("donor");
      Set<String> selectedEntityTypes = (Set<String>) sampleInfo.get("selected_entity_types");
      String description = (String) sampleInfo.get("description");
      String selectedMeasuredType = (String) sampleInfo.get("selected_measured_type");
      Object medium = sampleInfo.get("medium");
      Object timepoint = sampleInfo.get("timepoint");
      Object treatment = sampleInfo.get("treatment");

      LinkedHashMap<BasicSampleDTO, List<BasicSampleDTO>> sampleDTOMap = sop.getSamplesToSources();

      Map<BasicSampleDTO, TSVSampleBean> dtoToSampleMap = new HashMap<>();
      for (BasicSampleDTO s : sampleDTOMap.keySet()) {
        if(isParentOfSelection(s, selectedMeasuredType, sampleDTOMap)) {
          uniqueSampleCode++;
          SampleType type = s.getSampleType();
          String entityName = s.getFirstEntityTypeName();
          Set<String> possibleEntities = s.getValidEntityNames(selectedEntityTypes);
          //it is expected selectable options cannot overlap with non-selectable ones
          if (possibleEntities.size() > 0) {
            entityName = possibleEntities.iterator().next();
          }
          switch (type) {
            case Q_BIOLOGICAL_ENTITY:
              String uniqueDonorName = donor + entityName;
              if (knownDonors.containsKey(uniqueDonorName)) {
                dtoToSampleMap.put(s, knownDonors.get(uniqueDonorName));
              } else {
                TSVSampleBean donorSample =
                    createEntity(uniqueSampleCode, entityName, donor,
                        "Donor / sample source for " + name);
                speciesSamples.add(donorSample);
                dtoToSampleMap.put(s, donorSample);
                knownDonors.put(uniqueDonorName, donorSample);
              }
              break;
            case Q_BIOLOGICAL_SAMPLE:
              String uniqueSampleName = name+donor+entityName;
              if (knownSamples.containsKey(uniqueSampleName)) {
                System.err.println("name "+uniqueSampleName+" is known");
                dtoToSampleMap.put(s, knownSamples.get(uniqueSampleName));
              } else {
                TSVSampleBean tissue = createSample(uniqueSampleCode, entityName, name,
                    description);
                if (medium != null) {
                  tissue.addProperty("medium", medium);
                }
                if (timepoint != null) {
                  tissue.addProperty("timepoint", timepoint);
                }
                if (treatment != null) {
                  tissue.addProperty("treatment", treatment);
                }
                extractSamples.add(tissue);
                dtoToSampleMap.put(s, tissue);
                knownSamples.put(uniqueSampleName, tissue);
              }
              break;
            default:
              log.error("Sample type " + type + " not handled.");
          }
        }
      }
      for (BasicSampleDTO s : sampleDTOMap.keySet()) {
        List<BasicSampleDTO> parents = sampleDTOMap.get(s);
        for(BasicSampleDTO p : parents) {
          if(!dtoToSampleMap.containsKey(p)) {
            log.error("no parent");
            log.error(p);
          }
          if(dtoToSampleMap.containsKey(s)) {
            dtoToSampleMap.get(s).addParent(dtoToSampleMap.get(p));
          }
        }
      }
    }
    log.error(speciesSamples);
    log.error(extractSamples);
    OpenbisExperiment design = new OpenbisExperiment("preliminary name 1",
        ExperimentType.Q_EXPERIMENTAL_DESIGN, designProps);
    OpenbisExperiment extraction = new OpenbisExperiment("preliminary name 2",
        ExperimentType.Q_SAMPLE_EXTRACTION, extractProps);

    // we reverse the lists, so sources of other samples are handled first in the following registration steps
    Collections.reverse(speciesSamples);
    Collections.reverse(extractSamples);
    return new PreparationExperimentStructure(design, extraction, speciesSamples, extractSamples);
  }

  private TSVSampleBean createEntity(int uniqueCode, String speciesLabel, String name, String info) {
    HashMap<String, Object> metadata = new HashMap<>();
    metadata.put("Q_ADDITIONAL_INFO", info);
    metadata.put("Q_SECONDARY_NAME", name);
    String speciesCode = taxonomyMap.get(speciesLabel);
    if(speciesCode==null) {
      log.error("Code for "+speciesLabel+" not found in vocabulary.");
    }
    metadata.put("Q_NCBI_ORGANISM", speciesCode);

    return new TSVSampleBean(Integer.toString(uniqueCode), SampleType.Q_BIOLOGICAL_ENTITY, name, metadata);
  }

  private TSVSampleBean createSample(int uniqueCode, String tissue, String name, String info) {
    HashMap<String, Object> metadata = new HashMap<>();
    metadata.put("Q_ADDITIONAL_INFO", info);
    String tissueCode = tissueMap.get(tissue);
    if(tissueCode==null) {
      log.error("Code for "+tissue+" not found in vocabulary.");
    }
    metadata.put("Q_PRIMARY_TISSUE", tissueCode);

    return new TSVSampleBean(Integer.toString(uniqueCode), SampleType.Q_BIOLOGICAL_SAMPLE, name, metadata);
  }

  public List<JSONSOP> getSOPs() {
    List<JSONSOP> res = new ArrayList<>();
    try {
      List<File> sops = getAllFilesFromFolder(designsFolder);
      for (File sop : sops) {
        res.add(parseJSON(sop));
      }
    } catch (Exception e) {
      log.error("Could not parse SOPs");
    }
    return res;
  }
}
