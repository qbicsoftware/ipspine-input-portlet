package life.qbic.ipspine.control;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import life.qbic.datamodel.experiments.ExperimentType;
import life.qbic.datamodel.experiments.OpenbisExperiment;
import life.qbic.datamodel.samples.SampleType;
import life.qbic.datamodel.samples.TSVSampleBean;
import life.qbic.ipspine.model.MeasurementExperimentStructure;

public class MeasurementExperimentCreator {

  private static final Logger log = LogManager.getLogger(MeasurementExperimentCreator.class);

  public MeasurementExperimentStructure createStructure(Map<String, String> experimentMetadata,
      List<Map<String, Object>> sampleInfos) {

    Map<String, Object> analyteProps = new HashMap<>();
    Map<String, Object> measurementProps = new HashMap<>();
    Map<TSVSampleBean, String> beanToInfoString = new HashMap<>();

    // extractProps.put("Q_ADDITIONAL_INFO", sopName);

    List<TSVSampleBean> analyteSamples = new ArrayList<>();
    List<TSVSampleBean> measurementSamples = new ArrayList<>();

    int uniqueSampleCode = 0;

    ExperimentType experimentType =
        ExperimentType.valueOf(experimentMetadata.get("EXPERIMENT_TYPE"));

    experimentMetadata.remove("EXPERIMENT_TYPE");

    switch (experimentType) {
      case Q_MS_MEASUREMENT:
        for (Map<String, Object> sample : sampleInfos) {
          String name = (String) sample.get("Name");
          String desc = (String) sample.get("Description");
          uniqueSampleCode++;
          TSVSampleBean proteins = createAnalyte(uniqueSampleCode, "PROTEINS", name, desc);
          analyteSamples.add(proteins);
          List<String> parents = (List<String>) sample.get("Parents");
          for (String parent : parents) {
            proteins.addParentID(parent);
          }

          uniqueSampleCode++;
          TSVSampleBean measurement =
              createMeasurement(uniqueSampleCode, SampleType.Q_MS_RUN, name + " run", desc);
          measurement.addParent(proteins);
          beanToInfoString.put(measurement, (String) sample.get("Info"));
          measurementSamples.add(measurement);
        }
        break;
      case Q_DIGIWEST_MEASUREMENT:
        for (Map<String, Object> sample : sampleInfos) {
          String name = (String) sample.get("Name");
          String desc = (String) sample.get("Description");
          uniqueSampleCode++;
          TSVSampleBean proteins = createAnalyte(uniqueSampleCode, "PROTEINS", name, desc);
          analyteSamples.add(proteins);
          List<String> parents = (List<String>) sample.get("Parents");
          for (String parent : parents) {
            proteins.addParentID(parent);
          }

          uniqueSampleCode++;
          TSVSampleBean measurement =
              createMeasurement(uniqueSampleCode, SampleType.Q_DIGIWEST_RUN, name + " run", desc);
          measurement.addParent(proteins);
          beanToInfoString.put(measurement, (String) sample.get("Info"));
          measurementSamples.add(measurement);
        }
        break;
      case Q_NGS_MEASUREMENT:
        String nucleicAcid = experimentMetadata.get("ANALYTE_TYPE");
        experimentMetadata.remove("ANALYTE_TYPE");
        for (Map<String, Object> sample : sampleInfos) {
          String name = (String) sample.get("Name");
          String desc = (String) sample.get("Description");
          uniqueSampleCode++;
          TSVSampleBean dna = createAnalyte(uniqueSampleCode, nucleicAcid, name, desc);
          analyteSamples.add(dna);
          List<String> parents = (List<String>) sample.get("Parents");
          for (String parent : parents) {
            dna.addParentID(parent);
          }
          uniqueSampleCode++;
          TSVSampleBean measurement = createMeasurement(uniqueSampleCode,
              SampleType.Q_NGS_SINGLE_SAMPLE_RUN, name + " run", desc);
          measurement.addParent(dna);
          beanToInfoString.put(measurement, (String) sample.get("Info"));
          measurementSamples.add(measurement);
        }
        break;
      default:
        log.error("Unknown or unimplemented experiment type: " + experimentType);
        break;
    }


    for (String key : experimentMetadata.keySet()) {
      measurementProps.put(key, experimentMetadata.get(key));
    }

    OpenbisExperiment measurementExp =
        new OpenbisExperiment("preliminary name 1", experimentType, measurementProps);
    OpenbisExperiment analyteExp = new OpenbisExperiment("preliminary name 2",
        ExperimentType.Q_SAMPLE_PREPARATION, analyteProps);

    return new MeasurementExperimentStructure(analyteExp, measurementExp, analyteSamples,
        measurementSamples, experimentType, beanToInfoString);
  }

  private TSVSampleBean createMeasurement(int uniqueCode, SampleType type, String name,
      String info) {
    HashMap<String, Object> metadata = new HashMap<>();
    metadata.put("Q_ADDITIONAL_INFO", info);
    metadata.put("Q_SECONDARY_NAME", name);

    return new TSVSampleBean(Integer.toString(uniqueCode), type, name, metadata);
  }

  private TSVSampleBean createAnalyte(int uniqueCode, String analyte, String name, String info) {
    HashMap<String, Object> metadata = new HashMap<>();
    metadata.put("Q_ADDITIONAL_INFO", info);
    metadata.put("Q_SAMPLE_TYPE", analyte);

    return new TSVSampleBean(Integer.toString(uniqueCode), SampleType.Q_TEST_SAMPLE, name,
        metadata);
  }
}
