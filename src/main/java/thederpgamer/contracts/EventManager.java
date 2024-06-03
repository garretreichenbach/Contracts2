package thederpgamer.contracts;

import api.common.GameClient;
import api.listener.Listener;
import api.listener.events.gui.GUITopBarCreateEvent;
import api.listener.events.gui.MainWindowTabAddEvent;
import api.mod.StarLoader;
import org.schema.game.client.view.gui.newgui.GUITopBar;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.GUIActivationHighlightCallback;
import org.schema.schine.graphicsengine.forms.gui.GUICallback;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.input.InputState;

public class EventManager {

    public static void initialize(Contracts instance) {
        StarLoader.registerListener(GUITopBarCreateEvent.class, new Listener<GUITopBarCreateEvent>() {
            @Override
            public void onEvent(final GUITopBarCreateEvent event) {
                GUITopBar.ExpandedButton dropDownButton = event.getDropdownButtons().get(event.getDropdownButtons().size() - 1);
                dropDownButton.addExpandedButton("CONTRACTS", new GUICallback() {
                    @Override
                    public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                        if(mouseEvent.pressedLeftMouse()) {
                            GameClient.getClientState().getController().queueUIAudio("0022_menu_ui - enter");
                            GUIManager.getInstance().toggleContractsControlManager(true);
                        }
                    }

                    @Override
                    public boolean isOccluded() {
                        return false;
                    }
                }, new GUIActivationHighlightCallback() {
                    @Override
                    public boolean isHighlighted(InputState inputState) {
                        return false; //Todo: Highlight when contracts are available
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
