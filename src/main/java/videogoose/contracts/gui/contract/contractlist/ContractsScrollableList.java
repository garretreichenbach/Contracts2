package videogoose.contracts.gui.contract.contractlist;

import api.common.GameClient;
import api.common.GameCommon;
import api.network.packets.PacketUtil;
import org.schema.common.util.CompareTools;
import org.schema.game.client.controller.PlayerOkCancelInput;
import org.schema.game.common.data.player.PlayerState;
import org.schema.schine.graphicsengine.core.GLFrame;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.*;
import org.schema.schine.graphicsengine.forms.gui.newgui.*;
import org.schema.schine.input.InputState;
import videogoose.contracts.data.DataManager;
import videogoose.contracts.data.contract.ContractData;
import videogoose.contracts.data.contract.ContractDataManager;
import videogoose.contracts.networking.AcceptContractPacket;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;
import java.util.Set;

public class ContractsScrollableList extends ScrollableTableList<ContractData> implements GUIActiveInterface {

	private final PlayerState player;
	private final GUIMainWindow window;

	public ContractsScrollableList(InputState state, GUIMainWindow window, GUIElement guiElement) {
		super(state, window.getWidth(), window.getHeight(), guiElement);
		this.window = window;
		player = GameClient.getClientPlayerState();
	}

	@Override
	public void initColumns() {
		addColumn("Task", 30.0f, Comparator.comparing(ContractData::getName));
		addColumn("Type", 5.0F, Comparator.comparing(ContractData::getContractType));
		addColumn("Contractor", 7.0F, (o1, o2) -> {
			String faction1 = GameCommon.getGameState().getFactionManager().getFactionName(o1.getContractor().getIdFaction());
			String faction2 = GameCommon.getGameState().getFactionManager().getFactionName(o2.getContractor().getIdFaction());
			return faction1.compareTo(faction2);
		});
		addColumn("Reward", 7.5F, (o1, o2) -> CompareTools.compare(o1.getReward(), o2.getReward()));

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
					case ESCORT:
						return contract.getContractType() == ContractData.ContractType.ESCORT;
				}
				return true;
			}

		}, new CreateGUIElementInterface<ContractData.ContractType>() {
			@Override
			public GUIElement create(ContractData.ContractType contractType) {
				GUIAncor anchor = new GUIAncor(getState(), 10.0F, 24.0F);
				GUITextOverlayTableDropDown dropDown;
				(dropDown = new GUITextOverlayTableDropDown(10, 10, getState())).setTextSimple(contractType.displayName.toUpperCase(Locale.ENGLISH));
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
		return new ArrayList<>(ContractDataManager.getInstance(false).getClientCache());
	}

	public GUIHorizontalButtonTablePane redrawButtonPane(ContractData contract, GUIAncor anchor) {
		GUIHorizontalButtonTablePane buttonPane;
		ContractDataManager dataManager = ContractDataManager.getInstance(false);
		if(player.getFactionId() == contract.getContractor().getIdFaction()) {
			buttonPane = new GUIHorizontalButtonTablePane(getState(), 1, 1, anchor);
			buttonPane.onInit();
			buttonPane.addButton(0, 0, "CANCEL CONTRACT", GUIHorizontalArea.HButtonColor.ORANGE, new GUICallback() {
				@Override
				public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
					if(mouseEvent.pressedLeftMouse()) {
						if(player.getFactionId() == contract.getContractor().getIdFaction()) {
							getState().getController().queueUIAudio("0022_action - buttons push small");
							PlayerOkCancelInput confirmBox = new PlayerOkCancelInput("ConfirmBox", getState(), "Confirm Cancellation", "Are you sure you wish to cancel this contract? You won't get a refund...") {
								@Override
								public void onDeactivate() {
								}

								@Override
								public void pressedOK() {
									getState().getController().queueUIAudio("0022_menu_ui - cancel");
									dataManager.sendPacket(contract, DataManager.REMOVE_DATA, true);
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
							getState().getController().popupAlertTextMessage("You cannot cancel this contract as you aren't the contractor!");
						}
					}
				}

				@Override
				public boolean isOccluded() {
					return contract.getContractor().getIdFaction() != player.getFactionId();
				}
			}, new GUIActivationCallback() {
				@Override
				public boolean isVisible(InputState inputState) {
					return true;
				}

				@Override
				public boolean isActive(InputState inputState) {
					return contract.getContractor().getIdFaction() == player.getFactionId();
				}
			});
		} else {
			buttonPane = new GUIHorizontalButtonTablePane(getState(), 1, 1, anchor);
			buttonPane.onInit();
			buttonPane.addButton(0, 0, "ACCEPT CONTRACT", GUIHorizontalArea.HButtonColor.GREEN, new GUICallback() {
				@Override
				public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
					if(mouseEvent.pressedLeftMouse()) {
						getState().getController().queueUIAudio("0022_action - buttons push small");
						PacketUtil.sendPacketToServer(new AcceptContractPacket(contract.getUUID()));
					}
				}

				@Override
				public boolean isOccluded() {
					return contract.getClaimants().containsKey(player.getName());
				}
			}, new GUIActivationCallback() {
				@Override
				public boolean isVisible(InputState inputState) {
					return true;
				}

				@Override
				public boolean isActive(InputState inputState) {
					return !contract.getClaimants().containsKey(player.getName());
				}
			});
		}
		return buttonPane;
	}

	@Override
	public void updateListEntries(GUIElementList guiElementList, Set<ContractData> set) {
		guiElementList.deleteObservers();
		guiElementList.addObserver(this);
		for(ContractData contract : set) {
			if(contract == null || contract.getContractType() == null) continue;
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

			ContractListRow contractListRow = new ContractListRow(getState(), contract, nameRowElement, contractTypeRowElement, contractorRowElement, rewardRowElement);
			GUIAncor anchor = new GUIAncor(getState(), window.getWidth() - 107.0f, 28.0f) {
				@Override
				public void draw() {
					setWidth(window.getWidth() - 107.0f);
					super.draw();
				}
			};
			GUIHorizontalButtonTablePane buttonTablePane = redrawButtonPane(contract, anchor);
			if(buttonTablePane != null) anchor.attach(buttonTablePane);
			contractListRow.expanded = new GUIElementList(getState());
			contractListRow.expanded.add(new GUIListElement(anchor, getState()));
			contractListRow.expanded.attach(anchor);
			contractListRow.onInit();
			guiElementList.add(contractListRow);
		}
		guiElementList.updateDim();
	}

	public class ContractListRow extends ScrollableTableList<ContractData>.Row {

		public ContractListRow(InputState inputState, ContractData contract, GUIElement... guiElements) {
			super(inputState, contract, guiElements);
			highlightSelect = true;
			highlightSelectSimple = true;
			setAllwaysOneSelected(true);
		}
	}
}
