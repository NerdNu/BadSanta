package nu.nerd.badsanta;

import org.bukkit.Color;
import org.bukkit.Effect;
import org.bukkit.FireworkEffect;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;

// ----------------------------------------------------------------------------
/**
 * Represents the state of one quest objective.
 *
 * The player must travel to the objective and click on it before it despawns.
 */
public class Objective {
    // ------------------------------------------------------------------------
    /**
     * Constructor.
     *
     * @param loc the location of the objective.
     * @param lifeTicks the number of ticks this objective should live.
     */
    public Objective(Location loc, int lifeTicks) {
        _location = loc.clone();
        _lifeTicks = lifeTicks;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the objective location.
     *
     * @return the objective location.
     */
    public Location getLocation() {
        return _location;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the objective block.
     *
     * @return the objective block.
     */
    public Block getBlock() {
        return _location.getBlock();
    }

    // ------------------------------------------------------------------------
    /**
     * Return the remaining life time in ticks.
     *
     * @return the remaining life time in ticks.
     */
    public int getLifeInTicks() {
        return _lifeTicks;
    }

    // ------------------------------------------------------------------------
    /**
     * Return true if the objective is still alive (has not been found or timed
     * out).
     *
     * This method is intended to be called once per tick for each objective in
     * existence. It also updates particle effects around the objective.
     *
     * @return true if the objective is still alive (has not been found or timed
     *         out).
     */
    public boolean isAlive() {
        --_lifeTicks;
        if (_lifeTicks <= 0) {
            BadSanta.PLUGIN.getLogger().info("Pot at " + Util.formatLocation(_location) + " timed out.");
            return false;
        }

        World.Spigot spigot = _location.getWorld().spigot();
        spigot.playEffect(_location, Effect.COLOURED_DUST, 0, 0,
                          BadSanta.CONFIG.OBJECTIVE_PARTICLE_RADIUS,
                          BadSanta.CONFIG.OBJECTIVE_PARTICLE_RADIUS,
                          BadSanta.CONFIG.OBJECTIVE_PARTICLE_RADIUS,
                          1.0f, BadSanta.CONFIG.OBJECTIVE_PARTICLE_COUNT, 64);
        for (Entity entity : _location.getWorld().getNearbyEntities(_location, 5, 5, 5)) {
            if (entity instanceof Player) {
                spawnLoot((Player) entity);
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------------
    /**
     * Spawn firework effect, level up sound and loot at the objective location.
     *
     * @param player the player who found the objective.
     */
    public void spawnLoot(Player player) {
        BadSanta.PLUGIN.getLogger().info("Objective reached by " + player.getName() + " " +
                                         Util.formatLocation(getLocation()));

        spawnFirework();

        World world = getLocation().getWorld();
        world.playSound(getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 3, 1);

        Drop drop = Util.randomChoice(BadSanta.CONFIG.DROPS_OBJECTIVE);
        world.dropItemNaturally(getLocation(), drop.generate());
    }

    // ------------------------------------------------------------------------
    /**
     * Spawn a firework at the objective location.
     */
    protected void spawnFirework() {
        World world = _location.getWorld();
        Firework firework = (Firework) world.spawnEntity(_location, EntityType.FIREWORK);
        if (firework != null) {
            FireworkEffect.Builder builder = FireworkEffect.builder();
            if (Math.random() < 0.3) {
                builder.withFlicker();
            }
            if (Math.random() < 0.3) {
                builder.withTrail();
            }

            builder.with(FIREWORK_TYPES[Util.randomInt(FIREWORK_TYPES.length)]);

            builder.withColor(Color.RED);
            builder.withColor(Color.WHITE);
            builder.withColor(Color.GREEN);

            final int reds = 1 + Util.randomInt(3);
            for (int i = 0; i < reds; ++i) {
                int darker = Util.randomInt(128);
                builder.withColor(Color.fromRGB(255 - darker, darker / 2, darker / 2));
            }

            final int greens = 1 + Util.randomInt(3);
            builder.withFade(Color.fromRGB(255, 255, 255));
            for (int i = 0; i < greens; ++i) {
                int darker = Util.randomInt(128);
                builder.withColor(Color.fromRGB(darker, 255 - darker, darker));
            }

            FireworkMeta meta = firework.getFireworkMeta();
            meta.setPower(Util.randomInt(2));
            meta.addEffect(builder.build());
            firework.setFireworkMeta(meta);
        }
    } // spawnFirework

    // ------------------------------------------------------------------------
    /**
     * Remove the objective by turning it into air.
     */
    public void vaporise() {
        getBlock().setType(Material.AIR);
    }

    // ------------------------------------------------------------------------
    /**
     * Firework types.
     */
    protected static final FireworkEffect.Type[] FIREWORK_TYPES = { Type.BALL, Type.BALL_LARGE, Type.STAR, Type.BURST };

    /**
     * Location of the objective.
     */
    protected Location _location;

    /**
     * The number of ticks this objective should live.
     */
    protected int _lifeTicks;
} // class Objective