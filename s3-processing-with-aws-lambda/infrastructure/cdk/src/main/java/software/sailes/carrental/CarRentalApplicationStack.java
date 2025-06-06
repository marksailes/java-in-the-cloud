package software.sailes.carrental;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ec2.SubnetSelection;
import software.amazon.awscdk.services.ec2.SubnetType;
import software.amazon.awscdk.services.lambda.Architecture;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.EventType;
import software.amazon.awscdk.services.s3.NotificationKeyFilter;
import software.amazon.awscdk.services.s3.notifications.LambdaDestination;
import software.constructs.Construct;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class CarRentalApplicationStack extends Stack {

    public CarRentalApplicationStack(final Construct scope, final String id, final StackProps props, CarRentalInfrastructureStack infrastructureStack) {
        super(scope, id, props);

        Bucket csvUploadsBucket = Bucket.Builder.create(this, "csvUploadsBucket")
                .bucketName("csv-uploads-bucket-" + UUID.randomUUID().toString())
                .build();

        Function carChargeCSVProcessor = Function.Builder.create(this, "CarChargeCSVProcessor")
                .runtime(Runtime.JAVA_21)
                .memorySize(2048)
                .timeout(Duration.seconds(30))
                .code(Code.fromAsset("../../software/s3-event-processor/target/s3-event-processor.jar"))
                .handler("org.springframework.cloud.function.adapter.aws.FunctionInvoker::handleRequest")
                .vpc(infrastructureStack.getCarRentalVpc())
                .vpcSubnets(SubnetSelection.builder()
                        .subnetType(SubnetType.PRIVATE_ISOLATED)  // Lambda in public subnets to access S3
                        .build())
                .allowPublicSubnet(false)
                .securityGroups(List.of(infrastructureStack.getApplicationSecurityGroup()))
                .architecture(Architecture.ARM_64)
                .environment(new HashMap<>() {{
                    put("MAIN_CLASS", "software.sailes.carrental.ProcessSupplierChargeS3Objects");
                    put("DB_PASSWORD", infrastructureStack.getDatabaseSecretString());
                    put("DB_CONNECTION_URL", infrastructureStack.getDatabaseJDBCConnectionString());
                    put("DB_USER", "postgres");
                }})
                .build();

        NotificationKeyFilter filter = NotificationKeyFilter.builder().suffix(".csv").build();
        csvUploadsBucket.addEventNotification(EventType.OBJECT_CREATED, new LambdaDestination(carChargeCSVProcessor), filter);
        csvUploadsBucket.grantRead(carChargeCSVProcessor);
    }
}
