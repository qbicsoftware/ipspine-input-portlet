package life.qbic.ipspine.model;

import java.util.Comparator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;

public class SampleCodeComparator implements Comparator<Sample> {

  private static final Logger logger = LogManager.getLogger(SampleCodeComparator.class);

  private static final SampleCodeComparator instance = new SampleCodeComparator();

  public static SampleCodeComparator getInstance() {
    return instance;
  }

  private SampleCodeComparator() {}

  @Override
  public int compare(Sample o1, Sample o2) {
    String c1 = o1.getCode();
    String c2 = o2.getCode();
    if (!c1.startsWith("Q") || c1.contains("ENTITY") || !c2.startsWith("Q")
        || c2.contains("ENTITY"))
      return o1.getCode().compareTo(o2.getCode());
    try {
      // compares sample codes by projects, ending letters (999A --> 001B) and numbers (001A -->
      // 002A)
      int projCompare = c1.substring(0, 5).compareTo(c2.substring(0, 5));
      int numCompare = c1.substring(5, 8).compareTo(c2.substring(5, 8));
      int letterCompare = c1.substring(8, 9).compareTo(c2.substring(8, 9));
      if (projCompare != 0)
        return projCompare;
      else {
        if (letterCompare != 0)
          return letterCompare;
        else
          return numCompare;
      }
    } catch (Exception e) {
      logger.warn("Could not split code " + c1 + " or " + c2
          + ". Falling back to primitive lexicographical comparison.");
    }
    return o1.getCode().compareTo(o2.getCode());
  }
  
}
