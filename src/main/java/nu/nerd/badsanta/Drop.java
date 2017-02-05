package nu.nerd.badsanta;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

// ----------------------------------------------------------------------------
/**
 * Represents a possible item drop.
 */
public class Drop {
    /**
     * Construct an instance by loading the ItemStack at "item" in the specified
     * configuration section.
     *
     * @param section the section.
     */
    public Drop(ConfigurationSection section) {
        _itemStack = (ItemStack) section.get("item");
        _min = section.getInt("min", 1);
        _max = section.getInt("max", Math.max(1, _min));
        _dropChance = section.getDouble("chance", 0.0);
    }

    // ------------------------------------------------------------------------
    /**
     * Return the probability of this drop, in the range [0.0,1.0].
     */
    public double getDropChance() {
        return _dropChance;
    }

    // ------------------------------------------------------------------------
    /**
     * Generate a new ItemStack by selecting a random number of items within the
     * configured range.
     *
     * @return the ItemStack.
     */
    public ItemStack generate() {
        ItemStack result = _itemStack.clone();
        result.setAmount(Util.random(_min, _max));
        return result;
    }

    // ------------------------------------------------------------------------
    /**
     * Return a brief description of the drop item, its probablility and count.
     * 
     * @return a brief description of the drop.
     */
    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append(_dropChance * 100).append("% ");
        if (_min == _max) {
            s.append(_min);
        } else {
            s.append('[').append(_min).append(',').append(_max).append(']');
        }
        s.append(' ');

        if (_itemStack == null) {
            s.append("nothing");
        } else {
            s.append(_itemStack.getType().name());
            if (_itemStack.getDurability() != 0) {
                s.append(':');
                s.append(_itemStack.getDurability());
            }

            ItemMeta meta = _itemStack.getItemMeta();
            if (meta instanceof SkullMeta) {
                SkullMeta skull = (SkullMeta) meta;
                s.append(" (").append(skull.getOwner()).append(')');

            }
            if (meta.getDisplayName() != null) {
                s.append(" \"").append(meta.getDisplayName()).append('"');
            }
        }
        return s.toString();
    }

    // ------------------------------------------------------------------------
    /**
     * Minimum number of items in item stack.
     */
    protected int _min;

    /**
     * Maximum number of items in item stack.
     */
    protected int _max;

    /**
     * Drop chance, [0.0,1.0].
     */
    protected double _dropChance;

    /**
     * The ItemStack.
     */
    protected ItemStack _itemStack;
} // class Drop