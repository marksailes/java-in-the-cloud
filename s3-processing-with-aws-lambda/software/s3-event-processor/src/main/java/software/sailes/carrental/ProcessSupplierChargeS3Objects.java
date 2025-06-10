package software.sailes.carrental;

import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.sailes.carrental.domain.CarId;
import software.sailes.carrental.features.RecordCarChargedAtSupplier;
import software.sailes.carrental.features.SupplierChargeRecord;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Function;

@SpringBootApplication
public class ProcessSupplierChargeS3Objects  {

    private static final Logger logger = LoggerFactory.getLogger(ProcessSupplierChargeS3Objects.class);

    private final RecordCarChargedAtSupplier recordCarChargedAtSupplier;
    private final S3Client s3Client;

    public ProcessSupplierChargeS3Objects(RecordCarChargedAtSupplier recordCarChargedAtSupplier, S3Client s3Client) {
        this.recordCarChargedAtSupplier = recordCarChargedAtSupplier;
        this.s3Client = s3Client;
    }

    public static void main(String[] args) {
        SpringApplication.run(ProcessSupplierChargeS3Objects.class, args);
    }

    @Bean
    public Function<S3Event, Void> handleS3Event() {
        return s3Event -> {
            s3Event.getRecords()
                    .forEach(this::processSingleRecord);
            return null;
        };
    }

    private void processSingleRecord(S3EventNotification.S3EventNotificationRecord eventNotification) {
        ResponseInputStream<GetObjectResponse> responseInputStream = s3Client.getObject(builder -> builder
                .key(eventNotification.getS3().getObject().getKey())
                .bucket(eventNotification.getS3().getBucket().getName())
        );

        try {
            processCsvStream(responseInputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void processCsvStream(ResponseInputStream<GetObjectResponse> inputStream)
            throws IOException {

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
             CSVReader csvReader = new CSVReader(reader)) {

            // Read all records at once (suitable for smaller files)
            List<String[]> records = csvReader.readAll();

            // Process header if present
            if (!records.isEmpty()) {
                String[] header = records.getFirst();
                logger.info("CSV Header: {}", String.join(", ", header));

                // Process data rows (skip header)
                records.stream()
                        .skip(1)
                        .forEach(this::processRow);
            }

            logger.info("Processed {} records", records.size() - 1);

        } catch (CsvException e) {
            throw new RuntimeException("CSV parsing error", e);
        }
    }

    private void processRow(String[] row) {
        logger.info("Processing row: {}", String.join(", ", row));

        try {
            if (row.length >= 3) {
                SupplierChargeRecord supplierChargeRecord = new SupplierChargeRecord(CarId.fromString(row[1]), row[0], Integer.valueOf(row[2]));
                recordCarChargedAtSupplier.recordCarCharge(supplierChargeRecord);
            } else {
                logger.warn("Row has insufficient columns: {}", String.join(", ", row));
            }
        } catch (Exception e) {
            logger.error("Error processing row: {}", String.join(", ", row), e);
        }
    }

}