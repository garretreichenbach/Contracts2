package thederpgamer.modtemplate.manager;

import api.utils.textures.StarLoaderTexture;
import org.schema.schine.graphicsengine.forms.Mesh;
import org.schema.schine.graphicsengine.forms.Sprite;
import thederpgamer.modtemplate.ModTemplate;

import java.util.HashMap;

public class ResourceManager {

	private static final String TEXTURE_PATH = "thederpgamer/modtemplate/resources/textures/";
	private static final String SPRITE_PATH = "thederpgamer/modtemplate/resources/sprites/";
	private static final String MODEL_PATH = "thederpgamer/modtemplate/resources/models/";

	private static final String[] textureNames = {

	};

	private static final String[] spriteNames = {

	};

	private static final String[] modelNames = {

	};

	private static final HashMap<String, StarLoaderTexture> textureMap = new HashMap<>();
	private static final HashMap<String, Sprite> spriteMap = new HashMap<>();
	private static final HashMap<String, Mesh> modelMap = new HashMap<>();

	public static void loadResources() {
		StarLoaderTexture.runOnGraphicsThread(new Runnable() {
			@Override
			public void run() {
				//Load Textures
				for(String texturePath : textureNames) {
					String textureName = texturePath.substring(texturePath.lastIndexOf('/') + 1);
					try {
						if(textureName.endsWith("icon")) textureMap.put(textureName, StarLoaderTexture.newIconTexture(ModTemplate.getInstance().getJarBufferedImage(TEXTURE_PATH + texturePath + ".png")));
						else textureMap.put(textureName, StarLoaderTexture.newBlockTexture(ModTemplate.getInstance().getJarBufferedImage(TEXTURE_PATH + texturePath + ".png")));
					} catch(Exception exception) {
						ModTemplate.logError("Failed to load texture \"" + texturePath + "\"", exception);
					}
				}

				//Load Sprites
				for(String spriteName : spriteNames) {
					try {
						Sprite sprite = StarLoaderTexture.newSprite(ModTemplate.getInstance().getJarBufferedImage(SPRITE_PATH + spriteName + ".png"), ModTemplate.getInstance(), spriteName, false, false);
						sprite.setName(spriteName);
						spriteMap.put(spriteName, sprite);
					} catch(Exception exception) {
						ModTemplate.logError("Failed to load sprite \"" + spriteName + "\"", exception);
					}
				}
			}
		});
	}

	public static StarLoaderTexture getTexture(String name) {
		return textureMap.get(name);
	}

	public static Sprite getSprite(String name) {
		return spriteMap.get(name);
	}

	public static Mesh getModel(String name) {
		return modelMap.get(name);
	}
}
