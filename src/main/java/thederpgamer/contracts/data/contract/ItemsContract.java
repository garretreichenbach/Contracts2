package thederpgamer.contracts.data.contract;

import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import api.utils.game.inventory.InventoryUtils;
import api.utils.game.inventory.ItemStack;
import org.json.JSONObject;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.inventory.Inventory;

import java.io.IOException;

/**
 * [Description]
 *
 * @author Garret Reichenbach
 */
public class ItemsContract extends Contract {

    private ItemStack target;

    public ItemsContract(PacketReadBuffer packetReadBuffer) throws IOException {
        super(packetReadBuffer);
    }

    public ItemsContract(int contractorID, String name, int reward, ItemStack target) {
        super(contractorID, name, reward);
        this.target = target;
    }

    public ItemsContract(JSONObject json) {
        super(json);
        fromJSON(json);
    }

    @Override
    public void fromJSON(JSONObject json) {
        super.fromJSON(json);
        target = new ItemStack((short) json.getInt("targetID"), json.getInt("targetAmount"));
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("targetID", target.getId());
        json.put("targetAmount", target.getAmount());
        return json;
    }

    @Override
    public boolean canComplete(PlayerState player) {
        if(!claimants.containsKey(player.getName())) return false;
        if(player.isAdmin() && player.isUseCreativeMode()) return true;
        else {
            Inventory playerInventory = player.getInventory();
            int count = InventoryUtils.getItemAmount(playerInventory, target.getId());
            return count >= target.getAmount();
        }
    }

    @Override
    public ContractType getContractType() {
        return ContractType.ITEMS;
    }

    @Override
    public void onCompletion(PlayerState player) {
        assert player.isOnServer();
        Inventory playerInventory = player.getInventory();
        InventoryUtils.consumeItems(playerInventory, target.getId(), target.getAmount());
        player.setCredits(player.getCredits() + reward);
    }

    @Override
    public void readFromBuffer(PacketReadBuffer readBuffer) throws IOException {
        target = new ItemStack(readBuffer.readShort(), readBuffer.readInt());
    }

    @Override
    public void writeToBuffer(PacketWriteBuffer writeBuffer) throws IOException {
        writeBuffer.writeShort(target.getId());
        writeBuffer.writeInt(target.getAmount());
    }
}
