package life.qbic.portal.portlet;

import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Widgetset;
import com.vaadin.navigator.Navigator;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Label;
import com.vaadin.ui.Layout;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import java.nio.file.Path;
import java.nio.file.Paths;
import life.qbic.ipspine.control.MainController;
import life.qbic.ipspine.model.omics.AOmicsComponent;
import life.qbic.ipspine.model.omics.DigiWestComponent;
import life.qbic.ipspine.model.omics.GlycomicsComponent;
import life.qbic.ipspine.model.omics.ProteomicsComponent;
import life.qbic.ipspine.model.omics.SequencingComponent;
import life.qbic.ipspine.registration.OpenbisV3APIWrapper;
import life.qbic.ipspine.registration.OpenbisV3CreationController;
import life.qbic.ipspine.registration.OpenbisV3ReadController;
import life.qbic.ipspine.view.MainView;
import life.qbic.openbis.openbisclient.OpenBisClient;
import life.qbic.portal.utils.ConfigurationManager;
import life.qbic.portal.utils.ConfigurationManagerFactory;
import life.qbic.portal.utils.PortalUtils;
import life.qbic.ipspine.control.DBConfig;
import life.qbic.ipspine.control.DBManager;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Entry point for portlet ipspine-portlet. This class derives from {@link QBiCPortletUI}, which is
 * found in the {@code portal-utils-lib} library.
 * 
 * @see <a href=https://github.com/qbicsoftware/portal-utils-lib>portal-utils-lib</a>
 */
@Theme("mytheme")
@Widgetset("life.qbic.portal.portlet.AppWidgetSet")
public class iPSpinePortlet extends QBiCPortletUI {

  private static final long serialVersionUID = -3245533336166686560L;
  private static final Logger logger = LogManager.getLogger(iPSpinePortlet.class);

  @Override
  protected Layout getPortletContent(final VaadinRequest request) {
    logger.info("Generating content for {}", iPSpinePortlet.class);

    ConfigurationManager config = ConfigurationManagerFactory.getInstance();
    VerticalLayout layout = new VerticalLayout();

    boolean success = true;
    String user = "admin";
    if (PortalUtils.isLiferayPortlet()) {
      user = PortalUtils.getUser().getScreenName();
    }
    OpenBisClient openbisClient = null;
    OpenbisV3APIWrapper v3 = null;
    OpenbisV3CreationController openBIScreationController = null;
    try {
      logger.debug("trying to connect to openbis");
      final String baseURL = config.getDataSourceUrl();
      final String apiURL = baseURL + "/openbis/openbis";

      openbisClient =
          new OpenBisClient(config.getDataSourceUser(), config.getDataSourcePassword(), apiURL);
      openbisClient.login();

      v3 = new OpenbisV3APIWrapper(apiURL, config.getDataSourceUser(),
          config.getDataSourcePassword(), user);
      openBIScreationController = new OpenbisV3CreationController(openbisClient, user, v3);

    } catch (Exception e) {
      success = false;
      logger.error(
          "User \"" + user + "\" could not connect to openBIS and has been informed of this.");
      layout.addComponent(new Label(
          "Data Management System could not be reached. Please try again later or contact us."));
    }

    layout.setSpacing(true);
    layout.setMargin(true);

    //TODO use same database for both
    DBConfig mainDBConfig = new DBConfig(config.getMysqlHost(), config.getMysqlPort(),
        config.getMysqlDB(), config.getMysqlUser(), config.getMysqlPass());
    DBManager mainDB = new DBManager(mainDBConfig);

    Map<String, String> taxonomyMap =
        openbisClient.getVocabCodesAndLabelsForVocab("Q_NCBI_TAXONOMY");
    Map<String, String> tissueMap =
        openbisClient.getVocabCodesAndLabelsForVocab("Q_PRIMARY_TISSUES");

    Map<String, AOmicsComponent> typesToComponents = new HashMap<>();
    typesToComponents.put("NGS Sequencing", new SequencingComponent(
        openbisClient.getVocabCodesAndLabelsForVocab("Q_SEQUENCER_DEVICES")));
    typesToComponents.put("Proteomics",
        new ProteomicsComponent(openbisClient.getVocabCodesAndLabelsForVocab("Q_MS_DEVICES")));
    typesToComponents.put("DigiWest", new DigiWestComponent());
    typesToComponents.put("Glycomics",
        new GlycomicsComponent(openbisClient.getVocabCodesAndLabelsForVocab("Q_MS_DEVICES")));

    //TODO
    Path isaFolder = Paths.get(config.getISAConfigPath());
    Path parentFolder = isaFolder.getParent();
    String designsFolder = parentFolder.toString()+"/ipspine_designs";

    MainView mainView = new MainView(taxonomyMap, tissueMap, designsFolder, typesToComponents);
    new MainController(openbisClient, new OpenbisV3ReadController(v3),
        openBIScreationController, mainDB, mainView);
    
    final Navigator navigator = new Navigator(UI.getCurrent(), layout);

    navigator.addView(MainView.navigateToLabel, mainView);
    navigator.setErrorView(mainView);

    setNavigator(navigator);
    
    layout.addComponent(mainView);
    return layout;
  }

}
