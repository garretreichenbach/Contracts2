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
import thederpgamer.contracts.data.contract.ActiveContractRunnable;
import thederpgamer.contracts.manager.ConfigManager;
import thederpgamer.contracts.data.contract.Contract;
import thederpgamer.contracts.networking.client.ClientActionType;
import thederpgamer.contracts.networking.client.ClientDataManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Set;

public class ContractsScrollableList extends ScrollableTableList<Contract> implements GUIActiveInterface {

	private final PlayerState player;
	private final GUIMainWindow window;

	public ContractsScrollableList(InputState state, GUIMainWindow window, GUIElement guiElement) {
		super(state, window.getWidth(), window.getHeight(), guiElement);
		this.window = window;
		player = GameClient.getClientPlayerState();
	}

	@Override
	public void initColumns() {
		addColumn("Task", 15.0F, new Comparator<Contract>() {
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
				String faction1 = GameCommon.getGameState().getFactionManager().getFactionName(o1.getContractor().getIdFaction());
				String faction2 = GameCommon.getGameState().getFactionManager().getFactionName(o2.getContractor().getIdFaction());
				return faction1.compareTo(faction2);
			}
		});

		addColumn("Reward", 5.0F, new Comparator<Contract>() {
			public int compare(Contract o1, Contract o2) {
				return CompareTools.compare(o1.getReward(), o2.getReward());
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
		ArrayList<Contract> list = new ArrayList<>();
		for(Contract contract : ClientDataManager.getClientData().values()) {
			if(contract != null) list.add(contract);
		}
		return list;
	}

	public GUIHorizontalButtonTablePane redrawButtonPane(final Contract contract, GUIAncor anchor) {
		GUIHorizontalButtonTablePane buttonPane;
		if(player.getFactionId() == contract.getContractor().getIdFaction()) {
			buttonPane = new GUIHorizontalButtonTablePane(getState(), 1, 1, anchor);
			buttonPane.onInit();
			buttonPane.addButton(0, 0, "CANCEL CONTRACT", GUIHorizontalArea.HButtonColor.ORANGE, new GUICallback() {
				@Override
				public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
					if(mouseEvent.pressedLeftMouse()) {
						if(player.getFactionId() == contract.getContractor().getIdFaction()) {
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
			buttonPane = new GUIHorizontalButtonTablePane(getState(), 2, 1, anchor);
			buttonPane.onInit();
			if(contract.canClaim(player)) {
				buttonPane.addButton(0, 0, "CLAIM CONTRACT", GUIHorizontalArea.HButtonColor.BLUE, new GUICallback() {
					@Override
					public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
						if(mouseEvent.pressedLeftMouse()) {
							if(contract.canClaim(player)) {
								if(ClientDataManager.getClaimedContracts().size() >= ConfigManager.getMainConfig().getInt("client-max-active-contracts")) {
									(new SimplePopup(getState(), "Cannot Claim Contract", "You have too many active contracts!")).activate();
								} else {
									getState().getController().queueUIAudio("0022_menu_ui - enter");
									if(contract instanceof ActiveContractRunnable && !((ActiveContractRunnable) contract).canStartRunner(player)) {
										(new SimplePopup(getState(), "Cannot Claim Contract", "You must be in the correct sector to start this contract!")).activate();
									} else ClientDataManager.claimContract(contract.getUID());
								}
							} else {
								SimplePopup popup = new SimplePopup(getState(), "Cannot Claim Contract", "You can't claim this contract!");
								popup.activate();
							}
						}
					}

					@Override
					public boolean isOccluded() {
						return !contract.canClaim(player);
					}
				}, new GUIActivationCallback() {
					@Override
					public boolean isVisible(InputState inputState) {
						return true;
					}

					@Override
					public boolean isActive(InputState inputState) {
						return contract.canClaim(player);
					}
				});

				buttonPane.addButton(1, 0, "COMPLETE CONTRACT", GUIHorizontalArea.HButtonColor.GREEN, new GUICallback() {
					@Override
					public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
						if(mouseEvent.pressedLeftMouse()) {
							if(contract.canComplete(player)) {
								getState().getController().queueUIAudio("0022_menu_ui - enter");
								ClientActionType.COMPLETE_CONTRACT.send(contract.getUID());
							} else {
								SimplePopup popup = new SimplePopup(getState(), "Cannot Complete Contract", "You can't complete this contract!");
								popup.activate();
							}
						}
					}

					@Override
					public boolean isOccluded() {
						return !contract.canComplete(player);
					}
				}, new GUIActivationCallback() {
					@Override
					public boolean isVisible(InputState inputState) {
						return true;
					}

					@Override
					public boolean isActive(InputState inputState) {
						return contract.canComplete(player);
					}
				});
			} else if(contract.getClaimants().containsKey(player.getName())) {
				buttonPane.addButton(0, 0, "CANCEL CLAIM", GUIHorizontalArea.HButtonColor.ORANGE, new GUICallback() {
					@Override
					public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
						if(mouseEvent.pressedLeftMouse()) {
							getState().getController().queueUIAudio("0022_menu_ui - back");
							contract.getClaimants().remove(player.getName());
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

				buttonPane.addButton(1, 0, "COMPLETE CONTRACT", GUIHorizontalArea.HButtonColor.GREEN, new GUICallback() {
					@Override
					public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
						if(mouseEvent.pressedLeftMouse()) {
							if(contract.canComplete(player)) {
								getState().getController().queueUIAudio("0022_menu_ui - enter");
								ClientActionType.COMPLETE_CONTRACT.send(contract.getUID());
								flagDirty();
							} else {
								SimplePopup popup = new SimplePopup(getState(), "Cannot Complete Contract", "You can't complete this contract!");
								popup.activate();
							}
						}
					}

					@Override
					public boolean isOccluded() {
						return !contract.canComplete(player);
					}
				}, new GUIActivationCallback() {
					@Override
					public boolean isVisible(InputState inputState) {
						return true;
					}

					@Override
					public boolean isActive(InputState inputState) {
						return contract.canComplete(player);
					}
				});
			}
		}
		return buttonPane;
	}

	@Override
	public void updateListEntries(GUIElementList guiElementList, Set<Contract> set) {
		guiElementList.deleteObservers();
		guiElementList.addObserver(this);
		for(Contract contract : set) {
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

	public class ContractListRow extends ScrollableTableList<Contract>.Row {

		public ContractListRow(InputState inputState, Contract contract, GUIElement... guiElements) {
			super(inputState, contract, guiElements);
			highlightSelect = true;
			highlightSelectSimple = true;
			setAllwaysOneSelected(true);
		}
	}
}
