package software.sailes.carrental.features;

import software.sailes.carrental.domain.CarId;

public record SupplierChargeRecord(CarId carId, String supplier, Integer amount) {
}
