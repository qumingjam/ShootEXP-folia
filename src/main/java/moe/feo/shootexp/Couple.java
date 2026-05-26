package moe.feo.shootexp;

import moe.feo.shootexp.config.Config;
import moe.feo.shootexp.config.Language;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Couple表示一对情侣
 * 包括一个进攻方和防守方
 */
public class Couple {

	private final Player attacker;// 进攻方
	private Entity defender;// 防守方
	private int numOfAttack;// 攻击次数
	private final int period = 20;// 循环检查的间隔
	private Object timerTask = null;// 定时任务对象
	private final AtomicBoolean isRunning = new AtomicBoolean(true);// 任务运行标志

	/**
	 * 初始化一对情侣
	 * @param attacker
	 * 进攻方
	 * @param defender
	 * 防守方
	 */
	public Couple(Player attacker, Entity defender) {
		this.attacker = attacker;
		this.defender = defender;
		// 创建定时检查任务
		Runnable timerRunnable = createTimerRunnable();
		timerTask = FoliaScheduler.runAtEntityFixedRate(attacker, ShootEXP.getPlugin(ShootEXP.class), timerRunnable, 1, period);
	}

	/**
	 * 创建定时检查的Runnable
	 */
	private Runnable createTimerRunnable() {
		return new Runnable() {
			private int cacheNumOfAttack = 0;// 缓存的攻击次数
			private int attackTimeoutCount = 0;// 攻击超时计数

			@Override
			public void run() {
				if (!isRunning.get()) {
					return;
				}
				checkNum();
				checkTimes();
			}

			/**
			 * 检查玩家是否超时
			 */
			public void checkTimes() {
				if (numOfAttack > cacheNumOfAttack) {// 攻击次数增加
					attackTimeoutCount = 0;// 超时计数置零
				} else {
					attackTimeoutCount++;// 超时计数增加
				}
				int timeoutCount = Config.ATTACK_TIMEOUT.getInt() / period;
				if (attackTimeoutCount > timeoutCount) {// 超时
					stopTimer();
					CoupleManager.removeCouple(attacker.getUniqueId());// 从干活玩家名单中删除
				}
				cacheNumOfAttack = numOfAttack;
			}

			/**
			 * 检查攻击次数
			 */
			public void checkNum() {
				// 检查防守者是否有效
				if (defender == null || !defender.isValid()) {
					stopTimer();
					CoupleManager.removeCouple(attacker.getUniqueId());
					return;
				}
				
				if (!PlayerStatusManager.hasStatus(attacker.getUniqueId())) {// 如果不存在攻击者的状态数据
					PlayerStatusManager.addStatus(attacker.getUniqueId(), new PlayerStatus());// 放一个数据进去
				}
				if (numOfAttack >= PlayerStatusManager.getStatus(attacker.getUniqueId()).getRequiredAttackTimes()) {// 当攻击次数大于所需次数
					int EXPAmount = PlayerStatusManager.getStatus(attacker.getUniqueId()).ejaculation();// 射一次
					boolean isTranslate = false;
					String msg;
					String sound;
					if (EXPAmount != 0) {
						ItemStack EXPItem = new EXP(attacker.getName(), defender.getName(), EXPAmount).getEXPItem();
						// 防守者线程掉物
						FoliaScheduler.runAtEntity(defender, () -> {
							org.bukkit.entity.Item itemEntity = defender.getWorld().dropItem(defender.getLocation(), EXPItem);
							itemEntity.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
						});
						msg = Language.MESSAGES_SHOOT.getString().replace("%ATTACKER%", attacker.getName())
								.replace("%TIMES%", String.valueOf(numOfAttack)).replace("%AMOUNT%", String.valueOf(EXPAmount));
						sound = Config.SOUND_SHOOT.getString();
					} else {
						msg = Language.MESSAGES_SHOOT_NO_EXP.getString().replace("%ATTACKER%", attacker.getName())
								.replace("%TIMES%", String.valueOf(numOfAttack));
						sound = Config.SOUND_SHOOT_NO_EXP.getString();
					}
					if (defender instanceof Player) {
						msg = msg.replace("%DEFENDER%", defender.getName());
					} else {
						String defenderName = defender.getCustomName();
						// 没有自定义名称，显示可翻译字符串名称
						if (defenderName == null) {
							isTranslate = true;
						} else {// 有自定义名称，显示自定义名称
							msg = msg.replace("%DEFENDER%", defender.getCustomName());
						}
					}
					// 创建 final 副本用于 lambda 表达式
					final String finalMsg = msg;
					final String finalSound = sound;
					if (isTranslate) {
						String path = "entity.minecraft." + defender.getType().toString().toLowerCase();
						TextComponent component = Util.translateEntityComponent(finalMsg, "%DEFENDER%", path);
						if (Config.PRIVATE_MESSAGE.getBoolean()) {
							// 私聊模式：在各自实体线程中发送
							FoliaScheduler.runAtEntity(attacker, () -> attacker.spigot().sendMessage(component));
							if (defender instanceof Player) {
								FoliaScheduler.runAtEntity(defender, () -> ((Player) defender).spigot().sendMessage(component));
							}
						} else {
							// 广播模式：使用全局调度器
							FoliaScheduler.runGlobal(ShootEXP.getPlugin(ShootEXP.class), () -> Bukkit.spigot().broadcast(component));
						}
					} else {
						if (Config.PRIVATE_MESSAGE.getBoolean()) {
							// 私聊模式：在各自实体线程中发送
							FoliaScheduler.runAtEntity(attacker, () -> attacker.sendMessage(finalMsg));
							if (defender instanceof Player) {
								// 检查防守者是否接收消息
								UUID defenderUuid = ((Player) defender).getUniqueId();
								if (PlayerStatusManager.hasStatus(defenderUuid)) {
									PlayerStatus defenderStatus = PlayerStatusManager.getStatus(defenderUuid);
									if (defenderStatus.isReceiveMessages()) {
										FoliaScheduler.runAtEntity(defender, () -> defender.sendMessage(finalMsg));
									}
								} else {
									FoliaScheduler.runAtEntity(defender, () -> defender.sendMessage(finalMsg));
								}
							}
						} else {
							// 广播模式：向所有在线玩家发送，但尊重每个人的 receiveMessages 设置
							FoliaScheduler.runGlobal(ShootEXP.getPlugin(ShootEXP.class), () -> {
								for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
									UUID playerUuid = onlinePlayer.getUniqueId();
									// 检查玩家是否接收消息
									boolean shouldReceive = true; // 默认接收
									if (PlayerStatusManager.hasStatus(playerUuid)) {
										PlayerStatus playerStatus = PlayerStatusManager.getStatus(playerUuid);
										shouldReceive = playerStatus.isReceiveMessages();
									}
									if (shouldReceive) {
										onlinePlayer.sendMessage(finalMsg);
									}
								}
							});
						}
					}
					// 在攻击者所在区域线程播放声音
					FoliaScheduler.runAtEntity(attacker, () -> {
						attacker.getWorld().playSound(attacker.getLocation(), finalSound, SoundCategory.PLAYERS, 1, 1);
					});
					stopTimer();// 将定时器移除
					CoupleManager.removeCouple(attacker.getUniqueId());// 把这个对象从正在干活的列表中移除
				}
			}
		};
	}

	/**
	 * 停止定时器
	 */
	private void stopTimer() {
		isRunning.set(false);
		FoliaScheduler.cancelTask(timerTask);
		timerTask = null;
	}

	/**
	 * 设置防守方
	 * @param defender
	 * 防守方
	 */
	public void setDefender(Entity defender) {
		this.defender = defender;
	}

	/**
	 * 攻击一次
	 */
	public void attack() {
		numOfAttack++;// 攻击次数增加
	}
}