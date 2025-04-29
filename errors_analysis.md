# PlayerStats Error Analysis and Fix Plan



## 2. Plugin Enablement Error (`NoClassDefFoundError`)

*   **Issue:** The plugin fails to enable entirely, throwing a fatal error.
    *   Log Message: `java.lang.NoClassDefFoundError: com/artemis/the/gr8/playerstats/lib/kyori/adventure/text/Component`
    *   Caused by: `java.lang.ClassNotFoundException: com.artemis.the.gr8.playerstats.lib.kyori.adventure.text.Component`
*   **Root Cause:** The core issue is that the Java Virtual Machine cannot find the required `Component` class from the Kyori Adventure library at runtime *at the specified path*. The path `com.artemis.the.gr8.playerstats.lib.kyori...` strongly indicates the plugin is attempting to use a *shaded* (relocated/packaged within the plugin JAR) version of the Adventure library. However, modern Minecraft servers like PaperMC *provide* the Adventure library natively. Plugins should use the server-provided library (`net.kyori.adventure.text.Component`) to ensure compatibility and avoid conflicts. The error arises because either:
    *   The shading process in the build (`pom.xml` using `maven-shade-plugin`) was incomplete or incorrect, failing to include the necessary classes.
    *   The Java code incorrectly imports the shaded path (`com.artemis.the.gr8.playerstats.lib...`) instead of the standard path (`net.kyori.adventure...`).
    *   The `pom.xml` might incorrectly define the Adventure API dependency (e.g., not marking it as `<scope>provided</scope>`).
*   **Optimal Fix:**
    1.  **Check `pom.xml`:** Verify that the `adventure-api` dependency exists and is marked with `<scope>provided</scope>`. This tells Maven that the server environment will supply this library.
    2.  **Adjust Shading:** Examine the `maven-shade-plugin` configuration in `pom.xml`. Remove any rules that attempt to shade and relocate the `net.kyori.adventure` packages. The plugin should *not* bundle its own copy of Adventure.
    3.  **Correct Imports:** Perform a codebase search for any Java import statements using the `com.artemis.the.gr8.playerstats.lib.kyori` path and replace them with the correct `net.kyori.adventure` path.
