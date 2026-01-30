package com.euphoria.party.command;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.data.CommandParameter;
import com.euphoria.party.EuphoriaPartyPlugin;

public class CompassCommand extends Command {
    
    private final EuphoriaPartyPlugin plugin;
    
    public CompassCommand(EuphoriaPartyPlugin plugin) {
        super("compass", "Toggle compass display", "/compass");
        this.plugin = plugin;
        
        // Add command parameters for auto-completion
        this.commandParameters.clear();
        this.commandParameters.put("default", new CommandParameter[]{});
    }
    
    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("euphoria.hud.compass")) {
            player.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }
        
        plugin.getHUDManager().toggleCompass(player);
        
        return true;
    }
}
