package moe.feo.shootexp;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;

/**
 * 玩家数据持久化管理器
 * 负责保存和加载玩家的 toggle 设置
 */
public class DataManager {

	private static final ShootEXP plugin = ShootEXP.getPlugin(ShootEXP.class);
	private static File dataFile;
	private static FileConfiguration dataConfig;

	/**
	 * 初始化数据管理器
	 */
	public static void init() {
		dataFile = new File(plugin.getDataFolder(), "playerdata.yml");
		if (!dataFile.exists()) {
			try {
				dataFile.getParentFile().mkdirs();
				dataFile.createNewFile();
			} catch (IOException e) {
				plugin.getLogger().log(Level.SEVERE, "无法创建玩家数据文件", e);
			}
		}
		dataConfig = YamlConfiguration.loadConfiguration(dataFile);
	}

	/**
	 * 保存所有玩家数据
	 */
	public static void saveAllData() {
		for (UUID uuid : PlayerStatusManager.getAllStatuses().keySet()) {
			savePlayerData(uuid);
		}
	}

	/**
	 * 保存单个玩家数据
	 * @param uuid 玩家UUID
	 */
	public static synchronized void savePlayerData(UUID uuid) {
		if (!PlayerStatusManager.hasStatus(uuid)) {
			return;
		}

		PlayerStatus status = PlayerStatusManager.getStatus(uuid);
		String path = "players." + uuid.toString();

		dataConfig.set(path + ".receiveMessages", status.isReceiveMessages());
		dataConfig.set(path + ".canBeAttacked", status.isCanBeAttacked());

		try {
			dataConfig.save(dataFile);
		} catch (IOException e) {
			plugin.getLogger().log(Level.WARNING, "无法保存玩家数据: " + uuid, e);
		}
	}

	/**
	 * 加载单个玩家数据
	 * @param uuid 玩家UUID
	 */
	public static synchronized void loadPlayerData(UUID uuid) {
		String path = "players." + uuid.toString();

		if (!dataConfig.contains(path)) {
			return; // 没有该玩家的数据，使用默认值
		}

		if (!PlayerStatusManager.hasStatus(uuid)) {
			PlayerStatusManager.addStatus(uuid, new PlayerStatus());
		}

		PlayerStatus status = PlayerStatusManager.getStatus(uuid);

		// 加载消息接收设置
		if (dataConfig.contains(path + ".receiveMessages")) {
			status.setReceiveMessages(dataConfig.getBoolean(path + ".receiveMessages"));
		}

		// 加载被攻击权限设置
		if (dataConfig.contains(path + ".canBeAttacked")) {
			status.setCanBeAttacked(dataConfig.getBoolean(path + ".canBeAttacked"));
		}
	}

	/**
	 * 加载所有玩家数据
	 */
	public static void loadAllData() {
		if (!dataConfig.contains("players")) {
			return; // 没有玩家数据
		}

		for (String key : dataConfig.getConfigurationSection("players").getKeys(false)) {
			try {
				UUID uuid = UUID.fromString(key);
				loadPlayerData(uuid);
			} catch (IllegalArgumentException e) {
				plugin.getLogger().log(Level.WARNING, "无效的玩家UUID: " + key, e);
			}
		}
	}
}
