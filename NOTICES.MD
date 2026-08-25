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
| Feature scope | DeskClock as a whole | Which features a clock app is expected to have at all, and how they divide into sections. |

### What is planned but not yet present

- **The city dataset.** DeskClock's `res/xml/cities.xml` is a large, carefully
  curated list of cities and time zones. Basalt intends to import it as **data**
  under the Apache 2.0 licence. It is not in this repository yet; the current
  city list is a five-entry placeholder written for development. When the import
  happens, this section moves up into the table above and the licence header is
  carried with the file.
- **Widget sizing behaviour.** DeskClock's `WidgetUtils` works out how to scale
  a clock widget's contents to the box the launcher gives it. Basalt's widget
  work (v0.3.0) will reference that reasoning.

### What is *not* from DeskClock

Everything visual. The palette, the dot-matrix renderer and its glyphs, the
chrome vocabulary, the icon set, the launcher icon, the navigation structure and
every control are original to Basalt. DeskClock is a Material app; Basalt
deliberately contains no Material Design at all.

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

## Icon generation

[`tools/generate_icon.py`](tools/generate_icon.py) is original work. It uses
[`resvg-py`](https://pypi.org/project/resvg-py/) at authoring time only, to
rasterise the generated SVGs. `resvg-py` is not a dependency of the app and
ships nothing into the APK.

---

## Reporting an attribution problem

If something here is wrong, incomplete, or credits the wrong party, that is a
bug and should be reported like one: <debadityamalakar@gmail.com>.
