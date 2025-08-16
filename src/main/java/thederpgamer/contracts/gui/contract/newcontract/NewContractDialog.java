package thederpgamer.contracts.gui.contract.newcontract;

import api.common.GameClient;
import api.utils.gui.SimplePopup;
import org.schema.game.client.controller.PlayerInput;
import org.schema.game.client.data.GameClientState;
import org.schema.game.common.data.player.PlayerState;
import org.schema.schine.graphicsengine.core.GLFW;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.input.KeyEventInterface;
import org.schema.schine.input.KeyboardMappings;
import thederpgamer.contracts.data.contract.ContractData;

public class NewContractDialog extends PlayerInput {

    private final NewContractPanel panel;
    private ContractData.ContractType selectedType;

    public NewContractDialog(GameClientState gameClientState) {
        super(gameClientState);
        panel = new NewContractPanel(getState(), this);
        panel.onInit();
        selectedType = ContractData.ContractType.BOUNTY;
    }

    @Override
    public void onDeactivate() {
        panel.cleanUp();
    }

    @Override
    public void handleKeyEvent(KeyEventInterface event) {
        if(KeyboardMappings.getEventKeyState(event, getState())) {
            if(KeyboardMappings.getEventKeyRaw(event) == GLFW.GLFW_KEY_ESCAPE) deactivate();
        }
    }

    @Override
    public void handleMouseEvent(MouseEvent event) {
    }

    @Override
    public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
        if(mouseEvent.pressedLeftMouse()) {
            if(guiElement != null && guiElement.getUserPointer() != null) {
                PlayerState currentPlayer = GameClient.getClientPlayerState();
                if(guiElement.getUserPointer().equals("OK")) {
                    if(currentPlayer.getFactionId() == 0) {
                        (new SimplePopup(getState(), "Cannot Add Contract", "You must be in a faction to do this!")).activate();
                        return;
                    }
                    if(panel.getReward() <= 0) {
                        (new SimplePopup(getState(), "Cannot Add Bounty", "The reward must be above 0!")).activate();
                    } else if(currentPlayer.getCredits() < panel.getReward()) {
                        (new SimplePopup(getState(), "Cannot Add Bounty", "You do not have enough credits!")).activate();
                    } else {
                        switch(selectedType) {
                            case BOUNTY:
                                panel.createBountyContract();
                                deactivate();
                                break;
                            case ITEMS:
                                panel.createItemsContract();
                                deactivate();
                                break;
                            default:
                                throw new IllegalStateException("Unexpected value: " + selectedType);
                        }
                    }
                } else if(guiElement.getUserPointer().equals("CANCEL") || guiElement.getUserPointer().equals("X")) {
                    deactivate();
                } else if(guiElement.getUserPointer().equals("BOUNTY")) {
                    selectedType = ContractData.ContractType.BOUNTY;
                    panel.drawBountyPanel();
                } else if(guiElement.getUserPointer().equals("ITEMS")) {
                    selectedType = ContractData.ContractType.ITEMS;
                    panel.drawItemsPanel();
                }
            }
        }
    }

    @Override
    public NewContractPanel getInputPanel() {
        return panel;
    }
}
