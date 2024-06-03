package thederpgamer.contracts.gui.contract.playercontractlist;

import api.common.GameClient;
import api.common.GameCommon;
import api.utils.gui.SimplePopup;
import org.schema.common.util.CompareTools;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.server.data.PlayerNotFountException;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.*;
import org.schema.schine.graphicsengine.forms.gui.newgui.*;
import org.schema.schine.input.InputState;
import thederpgamer.contracts.data.contract.ClientContractData;
import thederpgamer.contracts.data.contract.Contract;
import thederpgamer.contracts.networking.client.ClientActionType;
import thederpgamer.contracts.networking.client.ClientDataManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Set;

public class PlayerContractsScrollableList extends ScrollableTableList<ClientContractData> implements GUIActiveInterface {

    private final PlayerState player;
    private final float width;

    public PlayerContractsScrollableList(InputState state, float width, float height, GUIElement guiElement) {
        super(state, width, height, guiElement);
        this.width = width;
        player = GameClient.getClientPlayerState();
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
                String name1 = GameCommon.getGameState().getFactionManager().getFactionName(o1.getContractor());
                String name2 = GameCommon.getGameState().getFactionManager().getFactionName(o2.getContractor());
                return name1.compareTo(name2);
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
        return ClientDataManager.getClaimedContracts();
    }

    public GUIHorizontalButtonTablePane redrawButtonPane(final ClientContractData contract, GUIAncor anchor) throws PlayerNotFountException {
        GUIHorizontalButtonTablePane buttonPane = new GUIHorizontalButtonTablePane(getState(), 2, 1, anchor);
        buttonPane.onInit();

        buttonPane.addButton(0, 0, "CANCEL CLAIM", GUIHorizontalArea.HButtonColor.ORANGE, new GUICallback() {
            @Override
            public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                if(mouseEvent.pressedLeftMouse()) {
                    getState().getController().queueUIAudio("0022_menu_ui - back");
                    ClientActionType.CANCEL_CLAIM.send(contract.getUID());
                    flagDirty();
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

        GUICallback completeContractCallback = null;
        if(contract.getContractType() == Contract.ContractType.ITEMS) {
            completeContractCallback = new GUICallback() {
                @Override
                public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                    if(mouseEvent.pressedLeftMouse()) {
                        if(contract.canComplete()) {
                            getState().getController().queueUIAudio("0022_menu_ui - enter");
                            ClientActionType.COMPLETE_CONTRACT.send(contract.getUID());
                            flagDirty();
                        } else (new SimplePopup(getState(), "Cannot Complete Contract", "You must have the contract items in your inventory!")).activate();
                    }
                }

                @Override
                public boolean isOccluded() {
                    return false;
                }
            };
        }

        if(completeContractCallback != null) {
            buttonPane.addButton(1, 0, "COMPLETE CONTRACT", GUIHorizontalArea.HButtonColor.GREEN, completeContractCallback, new GUIActivationCallback() {
                @Override
                public boolean isVisible(InputState inputState) {
                    return true;
                }

                @Override
                public boolean isActive(InputState inputState) {
                    return contract.canComplete();
                }
            });
        }

        return buttonPane;
    }

    @Override
    public void updateListEntries(GUIElementList guiElementList, Set<ClientContractData> set) {
        guiElementList.deleteObservers();
        guiElementList.addObserver(this);
        for(ClientContractData contract : set) {
            try {
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

                PlayerContractListRow playerContractListRow = new PlayerContractListRow(getState(), contract, nameRowElement, contractTypeRowElement, contractorRowElement, rewardRowElement);
                GUIAncor anchor = new GUIAncor(getState(), width, 28.0f);
                anchor.attach(redrawButtonPane(contract, anchor));
                playerContractListRow.expanded = new GUIElementList(getState());
                playerContractListRow.expanded.add(new GUIListElement(anchor, getState()));
                playerContractListRow.expanded.attach(anchor);
                playerContractListRow.onInit();
                guiElementList.add(playerContractListRow);
            } catch(PlayerNotFountException e) {
                e.printStackTrace();
            }
        }
        guiElementList.updateDim();
    }

    public class PlayerContractListRow extends ScrollableTableList<ClientContractData>.Row {

        public PlayerContractListRow(InputState inputState, ClientContractData contract, GUIElement... guiElements) {
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