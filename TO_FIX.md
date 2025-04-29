# PlayerStats Code Review Findings

This document lists issues found during the code review, categorized by priority.

## Highest Priority

*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/core/statistic/StatRequestManager.java`
    *   **API/Implementation Mismatch & Broken Execution:** The `StatManager` API defines `execute...Request` methods to return `StatResult` synchronously. However, the `RequestProcessor` (`BukkitProcessor`) implementation now processes requests asynchronously (returns `void`) and sends output via `OutputManager`. `StatRequestManager` attempts to bridge this by calling the void methods and returning *dummy* `StatResult` objects, breaking the API contract. **This is a fundamental design flaw.** Either the API needs redesigning for async results (e.g., `CompletableFuture`), or the processor needs to return results synchronously again.
*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/core/statistic/BukkitProcessor.java`
    *   **API Mismatch Implementation:** Implements the asynchronous processing (`void` methods, callbacks) that conflicts with the synchronous `StatManager` API contract.
    *   **Deprecated Logic Fallback (`getPlayerStat`):** Uses deprecated `StatRequest.Settings` fields as a fallback if `ApprovedStat` is null. Enforce `ApprovedStat` usage and remove fallback.
*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/api/StatRequest.java`
    *   **Deprecated Fields/Methods & Redundancy:** The `StatRequest.Settings` inner class contains several deprecated fields (`statistic`, `subStatEntryName`, `entity`, `block`, `item`) and getters. The `approvedStat` field should be used instead. This redundancy creates potential for inconsistencies. Methods relying on deprecated fields (`configureUntyped`, `configureBlockOrItemType`, `configureEntityType`) likely need refactoring/deprecation.
*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/core/statistic/ServerStatRequest.java`
    *   **Relies on Deprecated `StatRequest` Configuration:** Uses the deprecated configuration methods (`configureUntyped`, `configureBlockOrItemType`, `configureEntityType`) from `StatRequest`. Needs refactoring to use `ApprovedStat` instead.
*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/core/statistic/TopStatRequest.java`
    *   **Mixed Configuration Methods:** Implements `RequestGenerator` using deprecated `StatRequest` methods (`configureUntyped`, etc.) alongside the correct `approvedStat()` method. Remove or refactor deprecated methods to align with `ApprovedStat` usage.
*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/core/statistic/PlayerStatRequest.java`
    *   **Relies on Deprecated `StatRequest` Configuration:** Uses the deprecated configuration methods (`configureUntyped`, `configureBlockOrItemType`, `configureEntityType`) from `StatRequest`. Needs refactoring to use `ApprovedStat` instead.
*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/core/msg/msgutils/EasterEggProvider.java`
    *   **Hardcoded UUIDs & Potentially Offensive Content:** Contains hardcoded player UUIDs tied to specific, non-portable nicknames. One nickname is potentially offensive/unprofessional. This entire feature is highly server-specific and should likely be removed or made configurable.
*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/core/multithreading/StatAction.java`
    *   **Deprecated Logic Fallback:** Uses deprecated `getStatValue_Legacy` and deprecated fields from `StatRequest.Settings` as a fallback. This indicates parts of the code still rely on the old system. Remove the fallback and enforce usage of `ApprovedStat` everywhere.
*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/core/multithreading/ThreadManager.java`
    *   **API Mismatch Interaction:** Part of the broken asynchronous execution flow that contradicts the synchronous `StatManager` API.

## Medium Priority

*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/core/utils/MyLogger.java`
    *   **Static State for Threads:** Uses a static map (`threadNames`) to track threads across potentially concurrent recursive actions. This can lead to race conditions and incorrect logging. Thread tracking state should be managed per-action, not statically in the logger.
*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/api/StatRequest.java`
    *   **Potential NullPointerException in `hasMatchingSubStat()`:** The method needs more robust null-checking. It accesses `approvedStat.getComponents().get(0)` without verifying if the list is non-null and not empty. Calls to `component.material()` and `component.entityType()` within the switch also lack null checks.
*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/core/msg/msgutils/FontUtils.java`
    *   **Magic Numbers & Unclear Calculations:** Uses magic numbers (130.0, 2, 6, 7, 1.5) for alignment calculations without clear explanation. Use named constants and add comments explaining the logic.
*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/core/commands/ReloadCommand.java`
    *   **Static Field Misuse:** The `threadManager` field is static but assigned in the constructor. This is problematic if multiple instances are created, as they will overwrite the shared static field. Make `threadManager` an instance field or use proper static initialization/injection.
*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/core/listeners/JoinListener.java`
    *   **Static Field Misuse:** Similar to `ReloadCommand`, assigns to a `static` field (`threadManager`) in the constructor. Use an instance field instead.
*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/core/commands/ShareCommand.java`
    *   **Static Field Initialization:** Static fields (`outputManager`, `shareManager`) are initialized in the constructor using `getInstance()`. Initialize in a static block or retrieve directly in `onCommand` for clarity if they are singletons.
*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/core/msg/msgutils/StringUtils.java`
    *   **Inefficient String Manipulation:** The `prettify` method uses multiple loops and repeated modifications to `StringBuilder`. This can be optimized, likely into a single pass or using more efficient regex.
*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/core/commands/ExcludeCommand.java`
    *   **Static Field Initialization:** Static field `outputManager` is initialized in the constructor. Use static initialization or retrieve directly in `onCommand`.
*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/core/multithreading/ReloadThread.java`
    *   **Static Field Misuse:** The `outputManager` field is static but assigned in the constructor. Use an instance field instead.
*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/core/statistic/StatRequestManager.java`
    *   **Static Field Usage:** The `processor` field is static but managed awkwardly between instance constructor, reload method, and static getter. Clarify if it should be static or instance-based.
    *   **Deprecated `execute` Method:** Contains a static deprecated `execute` method that should likely be removed.
*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/core/msg/msgutils/ComponentSerializer.java`
    *   **Complex/Fragile Logic:** The `getTranslatableComponentSerializer` method's `ComponentFlattener` mapper logic is extremely complex, deeply nested, and likely fragile. Needs significant simplification and clarification, potentially breaking it into helper methods or using a different approach for custom entity translation.
*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/core/msg/components/ExampleMessage.java`
    *   **Component Delegation:** Implements `TextComponent` by delegating all methods to an internal `TextComponent`. Consider simplifying by having `construct` directly return the built `TextComponent` instead of wrapping it in `ExampleMessage`.
*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/core/msg/components/ExcludeInfoMessage.java`
    *   **Component Delegation:** Similar to `ExampleMessage`, implements `TextComponent` by delegating. Consider having `construct` return the built `TextComponent` directly.
*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/core/utils/YamlFileHandler.java`
    *   **Error Handling:** Catches `IOException` in `save()`/`updateFile()` but only prints stack trace. Needs more robust logging and potentially user feedback.
*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/core/multithreading/StatAction.java`
    *   **Incomplete Compound Stat Handling:** Logs warnings and skips components for total BLOCK/ITEM stats within compound `ApprovedStat`s, indicating incomplete functionality. Document or implement this fully.
*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/core/commands/TopCommand.java`
    *   **Static Field Initialization:** Static fields (`outputManager`, `config`) initialized in constructor. Use static block or direct `getInstance()` calls in `onCommand`.
    *   **Unused Constructor Parameter:** The `threadManager` constructor parameter is unused.
*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/core/sharing/ShareManager.java`
    *   **Singleton Implementation:** Uses double-checked locking. Consider simpler modern alternatives (Initialization-on-demand holder, enum).
    *   **Thread Safety in `reload()`:** Reload method modifies shared state without synchronization, potentially causing race conditions if called concurrently with other methods.
    *   **Queue Resize Data Loss:** Resizing the `sharedResults` queue discards the oldest 450 results, which might be unexpected. Logic could be simplified.
*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/core/msg/components/HelpMessage.java`
    *   **Component Delegation:** Similar to `ExampleMessage`, implements `TextComponent` by delegating. Consider having `construct...Msg` return the built `TextComponent` directly.
*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/core/msg/TopCommandFormatter.java`
    *   **Dot Alignment Logic:** Alignment logic uses magic numbers and string length, which is unreliable. Use character width data (e.g., via Adventure API) instead.
    *   **Number Formatting Replication:** Replicates complex number formatting logic instead of fully delegating to `NumberFormatter`/`ComponentFactory`.
*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/core/multithreading/ThreadManager.java`
    *   **Static Field Usage:** Class heavily relies on static fields, hindering testability and clarity. Refactor for dependency injection or proper singleton pattern.
    *   **Request/Time Tracking:** `activeRequests` might not limit console requests properly. `taskTime` key generation is fragile. Average calculation considers all historical times.
*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/core/msg/MessageBuilder.java`
    *   **Relies on Deprecated `StatRequest.Settings`:** Methods like `formatted...Function` rely on the deprecated `StatRequest.Settings` object and its fields. Refactor to use `ApprovedStat` and context directly.

## Low Priority

*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/api/enums/Unit.java`
    *   The `getSmallerUnit` method's switch statement contains nested `if-else` logic which could potentially be refactored for simplicity.
    *   The `fromString` method could benefit from clearer documentation on exactly which string variations are accepted for each unit.
    *   The `getTypeFromStatistic` method uses string checking (`.contains()`) on statistic names, which could break if Bukkit/Spigot changes these names. Consider mapping `Statistic` enums to `Unit.Type` more directly if possible.
    *   Magic numbers (e.g., 20, 86400, 3600, 60, 100000) are used for time and distance conversions in `getMostSuitableUnit` and `getSeconds`. Defining named constants would improve readability and maintainability.
*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/api/StatResult.java`
    *   The methods `getNumericalValue()`, `getFormattedTextComponent()`, and `formattedString()` are simple accessors for the record's components (`value`, `formattedComponent`, `formattedString`). These could potentially be removed as the record components are implicitly public and accessible, unless kept for stylistic reasons or future expansion.
*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/api/StatRequest.java`
    *   **Validation Logic Placement:** The validation logic in `hasMatchingSubStat()` might be better integrated into the abstract `isValid()` method's implementations.
    *   **Exception Clarity:** `IllegalArgumentException` messages in `configure` methods could be more specific about invalid combinations, especially relating to `ApprovedStat`.
*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/api/StatTextFormatter.java`
    *   **Method Overloading:** Numerous overloaded methods for formatting different stat types and units. While functional, it makes the interface large. Consider if simplification is possible (e.g., parameter objects).
    *   **JavaDoc Details:** Some JavaDocs refer to internal implementation details (e.g., config settings), which is generally discouraged for interfaces.
    *   **JavaDoc Typo:** Minor typo ("s*/") in `getRainbowPluginPrefix()` JavaDoc.
*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/core/utils/Closable.java`
    *   **Redundant Interface:** Consider replacing the custom `Closable` interface with the standard `java.lang.AutoCloseable` to enable try-with-resources usage.
*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/core/msg/msgutils/FontUtils.java`
    *   **Deprecated API Usage:** Relies on the deprecated `org.bukkit.map.MinecraftFont`. Consider alternatives using Adventure API or other methods for text alignment.
*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/core/utils/UnixTimeHandler.java`
    *   **Magic Numbers:** Uses magic numbers (24, 60, 60, 1000) for time conversion. Use `TimeUnit.DAYS.toMillis()` or named constants for clarity.
    *   **Potential Overflow:** Direct multiplication for time conversion could theoretically overflow for extremely large inputs (minor risk). `TimeUnit` methods are safer.
*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/core/listeners/JoinListener.java`
    *   **Reload Trigger:** Reloading the entire plugin on every *first* player join might be excessive and could cause performance issues on servers with frequent new players. Consider a more lightweight update mechanism.
*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/core/msg/msgutils/FormattingFunction.java`
    *   **Constructor Null Check:** The constructor should check if the provided `BiFunction` is null to prevent potential `NullPointerException` later.
    *   **Implicit Null Handling:** Relies on passing `null` to the wrapped function to control behavior, which is less explicit than alternatives but acceptable for a simple wrapper.
*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/core/commands/ShareCommand.java`
    *   **Error Handling Feedback:** Does not inform the sender if the provided share code argument is invalid (not an integer). Only logs the error.
    *   **Comment Clarity:** Comment about null `formattedComponent` should clarify why the `StoredResult` (`result`) itself might be null.
*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/core/msg/msgutils/StringUtils.java`
    *   **TODO Comment:** Contains a `//TODO` for removing excessive logging.
    *   **Null Handling:** Correctly handles null input.
*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/core/config/DefaultValueGetter.java`
    *   **Class Naming:** Name `DefaultValueGetter` is slightly misleading; the class primarily adjusts values for migration. Consider renaming (e.g., `ConfigMigrationHelper`).
    *   **Hardcoded Migration Logic:** Migration rules are hardcoded. Consider a more extensible approach if future migrations are anticipated.
*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/core/commands/ExcludeCommand.java`
    *   **Argument Validation:** Does not explicitly validate player names for `add`/`remove` or provide feedback if a name is invalid/not found.
    *   **Command Structure:** Nested if/else/switch logic could potentially be refactored for clarity.
*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/core/statistic/ServerStatRequest.java`
    *   **Constructor Ambiguity:** Default constructor uses `Bukkit.getConsoleSender()`. Consider if a sender is truly needed for server requests or make the dependency more explicit/optional.
    *   **`isValid()` Implementation:** Relies on potentially flawed and deprecated-field-based `super.hasMatchingSubStat()`. Validation should align with `ApprovedStat`.
*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/core/msg/components/HalloweenComponentFactory.java`
    *   **Random Instance Creation:** Creates a new `Random` instance on each call to `decorateWithRandomGradient`. Reuse a single instance for better performance.
    *   **Magic Numbers in Switch:** Uses integer literals for switch cases. Consider named constants or an enum for clarity.
*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/core/statistic/TopStatRequest.java`
    *   **Constructor Ambiguity:** Constructor defaults to `Bukkit.getConsoleSender()`. Clarify need for sender or make optional.
    *   **`isValid()` Implementation:** Relies partially on potentially flawed and deprecated-field-based `super.hasMatchingSubStat()`. Base validation solely on `ApprovedStat` properties.
*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/core/statistic/PlayerStatRequest.java`
    *   **Constructor Ambiguity:** Constructor defaults to `Bukkit.getConsoleSender()`. Clarify need for sender or make optional.
    *   **`isValid()` Implementation:** Relies partially on potentially flawed and deprecated-field-based `super.hasMatchingSubStat()`. Base validation solely on `ApprovedStat`.
    *   **Singleton Usage:** Uses `getInstance()` directly within `hasValidTarget()`. Consider dependency injection for better testability.
*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/core/msg/components/PrideComponentFactory.java`
    *   **Random Instance Creation:** Creates a new `Random` instance on each call to `decorateWithRandomGradient`. Reuse a single instance.
    *   **Magic Numbers in Switch:** Uses integer literals for switch cases. Consider named constants or an enum.
    *   **Hardcoded Prefix:** `pluginPrefix()` builds the prefix string character by character with hardcoded colors. Less maintainable than using gradients or defined colors.
*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/core/multithreading/ReloadThread.java`
    *   **Magic Numbers:** Uses magic numbers (20, 50) for wait loop timing/messaging. Use named constants.
*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/core/multithreading/StatAction.java`
    *   **Magic Number:** `THRESHOLD` constant could use a comment explaining its value (1000).
    *   **Array Copy:** Uses `System.arraycopy`; `Arrays.copyOfRange` might be more readable.
    *   **Logging:** Contains potentially excessive debug logging.
*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/core/commands/TopCommand.java`
    *   **Hardcoded Feedback:** `sendAvailableStats` sends a plain string list of aliases. Use `OutputManager` for formatted feedback.
    *   **Missing Standard Messages:** References `NO_APPROVED_STATS` and `AVAILABLE_STATS` which are not in the `StandardMessage` enum.
    *   **Calculating Message Format:** Sends generic calculating message and stat name separately. Combine into one formatted message.
*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/core/msg/msgutils/ComponentSerializer.java`
    *   **Singleton Usage:** Uses `LanguageKeyHandler.getInstance()` in constructor. Consider dependency injection.
    *   **Redundant Serializer Creation:** Could potentially build the final serializer directly instead of modifying a base serializer's builder.
*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/core/msg/components/BukkitConsoleComponentFactory.java`
    *   **Color Simplification:** Since this factory is for Bukkit consoles (no hex), consider defining colors directly using `NamedTextColor` constants instead of converting `PluginColor` enum values, removing repeated `NamedTextColor.nearestTo()` calls.
*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/core/utils/YamlFileHandler.java`
    *   **Redundant Check:** `reload()` checks if file exists, which is likely redundant as it's checked/created in constructor.
    *   **Static Access:** Uses `Main.getPluginInstance()` directly. Consider dependency injection.
    *   **List Modification:** List writing/removing could be slightly optimized.
    *   **Update Frequency:** `updateFile()` is called after every modification. Consider if batching is needed.
*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/core/msg/msgutils/EasterEggProvider.java`
    *   **Random Logic:** Nickname probability is based on random numbers and magic number ranges.
    *   **PAPI Integration:** Uses a custom MiniMessage tag resolver for PAPI placeholders, which might be overly complex for this context.
*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/core/msg/msgutils/NumberFormatter.java`
    *   **Magic Numbers:** Uses magic numbers for damage, distance, and time conversions. Use named constants.
    *   **Time Formatting Complexity:** The logic in `formatTimeNumber` to break down seconds into D/H/M/S is functional but could be simplified/clarified.
*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/core/commands/TabCompleter.java`
    *   **Singleton Usage:** Uses `getInstance()` in constructor. Consider dependency injection.
    *   **Logic Complexity:** `getStatCommandSuggestions` logic is nested and complex due to context dependence. Could potentially be refactored for clarity.
    *   **Stream API:** Uses stream for simple filtering in `getDynamicTabSuggestions`; a loop might be marginally faster.
*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/core/utils/MyLogger.java`
    *   **Static Plugin Access:** Uses static `Bukkit.getPluginManager().getPlugin(...)`. Consider injecting Plugin/Logger.
    *   **Debug Level Setting:** `setDebugLevel` uses if/else if/else; consider `switch`.
    *   **Thread Name Logging:** Logic seems correct but tied to problematic static state.
*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/core/msg/components/HelpMessage.java`
    *   **Redundant Helpers:** Private `space()` and `newline()` methods are trivial; use `Component.space/newline()` directly.
    *   **Code Duplication:** `buildPlainMsg` and `buildHoverMsg` have significant structural overlap; consider refactoring common parts.
*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/core/msg/TopCommandFormatter.java`
    *   **Singleton Usage:** Uses `ConfigHandler.getInstance()` and `EnumHandler.getInstance()`. Consider dependency injection.
    *   **Sender Rank Handling:** Uses `senderRank <= 0` check and string replacement for unranked lines; consider a cleaner approach.
*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/core/statistic/BukkitProcessor.java`
    *   **Stream Sorting:** Uses streams for sorting/limiting top stats; performance on very large maps might need consideration.
*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/core/msg/MessageBuilder.java`
    *   **Dot Alignment Logic:** Uses unreliable `getNumberOfDotsToAlign` (based on `FontUtils`).
    *   **Complexity & Length:** Class is extremely large (>900 lines) with many long, complex methods. Needs significant refactoring into smaller, focused components.
*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/core/msg/components/ComponentFactory.java`
    *   **Complexity & Responsibility:** Mixes component creation, config retrieval, translation logic, hover text construction, and color/style parsing. Violates SRP and needs refactoring.

*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/core/Main.java`
    *   **Static Fields/Singletons:** Heavy reliance on static fields and `getInstance()` makes dependencies implicit and harder to test/manage. Consider dependency injection.
    *   **Initialization Order:** Relies on a fragile manual initialization order in `initializeMainClassesInOrder()`.

## Low Priority

*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/core/multithreading/ThreadManager.java`
    *   **Magic Number:** Unused `threshold` constant.
    *   **Unused Field:** `lastRecordedCalcTime` field is unused.
    *   **Error Handling:** Doesn't provide feedback to sender on calculation/callback exceptions.

*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/core/Main.java`
    *   **`getVersion()`:** Returns only the first character of the version, likely incorrect.
    *   **`getStatTextFormatter()`:** Exposes internal `MessageBuilder` implementation; should return the interface type.
    *   **`getStatNumberFormatter()`:** Creates a new instance on each call; should return a shared instance.
    *   **Metrics:** Commented-out bStats chart needs investigation/fixing/removal.
    *   **Reload Logic:** Ensure `reloadPlugin()` order is sufficient for all components.

*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/core/config/ConfigHandler.java`
    *   **Approved Stats Modification:** `addApprovedStat`/`removeApprovedStat` modify the config *in memory only* and do not save changes to file, despite logging messages suggesting otherwise. Changes are lost on next load. Needs to call `save()`/`updateFile()`.
    *   **Approved Stats Loading Complexity:** `loadApprovedStats` logic is complex (handling missing sections, defaults, parsing types). Consider simplification.
    *   **Singleton Implementation:** Uses double-checked locking. Consider simpler modern alternatives.

*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/core/multithreading/ThreadManager.java`
    *   **Magic Number:** Unused `threshold` constant.
    *   **Unused Field:** `lastRecordedCalcTime` field is unused.
    *   **Error Handling:** Doesn't provide feedback to sender on calculation/callback exceptions.

*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/core/Main.java`
    *   **`getVersion()`:** Returns only the first character of the version, likely incorrect.
    *   **`getStatTextFormatter()`:** Exposes internal `MessageBuilder` implementation; should return the interface type.
    *   **`getStatNumberFormatter()`:** Creates a new instance on each call; should return a shared instance.
    *   **Metrics:** Commented-out bStats chart needs investigation/fixing/removal.
    *   **Reload Logic:** Ensure `reloadPlugin()` order is sufficient for all components.

*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/core/config/ConfigHandler.java`
    *   **Limited `addApprovedStat`:** Only supports adding simple, not compound, stats.
    *   **Redundant `reload()` Call:** `checkAndUpdateConfigVersion` calls `reload()` potentially unnecessarily.
    *   **Parser Validation:** Validation in `parseStatComponent` uses a heuristic for sub-stat requirements.
    *   **Getter Complexity:** Many getters have conditional logic.
    *   **Singleton Usage:** Uses `EnumHandler.getInstance()`. Consider injection.
    *   **Magic Strings/Numbers:** Uses hardcoded config paths and default values.

*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/core/utils/EnumHandler.java`
    *   **Static Fields/Singleton:** Holds large static lists of all enum names, initialized once. High memory usage and poor testability. Consider alternative approaches.

## Low Priority

*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/core/msg/components/ComponentFactory.java`
    *   **Singleton Usage:** Uses `ConfigHandler.getInstance()`. Consider injection.
    *   **Prefix Methods:** `pluginPrefix`/`pluginPrefixAsTitle` return empty components.
    *   **Color/Style Parsing:** Contains utility logic for parsing config strings; could be helper class.
    *   **Magic Characters/Strings:** Uses unicode chars and hardcoded strings.

*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/core/utils/EnumHandler.java`
    *   **Redundant Getters:** Contains duplicate getter methods (e.g., `getAllStatNames` vs `getStatNames`). Remove older versions.
    *   **Stream Usage:** Uses parallel streams in `prepareLists` where benefits might be minimal.
    *   **Enum Comparison:** `isEntityStatistic` uses string comparison; direct enum comparison (`==`) is better.

*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/core/msg/msgutils/LanguageKeyHandler.java`
    *   **Static Field/Singleton:** Uses singleton pattern with a static key map.
    *   **Complexity (`convertLanguageKeyToDisplayName`):** Complex logic mixing translation file lookup, hardcoded rules, and regex parsing.
    *   **Hardcoded Custom Keys:** Relies on hardcoded custom language keys for specific stats (kill/killed_by).

*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/core/utils/OfflinePlayerHandler.java`
    *   **Static Fields/Singleton:** Uses static maps and singleton pattern, leading to global state and testability issues. Potential high memory use.

## Medium Priority

*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/core/utils/OfflinePlayerHandler.java`
    *   **Potential Blocking:** Initial load/reload might block the calling thread due to expensive player loading operations.
    *   **`Bukkit.getOfflinePlayers()` Efficiency:** Calling `Bukkit.getOfflinePlayers()` is very inefficient and can cause server lag/delays.

*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/core/msg/msgutils/LanguageKeyHandler.java`
    *   **Wall Banner Handling:** Contains hardcoded logic for wall banners.
    *   **Hardcoded Key Mapping:** `generateStatisticKeys` hardcodes mappings between Statistic enums and key parts.
    *   **Singleton Usage:** Uses `EnumHandler.getInstance()` and `Main.registerReloadable()`. Consider injection.

*   **File:** `src/main/java/com/artemis/the/gr8/playerstats/core/utils/OfflinePlayerHandler.java`
    *   **Parallel Stream Usage:** Parallel streams may offer negligible benefit here.
    *   **Exception Handling:** Consider `Optional` instead of throwing `IllegalArgumentException`.
    *   **Concurrency in Add/Remove:** Add/remove operations are not atomic.
    *   **Minor Redundancy:** `getIncludedOfflinePlayer` calls `get()` twice.
    *   **Singleton Usage:** Uses `ConfigHandler.getInstance()`, `ThreadManager`, `Main.registerReloadable`. Consider injection.