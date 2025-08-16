package thederpgamer.contracts.gui.contract.playercontractlist;

import api.common.GameClient;
import api.common.GameCommon;
import org.schema.common.util.CompareTools;
import org.schema.common.util.StringTools;
import org.schema.game.server.data.PlayerNotFountException;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.*;
import org.schema.schine.graphicsengine.forms.gui.newgui.*;
import org.schema.schine.input.InputState;
import thederpgamer.contracts.data.DataManager;
import thederpgamer.contracts.data.contract.ContractData;
import thederpgamer.contracts.data.contract.ContractDataManager;
import thederpgamer.contracts.data.player.PlayerData;
import thederpgamer.contracts.data.player.PlayerDataManager;
import thederpgamer.contracts.manager.ConfigManager;
import thederpgamer.contracts.manager.GUIManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Set;

public class PlayerContractsScrollableList extends ScrollableTableList<ContractData> implements GUIActiveInterface {

	private final GUIElement window;

	public PlayerContractsScrollableList(InputState state, GUIMainWindow window, GUIElement content) {
		super(state, window.getWidth(), window.getHeight(), content);
		this.window = window;
	}

	@Override
	public void initColumns() {
		addColumn("Task", 20.0F, new Comparator<ContractData>() {
			public int compare(ContractData o1, ContractData o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});

		addColumn("Type", 7.0F, new Comparator<ContractData>() {
			public int compare(ContractData o1, ContractData o2) {
				return o1.getContractType().compareTo(o2.getContractType());
			}
		});

		addColumn("Contractor", 7.0F, new Comparator<ContractData>() {
			public int compare(ContractData o1, ContractData o2) {
				String name1 = GameCommon.getGameState().getFactionManager().getFactionName(o1.getContractor().getIdFaction());
				String name2 = GameCommon.getGameState().getFactionManager().getFactionName(o2.getContractor().getIdFaction());
				return name1.compareTo(name2);
			}
		});

		addColumn("Reward", 5.0F, new Comparator<ContractData>() {
			public int compare(ContractData o1, ContractData o2) {
				return CompareTools.compare(o1.getReward(), o2.getReward());
			}
		});

		addColumn("Time Remaining", 10.0F, new Comparator<ContractData>() {
			public int compare(ContractData o1, ContractData o2) {
				return CompareTools.compare(o1.getTimeRemaining(GameClient.getClientPlayerState().getName()), o2.getTimeRemaining(GameClient.getClientPlayerState().getName()));
			}
		});

		addTextFilter(new GUIListFilterText<ContractData>() {
			public boolean isOk(String s, ContractData contract) {
				return contract.getName().toLowerCase().contains(s.toLowerCase());
			}
		}, ControllerElement.FilterRowStyle.LEFT);

		addDropdownFilter(new GUIListFilterDropdown<ContractData, ContractData.ContractType>(ContractData.ContractType.values()) {
			public boolean isOk(ContractData.ContractType contractType, ContractData contract) {
				switch(contractType) {
					case ALL:
						return true;
					case BOUNTY:
						return contract.getContractType() == ContractData.ContractType.BOUNTY;
					case ITEMS:
						return contract.getContractType() == ContractData.ContractType.ITEMS;
				}
				return true;
			}

		}, new CreateGUIElementInterface<ContractData.ContractType>() {
			@Override
			public GUIElement create(ContractData.ContractType contractType) {
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
	public ArrayList<ContractData> getElementList() {
		ArrayList<ContractData> contracts = new ArrayList<>();
		for(ContractData contract : ContractDataManager.getInstance(false).getClientCache()) {
			if(contract.getClaimants().containsKey(GameClient.getClientPlayerState().getName())) {
				contracts.add(contract);
			}
		}
		return contracts;
	}

	public GUIHorizontalButtonTablePane redrawButtonPane(final ContractData contract, GUIAncor anchor) throws PlayerNotFountException {
		GUIHorizontalButtonTablePane buttonPane = new GUIHorizontalButtonTablePane(getState(), 2, 1, anchor);
		buttonPane.onInit();
		buttonPane.onInit();
		final ContractDataManager dataManager = ContractDataManager.getInstance(false);
		final PlayerData playerData = PlayerDataManager.getInstance(false).getClientOwnData();
		buttonPane.addButton(0, 0, "CANCEL CLAIM", GUIHorizontalArea.HButtonColor.ORANGE, new GUICallback() {
			@Override
			public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
				if(mouseEvent.pressedLeftMouse()) {
					getState().getController().queueUIAudio("0022_menu_ui - cancel");
					contract.getClaimants().remove(GameClient.getClientPlayerState().getName());
					dataManager.sendPacket(contract, DataManager.UPDATE_DATA, true);
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
						getState().getController().queueUIAudio("0022_action - buttons push small");
						ContractDataManager.completeContract(playerData, contract);
						if(GUIManager.getInstance().contractsTab != null) {
							GUIManager.getInstance().contractsTab.flagForRefresh();
						}
						flagDirty();
					} else {
						getState().getController().queueUIAudio("0022_menu_ui - error");
						getState().getController().popupAlertTextMessage("You must have the contract items in your inventory!");
					}
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
	public void updateListEntries(GUIElementList guiElementList, Set<ContractData> set) {
		guiElementList.deleteObservers();
		guiElementList.addObserver(this);
		for(final ContractData contract : set) {
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
						if(timeRemaining == 0)
							timeRemaining = ConfigManager.getMainConfig().getLong("contract-timer-max");
						String timeRemainingString = StringTools.formatRaceTime(timeRemaining);
						setTextSimple(timeRemainingString.substring(0, timeRemainingString.indexOf(".")));
						updateCacheForced();
						super.draw();
					}
				};
				long timeRemaining = ConfigManager.getMainConfig().getLong("contract-timer-max");
				String timeRemainingString = StringTools.formatRaceTime(timeRemaining);
				timeTextElement.setTextSimple(timeRemainingString.substring(0, timeRemainingString.indexOf(".")));

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

	public class PlayerContractListRow extends ScrollableTableList<ContractData>.Row {

		public PlayerContractListRow(InputState inputState, ContractData contract, GUIElement... guiElements) {
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