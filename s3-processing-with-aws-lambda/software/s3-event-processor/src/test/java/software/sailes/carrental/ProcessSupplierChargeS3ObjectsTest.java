package software.sailes.carrental;

import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.sailes.carrental.domain.CarCharge;
import software.sailes.carrental.domain.CarChargeRepository;
import software.sailes.carrental.domain.CarId;
import software.sailes.carrental.features.RecordCarChargedAtSupplier;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;
import static software.sailes.carrental.TestData.createS3Record;

@Testcontainers
@SpringBootTest(classes = ProcessSupplierChargeS3Objects.class)
class ProcessSupplierChargeS3ObjectsTest {

    private static final DockerImageName localstackImage = DockerImageName.parse("localstack/localstack:3.5.0");

    @Container
    static LocalStackContainer localstack = new LocalStackContainer(localstackImage)
            .withServices(S3);

    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

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

    static S3Client s3Client;

    @Autowired
    RecordCarChargedAtSupplier recordCarChargedAtSupplier;

    @Autowired
    CarChargeRepository carChargeRepository;

    @BeforeEach
    void setUp() {
        s3Client = S3Client.builder()
                .endpointOverride(localstack.getEndpoint())
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey())
                        )
                )
                .region(Region.of(localstack.getRegion()))
                .build();

        ProcessSupplierChargeS3Objects handler = new ProcessSupplierChargeS3Objects(recordCarChargedAtSupplier, s3Client);
    }

    @Test
    public void endToEndTest() {
        ProcessSupplierChargeS3Objects processSupplierChargeS3Objects = new ProcessSupplierChargeS3Objects(recordCarChargedAtSupplier, s3Client);
        Function<S3Event, Void> s3EventVoidFunction = processSupplierChargeS3Objects.handleS3Event();
        S3EventNotification.S3EventNotificationRecord notificationRecord = addTestDataToS3("single-record.csv");

        s3EventVoidFunction.apply(new S3Event(List.of((notificationRecord))));

        List<CarCharge> allByCarId = carChargeRepository.findAllByCarId(CarId.fromString("55f8134a-48f2-4615-827e-eecb50615b77"));
        assertEquals(10, allByCarId.getFirst().getAmount());
    }

    private static S3EventNotification.@NotNull S3EventNotificationRecord addTestDataToS3(String objectKey) {
        String bucketName = "bucket";
        s3Client.createBucket(CreateBucketRequest.builder()
                .bucket(bucketName).build());

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();
        s3Client.putObject(putObjectRequest, Path.of("src", "test", "resources", objectKey));
        return createS3Record(bucketName, objectKey);
    }


}