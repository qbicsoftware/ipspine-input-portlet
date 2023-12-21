package life.qbic.ipspine.model;

import java.util.List;
import life.qbic.ipspine.registration.RegisterableExperiment;
import life.qbic.ipspine.registration.SampleCounter;

public interface IExperimentStructure {

  List<RegisterableExperiment> createExperimentsForRegistration(SampleCounter counter, String space,
      String project, ExperimentMetadata experimentLevelMetadata);

}
