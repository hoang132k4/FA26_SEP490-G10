package com.jlpp.modules.order.repository;

import com.jlpp.modules.order.entity.Order;
import com.jlpp.modules.order.entity.OrderStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = {"items"})
    List<Order> findByUserIdOrderByCreatedAtDescIdDesc(Long userId);

    @EntityGraph(attributePaths = {"items"})
    List<Order> findByUserIdAndOrderStatusOrderByCreatedAtDescIdDesc(Long userId, OrderStatus orderStatus);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.id = :id AND o.user.id = :userId")
    Optional<Order> findByIdAndUserIdWithItems(@Param("id") Long id, @Param("userId") Long userId);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.orderNumber = :orderNumber AND o.user.id = :userId")
    Optional<Order> findByOrderNumberAndUserIdWithItems(@Param("orderNumber") String orderNumber, @Param("userId") Long userId);

    Optional<Order> findByOrderNumber(String orderNumber);

    boolean existsByOrderNumber(String orderNumber);
}
