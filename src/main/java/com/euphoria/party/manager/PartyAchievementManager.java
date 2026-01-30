package com.euphoria.party.manager;

import cn.nukkit.Player;
import com.euphoria.party.EuphoriaPartyPlugin;
import com.euphoria.party.model.Party;
import com.euphoria.party.model.PartyAchievement;

import java.util.*;

public class PartyAchievementManager {
    
    private final EuphoriaPartyPlugin plugin;
    private final Map<String, PartyAchievement> achievements;
    
    public PartyAchievementManager(EuphoriaPartyPlugin plugin) {
        this.plugin = plugin;
        this.achievements = new HashMap<>();
        registerAchievements();
    }
    
    private void registerAchievements() {
        // Member-based achievements
        achievements.put("party_started", new PartyAchievement(
            "party_started", "§6Party Started", "§7Create your first party", 1, "xp", 100
        ));
        achievements.put("team_player", new PartyAchievement(
            "team_player", "§6Team Player", "§7Have 5 members in your party", 5, "xp", 250
        ));
        achievements.put("full_house", new PartyAchievement(
            "full_house", "§6Full House", "§7Fill your party to max capacity", 8, "xp", 500
        ));
        
        // Time-based achievements
        achievements.put("dedicated", new PartyAchievement(
            "dedicated", "§6Dedicated", "§710 hours of party playtime", 36000000, "xp", 500
        ));
        achievements.put("veteran", new PartyAchievement(
            "veteran", "§6Veteran", "§750 hours of party playtime", 180000000, "xp", 2000
        ));
        
        // Combat achievements
        achievements.put("first_blood", new PartyAchievement(
            "first_blood", "§6First Blood", "§7Get 10 party kills", 10, "xp", 200
        ));
        achievements.put("slayer", new PartyAchievement(
            "slayer", "§6Slayer", "§7Get 100 party kills", 100, "xp", 1000
        ));
        achievements.put("survivor", new PartyAchievement(
            "survivor", "§6Survivor", "§7Reach 2.0 K/D ratio", 0, "xp", 750
        ));
        
        // Daily streak achievements
        achievements.put("consistent", new PartyAchievement(
            "consistent", "§6Consistent", "§7Claim rewards for 7 days straight", 7, "xp", 500
        ));
        achievements.put("devoted", new PartyAchievement(
            "devoted", "§6Devoted", "§7Claim rewards for 30 days straight", 30, "xp", 2500
        ));
    }
    
    public void checkAchievements(Party party) {
        // Check member count achievements
        int memberCount = party.getMemberCount();
        checkAndUnlock(party, "party_started", memberCount >= 1);
        checkAndUnlock(party, "team_player", memberCount >= 5);
        checkAndUnlock(party, "full_house", memberCount >= plugin.getConfig().getInt("party.max-members", 8));
        
        // Check playtime achievements
        long playTime = party.getTotalPlayTime();
        checkAndUnlock(party, "dedicated", playTime >= 36000000); // 10 hours
        checkAndUnlock(party, "veteran", playTime >= 180000000); // 50 hours
        
        // Check combat achievements
        int kills = party.getTotalKills();
        checkAndUnlock(party, "first_blood", kills >= 10);
        checkAndUnlock(party, "slayer", kills >= 100);
        
        // Check K/D ratio
        if (party.getTotalDeaths() > 0) {
            double kd = (double) party.getTotalKills() / party.getTotalDeaths();
            checkAndUnlock(party, "survivor", kd >= 2.0);
        }
        
        // Check daily streak achievements
        int streak = party.getConsecutiveDays();
        checkAndUnlock(party, "consistent", streak >= 7);
        checkAndUnlock(party, "devoted", streak >= 30);
    }
    
    private void checkAndUnlock(Party party, String achievementId, boolean condition) {
        if (condition && !party.hasAchievement(achievementId)) {
            party.unlockAchievement(achievementId);
            
            PartyAchievement achievement = achievements.get(achievementId);
            if (achievement != null) {
                // Notify all party members
                for (UUID memberId : party.getMembers()) {
                    Player member = plugin.getServer().getPlayer(memberId).orElse(null);
                    if (member != null) {
                        member.sendMessage("§8[§6Party§8] §eAchievement Unlocked!");
                        member.sendMessage(achievement.getName() + " §7- " + achievement.getDescription());
                        member.sendMessage("§7Reward: §e+" + achievement.getRewardAmount() + " XP");
                        
                        // Give reward
                        if (achievement.getRewardType().equals("xp")) {
                            member.addExperience(achievement.getRewardAmount());
                        }
                    }
                }
            }
        }
    }
    
    public Collection<PartyAchievement> getAllAchievements() {
        return achievements.values();
    }
    
    public PartyAchievement getAchievement(String id) {
        return achievements.get(id);
    }
}
