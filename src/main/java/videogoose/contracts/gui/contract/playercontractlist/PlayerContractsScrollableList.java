package videogoose.contracts.gui.contract.playercontractlist;

import api.common.GameClient;
import api.common.GameCommon;
import org.schema.common.util.CompareTools;
import org.schema.common.util.StringTools;
import org.schema.game.server.data.PlayerNotFountException;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.*;
import org.schema.schine.graphicsengine.forms.gui.newgui.*;
import org.schema.schine.input.InputState;
import videogoose.contracts.data.DataManager;
import videogoose.contracts.data.contract.ContractData;
import videogoose.contracts.data.contract.ContractDataManager;
import videogoose.contracts.data.player.PlayerData;
import videogoose.contracts.data.player.PlayerDataManager;
import videogoose.contracts.manager.ConfigManager;
import videogoose.contracts.manager.GUIManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;
import java.util.Set;

public class PlayerContractsScrollableList extends ScrollableTableList<ContractData> implements GUIActiveInterface {

	private final GUIMainWindow window;

	public PlayerContractsScrollableList(InputState state, GUIMainWindow window, GUIElement content) {
		super(state, window.getWidth(), window.getHeight(), content);
		this.window = window;
	}

	@Override
	public void initColumns() {
		addColumn("Task", 20.0F, Comparator.comparing(ContractData::getName));
		addColumn("Type", 7.0F, Comparator.comparing(ContractData::getContractType));
		addColumn("Contractor", 7.0F, (o1, o2) -> {
			String name1 = GameCommon.getGameState().getFactionManager().getFactionName(o1.getContractor().getIdFaction());
			String name2 = GameCommon.getGameState().getFactionManager().getFactionName(o2.getContractor().getIdFaction());
			return name1.compareTo(name2);
		});
		addColumn("Reward", 5.0F, (o1, o2) -> CompareTools.compare(o1.getReward(), o2.getReward()));
		addColumn("Time Remaining", 10.0F, (o1, o2) -> CompareTools.compare(
				o1.getTimeRemaining(GameClient.getClientPlayerState().getName()),
				o2.getTimeRemaining(GameClient.getClientPlayerState().getName())));

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
		String playerName = GameClient.getClientPlayerState().getName();
		ArrayList<ContractData> contracts = new ArrayList<>();
		for(ContractData contract : ContractDataManager.getInstance(false).getClientCache()) {
			if(contract.getClaimants().containsKey(playerName)) {
				contracts.add(contract);
			}
		}
		return contracts;
	}

	public GUIHorizontalButtonTablePane redrawButtonPane(ContractData contract, GUIAncor anchor) throws PlayerNotFountException {
		GUIHorizontalButtonTablePane buttonPane = new GUIHorizontalButtonTablePane(getState(), 2, 1, anchor);
		buttonPane.onInit();
		ContractDataManager dataManager = ContractDataManager.getInstance(false);
		PlayerData playerData = PlayerDataManager.getInstance(false).getClientOwnData();

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

		buttonPane.addButton(1, 0, "COMPLETE CONTRACT", GUIHorizontalArea.HButtonColor.GREEN, new GUICallback() {
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
						getState().getController().popupAlertTextMessage("You must meet the contract requirements!");
					}
				}
			}

			@Override
			public boolean isOccluded() {
				return !contract.canComplete(GameClient.getClientPlayerState());
			}
		}, new GUIActivationCallback() {
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
		for(ContractData contract : set) {
			try {
				GUITextOverlayTable nameTextElement;
				(nameTextElement = new GUITextOverlayTable(10, 10, getState())).setTextSimple(contract.getName());
				GUIClippedRow nameRowElement;
				(nameRowElement = new GUIClippedRow(getState())).attach(nameTextElement);

				GUITextOverlayTable contractTypeTextElement;
				(contractTypeTextElement = new GUITextOverlayTable(10, 10, getState())).setTextSimple(contract.getContractType().displayName.toUpperCase(Locale.ENGLISH));
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
						if(timeRemaining == 0) timeRemaining = ConfigManager.getContractTimerMax();
						String formatted = StringTools.formatRaceTime(timeRemaining);
						setTextSimple(formatted.substring(0, formatted.indexOf(".")));
						updateCacheForced();
						super.draw();
					}
				};
				long timeRemaining = ConfigManager.getContractTimerMax();
				String formatted = StringTools.formatRaceTime(timeRemaining);
				timeTextElement.setTextSimple(formatted.substring(0, formatted.indexOf(".")));

				PlayerContractListRow row = new PlayerContractListRow(getState(), contract, nameRowElement, contractTypeRowElement, contractorRowElement, rewardRowElement, timeTextElement);
				GUIAncor anchor = new GUIAncor(getState(), window.getWidth() - 107.0f, 28.0f) {
					@Override
					public void draw() {
						setWidth(window.getWidth() - 107.0f);
						super.draw();
					}
				};
				anchor.attach(redrawButtonPane(contract, anchor));
				row.expanded = new GUIElementList(getState());
				row.expanded.add(new GUIListElement(anchor, getState()));
				row.expanded.attach(anchor);
				row.onInit();
				guiElementList.add(row);
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
