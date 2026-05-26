package moe.feo.shootexp;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 这个类维护了一个玩家状态的map
 */
public class PlayerStatusManager {

	private static final Map<UUID, PlayerStatus> statusMap = new ConcurrentHashMap<>();

	/**
	 * 添加一个玩家状态
	 * @param uuid
	 * 玩家的UUID
	 * @param status
	 * 玩家的状态
	 */
	public static void addStatus(UUID uuid, PlayerStatus status) {
		statusMap.put(uuid, status);
	}

	/** 判断指定的uuid的玩家是否有状态
	 * @param uuid
	 * 玩家的UUID
	 * @return 玩家是否存在状态
	 */
	public static boolean hasStatus(UUID uuid) {
		return statusMap.containsKey(uuid);
	}

	/**
	 * 获取指定的uuid的玩家的状态
	 * @param uuid
	 * 玩家的UUID
	 * @return 玩家的状态
	 */

	public static PlayerStatus getStatus(UUID uuid) {
		return statusMap.get(uuid);
	}

	/**
	 * 获取所有玩家状态
	 * @return 所有玩家状态的 Map
	 */
	public static Map<UUID, PlayerStatus> getAllStatuses() {
		return statusMap;
	}
}
