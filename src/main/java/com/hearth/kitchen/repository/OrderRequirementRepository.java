package com.hearth.kitchen.repository;

import com.hearth.kitchen.model.OrderRequirement;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRequirementRepository extends JpaRepository<OrderRequirement, Long> {

    List<OrderRequirement> findByOrderId(Long orderId);
}
