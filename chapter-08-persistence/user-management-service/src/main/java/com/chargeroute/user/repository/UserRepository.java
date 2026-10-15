package com.chargeroute.user.repository;

import com.chargeroute.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, String> {

    /**
     * Find user by email address
     */
    Optional<UserEntity> findByEmail(String email);

    /**
     * Find all users with a specific status
     */
    List<UserEntity> findByStatus(UserEntity.UserStatus status);

    /**
     * Find all users with a specific membership type
     */
    List<UserEntity> findByMembershipType(String membershipType);

    /**
     * Check if a user exists by email
     */
    boolean existsByEmail(String email);

    /**
     * Find users by status and membership type
     */
    @Query("SELECT u FROM UserEntity u WHERE u.status = :status AND u.membershipType = :membershipType")
    List<UserEntity> findByStatusAndMembershipType(
            @Param("status") UserEntity.UserStatus status,
            @Param("membershipType") String membershipType
    );
}
