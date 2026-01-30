package com.euphoria.party.integration;

import cn.nukkit.Player;
import com.euphoria.party.EuphoriaPartyPlugin;
import com.euphoria.party.model.Party;
import org.powernukkitx.placeholderapi.PlaceholderAPI;

import java.util.UUID;

public class PartyPlaceholders {
    
    private final EuphoriaPartyPlugin plugin;
    
    public PartyPlaceholders(EuphoriaPartyPlugin plugin) {
        this.plugin = plugin;
    }
    
    public void register() {
        PlaceholderAPI api = PlaceholderAPI.get();
        
        // %euphoria_party_size% - Number of members in player's party
        api.register("euphoria_party_size", (player, params) -> {
            Party party = plugin.getPartyManager().getPlayerParty(player.getUniqueId());
            return party != null ? String.valueOf(party.getMembers().size()) : "0";
        });
        
        // %euphoria_party_leader% - Name of party leader
        api.register("euphoria_party_leader", (player, params) -> {
            Party party = plugin.getPartyManager().getPlayerParty(player.getUniqueId());
            if (party == null) return "None";
            Player leader = plugin.getServer().getPlayer(party.getLeader()).get();
            return leader != null ? leader.getName() : "Unknown";
        });
        
        // %euphoria_party_members% - List of party member names
        api.register("euphoria_party_members", (player, params) -> {
            Party party = plugin.getPartyManager().getPlayerParty(player.getUniqueId());
            if (party == null) return "None";
            
            StringBuilder members = new StringBuilder();
            for (UUID memberId : party.getMembers()) {
                Player member = plugin.getServer().getPlayer(memberId).get();
                if (member != null) {
                    if (members.length() > 0) members.append(", ");
                    members.append(member.getName());
                }
            }
            return members.length() > 0 ? members.toString() : "None";
        });
        
        // %euphoria_party_online% - Number of online party members
        api.register("euphoria_party_online", (player, params) -> {
            Party party = plugin.getPartyManager().getPlayerParty(player.getUniqueId());
            if (party == null) return "0";
            
            int online = 0;
            for (UUID memberId : party.getMembers()) {
                if (plugin.getServer().getPlayer(memberId).isPresent()) {
                    online++;
                }
            }
            return String.valueOf(online);
        });
        
        // %euphoria_party_has_home% - Whether party has a home set
        api.register("euphoria_party_has_home", (player, params) -> {
            Party party = plugin.getPartyManager().getPlayerParty(player.getUniqueId());
            return party != null && party.getHome() != null ? "Yes" : "No";
        });
        
        // %euphoria_party_is_leader% - Whether player is party leader
        api.register("euphoria_party_is_leader", (player, params) -> {
            Party party = plugin.getPartyManager().getPlayerParty(player.getUniqueId());
            return party != null && party.getLeader().equals(player.getUniqueId()) ? "Yes" : "No";
        });
        
        // %euphoria_party_invites% - Number of pending invites
        api.register("euphoria_party_invites", (player, params) -> {
            int count = 0;
            long expirationTime = plugin.getConfig().getLong("party.invite-expiration", 300000);
            for (Party party : plugin.getPartyManager().getAllParties()) {
                if (party.hasInvite(player.getUniqueId()) && !party.isInviteExpired(player.getUniqueId(), expirationTime)) {
                    count++;
                }
            }
            return String.valueOf(count);
        });
        
        // Placeholders registered
    }
}
