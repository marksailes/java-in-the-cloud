package software.sailes.carrental;

import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;

public class TestData {

    public static S3EventNotification.S3EventNotificationRecord createS3Record(String bucketName, String objectKey) {
        S3EventNotification.S3BucketEntity bucket = new S3EventNotification.S3BucketEntity(
                bucketName,
                new S3EventNotification.UserIdentityEntity("user-id"),
                "arn:aws:s3:::" + bucketName
        );

        S3EventNotification.S3ObjectEntity object = new S3EventNotification.S3ObjectEntity(
                objectKey,
                1024L,
                "etag123",
                "1.0",
                "sequencer123"
        );

        S3EventNotification.S3Entity s3Entity = new S3EventNotification.S3Entity(
                "configuration-id",
                bucket,
                object,
                "1.0"
        );

        return new S3EventNotification.S3EventNotificationRecord(
                "us-east-1",
                "event-name",
                "aws:s3",
                "2023-01-01T00:00:00.000Z",
                "1.0",
                new S3EventNotification.RequestParametersEntity("127.0.0.1"),
                new S3EventNotification.ResponseElementsEntity("request-id", "host-id"),
                s3Entity,
                new S3EventNotification.UserIdentityEntity("user-id")
        );
    }
}
