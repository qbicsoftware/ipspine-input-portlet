package life.qbic.ipspine.components;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.VerticalLayout;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import life.qbic.ipspine.model.JSONSOP;
import life.qbic.ipspine.model.SampleCodeComparator;
import life.qbic.portal.Styles;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ExperimentsComponent extends VerticalLayout {
  Logger logger = LogManager.getLogger(ExperimentsComponent.class);

  private final Table experiments;
  private final Table sampleTable;
  // private Map<String, Sample> samplesByCode;
  Map<String, String> taxMap;
  Map<String, String> tissueMap;
  private Map<Experiment, JSONSOP> experimentsToDesigns;

  public ExperimentsComponent(Map<String, String> taxMap, Map<String, String> tissueMap) {

    this.taxMap = taxMap;
    this.tissueMap = tissueMap;

    setSpacing(true);

    addComponent(new Label("Select existing experiments to add new measurements"));
    experiments = new Table("Experiments");

    experiments.setStyleName(Styles.tableTheme);
    experiments.addContainerProperty("Name", String.class, "");
    experiments.addContainerProperty("Description", String.class, "");
    experiments.addContainerProperty("Samples", Integer.class, 0);
    experiments.addContainerProperty("Registration Date", Date.class, null);
    //experiments.addContainerProperty("SOP", Link.class, null);

    addComponent(experiments);
    experiments.setVisible(false);
    experiments.setSelectable(true);

    sampleTable = new Table("Samples");

    sampleTable.setStyleName(Styles.tableTheme);
    sampleTable.addContainerProperty("Name", String.class, "");
    sampleTable.addContainerProperty("QBiC Identifier", String.class, "");
    sampleTable.addContainerProperty("Type", String.class, "");
    //sampleTable.addContainerProperty("Registration Date", Date.class, null);

    addComponent(sampleTable);
    sampleTable.setVisible(false);
    sampleTable.setSelectable(true);
    sampleTable.setMultiSelect(true);

    experiments.addValueChangeListener(new ValueChangeListener() {

      @Override
      public void valueChange(ValueChangeEvent event) {
        sampleTable.removeAllItems();
        Object selected = experiments.getValue();
        if (selected != null) {
          fillSampleTable((Experiment) selected);
        }
        sampleTable.setVisible(sampleTable.size() > 0);
        sampleTable.setPageLength(sampleTable.size() + 1);
      }
    });
  }

  private List<Sample> getMeasuredSamples(Experiment experiment) {
    List<Sample> res = new ArrayList<>();
    for (Sample s : experiment.getSamples()) {
      String type = getType(s);
      if (experimentsToDesigns.get(experiment).isMeasuredEntity(type)) {
        res.add(s);
      }
    }
    return res;
  }

  protected void fillSampleTable(Experiment experiment) {
    for (Sample s : getMeasuredSamples(experiment)) {
      String type = getType(s);
      String code = s.getCode();

      String name = s.getProperty("Q_SECONDARY_NAME");

      //Date date = s.getRegistrationDate();

      List<Object> row = new ArrayList<Object>();
      row.add(name);
      row.add(code);
      row.add(type);
      //row.add(date);

      sampleTable.addItem(row.toArray(new Object[row.size()]), s);
    }
  }

  public String getType(Sample s) {
    switch (s.getType().getCode()) {
      case "Q_BIOLOGICAL_ENTITY":
        String organism = s.getProperty("Q_NCBI_ORGANISM");
        if (taxMap.containsKey(organism)) {
          organism = taxMap.get(organism);
        }
        return organism;
      case "Q_BIOLOGICAL_SAMPLE":
        String tissue = s.getProperty("Q_PRIMARY_TISSUE");
        if (tissue.equals("CELL_CULTURE")) {
          return s.getProperty("Q_TISSUE_DETAILED");
        }
        if (tissueMap.containsKey(tissue)) {

          tissue = tissueMap.get(tissue);
        }
        return tissue;
      case "Q_TEST_SAMPLE":
        return s.getProperty("Q_SAMPLE_TYPE");
      default:
        return "";
    }
  }

  public void setExistingExperiments(Map<Experiment, JSONSOP> experimentsToSOP) {
    experiments.removeAllItems();
    this.experimentsToDesigns = experimentsToSOP;
    for (Experiment e : experimentsToSOP.keySet()) {

      String description = e.getProperties().get("Q_SECONDARY_NAME");
      Integer sampleSize = new Integer(getMeasuredSamples(e).size());
      Date date = e.getRegistrationDate();

      //String sopName = sop.getName();
      //Resource resource = sop.getResource();

      //Link download = new Link(null, resource);
      //download.setTargetName("_blank");
      //download.setIcon(FontAwesome.DOWNLOAD);

      List<Object> row = new ArrayList<Object>();
      row.add(experimentsToSOP.get(e).getName());
      row.add(description);
      row.add(sampleSize);
      row.add(date);
      //row.add(download);

      experiments.addItem(row.toArray(new Object[row.size()]), e);
    }
    experiments.setPageLength(experiments.size() + 1);

    logger.info(experiments.size());
    if (experiments.size() > 0) {
      experiments.setVisible(true);
    }
  }

  public Table getSampleTable() {
    return sampleTable;
  }

  public List<Sample> getSelectedSamplesSortedByCode() {
    List<Sample> samples = new ArrayList<>();
    samples.addAll((Collection<? extends Sample>) sampleTable.getValue());
    Collections.sort(samples, SampleCodeComparator.getInstance());
    return samples;
  }

}
