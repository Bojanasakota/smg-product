package app.service;

import app.model.event.ProductCreatedEvent;
import app.model.response.ProductResponse;
import jakarta.persistence.OptimisticLockException;
import jakarta.transaction.Transactional;
import app.model.entity.Product;
import app.model.request.ProductRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import app.repository.ProductRepository;

import java.util.Optional;
import java.util.UUID;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final KafkaTemplate<String, ProductCreatedEvent> kafkaTemplate;
    private final String productTopic;

    @Autowired
    public ProductService(ProductRepository productRepository, KafkaTemplate<String, ProductCreatedEvent> kafkaTemplate,
                          @Value("${kafka.topic.name}") String productTopic) {
        this.productRepository = productRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.productTopic = productTopic;
    }

    @Transactional
    public Product createProduct(ProductRequest product) {
        Product savedProduct = productRepository.save(new Product(product));
        ProductCreatedEvent productCreatedEvent = new ProductCreatedEvent(savedProduct.getId(),
                savedProduct.getName(), savedProduct.getPrice());
        kafkaTemplate.send(productTopic, savedProduct.getId().toString(), productCreatedEvent);
        return savedProduct;
    }

    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    public Page<Product> getAllProducts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return productRepository.findAll(pageable);
    }

    @Transactional
    public Optional<Product> updateProduct(Long id, ProductRequest productRequest) {
        Optional<Product> productOptional = productRepository.findById(id);

        if (productOptional.isPresent()) {
            Product product = productOptional.get();
            product.setName(productRequest.getName());
            product.setPrice(productRequest.getPrice());

            try {
                Product savedProduct = productRepository.save(product);
                return Optional.of(savedProduct);
            } catch (OptimisticLockException e) {
                System.out.println("Ignoring update because of concurrent modification");
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }

    @Transactional
    public boolean deleteProduct(Long id) {
        if (productRepository.existsById(id)) {
            productRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public static ProductResponse mapToProductResponse(Product product) {
        return new ProductResponse(product.getId(), product.getName(), product.getPrice());
    }
}

