package co.udea.crowdfunding.repository;

import co.udea.crowdfunding.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    List<Category> findAllByOrderByNameAsc();
}
