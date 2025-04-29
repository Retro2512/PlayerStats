# Fixes and Improvement List

_Ordered by priority (most to least urgent)_

## 1. Critical (API/Processor mismatch)

1. **StatRequestManager dummy results and TODOs**
   - File: `src/main/java/com/artemis/the/gr8/playerstats/core/statistic/StatRequestManager.java`
   - Issue: The methods `executePlayerStatRequest`, `executeServerStatRequest`, and `executeTopRequest` return placeholder `StatResult` instances due to API/processor mismatch. Multiple TODOs present:
     - `// TODO: Adapt API or processor - how should results be retrieved synchronously now?`
     - `// TODO: Adapt API or processor`
   - Impact: Synchronous API calls always return zeros or empty collections, breaking plugins expecting real data.

2. **ConfigHandler missing validation for derived stat components**
   - File: `src/main/java/com/artemis/the/gr8/playerstats/core/config/ConfigHandler.java` (around line 761)
   - Issue: Contains `// TODO: Add check later during calculation to ensure compAlias refers to an existing ApprovedStat`. Without this validation, derived statistics may reference nonexistent stats at runtime.
   - Impact: Potential runtime errors or silent failures when computing derived stats.

## 2. High (Incomplete or half-baked implementations)

3. **Excessive logging in StringUtils**
   - File: `src/main/java/com/artemis/the/gr8/playerstats/core/msg/msgutils/StringUtils.java`
   - Issue: Contains `//TODO remove excessive logging` comment.
   - Impact: Log spam and possible performance degradation.

4. **Additional pending TODOs in StatRequestManager**
   - File: `src/main/java/com/artemis/the/gr8/playerstats/core/statistic/StatRequestManager.java`
   - Issue: Beyond the critical stubbed methods, there are extra TODOs in `executeServerStatRequest` and `executeTopRequest` methods.
   - Impact: Highlights repeated incomplete integration between API and async processor.

## 3. Medium (Code duplication and potential refactor)

5. **Duplicate mapping logic in TopCommand and TabCompleter**
   - Files:
     - `src/main/java/com/artemis/the/gr8/playerstats/core/commands/TopCommand.java`
     - `src/main/java/com/artemis/the/gr8/playerstats/core/commands/TabCompleter.java`
   - Issue: Both classes replicate logic for iterating `commandCategories`, calling `mapCommandToAlias`, and filtering unmapped aliases. Violates DRY.
   - Impact: Maintenance burden, risk of inconsistencies between command execution and tab completion.

6. **Incomplete variable-argument message handling in OutputManager**
   - File: `src/main/java/com/artemis/the/gr8/playerstats/core/msg/OutputManager.java`
   - Issue: The overloaded `sendFeedbackMsg(CommandSender, StandardMessage, String...)` method explicitly handles only `INVALID_SUBSTAT_NAME`, defaulting all other parameterized messages to generic fallback.
   - Impact: Other messages requiring arguments may be misformatted or dropped.

## 4. Low (Minor code smells and consistency issues)

7. **Hardcoded text color in TopCommand**
   - File: `src/main/java/com/artemis/the/gr8/playerstats/core/commands/TopCommand.java`
   - Issue: Uses `NamedTextColor.GOLD` directly for stat display. Should be configurable through `ConfigHandler`.
   - Impact: Inconsistent styling and limited theming flexibility.

8. **Commented-out exception in ApprovedStat**
   - File: `src/main/java/com/artemis/the/gr8/playerstats/core/config/ApprovedStat.java` (around line 285)
   - Issue: A `throw` statement for entity-type validation is commented out, suggesting uncertainty in validation requirements.
   - Impact: Potential silent misconfiguration or unexpected behavior for ENTITY-type stats.

9. **Potential resource handling in OutputManager.close()**
   - File: `src/main/java/com/artemis/the/gr8/playerstats/core/msg/OutputManager.java`
   - Issue: Static `adventure` field is closed and nulled, but repeated reloads or plugin disable/enable cycles may not reinitialize or close properly.
   - Impact: Memory leaks or `NullPointerException` on subsequent reloads.