package com.jpa.querydsl.controller;

import com.jpa.querydsl.dto.MemberSearchCondition;
import com.jpa.querydsl.dto.MemberTeamDto;
import com.jpa.querydsl.respository.MemberJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

/**
 * 조회 컨트롤러
 */
@RestController
@RequiredArgsConstructor
public class MemberController {

    private final MemberJpaRepository memberJpaRepository;

    @GetMapping("/v1/members")
    public List<MemberTeamDto> searchMemberV1(MemberSearchCondition condition)
    {
        return memberJpaRepository.search(condition);
    }
}