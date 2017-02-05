package nu.nerd.badsanta;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

// ----------------------------------------------------------------------------
/**
 * Utility methods.
 */
public class Util {
    // ------------------------------------------------------------------------

    public static void main(String[] args) {
        int[] bins = new int[4];
        for (int i = 0; i < 100; ++i) {
            ++bins[Util.random(1, 4) - 1];
        }

        for (int total : bins) {
            System.out.println(total);
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Set the item name and lore lines of an item.
     *
     * @param item the item.
     * @param name the name to give the item.
     * @param lore a list of lore lines to add in sequence, with colour codes
     *        already translated.
     */
    public static void setItemNameAndLore(ItemStack item, String name, List<String> lores) {
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        meta.setLore(lores);
        item.setItemMeta(meta);
    }

    // ------------------------------------------------------------------------
    /**
     * Format a Location as a string.
     *
     * @param loc the Location.
     * @return a String containing (world, x, y, z).
     */
    public static String formatLocation(Location loc) {
        StringBuilder s = new StringBuilder();
        s.append('(').append(loc.getWorld().getName());
        s.append(", ").append(loc.getBlockX());
        s.append(", ").append(loc.getBlockY());
        s.append(", ").append(loc.getBlockZ());
        return s.append(')').toString();
    }

    // ------------------------------------------------------------------------
    /**
     * Return a random integer in the range [0,values-1].
     *
     * @param values the number of possible values.
     * @return a random integer in the range [0,values-1].
     */
    public static int randomInt(int values) {
        return _random.nextInt(values);
    }

    // ------------------------------------------------------------------------
    /**
     * Return a random selection from the specified list of options.
     *
     * @param options a list of possible options to return; must be non-empty.
     * @return a random selection from the list.
     */
    public static <T> T randomChoice(ArrayList<T> options) {
        return options.get(_random.nextInt(options.size()));
    }

    // ------------------------------------------------------------------------
    /**
     * Return a random integer in the range [min,max].
     *
     * @param min the minimum possible value.
     * @param max the maximum possible value.
     * @return a random integer in the range [min,max].
     */
    public static int random(int min, int max) {
        return min + _random.nextInt(max - min + 1);
    }

    // ------------------------------------------------------------------------
    /**
     * Return a random double in the range [min,max].
     *
     * @param min the minimum possible value.
     * @param max the maximum possible value.
     * @return a random double in the range [min,max].
     */
    public static double random(double min, double max) {
        return min + _random.nextDouble() * (max - min);
    }

    // ------------------------------------------------------------------------
    /**
     * Random number generator.
     */
    protected static Random _random = new Random();
} // class Util