package com.mustafa8907.nexusmute;

import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class NexusMute extends JavaPlugin {

    // Gizli reklam ve imza (Decompile edilince görünür)
    @SuppressWarnings({"unused", "FieldCanBeLocal"})
    private final String INTERNAL_AUTHOR = "mustafa8907 | discord.gg/mustafa0907 | mustafa8907.com.tr";

    private Connection connection;
    private MuteManager muteManager;

    @Override
    public void onEnable() {
        // Dosyaları oluştur
        saveDefaultConfig();
        saveResource("messages.yml", false);
        saveResource("layouts.yml", false);

        // Veritabanı ve Manager kurulumu
        setupDatabase();
        this.muteManager = new MuteManager(this);
        this.muteManager.loadActiveMutes();

        // Diğer sınıflarla bağlantı (Komutlar ve Dinleyiciler aktif edildi!)
        MuteCommand commandHandler = new MuteCommand(this);
        getCommand("mute").setExecutor(commandHandler);
        getCommand("tempmute").setExecutor(commandHandler);
        getCommand("unmute").setExecutor(commandHandler);
        getCommand("nexusmute-reload").setExecutor(commandHandler);

        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
    }

    @Override
    public void onDisable() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            getLogger().severe("Database closing error: " + e.getMessage());
        }
    }

    private void setupDatabase() {
        try {
            File dataFolder = getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdir();
            }
            
            File dbFile = new File(dataFolder, "database.db");
            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            connection = DriverManager.getConnection(url);

            try (Statement statement = connection.createStatement()) {
                // uuid ve player_name (cracked için) destekli, indexli tablo oluşturma
                String sql = "CREATE TABLE IF NOT EXISTS nexus_mutes (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "uuid VARCHAR(36) NOT NULL," +
                        "player_name VARCHAR(16) NOT NULL," +
                        "reason TEXT NOT NULL," +
                        "operator VARCHAR(16) NOT NULL," +
                        "execution_time LONG NOT NULL," +
                        "end_time LONG NOT NULL" +
                        ");";
                statement.execute(sql);
                
                // Oyuncu ismine göre hızlı sorgu için INDEX
                statement.execute("CREATE INDEX IF NOT EXISTS idx_player_name ON nexus_mutes(player_name);");
            }
        } catch (SQLException e) {
            getLogger().severe("Could not setup SQLite database: " + e.getMessage());
        }
    }

    public Connection getConnection() {
        return connection;
    }
    
    public MuteManager getMuteManager() {
        return muteManager;
    }
  }
