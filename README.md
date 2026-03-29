<div align="center">

[![Meteorist Download][meteorist-banner-download]][meteorist-download]

[![License][shield-repo-license]][repo-license]
[![Release][shield-repo-latest]][repo-latest]
[![Downloads][shield-repo-releases]][repo-releases]
[![Stars][shield-repo-stargazers]][repo-stargazers]
[![Code Size][shield-repo-code-size]][repo-code-size]
[![Issues][shield-repo-issues]][repo-issues]
[![Pulls][shield-repo-pulls]][repo-pulls]
[![Forks][shield-repo-forks]][repo-forks]

[![Discord][vidget-discord]][discord]

[![Fabric][shield-fabric]][fabric]

</div>

## 📖 About

Add-on for [Meteor Client](https://meteorclient.com) that adds modules, commands, and other useful features for anarchy servers.

<div align="center">
    <a href="https://meteorist.pages.dev" target="_blank">
        <img align="center" height="64px" alt="Meteorist Download" src="website/button.svg">
    </a>
</div>

## 📦 Requirements

### Required
- **[Java](https://adoptium.net/temurin/releases)**: 21 or higher
- **[Fabric Loader](https://fabricmc.net/use/installer/)**
- **[Meteor Client](https://meteorclient.com/)**

### Optional
- **[Baritone](https://meteorclient.com/api/downloadBaritone)** - required for **ItemSucker** module (pathfinding to items and returning to origin)
- **[Minescript](https://minescript.net/downloads)** - required for **Minescript Integration** module (custom script execution)

## 🧩 Features

### Modules
A wide variety of modules for automation, combat, movement, rendering, and general quality of life improvements.

[**View all modules**](https://meteorist.pages.dev/#modules)

### Commands
Custom commands for running scheduled instructions, managing player data, inspecting NBT data, and more.

[**View all commands**](https://meteorist.pages.dev/#commands)

### Presets
Additional HUD presets for displaying various statistics.

[**View all presets**](https://meteorist.pages.dev/#presets)

## 📁 Project Structure
```
Meteorist/
├── src/
│   ├── main/
│   │   ├── java/zgoly/meteorist/
│   │   │   ├── commands/      # Custom commands
│   │   │   ├── devmodules/    # Development-only modules
│   │   │   ├── events/        # Event handlers
│   │   │   ├── gui/           # Custom GUI screens
│   │   │   ├── hud/           # HUD elements
│   │   │   ├── mixin/         # Mixin classes
│   │   │   ├── modules/       # Game modules
│   │   │   ├── settings/      # Configuration settings
│   │   │   ├── utils/         # Utility classes
│   │   │   └── Meteorist.java # Main entry point
│   │   └── resources/
│   │       ├── assets/        # Mod stuff (icons, etc.)
│   │       ├── fabric.mod.json
│   │       └── meteorist.mixins.json
│   └── dev/java/              # Development stuff
├── gradle/                    # Gradle wrapper stuff
└── website/                   # Website stuff (meteorist.pages.dev)
```

## 🛠️ Building from Source

### Prerequisites
- **[JDK](https://adoptium.net/temurin/releases)**: 21 or higher
- **[Git](https://git-scm.com/install/)**

### Steps
```bash
# Clone the repository
git clone https://github.com/Zgoly/Meteorist.git
cd Meteorist

# Build with Gradle
./gradlew build
```

The compiled JAR will be in `build/libs/`.

### Development Commands
```bash
# Run in development environment
./gradlew runClient

# Generate module/command/preset info JSON
./gradlew generateMeteoristInfo

# Clean build artifacts
./gradlew clean
```

## 🤝 Contributing
Contributions are welcome! Please feel free to submit a Pull Request.

- [Report a bug][report-bug]
- [Suggest a feature][suggest-feature]
- [Report a crash][report-crash]

## 📄 License
This project is licensed under the [MIT License][repo-license].

## 👥 Community
[![Join Discord][shield-discord-server]][discord]
[![Join Discord][shield-discord-members]][discord]

Join our [Discord server][discord] for support and discussions.

---

<div align="center">

<a href="https://github.com/Zgoly/Meteorist/stargazers">
    <picture>
        <source media="(prefers-color-scheme: dark)" srcset="https://bytecrank.com/nastyox/reporoster/php/stargazersSVG.php?theme=dark&user=Zgoly&repo=Meteorist">
        <source media="(prefers-color-scheme: light)" srcset="https://bytecrank.com/nastyox/reporoster/php/stargazersSVG.php?theme=light&user=Zgoly&repo=Meteorist">
        <img alt="Stargazers" src="https://bytecrank.com/nastyox/reporoster/php/stargazersSVG.php?theme=dark&user=Zgoly&repo=Meteorist">
    </picture>
</a>

<a href="https://www.star-history.com/?repos=zgoly%2Fmeteorist">
    <picture>
        <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/image?repos=zgoly/meteorist&theme=dark">
        <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/image?repos=zgoly/meteorist">
        <img alt="Star History Chart" src="https://api.star-history.com/image?repos=zgoly/meteorist">
    </picture>
</a>

</div>

[meteorist-banner-download]: website/banner.svg
[meteorist-button-download]: website/button.svg
[meteorist-download]: https://meteorist.pages.dev
[meteor-download]: https://meteorclient.com
[baritone-download]: https://meteorclient.com/api/downloadBaritone

[shield-repo-license]: https://img.shields.io/github/license/Zgoly/Meteorist?style=flat&labelColor=001932&color=001932
[repo-license]: https://github.com/Zgoly/Meteorist/blob/main/LICENSE

[shield-repo-latest]: https://img.shields.io/github/v/release/Zgoly/Meteorist?display_name=release&labelColor=001932&color=001932
[repo-latest]: https://github.com/Zgoly/Meteorist/releases/latest

[shield-repo-releases]: https://img.shields.io/github/downloads/Zgoly/Meteorist/total?labelColor=001932&color=001932
[repo-releases]: https://tooomm.github.io/github-release-stats/?username=Zgoly&repository=Meteorist

[shield-repo-stargazers]: https://img.shields.io/github/stars/Zgoly/Meteorist?style=flat&labelColor=001932&color=001932
[repo-stargazers]: https://github.com/Zgoly/Meteorist/stargazers

[shield-repo-code-size]: https://img.shields.io/github/languages/code-size/Zgoly/Meteorist?labelColor=001932&color=001932
[repo-code-size]: https://github.com/Zgoly/Meteorist/archive/refs/heads/main.zip

[shield-repo-issues]: https://img.shields.io/github/issues/Zgoly/Meteorist?labelColor=001932&color=001932
[repo-issues]: https://github.com/Zgoly/Meteorist/issues

[shield-repo-pulls]: https://img.shields.io/github/issues-pr/Zgoly/Meteorist?labelColor=001932&color=001932
[repo-pulls]: https://github.com/Zgoly/Meteorist/pulls

[shield-repo-forks]: https://img.shields.io/github/forks/Zgoly/Meteorist?style=flat&labelColor=001932&color=001932
[repo-forks]: https://github.com/Zgoly/Meteorist/network/members

[vidget-discord]: https://invidget.switchblade.xyz/y8fBWPNJFm
[discord]: https://dsc.gg/zgoly

[shield-fabric]: https://raw.githubusercontent.com/intergrav/devins-badges/v3/assets/cozy/supported/fabric_vector.svg
[fabric]: https://fabricmc.net/

[shield-discord-server]: https://img.shields.io/badge/dynamic/json?url=https%3A%2F%2Fdiscord.com%2Fapi%2Fv9%2Fguilds%2F1035620564133490809%2Fwidget.json&query=name&logo=discord&logoColor=white&label=Server&labelColor=5865F2&color=5865F2
[shield-discord-members]: https://img.shields.io/discord/1035620564133490809?label=%20&color=5865F2

[shield-contribute]: https://img.shields.io/badge/Contribute-00967d
[contribute]: https://github.com/Zgoly/Meteorist/pulls

[shield-suggest-feature]: https://img.shields.io/badge/Suggest%20a%20feature-00967d
[suggest-feature]: https://github.com/Zgoly/Meteorist/issues/new?assignees=&labels=enhancement&projects=&template=suggestion.yml

[shield-report-bug]: https://img.shields.io/badge/Report%20a%20bug-ff6600
[report-bug]: https://github.com/Zgoly/Meteorist/issues/new?assignees=&labels=bug&projects=&template=bug.yml

[shield-report-crash]: https://img.shields.io/badge/Report%20a%20crash-c83232
[report-crash]: https://github.com/Zgoly/Meteorist/issues/new?assignees=&labels=crash&projects=&template=crash.yml
