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

    Optional<UsersInTravelEntity> findByUserAndTrip(UserEntity user, TripEntity trip);

    void deleteAllByTrip(TripEntity trip);
}
