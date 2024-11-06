package app.service;

import app.model.entity.Product;
import app.model.event.ProductCreatedEvent;
import app.model.request.ProductRequest;
import app.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private KafkaTemplate<String, ProductCreatedEvent> kafkaTemplate;

    private ProductService productService;

    private final String productTopic = "product-topic";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        productService = new ProductService(productRepository, kafkaTemplate, productTopic);
    }

    @Test
    void testCreateProduct() {
        ProductRequest productRequest = new ProductRequest("Test Product", new BigDecimal("100.0"));
        Product savedProduct = new Product(productRequest);
        savedProduct.setId(1L);

        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);
        when(kafkaTemplate.send(anyString(), anyString(), any(ProductCreatedEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        Product result = productService.createProduct(productRequest);

        assertNotNull(result);
        assertEquals("Test Product", result.getName());
        assertEquals(new BigDecimal("100.0"), result.getPrice());

        verify(productRepository, times(1)).save(any(Product.class));
        verify(kafkaTemplate, times(1)).send(eq(productTopic), anyString(), any(ProductCreatedEvent.class));
    }

    @Test
    void testGetProductById_Found() {
        Product product = new Product();
        product.setId(1L);
        product.setName("Test Product");
        product.setPrice(new BigDecimal("100.0"));

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        Optional<Product> result = productService.getProductById(1L);

        assertTrue(result.isPresent());
        assertEquals("Test Product", result.get().getName());
        assertEquals(new BigDecimal("100.0"), result.get().getPrice());
        verify(productRepository, times(1)).findById(1L);
    }

    @Test
    void testGetProductById_NotFound() {
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        Optional<Product> result = productService.getProductById(1L);

        assertFalse(result.isPresent());
        verify(productRepository, times(1)).findById(1L);
    }

    @Test
    void testGetAllProducts() {
        Product product1 = new Product("Product 1", new BigDecimal("50.0"));
        Product product2 = new Product("Product 2", new BigDecimal("150.0"));
        Page<Product> productPage = new PageImpl<>(Arrays.asList(product1, product2));

        when(productRepository.findAll(any(PageRequest.class))).thenReturn(productPage);

        Page<Product> result = productService.getAllProducts(0, 2);

        assertEquals(2, result.getContent().size());
        assertEquals("Product 1", result.getContent().get(0).getName());
        assertEquals("Product 2", result.getContent().get(1).getName());
        verify(productRepository, times(1)).findAll(any(PageRequest.class));
    }

    @Test
    void testUpdateProduct_Success() {
        ProductRequest productRequest = new ProductRequest("Updated Product", new BigDecimal("200.0"));
        Product existingProduct = new Product("Old Product", new BigDecimal("100.0"));
        existingProduct.setId(1L);

        when(productRepository.findById(1L)).thenReturn(Optional.of(existingProduct));
        when(productRepository.save(any(Product.class))).thenReturn(existingProduct);

        Optional<Product> result = productService.updateProduct(1L, productRequest);

        assertTrue(result.isPresent());
        assertEquals("Updated Product", result.get().getName());
        assertEquals(new BigDecimal("200.0"), result.get().getPrice());
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    void testUpdateProduct_NotFound() {
        ProductRequest productRequest = new ProductRequest("Updated Product", new BigDecimal("200.0"));

        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        Optional<Product> result = productService.updateProduct(1L, productRequest);

        assertFalse(result.isPresent());
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void testDeleteProduct_Success() {
        when(productRepository.existsById(1L)).thenReturn(true);
        doNothing().when(productRepository).deleteById(1L);

        boolean result = productService.deleteProduct(1L);

        assertTrue(result);
        verify(productRepository, times(1)).deleteById(1L);
    }

    @Test
    void testDeleteProduct_NotFound() {
        when(productRepository.existsById(1L)).thenReturn(false);

        boolean result = productService.deleteProduct(1L);

        assertFalse(result);
        verify(productRepository, never()).deleteById(anyLong());
    }
}
