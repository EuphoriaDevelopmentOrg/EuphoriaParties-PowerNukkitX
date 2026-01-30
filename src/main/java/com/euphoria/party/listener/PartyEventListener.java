package com.euphoria.party.listener;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityDeathEvent;
import cn.nukkit.event.player.PlayerDeathEvent;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.item.Item;
import com.euphoria.party.EuphoriaPartyPlugin;
import com.euphoria.party.model.Party;

public class PartyEventListener implements Listener {
    
    private final EuphoriaPartyPlugin plugin;
    
    public PartyEventListener(EuphoriaPartyPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Notify party members that player is online
        if (!plugin.getConfig().getBoolean("party.notify-online-offline", true)) {
            return;
        }
        
        Party party = plugin.getPartyManager().getPlayerParty(event.getPlayer().getUniqueId());
        if (party != null) {
            for (java.util.UUID memberId : party.getMembers()) {
                if (memberId.equals(event.getPlayer().getUniqueId())) continue;
                
                Player member = plugin.getServer().getPlayer(memberId).orElse(null);
                if (member != null) {
                    member.sendMessage("§a+ §7" + event.getPlayer().getName() + " §ais now online");
                }
            }
        }
    }
    
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Party party = plugin.getPartyManager().getPlayerParty(player.getUniqueId());
        
        if (party != null) {
            party.incrementDeaths();
            
            // Share XP if enabled
            if (plugin.getConfig().getBoolean("party.share-xp", false)) {
                int totalXp = player.getExperience();
                if (totalXp > 0) {
                    // Find nearby party members
                    java.util.List<Player> nearbyMembers = new java.util.ArrayList<>();
                    double shareRadius = plugin.getConfig().getDouble("party.xp-share-radius", 50.0);
                    
                    for (java.util.UUID memberId : party.getMembers()) {
                        if (memberId.equals(player.getUniqueId())) continue;
                        
                        Player member = plugin.getServer().getPlayer(memberId).orElse(null);
                        if (member != null && member.getLevel() == player.getLevel()) {
                            if (member.distance(player) <= shareRadius) {
                                nearbyMembers.add(member);
                            }
                        }
                    }
                    
                    if (!nearbyMembers.isEmpty()) {
                        int xpPerMember = totalXp / (nearbyMembers.size() + 1); // +1 for the dead player
                        for (Player member : nearbyMembers) {
                            member.addExperience(xpPerMember);
                            member.sendMessage("§e+§6" + xpPerMember + " §eXP §7(Party Share)");
                        }
                    }
                }
            }
        }
    }
    
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Entity killer = event.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent ?
                ((EntityDamageByEntityEvent) event.getEntity().getLastDamageCause()).getDamager() : null;
        
        if (!(killer instanceof Player)) {
            return;
        }
        
        Player player = (Player) killer;
        Party party = plugin.getPartyManager().getPlayerParty(player.getUniqueId());
        
        if (party != null) {
            party.incrementKills();
            
            // Share XP if enabled
            if (plugin.getConfig().getBoolean("party.share-xp", false)) {
                java.util.List<Player> nearbyMembers = new java.util.ArrayList<>();
                double shareRadius = plugin.getConfig().getDouble("party.xp-share-radius", 50.0);
                
                for (java.util.UUID memberId : party.getMembers()) {
                    if (memberId.equals(player.getUniqueId())) continue;
                    
                    Player member = plugin.getServer().getPlayer(memberId).orElse(null);
                    if (member != null && member.getLevel() == player.getLevel()) {
                        if (member.distance(player) <= shareRadius) {
                            nearbyMembers.add(member);
                        }
                    }
                }
                
                if (!nearbyMembers.isEmpty()) {
                    // Give bonus XP to nearby members (10% of typical mob XP)
                    int bonusXp = 5;
                    for (Player member : nearbyMembers) {
                        member.addExperience(bonusXp);
                    }
                }
            }
            
            // Share loot if enabled
            if (plugin.getConfig().getBoolean("party.share-loot", false)) {
                Item[] drops = event.getDrops();
                if (drops.length > 0) {
                    java.util.List<Player> nearbyMembers = new java.util.ArrayList<>();
                    double shareRadius = plugin.getConfig().getDouble("party.loot-share-radius", 30.0);
                    
                    for (java.util.UUID memberId : party.getMembers()) {
                        if (memberId.equals(player.getUniqueId())) continue;
                        
                        Player member = plugin.getServer().getPlayer(memberId).orElse(null);
                        if (member != null && member.getLevel() == player.getLevel()) {
                            if (member.distance(player) <= shareRadius) {
                                nearbyMembers.add(member);
                            }
                        }
                    }
                    
                    if (!nearbyMembers.isEmpty() && plugin.getConfig().getDouble("party.loot-share-chance", 0.3) > Math.random()) {
                        // Randomly select an item to duplicate
                        Item sharedItem = drops[(int) (Math.random() * drops.length)].clone();
                        
                        // Give to random nearby member
                        Player luckyMember = nearbyMembers.get((int) (Math.random() * nearbyMembers.size()));
                        luckyMember.getInventory().addItem(sharedItem);
                        luckyMember.sendMessage("§e+§6" + sharedItem.getName() + " §7(Party Loot Share)");
                    }
                }
            }
        }
    }
}
