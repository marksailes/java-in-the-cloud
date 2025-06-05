package software.sailes.carrental;

import software.amazon.awscdk.App;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.Tags;

public class CarRentalInfrastructureApp {

    public static final String PROJECT_NAME = "car-rentals-s3-processing";

    public static void main(final String[] args) {
        App app = new App();

        CarRentalInfrastructureStack infrastructureStack = new CarRentalInfrastructureStack(app, "CarRentalInfrastructureStack", StackProps.builder().build());
        CarRentalApplicationStack applicationStack = new CarRentalApplicationStack(app, "CarRentalApplicationStack", StackProps.builder().build(), infrastructureStack);

        Tags.of(infrastructureStack).add("project", PROJECT_NAME);
        Tags.of(applicationStack).add("project", PROJECT_NAME);

        app.synth();
    }
}

