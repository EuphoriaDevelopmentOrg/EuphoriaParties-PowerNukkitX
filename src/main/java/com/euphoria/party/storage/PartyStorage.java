package com.euphoria.party.storage;

import cn.nukkit.level.Location;
import com.euphoria.party.model.Party;
import com.euphoria.party.model.PartyRole;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

public class PartyStorage {
    
    private final File dataFile;
    private final Gson gson;
    private Map<UUID, LocationData> homeDataCache = new HashMap<>();
    
    public PartyStorage(File dataFolder) {
        this.dataFile = new File(dataFolder, "parties.json");
        this.gson = new GsonBuilder()
                .registerTypeAdapter(Location.class, new LocationAdapter())
                .setPrettyPrinting()
                .create();
    }
    
    public Map<UUID, Party> loadParties() {
        Map<UUID, Party> parties = new HashMap<>();
        homeDataCache.clear();
        
        if (!dataFile.exists()) {
            return parties;
        }
        
        try (Reader reader = new FileReader(dataFile)) {
            Type type = new TypeToken<List<PartyData>>(){}.getType();
            List<PartyData> partyDataList = gson.fromJson(reader, type);
            
            if (partyDataList != null) {
                for (PartyData data : partyDataList) {
                    try {
                        Party party = data.toParty();
                        if (party != null) {
                            parties.put(party.getId(), party);
                            
                            // Cache location data for reconstruction
                            if (data.getHomeData() != null) {
                                homeDataCache.put(party.getId(), data.getHomeData());
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Error loading party data, skipping: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading party data file: " + e.getMessage());
            e.printStackTrace();
            
            // Try to load from backup
            File backupFile = new File(dataFile.getParentFile(), "parties.json.backup");
            if (backupFile.exists()) {
                System.err.println("Attempting to load from backup file...");
                try (Reader reader = new FileReader(backupFile)) {
                    Type type = new TypeToken<List<PartyData>>(){}.getType();
                    List<PartyData> partyDataList = gson.fromJson(reader, type);
                    
                    if (partyDataList != null) {
                        for (PartyData data : partyDataList) {
                            try {
                                Party party = data.toParty();
                                if (party != null) {
                                    parties.put(party.getId(), party);
                                    if (data.getHomeData() != null) {
                                        homeDataCache.put(party.getId(), data.getHomeData());
                                    }
                                }
                            } catch (Exception ex) {
                                // Skip corrupted entries
                            }
                        }
                        System.err.println("Successfully loaded " + parties.size() + " parties from backup");
                    }
                } catch (IOException ex) {
                    System.err.println("Backup file also corrupted: " + ex.getMessage());
                }
            }
        } catch (JsonSyntaxException e) {
            System.err.println("Party data file is corrupted: " + e.getMessage());
            e.printStackTrace();
        }
        
        return parties;
    }
    
    public LocationData getHomeData(UUID partyId) {
        return homeDataCache.get(partyId);
    }
    
    public void saveParties(Map<UUID, Party> parties) {
        try {
            if (!dataFile.getParentFile().exists()) {
                dataFile.getParentFile().mkdirs();
            }
            
            // Create backup before saving
            if (dataFile.exists()) {
                File backupFile = new File(dataFile.getParentFile(), "parties.json.backup");
                try {
                    java.nio.file.Files.copy(dataFile.toPath(), backupFile.toPath(), 
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    // Log but don't fail if backup creation fails
                    System.err.println("Warning: Could not create backup file: " + e.getMessage());
                }
            }
            
            List<PartyData> partyDataList = new ArrayList<>();
            for (Party party : parties.values()) {
                if (party != null) {
                    partyDataList.add(PartyData.fromParty(party));
                }
            }
            
            // Write to temporary file first, then rename (atomic operation)
            File tempFile = new File(dataFile.getParentFile(), "parties.json.tmp");
            try (Writer writer = new FileWriter(tempFile)) {
                gson.toJson(partyDataList, writer);
            }
            
            // Rename temp file to actual file (atomic on most systems)
            if (!tempFile.renameTo(dataFile)) {
                throw new IOException("Failed to rename temp file to data file");
            }
            
        } catch (IOException e) {
            System.err.println("Critical error saving party data: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Data class for JSON serialization
    private static class PartyData {
        String id;
        String leader;
        List<String> members;
        LocationData home;
        String name;
        String color;
        String icon;
        Map<String, String> roles;
        List<String> bannedPlayers;
        List<String> allies;
        long totalPlayTime;
        int totalKills;
        int totalDeaths;
        List<String> achievements;
        long lastRewardClaim;
        int consecutiveDays;
        long creationTime;
        
        static PartyData fromParty(Party party) {
            PartyData data = new PartyData();
            data.id = party.getId().toString();
            data.leader = party.getLeader().toString();
            data.members = new ArrayList<>();
            for (UUID memberId : party.getMembers()) {
                data.members.add(memberId.toString());
            }
            if (party.getHome() != null) {
                data.home = LocationData.fromLocation(party.getHome());
            }
            
            // Save new fields
            data.name = party.getName();
            data.color = party.getColor();
            data.icon = party.getIcon();
            
            // Save roles
            data.roles = new HashMap<>();
            for (UUID memberId : party.getMembers()) {
                PartyRole role = party.getRole(memberId);
                data.roles.put(memberId.toString(), role.name());
            }
            
            // Save banned players
            data.bannedPlayers = new ArrayList<>();
            for (UUID banned : party.getBannedPlayers()) {
                data.bannedPlayers.add(banned.toString());
            }
            
            // Save allies
            data.allies = new ArrayList<>();
            for (UUID ally : party.getAllies()) {
                data.allies.add(ally.toString());
            }
            
            // Save statistics
            data.totalPlayTime = party.getTotalPlayTime();
            data.totalKills = party.getTotalKills();
            data.totalDeaths = party.getTotalDeaths();
            
            // Save achievements
            data.achievements = new ArrayList<>(party.getUnlockedAchievements());
            
            // Save daily reward data
            data.lastRewardClaim = party.getLastDailyReward().isEmpty() ? 0 : 
                party.getLastDailyReward().values().stream().max(Long::compare).orElse(0L);
            data.consecutiveDays = party.getConsecutiveDays();
            data.creationTime = party.getCreatedAt();
            
            return data;
        }
        
        Party toParty() {
            UUID partyId = UUID.fromString(id);
            UUID leaderId = UUID.fromString(leader);
            Party party = new Party(partyId, leaderId);
            
            for (String memberId : members) {
                party.addMember(UUID.fromString(memberId));
            }
            
            // Note: home will be reconstructed in PartyManager with server instance
            // We store the LocationData here for PartyManager to access
            
            // Restore new fields
            if (name != null) party.setName(name);
            if (color != null) party.setColor(color);
            if (icon != null) party.setIcon(icon);
            
            // Restore roles
            if (roles != null) {
                for (Map.Entry<String, String> entry : roles.entrySet()) {
                    try {
                        UUID memberId = UUID.fromString(entry.getKey());
                        com.euphoria.party.model.PartyRole role = com.euphoria.party.model.PartyRole.valueOf(entry.getValue());
                        party.setRole(memberId, role);
                    } catch (Exception e) {
                        // Skip invalid roles
                    }
                }
            }
            
            // Restore banned players
            if (bannedPlayers != null) {
                for (String banned : bannedPlayers) {
                    party.banPlayer(UUID.fromString(banned));
                }
            }
            
            // Restore allies
            if (allies != null) {
                for (String ally : allies) {
                    party.addAlly(UUID.fromString(ally));
                }
            }
            
            // Restore statistics
            if (totalPlayTime > 0) {
                party.addPlayTime(totalPlayTime);
            }
            for (int i = 0; i < totalKills; i++) {
                party.incrementKills();
            }
            for (int i = 0; i < totalDeaths; i++) {
                party.incrementDeaths();
            }
            
            // Restore achievements
            if (achievements != null) {
                for (String achievement : achievements) {
                    party.unlockAchievement(achievement);
                }
            }
            
            // Restore daily reward data
            if (lastRewardClaim > 0) {
                // Access via reflection or add setter methods
                try {
                    java.lang.reflect.Field lastRewardField = Party.class.getDeclaredField("lastRewardDate");
                    lastRewardField.setAccessible(true);
                    lastRewardField.setLong(party, lastRewardClaim);
                    
                    java.lang.reflect.Field consecutiveDaysField = Party.class.getDeclaredField("consecutiveDays");
                    consecutiveDaysField.setAccessible(true);
                    consecutiveDaysField.setInt(party, consecutiveDays);
                } catch (Exception e) {
                    // Ignore if fields don't exist
                }
            }
            
            // Restore creation time
            if (creationTime > 0) {
                try {
                    java.lang.reflect.Field createdAtField = Party.class.getDeclaredField("createdAt");
                    createdAtField.setAccessible(true);
                    createdAtField.setLong(party, creationTime);
                } catch (Exception e) {
                    // Ignore if field doesn't exist
                }
            }
            
            return party;
        }
        
        LocationData getHomeData() {
            return home;
        }
    }
    
    // Location data class - make public static so PartyManager can access
    public static class LocationData {
        public String level;
        public double x;
        public double y;
        public double z;
        public float yaw;
        public float pitch;
        
        static LocationData fromLocation(Location loc) {
            LocationData data = new LocationData();
            data.level = loc.getLevel().getName();
            data.x = loc.getX();
            data.y = loc.getY();
            data.z = loc.getZ();
            data.yaw = (float) loc.getYaw();
            data.pitch = (float) loc.getPitch();
            return data;
        }
        
        Location toLocation() {
            // This will need to be reconstructed with server instance
            // We'll handle this in PartyManager
            return null;
        }
    }
    
    // Custom Location adapter for Gson
    private static class LocationAdapter implements JsonSerializer<Location>, JsonDeserializer<Location> {
        @Override
        public JsonElement serialize(Location src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject obj = new JsonObject();
            obj.addProperty("level", src.getLevel().getName());
            obj.addProperty("x", src.getX());
            obj.addProperty("y", src.getY());
            obj.addProperty("z", src.getZ());
            obj.addProperty("yaw", src.getYaw());
            obj.addProperty("pitch", src.getPitch());
            return obj;
        }
        
        @Override
        public Location deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            // Return null, we'll reconstruct in PartyManager with server instance
            return null;
        }
    }
}
