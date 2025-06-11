package software.sailes.carrental.infrastructure.db;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import java.io.IOException;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBSetupHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String DB_CONNECTION = System.getenv("DB_CONNECTION_URL");
    private static final SecretsManagerClient secretsManagerClient = SecretsManagerClient.create();
    private String username;
    private String password;

    public DBSetupHandler() {
        GetSecretValueResponse getSecretValueResponse = secretsManagerClient.getSecretValue(GetSecretValueRequest.builder()
                .secretId("car-rental-db-secret").build());
        String secretString = getSecretValueResponse.secretString();
        try {
            DbSecret dbSecret = objectMapper.readValue(secretString, DbSecret.class);
            username = dbSecret.username();
            password = dbSecret.password();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("error with db password", e);
        }
    }

    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input, final Context context) {
        try (var connection = DriverManager.getConnection(DB_CONNECTION, username, password)) {
            try (var statement = connection.createStatement()) {
                try (var sqlFile = getClass().getClassLoader().getResourceAsStream("setup.sql")) {
                    statement.executeUpdate(IOUtils.toString(sqlFile));
                    return new APIGatewayProxyResponseEvent()
                            .withStatusCode(200)
                            .withBody("DB Setup successful");
                }
            }
        } catch (SQLException | IOException sqlException) {
            context.getLogger().log("Error connection to the database:" + sqlException.getMessage());
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withBody("Error initializing the database");
        }
    }
}
