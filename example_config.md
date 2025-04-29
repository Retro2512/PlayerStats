# Example PlayerStats Approved Stats Configuration

This file contains a sample configuration for the `approved-stats` section
of the PlayerStats `config.yml`. It includes standard stats, combined stats
(like ores and travel distance), and various fun/interesting metrics suitable
for a Survival Multiplayer server.

**Note:** Ensure correct YAML indentation (typically 2 spaces) when pasting
into your `config.yml`.

```yaml
approved-stats:
  # --- Standard Useful Stats ---
  play_time:
    display-name: "Play Time"
    statistic: PLAY_ONE_MINUTE
    type: UNTYPED
  player_kills:
    display-name: "Player Kills"
    statistic: PLAYER_KILLS
    type: UNTYPED
  mob_kills:
    display-name: "Mob Kills"
    statistic: MOB_KILLS
    type: UNTYPED
  deaths:
    display-name: "Deaths"
    statistic: DEATHS
    type: UNTYPED
  damage_dealt:
    display-name: "Damage Dealt"
    statistic: DAMAGE_DEALT
    type: UNTYPED
  damage_taken:
    display-name: "Damage Taken"
    statistic: DAMAGE_TAKEN
    type: UNTYPED

  # --- Total Counts (Requires Plugin Fix from Earlier) ---
  blocks_mined_any:
    display-name: "Blocks Mined (Total)"
    statistic: MINE_BLOCK
    type: BLOCK # No sub-statistic = total of all blocks
  items_used_any:
    display-name: "Items Used (Total)"
    statistic: USE_ITEM
    type: ITEM # No sub-statistic = total of all items

  # --- Combined Ore Stats ---
  # Note: These include *all* mined ores (Silk Touch + Fortune/Normal).
  # Subtracting Silk Touch is not possible with standard Bukkit stats via config alone.
  diamonds_mined:
    display-name: "Diamonds Mined"
    components:
      - statistic: MINE_BLOCK
        type: BLOCK
        sub-statistic: DIAMOND_ORE
      - statistic: MINE_BLOCK
        type: BLOCK
        sub-statistic: DEEPSLATE_DIAMOND_ORE
  emeralds_mined:
    display-name: "Emeralds Mined"
    components:
      - statistic: MINE_BLOCK
        type: BLOCK
        sub-statistic: EMERALD_ORE
      - statistic: MINE_BLOCK
        type: BLOCK
        sub-statistic: DEEPSLATE_EMERALD_ORE
  iron_mined:
    display-name: "Iron Mined"
    components:
      - statistic: MINE_BLOCK
        type: BLOCK
        sub-statistic: IRON_ORE
      - statistic: MINE_BLOCK
        type: BLOCK
        sub-statistic: DEEPSLATE_IRON_ORE
  gold_mined:
    display-name: "Gold Mined"
    components:
      - statistic: MINE_BLOCK
        type: BLOCK
        sub-statistic: GOLD_ORE
      - statistic: MINE_BLOCK
        type: BLOCK
        sub-statistic: DEEPSLATE_GOLD_ORE
  copper_mined:
    display-name: "Copper Mined"
    components:
      - statistic: MINE_BLOCK
        type: BLOCK
        sub-statistic: COPPER_ORE
      - statistic: MINE_BLOCK
        type: BLOCK
        sub-statistic: DEEPSLATE_COPPER_ORE
  lapis_mined:
    display-name: "Lapis Lazuli Mined"
    components:
      - statistic: MINE_BLOCK
        type: BLOCK
        sub-statistic: LAPIS_ORE
      - statistic: MINE_BLOCK
        type: BLOCK
        sub-statistic: DEEPSLATE_LAPIS_ORE
  redstone_mined:
    display-name: "Redstone Mined"
    components:
      - statistic: MINE_BLOCK
        type: BLOCK
        sub-statistic: REDSTONE_ORE
      - statistic: MINE_BLOCK
        type: BLOCK
        sub-statistic: DEEPSLATE_REDSTONE_ORE
  coal_mined:
    display-name: "Coal Mined"
    components:
      - statistic: MINE_BLOCK
        type: BLOCK
        sub-statistic: COAL_ORE
      - statistic: MINE_BLOCK
        type: BLOCK
        sub-statistic: DEEPSLATE_COAL_ORE
  nether_quartz_mined:
    display-name: "Nether Quartz Mined"
    components:
      - statistic: MINE_BLOCK
        type: BLOCK
        sub-statistic: NETHER_QUARTZ_ORE
  nether_gold_mined:
    display-name: "Nether Gold Mined"
    components:
      - statistic: MINE_BLOCK
        type: BLOCK
        sub-statistic: NETHER_GOLD_ORE
  ancient_debris_mined:
    display-name: "Ancient Debris Mined"
    components:
      - statistic: MINE_BLOCK
        type: BLOCK
        sub-statistic: ANCIENT_DEBRIS

  # --- Combined Travel Stats ---
  distance_walked: # Basic walking/sprinting
    display-name: "Distance Walked/Sprinted"
    components:
      - statistic: WALK_ONE_CM
        type: UNTYPED
      - statistic: SPRINT_ONE_CM
        type: UNTYPED
  distance_by_water: # Swimming + Boating
    display-name: "Distance by Water"
    components:
      - statistic: SWIM_ONE_CM
        type: UNTYPED
      - statistic: WALK_ON_WATER_ONE_CM # May include Frost Walker?
        type: UNTYPED
      - statistic: BOAT_ONE_CM
        type: UNTYPED
  distance_by_air: # Flying (Creative/Spectator) + Elytra
    display-name: "Distance by Air"
    components:
      - statistic: FLY_ONE_CM
        type: UNTYPED
      - statistic: AVIATE_ONE_CM
        type: UNTYPED
  distance_by_mount: # Horse, Pig, Strider
    display-name: "Distance by Mount"
    components:
      - statistic: HORSE_ONE_CM
        type: UNTYPED
      - statistic: PIG_ONE_CM
        type: UNTYPED
      - statistic: STRIDER_ONE_CM
        type: UNTYPED
  distance_by_rail:
    display-name: "Distance by Minecart"
    components:
      - statistic: MINECART_ONE_CM
        type: UNTYPED
  distance_total: # Sum of most common travel types
    display-name: "Total Distance Traveled"
    components:
      - statistic: WALK_ONE_CM
        type: UNTYPED
      - statistic: SPRINT_ONE_CM
        type: UNTYPED
      - statistic: SWIM_ONE_CM
        type: UNTYPED
      - statistic: FLY_ONE_CM
        type: UNTYPED
      - statistic: AVIATE_ONE_CM # Elytra
        type: UNTYPED
      - statistic: CROUCH_ONE_CM
        type: UNTYPED
      - statistic: BOAT_ONE_CM
        type: UNTYPED
      - statistic: HORSE_ONE_CM
        type: UNTYPED
      - statistic: MINECART_ONE_CM
        type: UNTYPED
      - statistic: PIG_ONE_CM
        type: UNTYPED
      - statistic: STRIDER_ONE_CM
        type: UNTYPED
      - statistic: WALK_ON_WATER_ONE_CM
        type: UNTYPED
      - statistic: CLIMB_ONE_CM # Ladders/Vines etc.
        type: UNTYPED

  # --- Fun Interaction Stats ---
  jumps:
    display-name: "Total Jumps"
    statistic: JUMP
    type: UNTYPED
  animals_bred:
    display-name: "Animals Bred"
    statistic: ANIMALS_BRED
    type: UNTYPED
  villager_trades:
    display-name: "Villager Trades Made"
    statistic: TRADED_WITH_VILLAGER
    type: UNTYPED
  talked_to_villagers:
    display-name: "Villagers Talked To"
    statistic: TALKED_TO_VILLAGER
    type: UNTYPED
  cake_eaten:
    display-name: "Cake Slices Eaten"
    statistic: EAT_CAKE_SLICE
    type: UNTYPED
  fish_caught:
    display-name: "Fish Caught"
    statistic: FISH_CAUGHT
    type: UNTYPED
  items_enchanted:
    display-name: "Items Enchanted"
    statistic: ITEM_ENCHANTED
    type: UNTYPED
  music_discs_played:
    display-name: "Music Discs Played"
    statistic: PLAY_RECORD
    type: UNTYPED
  noteblocks_played:
    display-name: "Noteblocks Played"
    statistic: NOTEBLOCK_PLAYED
    type: UNTYPED
  noteblocks_tuned:
    display-name: "Noteblocks Tuned"
    statistic: NOTEBLOCK_TUNED
    type: UNTYPED
  potions_brewed:
    display-name: "Brewing Stand Interactions" # Closest stat to actual brewing
    statistic: BREWINGSTAND_INTERACTION
    type: UNTYPED
  cauldrons_filled:
    display-name: "Cauldrons Filled"
    statistic: CAULDRON_FILLED
    type: UNTYPED
  water_taken_from_cauldron:
    display-name: "Water Taken (Cauldron)"
    statistic: CAULDRON_USED
    type: UNTYPED
  armor_cleaned:
    display-name: "Armor Cleaned (Cauldron)"
    statistic: ARMOR_CLEANED
    type: UNTYPED
  banners_cleaned:
    display-name: "Banners Cleaned (Cauldron)"
    statistic: BANNER_CLEANED
    type: UNTYPED
  shulker_boxes_opened:
    display-name: "Shulker Boxes Opened"
    statistic: OPEN_SHULKER_BOX
    type: UNTYPED
  chests_opened:
    display-name: "Chests Opened"
    statistic: OPEN_CHEST
    type: UNTYPED
  ender_chests_opened:
    display-name: "Ender Chests Opened"
    statistic: OPEN_ENDERCHEST
    type: UNTYPED
  hoppers_inspected:
    display-name: "Hoppers Inspected"
    statistic: INSPECT_HOPPER
    type: UNTYPED
  droppers_inspected:
    display-name: "Droppers Inspected"
    statistic: INSPECT_DROPPER
    type: UNTYPED
  dispensers_inspected:
    display-name: "Dispensers Inspected"
    statistic: INSPECT_DISPENSER
    type: UNTYPED
  beacons_interacted:
    display-name: "Beacon Interactions"
    statistic: BEACON_INTERACTION
    type: UNTYPED
  crafting_tables_used:
    display-name: "Crafting Table Interactions"
    statistic: CRAFTING_TABLE_INTERACTION
    type: UNTYPED
  furnaces_used:
    display-name: "Furnace Interactions"
    statistic: FURNACE_INTERACTION
    type: UNTYPED
  sleep_in_bed:
    display-name: "Times Slept in Bed"
    statistic: SLEEP_IN_BED
    type: UNTYPED
  time_since_rest:
    display-name: "Time Since Last Sleep"
    statistic: TIME_SINCE_REST
    type: UNTYPED
  damage_blocked_by_shield:
    display-name: "Damage Blocked (Shield)"
    statistic: DAMAGE_BLOCKED_BY_SHIELD
    type: UNTYPED
  damage_absorbed: # Golden Apples, Totems?
    display-name: "Damage Absorbed"
    statistic: DAMAGE_ABSORBED
    type: UNTYPED
  damage_resisted: # Resistance effect
    display-name: "Damage Resisted"
    statistic: DAMAGE_RESISTED
    type: UNTYPED
  raids_triggered:
    display-name: "Raids Triggered"
    statistic: RAID_TRIGGER
    type: UNTYPED
  raids_won:
    display-name: "Raids Won"
    statistic: RAID_WIN
    type: UNTYPED

  # --- Specific Mob Kills (Examples) ---
  ender_dragon_kills:
    display-name: "Ender Dragon Kills"
    statistic: KILL_ENTITY
    type: ENTITY
    sub-statistic: ENDER_DRAGON
  wither_kills:
    display-name: "Wither Kills"
    statistic: KILL_ENTITY
    type: ENTITY
    sub-statistic: WITHER
  warden_kills:
    display-name: "Warden Kills"
    statistic: KILL_ENTITY
    type: ENTITY
    sub-statistic: WARDEN
  elder_guardian_kills:
    display-name: "Elder Guardian Kills"
    statistic: KILL_ENTITY
    type: ENTITY
    sub-statistic: ELDER_GUARDIAN
  shulker_kills:
    display-name: "Shulker Kills"
    statistic: KILL_ENTITY
    type: ENTITY
    sub-statistic: SHULKER

  # --- Items Crafted (Examples) ---
  diamonds_crafted: # For crafting diamond blocks/tools/armor
    display-name: "Diamonds Crafted With"
    statistic: CRAFT_ITEM
    type: ITEM
    sub-statistic: DIAMOND
  netherite_ingots_crafted:
    display-name: "Netherite Ingots Crafted"
    statistic: CRAFT_ITEM
    type: ITEM
    sub-statistic: NETHERITE_INGOT

  # --- Items Used (Examples) ---
  elytra_used: # Specifically using elytra (AVIATE_ONE_CM covers distance)
     display-name: "Elytra Used (Distance)" # Name reflects the stat better
     statistic: AVIATE_ONE_CM
     type: UNTYPED # Using distance as proxy for usage
  tridents_thrown:
     display-name: "Tridents Thrown"
     statistic: USE_ITEM
     type: ITEM
     sub-statistic: TRIDENT
  ender_pearls_thrown:
     display-name: "Ender Pearls Thrown"
     statistic: USE_ITEM
     type: ITEM
     sub-statistic: ENDER_PEARL

# --- End of approved-stats section --- 