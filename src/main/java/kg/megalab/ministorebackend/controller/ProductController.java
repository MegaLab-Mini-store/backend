
package kg.megalab.ministorebackend.controller;

import kg.megalab.ministorebackend.entity.Product;
import kg.megalab.ministorebackend.service.ProductService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<Product> getAllProducts() {
        return productService.getAllProducts();
    }

    @PostMapping
    public Product createProduct(@RequestBody Product product) {
        return productService.createProduct(product);
    }

    @PutMapping("/{id}")
    public Product updateProduct(@PathVariable Long id,
                                 @RequestBody Product product) {
        return productService.updateProduct(id, product);
    }
    @GetMapping("/{id}")
    public Product getProductById(@PathVariable Long id) {
        return productService.getProductById(id);
    }

    @GetMapping("/barcode/{barcode}")
    public Product getProductByBarcode(@PathVariable String barcode) {
        return productService.getProductByBarcode(barcode);
    }


}
