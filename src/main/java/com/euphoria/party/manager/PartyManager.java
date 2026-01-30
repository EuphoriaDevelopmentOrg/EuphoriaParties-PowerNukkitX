package com.euphoria.party.manager;

import cn.nukkit.Player;
import cn.nukkit.level.Location;
import cn.nukkit.level.particle.HeartParticle;
import cn.nukkit.scheduler.Task;
import cn.nukkit.scheduler.TaskHandler;
import com.euphoria.party.EuphoriaPartyPlugin;
import com.euphoria.party.model.Party;
import com.euphoria.party.storage.PartyStorage;
import com.euphoria.party.util.Cache;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PartyManager {
    
    private final EuphoriaPartyPlugin plugin;
    private final Map<UUID, Party> parties;
    private final Map<UUID, UUID> playerToParty;
    private final Map<UUID, UUID> playerInvites;  // Player UUID -> Party UUID for quick invite lookups
    private final Map<UUID, Long> lastCommandUse;  // Command cooldown tracking
    private final Map<UUID, Long> lastTeleport;  // Teleport cooldown tracking
    private final Map<UUID, Location> lastPlayerLocations;  // For optimized marker updates
    private final PartyStorage storage;
    private final Cache<UUID, Party> partyCache;  // Cache for party lookups
    private final Cache<String, List<Party>> leaderboardCache;  // Cache for leaderboard queries
    private cn.nukkit.scheduler.TaskHandler markerTaskId = null;
    private cn.nukkit.scheduler.TaskHandler distanceCheckTaskId = null;
    private cn.nukkit.scheduler.TaskHandler playTimeTaskId = null;
    private cn.nukkit.scheduler.TaskHandler cleanupTaskId = null;
    private long inviteExpirationTime;
    private int commandCooldown;
    private boolean optimizeMarkers;
    private long lastAchievementCheck = 0;
    
    public PartyManager(EuphoriaPartyPlugin plugin) {
        this.plugin = plugin;
        this.parties = new ConcurrentHashMap<>();
        this.playerToParty = new ConcurrentHashMap<>();
        this.playerInvites = new ConcurrentHashMap<>();
        this.lastCommandUse = new ConcurrentHashMap<>();
        this.lastTeleport = new ConcurrentHashMap<>();
        this.lastPlayerLocations = new ConcurrentHashMap<>();
        this.storage = new PartyStorage(plugin.getDataFolder());
        
        // Initialize caches (30 second TTL for party cache, 5 second for leaderboards)
        this.partyCache = new Cache<>(30000);
        this.leaderboardCache = new Cache<>(5000);
        
        // Load config values
        this.inviteExpirationTime = plugin.getConfig().getLong("party.invite-expiration", 300000);
        this.commandCooldown = plugin.getConfig().getInt("security.command-cooldown", 3);
        this.optimizeMarkers = plugin.getConfig().getBoolean("performance.optimize-markers", true);
        
        // Load saved parties
        loadAllParties();
        
        // Start memory cleanup task
        startCleanupTask();
    }
    
    public Party createParty(Player leader) {
        if (leader == null || !leader.isOnline()) {
            return null;
        }
        
        if (isInParty(leader.getUniqueId())) {
            return null;
        }
        
        Party party = new Party(leader.getUniqueId());
        parties.put(party.getId(), party);
        playerToParty.put(leader.getUniqueId(), party.getId());
        
        // Start marker task if this is the first party
        if (parties.size() == 1 && markerTaskId == null) {
            startMarkerTask();
        }
        
        return party;
    }
    
    /**
     * Check if player is on cooldown for commands
     * @param playerId Player UUID
     * @return true if on cooldown
     */
    public boolean isOnCooldown(UUID playerId) {
        Long lastUse = lastCommandUse.get(playerId);
        if (lastUse == null) {
            return false;
        }
        long elapsed = (System.currentTimeMillis() - lastUse) / 1000;
        return elapsed < commandCooldown;
    }
    
    /**
     * Get remaining cooldown time in seconds
     */
    public int getRemainingCooldown(UUID playerId) {
        Long lastUse = lastCommandUse.get(playerId);
        if (lastUse == null) {
            return 0;
        }
        long elapsed = (System.currentTimeMillis() - lastUse) / 1000;
        return Math.max(0, commandCooldown - (int)elapsed);
    }
    
    /**
     * Update last command use time
     */
    public void updateCooldown(UUID playerId) {
        lastCommandUse.put(playerId, System.currentTimeMillis());
    }
    
    /**
     * Check if player is on teleport cooldown
     */
    public boolean isOnTeleportCooldown(UUID playerId) {
        Long lastUse = lastTeleport.get(playerId);
        if (lastUse == null) {
            return false;
        }
        int cooldown = plugin.getConfig().getInt("security.teleport-cooldown", 30);
        long elapsed = (System.currentTimeMillis() - lastUse) / 1000;
        return elapsed < cooldown;
    }
    
    /**
     * Get remaining teleport cooldown time in seconds
     */
    public int getRemainingTeleportCooldown(UUID playerId) {
        Long lastUse = lastTeleport.get(playerId);
        if (lastUse == null) {
            return 0;
        }
        int cooldown = plugin.getConfig().getInt("security.teleport-cooldown", 30);
        long elapsed = (System.currentTimeMillis() - lastUse) / 1000;
        return Math.max(0, cooldown - (int)elapsed);
    }
    
    /**
     * Update last teleport time
     */
    public void updateTeleportCooldown(UUID playerId) {
        lastTeleport.put(playerId, System.currentTimeMillis());
    }
    
    public boolean disbandParty(UUID partyId) {
        Party party = parties.get(partyId);
        if (party == null) {
            return false;
        }
        
        // Remove all members from tracking
        for (UUID memberId : party.getMembers()) {
            playerToParty.remove(memberId);
            lastPlayerLocations.remove(memberId);  // Clear location data
            partyCache.invalidate(memberId);  // Clear cache
        }
        
        parties.remove(partyId);
        leaderboardCache.clear();  // Clear leaderboard cache
        
        // Stop marker task if no parties remain
        if (parties.isEmpty() && markerTaskId != null) {
            stopMarkerTask();
        }
        
        return true;
    }
    
    public void invitePlayer(Party party, UUID playerId) {
        if (party == null || playerId == null) {
            return;
        }
        
        // Clean expired invites first
        party.cleanExpiredInvites(inviteExpirationTime);
        
        // Check if already invited
        if (party.hasInvite(playerId)) {
            return;  // Already has pending invite
        }
        
        // Check max invites limit
        int maxInvites = plugin.getConfig().getInt("party.max-pending-invites", 10);
        if (party.getInvites().size() >= maxInvites) {
            return;  // Too many pending invites
        }
        
        party.invitePlayer(playerId);
        playerInvites.put(playerId, party.getId());
    }
    
    public boolean acceptInvite(Player player, Party party) {
        if (player == null || party == null) {
            return false;
        }
        
        if (!party.hasInvite(player.getUniqueId())) {
            return false;
        }
        
        // Check if invite expired
        if (party.isInviteExpired(player.getUniqueId(), inviteExpirationTime)) {
            party.removeInvite(player.getUniqueId());
            playerInvites.remove(player.getUniqueId());
            player.sendMessage(plugin.getMessage("invite-expired"));
            return false;
        }
        
        int maxMembers = plugin.getConfig().getInt("party.max-members", 8);
        if (party.getMemberCount() >= maxMembers) {
            return false;
        }
        
        party.addMember(player.getUniqueId());
        playerToParty.put(player.getUniqueId(), party.getId());
        playerInvites.remove(player.getUniqueId());
        
        return true;
    }
    
    public void leaveParty(UUID playerId) {
        UUID partyId = playerToParty.get(playerId);
        if (partyId == null) {
            return;
        }
        
        Party party = parties.get(partyId);
        if (party == null) {
            playerToParty.remove(playerId);
            partyCache.invalidate(playerId);
            return;
        }
        
        party.removeMember(playerId);
        playerToParty.remove(playerId);
        partyCache.invalidate(playerId);
        leaderboardCache.clear();
        
        // If party is empty after removal, disband
        if (party.getMemberCount() == 0) {
            disbandParty(partyId);
            return;
        }
        
        // If leader leaves, promote next online member
        if (party.isLeader(playerId)) {
            UUID newLeader = null;
            
            // Find first online member to promote
            for (UUID memberId : party.getMembers()) {
                Player member = plugin.getServer().getPlayer(memberId).orElse(null);
                if (member != null && member.isOnline()) {
                    newLeader = memberId;
                    break;
                }
            }
            
            // If no online member, pick first member
            if (newLeader == null) {
                newLeader = party.getMembers().iterator().next();
            }
            
            party.transferLeadership(newLeader);
            
            // Notify all members about leadership change
            Player newLeaderPlayer = plugin.getServer().getPlayer(newLeader).orElse(null);
            String newLeaderName = newLeaderPlayer != null ? newLeaderPlayer.getName() : "Unknown";
            
            for (UUID memberId : party.getMembers()) {
                Player member = plugin.getServer().getPlayer(memberId).orElse(null);
                if (member != null) {
                    member.sendMessage(plugin.getMessage("leader-transferred")
                        .replace("{player}", newLeaderName));
                }
            }
        }
    }
    
    public void kickPlayer(Party party, UUID playerId) {
        party.removeMember(playerId);
        playerToParty.remove(playerId);
        
        if (party.getMemberCount() == 0) {
            disbandParty(party.getId());
        }
    }
    
    public Party getParty(UUID partyId) {
        return parties.get(partyId);
    }
    
    public Party getPlayerParty(UUID playerId) {
        // Check cache first
        Party cached = partyCache.get(playerId);
        if (cached != null) {
            return cached;
        }
        
        // Lookup and cache
        UUID partyId = playerToParty.get(playerId);
        Party party = partyId != null ? parties.get(partyId) : null;
        
        if (party != null) {
            partyCache.put(playerId, party);
        }
        
        return party;
    }
    
    public Collection<Party> getAllParties() {
        return parties.values();
    }
    
    public boolean isInParty(UUID playerId) {
        return playerToParty.containsKey(playerId);
    }
    
    /**
     * Get party that has invited this player (efficient O(1) lookup)
     */
    public Party getPendingInvite(UUID playerId) {
        UUID partyId = playerInvites.get(playerId);
        return partyId != null ? parties.get(partyId) : null;
    }
    
    public void setPartyHome(Party party, Location location) {
        party.setHome(location);
    }
    
    public void startMarkerTask() {
        int interval = plugin.getConfig().getInt("party.marker-update-interval", 10);
        
        markerTaskId = plugin.getServer().getScheduler().scheduleRepeatingTask(new Task() {
            @Override
            public void onRun(int currentTick) {
                updatePartyMarkers();
            }
        }, interval);
    }
    
    public void stopMarkerTask() {
        if (markerTaskId != null) {
            markerTaskId.cancel();
            markerTaskId = null;
        }
    }
    
    private void updatePartyMarkers() {
        double maxDistance = plugin.getConfig().getDouble("party.marker-distance", 200.0);
        int particleCount = plugin.getConfig().getInt("party.marker-particle-count", 3);
        
        // Pre-cache online players to avoid repeated lookups
        Map<UUID, Player> onlinePlayers = new HashMap<>();
        for (Player p : plugin.getServer().getOnlinePlayers().values()) {
            onlinePlayers.put(p.getUniqueId(), p);
        }
        
        for (Party party : parties.values()) {
            Set<UUID> members = party.getMembers();
            
            // Skip parties with no online members
            boolean hasOnlineMembers = false;
            for (UUID memberId : members) {
                if (onlinePlayers.containsKey(memberId)) {
                    hasOnlineMembers = true;
                    break;
                }
            }
            if (!hasOnlineMembers) {
                continue;
            }
            
            for (UUID memberId : members) {
                Player player = onlinePlayers.get(memberId);
                if (player == null) {
                    continue;
                }
                
                // Performance optimization: Skip if player hasn't moved much
                if (optimizeMarkers) {
                    Location lastLoc = lastPlayerLocations.get(memberId);
                    Location currentLoc = player.getLocation();
                    if (lastLoc != null && lastLoc.distance(currentLoc) < 1.0) {
                        continue;  // Player hasn't moved significantly
                    }
                    lastPlayerLocations.put(memberId, currentLoc.clone());
                }
                
                // Batch collect particles to send
                java.util.List<HeartParticle> particlesToSend = new java.util.ArrayList<>();
                
                // Show markers for other party members
                for (UUID otherMemberId : members) {
                    if (memberId.equals(otherMemberId)) {
                        continue;
                    }
                    
                    Player otherPlayer = onlinePlayers.get(otherMemberId);
                    if (otherPlayer == null) {
                        continue;
                    }
                    
                    // Check if in same level
                    if (!player.getLevel().equals(otherPlayer.getLevel())) {
                        continue;
                    }
                    
                    // Check distance (use squared distance to avoid sqrt)
                    double dx = player.x - otherPlayer.x;
                    double dy = player.y - otherPlayer.y;
                    double dz = player.z - otherPlayer.z;
                    double distanceSquared = dx * dx + dy * dy + dz * dz;
                    
                    if (distanceSquared > maxDistance * maxDistance) {
                        continue;
                    }
                    
                    // Create particles at location above other player
                    Location particleLocation = otherPlayer.getLocation().add(0, 2.5, 0);
                    for (int i = 0; i < particleCount; i++) {
                        particlesToSend.add(new HeartParticle(particleLocation));
                    }
                }
                
                // Send all particles in batch
                for (HeartParticle particle : particlesToSend) {
                    player.getLevel().addParticle(particle, player);
                }
            }
        }
    }
    
    public void saveAllParties() {
        saveAllParties(false);
    }
    
    /**
     * Save all parties with option to force synchronous save
     * @param forceSync If true, saves synchronously even if async-save is enabled
     */
    public void saveAllParties(boolean forceSync) {
        plugin.getLogger().info("Saving " + parties.size() + " parties...");
        
        boolean async = plugin.getConfig().getBoolean("performance.async-save", true) && !forceSync;
        
        // Check if plugin is enabled before scheduling async tasks
        if (async && plugin.isEnabled()) {
            // Create a snapshot of current party data to avoid concurrent modification
            Map<UUID, Party> partySnapshot = new HashMap<>(parties);
            try {
                plugin.getServer().getScheduler().scheduleTask(plugin, () -> {
                    storage.saveParties(partySnapshot);
                }, true); // true = async
            } catch (Exception e) {
                // If async scheduling fails, fall back to sync
                plugin.getLogger().warning("Async save failed, falling back to synchronous save");
                storage.saveParties(parties);
            }
        } else {
            // Synchronous save
            storage.saveParties(parties);
        }
    }
    
    public void loadAllParties() {
        plugin.getLogger().info("Loading parties from storage...");
        Map<UUID, Party> loadedParties = storage.loadParties();
        
        for (Map.Entry<UUID, Party> entry : loadedParties.entrySet()) {
            Party party = entry.getValue();
            parties.put(entry.getKey(), party);
            
            // Reconstruct party home with server instance
            PartyStorage.LocationData homeData = storage.getHomeData(party.getId());
            if (homeData != null) {
                cn.nukkit.level.Level level = plugin.getServer().getLevelByName(homeData.level);
                if (level != null) {
                    Location home = new Location(homeData.x, homeData.y, homeData.z, homeData.yaw, homeData.pitch, level);
                    party.setHome(home);
                }
            }
            
            // Rebuild player to party mapping
            for (UUID memberId : party.getMembers()) {
                playerToParty.put(memberId, party.getId());
            }
        }
        
        plugin.getLogger().info("Loaded " + parties.size() + " parties from storage.");
    }
    
    public boolean hasParties() {
        return !parties.isEmpty();
    }
    
    /**
     * Clean up player-specific data on quit
     */
    public void cleanupPlayerData(UUID playerId) {
        lastCommandUse.remove(playerId);
        lastTeleport.remove(playerId);
        lastPlayerLocations.remove(playerId);
        playerInvites.remove(playerId);
    }
    
    /**
     * Check if party should be cleaned up (all members offline)
     */
    public void checkPartyCleanup(UUID partyId) {
        Party party = parties.get(partyId);
        if (party == null) return;
        
        boolean anyOnline = false;
        for (UUID memberId : party.getMembers()) {
            Player member = plugin.getServer().getPlayer(memberId).orElse(null);
            if (member != null && member.isOnline()) {
                anyOnline = true;
                break;
            }
        }
        
        // If no members online and config allows cleanup, remove the party
        if (!anyOnline && plugin.getConfig().getBoolean("party.disband-when-all-offline", false)) {
            plugin.getLogger().info("Auto-disbanding party " + partyId + " (all members offline)");
            disbandParty(partyId);
        }
    }
    
    /**
     * Clean up expired invites across all parties
     */
    public void cleanupExpiredInvites() {
        for (Party party : parties.values()) {
            // Get invites before cleaning
            Set<UUID> invitedPlayers = new HashSet<>(party.getInvites().keySet());
            
            // Clean expired from party
            party.cleanExpiredInvites(inviteExpirationTime);
            
            // Remove from playerInvites if no longer in party invites
            for (UUID playerId : invitedPlayers) {
                if (!party.hasInvite(playerId)) {
                    playerInvites.remove(playerId);
                }
            }
            
            // Clean join requests too
            party.cleanExpiredJoinRequests(inviteExpirationTime);
        }
    }
    
    /**
     * Start distance check task to auto-remove members who are too far
     */
    public void startDistanceCheckTask() {
        if (!plugin.getConfig().getBoolean("party.max-distance-check-enabled", false)) {
            return;
        }
        
        int interval = plugin.getConfig().getInt("party.max-distance-check-interval", 200); // 10 seconds
        
        distanceCheckTaskId = plugin.getServer().getScheduler().scheduleDelayedRepeatingTask(plugin, new Task() {
            @Override
            public void onRun(int currentTick) {
                checkDistances();
            }
        }, interval, interval);
    }
    
    /**
     * Check all party members distances and remove if too far
     */
    private void checkDistances() {
        double maxDistance = plugin.getConfig().getDouble("party.max-distance", 500.0);
        boolean kickOnWorldChange = plugin.getConfig().getBoolean("party.kick-on-world-change", false);
        
        for (Party party : parties.values()) {
            Player leader = plugin.getServer().getPlayer(party.getLeader()).orElse(null);
            if (leader == null || !leader.isOnline()) {
                continue;
            }
            
            List<UUID> toRemove = new ArrayList<>();
            
            for (UUID memberId : party.getMembers()) {
                if (memberId.equals(party.getLeader())) {
                    continue;
                }
                
                Player member = plugin.getServer().getPlayer(memberId).orElse(null);
                if (member == null || !member.isOnline()) {
                    continue;
                }
                
                // Check world change
                if (kickOnWorldChange && !member.getLevel().getName().equals(leader.getLevel().getName())) {
                    toRemove.add(memberId);
                    member.sendMessage("§cYou were removed from the party (changed world)");
                    continue;
                }
                
                // Check distance (only if in same world)
                if (member.getLevel().getName().equals(leader.getLevel().getName())) {
                    double distance = member.distance(leader);
                    if (distance > maxDistance) {
                        toRemove.add(memberId);
                        member.sendMessage("§cYou were removed from the party (too far from leader)");
                    }
                }
            }
            
            // Remove players who are too far
            for (UUID memberId : toRemove) {
                party.removeMember(memberId);
                playerToParty.remove(memberId);
                
                // Notify leader
                if (leader != null) {
                    Player member = plugin.getServer().getPlayer(memberId).orElse(null);
                    String name = member != null ? member.getName() : "Player";
                    leader.sendMessage("§c" + name + " was removed (distance/world)");
                }
            }
            
            // Disband if empty
            if (party.getMemberCount() == 0) {
                disbandParty(party.getId());
            }
        }
    }
    
    /**
     * Start play time tracker task
     */
    public void startPlayTimeTracker() {
        if (!plugin.getConfig().getBoolean("party.track-playtime", true)) {
            return;
        }
        
        // Update every minute
        playTimeTaskId = plugin.getServer().getScheduler().scheduleDelayedRepeatingTask(plugin, new Task() {
            @Override
            public void onRun(int currentTick) {
                updatePlayTime();
            }
        }, 1200, 1200); // 1200 ticks = 1 minute
    }
    
    /**
     * Update play time for all parties with online members
     */
    private void updatePlayTime() {
        for (Party party : parties.values()) {
            // Check if any members are online
            boolean hasOnlineMembers = false;
            for (UUID memberId : party.getMembers()) {
                Player member = plugin.getServer().getPlayer(memberId).orElse(null);
                if (member != null && member.isOnline()) {
                    hasOnlineMembers = true;
                    break;
                }
            }
            
            // Add 1 minute of play time if members are online
            if (hasOnlineMembers) {
                party.addPlayTime(60000); // 60000 milliseconds = 1 minute
            }
        }
    }
    
    /**
     * Broadcast message to all party members
     */
    public void broadcastToParty(Party party, String message) {
        if (party == null || message == null) {
            return;
        }
        
        for (UUID memberId : party.getMembers()) {
            Player member = plugin.getServer().getPlayer(memberId).orElse(null);
            if (member != null && member.isOnline()) {
                member.sendMessage(message);
            }
        }
    }
    
    /**
     * Add a player to an existing party
     */
    public void addPlayerToParty(Player player, Party party) {
        party.addMember(player.getUniqueId());
        playerToParty.put(player.getUniqueId(), party.getId());
        playerInvites.remove(player.getUniqueId());
        
        // Invalidate caches
        partyCache.invalidate(player.getUniqueId());
        leaderboardCache.clear();
        
        String message = plugin.getMessage("player-joined")
                .replace("{player}", player.getName());
        broadcastToParty(party, message);
    }
    
    /**
     * Remove player from their party
     */
    public void removePlayerFromParty(Player player) {
        Party party = getPlayerParty(player.getUniqueId());
        if (party != null) {
            leaveParty(player.getUniqueId());
        }
    }
    
    /**
     * Start memory cleanup task - runs every 5 minutes
     */
    private void startCleanupTask() {
        cleanupTaskId = plugin.getServer().getScheduler().scheduleDelayedRepeatingTask(plugin, new Task() {
            @Override
            public void onRun(int currentTick) {
                performMemoryCleanup();
            }
        }, 6000, 6000); // 5 minutes
    }
    
    /**
     * Perform memory cleanup operations
     */
    private void performMemoryCleanup() {
        // Clean expired cache entries
        partyCache.cleanExpired();
        leaderboardCache.cleanExpired();
        
        // Clean up stale cooldowns (older than 1 hour)
        long oneHourAgo = System.currentTimeMillis() - 3600000;
        lastCommandUse.entrySet().removeIf(entry -> entry.getValue() < oneHourAgo);
        lastTeleport.entrySet().removeIf(entry -> entry.getValue() < oneHourAgo);
        
        // Clean up location data for offline players
        Set<UUID> onlinePlayers = new HashSet<>();
        for (Player p : plugin.getServer().getOnlinePlayers().values()) {
            onlinePlayers.add(p.getUniqueId());
        }
        lastPlayerLocations.keySet().retainAll(onlinePlayers);
        
        plugin.getLogger().info("Memory cleanup completed. Cache size: " + partyCache.size());
    }
    
    /**
     * Stop cleanup task
     */
    public void stopCleanupTask() {
        if (cleanupTaskId != null) {
            cleanupTaskId.cancel();
            cleanupTaskId = null;
        }
    }
    
    /**
     * Get leaderboard with caching
     */
    public List<Party> getCachedLeaderboard(String type, int limit) {
        String cacheKey = type + "_" + limit;
        List<Party> cached = leaderboardCache.get(cacheKey);
        
        if (cached != null) {
            return cached;
        }
        
        // Will be populated by LeaderboardManager
        return null;
    }
    
    /**
     * Cache leaderboard results
     */
    public void cacheLeaderboard(String type, int limit, List<Party> results) {
        String cacheKey = type + "_" + limit;
        leaderboardCache.put(cacheKey, results);
    }
    
    /**
     * Graceful shutdown - save all data and cleanup resources
     */
    public void shutdown() {
        plugin.getLogger().info("Shutting down PartyManager...");
        
        // Save all parties synchronously (plugin is being disabled)
        saveAllParties(true);
        
        // Stop all tasks
        stopMarkerTask();
        stopCleanupTask();
        if (playTimeTaskId != null) {
            playTimeTaskId.cancel();
            playTimeTaskId = null;
        }
        
        // Clear all caches
        partyCache.clear();
        leaderboardCache.clear();
        
        plugin.getLogger().info("PartyManager shutdown complete");
    }
    
    /**
     * Reload configuration values
     */
    public void reloadConfig() {
        this.inviteExpirationTime = plugin.getConfig().getLong("party.invite-expiration", 300000);
        this.commandCooldown = plugin.getConfig().getInt("security.command-cooldown", 3);
        this.optimizeMarkers = plugin.getConfig().getBoolean("performance.optimize-markers", true);
        plugin.getLogger().info("PartyManager configuration reloaded");
    }
}

