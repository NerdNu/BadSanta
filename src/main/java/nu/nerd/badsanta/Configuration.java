package nu.nerd.badsanta;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.entity.EntityEvent;

// ----------------------------------------------------------------------------
/**
 * Reads and exposes the plugin configuration.
 */
public class Configuration {
    /**
     * If true, log configuration when loaded.
     */
    public boolean DEBUG_CONFIG;

    /**
     * If true, log special mob deaths.
     */
    public boolean DEBUG_DEATH;

    /**
     * If true, log special mob spawns.
     */
    public boolean DEBUG_SPAWN;

    /**
     * The world where mobs are affected.
     */
    public World WORLD;

    /**
     * World border radius in the affected world (square world assumed).
     */
    public int WORLD_BORDER;

    /**
     * Santa chance.
     */
    public double MOB_SANTA_CHANCE;

    /**
     * Santa health.
     */
    public int MOB_SANTA_HEALTH;

    /**
     * Santa mob player head.
     */
    public String MOB_SANTA_HEAD_SKIN;

    /**
     * Chance of dropping a Santa head.
     */
    public float MOB_SANTA_HEAD_CHANCE;

    /**
     * Elf health.
     */
    public int MOB_ELF_HEALTH;

    /**
     * Chance that an elf is a baby zombie.
     */
    public double MOB_ELF_BABY_CHANCE;

    /**
     * True if the mobs wear coloured leather armour.
     */
    public boolean ARMOUR_WORN;

    /**
     * Base drop chance for armour pieces, modified by looting.
     */
    public float ARMOUR_DROP_CHANCE;

    /**
     * Chance of an objective spawning upon mob death.
     */
    public double OBJECTIVE_CHANCE;

    /**
     * Maximum number of objectives allowed in the world simultaneously.
     */
    public int OBJECTIVE_MAX;

    /**
     * Minimum distance from mob death for an objective to spawn.
     */
    public int OBJECTIVE_RANGE_MIN;

    /**
     * Maximum distance from mob death for an objective to spawn.
     */
    public int OBJECTIVE_RANGE_MAX;

    /**
     * Extra ticks of time given to the player to find the pot.
     */
    public int OBJECTIVE_EXTRA_TICKS;

    /**
     * The minimum expected movement speed of players, based upon which the
     * duration of existence of an objective will be calculated.
     */
    public double OBJECTIVE_MIN_PLAYER_SPEED;

    /**
     * Radius of the particle cloud around the objective.
     */
    public float OBJECTIVE_PARTICLE_RADIUS;

    /**
     * Number of particles in the particle cloud around the objective.
     */
    public int OBJECTIVE_PARTICLE_COUNT;

    /**
     * Player skins applied to objective skulls.
     */
    public ArrayList<String> OBJECTIVE_SKINS;

    /**
     * Item name on maps to objectives.
     */
    public String OBJECTIVE_MAP_NAME;

    /**
     * Lore on maps to objectives.
     * 
     * By convention, the String must be split at the pipe character, after
     * message substitution of {0} as the objective coordinates.
     */
    public String OBJECTIVE_MAP_LORE;

    /**
     * Chance of a firework drop.
     */
    public double DROPS_FIREWORK_CHANCE;

    /**
     * Regular drops.
     */
    public ArrayList<Drop> DROPS_REGULAR;

    /**
     * Special drops, which drop when a player has damaged the mob in the last 5
     * seconds.
     */
    public ArrayList<Drop> DROPS_SPECIAL;

    /**
     * Objective drops, one of which is randomly selected and dropped when a
     * player reaches an objective within the time limit.
     */
    public ArrayList<Drop> DROPS_OBJECTIVE;

    // ------------------------------------------------------------------------
    /**
     * Load the plugin configuration.
     */
    public void reload() {
        BadSanta.PLUGIN.reloadConfig();

        DEBUG_CONFIG = BadSanta.PLUGIN.getConfig().getBoolean("debug.config");
        DEBUG_DEATH = BadSanta.PLUGIN.getConfig().getBoolean("debug.death");
        DEBUG_SPAWN = BadSanta.PLUGIN.getConfig().getBoolean("debug.spawn");

        WORLD = Bukkit.getWorld(BadSanta.PLUGIN.getConfig().getString("world.name"));
        if (WORLD == null) {
            WORLD = Bukkit.getWorld("world");
        }
        WORLD_BORDER = BadSanta.PLUGIN.getConfig().getInt("world.border");

        MOB_SANTA_CHANCE = BadSanta.PLUGIN.getConfig().getDouble("mob.santa.chance");
        MOB_SANTA_HEALTH = BadSanta.PLUGIN.getConfig().getInt("mob.santa.health");
        MOB_SANTA_HEAD_SKIN = BadSanta.PLUGIN.getConfig().getString("mob.santa.head.skin");
        MOB_SANTA_HEAD_CHANCE = (float) BadSanta.PLUGIN.getConfig().getDouble("mob.santa.head.chance");
        MOB_ELF_HEALTH = BadSanta.PLUGIN.getConfig().getInt("mob.elf.health");
        MOB_ELF_BABY_CHANCE = BadSanta.PLUGIN.getConfig().getDouble("mob.elf.baby_chance");

        ARMOUR_WORN = BadSanta.PLUGIN.getConfig().getBoolean("armour.worn");
        ARMOUR_DROP_CHANCE = (float) BadSanta.PLUGIN.getConfig().getDouble("armour.drop_chance");

        OBJECTIVE_CHANCE = BadSanta.PLUGIN.getConfig().getDouble("objective.chance");
        OBJECTIVE_MAX = BadSanta.PLUGIN.getConfig().getInt("objective.max");
        OBJECTIVE_RANGE_MIN = BadSanta.PLUGIN.getConfig().getInt("objective.range.min");
        OBJECTIVE_RANGE_MAX = BadSanta.PLUGIN.getConfig().getInt("objective.range.max");
        OBJECTIVE_EXTRA_TICKS = BadSanta.PLUGIN.getConfig().getInt("objective.extra_ticks");
        OBJECTIVE_MIN_PLAYER_SPEED = BadSanta.PLUGIN.getConfig().getDouble("objective.min_player_speed");
        OBJECTIVE_SKINS = new ArrayList<String>(BadSanta.PLUGIN.getConfig().getStringList("objective.skins"));
        OBJECTIVE_MAP_NAME = ChatColor.translateAlternateColorCodes('&', BadSanta.PLUGIN.getConfig().getString("objective.map.name"));
        OBJECTIVE_MAP_LORE = ChatColor.translateAlternateColorCodes('&', BadSanta.PLUGIN.getConfig().getString("objective.map.lore"));
        OBJECTIVE_PARTICLE_RADIUS = (float) BadSanta.PLUGIN.getConfig().getDouble("objective.particle.radius");
        OBJECTIVE_PARTICLE_COUNT = BadSanta.PLUGIN.getConfig().getInt("objective.particle.count");

        DROPS_FIREWORK_CHANCE = BadSanta.PLUGIN.getConfig().getDouble("drops.firework.chance");
        DROPS_REGULAR = loadDrops(BadSanta.PLUGIN.getConfig().getConfigurationSection("drops.regular"));
        DROPS_SPECIAL = loadDrops(BadSanta.PLUGIN.getConfig().getConfigurationSection("drops.special"));
        DROPS_OBJECTIVE = loadDrops(BadSanta.PLUGIN.getConfig().getConfigurationSection("drops.objective"));

        extractSchematics();

        if (DEBUG_CONFIG) {
            Logger logger = BadSanta.PLUGIN.getLogger();
            logger.info("Configuration:");
            logger.info("DEBUG_DEATH: " + DEBUG_DEATH);
            logger.info("DEBUG_SPAWN: " + DEBUG_SPAWN);

            logger.info("WORLD: " + WORLD.getName());
            logger.info("WORLD_BORDER: " + WORLD_BORDER);

            logger.info("MOB_SANTA_CHANCE: " + MOB_SANTA_CHANCE);
            logger.info("MOB_SANTA_HEALTH: " + MOB_SANTA_HEALTH);
            logger.info("MOB_SANTA_HEAD_SKIN: " + MOB_SANTA_HEAD_SKIN);
            logger.info("MOB_SANTA_HEAD_CHANCE: " + MOB_SANTA_HEAD_CHANCE);
            logger.info("MOB_ELF_HEALTH: " + MOB_ELF_HEALTH);
            logger.info("MOB_ELF_BABY_CHANCE: " + MOB_ELF_BABY_CHANCE);

            logger.info("ARMOUR_WORN: " + ARMOUR_WORN);
            logger.info("ARMOUR_DROP_CHANCE: " + ARMOUR_DROP_CHANCE);

            logger.info("OBJECTIVE_CHANCE: " + OBJECTIVE_CHANCE);
            logger.info("OBJECTIVE_MAX: " + OBJECTIVE_MAX);
            logger.info("OBJECTIVE_RANGE_MIN: " + OBJECTIVE_RANGE_MIN);
            logger.info("OBJECTIVE_RANGE_MAX: " + OBJECTIVE_RANGE_MAX);
            logger.info("OBJECTIVE_EXTRA_TICKS: " + OBJECTIVE_EXTRA_TICKS);
            logger.info("OBJECTIVE_MIN_PLAYER_SPEED: " + OBJECTIVE_MIN_PLAYER_SPEED);

            StringBuilder skins = new StringBuilder("OBJECTIVE_SKINS:");
            for (String skin : OBJECTIVE_SKINS) {
                skins.append(' ').append(skin);
            }
            logger.info(skins.toString());

            logger.info("OBJECTIVE_MAP_NAME: " + OBJECTIVE_MAP_NAME);
            logger.info("OBJECTIVE_MAP_LORE: " + OBJECTIVE_MAP_LORE);
            logger.info("OBJECTIVE_PARTICLE_RADIUS: " + OBJECTIVE_PARTICLE_RADIUS);
            logger.info("OBJECTIVE_PARTICLE_COUNT: " + OBJECTIVE_PARTICLE_COUNT);

            logger.info("DROPS_FIREWORK_CHANCE: " + DROPS_FIREWORK_CHANCE);
            logger.info("DROPS_REGULAR:");
            for (Drop drop : DROPS_REGULAR) {
                logger.info(drop.toString());
            }
            logger.info("DROPS_SPECIAL:");
            for (Drop drop : DROPS_SPECIAL) {
                logger.info(drop.toString());
            }
            logger.info("DROPS_OBJECTIVE:");
            for (Drop drop : DROPS_OBJECTIVE) {
                logger.info(drop.toString());
            }
        }
    } // reload

    // ------------------------------------------------------------------------
    /**
     * Return true if the world of the entity event is the configured affected
     * world.
     *
     * @param event an entity-related event.
     * @return true if the world of the entity event is the configured affected
     *         world.
     */
    public boolean isAffectedWorld(EntityEvent event) {
        return isAffectedWorld(event.getEntity().getLocation().getWorld());
    }

    // ------------------------------------------------------------------------
    /**
     * Return true if the specified world is the configured affected world.
     *
     * @param world the world to check.
     * @return true if the specified world is the configured affected world.
     */
    public boolean isAffectedWorld(World world) {
        return world.equals(WORLD);
    }

    // ------------------------------------------------------------------------
    /**
     * Load an array of {@link Drop}s from the specified section,
     *
     * @param section the configuration section.
     * @return the array of {@link Drop}s.
     */
    protected ArrayList<Drop> loadDrops(ConfigurationSection section) {
        ArrayList<Drop> drops = new ArrayList<Drop>();
        for (String key : section.getKeys(false)) {
            drops.add(new Drop(section.getConfigurationSection(key)));
        }
        return drops;
    }

    // ------------------------------------------------------------------------
    /**
     * Extract schematics of configured presents into the plugin folder, if not
     * already present.
     */
    protected void extractSchematics() {
        File schematicsDir = new File(BadSanta.PLUGIN.getDataFolder(), "schematics");
        schematicsDir.mkdirs();

        for (String name : OBJECTIVE_SKINS) {
            String baseName = name + ".schematic";
            File toWrite = new File(schematicsDir, baseName);
            if (!toWrite.exists()) {
                try (InputStream in = getClass().getResourceAsStream("/schematics/" + baseName)) {
                    if (in == null) {
                        BadSanta.PLUGIN.getLogger().severe("Unable to find configured schematic: " +
                                                           baseName + " in plugin JAR.");
                    } else {
                        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(toWrite, false));
                        byte[] buf = new byte[4096];
                        int totalLen = 0;
                        int readLen;
                        while ((readLen = in.read(buf)) > 0) {
                            out.write(buf, 0, readLen);
                            totalLen += readLen;
                        }
                        out.close();
                        BadSanta.PLUGIN.getLogger().info("Wrote schematic: " + baseName +
                                                         " (" + totalLen + " bytes)");
                    }
                } catch (IOException ex) {
                    BadSanta.PLUGIN.getLogger().severe("Error writing schematic: " + baseName);
                }
            }
        }
    } // extractSchematics

    // ------------------------------------------------------------------------

} // class Configuration