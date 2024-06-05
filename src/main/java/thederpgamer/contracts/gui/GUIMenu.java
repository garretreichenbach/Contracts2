package thederpgamer.contracts.gui;

import api.common.GameClient;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.GUICallback;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIActiveInterface;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIMainWindow;

public abstract class GUIMenu extends GUIElement implements GUIActiveInterface {

	protected GUIMainWindow guiWindow;
	private boolean initialized;

	protected GUIMenu(String windowID, int width, int height) {
		super(GameClient.getClientState());
		guiWindow = new GUIMainWindow(getState(), width, height, windowID);
	}

	@Override
	public void onInit() {
		guiWindow.onInit();
		guiWindow.setCloseCallback(new GUICallback() {
			public void callback(GUIElement guiElement, MouseEvent event) {
				if(event.pressedLeftMouse()) {
					GameClient.getClientState().getController().queueUIAudio("0022_menu_ui - back");
					GameClient.getClientState().getWorldDrawer().getGuiDrawer().getPlayerPanel().deactivateAll();
				}
			}

			public boolean isOccluded() {
				return !getState().getController().getPlayerInputs().isEmpty();
			}
		});
		recreateTabs();
		initialized = true;
	}

	@Override
	public void draw() {
		if(!initialized) onInit();
	}

	@Override
	public void cleanUp() {
		if(initialized) {
			guiWindow.cleanUp();
			initialized = false;
		}
	}

	@Override
	public float getWidth() {
		return guiWindow.getWidth();
	}

	@Override
	public float getHeight() {
		return guiWindow.getHeight();
	}

	public abstract void recreateTabs();
}
