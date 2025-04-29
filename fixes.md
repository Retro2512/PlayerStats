# Fixes and Potential Improvements for `/top` Command

This document analyzes the current `/top` command implementation, identifies logical gaps or error-prone areas across subcommands and related classes, and proposes fixes or enhancements.

---

## 1. General Observations

- **Alias Naming Consistency**  
  - Config aliases (`play_time`, `times_jumped`) differ from dynamic aliases in code (`playtime`). Align naming (e.g., use `play_time` everywhere or `playtime` everywhere) to avoid confusion.

- **Argument Validation**  
  - Several handlers only check minimum `args.length` but do not reject extra arguments. This can silently ignore user mistakes. Consider:
    - Validating `args.length` exactly for each subcommand.
    - Sending a usage hint on unexpected extra arguments.

- **Help & Usage Feedback**  
  - Only `/top ores_mined` handler sends detailed usage. Other handlers fall back to generic help. Improve UX by adding specific usage messages for each subcommand when syntax is wrong.

- **Error Logging vs. User Feedback**  
  - Code logs low-level warnings (e.g., missing sub-stat) but may not inform the user. Ensure critical errors (invalid config entries, missing aliases) are communicated clearly in chat.

---

## 2. `handleDistanceTravelled`

### Code excerpt
```java
if (args.length >= 2) { ... } else { /* total */ }
```

### Issues & Suggestions
- **Tab-Completion Mismatch**: `distanceModeStats` includes modes like `climb`, `mount`, but `distanceModes` (for suggestions) omits some. Synchronize these lists.
- **Extra Arguments**: If `args.length > 2`, extra args are ignored. Add a branch to reject or warn on too many args.
- **Alias Tracking**: Pseudo-alias `distance_total` is never registered in config. If users want to reference it elsewhere (e.g., in shared settings), consider adding a real alias in config.

---

## 3. `handleKills`

### Code excerpt
```java
if (args.length == 1) { /* default mob kills */ }
else { switch(filterType) { ... } }
request.getSettings().setKillFilterType(filterType);
```

### Issues & Suggestions
- **Filter Implementation**: Confirm that `StatRequestSettings#setKillFilterType` exists and is honored in the processor. Otherwise, `hostile`/`passive` and `/top kills entity <type>` behave identically.
- **Default FilterType Value**: On `/top kills`, `filterType` remains `null`, then `setKillFilterType(null)` – ensure processor handles `null` as total or defaults.
- **Extra Arguments**: `/top kills player extra` is accepted without error. Enforce exact `args.length` or explicit help on invalid extra args.
- **Help Message**: Provide usage string: `Usage: /top kills [player|hostile|passive|entity <type>]`.

---

## 4. `handleOresMined`

### Code excerpt
```java
if (args.length < 2) { send usage; return; }
Material ore = get from enum; if null -> error
```

### Issues & Suggestions
- **Case Insensitive Handling**: `enumHandler.getBlockEnum(oreArg + "_ore")` may fail for names like `Nether_Quartz`. Ensure consistent normalization (lowercase + underscores) on config and input.
- **Special Cases Hard-Coded**: `nether_quartz`, `nether_gold`, `ancient_debris` are coded manual cases. Document these or handle programmatically by checking `Material` names.
- **Suggesting Valid Ores**: Tab-completer lists only configured aliases. Consider generating a complete ore-type list from `EnumHandler.getAllBlockNames()` filtered by `_ORE` or a known whitelist.

---

## 5. `handleMined`

### Code excerpt
```java
String alias = "total_blocks_mined";
ApprovedStat stat = config.getApprovedStat(alias);
if (stat == null) -> error
```

### Issues & Suggestions
- **Config Dependency**: This subcommand relies on the `total_blocks_mined` alias exactly. If config alias differs (e.g., `blocks_mined`), the command breaks. Consider:
  - Document required aliases in `README.md`.
  - Automatically register `blocks_mined` as an alias by alias mapping in `ConfigHandler`.
- **Dynamic Fallback**: For missing alias, fallback could create a dynamic total‐request stat like other handlers do.

---

## 6. `handleCraft`

### Code excerpt
```java
if (args.length >= 2) { // specific item }
else { alias = total_items_crafted };
```

### Issues & Suggestions
- **Alias Mismatch**: Config uses `total_items_crafted`; user might try `/top craft` expecting total, but alias is `craft` in code? Confirm command help suggests `/top craft` for total.
- **Subcommand Name vs. Alias**: The pseudo alias for specific items is `craft_<item>`, but help lists `craft` only. Add help for `/top craft <item>`.
- **Tab-Completion Data**: `craftableItems` is loaded from all registered `enumHandler.getItemNames()` – this is good.

---

## 7. `handlePlaytime`

### Code excerpt
```java
StatComponent comp = new StatComponent(Statistic.PLAY_ONE_MINUTE);
ApprovedStat(alias="playtime")
```

### Issues & Suggestions
- **Naming Difference**: Config uses `play_time` but dynamic alias is `playtime`. Users must type `/top playtime`. Align to config alias or add both aliases to help.
- **Missing Config Entry**: If user expects to configure display-name via config, dynamic stat bypasses config. Either:
  - Add `playtime` to config defaults and load it.
  - Or accept a config override for dynamic stats.

---

## 8. `TabCompleter` Integration

### Observations & Suggestions
- **Subcommand List**: Hard-coded list matches `TopCommand.subcommands`, but must keep in sync when adding new aliases or changing names.
- **`oreTypes` Calculation**: Extracts all aliases ending with `_mined`. But alias `total_blocks_mined` ends with `_mined`? It ends with `_mined`. Good. Yet usage suggests bare ore types, not `blocks` or `total`. Confirm filtering logic.
- **`killTypes` vs. `TopCommand`**: `TabCompleter` suggests `entity`, `player`, `hostile`, `passive` for `/top kills`. Matches code. Good.
- **Extra-Alias Suggestions**: After `/top mined`, no further args; suggestions list empty. Acceptable.
- **Consistency**: Ensure `TabCompleter` respects dynamically added config aliases (like `total_blocks_mined`). If new aliases added, they may not appear in suggestions for no-arg case – consider adding `config.getApprovedAliases()` into top-level suggestions if `/top` is called with no subcommand.

---

## 9. ConfigHandler Approval Logic

- **Typed Stats Without Sub-stat**: Previously simple stats like `MINE_BLOCK` were skipped. We added total-request support. Ensure tests cover:
  - `total_blocks_mined`, `total_items_crafted` loaded as total.
  - `blocks_placed` total-request works.
- **Derived Stats**: `StatType.DERIVED` entries require a `components` list. Validate errors for missing list or invalid operations.

---

## 10. Automated Tests & Documentation

- **Unit Tests**: Add tests for each `/top` subcommand covering valid and invalid argument patterns, ensuring correct exceptions or help messages.
- **README / Wiki**: Document all `/top` subcommands, required config aliases, and expected behaviors to assist server administrators.

---

*End of fixes.md.* 