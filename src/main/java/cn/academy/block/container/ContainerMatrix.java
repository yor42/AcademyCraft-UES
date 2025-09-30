package cn.academy.block.container;

import cn.academy.ACItems;
import cn.academy.block.tileentity.TileMatrix;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/**
 * @author WeAthFolD
 */
public class ContainerMatrix extends TechUIContainer<TileMatrix> {
    
    public ContainerMatrix(TileMatrix _tile, EntityPlayer _player) {
        super(_player, _tile);

        initInventory();
    }
    
    private void initInventory() {
        this.addSlotToContainer(new SlotPlate(tile, 0, 78, 11));
        this.addSlotToContainer(new SlotPlate(tile, 1, 53, 60));
        this.addSlotToContainer(new SlotPlate(tile, 2, 104, 60));
        
        this.addSlotToContainer(new SlotCore(tile, 3, 78, 36));

        mapPlayerInventory();

        SlotGroup invGroup = gRange(4, 4 + 36);

        addTransferRule(invGroup, stack -> stack.getItem() == ACItems.constraint_plate, gSlots(0, 1, 2));
        addTransferRule(invGroup, stack -> stack.getItem() == ACItems.mat_core, gSlots(3));
        addTransferRule(gRange(0, 4), invGroup);
    }
    
    public static class SlotCore extends Slot {

        private final TileMatrix TE;

        public SlotCore(TileMatrix inv, int slot, int x, int y) {
            super(inv, slot, x, y);
            this.TE = inv;
        }
        
        @Override
        public boolean isItemValid(ItemStack stack) {
            return !stack.isEmpty() && stack.getItem() == ACItems.mat_core;
        }

        @Override
        public void onSlotChanged() {
            super.onSlotChanged();
            TE.sync();
        }
        
    }
    
    public static class SlotPlate extends Slot {

        private final TileMatrix TE;

        public SlotPlate(TileMatrix inv, int slot, int x, int y) {
            super(inv, slot, x, y);
            this.TE = inv;
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            return !stack.isEmpty() && stack.getItem() == ACItems.constraint_plate;
        }

        @Override
        public void onSlotChanged() {
            super.onSlotChanged();
            TE.sync();
        }
    }

}