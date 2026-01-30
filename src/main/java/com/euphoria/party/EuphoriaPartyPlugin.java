package com.euphoria.party;

import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.TextFormat;
import com.euphoria.party.command.CompassCommand;
import com.euphoria.party.command.CoordinatesCommand;
import com.euphoria.party.command.PartyAdminCommand;
import com.euphoria.party.command.PartyCommand;
import com.euphoria.party.integration.PartyPlaceholders;
import com.euphoria.party.listener.PlayerListener;
import com.euphoria.party.manager.HUDManager;
import com.euphoria.party.manager.PartyManager;
import com.euphoria.party.manager.PartyBuffManager;
import com.euphoria.party.manager.PartyAchievementManager;
import com.euphoria.party.manager.PartyScoreboardManager;
import com.euphoria.party.manager.PartyLeaderboardManager;

public class EuphoriaPartyPlugin extends PluginBase {
    
    private static EuphoriaPartyPlugin instance;
    private PartyManager partyManager;
    private HUDManager hudManager;
    private PartyBuffManager buffManager;
    private PartyAchievementManager achievementManager;
    private PartyScoreboardManager scoreboardManager;
    private PartyLeaderboardManager leaderboardManager;
    private PartyPlaceholders placeholders;
    
    @Override
    public void onLoad() {
        instance = this;
    }
    
    @Override
    public void onEnable() {
        // Save default config
        this.saveDefaultConfig();
        
        // Initialize managers
        this.partyManager = new PartyManager(this);
        this.hudManager = new HUDManager(this);
        this.buffManager = new PartyBuffManager(this);
        this.achievementManager = new PartyAchievementManager(this);
        this.scoreboardManager = new PartyScoreboardManager(this);
        this.leaderboardManager = new PartyLeaderboardManager(this);
        
        // Register PlaceholderAPI if present
        if (this.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            this.placeholders = new PartyPlaceholders(this);
            this.placeholders.register();
            this.getLogger().info("PlaceholderAPI integration enabled");
        }
        
        // Register commands
        this.getServer().getCommandMap().register("party", new PartyCommand(this));
        this.getServer().getCommandMap().register("partyadmin", new PartyAdminCommand(this));
        this.getServer().getCommandMap().register("coordinates", new CoordinatesCommand(this));
        this.getServer().getCommandMap().register("compass", new CompassCommand(this));
        
        // Register listeners
        this.getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        this.getServer().getPluginManager().registerEvents(new com.euphoria.party.listener.PartyEventListener(this), this);
        this.getServer().getPluginManager().registerEvents(new com.euphoria.party.listener.PartyRespawnListener(this), this);
        this.getServer().getPluginManager().registerEvents(new com.euphoria.party.listener.PartyTabListListener(this), this);
        
        // Start tasks
        this.hudManager.startHUDTask();
        this.partyManager.startMarkerTask();
        this.partyManager.startDistanceCheckTask();
        this.partyManager.startPlayTimeTracker();
        this.buffManager.startBuffTask();
        this.scoreboardManager.startScoreboardTask();
        
        // Auto-save task (every 5 minutes) - only saves if there are players online and parties exist
        this.getServer().getScheduler().scheduleDelayedRepeatingTask(this, () -> {
            // Clean up expired invites
            partyManager.cleanupExpiredInvites();
            
            // Save if needed
            if (this.getServer().getOnlinePlayers().size() > 0 && partyManager.hasParties()) {
                partyManager.saveAllParties();
            }
        }, 6000, 6000); // 6000 ticks = 5 minutes
        
        this.getLogger().info("Plugin enabled successfully");
    }
    
    @Override
    public void onDisable() {
        this.getLogger().info("Disabling plugin gracefully...");
        
        try {
            // Save all party data synchronously (plugin is being disabled)
            if (partyManager != null) {
                partyManager.saveAllParties(true); // Force synchronous save
            }
            
            // Cancel tasks
            if (hudManager != null) {
                hudManager.stopHUDTask();
            }
            if (partyManager != null) {
                partyManager.stopMarkerTask();
                partyManager.stopCleanupTask();
            }
            if (buffManager != null) {
                buffManager.stopBuffTask();
            }
            if (scoreboardManager != null) {
                scoreboardManager.stopScoreboardTask();
            }
            
            this.getLogger().info("Plugin disabled successfully");
        } catch (Exception e) {
            this.getLogger().error("Error during plugin shutdown", e);
        }
    }
    
    public static EuphoriaPartyPlugin getInstance() {
        return instance;
    }
    
    public PartyManager getPartyManager() {
        return partyManager;
    }
    
    public HUDManager getHUDManager() {
        return hudManager;
    }
    
    public PartyBuffManager getBuffManager() {
        return buffManager;
    }
    
    public PartyAchievementManager getAchievementManager() {
        return achievementManager;
    }
    
    public PartyScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }
    
    public PartyLeaderboardManager getLeaderboardManager() {
        return leaderboardManager;
    }
    
    public String getMessage(String key) {
        String prefix = this.getConfig().getString("messages.prefix", "§8[§6Party§8]§r ");
        String message = this.getConfig().getString("messages." + key, key);
        return prefix + message;
    }
    
    public String getMessageWithoutPrefix(String key) {
        return this.getConfig().getString("messages." + key, key);
    }
    
    /**
     * Reload configuration and update all managers
     */
    public void reloadConfiguration() {
        try {
            this.reloadConfig();
            
            // Reload manager configurations
            if (partyManager != null) {
                partyManager.reloadConfig();
            }
            
            this.getLogger().info("Configuration reloaded successfully");
        } catch (Exception e) {
            this.getLogger().error("Error reloading configuration", e);
        }
    }
}
