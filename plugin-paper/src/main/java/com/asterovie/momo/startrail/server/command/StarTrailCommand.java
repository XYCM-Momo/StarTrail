package com.asterovie.momo.startrail.server.command;

import com.asterovie.momo.startrail.server.StarTrailPlugin;
import com.asterovie.momo.startrail.server.image.MapImage;
import com.asterovie.momo.startrail.server.region.PolygonRegion;
import com.asterovie.momo.startrail.server.util.LanguageManager;
import com.asterovie.momo.startrail.server.warp.WarpPoint;
import com.asterovie.momo.startrail.server.waypoint.Waypoint;
import com.asterovie.momo.startrail.server.waypoint.WaypointManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class StarTrailCommand implements CommandExecutor, TabCompleter {
   private final StarTrailPlugin plugin;
   private final LanguageManager lang;

   public StarTrailCommand(StarTrailPlugin plugin) {
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
            case "list":
               return this.handleList(sender, args);
            case "info":
               return this.handleInfo(sender, args);
            case "reload":
               return this.handleReload(sender, args);
            case "map":
               return this.handleMap(sender, args);
            case "tp":
               return this.handleTp(sender, args);
            case "warp":
               return this.handleWarp(sender, args);
            case "waypoint":
               return this.handleWaypoint(sender, args);
            case "crypt":
               sender.sendMessage(
                  ChatColor.AQUA
                     + "[StarTrail] "
                     + ChatColor.WHITE
                     + "\u56fe\u7247\u52a0\u5bc6\u662f\u5ba2\u6237\u7aef\u672c\u5730\u6307\u4ee4\uff0c\u9700\u8981\u5728\u5b89\u88c5\u4e86 StarTrail \u6a21\u7ec4\u7684\u5ba2\u6237\u7aef\u6267\u884c\uff1a"
               );
               sender.sendMessage(ChatColor.YELLOW + "  /startrail crypt" + ChatColor.GRAY + " - \u52a0\u5bc6\u56fe\u7247\uff08\u4fdd\u7559\u660e\u6587\uff09");
               sender.sendMessage(ChatColor.YELLOW + "  /startrail crypt delete" + ChatColor.GRAY + " - \u52a0\u5bc6\u5e76\u5220\u9664\u660e\u6587");
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

   private boolean handleList(CommandSender sender, String[] args) {
      List<PolygonRegion> regions;
      if (args.length >= 2) {
         String world = args[1];
         regions = this.plugin.getPolygonRegionManager().getInWorld(world);
         sender.sendMessage(this.msg("region-list-world", world));
      } else {
         regions = new ArrayList<>(this.plugin.getPolygonRegionManager().getAll());
         sender.sendMessage(this.msg("region-list-all"));
      }

      if (regions.isEmpty()) {
         sender.sendMessage(this.msg("region-list-empty"));
         return true;
      } else {
         for (PolygonRegion region : regions) {
            sender.sendMessage(
               ChatColor.AQUA
                  + region.getId()
                  + ChatColor.RESET
                  + " - "
                  + ChatColor.WHITE
                  + region.getName()
                  + ChatColor.GRAY
                  + " ["
                  + region.getWorld()
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
         PolygonRegion region = this.plugin.getPolygonRegionManager().get(id);
         if (region == null) {
            sender.sendMessage(this.msg("region-not-found", id));
            return true;
         } else {
            sender.sendMessage(this.msg("region-info-title"));
            sender.sendMessage(this.msg("region-info-id", region.getId()));
            sender.sendMessage(this.msg("region-info-name", region.getName()));
            sender.sendMessage(this.msg("region-info-world", region.getWorld()));
            sender.sendMessage(this.msg("region-info-vertices", region.getVertices().size()));
            sender.sendMessage(this.msg("region-info-area", region.getArea()));
            sender.sendMessage(this.msg("region-info-vertices-title"));
            List<int[]> verts = region.getVertices();
            int maxShown = 15;

            for (int i = 0; i < verts.size() && i < maxShown; i++) {
               sender.sendMessage(this.msg("region-info-vertex", verts.get(i)[0], verts.get(i)[1]));
            }

            if (verts.size() > maxShown) {
               sender.sendMessage(this.msg("region-info-vertices-more", verts.size() - maxShown));
            }

            sender.sendMessage(this.msg("region-info-center", (int)region.getCenterX(), (int)region.getCenterZ()));
            sender.sendMessage(this.msg("region-info-commands", region.getEnterCommands().size()));
            return true;
         }
      }
   }

   private boolean handleReload(CommandSender sender, String[] args) {
      this.plugin.reloadConfig();
      this.plugin.getLanguageManager().load();
      this.plugin.getMapImageManager().loadImages();
      this.plugin.getPolygonRegionManager().load();
      if (this.plugin.getWarpPointManager() != null) {
         this.plugin.getWarpPointManager().load();
      }

      this.plugin.getWaypointManager().loadAll();

      sender.sendMessage(this.msg("reload-success"));
      sender.sendMessage(
         this.msg(
            "reload-summary",
            this.plugin.getPolygonRegionManager().getAll().size(),
            this.plugin.getMapImageManager().getImageCount(),
            this.plugin.getWarpPointManager() != null ? this.plugin.getWarpPointManager().size() : 0
         )
      );

      for (Player player : this.plugin.getServer().getOnlinePlayers()) {
         this.plugin.getPacketSender().sendHandshake(player);
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

   private boolean handleTp(CommandSender sender, String[] args) {
      if (!(sender instanceof Player)) {
         sender.sendMessage(this.msg("player-only"));
         return true;
      } else {
         Player player = (Player)sender;
         if (!player.hasPermission("startrail.command.tp")) {
            sender.sendMessage(this.msg("tp-no-permission"));
            return true;
         } else if (args.length < 3) {
            sender.sendMessage(this.msg("tp-usage"));
            return true;
         } else {
            try {
               int x = Integer.parseInt(args[1]);
               int z = Integer.parseInt(args[2]);
               World world = player.getWorld();
               int y;
               if (args.length >= 4) {
                  y = Integer.parseInt(args[3]);
               } else {
                  // 未指定 Y：取该列最高方块 +1，避免传进地里
                  y = world.getHighestBlockYAt(x, z) + 1;
                  if (y < world.getMinHeight() + 1) {
                     y = world.getMinHeight() + 1;
                  }
               }

               player.teleport(new Location(world, (double)x + 0.5, (double)y, (double)z + 0.5, player.getYaw(), player.getPitch()));
               sender.sendMessage(this.msg("tp-success", x, y, z));
               return true;
            } catch (NumberFormatException var11) {
               sender.sendMessage(this.msg("coord-must-number"));
               return true;
            }
         }
      }
   }

   private boolean handleWarp(CommandSender sender, String[] args) {
      if (args.length < 3 || !args[1].equalsIgnoreCase("tp")) {
         sender.sendMessage(this.msg("warp-usage"));
         return true;
      } else if (!(sender instanceof Player)) {
         sender.sendMessage(this.msg("player-only"));
         return true;
      } else {
         Player player = (Player)sender;
         if (!player.hasPermission("startrail.command.tp")) {
            sender.sendMessage(this.msg("tp-no-permission"));
            return true;
         } else if (this.plugin.getWarpPointManager() == null) {
            sender.sendMessage(this.msg("warp-not-found", args[2]));
            return true;
         } else {
            WarpPoint warp = this.plugin.getWarpPointManager().get(args[2]);
            if (warp == null) {
               sender.sendMessage(this.msg("warp-not-found", args[2]));
               return true;
            } else {
               World world = Bukkit.getWorld(warp.getWorld());
               if (world == null) {
                  world = player.getWorld();
               }

               player.teleport(
                  new Location(world, (double)warp.getX() + 0.5, (double)warp.getY() + 0.5, (double)warp.getZ() + 0.5, player.getYaw(), player.getPitch())
               );
               sender.sendMessage(this.msg("warp-tp-success", warp.getName(), warp.getWorld()));
               return true;
            }
         }
      }
   }

   // ==================== 导航点位命令 ====================

   private boolean handleWaypoint(CommandSender sender, String[] args) {
      if (args.length < 2) {
         sender.sendMessage(this.msg("waypoint-usage"));
         this.sendWaypointHelp(sender);
         return true;
      } else {
         String action = args[1].toLowerCase();
         switch (action) {
            case "add":
               return this.handleWaypointAdd(sender, args);
            case "remove":
               return this.handleWaypointRemove(sender, args);
            case "list":
               return this.handleWaypointList(sender, args);
            case "link":
               return this.handleWaypointLink(sender, args);
            case "unlink":
               return this.handleWaypointUnlink(sender, args);
            case "help":
               this.sendWaypointHelp(sender);
               return true;
            default:
               sender.sendMessage(this.msg("unknown-command", action));
               this.sendWaypointHelp(sender);
               return true;
         }
      }
   }

   private boolean handleWaypointAdd(CommandSender sender, String[] args) {
      if (args.length < 6) {
         sender.sendMessage(this.msg("waypoint-add-usage"));
         return true;
      } else {
         String mapId = args[2];
         String wpId = args[3];

         try {
            double x = Double.parseDouble(args[4]);
            double y;
            double z;
            if (args.length >= 7) {
               y = Double.parseDouble(args[5]);
               z = Double.parseDouble(args[6]);
            } else if (sender instanceof Player) {
               // 未给 y 时取玩家当前高度，args[5] 视为 z
               z = Double.parseDouble(args[5]);
               y = ((Player)sender).getLocation().getY();
            } else {
               sender.sendMessage(this.msg("waypoint-y-needed"));
               return true;
            }

            String world = null;
            if (args.length >= 8) {
               world = args[7];
            } else {
               MapImage img = this.plugin.getMapImageManager().getImage(mapId);
               if (img != null) {
                  world = img.getWorld();
               } else if (sender instanceof Player) {
                  world = ((Player)sender).getWorld().getName();
               }
            }

            Waypoint wp = new Waypoint(wpId, x, y, z);
            if (this.plugin.getWaypointManager().add(mapId, wp, world)) {
               sender.sendMessage(this.msg("waypoint-added", wpId, mapId, fmtCoord(x), fmtCoord(y), fmtCoord(z)));
               sender.sendMessage(this.msg("waypoint-world", world == null ? "?" : world));
               sender.sendMessage(this.msg("waypoint-saved", "plugins/StarTrail/waypoints/" + WaypointManager.sanitize(mapId) + ".json"));
            } else {
               sender.sendMessage(this.msg("waypoint-exists", mapId, wpId));
            }
         } catch (NumberFormatException var12) {
            sender.sendMessage(this.msg("coord-must-number"));
         }

         return true;
      }
   }

   private boolean handleWaypointRemove(CommandSender sender, String[] args) {
      if (args.length < 4) {
         sender.sendMessage(this.msg("waypoint-remove-usage"));
         return true;
      } else {
         String mapId = args[2];
         String wpId = args[3];
         if (this.plugin.getWaypointManager().remove(mapId, wpId)) {
            sender.sendMessage(this.msg("waypoint-removed", wpId, mapId));
         } else {
            sender.sendMessage(this.msg("waypoint-not-found", wpId));
         }

         return true;
      }
   }

   private boolean handleWaypointList(CommandSender sender, String[] args) {
      WaypointManager wm = this.plugin.getWaypointManager();
      if (args.length < 3) {
         if (wm.getMapIds().isEmpty()) {
            sender.sendMessage(this.msg("waypoint-list-maps-empty"));
            return true;
         }

         sender.sendMessage(this.msg("waypoint-list-maps-title"));
         for (String mapId : wm.getMapIds()) {
            sender.sendMessage(ChatColor.AQUA + mapId + ChatColor.GRAY + " - " + wm.getWaypoints(mapId).size() + " \u4e2a\u70b9\u4f4d");
         }

         return true;
      } else {
         String mapId = args[2];
         if (!wm.hasMap(mapId)) {
            sender.sendMessage(this.msg("waypoint-map-not-found", mapId));
            return true;
         }

         List<Waypoint> wps = wm.getWaypoints(mapId);
         sender.sendMessage(this.msg("waypoint-list-title", mapId));
         if (wps.isEmpty()) {
            sender.sendMessage(this.msg("waypoint-list-empty"));
            return true;
         }

         for (Waypoint wp : wps) {
            String links = wp.getLinks().isEmpty() ? "\u65e0" : String.join(", ", wp.getLinks());
            sender.sendMessage(this.msg("waypoint-list-entry", wp.getId(), fmtCoord(wp.getX()), fmtCoord(wp.getY()), fmtCoord(wp.getZ()), links));
         }

         sender.sendMessage(this.msg("waypoint-list-count", wps.size()));
         return true;
      }
   }

   /** 坐标显示：整数值去掉小数点（58.0 -> 58），小数保留（110.5 -> 110.5）。 */
   private static String fmtCoord(double v) {
      if (v == Math.floor(v) && !Double.isInfinite(v)) {
         return String.valueOf((long)v);
      }

      return String.valueOf(v);
   }

   private boolean handleWaypointLink(CommandSender sender, String[] args) {
      if (args.length < 5) {
         sender.sendMessage(this.msg("waypoint-link-usage"));
         return true;
      } else {
         String mapId = args[2];
         String a = args[3];
         String b = args[4];
         if (this.plugin.getWaypointManager().link(mapId, a, b)) {
            sender.sendMessage(this.msg("waypoint-linked", a, b));
         } else {
            sender.sendMessage(this.msg("waypoint-link-fail", a, b));
         }

         return true;
      }
   }

   private boolean handleWaypointUnlink(CommandSender sender, String[] args) {
      if (args.length < 5) {
         sender.sendMessage(this.msg("waypoint-unlink-usage"));
         return true;
      } else {
         String mapId = args[2];
         String a = args[3];
         String b = args[4];
         if (this.plugin.getWaypointManager().unlink(mapId, a, b)) {
            sender.sendMessage(this.msg("waypoint-unlinked", a, b));
         } else {
            sender.sendMessage(this.msg("waypoint-link-fail", a, b));
         }

         return true;
      }
   }

   private void sendWaypointHelp(CommandSender sender) {
      sender.sendMessage(this.msg("waypoint-help-title"));
      sender.sendMessage(
         ChatColor.AQUA + "/startrail waypoint add <\u5730\u56feID> <\u70b9\u4f4dID> <x> <y> <z> [\u4e16\u754c]"
            + ChatColor.GRAY
            + " - \u6dfb\u52a0\u70b9\u4f4d"
      );
      sender.sendMessage(ChatColor.AQUA + "/startrail waypoint remove <\u5730\u56feID> <\u70b9\u4f4dID>" + ChatColor.GRAY + " - \u5220\u9664\u70b9\u4f4d");
      sender.sendMessage(ChatColor.AQUA + "/startrail waypoint list [\u5730\u56feID]" + ChatColor.GRAY + " - \u5217\u51fa\u70b9\u4f4d");
      sender.sendMessage(
         ChatColor.AQUA + "/startrail waypoint link <\u5730\u56feID> <\u70b9\u4f4dA> <\u70b9\u4f4dB>" + ChatColor.GRAY + " - \u4e92\u76f8\u8fde\u901a"
      );
      sender.sendMessage(
         ChatColor.AQUA + "/startrail waypoint unlink <\u5730\u56feID> <\u70b9\u4f4dA> <\u70b9\u4f4dB>" + ChatColor.GRAY + " - \u53d6\u6d88\u8fde\u901a"
      );
   }

   private void sendMapHelp(CommandSender sender) {
      sender.sendMessage(this.msg("map-list-title"));
      sender.sendMessage(
         ChatColor.AQUA + "/startrail map add <ID> <\u6587\u4ef6\u540d> <x1> <z1> <x2> <z2>" + ChatColor.GRAY + " - \u6dfb\u52a0\u56fe\u7247\u7ed1\u5b9a"
      );
      sender.sendMessage(ChatColor.AQUA + "/startrail map remove <ID>" + ChatColor.GRAY + " - \u5220\u9664\u56fe\u7247\u7ed1\u5b9a");
      sender.sendMessage(ChatColor.AQUA + "/startrail map list" + ChatColor.GRAY + " - \u5217\u51fa\u6240\u6709\u56fe\u7247\u7ed1\u5b9a");
      sender.sendMessage(this.msg("map-coord-hint"));
      sender.sendMessage(this.msg("map-file-hint"));
   }

   private void sendHelp(CommandSender sender) {
      sender.sendMessage(this.msg("help-title"));
      sender.sendMessage(this.msg("help-list"));
      sender.sendMessage(this.msg("help-info"));
      sender.sendMessage(this.msg("help-map-add"));
      sender.sendMessage(this.msg("help-map-remove"));
      sender.sendMessage(this.msg("help-map-list"));
      sender.sendMessage(this.msg("help-tp"));
      sender.sendMessage(this.msg("help-warp-tp"));
      sender.sendMessage(this.msg("help-waypoint"));
      sender.sendMessage(this.msg("help-reload"));
      sender.sendMessage(
         ChatColor.AQUA + "/startrail crypt" + ChatColor.GRAY + " - \u52a0\u5bc6\u5730\u56fe\u56fe\u7247\uff08\u5ba2\u6237\u7aef\u672c\u5730\u6307\u4ee4\uff09"
      );
      sender.sendMessage(this.msg("help-help"));
   }

   public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
      List<String> completions = new ArrayList<>();
      if (args.length == 1) {
         completions.addAll(Arrays.asList("list", "info", "reload", "map", "tp", "warp", "waypoint", "crypt", "help"));
      } else if (args.length == 2) {
         String subCommand = args[0].toLowerCase();
         if (subCommand.equals("list")) {
            this.plugin.getServer().getWorlds().forEach(world -> completions.add(world.getName()));
         } else if (subCommand.equals("info")) {
            completions.addAll(this.plugin.getPolygonRegionManager().getAll().stream().map(PolygonRegion::getId).collect(Collectors.toList()));
         } else if (subCommand.equals("map")) {
            completions.addAll(Arrays.asList("add", "remove", "list", "help"));
         } else if (subCommand.equals("warp")) {
            completions.addAll(Arrays.asList("tp"));
         } else if (subCommand.equals("waypoint")) {
            completions.addAll(Arrays.asList("add", "remove", "list", "link", "unlink", "help"));
         }
      } else if (args.length == 3 && args[0].equalsIgnoreCase("map")) {
         if (args[1].toLowerCase().equals("remove")) {
            completions.addAll(this.plugin.getMapImageManager().getAllImages().stream().map(MapImage::getId).collect(Collectors.toList()));
         }
      } else if (args.length == 3 && args[0].equalsIgnoreCase("waypoint")) {
         if (Arrays.asList("add", "remove", "link", "unlink", "list").contains(args[1].toLowerCase())) {
            completions.addAll(this.plugin.getWaypointManager().getMapIds());
            completions.addAll(this.plugin.getMapImageManager().getAllImages().stream().map(MapImage::getId).collect(Collectors.toList()));
         }
      } else if (args.length == 4 && args[0].equalsIgnoreCase("waypoint")) {
         String action = args[1].toLowerCase();
         if (action.equals("remove") || action.equals("link") || action.equals("unlink")) {
            completions.addAll(
               this.plugin.getWaypointManager().getWaypoints(args[2]).stream().map(Waypoint::getId).collect(Collectors.toList())
            );
         }
      } else if (args.length == 3 && args[0].equalsIgnoreCase("warp") && args[1].equalsIgnoreCase("tp")) {
         if (this.plugin.getWarpPointManager() != null) {
            completions.addAll(this.plugin.getWarpPointManager().getAll().stream().map(WarpPoint::getId).collect(Collectors.toList()));
         }
      }

      return completions.stream().filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase())).collect(Collectors.toList());
   }
}
