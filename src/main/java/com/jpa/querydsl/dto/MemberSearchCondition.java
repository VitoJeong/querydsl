package com.jpa.querydsl.dto;

import lombok.Data;

@Data
public class MemberSearchCondition {

    // 회원명, 팀명, 나이(goe,loe
    private String username;
    private String teamName;
    private Integer ageGoe;
    private Integer ageLoe;
}
