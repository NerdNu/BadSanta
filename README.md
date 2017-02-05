BadSanta
========
A Bukkit plugin that replaces hostile mob spawns with zombie Santas and elves.


Features
--------
 * The plugin affects hostile mobs in a single configured world (by default,
   the overworld) only. Spawner mobs are not modified.
 * Affected hostile mobs are replaced with zombies dressed in leather armour
   (or skinned with a server resource pack).
 * For custom mobs to drop special drops when they die, they must have been
   recently hurt by a player.
 * A small percentage of Santas drop a piece of paper named a "Gift Card",
   which has as its lore the coordinates of a wrapped gift (the objective).
 * A wrapped gift is a skull textured as a Christmas present that is spawned
   into the affected world (only) for a limited period of time within a range of
   distances from the Santa's death point.
 * The objective is marked with a rainbow of coloured particles. If the
   player breaks it or moves to within 5 blocks of it, then it disappears and
   drops configurable loot.


Commands
--------
 * `/badsanta reload` - Reload the plugin configuration.


General Configuration
---------------------
In the discussion that follows, the term *"special mob"* refers to any replaced
hostile mob (Santa or his elves).

| Setting | Description |
| :--- | :--- |
| `debug.config` | If true, log the configuration on reload. |
| `debug.death` | If true, log special mob deaths. |
| `debug.spawn` | If true, log special mob spawns. |
| `world.name` | The world where mobs are affected. |
| `world.border` | World border radius in the affected world (square world assumed). |
| `mob.santa.chance` | Chance of replacing a hostile mob in the configured world with a Santa, [0.0,1.0]. |
| `mob.santa.health` | Santa health. |
| `mob.santa.head.skin` | Player name of the Santa head skin. |
| `mob.santa.head.chance` | Chance of dropping a Santa head. |
| `mob.elf.health` | Elf health. |
| `mob.elf.baby_chance` | Chance that an elf is a baby zombie. |
| `armour.worn` | True if the mobs wear coloured leather armour; otherwise a server texture-pack should be configured. |
| `armour.drop_chance` | Base drop chance for armour pieces, [0.0,1.0], modified by looting. |
| `objective.chance` | Chance of an objective spawning upon mob death. |
| `objective.max` | Maximum number of objectives allowed in the world simultaneously. |
| `objective.range.min` | Minimum distance from mob death for an objective to spawn. |
| `objective.range.max` | Maximum distance from mob death for an objective to spawn. |
| `objective.extra_ticks` | Extra ticks of time given to the player to find the pot. |
| `objective.min_player_speed` | The minimum expected movement speed of players, based upon which the duration of existence of an objective will be calculated. |
| `objective.skins` | A list of all possible player names of skins applied to the skull block used to mark an objective. Due to a bug that is apparently caused by a Spigot optimisation, setting the skull block through the Bukkit API doesn't apply the skin without a relog. As a work-around, `BadSanta` uses `WorldEdit` to paste `plugins/BadSanta/schematics/<skin>.schematic` into the world, where `<skin>` is one of the listed player names. The corresponding schematic file should contain a single block: the specified player head. `WorldEdit` includes NMS code that fixes the skin issue. |
| `objective.map.name` | The custom name on maps to objectives. |
| `objective.map.lore` | Lore on maps to objectiveswhich will be broken into multiple lines at the pipe ('|') character, after message substitution of {0} as the objective coordinates. |
| `objective.particle.radius` | Radius of the particle cloud around the objective. |
| `objective.particle.count` | Number of particles in the particle cloud around the objective. |


Drop Configuration
------------------
There are three categories of drops: `regular`, `special` and `objective`:

 * `regular` drops can be dropped by any special mob, no matter how it dies.
 * `special` drops only drop when a special mob has been damaged by a player
   in the 5 seconds prior to its death.
 * `objective` drops are dropped when a player reaches an objective (wrapped
   present) before it despawns.

More than one kind of drop in the `regular` and `special` categories can
drop for a given mob death. For example, in the `regular` category, 
`chocolates` and `gingerbread` defined in the default configuration have
to chance to both drop simultaneously for a single mob death.

In the `objective` category, only a single drop will be selected from all
options in the configuration. It is not possible for an objective to drop two
drops with different identifiers in the configuration.

In the following table, `<category>` is one of the three aforementioned
category names. The string `<id>` is some arbitrary name used to identify
a drop in the configuration file, e.g. `chocolates`.

| Setting | Description |
| :--- | :--- |
| `drops.<category>.<id>.item` | The Bukkit-serialisation encoded item to drop. |
| `drops.<category>.<id>.min` | The minimum number of items to drop (defaults to 1 if unspecified). |
| `drops.<category>.<id>.max` | The maximum number of items to drop (defaults to 1 if unspecified). |
| `drops.<category>.<id>.chance` | The probability, in the range [0.0,1.0], of the [min,max] items of the specified type being dropped. |


Permissions
-----------
 * `badsanta.admin` - Permission to administer the plugin (run `/badsanta reload`).
