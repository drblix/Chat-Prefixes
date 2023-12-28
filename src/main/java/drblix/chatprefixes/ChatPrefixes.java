package drblix.chatprefixes;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class ChatPrefixes extends JavaPlugin {

    private static final Hashtable<UUID, PrefixInfo> prefixInfoTable = new Hashtable<>();

    private static char borderStart;
    private static char borderEnd;

    @Override
    public void onEnable() {
        // registers events
        getServer().getPluginManager().registerEvents(new EventListeners(), this);

        // creates the configuration file for this plugin
        // for some reason, .getKeys() and other associated config methods don't work until this method is called
        saveDefaultConfig();

        // gets the border from the config file, creating the field if it isn't present already
        String border = getConfig().getString("prefix-border");
        if (border == null) {
            getConfig().addDefault("prefix-border", "[]");
            border = getConfig().getString("prefix-border");
        }

        // assign border start/end characters, resorting to default if an exception occurs
        try {
            borderStart = border.charAt(0);
            borderEnd = border.charAt(1);
        }
        catch (IndexOutOfBoundsException e) {
            getLogger().severe("Provided prefix border is less than 2 characters; resorting to defaults!");
            borderStart = '[';
            borderEnd = ']';
        }

        // fill prefixInfoTable with information currently in configuration file
        updateInfoTable();

        getCommand("prefix-add").setExecutor(this);
        getCommand("prefix-remove").setExecutor(this);

        sendMessageToServer(Component.text("Plugin has been successfully ")
                .append(Component.text("loaded", NamedTextColor.GREEN))
                .append(Component.text("!")));
    }

    @Override
    public void onDisable() {
        sendMessageToServer(Component.text("Plugin has been successfully ")
                .append(Component.text("unloaded", NamedTextColor.RED))
                .append(Component.text("!")));

        saveConfig();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // sender of command is not a player in game
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Only players may use this command!", NamedTextColor.RED));
            return false;
        }

        Player player = (Player)sender;

        // player is not operator
        if (!player.isOp()) {
            sender.sendMessage(Component.text("You must be an operator to use this command!", NamedTextColor.RED));
            return false;
        }

        Player target = Bukkit.getServer().getPlayer(args[0]);

        // target player is not online
        if (target == null) {
            sender.sendMessage(Component.text(args[0] + " is not online! It is suggested you remove them manually via the config file.", NamedTextColor.RED));
            return false;
        }

        if (command.getName().equals("prefix-add")) {
            // incorrect amount of arguments provided
            if (args.length != 3) {
                player.sendMessage(Component.text("Incorrect number of arguments provided! Must be 3", NamedTextColor.RED));
                return false;
            }

            String tagName = args[1];
            String colour = args[2];
            // colour argument is not in valid format
            if (TextColor.fromHexString(colour) == null) {
                player.sendMessage(Component.text("Invalid colour has been provided! Must be in hexadecimal format (#ff0000)!", NamedTextColor.RED));
                return false;
            }

            // player must not already have a prefix assigned
            if (!addInfoToConfig(target.getUniqueId(), tagName, colour)) {
                player.sendMessage(Component.text("Player already has a prefix! Remove the one they currently have before assigning another one"));
                return false;
            }

            player.sendMessage(Component.text(target.getName() + " has been successfully assigned the prefix ")
                    .append(Component.text(borderStart + tagName + borderEnd, TextColor.fromHexString(colour))));
        }
        else if (command.getName().equals("prefix-remove")) {
            // player must have a prefix assigned
            if (!removeInfoFromConfig(target.getUniqueId())) {
                player.sendMessage(Component.text("This player does not currently have a prefix assigned!", NamedTextColor.RED));
                return false;
            }

            player.sendMessage(Component.text(target.getName() + " has had their prefix successfully removed"));
        }

        return true;
    }

    @Contract(pure = true)
    public static @Nullable PrefixInfo getInfoForUuid(UUID uuid) {
        return prefixInfoTable.get(uuid);
    }

    /**
     * Retrieves the front border to be used with the tag name
     */
    public static char getFrontBorder() {
        return borderStart;
    }

    /**
     * Retrieves the end border to be used with the tag name
     */
    public static char getEndBorder() {
        return borderEnd;
    }

    /**
     * Attempts to add the provided information to the configuration file
     * @param playerUuid The UUID of the player
     * @param tagName The name of the tag that is to be created
     * @param colour The colour of that tag that is to be created (must be hexadecimal)
     * @return True if the info was successfully processed, false if otherwise
     */
    private boolean addInfoToConfig(UUID playerUuid, String tagName, String colour) {
        // player is already in table
        if (prefixInfoTable.get(playerUuid) != null) return false;

        // gets the prefix section from the config file, creating it if not already existing
        ConfigurationSection prefixSection = getConfig().getConfigurationSection("prefixes");
        if (prefixSection == null) {
            prefixSection = getConfig().createSection("prefixes");
        }

        // creates hashmap for each config value that is to be added
        final HashMap<String, String> valueMap = new HashMap<>();
        valueMap.put("tag-name", tagName);
        valueMap.put("colour", colour);

        // adds PrefixInfo object to table
        prefixInfoTable.put(playerUuid, new PrefixInfo(tagName, colour));

        // creates new section with provided hashmap
        prefixSection.createSection(playerUuid.toString(), valueMap);

        saveConfig();
        return true;
    }

    /**
     * Attempts to remove tag information from the provided player UUID
     * @param playerUuid The UUID of the player to remove
     * @return True if the information was successfully removed from the configuration file, false otherwise
     */
    private boolean removeInfoFromConfig(UUID playerUuid) {
        // player is not in table, so is not in config file
        if (prefixInfoTable.get(playerUuid) == null) return false;

        // attempt to get the prefix config section
        ConfigurationSection prefixSection = getConfig().getConfigurationSection("prefixes");
        if (prefixSection == null) {
            prefixSection = getConfig().createSection("prefixes");
        }

        // delete the player's tag information from the config file
        prefixSection.set(playerUuid.toString(), null);
        // remove player from table
        prefixInfoTable.remove(playerUuid);

        saveConfig();
        return true;
    }

    /**
     * Updates the {@link #prefixInfoTable} with all latest prefix information
     */
    private void updateInfoTable() {
        // clear all info that's currently in the table
        prefixInfoTable.clear();

        // retrieves the 'prefixes' config section and its associated keys
        // the keys for each container of information is the UUID of the player
        ConfigurationSection prefixSection = getConfig().getConfigurationSection("prefixes");
        if (prefixSection == null) {
            prefixSection = getConfig().createSection("prefixes");
            saveConfig();
        }

        final Set<String> keys = prefixSection.getKeys(false);

        // creates prefix info objects for each key in 'prefixes' section
        for (String key : keys) {
            // gets the associated information for this key
            String tagName = prefixSection.getString(key + ".tag-name");
            String colour = prefixSection.getString(key + ".colour");
            PrefixInfo info = new PrefixInfo(tagName, colour);

            // adds information to hashtable
            prefixInfoTable.put(UUID.fromString(key), info);
        }
    }

    /**
     * Sends a fancy coloured message to the server
     * @param msg The message to send
     */
    private void sendMessageToServer(TextComponent msg) {
        final TextComponent textComponent = Component.text("[")
                .append(Component.text("ChatPrefixes", NamedTextColor.GREEN))
                .append(Component.text("] "))
                .append(msg);

        getServer().sendMessage(textComponent);
    }


    /**
     * Represents a container of information for each player that possesses a chat tag
     */
    public static class PrefixInfo {
        private final String tagName;
        private final String colour;

        public PrefixInfo(String tagName, String colour) {
            this.tagName = tagName;
            this.colour = colour;
        }

        public String getTagName() {
            return tagName;
        }

        public String getColour() {
            return colour;
        }
    }
}
