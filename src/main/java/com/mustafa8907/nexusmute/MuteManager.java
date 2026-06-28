package com.mustafa8907.nexusmute;

import org.bukkit.Bukkit;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MuteManager {

    private final NexusMute plugin;
    // Lowercase oyuncu ismi -> Mute verisi (Cracked uyumu için isim bazlı)
    private final Map<String, MuteInfo> activeMutes = new ConcurrentHashMap<>();

    public MuteManager(NexusMute plugin) {
        this.plugin = plugin;
    }

    // Java 17 Record ile ultra hafif veri kalıbı (Data Model)
    public record MuteInfo(String uuid, String playerName, String reason, String operator, long executionTime, long endTime) {
        public boolean isExpired() {
            return endTime != -1 && System.currentTimeMillis() > endTime;
        }
    }

    // Sunucu açılırken veritabanındaki muteleri RAM'e çeker
    public void loadActiveMutes() {
        activeMutes.clear();
        String sql = "SELECT * FROM nexus_mutes";
        
        try (PreparedStatement stmt = plugin.getConnection().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                MuteInfo info = new MuteInfo(
                        rs.getString("uuid"),
                        rs.getString("player_name"),
                        rs.getString("reason"),
                        rs.getString("operator"),
                        rs.getLong("execution_time"),
                        rs.getLong("end_time")
                );

                if (info.isExpired()) {
                    removeMuteFromDB(info.playerName()); // Süresi dolmuşları temizle
                } else {
                    activeMutes.put(info.playerName().toLowerCase(), info);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error loading mutes: " + e.getMessage());
        }
    }

    public MuteInfo getMute(String playerName) {
        String lowerName = playerName.toLowerCase();
        MuteInfo info = activeMutes.get(lowerName);
        
        if (info != null && info.isExpired()) {
            removeMute(lowerName); // Anlık kontrol, süresi bittiyse kaldır
            return null;
        }
        return info;
    }

    public void addMute(String uuid, String playerName, String reason, String operator, long durationMillis) {
        long executionTime = System.currentTimeMillis();
        long endTime = (durationMillis == -1) ? -1 : (executionTime + durationMillis);

        MuteInfo newMute = new MuteInfo(uuid, playerName, reason, operator, executionTime, endTime);
        activeMutes.put(playerName.toLowerCase(), newMute);

        // Veritabanına asenkron kaydet (Sunucuyu yormaz)
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "INSERT INTO nexus_mutes (uuid, player_name, reason, operator, execution_time, end_time) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = plugin.getConnection().prepareStatement(sql)) {
                stmt.setString(1, uuid);
                stmt.setString(2, playerName.toLowerCase());
                stmt.setString(3, reason);
                stmt.setString(4, operator);
                stmt.setLong(5, executionTime);
                stmt.setLong(6, endTime);
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Could not save mute: " + e.getMessage());
            }
        });
    }

    public void removeMute(String playerName) {
        activeMutes.remove(playerName.toLowerCase());
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> removeMuteFromDB(playerName));
    }

    private void removeMuteFromDB(String playerName) {
        String sql = "DELETE FROM nexus_mutes WHERE player_name = ?";
        try (PreparedStatement stmt = plugin.getConnection().prepareStatement(sql)) {
            stmt.setString(1, playerName.toLowerCase());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not remove mute from DB: " + e.getMessage());
        }
    }

    // 1h, 30m, 1d gibi süreleri milisaniyeye çeviren metod
    public long parseTime(String timeString) {
        if (timeString == null || timeString.isEmpty()) return -1;
        Pattern pattern = Pattern.compile("(\\d+)([smhd])");
        Matcher matcher = pattern.matcher(timeString.toLowerCase());
        
        long totalMillis = 0;
        while (matcher.find()) {
            long amount = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2);
            switch (unit) {
                case "s" -> totalMillis += amount * 1000L;
                case "m" -> totalMillis += amount * 60000L;
                case "h" -> totalMillis += amount * 3600000L;
                case "d" -> totalMillis += amount * 86400000L;
            }
        }
        return totalMillis > 0 ? totalMillis : -1;
    }
                  }
