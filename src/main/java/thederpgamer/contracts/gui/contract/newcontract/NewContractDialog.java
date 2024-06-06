package thederpgamer.contracts.gui.contract.newcontract;

import api.common.GameClient;
import api.utils.game.inventory.ItemStack;
import api.utils.gui.SimplePopup;
import org.schema.game.client.controller.PlayerInput;
import org.schema.game.client.data.GameClientState;
import org.schema.game.common.data.player.PlayerState;
import org.schema.schine.graphicsengine.core.GLFW;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.input.KeyEventInterface;
import org.schema.schine.input.KeyboardMappings;
import thederpgamer.contracts.data.contract.BountyContract;
import thederpgamer.contracts.data.contract.Contract;
import thederpgamer.contracts.data.contract.ItemsContract;
import thederpgamer.contracts.data.player.PlayerData;
import thederpgamer.contracts.networking.client.ClientActionType;
import thederpgamer.contracts.networking.server.ServerDataManager;

public class NewContractDialog extends PlayerInput {

    private final NewContractPanel panel;
    private Contract.ContractType selectedType;

    public NewContractDialog(GameClientState gameClientState) {
        super(gameClientState);
        panel = new NewContractPanel(getState(), this);
        panel.onInit();
        selectedType = Contract.ContractType.BOUNTY;
    }

    @Override
    public void onDeactivate() {

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
                                String name = panel.getName();
                                int bountyAmount = panel.getReward();
                                PlayerData playerData = ServerDataManager.getPlayerData(name);
                                PlayerData currentPlayerData = ServerDataManager.getPlayerData(currentPlayer.getName());
                                if(playerData == null) {
                                    (new SimplePopup(getState(), "Cannot Add Bounty", "Player " + name + " does not exist!")).activate();
                                } else {
                                    if(currentPlayer.getName().equals(name) && !currentPlayer.isAdmin()) {
                                        (new SimplePopup(getState(), "Cannot Add Bounty", "You can't put a bounty on yourself!")).activate();
                                    } else if(currentPlayer.getFactionId() == playerData.factionID && !currentPlayer.isAdmin()) {
                                        (new SimplePopup(getState(), "Cannot Add Bounty", "You can't put a bounty on a member of your own faction!")).activate();
                                    } else if(ServerDataManager.getFactionAllies(currentPlayerData.factionID).contains(playerData.factionID) && !currentPlayer.isAdmin()) {
                                        (new SimplePopup(getState(), "Cannot Add Bounty", "You can't put a bounty on a member of an allied faction!")).activate();
                                    } else {
                                        BountyContract contract = new BountyContract(currentPlayer.getFactionId(), "Kill " + name, bountyAmount, playerData.name);
                                        ClientActionType.CREATE_CONTRACT.send(contract);
                                        currentPlayer.setCredits(currentPlayer.getCredits() - contract.getReward());
                                        deactivate();
                                    }
                                }
                                break;
                            case ITEMS:
                                int count = panel.getCount();
                                if(count <= 0) {
                                    (new SimplePopup(getState(), "Cannot Add Contract", "The amount must be above 0!")).activate();
                                } else {
                                    ItemStack itemStack = new ItemStack(panel.getSelectedBlockType().id, count);
                                    ItemsContract contract = new ItemsContract(currentPlayer.getFactionId(), "Obtain x" + count + " " + itemStack.getElementInfo().getName(), panel.getReward(), itemStack);
                                    ClientActionType.CREATE_CONTRACT.send(contract);
                                    currentPlayer.setCredits(currentPlayer.getCredits() - contract.getReward());
                                    deactivate();
                                }
                                break;
                            default:
                                throw new IllegalStateException("Unexpected value: " + selectedType);
                        }
                    }
                } else if(guiElement.getUserPointer().equals("CANCEL") || guiElement.getUserPointer().equals("X")) {
                    deactivate();
                } else if(guiElement.getUserPointer().equals("BOUNTY")) {
                    selectedType = Contract.ContractType.BOUNTY;
                    panel.drawBountyPanel();
                } else if(guiElement.getUserPointer().equals("ITEMS")) {
                    selectedType = Contract.ContractType.ITEMS;
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
