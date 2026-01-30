package com.euphoria.party.command;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.data.CommandParamType;
import cn.nukkit.command.data.CommandParameter;
import com.euphoria.party.EuphoriaPartyPlugin;
import com.euphoria.party.model.Party;

import java.util.UUID;

public class PartyAdminCommand extends Command {
    
    private final EuphoriaPartyPlugin plugin;
    
    public PartyAdminCommand(EuphoriaPartyPlugin plugin) {
        super("partyadmin", "Party administration command", "/partyadmin <disband|list|info|teleport> [args]", new String[]{"pa"});
        this.plugin = plugin;
        
        // Add command parameters for auto-completion
        this.commandParameters.clear();
        
        // Simple commands
        this.commandParameters.put("default", new CommandParameter[]{
                CommandParameter.newEnum("action", new String[]{"list", "reload", "health"})
        });
        
        // Commands with player target
        this.commandParameters.put("withPlayer", new CommandParameter[]{
                CommandParameter.newEnum("action", new String[]{"disband", "info", "teleport"}),
                CommandParameter.newType("player", CommandParamType.TARGET)
        });
    }
    
    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("euphoria.party.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }
        
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "disband":
                return handleDisband(sender, args);
            case "list":
                return handleList(sender);
            case "info":
                return handleInfo(sender, args);
            case "teleport":
                return handleTeleport(sender, args);
            case "reload":
                return handleReload(sender);
            case "health":
                return handleHealth(sender);
            case "help":
            default:
                sendHelp(sender);
                return true;
        }
    }
    
    private boolean handleDisband(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /partyadmin disband <player>");
            return false;
        }
        
        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cPlayer not found!");
            return false;
        }
        
        Party party = plugin.getPartyManager().getPlayerParty(target.getUniqueId());
        if (party == null) {
            sender.sendMessage("§c" + target.getName() + " is not in a party!");
            return false;
        }
        
        // Notify all members
        for (UUID memberId : party.getMembers()) {
            Player member = plugin.getServer().getPlayer(memberId).orElse(null);
            if (member != null) {
                member.sendMessage("§cYour party has been disbanded by an administrator.");
            }
        }
        
        plugin.getPartyManager().disbandParty(party.getId());
        sender.sendMessage("§aSuccessfully disbanded " + target.getName() + "'s party.");
        
        return true;
    }
    
    private boolean handleList(CommandSender sender) {
        var parties = plugin.getPartyManager().getAllParties();
        
        if (parties.isEmpty()) {
            sender.sendMessage("§eThere are currently no active parties.");
            return true;
        }
        
        sender.sendMessage("§8========== §6Active Parties §8==========");
        sender.sendMessage("§eTotal parties: §f" + parties.size());
        
        int count = 0;
        for (Party party : parties) {
            count++;
            Player leader = plugin.getServer().getPlayer(party.getLeader()).orElse(null);
            String leaderName = leader != null ? leader.getName() : party.getLeader().toString();
            
            int onlineCount = 0;
            for (UUID memberId : party.getMembers()) {
                Player member = plugin.getServer().getPlayer(memberId).orElse(null);
                if (member != null && member.isOnline()) {
                    onlineCount++;
                }
            }
            
            sender.sendMessage("§7" + count + ". §fLeader: §e" + leaderName + 
                " §7| §fMembers: §e" + onlineCount + "§7/§e" + party.getMemberCount() + 
                " §7| §fHome: " + (party.hasHome() ? "§a✓" : "§c✗"));
        }
        sender.sendMessage("§8================================");
        
        return true;
    }
    
    private boolean handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /partyadmin info <player>");
            return false;
        }
        
        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cPlayer not found!");
            return false;
        }
        
        Party party = plugin.getPartyManager().getPlayerParty(target.getUniqueId());
        if (party == null) {
            sender.sendMessage("§c" + target.getName() + " is not in a party!");
            return false;
        }
        
        Player leader = plugin.getServer().getPlayer(party.getLeader()).orElse(null);
        String leaderName = leader != null ? leader.getName() : party.getLeader().toString();
        
        sender.sendMessage("§8========== §6Party Info §8==========");
        sender.sendMessage("§eParty ID: §f" + party.getId());
        sender.sendMessage("§eLeader: §f" + leaderName);
        sender.sendMessage("§eMembers (§f" + party.getMemberCount() + "§e):");
        
        for (UUID memberId : party.getMembers()) {
            Player member = plugin.getServer().getPlayer(memberId).orElse(null);
            String memberName = member != null ? member.getName() : memberId.toString();
            String status = member != null && member.isOnline() ? "§a●" : "§c●";
            String role = party.isLeader(memberId) ? " §e[Leader]" : "";
            sender.sendMessage("  " + status + " §f" + memberName + role);
        }
        
        if (party.hasHome()) {
            cn.nukkit.level.Location home = party.getHome();
            sender.sendMessage("§eParty Home: §f" + home.getLevel().getName() + 
                " (" + (int)home.getX() + ", " + (int)home.getY() + ", " + (int)home.getZ() + ")");
        } else {
            sender.sendMessage("§eParty Home: §cNot set");
        }
        
        sender.sendMessage("§8================================");
        
        return true;
    }
    
    private boolean handleTeleport(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return false;
        }
        
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /partyadmin teleport <player>");
            return false;
        }
        
        Player admin = (Player) sender;
        Player target = plugin.getServer().getPlayer(args[1]);
        
        if (target == null) {
            sender.sendMessage("§cPlayer not found!");
            return false;
        }
        
        Party party = plugin.getPartyManager().getPlayerParty(target.getUniqueId());
        if (party == null) {
            sender.sendMessage("§c" + target.getName() + " is not in a party!");
            return false;
        }
        
        if (!party.hasHome()) {
            sender.sendMessage("§cThat party doesn't have a home set!");
            return false;
        }
        
        admin.teleport(party.getHome());
        sender.sendMessage("§aTeleported to " + target.getName() + "'s party home.");
        
        return true;
    }
    
    private boolean handleReload(CommandSender sender) {
        try {
            plugin.reloadConfiguration();
            sender.sendMessage("§aConfiguration reloaded successfully!");
            sender.sendMessage("§7All managers have been updated with the new configuration.");
        } catch (Exception e) {
            sender.sendMessage("§cError reloading configuration: " + e.getMessage());
            plugin.getLogger().error("Error in reload command", e);
        }
        return true;
    }
    
    private boolean handleHealth(CommandSender sender) {
        com.euphoria.party.util.HealthCheck healthCheck = 
            new com.euphoria.party.util.HealthCheck(plugin);
        sender.sendMessage(healthCheck.formatHealthStatus());
        return true;
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§8========== §6Party Admin Commands §8==========");
        sender.sendMessage("§e/partyadmin disband <player> §7- Force disband a party");
        sender.sendMessage("§e/partyadmin list §7- List all active parties");
        sender.sendMessage("§e/partyadmin info <player> §7- View detailed party info");
        sender.sendMessage("§e/partyadmin teleport <player> §7- Teleport to a party's home");
        sender.sendMessage("§e/partyadmin reload §7- Reload the configuration");
        sender.sendMessage("§e/partyadmin health §7- Check plugin health status");
        sender.sendMessage("§8================================");
    }
}
