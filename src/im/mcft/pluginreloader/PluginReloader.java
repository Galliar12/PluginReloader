package im.mcft.pluginreloader;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.entity.Player;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.plugin.UnknownDependencyException;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * A plugin used to load, unload, or reload plugins on your Bukkit server.
 * 
 * @author Jon la Cour
 * @author MadJawa
 * @version 1.0.0
 */
public class PluginReloader extends JavaPlugin {
	public static final Logger logger = Logger.getLogger("Minecraft");

	public final void onDisable() {
		getServer().getServicesManager().unregisterAll(this);
		PluginDescriptionFile pdfFile = getDescription();
		String version = pdfFile.getVersion();
		log("Version " + version + " is disabled!", "info");
	}

	public final void onEnable() {
		PluginDescriptionFile pdfFile = getDescription();
		String version = pdfFile.getVersion();
		log("Version " + version + " enabled", "info");
	}

	@Override
	public final boolean onCommand(final CommandSender sender, final Command cmd, final String commandLabel, final String[] args) {
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

			for (int i = 1; i < args.length; ++i) {
				String plName = args[i];

				try {
					if (action.equalsIgnoreCase("unload")) {
						unloadPlugin(plName);
						System.gc(); // Remove the Windows file lock
						sender.sendMessage(ChatColor.GRAY + "Unloaded " + ChatColor.RED + plName + ChatColor.GRAY + " successfully!");
						if (sender instanceof Player) {
							log(sender.getName() + " has unloaded " + plName + ".", "info");
						}
					} else if (action.equalsIgnoreCase("load")) {
						loadPlugin(plName);
						sender.sendMessage(ChatColor.GRAY + "Loaded " + ChatColor.GREEN + plName + ChatColor.GRAY + " successfully!");
						if (sender instanceof Player) {
							log(sender.getName() + " has loaded " + plName + ".", "info");
						}
					} else if (action.equalsIgnoreCase("reload")) {
						unloadPlugin(plName);
						loadPlugin(plName);
						sender.sendMessage(ChatColor.GRAY + "Reloaded " + ChatColor.GREEN + plName + ChatColor.GRAY + " successfully!");
						if (sender instanceof Player) {
							log(sender.getName() + " has reloaded " + plName + ".", "info");
						}
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

	/**
	 * Retrieves a precise and short error message returing only where something
	 * went wrong.
	 * 
	 * @param e
	 *            Throwable exception
	 * @return String Name of class where something went wrong.
	 */
	private static String getExceptionMessage(final Throwable e) {
		if (e.getCause() != null) {
			String msg = getExceptionMessage(e.getCause());
			if (!msg.equalsIgnoreCase(e.getClass().getName())) {
				return msg;
			}
		}

		if (e.getLocalizedMessage() != null) {
			return e.getLocalizedMessage();
		} else if (e.getMessage() != null) {
			return e.getMessage();
		} else if (e.getClass().getCanonicalName() != null) {
			return e.getClass().getCanonicalName();
		} else {
			return e.getClass().getName();
		}
	}

	/**
	 * Unloads a plugin by name.
	 * 
	 * @param pluginName
	 *            The name of the plugin to unload.
	 * @throws NoSuchFieldException
	 *             Unknown field for
	 *             plugins/lookupNames/commandMap/knownCommands.
	 * @throws IllegalAccessException
	 *             Unable to access plugin field(s).
	 */
	@SuppressWarnings("unchecked")
	private void unloadPlugin(final String pluginName)
			throws NoSuchFieldException, IllegalAccessException {
		PluginManager manager = getServer().getPluginManager();
		SimplePluginManager spm = (SimplePluginManager) manager;
		SimpleCommandMap commandMap = null;
		List<Plugin> plugins = null;
		Map<String, Plugin> lookupNames = null;
		Map<String, Command> knownCommands = null;

		if (spm != null) {
			Field pluginsField = spm.getClass().getDeclaredField("plugins");
			pluginsField.setAccessible(true);
			plugins = (List<Plugin>) pluginsField.get(spm);

			Field lookupNamesField = spm.getClass().getDeclaredField("lookupNames");
			lookupNamesField.setAccessible(true);
			lookupNames = (Map<String, Plugin>) lookupNamesField.get(spm);

			Field commandMapField = spm.getClass().getDeclaredField("commandMap");
			commandMapField.setAccessible(true);
			commandMap = (SimpleCommandMap) commandMapField.get(spm);

			Field knownCommandsField = commandMap.getClass().getDeclaredField("knownCommands");
			knownCommandsField.setAccessible(true);
			knownCommands = (Map<String, Command>) knownCommandsField.get(commandMap);
		}

		for (Plugin pl : manager.getPlugins()) {
			if (pl.getDescription().getName().equalsIgnoreCase(pluginName)) {
				manager.disablePlugin(pl);
				if (plugins != null && plugins.contains(pl)) {
					plugins.remove(pl);
				}

				if (lookupNames != null && lookupNames.containsKey(pluginName)) {
					lookupNames.remove(pluginName);
				}

				if (commandMap != null) {
					for (Iterator<Map.Entry<String, Command>> it = knownCommands.entrySet().iterator(); it.hasNext();) {
						Map.Entry<String, Command> entry = it.next();
						if (entry.getValue() instanceof PluginCommand) {
							PluginCommand c = (PluginCommand) entry.getValue();
							if (c.getPlugin() == pl) {
								c.unregister(commandMap);
								it.remove();
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Loads a plugin by name.
	 * 
	 * @param pluginName
	 *            The name of the plugin to load.
	 * @throws InvalidPluginException
	 *             Not a plugin.
	 * @throws InvalidDescriptionException
	 *             Invalid description.
	 * @throws UnknownDependencyException
	 *             Missing dependency.
	 * @since 1.0.0
	 */
	private void loadPlugin(final String pluginName)
			throws InvalidPluginException, InvalidDescriptionException,
			UnknownDependencyException {
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
