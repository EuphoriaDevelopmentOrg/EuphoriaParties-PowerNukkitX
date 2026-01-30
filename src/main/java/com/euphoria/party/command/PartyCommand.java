package com.euphoria.party.command;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.data.CommandParamType;
import cn.nukkit.command.data.CommandParameter;
import cn.nukkit.level.Location;
import com.euphoria.party.EuphoriaPartyPlugin;
import com.euphoria.party.model.Party;

import java.util.Collection;
import java.util.UUID;

public class PartyCommand extends Command {
    
    private final EuphoriaPartyPlugin plugin;
    
    public PartyCommand(EuphoriaPartyPlugin plugin) {
        super("party", "Party management command", "/party <create|invite|accept|leave|kick|promote|list|sethome|home|help>", new String[]{"p"});
        this.plugin = plugin;
        
        // Add command parameters for auto-completion
        this.commandParameters.clear();
        
        // Simple commands without additional parameters
        this.commandParameters.put("default", new CommandParameter[]{
                CommandParameter.newEnum("action", new String[]{"create", "accept", "leave", "list", "sethome", "home", "help", "join", "requests", "public", "private", "stats", "daily", "scoreboard", "leaderboard", "achievements", "color", "icon", "ally"})
        });
        
        // Commands that require a player target
        this.commandParameters.put("withPlayer", new CommandParameter[]{
                CommandParameter.newEnum("action", new String[]{"invite", "kick", "promote", "setrank", "ban", "unban"}),
                CommandParameter.newType("player", CommandParamType.TARGET)
        });
        
        // Commands with text input
        this.commandParameters.put("withName", new CommandParameter[]{
                CommandParameter.newEnum("action", new String[]{"name"}),
                CommandParameter.newType("name", CommandParamType.TEXT)
        });
    }
    
    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "create":
                return handleCreate(player);
            case "invite":
                return handleInvite(player, args);
            case "accept":
                return handleAccept(player);
            case "leave":
                return handleLeave(player);
            case "kick":
                return handleKick(player, args);
            case "promote":
                return handlePromote(player, args);
            case "list":
                return handleList(player);
            case "info":
                return handleInfo(player);
            case "sethome":
                return handleSetHome(player);
            case "home":
                return handleHome(player);
            case "warpleader":
            case "warp":
                return handleWarpLeader(player);
            case "name":
                return handleName(player, args);
            case "join":
                return handleJoin(player, args);
            case "requests":
                return handleRequests(player);
            case "acceptrequest":
            case "arequest":
                return handleAcceptRequest(player, args);
            case "denyrequest":
            case "drequest":
                return handleDenyRequest(player, args);
            case "public":
                return handlePublic(player);
            case "private":
                return handlePrivate(player);
            case "setrank":
                return handleSetRank(player, args);
            case "ban":
                return handleBan(player, args);
            case "unban":
                return handleUnban(player, args);
            case "stats":
                return handleStats(player);
            case "daily":
            case "dailyreward":
                return handleDailyReward(player);
            case "scoreboard":
            case "sb":
                return handleScoreboard(player);
            case "leaderboard":
            case "lb":
            case "top":
                return handleLeaderboard(player, args);
            case "achievements":
            case "achievement":
                return handleAchievements(player);
            case "color":
                return handleColor(player, args);
            case "icon":
                return handleIcon(player, args);
            case "ally":
                return handleAlly(player, args);
            case "help":
            default:
                sendHelp(player);
                return true;
        }
    }
    
    private boolean handleCreate(Player player) {
        if (!player.hasPermission("euphoria.party.create")) {
            player.sendMessage("§cYou don't have permission to create parties!");
            return true;
        }
        
        Party party = plugin.getPartyManager().createParty(player);
        if (party == null) {
            player.sendMessage(plugin.getMessage("already-in-party"));
            return true;
        }
        
        player.sendMessage(plugin.getMessage("party-created"));
        
        // Check for party creation achievement
        plugin.getAchievementManager().checkAchievements(party);
        
        return true;
    }
    
    private boolean handleInvite(Player player, String[] args) {
        if (!player.hasPermission("euphoria.party.invite")) {
            player.sendMessage("§cYou don't have permission to invite players!");
            return true;
        }
        
        // Check cooldown
        if (plugin.getPartyManager().isOnCooldown(player.getUniqueId())) {
            int remaining = plugin.getPartyManager().getRemainingCooldown(player.getUniqueId());
            player.sendMessage(plugin.getMessage("command-cooldown").replace("{seconds}", String.valueOf(remaining)));
            return true;
        }
        
        if (args.length < 2) {
            player.sendMessage("§cUsage: /party invite <player>");
            return true;
        }
        
        Party party = plugin.getPartyManager().getPlayerParty(player.getUniqueId());
        if (party == null) {
            player.sendMessage(plugin.getMessage("not-in-party"));
            return true;
        }
        
        if (!party.isLeader(player.getUniqueId())) {
            player.sendMessage(plugin.getMessage("not-party-leader"));
            return true;
        }
        
        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            player.sendMessage("§cPlayer not found!");
            return true;
        }
        
        if (plugin.getPartyManager().isInParty(target.getUniqueId())) {
            player.sendMessage("§c" + target.getName() + " is already in a party!");
            return true;
        }
        
        // Check if already invited
        if (party.hasInvite(target.getUniqueId())) {
            player.sendMessage(plugin.getMessage("already-invited").replace("{player}", target.getName()));
            return true;
        }
        
        int maxMembers = plugin.getConfig().getInt("party.max-members", 8);
        if (party.getMemberCount() >= maxMembers) {
            player.sendMessage(plugin.getMessage("party-full"));
            return true;
        }
        
        // Check max invites
        int maxInvites = plugin.getConfig().getInt("party.max-pending-invites", 10);
        if (party.getInvites().size() >= maxInvites) {
            player.sendMessage(plugin.getMessage("too-many-invites"));
            return true;
        }
        
        plugin.getPartyManager().invitePlayer(party, target.getUniqueId());
        player.sendMessage(plugin.getMessage("invite-sent").replace("{player}", target.getName()));
        target.sendMessage(plugin.getMessage("invite-received").replace("{player}", player.getName()));
        
        // Update cooldown
        plugin.getPartyManager().updateCooldown(player.getUniqueId());
        
        return true;
    }
    
    private boolean handleAccept(Player player) {
        // Use efficient invite lookup
        Party invitingParty = plugin.getPartyManager().getPendingInvite(player.getUniqueId());
        
        if (invitingParty == null) {
            player.sendMessage("§cYou don't have any pending party invites!");
            return true;
        }
        
        if (plugin.getPartyManager().isInParty(player.getUniqueId())) {
            player.sendMessage(plugin.getMessage("already-in-party"));
            return true;
        }
        
        boolean success = plugin.getPartyManager().acceptInvite(player, invitingParty);
        if (!success) {
            player.sendMessage(plugin.getMessage("party-full"));
            return true;
        }
        
        // Notify all party members
        for (UUID memberId : invitingParty.getMembers()) {
            Player member = plugin.getServer().getPlayer(memberId).orElse(null);
            if (member != null) {
                member.sendMessage(plugin.getMessage("player-joined").replace("{player}", player.getName()));
            }
        }
        
        // Check achievements after new member joins
        plugin.getAchievementManager().checkAchievements(invitingParty);
        
        return true;
    }
    
    private boolean handleLeave(Player player) {
        if (!plugin.getPartyManager().isInParty(player.getUniqueId())) {
            player.sendMessage(plugin.getMessage("not-in-party"));
            return true;
        }
        
        Party party = plugin.getPartyManager().getPlayerParty(player.getUniqueId());
        
        // Notify all members before leaving
        for (UUID memberId : party.getMembers()) {
            if (memberId.equals(player.getUniqueId())) continue;
            
            Player member = plugin.getServer().getPlayer(memberId).orElse(null);
            if (member != null) {
                member.sendMessage(plugin.getMessage("player-left").replace("{player}", player.getName()));
            }
        }
        
        plugin.getPartyManager().leaveParty(player.getUniqueId());
        player.sendMessage(plugin.getMessage("player-left").replace("{player}", "You"));
        
        return true;
    }
    
    private boolean handleKick(Player player, String[] args) {
        if (!player.hasPermission("euphoria.party.kick")) {
            player.sendMessage("§cYou don't have permission to kick players!");
            return true;
        }
        
        if (args.length < 2) {
            player.sendMessage("§cUsage: /party kick <player>");
            return true;
        }
        
        Party party = plugin.getPartyManager().getPlayerParty(player.getUniqueId());
        if (party == null) {
            player.sendMessage(plugin.getMessage("not-in-party"));
            return true;
        }
        
        if (!party.isLeader(player.getUniqueId())) {
            player.sendMessage(plugin.getMessage("not-party-leader"));
            return true;
        }
        
        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            player.sendMessage("§cPlayer not found!");
            return true;
        }
        
        if (!party.isMember(target.getUniqueId())) {
            player.sendMessage("§c" + target.getName() + " is not in your party!");
            return true;
        }
        
        if (party.isLeader(target.getUniqueId())) {
            player.sendMessage("§cYou cannot kick yourself!");
            return true;
        }
        
        plugin.getPartyManager().kickPlayer(party, target.getUniqueId());
        
        // Notify
        for (UUID memberId : party.getMembers()) {
            Player member = plugin.getServer().getPlayer(memberId).orElse(null);
            if (member != null) {
                member.sendMessage(plugin.getMessage("player-kicked").replace("{player}", target.getName()));
            }
        }
        target.sendMessage(plugin.getMessage("player-kicked").replace("{player}", "You were"));
        
        return true;
    }
    
    private boolean handlePromote(Player player, String[] args) {
        if (!player.hasPermission("euphoria.party.promote")) {
            player.sendMessage("§cYou don't have permission to promote players!");
            return true;
        }
        
        if (args.length < 2) {
            player.sendMessage("§cUsage: /party promote <player>");
            return true;
        }
        
        Party party = plugin.getPartyManager().getPlayerParty(player.getUniqueId());
        if (party == null) {
            player.sendMessage(plugin.getMessage("not-in-party"));
            return true;
        }
        
        if (!party.isLeader(player.getUniqueId())) {
            player.sendMessage(plugin.getMessage("not-party-leader"));
            return true;
        }
        
        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            player.sendMessage("§cPlayer not found!");
            return true;
        }
        
        if (!party.isMember(target.getUniqueId())) {
            player.sendMessage("§c" + target.getName() + " is not in your party!");
            return true;
        }
        
        if (party.isLeader(target.getUniqueId())) {
            player.sendMessage("§c" + target.getName() + " is already the leader!");
            return true;
        }
        
        party.transferLeadership(target.getUniqueId());
        
        // Notify all party members
        for (UUID memberId : party.getMembers()) {
            Player member = plugin.getServer().getPlayer(memberId).orElse(null);
            if (member != null) {
                member.sendMessage(plugin.getMessage("leader-transferred")
                    .replace("{player}", target.getName()));
            }
        }
        
        return true;
    }
    
    private boolean handleList(Player player) {
        Party party = plugin.getPartyManager().getPlayerParty(player.getUniqueId());
        if (party == null) {
            player.sendMessage(plugin.getMessage("not-in-party"));
            return true;
        }
        
        player.sendMessage("§8[§6Party Members§8]");
        for (UUID memberId : party.getMembers()) {
            Player member = plugin.getServer().getPlayer(memberId).orElse(null);
            String name = member != null ? member.getName() : memberId.toString();
            String status = member != null && member.isOnline() ? "§a●" : "§c●";
            String role = party.isLeader(memberId) ? " §e[Leader]" : "";
            player.sendMessage(status + " §f" + name + role);
        }
        
        return true;
    }
    
    private boolean handleInfo(Player player) {
        Party party = plugin.getPartyManager().getPlayerParty(player.getUniqueId());
        if (party == null) {
            player.sendMessage(plugin.getMessage("not-in-party"));
            return true;
        }
        
        Player leader = plugin.getServer().getPlayer(party.getLeader()).orElse(null);
        String leaderName = leader != null ? leader.getName() : "Unknown";
        
        int onlineCount = 0;
        for (UUID memberId : party.getMembers()) {
            Player member = plugin.getServer().getPlayer(memberId).orElse(null);
            if (member != null && member.isOnline()) {
                onlineCount++;
            }
        }
        
        // Calculate party age
        long ageMillis = System.currentTimeMillis() - party.getCreatedAt();
        long ageMinutes = ageMillis / 60000;
        long ageHours = ageMinutes / 60;
        String age;
        if (ageHours > 0) {
            age = ageHours + "h " + (ageMinutes % 60) + "m";
        } else {
            age = ageMinutes + "m";
        }
        
        player.sendMessage("§8========== §6Party Info §8==========");
        player.sendMessage("§eLeader: §f" + leaderName);
        player.sendMessage("§eMembers: §f" + onlineCount + "§7/§f" + party.getMemberCount());
        player.sendMessage("§ePending Invites: §f" + party.getInvites().size());
        player.sendMessage("§eParty Home: " + (party.hasHome() ? "§a✓" : "§c✗"));
        player.sendMessage("§eAge: §f" + age);
        player.sendMessage("§8================================");
        
        return true;
    }
    
    private boolean handleSetHome(Player player) {
        if (!player.hasPermission("euphoria.party.sethome")) {
            player.sendMessage("§cYou don't have permission to set party home!");
            return true;
        }
        
        Party party = plugin.getPartyManager().getPlayerParty(player.getUniqueId());
        if (party == null) {
            player.sendMessage(plugin.getMessage("not-in-party"));
            return true;
        }
        
        if (!party.isLeader(player.getUniqueId())) {
            player.sendMessage(plugin.getMessage("not-party-leader"));
            return true;
        }
        
        plugin.getPartyManager().setPartyHome(party, player.getLocation());
        player.sendMessage(plugin.getMessage("home-set"));
        
        return true;
    }
    
    private boolean handleWarpLeader(Player player) {
        // Check if teleport is enabled
        if (!plugin.getConfig().getBoolean("party.teleport-enabled", true)) {
            player.sendMessage(plugin.getMessage("teleport-disabled"));
            return true;
        }
        
        // Check teleport cooldown
        if (plugin.getPartyManager().isOnTeleportCooldown(player.getUniqueId())) {
            int remaining = plugin.getPartyManager().getRemainingTeleportCooldown(player.getUniqueId());
            player.sendMessage(plugin.getMessage("teleport-cooldown").replace("{seconds}", String.valueOf(remaining)));
            return true;
        }
        
        Party party = plugin.getPartyManager().getPlayerParty(player.getUniqueId());
        if (party == null) {
            player.sendMessage(plugin.getMessage("not-in-party"));
            return true;
        }
        
        // Can't warp to yourself
        if (party.isLeader(player.getUniqueId())) {
            player.sendMessage("§cYou are the party leader!");
            return true;
        }
        
        Player leader = plugin.getServer().getPlayer(party.getLeader()).orElse(null);
        if (leader == null || !leader.isOnline()) {
            player.sendMessage("§cParty leader is not online!");
            return true;
        }
        
        Location leaderLoc = leader.getLocation();
        
        // Security: Check distance limit
        int maxDistance = plugin.getConfig().getInt("security.max-teleport-distance", 10000);
        if (player.distance(leaderLoc) > maxDistance) {
            player.sendMessage(plugin.getMessage("teleport-too-far"));
            return true;
        }
        
        // Safety: Check if location is safe
        boolean safeCheck = plugin.getConfig().getBoolean("security.safe-teleport", true);
        if (safeCheck && !isSafeLocation(leaderLoc)) {
            player.sendMessage(plugin.getMessage("unsafe-location"));
            return true;
        }
        
        player.teleport(leaderLoc);
        player.sendMessage("§aTeleported to party leader!");
        
        // Update teleport cooldown
        plugin.getPartyManager().updateTeleportCooldown(player.getUniqueId());
        
        return true;
    }
    
    private boolean handleHome(Player player) {
        // Check if teleport is enabled
        if (!plugin.getConfig().getBoolean("party.teleport-enabled", true)) {
            player.sendMessage(plugin.getMessage("teleport-disabled"));
            return true;
        }
        
        // Check teleport cooldown
        if (plugin.getPartyManager().isOnTeleportCooldown(player.getUniqueId())) {
            int remaining = plugin.getPartyManager().getRemainingTeleportCooldown(player.getUniqueId());
            player.sendMessage(plugin.getMessage("teleport-cooldown").replace("{seconds}", String.valueOf(remaining)));
            return true;
        }
        
        Party party = plugin.getPartyManager().getPlayerParty(player.getUniqueId());
        if (party == null) {
            player.sendMessage(plugin.getMessage("not-in-party"));
            return true;
        }
        
        if (!party.hasHome()) {
            player.sendMessage(plugin.getMessage("home-not-set"));
            return true;
        }
        
        Location home = party.getHome();
        
        // Security: Check distance limit
        int maxDistance = plugin.getConfig().getInt("security.max-teleport-distance", 10000);
        if (player.distance(home) > maxDistance) {
            player.sendMessage(plugin.getMessage("teleport-too-far"));
            return true;
        }
        
        // Safety: Check if location is safe
        boolean safeCheck = plugin.getConfig().getBoolean("security.safe-teleport", true);
        if (safeCheck && !isSafeLocation(home)) {
            player.sendMessage(plugin.getMessage("unsafe-location"));
            return true;
        }
        
        player.teleport(home);
        player.sendMessage(plugin.getMessage("teleporting"));
        
        // Update teleport cooldown
        plugin.getPartyManager().updateTeleportCooldown(player.getUniqueId());
        
        return true;
    }
    
    /**
     * Check if a location is safe for teleportation
     */
    private boolean isSafeLocation(Location loc) {
        if (loc == null || loc.getLevel() == null) {
            return false;
        }
        
        // Check if blocks above are air (not suffocating)
        for (int y = 0; y < 2; y++) {
            if (loc.getLevel().getBlock(loc.add(0, y, 0)).isSolid()) {
                return false;
            }
        }
        
        // Check if standing on solid ground or has block below
        if (!loc.getLevel().getBlock(loc.add(0, -1, 0)).isSolid()) {
            // Check if in void
            if (loc.getY() < 0) {
                return false;
            }
        }
        
        return true;
    }
    
    private boolean handleName(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /party name <name>");
            return true;
        }
        
        Party party = plugin.getPartyManager().getPlayerParty(player.getUniqueId());
        if (party == null) {
            player.sendMessage(plugin.getMessage("not-in-party"));
            return true;
        }
        
        if (!party.getRole(player.getUniqueId()).canChangeName()) {
            player.sendMessage(plugin.getMessage("not-party-leader"));
            return true;
        }
        
        String name = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        
        if (name.length() > 32) {
            player.sendMessage("§cParty name cannot exceed 32 characters!");
            return true;
        }
        
        party.setName(name);
        plugin.getPartyManager().broadcastToParty(party, plugin.getMessage("party-name-set").replace("{name}", name));
        
        // Update tab lists for all party members
        if (plugin.getConfig().getBoolean("party.tab-list-formatting", false)) {
            com.euphoria.party.listener.PartyTabListListener tabListListener = new com.euphoria.party.listener.PartyTabListListener(plugin);
            tabListListener.updatePartyTabLists(party);
        }
        
        return true;
    }
    
    private boolean handleJoin(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /party join <player>");
            return true;
        }
        
        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            player.sendMessage("§cPlayer not found!");
            return true;
        }
        
        Party targetParty = plugin.getPartyManager().getPlayerParty(target.getUniqueId());
        if (targetParty == null) {
            player.sendMessage("§cThat player is not in a party!");
            return true;
        }
        
        if (!targetParty.isPublic()) {
            player.sendMessage("§cThat party is private!");
            return true;
        }
        
        if (targetParty.isBanned(player.getUniqueId())) {
            player.sendMessage("§cYou are banned from that party!");
            return true;
        }
        
        if (plugin.getPartyManager().getPlayerParty(player.getUniqueId()) != null) {
            player.sendMessage(plugin.getMessage("already-in-party"));
            return true;
        }
        
        if (targetParty.hasJoinRequest(player.getUniqueId())) {
            player.sendMessage("§cYou already have a pending join request for this party!");
            return true;
        }
        
        targetParty.addJoinRequest(player.getUniqueId());
        player.sendMessage("§aJoin request sent to " + (targetParty.hasName() ? targetParty.getName() : target.getName() + "'s party") + "!");
        
        Player leader = plugin.getServer().getPlayer(targetParty.getLeader()).orElse(null);
        if (leader != null) {
            leader.sendMessage("§e" + player.getName() + " §7wants to join your party! Use §e/party arequest " + player.getName() + " §7to accept.");
        }
        
        return true;
    }
    
    private boolean handleRequests(Player player) {
        Party party = plugin.getPartyManager().getPlayerParty(player.getUniqueId());
        if (party == null) {
            player.sendMessage(plugin.getMessage("not-in-party"));
            return true;
        }
        
        if (!party.getRole(player.getUniqueId()).canInvite()) {
            player.sendMessage("§cOnly officers and leaders can view join requests!");
            return true;
        }
        
        party.cleanExpiredJoinRequests(plugin.getConfig().getLong("party.invite-expiration", 300000L));
        
        if (party.getJoinRequests().isEmpty()) {
            player.sendMessage("§7No pending join requests.");
            return true;
        }
        
        player.sendMessage("§8========== §6Join Requests §8==========");
        for (UUID requestId : party.getJoinRequests().keySet()) {
            Player requester = plugin.getServer().getPlayer(requestId).orElse(null);
            if (requester != null) {
                player.sendMessage("§e" + requester.getName() + " §7- /party arequest " + requester.getName());
            }
        }
        player.sendMessage("§8================================");
        return true;
    }
    
    private boolean handleAcceptRequest(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /party arequest <player>");
            return true;
        }
        
        Party party = plugin.getPartyManager().getPlayerParty(player.getUniqueId());
        if (party == null) {
            player.sendMessage(plugin.getMessage("not-in-party"));
            return true;
        }
        
        if (!party.getRole(player.getUniqueId()).canInvite()) {
            player.sendMessage("§cOnly officers and leaders can accept join requests!");
            return true;
        }
        
        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            player.sendMessage("§cPlayer not found!");
            return true;
        }
        
        if (!party.hasJoinRequest(target.getUniqueId())) {
            player.sendMessage("§cThat player hasn't requested to join!");
            return true;
        }
        
        if (party.getMemberCount() >= plugin.getConfig().getInt("party.max-members", 8)) {
            player.sendMessage(plugin.getMessage("party-full"));
            party.removeJoinRequest(target.getUniqueId());
            return true;
        }
        
        plugin.getPartyManager().addPlayerToParty(target, party);
        player.sendMessage("§aAccepted join request from " + target.getName() + "!");
        
        // Check achievements after new member joins
        plugin.getAchievementManager().checkAchievements(party);
        
        return true;
    }
    
    private boolean handleDenyRequest(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /party drequest <player>");
            return true;
        }
        
        Party party = plugin.getPartyManager().getPlayerParty(player.getUniqueId());
        if (party == null) {
            player.sendMessage(plugin.getMessage("not-in-party"));
            return true;
        }
        
        if (!party.getRole(player.getUniqueId()).canInvite()) {
            player.sendMessage("§cOnly officers and leaders can deny join requests!");
            return true;
        }
        
        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            player.sendMessage("§cPlayer not found!");
            return true;
        }
        
        if (!party.hasJoinRequest(target.getUniqueId())) {
            player.sendMessage("§cThat player hasn't requested to join!");
            return true;
        }
        
        party.removeJoinRequest(target.getUniqueId());
        player.sendMessage("§cDenied join request from " + target.getName() + ".");
        target.sendMessage("§cYour join request to " + (party.hasName() ? party.getName() : player.getName() + "'s party") + " was denied.");
        return true;
    }
    
    private boolean handlePublic(Player player) {
        Party party = plugin.getPartyManager().getPlayerParty(player.getUniqueId());
        if (party == null) {
            player.sendMessage(plugin.getMessage("not-in-party"));
            return true;
        }
        
        if (!party.isLeader(player.getUniqueId())) {
            player.sendMessage(plugin.getMessage("not-party-leader"));
            return true;
        }
        
        party.setPublic(true);
        plugin.getPartyManager().broadcastToParty(party, "§aParty is now public! Players can request to join.");
        return true;
    }
    
    private boolean handlePrivate(Player player) {
        Party party = plugin.getPartyManager().getPlayerParty(player.getUniqueId());
        if (party == null) {
            player.sendMessage(plugin.getMessage("not-in-party"));
            return true;
        }
        
        if (!party.isLeader(player.getUniqueId())) {
            player.sendMessage(plugin.getMessage("not-party-leader"));
            return true;
        }
        
        party.setPublic(false);
        plugin.getPartyManager().broadcastToParty(party, "§cParty is now private! Only invited players can join.");
        return true;
    }
    
    private boolean handleSetRank(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§cUsage: /party setrank <player> <rank>");
            player.sendMessage("§7Ranks: officer, member, recruit");
            return true;
        }
        
        Party party = plugin.getPartyManager().getPlayerParty(player.getUniqueId());
        if (party == null) {
            player.sendMessage(plugin.getMessage("not-in-party"));
            return true;
        }
        
        if (!party.getRole(player.getUniqueId()).canPromote()) {
            player.sendMessage(plugin.getMessage("not-party-leader"));
            return true;
        }
        
        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            player.sendMessage("§cPlayer not found!");
            return true;
        }
        
        if (!party.isMember(target.getUniqueId())) {
            player.sendMessage("§cThat player is not in your party!");
            return true;
        }
        
        if (party.isLeader(target.getUniqueId())) {
            player.sendMessage("§cYou cannot change the leader's rank!");
            return true;
        }
        
        com.euphoria.party.model.PartyRole role;
        try {
            role = com.euphoria.party.model.PartyRole.valueOf(args[2].toUpperCase());
            if (role == com.euphoria.party.model.PartyRole.LEADER) {
                player.sendMessage("§cUse /party promote to transfer leadership!");
                return true;
            }
        } catch (IllegalArgumentException e) {
            player.sendMessage("§cInvalid rank! Use: officer, member, or recruit");
            return true;
        }
        
        party.setRole(target.getUniqueId(), role);
        player.sendMessage("§aSet " + target.getName() + "'s rank to §e" + role.toString().toLowerCase() + "§a!");
        target.sendMessage("§eYour party rank has been set to §6" + role.toString().toLowerCase() + "§e!");
        return true;
    }
    
    private boolean handleBan(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /party ban <player>");
            return true;
        }
        
        Party party = plugin.getPartyManager().getPlayerParty(player.getUniqueId());
        if (party == null) {
            player.sendMessage(plugin.getMessage("not-in-party"));
            return true;
        }
        
        if (!party.getRole(player.getUniqueId()).canBanPlayers()) {
            player.sendMessage("§cOnly officers and leaders can ban players!");
            return true;
        }
        
        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            player.sendMessage("§cPlayer not found!");
            return true;
        }
        
        if (party.isLeader(target.getUniqueId())) {
            player.sendMessage("§cYou cannot ban the party leader!");
            return true;
        }
        
        if (party.isBanned(target.getUniqueId())) {
            player.sendMessage("§cThat player is already banned!");
            return true;
        }
        
        party.banPlayer(target.getUniqueId());
        plugin.getPartyManager().removePlayerFromParty(target);
        
        plugin.getPartyManager().broadcastToParty(party, "§c" + target.getName() + " was banned from the party!");
        target.sendMessage("§cYou have been banned from the party!");
        return true;
    }
    
    private boolean handleUnban(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /party unban <player>");
            return true;
        }
        
        Party party = plugin.getPartyManager().getPlayerParty(player.getUniqueId());
        if (party == null) {
            player.sendMessage(plugin.getMessage("not-in-party"));
            return true;
        }
        
        if (!party.getRole(player.getUniqueId()).canBanPlayers()) {
            player.sendMessage("§cOnly officers and leaders can unban players!");
            return true;
        }
        
        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            player.sendMessage("§cPlayer not found!");
            return true;
        }
        
        if (!party.isBanned(target.getUniqueId())) {
            player.sendMessage("§cThat player is not banned!");
            return true;
        }
        
        party.unbanPlayer(target.getUniqueId());
        player.sendMessage("§aUnbanned " + target.getName() + " from the party!");
        return true;
    }
    
    private boolean handleStats(Player player) {
        Party party = plugin.getPartyManager().getPlayerParty(player.getUniqueId());
        if (party == null) {
            player.sendMessage(plugin.getMessage("not-in-party"));
            return true;
        }
        
        long hours = party.getTotalPlayTime() / (1000 * 60 * 60);
        long minutes = (party.getTotalPlayTime() / (1000 * 60)) % 60;
        
        player.sendMessage("§8========== §6Party Statistics §8==========");
        if (party.hasName()) {
            player.sendMessage("§eParty: §f" + party.getName());
        }
        player.sendMessage("§eTotal Play Time: §f" + hours + "h " + minutes + "m");
        player.sendMessage("§eTotal Kills: §f" + party.getTotalKills());
        player.sendMessage("§eTotal Deaths: §f" + party.getTotalDeaths());
        if (party.getTotalDeaths() > 0) {
            double kd = (double) party.getTotalKills() / party.getTotalDeaths();
            player.sendMessage("§eK/D Ratio: §f" + String.format("%.2f", kd));
        }
        player.sendMessage("§8================================");
        return true;
    }
    
    private boolean handleDailyReward(Player player) {
        Party party = plugin.getPartyManager().getPlayerParty(player.getUniqueId());
        if (party == null) {
            player.sendMessage(plugin.getMessage("not-in-party"));
            return true;
        }
        
        if (!party.canClaimDailyReward(player.getUniqueId())) {
            player.sendMessage("§cYou've already claimed your daily reward! Come back tomorrow.");
            return true;
        }
        
        party.claimDailyReward(player.getUniqueId());
        
        int consecutiveDays = party.getConsecutiveDays();
        int baseXP = plugin.getConfig().getInt("party.daily-reward-xp", 50);
        int bonusXP = (consecutiveDays - 1) * plugin.getConfig().getInt("party.daily-reward-streak-bonus", 10);
        int totalXP = baseXP + bonusXP;
        
        player.addExperience(totalXP);
        player.sendMessage("§8[§6Party§8] §aDaily Reward Claimed!");
        player.sendMessage("§e+" + totalXP + " XP §7(Day " + consecutiveDays + " Streak)");
        
        if (consecutiveDays % 7 == 0) {
            player.sendMessage("§6§l✦ Weekly Streak Bonus! §6+50 XP");
            player.addExperience(50);
        }
        
        // Check achievements
        plugin.getAchievementManager().checkAchievements(party);
        
        return true;
    }
    
    private boolean handleScoreboard(Player player) {
        plugin.getScoreboardManager().toggleScoreboard(player.getUniqueId());
        return true;
    }
    
    private boolean handleLeaderboard(Player player, String[] args) {
        String type = args.length > 1 ? args[1].toLowerCase() : "kills";
        
        StringBuilder message = new StringBuilder();
        message.append("§8========== §6Party Leaderboard §8==========\n");
        
        java.util.List<Party> topParties;
        switch (type) {
            case "kills":
            case "kill":
                topParties = plugin.getLeaderboardManager().getTopPartiesByKills(10);
                message.append("§eTop Parties by Kills:\n");
                break;
            case "playtime":
            case "time":
                topParties = plugin.getLeaderboardManager().getTopPartiesByPlaytime(10);
                message.append("§eTop Parties by Playtime:\n");
                break;
            case "members":
            case "size":
                topParties = plugin.getLeaderboardManager().getTopPartiesByMembers(10);
                message.append("§eTop Parties by Members:\n");
                break;
            case "kd":
            case "ratio":
                topParties = plugin.getLeaderboardManager().getTopPartiesByKD(10);
                message.append("§eTop Parties by K/D:\n");
                break;
            case "achievements":
            case "achieve":
                topParties = plugin.getLeaderboardManager().getTopPartiesByAchievements(10);
                message.append("§eTop Parties by Achievements:\n");
                break;
            default:
                player.sendMessage("§cUsage: /party leaderboard <kills|playtime|members|kd|achievements>");
                return true;
        }
        
        message.append("\n");
        int rank = 1;
        for (Party party : topParties) {
            String displayName = party.hasName() ? party.getColor() + party.getIcon() + " " + party.getName() : "Party #" + party.getId().toString().substring(0, 8);
            
            String value;
            switch (type) {
                case "kills":
                case "kill":
                    value = party.getTotalKills() + " kills";
                    break;
                case "playtime":
                case "time":
                    long hours = party.getTotalPlayTime() / (1000 * 60 * 60);
                    value = hours + " hours";
                    break;
                case "members":
                case "size":
                    value = party.getMemberCount() + " members";
                    break;
                case "kd":
                case "ratio":
                    double kd = party.getTotalDeaths() > 0 ? (double) party.getTotalKills() / party.getTotalDeaths() : party.getTotalKills();
                    value = String.format("%.2f K/D", kd);
                    break;
                case "achievements":
                case "achieve":
                    value = party.getAchievementCount() + " achievements";
                    break;
                default:
                    value = "";
            }
            
            message.append("§7#").append(rank).append(" §f").append(displayName).append(" §7- §e").append(value).append("\n");
            rank++;
        }
        
        message.append("§8================================");
        player.sendMessage(message.toString());
        return true;
    }
    
    private boolean handleAchievements(Player player) {
        Party party = plugin.getPartyManager().getPlayerParty(player.getUniqueId());
        if (party == null) {
            player.sendMessage(plugin.getMessage("not-in-party"));
            return true;
        }
        
        Collection<com.euphoria.party.model.PartyAchievement> allAchievements = plugin.getAchievementManager().getAllAchievements();
        
        if (allAchievements == null || allAchievements.isEmpty()) {
            player.sendMessage("§cNo achievements are currently registered.");
            return true;
        }
        
        // Build message as a string array to send fewer packets
        StringBuilder message = new StringBuilder();
        message.append("§8========== §6Party Achievements §8==========\n");
        
        int unlocked = 0;
        for (com.euphoria.party.model.PartyAchievement achievement : allAchievements) {
            if (achievement == null) continue;
            
            boolean hasIt = party.hasAchievement(achievement.getId());
            if (hasIt) {
                message.append("§a✓ §f").append(achievement.getName()).append("\n");
                unlocked++;
            } else {
                message.append("§c✗ §8").append(achievement.getName()).append("\n");
            }
            if (achievement.getDescription() != null && !achievement.getDescription().isEmpty()) {
                message.append("  ").append(achievement.getDescription()).append("\n");
            }
        }
        
        message.append("\n");
        message.append("§eUnlocked: §f").append(unlocked).append("§7/§f").append(allAchievements.size()).append("\n");
        message.append("§8================================");
        
        // Send as single message
        player.sendMessage(message.toString());
        return true;
    }
    
    private boolean handleColor(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /party color <color>");
            player.sendMessage("§7Available: §6gold §eyanow §agreen §baqua §cred §5purple §fwhite §7gray");
            return true;
        }
        
        Party party = plugin.getPartyManager().getPlayerParty(player.getUniqueId());
        if (party == null) {
            player.sendMessage(plugin.getMessage("not-in-party"));
            return true;
        }
        
        if (!party.isLeader(player.getUniqueId())) {
            player.sendMessage(plugin.getMessage("not-party-leader"));
            return true;
        }
        
        String colorName = args[1].toLowerCase();
        String colorCode;
        switch (colorName) {
            case "gold": colorCode = "§6"; break;
            case "yellow": colorCode = "§e"; break;
            case "green": colorCode = "§a"; break;
            case "aqua": colorCode = "§b"; break;
            case "red": colorCode = "§c"; break;
            case "purple": colorCode = "§5"; break;
            case "white": colorCode = "§f"; break;
            case "gray": colorCode = "§7"; break;
            case "blue": colorCode = "§9"; break;
            case "dark_green": colorCode = "§2"; break;
            default:
                player.sendMessage("§cInvalid color! Available: gold, yellow, green, aqua, red, purple, white, gray, blue");
                return true;
        }
        
        party.setColor(colorCode);
        plugin.getPartyManager().broadcastToParty(party, "§eParty color changed to " + colorCode + colorName + "§e!");
        
        // Update tab lists for all party members
        if (plugin.getConfig().getBoolean("party.tab-list-formatting", false)) {
            com.euphoria.party.listener.PartyTabListListener tabListListener = new com.euphoria.party.listener.PartyTabListListener(plugin);
            tabListListener.updatePartyTabLists(party);
        }
        
        return true;
    }
    
    private boolean handleIcon(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /party icon <icon>");
            player.sendMessage("§7Examples: ★ ⚔ ❤ ⚡ ☀ ☾ ♦ ✦");
            return true;
        }
        
        Party party = plugin.getPartyManager().getPlayerParty(player.getUniqueId());
        if (party == null) {
            player.sendMessage(plugin.getMessage("not-in-party"));
            return true;
        }
        
        if (!party.isLeader(player.getUniqueId())) {
            player.sendMessage(plugin.getMessage("not-party-leader"));
            return true;
        }
        
        String icon = args[1];
        if (icon.length() > 3) {
            player.sendMessage("§cIcon must be 1-3 characters!");
            return true;
        }
        
        party.setIcon(icon);
        plugin.getPartyManager().broadcastToParty(party, "§eParty icon changed to " + party.getColor() + icon + "§e!");
        
        // Update tab lists for all party members
        if (plugin.getConfig().getBoolean("party.tab-list-formatting", false)) {
            com.euphoria.party.listener.PartyTabListListener tabListListener = new com.euphoria.party.listener.PartyTabListListener(plugin);
            tabListListener.updatePartyTabLists(party);
        }
        
        return true;
    }
    
    private boolean handleAlly(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§cUsage: /party ally <add|remove|list> [player]");
            return true;
        }
        
        Party party = plugin.getPartyManager().getPlayerParty(player.getUniqueId());
        if (party == null) {
            player.sendMessage(plugin.getMessage("not-in-party"));
            return true;
        }
        
        if (!party.isLeader(player.getUniqueId())) {
            player.sendMessage(plugin.getMessage("not-party-leader"));
            return true;
        }
        
        String action = args[1].toLowerCase();
        
        if (action.equals("list")) {
            player.sendMessage("§8========== §6Party Allies §8==========");
            if (party.getAllies().isEmpty()) {
                player.sendMessage("§7No allies yet.");
            } else {
                for (UUID allyId : party.getAllies()) {
                    Party ally = plugin.getPartyManager().getParty(allyId);
                    if (ally != null) {
                        String name = ally.hasName() ? ally.getColor() + ally.getIcon() + " " + ally.getName() : "Party #" + allyId.toString().substring(0, 8);
                        player.sendMessage("§7- §f" + name);
                    }
                }
            }
            player.sendMessage("§8================================");
            return true;
        }
        
        if (args.length < 3) {
            player.sendMessage("§cUsage: /party ally " + action + " <player>");
            return true;
        }
        
        Player target = plugin.getServer().getPlayer(args[2]);
        if (target == null) {
            player.sendMessage("§cPlayer not found!");
            return true;
        }
        
        Party targetParty = plugin.getPartyManager().getPlayerParty(target.getUniqueId());
        if (targetParty == null) {
            player.sendMessage("§cThat player is not in a party!");
            return true;
        }
        
        if (targetParty.getId().equals(party.getId())) {
            player.sendMessage("§cYou can't ally with your own party!");
            return true;
        }
        
        if (action.equals("add")) {
            party.addAlly(targetParty.getId());
            targetParty.addAlly(party.getId());
            
            plugin.getPartyManager().broadcastToParty(party, "§aFormed alliance with " + (targetParty.hasName() ? targetParty.getColor() + targetParty.getName() : target.getName() + "'s party") + "§a!");
            plugin.getPartyManager().broadcastToParty(targetParty, "§aFormed alliance with " + (party.hasName() ? party.getColor() + party.getName() : player.getName() + "'s party") + "§a!");
            
        } else if (action.equals("remove")) {
            party.removeAlly(targetParty.getId());
            targetParty.removeAlly(party.getId());
            
            plugin.getPartyManager().broadcastToParty(party, "§cRemoved alliance with " + (targetParty.hasName() ? targetParty.getName() : target.getName() + "'s party") + "§c.");
            plugin.getPartyManager().broadcastToParty(targetParty, "§cRemoved alliance with " + (party.hasName() ? party.getName() : player.getName() + "'s party") + "§c.");
        }
        
        return true;
    }
    
    private void sendHelp(Player player) {
        player.sendMessage("§8========== §6Party Commands §8==========");
        player.sendMessage("§e/party create §7- Create a new party");
        player.sendMessage("§e/party name <name> §7- Set party name");
        player.sendMessage("§e/party invite <player> §7- Invite a player");
        player.sendMessage("§e/party join <player> §7- Request to join a party");
        player.sendMessage("§e/party accept §7- Accept a party invite");
        player.sendMessage("§e/party requests §7- View join requests");
        player.sendMessage("§e/party leave §7- Leave your current party");
        player.sendMessage("§e/party kick <player> §7- Kick a player");
        player.sendMessage("§e/party promote <player> §7- Transfer leadership");
        player.sendMessage("§e/party setrank <player> <rank> §7- Set member rank");
        player.sendMessage("§e/party ban/unban <player> §7- Ban/unban players");
        player.sendMessage("§e/party public/private §7- Toggle party privacy");
        player.sendMessage("§e/party list §7- List party members");
        player.sendMessage("§e/party info §7- View party info");
        player.sendMessage("§e/party stats §7- View party statistics");
        player.sendMessage("§e/party sethome §7- Set party home");
        player.sendMessage("§e/party home §7- Teleport to party home");
        player.sendMessage("§e/party warp §7- Teleport to party leader");
        player.sendMessage("§8================================");
    }
}
