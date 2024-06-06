package thederpgamer.contracts.manager;

import api.common.GameClient;
import api.listener.Listener;
import api.listener.events.gui.GUITopBarCreateEvent;
import api.listener.events.gui.MainWindowTabAddEvent;
import api.listener.events.player.PlayerDeathEvent;
import api.listener.events.player.PlayerSpawnEvent;
import api.mod.StarLoader;
import api.utils.StarRunnable;
import api.utils.game.SegmentControllerUtils;
import org.schema.game.client.view.gui.newgui.GUITopBar;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.damage.Damager;
import org.schema.game.common.data.player.PlayerState;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.GUIActivationHighlightCallback;
import org.schema.schine.graphicsengine.forms.gui.GUICallback;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.input.InputState;
import org.schema.schine.network.server.ServerMessage;
import thederpgamer.contracts.Contracts;
import thederpgamer.contracts.data.contract.BountyContract;
import thederpgamer.contracts.data.contract.Contract;
import thederpgamer.contracts.gui.contract.playercontractlist.PlayerContractsDialog;
import thederpgamer.contracts.networking.client.ClientDataManager;
import thederpgamer.contracts.networking.server.ServerActionType;
import thederpgamer.contracts.networking.server.ServerDataManager;

import java.util.ArrayList;

public class EventManager {

	public static void initialize(final Contracts instance) {
		StarLoader.registerListener(PlayerDeathEvent.class, new Listener<PlayerDeathEvent>() {
			@Override
			public void onEvent(PlayerDeathEvent event) {
				if(event.getDamager() != null) {
					Damager damager = event.getDamager();
					PlayerState killer = null;
					if(damager instanceof PlayerState) killer = (PlayerState) damager;
					else if(damager instanceof SegmentController) {
						SegmentController segmentController = (SegmentController) damager;
						if(segmentController.isConrolledByActivePlayer()) killer = SegmentControllerUtils.getAttachedPlayers(segmentController).get(0);
						else {
							segmentController = segmentController.railController.getRoot();
							if(segmentController.isConrolledByActivePlayer()) killer = SegmentControllerUtils.getAttachedPlayers(segmentController).get(0);
						}
					}
					if(killer != null) {
						ArrayList<BountyContract> bounties = ServerDataManager.getBountyContracts();
						for(BountyContract bounty : bounties) {
							if(bounty.getTarget().equals(event.getPlayer().getName())) {
								if(event.getPlayer().equals(killer) && event.getPlayer().getFactionId() != killer.getFactionId() && !killer.isAdmin()) return; //Prevent player from killing themselves
								bounty.setKilledTarget(true);
								ServerDataManager.addOrUpdateContract(bounty);
								killer.sendServerMessage(new ServerMessage(new String[] {Lng.str("You can now turn in the \"" + bounty.getName() + "\" contract!")},  ServerMessage.MESSAGE_TYPE_INFO));
								return;
							}
						}
					}
				}
			}
		}, instance);

		StarLoader.registerListener(PlayerSpawnEvent.class, new Listener<PlayerSpawnEvent>() {
			@Override
			public void onEvent(final PlayerSpawnEvent event) {
				final ArrayList<Contract> contractDataList = new ArrayList<>(ServerDataManager.getAllContracts());
				if(event.getPlayer().getOwnerState() != null) {
					ServerActionType.SEND_CONTRACTS_LIST.send(event.getPlayer().getOwnerState(), contractDataList);
				} else { //Wait for player to spawn
					(new StarRunnable() {
						@Override
						public void run() {
							if(event.getPlayer().getOwnerState() != null) {
								ServerActionType.SEND_CONTRACTS_LIST.send(event.getPlayer().getOwnerState(), contractDataList);
								cancel();
							}
						}
					}).runTimer(instance, 10);
				}
			}
		}, instance);

		StarLoader.registerListener(GUITopBarCreateEvent.class, new Listener<GUITopBarCreateEvent>() {
			@Override
			public void onEvent(GUITopBarCreateEvent event) {
				GUITopBar.ExpandedButton dropDownButton = event.getDropdownButtons().get(event.getDropdownButtons().size() - 1);
				dropDownButton.addExpandedButton("CONTRACTS", new GUICallback() {
					@Override
					public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
						if(mouseEvent.pressedLeftMouse()) {
							GameClient.getClientState().getController().queueUIAudio("0022_menu_ui - enter");
							(new PlayerContractsDialog()).activate();
						}
					}

					@Override
					public boolean isOccluded() {
						return false;
					}
				}, new GUIActivationHighlightCallback() {
					@Override
					public boolean isHighlighted(InputState inputState) {
						return ClientDataManager.canCompleteAny();
					}

					@Override
					public boolean isVisible(InputState inputState) {
						return true;
					}

					@Override
					public boolean isActive(InputState inputState) {
						return true;
					}
				});
			}
		}, instance);

		StarLoader.registerListener(MainWindowTabAddEvent.class, new Listener<MainWindowTabAddEvent>() {
			@Override
			public void onEvent(MainWindowTabAddEvent event) {
				GUIManager.getInstance().createContractsShopTab(event);
			}
		}, instance);
	}
}
