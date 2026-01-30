# EuphoriaParties - Quick Admin Reference

## 🔧 Essential Admin Commands

### Health & Monitoring
```
/partyadmin health          # Check plugin status, memory usage, and performance
/partyadmin list           # List all active parties
/partyadmin info <player>  # Detailed info about a player's party
```

### Configuration
```
/partyadmin reload         # Reload configuration without restarting server
```

### Party Management
```
/partyadmin disband <player>    # Force disband a party
/partyadmin teleport <player>   # Teleport to a party's home
```

---

## ⚡ Performance Optimization Tips

### 1. Tune Cache Settings
**For high-traffic servers:**
```yaml
performance:
  cache-ttl: 60000          # Longer cache = less lookups
  max-cache-size: 2000      # More cache space
  leaderboard-cache-ttl: 10000
```

### 2. Optimize Update Intervals
**Reduce overhead on busy servers:**
```yaml
party:
  marker-update-interval: 10   # Higher = less frequent updates
hud:
  coordinates:
    update-interval: 40        # 2 seconds instead of 1
```

### 3. Enable All Performance Features
```yaml
performance:
  async-save: true
  cache-party-lookups: true
  optimize-markers: true
  skip-offline-party-tasks: true
  batch-achievement-checks: true
```

---

## 🛡️ Security Best Practices

### Anti-Abuse Settings
```yaml
security:
  command-cooldown: 3           # Prevent command spam
  teleport-cooldown: 30         # Prevent teleport abuse
  invite-cooldown: 5            # Prevent invite spam
  require-confirmation-disband: true
  prevent-invite-spam: true
```

### Safety Features
```yaml
security:
  safe-teleport: true           # Check landing safety
  max-teleport-distance: 10000  # Prevent cross-world exploits
```

---

## 🐛 Troubleshooting

### Enable Debug Mode
```yaml
debug:
  enabled: true
  log-commands: true      # See all commands executed
  log-events: true        # See all party events
  log-performance: true   # See performance metrics
```

### Common Issues & Fixes

**Problem: High memory usage**
```
1. Run: /partyadmin health
2. Check memory percentage
3. If >80%, reduce max-cache-size in config
4. Run: /partyadmin reload
```

**Problem: Lag spikes**
```
1. Increase marker-update-interval to 15-20
2. Increase HUD update-interval to 40
3. Enable optimize-markers
4. Reload config
```

**Problem: Data corruption**
```
1. Check plugins/EuphoriaPartyPlugin/parties.json.backup
2. Copy backup to parties.json
3. Restart server
Note: Plugin auto-creates backups before each save!
```

**Problem: Parties not saving**
```
1. Check console for errors
2. Verify file permissions on plugin folder
3. Check async-save setting
4. Enable debug.enabled for detailed logs
```

---

## 📊 Health Check Interpretation

When you run `/partyadmin health`, here's what to look for:

**Memory Usage**
- `< 70%` - ✅ Healthy
- `70-85%` - ⚠️ Monitor closely
- `> 85%` - 🔴 Action needed

**Active Parties**
- Sudden drops may indicate crashes
- Gradual growth is normal

**Cache Status**
- All should show ✓ for optimal performance
- If ✗, check config and reload

---

## 🔄 Config Reload Workflow

1. Edit `config.yml` file
2. Save changes
3. Run `/partyadmin reload` in-game
4. Check console for "Configuration reloaded successfully"
5. Verify changes with `/partyadmin health`

**Note:** No server restart required!

---

## 💾 Backup Strategy

The plugin automatically:
- ✅ Creates `.backup` file before each save
- ✅ Saves every 5 minutes (configurable)
- ✅ Uses atomic writes to prevent corruption

**Manual backup recommended:**
- Before major updates
- Before server migrations
- Weekly for large servers

**Location:** `plugins/EuphoriaPartyPlugin/parties.json`

---

## 📈 Performance Monitoring Schedule

**Daily:**
- Check `/partyadmin health` during peak hours
- Monitor memory percentage

**Weekly:**
- Review debug logs if enabled
- Check backup file integrity
- Optimize settings based on load

**Monthly:**
- Review and tune cache settings
- Update configurations for seasonal player counts

---

## 🎯 Recommended Settings by Server Size

### Small (1-20 players)
```yaml
party:
  max-members: 8
  marker-update-interval: 5
performance:
  cache-ttl: 30000
  max-cache-size: 500
  auto-save-interval: 6000
```

### Medium (20-100 players)
```yaml
party:
  max-members: 10
  marker-update-interval: 8
performance:
  cache-ttl: 45000
  max-cache-size: 1000
  auto-save-interval: 6000
```

### Large (100+ players)
```yaml
party:
  max-members: 12
  marker-update-interval: 15
performance:
  cache-ttl: 60000
  max-cache-size: 2000
  auto-save-interval: 12000
```

---

## 🔐 Permission Nodes

```
euphoria.party.admin  - Access to all /partyadmin commands
euphoria.party.use    - Access to basic /party commands
```

---

## 📞 Quick Support Checklist

When reporting issues, provide:
1. Output of `/partyadmin health`
2. Relevant section of config.yml
3. Console errors (if any)
4. Server version and player count
5. Steps to reproduce

---

## 🚀 Optimization Checklist

- [ ] Set appropriate cache-ttl for your server size
- [ ] Enable all performance features
- [ ] Set reasonable update intervals
- [ ] Configure security cooldowns
- [ ] Test backup recovery
- [ ] Set up monitoring schedule
- [ ] Document custom settings
- [ ] Enable debug mode for testing
- [ ] Run /partyadmin health regularly
- [ ] Keep backup copy of working config

---

**Remember:** After any config changes, always run `/partyadmin reload`!
