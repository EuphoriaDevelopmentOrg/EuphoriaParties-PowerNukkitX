package com.euphoria.party.model;

public class PartyAchievement {
    
    private final String id;
    private final String name;
    private final String description;
    private final int requirement;
    private final String rewardType;
    private final int rewardAmount;
    
    public PartyAchievement(String id, String name, String description, int requirement, String rewardType, int rewardAmount) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.requirement = requirement;
        this.rewardType = rewardType;
        this.rewardAmount = rewardAmount;
    }
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public int getRequirement() {
        return requirement;
    }
    
    public String getRewardType() {
        return rewardType;
    }
    
    public int getRewardAmount() {
        return rewardAmount;
    }
}
