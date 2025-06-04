package software.sailes.carrental.domain;

import org.springframework.util.Assert;

import java.util.UUID;

public record CarChargeId(UUID id) {

    public CarChargeId {
        Assert.notNull(id, "id must not be null");
    }

    public CarChargeId() {
        this(UUID.randomUUID());
    }
}
