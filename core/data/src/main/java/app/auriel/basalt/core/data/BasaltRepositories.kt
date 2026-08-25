package app.auriel.basalt.core.data

import app.auriel.basalt.core.data.repository.AlarmRepository
import app.auriel.basalt.core.data.repository.CityRepository
import app.auriel.basalt.core.data.repository.InMemoryAlarmRepository
import app.auriel.basalt.core.data.repository.InMemoryCityRepository
import app.auriel.basalt.core.data.repository.InMemoryStopwatchRepository
import app.auriel.basalt.core.data.repository.InMemoryTimerRepository
import app.auriel.basalt.core.data.repository.StopwatchRepository
import app.auriel.basalt.core.data.repository.TimerRepository

/**
 * The object graph, such as it is.
 *
 * A hand-rolled singleton holding the in-memory repositories, so features
 * can share state before Hilt and Room are wired in. It is deliberately the
 * only place that names an `InMemory*` type: when the Room implementations
 * land, this file changes and nothing above it does.
 *
 * Process-scoped, so state survives navigation and configuration changes
 * but not process death — which is exactly the limitation the persistence
 * pass exists to remove.
 */
object BasaltRepositories {
    val alarms: AlarmRepository by lazy { InMemoryAlarmRepository() }
    val timers: TimerRepository by lazy { InMemoryTimerRepository() }
    val stopwatch: StopwatchRepository by lazy { InMemoryStopwatchRepository() }
    val cities: CityRepository by lazy { InMemoryCityRepository() }
}
