package software.sailes.carrental;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.SecretValue;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.lambda.Architecture;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.rds.*;
import software.constructs.Construct;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;

import java.util.HashMap;
import java.util.List;

public class CarRentalInfrastructureStack extends Stack {

    private static final String DATABASE_NAME = "carrentals";
    private final IVpc carRentalVpc;
    private final SecurityGroup applicationSecurityGroup;
    private final DatabaseSecret databaseSecret;
    private final DatabaseInstance database;

    public CarRentalInfrastructureStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        carRentalVpc = createCarRentalVpc();
        SecurityGroup secretManagerEndpointSecurityGroup = createSecretManagerEndpointSecurityGroup();

        createSecretManagerVpcEndpoint(secretManagerEndpointSecurityGroup);

        databaseSecret = createDatabaseSecret();
        applicationSecurityGroup = createApplicationSecurityGroup(carRentalVpc);
        database = createRDSPostgresInstance(carRentalVpc, databaseSecret, applicationSecurityGroup);

        createDBSetupFunction(carRentalVpc, applicationSecurityGroup, databaseSecret);
    }

    private SecurityGroup createSecretManagerEndpointSecurityGroup() {
        SecurityGroup secretsManagerEndpointSecurityGroup = SecurityGroup.Builder.create(this, "SecretsManagerEndpointSG")
                .vpc(carRentalVpc)
                .description("Security group for Secrets Manager VPC endpoint")
                .allowAllOutbound(false)
                .build();

        // Allow HTTPS inbound from VPC CIDR
        secretsManagerEndpointSecurityGroup.addIngressRule(
                Peer.ipv4(carRentalVpc.getVpcCidrBlock()),
                Port.tcp(443),
                "Allow HTTPS from VPC"
        );

        return secretsManagerEndpointSecurityGroup;
    }

    private void createSecretManagerVpcEndpoint(SecurityGroup secretManagerEndpointSecurityGroup) {
        InterfaceVpcEndpoint.Builder.create(this, "SecretsManagerVpcEndpoint")
                .vpc(carRentalVpc)
                .service(InterfaceVpcEndpointAwsService.SECRETS_MANAGER)
                .subnets(SubnetSelection.builder()
                        .subnetType(SubnetType.PRIVATE_ISOLATED)
                        .build())
                .securityGroups(List.of(secretManagerEndpointSecurityGroup))
                .privateDnsEnabled(true)
                .build();
    }

    private IVpc createCarRentalVpc() {
        var vpc = Vpc.Builder.create(this, "CarRentalVpc")
                .vpcName("CarRental")
                .natGateways(0)  // Keep NAT Gateway disabled
                .build();

        // Add S3 VPC Endpoint
        vpc.addGatewayEndpoint("S3Endpoint", GatewayVpcEndpointOptions.builder()
                .service(GatewayVpcEndpointAwsService.S3)
                .build());

        return vpc;
    }

    private DatabaseSecret createDatabaseSecret() {
        return DatabaseSecret.Builder.create(this, "postgres")
                .secretName("car-rental-db-secret")
                .username("postgres")
                .build();
    }

    private SecurityGroup createApplicationSecurityGroup(IVpc vpc) {
        return SecurityGroup.Builder.create(this, "ApplicationSecurityGroup")
                .securityGroupName("applicationSG")
                .vpc(vpc)
                .allowAllOutbound(true)  // Allow all outbound traffic
                .build();
    }

    private SecurityGroup createDatabaseSecurityGroup(IVpc vpc, SecurityGroup applicationSecurityGroup) {
        var databaseSecurityGroup = SecurityGroup.Builder.create(this, "DatabaseSG")
                .securityGroupName("DatabaseSG")
                .allowAllOutbound(false)
                .vpc(vpc)
                .build();

        // Allow connections from application security group instead of broad CIDR
        databaseSecurityGroup.addIngressRule(
                Peer.securityGroupId(applicationSecurityGroup.getSecurityGroupId()),
                Port.tcp(5432),
                "Allow Database Traffic from Lambda"
        );

        return databaseSecurityGroup;
    }

    private DatabaseInstance createRDSPostgresInstance(IVpc vpc, DatabaseSecret databaseSecret, SecurityGroup applicationSecurityGroup) {
        var databaseSecurityGroup = createDatabaseSecurityGroup(vpc, applicationSecurityGroup);
        var engine = DatabaseInstanceEngine.postgres(PostgresInstanceEngineProps.builder().version(PostgresEngineVersion.VER_17_4).build());

        return DatabaseInstance.Builder.create(this, "CarRentalInstance")
                .engine(engine)
                .vpc(vpc)
                .allowMajorVersionUpgrade(true)
                .backupRetention(Duration.days(0))
                .databaseName(DATABASE_NAME)
                .instanceIdentifier("car-rentals")
                .instanceType(InstanceType.of(InstanceClass.BURSTABLE4_GRAVITON, InstanceSize.MICRO))
                .vpcSubnets(SubnetSelection.builder()
                        .subnetType(SubnetType.PRIVATE_ISOLATED)  // RDS in isolated subnets
                        .build())
                .securityGroups(List.of(databaseSecurityGroup))
                .credentials(Credentials.fromSecret(databaseSecret))
                .build();
    }

    private void createDBSetupFunction(IVpc vpc, ISecurityGroup applicationSecurityGroup, DatabaseSecret databaseSecret) {
        Function dbSetupFunction = Function.Builder.create(this, "DBSetupLambdaFunction")
                .runtime(Runtime.JAVA_21)
                .memorySize(512)
                .timeout(Duration.seconds(29))
                .code(Code.fromAsset("../db-setup/target/db-setup.jar"))
                .handler("software.sailes.carrental.infrastructure.db.DBSetupHandler")
                .vpc(vpc)
                .vpcSubnets(SubnetSelection.builder()
                        .subnetType(SubnetType.PRIVATE_ISOLATED)
                        .build())
                .allowPublicSubnet(false)
                .securityGroups(List.of(applicationSecurityGroup))
                .architecture(Architecture.ARM_64)
                .environment(new HashMap<>() {{
                    put("DB_CONNECTION_URL", getDatabaseJDBCConnectionString());
                }})
                .build();

        databaseSecret.grantRead(dbSetupFunction);
    }

    @Deprecated
    public String getDatabaseSecretString() {
        SecretValue password = databaseSecret.secretValueFromJson("password");
        // TODO FIX - get password at runtime
        return password.unsafeUnwrap();
    }

    public String getDatabaseJDBCConnectionString() {
        return "jdbc:postgresql://" + database.getDbInstanceEndpointAddress() + ":5432/" + DATABASE_NAME;
    }

    public IVpc getCarRentalVpc() {
        return carRentalVpc;
    }

    public ISecurityGroup getApplicationSecurityGroup() {
        return applicationSecurityGroup;
    }

    public DatabaseSecret getDatabaseSecret() {
        return databaseSecret;
    }
}