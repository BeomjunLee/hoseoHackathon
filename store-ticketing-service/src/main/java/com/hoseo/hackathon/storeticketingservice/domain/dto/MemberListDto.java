package com.hoseo.hackathon.storeticketingservice.domain.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
public class MemberListDto {
    private Long ticket_id;
    private Long member_id;

    private String username;                   //아이디
    private String name;                        //이름
    private String phoneNum;                    //전화번호
    private String email;                           //이메일
    private LocalDateTime createdDate;             //가입일자

    @Builder
    @QueryProjection
    public MemberListDto(Long ticket_id, Long member_id, String username, String name, String phoneNum, String email, LocalDateTime createdDate) {
        this.ticket_id = ticket_id;
        this.member_id = member_id;
        this.username = username;
        this.name = name;
        this.phoneNum = phoneNum;
        this.email = email;
        this.createdDate = createdDate;
    }
}
