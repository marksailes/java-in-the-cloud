package software.sailes.carrental.infrastructure.db;

public record DbSecret(String password,
                       String dbname,
                       String engine,
                       String port,
                       String dbInstanceIdentifier,
                       String host,
                       String username) {
}
