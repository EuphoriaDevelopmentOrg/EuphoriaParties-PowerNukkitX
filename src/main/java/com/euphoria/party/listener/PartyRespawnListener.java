package com.euphoria.party.listener;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerDeathEvent;
import cn.nukkit.event.player.PlayerRespawnEvent;
import cn.nukkit.level.Location;
import com.euphoria.party.EuphoriaPartyPlugin;
import com.euphoria.party.model.Party;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PartyRespawnListener implements Listener {
    
    private final EuphoriaPartyPlugin plugin;
    private final Map<UUID, Location> pendingRespawns;
    
    public PartyRespawnListener(EuphoriaPartyPlugin plugin) {
        this.plugin = plugin;
        this.pendingRespawns = new HashMap<>();
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!plugin.getConfig().getBoolean("party.respawn-at-home", false)) {
            return;
        }
        
        Player player = event.getEntity();
        Party party = plugin.getPartyManager().getPlayerParty(player.getUniqueId());
        
        if (party != null && party.hasHome()) {
            Location home = party.getHome();
            if (home != null && home.getLevel() != null) {
                pendingRespawns.put(player.getUniqueId(), home);
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Location respawnLocation = pendingRespawns.remove(player.getUniqueId());
        
        if (respawnLocation != null) {
            // Teleport after spawn
            plugin.getServer().getScheduler().scheduleDelayedTask(plugin, new cn.nukkit.scheduler.Task() {
                @Override
                public void onRun(int currentTick) {
                    player.teleport(respawnLocation);
                    player.sendMessage("§aRespawned at party home!");
                }
            }, 10); // Wait 0.5 seconds after respawn
        }
    }
}
