package life.qbic.ipspine.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import life.qbic.datamodel.experiments.ExperimentType;
import life.qbic.datamodel.experiments.OpenbisExperiment;
import life.qbic.datamodel.samples.ISampleBean;
import life.qbic.datamodel.samples.SampleType;
import life.qbic.datamodel.samples.TSVSampleBean;
import life.qbic.ipspine.registration.RegisterableExperiment;
import life.qbic.ipspine.registration.SampleCounter;

public class MeasurementExperimentStructure implements IExperimentStructure {

  private OpenbisExperiment analyteExperiment;
  private OpenbisExperiment measureExperiment;
  private List<TSVSampleBean> analytes;
  private List<TSVSampleBean> measurements;
  private ExperimentType measurementType;
  private Map<TSVSampleBean, String> beanToInfoString;

  public MeasurementExperimentStructure(OpenbisExperiment analyteExperiment,
      OpenbisExperiment measureExperiment, List<TSVSampleBean> analytes,
      List<TSVSampleBean> measurements, ExperimentType measurementType,
      Map<TSVSampleBean, String> beanToInfoString) {
    this.analyteExperiment = analyteExperiment;
    this.measureExperiment = measureExperiment;
    this.analytes = analytes;
    this.measurements = measurements;
    this.measurementType = measurementType;
    this.beanToInfoString = beanToInfoString;
  }

  public List<RegisterableExperiment> createExperimentsForRegistration(SampleCounter counter,
      String space, String project, ExperimentMetadata experimentDescription) {
    //TODO Experiment metadata?
    List<RegisterableExperiment> res = new ArrayList<>();
    String expCode = counter.getNewExperiment();
    Map<String, String> oldCodeToNew = new HashMap<>();
    for (TSVSampleBean s : analytes) {
      String analyteCode = counter.getNewBarcode();
      oldCodeToNew.put(s.getCode(), analyteCode);
      s.setCode(analyteCode);
      s.setExperiment(expCode);
      s.setSpace(space);
      s.setProject(project);
    }
    List<ISampleBean> analyteBeans = new ArrayList<>(analytes);
    RegisterableExperiment preparationExperiment = new RegisterableExperiment(expCode,
        ExperimentType.Q_SAMPLE_PREPARATION, analyteBeans, analyteExperiment.getMetadata());
    res.add(preparationExperiment);

    expCode = counter.getNewExperiment();
    for (TSVSampleBean s : measurements) {
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
    List<ISampleBean> measurementBeans = new ArrayList<>(measurements);
    RegisterableExperiment measurementExperiment = new RegisterableExperiment(expCode,
        measurementType, measurementBeans, measureExperiment.getMetadata());
    res.add(measurementExperiment);

    return res;
  }

  public String getTSVContent() {
    StringBuilder builder = new StringBuilder(1000);
    String header =
        String.join("\t", Arrays.asList("Identifier ", "Name", "Type", "Information")) + "\n";
    builder.append(header);
    for (TSVSampleBean s : measurements) {
      String info = beanToInfoString.get(s);
      String code = s.getCode();
      String name = s.getSecondaryName();
      String type = typeToReadable(s.getType());
      String row = String.join("\t", Arrays.asList(code, name, type, info)) + "\n";
      builder.append(row);
    }
    return builder.toString();
  }

  private String typeToReadable(SampleType type) {
    Map<SampleType, String> typeMap = new HashMap<>();
    typeMap.put(SampleType.Q_MS_RUN, "Mass Spectrometry");
    typeMap.put(SampleType.Q_NGS_SINGLE_SAMPLE_RUN, "NGSequencing");
    // typeMap.put(SampleType.Q_MS_RUN, "Mass Spectrometry");
    if (typeMap.containsKey(type))
      return typeMap.get(type);
    return "";
  }

}
