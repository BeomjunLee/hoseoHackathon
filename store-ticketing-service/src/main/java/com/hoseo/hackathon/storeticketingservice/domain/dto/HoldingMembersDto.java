package com.hoseo.hackathon.storeticketingservice.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class HoldingMembersDto {
    private String name;
    private String phoneNum;
    @JsonIgnore
    private Long ticket_id;
    @JsonIgnore
    private Long store_id;
}
