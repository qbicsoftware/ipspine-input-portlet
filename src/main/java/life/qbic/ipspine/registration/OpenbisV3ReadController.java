package life.qbic.ipspine.registration;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.DataSetFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.search.DataSetSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.id.ExperimentIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.update.ExperimentUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.person.Person;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.person.fetchoptions.PersonFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.person.search.PersonSearchCriteria;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

  public String getPersonMail(String personID) {
    PersonSearchCriteria criteria = new PersonSearchCriteria();
    criteria.withUserId().thatEquals(personID);
    SearchResult<Person> res = v3Wrapper.searchPersons(criteria, new PersonFetchOptions());
    for (Person p : res.getObjects()) {
      return p.getEmail();
    }
    return "";
  }

  public List<String> getProjectCodesOfSpace(String space) {
    List<String> res = new ArrayList<>();
    SearchResult<Project> searchResults = v3Wrapper.getProjectsOfSpace(space);
    for (Project p : searchResults.getObjects()) {
      res.add(p.getCode());
    }
    return res;
  }

  public List<DataSet> findDatasetsOfProject(String projectCode, String dataType) {
    DataSetSearchCriteria criteria = new DataSetSearchCriteria();
    criteria.withAndOperator();
    criteria.withSample().withCode().thatContains(projectCode);
    criteria.withType().withCode().thatContains(dataType);

    DataSetFetchOptions fetchOptions = new DataSetFetchOptions();
    fetchOptions.withSample().withProject();
    fetchOptions.withProperties();
    fetchOptions.withType();

    SearchResult<DataSet> result = v3Wrapper.searchDatasets(criteria, fetchOptions);
    return result.getObjects();
  }

  public List<Set<DataSet>> findDuplicateDatasetsOfProject(String projectCode, List<String> dataTypes) {
    List<Set<DataSet>> res = new ArrayList<>();

    for(String type : dataTypes) {
      Map<String,List<DataSet>> datasetsPerSample = new HashMap<>();
      List<DataSet> datasets = findDatasetsOfProject(projectCode, type);

      for (DataSet d : datasets) {
        String sampleCode = d.getSample().getCode();
        if(datasetsPerSample.containsKey(sampleCode)) {
          datasetsPerSample.get(sampleCode).add(d);
        } else {
          List<DataSet> newList = new ArrayList<>(Arrays.asList(d));
          datasetsPerSample.put(sampleCode, newList);
        }
      }

      for(List<DataSet> dList : datasetsPerSample.values()) {
        Set<DataSet> duplicates = new HashSet<>();
        if (dList.size() > 1) {
          for (int i = 0; i < dList.size(); i++) {
            for (int j = i + 1; j < dList.size(); j++) {
              DataSet a = dList.get(i);
              DataSet b = dList.get(j);
              if (compareDataSetMetadata(a, b)) {
                duplicates.add(a);
                duplicates.add(b);
              }
            }
          }
        }
        if (!duplicates.isEmpty()) {
          res.add(duplicates);
        }
      }
    }
    return res;
  }

  private boolean compareDataSetMetadata(DataSet a, DataSet b) {
    boolean res = a.getType().equals(b.getType());
    for(String key : a.getProperties().keySet()) {
      if(key.equals("Q_MEASUREMENT_START_DATE")) {
        res &= isSimilarTime(a.getProperties().get(key), b.getProperties().get(key));
      } else {
        res &= a.getProperties().get(key).equals(b.getProperties().get(key));
      }
    }
    return res;
  }

  private boolean isSimilarTime(String date1, String date2) {
    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss XXXX");
    LocalDateTime t1 = LocalDateTime.parse(date1, dtf);
    LocalDateTime t2 = LocalDateTime.parse(date2, dtf);
    if(t1.isEqual(t2)) {
      return true;
    }
    if(t1.isAfter(t2)) {
      return t1.minusHours(1).equals(t2);
    }
    if(t1.isBefore(t2)) {
      return t2.minusHours(1).equals(t1);
    }
    return false;
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

  public void updateExperiment(Experiment experiment) {
    ExperimentUpdate exp = new ExperimentUpdate();
    String expID = experiment.getIdentifier().getIdentifier();
    exp.setExperimentId(new ExperimentIdentifier(expID));
    Map<String, String> props = new HashMap<>();

    exp.setProperties(experiment.getProperties());

    logger.info("Updating " + expID);

    v3Wrapper.updateExperiments(Arrays.asList(exp));
    }
}
