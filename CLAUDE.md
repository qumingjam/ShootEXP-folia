# ShootEXP — 经验射击插件

## 项目信息
- **技术栈**: Java 21, Maven, Paper API 1.21.4
- **打包**: `mvn clean package` → `target/ShootEXP-folia-<version>.jar`
- **Folia 兼容**: 是（folia 分支）
- **GitHub**: https://github.com/qumingjam/ShootEXP-folia

## 功能
- 玩家通过蹲起交互射出"经验"物品
- 其他玩家拾取获得经验值
- Folia 兼容版本

## 共享规则
继承自 `YinwuForge/agents.md`（适用于所有 Yinwu 插件）：

### 调度规范（Folia）
- ✅ 使用 `RegionScheduler` / `GlobalRegionScheduler` / `EntityScheduler`
- ❌ 禁止 `Bukkit.getScheduler()`、`runTask`、`runTaskAsynchronously`
- ❌ 初始延迟禁止为 `0L`（必须 ≥ `1L`）

### 代码风格
- 注释极简，无废话
- 仅使用 Paper / Folia API，禁止 NMS
