package life.qbic.ipspine.model;

import life.qbic.datamodel.samples.SampleType;
import org.apache.commons.lang3.tuple.Pair;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JSONSOP {

    private String name;
    private String description;
    private Set<String> measuredEntities;
    private LinkedHashMap<BasicSampleDTO, List<BasicSampleDTO>> samplesToSources;
    private List<Pair<SampleType, Set<String>>> availableEntities;

    public JSONSOP(String name, String description, Set<String> measuredEntities, LinkedHashMap<BasicSampleDTO, List<BasicSampleDTO>> samplesToSources, List<Pair<SampleType, Set<String>>> availableEntities) {
        this.name = name;
        this.description = description;
        this.measuredEntities = measuredEntities;
        this.samplesToSources = samplesToSources;
        this.availableEntities = availableEntities;
    }

    public String getName() {
        return name;
    }

    public LinkedHashMap<BasicSampleDTO, List<BasicSampleDTO>> getSamplesToSources() {
        return samplesToSources;
    }

    public String getDescription() {
        return description;
    }

    public boolean isMeasuredEntity(String entityName) {
        return measuredEntities.contains(entityName);
    }

    public Set<String> getMeasuredEntities() {
        return measuredEntities;
    }

    public List<Pair<SampleType, Set<String>>> getAvailableEntities() {
        return availableEntities;
    }
}
