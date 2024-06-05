package thederpgamer.contracts;

import api.listener.events.gui.MainWindowTabAddEvent;
import org.schema.schine.common.language.Lng;
import thederpgamer.contracts.gui.contract.contractlist.ContractsTab;

/**
 * [Description]
 *
 * @author Garret Reichenbach
 */
public class GUIManager {

    private static GUIManager instance;

    public static GUIManager getInstance() {
        return instance;
    }

    public ContractsTab contractsTab;

    public static void initialize() {
        instance = new GUIManager();
    }

    public void createContractsShopTab(MainWindowTabAddEvent event) {
        if(event.getTitle().equals(Lng.str("SHOP")) && contractsTab == null) {
            contractsTab = new ContractsTab(event.getWindow());
            contractsTab.onInit();
            event.getWindow().getTabs().add(contractsTab);
        }
    }
}
