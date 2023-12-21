/*******************************************************************************
 * QBiC Project Wizard enables users to create hierarchical experiments including different study
 * conditions using factorial design. Copyright (C) "2016" Andreas Friedrich
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package life.qbic.ipspine.registration;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import life.qbic.datamodel.identifiers.SampleCodeFunctions;

public class SampleCounter {

  private int entityID;
  private int expID;
  private String barcode;
  private String project;
  Logger logger = LogManager.getLogger(SampleCounter.class);
  private Set<String> knownAtypical = new HashSet<>();

  public SampleCounter(List<Sample> samples, List<Experiment> experiments, String project) {
    this(project);
    for (Sample s : samples)
      increment(s);
    for (Experiment e : experiments)
      increment(e);
  }

  public SampleCounter(String project) {
    entityID = 0;
    expID = 0;
    barcode = "";
    // barcodeID = 1;
    this.project = project;
    knownAtypical.add(project + "_INFO");
  }

  // TODO later updates (after initialization)
  public void increment(Experiment experiment) {
    String experimentID = experiment.getIdentifier().getIdentifier();

    try {
      String expNumString = experimentID.split(project + "E")[1];
      int expNum = Integer.parseInt(expNumString);
      if (expNum > expID)
        expID = expNum;
    } catch (Exception e) {
      if (!knownAtypical.contains(experimentID)) {
        knownAtypical.add(experimentID);
        logger.warn("While counting existing experiments in project " + project
            + " unfamiliar experiment identifier " + experimentID + " was found.");
      }
    }
  }

  // TODO later updates (after initialization)
  public void increment(Sample sample) {
    String code = sample.getCode();

    if (SampleCodeFunctions.isQbicBarcode(code)) {
      if (SampleCodeFunctions.compareSampleCodes(code, barcode) > 0)
        barcode = code;
    } else if (sample.getType().getCode().equals(("Q_BIOLOGICAL_ENTITY"))) {
      int num = Integer.parseInt(sample.getCode().split("-")[1]);
      if (num >= entityID)
        entityID = num;
    }
  }

  public String getNewExperiment() {
    expID++;
    return project + "E" + expID;
  }

  public String getNewEntity() {
    entityID++;
    return project + "ENTITY-" + Integer.toString(entityID);
  }

  public String getNewBarcode() {
    if (barcode == null || barcode.isEmpty()) {
      barcode = project + "001A";
      barcode = barcode + SampleCodeFunctions.checksum(barcode);
    }
    barcode = SampleCodeFunctions.incrementSampleCode(barcode);
    return barcode;
  }

}
