package com.euphoria.party.util;

import com.euphoria.party.EuphoriaPartyPlugin;
import com.euphoria.party.model.Party;

import java.util.HashMap;
import java.util.Map;

/**
 * Health check utility to monitor plugin status and performance
 */
public class HealthCheck {
    
    private final EuphoriaPartyPlugin plugin;
    
    public HealthCheck(EuphoriaPartyPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Get comprehensive health status
     */
    public Map<String, Object> getHealthStatus() {
        Map<String, Object> status = new HashMap<>();
        
        // Basic stats
        status.put("status", "healthy");
        status.put("activeParties", plugin.getPartyManager().getAllParties().size());
        status.put("onlinePlayers", plugin.getServer().getOnlinePlayers().size());
        
        // Memory stats
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory() / (1024 * 1024); // MB
        long freeMemory = runtime.freeMemory() / (1024 * 1024); // MB
        long usedMemory = totalMemory - freeMemory;
        
        status.put("memoryUsedMB", usedMemory);
        status.put("memoryTotalMB", totalMemory);
        status.put("memoryPercentage", (int)((usedMemory * 100.0) / totalMemory));
        
        // Party stats
        int totalMembers = 0;
        int partiesWithHome = 0;
        int onlineMembers = 0;
        
        for (Party party : plugin.getPartyManager().getAllParties()) {
            totalMembers += party.getMemberCount();
            if (party.hasHome()) {
                partiesWithHome++;
            }
            
            for (java.util.UUID memberId : party.getMembers()) {
                cn.nukkit.Player player = plugin.getServer().getPlayer(memberId).orElse(null);
                if (player != null && player.isOnline()) {
                    onlineMembers++;
                }
            }
        }
        
        status.put("totalPartyMembers", totalMembers);
        status.put("partiesWithHome", partiesWithHome);
        status.put("onlinePartyMembers", onlineMembers);
        
        // Performance indicators
        status.put("cacheEnabled", plugin.getConfig().getBoolean("performance.cache-party-lookups", true));
        status.put("asyncSaveEnabled", plugin.getConfig().getBoolean("performance.async-save", true));
        status.put("optimizeMarkersEnabled", plugin.getConfig().getBoolean("performance.optimize-markers", true));
        
        return status;
    }
    
    /**
     * Format health status as a readable string
     */
    public String formatHealthStatus() {
        Map<String, Object> status = getHealthStatus();
        StringBuilder sb = new StringBuilder();
        
        sb.append("§8========== §6Plugin Health Status §8==========\n");
        sb.append("§eStatus: §a").append(status.get("status")).append("\n");
        sb.append("§eActive Parties: §f").append(status.get("activeParties")).append("\n");
        sb.append("§eOnline Players: §f").append(status.get("onlinePlayers")).append("\n");
        sb.append("§eParty Members (Online/Total): §f").append(status.get("onlinePartyMembers"))
            .append("§7/§f").append(status.get("totalPartyMembers")).append("\n");
        sb.append("§eParties with Home: §f").append(status.get("partiesWithHome")).append("\n");
        sb.append("§eMemory Usage: §f").append(status.get("memoryUsedMB"))
            .append("§7/§f").append(status.get("memoryTotalMB"))
            .append(" MB §7(§f").append(status.get("memoryPercentage")).append("%§7)\n");
        sb.append("§eCache Enabled: ").append((boolean)status.get("cacheEnabled") ? "§a✓" : "§c✗").append("\n");
        sb.append("§eAsync Save: ").append((boolean)status.get("asyncSaveEnabled") ? "§a✓" : "§c✗").append("\n");
        sb.append("§eOptimized Markers: ").append((boolean)status.get("optimizeMarkersEnabled") ? "§a✓" : "§c✗").append("\n");
        sb.append("§8================================");
        
        return sb.toString();
    }
    
    /**
     * Check if system is healthy (enough resources, no critical errors)
     */
    public boolean isHealthy() {
        Map<String, Object> status = getHealthStatus();
        
        // Check memory usage
        int memoryPercent = (int) status.get("memoryPercentage");
        if (memoryPercent > 90) {
            plugin.getLogger().warning("High memory usage detected: " + memoryPercent + "%");
            return false;
        }
        
        return true;
    }
}
