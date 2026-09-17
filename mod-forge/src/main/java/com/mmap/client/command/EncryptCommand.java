package com.mmap.client.command;

import com.mmap.client.MMmapMod;
import com.mmap.client.crypto.ImageCrypto;
import java.io.File;
import java.nio.file.Files;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.loading.FMLPaths;

/**
 * Local (client side) image encryption command, reachable as
 * {@code /mmap crypt}, {@code /mmap crypt delete} and {@code /mmap crypt help}.
 *
 * <p>The Fabric build intercepted {@code ClientPacketListener#sendCommand} with a Mixin to keep
 * these commands local. Forge has first class support for client commands
 * ({@code RegisterClientCommandsEvent} + {@code ClientCommandHandler}), so the Mixin is not
 * needed and the command tree is registered directly - see {@link MMmapCommands}.
 */
public final class EncryptCommand {
    private EncryptCommand() {
    }

    public static int encrypt(CommandSourceStack source, boolean deleteOriginal) {
        File root = getMmapDir();
        if (root == null) {
            feedback(source, Component.literal("§c无法定位 mmap 配置目录"));
            return 1;
        }

        int totalOk = 0;
        int totalFail = 0;
        boolean anyProcessed = false;

        for (String sub : new String[]{"map", "legend", "mm"}) {
            File dir = new File(root, sub);
            if (!dir.isDirectory()) {
                continue;
            }

            File[] images = dir.listFiles((d, name) -> {
                String lower = name.toLowerCase();
                return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg");
            });
            if (images == null || images.length == 0) {
                continue;
            }

            anyProcessed = true;
            int ok = 0;
            int fail = 0;
            StringBuilder failNames = new StringBuilder();

            for (File img : images) {
                try {
                    byte[] raw = Files.readAllBytes(img.toPath());
                    byte[] enc = ImageCrypto.encrypt(raw, img.getName());
                    String base = img.getName();
                    int dot = base.lastIndexOf(46);
                    if (dot > 0) {
                        base = base.substring(0, dot);
                    }

                    File out = new File(dir, base + ".mmap");
                    Files.write(out.toPath(), enc);
                    if (deleteOriginal) {
                        Files.deleteIfExists(img.toPath());
                    }

                    ok++;
                } catch (Exception e) {
                    fail++;
                    if (failNames.length() > 0) {
                        failNames.append(", ");
                    }
                    failNames.append(img.getName());
                    MMmapMod.LOGGER.error("加密失败: {}/{}", new Object[]{sub, img.getName(), e});
                }
            }

            totalOk += ok;
            totalFail += fail;
            feedback(source, Component.literal(
                    "§a  " + sub + "/: 成功 §e" + ok + "§a, 失败 §e" + fail + (fail > 0 ? " §c(" + failNames + ")" : "")));
        }

        if (!anyProcessed) {
            feedback(source, Component.literal("§e没有找到任何 PNG/JPG 图片。"));
            feedback(source, Component.literal("§7扫描目录: " + root.getAbsolutePath() + "/{map,legend,mm}"));
            return 1;
        }

        feedback(source, Component.literal(
                "§a[MMmap] 加密完成: 共成功 §e" + totalOk + "§a 个, 失败 §e" + totalFail + "§a 个"
                        + (deleteOriginal ? " §7（已删除明文）" : " §7（明文已保留）")));
        feedback(source, Component.literal("§7提示: 分发给玩家时只拷贝 .mmap，不要拷贝明文 PNG。"));
        return totalFail > 0 ? 1 : 0;
    }

    public static int help(CommandSourceStack source) {
        feedback(source, Component.literal("§6[MMmap] 图片加密指令帮助"));
        feedback(source, Component.literal("§7/mmap crypt          §f加密 map/legend/mm 目录所有图片为 .mmap（保留明文）"));
        feedback(source, Component.literal("§7/mmap crypt delete   §f加密并删除明文（推荐）"));
        feedback(source, Component.literal("§7/mmap crypt help     §f显示本帮助"));
        feedback(source, Component.literal("§7扫描目录: .minecraft/mmap/{map,legend,mm}/"));
        return 0;
    }

    private static void feedback(CommandSourceStack source, Component message) {
        if (source != null) {
            source.sendSuccess(() -> message, false);
        }
    }

    private static File getMmapDir() {
        try {
            return new File(FMLPaths.GAMEDIR.get().toFile(), "mmap");
        } catch (Exception var1) {
            return null;
        }
    }
}
