package com.github.travelbuddy.board.repository;

import com.github.travelbuddy.board.dto.BoardAllDto;
import com.github.travelbuddy.board.dto.BoardSimpleDto;
import com.github.travelbuddy.board.entity.BoardEntity;
import com.github.travelbuddy.routes.entity.RouteDayEntity;
import com.github.travelbuddy.routes.entity.RouteEntity;
import com.github.travelbuddy.trip.entity.TripEntity;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface BoardRepository extends JpaRepository<BoardEntity, Integer> {

    @Query("SELECT b " +
            "FROM BoardEntity b " +
            "LEFT JOIN FETCH b.user u " +
            "LEFT JOIN FETCH b.route r " +
            "LEFT JOIN FETCH b.postImages pi " +
            "WHERE (:category IS NULL OR b.category = :category) " +
            "AND (:startDate IS NULL OR :endDate IS NULL OR (r.startAt <= :endDate AND r.endAt >= :startDate))")
    List<BoardEntity> findAllWithRepresentativeImageAndDateRange(@Param("category") BoardEntity.Category category, @Param("startDate") Date startDate, @Param("endDate") Date endDate, Sort sort);

    @Query("SELECT COUNT(l) FROM LikesEntity l WHERE l.board.id = :boardId")
    Long countLikesByBoardId(@Param("boardId") Integer boardId);


    @Query("SELECT b FROM BoardEntity b " +
            "LEFT JOIN FETCH b.user u " +
            "LEFT JOIN FETCH b.route r " +
            "LEFT JOIN FETCH b.postImages pi " +
            "WHERE b.id = :postId")
    BoardEntity findPostDetailsById(@Param("postId") Integer postId);

    @Query("SELECT r FROM RouteEntity r " +
            "LEFT JOIN FETCH r.routeDays rd " +
            "WHERE r.id = :routeId")
    RouteEntity findRouteDetailsByRouteId(@Param("routeId") Integer routeId);

    @Query("SELECT rd FROM RouteDayEntity rd " +
            "LEFT JOIN FETCH rd.routeDayPlaces rdp " +
            "WHERE rd.route.id = :routeId")
    List<RouteDayEntity> findRouteDayDetailsByRouteId(@Param("routeId") Integer routeId);

    @Query("SELECT t FROM TripEntity t " +
            "WHERE t.board.id = :postId")
    TripEntity findTripDetailsByPostId(@Param("postId") Integer postId);

    @Query("SELECT b " +
            "FROM BoardEntity b " +
            "LEFT JOIN FETCH b.postImages pi " +
            "WHERE b.user.id = :userId AND b.category = :category")
    List<BoardEntity> findBoardsByUserIdAndCategory(@Param("userId") Integer userId, @Param("category") BoardEntity.Category category);

    @Query("SELECT b " +
            "FROM BoardEntity b " +
            "LEFT JOIN FETCH b.user u " +
            "LEFT JOIN FETCH b.route r " +
            "LEFT JOIN FETCH b.postImages pi " +
            "LEFT JOIN LikesEntity l ON b.id = l.board.id " +
            "WHERE l.user.id = :userId " +
            "AND (:category IS NULL OR b.category = :category) " +
            "AND (:startDate IS NULL OR :endDate IS NULL OR (r.startAt <= :endDate AND r.endAt >= :startDate)) " +
            "GROUP BY b.id, u.id, r.id, pi.id")
    List<BoardEntity> findLikedPostsByUserIdAndCategory(
            @Param("userId") Integer userId,
            @Param("category") BoardEntity.Category category,
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate,
            Sort sort);

    @Query("SELECT b.id, b.title, b.createdAt, u.name, COUNT(l.id) as likeCount," +
            "(SELECT pi.url FROM PostImageEntity pi WHERE pi.board.id = b.id ORDER BY pi.id LIMIT 1) as representativeImage " +
            "FROM BoardEntity b LEFT JOIN LikesEntity l ON b.id = l.board.id " +
            "JOIN UserEntity u ON b.user.id = u.id " +
            "WHERE b.category = :category " +
            "GROUP BY b.id, b.title, b.createdAt " +
            "ORDER BY " +
            "CASE WHEN :sortBy = 'likeCount' THEN COUNT(l.id) END DESC, " +
            "CASE WHEN :sortBy = 'createdAt' THEN b.createdAt END DESC " +
            "LIMIT 4")
    List<Object[]> findTop4BoardsByCategoryWithRepresentativeImage(
            @Param("category") BoardEntity.Category category,
            @Param("sortBy") String sortBy);

    @Query("SELECT COUNT(b.id) FROM BoardEntity b WHERE b.user.id = :userId and b.category = :category")
    Integer countByUserIdAndCategory(@Param("userId") Integer userId, @Param("category") BoardEntity.Category category);

    @Query("SELECT b FROM BoardEntity b " +
            "JOIN b.route r " +
            "JOIN UsersInTravelEntity uit ON uit.trip.board.id = b.id " +
            "WHERE uit.user.id = :userId " +
            "AND (:category IS NULL OR b.category = :category) " +
            "AND (:startDate IS NULL OR :endDate IS NULL OR (r.startAt <= :endDate AND r.endAt >= :startDate)) " +
            "GROUP BY b.id " +
            "ORDER BY b.createdAt DESC")
    List<BoardEntity> findParticipatedTripsByUserWithLikeCountAndCategory(
            @Param("userId") Integer userId,
            @Param("category") BoardEntity.Category category,
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate,
            Sort sort);
}