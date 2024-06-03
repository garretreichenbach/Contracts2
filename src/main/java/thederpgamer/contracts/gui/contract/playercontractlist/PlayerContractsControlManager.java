package thederpgamer.contracts.gui.contract.playercontractlist;

import api.utils.gui.GUIControlManager;
import org.schema.game.client.data.GameClientState;

/**
 * PlayerContractsControlManager.java
 * <Description>
 *
 * @author TheDerpGamer
 * @since 03/17/2021
 */
public class PlayerContractsControlManager extends GUIControlManager {

    public PlayerContractsControlManager(GameClientState clientState) {
        super(clientState);
    }

    @Override
    public PlayerContractsPanel createMenuPanel() {
        return new PlayerContractsPanel(getState());
    }
}
