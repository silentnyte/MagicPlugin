package com.elmakers.mine.bukkit.magic.command;

import com.elmakers.mine.bukkit.api.entity.EntityData;
import com.elmakers.mine.bukkit.api.magic.Mage;
import com.elmakers.mine.bukkit.api.magic.MageController;
import com.elmakers.mine.bukkit.api.magic.MagicAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class MagicMobCommandExecutor extends MagicTabExecutor {
	public MagicMobCommandExecutor(MagicAPI api) {
		super(api);
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!api.hasPermission(sender, "Magic.commands.mmob"))
        {
            sendNoPermission(sender);
            return true;
        }

        if (args.length == 0)
		{
            sender.sendMessage(ChatColor.RED + "Usage: mmob [spawn|list] <type>");
			return true;
		}
        
        if (args[0].equalsIgnoreCase("list"))
        {
            onListMobs(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("clear"))
        {
            String mobType = args.length > 1 ? args[1] : null;
            String worldName = args.length > 2 ? args[2] : null;
            onClearMobs(sender, mobType, worldName);
            return true;
        }

        if (!args[0].equalsIgnoreCase("spawn") || args.length < 2)
        {
            return false;
        }

        if (!(sender instanceof Player) && !(sender instanceof BlockCommandSender) && args.length < 6) {
            sender.sendMessage(ChatColor.RED + "Usage: mmob spawn <type> <x> <y> <z> <world>");
            return true;
        }
        
        Location targetLocation = null;
        World targetWorld = null;
        Player player = (sender instanceof Player) ? (Player)sender : null;
        if (args.length >= 6) {
            targetWorld = Bukkit.getWorld(args[5]);
            if (targetWorld == null) {
                sender.sendMessage(ChatColor.RED + "Invalid world: " + ChatColor.GRAY + args[5]);
                return true;
            }
        } else if (player != null) {
            targetWorld = player.getWorld();
        }

        if (args.length >= 5) {
            try {
                targetLocation = new Location(targetWorld, Double.parseDouble(args[2]), Double.parseDouble(args[3]), Double.parseDouble(args[4]));
            } catch (Exception ex) {
                targetLocation = null;
            }
        } else if (player != null) {
            Location location = player.getEyeLocation();
            BlockIterator iterator = new BlockIterator(location.getWorld(), location.toVector(), location.getDirection(), 0, 64);
            Block block = location.getBlock();
            while (block.getType() == Material.AIR && iterator.hasNext()) {
                block = iterator.next();
            }
            block = block.getRelative(BlockFace.UP);
            targetLocation = block.getLocation();
        }
        
        if (targetLocation == null || targetLocation.getWorld() == null) {
            sender.sendMessage(ChatColor.RED + "Usage: mmob spawn <type> <x> <y> <z> <world>");
            return true;
        }
        
        String mobKey = args[1];

        MageController controller = api.getController();
        Entity spawned = controller.spawnMob(mobKey, targetLocation);
        if (spawned == null) {
            sender.sendMessage(ChatColor.RED + "Unknown mob type " + mobKey);
            return true;
        }
        
        String name = spawned.getName();
        if (name == null) {
            name = mobKey;
        }
        sender.sendMessage(ChatColor.AQUA + "Spawned mob: " + ChatColor.LIGHT_PURPLE + name);
        return true;
	}
    
    protected void onListMobs(CommandSender sender) {
        Set<String> keys = api.getController().getMobKeys();
        for (String key : keys) {
            EntityData mobType = api.getController().getMob(key);
            sender.sendMessage(ChatColor.AQUA + key + ChatColor.WHITE + " : " + ChatColor.DARK_AQUA + mobType.describe());
        }
    }

    protected void onClearMobs(CommandSender sender, String mobType, String worldName) {
        Collection<Mage> mages = new ArrayList<Mage>(api.getController().getMages());
        int removed = 0;
        for (Mage mage : mages) {
            EntityData entityData = mage.getEntityData();
            if (entityData == null) continue;
            if (worldName != null && !mage.getLocation().getWorld().getName().equals(worldName)) continue;
            if (mobType != null && !entityData.getKey().equals(mobType)) continue;

            Entity entity = mage.getEntity();
            mage.undoScheduled();
            api.getController().removeMage(mage);
            if (entity != null) {
                entity.remove();
            }
            removed++;
        }
        sender.sendMessage("Removed " + removed + " magic mobs");
    }

	@Override
	public Collection<String> onTabComplete(CommandSender sender, String commandName, String[] args) {
		List<String> options = new ArrayList<String>();
        if (!sender.hasPermission("Magic.commands.mmob")) return options;

		if (args.length == 1) {
            options.add("spawn");
            options.add("list");
            options.add("clear");
		} else if (args.length == 2 && (args[0].equalsIgnoreCase("spawn") || args[0].equalsIgnoreCase("clear"))) {
            options.addAll(api.getController().getMobKeys());
            for (EntityType entityType : EntityType.values()) {
                if (entityType.isAlive() && entityType.isSpawnable()) {
                    options.add(entityType.name().toLowerCase());
                }
            }
		} else if (args.length == 3 && args[0].equalsIgnoreCase("clear")) {
            List<World> worlds = api.getPlugin().getServer().getWorlds();
            for (World world : worlds) {
                options.add(world.getName());
            }
        }
		return options;
	}
}
