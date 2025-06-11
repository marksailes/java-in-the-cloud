# S3 Processing with Java and AWS Lambda

![Simple architecture diagram with S3, AWS Lambda and RDS](docs/simple-architecture.png)

## Introduction

In this project I will show you how to process files with Java which have been uploaded to S3. I use
the example of an electric car rental business, lets call it ECarRentals. ECarRentals has made agreements with different
charging providers. When an ECarRental car is charged at one of their points, they will upload a CSV file to an S3 
bucket with details of the car, and how much the car was charged. ECarRentals will process this data and pay each 
provider at the end of the month.

![Use case diagram show a car being charge and a csv file being uploaded to S3](docs/use-case.png)

## Solution

To solve this problem you will learn how to make an AWS Lambda function to listen to S3 Object Created events. Download
the files from S3, process them and save them to Postgres. Although the description sounds trivial there is a lot of 
detail to understand. 

### Architecture Diagram

![Detailed architecture diagram with networking and availability zones](docs/architecture-diagram.png)

Because this is an example I want people to experimental with I've designed the solution to make use of the AWS 
free-tier, and not use resources that are expensive to personal users and students. For example there are no NAT 
Gateways in this solution, and the RDS instance is available on the free tier.

We are using private subnets for our Lambda functions and database instance. There is no reason to make our database
accessible from a public ip address. So that our Lambda function can access the private database we have attached our
Lambda function to the VPC and are using security groups to further increase security.

To access S3 from a private subnet you will need a VPC Endpoint. If you don't have this your API calls to S3 will 
time out as there is no route to the public internet.

### Trade-offs

- Single RDS Postgres instance

## Deployment

### Requirements

- AWS Account
- Docker
- Java 21
- Maven
- AWS CDK

### Build 

Using Maven build the project

```bash
mvn clean package
```

This will build all the Java applications and run their tests. Docker is required to run the integration tests.

### Deploy to AWS

```bash
cd infrastructure/cdk
cdk deploy -all
```

### Testing

Log into the AWS console, browse to S3 and look for a bucket starting with the name `csv-uploads-bucket-`.

Upload a .csv file in the correct format. An example can be found in `software/s3-event-processor/src/test/resources/single-record.csv`

Once uploaded you Lambda function will execute and write the record to Postgres.

## Cost

| Service                                      |    Free Tier?     |                                                       Cost |
|:---------------------------------------------|:-----------------:|-----------------------------------------------------------:|
| S3                                           |        Yes        |                                              $0.023 per GB |
| Lambda                                       |        Yes        |      $0.20 per 1M requests<br/>2048MB	$0.0000000333 per ms |
| RDS                                          | Yes<br/>T4g.Micro |                                            $0.016 per hour | 
| VPC / S3 Gateway Endpoint                    |        Yes        |                                                       Free |
| Secrets Manager                              |        Yes        | $0.40 per secret per month<br/>$0.05 per 10,000 API calls. | 
| Interface VPC Endpoint (for Secrets Manager) |        No         |                                    ~$8.76 per month per AZ |                   

This information is provided to be a guide.

## Clean up

> [!TIP]
> From the CDK project use `cdk destroy -all` to remove all the project resources.

You can verify this by using AWS Resource Explorer to search for resources with the tag value of `car-rentals-s3-processing`

## Feedback

If you have any feedback on this project, or find an issue, please create a GitHub issue and I'll do my best to correct it.