// SPDX-License-Identifier: LGPL-3.0-or-later
// TwoSeat modification, 2026-11-12: retain the optional integration entry point without steamdeck4j.
package dev.isxander.controlify.driver.steamdeck;

import dev.isxander.controlify.driver.Driver;
import dev.isxander.controlify.utils.log.ControlifyLogger;
import java.util.Optional;

public final class SteamDeckDriver implements Driver {
    public static Optional<SteamDeckDriver> create(ControlifyLogger logger) { return Optional.empty(); }
    public static Optional<Object> getDeck() { return Optional.empty(); }
}
