package moe.feo.shootexp;

import moe.feo.shootexp.config.Config;
import moe.feo.shootexp.config.Language;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * 插件主类
 */
public class ShootEXP extends JavaPlugin {

	@Override
	public void onEnable() {
		saveDefaultConfig();
		Config.load();
		Language.saveDefault();
		Language.load();
		
		// 初始化数据管理器并加载玩家数据
		DataManager.init();
		DataManager.loadAllData();
		
		getServer().getPluginManager().registerEvents(new AttackListener(), this);
		getServer().getPluginManager().registerEvents(new EatListener(), this);
		getServer().getPluginManager().registerEvents(new SettingsGUI(), this);
		this.getCommand("shootexp").setExecutor(Commands.getInstance());
		this.getCommand("shootexp").setTabCompleter(Commands.getInstance());
	}

	@Override
	public void onDisable() {
		// 保存所有玩家数据
		DataManager.saveAllData();
	}
}
