package com.github.travelbuddy.board.service;

import com.github.travelbuddy.board.dto.*;
import com.github.travelbuddy.board.entity.BoardEntity;
import com.github.travelbuddy.board.mapper.BoardMapper;
import com.github.travelbuddy.board.repository.BoardRepository;
import com.github.travelbuddy.comment.repository.CommentRepository;
import com.github.travelbuddy.common.service.S3Service;
import com.github.travelbuddy.common.util.UUIDUtil;
import com.github.travelbuddy.likes.repository.LikesRepository;
import com.github.travelbuddy.likes.service.LikesService;
import com.github.travelbuddy.postImage.entity.PostImageEntity;
import com.github.travelbuddy.postImage.repository.PostImageRepository;
import com.github.travelbuddy.routes.entity.RouteDayEntity;
import com.github.travelbuddy.routes.entity.RouteEntity;
import com.github.travelbuddy.routes.repository.RouteRepository;
import com.github.travelbuddy.trip.entity.TripEntity;
import com.github.travelbuddy.trip.repository.TripRepository;
import com.github.travelbuddy.trip.service.TripService;
import com.github.travelbuddy.users.dto.CustomUserDetails;
import com.github.travelbuddy.users.entity.UserEntity;
import com.github.travelbuddy.users.enums.Role;
import com.github.travelbuddy.users.repository.UserRepository;
import com.github.travelbuddy.usersInTravel.repository.UsersInTravelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BoardService {
    private final BoardRepository boardRepository;
    private final UserRepository userRepository;
    private final RouteRepository routeRepository;
    private final PostImageRepository postImageRepository;
    private final TripRepository tripRepository;
    private final S3Service s3Service;
    private final TripService tripService;
    private final UsersInTravelRepository usersInTravelRepository;
    private final LikesRepository likesRepository;
    private final LikesService likesService;
    private final CommentRepository commentRepository;

    public List<BoardAllDto> getAllBoards(String category, Date startDate, Date endDate, String sortBy, String order) {
        log.info("Category: " + category);
        log.info("StartDate: " + startDate);
        log.info("EndDate: " + endDate);
        log.info("SortBy: " + sortBy);
        log.info("Order: " + order);

        BoardEntity.Category categoryEnum = category != null ? BoardEntity.Category.valueOf(category) : null;

        List<BoardEntity> boardEntities;
        if ("likes".equals(sortBy)) {
            boardEntities = boardRepository.findAllWithRepresentativeImageAndDateRange(categoryEnum, startDate, endDate, Sort.unsorted());
        } else {
            Sort sort = Sort.by(Sort.Order.by(sortBy).with(Sort.Direction.fromString(order)));
            boardEntities = boardRepository.findAllWithRepresentativeImageAndDateRange(categoryEnum, startDate, endDate, sort);
        }

        List<BoardAllDto> boardDtos = boardEntities.stream().map(board -> {
            BoardAllDto dto = BoardMapper.INSTANCE.boardEntityToBoardAllDto(board);
            Long likeCount = boardRepository.countLikesByBoardId(board.getId());
            dto.setLikeCount(likeCount);
            return dto;
        }).collect(Collectors.toList());

        if ("likes".equals(sortBy)) {
            boardDtos = boardDtos.stream()
                    .sorted((b1, b2) -> "desc".equals(order) ? b2.getLikeCount().compareTo(b1.getLikeCount()) : b1.getLikeCount().compareTo(b2.getLikeCount()))
                    .collect(Collectors.toList());
        }

        return boardDtos;
    }

    public BoardDetailDto getPostDetails(Integer postId) {
        BoardEntity boardEntity = boardRepository.findPostDetailsById(postId);

        if (boardEntity == null) {
            log.error("이 게시물 아이디로 찾은 쿼리 결과가 없습니다. " + postId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "게시물의 관련된 정보를 찾을 수 없습니다.");
        }

        Long likeCount = boardRepository.countLikesByBoardId(postId);

        BoardDetailDto.BoardDto boardDto = BoardMapper.INSTANCE.toBoardDto(boardEntity);
        boardDto.setLikeCount(likeCount);

        RouteEntity routeEntity = boardRepository.findRouteDetailsByRouteId(boardEntity.getRoute().getId());
        List<RouteDayEntity> routeDayEntities = boardRepository.findRouteDayDetailsByRouteId(routeEntity.getId());
        routeEntity.setRouteDays(routeDayEntities);

        BoardDetailDto.RouteDto routeDto = BoardMapper.INSTANCE.toRouteDto(routeEntity);

        TripEntity tripEntity = boardRepository.findTripDetailsByPostId(postId);
        BoardDetailDto.TripDto tripDto = BoardMapper.INSTANCE.toTripDto(tripEntity);

        return new BoardDetailDto(boardDto, routeDto, tripDto);
    }

    public BoardResponseDto<BoardSimpleDto> getBoardsByUserAndCategory(CustomUserDetails userDetails, BoardEntity.Category category) {
        Integer userId = userDetails.getUserId();
        List<Object[]> results = boardRepository.findBoardsByUserIdAndCategory(userId, category);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        List<BoardSimpleDto> boardSimpleDtos =  results.stream().map(result -> new BoardSimpleDto(
                (Integer) result[0],
                (String) result[1],
                (String) result[2],
                (String) result[3],
                (BoardEntity.Category) result[4],
                ((LocalDateTime) result[5]).format(formatter)
        )).collect(Collectors.toList());

        String message;
        if (boardSimpleDtos.isEmpty()) {
            switch (category) {
                case REVIEW:
                    message = "아직 작성한 후기가 없습니다.";
                    break;
                case COMPANION:
                    message = "아직 작성한 동행 게시글이 없습니다.";
                    break;
                case GUIDE:
                    message = "아직 작성한 가이드 게시글이 없습니다.";
                    break;
                default:
                    message = "아직 작성한 게시물이 없습니다.";
            }
        } else {
            message = "게시물을 성공정으로 조회했습니다.";
        }
        return new BoardResponseDto<>(message , boardSimpleDtos);
    }

    public List<BoardAllDto> getLikedPostsByUser(CustomUserDetails userDetails, BoardEntity.Category category, Date startDate, Date endDate, String sortBy, String order) {
        log.info("UserId: " + userDetails.getUserId());
        log.info("Category: " + category);
        log.info("StartDate: " + startDate);
        log.info("EndDate: " + endDate);
        log.info("SortBy: " + sortBy);
        log.info("Order: " + order);

        Integer userId = userDetails.getUserId();

        List<BoardEntity> boardEntities;
        if ("likes".equals(sortBy)) {
            boardEntities = boardRepository.findLikedPostsByUserIdAndCategory(userId, category, startDate, endDate , Sort.unsorted());
        } else {
            Sort sort = Sort.by(Sort.Order.by(sortBy).with(Sort.Direction.fromString(order)));
            boardEntities = boardRepository.findLikedPostsByUserIdAndCategory(userId, category, startDate, endDate , sort);
        }

        List<BoardAllDto> boardDtos = boardEntities.stream().map(board -> {
            BoardAllDto dto = BoardMapper.INSTANCE.boardEntityToBoardAllDto(board);
            Long likeCount = boardRepository.countLikesByBoardId(board.getId());
            dto.setLikeCount(likeCount);
            return dto;
        }).collect(Collectors.toList());

        if ("likes".equals(sortBy)) {
            boardDtos = boardDtos.stream()
                    .sorted((b1, b2) -> "desc".equals(order) ? b2.getLikeCount().compareTo(b1.getLikeCount()) : b1.getLikeCount().compareTo(b2.getLikeCount()))
                    .collect(Collectors.toList());
        }

        return boardDtos;
    }

    @Transactional
    public void createBoard(BoardCreateDto createDto, CustomUserDetails userDetails) throws IOException{
        Integer userId = userDetails.getUserId();
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"유저 찾을 수 없음"));
        RouteEntity route = routeRepository.findById(createDto.getRouteId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"경로 찾을 수 없음"));

        BoardEntity board = BoardEntity.builder()
                .user(user)
                .route(route)
                .title(createDto.getTitle())
                .summary(createDto.getSummary())
                .content(createDto.getContent())
                .category(createDto.getCategory())
                .createdAt(LocalDateTime.now())
                .build();

        boardRepository.save(board);

        if (createDto.getImages() != null) {
            for (MultipartFile image : createDto.getImages()) {
                String imageUrl = s3Service.uploadFile(image);
                PostImageEntity postImage = PostImageEntity.builder()
                        .board(board)
                        .url(imageUrl)
                        .build();
                postImageRepository.save(postImage);
            }
        }

        if (createDto.getCategory() == BoardEntity.Category.COMPANION || createDto.getCategory() == BoardEntity.Category.GUIDE){
            tripService.createTrip(user, board, createDto.getAgeMin(), createDto.getAgeMax(), createDto.getTargetNumber(), TripEntity.Gender.valueOf(createDto.getGender()));
        }

        if(user.getRole().equals(Role.USER)){
            Integer boardCnt = boardRepository.countByUserIdAndCategory(userId, BoardEntity.Category.COMPANION);
            LocalDateTime currentTime = LocalDateTime.now();
            LocalDateTime signupTime = user.getCreatedAt();

            Period signupDuration = Period.between(signupTime.toLocalDate(), currentTime.toLocalDate());

            if(boardCnt >= 20 && signupDuration.toTotalMonths() >= 6){
                UserEntity updateUser = user.toBuilder().role(Role.ALL).build();
                userRepository.save(updateUser);
            }
        }
    }

    @Transactional
    public void updateBoard(BoardCreateDto updateDto, CustomUserDetails userDetails, Integer postId) throws IOException{
        Integer userId = userDetails.getUserId();
        BoardEntity board = boardRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"게시글을 찾을 수 없습니다."));
        RouteEntity route = routeRepository.findById(updateDto.getRouteId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"경로 찾을 수 없음"));

        if (!board.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"게시글을 수정할 권한이 없습니다.");
        }

        board.setRoute(route);
        board.setTitle(updateDto.getTitle());
        board.setSummary(updateDto.getSummary());
        board.setContent(updateDto.getContent());
        board.setCategory(updateDto.getCategory());
        board.setCreatedAt(LocalDateTime.now());

        boardRepository.save(board);

        if (updateDto.getImages() != null) {
            List<PostImageEntity> existingImages = postImageRepository.findAllByBoard(board);
            for (PostImageEntity image : existingImages) {
                s3Service.deleteFile(image.getUrl());
                postImageRepository.delete(image);
            }

            for (MultipartFile image : updateDto.getImages()) {
                String imageUrl = s3Service.uploadFile(image);
                PostImageEntity postImage = PostImageEntity.builder()
                        .board(board)
                        .url(imageUrl)
                        .build();
                postImageRepository.save(postImage);
            }
        }

        if (updateDto.getCategory() == BoardEntity.Category.COMPANION || updateDto.getCategory() == BoardEntity.Category.GUIDE) {
            tripService.updateTrip(userId , board, updateDto.getAgeMin(), updateDto.getAgeMax(), updateDto.getTargetNumber(), TripEntity.Gender.valueOf(updateDto.getGender()));
        }
    }

    @Transactional
    public void deleteBoard(Integer postId, CustomUserDetails userDetails) {
        Integer userId = userDetails.getUserId();
        BoardEntity board = boardRepository.findById(postId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND ,"게시글을 찾을 수 없습니다."));
        TripEntity trip = tripRepository.findByBoard(board).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND ,"여행 정보를 찾을 수 없습니다."));

        if (!board.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"게시글을 삭제할 권한이 없습니다.");
        }

        // 게시글의 모든 이미지 삭제
        postImageRepository.deleteAllByBoard(board);

        // 여행정보로 관련 모든 여행참여자 정보 삭제
        usersInTravelRepository.deleteAllByTrip(trip);

        // 게시글 정보로 여행정보 삭제
        tripRepository.deleteByBoard(board);

        // 게시글 정보로 관련 모든 좋아요 삭제
        likesRepository.deleteAllByBoard(board);

        // 게시글 정보로 관련 모든 댓글 삭제
        commentRepository.deleteAllByBoard(board);

        boardRepository.delete(board);
    }

    public BoardResponseDto<BoardAllDto> getParticipatedTripsByUser(CustomUserDetails userDetails , BoardEntity.Category category, Date startDate, Date endDate, String sortBy, String order) {
        Integer userId = userDetails.getUserId();
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"유저를 찾을 수 없습니다."));

        if (category.equals(BoardEntity.Category.REVIEW)){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST , "리뷰 카테고리는 조회할 수 없습니다");
        }

        List<Object[]> results = usersInTravelRepository.findBoardsByUserWithLikeCountAndCategory(user, category, startDate, endDate, sortBy, order);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        List<BoardAllDto> participatedTrips = results.stream().map(result -> {
            BoardEntity board = (BoardEntity) result[0];
            Long likeCount = (Long) result[1];
            String startAt = dateFormat.format(board.getRoute().getStartAt());
            String endAt = dateFormat.format(board.getRoute().getEndAt());
            return new BoardAllDto(
                    board.getId(),
                    board.getCategory(),
                    board.getTitle(),
                    board.getSummary(),
                    board.getUser().getName(),
                    startAt,
                    endAt,
                    board.getPostImages().isEmpty() ? null : board.getPostImages().get(0).getUrl(),
                    likeCount
            );
        }).collect(Collectors.toList());

        if(participatedTrips.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "조회할 수 있는 데이터가 없습니다.");
        }else {
            String message = "참여한 여행 게시물을 성공적으로 조회했습니다.";
            return new BoardResponseDto<>(message, participatedTrips);
        }
        }

        public BoardMainDto getTop4BoardsByCategories() {
            List<BoardMainSimpleDto> top4ReviewBoards = getTop4BoardsByCategory(BoardEntity.Category.REVIEW, "likeCount");
            List<BoardMainSimpleDto> top4GuideBoards = getTop4BoardsByCategory(BoardEntity.Category.GUIDE, "createdAt");
            List<BoardMainSimpleDto> top4CompanionBoards = getTop4BoardsByCategory(BoardEntity.Category.COMPANION, "createdAt");

            return new BoardMainDto(top4ReviewBoards, top4GuideBoards, top4CompanionBoards);
        }

        private List<BoardMainSimpleDto> getTop4BoardsByCategory(BoardEntity.Category category, String sortBy) {
            List<Object[]> results = boardRepository.findTop4BoardsByCategoryWithRepresentativeImage(category, sortBy);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            return results.stream().map(result -> {
                Integer id = (Integer) result[0];
                String title = (String) result[1];
                String createdAt = ((LocalDateTime) result[2]).format(formatter);
                String author = (String) result[3];
                Long likeCount = (Long) result[4];
                String representativeImage = (String) result[5];

                return new BoardMainSimpleDto(id, title, representativeImage, author, createdAt, likeCount);
            }).collect(Collectors.toList());
        }
    }