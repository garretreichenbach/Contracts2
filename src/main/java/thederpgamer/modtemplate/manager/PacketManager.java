package thederpgamer.modtemplate.manager;

import api.network.packets.PacketUtil;
import thederpgamer.modtemplate.network.client.ExampleClientPacket;

public class PacketManager {

	public static void initialize() {
		PacketUtil.registerPacket(ExampleClientPacket.class);
	}
}