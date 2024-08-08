package com.github.travelbuddy.board.mapper;

import com.github.travelbuddy.board.dto.BoardAllDto;
import com.github.travelbuddy.board.dto.BoardDetailDto;
import com.github.travelbuddy.board.dto.BoardMainSimpleDto;
import com.github.travelbuddy.board.dto.BoardSimpleDto;
import com.github.travelbuddy.board.entity.BoardEntity;
import com.github.travelbuddy.postImage.entity.PostImageEntity;
import com.github.travelbuddy.routes.entity.RouteDayEntity;
import com.github.travelbuddy.routes.entity.RouteEntity;
import com.github.travelbuddy.trip.entity.TripEntity;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Mapper
public interface BoardMapper {

    BoardMapper INSTANCE = Mappers.getMapper(BoardMapper.class);

    @Mapping(source = "user.name", target = "author")
    @Mapping(source = "route.startAt", target = "startAt", dateFormat = "yyyy-MM-dd")
    @Mapping(source = "route.endAt", target = "endAt", dateFormat = "yyyy-MM-dd")
    @Mapping(source = "postImages", target = "representativeImage", qualifiedByName = "firstImageUrl")
    BoardAllDto boardEntityToBoardAllDto(BoardEntity boardEntity);

    @Mapping(source = "user.id", target = "authorID")
    @Mapping(source = "user.name", target = "author")
    @Mapping(source = "user.profilePictureUrl", target = "userProfile")
    @Mapping(source = "postImages", target = "images", qualifiedByName = "mapPostImagesToUrls")
    @Mapping(target = "likeCount", ignore = true)
    BoardDetailDto.BoardDto toBoardDto(BoardEntity boardEntity);

    @Mapping(source = "routeDays", target = "routeDetails", qualifiedByName = "mapRouteDaysToDetails")
    BoardDetailDto.RouteDto toRouteDto(RouteEntity routeEntity);

    BoardDetailDto.TripDto toTripDto(TripEntity tripEntity);

    @Named("firstImageUrl")
    default String mapFirstImageUrl(List<PostImageEntity> postImages) {
        return postImages != null && !postImages.isEmpty() ? postImages.get(0).getUrl() : null;
    }

    @Named("mapPostImagesToUrls")
    default List<String> mapPostImagesToUrls(List<PostImageEntity> postImages) {
        return postImages != null ? postImages.stream().map(PostImageEntity::getUrl).collect(Collectors.toList()) : Collections.emptyList();
    }

    @Named("mapRouteDaysToDetails")
    default Map<String, List<Map<String, String>>> mapRouteDaysToDetails(List<RouteDayEntity> routeDays) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        return routeDays.stream()
                .flatMap(day -> day.getRouteDayPlaces().stream()
                        .map(place -> {
                            Map<String, String> placeDetails = new LinkedHashMap<>();
                            placeDetails.put("placeName", place.getPlaceName());
                            placeDetails.put("placeCategory", place.getPlaceCategory().name()); // Enum to String
                            return new AbstractMap.SimpleEntry<>(dateFormat.format(day.getDay()), placeDetails);
                        })
                )
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        LinkedHashMap::new,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())
                ));
    }

    @Mapping(target = "createdAt", expression = "java(formatDateTime(board.getCreatedAt()))")
    @Mapping(target = "representativeImage", source = "representativeImage")
    BoardSimpleDto boardEntityToBoardSimpleDto(BoardEntity board, String representativeImage);

    default String formatDateTime(LocalDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    @Mapping(target = "createdAt", expression = "java(formatDateTime(board.getCreatedAt()))")
    @Mapping(target = "representativeImage", expression = "java(getRepresentativeImage(board))")
    @Mapping(target = "author", source = "board.user.name")
    @Mapping(target = "likeCount", ignore = true)
    BoardMainSimpleDto boardEntityToBoardMainSimpleDto(BoardEntity board);


    default String getRepresentativeImage(BoardEntity board) {
        return board.getPostImages().isEmpty() ? null : board.getPostImages().get(0).getUrl();
    }
}
