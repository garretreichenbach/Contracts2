package thederpgamer.contracts.gui.contract.contractlist;

import api.common.GameClient;
import api.common.GameCommon;
import api.utils.gui.SimplePopup;
import org.schema.common.util.CompareTools;
import org.schema.game.client.controller.PlayerOkCancelInput;
import org.schema.game.common.data.player.PlayerState;
import org.schema.schine.graphicsengine.core.GLFrame;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.*;
import org.schema.schine.graphicsengine.forms.gui.newgui.*;
import org.schema.schine.input.InputState;
import thederpgamer.contracts.data.contract.ClientContractData;
import thederpgamer.contracts.data.contract.Contract;
import thederpgamer.contracts.data.player.PlayerData;
import thederpgamer.contracts.networking.client.ClientActionType;
import thederpgamer.contracts.networking.client.ClientDataManager;
import thederpgamer.contracts.networking.server.ServerDataManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Set;

public class ContractsScrollableList extends ScrollableTableList<ClientContractData> implements GUIActiveInterface {

    private final PlayerState player;
    private final float width;

    public ContractsScrollableList(InputState state, float width, float height, GUIElement guiElement) {
        super(state, width, height, guiElement);
        player = GameClient.getClientPlayerState();
        this.width = width;
    }

    @Override
    public void initColumns() {
        addColumn("Task", 15.0F, new Comparator<ClientContractData>() {
            public int compare(ClientContractData o1, ClientContractData o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });

        addColumn("Type", 7.0F, new Comparator<ClientContractData>() {
            public int compare(ClientContractData o1, ClientContractData o2) {
                return o1.getContractType().compareTo(o2.getContractType());
            }
        });

        addColumn("Contractor", 7.0F, new Comparator<ClientContractData>() {
            public int compare(ClientContractData o1, ClientContractData o2) {
                String faction1 = GameCommon.getGameState().getFactionManager().getFactionName(o1.getContractor());
                String faction2 = GameCommon.getGameState().getFactionManager().getFactionName(o2.getContractor());
                return faction1.compareTo(faction2);
            }
        });

        addColumn("Reward", 5.0F, new Comparator<ClientContractData>() {
            public int compare(ClientContractData o1, ClientContractData o2) {
                return CompareTools.compare(o1.getReward(), o2.getReward());
            }
        });

        addTextFilter(new GUIListFilterText<ClientContractData>() {
            public boolean isOk(String s, ClientContractData contract) {
                return contract.getName().toLowerCase().contains(s.toLowerCase());
            }
        }, ControllerElement.FilterRowStyle.LEFT);

        addDropdownFilter(new GUIListFilterDropdown<ClientContractData, Contract.ContractType>(Contract.ContractType.values()) {
            public boolean isOk(Contract.ContractType contractType, ClientContractData contract) {
                switch(contractType) {
                    case ALL:
                        return true;
                    case BOUNTY:
                        return contract.getContractType() == Contract.ContractType.BOUNTY;
                    case ITEMS:
                        return contract.getContractType() == Contract.ContractType.ITEMS;
                }
                return true;
            }

        }, new CreateGUIElementInterface<Contract.ContractType>() {
            @Override
            public GUIElement create(Contract.ContractType contractType) {
                GUIAncor anchor = new GUIAncor(getState(), 10.0F, 24.0F);
                GUITextOverlayTableDropDown dropDown;
                (dropDown = new GUITextOverlayTableDropDown(10, 10, getState())).setTextSimple(contractType.displayName);
                dropDown.setPos(4.0F, 4.0F, 0.0F);
                anchor.setUserPointer(contractType);
                anchor.attach(dropDown);
                return anchor;
            }

            @Override
            public GUIElement createNeutral() {
                return null;
            }
        }, ControllerElement.FilterRowStyle.RIGHT);

        activeSortColumnIndex = 0;
    }

    @Override
    public ArrayList<ClientContractData> getElementList() {
        return new ArrayList<>(ClientDataManager.getClientData().values());
    }

    public GUIHorizontalButtonTablePane redrawButtonPane(final ClientContractData contract, GUIAncor anchor) {
        GUIHorizontalButtonTablePane buttonPane = new GUIHorizontalButtonTablePane(getState(), 3, 1, anchor);
        buttonPane.onInit();
        final PlayerData playerData = ServerDataManager.getPlayerData(player);
        final ArrayList<Contract> playerContracts = ServerDataManager.getPlayerContracts(playerData);
        int x = 0;
        if(!playerContracts.contains(contract) && (contract.getContractor() != playerData.factionID || player.isAdmin())) {
            buttonPane.addButton(x, 0, "CLAIM CONTRACT", GUIHorizontalArea.HButtonColor.BLUE, new GUICallback() {
                @Override
                public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                    if(mouseEvent.pressedLeftMouse()) {
                        if(contract.canClaim()) {
                            if(playerContracts.size() >= 5) (new SimplePopup(getState(), "Cannot Claim Contract", "You have too many active contracts!")).activate();
                            else {
                                getState().getController().queueUIAudio("0022_menu_ui - enter");
                                ClientDataManager.claimContract(contract.getUID());
                            }
                        } else {
                            SimplePopup popup = new SimplePopup(getState(), "Cannot Claim Contract", "You can't claim this contract!");
                            popup.activate();
                        }
                    }
                }

                @Override
                public boolean isOccluded() {
                    return false;
                }
            }, new GUIActivationCallback() {
                @Override
                public boolean isVisible(InputState inputState) {
                    return true;
                }

                @Override
                public boolean isActive(InputState inputState) {
                    return true;
                }
            });
            x++;
        } else if(playerContracts.contains(contract)) {
            buttonPane.addButton(x, 0, "CANCEL CLAIM", GUIHorizontalArea.HButtonColor.ORANGE, new GUICallback() {
                @Override
                public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                    if(mouseEvent.pressedLeftMouse()) {
                        getState().getController().queueUIAudio("0022_menu_ui - back");
                        ClientActionType.CANCEL_CLAIM.send(contract.getUID());
                    }
                }

                @Override
                public boolean isOccluded() {
                    return false;
                }
            }, new GUIActivationCallback() {
                @Override
                public boolean isVisible(InputState inputState) {
                    return true;
                }

                @Override
                public boolean isActive(InputState inputState) {
                    return true;
                }
            });
            x++;

            GUICallback completeContractCallback = null;
            if(contract.getContractType() == Contract.ContractType.ITEMS) {
                completeContractCallback = new GUICallback() {
                    @Override
                    public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                        if(mouseEvent.pressedLeftMouse()) {
                            if(contract.canComplete()) {
                                getState().getController().queueUIAudio("0022_menu_ui - enter");
                                ClientActionType.COMPLETE_CONTRACT.send(contract.getUID());
                            } else {
                                getState().getController().queueUIAudio("0022_menu_ui - error 1");
                                (new SimplePopup(getState(), "Cannot Complete Contract", "You have not completed the contract requirements!")).activate();
                            }
                        }
                    }

                    @Override
                    public boolean isOccluded() {
                        return false;
                    }
                };
            }

            if(completeContractCallback != null) {
                buttonPane.addButton(x, 0, "COMPLETE CONTRACT", GUIHorizontalArea.HButtonColor.GREEN, completeContractCallback, new GUIActivationCallback() {
                    @Override
                    public boolean isVisible(InputState inputState) {
                        return true;
                    }

                    @Override
                    public boolean isActive(InputState inputState) {
                        return true;
                    }
                });
                x++;
            }

            if(contract.getContractor() == playerData.factionID || player.isAdmin()) {
                buttonPane.addButton(x, 0, "CANCEL CONTRACT", GUIHorizontalArea.HButtonColor.ORANGE, new GUICallback() {
                    @Override
                    public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                        if(mouseEvent.pressedLeftMouse()) {
                            if(player.getFactionId() == contract.getContractor() || player.isAdmin()) {
                                getState().getController().queueUIAudio("0022_menu_ui - enter");
                                PlayerOkCancelInput confirmBox = new PlayerOkCancelInput("ConfirmBox", getState(), "Confirm Cancellation", "Are you sure you wish to cancel this contract? You won't get a refund...") {
                                    @Override
                                    public void onDeactivate() {
                                    }

                                    @Override
                                    public void pressedOK() {
                                        getState().getController().queueUIAudio("0022_menu_ui - cancel");
                                        ClientActionType.CANCEL_CONTRACT.send(contract.getUID());
                                        deactivate();
                                    }
                                };
                                confirmBox.getInputPanel().onInit();
                                confirmBox.getInputPanel().background.setPos(470.0F, 35.0F, 0.0F);
                                confirmBox.getInputPanel().background.setWidth((GLFrame.getWidth() - 435));
                                confirmBox.getInputPanel().background.setHeight((GLFrame.getHeight() - 70));
                                confirmBox.activate();
                            } else {
                                getState().getController().queueUIAudio("0022_menu_ui - error 1");
                                (new SimplePopup(getState(), "Cannot Cancel Contract", "You cannot cancel this contract as you aren't the contractor!")).activate();
                            }
                        }
                    }

                    @Override
                    public boolean isOccluded() {
                        return false;
                    }
                }, new GUIActivationCallback() {
                    @Override
                    public boolean isVisible(InputState inputState) {
                        return true;
                    }

                    @Override
                    public boolean isActive(InputState inputState) {
                        return true;
                    }
                });
                x++;
            }
        }
        return buttonPane;
    }

    @Override
    public void updateListEntries(GUIElementList guiElementList, Set<ClientContractData> set) {
        guiElementList.deleteObservers();
        guiElementList.addObserver(this);
        for(ClientContractData contract : set) {
            GUITextOverlayTable nameTextElement;
            (nameTextElement = new GUITextOverlayTable(10, 10, getState())).setTextSimple(contract.getName());
            GUIClippedRow nameRowElement;
            (nameRowElement = new GUIClippedRow(getState())).attach(nameTextElement);

            GUITextOverlayTable contractTypeTextElement;
            (contractTypeTextElement = new GUITextOverlayTable(10, 10, getState())).setTextSimple(contract.getContractType().displayName);
            GUIClippedRow contractTypeRowElement;
            (contractTypeRowElement = new GUIClippedRow(getState())).attach(contractTypeTextElement);

            GUITextOverlayTable contractorTextElement;
            String contractorName = GameCommon.getGameState().getFactionManager().getFactionName(contract.getContractor());
            (contractorTextElement = new GUITextOverlayTable(10, 10, getState())).setTextSimple(contractorName);
            GUIClippedRow contractorRowElement;
            (contractorRowElement = new GUIClippedRow(getState())).attach(contractorTextElement);

            GUITextOverlayTable rewardTextElement;
            (rewardTextElement = new GUITextOverlayTable(10, 10, getState())).setTextSimple(String.valueOf(contract.getReward()));
            GUIClippedRow rewardRowElement;
            (rewardRowElement = new GUIClippedRow(getState())).attach(rewardTextElement);

            ContractListRow contractListRow = new ContractListRow(getState(), contract, nameRowElement, contractTypeRowElement, contractorRowElement, rewardRowElement);
            GUIAncor anchor = new GUIAncor(getState(), width - 49.0f, 28.0f);
            anchor.attach(redrawButtonPane(contract, anchor));
            contractListRow.expanded = new GUIElementList(getState());
            contractListRow.expanded.add(new GUIListElement(anchor, getState()));
            contractListRow.expanded.attach(anchor);
            contractListRow.onInit();
            guiElementList.add(contractListRow);
        }
        guiElementList.updateDim();
    }

    public class ContractListRow extends ScrollableTableList<ClientContractData>.Row {

        public ContractListRow(InputState inputState, ClientContractData contract, GUIElement... guiElements) {
            super(inputState, contract, guiElements);
            highlightSelect = true;
            highlightSelectSimple = true;
            setAllwaysOneSelected(true);
        }

        @Override
        public void clickedOnRow() {
            super.clickedOnRow();
            setSelectedRow(this);
            setChanged();
            notifyObservers();
        }
    }
}
