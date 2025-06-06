package software.sailes.carrental.features;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.sailes.carrental.domain.CarCharge;
import software.sailes.carrental.domain.CarChargeId;
import software.sailes.carrental.domain.CarChargeRepository;

@Service
public class RecordCarChargedAtSupplier {

    private static final Logger logger = LoggerFactory.getLogger(RecordCarChargedAtSupplier.class);

    private final CarChargeRepository carChargeRepository;

    public RecordCarChargedAtSupplier(CarChargeRepository carChargeRepository) {
        this.carChargeRepository = carChargeRepository;
    }

    public CarChargeId recordCarCharge(SupplierChargeRecord supplierChargeRecord) {
        CarCharge carCharge = new CarCharge(supplierChargeRecord.carId(), supplierChargeRecord.supplier(), supplierChargeRecord.amount());
        CarCharge saved = carChargeRepository.save(carCharge);

        logger.info("Record of supplier car charge saved: {}", saved);

        return saved.getId();
    }
}
