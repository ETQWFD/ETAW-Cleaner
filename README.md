# ETAW清理 (ETAW Cleaner)

> 简洁 · 真实 · 干净 —— 一款面向安卓设备的第三方应用扫描、卸载与残留深度清理工具。

[![Release](https://img.shields.io/badge/Release-v1.1.0-0FA893)](https://github.com/ETQWFD/ETAW-Cleaner/releases)
[![Platform](https://img.shields.io/badge/Platform-Android%207.0%2B-0FA893)](https://github.com/ETQWFD/ETAW-Cleaner)
[![License](https://img.shields.io/badge/License-MIT-0FA893)](LICENSE)

## 这是什么

ETAW清理是一款本地化的安卓清理工具，所有操作真实可验证：

- **第三方应用扫描**：扫描设备上全部第三方应用，列表展示应用名称、包名、安装时间与官方网站。
- **双重验证卸载**：点击应用 → 卸载询问 → 二次验证确认 → 真实卸载（Shizuku 授权下静默卸载，否则走系统确认通道）。
- **残留深度清理**：扫描并删除 /data 与 /sdcard 下与卸载应用相关的数据目录、缓存、OBB 等残留。
- **SHA-256 回收站**：卸载信息经 SHA-256 哈希指纹记录进本地回收站，保存安装时间、官网等完整信息，随时查找并直达官网重新下载。
- **主题与多语言**：跟随系统 / 浅色 / 深色三档主题；简体中文、English、繁體中文即时切换。
- **Shizuku 授权**：可选授权，获得系统级卸载与深度清理能力。
- **检查更新**：直连 GitHub Release 获取最新版本。

## 下载

- 官网：<https://etqwfd.github.io/ETAW-Cleaner/>
- GitHub Releases：<https://github.com/ETQWFD/ETAW-Cleaner/releases>

## 权限说明

| 权限 | 用途 |
| --- | --- |
| `QUERY_ALL_PACKAGES` | 枚举并扫描第三方应用 |
| `REQUEST_DELETE_PACKAGES` | 调起系统卸载确认（第二重验证） |
| `moe.shizuku.manager.permission.API_V23` | Shizuku 授权（可选，用于静默卸载与深度残留清理） |
| `INTERNET` | 检查更新 |

> 所有记录仅保存在本机数据库，不收集、不上传任何数据。

## Shizuku 授权方式

1. 安装 [Shizuku](https://shizuku.rikka.app) 应用并开启服务；
2. 或通过电脑执行 adb：
   ```bash
   adb shell sh /storage/emulated/0/Android/data/moe.shizuku.privileged.api/start.sh
   ```
3. 在 ETAW清理「设置 → Shizuku 授权」中点击授权。

未授权 Shizuku 时，卸载走系统确认通道（双重验证依然生效），外置存储残留可清理。

## 从源码构建

```bash
# 环境：JDK 17、Android SDK（platform 34、build-tools 34.0.0）
git clone https://github.com/ETQWFD/ETAW-Cleaner.git
cd ETAW-Cleaner
# 签名：在项目根目录准备 keystore.properties（storeFile/storePassword/keyAlias/keyPassword）
./gradlew assembleRelease
# 产物：app/build/outputs/apk/release/app-release.apk
```

## 开发者

- 开发者：etc
- 联系方式：<mailto:2416444244@qq.com>
- 致敬每一位开发者。

## License

[MIT](LICENSE) © 2026 ETAW
