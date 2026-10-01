package be.vlaanderen.omgeving.spiegel.core.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("FR-AC-02: access levels form an ordered type")
class AccessLevelTest {

    @Test
    void callerWithoutRolesIsPublic() {
        assertThat(AccessLevel.highestOf(List.of())).isEqualTo(AccessLevel.PUBLIC);
    }

    @Test
    void highestGrantedLevelWins() {
        var levels = List.of(AccessLevel.INTERNAL, AccessLevel.SECRET, AccessLevel.CONFIDENTIAL);

        assertThat(AccessLevel.highestOf(levels)).isEqualTo(AccessLevel.SECRET);
    }

    @Test
    void higherLevelPermitsLowerLevels() {
        assertThat(AccessLevel.SECRET.permits(AccessLevel.PUBLIC)).isTrue();
        assertThat(AccessLevel.SECRET.permits(AccessLevel.SECRET)).isTrue();
        assertThat(AccessLevel.SECRET.permits(AccessLevel.TOP_SECRET)).isFalse();
    }
}
