package com.alta189.cyborg.perms;

import com.alta189.cyborg.Cyborg;
import com.alta189.cyborg.api.command.annotation.EmptyConstructorInjector;
import com.alta189.cyborg.api.plugin.CommonPlugin;
import com.alta189.cyborg.api.util.yaml.YAMLFormat;
import com.alta189.cyborg.api.util.yaml.YAMLProcessor;
import com.alta189.simplesave.Configuration;
import com.alta189.simplesave.mysql.MySQLConfiguration;
import com.alta189.simplesave.mysql.MySQLConstants;
import com.alta189.simplesave.sqlite.SQLiteConfiguration;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;

public class CyborgPerms extends CommonPlugin {
	@Override
	public void onEnable() {
		getLogger().log(Level.INFO, "Enabling...");

		YAMLProcessor config = setupConfig(new File(getDataFolder(), "config.yml"));

		try {
			config.load();
		} catch (IOException e) {
			e.printStackTrace();
		}

		String databaseDriver = config.getString("database.driver", "mysql");
		Configuration dbConfig;

		if (databaseDriver.equals("mysql")) {
			MySQLConfiguration mdbConfig = new MySQLConfiguration();
			mdbConfig.setHost(config.getString("database.mysql.host", "127.0.0.1"));
			mdbConfig.setPort(config.getInt("database.mysql.port", MySQLConstants.DefaultPort));
			mdbConfig.setDatabase(config.getString("database.mysql.database"));
			mdbConfig.setUser(config.getString("database.mysql.user", MySQLConstants.DefaultUser));
			mdbConfig.setPassword(config.getString("database.mysql.password", MySQLConstants.DefaultPass));
			dbConfig = mdbConfig;
		} else if (databaseDriver.equals("sqlite")) {
			SQLiteConfiguration sdbConfig = new SQLiteConfiguration();
			sdbConfig.setPath(config.getString("database.sqlite.path", "cyborgperms.sqlite"));
			dbConfig = sdbConfig;
		} else {
			getLogger().log(Level.SEVERE, "Unknown database driver: " + databaseDriver);
			Cyborg.getInstance().getPluginManager().disablePlugin(this);
			return;
		}

		PermissionManager.setDbConfig(dbConfig);
		if (!PermissionManager.init()) {
			getLogger().log(Level.SEVERE, "Error on connection to database");
			Cyborg.getInstance().getPluginManager().disablePlugin(this);
		}

		getCyborg().getCommandManager().registerCommands(this, PermsCommands.class, new EmptyConstructorInjector());

		getLogger().log(Level.INFO, "Successfully enabled!");
	}

	@Override
	public void onDisable() {
		getLogger().log(Level.INFO, "Disabling...");
		PermissionManager.close();
		getLogger().log(Level.SEVERE, "Successfully disabled!");
	}

	private YAMLProcessor setupConfig(File file) {
		if (!file.exists()) {
			try {
				InputStream input = getClass().getResource("config.yml").openStream();
				if (input != null) {
					FileOutputStream output = null;
					try {
						if (file.getParentFile() != null) {
							file.getParentFile().mkdirs();
						}
						output = new FileOutputStream(file);
						byte[] buf = new byte[8192];
						int length;

						while ((length = input.read(buf)) > 0) {
							output.write(buf, 0, length);
						}
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						try {
							input.close();
						} catch (Exception ignored) {
						}
						try {
							if (output != null) {
								output.close();
							}
						} catch (Exception e) {
						}
					}
				}
			} catch (Exception e) {
			}
		}

		return new YAMLProcessor(file, false, YAMLFormat.EXTENDED);
	}
}
