package life.qbic.ipspine.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import life.qbic.datamodel.experiments.ExperimentType;
import life.qbic.datamodel.experiments.OpenbisExperiment;
import life.qbic.datamodel.samples.ISampleBean;
import life.qbic.datamodel.samples.TSVSampleBean;
import life.qbic.ipspine.control.ExperimentalDesignPropertyWrapper;
import life.qbic.ipspine.control.ParserHelpers;
import life.qbic.ipspine.registration.RegisterableExperiment;
import life.qbic.ipspine.registration.SampleCounter;

public class PreparationExperimentStructure implements IExperimentStructure {

  private final OpenbisExperiment designExperiment;
  private final OpenbisExperiment extractExperiment;
  private final List<TSVSampleBean> speciesSamples;
  private final List<TSVSampleBean> extractSamples;

  private ExperimentalDesignPropertyWrapper experimentalDesign;
  public PreparationExperimentStructure(OpenbisExperiment designExperiment,
      OpenbisExperiment extractExperiment, List<TSVSampleBean> speciesSamples,
      List<TSVSampleBean> extractSamples) {
    this.designExperiment = designExperiment;
    this.extractExperiment = extractExperiment;
    this.speciesSamples = speciesSamples;
    this.extractSamples = extractSamples;
  }

  public List<RegisterableExperiment> createExperimentsForRegistration(SampleCounter counter,
      String space, String project, ExperimentMetadata experimentLevelMetadata) {
    List<RegisterableExperiment> res = new ArrayList<>();
    String expCode = counter.getNewExperiment();
    Map<String, String> oldCodeToNew = new HashMap<>();
    for (TSVSampleBean s : speciesSamples) {
      String entityCode = counter.getNewEntity();
      oldCodeToNew.put(s.getCode(), entityCode);
      s.setCode(entityCode);
      s.setExperiment(expCode);
      s.setSpace(space);
      s.setProject(project);
    }
    List<ISampleBean> speciesBeans = new ArrayList<>(speciesSamples);
    RegisterableExperiment design = new RegisterableExperiment(expCode,
        ExperimentType.Q_EXPERIMENTAL_DESIGN, speciesBeans, designExperiment.getMetadata());
    res.add(design);

    expCode = counter.getNewExperiment();
    for (TSVSampleBean s : extractSamples) {
      String barcode = counter.getNewBarcode();
      oldCodeToNew.put(s.getCode(), barcode);
      s.setCode(barcode);
      s.setExperiment(expCode);
      s.setSpace(space);
      s.setProject(project);

      // set parents
      List<String> newParents = new ArrayList<String>();
      for (String par : s.getParentIDs()) {
        newParents.add(oldCodeToNew.get(par));
      }
      s.setParents(new ArrayList<ISampleBean>());
      for (String par : newParents) {
        s.addParentID(par);
      }
    }
    List<ISampleBean> extractBeans = new ArrayList<>(extractSamples);

    Map<String, Object> metadata = extractExperiment.getMetadata();
    System.out.println("experiment metadata: "+metadata);
    metadata.putAll(experimentLevelMetadata.getExtractionExperimentMetadata());
    System.out.println("experiment level metadata: "+metadata);

    RegisterableExperiment culture = new RegisterableExperiment(expCode,
        ExperimentType.Q_SAMPLE_EXTRACTION, extractBeans, metadata);
    res.add(culture);

    List<ISampleBean> allSamples = new ArrayList<>(speciesSamples);
    allSamples.addAll(extractSamples);

    this.experimentalDesign = ParserHelpers.samplesWithMetadataToExperimentalFactorStructure(allSamples);

    return res;
  }

  public ExperimentalDesignPropertyWrapper getExperimentalDesignProperties() {
    return this.experimentalDesign;
  }

  public List<TSVSampleBean> getExtracts() {
    return extractSamples;
  }
  public List<TSVSampleBean> getSpeciesSamples() {
    return speciesSamples;
  }

  public OpenbisExperiment getExtractExperiment() {
    return extractExperiment;
  }

  public OpenbisExperiment getDesignExperiment() {
    return designExperiment;
  }
}
