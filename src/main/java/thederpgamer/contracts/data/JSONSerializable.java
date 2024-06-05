package thederpgamer.contracts.data;

import org.json.JSONObject;

/**
 * [Description]
 *
 * @author Garret Reichenbach
 */
public interface JSONSerializable {

	void fromJSON(JSONObject json);

	JSONObject toJSON();
}
