<div align="center">
  <h1>Meteorist</h1>
  <p>Addon for Meteor Client ☄️.</p>
  <img alt="Minecraft" src="https://img.shields.io/badge/Minecraft-1.19-blue?logo=hackthebox&logoColor=white">
  <img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/Zgoly/Meteorist?color=green&logo=verizon&logoColor=white">
  <img alt="GitHub downloads" src="https://img.shields.io/github/downloads/Zgoly/Meteorist/total?color=purple&logo=github">
  <img alt="GitHub Repo stars" src="https://img.shields.io/github/stars/zgoly/meteorist?color=gold&logo=apachespark&logoColor=white">
  <img alt="GitHub code size in bytes" src="https://img.shields.io/github/languages/code-size/zgoly/meteorist?style=flat">
  <img alt="GitHub issues" src="https://img.shields.io/github/issues/zgoly/meteorist?style=flat">
  <img src="https://img.shields.io/badge/Tacos-Tasty-blue">
</div>

## ℹ️ Information
```
When update to 1.19.x?

Update will be released when useless reporting system and meteor finally released.
Current version - 1.19
Sorry.
```

This addon adds many new features to the [Meteor Client](https://meteorclient.com/). You can find a list of all available features below.

If you want to add or suggest any feature, then write [here](https://github.com/Zgoly/Meteorist/issues/new?assignees=&labels=enhancement&template=feature_request.yml&title=%5BSuggestion%5D+). Also, if you find any bug or have any problem check "Problems" below, and if it doesn't help write [here](https://github.com/Zgoly/Meteorist/issues/new?assignees=&labels=bug&template=bug.yml&title=%5BBug%5D+).

## ⬇️ Download
1. Click **[here to download](https://github.com/zgoly/meteorist/releases/latest/download/meteorist.jar)**
2. Move `meteorist.jar` in `.minecraft/mods` folder

Don't forget to download **[meteor dev build](https://meteorclient.com/download?devBuild=latest)**

## ⁉️ Problems
>*I can't start minecraft, fabric gives me an error:*
```
Mod resolution encountered an incompatible mod set!
  A potential solution has been determined:
    Replace mod 'Baritone' (baritone) x.xx-SNAPSHOT with any version that is compatible with:
      minecraft x.xx`
```
1. Try updating [Meteorist](https://github.com/zgoly/meteorist/releases/latest/download/meteorist.jar), [Fabric](https://fabricmc.net/use/installer) and [Meteor Client](https://meteorclient.com/download?devBuild=latest) to latest versions.
2. If a Minecraft update is available, update it.
3. Try install latest version of [Baritone](https://github.com/cabaletta/baritone/actions).
    1. Find latest workflow containing "Artifacts" and click on "Artifacts" file to download it.
    2. After downloading, open it as archive and drag and drop version you need to `.minecraft/mods` folder.

## 🤓 Modules
|№|Module|Function|Info|
|:----:|--|--|--|
|1|`Auto Feed`|Writes command in chat when hunger level is low.|*Default command is /feed, but you can change it.*|
|2|`Auto Floor`|Put blocks under you like "scaffold" module, but in range.|*Requires improvement.*|
|3|`Auto Heal`|Writes command in chat when health level is low.|*Default command is /heal, but you can change it.*|
|4|`Auto Leave`|Automatically leaves if player in range.|*Meteor has Auto Log, but this module has different settings that are better for non-anarchy servers.*|
|5|`Auto Light`|Shows best place to place light source block.|*Can also place for you. May not work on some servers.*|
|6|`Auto Login`|Automatically logs in your account.|**[read important info here](https://github.com/Zgoly/Meteorist/wiki/Meteorist-wiki#auto-login)**|
|7|`Container Cleaner`|Throw items from container.|*May not work on some servers.*|
|8|`High Jump`|Makes you jump higher than normal.|*Uses many jumps to simulate a big jump.*|
|9|`Item Sucker`|Sucks up all items on the ground.|*This module uses baritone, so don't forget to stop it if it works.*|
|10|`Jump Flight`|Flight that using jumps for fly.|*Same as Speed + AirJump, but speed can be changed using scroll wheel.*|
|11|`New Velocity`|Velocity that can bypass some anti-cheats.||
|12|`Slot Click`|Automatically clicks on slot.|*This works on any storage that has slots.*|
|13|`ZKillaura`|Killaura that only attacks when you jump.|*Requires improvement.*|

## 📄 Commands
|№|Command|Function|Info|
|:----:|--|--|--|
|1|`.coords`|Copies your coordinates to the clipboard.|*Also command `.coords share_in_chat` will share your coordinates in chat.*|
