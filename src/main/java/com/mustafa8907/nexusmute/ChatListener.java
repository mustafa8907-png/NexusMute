package com.mustafa8907.nexusmute;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class ChatListener implements Listener {

    private final NexusMute plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public ChatListener(NexusMute plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        MuteManager.MuteInfo muteInfo = plugin.getMuteManager().getMute(player.getName());

        if (muteInfo != null) {
            // Mute bitmiş mi diye son bir kontrol
            if (muteInfo.isExpired()) {
                return; // Süresi bittiyse mesaj göndermesine izin ver (RAM'den zaten otomatik silindi)
            }

            event.setCancelled(true); // Mesajı sunucudan tamamen iptal et

            // Ses Çalma (config.yml içinden)
            String soundName = plugin.getConfig().getString("Mute-Sound", "ENTITY_VILLAGER_NO");
            try {
                player.playSound(player.getLocation(), Sound.valueOf(soundName), 1.0f, 1.0f);
            } catch (IllegalArgumentException ignored) {
                // Eğer ses ismi Minecraft sürümünde yoksa, sessizce hatayı yoksay (konsolu spamlamaz)
            }

            // Kalan Süreyi Formatla
            String timeLeft = (muteInfo.endTime() == -1) ? "Permanent" : formatTimeLeft(muteInfo.endTime() - System.currentTimeMillis());

            // Bildirim Gönderme Tercihi (Chat, Action_Bar veya Her İkisi)
            String notifyType = plugin.getConfig().getString("Mute-Notification", "chat").toLowerCase();
            
            if (notifyType.equals("chat") || notifyType.equals("both")) {
                String chatMsg = plugin.getConfig().getString("messages.chat-blocked-chat", "<red>You are muted!</red>")
                        .replace("<time_left>", timeLeft)
                        .replace("<reason>", muteInfo.reason());
                player.sendMessage(parseMiniMessage(chatMsg));
            }
            
            if (notifyType.equals("action_bar") || notifyType.equals("both")) {
                String actionMsg = plugin.getConfig().getString("messages.chat-blocked-actionbar", "<red>MUTED!</red>")
                        .replace("<time_left>", timeLeft);
                // Action Bar gönderimi 1.16.5+ Spigot uyumlu API
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(parseMiniMessage(actionMsg)));
            }
        }
    }

    private String formatTimeLeft(long millis) {
        long seconds = millis / 1000;
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = ((seconds % 86400) % 3600) / 60;
        
        if (days > 0) return days + "d " + hours + "h";
        if (hours > 0) return hours + "h " + minutes + "m";
        if (minutes > 0) return minutes + "m";
        return (seconds % 60) + "s";
    }

    // MiniMessage'ı klasik Spigot renklerine güvenli çevirici
    private String parseMiniMessage(String miniMessageString) {
        return LegacyComponentSerializer.legacySection().serialize(mm.deserialize(miniMessageString));
    }
              }
