package life.qbic.ipspine.registration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import life.qbic.datamodel.experiments.ExperimentType;
import life.qbic.datamodel.samples.ISampleBean;
import life.qbic.datamodel.samples.TSVSampleBean;

public class RegisterableExperiment {

  private String code;
  private String type;
  private List<ISampleBean> samples;
  private Map<String, Object> properties;

  public RegisterableExperiment(String code, ExperimentType type, List<ISampleBean> speciesSamples,
      Map<String, Object> properties) {
    this.code = code;
    this.type = type.toString();
    this.samples = speciesSamples;
    this.properties = properties;
  }

  public String getType() {
    return type;
  }

  public String getCode() {
    return code;
  }

  public List<ISampleBean> getSamples() {
    List<ISampleBean> res = new ArrayList<>(samples);
    return res;
  }

  public Map<String, Object> getProperties() {
    return properties;
  }

  public Map<String, String> getStringProperties() {
    Map<String, String> res = new HashMap<>();
    for (String key : properties.keySet()) {
      res.put(key, properties.get(key).toString());
    }
    return res;
  }

  public void addSample(ISampleBean s) {
    samples.add((TSVSampleBean) s);
  }
}
