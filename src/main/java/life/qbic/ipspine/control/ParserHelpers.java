package life.qbic.ipspine.control;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import life.qbic.datamodel.samples.ISampleBean;
import life.qbic.xml.loci.GeneLocus;
import life.qbic.xml.manager.LociParser;
import life.qbic.xml.manager.StudyXMLParser;
import life.qbic.xml.properties.Property;
import life.qbic.xml.properties.PropertyType;
import life.qbic.xml.properties.Unit;
import life.qbic.xml.study.Qexperiment;
import life.qbic.xml.study.Qproperty;
import life.qbic.xml.study.TechnologyType;

/**
 * Helper functions used for sample creation
 * 
 * @author Andreas Friedrich
 * 
 */
public class ParserHelpers {

  private static final Pattern COLON = Pattern.compile(":");
  private static final Pattern SEMICOLON = Pattern.compile(";");
  private static final Pattern WHITESPACE = Pattern.compile(" ");
  private static final String UTF8_BOM = "\uFEFF";

  private static Logger logger = LogManager.getLogger(ParserHelpers.class);

  public static final Map<String, TechnologyType> typeToTechnology =
      new HashMap<String, TechnologyType>() {
        /**
         * 
         */
        private static final long serialVersionUID = -5482251206396114925L;

        {
          put("CARBOHYDRATES", new TechnologyType("Metabolite Profiling"));
          put("SMALLMOLECULES", new TechnologyType("Metabolite Profiling"));
          put("LIPIDS", new TechnologyType("Lipidomics"));
          put("M_RNA", new TechnologyType("mRNA Profiling"));
          put("PEPTIDES", new TechnologyType("Peptidomics"));
          put("PHOSPHOLIPIDS", new TechnologyType("Lipidomics"));
          put("PHOSPHOPEPTIDES", new TechnologyType("Peptidomics"));
          put("PHOSPHOPROTEINS", new TechnologyType("Proteomics"));
          put("R_RNA", new TechnologyType("rRNA Profiling"));
          put("PROTEINS", new TechnologyType("Proteomics"));
          put("RNA", new TechnologyType("Transcriptomics"));
          put("DNA", new TechnologyType("Genomics"));
        };
      };

  /**
   * 
   * @param sampleInfos
   * @param omicsTypes
   * @param designTypes optional keywords denoting which experimental designs were used
   * @return
   * @throws JAXBException
   */
  public static String createDesignXML(ExperimentalDesignPropertyWrapper sampleInfos,
      List<TechnologyType> omicsTypes, Set<String> designTypes) throws JAXBException {
    StudyXMLParser p = new StudyXMLParser();
    JAXBElement<Qexperiment> res = p.createNewDesign(designTypes, omicsTypes,
        sampleInfos.getExperimentalDesign(), sampleInfos.getProperties());
    String xml = p.toString(res);
    return xml;
  }


  /**
   * updates the existing experimental design xml of a project: deletes sample references to samples
   * that don't exist in openbis, adds factors, properties for new samples to be registered.
   * 
   * @param oldDesignXML old experimental design xml
   * @param newDesignProperties properties and factors of the new samples
   * @param techTypes technology types of the newly registered experiments
   * @param designTypes optional keywords denoting which experimental designs were used
   * @return xml string of the merged design to be registered in openbis
   */
  public static String mergeExperimentalDesignXMLs(String oldDesignXML,
      ExperimentalDesignPropertyWrapper newDesignProperties, List<TechnologyType> techTypes,
      Set<String> designTypes) {
    Map<String, Map<Pair<String, String>, List<String>>> design =
        newDesignProperties.getExperimentalDesign();
    Map<String, List<Qproperty>> props = newDesignProperties.getProperties();

    String res = null;
    StudyXMLParser xmlParser = new StudyXMLParser();
    try {
      JAXBElement<Qexperiment> existing = xmlParser.parseXMLString(oldDesignXML);
      if (existing == null) {
        existing = xmlParser.getEmptyXML();
      }
      JAXBElement<Qexperiment> mergedDesign =
          xmlParser.mergeDesigns(existing, designTypes, techTypes, design, props);
      res = xmlParser.toString(mergedDesign);
    } catch (JAXBException e) {
      e.printStackTrace();
    }
    return res;
  }

  private static Property createPropertyAndCleanMap(String label, Map<String, Object> metadata) {
    String value = (String) metadata.get(label);
    metadata.remove(label);
    return new Property(label, value, PropertyType.Factor);
  }

  /**
   * collects experimental factors and properties from preliminary sample objects and wraps them for
   * later conversion to xml. removes preliminary information from sample metadata.
   * 
   * @param samples
   * @return
   * @throws JAXBException
   */
  public static ExperimentalDesignPropertyWrapper samplesWithMetadataToExperimentalFactorStructure(
      List<ISampleBean> samples) {
    System.err.println("creating new experimental design");
    Map<String, Map<Pair<String, String>, List<String>>> expDesign =
        new HashMap<>();
    Map<String, List<Property>> otherProps = new HashMap<>();
    for (ISampleBean s : samples) {
      String code = s.getCode();
      List<Property> props = new ArrayList<>();
      Map<String, Object> metadata = s.getMetadata();

      // typical factors
      List<String> knownLabels = new ArrayList<>(Arrays.asList("medium", "treatment", "timepoint"));
      for(String label : knownLabels) {
        if(metadata.containsKey(label)) {
          props.add(createPropertyAndCleanMap(label, metadata));
        }
      }

      for (Property p : props) {
        if (p.getType().equals(PropertyType.Factor)) {
          String lab = p.getLabel();
          String val = p.getValue();
          String unit = "";
          if (p.hasUnit())
            unit = p.getUnit().getValue();
          Pair<String, String> valunit = new ImmutablePair<>(val, unit);
          if (expDesign.containsKey(lab)) {
            Map<Pair<String, String>, List<String>> levels = expDesign.get(lab);
            if (levels.containsKey(valunit)) {
              levels.get(valunit).add(code);
            } else {
              levels.put(valunit, new ArrayList<>(Arrays.asList(code)));
            }
          } else {
            Map<Pair<String, String>, List<String>> newLevel =
                new HashMap<>();
            newLevel.put(valunit, new ArrayList<>(Arrays.asList(code)));
            expDesign.put(lab, newLevel);
          }

        } else {
          if (otherProps.containsKey(code)) {
            otherProps.get(code).add(p);
          } else {
            otherProps.put(code, new ArrayList<>(Arrays.asList(p)));
          }
        }
      }
    }
    System.err.println("returning new experimental design");
    return new ExperimentalDesignPropertyWrapper(expDesign, otherProps);
  }

  public static void translateIdentifiersInExperimentalDesign(Map<String, String> idMap,
      ExperimentalDesignPropertyWrapper importedDesignProperties) {
    Map<String, List<Qproperty>> properties = importedDesignProperties.getProperties();
    Map<String, Map<Pair<String, String>, List<String>>> factors =
        importedDesignProperties.getExperimentalDesign();
    for (String extID : idMap.keySet()) {
      if (properties.containsKey(extID)) {
        String barcode = idMap.get(extID);
        properties.put(barcode, properties.get(extID));
        properties.remove(extID);
      }
    }
    for (String label : factors.keySet()) {
      Map<Pair<String, String>, List<String>> levels = factors.get(label);
      for (Pair<String, String> level : levels.keySet()) {
        List<String> ids = levels.get(level);
        List<String> translatedIDs = new ArrayList<>();
        for (String id : ids) {
          if (idMap.containsKey(id)) {
            translatedIDs.add(idMap.get(id));
          } else {
            translatedIDs.add(id);
          }
        }
        levels.put(level, translatedIDs);
      }
    }
  }

  /**
   * old function to convert intermediary experimental factors and properties found in a property
   * map to usable objects. used by graph creator
   * 
   * @param metadata
   */
  public static void fixProps(Map<String, Object> metadata) {
    LociParser lp = new LociParser();
    List<Property> factors = new ArrayList<Property>();

    if (metadata.get("Factors") != null) {
      @SuppressWarnings("unchecked")
      List<Property> properties = (List<Property>) metadata.get("Factors");
      for (Property f : properties) {
        String factorLabel = factorNameForXML(f.getLabel());
        if (f.hasUnit())
          factors.add(new Property(factorLabel, f.getValue(), f.getUnit(), f.getType()));
        else
          factors.add(new Property(factorLabel, f.getValue(), f.getType()));
      }
      metadata.put("Q_PROPERTIES", factors);
      factors = new ArrayList<Property>();
    }
    metadata.remove("Factors");

    if (metadata.get("XML_FACTORS") != null) {
      String[] fStrings = SEMICOLON.split((String) metadata.get("XML_FACTORS"));
      for (String factor : fStrings) {
        if (factor.length() > 1) {
          String[] fields = COLON.split(factor);
          for (int i = 0; i < fields.length; i++)
            fields[i] = fields[i].trim();
          Matcher matcher = WHITESPACE.matcher(fields[0]);
          String lab = matcher.replaceAll("");
          String val = fields[1];
          if (fields.length > 2)
            factors.add(new Property(lab, val, Unit.valueOf(fields[2]), PropertyType.Factor));
          else
            factors.add(new Property(lab, val, PropertyType.Factor));
        }
      }
      metadata.put("Q_PROPERTIES", factors);
    }
    metadata.remove("XML_FACTORS");

    List<GeneLocus> loci = new ArrayList<GeneLocus>();
    if (metadata.get("XML_LOCI") != null) {
      String[] lStrings = ((String) metadata.get("XML_LOCI")).split(";");
      for (String locus : lStrings) {
        if (locus.length() > 1) {
          String[] fields = locus.split(":");
          for (int i = 0; i < fields.length; i++)
            fields[i] = fields[i].trim();
          String lab = fields[0];
          String[] alleles = fields[1].split("/");
          loci.add(new GeneLocus(lab, new ArrayList<String>(Arrays.asList(alleles))));
        }
      }
      try {
        metadata.put("Q_LOCI", lp.toString(lp.createXMLFromLoci(loci)));
      } catch (JAXBException e) {
        e.printStackTrace();
      }
    }
    metadata.remove("XML_LOCI");
  }

  public static List<Property> getPropsFromString(Map<String, Object> metadata) {
    List<Property> props = new ArrayList<Property>();
    if (metadata.get("XML_FACTORS") != null) {
      String[] fStrings = SEMICOLON.split((String) metadata.get("XML_FACTORS"));
      for (String factor : fStrings) {
        if (factor.length() > 1) {
          String[] fields = COLON.split(factor);
          for (int i = 0; i < fields.length; i++)
            fields[i] = fields[i].trim();
          Matcher matcher = WHITESPACE.matcher(fields[0]);
          String lab = matcher.replaceAll("");
          String val = fields[1];
          if (fields.length > 2)
            props.add(new Property(lab, val, Unit.valueOf(fields[2]), PropertyType.Factor));
          else
            props.add(new Property(lab, val, PropertyType.Factor));
        }
      }
    }
    return props;
  }

  /**
   * checks if a string matches the xml schema for properties factor labels, changes it otherwise
   * 
   * @param label A String to be used as factor label in the properties xml
   * @return the label if it matches, a similar, matching label, otherwise
   */
  public static String factorNameForXML(String label) {
    Pattern p = Pattern.compile("([a-z]+_?[a-z]*)+([a-z]|[0-9]*)");
    if (p.matcher(label).matches())
      return label;

    label = label.trim();
    label = label.replace(" ", "_");
    char first = label.charAt(0);
    if (Character.isDigit(first))
      label = label.replaceFirst(Character.toString(first), "factor_" + first);
    return label;
  }

  public static String removeUTF8BOM(String s) {
    if (s.startsWith(UTF8_BOM)) {
      s = s.substring(1);
    }
    return s;
  }

}
