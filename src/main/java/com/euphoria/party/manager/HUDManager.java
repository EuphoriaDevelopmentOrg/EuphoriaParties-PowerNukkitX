package com.euphoria.party.manager;

import cn.nukkit.Player;
import cn.nukkit.scheduler.Task;
import cn.nukkit.scheduler.TaskHandler;
import com.euphoria.party.EuphoriaPartyPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HUDManager {
    
    private final EuphoriaPartyPlugin plugin;
    private final Map<UUID, Boolean> coordinatesEnabled;
    private final Map<UUID, Boolean> compassEnabled;
    private TaskHandler hudTaskId = null;
    
    public HUDManager(EuphoriaPartyPlugin plugin) {
        this.plugin = plugin;
        this.coordinatesEnabled = new ConcurrentHashMap<>();
        this.compassEnabled = new ConcurrentHashMap<>();
    }
    
    public void toggleCoordinates(Player player) {
        UUID uuid = player.getUniqueId();
        boolean current = coordinatesEnabled.getOrDefault(uuid, 
            plugin.getConfig().getBoolean("hud.coordinates.default-enabled", true));
        coordinatesEnabled.put(uuid, !current);
        
        String message = !current ? 
            plugin.getMessage("coordinates-enabled") : 
            plugin.getMessage("coordinates-disabled");
        player.sendMessage(message);
    }
    
    public void toggleCompass(Player player) {
        UUID uuid = player.getUniqueId();
        boolean current = compassEnabled.getOrDefault(uuid, 
            plugin.getConfig().getBoolean("hud.compass.default-enabled", true));
        compassEnabled.put(uuid, !current);
        
        String message = !current ? 
            plugin.getMessage("compass-enabled") : 
            plugin.getMessage("compass-disabled");
        player.sendMessage(message);
    }
    
    public boolean isCoordinatesEnabled(UUID playerId) {
        return coordinatesEnabled.getOrDefault(playerId, 
            plugin.getConfig().getBoolean("hud.coordinates.default-enabled", true));
    }
    
    public boolean isCompassEnabled(UUID playerId) {
        return compassEnabled.getOrDefault(playerId, 
            plugin.getConfig().getBoolean("hud.compass.default-enabled", true));
    }
    
    public void startHUDTask() {
        int interval = plugin.getConfig().getInt("hud.coordinates.update-interval", 20);
        
        hudTaskId = plugin.getServer().getScheduler().scheduleRepeatingTask(new Task() {
            @Override
            public void onRun(int currentTick) {
                updateHUD();
            }
        }, interval);
    }
    
    public void stopHUDTask() {
        if (hudTaskId != null) {
            hudTaskId.cancel();
            hudTaskId = null;
        }
    }
    
    private void updateHUD() {
        // Cache format strings outside loop
        String coordFormat = plugin.getConfig().getString("hud.coordinates.format", 
            "§eX: §f{x} §eY: §f{y} §eZ: §f{z}");
        
        for (Player player : plugin.getServer().getOnlinePlayers().values()) {
            if (player == null || !player.isOnline()) {
                continue;
            }
            
            UUID playerId = player.getUniqueId();
            
            // Skip if player has both disabled (optimization)
            if (!isCoordinatesEnabled(playerId) && !isCompassEnabled(playerId)) {
                continue;
            }
            
            StringBuilder actionBar = new StringBuilder();
            
            // Add coordinates
            if (isCoordinatesEnabled(playerId)) {
                String coords = coordFormat
                    .replace("{x}", String.valueOf((int) player.getX()))
                    .replace("{y}", String.valueOf((int) player.getY()))
                    .replace("{z}", String.valueOf((int) player.getZ()));
                actionBar.append(coords);
            }
            
            // Add compass
            if (isCompassEnabled(playerId)) {
                if (actionBar.length() > 0) {
                    actionBar.append("  §7|  ");
                }
                actionBar.append(getDirectionText(player));
            }
            
            // Send action bar
            if (actionBar.length() > 0) {
                player.sendActionBar(actionBar.toString());
            }
        }
    }
    
    private String getDirectionText(Player player) {
        double yaw = player.getYaw();
        
        // Normalize yaw to 0-360
        while (yaw < 0) yaw += 360;
        while (yaw >= 360) yaw -= 360;
        
        String direction;
        if (yaw >= 337.5 || yaw < 22.5) {
            direction = plugin.getConfig().getString("hud.compass.directions.south", "§cS");
        } else if (yaw >= 22.5 && yaw < 67.5) {
            direction = plugin.getConfig().getString("hud.compass.directions.southwest", "§cSW");
        } else if (yaw >= 67.5 && yaw < 112.5) {
            direction = plugin.getConfig().getString("hud.compass.directions.west", "§cW");
        } else if (yaw >= 112.5 && yaw < 157.5) {
            direction = plugin.getConfig().getString("hud.compass.directions.northwest", "§cNW");
        } else if (yaw >= 157.5 && yaw < 202.5) {
            direction = plugin.getConfig().getString("hud.compass.directions.north", "§cN");
        } else if (yaw >= 202.5 && yaw < 247.5) {
            direction = plugin.getConfig().getString("hud.compass.directions.northeast", "§cNE");
        } else if (yaw >= 247.5 && yaw < 292.5) {
            direction = plugin.getConfig().getString("hud.compass.directions.east", "§cE");
        } else {
            direction = plugin.getConfig().getString("hud.compass.directions.southeast", "§cSE");
        }
        
        return "§eDir: " + direction;
    }
    
    public void removePlayer(UUID playerId) {
        coordinatesEnabled.remove(playerId);
        compassEnabled.remove(playerId);
    }
}
