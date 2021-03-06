package com.hoseo.hackathon.storeticketingservice.domain;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hoseo.hackathon.storeticketingservice.domain.status.ErrorStatus;
import com.hoseo.hackathon.storeticketingservice.domain.status.StoreStatus;
import com.hoseo.hackathon.storeticketingservice.domain.status.StoreTicketStatus;
import lombok.*;
import javax.persistence.*;
import java.time.LocalDateTime;


@Entity
@Getter
@AllArgsConstructor
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Store {
    @Id @GeneratedValue
    @Column(name = "store_id")
    private Long id;
    @Column(unique = true)
    private String name;                //이름 ->중복되면 안됨
    private String phoneNum;            //전화번호
    private String address;             //주소
    private String latitude;            //위도
    private String longitude;           //경도
    private int totalWaitingCount;       //전체 대기인원
    private int avgWaitingTimeByOne;    //한 사람당 평균 대기시간
    private int totalWaitingTime;       //전체 대기시간
    private String notice;              //공지사항
    private String companyNumber;       //사업자등록번호

    @Enumerated(EnumType.STRING)
    private StoreTicketStatus storeTicketStatus;   //매장 번호표 발급 활성화 (OPEN, CLOSE) 상태
    @Enumerated(EnumType.STRING)
    private StoreStatus storeStatus;    //매장 승인 여부 (VALID, INVALID)
    @Enumerated(EnumType.STRING)
    private ErrorStatus errorStatus;         //시스템 장애 여부 (ERROR, GOOD)

    private LocalDateTime createdDate;        //생성일

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    @JsonIgnore
    private Member member;              //member_id

    //==연관관계 메서드==
    public void setMember(Member member) {
        this.member = member;
    }

    //=========================비지니스로직=====================
    //매장 수정
    public void changeStore(String phoneNum, String address) {
        this.phoneNum = phoneNum;
        this.address = address;
    }

    //매장 수정(관리자용)
    public void changeStoreByAdmin(String name, String phoneNum, String address, String companyNumber) {
        this.name = name;
        this.phoneNum = phoneNum;
        this.address = address;
        this.companyNumber = companyNumber;
    }
    
    //번호표 뽑을때 Store 변경점
    public void changeStoreByTicketing(int totalWaitingCount) {
        this.totalWaitingCount = totalWaitingCount + 1;     //전체 대기인원수 설정
        this.totalWaitingTime = this.avgWaitingTimeByOne * (totalWaitingCount + 1); //전체 대기시간 설정
    }
    
    //번호표 취소, 넘기기 Store 변경점
    public void changeStoreByCancelOrNext(int totalWaitingCount) {
        this.totalWaitingCount = totalWaitingCount - 1;     //전체 대기인원수 설정
        this.totalWaitingTime = this.avgWaitingTimeByOne * (totalWaitingCount - 1); //전체 대기시간 설정
    }

    //번호표 발급 활성화 비활성화 변경
    public void changeStoreTicketStatus(StoreTicketStatus storeTicketStatus) {
        this.storeTicketStatus = storeTicketStatus;
    }

    //매장 승인 여부 변경
    public void changeStoreStatus(StoreStatus storeStatus) {
        this.storeStatus = storeStatus;
    }

    //매장 시스템 장애 여부 변경
    public void changeErrorStatus(ErrorStatus errorStatus) {
        this.errorStatus = errorStatus;
    }
    
    //승인 날짜 설정
    public void changeCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    //공지사항 변경
    public void changeNotice(String notice) {
        this.notice = notice;
    }

    //한사람당 대기시간 변경
    public void changeAvgWaitingTimeByOne(int avgWaitingTimeByOne) {
        this.avgWaitingTimeByOne = avgWaitingTimeByOne;
    }
}
