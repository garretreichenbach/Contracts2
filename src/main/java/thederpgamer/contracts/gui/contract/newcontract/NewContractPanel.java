package thederpgamer.contracts.gui.contract.newcontract;

import api.common.GameClient;
import api.common.GameCommon;
import api.utils.gui.SimplePopup;
import org.json.JSONObject;
import org.schema.game.client.data.GameClientState;
import org.schema.game.client.view.gui.GUIBlockSprite;
import org.schema.game.client.view.gui.GUIInputPanel;
import org.schema.game.client.view.gui.advanced.tools.*;
import org.schema.game.common.controller.BlockTypeSearchRunnableManager;
import org.schema.game.common.data.element.ElementInformation;
import org.schema.game.common.data.element.ElementKeyMap;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.faction.FactionRelation;
import org.schema.schine.common.TextCallback;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.core.settings.PrefixNotFoundException;
import org.schema.schine.graphicsengine.forms.font.FontLibrary;
import org.schema.schine.graphicsengine.forms.gui.*;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIActivatableTextBar;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIHorizontalArea;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIHorizontalButtonTablePane;
import org.schema.schine.input.InputState;
import thederpgamer.contracts.data.DataManager;
import thederpgamer.contracts.data.contract.BountyContract;
import thederpgamer.contracts.data.contract.ContractData;
import thederpgamer.contracts.data.contract.ContractDataManager;
import thederpgamer.contracts.data.player.PlayerData;
import thederpgamer.contracts.data.player.PlayerDataManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class NewContractPanel extends GUIInputPanel implements BlockTypeSearchRunnableManager.BlockTypeSearchProgressCallback {

	private final GUICallback guiCallback;
	private GUIActivatableTextBar rewardInput;
	private GUIActivatableTextBar nameInput;
	private GUIActivatableTextBar itemInput;
	private GUIAdvTextBar textBar;
	private ElementInformation selectedBlockType;
	private GUIAdvDropdown dropdown;
	private GUIAdvBlockDisplay display;
	private boolean textChanged;
	private String curText;
	private GUIHorizontalButtonTablePane buttonPane;

	public NewContractPanel(InputState inputState, GUICallback guiCallback) {
		super("NewContractPanel", inputState, 300, 300, guiCallback, "New Contract", "");
		this.guiCallback = guiCallback;
		curText = "";
	}

	public int getReward() {
		try {
			return Integer.parseInt(rewardInput.getText());
		} catch(Exception e) {
			return 0;
		}
	}

	public String getName() {
		return nameInput.getText();
	}

	public void drawItemsPanel() {
		curText = "";
		if(dropdown != null) {
			dropdown.cleanUp();
			getContent().detach(dropdown);
		}
		if(display != null) {
			display.cleanUp();
			getContent().detach(display);
		}
		if(textBar != null) {
			textBar.cleanUp();
			getContent().detach(textBar);
		}

		addDropdown(new DropdownResult() {
			private List<GUIElement> blockElements;

			@Override
			public DropdownCallback initCallback() {
				return new DropdownCallback() {
					@Override
					public void onChanged(Object value) {
						if(value instanceof ElementInformation) {
							selectedBlockType = (ElementInformation) value;
						}
					}
				};
			}

			@Override
			public String getToolTipText() {
				return "Select resource to request";
			}

			@Override
			public String getName() {
				return "Resource";
			}

			@Override
			public boolean needsListUpdate() {
				return textChanged;
			}

			@Override
			public Collection<? extends GUIElement> getDropdownElements(GUIElement guiElement) {
				blockElements = getProductionElements();
				return blockElements;
			}

			@Override
			public int getDropdownHeight() {
				return 26;
			}

			@Override
			public Object getDefault() {
				if(blockElements != null && !blockElements.isEmpty()) {
					return blockElements.get(0);
				} else {
					return null;
				}
			}

			@Override
			public void flagListNeedsUpdate(boolean flag) {
				textChanged = flag;
			}
		});

		addBlockDisplay(new BlockDisplayResult() {

			@Override
			public BlockSelectCallback initCallback() {
				return super.callback;
			}

			@Override
			public String getToolTipText() {
				return "Selected Block";
			}

			@Override
			public short getDefault() {
				return ElementKeyMap.RESS_CRYS_BASTYN;
			}

			@Override
			public short getCurrentValue() {
				if(selectedBlockType != null) {
					return selectedBlockType.getId();
				} else {
					return 0;
				}
			}

			@Override
			public float getIconScale() {
				return 0.5f;
			}

			@Override
			public float getWeight() {
				return 0.3f;
			}

		});


		addTextBar(new TextBarResult() {

			@Override
			public TextBarCallback initCallback() {
				return super.callback;
			}

			@Override
			public String getToolTipText() {
				return Lng.str("Enter blocks to search for in the drop down");
			}

			@Override
			public String getName() {
				return Lng.str("Search Blocks");
			}

			@Override
			public String onTextChanged(String text) {
				String t = text.trim();
				if(!t.equals(curText)) {
					curText = t;
					textChanged = true;
				}
				return text;
			}
		});

		if(itemInput == null) {
			itemInput = new GUIActivatableTextBar(getState(), FontLibrary.FontSize.MEDIUM, 10, 1, "COUNT", getContent(), new TextCallback() {
				@Override
				public String[] getCommandPrefixes() {
					return null;
				}

				@Override
				public String handleAutoComplete(String s, TextCallback textCallback, String s1) {
					return null;
				}

				@Override
				public void onFailedTextCheck(String s) {

				}

				@Override
				public void onTextEnter(String s, boolean b, boolean b1) {

				}

				@Override
				public void newLine() {

				}
			}, null);
			itemInput.onInit();
		}
		itemInput.setPos(0, 30, 0);
		getContent().attach(itemInput);
		itemInput.draw();
		if(nameInput != null) {
			nameInput.setText("");
			nameInput.deactivateBar();
			nameInput.cleanUp();
		}

		if(rewardInput == null) {
			rewardInput = new GUIActivatableTextBar(getState(), FontLibrary.FontSize.MEDIUM, 10, 1, "REWARD", getContent(), new TextCallback() {
				@Override
				public String[] getCommandPrefixes() {
					return null;
				}

				@Override
				public String handleAutoComplete(String s, TextCallback textCallback, String s1) throws PrefixNotFoundException {
					return null;
				}

				@Override
				public void onFailedTextCheck(String s) {

				}

				@Override
				public void onTextEnter(String s, boolean b, boolean b1) {

				}

				@Override
				public void newLine() {

				}
			}, null);
			rewardInput.onInit();
			rewardInput.setPos(0, 60, 0);
			getContent().attach(rewardInput);
		}

		rewardInput.setText("");
	}

	public void drawProductionPanel() {

		curText = "";
		if(dropdown != null) {
			dropdown.cleanUp();
			getContent().detach(dropdown);
		}
		if(display != null) {
			display.cleanUp();
			getContent().detach(display);
		}
		if(textBar != null) {
			textBar.cleanUp();
			getContent().detach(textBar);
		}

		addDropdown(new DropdownResult() {
			private List<GUIElement> blockElements;

			@Override
			public DropdownCallback initCallback() {
				return new DropdownCallback() {
					@Override
					public void onChanged(Object value) {
						if(value instanceof ElementInformation) {
							selectedBlockType = (ElementInformation) value;
						}
					}
				};
			}

			@Override
			public String getToolTipText() {
				return "Select resource to request";
			}

			@Override
			public String getName() {
				return "Resource";
			}

			@Override
			public boolean needsListUpdate() {
				return textChanged;
			}

			@Override
			public Collection<? extends GUIElement> getDropdownElements(GUIElement guiElement) {
				blockElements = getProductionElements();
				return blockElements;
			}

			@Override
			public int getDropdownHeight() {
				return 26;
			}

			@Override
			public Object getDefault() {
				if(blockElements != null && blockElements.size() > 0) {
					return blockElements.get(0);
				} else {
					return null;
				}
			}

			@Override
			public void flagListNeedsUpdate(boolean flag) {
				textChanged = flag;
			}
		});

		addBlockDisplay(new BlockDisplayResult() {

			@Override
			public BlockSelectCallback initCallback() {
				return super.callback;
			}

			@Override
			public String getToolTipText() {
				return "Selected Block";
			}

			@Override
			public short getDefault() {
				return ElementKeyMap.CORE_ID;
			}

			@Override
			public short getCurrentValue() {
				if(selectedBlockType != null) {
					return selectedBlockType.getId();
				} else {
					return 0;
				}
			}

			@Override
			public float getIconScale() {
				return 0.5f;
			}

			@Override
			public float getWeight() {
				return 0.3f;
			}

		});

		addTextBar(new TextBarResult() {

			@Override
			public TextBarCallback initCallback() {
				return super.callback;
			}

			@Override
			public String getToolTipText() {
				return Lng.str("Enter blocks to search for in the drop down");
			}

			@Override
			public String getName() {
				return Lng.str("Search Blocks");
			}

			@Override
			public String onTextChanged(String text) {
				String t = text.trim();
				if(!t.equals(curText)) {
					curText = t;
					textChanged = true;
				}
				return text;
			}
		});

		if(itemInput == null) {
			itemInput = new GUIActivatableTextBar(getState(), FontLibrary.FontSize.MEDIUM, 10, 1, "COUNT", getContent(), new TextCallback() {
				@Override
				public String[] getCommandPrefixes() {
					return null;
				}

				@Override
				public String handleAutoComplete(String s, TextCallback textCallback, String s1) {
					return null;
				}

				@Override
				public void onFailedTextCheck(String s) {

				}

				@Override
				public void onTextEnter(String s, boolean b, boolean b1) {

				}

				@Override
				public void newLine() {

				}
			}, null);
			itemInput.onInit();
		}
		itemInput.setPos(0, 30, 0);
		getContent().attach(itemInput);
		itemInput.draw();
		if(nameInput != null) {
			nameInput.setText("");
			nameInput.deactivateBar();
			nameInput.cleanUp();
		}

		if(rewardInput == null) {
			rewardInput = new GUIActivatableTextBar(getState(), FontLibrary.FontSize.MEDIUM, 10, 1, "REWARD", getContent(), new TextCallback() {
				@Override
				public String[] getCommandPrefixes() {
					return null;
				}

				@Override
				public String handleAutoComplete(String s, TextCallback textCallback, String s1) throws PrefixNotFoundException {
					return null;
				}

				@Override
				public void onFailedTextCheck(String s) {

				}

				@Override
				public void onTextEnter(String s, boolean b, boolean b1) {

				}

				@Override
				public void newLine() {

				}
			}, null);
			rewardInput.onInit();
			rewardInput.setPos(0, 60, 0);
			getContent().attach(rewardInput);
		}

		rewardInput.setText("");
	}

	public void drawBountyPanel() {
		if(nameInput == null) {
			nameInput = new GUIActivatableTextBar(getState(), FontLibrary.FontSize.MEDIUM, 30, 1, "NAME", getContent(), new TextCallback() {
				@Override
				public String[] getCommandPrefixes() {
					return null;
				}

				@Override
				public String handleAutoComplete(String s, TextCallback textCallback, String s1) {
					return null;
				}

				@Override
				public void onFailedTextCheck(String s) {

				}

				@Override
				public void onTextEnter(String s, boolean b, boolean b1) {

				}

				@Override
				public void newLine() {

				}
			}, null);
			nameInput.onInit();
		}
		nameInput.setPos(0, 30, 0);
		getContent().attach(nameInput);
		nameInput.draw();
		if(itemInput != null) {
			itemInput.setText("");
			itemInput.deactivateBar();
			itemInput.cleanUp();
		}

		if(dropdown != null) {
			dropdown.cleanUp();
			getContent().detach(dropdown);
		}
		if(display != null) {
			display.cleanUp();
			getContent().detach(display);
		}
		if(textBar != null) {
			textBar.cleanUp();
			getContent().detach(textBar);
		}

		if(rewardInput == null) {
			rewardInput = new GUIActivatableTextBar(getState(), FontLibrary.FontSize.MEDIUM, 10, 1, "REWARD", getContent(), new TextCallback() {
				@Override
				public String[] getCommandPrefixes() {
					return null;
				}

				@Override
				public String handleAutoComplete(String s, TextCallback textCallback, String s1) {
					return null;
				}

				@Override
				public void onFailedTextCheck(String s) {

				}

				@Override
				public void onTextEnter(String s, boolean b, boolean b1) {

				}

				@Override
				public void newLine() {

				}
			}, null);
			rewardInput.onInit();
			rewardInput.setPos(0, 60, 0);
			getContent().attach(rewardInput);
		}

		rewardInput.setText("");
	}

	@Override
	public float getHeight() {
		return getContent().getHeight();
	}

	@Override
	public float getWidth() {
		return getContent().getWidth();
	}

	private void addDropdown(DropdownResult result) {
		dropdown = new GUIAdvDropdown(getState(), getContent(), result);
		dropdown.setPos(0, 120, 0);
		getContent().attach(dropdown);
	}

	private void addBlockDisplay(BlockDisplayResult displayResult) {
		display = new GUIAdvBlockDisplay(getState(), getContent(), displayResult);
		display.setPos(0, 120, 0);
		getContent().attach(display);
	}

	private void addTextBar(TextBarResult textBarResult) {
		textBar = new GUIAdvTextBar(getState(), getContent(), textBarResult);
		textBar.setPos(0, 90, 0);
		getContent().attach(textBar);
	}

	@Override
	public void onDone() {

	}

	@Override
	public void onInit() {
		super.onInit();
		setPos(0, 0, 0);
		(buttonPane = new GUIHorizontalButtonTablePane(getState(), ContractData.ContractType.values().length - 1, 1, getContent())).onInit();
		GUIActivationCallback activationCallback = new GUIActivationCallback() {
			@Override
			public boolean isVisible(InputState inputState) {
				return true;
			}

			@Override
			public boolean isActive(InputState inputState) {
				return NewContractPanel.this.isActive();
			}
		};

		buttonPane.addButton(0, 0, "BOUNTY", GUIHorizontalArea.HButtonColor.BLUE, guiCallback, activationCallback).setUserPointer("BOUNTY");
		buttonPane.addButton(1, 0, "ITEMS", GUIHorizontalArea.HButtonColor.BLUE, guiCallback, activationCallback).setUserPointer("ITEMS");

		buttonPane.setPos(0, 0, 0);
		getContent().attach(buttonPane);
		background.setWidth(getWidth());
	}

	public static ArrayList<ElementInformation> getResourcesFilter() {
		ArrayList<ElementInformation> filter = new ArrayList<>();
		ArrayList<ElementInformation> elementList = new ArrayList<>();
		ElementKeyMap.getCategoryHirarchy().getChild("Manufacturing").getInfoElementsRecursive(elementList);
		for(ElementInformation info : elementList) {
			if(!info.isDeprecated() && info.isShoppable() && info.isInRecipe() && !info.getName().contains("Paint") && !info.getName().contains("Hardener") && !info.getName().contains("Scrap")) filter.add(info);
		}
		return filter;
	}

	public static ArrayList<ElementInformation> getProductionFilter() {
		ArrayList<ElementInformation> filter = new ArrayList<>();
		ArrayList<ElementInformation> elementList = new ArrayList<>();
		ElementKeyMap.getCategoryHirarchy().getChild("General").getInfoElementsRecursive(elementList);
		ElementKeyMap.getCategoryHirarchy().getChild("Ship").getInfoElementsRecursive(elementList);
		ElementKeyMap.getCategoryHirarchy().getChild("SpaceStation").getInfoElementsRecursive(elementList);
		for(ElementInformation info : elementList) {
			if(!info.isDeprecated() && info.isShoppable() && info.isInRecipe()) filter.add(info);
		}
		return filter;
	}

	private ArrayList<GUIElement> getResourceElements() {
		ArrayList<GUIElement> elementList = new ArrayList<>();
		GameClientState gameClientState = GameClient.getClientState();
		for(ElementInformation elementInfo : getResourcesFilter()) {
			GUIAncor anchor = new GUIAncor(gameClientState, getWidth(), 26.0F);
			elementList.add(anchor);
			GUITextOverlay textOverlay = new GUITextOverlay(100, 26, FontLibrary.getBoldArial12White(), gameClientState);

			textOverlay.setTextSimple(elementInfo.getName());
			anchor.setUserPointer(elementInfo);
			GUIBlockSprite blockSprite = new GUIBlockSprite(gameClientState, elementInfo.getId());
			blockSprite.getScale().set(0.4F, 0.4F, 0.0F);
			anchor.attach(blockSprite);
			textOverlay.getPos().x = 50.0F;
			textOverlay.getPos().y = 7.0F;
			anchor.attach(textOverlay);
		}
		return elementList;
	}

	private List<GUIElement> getProductionElements() {
		ArrayList<GUIElement> elementList = new ArrayList<>();
		GameClientState gameClientState = GameClient.getClientState();

		for(ElementInformation elementInfo : getProductionFilter()) {

			GUIAncor anchor = new GUIAncor(gameClientState, getWidth(), 26.0F);
			elementList.add(anchor);
			GUITextOverlay textOverlay = new GUITextOverlay(100, 26, FontLibrary.getBoldArial12White(), gameClientState);

			textOverlay.setTextSimple(elementInfo.getName());
			anchor.setUserPointer(elementInfo);
			GUIBlockSprite blockSprite = new GUIBlockSprite(gameClientState, elementInfo.getId());
			blockSprite.getScale().set(0.4F, 0.4F, 0.0F);
			anchor.attach(blockSprite);
			textOverlay.getPos().x = 50.0F;
			textOverlay.getPos().y = 7.0F;
			anchor.attach(textOverlay);
		}
		return elementList;
	}

	public void createBountyContract() {
		PlayerState currentPlayer = GameClient.getClientPlayerState();
		if(nameInput == null || nameInput.getText().isEmpty()) {
			(new SimplePopup(getState(), "Cannot Add Bounty", "You must enter a name for the bounty!")).activate();
			return;
		}
		if(rewardInput == null || rewardInput.getText().isEmpty()) {
			(new SimplePopup(getState(), "Cannot Add Bounty", "You must enter a reward amount!")).activate();
			return;
		}
		if(currentPlayer.getFactionId() == 0) {
			(new SimplePopup(getState(), "Cannot Add Bounty", "You must be in a faction to do this!")).activate();
			return;
		}
		if(getReward() <= 0) {
			(new SimplePopup(getState(), "Cannot Add Bounty", "The reward must be above 0!")).activate();
			return;
		}
		if(currentPlayer.getCredits() < getReward()) {
			(new SimplePopup(getState(), "Cannot Add Bounty", "You do not have enough credits!")).activate();
			return;
		}
		String name = nameInput.getText();
		int bountyAmount = getReward();
		PlayerData playerData = PlayerDataManager.getInstance(currentPlayer.isOnServer()).getFromName(name, currentPlayer.isOnServer());
		PlayerData currentPlayerData = PlayerDataManager.getInstance(currentPlayer.isOnServer()).getFromName(currentPlayer.getName(), currentPlayer.isOnServer());
		if(playerData == null) {
			(new SimplePopup(getState(), "Cannot Add Bounty", "Player " + name + " does not exist!")).activate();
		} else {
			if(currentPlayer.getName().equals(name) && !currentPlayer.isAdmin()) {
				(new SimplePopup(getState(), "Cannot Add Bounty", "You can't put a bounty on yourself!")).activate();
			} else if(currentPlayer.getFactionId() == playerData.getFactionID() && !currentPlayer.isAdmin()) {
				(new SimplePopup(getState(), "Cannot Add Bounty", "You can't put a bounty on a member of your own faction!")).activate();
			} else if(GameCommon.getGameState().getFactionManager().getRelation(currentPlayerData.getFactionID(), playerData.getFactionID()) == FactionRelation.RType.FRIEND && !currentPlayer.isAdmin()) {
				(new SimplePopup(getState(), "Cannot Add Bounty", "You can't put a bounty on a member of an allied faction!")).activate();
			} else {
				JSONObject targetData = new JSONObject();
				targetData.put("player_name", playerData.getName());
				String contractName = "Kill " + playerData.getName();
				BountyContract contract = new BountyContract(currentPlayer.getFactionId(), contractName, bountyAmount, targetData);
				ContractDataManager.getInstance(currentPlayer.isOnServer()).addData(contract, currentPlayer.isOnServer());
				ContractDataManager.getInstance(currentPlayer.isOnServer()).sendPacket(contract, DataManager.ADD_DATA, currentPlayer.isOnServer());
				currentPlayer.setCredits(currentPlayer.getCredits() - contract.getReward());
			}
		}
	}

	public void createItemsContract() {
	}
}
