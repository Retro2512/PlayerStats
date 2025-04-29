# Implementation Plan for `/top` Command Fixes

This plan outlines small, discrete steps to address the issues identified in `fixes.md`. Each step includes a corresponding test to verify correctness before moving on.

---

## Step 1: Alias Consistency

**Goal**: Unify alias naming for playtime and config entries.

- Change code to accept both `play_time` and `playtime` (alias lookup).
- Update help text and TabCompleter to suggest both variants.

**Test**:
- `/top play_time` and `/top playtime` both display playtime stats.

---

## Step 2: Total-Request Fallback for `handleMined`

**Goal**: If `total_blocks_mined` is not defined in config, dynamically create a total-request stat.

- Modify `handleMined` to fallback to dynamic ApprovedStat when `config.getApprovedStat` returns null.
- Ensure display name matches user expectation (e.g., "Blocks Mined (Total)").

**Test**:
- Remove `total_blocks_mined` from config; `/top mined` still works and shows top miners.

---

## Step 3: Argument Validation & Usage Messages

**Goal**: Enforce exact argument counts and provide subcommand-specific usage hints.

### 3.1 Distance Travelled
- If `args.length > 2`, send usage: `Usage: /top distance_travelled [mode]`

### 3.2 Kills
- If `args.length > 3` or invalid combination, send usage: `Usage: /top kills [player|hostile|passive|entity <type>]`

### 3.3 Ores Mined
- Already sends usage on `<2` args; add check if `args.length > 2` to warn.

### 3.4 Craft
- If `args.length > 2`, send usage: `Usage: /top craft [<item>]`

### 3.5 Mined & Playtime
- Enforce `args.length == 1` only; warn on extra args: `Usage: /top mined` or `Usage: /top playtime`

**Test**:
- Call each subcommand with too many args; verify usage messages appear.

---

## Step 4: TabCompleter Synchronization

**Goal**: Ensure suggestions match actual handlers and config aliases.

- Add `total_blocks_mined`, `total_items_crafted`, `play_time`, `times_jumped`, etc. to top-level suggestions when `args.length == 1`.
- Sync `distanceModes` with keys in `distanceModeStats`.
- Ensure `oreTypes` are derived from available `_mined` aliases correctly.

**Test**:
- Tab-complete `/top ` and see all valid aliases.
- Tab-complete `/top distance_travelled ` shows the full list.

---

## Step 5: plugin.yml & README Updates

**Goal**: Update command usage descriptions to reflect new syntax and aliases.

- In `plugin.yml`, adjust `usage` for `/top`, include subcommands and options.
- Update README.md to document all `/top` subcommands and required config keys.

**Test**:
- Open plugin help (`/help top`); usage line is clear and accurate.

---

## Step 6: Unit Tests (Optional)

**Goal**: Introduce automated tests for each handler.

- Add JUnit tests for `TopCommand` methods using mock `CommandSender` and arguments.

**Test**:
- All tests pass locally.

---

Please review this plan and let me know if you'd like any adjustments or to proceed with these steps. 