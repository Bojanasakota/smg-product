package app.integration;

import app.model.entity.Product;
import app.model.event.ProductCreatedEvent;
import app.model.request.ProductRequest;
import app.repository.ProductRepository;
import app.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProductServiceIntegrationTest {

    private static final String PRODUCT_TOPIC = "product-topic";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final KafkaContainer kafkaContainer =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:latest"));

    private static final PostgreSQLContainer<?> postgreSQLContainer =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:latest"))
                    .withDatabaseName("testdb")
                    .withUsername("test")
                    .withPassword("test");

    static {
        postgreSQLContainer.start();
        kafkaContainer.start();
    }

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
    }

    @Test
    void testCreateProductAndKafkaMessage() throws Exception {
        // Create a product request and save it
        ProductRequest productRequest = new ProductRequest("Test Product Name", new BigDecimal("89.99"));
        Product createdProduct = productService.createProduct(productRequest);

        // Verify that the product is saved to the database
        Optional<Product> savedProduct = productRepository.findById(createdProduct.getId());
        assertTrue(savedProduct.isPresent());
        assertEquals("Test Product Name", savedProduct.get().getName());
        assertEquals(new BigDecimal("89.99"), savedProduct.get().getPrice());

        // Set up a Kafka consumer to verify the message
        Map<String, Object> consumerProps = new HashMap<>(KafkaTestUtils.consumerProps("testGroup", "true", kafkaContainer.getBootstrapServers()));

        // Configure Kafka properties for the test container
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        DefaultKafkaConsumerFactory<String, String> consumerFactory = new DefaultKafkaConsumerFactory<>(consumerProps);
        Consumer<String, String> consumer = consumerFactory.createConsumer();
        consumer.subscribe(Collections.singletonList(PRODUCT_TOPIC));

        // Consume the message and assert its contents
        ConsumerRecord<String, String> record = KafkaTestUtils.getSingleRecord(consumer, PRODUCT_TOPIC, Duration.ofSeconds(10));

        ProductCreatedEvent event = OBJECT_MAPPER.readValue(record.value(), ProductCreatedEvent.class);
        assertEquals(createdProduct.getId(), event.getProductId());
        assertEquals(createdProduct.getName(), event.getName());
        assertEquals(createdProduct.getPrice(), event.getPrice());

        consumer.close();
    }
}
