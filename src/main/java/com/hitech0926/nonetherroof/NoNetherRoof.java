package com.hitech0926.nonetherroof;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.ChatColor;

import java.util.HashMap;
import java.util.Map;

public final class NoNetherRoof extends JavaPlugin implements Listener {

    public FileConfiguration config;
    public Map<String, Boolean> functionStates = new HashMap<>();

    @Override
    public void onEnable() {
        // 注册事件监听器
        Bukkit.getPluginManager().registerEvents(this, this);

        // 加载配置
        saveDefaultConfig();
        config = getConfig();

        // 初始化功能状态
        updateFunctionStates();
    }

    public void updateFunctionStates() {
        getLogger().info("Updating function states...");
        functionStates.put("back", config.getBoolean("back.enable"));
        getLogger().info("Function states updated successfully.");
    }

    @EventHandler
    public void onPlayerMoveBack(PlayerMoveEvent event) {
        if (!functionStates.get("back")) return;

        Player player = event.getPlayer();
        Location to = event.getTo();

        if (to == null || to.getWorld().getEnvironment() != World.Environment.NETHER) return;

        if (to.getBlockY() >= 127 && !player.hasPermission("nonetherroof.bypass")) {
            event.setCancelled(true);
            new BukkitRunnable() {
                @Override
                public void run() {
                    Location safeLocation = findSafeLocation(to);
                    if (safeLocation != null) {
                        player.teleport(safeLocation);
                        player.sendMessage(getMessage("succeesful"));
                    }
                }
            }.runTaskLater(this, 1L);
        }
    }

    private Location findSafeLocation(Location location) {
        World world = location.getWorld();
        int x = location.getBlockX();
        int z = location.getBlockZ();
        int maxY = config.getInt("back.max-y", 125);
        int minY = config.getInt("back.min-y", 5);
        int searchRange = config.getInt("back.range", 5);
        
        for (int y = maxY; y >= minY; y--) {
            Location checkLocation = new Location(world, x, y, z);
            if (isSafeLocation(checkLocation)) {
                return checkLocation;
            }
        }

        for (int offsetX = -searchRange; offsetX <= searchRange; offsetX++) {
            for (int offsetZ = -searchRange; offsetZ <= searchRange; offsetZ++) {
                for (int y = maxY; y >= minY; y--) {
                    Location checkLocation = new Location(world, x + offsetX, y, z + offsetZ);
                    if (isSafeLocation(checkLocation)) {
                        return checkLocation;
                    }
                }
            }
        }

        return null;
    }

    private boolean isSafeLocation(Location location) {
        Material block = location.getBlock().getType();
        return block != Material.LAVA && block != Material.WATER && block != Material.FIRE && location.getBlock().isPassable();
    }

    public String getMessage(String key) {
        String rawMessage = getConfig().getString("message." + key);
        if (rawMessage == null) {
            return ""; // 如果消息为空，返回空字符串
        }
        String formattedPrefix = formatMessage(getConfig().getString("message.prefix"));
        return String.format(formattedPrefix + rawMessage.replace("<prefix>", "%s"), formattedPrefix);
    }

    public String formatMessage(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}