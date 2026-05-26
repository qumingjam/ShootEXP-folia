package moe.feo.shootexp;

import moe.feo.shootexp.config.Language;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * ShootEXP GUI 界面
 */
public class SettingsGUI implements Listener {

	private static final String GUI_TITLE = "§3§lShootEXP 设置";
	private static final int GUI_SIZE = 27; // 3行

	/**
	 * 打开设置 GUI
	 */
	public static void openGUI(Player player) {
		UUID playerUuid = player.getUniqueId();
		
		// 确保玩家有状态数据
		if (!PlayerStatusManager.hasStatus(playerUuid)) {
			PlayerStatusManager.addStatus(playerUuid, new PlayerStatus());
		}
		PlayerStatus status = PlayerStatusManager.getStatus(playerUuid);
		
		// 创建 inventory
		Inventory inventory = Bukkit.createInventory(null, GUI_SIZE, GUI_TITLE);
		
		// === 第一层：指令快捷方式（第0行）===
		
		// 帮助指令 - 书本
		ItemStack helpBook = createItem(Material.BOOK, "§e§l帮助指令", 
			"§7点击查看 ShootEXP 帮助",
			"§7命令: /shootexp help");
		inventory.setItem(0, helpBook);
		
		// 查看状态 - 钟表
		ItemStack statusClock = createItem(Material.CLOCK, "§e§l查看状态", 
			"§7点击查看你的当前状态",
			"§7命令: /shootexp status");
		inventory.setItem(1, statusClock);
		
		// 重载插件（仅OP）- 红石火把
		if (player.isOp() || player.hasPermission("shootexp.reload")) {
			ItemStack reloadTorch = createItem(Material.REDSTONE_TORCH, "§c§l重载插件", 
				"§7点击重载插件配置",
				"§7命令: /shootexp reload");
			inventory.setItem(8, reloadTorch);
		}
		
		// === 第二层：开关控制（第2行）===
		
		// 消息接收开关
		boolean receiveMessages = status.isReceiveMessages();
		Material messageMaterial = receiveMessages ? Material.LIME_DYE : Material.RED_DYE;
		String messageStatus = receiveMessages ? "§a开启" : "§c关闭";
		ItemStack messageToggle = createItem(messageMaterial, "§e§l消息接收", 
			"§7当前状态: " + messageStatus,
			"",
			"§7点击切换是否接收",
			"§7ShootEXP 相关消息",
			"",
			receiveMessages ? "§a✓ 已启用" : "§c✗ 已禁用");
		inventory.setItem(11, messageToggle);
		
		// 被攻击权限开关
		boolean canBeAttacked = status.isCanBeAttacked();
		Material attackMaterial = canBeAttacked ? Material.LIME_DYE : Material.RED_DYE;
		String attackStatus = canBeAttacked ? "§a允许" : "§c禁止";
		ItemStack attackToggle = createItem(attackMaterial, "§e§l被攻击权限", 
			"§7当前状态: " + attackStatus,
			"",
			"§7点击切换是否允许",
			"§7其他玩家对你使用",
			"§7ShootEXP 功能",
			"",
			canBeAttacked ? "§a✓ 已允许" : "§c✗ 已禁止");
		inventory.setItem(15, attackToggle);
		
		// 装饰物品 - 玻璃板填充空位
		ItemStack glass = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
		for (int i = 0; i < GUI_SIZE; i++) {
			if (inventory.getItem(i) == null) {
				inventory.setItem(i, glass);
			}
		}
		
		// 打开 GUI
		player.openInventory(inventory);
	}
	
	/**
	 * 创建物品
	 */
	private static ItemStack createItem(Material material, String name, String... lore) {
		ItemStack item = new ItemStack(material);
		ItemMeta meta = item.getItemMeta();
		if (meta != null) {
			meta.setDisplayName(name);
			if (lore != null && lore.length > 0) {
				List<String> loreList = new ArrayList<>();
				for (String line : lore) {
					loreList.add(line);
				}
				meta.setLore(loreList);
			}
			item.setItemMeta(meta);
		}
		return item;
	}
	
	/**
	 * 处理 GUI 点击事件
	 */
	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		// 检查是否是 ShootEXP GUI
		if (!event.getView().getTitle().equals(GUI_TITLE)) {
			return;
		}
		
		event.setCancelled(true); // 阻止物品移动
		
		Player player = (Player) event.getWhoClicked();
		int slot = event.getSlot();
		ItemStack clickedItem = event.getCurrentItem();
		
		if (clickedItem == null || clickedItem.getType() == Material.AIR) {
			return;
		}
		
		UUID playerUuid = player.getUniqueId();
		if (!PlayerStatusManager.hasStatus(playerUuid)) {
			PlayerStatusManager.addStatus(playerUuid, new PlayerStatus());
		}
		PlayerStatus status = PlayerStatusManager.getStatus(playerUuid);
		
		// === 第一层：指令快捷方式 ===
		
		// 帮助指令 (slot 0)
		if (slot == 0) {
			player.closeInventory();
			player.performCommand("shootexp help");
			return;
		}
		
		// 查看状态 (slot 1)
		if (slot == 1) {
			player.closeInventory();
			player.performCommand("shootexp status");
			return;
		}
		
		// 重载插件 (slot 8)
		if (slot == 8 && (player.isOp() || player.hasPermission("shootexp.reload"))) {
			player.closeInventory();
			player.performCommand("shootexp reload");
			player.sendMessage(Language.COMMAND_RELOADED.getString());
			return;
		}
		
		// === 第二层：开关控制 ===
		
		// 消息接收开关 (slot 11)
		if (slot == 11) {
			boolean current = status.isReceiveMessages();
			status.setReceiveMessages(!current);
			
			// 保存玩家数据
			DataManager.savePlayerData(playerUuid);
			
			if (!current) {
				player.sendMessage(Language.COMMAND_TOGGLE_MESSAGES_ON.getString());
			} else {
				player.sendMessage(Language.COMMAND_TOGGLE_MESSAGES_OFF.getString());
			}
			
			// 刷新 GUI
			openGUI(player);
			return;
		}
		
		// 被攻击权限开关 (slot 15)
		if (slot == 15) {
			boolean current = status.isCanBeAttacked();
			status.setCanBeAttacked(!current);
			
			// 保存玩家数据
			DataManager.savePlayerData(playerUuid);
			
			if (!current) {
				player.sendMessage(Language.COMMAND_TOGGLE_ATTACK_ON.getString());
			} else {
				player.sendMessage(Language.COMMAND_TOGGLE_ATTACK_OFF.getString());
			}
			
			// 刷新 GUI
			openGUI(player);
			return;
		}
	}
}
