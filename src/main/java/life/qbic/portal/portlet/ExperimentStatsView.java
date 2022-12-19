package life.qbic.portal.portlet;

import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Table;

public class ExperimentStatsView {

  private Table stats;
  private ComboBox genes;
  private ComboBox factors;
  private Table geneInfo;
  private Table factorInfo;

  public ExperimentStatsView(ExpressionExperiment experiment) {
    stats = new Table("Statistics");
    
  }
}
