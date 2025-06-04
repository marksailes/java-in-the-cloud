package software.sailes.carrental.features;

import org.springframework.stereotype.Service;
import software.sailes.carrental.domain.CarCharge;
import software.sailes.carrental.domain.CarChargeId;
import software.sailes.carrental.domain.CarChargeRepository;

@Service
public class RecordCarChargedAtSupplier {

    private final CarChargeRepository carChargeRepository;

    public RecordCarChargedAtSupplier(CarChargeRepository carChargeRepository) {
        this.carChargeRepository = carChargeRepository;
    }

    public CarChargeId recordCarCharge(SupplierChargeRecord supplierChargeRecord) {
        CarCharge carCharge = new CarCharge(supplierChargeRecord.carId(), supplierChargeRecord.supplier(), supplierChargeRecord.amount());
        carChargeRepository.save(carCharge);

        return carCharge.getId();
    }
}
