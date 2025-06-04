package software.sailes.carrental.domain;

import jakarta.persistence.*;
import org.springframework.util.Assert;

@Entity
public class CarCharge {

    @EmbeddedId
    private CarChargeId id;

    @Embedded
    @AttributeOverride(name = "carId", column = @Column(name = "carId"))
    private CarId carId;

    private String supplier;
    private Integer amount;

    CarCharge() {
    }

    public CarCharge(CarId carId, String supplier, Integer amount) {
        Assert.notNull(carId, "carId must not be null");
        Assert.notNull(supplier, "supplier must not be null");
        Assert.notNull(amount, "amount must not be null");
        this.id = new CarChargeId();
        this.carId = carId;
        this.supplier = supplier;
        this.amount = amount;
    }

    public CarChargeId getId() {
        return id;
    }

    public void setId(CarChargeId id) {
        this.id = id;
    }

    public CarId getCarId() {
        return carId;
    }

    public void setCarId(CarId carId) {
        this.carId = carId;
    }

    public String getSupplier() {
        return supplier;
    }

    public void setSupplier(String supplier) {
        this.supplier = supplier;
    }

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }
}
