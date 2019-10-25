package io.github.thebusybiscuit.chestterminal;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ClickAction;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.Item.CustomItem;
import me.mrCookieSlime.Slimefun.SlimefunPlugin;
import me.mrCookieSlime.Slimefun.GEO.OreGenSystem;
import me.mrCookieSlime.Slimefun.Lists.RecipeType;
import me.mrCookieSlime.Slimefun.Objects.Category;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.AContainer;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.MachineHelper;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.MachineRecipe;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.energy.ChargableBlock;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;
import me.mrCookieSlime.Slimefun.cscorelib2.protection.ProtectableAction;

@Deprecated
public abstract class QuartzDrill extends AContainer {
	
	private static final int[] border = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 13, 31, 36, 37, 38, 39, 40, 41, 42, 43, 44, 9, 10, 11, 12, 18, 21, 27, 28, 29, 30, 19, 20 };
	private static final int[] border_out = { 14, 15, 16, 17, 23, 26, 32, 33, 34, 35 };

	public QuartzDrill(Category category, ItemStack item, String name, RecipeType recipeType, ItemStack[] recipe) {
		super(category, item, name, recipeType, recipe);
		
		new BlockMenuPreset(name, getInventoryTitle()) {
			
			@Override
			public void init() {
				this.constructMenu(this);
			}

			private void constructMenu(BlockMenuPreset preset) {
				MenuClickHandler click = (p, slot, item, action) -> false;
				
				for (int i: border) {
					preset.addItem(i, new CustomItem(Material.GRAY_STAINED_GLASS_PANE, " "), click);
				}
				for (int i: border_out) {
					preset.addItem(i, new CustomItem(Material.ORANGE_STAINED_GLASS_PANE, " "), click);
				}
				
				preset.addItem(22, new CustomItem(Material.BLACK_STAINED_GLASS_PANE, " "), click);
				
				for (int i: getOutputSlots()) {
					preset.addMenuClickHandler(i, new AdvancedMenuClickHandler() {
						
						@Override
						public boolean onClick(Player p, int slot, ItemStack cursor, ClickAction action) {
							return false;
						}

						@Override
						public boolean onClick(InventoryClickEvent e, Player p, int slot, ItemStack cursor, ClickAction action) {
							return cursor == null || cursor.getType() == null || cursor.getType() == Material.AIR;
						}
					});
				}
			}

			@Override
			public boolean canOpen(Block b, Player p) {
				if (!(p.hasPermission("slimefun.inventory.bypass") || SlimefunPlugin.getProtectionManager().hasPermission(p, b.getLocation(), ProtectableAction.ACCESS_INVENTORIES))) {
					return false;
				}
				
				if (!OreGenSystem.wasResourceGenerated(OreGenSystem.getResource("Млечный кварц"), b.getChunk())) {
					SlimefunPlugin.getLocal().sendMessage(p, "gps.geo.scan-required", true);
					return false;
				}
				return true;
			}

			@Override
			public int[] getSlotsAccessedByItemTransport(ItemTransportFlow flow) {
				if (flow.equals(ItemTransportFlow.INSERT)) return getInputSlots();
				else return getOutputSlots();
			}
		};
	}
	
	@Override
	public int[] getInputSlots() { 
		return new int[0]; 
	}

	@Override
	public String getMachineIdentifier() {
		return "QUARTZ_DRILL";
	}

	@Override
	public String getInventoryTitle() {
		return "&3Кварцевый бур";
	}

	@Override
	public ItemStack getProgressBar() {
		return new ItemStack(Material.DIAMOND_PICKAXE);
	}

	@Override
	public void registerDefaultRecipes() {
		// This machine has no Recipes
	}
	
	public abstract ItemStack getOutput();
	
	protected void tick(Block b) {
		if (isProcessing(b)) {
			int timeleft = progress.get(b);
			if (timeleft > 0) {
				ItemStack item = getProgressBar().clone();
				ItemMeta im = item.getItemMeta();
				((Damageable) im).setDamage(MachineHelper.getDurability(item, timeleft, processing.get(b).getTicks()));
				im.setDisplayName(" ");
				List<String> lore = new ArrayList<>();
				lore.add(MachineHelper.getProgress(timeleft, processing.get(b).getTicks()));
				lore.add("");
				lore.add(MachineHelper.getTimeLeft(timeleft / 2));
				im.setLore(lore);
				item.setItemMeta(im);
				
				BlockStorage.getInventory(b).replaceExistingItem(22, item);
				
				if (ChargableBlock.getCharge(b) < getEnergyConsumption()) return;
				ChargableBlock.addCharge(b, -getEnergyConsumption());
				
				progress.put(b, timeleft - 1);
			}
			else {
				BlockStorage.getInventory(b).replaceExistingItem(22, new CustomItem(Material.BLACK_STAINED_GLASS_PANE, " "));
				pushItems(b, processing.get(b).getOutput());
				
				progress.remove(b);
				processing.remove(b);
			}
		}
		else if (OreGenSystem.getSupplies(OreGenSystem.getResource("Млечный кварц"), b.getChunk(), false) > 0) {
			MachineRecipe r = new MachineRecipe(24, new ItemStack[0], new ItemStack[] {getOutput()});
			if (!fits(b, r.getOutput())) return;
			processing.put(b, r);
			progress.put(b, r.getTicks());
			OreGenSystem.setSupplies(OreGenSystem.getResource("Млечный кварц"), b.getChunk(), OreGenSystem.getSupplies(OreGenSystem.getResource("Млечный кварц"), b.getChunk(), false) - 1);
		}
	}

}
