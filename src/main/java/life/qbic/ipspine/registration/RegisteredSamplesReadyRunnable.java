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

import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import life.qbic.ipspine.control.MainController;

/**
 * Class implementing the Runnable interface so it can be run and trigger a response in the view
 * after the sample creation thread finishes
 * 
 * @author Andreas Friedrich
 *
 */
public class RegisteredSamplesReadyRunnable implements Runnable {
  
  private static final Logger logger = LogManager.getLogger(RegisteredSamplesReadyRunnable.class);
  private final List<RegisterableExperiment> experiments;
  private final String designName;

  private String error;
  private MainController controller;
  private boolean measurements = false;

  public RegisteredSamplesReadyRunnable(MainController controller,
      List<RegisterableExperiment> experiments, String designName, boolean measurements) {
    this.controller = controller;
    this.experiments = experiments;
    this.measurements = measurements;
    this.designName = designName;
  }

  @Override
  public void run() {
    List<String> experimentCodes = new ArrayList<>();
    logger.info("running");
    if (error != null && error.length() > 0) {
      logger.info("error case");
      controller.sampleRegistrationFailed(error, measurements);
    } else {
      logger.info("success case");
      if(!measurements) {
        for (RegisterableExperiment e : experiments) {
          if(e.getType().equals("Q_SAMPLE_EXTRACTION")) {
            experimentCodes.add(e.getCode());
          }
        }

      }
      controller.sampleRegistrationDone(measurements, designName, experimentCodes);
    }
  }

  public String getError() {
    return error;
  }

  public void setError(String error) {
    this.error = error;
  }
}
