package software.sailes.carrental.features;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import software.sailes.carrental.domain.CarCharge;
import software.sailes.carrental.domain.CarChargeId;
import software.sailes.carrental.domain.CarChargeRepository;
import software.sailes.carrental.domain.CarId;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class RecordCarChargedAtSupplierTest {

    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            "postgres:16-alpine"
    );

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @BeforeAll
    static void beforeAll() {
        postgres.start();
    }

    @AfterAll
    static void afterAll() {
        postgres.stop();
    }

    @Autowired
    private RecordCarChargedAtSupplier recordCarChargedAtSupplier;

    @Autowired
    private CarChargeRepository carChargeRepository;

    @Test
    public void recordSingleChargingEvent() {
        SupplierChargeRecord supplierChargeRecord = new SupplierChargeRecord(new CarId(), "QuickCharging", 10);

        CarChargeId carChargeId = recordCarChargedAtSupplier.recordCarCharge(supplierChargeRecord);

        CarCharge carCharge = carChargeRepository.findById(carChargeId).orElseThrow();
        assertEquals(10, carCharge.getAmount());
    }
}