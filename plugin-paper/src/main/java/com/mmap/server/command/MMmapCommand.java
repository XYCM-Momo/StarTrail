package com.mmap.server.command;

import com.mmap.server.MMmapPlugin;
import com.mmap.server.image.MapImage;
import com.mmap.server.network.PacketSender;
import com.mmap.server.region.MapRegion;
import com.mmap.server.region.RegionType;
import com.mmap.server.util.LanguageManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class MMmapCommand implements CommandExecutor, TabCompleter {
   private final MMmapPlugin plugin;
   private final LanguageManager lang;

   public MMmapCommand(MMmapPlugin plugin) {
      this.plugin = plugin;
      this.lang = plugin.getLanguageManager();
   }

   private String msg(String key, Object... args) {
      return this.lang.get(key, args);
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (args.length == 0) {
         this.sendHelp(sender);
         return true;
      } else {
         String subCommand = args[0].toLowerCase();
         switch (subCommand) {
            case "add":
               return this.handleAdd(sender, args);
            case "remove":
               return this.handleRemove(sender, args);
            case "list":
               return this.handleList(sender, args);
            case "info":
               return this.handleInfo(sender, args);
            case "reload":
               return this.handleReload(sender, args);
            case "map":
               return this.handleMap(sender, args);
            case "crypt":
               sender.sendMessage(
                  ChatColor.AQUA
                     + "[MMmap] "
                     + ChatColor.WHITE
                     + "\u56fe\u7247\u52a0\u5bc6\u662f\u5ba2\u6237\u7aef\u672c\u5730\u6307\u4ee4\uff0c\u9700\u8981\u5728\u5b89\u88c5\u4e86 MMmap \u6a21\u7ec4\u7684\u5ba2\u6237\u7aef\u6267\u884c\uff1a"
               );
               sender.sendMessage(ChatColor.YELLOW + "  /mmap crypt" + ChatColor.GRAY + " - \u52a0\u5bc6\u56fe\u7247\uff08\u4fdd\u7559\u660e\u6587\uff09");
               sender.sendMessage(ChatColor.YELLOW + "  /mmap crypt delete" + ChatColor.GRAY + " - \u52a0\u5bc6\u5e76\u5220\u9664\u660e\u6587");
               return true;
            case "help":
               this.sendHelp(sender);
               return true;
            default:
               sender.sendMessage(this.msg("unknown-command", subCommand));
               this.sendHelp(sender);
               return true;
         }
      }
   }

   private boolean handleAdd(CommandSender sender, String[] args) {
      if (!(sender instanceof Player)) {
         sender.sendMessage(this.msg("player-only"));
         return true;
      } else if (args.length < 7) {
         sender.sendMessage(this.msg("region-add-usage"));
         sender.sendMessage(this.msg("region-type-list"));
         return true;
      } else {
         Player player = (Player)sender;
         String name = args[1];

         try {
            int x1 = Integer.parseInt(args[2]);
            int z1 = Integer.parseInt(args[3]);
            int x2 = Integer.parseInt(args[4]);
            int z2 = Integer.parseInt(args[5]);
            RegionType type = RegionType.NORMAL;
            if (args.length >= 7) {
               try {
                  type = RegionType.valueOf(args[6].toUpperCase());
               } catch (IllegalArgumentException var13) {
                  sender.sendMessage(this.msg("region-invalid-type", args[6]));
                  return true;
               }
            }

            String id = this.plugin.getRegionManager().generateRegionId(name);
            String world = player.getWorld().getName();
            MapRegion region = new MapRegion(id, name, world, x1, z1, x2, z2);
            region.setType(type);
            if (this.plugin.getRegionManager().addRegion(region)) {
               sender.sendMessage(this.msg("region-created", name));
               sender.sendMessage(this.msg("region-id", id));
               sender.sendMessage(this.msg("region-coords", x1, z1, x2, z2));
               sender.sendMessage(this.msg("region-type", type.getDisplayName()));
               sender.sendMessage(this.msg("region-migrate-warn"));
               sender.sendMessage(this.msg("region-migrate-warn2"));
               this.plugin.getPacketSender().broadcastRegionUpdate(region, PacketSender.UpdateOperation.ADD);
            } else {
               sender.sendMessage(this.msg("region-create-fail"));
            }
         } catch (NumberFormatException var14) {
            sender.sendMessage(this.msg("coord-must-number"));
         }

         return true;
      }
   }

   private boolean handleRemove(CommandSender sender, String[] args) {
      if (args.length < 2) {
         sender.sendMessage(this.msg("region-remove-usage"));
         return true;
      } else {
         String id = args[1];
         MapRegion region = this.plugin.getRegionManager().getRegion(id);
         if (region == null) {
            sender.sendMessage(this.msg("region-not-found", id));
            return true;
         } else {
            if (this.plugin.getRegionManager().removeRegion(id)) {
               sender.sendMessage(this.msg("region-deleted", region.getName()));
               sender.sendMessage(this.msg("region-delete-migrate-warn"));
               this.plugin.getPacketSender().broadcastRegionUpdate(region, PacketSender.UpdateOperation.REMOVE);
            } else {
               sender.sendMessage(this.msg("region-delete-fail"));
            }

            return true;
         }
      }
   }

   private boolean handleList(CommandSender sender, String[] args) {
      List<MapRegion> regions;
      if (args.length >= 2) {
         String world = args[1];
         regions = this.plugin.getRegionManager().getRegionsInWorld(world);
         sender.sendMessage(this.msg("region-list-world", world));
      } else {
         regions = new ArrayList<>(this.plugin.getRegionManager().getAllRegions());
         sender.sendMessage(this.msg("region-list-all"));
      }

      if (regions.isEmpty()) {
         sender.sendMessage(this.msg("region-list-empty"));
         return true;
      } else {
         for (MapRegion region : regions) {
            sender.sendMessage(
               ChatColor.AQUA
                  + region.getId()
                  + ChatColor.RESET
                  + " - "
                  + ChatColor.WHITE
                  + region.getName()
                  + ChatColor.GRAY
                  + " ["
                  + region.getType().getDisplayName()
                  + "]"
            );
         }

         sender.sendMessage(this.msg("region-list-count", regions.size()));
         return true;
      }
   }

   private boolean handleInfo(CommandSender sender, String[] args) {
      if (args.length < 2) {
         sender.sendMessage(this.msg("region-info-usage"));
         return true;
      } else {
         String id = args[1];
         MapRegion region = this.plugin.getRegionManager().getRegion(id);
         if (region == null) {
            sender.sendMessage(this.msg("region-not-found", id));
            return true;
         } else {
            sender.sendMessage(this.msg("region-info-title"));
            sender.sendMessage(this.msg("region-info-id", region.getId()));
            sender.sendMessage(this.msg("region-info-name", region.getName()));
            sender.sendMessage(this.msg("region-info-world", region.getWorld()));
            sender.sendMessage(this.msg("region-info-bounds"));
            sender.sendMessage(this.msg("region-info-x", region.getMinX(), region.getMaxX()));
            sender.sendMessage(this.msg("region-info-z", region.getMinZ(), region.getMaxZ()));
            sender.sendMessage(this.msg("region-info-y", region.getMinY(), region.getMaxY()));
            sender.sendMessage(this.msg("region-info-type", region.getType().getDisplayName()));
            sender.sendMessage(this.msg("region-info-area", region.getArea()));
            if (!region.getDescription().isEmpty()) {
               sender.sendMessage(this.msg("region-info-desc", region.getDescription()));
            }

            return true;
         }
      }
   }

   private boolean handleReload(CommandSender sender, String[] args) {
      this.plugin.reloadConfig();
      this.plugin.getLanguageManager().load();
      this.plugin.getRegionManager().loadRegions();
      this.plugin.getMapImageManager().loadImages();
      this.plugin.getPolygonRegionManager().load();
      if (this.plugin.getWarpPointManager() != null) {
         this.plugin.getWarpPointManager().load();
      }

      sender.sendMessage(this.msg("reload-success"));
      sender.sendMessage(
         this.msg(
            "reload-summary",
            this.plugin.getRegionManager().getRegionCount(),
            this.plugin.getMapImageManager().getImageCount(),
            this.plugin.getPolygonRegionManager().getAll().size(),
            this.plugin.getWarpPointManager() != null ? this.plugin.getWarpPointManager().size() : 0,
            0
         )
      );

      for (Player player : this.plugin.getServer().getOnlinePlayers()) {
         this.plugin.getPacketSender().sendHandshake(player);
         this.plugin.getPacketSender().sendRegionSync(player);
         this.plugin.getPacketSender().sendMapImageSync(player);
         this.plugin.getPacketSender().sendPolygonRegions(player);
         if (this.plugin.getWarpPointManager() != null) {
            this.plugin.getPacketSender().sendWarps(player);
         }
      }

      return true;
   }

   private boolean handleMap(CommandSender sender, String[] args) {
      if (args.length < 2) {
         this.sendMapHelp(sender);
         return true;
      } else {
         String action = args[1].toLowerCase();
         switch (action) {
            case "add":
               return this.handleMapAdd(sender, args);
            case "remove":
               return this.handleMapRemove(sender, args);
            case "list":
               return this.handleMapList(sender);
            case "help":
               this.sendMapHelp(sender);
               return true;
            default:
               sender.sendMessage(this.msg("unknown-command", action));
               this.sendMapHelp(sender);
               return true;
         }
      }
   }

   private boolean handleMapAdd(CommandSender sender, String[] args) {
      if (args.length < 8) {
         sender.sendMessage(this.msg("map-add-usage"));
         sender.sendMessage(this.msg("map-file-hint"));
         sender.sendMessage(this.msg("map-coord-hint"));
         sender.sendMessage(this.msg("map-console-world"));
         return true;
      } else {
         String id = args[2];
         String fileName = args[3];

         try {
            int x1 = Integer.parseInt(args[4]);
            int z1 = Integer.parseInt(args[5]);
            int x2 = Integer.parseInt(args[6]);
            int z2 = Integer.parseInt(args[7]);
            String world;
            if (sender instanceof Player) {
               world = args.length >= 9 ? args[8] : ((Player)sender).getWorld().getName();
            } else {
               if (args.length < 9) {
                  sender.sendMessage(this.msg("map-console-world-error"));
                  sender.sendMessage(this.msg("map-console-world-usage"));
                  return true;
               }

               world = args[8];
            }

            MapImage image = new MapImage(id, fileName, world, x1, z1, x2, z2);
            if (this.plugin.getMapImageManager().addImage(image)) {
               sender.sendMessage(this.msg("map-created", id));
               sender.sendMessage(this.msg("map-file", fileName));
               sender.sendMessage(this.msg("map-world", world));
               sender.sendMessage(this.msg("map-range", x1, z1, x2, z2));
               sender.sendMessage(this.msg("map-area", image.getArea()));
               sender.sendMessage(this.msg("map-file-place", fileName));
               this.plugin.getPacketSender().broadcastMapImageSync();
            } else {
               sender.sendMessage(this.msg("map-create-fail", id));
            }
         } catch (NumberFormatException var11) {
            sender.sendMessage(this.msg("coord-must-number"));
         }

         return true;
      }
   }

   private boolean handleMapRemove(CommandSender sender, String[] args) {
      if (args.length < 3) {
         sender.sendMessage(this.msg("map-remove-usage"));
         return true;
      } else {
         String id = args[2];
         MapImage image = this.plugin.getMapImageManager().getImage(id);
         if (image == null) {
            sender.sendMessage(this.msg("map-not-found", id));
            return true;
         } else {
            if (this.plugin.getMapImageManager().removeImage(id)) {
               sender.sendMessage(this.msg("map-deleted", id));
               this.plugin.getPacketSender().broadcastMapImageSync();
            } else {
               sender.sendMessage(this.msg("map-delete-fail"));
            }

            return true;
         }
      }
   }

   private boolean handleMapList(CommandSender sender) {
      Collection<MapImage> images = this.plugin.getMapImageManager().getAllImages();
      sender.sendMessage(this.msg("map-list-title"));
      if (images.isEmpty()) {
         sender.sendMessage(this.msg("map-list-empty"));
         return true;
      } else {
         for (MapImage image : images) {
            sender.sendMessage(
               ChatColor.AQUA
                  + image.getId()
                  + ChatColor.RESET
                  + " - "
                  + ChatColor.WHITE
                  + image.getFileName()
                  + ChatColor.GRAY
                  + " ["
                  + image.getWorld()
                  + "] ("
                  + image.getMinX()
                  + ","
                  + image.getMinZ()
                  + ")->("
                  + image.getMaxX()
                  + ","
                  + image.getMaxZ()
                  + ")"
            );
         }

         sender.sendMessage(this.msg("map-list-count", images.size()));
         return true;
      }
   }

   private void sendMapHelp(CommandSender sender) {
      sender.sendMessage(this.msg("map-list-title"));
      sender.sendMessage(
         ChatColor.AQUA + "/mmap map add <ID> <\u6587\u4ef6\u540d> <x1> <z1> <x2> <z2>" + ChatColor.GRAY + " - \u6dfb\u52a0\u56fe\u7247\u7ed1\u5b9a"
      );
      sender.sendMessage(ChatColor.AQUA + "/mmap map remove <ID>" + ChatColor.GRAY + " - \u5220\u9664\u56fe\u7247\u7ed1\u5b9a");
      sender.sendMessage(ChatColor.AQUA + "/mmap map list" + ChatColor.GRAY + " - \u5217\u51fa\u6240\u6709\u56fe\u7247\u7ed1\u5b9a");
      sender.sendMessage(this.msg("map-coord-hint"));
      sender.sendMessage(this.msg("map-file-hint"));
   }

   private void sendHelp(CommandSender sender) {
      sender.sendMessage(this.msg("help-title"));
      sender.sendMessage(this.msg("help-add"));
      sender.sendMessage(this.msg("help-remove"));
      sender.sendMessage(this.msg("help-list"));
      sender.sendMessage(this.msg("help-info"));
      sender.sendMessage(this.msg("help-map-add"));
      sender.sendMessage(this.msg("help-map-remove"));
      sender.sendMessage(this.msg("help-map-list"));
      sender.sendMessage(this.msg("help-reload"));
      sender.sendMessage(
         ChatColor.AQUA + "/mmap crypt" + ChatColor.GRAY + " - \u52a0\u5bc6\u5730\u56fe\u56fe\u7247\uff08\u5ba2\u6237\u7aef\u672c\u5730\u6307\u4ee4\uff09"
      );
      sender.sendMessage(this.msg("help-help"));
   }

   public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
      List<String> completions = new ArrayList<>();
      if (args.length == 1) {
         completions.addAll(Arrays.asList("add", "remove", "list", "info", "reload", "map", "crypt", "help"));
      } else if (args.length == 2) {
         String subCommand = args[0].toLowerCase();
         if (subCommand.equals("remove") || subCommand.equals("info")) {
            completions.addAll(this.plugin.getRegionManager().getAllRegions().stream().map(MapRegion::getId).collect(Collectors.toList()));
         } else if (subCommand.equals("list")) {
            this.plugin.getServer().getWorlds().forEach(world -> completions.add(world.getName()));
         } else if (subCommand.equals("map")) {
            completions.addAll(Arrays.asList("add", "remove", "list", "help"));
         }
      } else if (args.length == 3 && args[0].equalsIgnoreCase("map")) {
         if (args[1].toLowerCase().equals("remove")) {
            completions.addAll(this.plugin.getMapImageManager().getAllImages().stream().map(MapImage::getId).collect(Collectors.toList()));
         }
      } else if (args.length == 7 && args[0].equalsIgnoreCase("add")) {
         for (RegionType type : RegionType.values()) {
            completions.add(type.name());
         }
      }

      return completions.stream().filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase())).collect(Collectors.toList());
   }
}
