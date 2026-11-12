package kg.megalab.ministorebackend.repository;

import kg.megalab.ministorebackend.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByBarcodeAndIsActiveTrue(String barcode);
}
