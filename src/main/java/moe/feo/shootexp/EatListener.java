package moe.feo.shootexp;

import com.dre.brewery.api.BreweryApi;
import moe.feo.shootexp.config.Config;
import moe.feo.shootexp.config.Language;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import static org.bukkit.Bukkit.getServer;

/**
 * 监听玩家右键
 */
public class EatListener implements Listener {

	@EventHandler
	public void onClick(PlayerInteractEvent e) {
		Player player = e.getPlayer();
		Action action = e.getAction();
		ItemStack item = e.getItem();
		if (item == null) {
			return;
		}
		if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
			if (!EXP.isEXPItem(item)) {
				return;
			}
			if (((getServer().getPluginManager().getPlugin("Brewery") != null && getServer().getPluginManager().getPlugin("Brewery").isEnabled())
					|| (getServer().getPluginManager().getPlugin("BreweryX") != null && getServer().getPluginManager().getPlugin("BreweryX").isEnabled()))
					&& action == Action.RIGHT_CLICK_BLOCK) {
				org.bukkit.block.Block clicked = e.getClickedBlock();
				if (clicked == null) return;
				if (BreweryApi.getBarrel(clicked) != null || BreweryApi.getCauldron(clicked) != null) {
					return;
				} else if (clicked.getType() == Material.WATER_CAULDRON) {
					Location down = clicked.getLocation().subtract(0, 1, 0);
					if (down.getBlock().getType() == Material.FIRE) {
						return;
					}
				}
			}
			EXP exp = new EXP(item);
			switch (Config.EXP_TYPE.getString()){
				case "SKILLAPI":
					com.sucy.skill.SkillAPI.getPlayerData(player)
							.giveExp(exp.getAmount(), com.sucy.skill.api.enums.ExpSource.SPECIAL);
					break;
				// MMOCore 支持暂时禁用，如需使用请取消注释并确保已安装 MMOCore
				/*
				case "MMOCORE":
					net.Indyuce.mmocore.api.player.PlayerData.get(player)
							.giveExperience(exp.getAmount(), net.Indyuce.mmocore.api.experience.EXPSource.OTHER);
					break;
				*/
				default:
					player.giveExp(exp.getAmount());
			}
			player.getWorld().playSound(player.getLocation(), Config.SOUND_EAT.getString(), SoundCategory.PLAYERS, 1, 1);
			String msg = Language.MESSAGES_EAT.getString();
			java.util.Map<String, String> replacements = new java.util.HashMap<>();
			replacements.put("%PLAYER%", player.getName());
			replacements.put("%OWNER%", exp.getOwner());
			replacements.put("%RECIPIENT%", exp.getRecipient());
			replacements.put("%AMOUNT%", String.valueOf(exp.getAmount()));
			for (java.util.Map.Entry<String, String> entry : replacements.entrySet()) {
				msg = msg.replace(entry.getKey(), entry.getValue());
			}
			if (Config.PRIVATE_MESSAGE.getBoolean()) {
				player.sendMessage(msg);
				Player owner = Bukkit.getPlayer(exp.getOwner());
				Player recipient = Bukkit.getPlayer(exp.getRecipient());
				if (owner != null && owner.isOnline()) {
					owner.sendMessage(msg);
				}
				if (recipient != null && recipient.isOnline()) {
					recipient.sendMessage(msg);
				}
			} else {
				getServer().broadcastMessage(msg);
			}
			item.setAmount(0);
			e.setCancelled(true);
		}
	}
}
