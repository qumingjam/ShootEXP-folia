package moe.feo.shootexp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * 工具类
 */
public class Util {

    // 缓存已加载的实体类，避免重复反射
    private static final Map<String, Class<?>> entityClassCache = new ConcurrentHashMap<>();

    /**
     * 将占位符替换为可翻译字符串，模仿原版聊天的颜色与格式的运作方法
     * @param msg 原始消息
     * @param placeholder 占位符
     * @param path 可翻译字符串路径
     * @return 最终的 TextComponent
     */
    public static TextComponent translateEntityComponent(String msg, String placeholder, String path) {
        String[] normalMessages = msg.split(placeholder);
        TextComponent component = Component.empty();
        // 上一个组件的颜色与格式
        TextColor color = null;
        List<TextDecoration> decorations = new ArrayList<>();
        // 每一句的后面插入一个可翻译的被施法者组件
        for (int i = 0; i < normalMessages.length; i++) {
            String normalMessage = normalMessages[i];
            TextComponent normalMessageComponent = Component.text(normalMessage);
            // 用上一个组件为这个普通消息组件设置格式
            component = component.append(formatComponent(normalMessageComponent, color, decorations));
            // 如果已经遍历到最后一句，就不用再往后加组件了
            if (i == normalMessages.length - 1) {
                // 除非这条消息以占位符结尾...
                if (!msg.endsWith(placeholder)) {
                    break;
                }
            }
            // 处理颜色字符
            char[] chars = normalMessage.toCharArray();
            boolean isColor = false;
            for (char code : chars) {
                if (isColor) {
                    TextColor parsedColor = parseColorCode(code);
                    if (parsedColor != null) {
                        color = parsedColor;
                        // 原版的颜色字符会清掉后面字符的格式
                        decorations.clear();
                    } else {
                        TextDecoration decoration = parseDecorationCode(code);
                        if (decoration != null) {
                            decorations.add(decoration);
                        }
                    }
                    isColor = false;
                }
                if (code == '§') {
                    isColor = true;
                }
            }
            // 要替换上去的被施法者组件
            TranslatableComponent translatableComponent = Component.translatable(path);
            // 用之前的普通消息组件为这个可翻译字符串组件设置格式
            component = component.append(formatComponent(translatableComponent, color, decorations));
        }
        return component;
    }

    /**
     * 用给定的颜色和格式格式化组件
     * @param component 需要被格式化的组件
     * @param color 颜色
     * @param decorations 格式列表
     * @return 格式化后的组件
     */
    public static Component formatComponent(Component component, TextColor color, List<TextDecoration> decorations) {
        Component result = component;
        if (color != null) {
            result = result.color(color);
        }
        for (TextDecoration decoration : decorations) {
            result = result.decorate(decoration);
        }
        return result;
    }

    /**
     * 解析 Minecraft 颜色代码字符为 TextColor
     */
    private static TextColor parseColorCode(char code) {
        return switch (Character.toLowerCase(code)) {
            case '0' -> NamedTextColor.BLACK;
            case '1' -> NamedTextColor.DARK_BLUE;
            case '2' -> NamedTextColor.DARK_GREEN;
            case '3' -> NamedTextColor.DARK_AQUA;
            case '4' -> NamedTextColor.DARK_RED;
            case '5' -> NamedTextColor.DARK_PURPLE;
            case '6' -> NamedTextColor.GOLD;
            case '7' -> NamedTextColor.GRAY;
            case '8' -> NamedTextColor.DARK_GRAY;
            case '9' -> NamedTextColor.BLUE;
            case 'a' -> NamedTextColor.GREEN;
            case 'b' -> NamedTextColor.AQUA;
            case 'c' -> NamedTextColor.RED;
            case 'd' -> NamedTextColor.LIGHT_PURPLE;
            case 'e' -> NamedTextColor.YELLOW;
            case 'f' -> NamedTextColor.WHITE;
            default -> null;
        };
    }

    /**
     * 解析 Minecraft 格式代码字符为 TextDecoration
     */
    private static TextDecoration parseDecorationCode(char code) {
        return switch (Character.toLowerCase(code)) {
            case 'k' -> TextDecoration.OBFUSCATED;
            case 'l' -> TextDecoration.BOLD;
            case 'm' -> TextDecoration.STRIKETHROUGH;
            case 'n' -> TextDecoration.UNDERLINED;
            case 'o' -> TextDecoration.ITALIC;
            default -> null;
        };
    }

    /**
     * 获取给定范围内属于给定类型的最近的实体
     * @param self 需要排除的自己
     * @param range 范围
     * @param includes 实体的类型列表
     * @return 最近的实体
     */
    public static Entity getNearestEntity(Entity self, double range, List<String> includes) {
        World world = self.getWorld();
        Location location = self.getLocation();
        Collection<Entity> entityList = world.getNearbyEntities(location, range, range, range);
        Entity partner = null;
        List<Class<?>> classes = new ArrayList<>();
        // 实体类型
        for (String include : includes) {
            try {
                // 先从缓存中获取
                Class<?> clazz = entityClassCache.get(include);
                if (clazz == null) {
                    // 缓存中没有，通过反射加载
                    clazz = Class.forName("org.bukkit.entity." + include);
                    entityClassCache.put(include, clazz);
                }
                classes.add(clazz);
            } catch (ClassNotFoundException e) {
                Bukkit.getLogger().log(Level.SEVERE, "Illegal Entity type: " + include, e);
            }
        }
        // 遍历实体列表
        double partnerDistance = range;// 同伴最大距离
        for (Entity entity : entityList) {
            if (entity.equals(self)) {
                continue;
            }
            // 判断是否为配置文件中定义的实体类型
            boolean isDefinition = false;
            for (Class<?> clazz : classes) {
                // 如果partner是这种类型
                if (clazz.isInstance(entity)) {
                    isDefinition = true;
                    break;
                }
            }
            if (!isDefinition) {
                continue;
            }
            double cacheDistance = self.getLocation().distance(entity.getLocation());
            if (partner == null || cacheDistance < partnerDistance) {
                partner = entity;
                partnerDistance = cacheDistance;
            }
        }
        return partner;
    }

    /**
     * 清空实体类缓存
     */
    public static void clearEntityClassCache() {
        entityClassCache.clear();
    }
}
