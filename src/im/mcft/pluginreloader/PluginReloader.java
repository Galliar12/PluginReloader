package im.mcft.pluginreloader;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.plugin.UnknownDependencyException;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * A plugin used to manage other plugins.
 * 
 * @author Jon la Cour
 * @author MadJawa
 * @version 1.0.0
 */
public class PluginReloader extends JavaPlugin {
	public static final Logger logger = Logger.getLogger("Minecraft");

	@Override
	public void onDisable() {
		getServer().getServicesManager().unregisterAll(this);
		PluginDescriptionFile pdfFile = getDescription();
		String version = pdfFile.getVersion();
		log("Version " + version + " is disabled!", "info");
	}

	@Override
	public void onEnable() {
		PluginDescriptionFile pdfFile = getDescription();
		String version = pdfFile.getVersion();
		log("Version " + version + " enabled", "info");
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		if (cmd.getName().equalsIgnoreCase("plugin")) {
			if (args.length < 1) {
				return false;
			}
			String action = args[0];

			if (!(action.equalsIgnoreCase("load") || action.equalsIgnoreCase("unload") || action.equalsIgnoreCase("reload"))) {
				sender.sendMessage(ChatColor.GOLD + "Invalid action specified");
				return false;
			}

			if (!sender.hasPermission("pluginreloader." + action)) {
				sender.sendMessage(ChatColor.RED + "You do not have the permission to do this");
				return true;
			}

			if (args.length == 1) {
				sender.sendMessage(ChatColor.GOLD + "You must specify at least one plugin");
				return true;
			}

			// Reload all specified plugins.
			for (int i = 1; i < args.length; ++i) {
				String plName = args[i];

				try {
					if (action.equalsIgnoreCase("unload")) {
						unloadPlugin(plName);
						System.gc(); // Remove the Windows file lock
						sender.sendMessage(ChatColor.GRAY + "Unloaded " + ChatColor.RED + plName + ChatColor.GRAY + " successfully!");
					}
					else if (action.equalsIgnoreCase("load")) {
						loadPlugin(plName);
						sender.sendMessage(ChatColor.GRAY + "Loaded " + ChatColor.GREEN + plName + ChatColor.GRAY + " successfully!");
					}
					else if (action.equalsIgnoreCase("reload")) {
						unloadPlugin(plName);
						loadPlugin(plName);
						sender.sendMessage(ChatColor.GRAY + "Reloaded " + ChatColor.GREEN + plName + ChatColor.GRAY + " successfully!");
					}
				} catch (Exception e) {
					e.printStackTrace();
					sender.sendMessage(ChatColor.GRAY + "Error with " + ChatColor.RED + plName + ChatColor.GRAY + ": " + ChatColor.GOLD + getExceptionMessage(e) + ChatColor.GRAY + " (check console for more details)");
				}
			}

			return true;
		}

		return false;
	}

	// tries to retrieve the most precise error message
	private static String getExceptionMessage(Throwable e) {
		if (e.getCause() != null) {
			String msg = getExceptionMessage(e.getCause());
			if (!msg.equalsIgnoreCase(e.getClass().getName())) {
				return msg;
			}
		}

		if (e.getLocalizedMessage() != null)
			return e.getLocalizedMessage();
		else if (e.getMessage() != null)
			return e.getMessage();
		else if (e.getClass().getCanonicalName() != null)
			return e.getClass().getCanonicalName();
		else
			return e.getClass().getName();
	}

	@SuppressWarnings("unchecked")
	private void unloadPlugin(String pluginName) throws SecurityException,
			NoSuchFieldException, IllegalArgumentException,
			IllegalAccessException {
		PluginManager manager = getServer().getPluginManager();
		SimplePluginManager spm = (SimplePluginManager) manager;
		List<Plugin> plugins = null;

		if (spm != null) {
			Field pluginsField = spm.getClass().getDeclaredField("plugins");
			pluginsField.setAccessible(true);
			plugins = (List<Plugin>) pluginsField.get(spm);
		}

		// Sometimes plugins load multiple times...
		for (Plugin pl : manager.getPlugins()) {
			if (pl.getDescription().getName().equalsIgnoreCase(pluginName)) {
				manager.disablePlugin(pl);
				if (plugins != null && plugins.contains(pl)) {
					plugins.remove(pl);
				}
			}
		}
	}

	private void loadPlugin(String pluginName) throws InvalidPluginException,
			InvalidDescriptionException, UnknownDependencyException {
		PluginManager manager = getServer().getPluginManager();
		Plugin plugin = manager.loadPlugin(new File("plugins", pluginName + ".jar"));

		if (plugin == null) {
			return;
		}

		manager.enablePlugin(plugin);
	}

	/**
	 * Sends a message to the logger.
	 * 
	 * @param s
	 *            The message to send
	 * @param type
	 *            The level (info, warning, severe)
	 * @since 1.0.0
	 */
	public static void log(final String s, final String type) {
		String message = "[PluginReloader] " + s;
		String t = type.toLowerCase();
		if (t != null) {
			boolean info = t.equals("info");
			boolean warning = t.equals("warning");
			boolean severe = t.equals("severe");
			if (info) {
				logger.info(message);
			} else if (warning) {
				logger.warning(message);
			} else if (severe) {
				logger.severe(message);
			} else {
				logger.info(message);
			}
		}
	}
}
