package moe.feo.shootexp;

import io.papermc.paper.threadedregions.scheduler.EntityScheduler;
import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.function.Consumer;

/**
 * Folia 调度器工具类
 * 直接使用 Paper API 编译期安全调用
 */
public class FoliaScheduler {

	/**
	 * 在实体所在的区域线程中执行任务
	 */
	public static void runAtEntity(Entity entity, Runnable runnable) {
		EntityScheduler scheduler = entity.getScheduler();
		scheduler.execute(ShootEXP.getPlugin(ShootEXP.class), runnable, null, 1L);
	}

	/**
	 * 在实体所在的区域线程中延迟执行任务
	 */
	public static void runAtEntityLater(Entity entity, Runnable runnable, long delayTicks) {
		EntityScheduler scheduler = entity.getScheduler();
		long actualDelay = delayTicks <= 0 ? 1 : delayTicks;
		scheduler.runDelayed(ShootEXP.getPlugin(ShootEXP.class), task -> runnable.run(), null, actualDelay);
	}

	/**
	 * 在实体所在的区域线程中重复执行任务
	 */
	public static Object runAtEntityFixedRate(Entity entity, Plugin plugin, Runnable runnable, long delayTicks, long periodTicks) {
		if (entity == null) {
			ShootEXP.getPlugin(ShootEXP.class).getLogger().warning("Cannot schedule task: entity is null");
			return null;
		}
		EntityScheduler scheduler = entity.getScheduler();
		long actualDelay = delayTicks <= 0 ? 1 : delayTicks;
		Consumer<ScheduledTask> consumer = task -> runnable.run();
		return scheduler.runAtFixedRate(plugin, consumer, null, actualDelay, periodTicks);
	}

	/**
	 * 在全局区域执行任务
	 */
	public static void runGlobal(Plugin plugin, Runnable runnable) {
		Bukkit.getGlobalRegionScheduler().execute(plugin, runnable);
	}

	/**
	 * 在全局区域延迟执行任务
	 */
	public static Object runGlobalLater(Plugin plugin, Runnable runnable, long delayTicks) {
		return Bukkit.getGlobalRegionScheduler().runDelayed(plugin, task -> runnable.run(), delayTicks);
	}

	/**
	 * 在全局区域重复执行任务
	 */
	public static Object runGlobalTimer(Plugin plugin, Runnable runnable, long delayTicks, long periodTicks) {
		long actualDelay = delayTicks <= 0 ? 1 : delayTicks;
		Consumer<ScheduledTask> consumer = task -> runnable.run();
		return Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, consumer, actualDelay, periodTicks);
	}

	/**
	 * 取消任务
	 */
	public static void cancelTask(Object task) {
		if (task instanceof ScheduledTask scheduledTask) {
			scheduledTask.cancel();
		}
	}
}
