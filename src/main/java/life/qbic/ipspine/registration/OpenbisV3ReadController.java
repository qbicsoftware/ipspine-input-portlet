package life.qbic.ipspine.registration;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.Project;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.Space;

public class OpenbisV3ReadController {

  private OpenbisV3APIWrapper v3Wrapper;
  private static final Logger logger = LogManager.getLogger(OpenbisV3ReadController.class);

  public OpenbisV3ReadController(OpenbisV3APIWrapper v3) {
    v3Wrapper = v3;
  }

  public List<String> getSpaceNames() {
    List<String> res = new ArrayList<>();
    SearchResult<Space> searchResults = v3Wrapper.getSpacesForUser();
    for (Space s : searchResults.getObjects()) {
      res.add(s.getCode());
    }
    return res;
  }

  public List<Project> getProjectsOfSpace(String space) {
    SearchResult<Project> searchResults = v3Wrapper.getProjectsOfSpace(space);
    return searchResults.getObjects();
  }
  
  public List<String> getProjectCodesOfSpace(String space) {
    List<String> res = new ArrayList<>();
    SearchResult<Project> searchResults = v3Wrapper.getProjectsOfSpace(space);
    for (Project p : searchResults.getObjects()) {
      res.add(p.getCode());
    }
    return res;
  }

  public Date getExperimentModificationDate(String experimentID) {
    try {
      return v3Wrapper.getExperimentByID(experimentID).getModificationDate();
    } catch (NullPointerException e) {
      return null;
    }
  }

  public boolean spaceExists(String space) {
    return !v3Wrapper.getSpace(space).getObjects().isEmpty();
  }

  public boolean projectExists(String project) {
    return !v3Wrapper.getProject(project).getObjects().isEmpty();
  }

  public boolean experimentWithIDExists(String experimentID) {
    return v3Wrapper.getExperimentByID(experimentID) != null;
  }

  public boolean sampleExists(String sampleCode) {
    return !v3Wrapper.searchSampleWithCode(sampleCode).getObjects().isEmpty();
  }

  public Sample findSampleWithDescendantsByCode(String code) {
    for (Sample s : v3Wrapper.searchSampleWithCodeAndDescendants(code).getObjects()) {
      return s;
    }
    return null;
  }

  public Sample findSampleByCode(String code) {
    for (Sample s : v3Wrapper.searchSampleWithCode(code).getObjects()) {
      return s;
    }
    return null;
  }

  public List<Sample> getSamplesOfProject(String projectCode) {
    return v3Wrapper.getSamplesOfProject(projectCode);
  }

  public List<Experiment> getExperimentsForProject(String projectID) {
    return v3Wrapper.getExperimentsOfProject(projectID);
  }


}
