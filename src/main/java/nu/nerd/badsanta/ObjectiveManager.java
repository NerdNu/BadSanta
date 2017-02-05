package nu.nerd.badsanta;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import com.sk89q.worldedit.CuboidClipboard;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.world.DataException;

// ----------------------------------------------------------------------------
/**
 * Manages the collection of all {@link Objective}s in the world.
 */
public class ObjectiveManager {
    // ------------------------------------------------------------------------
    /**
     * If the maximum number of extant {@link Objective}s has not been exceeded,
     * then calculate a random location within the configured range limits of
     * the mob death point, and spawn an Objective there.
     *
     * This method should only be called if the death location was in the
     * affected world.
     *
     * The objective will not be spawned if it might collide with an entity
     * (e.g. a painting or item frame) - thus breaking it, if it would be above
     * the height limit of the world, if the maximum number of objectives
     * currently exists in the world, or if the objective would be over the
     * world border.
     *
     * @param deathLocation death location in the affected world.
     * @return a new Objective, or null if it could not be spawned.
     */
    public Objective spawnObjective(Location deathLocation) {
        if (_objectives.size() >= BadSanta.CONFIG.OBJECTIVE_MAX) {
            return null;
        }

        double angleRadians = 2 * Math.PI * Math.random();
        double range = Util.random(BadSanta.CONFIG.OBJECTIVE_RANGE_MIN, BadSanta.CONFIG.OBJECTIVE_RANGE_MAX);
        double dX = range * Math.cos(angleRadians);
        double dZ = range * Math.sin(angleRadians);

        World world = deathLocation.getWorld();
        Block floorBlock = world.getHighestBlockAt((int) (deathLocation.getX() + dX),
                                                   (int) (deathLocation.getZ() + dZ));
        Location objLocation = floorBlock.getLocation();

        if (Math.abs(objLocation.getBlockX()) < BadSanta.CONFIG.WORLD_BORDER &&
            Math.abs(objLocation.getBlockZ()) < BadSanta.CONFIG.WORLD_BORDER &&
            objLocation.getBlockY() < world.getMaxHeight() - 1) {
            // Don't spawn the objective if it might break item
            // frames/paintings.
            Collection<Entity> entities = objLocation.getWorld().getNearbyEntities(objLocation, 2, 2, 2);
            if (entities.size() == 0) {
                double distance = objLocation.distance(deathLocation);
                BadSanta.PLUGIN.getLogger().info("Distance " + distance);
                BadSanta.PLUGIN.getLogger().info("Travel ticks " +
                                                 (20 * (int) (distance / BadSanta.CONFIG.OBJECTIVE_MIN_PLAYER_SPEED)));
                int lifeInTicks = BadSanta.CONFIG.OBJECTIVE_EXTRA_TICKS +
                                  20 * (int) (distance / BadSanta.CONFIG.OBJECTIVE_MIN_PLAYER_SPEED);
                BadSanta.PLUGIN.getLogger().info("Life in ticks " + lifeInTicks);

                // This check should be redundant after the height check.
                if (objLocation.getBlock().getType() == Material.AIR) {
                    Objective objective = new Objective(objLocation, lifeInTicks);
                    _objectives.put(objective.getBlock(), objective);
                    markObjective(objLocation);
                    return objective;
                }
            }
        }
        return null;
    } // spawnObjective

    // ------------------------------------------------------------------------
    /**
     * Tick all objectives in the world, updating particle effects and removing
     * those that were found or timed out.
     */
    public void tickAll() {
        Iterator<Entry<Block, Objective>> it = _objectives.entrySet().iterator();
        while (it.hasNext()) {
            Entry<Block, Objective> entry = it.next();
            if (!entry.getValue().isAlive()) {
                entry.getValue().vaporise();
                it.remove();
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * If the block marks an objective, return the corresponding
     * {@link Objective} instance.
     *
     * @param block the block that might be an objective.
     * @return the {@link Objective} or null if there is none at the specified
     *         Block.
     */
    public Objective getObjective(Block block) {
        if (block.getType() == Material.SKULL) {
            Objective objective = _objectives.get(block);
            if (objective != null) {
                return objective;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------------
    /**
     * Remove a {@link Objective} from the the world, thus cancelling the
     * particle effects.
     *
     * @param objective the Objective.
     */
    public void removeObjective(Objective objective) {
        objective.vaporise();
        _objectives.remove(objective.getBlock());
    }

    // ------------------------------------------------------------------------
    /**
     * Remove all the pots.
     */
    public void removeAll() {
        Iterator<Entry<Block, Objective>> it = _objectives.entrySet().iterator();
        while (it.hasNext()) {
            Entry<Block, Objective> entry = it.next();
            entry.getValue().vaporise();
            it.remove();
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Set the specified block to a skull with a Christmas present skin.
     * 
     * The following code does not work on Spigot 1.10.2, despite it being in
     * excess of the best recommendations found online regarding how to achieve
     * the desired effect:
     * 
     * <pre>
     * Block objectiveBlock = objLocation.getBlock();
     * objectiveBlock.setType(Material.SKULL);
     * objectiveBlock.setData((byte) 1);
     * 
     * String skullOwner = Util.randomChoice(BadSanta.CONFIG.OBJECTIVE_SKINS);
     * Skull skull = (Skull) objectiveBlock.getState();
     * skull.setSkullType(SkullType.PLAYER);
     * skull.setRotation(BlockFace.SOUTH_SOUTH_EAST);
     * skull.setOwner(skullOwner);
     * skull.update(true);
     * 
     * for (Player player : objLocation.getWorld().getPlayers()) {
     *     player.sendBlockChange(objLocation, Material.SKULL, (byte) 1);
     * }
     * </pre>
     * 
     * The code above does set the block correctly and it can be seen if you
     * relog. However, if you don't relog, the client sees the objective marker
     * as just a rotated, regular skeleton skull. Despite trying several
     * different API calls I have not been able to get the client to see the
     * updated block.
     * 
     * I know that WorldEdit is able to paste skulls correctly. I suspect that
     * it is doing some kind of NMS magic. Rather than reverse engineer that and
     * add an NMS dependency, this method simply delegates the task to
     * WorldEdit.
     * 
     * @param loc the Location to mark as an objective.
     */
    protected void markObjective(Location loc) {
        String fileName = Util.randomChoice(BadSanta.CONFIG.OBJECTIVE_SKINS) + ".schematic";
        File schematicsDir = new File(BadSanta.PLUGIN.getDataFolder(), "schematics");
        File presentSchematicFile = new File(schematicsDir, fileName);
        try {
            pasteSchematic(presentSchematicFile, loc, new Vector(), false, false);
        } catch (Exception ex) {
            BadSanta.PLUGIN.getLogger().warning("Unable to paste objective marker: " + ex.getMessage());
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Paste a schematic to a specified location, with specified position
     * offset.
     * 
     * @param file the schematic file to load.
     * @param loc the location to paste the reference point.
     * @param offset the relative position adjustment to apply to the location.
     * @param copyAir if true, air in the schematic is written into the world;
     *        otherwise, the block at that location is unchanged.
     * @param copyEntities if true, paste entities from the schematic into the
     *        world.
     */
    @SuppressWarnings("deprecation")
    protected void pasteSchematic(File file, Location loc, Vector offset, boolean copyAir, boolean copyEntities)
    throws DataException, IOException, MaxChangedBlocksException {
        WorldEdit we = getWorldEdit();
        EditSession es = we.getEditSessionFactory().getEditSession(new BukkitWorld(loc.getWorld()), -1);

        // There doesn't seem to be any non-deprecated way to do this.
        CuboidClipboard cc = CuboidClipboard.loadSchematic(file);
        cc.rotate2D(90 * Util.randomInt(4));
        cc.paste(es, toWEVector(loc.clone().add(offset)), !copyAir, copyEntities);
    }

    // ------------------------------------------------------------------------
    /**
     * Convert a Bukkit Location into a WorldEdit Vector.
     * 
     * @param loc the location.
     * @return the Vector.
     */
    com.sk89q.worldedit.Vector toWEVector(Location loc) {
        return new com.sk89q.worldedit.Vector(loc.getX(), loc.getY(), loc.getZ());
    }

    // ------------------------------------------------------------------------
    /**
     * Return a reference to the WorldEdit plugin.
     *
     * @return a reference to the WorldEdit plugin.
     */
    protected WorldEdit getWorldEdit() {
        WorldEditPlugin plugin = (WorldEditPlugin) Bukkit.getPluginManager().getPlugin("WorldEdit");
        return plugin.getWorldEdit();
    }

    // ------------------------------------------------------------------------
    /**
     * Map from Block to Objective at that block.
     */
    protected HashMap<Block, Objective> _objectives = new HashMap<Block, Objective>();

} // class ObjectiveManager