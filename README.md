# AE2 Desktop Ping

Client-side addon for Applied Energistics 2 in GTNH 2.9. The server needs nothing.

When a craft started with **Start and follow** finishes, AE2 already tells your client (chat line, sound, toast).
This addon adds alerts that reach you when you're away from the game window or away from the PC.

Built against AE2 rv3-beta-1095-GTNH. Desktop alerts need Windows; the taskbar flash needs lwjgl3ify (Java 17+ GTNH).

## What happens when a followed craft finishes

| Alert | When |
| --- | --- |
| Taskbar flash | Every followed craft, while the game window isn't focused. Flashes until you click back in. |
| Windows notification | Same, as a toast with the GTNH logo (loaded from the pack's core mod at runtime). |
| Discord ping (you) | Only crafts started with **Start and follow** while the bell toggle was on. |
| Discord ping (@here) | Only crafts started with **Start and follow** while the team toggle was on. |

## The toggles

Two buttons on the crafting plan screen, one row above AE2's "save as image" and "terminal style" buttons:

- **Bell (ping me)**, above save as image: stays as you set it until the game closes. Starts off every launch.
- **Team (ping @here)**, above terminal style: only clickable while the bell is on. Off every time a plan screen opens, so it has to be
  switched on for each craft.

Both are greyed out until Discord is set up.

Every **Start and follow** records the choice for that item and amount, and a newer choice replaces an older one for
the same item and amount. A finished craft is matched by item and amount only, so this keeps a cancelled craft's
choice from applying to a later identical craft. A craft merged into one already running finishes with the combined
amount and doesn't ping.

## What is deliberately not alerted

- **Crafts that finished while you were offline.** The server replays those right after you log in; anything arriving
  in the first 20 seconds after joining is ignored. This addon is for being AFK, not offline.
- **Flags from earlier sessions.** Which crafts should ping is kept in memory only; closing the game forgets it.
- **Crafts started on another server.** A flag only matches finishes on the server it was set on.

## Config

In game: **Mods → AE2 Desktop Ping → Config**, **Mod Options** in the pause menu, or `/ae2ping config`.
Changes apply when you click Done. The file is `config/ae2desktopping.cfg`; after editing it by hand, `/ae2ping reload`.

| Key | Default | |
| --- | --- | --- |
| `desktop.flashTaskbar` | `true` | |
| `desktop.windowsNotification` | `true` | |
| `desktop.onlyWhenUnfocused` | `true` | Skip desktop alerts while the game has focus. |
| `discord.webhookUrl` | empty | Server Settings → Integrations → Webhooks → Copy Webhook URL. |
| `discord.userId` | empty | Settings → Advanced → Developer Mode, then right-click yourself → Copy User ID. |

The webhook URL is a secret: anyone who has it can post in that channel. Don't share your config folder with it inside.

## Commands

- `/ae2ping` or `/ae2ping status`: is Discord set up, how many crafts are waiting to ping, bell state.
- `/ae2ping config`: open the config screen.
- `/ae2ping reload`: re-read the config file after editing it by hand.
- `/ae2ping test`: desktop alert in 5 seconds (switch windows to see it) and a test message that pings only you.

## Safety

- Only https URLs on `discord.com`, `discordapp.com`, `ptb.discord.com` or `canary.discord.com` with the webhook path
  are accepted. Redirects are not followed. The URL is never written to the log.
- Each message sets Discord's `allowed_mentions` to exactly you (plus @here when that toggle was on), so text in item
  names can't ping anyone. Item names are also stripped of formatting codes, have markdown escaped and `@` defused.
- A message holds the item, amount and craft time. No player name, server address or coordinates.
- Only crafts you flagged can ping, each once, so a server can't make the addon spam your Discord.
- Posts run on one background thread with timeouts, at most one every 2 seconds and 16 queued.
- The Windows notification passes plain strings to the OS tray API. No shell or script is started.

## Singleplayer

Minecraft pauses a singleplayer world when the window loses focus, and the crafts pause with it.
Turn that off with **F3 + P** before going AFK.

## Build

```bash
./gradlew build
```

The jar is in `build/libs/` (the one without `-dev` / `-sources`).
