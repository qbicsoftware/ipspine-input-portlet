package life.qbic.portal.portlet;

import java.util.List;

public class ExpressionExperiment {
  
  private List<String> genes;
  private List<String> samples;
  private double[][] expressionMatrix;
  private List<Double> foldChanges;
  private List<Double> pValues;

  public ExpressionExperiment(List<String> genes, List<String> samples,
      double[][] expression, List<Double> foldChanges, List<Double> pValues) {
    super();
    this.genes = genes;
    this.samples = samples;
    this.expressionMatrix = expression;
    this.foldChanges = foldChanges;
    this.pValues = pValues;
  }

  public List<String> getGenes() {
    return genes;
  }

  public List<Double> getPValues() {
    return pValues;
  }

  public List<Double> getFoldChanges() {
    return foldChanges;
  }

  public List<String> getSamples() {
    return samples;
  }

  public double[][] getExpressionMatrix() {
    return expressionMatrix;
  }

}
