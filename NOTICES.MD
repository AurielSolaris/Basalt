# Notices

Basalt is an independent work, but it did not arrive from nowhere. This file
records what it owes to other projects, and how.

Full licence texts for everything named here are in
[THIRD_PARTY_LICENSE.MD](THIRD_PARTY_LICENSE.MD).

---

## Android Open Source Project — DeskClock

- **Project:** AOSP DeskClock (the stock Android Clock app)
- **Source:** https://android.googlesource.com/platform/packages/apps/DeskClock
- **Reference commit:** `04e481f37e0b52b74c5a5c7b78b662d1f94e3478` (2025-02-27)
- **Copyright:** Copyright (C) The Android Open Source Project
- **Licence:** Apache License 2.0

Basalt is a **reimplementation**, not a fork. No DeskClock source file is
present in this repository, and no DeskClock code has been copied into it. What
was taken is *behaviour* — the semantics a clock app has to get right, worked
out over many years by people who had to ship it to a billion devices.

### What is derived from DeskClock

| Basalt | Derived from | What was taken |
|---|---|---|
| `core/time/Weekdays.kt` | `data/Weekdays.kt` | The idea of storing repeat days as a 7-bit mask on the alarm row rather than seven booleans, and the "days until next occurrence" walk. Basalt's bit order is `java.time.DayOfWeek`'s (Monday = 1), **not** DeskClock's legacy `Calendar` order, so the stored values are not interchangeable. |
| `core/data/model/Alarm.kt`, `AlarmInstance.kt` | `provider/Alarm.java`, `provider/AlarmInstance.java` | The separation of an alarm *rule* from a materialised *occurrence*, so that snoozing or dismissing one morning cannot mutate the schedule. |
| `AlarmInstanceState` | `alarms/AlarmStateManager` | The shape of the state machine: quiet pre-alarm notification → dismissible high-priority notification → firing → snoozed → missed → dismissed. The intermediate notification states exist so an alarm can be dismissed *before* it makes noise; "missed" exists so a phone that was off can still report what it failed to do. Both are non-obvious and both are DeskClock's. |
| `core/data/model/Timer.kt` | `data/Timer.kt`, `data/TimerModel.kt` | Timer state semantics: the deadline-while-running / remainder-while-paused split, and the decision that a timer reaching zero keeps counting *negatively* rather than stopping. |
| `core/data/model/Stopwatch.kt` | `data/Stopwatch.kt`, `data/Lap.kt` | Accumulated time plus an optional start mark, so pausing needs no clock read and elapsed time does not drift across start/stop cycles; and storing each lap's split *and* running total rather than deriving one from the other. |
| `core/data/model/City.kt` | `data/City.kt` | Storing a zone id rather than a fixed UTC offset. A world clock that stores offsets is wrong twice a year. |
| `widget/render/FaceRenderer.kt` | `WidgetUtils.kt` | The *reasoning* about scaling a widget's contents to the box the launcher gives it, rather than authoring at a fixed size. Basalt's implementation shares no structure with it: DeskClock scales a `RemoteViews` text size, Basalt measures a face and rasterises it. |
| Feature scope | DeskClock as a whole | Which features a clock app is expected to have at all, and how they divide into sections. |

### What is planned but not yet present

- **The city dataset.** DeskClock's `res/xml/cities.xml` is a large, carefully
  curated list of cities and time zones. Basalt intends to import it as **data**
  under the Apache 2.0 licence. It is not in this repository yet; the current
  city list is a five-entry placeholder written for development. When the import
  happens, this section moves up into the table above and the licence header is
  carried with the file.
- *(Nothing else outstanding.)*

### What is *not* from DeskClock

Everything visual. The palette, the dot-matrix renderer and its glyphs, the
chrome vocabulary, the icon set, the launcher icon, the navigation structure and
every control are original to Basalt. DeskClock is a Material app; Basalt
deliberately contains no Material Design at all.

### A note on the widget catalogue

Basalt ships ten widgets. AOSP DeskClock ships **two** — an analog clock and a
digital one — and that is as true of the Android 16 tree as it was of Android
10; the file set is unchanged between them. The large widget collections people
associate with "the Android clock" belong to Google's proprietary Clock app,
which is not open source and was not consulted.

The catalogue is therefore **not** derived from DeskClock. It is the union of
the widget *ideas* published by Google Clock, Samsung Clock and Nothing OS,
deduplicated — which is a description of what those apps show a user, not of
how any of them is built. Each entry records where its idea came from in
`BasaltWidget.origin`. No code, layout or resource from any of them is present
here, and none of those vendors endorses this app.

---

## Android Open Source Project — Launcher3

- **Project:** AOSP Launcher3
- **Source:** https://android.googlesource.com/platform/packages/apps/Launcher3
- **Copyright:** Copyright (C) The Android Open Source Project
- **Licence:** Apache License 2.0

Basalt's launcher icon updates with the time. It does this by declaring
meta-data that Launcher3 reads:

```
com.android.launcher3.HOUR_LAYER_INDEX
com.android.launcher3.MINUTE_LAYER_INDEX
com.android.launcher3.SECOND_LAYER_INDEX
com.android.launcher3.DEFAULT_HOUR / DEFAULT_MINUTE / DEFAULT_SECOND
```

No Launcher3 code is used. These are metadata keys defined by Launcher3's
`ClockDrawableWrapper`, and Basalt uses them the way any clock app is expected
to. They are a convention, not a platform API: launchers that do not implement
it simply show a static icon.

---

## Jetpack / AndroidX

Basalt depends on AndroidX libraries — Compose (`foundation`, `ui`, `runtime`),
Navigation, Lifecycle, Core, DataStore, Room and Glance — all
Copyright (C) The Android Open Source Project, all under the Apache License 2.0.
They are consumed as published artifacts; no source is vendored.

Note that `androidx.compose.material` and `androidx.compose.material3` are
deliberately **not** dependencies of this project.

## Kotlin and kotlinx.coroutines

Copyright (C) JetBrains s.r.o. and Kotlin Programming Language contributors.
Licensed under the Apache License 2.0.

## Build tooling

Gradle (Apache 2.0) and the Android Gradle Plugin (Apache 2.0) are build-time
tools and are not distributed as part of the application.

## Fonts

| Font | Used for | Copyright | Licence |
|---|---|---|---|
| **EB Garamond** | The **Vintage** UI style | Copyright 2017 The EB Garamond Project Authors | SIL Open Font License 1.1 |

- **Source:** <https://github.com/octaviopardo/EBGaramond12>, via
  [Google Fonts](https://fonts.google.com/specimen/EB+Garamond)
- **Shipped as:** `core/design/src/main/res/font/eb_garamond.ttf` — the
  upstream variable font (`wght` axis), unmodified.

The OFL permits bundling in an application, including a commercial one. The
font is not renamed, so the Reserved Font Name clause is satisfied by leaving
it alone. Its full licence text is in
[THIRD_PARTY_LICENSE.MD](THIRD_PARTY_LICENSE.MD).

Basalt's **Retro** style is not a font and is not covered by any of this: its
glyphs are hand-authored bitmasks in `core/dotmatrix`, original to this project.

---

## Colour schemes

Three of Basalt's themes take their colour values from existing, separately
authored palettes. No code is used from either project — only the published
hex values, re-mapped onto Basalt's own eleven roles.

| Basalt theme | Derived from | Copyright | Licence |
|---|---|---|---|
| **Mocha** | [Catppuccin](https://github.com/catppuccin/catppuccin) — Mocha flavour | Copyright (c) 2021 Catppuccin | MIT |
| **Latte** | [Catppuccin](https://github.com/catppuccin/catppuccin) — Latte flavour | Copyright (c) 2021 Catppuccin | MIT |
| **Solar** | [Nord](https://github.com/nordtheme/nord) — Polar Night and Snow Storm | Copyright (c) 2016-present Sven Greb | MIT |

These are **adaptations, not ports**, and should not be taken as
reference-accurate renditions of either scheme:

- Catppuccin and Nord are *named-colour sets* (`base`, `surface0`, `peach`,
  `nord3`); Basalt is a *role set* (`ink`, `ironOxide`, `copper`, `pewter`).
  Assigning one to the other is an interpretation, and a different one would
  be equally defensible.
- Several values are darkened or lightened from what those projects ship, to
  clear the contrast bars enforced in
  [`ThemeCatalogTest`](core/design/src/test/java/app/auriel/basalt/core/design/ThemeCatalogTest.kt).
  Latte's accents in particular are tuned to sit beside its base rather than
  to be read on it, and Basalt puts the accent colour into digits.
- **Solar deliberately departs from Nord.** It keeps the Polar Night and Snow
  Storm greys and replaces the Frost blues with a soft yellow pastel accent.
  It is a Nord-derived chassis, not Nord.

Neither project endorses this app.

## Icon generation

[`tools/generate_icon.py`](tools/generate_icon.py) is original work. It uses
[`resvg-py`](https://pypi.org/project/resvg-py/) at authoring time only, to
rasterise the generated SVGs. `resvg-py` is not a dependency of the app and
ships nothing into the APK.

---

## Reporting an attribution problem

If something here is wrong, incomplete, or credits the wrong party, that is a
bug and should be reported like one: <debadityamalakar@gmail.com>.
