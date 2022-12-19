package life.qbic.ipspine.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import life.qbic.datamodel.experiments.ExperimentType;
import life.qbic.datamodel.experiments.OpenbisExperiment;
import life.qbic.datamodel.samples.ISampleBean;
import life.qbic.datamodel.samples.TSVSampleBean;
import life.qbic.ipspine.registration.RegisterableExperiment;
import life.qbic.ipspine.registration.SampleCounter;

public class PreparationExperimentStructure implements IExperimentStructure {

  private OpenbisExperiment designExperiment;
  private OpenbisExperiment extractExperiment;
  private List<TSVSampleBean> speciesSamples;
  private List<TSVSampleBean> extractSamples;

  public PreparationExperimentStructure(OpenbisExperiment designExperiment,
      OpenbisExperiment extractExperiment, List<TSVSampleBean> speciesSamples,
      List<TSVSampleBean> extractSamples) {
    this.designExperiment = designExperiment;
    this.extractExperiment = extractExperiment;
    this.speciesSamples = speciesSamples;
    this.extractSamples = extractSamples;
  }

  public List<RegisterableExperiment> createExperimentsForRegistration(SampleCounter counter,
      String space, String project, String experimentDescription) {
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
    if(experimentDescription!=null && !experimentDescription.isEmpty()) {
      metadata.put("Q_SECONDARY_NAME", experimentDescription);
    }

    RegisterableExperiment culture = new RegisterableExperiment(expCode,
        ExperimentType.Q_SAMPLE_EXTRACTION, extractBeans, metadata);
    res.add(culture);

    return res;
  }

}
