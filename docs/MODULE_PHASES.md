# Catalogue module phases

The Mod Menu's PvP catalogue lists 200+ entries. Most of them were previously badged and refused
with one shared reason ("no game data"), which was honest but coarse: it refused a clock for lack of
a native entity feed. The work below sorts the list by **the route each module needs**, implements
every route this build actually owns, and keeps an honest badge on the entries that genuinely need a
feed the build does not expose.

The single source of truth for "what does this module need" is
`core/mods/inbuilt/catalog/ModuleScope` plus the reachable table in
`core/mods/inbuilt/catalog/PvpModuleScopes`. `ModIds.requiresGameData` reads that classification, so
the badge, the grey-out and the refused toggle all come from one rule.

## Phase A — device input and launcher state (no game data at all)

Route: `ModuleScope.DEVICE_INPUT`. These read nothing from the game, so they work on every build.

| Shape | Modules |
| --- | --- |
| Readouts | `hud_clock`, `hud_keystrokes`, `hud_memory`, `hud_watermark`, `hud_combo_display`, `combat_combo_counter`, `combat_death_counter`, `combat_kdr_tracker`, `combat_streak_tracker`, `combat_kill_counter` |
| Actions (one-shot on enable) | `util_drop_all`, `util_chat_macro`, `util_command_bind`, `util_auto_gg`, `util_auto_tip` |
| Input-driven cooldown readouts | `combat_attack_cooldown`, `combat_attack_indicator`, `combat_crit_timer`, `combat_pearl_timer`, `combat_gapple_timer`, `combat_shield_timer`, `combat_hit_marker` |
| Sprint helpers | `combat_no_sprint_reset`, `move_sprint_swing`, `util_keep_sprint` |

Implementation: `CatalogModuleRuntime` (live behaviour), `CatalogReadoutOverlay` (the draggable
window), `CatalogModuleHost` (routes enable/disable by shape), `CatalogSessionCounters`,
`KeyStateTracker`, `ChatInjector`, `ActionTimers` (all pure and unit-tested).

The counters read the same signals the shipped modules already use — the Select Hit combo advance
for a landed hit, the local health feed for a death, and the Kill Effects credit heuristic for a
credited kill — so a readout cannot disagree with the module it mirrors. Chat and command macros go
through `ChatInjector` into the existing `PreloaderInput.onKeyEvent` path, the same one a real key
press takes.

## Phase B — the game options file

Route: `ModuleScope.GAME_OPTIONS`. Each writes a key in the instance's `minecraftpe/options.txt`,
which the game applies on its next load. This is a settings writer, not a live hook, and the scope
note says so.

`move_bob_off` (`bobView:0`), `util_fullbright` / `world_fullbright` (`gamma:1.0`),
`world_particle_off` (`particles:0`), `move_no_sprint_particle` / `move_sprint_particles`
(`particles:2`), `move_fov_boost` (`fov`). Implementation: `CatalogOptionsWriter`.

## Phase C — the local-player feed

Route: `ModuleScope.LOCAL_FEED`. Reads your own position, facing and health through the
already-installed native local-player feed (the `ClientInstance` local-player slot). It degrades to
an honest empty state when the feed is absent.

`hud_coordinates`, `hud_direction`, `hud_compass`, `hud_chunk_borders`, `hud_speedometer`,
`hud_health_display`, `move_position_display`, `move_speed_display`, `move_velocity_display`,
`move_fall_damage_warn`, `world_waypoints`, `world_death_waypoint`. Implementation:
`LocalPlayerReadouts` (pure) plus the readout plumbing from Phase A.

## Phase D — the peer feed

Route: `ModuleScope.PEER_FEED`. Reaches other players through the proximity-voice peer feed, which
needs no game hook at all. Vanilla players are invisible to it and the readout says so.

`combat_reach_circle` — the distance to the nearest peer. Implementation: `CatalogModuleRuntime`
reading `PeerPositions` plus the local-player position.

## Phase E — engine / render hooks (not reachable in this build)

Route: `ModuleScope.ENGINE_HOOK`. These act on the game's rendered frame or its entity/block/effect
list. Game classes export no symbols and no static vtable address yields a live `Actor*` on the
shipped builds, so a mod cannot read an entity, a block, an inventory or an effect. They stay badged
and their toggle is refused rather than pretending to work. This is the documented limitation, not a
missing implementation — see the "Native entity/camera feed" section of `AGENTS.md`.

## Phase F — server decides / ban risk (not shipped on purpose)

Route: `ModuleScope.SERVER_DECIDED`. Reach, knockback, crits, TPS and inventory automation are
server-arbitrated or a ban risk. The classification is read from the catalogue's own scope note, so
no second hand-maintained list exists. They stay badged with the note that says which.

## Tests

Pure logic, no mocks, no device:

- `LocalPlayerReadoutsTest` — compass anchors, chunk flooring, the "no data" nulls
- `ActionTimersTest` — countdown, re-arm, honest empty state
- `CatalogSessionCountersTest` — combo reset on death, best-streak retention, K/D guard
- `PvpModuleScopesTest` — the reachable/refused classification
- `ChatInjectorTest` — open-with-no-character, slash handling, key codes
- `KeyStateTrackerTest` — hold/release and reset

Run `./gradlew :app:testDebugUnitTest`.
