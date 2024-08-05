package com.github.travelbuddy.usersInTravel.repository;

import com.github.travelbuddy.board.entity.BoardEntity;
import com.github.travelbuddy.trip.entity.TripEntity;
import com.github.travelbuddy.users.entity.UserEntity;
import com.github.travelbuddy.usersInTravel.entity.UsersInTravelEntity;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface UsersInTravelRepository extends JpaRepository<UsersInTravelEntity , Integer> {
    @Query("SELECT b " +
            "FROM UsersInTravelEntity uit " +
            "JOIN uit.trip t " +
            "JOIN t.board b " +
            "JOIN b.route r " +
            "LEFT JOIN FETCH b.user u " +
            "LEFT JOIN FETCH b.postImages pi " +
            "LEFT JOIN LikesEntity l ON b.id = l.board.id " +
            "WHERE uit.user.id = :userId " +
            "AND (:category IS NULL OR b.category = :category) " +
            "AND (:startDate IS NULL OR :endDate IS NULL OR (r.startAt <= :endDate AND r.endAt >= :startDate)) " +
            "GROUP BY b.id, u.id, r.id, pi.id " +
            "ORDER BY b.createdAt DESC")
    List<BoardEntity> findParticipatedTripsByUserWithLikeCountAndCategory(
            @Param("userId") Integer userId,
            @Param("category") BoardEntity.Category category,
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate,
            Sort sort);

    Optional<UsersInTravelEntity> findByUserAndTrip(UserEntity user, TripEntity trip);

    void deleteAllByTrip(TripEntity trip);
}
