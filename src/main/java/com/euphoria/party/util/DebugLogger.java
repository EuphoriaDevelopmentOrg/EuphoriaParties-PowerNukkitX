package com.euphoria.party.util;

import com.euphoria.party.EuphoriaPartyPlugin;

/**
 * Utility class for debug logging and performance monitoring
 */
public class DebugLogger {
    
    private final EuphoriaPartyPlugin plugin;
    private boolean debugEnabled;
    private boolean logCommands;
    private boolean logEvents;
    private boolean logPerformance;
    
    public DebugLogger(EuphoriaPartyPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }
    
    public void loadConfig() {
        this.debugEnabled = plugin.getConfig().getBoolean("debug.enabled", false);
        this.logCommands = plugin.getConfig().getBoolean("debug.log-commands", false);
        this.logEvents = plugin.getConfig().getBoolean("debug.log-events", false);
        this.logPerformance = plugin.getConfig().getBoolean("debug.log-performance", false);
    }
    
    /**
     * Log a debug message
     */
    public void debug(String message) {
        if (debugEnabled) {
            plugin.getLogger().info("[DEBUG] " + message);
        }
    }
    
    /**
     * Log a command execution
     */
    public void logCommand(String player, String command, String[] args) {
        if (logCommands) {
            plugin.getLogger().info("[COMMAND] " + player + " executed: " + command + 
                " with args: " + String.join(", ", args));
        }
    }
    
    /**
     * Log an event
     */
    public void logEvent(String eventName, String details) {
        if (logEvents) {
            plugin.getLogger().info("[EVENT] " + eventName + ": " + details);
        }
    }
    
    /**
     * Log performance metrics
     */
    public void logPerformance(String operation, long startTime) {
        if (logPerformance) {
            long elapsed = System.currentTimeMillis() - startTime;
            plugin.getLogger().info("[PERFORMANCE] " + operation + " took " + elapsed + "ms");
        }
    }
    
    /**
     * Time an operation and log the result
     */
    public <T> T timeOperation(String name, java.util.function.Supplier<T> operation) {
        long start = System.currentTimeMillis();
        try {
            return operation.get();
        } finally {
            logPerformance(name, start);
        }
    }
    
    /**
     * Check if debug is enabled
     */
    public boolean isDebugEnabled() {
        return debugEnabled;
    }
}
