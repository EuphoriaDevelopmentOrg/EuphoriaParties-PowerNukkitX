package com.euphoria.party.model;

public enum PartyRole {
    LEADER(3),      // Can do everything
    OFFICER(2),     // Can invite, kick, set home
    MEMBER(1),      // Standard member
    RECRUIT(0);     // Limited permissions
    
    private final int level;
    
    PartyRole(int level) {
        this.level = level;
    }
    
    public int getLevel() {
        return level;
    }
    
    public boolean canInvite() {
        return level >= OFFICER.level;
    }
    
    public boolean canKick() {
        return level >= OFFICER.level;
    }
    
    public boolean canSetHome() {
        return level >= OFFICER.level;
    }
    
    public boolean canPromote() {
        return level >= LEADER.level;
    }
    
    public boolean canDisband() {
        return level >= LEADER.level;
    }
    
    public boolean canChangeName() {
        return level >= LEADER.level;
    }
    
    public boolean canBanPlayers() {
        return level >= OFFICER.level;
    }
}
