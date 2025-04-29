# PlayerStats Plugin Enhancement Instructions

## User Requirements

1.  **Update Compatibility:** Update the plugin to be compatible with Purpur MC 1.21.4.
2.  **New `/top` Command:**
    *   Syntax: `/top <aliasedstatname>`
    *   Displays the top 10 players for a specific statistic.
    *   Only works for "approved" statistics defined in configuration.
    *   Uses user-friendly "aliases" for statistic names (also defined in config).
    *   Appends the command sender's rank and statistic value at the bottom of the top 10 list.
    *   Messages should not have any plugin name prefix.
    *   **Excludes players listed in `excluded_players.yml` (managed by `/statisticexclude` command).**
3.  **Approved Stats Management:**
    *   Define a configuration section for approved stats, mapping aliases to internal statistic names (and potentially material/entity types).
    *   Include some basic, vanilla SMP-friendly stats by default.
4.  **New Admin Command:**
    *   Syntax: `/statadmin addapproved <alias> <internal_stat_name> [material_or_entity_type]` (subject to refinement based on stat system)
    *   Requires admin permissions (e.g., `playerstats.admin`).
    *   Adds a statistic and its alias to the approved list in the configuration.
5.  **Remove Prefixes:** Remove all plugin name prefixes from user-facing messages throughout the plugin.
6.  **Maintain Compatibility:** Avoid breaking changes to existing configuration or data storage where possible. Ensure compatibility with older versions' data if feasible.
7.  **Code Quality:**
    *   Understand existing code before modifying.
    *   Fix root causes of errors, no hacks.
    *   Update dependencies/imports as needed.

## Project File Tree (Initial - Placeholder)

```
PlayerStats
├── .idea/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── artemis/
│       │           └── the/
│       │               └── gr8/
│       │                   └── playerstats/
│       │                       ├── api/
│       │                       │   └── enums/
│       │                       ├── core/
│       │                       │   ├── commands/
│       │                       │   ├── config/
│       │                       │   ├── enums/
│       │                       │   ├── listeners/
│       │                       │   ├── msg/
│       │                       │   │   ├── components/
│       │                       │   │   └── msgutils/
│       │                       │   ├── multithreading/
│       │                       │   ├── sharing/
│       │                       │   ├── statistic/
│       │                       │   └── utils/
│       │                       └── PlayerStats.java  # Assuming main class location
│       └── resources/
│           ├── images/
│           └── plugin.yml        # Assuming standard location
│           └── config.yml        # Assuming standard location
├── target/
├── dependency-reduced-pom.xml
├── LICENSE
├── PlayerStats.iml
├── pom.xml
└── README.md
└── INSTRUCTIONS.md # This file
```
*(Note: Tree will be updated as structure is confirmed)*

## Roadmap / Todo

*   [X] **Phase 1: Setup & Analysis**
    *   [x] Create `INSTRUCTIONS.md`.
    *   [x] Update `pom.xml` with Paper 1.21 API dependency.
    *   [x] Analyze codebase:
        *   [x] Identify main plugin class (`core/Main.java`).
        *   [x] Locate command registration (`plugin.yml`, `core/Main.java`).
        *   [x] Find existing `/statistic` command logic (`core/commands/StatCommand.java`).
        *   [x] Understand statistic retrieval (`core/statistic/StatRequestManager.java`, `BukkitProcessor.java` using Bukkit API).
        *   [x] Locate configuration handling (`core/config/ConfigHandler.java`).
        *   [x] Find message formatting/prefix usage (`core/msg/OutputManager.java`, `MessageBuilder.java`, `ComponentFactory.java`).
*   [X] **Phase 2: Core Implementation**
    *   [x] Define and implement approved stats configuration (`config.yml`, `core/config/ConfigHandler.java`, `ApprovedStat.java`).
    *   [x] Add default approved stats (`config.yml`).
    *   [x] Implement `/top` command logic (fetching, formatting, sender rank) (`core/commands/TopCommand.java`, `core/statistic/BukkitProcessor.java`, `core/msg/TopCommandFormatter.java`).
    *   [x] Implement `/statadmin addapproved | removeapproved | listapproved` command logic (`core/commands/StatAdminCommand.java`).
*   [X] **Phase 3: Refinement & Cleanup**
    *   [x] Remove all plugin prefixes from messages (`core/msg/MessageBuilder.java`, `core/msg/components/ComponentFactory.java`).
    *   [x] Review code for potential compatibility issues (Maintained Bukkit stats API usage).
    *   [x] Ensure commands are registered in `plugin.yml`.
    *   [x] Add necessary permissions to `plugin.yml`.
    *   [x] **Fix:** Restore safe reload functionality by making `/playerstats reload` wait for active statistic calculations (`core/multithreading/ReloadThread.java`, `core/multithreading/ThreadManager.java`).
*   [ ] **Phase 4: Testing & Documentation**
    *   [ ] (User) Test all new and existing functionality on Purpur 1.21.4.
    *   [ ] (User) Consider refining `/top` for "total" BLOCK/ITEM/ENTITY stats.
    *   [x] Update `INSTRUCTIONS.md` with final details.
    *   [x] **Document Existing Exclusions:** Note that `/top` automatically respects the player exclusion list.
*   [ ] **Phase 5: Final Review**
    *   [ ] (User) Final code review and testing before deployment.

## Player Exclusion

*   **Functionality:** Players can be excluded from all statistic calculations (including `/top`, `/stat server`, and individual `/stat player <name>` lookups if `allow-stat-lookup-for-excluded-players` is false in `config.yml`).
*   **Command:** `/statisticexclude` (Alias: `/statexclude`)
    *   `/statisticexclude add <player_name>`: Adds a player to the exclusion list.
    *   `/statisticexclude remove <player_name>`: Removes a player from the exclusion list.
    *   `/statisticexclude list`: Shows the current list of excluded players.
    *   `/statisticexclude info`: Shows help information for the command.
*   **Persistence:** The exclusion list is stored in `excluded_players.yml`.
*   **Permissions:** Requires `playerstats.exclude` permission (add this to `plugin.yml` if not already present).
*   **/top Integration:** The `/top` command automatically filters out players listed in `excluded_players.yml` *before* calculating the top statistics. No specific exclusion command is needed for `/top` itself.

## Debugging /stat Command (Ongoing)

**Problem:** The `/stat` command was reported to be broken, failing to recognize valid sub-statistics (blocks, items, entities) and sometimes providing no feedback at all.

**Analysis:**
1.  **Argument Parsing (`StatCommand.ArgProcessor`):** Argument parsing logic was somewhat fragile and could misinterpret arguments.
2.  **Feedback (`StatCommand.sendFeedback`):** When a sub-statistic was required but invalid (e.g., using an entity name for a block statistic), the feedback was generic (`WRONG_SUB_STAT`), not explaining *why* it was wrong. This made it seem like valid names weren't recognized.
3.  **Statistic Retrieval (`BukkitProcessor.getPlayerStat`):** When errors occurred during statistic retrieval (e.g., player not found, player excluded, internal Bukkit exceptions), the method returned 0 silently without informing the command sender. This caused the command to fail without any feedback.
4.  **Async Errors (`BukkitProcessor.processServerRequest`/`processTopRequest`):** Errors during asynchronous calculation of server/top stats might not be properly reported back to the sender (requires further investigation).

**Fixes Applied:**
1.  **Enhanced Feedback (`StatCommand`, `OutputManager`, `MessageBuilder`, `StandardMessage`):**
    *   Modified `StatCommand.sendFeedback` to distinguish between missing sub-stats, generally invalid sub-stat names, and sub-stats of the wrong type for the specific statistic.
    *   Added new methods (`sendFeedbackMsgWrongSubStatType`, `invalidSubStatName`, `invalidCommandSyntax`) to `OutputManager` and `MessageBuilder`.
    *   Added new `StandardMessage` enums (`INVALID_SUBSTAT_NAME`, `INVALID_COMMAND_SYNTAX`).
    *   Added an overloaded `OutputManager.sendFeedbackMsg` to handle messages with arguments.
2.  **Error Reporting in Stat Retrieval (`BukkitProcessor`):**
    *   Modified `getPlayerStat` to catch errors (null player name, excluded player, player not found, exceptions).
    *   Instead of returning 0 silently, it now sends specific feedback messages (`PLAYER_NOT_FOUND`, `PLAYER_IS_EXCLUDED`, `INTERNAL_ERROR`, etc.) using `OutputManager`.
    *   `getPlayerStat` now returns `-1` on error.
    *   Modified `processPlayerRequest` to check for the `-1` return value and abort processing if an error occurred (as feedback was already sent).

**Remaining Tasks/Potential Issues:**
*   Investigate error handling in asynchronous statistic calculations (`ThreadManager`/`StatCalculator`) for server/top stats to ensure errors are reported to the user.
*   (Low Priority) Consider making `StatCommand.ArgProcessor` more robust to argument order variations.

---

*This document will be updated as progress is made.* 