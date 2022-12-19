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
package life.qbic.ipspine.view;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import life.qbic.datamodel.persons.OpenbisSpaceUserRole;
import life.qbic.openbis.openbisclient.IOpenBisClient;
import life.qbic.ipspine.model.SamplePrepSOP;
import life.qbic.ipspine.model.Vocabularies;
import life.qbic.ipspine.registration.IOpenbisCreationController;
import life.qbic.portal.Styles;
import life.qbic.portal.Styles.NotificationType;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.validator.RegexpValidator;
import com.vaadin.server.ExternalResource;
import com.vaadin.ui.Button;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Upload;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Link;
import com.vaadin.ui.themes.ValoTheme;

public class IpspineTestView extends VerticalLayout {

  /**
   * 
   */
  private static final long serialVersionUID = -1713715806593305379L;

  private Logger logger = LogManager.getLogger(IpspineTestView.class);

  public IpspineTestView(IOpenBisClient openbis, Vocabularies vocabularies, IOpenbisCreationController creationController, String user) {

    setSpacing(true);
    setMargin(true);

    Upload upload = new Upload();
    upload.setButtonCaption("Upload sample table");
    upload.setVisible(false);

    String testlink = "http://just-a-testabcd.com/";

    SamplePrepSOP s11 = new SamplePrepSOP("D1.1 - Blood sampling",
        "Blood Sampling of human patients.", new ArrayList<>(Arrays.asList("Homo sapiens")),
        new ArrayList<>(Arrays.asList("Whole blood")), "", testlink);
    SamplePrepSOP s12 = new SamplePrepSOP("D1.1 - Bone marrow sampling",
        "Bone Marrow Sampling of human patients.", new ArrayList<>(Arrays.asList("Homo sapiens")),
        new ArrayList<>(Arrays.asList("Bone marrow")), "", testlink);
    SamplePrepSOP s13 = new SamplePrepSOP(
        "D1.1 - Isolation and Expansion of Nucleus Pulposus cells (NP)",
        "Isolation and expansion of Tie2+ cells from human discs for the generation of iPS cell lines",
        new ArrayList<>(Arrays.asList("Homo sapiens")),
        new ArrayList<>(Arrays.asList("Intervertebral Disc")), "Tie2+ cells", testlink);
    SamplePrepSOP s3 = new SamplePrepSOP("D3.1 - NP cell extraction and expansion",
        "Extraction of nucleus pulpous cells from human discs, expansion in monolayer culture and re-differentiation within alginate bead culture.",
        new ArrayList<>(Arrays.asList("Homo sapiens")),
        new ArrayList<>(Arrays.asList("Intervertebral Disc")), "NP cells", testlink);

    Map<String, SamplePrepSOP> sopMap = new HashMap<>();
    sopMap.put(s11.getName(), s11);
    sopMap.put(s12.getName(), s12);
    sopMap.put(s13.getName(), s13);
    sopMap.put(s3.getName(), s3);

    ComboBox sopBox = new ComboBox("Sample preparation");
    sopBox.addItem(s11.getName());
    sopBox.addItem(s12.getName());
    sopBox.addItem(s13.getName());
    sopBox.addItem(s3.getName());

    HorizontalLayout infoBox = new HorizontalLayout();
    infoBox.setSpacing(true);


    TextArea description = new TextArea("Description");

    TextField species = new TextField("Source organism");

    TextField tissue = new TextField("Source tissue");

    TextField celltype = new TextField("Cell type");

    TextField extract = new TextField("Measured sample type");


    ComboBox techBox = new ComboBox("Omics technology");

    Map<String, String> techMap = new HashMap<>();
    techMap.put("DigiWest", "Proteins");
    techMap.put("DNA Sequencing", "DNA");
    techMap.put("Proteomics Mass Spectrometry", "Proteins/Peptides");
    techMap.put("Metabolomics Mass Spectrometry", "Small molecules");
    techMap.put("Glycomics", "Glyproteins");
    techMap.put("RNA Sequencing", "RNA");
    List<String> techniques = new ArrayList<>(techMap.keySet());
    Collections.sort(techniques);
    techBox.addItems(techniques);

    sopBox.addValueChangeListener(new ValueChangeListener() {

      @Override
      public void valueChange(ValueChangeEvent event) {
        Object val = sopBox.getValue();
        if (val != null) {
          SamplePrepSOP sop = sopMap.get(val);
          description.setEnabled(true);
          description.setValue(sop.getDescription());
          description.setEnabled(false);
          species.setValue(sop.getSpecies().get(0));
          tissue.setValue(sop.getTissues().get(0));
          celltype.setValue(sop.getAnalyte());

          infoBox.removeAllComponents();
          infoBox.addComponent(description);
          Link link = new Link("Read SOP", new ExternalResource(sop.getSOPUrl()));
          infoBox.addComponent(link);

          if (!techBox.isEmpty()) {
            upload.setVisible(true);
          }
        }
      }
    });

    techBox.addValueChangeListener(new ValueChangeListener() {

      @Override
      public void valueChange(ValueChangeEvent event) {
        Object val = techBox.getValue();
        if (val != null) {
          String tech = techMap.get(val);
          extract.setValue(tech);

          if (!sopBox.isEmpty()) {
            upload.setVisible(true);
          }
        }
      }
    });

    String width = "300";
    sopBox.setWidth(width);
    description.setWidth(width);
    species.setWidth(width);
    tissue.setWidth(width);
    celltype.setWidth(width);
    extract.setWidth(width);
    techBox.setWidth(width);

    addComponent(sopBox);
    addComponent(infoBox);
    addComponent(techBox);
    addComponent(species);
    addComponent(tissue);
    addComponent(celltype);
    addComponent(extract);


    addComponent(upload);
  }
  
  

}
