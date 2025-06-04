package software.sailes.carrental.domain;

import org.springframework.util.Assert;

import java.util.UUID;

public record CarId(UUID carId) {

    public CarId {
        Assert.notNull(carId, "id must not be null");
    }

    public CarId() {
        this(UUID.randomUUID());
    }

    public static CarId fromString(String carId) {
        return new CarId(UUID.fromString(carId));
    }
}
