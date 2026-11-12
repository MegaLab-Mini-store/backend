package kg.megalab.ministorebackend.repository;

import kg.megalab.ministorebackend.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>{
    Optional<Product> findByBarcode(String barcode);
}