package app.auriel.basalt.core.data.model

import java.time.ZoneId

/**
 * A city on the world-clock board.
 *
 * [zoneId] rather than a fixed offset: offsets are a property of an instant,
 * not of a place, and a world clock that stores offsets is a world clock
 * that is wrong twice a year.
 */
data class City(
    val id: String,
    val name: String,
    val zoneId: ZoneId,
    /** Sort position chosen by the user; unsorted cities share 0. */
    val order: Int = 0,
)
