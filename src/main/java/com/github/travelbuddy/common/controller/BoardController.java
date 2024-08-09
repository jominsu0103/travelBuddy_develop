package com.github.travelbuddy.common.controller;

import com.github.travelbuddy.board.dto.*;
import com.github.travelbuddy.board.entity.BoardEntity;
import com.github.travelbuddy.board.service.BoardService;
import com.github.travelbuddy.users.dto.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Date;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/boards")
@Slf4j
public class BoardController {
    private final BoardService boardService;


    @GetMapping
    public List<BoardAllDto> getAllBoards(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date endDate,
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String order) {
        log.info("게시물 전체조회 API 실행");
        return boardService.getAllBoards(category, startDate, endDate, sortBy, order);
    }

    @GetMapping("/{postId}")
    public ResponseEntity<?> getPostDetails(@PathVariable Integer postId) {
        try{
            log.info("게시물 상세조회 API 실행");
            BoardDetailDto boardDetailDto =  boardService.getPostDetails(postId);
            return ResponseEntity.status(HttpStatus.OK).body(boardDetailDto);
        }catch (ResponseStatusException e){
            log.error("게시물 상세정보 조회 중 에러 발생" , e);
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());

        }
    }

    @GetMapping("/my")
    public ResponseEntity<?> getBoardsByUserAndCategory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false , defaultValue = "REVIEW") BoardEntity.Category category) {
            log.info("내가 작성한 게시물 조회 API 실행");
            BoardResponseDto<BoardSimpleDto> results = boardService.getBoardsByUserAndCategory(userDetails, category);
            return ResponseEntity.status(HttpStatus.OK).body(results);
    }

    @GetMapping("/liked")
    public ResponseEntity<BoardResponseDto<BoardAllDto>> getLikedPostsByUser(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) BoardEntity.Category category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date endDate,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String order) {
        log.info("내가 추천한 게시물 조회 API 실행");
        List<BoardAllDto> likedPosts = boardService.getLikedPostsByUser(userDetails, category, startDate, endDate, sortBy, order);
        String message = likedPosts.isEmpty() ? "추천한 게시물이 없습니다." : "추천한 게시물을 성공적으로 조회했습니다.";
        return ResponseEntity.ok(new BoardResponseDto<>(message, likedPosts));
    }

    @PostMapping
    public ResponseEntity<?> createPost(@AuthenticationPrincipal CustomUserDetails userDetails, @ModelAttribute BoardCreateDto createDto){
        try {
            log.info("게시물 등록 API 실행");
            log.info("createDto" ,createDto);
            boardService.createBoard(createDto, userDetails);
            return ResponseEntity.status(HttpStatus.CREATED).body("게시물이 성공적으로 등록되었습니다.");
        } catch (IOException e) {
            log.error("게시물 등록중 오류발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("게시물 등록 중 오류가 발생했습니다.");
        }
    }

    @PutMapping("/{postId}")
    public ResponseEntity<?> updatePost(@AuthenticationPrincipal CustomUserDetails userDetails , @ModelAttribute BoardCreateDto updateDto , @PathVariable Integer postId){
        try{
            log.info("게시물 수정 API 실행");
            boardService.updateBoard(updateDto , userDetails , postId);
            return ResponseEntity.status(HttpStatus.OK).body("게시물이 성공적으로 수정되었습니다.");
        }catch (IOException e){
            log.error("게시물 수정중 오류발생",e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("게시물 수정 중 오류가 발생했습니다.");
        }
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<?> deletePost(@PathVariable Integer postId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            log.info("게시물 삭제 API 실행");
            boardService.deleteBoard(postId, userDetails);
            return ResponseEntity.status(HttpStatus.OK).body("게시물이 성공적으로 삭제되었습니다.");
        } catch (IllegalArgumentException e) {
            log.error("게시물 삭제 중 권한 오류", e);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @GetMapping("/participated")
    public ResponseEntity<BoardResponseDto<BoardAllDto>> getParticipatedTripsByUser(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) BoardEntity.Category category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date endDate,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String order) {
        log.info("내가 참여한 여행 게시물 조회 API 실행");
        BoardResponseDto<BoardAllDto> participatedTrips = boardService.getParticipatedTripsByUser(userDetails, category, startDate, endDate, sortBy, order);
        return ResponseEntity.ok(participatedTrips);
    }

    @GetMapping("/top4-categories")
    public ResponseEntity<BoardMainDto> getTop4BoardsByCategories() {
        log.info("메인 페이지 게시물 조회 API 실행");
        BoardMainDto response = boardService.getTop4BoardsByCategories();
        return ResponseEntity.ok(response);
    }
}