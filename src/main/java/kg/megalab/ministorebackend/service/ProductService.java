package kg.megalab.ministorebackend.service;

import kg.megalab.ministorebackend.entity.Product;
import kg.megalab.ministorebackend.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Product createProduct(Product product) {
        validateProductName(product);
        if (product.getPurchasePrice() == null ||
                product.getSalePrice() == null ||
                product.getPurchasePrice() < 0 ||
                product.getSalePrice() < 0) {
            throw new IllegalArgumentException("Invalid product price");
        }
        if (product.getStockQuantity() == null ||
                product.getStockQuantity() < 0) {
            throw new IllegalArgumentException("Invalid stock quantity");
        }
        if (product.getBarcode() != null &&
                productRepository.findByBarcode(product.getBarcode()).isPresent()) {
            throw new IllegalArgumentException("Barcode already exists");
        }

        return productRepository.save(product);
    }

    public Product updateProduct(Long id, Product product) {
        validateProductName(product);
        if (product.getPurchasePrice() == null ||
                product.getSalePrice() == null ||
                product.getPurchasePrice() < 0 ||
                product.getSalePrice() < 0 ||
                product.getStockQuantity() == null ||
                product.getStockQuantity() < 0) {

            throw new IllegalArgumentException("Invalid product data");
          }
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Product not found"));
        if (product.getBarcode() != null) {
            productRepository.findByBarcode(product.getBarcode())
                    .ifPresent(otherProduct -> {
                        if (!otherProduct.getId().equals(id)) {
                            throw new IllegalArgumentException(
                                    "Barcode already exists"
                            );
                        }
                    });
        }

        existingProduct.setBarcode(product.getBarcode());
        existingProduct.setName(product.getName());
        existingProduct.setPurchasePrice(product.getPurchasePrice());
        existingProduct.setSalePrice(product.getSalePrice());
        existingProduct.setStockQuantity(product.getStockQuantity());
        existingProduct.setActive(product.getActive());

        return productRepository.save(existingProduct);
    }

    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Product not found"));
    }

    public Product getProductByBarcode(String barcode) {
        return productRepository.findByBarcode(barcode)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Product not found"));
    }

    private void validateProductName(Product product) {
        if (product.getName() == null ||
                product.getName().isBlank()) {
            throw new IllegalArgumentException("Product name is required");
        }
    }
}
