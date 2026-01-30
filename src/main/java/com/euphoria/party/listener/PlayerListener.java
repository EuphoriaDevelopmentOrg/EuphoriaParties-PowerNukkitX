package com.euphoria.party.listener;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.player.PlayerChatEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import com.euphoria.party.EuphoriaPartyPlugin;
import com.euphoria.party.model.Party;

import java.util.UUID;

public class PlayerListener implements Listener {
    
    private final EuphoriaPartyPlugin plugin;
    
    public PlayerListener(EuphoriaPartyPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }
        
        UUID playerId = player.getUniqueId();
        
        // Clean up HUD settings
        plugin.getHUDManager().removePlayer(playerId);
        
        // Clean up party manager tracking
        plugin.getPartyManager().cleanupPlayerData(playerId);
        
        // Handle party cleanup if needed
        Party party = plugin.getPartyManager().getPlayerParty(playerId);
        if (party != null) {
            // Notify other members that player is offline
            if (plugin.getConfig().getBoolean("party.notify-online-offline", true)) {
                for (java.util.UUID memberId : party.getMembers()) {
                    if (memberId.equals(event.getPlayer().getUniqueId())) continue;
                    
                    Player member = plugin.getServer().getPlayer(memberId).orElse(null);
                    if (member != null) {
                        member.sendMessage("§c- §7" + event.getPlayer().getName() + " §cis now offline");
                    }
                }
            }
            
            // Check if party should be cleaned up (all members offline)
            plugin.getPartyManager().checkPartyCleanup(party.getId());
        }
        
        // Save parties if this is the last player
        if (plugin.getServer().getOnlinePlayers().size() == 1) { // 1 because this player hasn't fully quit yet
            plugin.getPartyManager().saveAllParties();
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(PlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        String chatPrefix = plugin.getConfig().getString("party.party-chat-prefix", "@");
        
        // Check if message starts with party chat prefix
        if (plugin.getConfig().getBoolean("party.party-chat-enabled", true) && message.startsWith(chatPrefix)) {
            // Handle party chat
            handlePartyChat(event, player, message, chatPrefix);
            return;
        }
        
        // Add party name prefix to regular chat
        if (plugin.getConfig().getBoolean("party.show-party-in-chat", true)) {
            Party party = plugin.getPartyManager().getPlayerParty(player.getUniqueId());
            if (party != null && party.hasName()) {
                String format = plugin.getConfig().getString("party.party-prefix-format", "§8[§6{party}§8] ");
                String partyPrefix = format.replace("{party}", party.getName());
                
                // Cancel the event and manually broadcast
                event.setCancelled(true);
                
                // Broadcast to all players
                String formattedMessage = partyPrefix + "<" + player.getName() + "> " + message;
                plugin.getServer().broadcastMessage(formattedMessage);
                
                // Also log to console
                plugin.getLogger().info(formattedMessage);
            }
        }
    }
    
    private void handlePartyChat(PlayerChatEvent event, Player player, String message, String prefix) {
        if (player == null || message == null || message.length() <= prefix.length()) {
            event.setCancelled(true);
            return;
        }
        
        Party party = plugin.getPartyManager().getPlayerParty(player.getUniqueId());
        
        if (party == null) {
            player.sendMessage(plugin.getMessage("not-in-party"));
            event.setCancelled(true);
            return;
        }
        
        // Cancel the original event
        event.setCancelled(true);
        
        // Remove prefix from message
        String actualMessage = message.substring(prefix.length()).trim();
        
        if (actualMessage.isEmpty()) {
            player.sendMessage("§cPlease provide a message to send!");
            return;
        }
        
        // Format and send to all party members
        String format = plugin.getConfig().getString("party.party-chat-format", "§8[§6Party§8] §f{player}§7: §f{message}");
        String formattedMessage = format
            .replace("{player}", player.getName())
            .replace("{message}", actualMessage);
        
        for (java.util.UUID memberId : party.getMembers()) {
            Player member = plugin.getServer().getPlayer(memberId).orElse(null);
            if (member != null && member.isOnline()) {
                member.sendMessage(formattedMessage);
            }
        }
    }
}
