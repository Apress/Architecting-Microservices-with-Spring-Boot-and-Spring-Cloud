package com.chargeroute.user.repository;

import com.chargeroute.user.entity.UserEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Reactive repository for User entities using R2DBC.
 * Returns Mono for single results, Flux for multiple results.
 */
@Repository
public interface UserRepository extends R2dbcRepository<UserEntity, String> {

    /**
     * Find user by email address
     *
     * @return Mono<UserEntity> - emits single user or empty if not found
     */
    Mono<UserEntity> findByEmail(String email);

    /**
     * Find all users with a specific status
     *
     * @return Flux<UserEntity> - emits 0 to N users
     */
    Flux<UserEntity> findByStatus(UserEntity.UserStatus status);

    /**
     * Find all users with a specific membership type
     *
     * @return Flux<UserEntity> - emits 0 to N users
     */
    Flux<UserEntity> findByMembershipType(String membershipType);

    /**
     * Check if a user exists by email
     *
     * @return Mono<Boolean> - emits true if exists, false otherwise
     */
    Mono<Boolean> existsByEmail(String email);

    /**
     * Find users by status and membership type using custom query
     * Note: R2DBC uses SQL, not JPQL
     *
     * @return Flux<UserEntity> - emits 0 to N users
     */
    @Query("SELECT * FROM users WHERE status = :status AND membership_type = :membershipType")
    Flux<UserEntity> findByStatusAndMembershipType(
            @Param("status") String status,
            @Param("membershipType") String membershipType
    );
}
