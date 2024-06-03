package thederpgamer.contracts;

import api.listener.events.gui.MainWindowTabAddEvent;
import api.utils.gui.ModGUIHandler;
import thederpgamer.contracts.gui.contract.contractlist.ContractsTab;
import thederpgamer.contracts.gui.contract.playercontractlist.PlayerContractsControlManager;

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
    public PlayerContractsControlManager playerContractsControlManager;

    public static void initialize(Contracts mod) {
        instance = new GUIManager();
    }

    public void toggleContractsControlManager(boolean active) {
        if(playerContractsControlManager == null) {
            playerContractsControlManager = new PlayerContractsControlManager(null);
            ModGUIHandler.registerNewControlManager(null, playerContractsControlManager);
        }
        playerContractsControlManager.setActive(active);
    }

    public void createContractsShopTab(MainWindowTabAddEvent event) {
        if(event.getTitle().equals("SHOP") && contractsTab == null) {
            contractsTab = new ContractsTab(event.getWindow().getState(), event.getWindow());
            contractsTab.onInit();
            event.getWindow().getTabs().add(contractsTab);
        }
    }
}
