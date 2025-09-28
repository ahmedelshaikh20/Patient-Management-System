package org.example;

import software.amazon.awscdk.*;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.ecs.Protocol;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.msk.CfnCluster;
import software.amazon.awscdk.services.rds.*;
import software.amazon.awscdk.services.route53.CfnHealthCheck;

import java.util.*;
import java.util.stream.Collectors;

public class LocalStack extends Stack {

  private final Vpc vpc;
  private final Cluster ecsCluster;

  // Control MSK creation via env var (defaults to false for LocalStack)
  // Set CREATE_MSK=true when deploying to real AWS if you want MSK created.
  private final boolean createMsk = Boolean.parseBoolean(
    Optional.ofNullable(System.getenv("CREATE_MSK")).orElse("false")
  );

  public LocalStack(final App scope, final String id, final StackProps props) {
    super(scope, id, props);

    this.vpc = createVpc();

    DatabaseInstance authServiceDb = createDatabaseInstance("AuthServiceDb", "auth-service-db");
    DatabaseInstance patientServiceDb = createDatabaseInstance("PatientServiceDb", "patient-service-db");

    CfnHealthCheck authDbHealthCheck = createDbHealthCheck(authServiceDb, "AuthServiceDBHealthCheck");
    CfnHealthCheck patientDbHealthCheck = createDbHealthCheck(patientServiceDb, "PatientServiceDBHealthCheck");

    // Create ECS cluster
    this.ecsCluster = createEcsCluster();

    // Conditionally create MSK (skip on LocalStack)
    CfnCluster mskCluster = createMsk ? createMskCluster() : null;

    // === Services ===
    FargateService authService = createFargateService(
      "AuthService",
      "auth-service",
      List.of(4005),
      authServiceDb,
      Map.of("JWT_SECRET", "Y2hhVEc3aHJnb0hYTzMyZ2ZqVkpiZ1RkZG93YWxrUkM=")
    );
    authService.getNode().addDependency(authDbHealthCheck);
    authService.getNode().addDependency(authServiceDb);

    FargateService billingService = createFargateService(
      "BillingService",
      "billing-service",
      List.of(4001, 9001),
      null,
      null
    );

    FargateService analyticsService = createFargateService(
      "AnalyticsService",
      "analytics-service",
      List.of(4002),
      null,
      null
    );
    if (mskCluster != null) {
      analyticsService.getNode().addDependency(mskCluster);
    }

    FargateService patientService = createFargateService(
      "PatientService",
      "patient-service",
      List.of(4000),
      patientServiceDb,
      Map.of(
        "BILLING_SERVICE_ADDRESS", "host.docker.internal",
        "BILLING_SERVICE_GRPC_PORT", "9001"
      )
    );
    patientService.getNode().addDependency(patientServiceDb);
    patientService.getNode().addDependency(patientDbHealthCheck);
    patientService.getNode().addDependency(billingService);
    if (mskCluster != null) {
      patientService.getNode().addDependency(mskCluster);
    }

    createApiGatewayService();
  }

  private Cluster createEcsCluster() {
    return Cluster.Builder.create(this , "PatientManagementCluster")
      .vpc(vpc)
      .defaultCloudMapNamespace(CloudMapNamespaceOptions.builder()
        .name("patient-management-name")
        .build())
      .build();
  }

  private Vpc createVpc() {
    return Vpc.Builder
      .create(this, "PatientManagementVPC")
      .vpcName("PatientManagementVPC")
      .maxAzs(2) // keep 2 AZs; MSK will use 2 brokers if enabled
      .build();
  }

  private CfnCluster createMskCluster() {
    // IMPORTANT: For 2 AZs, numberOfBrokerNodes must be a multiple of 2.
    return CfnCluster.Builder.create(this, "MskCluster")
      .clusterName("kafka-cluster")
      .kafkaVersion("2.8.0")
      .numberOfBrokerNodes(2) // ✅ one broker per AZ
      .brokerNodeGroupInfo(CfnCluster.BrokerNodeGroupInfoProperty.builder()
        .instanceType("kafka.m5.xlarge")
        .clientSubnets(vpc.getPrivateSubnets().stream()
          .map(ISubnet::getSubnetId)
          .collect(Collectors.toList()))
        .brokerAzDistribution("DEFAULT")
        .build())
      .build();
  }

  private DatabaseInstance createDatabaseInstance(String id, String dbName) {
    return DatabaseInstance.Builder.create(this, id)
      .engine(DatabaseInstanceEngine.postgres(
        PostgresInstanceEngineProps.builder().version(PostgresEngineVersion.VER_17_2).build()
      ))
      .vpc(vpc)
      .instanceType(InstanceType.of(InstanceClass.BURSTABLE2, InstanceSize.MICRO))
      .credentials(Credentials.fromGeneratedSecret("admin"))
      .databaseName(dbName)
      .removalPolicy(RemovalPolicy.DESTROY)
      .build();
  }

  private CfnHealthCheck createDbHealthCheck(DatabaseInstance db, String id) {
    return CfnHealthCheck.Builder.create(this, id)
      .healthCheckConfig(CfnHealthCheck.HealthCheckConfigProperty.builder()
        .type("TCP")
        .port(Token.asNumber(db.getDbInstanceEndpointPort()))
        .ipAddress(db.getDbInstanceEndpointAddress())
        .requestInterval(30)
        .failureThreshold(3)
        .build())
      .build();
  }

  private FargateService createFargateService(String id,
                                              String imageName,
                                              List<Integer> ports,
                                              DatabaseInstance db,
                                              Map<String, String> additionalEnvVars) {

    FargateTaskDefinition taskDefinition =
      FargateTaskDefinition.Builder.create(this, id + "Task")
        .cpu(256)
        .memoryLimitMiB(512)
        .build();

    // Build env vars
    Map<String, String> envVars = new HashMap<>();
    // keep your local Kafka bootstrap servers (works with LocalStack skip or external Kafka)
    envVars.put("SPRING_KAFKA_BOOTSTRAP_SERVERS",
      "localhost.localstack.cloud:4510, localhost.localstack.cloud:4511, localhost.localstack.cloud:4512");

    if (additionalEnvVars != null) {
      envVars.putAll(additionalEnvVars);
    }

    if (db != null) {
      envVars.put("SPRING_DATASOURCE_URL", String.format(
        "jdbc:postgresql://%s:%s/%s-db",
        db.getDbInstanceEndpointAddress(),
        db.getDbInstanceEndpointPort(),
        imageName
      ));
      envVars.put("SPRING_DATASOURCE_USERNAME", "admin");
      envVars.put("SPRING_DATASOURCE_PASSWORD", db.getSecret().secretValueFromJson("password").toString());
      envVars.put("SPRING_JPA_HIBERNATE_DDL_AUTO", "update");
      envVars.put("SPRING_SQL_INIT_MODE", "always");
      envVars.put("SPRING_DATASOURCE_HIKARI_INITIALIZATION_FAIL_TIMEOUT", "60000");
    }

    // Avoid log group name collisions across redeploys:
    String logGroupName = "/ecs/" + imageName + "-" + Stack.of(this).getStackName();

    ContainerDefinitionOptions containerOptions =
      ContainerDefinitionOptions.builder()
        .image(ContainerImage.fromRegistry(imageName))
        .environment(envVars)
        .portMappings(ports.stream()
          .map(port -> PortMapping.builder()
            .containerPort(port)
            .hostPort(port)
            .protocol(Protocol.TCP)
            .build())
          .toList())
        .logging(LogDriver.awsLogs(AwsLogDriverProps.builder()
          .logGroup(LogGroup.Builder.create(this, id + "LogGroup")
            .logGroupName(logGroupName)
            .removalPolicy(RemovalPolicy.DESTROY)
            .retention(RetentionDays.ONE_DAY)
            .build())
          .streamPrefix(imageName)
          .build()))
        .build();

    taskDefinition.addContainer(imageName + "Container", containerOptions);

    return FargateService.Builder.create(this, id)
      .cluster(ecsCluster)
      .taskDefinition(taskDefinition)
      .assignPublicIp(false)
      .serviceName(imageName)
      .build();
  }

  private void createApiGatewayService() {
    FargateTaskDefinition taskDefinition =
      FargateTaskDefinition.Builder.create(this, "APIGatewayTaskDefinition")
        .cpu(256)
        .memoryLimitMiB(512)
        .build();

    String apiGwLogGroup = "/ecs/api-gateway-" + Stack.of(this).getStackName();

    ContainerDefinitionOptions containerOptions =
      ContainerDefinitionOptions.builder()
        .image(ContainerImage.fromRegistry("api-gateway"))
        .environment(Map.of(
          "SPRING_PROFILES_ACTIVE", "prod",
          "AUTH_SERVICE_URL", "http://host.docker.internal:4005"
        ))
        .portMappings(List.of(4004).stream()
          .map(port -> PortMapping.builder()
            .containerPort(port)
            .hostPort(port)
            .protocol(Protocol.TCP)
            .build())
          .toList())
        .logging(LogDriver.awsLogs(AwsLogDriverProps.builder()
          .logGroup(LogGroup.Builder.create(this, "ApiGatewayLogGroup")
            .logGroupName(apiGwLogGroup)
            .removalPolicy(RemovalPolicy.DESTROY)
            .retention(RetentionDays.ONE_DAY)
            .build())
          .streamPrefix("api-gateway")
          .build()))
        .build();

    taskDefinition.addContainer("APIGatewayContainer", containerOptions);

    ApplicationLoadBalancedFargateService apiGateway =
      ApplicationLoadBalancedFargateService.Builder.create(this, "APIGatewayService")
        .cluster(ecsCluster)
        .serviceName("api-gateway")
        .taskDefinition(taskDefinition)
        .desiredCount(1)
        .healthCheckGracePeriod(Duration.seconds(60))
        .build();
  }

  public static void main(final String[] args) {
    App app = new App(AppProps.builder().outdir("./cdk.out").build());
    StackProps props = StackProps.builder()
      .synthesizer(new BootstraplessSynthesizer())
      .build();

    // NOTE: stack id must match ^[A-Za-z][A-Za-z0-9-]*$ (no underscores)
    new LocalStack(app, "local-stack", props);
    app.synth();
    System.out.println("App Synthesizing in Progress......");
  }
}
