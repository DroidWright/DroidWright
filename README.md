<div align="center">

![DroidWright Banner](assets/droidwright-banner.jpg)

**Powerful, flexible automation for Android - right on your device**

[![Version](https://img.shields.io/badge/version-1.0.0--beta-blue)](https://github.com/tas33n/droidwright/releases)
[![License](https://img.shields.io/badge/license-MIT-green)](LICENSE)
[![Platform](https://img.shields.io/badge/platform-Android-lightgrey)](https://www.android.com)

</div>

---

## Why We Built This

Let's be honest - most automation apps on the Play Store are pretty limited. They give you basic click-and-swipe actions with rigid templates, but when you need real control and flexibility, they fall short. That's why we built DroidWright.

**DroidWright is different.** It's built for developers and power users who want:

- **Full script control** - Write your automation logic exactly how you want it, not limited by pre-made templates
- **Developer-friendly** - Use JavaScript, a language you already know, with a clean API that makes sense
- **Within-Android automation** - No need for a desktop computer or ADB access. Everything runs directly on your device
- **More power and flexibility** - Tap, swipe, scroll, type, wait, loop, conditionals - you name it, you can do it

Think of it as the difference between using a basic macro recorder versus writing your own automation code. If you've ever been frustrated by the limitations of those click automation apps, DroidWright is what you've been waiting for.

## Our Inspiration

- **[Appium](https://appium.io/)** proved how powerful mobile automation can be when it is scriptable.
- **[Playwright](https://playwright.dev/)** showed how ergonomic a modern automation API should feel.

DroidWright brings the same level of control to a self-contained Android experience.

## Features

- **JavaScript-Based Scripts** - Author automation in a familiar language with a clean, composable API.
- **AI-Powered Automation** - Optional Google Gemini integration that can turn natural-language goals into action plans.
- **Comprehensive UI Interaction** - Tap, scroll, swipe, type, wait, loop, and chain complex selectors.
- **App Control** - Launch, close, and monitor any installed application from within your script.
- **Real-time Logging** - Live console output plus logcat integration for deeper debugging.
- **Script Management** - Create, edit, import, export, and organize scripts in-app.
- **Material Design 3 UI** - A modern, responsive interface for managing automation.
- **Safety Features** - Rate limiting, resource monitoring, and ANR prevention baked in.

## Requirements

- Android device running API level 24 (Android 7.0) or newer.
- Accessibility service enabled for DroidWright plus overlay permission for floating controls.
- Android Studio Iguana (or newer) with AGP 8.2.0 support and JDK 17 (required by the Gradle 8.2 wrapper).
- Android SDK Platform 34, Build Tools 34.0.0, and platform tools (ADB) installed.
- USB debugging enabled (only needed when sideloading from source).

## Installation

### Build From Source

```bash
git clone https://github.com/tas33n/droidwright.git
cd droidwright
./gradlew clean assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Android Studio users can simply open the project and use the **Run** or **Build > Build Bundle(s)/APK(s) > Build APK** flow.

### APK Download

Grab the latest signed APK from the [Releases](https://github.com/tas33n/droidwright/releases) page and install it on your device.

### Verify the Build

Before submitting patches, run the standard checks:

```bash
./gradlew lint
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest   # Requires a device or emulator
```

## Quick Start

1. **Enable Accessibility** - Settings -> Accessibility -> DroidWright Automation Service -> Enable.
2. **Grant Permissions** - Accept overlay and runtime permissions when prompted.
3. **Create a Script** - Open the Scripts tab, tap **+**, and follow the [Docs.md](Docs.md) guide.
4. **Run and Observe** - Press the play button on your script, watch the live logs, and stop via floating controls anytime.

## Documentation

- [Docs.md](Docs.md) - Full scripting guide.
- [Finding UI Elements](Docs.md#finding-ui-elements) - Inspectors, selectors, and tips.
- [API Reference](Docs.md#api-reference) - Every context/helper exposed to scripts.
- [Examples](examples/) - Ready-to-run automation samples.
- [Changelog](CHANGELOG.md) - Version history.

## Example Script

```javascript
// ==DroidScript==
// @id              quick-demo
// @name            Quick Demo Script
// @description     Launch Instagram, scroll, and like a post.
// @author          tas33n
// @version         1.0.0
// @targetApp       com.instagram.android
// @url             https://github.com/tas33n/droidwright
// @created         2024-11-08
// ==/DroidScript==

function droidRun(ctx) {
  log("Starting Instagram automation");

  const targetPackage = typeof targetApp === "string" ? targetApp : "com.instagram.android";
  ctx.app.launch(targetPackage);
  ctx.device.sleep(3000);

  ctx.ui.waitFor({ text: "Home" }, 5000);

  const size = ctx.device.getScreenSize();
  ctx.ui.swipe(size.width / 2, size.height * 0.8, size.width / 2, size.height * 0.2, 500);
  ctx.device.sleep(2000);

  ctx.ui.tap({ id: "like_button" });
  ctx.device.sleep(1000);

  log("Automation completed");
  return { status: "ok", note: "Success" };
}
```

## Technologies

- Kotlin + Jetpack Compose UI
- Android Accessibility Service
- QuickJS runtime for executing scripts safely
- Material Design 3 for UI components
- Room Database for local script storage
- Kotlin Coroutines for concurrency

## Permissions

- `BIND_ACCESSIBILITY_SERVICE` - Required for UI automation.
- `SYSTEM_ALERT_WINDOW` - Enables floating controls.
- `INTERNET` - Needed only when AI or remote logging is enabled.
- Storage access - Optional, for importing/exporting scripts.

## Important Notes

- **Rate Limiting** - The runtime inserts delays to keep the UI responsive and avoid ANRs.
- **Safety First** - Scripts inherit sensible defaults (timeouts, retries, logging).
- **AI Integration** - Optional and off by default; requires explicit API key configuration.
- **Use Responsibly** - Respect each app's Terms of Service when automating flows.

## Privacy & Data Use

- Accessibility interactions stay on-device; DroidWright does not transmit UI hierarchies or event logs unless you explicitly export them.
- Logs are stored locally in the app sandbox and cleared when you reset the workspace.
- When AI features are enabled, only the prompt you provide plus high-level execution context is sent to Google Gemini through your API key. Disable AI support or remove the key to keep scripts fully offline.

## Troubleshooting

**Scripts not running**
1. Confirm the accessibility service is enabled.
2. Verify the target app is installed and in the foreground.
3. Review the in-app console or `adb logcat` for stack traces.
4. Reinstall the APK if permissions were revoked.

**Element not found**
1. Inspect the UI with Appium Inspector or Developer Assistant.
2. Ensure the element is visible (scroll if needed).
3. Try alternative selectors (resource ID, text, content description).
4. Use `ui.dumpTree()` for a full snapshot.

**ANR or slow execution**
- Increase `device.sleep` intervals between heavy actions.
- Close background apps to free memory.
- Split large flows into smaller scripts.

**Import issues**
- Confirm the file is UTF-8 encoded JavaScript.
- Ensure every script exports `droidRun(ctx)`.
- Avoid bundling external dependencies - keep scripts self-contained.

## Contributing

We welcome issues, feature requests, and pull requests.

1. Fork the repo and create a feature branch: `git checkout -b feature/my-change`.
2. Make your updates and keep commits focused.
3. Run `./gradlew lint testDebugUnitTest` (and `connectedDebugAndroidTest` when UI changes need coverage).
4. Document user-facing changes in `Docs.md` or `CHANGELOG.md` when applicable.
5. Open a Pull Request that explains the motivation, testing, and potential impact.

Please be respectful in discussions; this project follows the [GitHub Community Guidelines](https://docs.github.com/en/site-policy/github-terms/github-community-guidelines).

## Support & Security

- Bug reports and feature ideas -> [GitHub Issues](https://github.com/tas33n/droidwright/issues).
- General questions -> Discussions tab or the contact methods below.
- Security or sensitive reports -> email `farhanisteak84@gmail.com` with the subject line `SECURITY: DroidWright`.

## Sponsorship

If DroidWright saves you time, consider supporting the project:

- * Star the repository to help others find it.
- Report bugs, suggest features, or contribute code/docs.
- Financial support keeps the project maintained, documented, and community-driven.

### Donation Methods

**Crypto**
- Bitcoin (BTC): `14FmP4WQ61mBEqfGed1FAqdYnHDba2Fc4U`
- Ethereum (ETH - ERC20): `0x02537e13e5471cebf857fcd1f743d4af408af437`
- BNB (BEP20/BSC): `0x02537e13e5471cebf857fcd1f743d4af408af437`
- USDT (TRC20): `TDKQzgQni56eRgKLWanUey21UPSKToDQjJ`
- Binance Pay ID: `1150943446`

**Other**
- [GitHub Sponsors](https://github.com/sponsors/tas33n) (coming soon)

## License

This project is available under the [MIT License](LICENSE).

Copyright (c) 2025 tas33n

## Author

**tas33n**

- GitHub: [@tas33n](https://github.com/tas33n)
- Project: [DroidWright](https://github.com/tas33n/droidwright)

## Contact

- Email: [farhanisteak84@gmail.com](mailto:farhanisteak84@gmail.com)
- Telegram: [@lamb3rt](https://t.me/lamb3rt)

## Acknowledgments

- Android Accessibility Service engineering teams
- QuickJS project maintainers
- Material Design contributors
- Appium and Playwright communities for inspiration
- Everyone who tests, reports issues, or contributes code

## Resources

- [Script Writing Guide](Docs.md)
- [Finding UI Elements](Docs.md#finding-ui-elements)
- [Changelog](CHANGELOG.md)
- [Android Developers](https://developer.android.com)
- [Jetpack Compose Documentation](https://developer.android.com/jetpack/compose)
- [Material Design 3](https://m3.material.io)
- [Appium Inspector](https://github.com/appium/appium-inspector)

---

**Note**: DroidWright is currently in beta. APIs and UI are subject to change as we incorporate community feedback.
