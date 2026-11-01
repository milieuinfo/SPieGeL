package be.vlaanderen.omgeving.spiegel.model;

import java.util.Objects;
import java.util.Set;

/**
 *
 */
public record UserContext(String username, Set<String> roles) {
    public UserContext {
        if (username == null) {
            throw new IllegalArgumentException("Username cannot be null");
        }
        if (roles == null) {
            throw new IllegalArgumentException("Roles cannot be null");
        }
    }

    public static UserContext of(String username, Set<String> roles) {
        return new UserContext(username, roles);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UserContext that = (UserContext) o;
        return Objects.equals(username, that.username) && Objects.equals(roles, that.roles);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, roles);
    }
}
