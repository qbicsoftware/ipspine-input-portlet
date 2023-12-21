package life.qbic.ipspine.model;

import life.qbic.datamodel.samples.SampleType;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BasicSampleDTO {

    SampleType sampleType;
    Set<String> validEntityTypeNames;
    boolean isMeasured = false;

    private static final Logger logger = LogManager.getLogger(BasicSampleDTO.class);

    public BasicSampleDTO(SampleType type, Set<String> validNames, boolean isMeasured) {
        this.sampleType = type;
        this.validEntityTypeNames = validNames;
        this.isMeasured = isMeasured;
    }

    public SampleType getSampleType() {
        return sampleType;
    }

    public boolean isMeasured() {
        return isMeasured;
    }

    /**
     * returns all entries from a set of Strings that this sample allows according to the vocabulary it was created with
     * @param entityValues a set of possible entity values that should be checked
     * @return the intersection of the provided set and the valid entity names this sample was created with
     */
    public Set<String> getValidEntityNames(Set<String> entityValues) {
        Set<String> intersection = new HashSet<>(entityValues);
        intersection.retainAll(validEntityTypeNames);
        if(intersection.size() > 1) {
            logger.error("more than 1 valid entity provided: "+intersection);
        }
        return intersection;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BasicSampleDTO that = (BasicSampleDTO) o;
        return isMeasured == that.isMeasured && sampleType == that.sampleType && validEntityTypeNames.equals(that.validEntityTypeNames);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sampleType, validEntityTypeNames, isMeasured);
    }

    @Override
    public String toString() {
        String measured = "";
        if(isMeasured) {
            measured = "measured ";
        }
        return measured+sampleType+" containing "+ validEntityTypeNames;
    }

    //TODO
    public String getFirstEntityTypeName() {
        return validEntityTypeNames.iterator().next();
    }

    public Set<String> getValidEntityTypeNames() {
        return validEntityTypeNames;
    }
}
