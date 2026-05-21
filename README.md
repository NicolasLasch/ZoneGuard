# 🛡️ ZoneGuard

> **Simple, powerful region protection — built for Paper & Folia.**

ZoneGuard lets you define protected zones on your Minecraft server and control exactly what can happen inside them — block building, PvP, mob spawning, fire spread, interactions, and much more. It supports both **cuboid** and **polygon** regions, a granular **flag system**, and a beautiful **particle visualizer** to see your zones in real time.

Zero dependencies. One command. Fully async storage.

---

## ✨ Features

- 🔷 **Cuboid & Polygon regions** — protect rectangular areas or draw any custom shape
- 🏳️ **13 protection flags** — fine-tune exactly what is allowed or denied in each zone
- 🌍 **Global world flags** — apply rules server-wide or per-world without creating a region
- ✨ **Particle visualizer** — see your selection and zones drawn live with colored dust particles
- ⚡ **Folia-compatible** — fully async-safe, works on both Paper and Folia servers
- 💾 **Async YAML storage** — saves in the background, never lags your server
- 🎮 **Creative bypass** — admins in Creative mode bypass all protections automatically

---

## 📦 Installation

1. Download `ZoneGuard.jar` and place it in your server's `plugins/` folder.
2. Restart (or start) your server.
3. The plugin will create a `plugins/ZoneGuard/regions.yml` file automatically.

**Requirements:**
- Paper or Folia **1.21+**
- Java 21+

---

## 🔑 Permissions

| Permission | Description | Default |
|---|---|---|
| `zoneguard.admin` | Full access to all ZoneGuard commands | OP |

> Admins in **Creative mode** automatically bypass all zone protections.

---

## 🕹️ Commands

All commands use the `/zg` alias (or `/zoneguard`).

| Command | Description |
|---|---|
| `/zg edit` | Start a **cuboid** selection with the wooden axe |
| `/zg edit any` | Start a **polygon** selection with the wooden axe |
| `/zg define <name>` | Create a zone from your current selection |
| `/zg list` | List all defined zones |
| `/zg info <zone>` | Show details and flags for a zone |
| `/zg see <zone>` | Visualize a zone with particles for a few seconds |
| `/zg flag <zone\|global\|global:world> <flag> <allow\|deny\|unset>` | Set a protection flag |
| `/zg flaglist` | List all available flags and their descriptions |
| `/zg delete <zone>` | Delete a zone |
| `/zg reload` | Reload zones from disk |
| `/zg save` | Trigger an async save |

---

## 🗺️ Creating a Zone — Step by Step

### Cuboid Region (rectangular box)

1. Hold a **Wooden Axe** in your hand.
2. Run `/zg edit` to start a cuboid selection.
3. **Left-click** a block to set **Position 1**.
4. **Right-click** a block to set **Position 2**.
5. Run `/zg define <name>` to save the zone.

> The zone automatically spans **full world height** (from `minHeight` to `maxHeight`).

### Polygon Region (custom shape)

1. Hold a **Wooden Axe** in your hand.
2. Run `/zg edit any` to start a polygon selection.
3. **Right-click** blocks to add polygon points (minimum 3).
4. **Left-click** any block to close and finalize the polygon.
5. Run `/zg define <name>` to save the zone.

---

## 🏳️ Flags

Flags control what is **allowed** or **denied** inside a zone. Each flag can be set to:

| State | Aliases | Effect |
|---|---|---|
| `allow` | `true`, `yes`, `on` | Explicitly **permits** the action |
| `deny` | `false`, `no`, `off` | Explicitly **blocks** the action |
| `unset` | `default`, `remove` | **Removes** the flag (falls back to global or server default) |

### Available Flags

| Flag | Description |
|---|---|
| `block-break` | Allows or blocks block breaking |
| `block-place` | Allows or blocks block placing |
| `interact` | Allows or blocks interaction with blocks and entities |
| `sign` | Allows or blocks editing and dyeing signs |
| `explosions` | Allows or blocks explosion block damage |
| `fire-spread` | Allows or blocks fire ignition and spreading |
| `monster-spawn` | Allows or blocks monster spawning |
| `animal-spawn` | Allows or blocks animal spawning |
| `hunger` | Allows or blocks player hunger loss |
| `damage` | Allows or blocks all general damage |
| `fall-damage` | Allows or blocks fall damage specifically |
| `pve` | Allows or blocks entity damage against players |
| `pvp` | Allows or blocks player-vs-player combat |

### Examples

```
# Block all building in a zone named "spawn"
/zg flag spawn block-break deny
/zg flag spawn block-place deny

# Disable PvP globally in the current world
/zg flag global pvp deny

# Disable PvP globally in a specific world named "survival"
/zg flag global:survival pvp deny

# Allow fire spread inside a volcano zone
/zg flag volcano fire-spread allow

# Remove a flag (revert to default behaviour)
/zg flag spawn block-break unset
```

---

## 🌍 Global Flags

You can apply flags **world-wide** without creating a region:

```
# Apply to the world you're currently standing in
/zg flag global <flag> <allow|deny|unset>

# Apply to any specific world by name
/zg flag global:<worldname> <flag> <allow|deny|unset>
```

**Resolution order:** Region flags take priority over global world flags.

---

## 🎨 Particle Visualizer

The selection tool draws your region outline in real time using colored dust particles:

| Color | Meaning |
|---|---|
| 🔵 Cyan | Cuboid selection outline |
| 🟡 Yellow | Polygon selection edges |
| 🟢 Green | Corner or vertex points |

Use `/zg see <zone>` to preview any saved region for a few seconds.

---

## 💾 Storage

Zones are stored in `plugins/ZoneGuard/regions.yml`. Saves are always performed **asynchronously** on a dedicated background thread, so your server is never blocked. On shutdown, ZoneGuard waits up to 5 seconds for any pending save to complete before force-stopping.

---

## 🤝 Contributing

Pull requests and issues are welcome! If you find a bug or want a new flag, open an issue on the GitHub repository.

---

## 📄 License

This project is released as **free and open source**. Feel free to use, modify, and distribute it.

---

*Made with ❤️ by TheSpattt*
