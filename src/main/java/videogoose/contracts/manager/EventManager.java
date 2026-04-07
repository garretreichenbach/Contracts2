package videogoose.contracts.manager;

import api.common.GameClient;
import api.listener.Listener;
import api.listener.events.gui.GUITopBarCreateEvent;
import api.listener.events.gui.MainWindowTabAddEvent;
import api.listener.events.player.PlayerDeathEvent;
import api.mod.StarLoader;
import api.utils.game.SegmentControllerUtils;
import org.json.JSONObject;
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
import videogoose.contracts.Contracts;
import videogoose.contracts.data.DataManager;
import videogoose.contracts.data.contract.BountyContract;
import videogoose.contracts.data.contract.ContractDataManager;
import videogoose.contracts.gui.contract.playercontractlist.PlayerContractsDialog;

import java.util.List;

public class EventManager {

	public static void initialize(Contracts instance) {
		StarLoader.registerListener(PlayerDeathEvent.class, new Listener<PlayerDeathEvent>() {
			@Override
			public void onEvent(PlayerDeathEvent event) {
				if(event.getDamager() == null) return;
				Damager damager = event.getDamager();
				PlayerState killer = null;
				if(damager instanceof PlayerState) {
					killer = (PlayerState) damager;
				} else if(damager instanceof SegmentController) {
					SegmentController sc = (SegmentController) damager;
					if(sc.isConrolledByActivePlayer()) {
						killer = SegmentControllerUtils.getAttachedPlayers(sc).get(0);
					} else {
						sc = sc.railController.getRoot();
						if(sc.isConrolledByActivePlayer()) killer = SegmentControllerUtils.getAttachedPlayers(sc).get(0);
					}
				}
				if(killer == null) return;
				final PlayerState finalKiller = killer;
				ContractDataManager mgr = ContractDataManager.getInstance(event.getPlayer().isOnServer());
				List<BountyContract> bounties = (List<BountyContract>) mgr.getContractsOfType(BountyContract.class, event.getPlayer().isOnServer());
				for(BountyContract bounty : bounties) {
					if(bounty.getBountyType() != BountyContract.PLAYER) continue;
					JSONObject targetData = bounty.getTargetData();
					if(!targetData.has("player_name")) continue;
					if(!targetData.getString("player_name").equals(event.getPlayer().getName())) continue;
					if(event.getPlayer().equals(finalKiller) && event.getPlayer().getFactionId() != finalKiller.getFactionId() && !finalKiller.isAdmin()) return;
					bounty.setKilledTarget(event.getPlayer().isOnServer(), true);
					mgr.sendPacket(bounty, DataManager.UPDATE_DATA, !event.getPlayer().isOnServer());
					finalKiller.sendServerMessage(new ServerMessage(new String[]{Lng.str("You can now turn in the \"" + bounty.getName() + "\" contract!")}, ServerMessage.MESSAGE_TYPE_INFO));
					return;
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
							new PlayerContractsDialog().activate();
						}
					}

					@Override
					public boolean isOccluded() {
						return false;
					}
				}, new GUIActivationHighlightCallback() {
					@Override
					public boolean isHighlighted(InputState inputState) {
						ContractDataManager mgr = ContractDataManager.getInstance(false);
						return mgr.canCompleteAny(GameClient.getClientPlayerState());
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
