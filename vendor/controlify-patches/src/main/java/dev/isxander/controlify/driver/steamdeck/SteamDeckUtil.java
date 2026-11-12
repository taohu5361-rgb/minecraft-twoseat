// SPDX-License-Identifier: LGPL-3.0-or-later
// TwoSeat modification, 2026-11-12: omit the optional, unlicensed steamdeck4j dependency.
package dev.isxander.controlify.driver.steamdeck;

import dev.isxander.controlify.utils.CUtil;
import net.minecraft.resources.ResourceLocation;
import java.util.Optional;

/** Ordinary controllers still use upstream SDL/GLFW; enhanced Steam Deck CEF is not distributed. */
public final class SteamDeckUtil {
    public static final boolean IS_STEAM_DECK = false;
    public static final SteamDeckMode DECK_MODE = SteamDeckMode.NOT_STEAM_DECK;
    public static final boolean IS_SANDBOXED = "1".equals(System.getenv("container"));
    public static final ResourceLocation STEAM_DECK_NAMESPACE = CUtil.rl("steam_deck");
    public static Optional<Object> getDeckInstance() { return Optional.empty(); }
}
