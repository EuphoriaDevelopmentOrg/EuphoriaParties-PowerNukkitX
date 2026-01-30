# Euphoria Party Plugin

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen)]()
[![PowerNukkitX](https://img.shields.io/badge/PowerNukkitX-2.0.0-blue)]()
[![Java](https://img.shields.io/badge/Java-21-orange)]()
[![License](https://img.shields.io/badge/license-MIT-blue)]()

A comprehensive party system for PowerNukkitX with visual markers, teleportation, HUD features, and advanced management tools.

## 🌟 Features

### Party System
- **Create and manage parties** - Create your own party and invite friends
- **Party roles** - Leader has special permissions (invite, kick, set home)
- **Member limit** - Configurable maximum party size (default: 8)
- **Visual markers** - See heart particles above party members' heads (works at long distances!)
- **Smart tracking** - Markers only appear for party members in the same world
- **Party chat** - Private chat channel with @ prefix
- **Friendly fire protection** - Prevent accidental damage to party members
- **Party buffs** - Receive buffs when near party members
- **Achievements system** - Track party milestones
- **Safety checks** - Validates teleport locations for safety
- **Cooldowns** - Configurable teleport cooldowns to prevent abuse
- **Cross-world support** - Smart handling of different worlds

### Administrative Features
- **Health monitoring** - Check plugin status and memory usage with `/partyadmin health`
- **Config reload** - Reload configuration without restarting with `/partyadmin reload`
- **Party management** - Force disband, view info, teleport to party homes
- **Debug logging** - Comprehensive logging for troubleshooting
- **Automatic backups** - Data is backed up before each save

### Teleportation
- **Party home** - Leaders can set a party home location
- **Quick teleport** - `/party home` to teleport to your party's home base
- **Convenient** - Great for coordinating with your team

### HUD Features
- **Coordinate display** - Toggle on-screen coordinates showing X, Y, Z position
- **Compass display** - See your current direction (N, S, E, W, NE, NW, SE, SW)
- **Action bar integration** - Non-intrusive display using Minecraft's action bar
- **Fully configurable** - Customize colors, formats, and update rates

## 📋 Commands

### Party Commands (`/party`)
- `/party create` - Create a new party
- `/party invite <player>` - Invite a player to your party (leader only)
- `/party accept` - Accept a pending party invite
- `/party leave` - Leave your current party
- `/party kick <player>` - Kick a player from your party (leader only)
- `/party list` - List all party members and their status
- `/party sethome` - Set the party home location (leader only)
- `/party home` - Teleport to the party home
- `/party warp` - Alternative to party home
- `/party info` - View detailed party information
- `/party help` - Show all party commands

### Admin Commands (`/partyadmin` or `/pa`)
- `/partyadmin list` - List all active parties
- `/partyadmin info <player>` - View detailed party information
- `/partyadmin disband <player>` - Force disband a party
- `/partyadmin teleport <player>` - Teleport to a party's home
- `/partyadmin reload` - Reload the configuration
- `/partyadmin health` - Check plugin health and performance metrics

### HUD Commands
- `/coordinates` (or `/coords`) - Toggle coordinate display
- `/compass` - Toggle compass display

## 🔐 Permissions

### Party Permissions
- `euphoria.party.use` - Use basic party commands
- `euphoria.party.create` - Create a party
- `euphoria.party.invite` - Invite players
- `euphoria.party.kick` - Kick players
- `euphoria.party.sethome` - Set party home
- `euphoria.party.admin` - Access to all admin commands

### HUD Permissions
- `euphoria.hud.coordinates` - Toggle coordinate display
- `euphoria.hud.compass` - Toggle compass display

### Admin Permissions
- `euphoria.party.admin` - All admin commands
- `euphoria.*` - All plugin permissions (default: op)

## ⚙️ Configuration

The plugin includes a fully customizable `config.yml` with extensive options:

### Basic Settings
```yaml
party:
  max-members: 8
  teleport-enabled: true
  prevent-friendly-fire: true
  party-chat-enabled: true
  party-chat-prefix: "@"
```

### Performance Settings
```yaml
performance:
  async-save: true
  cache-party-lookups: true
  cache-ttl: 30000
  max-cache-size: 1000
  optimize-markers: true
  backup-on-save: true
```

### Security Settings
```yaml
security:
  command-cooldown: 3
  teleport-cooldown: 30
  safe-teleport: true
  prevent-invite-spam: true
```

### Debug Settings
```yaml
debug:
  enabled: false
  log-commands: false
  log-events: false
  log-performance: false
```

For complete configuration options, see [config.yml](src/main/resources/config.yml) or the [Admin Guide](ADMIN_GUIDE.md).

## 🚀 Installation

### For Server Owners

1. **Download** the latest release from the [Releases](../../releases) page
2. **Place** the JAR file in your PowerNukkitX server's `plugins/` folder
3. **Restart** the server
4. **Configure** the plugin in `plugins/EuphoriaPartyPlugin/config.yml`
5. **Optional**: Review the [Admin Guide](ADMIN_GUIDE.md) for advanced configuration

### From Source

Requirements:
- Java 21 or higher
- Maven 3.6+

```bash
# Clone the repository
git clone https://github.com/RepGraphics/EuphoriaParties-PowerNukkitX.git
cd EuphoriaParties

# Build the plugin
mvn clean package

# The compiled JAR will be in the target/ directory
```

## 📚 Documentation

- **[Admin Guide](ADMIN_GUIDE.md)** - Comprehensive guide for server administrators
- **[Configuration Reference](src/main/resources/config.yml)** - Full configuration options

## 🔧 Development

This plugin is built for **PowerNukkitX 2.0.0** (Minecraft Bedrock Edition).

### Tech Stack
- **Java 21** - Modern Java features
- **Maven** - Build automation
- **PowerNukkitX API** - Server platform
- **Gson** - JSON serialization
- **PlaceholderAPI** - Optional integration

### Project Structure
```
src/main/java/com/euphoria/party/
├── EuphoriaPartyPlugin.java      # Main plugin class
├── command/                       # Command handlers
│   ├── PartyCommand.java
│   ├── PartyAdminCommand.java
│   ├── CoordinatesCommand.java
│   └── CompassCommand.java
├── listener/                      # Event listeners
│   ├── PlayerListener.java
│   ├── PartyEventListener.java
│   ├── PartyRespawnListener.java
│   └── PartyTabListListener.java
├── manager/                       # Core managers
│   ├── PartyManager.java         # Party logic & data
│   ├── HUDManager.java           # HUD display
│   ├── PartyBuffManager.java     # Buff system
│   ├── PartyAchievementManager.java
│   ├── PartyScoreboardManager.java
│   └── PartyLeaderboardManager.java
├── model/                         # Data models
│   ├── Party.java
│   ├── PartyRole.java
│   └── PartyAchievement.java
├── storage/                       # Data persistence
│   └── PartyStorage.java
├── util/                          # Utilities
│   ├── Cache.java
│   ├── DebugLogger.java
│   └── HealthCheck.java
└── integration/                   # External integrations
    └── PartyPlaceholders.java
```

### Key Features Implementation

**Performance Optimizations:**
- Caching system with TTL and size limits
- Async data saving
- Batched particle rendering
- Optimized marker updates (movement threshold)
- Memory cleanup tasks

**Stability Enhancements:**
- Null safety checks throughout
- Automatic backup system
- Graceful shutdown handling
- Data corruption recovery
- Try-catch error boundaries

**Administrative Tools:**
- Health monitoring system
- Config hot-reload
- Debug logging framework
- Performance metrics tracking

## 🤝 Contributing

We welcome contributions from the community! Here's how you can help:

### Ways to Contribute

1. **🐛 Report Bugs**
   - Check existing issues first
   - Provide detailed reproduction steps
   - Include server version, plugin version, and logs
   - Use the bug report template

2. **💡 Suggest Features**
   - Open an issue with the feature request template
   - Explain the use case and benefits
   - Discuss implementation approach

3. **📝 Improve Documentation**
   - Fix typos or unclear explanations
   - Add examples or tutorials
   - Translate documentation

4. **💻 Submit Code**
   - Fork the repository
   - Create a feature branch (`git checkout -b feature/AmazingFeature`)
   - Follow the code style guidelines
   - Write clear commit messages
   - Add tests if applicable
   - Submit a pull request

### Development Setup

```bash
# Fork and clone the repository
git clone https://github.com/RepGraphics/EuphoriaParties-PowerNukkitX.git
cd EuphoriaParties

# Build the project
mvn clean package

# Run tests (when available)
mvn test

# Install to local Maven repository
mvn install
```

### Code Style Guidelines

- **Java Version**: Java 21
- **Indentation**: 4 spaces (no tabs)
- **Naming**: 
  - Classes: `PascalCase`
  - Methods: `camelCase`
  - Constants: `UPPER_SNAKE_CASE`
- **Comments**: JavaDoc for public methods
- **Null Safety**: Always validate inputs
- **Error Handling**: Use try-catch with meaningful logging

### Pull Request Process

1. **Update Documentation** - If you add/change features
2. **Test Thoroughly** - Verify your changes work
3. **Follow Conventions** - Match existing code style
4. **Describe Changes** - Clear PR description with examples
5. **Link Issues** - Reference related issues
6. **Be Responsive** - Address review feedback promptly

### Testing Checklist

Before submitting a PR, verify:
- [ ] Plugin compiles without errors
- [ ] No new warnings introduced
- [ ] Tested on PowerNukkitX server
- [ ] Config changes documented
- [ ] No performance regressions
- [ ] Backward compatibility maintained

## 🐛 Reporting Issues

When reporting bugs, please include:

1. **Plugin Version** - Check with `/version EuphoriaPartyPlugin`
2. **Server Version** - PowerNukkitX version
3. **Java Version** - `java -version` output
4. **Steps to Reproduce** - Clear, numbered steps
5. **Expected Behavior** - What should happen
6. **Actual Behavior** - What actually happens
7. **Logs** - Relevant console output or crash logs
8. **Config** - Your `config.yml` (remove sensitive data)

### Community Requests
Have an idea? [Open an issue](../../issues/new) with the `enhancement` label!

## 💬 Support

- **Issues**: [GitHub Issues](../../issues)
- **Discussions**: [GitHub Discussions](../../discussions)

## 📜 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- **PowerNukkitX Team** - For the amazing server platform
- **Contributors** - Everyone who has contributed code, ideas, or bug reports
- **Community** - Server owners and players who use and test the plugin

## ⭐ Star History

If you find this plugin useful, please consider giving it a star!

---

<div align="center">

**Made with ❤️ for the PowerNukkitX community**

[Report Bug](../../issues) · [Request Feature](../../issues) · [Contribute](#-contributing)

</div>
