package com.euphoria.party.listener;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.scheduler.Task;
import com.euphoria.party.EuphoriaPartyPlugin;
import com.euphoria.party.model.Party;

public class PartyTabListListener implements Listener {
    
    private final EuphoriaPartyPlugin plugin;
    
    public PartyTabListListener(EuphoriaPartyPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!plugin.getConfig().getBoolean("party.tab-list-formatting", false)) {
            return;
        }
        
        // Update tab list after a short delay
        plugin.getServer().getScheduler().scheduleDelayedTask(plugin, new Task() {
            @Override
            public void onRun(int currentTick) {
                updateAllTabLists();
            }
        }, 20); // 1 second delay
    }
    
    public void updateAllTabLists() {
        if (!plugin.getConfig().getBoolean("party.tab-list-formatting", false)) {
            return;
        }
        
        for (Player player : plugin.getServer().getOnlinePlayers().values()) {
            updatePlayerTabList(player);
        }
    }
    
    private void updatePlayerTabList(Player player) {
        Party party = plugin.getPartyManager().getPlayerParty(player.getUniqueId());
        
        if (party != null && party.hasName()) {
            String prefix = plugin.getConfig().getString("party.tab-list-prefix-format", "{color}[{party}] §r");
            prefix = prefix.replace("{party}", party.getName());
            prefix = prefix.replace("{color}", party.getColor());
            
            // Set player's display name (shown in tab list)
            player.setDisplayName(prefix + player.getName());
        } else {
            // Reset to normal name
            player.setDisplayName(player.getName());
        }
    }
    
    public void updatePartyTabLists(Party party) {
        for (java.util.UUID memberId : party.getMembers()) {
            Player member = plugin.getServer().getPlayer(memberId).orElse(null);
            if (member != null) {
                updatePlayerTabList(member);
            }
        }
    }
}
