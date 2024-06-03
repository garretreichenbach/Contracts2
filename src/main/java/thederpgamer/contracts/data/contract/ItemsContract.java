package thederpgamer.contracts.data.contract;

import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import api.utils.game.inventory.InventoryUtils;
import api.utils.game.inventory.ItemStack;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.inventory.Inventory;

import java.io.IOException;

/**
 * [Description]
 *
 * @author Garret Reichenbach
 */
public class ItemsContract extends Contract {

    public ItemsContract(PacketReadBuffer packetReadBuffer) throws IOException {
        super(packetReadBuffer);
    }

    public ItemsContract(int contractorID, String name, int reward, Object target) {
        super(contractorID, name, reward, target);
    }

    @Override
    public boolean canComplete(PlayerState player) {
        if(player.isAdmin() && player.isUseCreativeMode()) return true;
        else {
            Inventory playerInventory = player.getInventory();
            for(ItemStack itemStack : (ItemStack[]) target) {
                int count = InventoryUtils.getItemAmount(playerInventory, itemStack.getId());
                if(count < itemStack.getAmount()) return false;
            }
            return true;
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
        for(ItemStack itemStack : (ItemStack[]) target) InventoryUtils.consumeItems(playerInventory, itemStack.getId(), itemStack.getAmount());
        player.setCredits(player.getCredits() + reward);
    }

    @Override
    public void readFromBuffer(PacketReadBuffer readBuffer) throws IOException {

    }

    @Override
    public void writeToBuffer(PacketWriteBuffer writeBuffer) throws IOException {

    }
}
