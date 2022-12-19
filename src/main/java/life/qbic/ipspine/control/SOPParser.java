package life.qbic.ipspine.control;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import life.qbic.ipspine.model.JSONSOP;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import life.qbic.datamodel.experiments.ExperimentType;
import life.qbic.datamodel.experiments.OpenbisExperiment;
import life.qbic.datamodel.samples.SampleType;
import life.qbic.datamodel.samples.TSVSampleBean;
import life.qbic.ipspine.model.PreparationExperimentStructure;
import life.qbic.ipspine.model.SOP;

// TODO put this information into json files
@Deprecated
public class SOPParser {

  private static final Logger log = LogManager.getLogger(SOPParser.class);

  final String HOMO_SAPIENS_NCBI = "9606";
  final String DISC = "INTERVERTEBRAL_DISC";
  final String IPSC = "iPSCs";
  final String MARROW = "BONE_MARROW";
  final String BLOOD = "WHOLE_BLOOD";
  final String PBMC = "PBMC";
  final String TIE2 = "TIE2_POSITIVE_CELLS";
  final String NP_CELLS = "Nucleus pulposus cells";

  public PreparationExperimentStructure getDesignForSOP(SOP sop, Map<String, String> sampleInfo) {
    String sopName = sop.getName();

    Map<String, Object> designProps = new HashMap<>();
    Map<String, Object> extractProps = new HashMap<>();
    // SOP name is always set to the lowest level of sample preparation in order to be able to fetch
    // relevant samples later
    extractProps.put("Q_ADDITIONAL_INFO", sopName);

    List<TSVSampleBean> speciesSamples = new ArrayList<>();
    List<TSVSampleBean> extractSamples = new ArrayList<>();

    int uniqueSampleCode = 0;
    
    switch (sopName) {
      case "D1.1: NP cell isolation from human discs":
        for (String name : sampleInfo.keySet()) {
          uniqueSampleCode++;
          TSVSampleBean patient =
              createEntity(uniqueSampleCode, HOMO_SAPIENS_NCBI, "Source for disc sample " + name, "");
          speciesSamples.add(patient);

          uniqueSampleCode++;
          TSVSampleBean disc = createSample(uniqueSampleCode, DISC, name + " disc", "");
          disc.addParent(patient);
          extractSamples.add(disc);

          uniqueSampleCode++;
          TSVSampleBean npCells = createSample(uniqueSampleCode, "CELL_CULTURE", name, sampleInfo.get(name));
          npCells.addProperty("Q_TISSUE_DETAILED", NP_CELLS);
          npCells.addParent(disc);
          extractSamples.add(npCells);
        }
        break;
      case "D1.1: Blood and bone marrow sampling":
        for (String name : sampleInfo.keySet()) {
          uniqueSampleCode++;
          
          TSVSampleBean patient = createEntity(uniqueSampleCode, HOMO_SAPIENS_NCBI,
              "Source for blood and bone marrow sample " + name, "");
          speciesSamples.add(patient);
          
          uniqueSampleCode++;
          
          TSVSampleBean blood = createSample(uniqueSampleCode, BLOOD, name + " blood", sampleInfo.get(name));
          blood.addParent(patient);
          extractSamples.add(blood);
          
          uniqueSampleCode++;

          TSVSampleBean marrow = createSample(uniqueSampleCode, MARROW, name + " marrow", sampleInfo.get(name));
          marrow.addParent(patient);
          extractSamples.add(marrow);
        }
        break;
      case "D1.2: iPSC generation from Tie2+ cells":
        for (String name : sampleInfo.keySet()) {
          uniqueSampleCode++;
          
          TSVSampleBean source =
              createEntity(uniqueSampleCode, HOMO_SAPIENS_NCBI, "Source for Tie2+ cells for " + name, "");
          speciesSamples.add(source);
          
          uniqueSampleCode++;

          TSVSampleBean tie2 = createSample(uniqueSampleCode, TIE2, name + " tie2+", "");
          tie2.addParent(source);
          extractSamples.add(tie2);
          
          uniqueSampleCode++;

          TSVSampleBean ipscs = createSample(uniqueSampleCode, "CELL_CULTURE", name, sampleInfo.get(name));
          ipscs.addParent(tie2);
          ipscs.addProperty("Q_TISSUE_DETAILED", IPSC);
          extractSamples.add(ipscs);
        }
        break;
      case "D1.2: iPSC generation from PBMCs":
        for (String name : sampleInfo.keySet()) {
          
          uniqueSampleCode++;
          
          TSVSampleBean source =
              createEntity(uniqueSampleCode, HOMO_SAPIENS_NCBI, "Source for PBMCs for " + name, "");
          speciesSamples.add(source);

          uniqueSampleCode++;
          
          TSVSampleBean pbmcs = createSample(uniqueSampleCode, PBMC, name + " PBMCs", "");
          pbmcs.addParent(source);
          extractSamples.add(pbmcs);

          uniqueSampleCode++;
          
          TSVSampleBean ipscs = createSample(uniqueSampleCode, "CELL_CULTURE", name, sampleInfo.get(name));
          ipscs.addParent(pbmcs);
          ipscs.addProperty("Q_TISSUE_DETAILED", IPSC);
          extractSamples.add(ipscs);
        }
        break;
      case "D3.1: NP culture conditions of the disc niche":
        
        uniqueSampleCode++;
        
        TSVSampleBean source = createEntity(uniqueSampleCode, HOMO_SAPIENS_NCBI, "Source for NP culture", "");
        speciesSamples.add(source);

        for (String name : sampleInfo.keySet()) {

          uniqueSampleCode++;
          
          TSVSampleBean npCells = createSample(uniqueSampleCode, "CELL_CULTURE", name, sampleInfo.get(name));
          npCells.addParent(source);
          npCells.addProperty("Q_TISSUE_DETAILED", NP_CELLS);
          extractSamples.add(npCells);
        }
        break;
      // case "D3.1: NP culture conditions healthy disc niche":
      // TSVSampleBean source = createEntity(HOMO_SAPIENS_NCBI, "Source for NP culture", "");
      // break;
      // case "D3.1: NP cell culture conditions degenerative Disc niche":
      // TSVSampleBean source = createEntity(HOMO_SAPIENS_NCBI, "Source for NP culture", "");
      // break;

      default:
        log.error("Unknown SOP: " + sopName);
        break;
    }

    OpenbisExperiment design = new OpenbisExperiment("preliminary name 1",
        ExperimentType.Q_EXPERIMENTAL_DESIGN, designProps);
    OpenbisExperiment extraction = new OpenbisExperiment("preliminary name 2",
        ExperimentType.Q_SAMPLE_EXTRACTION, extractProps);

    return new PreparationExperimentStructure(design, extraction, speciesSamples, extractSamples);
  }

  private TSVSampleBean createEntity(int uniqueCode, String speciesCode, String name, String info) {
    HashMap<String, Object> metadata = new HashMap<>();
    metadata.put("Q_ADDITIONAL_INFO", info);
    metadata.put("Q_SECONDARY_NAME", name);
    metadata.put("Q_NCBI_ORGANISM", speciesCode);

    return new TSVSampleBean(Integer.toString(uniqueCode), SampleType.Q_BIOLOGICAL_ENTITY, name, metadata);
  }

  private TSVSampleBean createSample(int uniqueCode, String tissue, String name, String info) {
    HashMap<String, Object> metadata = new HashMap<>();
    metadata.put("Q_ADDITIONAL_INFO", info);
    metadata.put("Q_PRIMARY_TISSUE", tissue);

    return new TSVSampleBean(Integer.toString(uniqueCode), SampleType.Q_BIOLOGICAL_SAMPLE, name, metadata);
  }

}
