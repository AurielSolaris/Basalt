# Changelog

All notable changes to Basalt are documented here.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Planned

- Widgets — the reason the project exists. A modular renderer where a new
  style is a new configuration, not a rewrite
- The 7×13 hero glyph set and the 3×5 micro set
- World clock board (the city dataset exists; the UI does not)
- Bedtime wind-down notifications
- Overnight Doze soak testing across devices and OEM skins

## [0.2.0] — 2026-08-26

Themes, and a test suite under the alarm engine. The palette stops being a
constant.

### Added

- **Seven themes**, chosen in **Settings → THEME** and listed alphabetically:
  - **Basalt** — cold grey stone and an arc lamp. The energised colour goes
    *colder* than the structural one rather than warmer, because in a scheme
    with no hue to spare that is the only way "this is running" stays distinct
    from "this is chrome"
  - **BREM** — *Blood Red Eldritch Machinery.* Mechanisms built from the blood
    and organs of things that should not have had either. The roles are
    separated by temperature, because a red scheme leaves the alert colour
    nowhere else to go
  - **Copper** — the default. Oxidised copper on iron, lit from inside
  - **Latte** — Catppuccin's light flavour, and the harder of the two to map
    honestly: Latte's accents are tuned to sit beside its base, not to be read
    on it, and Basalt puts the accent colour into digits
  - **Mocha** — Catppuccin's dark flavour. The greys map cleanly; the accents
    were chosen for Basalt's roles rather than for fidelity
  - **Quartz** — milky stone in daylight, and a genuine light theme rather than
    an inversion. "Hotter" means *darker* here: on a pale ground contrast is
    bought by going down, not up
  - **Solar** — Nord's Polar Night and Snow Storm greys with the Frost blues
    replaced by a soft yellow pastel. A slate room with one warm lamp in it

  Mocha, Latte and Solar are **adaptations, not ports**. Both source projects
  are MIT-licensed and neither endorses this app; see [`NOTICES.MD`](NOTICES.MD)
  for exactly which values were changed and why
- `ClockFormat.seconds` — the seconds field on its own, always two padded
  digits
- `ThemePicker` — shows each theme as a swatch drawn in that theme's *own*
  colours, in the proportions the app uses them. A list of seven words would tell
  the user nothing; "BREM" is not a colour anyone can picture
- `BasaltThemeHost` — installs the palette and drags the window with it: the
  window background, which the system paints before the first frame, and the
  status and navigation bar icons, which the OS draws and has to be told the
  polarity of
- `BasaltColors.isLight`, for the two things that cannot be expressed in tokens
  alone — the system bar icons, and the bezel's specular edge
- A synchronous mirror of the theme id in SharedPreferences, so the alarm screen
  and the window background never spend a frame in the wrong palette on a cold
  start. DataStore stays authoritative; the mirror is a cache whose worst case
  is one frame
- `AlarmTransitions` — the alarm lifecycle decisions, extracted from the
  Android-bound classes so they can be tested without a device
- **137 JVM unit tests**, covering next-occurrence walking, the repeat mask,
  both duration formats, 12/24-hour formatting, daylight saving in both
  directions, timer and stopwatch arithmetic across pauses and process gaps, the
  sleep window's midnight wrap, the glyph fold and layout measurement, every
  palette's contrast per role, and the alarm lifecycle decisions
- The Room schema is exported and committed, so migrations are writable

### Changed

- `BasaltColors.Forge` is now `BasaltColors.Copper`, and the palette is reached
  through `BasaltThemes` rather than as a constant
- Section headings are brighter in every theme. Copper's `bronze` moved from
  `#7A5230` to `#8A5E36`: at the old value it sat at 2.9:1 against the ground,
  which is under the large-text bar
- `BezelPanel` swaps its specular and shadow edges on a light ground. "Bright on
  top" is not the rule; the rule is that the lit edge faces the light, and on
  Quartz the ground is already brighter than any highlight
- Dismissing, being missed and skipping now go through one path. They differ in
  what the user is told and what is cancelled, not in the schedule they leave
  behind, and pretending otherwise is how a skipped morning disables a repeating
  alarm

### Fixed — the display settings were not wired up

- **`SHOW SECONDS` did nothing.** The setting existed, persisted and had a
  caption warning that it redraws every second; the clock screen never read it.
  Seconds now appear in a small column beside the hours and minutes rather than
  inline with them — at the readout's cell size an inline `HH:MM:SS` is
  forty-seven cells wide and does not fit on a phone, and a field that changes
  every second should not be able to reflow the fields that do not
- **`24 HOUR CLOCK` did nothing either**, for the same reason: `ClockScreen`
  took `use24Hour` as a parameter with a hardcoded default and was never passed
  anything. It now reads real settings
- The clock's tick loop aligns to the second when seconds are shown and to the
  minute when they are not, instead of always waking once a minute

### Removed

- `fallbackToDestructiveMigration()`. A schema change with no matching migration
  now throws on open rather than silently deleting the user's alarms — a crash
  is a bug report, a wiped alarm list is a missed morning nobody can explain

### Fixed — documentation

- The README described timer deadlines as being on the monotonic clock. They are
  on the wall clock, and the reason is the whole point: `elapsedRealtime()`
  restarts at zero on reboot

## [0.1.0] — 2026-08-26

First release. A recreation of the AOSP Clock in Kotlin and Jetpack Compose,
with no Material Design anywhere. The goal of this version is reliability, not
personality.

### Added — foundation

- Multi-module structure: `:app`, `:core:{design,dotmatrix,time,data,notify}`,
  `:feature:{alarm,clock,timer,stopwatch,bedtime,settings}`, `:widget`
- `:core:dotmatrix` — hand-authored 5×7 glyph set written as ASCII pictures and
  folded to bitmasks; `DotMatrix` / `DotMatrixText` canvas renderers; round and
  square cell shapes. Unlit cells are drawn, not skipped
- `:core:design` — eleven palette tokens, `BasaltTheme`, `BezelPanel`,
  `BasaltGauge`, bespoke button / toggle / stepper / rows, six hand-drawn
  instrument icons, two motion curves
- `:core:time` — injectable `TimeSource`, `Weekdays` bitmask, `AlarmSchedule`
  (next-occurrence walk and zone resolution), duration and clock formatting
- `:core:data` — Room database, DataStore preferences, domain models,
  repository interfaces, and `BasaltGraph` as the object graph
- `:core:notify` — six notification channels, created once per process

### Added — features

- **Clock** — live dot-matrix readout, ticking on minute boundaries rather than
  polling on a fixed period
- **Alarms** — create, repeat days, enable, skip-next, delete; per-alarm sound;
  time until the next occurrence read from the materialised instance
- **Timers** — multiple concurrent timers, countdown past zero into negative
  time, pause / resume, add-a-minute, expiry ring
- **Stopwatch** — brass gauge sweeping the minute, laps carrying both split and
  cumulative time
- **Bedtime** — sleep window drawn as an arc across the 24-hour dial, with
  wind-down lead and a per-day schedule
- **Settings** — display, alarm behaviour, opt-in extras, and a SYSTEM section
  exposing the three permissions alarms depend on
- **Ringtone picker** — device alarm tones and ringtones, plus any MP3 or WAV
  via the document picker with a persistable URI permission, and a trim editor
  that loops the chosen section for audition

### Added — the alarm engine

- `AlarmScheduler` — idempotent `rescheduleAll()`, per-occurrence
  `setAlarmClock`, an inexact upcoming notice two hours ahead, and graceful
  degradation to `setAndAllowWhileIdle` without the exact-alarm permission
- `AlarmStateManager` — the only writer of occurrence state; schedules the next
  occurrence on every terminal transition
- `AlarmService` — foreground `mediaPlayback` service with alarm-stream audio,
  volume ramp, vibration, auto-silence, trimmed-section looping and a fallback
  tone when the chosen sound will not play
- `AlarmActivity` — `singleInstance`, shows over the keyguard, turns the screen
  on
- `BootReceiver` — `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, `TIMEZONE_CHANGED`,
  `TIME_SET`, `LOCALE_CHANGED`, all funnelling into `rescheduleAll()`

### Added — icon and packaging

- Launcher icon generated by `tools/generate_icon.py` into SVG sources,
  VectorDrawable layers and legacy mipmaps
- Live launcher icon: adaptive foreground layer-list plus Launcher3 clock
  meta-data, so the hands follow the real time. `SECOND_LAYER_INDEX = -1`
- Release signing from `keystore.properties` or `BASALT_*` environment
  variables; a missing configuration produces an unsigned APK and a warning
  rather than a build failure
- MIT licence, plus `NOTICES.MD` and `THIRD_PARTY_LICENSE.MD` documenting the
  AOSP DeskClock derivation per file

### Changed

- Anything persisted moved from the monotonic clock to the wall clock.
  `SystemClock.elapsedRealtime()` restarts at zero on reboot, so a persisted
  monotonic deadline is meaningless exactly when it matters most
- Timers and the stopwatch no longer use a foreground service while counting;
  they are a deadline plus a notification whose chronometer the system draws
- Application id and package root corrected from `dev.auriel.basalt` to
  `app.auriel.basalt`
- `minSdk` raised from 26 to 29; `compileSdk` and `targetSdk` raised to 36
- Window theme moved from `:app` to `:core:design`, so the alarm activity
  declared in `:feature:alarm` can reference it
- Bottom navigation reworked to five sections with Clock centred, engraved
  instrument icons over dot-matrix labels, and Settings moved to the top bar
- Gauge needle drawn as the outer third of a hand instead of pivoting from the
  centre, so it no longer crosses the readout

### Fixed

- Components declared in a library manifest need fully-qualified class names. A
  leading dot resolves against the library's own package, producing
  `…feature.alarm.feature.alarm.BootReceiver` and a `ClassNotFoundException`
  that only surfaces when the receiver actually fires
- A full-screen intent is suppressed when the device is unlocked and the posting
  app is already foregrounded (`FSI suppressed: no HUN or keyguard`), so an
  alarm rang with nothing on screen to stop it. The service now also starts the
  activity directly; the two routes cover opposite conditions
- Tapping CLOCK from another tab did nothing. `onSelect = ::go` passes a
  function reference, which Compose compares as equal across recompositions, so
  the tab strip kept the first one and its stale captured route. The current
  route is now read from the controller at click time
- Bottom navigation labels overflowed the screen at six full-word tabs

[Unreleased]: https://github.com/AurielSolaris/Basalt/compare/v0.2.0...HEAD
[0.2.0]: https://github.com/AurielSolaris/Basalt/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/AurielSolaris/Basalt/releases/tag/v0.1.0
