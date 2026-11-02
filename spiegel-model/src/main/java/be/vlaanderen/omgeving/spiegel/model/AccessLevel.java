package be.vlaanderen.omgeving.spiegel.model;

import java.util.Collection;
import java.util.Comparator;

/**
 * todo to decide this belongs to the core model or an adapter model
 * The sensitivity level of data, and the clearance of a caller.
 *
 * <p>Levels are ordered from least to most sensitive. A caller cleared for a level may also see
 * every lower level (requirement FR-AC-02).
 *
 * <p><b>Where this type lives in the hexagonal design:</b> the core only knows a caller's
 * {@link UserContext}, carrying a username and a set of roles; it has no notion of
 * {@code AccessLevel} itself. This type belongs to the outbound side: an outbound port adapter
 * (for example a repository querying a SPARQL endpoint or Virtuoso) maps the roles in a
 * {@code UserContext} to an {@code AccessLevel} of its own choosing, and uses that level to pick
 * the right credentials or graphs when resolving a {@link SpResource}. Two adapters may map the
 * same roles differently, and the core is unaware of that mapping.
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
