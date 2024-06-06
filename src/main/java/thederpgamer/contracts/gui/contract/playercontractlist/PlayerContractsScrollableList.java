package thederpgamer.contracts.gui.contract.playercontractlist;

import api.common.GameClient;
import api.common.GameCommon;
import api.utils.gui.SimplePopup;
import org.schema.common.util.CompareTools;
import org.schema.common.util.StringTools;
import org.schema.game.server.data.PlayerNotFountException;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.*;
import org.schema.schine.graphicsengine.forms.gui.newgui.*;
import org.schema.schine.input.InputState;
import thederpgamer.contracts.data.contract.Contract;
import thederpgamer.contracts.manager.GUIManager;
import thederpgamer.contracts.networking.client.ClientActionType;
import thederpgamer.contracts.networking.client.ClientDataManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Set;

public class PlayerContractsScrollableList extends ScrollableTableList<Contract> implements GUIActiveInterface {

	private final GUIElement window;

	public PlayerContractsScrollableList(InputState state, GUIMainWindow window, GUIElement content) {
		super(state, window.getWidth(), window.getHeight(), content);
		this.window = window;
	}

	@Override
	public void initColumns() {
		addColumn("Task", 20.0F, new Comparator<Contract>() {
			public int compare(Contract o1, Contract o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});

		addColumn("Type", 7.0F, new Comparator<Contract>() {
			public int compare(Contract o1, Contract o2) {
				return o1.getContractType().compareTo(o2.getContractType());
			}
		});

		addColumn("Contractor", 7.0F, new Comparator<Contract>() {
			public int compare(Contract o1, Contract o2) {
				String name1 = GameCommon.getGameState().getFactionManager().getFactionName(o1.getContractor().getIdFaction());
				String name2 = GameCommon.getGameState().getFactionManager().getFactionName(o2.getContractor().getIdFaction());
				return name1.compareTo(name2);
			}
		});

		addColumn("Reward", 5.0F, new Comparator<Contract>() {
			public int compare(Contract o1, Contract o2) {
				return CompareTools.compare(o1.getReward(), o2.getReward());
			}
		});

		addColumn("Time Remaining", 10.0F, new Comparator<Contract>() {
			public int compare(Contract o1, Contract o2) {
				return CompareTools.compare(o1.getTimeRemaining(GameClient.getClientPlayerState().getName()), o2.getTimeRemaining(GameClient.getClientPlayerState().getName()));
			}
		});

		addTextFilter(new GUIListFilterText<Contract>() {
			public boolean isOk(String s, Contract contract) {
				return contract.getName().toLowerCase().contains(s.toLowerCase());
			}
		}, ControllerElement.FilterRowStyle.LEFT);

		addDropdownFilter(new GUIListFilterDropdown<Contract, Contract.ContractType>(Contract.ContractType.values()) {
			public boolean isOk(Contract.ContractType contractType, Contract contract) {
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
	public ArrayList<Contract> getElementList() {
		return ClientDataManager.getClaimedContracts();
	}

	public GUIHorizontalButtonTablePane redrawButtonPane(final Contract contract, GUIAncor anchor) throws PlayerNotFountException {
		GUIHorizontalButtonTablePane buttonPane = new GUIHorizontalButtonTablePane(getState(), 2, 1, anchor);
		buttonPane.onInit();

		buttonPane.addButton(0, 0, "CANCEL CLAIM", GUIHorizontalArea.HButtonColor.ORANGE, new GUICallback() {
			@Override
			public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
				if(mouseEvent.pressedLeftMouse()) {
					getState().getController().queueUIAudio("0022_menu_ui - back");
					contract.getClaimants().remove(GameClient.getClientPlayerState().getName());
					ClientActionType.CANCEL_CLAIM.send(contract.getUID());
					if(GUIManager.getInstance().contractsTab != null)
						GUIManager.getInstance().contractsTab.flagForRefresh();
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

		GUICallback completeContractCallback = new GUICallback() {
			@Override
			public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
				if(mouseEvent.pressedLeftMouse()) {
					if(contract.canComplete(GameClient.getClientPlayerState())) {
						getState().getController().queueUIAudio("0022_menu_ui - enter");
						ClientActionType.COMPLETE_CONTRACT.send(contract.getUID());
						if(GUIManager.getInstance().contractsTab != null)
							GUIManager.getInstance().contractsTab.flagForRefresh();
						flagDirty();
					} else
						(new SimplePopup(getState(), "Cannot Complete Contract", "You must have the contract items in your inventory!")).activate();
				}
			}

			@Override
			public boolean isOccluded() {
				return !contract.canComplete(GameClient.getClientPlayerState());
			}
		};

		buttonPane.addButton(1, 0, "COMPLETE CONTRACT", GUIHorizontalArea.HButtonColor.GREEN, completeContractCallback, new GUIActivationCallback() {
			@Override
			public boolean isVisible(InputState inputState) {
				return true;
			}

			@Override
			public boolean isActive(InputState inputState) {
				return contract.canComplete(GameClient.getClientPlayerState());
			}
		});

		return buttonPane;
	}

	@Override
	public void updateListEntries(GUIElementList guiElementList, final Set<Contract> set) {
		guiElementList.deleteObservers();
		guiElementList.addObserver(this);
		for(final Contract contract : set) {
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
				String contractorName = GameCommon.getGameState().getFactionManager().getFactionName(contract.getContractor().getIdFaction());
				(contractorTextElement = new GUITextOverlayTable(10, 10, getState())).setTextSimple(contractorName);
				GUIClippedRow contractorRowElement;
				(contractorRowElement = new GUIClippedRow(getState())).attach(contractorTextElement);

				GUITextOverlayTable rewardTextElement;
				(rewardTextElement = new GUITextOverlayTable(10, 10, getState())).setTextSimple(String.valueOf(contract.getReward()));
				GUIClippedRow rewardRowElement;
				(rewardRowElement = new GUIClippedRow(getState())).attach(rewardTextElement);

				GUITextOverlayTable timeTextElement = new GUITextOverlayTable(10, 10, getState()) {
					@Override
					public void draw() {
						long timeRemaining = contract.getTimeRemaining(GameClient.getClientPlayerState().getName());
						setTextSimple(StringTools.formatRaceTime(timeRemaining));
						updateCacheForced();
						super.draw();
					}
				};
				long timeRemaining = contract.getTimeRemaining(GameClient.getClientPlayerState().getName());
				timeTextElement.setTextSimple(StringTools.formatRaceTime(timeRemaining));

				PlayerContractListRow playerContractListRow = new PlayerContractListRow(getState(), contract, nameRowElement, contractTypeRowElement, contractorRowElement, rewardRowElement, timeTextElement);
				GUIAncor anchor = new GUIAncor(getState(), window.getWidth() - 107.0f, 28.0f) {
					@Override
					public void draw() {
						setWidth(window.getWidth() - 107.0f);
						super.draw();
					}
				};
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

	public class PlayerContractListRow extends ScrollableTableList<Contract>.Row {

		public PlayerContractListRow(InputState inputState, Contract contract, GUIElement... guiElements) {
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