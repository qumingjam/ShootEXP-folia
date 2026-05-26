package moe.feo.shootexp;

import moe.feo.shootexp.config.Config;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 这个类表示一个玩家的状态
 */
public class PlayerStatus {

	private int timesOfShoot = 0;// 发射经验次数
	private int stock = Config.MAX_STOCK.getInt();
	private Object restoreShootTask = null;// 恢复发射次数任务
	private Object restoreStockTask = null;// 恢复经验存量任务
	private final AtomicBoolean isShootTaskRunning = new AtomicBoolean(false);// 恢复发射次数任务运行标志
	private final AtomicBoolean isStockTaskRunning = new AtomicBoolean(false);// 恢复经验存量任务运行标志
	private boolean receiveMessages = true;// 是否接收 ShootEXP 消息
	private boolean canBeAttacked = true;// 是否可以被 ShootEXP 攻击

	/**
	 * 获取一个新的恢复发射经验次数的Runnable
	 * @return 一个新的Runnable
	 */
	private Runnable getRestoreShootRunnable() {
		return () -> {
			// 只要发射经验次数小于0就不会恢复，否则恢复一次并判断是否恢复满
			if (timesOfShoot <= 0 || restoreShoot()) {
				isShootTaskRunning.set(false);
				FoliaScheduler.cancelTask(restoreShootTask);//如果恢复满则退出定时器
				restoreShootTask = null;
			}
		};
	}

	/**
	 * 获取一个新的恢复经验存量的Runnable
	 * @return 一个新的Runnable
	 */
	private Runnable getRestoreStockRunnable() {
		return () -> {
			// 只要存量大于设定值就不会恢复，否则恢复一次并判断是否恢复满
			if (stock >= Config.MAX_STOCK.getInt() || restoreStock()) {
				isStockTaskRunning.set(false);
				FoliaScheduler.cancelTask(restoreStockTask);//如果恢复满则退出定时器
				restoreStockTask = null;
			}
		};
	}

	/**
	 * 设置射出次数
	 * @param times
	 * 射出次数
	 */
	public void setTimesOfShoot(int times) {
		this.timesOfShoot = times;
	}

	/**
	 * 获取射出次数
	 * @return 射出次数
	 */
	public int getTimesOfShoot() {
		return this.timesOfShoot;
	}

	/**
	 * 设置经验存量
	 * @param stock
	 * 经验存量
	 */
	public void setStock(int stock) {
		this.stock = stock;
	}

	/**
	 * 获取经验存量
	 * @return 经验存量
	 */
	public int getStock() {
		return this.stock;
	}

	/**
	 * 获取下次成功施法所需的攻击次数
	 * @return 所需的蹲起次数
	 */
	public int getRequiredAttackTimes() {
		Expression e = new ExpressionBuilder(Config.REQUIRED_ATTACK_TIMES.getString())
				.variables("SHOOT", "STOCK", "MAXSTOCK")
				.build()
				.setVariable("SHOOT", timesOfShoot)
				.setVariable("STOCK", stock)
				.setVariable("MAXSTOCK", Config.MAX_STOCK.getInt());
		double result = e.evaluate();
		return (int) result;
	}

	/**
	 * 获取下次施法成功时射出的经验量
	 * @return 射出的经验量
	 */
	public int getShootAmount() {
		Expression e = new ExpressionBuilder(Config.SHOOT_AMOUNT.getString())
				.variables("SHOOT", "STOCK", "MAXSTOCK")
				.build()
				.setVariable("SHOOT", timesOfShoot)
				.setVariable("STOCK", stock)
				.setVariable("MAXSTOCK", Config.MAX_STOCK.getInt());
		double result = e.evaluate();
		return (int) result;
	}

	/**
	 * 射一次，注意这不是真的射了，而是让玩家在数据上射了一次
	 * @return 射出的经验量
	 */
	public int ejaculation() {
		int amount = 0;
		if (stock > 0) {// 经验存量大于0，开始计算射出量
			amount = getShootAmount();
		}
		stock = stock - amount;
		timesOfShoot++;
		// 使用 compareAndSet 避免竞态条件
		if (isShootTaskRunning.compareAndSet(false, true)) {
			int period = Config.RESTORE_SHOOT_PERIOD.getInt();
			restoreShootTask = FoliaScheduler.runGlobalTimer(ShootEXP.getPlugin(ShootEXP.class), getRestoreShootRunnable(), period, period);
		}
		if (isStockTaskRunning.compareAndSet(false, true)) {
			int period = Config.RESTORE_STOCK_PERIOD.getInt();
			restoreStockTask = FoliaScheduler.runGlobalTimer(ShootEXP.getPlugin(ShootEXP.class), getRestoreStockRunnable(), period, period);
		}
		return amount;
	}

	/**
	 * 恢复一次射出次数
	 * @return 是否恢复满
	 */
	public boolean restoreShoot() {
		timesOfShoot = timesOfShoot - Config.RESTORE_SHOOT_AMOUNT.getInt();
		// 检查属性是否合法
		if (timesOfShoot < 0) {
			timesOfShoot = 0;
		}
		return timesOfShoot == 0;
	}

	/**
	 * 恢复一次指定次数的射出次数，允许已射出次数为负数
	 * @param times
	 * 恢复的射出次数
	 * @return 是否恢复满
	 */
	public boolean restoreShoot(int times) {
		timesOfShoot = timesOfShoot - times;
		return timesOfShoot <= 0;
	}

	/**
	 * 将射出次数清零
	 */
	public void restoreShootFull() {
		if (timesOfShoot > 0) {
			timesOfShoot = 0;
		}
	}

	/**
	 * 恢复一次经验存量
	 * @return 是否恢复满
	 */
	public boolean restoreStock() {
		stock = stock + Config.RESTORE_STOCK_AMOUNT.getInt();
		// 检查属性是否合法
		int max = Config.MAX_STOCK.getInt();
		if (stock > max) {
			stock = max;
		}
		return stock == max;
	}

	/**
	 * 恢复一次指定数量的经验存量，并允许数量超过最大值
	 * @param amount
	 * 恢复的经验存量
	 * @return 是否恢复满
	 */
	public boolean restoreStock(int amount) {
		stock = stock + amount;
		return stock >= Config.MAX_STOCK.getInt();
	}

	/**
	 * 恢复满玩家经验存量
	 */
	public void restoreStockFull() {
		if (stock < Config.MAX_STOCK.getInt()) {
			stock = Config.MAX_STOCK.getInt();
		}
	}

	/**
	 * 设置是否接收消息
	 * @param receiveMessages 是否接收消息
	 */
	public void setReceiveMessages(boolean receiveMessages) {
		this.receiveMessages = receiveMessages;
	}

	/**
	 * 获取是否接收消息
	 * @return 是否接收消息
	 */
	public boolean isReceiveMessages() {
		return receiveMessages;
	}

	/**
	 * 设置是否可被攻击
	 * @param canBeAttacked 是否可被攻击
	 */
	public void setCanBeAttacked(boolean canBeAttacked) {
		this.canBeAttacked = canBeAttacked;
	}

	/**
	 * 获取是否可被攻击
	 * @return 是否可被攻击
	 */
	public boolean isCanBeAttacked() {
		return canBeAttacked;
	}
}