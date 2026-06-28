package com.mustafa8907.nexusmute;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class MuteCommand implements CommandExecutor {

    private final NexusMute plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public MuteCommand(NexusMute plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("nexusmute.admin")) {
            sendMessage(sender, plugin.getConfig().getString("messages.no-permission", "<red>No permission!</red>"));
            return true;
        }

        if (args.length == 0) {
            sendMessage(sender, "<red>Usage: /mute <player> <reason/layout> OR /tempmute <player> <time> <reason></red>");
            return true;
        }

        String targetName = args[0];

        // Unmute İşlemi
        if (label.equalsIgnoreCase("unmute") || label.equalsIgnoreCase("nexusunmute")) {
            plugin.getMuteManager().removeMute(targetName);
            String msg = plugin.getConfig().getString("messages.unmute-broadcast", "<green><player> unmuted.</green>")
                    .replace("<player>", targetName)
                    .replace("<operator>", sender.getName());
            broadcastMessage(msg, "nexusmute.admin");
            return true;
        }

        // Reload İşlemi
        if (label.equalsIgnoreCase("nexusmute-reload")) {
            plugin.reloadConfig();
            sendMessage(sender, plugin.getConfig().getString("messages.reload-success", "<green>Configuration reloaded!</green>"));
            return true;
        }

        if (args.length < 2) {
            sendMessage(sender, "<red>You must specify a reason or layout!</red>");
            return true;
        }

        // TempMute ve Normal Mute Ayrımı
        long durationMillis = -1;
        String reasonStr;

        if (label.equalsIgnoreCase("tempmute") || label.equalsIgnoreCase("nexustempmute")) {
            if (args.length < 3) {
                sendMessage(sender, "<red>Usage: /tempmute <player> <time> <reason></red>");
                return true;
            }
            durationMillis = plugin.getMuteManager().parseTime(args[1]);
            if (durationMillis == -1) {
                sendMessage(sender, plugin.getConfig().getString("messages.invalid-time-format", "<red>Invalid time format!</red>"));
                return true;
            }
            reasonStr = buildString(args, 2);
        } else {
            // Normal Mute - Layout kontrolü
            String possibleLayout = args[1].toLowerCase();
            // Layout dosyasından çekmeyi dener, yoksa normal sebep sayar
            // Not: onEnable içinde custom file oluşturmadık, config.yml içine layouts eklenebilir veya harici class yazılabilir.
            // Bu örnekte varsayılan config kullanılıyor.
            if (plugin.getConfig().contains("layouts." + possibleLayout)) {
                String layoutTime = plugin.getConfig().getString("layouts." + possibleLayout + ".duration");
                durationMillis = plugin.getMuteManager().parseTime(layoutTime);
                reasonStr = plugin.getConfig().getString("layouts." + possibleLayout + ".reason", "No reason provided.");
            } else {
                reasonStr = buildString(args, 1);
            }
        }

        // Target UUID Bulma (Offline olsa bile isimden mute atabilmek için)
        @SuppressWarnings("deprecation")
        Player targetPlayer = Bukkit.getPlayer(targetName);
        String uuid = (targetPlayer != null) ? targetPlayer.getUniqueId().toString() : "offline-uuid";
        String finalTargetName = (targetPlayer != null) ? targetPlayer.getName() : targetName;

        // Mute'yi RAM'e ve Veritabanına Ekle
        plugin.getMuteManager().addMute(uuid, finalTargetName, reasonStr, sender.getName(), durationMillis);

        // Duyuru Mesajı
        String broadcastPath = (durationMillis == -1) ? "messages.mute-broadcast" : "messages.tempmute-broadcast";
        String msg = plugin.getConfig().getString(broadcastPath, "<red><player> muted.</red>")
                .replace("<player>", finalTargetName)
                .replace("<operator>", sender.getName())
                .replace("<reason>", reasonStr)
                .replace("<duration>", (durationMillis == -1) ? "Permanent" : args[1]);

        broadcastMessage(msg, "nexusmute.admin");
        return true;
    }

    private String buildString(String[] args, int start) {
        StringBuilder builder = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            builder.append(args[i]).append(" ");
        }
        return builder.toString().trim();
    }

    // MiniMessage Component'lerini eski sürümlere uyumlu hale getirip gönderir
    private void sendMessage(CommandSender sender, String miniMessageString) {
        String legacyText = LegacyComponentSerializer.legacySection().serialize(mm.deserialize(miniMessageString));
        sender.sendMessage(legacyText);
    }

    private void broadcastMessage(String miniMessageString, String permission) {
        String legacyText = LegacyComponentSerializer.legacySection().serialize(mm.deserialize(miniMessageString));
        Bukkit.broadcast(legacyText, permission);
    }
                                                      }
