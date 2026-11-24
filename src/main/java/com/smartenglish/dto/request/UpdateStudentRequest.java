package com.smartenglish.dto.request;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStudentRequest {

    private String fullName;

    private String avatarUrl;

    private String currentLevel;

    private Boolean isPlacementCompleted;

    private Boolean isActive;

    private String password; // Nếu muốn đổi mật khẩu cho học sinh
}

