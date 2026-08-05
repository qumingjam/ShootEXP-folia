package moe.feo.shootexp;

import com.dre.brewery.api.BreweryApi;
import moe.feo.shootexp.config.Config;
import moe.feo.shootexp.config.Language;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.SoundCategory;
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
            msg = msg.replace("%PLAYER%", player.getName())
                    .replace("%OWNER%", exp.getOwner())
                    .replace("%AMOUNT%", String.valueOf(exp.getAmount()));
            String recipientVal = exp.getRecipient();
            Component message;
            if (recipientVal != null && recipientVal.startsWith("entity.minecraft.")) {
                message = buildWithEntity(msg, recipientVal);
            } else {
                message = parseLegacy(msg.replace("%RECIPIENT%", recipientVal == null ? "" : recipientVal));
            }
            if (Config.PRIVATE_MESSAGE.getBoolean()) {
                player.sendMessage(message);
                Player owner = Bukkit.getPlayer(exp.getOwner());
                Player recipient = Bukkit.getPlayer(exp.getRecipient());
                if (owner != null && owner.isOnline()) {
                    owner.sendMessage(message);
                }
                if (recipient != null && recipient.isOnline()) {
                    recipient.sendMessage(message);
                }
            } else {
                getServer().broadcast(message);
            }
            item.setAmount(0);
            e.setCancelled(true);
        }
    }

    /** 消息组件化：%RECIPIENT% 是可翻译实体 key 时用 translatable（客户端显示中文） */
    private static Component buildWithEntity(String msg, String key) {
        String[] parts = msg.split("%RECIPIENT%");
        Component comp = Component.empty();
        for (int i = 0; i < parts.length; i++) {
            comp = comp.append(parseLegacy(parts[i]));
            if (i < parts.length - 1) {
                comp = comp.append(Component.translatable(key));
            }
        }
        return comp;
    }

    private static Component parseLegacy(String text) {
        return net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                .legacyAmpersand().deserialize(text == null ? "" : text);
    }
}
