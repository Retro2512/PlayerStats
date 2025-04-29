# Minecraft Statistics for /top Command

This file organizes Minecraft statistics (base and derived) into categories corresponding to potential `/top` command arguments.

## `/top kills <Arg> ` if no arg specified, will  return total stat across everything

Stats related to dealing death, experiencing death, and kill/death ratios.

**Base Stats:**

*   `PLAYER_KILLS`: [Untyped] Number of other players killed.
*   `KILL_ENTITY`: [EntityType] Times a specific entity type was killed.
*   `MOB_KILLS`: [Untyped] Total number of mobs killed (deprecated in favor of `KILL_ENTITY`).
*   `DEATHS`: [Untyped] Number of times the player has died.
*   `ENTITY_KILLED_BY`: [EntityType] Times the player was killed by a specific entity type.

**Derived Stats:**

*   **Player K/D Ratio:** `(PLAYER_KILLS / DEATHS)`
*   **Hostile Mob K/D Ratio:** `(Sum of KILL_ENTITY[Type=Hostile]) / (Sum of ENTITY_KILLED_BY[Type=Hostile])`
*   **Deaths by Environment:** `(Sum of ENTITY_KILLED_BY[FALL, LAVA, DROWNING, etc.])` (Requires specific tracking)

---

## `/top combat <Arg>`  if no arg specified, will  return total stat across everything

Stats related to dealing and receiving damage, defense, and specific combat actions.

**Base Stats:**

*   `DAMAGE_DEALT`: [Untyped] Total damage dealt to entities.
*   `DAMAGE_TAKEN`: [Untyped] Total damage taken.
*   `DAMAGE_BLOCKED_BY_SHIELD`: [Untyped] Amount of damage blocked by a shield.
*   `DAMAGE_DEALT_ABSORBED`: [Untyped] Damage dealt that was absorbed (e.g., by Golden Apples).
*   `DAMAGE_ABSORBED`: [Untyped] Damage taken that was absorbed.
*   `DAMAGE_DEALT_RESISTED`: [Untyped] Damage dealt that was resisted (e.g., due to Resistance effect).
*   `DAMAGE_RESISTED`: [Untyped] Damage taken that was resisted.
*   `TARGET_HIT`: [Untyped] Number of times a target block was hit.

**Derived Stats:**

*   **Damage Efficiency:** `(DAMAGE_DEALT / DAMAGE_TAKEN)`
*   **Combat Damage Ratio:** `(DAMAGE_DEALT / (DAMAGE_DEALT + DAMAGE_TAKEN))`
*   **Shield Effectiveness:** `(DAMAGE_BLOCKED_BY_SHIELD / DAMAGE_TAKEN)`
*   **Absorption Effectiveness:** `(DAMAGE_ABSORBED / DAMAGE_TAKEN)`
*   **Resistance Effectiveness:** `(DAMAGE_RESISTED / DAMAGE_TAKEN)`
*   **Arrows Fired:** `(USE_ITEM[BOW] + USE_ITEM[CROSSBOW])` (Approximation)
*   **Times Struck by Lightning:** `(Requires Event Listener)`

---

## `/top travel`

Stats related to player movement distance and methods.

**Base Stats:**

*   `WALK_ONE_CM`: [Untyped] Distance walked (cm). Convert to blocks: `/ 100`. Convert to km: `/ 100000`.
*   `SPRINT_ONE_CM`: [Untyped] Distance sprinted (cm). Convert to blocks: `/ 100`. Convert to km: `/ 100000`.
*   `CROUCH_ONE_CM`: [Untyped] Distance crouched/snuck (cm). Convert to blocks: `/ 100`. Convert to km: `/ 100000`.
*   `SWIM_ONE_CM`: [Untyped] Distance swam (cm). Convert to blocks: `/ 100`. Convert to km: `/ 100000`.
*   `FALL_ONE_CM`: [Untyped] Distance fallen (cm). Convert to blocks: `/ 100`.
*   `CLIMB_ONE_CM`: [Untyped] Distance climbed (ladders, vines, etc.) (cm). Convert to blocks: `/ 100`.
*   `FLY_ONE_CM`: [Untyped] Distance flown (creative/other) (cm). Convert to blocks: `/ 100`. Convert to km: `/ 100000`.
*   `AVIATE_ONE_CM`: [Untyped] Distance flown with Elytra (cm). Convert to blocks: `/ 100`. Convert to km: `/ 100000`.
*   `BOAT_ONE_CM`: [Untyped] Distance travelled by boat (cm). Convert to blocks: `/ 100`. Convert to km: `/ 100000`.
*   `MINECART_ONE_CM`: [Untyped] Distance travelled by minecart (cm). Convert to blocks: `/ 100`. Convert to km: `/ 100000`.
*   `HORSE_ONE_CM`: [Untyped] Distance travelled by horse (cm). Convert to blocks: `/ 100`. Convert to km: `/ 100000`.
*   `PIG_ONE_CM`: [Untyped] Distance travelled by pig (cm). Convert to blocks: `/ 100`. Convert to km: `/ 100000`.
*   `STRIDER_ONE_CM`: [Untyped] Distance travelled by strider (cm). Convert to blocks: `/ 100`. Convert to km: `/ 100000`.
*   `WALK_ON_WATER_ONE_CM`: [Untyped] Distance walked on water (Frost Walker) (cm). Convert to blocks: `/ 100`. Convert to km: `/ 100000`.
*   `WALK_UNDER_WATER_ONE_CM`: [Untyped] Distance walked underwater (cm). Convert to blocks: `/ 100`. Convert to km: `/ 100000`.
*   `JUMP`: [Untyped] Number of times jumped.
*   `SNEAK_TIME`: [Untyped] Time spent sneaking (ticks). Convert to seconds: `/ 20`. Convert to minutes: `/ 1200`.

**Derived Stats:**

*   **Total Distance Traveled (km):** `(Sum of all _ONE_CM stats) / 100000`
*   **Total Distance Traveled (blocks):** `(Sum of all _ONE_CM stats) / 100`
*   **Primary Mode of Transport:** Whichever `_ONE_CM` stat is highest.
*   **Jumps per KM Walked:** `(JUMP / (WALK_ONE_CM / 100000))`
*   **Unique Biomes Visited:** `(Requires Location Tracking)`

---

## `/top mined`

Stats related to breaking general blocks (stone, wood, dirt, etc.). Use `/top ores` for valuable ores. Allows filtering by specific Material.

**Base Stats:**

*   `MINE_BLOCK`: [Material] Number of times a specific block type was mined.

**Derived Stats (Examples):**

*   **Stone Mined:** `(Sum of MINE_BLOCK[Stone, Cobblestone, Deepslate, Cobbled Deepslate, Andesite, Diorite, Granite, Tuff, Blackstone])`
*   **Wood Harvested:** `(Sum of MINE_BLOCK[Type=Any Log Material])`
*   **Dirt Displaced:** `(Sum of MINE_BLOCK[Dirt, Grass Block, Podzol, Mycelium, Coarse Dirt, Rooted Dirt, Mud])`

---

## `/top ores`

Derived stats focused specifically on mining valuable ores.

**Base Stats (Used for Derivations):**

*   `MINE_BLOCK`: [Material] (Used with ore Materials like `DIAMOND_ORE`, `DEEPSLATE_IRON_ORE`, etc.)

**Derived Stats:**

*   **Total Ores Mined:** `(Sum of MINE_BLOCK[Type=Any Ore Material])`
*   **Total Coal Mined:** `(MINE_BLOCK[COAL_ORE] + MINE_BLOCK[DEEPSLATE_COAL_ORE])`
*   **Total Copper Mined:** `(MINE_BLOCK[COPPER_ORE] + MINE_BLOCK[DEEPSLATE_COPPER_ORE])`
*   **Total Iron Mined:** `(MINE_BLOCK[IRON_ORE] + MINE_BLOCK[DEEPSLATE_IRON_ORE])`
*   **Total Gold Mined:** `(MINE_BLOCK[GOLD_ORE] + MINE_BLOCK[DEEPSLATE_GOLD_ORE])`
*   **Total Lapis Mined:** `(MINE_BLOCK[LAPIS_ORE] + MINE_BLOCK[DEEPSLATE_LAPIS_ORE])`
*   **Total Redstone Mined:** `(MINE_BLOCK[REDSTONE_ORE] + MINE_BLOCK[DEEPSLATE_REDSTONE_ORE])`
*   **Total Emerald Mined:** `(MINE_BLOCK[EMERALD_ORE] + MINE_BLOCK[DEEPSLATE_EMERALD_ORE])`
*   **Total Diamonds Mined:** `(MINE_BLOCK[DIAMOND_ORE] + MINE_BLOCK[DEEPSLATE_DIAMOND_ORE])`
*   **Total Ancient Debris Mined:** `MINE_BLOCK[ANCIENT_DEBRIS]`
*   **Valuables Mined Ratio:** `(Sum of MINE_BLOCK[Diamonds, Emeralds, Gold, Ancient Debris]) / (Total Ores Mined)`

---

## `/top craft`

Stats related to crafting items. Allows filtering by specific Material.

**Base Stats:**

*   `CRAFT_ITEM`: [Material] Number of times a specific item was crafted.

**Derived Stats:**

*   **Total Items Crafted:** `(Sum of CRAFT_ITEM[Type=Any Item Material])`
*   **Tools Crafted:** `(Sum of CRAFT_ITEM[Type=Tools])`
*   **Armor Crafted:** `(Sum of CRAFT_ITEM[Type=Armor])`

---

## `/top interactions`

Stats related to using items, interacting with blocks/entities, farming, fishing, raids, and other general actions.

**Base Stats:**

*   `USE_ITEM`: [Material] Number of times a specific item/block was used (placed, consumed, activated).
*   `BREAK_ITEM`: [Material] Number of times a specific item type broke from use.
*   `PICKUP`: [Material] Number of times a specific item type was picked up.
*   `DROP`: [Material] Number of times a specific item type was dropped.
*   `DROP_COUNT`: [Untyped] Total number of items dropped (any type).
*   `CHEST_OPENED`: [Untyped] Number of times a chest was opened.
*   `TRAPPED_CHEST_TRIGGERED`: [Untyped] Number of times a trapped chest was triggered.
*   `ENDERCHEST_OPENED`: [Untyped] Number of times an ender chest was opened.
*   `SHULKER_BOX_OPENED`: [Untyped] Number of times a shulker box was opened.
*   `OPEN_BARREL`: [Untyped] Number of times a barrel was opened.
*   `ITEM_ENCHANTED`: [Untyped] Number of items enchanted.
*   `FURNACE_INTERACTION`: [Untyped] Number of times a furnace was interacted with.
*   `CRAFTING_TABLE_INTERACTION`: [Untyped] Number of times a crafting table was interacted with.
*   `SMITHING_TABLE_INTERACTION` / `INTERACT_WITH_SMITHING_TABLE`: [Untyped] Number of times interacted with.
*   `LOOM_INTERACTION` / `INTERACT_WITH_LOOM`: [Untyped] Number of times interacted with.
*   `CARTOGRAPHY_TABLE_INTERACTION` / `INTERACT_WITH_CARTOGRAPHY_TABLE`: [Untyped] Number of times interacted with.
*   `STONECUTTER_INTERACTION` / `INTERACT_WITH_STONECUTTER`: [Untyped] Number of times interacted with.
*   `GRINDSTONE_INTERACTION` / `INTERACT_WITH_GRINDSTONE`: [Untyped] Number of times interacted with.
*   `ANVIL_INTERACTION` / `INTERACT_WITH_ANVIL`: [Untyped] Number of times interacted with.
*   `LECTERN_INTERACTION` / `INTERACT_WITH_LECTERN`: [Untyped] Number of times interacted with.
*   `BLAST_FURNACE_INTERACTION` / `INTERACT_WITH_BLAST_FURNACE`: [Untyped] Number of times interacted with.
*   `SMOKER_INTERACTION` / `INTERACT_WITH_SMOKER`: [Untyped] Number of times interacted with.
*   `CAMPFIRE_INTERACTION` / `INTERACT_WITH_CAMPFIRE`: [Untyped] Number of times interacted with.
*   `BEACON_INTERACTION`: [Untyped] Number of times a beacon was interacted with.
*   `BREWINGSTAND_INTERACTION`: [Untyped] Number of times a brewing stand was interacted with.
*   `DROPPER_INSPECTED`: [Untyped] Number of times a dropper was inspected (opened).
*   `HOPPER_INSPECTED`: [Untyped] Number of times a hopper was inspected (opened).
*   `DISPENSER_INSPECTED`: [Untyped] Number of times a dispenser was inspected (opened).
*   `BELL_RING`: [Untyped] Number of times a bell was rung.
*   `FLOWER_POTTED`: [Untyped] Number of flowers potted.
*   `RECORD_PLAYED`: [Untyped] Number of music discs played in a jukebox.
*   `NOTEBLOCK_PLAYED`: [Untyped] Number of times a noteblock was played (hit).
*   `NOTEBLOCK_TUNED`: [Untyped] Number of times a noteblock was tuned (interacted with).
*   `CAKE_SLICES_EATEN`: [Untyped] Number of cake slices eaten.
*   `CAULDRON_FILLED`: [Untyped] Number of times a cauldron was filled.
*   `CAULDRON_USED`: [Untyped] Number of times a cauldron was used (taking water/potion out).
*   `ARMOR_CLEANED`: [Untyped] Number of leather armor pieces cleaned in a cauldron.
*   `BANNER_CLEANED`: [Untyped] Number of banners cleaned in a cauldron.
*   `CLEAN_SHULKER_BOX`: [Untyped] Number of times a shulker box was cleaned.
*   `TALKED_TO_VILLAGER`: [Untyped] Number of times talked to a villager.
*   `TRADED_WITH_VILLAGER`: [Untyped] Number of times traded with a villager.
*   `RAID_WIN`: [Untyped] Number of raids won.
*   `RAID_TRIGGER`: [Untyped] Number of raids triggered.
*   `ANIMALS_BRED`: [Untyped] Number of animals bred.
*   `FISH_CAUGHT`: [Untyped] Number of fish caught.
*   `SLEEP_IN_BED`: [Untyped] Number of times slept in a bed.

**Derived Stats:**

*   **Total Blocks Placed:** `(Sum of USE_ITEM[Type=Placeable Block Material])`
*   **Total Items Used:** `(Sum of USE_ITEM[Type=Any Item Material])`
*   **Total Items Broken:** `(Sum of BREAK_ITEM[Type=Any Item Material])`
*   **Tools Broken:** `(Sum of BREAK_ITEM[Type=Tools])`
*   **Armor Broken:** `(Sum of BREAK_ITEM[Type=Armor])`
*   **Container Interactions:** `(CHEST_OPENED + ENDERCHEST_OPENED + SHULKER_BOX_OPENED + OPEN_BARREL)`
*   **Workstation Interactions:** `(Sum of _INTERACTION stats for Crafting Table, Furnace, Anvil, etc.)`
*   **Total Food Consumed:** `(Sum of USE_ITEM[Type=Edible Items])`
*   **Crops Harvested:** `(Sum of MINE_BLOCK[Type=Mature Crop Blocks])` (Approximation)
*   **Animals Bred per Hour:** `(ANIMALS_BRED / (PLAY_ONE_MINUTE / 72000))`
*   **Cake Slices Eaten per Hour:** `(CAKE_SLICES_EATEN / (PLAY_ONE_MINUTE / 72000))`
*   **Fish Caught per Hour:** `(FISH_CAUGHT / (PLAY_ONE_MINUTE / 72000))`

---

## `/top playtime`

Stats related to time spent playing, session length, and survival time.

**Base Stats:**

*   `PLAY_ONE_MINUTE`: [Untyped] Time played (ticks). Note: Name is misleading. Convert to seconds: `/ 20`. Convert to minutes: `/ 1200`. Convert to hours: `/ 72000`.
*   `TOTAL_WORLD_TIME`: [Untyped] Total world time (ticks). Convert to hours: `/ 72000`.
*   `TIME_SINCE_DEATH`: [Untyped] Time elapsed (ticks) since last death. Convert to hours: `/ 72000`.
*   `TIME_SINCE_REST`: [Untyped] Time elapsed (ticks) since last sleeping (phantom timer). Convert to hours: `/ 72000`.
*   `LEAVE_GAME`: [Untyped] Number of times the player has left the game.

**Derived Stats:**

*   **Time Played (Hours):** `(PLAY_ONE_MINUTE / 72000)`
*   **Average Session Length (Hours):** `(PLAY_ONE_MINUTE / 72000) / LEAVE_GAME`
*   **Time Since Last Advancement:** `(Requires Event Listener)` (Convert ticks to hours/days)
*   **Advancements Earned:** `(Requires Event Listener)` or reading player data.

---

**Note:** For stats requiring `[Material]` or `[EntityType]`, the base statistic without a qualifier often represents the total across all relevant types (e.g., `DROP` for any item drop vs `DROP[DIAMOND]` for specific diamond drops). Derived stats often involve summing these specific types. Implementation of derived stats requires custom plugin logic.