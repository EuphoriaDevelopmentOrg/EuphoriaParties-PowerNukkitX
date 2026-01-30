package com.euphoria.party.manager;

import cn.nukkit.Player;
import cn.nukkit.scheduler.Task;
import com.euphoria.party.EuphoriaPartyPlugin;
import com.euphoria.party.model.Party;

import java.util.*;

public class PartyScoreboardManager {
    
    private final EuphoriaPartyPlugin plugin;
    private cn.nukkit.scheduler.TaskHandler scoreboardTaskId = null;
    private final Set<UUID> enabledPlayers;
    
    public PartyScoreboardManager(EuphoriaPartyPlugin plugin) {
        this.plugin = plugin;
        this.enabledPlayers = new HashSet<>();
    }
    
    public void startScoreboardTask() {
        if (!plugin.getConfig().getBoolean("party.scoreboard.enabled", false)) {
            return;
        }
        
        int interval = plugin.getConfig().getInt("party.scoreboard.update-interval", 40); // 2 seconds
        
        scoreboardTaskId = plugin.getServer().getScheduler().scheduleDelayedRepeatingTask(plugin, new Task() {
            @Override
            public void onRun(int currentTick) {
                updateScoreboards();
            }
        }, interval, interval);
    }
    
    public void stopScoreboardTask() {
        if (scoreboardTaskId != null) {
            scoreboardTaskId.cancel();
            scoreboardTaskId = null;
        }
    }
    
    public void toggleScoreboard(UUID playerId) {
        Player player = plugin.getServer().getPlayer(playerId).orElse(null);
        if (player == null) {
            return;
        }
        
        if (enabledPlayers.contains(playerId)) {
            enabledPlayers.remove(playerId);
            // Just send the message - scoreboard will stop updating automatically
            player.sendMessage("§cParty scoreboard disabled!");
        } else {
            enabledPlayers.add(playerId);
            player.sendMessage("§aParty scoreboard enabled!");
            // Immediately update to show the scoreboard
            Party party = plugin.getPartyManager().getPlayerParty(playerId);
            if (party != null) {
                updatePlayerScoreboard(player, party);
            }
        }
    }
    
    private void updateScoreboards() {
        for (UUID playerId : enabledPlayers) {
            Player player = plugin.getServer().getPlayer(playerId).orElse(null);
            if (player == null || !player.isOnline()) {
                continue;
            }
            
            Party party = plugin.getPartyManager().getPlayerParty(playerId);
            if (party == null) {
                continue;
            }
            
            updatePlayerScoreboard(player, party);
        }
    }
    
    private void updatePlayerScoreboard(Player player, Party party) {
        // Count online members
        int onlineCount = 0;
        for (UUID memberId : party.getMembers()) {
            Player member = plugin.getServer().getPlayer(memberId).orElse(null);
            if (member != null && member.isOnline()) {
                onlineCount++;
            }
        }
        
        // Format playtime
        long hours = party.getTotalPlayTime() / (1000 * 60 * 60);
        long minutes = (party.getTotalPlayTime() / (1000 * 60)) % 60;
        
        // Build scoreboard lines
        List<String> lines = new ArrayList<>();
        lines.add("§8§m--------------------");
        if (party.hasName()) {
            lines.add(party.getColor() + party.getIcon() + " " + party.getName());
        } else {
            lines.add("§6" + party.getIcon() + " Party");
        }
        lines.add("");
        lines.add("§7Members: §f" + onlineCount + "§8/§f" + party.getMemberCount());
        lines.add("§7Playtime: §f" + hours + "h " + minutes + "m");
        lines.add("§7Kills: §f" + party.getTotalKills());
        lines.add("§7Deaths: §f" + party.getTotalDeaths());
        if (party.getTotalDeaths() > 0) {
            double kd = (double) party.getTotalKills() / party.getTotalDeaths();
            lines.add("§7K/D: §f" + String.format("%.2f", kd));
        }
        lines.add("§8§m--------------------");
        
        // Build message
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append(line).append("\n");
        }
        
        // Use popup for better positioning (appears in center-top instead of action bar)
        String displayType = plugin.getConfig().getString("party.scoreboard.display-type", "popup");
        if (displayType.equalsIgnoreCase("popup")) {
            player.sendPopup(sb.toString());
        } else {
            player.sendTip(sb.toString());
        }
    }
    
    private void clearScoreboard(Player player) {
        // Clear both tip and popup
        player.sendTip("");
        player.sendPopup("");
    }
    
    public boolean isEnabled(UUID playerId) {
        return enabledPlayers.contains(playerId);
    }
}
