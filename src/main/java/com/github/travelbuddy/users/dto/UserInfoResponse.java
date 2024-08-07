package com.github.travelbuddy.users.dto;

import com.github.travelbuddy.users.enums.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class UserInfoResponse {
    private String email;
    private String name;
    private String residentNum;
    private Gender gender;
    private String profilePictureUrl;
}
