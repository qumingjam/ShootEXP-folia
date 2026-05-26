package moe.feo.shootexp;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.Method;

/**
 * Folia 兼容的调度器工具类
 * 使用 Folia 官方的 EntityScheduler API（通过反射调用）
 */
public class FoliaScheduler {

	private static Boolean isFolia = null;

	/**
	 * 检测是否在 Folia 环境中
	 */
	public static boolean isFolia() {
		if (isFolia == null) {
			try {
				Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
				isFolia = true;
			} catch (ClassNotFoundException e) {
				isFolia = false;
			}
		}
		return isFolia;
	}

	/**
	 * 在实体所在的区域线程中执行任务
	 * @param entity 实体
	 * @param runnable 要执行的任务
	 */
	public static void runAtEntity(Entity entity, Runnable runnable) {
		if (isFolia()) {
			try {
				// 使用反射调用 entity.getScheduler().execute()
				Method getSchedulerMethod = entity.getClass().getMethod("getScheduler");
				Object scheduler = getSchedulerMethod.invoke(entity);
				Method executeMethod = scheduler.getClass().getMethod("execute", Plugin.class, Runnable.class, Runnable.class, long.class);
				executeMethod.invoke(scheduler, ShootEXP.getPlugin(ShootEXP.class), runnable, null, 0L);
			} catch (Exception e) {
				ShootEXP.getPlugin(ShootEXP.class).getLogger().warning("Failed to use EntityScheduler: " + e.getMessage());
			}
		} else {
			// 传统 Bukkit 服务器
			if (Bukkit.isPrimaryThread()) {
				runnable.run();
			} else {
				Bukkit.getScheduler().runTask(ShootEXP.getPlugin(ShootEXP.class), runnable);
			}
		}
	}

	/**
	 * 在实体所在的区域线程中延迟执行任务
	 * @param entity 实体
	 * @param runnable 要执行的任务
	 * @param delayTicks 延迟 tick 数
	 */
	public static void runAtEntityLater(Entity entity, Runnable runnable, long delayTicks) {
		if (isFolia()) {
			try {
				// 使用反射调用 entity.getScheduler().runDelayed()
				Method getSchedulerMethod = entity.getClass().getMethod("getScheduler");
				Object scheduler = getSchedulerMethod.invoke(entity);
				// 尝试不同的方法签名
				Method runDelayedMethod = null;
				try {
					runDelayedMethod = scheduler.getClass().getMethod("runDelayed", Plugin.class, java.util.function.Consumer.class, Runnable.class, long.class);
					java.util.function.Consumer<?> consumer = (task) -> runnable.run();
					runDelayedMethod.invoke(scheduler, ShootEXP.getPlugin(ShootEXP.class), consumer, null, delayTicks);
				} catch (NoSuchMethodException e1) {
					// 备选：使用 Runnable 版本
					runDelayedMethod = scheduler.getClass().getMethod("runDelayed", Plugin.class, Runnable.class, Runnable.class, long.class);
					runDelayedMethod.invoke(scheduler, ShootEXP.getPlugin(ShootEXP.class), runnable, null, delayTicks);
				}
			} catch (Exception e) {
				ShootEXP.getPlugin(ShootEXP.class).getLogger().warning("Failed to use EntityScheduler.runDelayed: " + e.getMessage());
			}
		} else {
			Bukkit.getScheduler().runTaskLater(ShootEXP.getPlugin(ShootEXP.class), runnable, delayTicks);
		}
	}

	/**
	 * 在实体所在的区域线程中重复执行任务
	 * @param entity 实体
	 * @param plugin 插件实例
	 * @param runnable 要执行的任务
	 * @param delayTicks 初始延迟 tick 数（必须 > 0）
	 * @param periodTicks 重复间隔 tick 数
	 * @return ScheduledTask 对象，可用于取消任务
	 */
	public static Object runAtEntityFixedRate(Entity entity, Plugin plugin, Runnable runnable, long delayTicks, long periodTicks) {
		if (entity == null) {
			ShootEXP.getPlugin(ShootEXP.class).getLogger().warning("Cannot schedule task: entity is null");
			return null;
		}
		
		if (isFolia()) {
			try {
				// 使用反射调用 entity.getScheduler().runAtFixedRate()
				Method getSchedulerMethod = entity.getClass().getMethod("getScheduler");
				Object scheduler = getSchedulerMethod.invoke(entity);
				
				// Folia 要求 delayTicks 必须 > 0，如果为 0 则设为 1
				long actualDelay = delayTicks <= 0 ? 1 : delayTicks;
				
				// 尝试标准 Folia API: runAtFixedRate(Plugin, Consumer, Runnable, long, long)
				try {
					Method runAtFixedRateMethod = scheduler.getClass().getMethod("runAtFixedRate", 
						Plugin.class, 
						java.util.function.Consumer.class, 
						Runnable.class, 
						long.class, 
						long.class);
					
					// 创建 Consumer
					java.util.function.Consumer<?> consumer = (task) -> runnable.run();
					
					return runAtFixedRateMethod.invoke(scheduler, 
						plugin, 
						consumer, 
						null, 
						actualDelay, 
						periodTicks);
				} catch (NoSuchMethodException e1) {
					// 备选：使用 Runnable 版本
					Method runAtFixedRateMethod = scheduler.getClass().getMethod("runAtFixedRate", 
						Plugin.class, 
						Runnable.class, 
						Runnable.class, 
						long.class, 
						long.class);
					
					return runAtFixedRateMethod.invoke(scheduler, 
						plugin, 
						runnable, 
						null, 
						actualDelay, 
						periodTicks);
				}
			} catch (Exception e) {
				ShootEXP.getPlugin(ShootEXP.class).getLogger().severe("Failed to use EntityScheduler.runAtFixedRate: " + e.getClass().getName() + " - " + e.getMessage());
				e.printStackTrace();
				return null;
			}
		} else {
			return Bukkit.getScheduler().runTaskTimer(plugin, runnable, delayTicks, periodTicks);
		}
	}

	/**
	 * 在全局区域执行任务（不依赖特定实体）
	 * @param plugin 插件实例
	 * @param runnable 要执行的任务
	 */
	public static void runGlobal(Plugin plugin, Runnable runnable) {
		if (isFolia()) {
			try {
				// 使用反射调用 Bukkit.getGlobalRegionScheduler().execute()
				Method getGlobalRegionSchedulerMethod = Bukkit.class.getMethod("getGlobalRegionScheduler");
				Object globalScheduler = getGlobalRegionSchedulerMethod.invoke(null);
				Method executeMethod = globalScheduler.getClass().getMethod("execute", Plugin.class, Runnable.class);
				executeMethod.invoke(globalScheduler, plugin, runnable);
			} catch (Exception e) {
				ShootEXP.getPlugin(ShootEXP.class).getLogger().warning("Failed to use GlobalRegionScheduler: " + e.getMessage());
			}
		} else {
			if (Bukkit.isPrimaryThread()) {
				runnable.run();
			} else {
				Bukkit.getScheduler().runTask(plugin, runnable);
			}
		}
	}

	/**
	 * 在全局区域延迟执行任务
	 * @param plugin 插件实例
	 * @param runnable 要执行的任务
	 * @param delayTicks 延迟 tick 数
	 * @return ScheduledTask 对象，可用于取消任务
	 */
	public static Object runGlobalLater(Plugin plugin, Runnable runnable, long delayTicks) {
		if (isFolia()) {
			try {
				Method getGlobalRegionSchedulerMethod = Bukkit.class.getMethod("getGlobalRegionScheduler");
				Object globalScheduler = getGlobalRegionSchedulerMethod.invoke(null);
				Method runDelayedMethod = globalScheduler.getClass().getMethod("runDelayed", Plugin.class, Runnable.class, long.class);
				return runDelayedMethod.invoke(globalScheduler, plugin, runnable, delayTicks);
			} catch (Exception e) {
				ShootEXP.getPlugin(ShootEXP.class).getLogger().warning("Failed to use GlobalRegionScheduler.runDelayed: " + e.getMessage());
				return null;
			}
		} else {
			return Bukkit.getScheduler().runTaskLater(plugin, runnable, delayTicks);
		}
	}

	/**
	 * 在全局区域重复执行任务
	 * @param plugin 插件实例
	 * @param runnable 要执行的任务
	 * @param delayTicks 初始延迟 tick 数（必须 > 0）
	 * @param periodTicks 重复间隔 tick 数
	 * @return ScheduledTask 对象，可用于取消任务
	 */
	public static Object runGlobalTimer(Plugin plugin, Runnable runnable, long delayTicks, long periodTicks) {
		if (isFolia()) {
			try {
				Method getGlobalRegionSchedulerMethod = Bukkit.class.getMethod("getGlobalRegionScheduler");
				Object globalScheduler = getGlobalRegionSchedulerMethod.invoke(null);
				
				// Folia 要求 delayTicks 必须 > 0
				long actualDelay = delayTicks <= 0 ? 1 : delayTicks;
				
				// 尝试不同的方法签名
				try {
					// 尝试: runAtFixedRate(Plugin, Consumer, Runnable, long, long)
					Method runAtFixedRateMethod = globalScheduler.getClass().getMethod("runAtFixedRate", 
						Plugin.class, java.util.function.Consumer.class, Runnable.class, long.class, long.class);
					
					java.util.function.Consumer<?> consumer = (task) -> runnable.run();
					return runAtFixedRateMethod.invoke(globalScheduler, plugin, consumer, null, actualDelay, periodTicks);
				} catch (NoSuchMethodException e1) {
					// 备选: runAtFixedRate(Plugin, Runnable, long, long)
					// 这是许多 Folia 分支的标准签名
					Method runAtFixedRateMethod = globalScheduler.getClass().getMethod("runAtFixedRate", 
						Plugin.class, Runnable.class, long.class, long.class);
					return runAtFixedRateMethod.invoke(globalScheduler, plugin, runnable, actualDelay, periodTicks);
				}
			} catch (NoSuchMethodException e) {
				// 两种方法签名都不存在，记录错误
				ShootEXP.getPlugin(ShootEXP.class).getLogger().warning("Failed to use GlobalRegionScheduler.runAtFixedRate: " + e.getMessage());
				return null;
			} catch (Exception e) {
				// 其他异常（如获取调度器失败、调用失败等）
				ShootEXP.getPlugin(ShootEXP.class).getLogger().warning("Failed to use GlobalRegionScheduler.runAtFixedRate: " + e.getMessage());
				return null;
			}
		} else {
			return Bukkit.getScheduler().runTaskTimer(plugin, runnable, delayTicks, periodTicks);
		}
	}

	/**
	 * 取消任务
	 * @param task 任务对象（ScheduledTask 或 BukkitTask）
	 */
	public static void cancelTask(Object task) {
		if (task == null) {
			return;
		}
		try {
			Method cancelMethod = task.getClass().getMethod("cancel");
			cancelMethod.setAccessible(true); // 设置可访问，解决反射权限问题
			cancelMethod.invoke(task);
		} catch (Exception e) {
			// 静默失败，避免日志刷屏
		}
	}
}