package com.euphoria.party.manager;

import com.euphoria.party.EuphoriaPartyPlugin;
import com.euphoria.party.model.Party;

import java.util.*;
import java.util.stream.Collectors;

public class PartyLeaderboardManager {
    
    private final EuphoriaPartyPlugin plugin;
    
    public PartyLeaderboardManager(EuphoriaPartyPlugin plugin) {
        this.plugin = plugin;
    }
    
    public List<Party> getTopPartiesByKills(int limit) {
        // Try cache first
        List<Party> cached = plugin.getPartyManager().getCachedLeaderboard("kills", limit);
        if (cached != null) {
            return cached;
        }
        
        // Compute and cache
        List<Party> result = plugin.getPartyManager().getAllParties().stream()
            .sorted((p1, p2) -> Integer.compare(p2.getTotalKills(), p1.getTotalKills()))
            .limit(limit)
            .collect(Collectors.toList());
        
        plugin.getPartyManager().cacheLeaderboard("kills", limit, result);
        return result;
    }
    
    public List<Party> getTopPartiesByPlaytime(int limit) {
        // Try cache first
        List<Party> cached = plugin.getPartyManager().getCachedLeaderboard("playtime", limit);
        if (cached != null) {
            return cached;
        }
        
        // Compute and cache
        List<Party> result = plugin.getPartyManager().getAllParties().stream()
            .sorted((p1, p2) -> Long.compare(p2.getTotalPlayTime(), p1.getTotalPlayTime()))
            .limit(limit)
            .collect(Collectors.toList());
        
        plugin.getPartyManager().cacheLeaderboard("playtime", limit, result);
        return result;
    }
    
    public List<Party> getTopPartiesByMembers(int limit) {
        // Try cache first
        List<Party> cached = plugin.getPartyManager().getCachedLeaderboard("members", limit);
        if (cached != null) {
            return cached;
        }
        
        // Compute and cache
        List<Party> result = plugin.getPartyManager().getAllParties().stream()
            .sorted((p1, p2) -> Integer.compare(p2.getMemberCount(), p1.getMemberCount()))
            .limit(limit)
            .collect(Collectors.toList());
        
        plugin.getPartyManager().cacheLeaderboard("members", limit, result);
        return result;
    }
    
    public List<Party> getTopPartiesByKD(int limit) {
        // Try cache first
        List<Party> cached = plugin.getPartyManager().getCachedLeaderboard("kd", limit);
        if (cached != null) {
            return cached;
        }
        
        // Compute and cache
        List<Party> result = plugin.getPartyManager().getAllParties().stream()
            .filter(p -> p.getTotalDeaths() > 0)
            .sorted((p1, p2) -> {
                double kd1 = (double) p1.getTotalKills() / p1.getTotalDeaths();
                double kd2 = (double) p2.getTotalKills() / p2.getTotalDeaths();
                return Double.compare(kd2, kd1);
            })
            .limit(limit)
            .collect(Collectors.toList());
        
        plugin.getPartyManager().cacheLeaderboard("kd", limit, result);
        return result;
    }
    
    public List<Party> getTopPartiesByAchievements(int limit) {
        // Try cache first
        List<Party> cached = plugin.getPartyManager().getCachedLeaderboard("achievements", limit);
        if (cached != null) {
            return cached;
        }
        
        // Compute and cache
        List<Party> result = plugin.getPartyManager().getAllParties().stream()
            .sorted((p1, p2) -> Integer.compare(p2.getAchievementCount(), p1.getAchievementCount()))
            .limit(limit)
            .collect(Collectors.toList());
        
        plugin.getPartyManager().cacheLeaderboard("achievements", limit, result);
        return result;
    }
    
    public int getPartyRankByKills(Party party) {
        List<Party> sorted = plugin.getPartyManager().getAllParties().stream()
            .sorted((p1, p2) -> Integer.compare(p2.getTotalKills(), p1.getTotalKills()))
            .collect(Collectors.toList());
        
        return sorted.indexOf(party) + 1;
    }
    
    public int getPartyRankByPlaytime(Party party) {
        List<Party> sorted = plugin.getPartyManager().getAllParties().stream()
            .sorted((p1, p2) -> Long.compare(p2.getTotalPlayTime(), p1.getTotalPlayTime()))
            .collect(Collectors.toList());
        
        return sorted.indexOf(party) + 1;
    }
}
