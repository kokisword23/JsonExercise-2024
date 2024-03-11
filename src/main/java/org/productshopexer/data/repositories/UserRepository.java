package org.productshopexer.data.repositories;

import org.productshopexer.data.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // user.getSold() ->  5   -> 1 -> Product().getBuyer = null;
    Set<User> findAllBySoldBuyerIsNotNull();
}
