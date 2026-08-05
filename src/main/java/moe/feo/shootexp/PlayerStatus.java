package moe.feo.shootexp;

import moe.feo.shootexp.config.Config;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 这个类表示一个玩家的状态
 */
public class PlayerStatus {

    private final AtomicInteger timesOfShoot = new AtomicInteger(0);// 发射经验次数
    private final AtomicInteger stock = new AtomicInteger(Config.MAX_STOCK.getInt());
    private volatile Object restoreShootTask = null;// 恢复发射次数任务
    private volatile Object restoreStockTask = null;// 恢复经验存量任务
    private final AtomicBoolean isShootTaskRunning = new AtomicBoolean(false);// 恢复发射次数任务运行标志
    private final AtomicBoolean isStockTaskRunning = new AtomicBoolean(false);// 恢复经验存量任务运行标志
    private volatile boolean receiveMessages = true;// 是否接收 ShootEXP 消息
    private volatile boolean canBeAttacked = true;// 是否可以被 ShootEXP 攻击
    /** 被尝试选中计数（即使禁止被攻击也累计），达到阈值后保护临时失效 */
    private volatile int attackedCount = 0;
    /** 保护失效到期时间戳（0 = 未失效） */
    private volatile long protectionBrokenUntil = 0;
    /** 被尝试选中多少次要失效 */
    private static final int ATTACK_THRESHOLD = 20;
    /** 失效后多久自动恢复（毫秒） */
    private static final long PROTECTION_BREAK_MS = 60_000L;

    /**
     * 获取一个新的恢复发射经验次数的Runnable
     * @return 一个新的Runnable
     */
    private Runnable getRestoreShootRunnable() {
        return () -> {
            // 只要发射经验次数小于0就不会恢复，否则恢复一次并判断是否恢复满
            if (timesOfShoot.get() <= 0 || restoreShoot()) {
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
            if (stock.get() >= Config.MAX_STOCK.getInt() || restoreStock()) {
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
        this.timesOfShoot.set(times);
    }

    /**
     * 获取射出次数
     * @return 射出次数
     */
    public int getTimesOfShoot() {
        return this.timesOfShoot.get();
    }

    /**
     * 设置经验存量
     * @param stock
     * 经验存量
     */
    public void setStock(int stock) {
        this.stock.set(stock);
    }

    /**
     * 获取经验存量
     * @return 经验存量
     */
    public int getStock() {
        return this.stock.get();
    }

    /**
     * 获取下次成功施法所需的攻击次数
     * @return 所需的蹲起次数
     */
    public int getRequiredAttackTimes() {
        Expression e = new ExpressionBuilder(Config.REQUIRED_ATTACK_TIMES.getString())
                .variables("SHOOT", "STOCK", "MAXSTOCK")
                .build()
                .setVariable("SHOOT", timesOfShoot.get())
                .setVariable("STOCK", stock.get())
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
                .setVariable("SHOOT", timesOfShoot.get())
                .setVariable("STOCK", stock.get())
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
        if (stock.get() > 0) {// 经验存量大于0，开始计算射出量
            amount = getShootAmount();
        }
        stock.addAndGet(-amount);
        timesOfShoot.incrementAndGet();
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
        timesOfShoot.addAndGet(-Config.RESTORE_SHOOT_AMOUNT.getInt());
        // 检查属性是否合法
        if (timesOfShoot.get() < 0) {
            timesOfShoot.set(0);
        }
        return timesOfShoot.get() == 0;
    }

    /**
     * 恢复一次指定次数的射出次数，允许已射出次数为负数
     * @param times
     * 恢复的射出次数
     * @return 是否恢复满
     */
    public boolean restoreShoot(int times) {
        timesOfShoot.addAndGet(-times);
        return timesOfShoot.get() <= 0;
    }

    /**
     * 将射出次数清零
     */
    public void restoreShootFull() {
        if (timesOfShoot.get() > 0) {
            timesOfShoot.set(0);
        }
    }

    /**
     * 恢复一次经验存量
     * @return 是否恢复满
     */
    public boolean restoreStock() {
        stock.addAndGet(Config.RESTORE_STOCK_AMOUNT.getInt());
        // 检查属性是否合法
        int max = Config.MAX_STOCK.getInt();
        if (stock.get() > max) {
            stock.set(max);
        }
        return stock.get() == max;
    }

    /**
     * 恢复一次指定数量的经验存量，并允许数量超过最大值
     * @param amount
     * 恢复的经验存量
     * @return 是否恢复满
     */
    public boolean restoreStock(int amount) {
        stock.addAndGet(amount);
        return stock.get() >= Config.MAX_STOCK.getInt();
    }

    /**
     * 恢复满玩家经验存量
     */
    public void restoreStockFull() {
        if (stock.get() < Config.MAX_STOCK.getInt()) {
            stock.set(Config.MAX_STOCK.getInt());
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
    public boolean isReceivingMessages() {
        return receiveMessages;
    }

    /**
     * 设置是否可被攻击
     * @param canBeAttacked 是否可被攻击
     */
    public void setCanBeAttacked(boolean canBeAttacked) {
        this.canBeAttacked = canBeAttacked;
        // 手动切换时重置保护失效状态和计数
        this.attackedCount = 0;
        this.protectionBrokenUntil = 0;
    }

    /**
     * 获取是否可被攻击
     * @return 是否可被攻击
     */
    public boolean canBeAttacked() {
        return canBeAttacked;
    }

    /**
     * 被尝试选中（即使禁止被攻击、攻击被阻止也计数）。
     * 达到阈值后保护临时失效，变为可被攻击。
     */
    public void onAttemptedAttack() {
        if (canBeAttacked) return;
        attackedCount++;
        if (attackedCount >= ATTACK_THRESHOLD) {
            canBeAttacked = true;
            protectionBrokenUntil = System.currentTimeMillis() + PROTECTION_BREAK_MS;
        }
    }

    /** 检查保护是否到期，到期自动恢复禁止并重置计数 */
    public void checkAndRestoreProtection() {
        if (protectionBrokenUntil > 0 && System.currentTimeMillis() >= protectionBrokenUntil) {
            canBeAttacked = false;
            attackedCount = 0;
            protectionBrokenUntil = 0;
        }
    }

    public int getAttackedCount() {
        return attackedCount;
    }

    public boolean isProtectionBroken() {
        return protectionBrokenUntil > 0;
    }
}
