package nu.nerd.badsanta;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Effect;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.SkullType;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Villager.Profession;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

// ----------------------------------------------------------------------------
/**
 * BadSanta plugin, command handling and event handler.
 */
public class BadSanta extends JavaPlugin implements Listener {
    /**
     * Configuration wrapper instance.
     */
    public static Configuration CONFIG = new Configuration();

    /**
     * This plugin, accessible as, effectively, a singleton.
     */
    public static BadSanta PLUGIN;

    // ------------------------------------------------------------------------
    /**
     * @see org.bukkit.plugin.java.JavaPlugin#onEnable()
     */
    @Override
    public void onEnable() {
        PLUGIN = this;
        MOB_META = new FixedMetadataValue(this, "BS_Mob");

        saveDefaultConfig();
        CONFIG.reload();

        getServer().getPluginManager().registerEvents(this, this);

        // Every tick, do particle effects for objectives.
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                _objectiveManager.tickAll();
            }
        }, 1, 1);
    }

    // ------------------------------------------------------------------------
    /**
     * @see org.bukkit.plugin.java.JavaPlugin#onDisable()
     */
    @Override
    public void onDisable() {
        Bukkit.getScheduler().cancelTasks(this);
        _objectiveManager.removeAll();
    }

    // ------------------------------------------------------------------------
    /**
     * @see org.bukkit.plugin.java.JavaPlugin#onCommand(org.bukkit.command.CommandSender,
     *      org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase(getName())) {
            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                CONFIG.reload();
                sender.sendMessage(ChatColor.GOLD + getName() + " configuration reloaded.");
                return true;
            }
        }

        sender.sendMessage(ChatColor.RED + "Invalid command syntax.");
        return true;
    }

    // ------------------------------------------------------------------------
    /**
     * In the configured World, replace all hostile natural spawns with zombies,
     * dressed as Santa or an elf.
     *
     * Spawner-spawned mobs are not affected in any way.
     */
    @EventHandler(ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!CONFIG.isAffectedWorld(event)) {
            return;
        }

        if (event.getSpawnReason() == SpawnReason.NATURAL && isEligibleHostileMob(event.getEntityType())) {
            Entity originalMob = event.getEntity();
            Location loc = originalMob.getLocation();
            originalMob.remove();
            spawnSpecialMob(loc);
        }
    }

    // ------------------------------------------------------------------------
    /**
     * If a player breaks a pot of gold, do treasure drops and stop that pot's
     * particle effects.
     */
    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Location loc = block.getLocation();
        if (!CONFIG.isAffectedWorld(loc.getWorld())) {
            return;
        }

        Objective objective = _objectiveManager.getObjective(block);
        if (objective != null) {
            // Prevent the objective break from being logged by LogBlock.
            event.setCancelled(true);

            objective.vaporise();
            objective.spawnLoot(event.getPlayer());
            _objectiveManager.removeObjective(objective);
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Tag special mobs hurt by players.
     *
     * Only those mobs hurt recently by players will have special drops.
     */
    @EventHandler(ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!CONFIG.isAffectedWorld(event)) {
            return;
        }

        Entity entity = event.getEntity();
        if (entity instanceof LivingEntity && !(entity instanceof ArmorStand)) {
            Location loc = entity.getLocation();
            loc.getWorld().spigot().playEffect(loc, Effect.TILE_DUST, 214, 0, 0.5f, 0.5f, 0.5f, 0, 20, 32);
        }

        if (isSpecialMob(entity)) {
            int lootingLevel = 0;
            boolean isPlayerAttack = false;
            if (event.getDamager() instanceof Player) {
                isPlayerAttack = true;
                Player player = (Player) event.getDamager();
                lootingLevel = player.getEquipment().getItemInMainHand().getEnchantmentLevel(Enchantment.LOOT_BONUS_MOBS);
            } else if (event.getDamager() instanceof Projectile) {
                Projectile projectile = (Projectile) event.getDamager();
                if (projectile.getShooter() instanceof Player) {
                    isPlayerAttack = true;
                }
            }

            // Tag mobs hurt by players with the damage time stamp.
            if (isPlayerAttack) {
                entity.setMetadata(PLAYER_DAMAGE_TIME_KEY, new FixedMetadataValue(this, new Long(entity.getWorld().getFullTime())));
                entity.setMetadata(PLAYER_LOOTING_LEVEL_KEY, new FixedMetadataValue(this, lootingLevel));
            }
        }
    } // onEntityDamageByEntity

    // ------------------------------------------------------------------------
    /**
     * On mob death, do special drops if a player hurt the mob recently.
     */
    @EventHandler(ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!CONFIG.isAffectedWorld(event)) {
            return;
        }

        Entity entity = event.getEntity();
        if (isSpecialMob(entity)) {
            if (CONFIG.DEBUG_DEATH) {
                getLogger().info("Mob died at " + Util.formatLocation(entity.getLocation()));
            }

            int lootingLevel = getLootingLevelMeta(entity);
            boolean specialDrops = false;
            Long damageTime = getPlayerDamageTime(entity);
            if (damageTime != null) {
                Location loc = entity.getLocation();
                if (loc.getWorld().getFullTime() - damageTime < PLAYER_DAMAGE_TICKS) {
                    specialDrops = true;
                }
            }

            doCustomDrops((Zombie) entity, event.getDrops(), specialDrops, lootingLevel);
        }
    } // onEntityDeath

    // ------------------------------------------------------------------------
    /**
     * Spawn a special mob at the specified location.
     *
     * @param loc the location.
     */
    protected Zombie spawnSpecialMob(Location loc) {
        if (CONFIG.DEBUG_SPAWN) {
            getLogger().info("Spawned mob at " + Util.formatLocation(loc));
        }

        Zombie mob = (Zombie) loc.getWorld().spawnEntity(loc, EntityType.ZOMBIE);
        boolean isSanta = Math.random() < CONFIG.MOB_SANTA_CHANCE;

        // Don't allow players to steal equipment by throwing items.
        mob.setCanPickupItems(false);
        mob.getEquipment().clear();
        mob.setMetadata(MOB_KEY, MOB_META);
        mob.setMaxHealth(CONFIG.MOB_SANTA_HEALTH);
        mob.setHealth(CONFIG.MOB_SANTA_HEALTH);
        mob.setVillagerProfession(Profession.NORMAL);
        mob.setBaby(!isSanta && Math.random() < CONFIG.MOB_ELF_BABY_CHANCE);

        if (CONFIG.ARMOUR_WORN) {
            ItemStack chestPlate = new ItemStack(Material.LEATHER_CHESTPLATE, 1, (short) Util.random(1, 80));
            ItemStack leggings = new ItemStack(Material.LEATHER_LEGGINGS, 1, (short) Util.random(1, 75));
            ItemStack boots = new ItemStack(Material.LEATHER_BOOTS, 1, (short) Util.random(1, 65));
            mob.getEquipment().setChestplate(dyeLeatherArmour(chestPlate, isSanta ? Color.RED : Color.LIME));
            mob.getEquipment().setLeggings(dyeLeatherArmour(leggings, isSanta ? Color.RED : Color.LIME));
            mob.getEquipment().setBoots(dyeLeatherArmour(boots, isSanta ? Color.BLACK : Color.LIME));

            // The helmet drop chance on Samta's head isn't working as expected.
            // Same with armour. Drop equipment in doCustomDrops().
            mob.getEquipment().setHelmetDropChance(0);
            mob.getEquipment().setChestplateDropChance(0);
            mob.getEquipment().setLeggingsDropChance(0);
            mob.getEquipment().setBootsDropChance(0);

            if (isSanta) {
                ItemStack head = new ItemStack(Material.SKULL_ITEM, 1, (short) SkullType.PLAYER.ordinal());
                SkullMeta meta = (SkullMeta) head.getItemMeta();
                meta.setOwner(CONFIG.MOB_SANTA_HEAD_SKIN);
                head.setItemMeta(meta);
                mob.getEquipment().setHelmet(head);
            } else {
                ItemStack helmet = new ItemStack(Material.LEATHER_HELMET, 1, (short) Util.random(1, 55));
                mob.getEquipment().setHelmet(dyeLeatherArmour(helmet, Color.LIME));
            }
        }
        return mob;
    } // spawnSpecialMob

    // ------------------------------------------------------------------------
    /**
     * Return the world time when a player damaged the specified entity, if
     * stored as a PLAYER_DAMAGE_TIME_KEY metadata value, or null if that didn't
     * happen.
     *
     * @param entity the entity (mob).
     * @return the damage time stamp as Long, or null.
     */
    protected Long getPlayerDamageTime(Entity entity) {
        List<MetadataValue> playerDamageTime = entity.getMetadata(PLAYER_DAMAGE_TIME_KEY);
        if (playerDamageTime.size() > 0) {
            MetadataValue value = playerDamageTime.get(0);
            if (value.value() instanceof Long) {
                return (Long) value.value();
            }
        }
        return null;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the looting level metadata value from a special mob.
     *
     * This metadata is added when a player damages a special mob. It is the
     * level of the Looting enchant on the weapon that did the damage, or 0 if
     * there was no such enchant.
     *
     * @param entity the damaged entity.
     * @return the level of the Looting enchant, or 0 if not so enchanted.
     */
    protected int getLootingLevelMeta(Entity entity) {
        List<MetadataValue> lootingLevel = entity.getMetadata(PLAYER_LOOTING_LEVEL_KEY);
        if (lootingLevel.size() > 0) {
            return lootingLevel.get(0).asInt();
        }
        return 0;
    }

    // ------------------------------------------------------------------------
    /**
     * Add custom drops.
     *
     * @param mob the dropping mob.
     * @param drops the list of drops for the EntityDeathEvent.
     * @param special if true, low-probability, special drops are possible;
     *        otherwise, the drops are custom but mundane.
     * @param lootingLevel the level of looting on the weapon ([0,3]).
     */
    protected void doCustomDrops(Zombie mob, List<ItemStack> drops, boolean special, int lootingLevel) {
        for (Drop drop : CONFIG.DROPS_REGULAR) {
            if (Math.random() < drop.getDropChance() * adjustedChance(lootingLevel)) {
                drops.add(drop.generate());
            }
        }

        if (special) {
            for (Drop drop : CONFIG.DROPS_SPECIAL) {
                if (Math.random() < drop.getDropChance() * adjustedChance(lootingLevel)) {
                    drops.add(drop.generate());
                }
            }

            if (Math.random() < CONFIG.DROPS_FIREWORK_CHANCE * adjustedChance(lootingLevel)) {
                ItemStack firework = new ItemStack(Material.FIREWORK);
                FireworkMeta meta = (FireworkMeta) firework.getItemMeta();
                meta.setDisplayName(ChatColor.DARK_RED + "Christmas Cracker");
                meta.setPower(Util.randomInt(3));
                meta.addEffect(randomFireworkEffect(false));
                firework.setItemMeta(meta);
                drops.add(firework);
            }

            // Equipment drops below about 10% don't seem to happen at all with
            // vanilla code.
            ItemStack headDrop = mob.getEquipment().getHelmet();
            if (headDrop != null) {
                if (headDrop.getType() == Material.SKULL_ITEM && Math.random() < CONFIG.MOB_SANTA_HEAD_CHANCE * adjustedChance(lootingLevel)) {
                    drops.add(headDrop);
                } else if (Math.random() < CONFIG.ARMOUR_DROP_CHANCE * adjustedChance(lootingLevel)) {
                    drops.add(headDrop);
                }
            }
            doArmourDrop(drops, mob.getEquipment().getChestplate(), lootingLevel);
            doArmourDrop(drops, mob.getEquipment().getLeggings(), lootingLevel);
            doArmourDrop(drops, mob.getEquipment().getBoots(), lootingLevel);

            // Spawn objective? Can drop from Santa or his elves.
            if (Math.random() < CONFIG.OBJECTIVE_CHANCE) {
                Objective objective = _objectiveManager.spawnObjective(mob.getLocation());
                if (objective != null) {
                    getLogger().info("Spawned objective at " + Util.formatLocation(objective.getLocation()) +
                                     " alive for " + objective.getLifeInTicks() + " ticks.");
                    ItemStack map = new ItemStack(Material.PAPER, 1);
                    ItemMeta meta = map.getItemMeta();
                    meta.setDisplayName(CONFIG.OBJECTIVE_MAP_NAME);

                    Location objectiveLoc = objective.getLocation();
                    String formattedLoc = objectiveLoc.getBlockX() + ", " +
                                          objectiveLoc.getBlockY() + ", " +
                                          objectiveLoc.getBlockZ();
                    meta.setLore(Arrays.asList(MessageFormat.format(CONFIG.OBJECTIVE_MAP_LORE, formattedLoc).split("\\|")));
                    map.setItemMeta(meta);
                    drops.add(map);
                }
            }
        }
    } // doCustomDrops

    // ------------------------------------------------------------------------
    /**
     * Add an armour drop.
     * 
     * @param drops collection of drops so far.
     * @param armour armour item.
     * @param lootingLevel player weapon looting level.
     */
    protected void doArmourDrop(List<ItemStack> drops, ItemStack armour, int lootingLevel) {
        if (armour != null && Math.random() < CONFIG.ARMOUR_DROP_CHANCE * adjustedChance(lootingLevel)) {
            drops.add(armour);
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Return multiplicative factor to apply to the base drop chance according
     * to a given looting level.
     *
     * The drop chance compounds by 20% per looting level.
     *
     * @param lootingLevel the looting level of the weapon.
     * @return a factor to be multiplied by the base drop chance to compute the
     *         actual drop chance.
     */
    protected double adjustedChance(int lootingLevel) {
        return Math.pow(1.2, lootingLevel);
    }

    // ------------------------------------------------------------------------
    /**
     * Return true if the specified entity type is that of a hostile mob that is
     * eligible to be replaced with a special mob.
     *
     * @param type the entity's type.
     * @return true if the specified entity type is that of a hostile mob that
     *         is eligible to be replaced with a special mob.
     */
    protected boolean isEligibleHostileMob(EntityType type) {
        return type == EntityType.CREEPER ||
               type == EntityType.SPIDER ||
               type == EntityType.SKELETON ||
               type == EntityType.ZOMBIE ||
               type == EntityType.ENDERMAN ||
               type == EntityType.WITCH;
    }

    // ------------------------------------------------------------------------
    /**
     * Return true if the specified mob is a special mob spawned by the plugin.
     *
     * @param mob the mob.
     * @return true if the specified mob is a special mob spawned by the plugin.
     */
    protected boolean isSpecialMob(Entity mob) {
        return mob.hasMetadata(MOB_KEY);
    }

    // ------------------------------------------------------------------------
    /**
     * Dye leather armour.
     *
     * @param armour the armour.
     * @param colour the colour to dye it.
     * @return the armour parameter.
     */
    protected ItemStack dyeLeatherArmour(ItemStack armour, Color colour) {
        LeatherArmorMeta meta = (LeatherArmorMeta) armour.getItemMeta();
        meta.setColor(colour);
        armour.setItemMeta(meta);
        return armour;
    }

    // ------------------------------------------------------------------------
    /**
     * Return a random firework effect.
     * 
     * @param boolean allowCreeperType if true, creeper shaped firework
     *        explosion types are allowed.
     * @return a FireworkEffect instance.
     */
    protected FireworkEffect randomFireworkEffect(boolean allowCreeperType) {
        FireworkEffect.Builder builder = FireworkEffect.builder();
        if (Math.random() < 0.3) {
            builder.withFlicker();
        }
        if (Math.random() < 0.3) {
            builder.withTrail();
        }

        final FireworkEffect.Type[] TYPES = allowCreeperType ? FireworkEffect.Type.values()
                                                             : NON_CREEPER_FIREWORK_TYPES;
        builder.with(TYPES[Util.random(0, TYPES.length - 1)]);

        final int primaryColors = Util.random(1, 4);
        for (int i = 0; i < primaryColors; ++i) {
            builder.withColor(Color.fromRGB(Util.randomInt(256), Util.randomInt(256), Util.randomInt(256)));
        }

        final int fadeColors = Util.random(1, 4);
        for (int i = 0; i < fadeColors; ++i) {
            builder.withFade(Color.fromRGB(Util.randomInt(256), Util.randomInt(256), Util.randomInt(256)));
        }

        return builder.build();
    }

    // ------------------------------------------------------------------------
    /**
     * Array of all firework types except the creeper-head-shaped type.
     */
    protected static FireworkEffect.Type NON_CREEPER_FIREWORK_TYPES[];
    static {
        NON_CREEPER_FIREWORK_TYPES = new FireworkEffect.Type[FireworkEffect.Type.values().length - 1];
        for (int i = 0; i < NON_CREEPER_FIREWORK_TYPES.length; ++i) {
            FireworkEffect.Type type = FireworkEffect.Type.values()[i];
            if (type != FireworkEffect.Type.CREEPER) {
                NON_CREEPER_FIREWORK_TYPES[i] = type;
            }
        }
    }

    /**
     * Metadata name (key) used to tag affected mobs.
     */
    protected static final String MOB_KEY = "BS_Mob";

    /**
     * Shared metadata value for all affected mobs.
     */
    protected static FixedMetadataValue MOB_META;

    /**
     * Metadata name used for metadata stored on mobs to record last damage time
     * (Long) by a player.
     */
    protected static final String PLAYER_DAMAGE_TIME_KEY = "BS_PlayerDamageTime";

    /**
     * Metadata name used for metadata stored on mobs to record looting
     * enchantment level of Looting weapon used by a player.
     */
    protected static final String PLAYER_LOOTING_LEVEL_KEY = "BS_PlayerLootingLevel";

    /**
     * Time in ticks (1/20ths of a second) for which player attack damage
     * "sticks" to a mob. The time between the last player damage on a mob and
     * its death must be less than this for it to drop special stuff.
     */
    protected static final int PLAYER_DAMAGE_TICKS = 100;

    /**
     * Manages objectives.
     */
    protected ObjectiveManager _objectiveManager = new ObjectiveManager();

    /**
     * Random number generator.
     */
    protected static Random _random = new Random();
} // class BadSanta