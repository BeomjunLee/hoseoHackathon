package com.hoseo.hackathon.storeticketingservice.repository;

import com.hoseo.hackathon.storeticketingservice.domain.Store;
import com.hoseo.hackathon.storeticketingservice.domain.status.ErrorStatus;
import com.hoseo.hackathon.storeticketingservice.domain.status.StoreStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StoreRepository extends JpaRepository<Store, Long> {

    //매장 이름으로 Store찾기
    Optional<Store> findByName(String name);

    //member_id로 select
    Optional<Store> findByMember_Id(Long member_id);

    //관리자의 매장 찾기
    @Query("select s from Store s join s.member m where m.username = :username")
    Optional<Store> findStoreJoinMemberByUsername(@Param("username") String username);

    //승인된 매장 목록 보기
    List<Store> findAllByStoreStatus(StoreStatus storeStatus);

    //승인된 매장 목록 보기
    Page<Store> findAllByStoreStatus(StoreStatus storeStatus, Pageable pageable);

    //승인된 매장 목록 이름으로 검색
    Page<Store> findAllByStoreStatusAndNameContaining(StoreStatus storeStatus, String q,  Pageable pageable);

    //에러 일어난 매장 목록 보기
    Page<Store> findAllByErrorStatus(ErrorStatus status, Pageable pageable);
    
    //에러 일어난 매장 목록 손님 많은 순으로 보기
    Page<Store> findAllByErrorStatusOrderByTotalWaitingCountDesc(ErrorStatus status, Pageable pageable);

    //등록된 매장 수
    int countByStoreStatus(StoreStatus storeStatus);

    //시스템 에러 수
    int countByErrorStatus(ErrorStatus errorStatus);

    //매장명 중복 방지
    int countByName(String name);

}
