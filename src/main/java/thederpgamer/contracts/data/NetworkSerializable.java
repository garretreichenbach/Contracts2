package thederpgamer.contracts.data;

import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;

import java.io.IOException;

/**
 * [Description]
 *
 * @author Garret Reichenbach
 */
public interface NetworkSerializable {

	void readFromBuffer(PacketReadBuffer readBuffer) throws IOException;

	void writeToBuffer(PacketWriteBuffer writeBuffer) throws IOException;
}
