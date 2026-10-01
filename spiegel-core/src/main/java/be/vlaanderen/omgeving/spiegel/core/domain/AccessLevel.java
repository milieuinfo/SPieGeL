package be.vlaanderen.omgeving.spiegel.core.domain;

import java.util.Collection;
import java.util.Comparator;

/**
 * The sensitivity level of data, and the clearance of a caller.
 *
 * <p>Levels are ordered from least to most sensitive. A caller cleared for a level may also see
 * every lower level (requirement FR-AC-02).
 */
public enum AccessLevel {

    PUBLIC,
    INTERNAL,
    CONFIDENTIAL,
    SECRET,
    TOP_SECRET;

    /**
     * Returns the highest level among the given levels, or {@link #PUBLIC} when there are none.
     *
     * @param levels the levels granted to a caller, for example by their roles
     * @return the caller's effective level
     */
    public static AccessLevel highestOf(Collection<AccessLevel> levels) {
        return levels.stream().max(Comparator.naturalOrder()).orElse(PUBLIC);
    }

    /**
     * Tells whether a caller at this level may see data classified at the given level.
     *
     * @param required the level at which the data is classified
     * @return {@code true} if this level is at least as high as {@code required}
     */
    public boolean permits(AccessLevel required) {
        return compareTo(required) >= 0;
    }
}
