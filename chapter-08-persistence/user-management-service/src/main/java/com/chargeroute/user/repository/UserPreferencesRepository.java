package com.chargeroute.user.repository;

import com.chargeroute.user.entity.UserPreferencesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserPreferencesRepository extends JpaRepository<UserPreferencesEntity, String> {

    /**
     * Find all users with notifications enabled
     */
    List<UserPreferencesEntity> findByNotificationsEnabled(Boolean enabled);

    /**
     * Find all users with a specific notification channel preference
     */
    List<UserPreferencesEntity> findByNotificationChannel(String channel);

    /**
     * Find all users with auto-charge enabled
     */
    List<UserPreferencesEntity> findByAutoChargeEnabled(Boolean enabled);
}
