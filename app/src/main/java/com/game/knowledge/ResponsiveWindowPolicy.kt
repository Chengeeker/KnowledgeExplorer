package com.game.knowledge

/**
 * Use the actual Compose window size (not physical screen inches).
 * Landscape tablets and desktop multi-window layouts share the same full-height
 * two-pane content; narrow/short windows keep the scrollable phone layout.
 */
internal object ResponsiveWindowPolicy {
    fun tabletLandscape(widthDp:Float,heightDp:Float):Boolean =
        widthDp>=720f && widthDp>heightDp && heightDp>=450f
}
