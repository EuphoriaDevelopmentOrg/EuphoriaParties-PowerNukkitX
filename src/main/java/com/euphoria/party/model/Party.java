package com.euphoria.party.model;

import cn.nukkit.Player;
import cn.nukkit.level.Location;

import java.util.*;

public class Party {
    
    // Basic party info
    private final UUID id;
    private UUID leader;
    private String name;
    private final Set<UUID> members;
    private final Map<UUID, Long> invites;
    private final Map<UUID, Long> joinRequests;
    private Location home;
    private final long createdAt;
    private boolean isPublic;
    
    // Roles system
    private final Map<UUID, PartyRole> memberRoles;
    
    // Ban list
    private final Set<UUID> bannedPlayers;
    
    // Statistics
    private long totalPlayTime;
    private int totalKills;
    private int totalDeaths;
    
    // Customization
    private String color;
    private String icon;
    
    // Allies
    private final Set<UUID> allies;
    
    // Daily rewards tracking
    private final Map<UUID, Long> lastDailyReward;
    private int consecutiveDays;
    private long lastRewardDate;
    
    // Achievements
    private final Set<String> unlockedAchievements;
    
    public Party(UUID leader) {
        this.id = UUID.randomUUID();
        this.leader = leader;
        this.name = null;
        this.members = new HashSet<>();
        this.invites = new HashMap<>();
        this.joinRequests = new HashMap<>();
        this.members.add(leader);
        this.createdAt = System.currentTimeMillis();
        this.isPublic = true;
        this.memberRoles = new HashMap<>();
        this.memberRoles.put(leader, PartyRole.LEADER);
        this.bannedPlayers = new HashSet<>();
        this.totalPlayTime = 0;
        this.totalKills = 0;
        this.totalDeaths = 0;
        this.color = "§6";
        this.icon = "★";
        this.allies = new HashSet<>();
        this.lastDailyReward = new HashMap<>();
        this.consecutiveDays = 0;
        this.lastRewardDate = 0;
        this.unlockedAchievements = new HashSet<>();
    }
    
    // Constructor for loading from storage
    public Party(UUID id, UUID leader) {
        this.id = id;
        this.leader = leader;
        this.name = null;
        this.members = new HashSet<>();
        this.invites = new HashMap<>();
        this.joinRequests = new HashMap<>();
        this.members.add(leader);
        this.createdAt = System.currentTimeMillis();
        this.isPublic = true;
        this.memberRoles = new HashMap<>();
        this.memberRoles.put(leader, PartyRole.LEADER);
        this.bannedPlayers = new HashSet<>();
        this.totalPlayTime = 0;
        this.totalKills = 0;
        this.totalDeaths = 0;
        this.color = "§6";
        this.icon = "★";
        this.allies = new HashSet<>();
        this.lastDailyReward = new HashMap<>();
        this.consecutiveDays = 0;
        this.lastRewardDate = 0;
        this.unlockedAchievements = new HashSet<>();
    }
    
    public UUID getId() {
        return id;
    }
    
    public UUID getLeader() {
        return leader;
    }
    
    public Set<UUID> getMembers() {
        return new HashSet<>(members);
    }
    
    public boolean isMember(UUID playerId) {
        return members.contains(playerId);
    }
    
    public boolean isLeader(UUID playerId) {
        return leader.equals(playerId);
    }
    
    /**
     * Transfer leadership to another member
     */
    public boolean transferLeadership(UUID newLeaderId) {
        if (!isMember(newLeaderId)) {
            return false;
        }
        this.memberRoles.put(this.leader, PartyRole.OFFICER);
        this.leader = newLeaderId;
        this.memberRoles.put(newLeaderId, PartyRole.LEADER);
        return true;
    }
    
    // Party name methods
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public boolean hasName() {
        return name != null && !name.isEmpty();
    }
    
    // Role methods
    public PartyRole getRole(UUID playerId) {
        return memberRoles.getOrDefault(playerId, PartyRole.MEMBER);
    }
    
    public void setRole(UUID playerId, PartyRole role) {
        if (isMember(playerId) && !isLeader(playerId)) {
            memberRoles.put(playerId, role);
        }
    }
    
    // Public/Private methods
    public boolean isPublic() {
        return isPublic;
    }
    
    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }
    
    // Join request methods
    public void addJoinRequest(UUID playerId) {
        joinRequests.put(playerId, System.currentTimeMillis());
    }
    
    public boolean hasJoinRequest(UUID playerId) {
        return joinRequests.containsKey(playerId);
    }
    
    public void removeJoinRequest(UUID playerId) {
        joinRequests.remove(playerId);
    }
    
    public Map<UUID, Long> getJoinRequests() {
        return new HashMap<>(joinRequests);
    }
    
    public void cleanExpiredJoinRequests(long expirationTime) {
        long currentTime = System.currentTimeMillis();
        joinRequests.entrySet().removeIf(entry -> (currentTime - entry.getValue()) > expirationTime);
    }
    
    // Ban list methods
    public void banPlayer(UUID playerId) {
        bannedPlayers.add(playerId);
        members.remove(playerId);
        invites.remove(playerId);
        joinRequests.remove(playerId);
        memberRoles.remove(playerId);
    }
    
    public void unbanPlayer(UUID playerId) {
        bannedPlayers.remove(playerId);
    }
    
    public boolean isBanned(UUID playerId) {
        return bannedPlayers.contains(playerId);
    }
    
    public Set<UUID> getBannedPlayers() {
        return new HashSet<>(bannedPlayers);
    }
    
    // Statistics methods
    public void addPlayTime(long milliseconds) {
        this.totalPlayTime += milliseconds;
    }
    
    public void incrementKills() {
        this.totalKills++;
    }
    
    public void incrementDeaths() {
        this.totalDeaths++;
    }
    
    public long getTotalPlayTime() {
        return totalPlayTime;
    }
    
    public int getTotalKills() {
        return totalKills;
    }
    
    public int getTotalDeaths() {
        return totalDeaths;
    }
    
    public void addMember(UUID playerId) {
        members.add(playerId);
        invites.remove(playerId);
        joinRequests.remove(playerId);
        if (!memberRoles.containsKey(playerId)) {
            memberRoles.put(playerId, PartyRole.MEMBER);
        }
    }
    
    public void removeMember(UUID playerId) {
        members.remove(playerId);
        if (!isLeader(playerId)) {
            memberRoles.remove(playerId);
        }
    }
    
    public int getMemberCount() {
        return members.size();
    }
    
    public void invitePlayer(UUID playerId) {
        invites.put(playerId, System.currentTimeMillis());
    }
    
    public boolean hasInvite(UUID playerId) {
        return invites.containsKey(playerId);
    }
    
    public boolean isInviteExpired(UUID playerId, long expirationTime) {
        Long inviteTime = invites.get(playerId);
        if (inviteTime == null) {
            return true;
        }
        return (System.currentTimeMillis() - inviteTime) > expirationTime;
    }
    
    public void cleanExpiredInvites(long expirationTime) {
        long currentTime = System.currentTimeMillis();
        invites.entrySet().removeIf(entry -> (currentTime - entry.getValue()) > expirationTime);
    }
    
    public void removeInvite(UUID playerId) {
        invites.remove(playerId);
    }
    
    public Map<UUID, Long> getInvites() {
        return new HashMap<>(invites);
    }
    
    public Location getHome() {
        return home;
    }
    
    public void setHome(Location home) {
        this.home = home;
    }
    
    public boolean hasHome() {
        return home != null;
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    // Color and icon methods
    public String getColor() {
        return color;
    }
    
    public void setColor(String color) {
        this.color = color;
    }
    
    public String getIcon() {
        return icon;
    }
    
    public void setIcon(String icon) {
        this.icon = icon;
    }
    
    // Ally methods
    public void addAlly(UUID partyId) {
        allies.add(partyId);
    }
    
    public void removeAlly(UUID partyId) {
        allies.remove(partyId);
    }
    
    public boolean isAlly(UUID partyId) {
        return allies.contains(partyId);
    }
    
    public Set<UUID> getAllies() {
        return new HashSet<>(allies);
    }
    
    // Daily reward methods
    public boolean canClaimDailyReward(UUID playerId) {
        Long lastClaim = lastDailyReward.get(playerId);
        if (lastClaim == null) {
            return true;
        }
        long daysSince = (System.currentTimeMillis() - lastClaim) / (1000 * 60 * 60 * 24);
        return daysSince >= 1;
    }
    
    public void claimDailyReward(UUID playerId) {
        lastDailyReward.put(playerId, System.currentTimeMillis());
        
        // Check if it's consecutive day
        long daysSince = 0;
        if (lastRewardDate > 0) {
            daysSince = (System.currentTimeMillis() - lastRewardDate) / (1000 * 60 * 60 * 24);
        }
        
        if (daysSince == 1) {
            consecutiveDays++;
        } else if (daysSince > 1) {
            consecutiveDays = 1;
        }
        
        lastRewardDate = System.currentTimeMillis();
    }
    
    public int getConsecutiveDays() {
        return consecutiveDays;
    }
    
    public Map<UUID, Long> getLastDailyReward() {
        return new HashMap<>(lastDailyReward);
    }
    
    // Achievement methods
    public void unlockAchievement(String achievementId) {
        unlockedAchievements.add(achievementId);
    }
    
    public boolean hasAchievement(String achievementId) {
        return unlockedAchievements.contains(achievementId);
    }
    
    public Set<String> getUnlockedAchievements() {
        return new HashSet<>(unlockedAchievements);
    }
    
    public int getAchievementCount() {
        return unlockedAchievements.size();
    }
}
