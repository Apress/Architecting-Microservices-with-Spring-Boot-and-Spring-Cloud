package com.chargeroute.user.repository;

import com.chargeroute.user.entity.UserPreferencesEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

/**
 * Reactive repository for UserPreferences entities using R2DBC.
 */
@Repository
public interface UserPreferencesRepository extends R2dbcRepository<UserPreferencesEntity, String> {

    /**
     * Find all users with notifications enabled
     *
     * @return Flux<UserPreferencesEntity> - emits 0 to N preferences
     */
    Flux<UserPreferencesEntity> findByNotificationsEnabled(Boolean enabled);

    /**
     * Find all users with a specific notification channel preference
     *
     * @return Flux<UserPreferencesEntity> - emits 0 to N preferences
     */
    Flux<UserPreferencesEntity> findByNotificationChannel(String channel);

    /**
     * Find all users with auto-charge enabled
     *
     * @return Flux<UserPreferencesEntity> - emits 0 to N preferences
     */
    Flux<UserPreferencesEntity> findByAutoChargeEnabled(Boolean enabled);
}
