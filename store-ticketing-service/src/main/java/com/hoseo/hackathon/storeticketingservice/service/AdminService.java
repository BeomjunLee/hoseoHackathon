package com.hoseo.hackathon.storeticketingservice.service;

import com.hoseo.hackathon.storeticketingservice.domain.Member;
import com.hoseo.hackathon.storeticketingservice.domain.Store;
import com.hoseo.hackathon.storeticketingservice.domain.Ticket;
import com.hoseo.hackathon.storeticketingservice.domain.condition.MemberSearchCondition;
import com.hoseo.hackathon.storeticketingservice.domain.dto.*;
import com.hoseo.hackathon.storeticketingservice.domain.form.AdminUpdateMemberForm;
import com.hoseo.hackathon.storeticketingservice.domain.form.AdminUpdateStoreAdminForm;
import com.hoseo.hackathon.storeticketingservice.domain.form.StoreNoticeForm;
import com.hoseo.hackathon.storeticketingservice.domain.form.UpdateStoreAdminForm;
import com.hoseo.hackathon.storeticketingservice.domain.status.*;
import com.hoseo.hackathon.storeticketingservice.exception.*;
import com.hoseo.hackathon.storeticketingservice.repository.MemberRepository;
import com.hoseo.hackathon.storeticketingservice.repository.StoreRepository;
import com.hoseo.hackathon.storeticketingservice.repository.TicketRepository;
import com.hoseo.hackathon.storeticketingservice.repository.custom.MemberRepositoryCustom;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {
    private final MemberRepository memberRepository;
    private final TicketRepository ticketRepository;
    private final StoreRepository storeRepository;
    private final PasswordEncoder passwordEncoder;
    private final MemberRepositoryCustom memberRepositoryCustom;

//===============================================가게 관리=============================================

//    /**
//     * 상태별 매장 목록 보기
//     */
//    public Page<StoreListDto> findStores(StoreStatus storeStatus, Pageable pageable) {
//        Page<Store> stores = storeRepository.findAllByStoreStatus(storeStatus, pageable);
//        return stores.map(store -> StoreListDto.builder()
//                .store_id(store.getId())
//                .member_id(store.getMember().getId())
//                .name(store.getName())
//                .phoneNum(store.getPhoneNum())
//                .address(store.getAddress())
//                .createdDate(store.getCreatedDate())
//                .companyNumber(store.getCompanyNumber())
//                .build());
//    }

    /**
     * 매장 목록 보기
     */
    public Page<StoreListDto> findStoresByName(StoreStatus storeStatus, String q, Pageable pageable) {
        Page<Store> stores = storeRepository.findAllByStoreStatusAndNameContaining(storeStatus, q, pageable);
        return stores.map(store -> StoreListDto.builder()
                .store_id(store.getId())
                .member_id(store.getMember().getId())
                .name(store.getName())
                .phoneNum(store.getPhoneNum())
                .address(store.getAddress())
                .createdDate(store.getCreatedDate())
                .companyNumber(store.getCompanyNumber())
                .build());
    }
        
    /**
     * 등록된 매장 수 보기
     */
    public int totalEnrollStoreCount() {
        return storeRepository.countByStoreStatus(StoreStatus.VALID);
    }


    /**
     * 매장 관리자 정보 보기
     */
    public Member findStoreAdmin(Long member_id) {
        return memberRepository.findById(member_id).orElseThrow(() -> new NoSuchElementException("매장 관리자를 찾을수 없습니다"));
    }

    /**
     * 매장 수정
     */


                   //================가게 번호표 관리 기능================
    /**
     *  번호표 발급 활성화
     */
    @Transactional
    public void openTicket(Long store_id) {
        Store store = storeRepository.findById(store_id)
                .orElseThrow(() -> new NotFoundStoreException("등록된 매장을 찾을수 없습니다"));
        if (store.getStoreTicketStatus().equals(StoreTicketStatus.OPEN)) {
            throw new IsAlreadyCompleteException("이미 활성화 되어있습니다");
        }
        store.changeStoreTicketStatus(StoreTicketStatus.OPEN);
    }

    /**
     *  번호표 발급 비활성화
     */
    @Transactional
    public void closeTicket(Long store_id) {
        Store store = storeRepository.findById(store_id)
                .orElseThrow(() -> new NotFoundStoreException("등록된 매장을 찾을수 없습니다"));
        if (store.getStoreTicketStatus().equals(StoreTicketStatus.CLOSE)) {
            throw new IsAlreadyCompleteException("이미 비활성화 되어있습니다");
        }
        store.changeStoreTicketStatus(StoreTicketStatus.CLOSE);
    }

    /**
     * [관리자]번호표 취소
     */
    @Transactional
    public Ticket cancelTicketByAdmin(Long store_id, Long ticket_id) {

        Store store = storeRepository.findById(store_id)
                .orElseThrow(() -> new NotFoundStoreException("등록된 매장을 찾을수 없습니다"));

        Ticket ticket = ticketRepository.findById(ticket_id).orElseThrow(() -> new NotFoundTicketException("번호표를 찾을수 없습니다"));

        if (ticket.getStatus().equals(TicketStatus.CANCEL)) {
            throw new IsAlreadyCompleteException("이미 취소처리 되었습니다");
        }

        int totalWaitingCount = ticketRepository.countByStore_IdAndStatus(store.getId(), TicketStatus.VALID);

        ticketRepository.updateTicketsMinus1(TicketStatus.VALID, ticket.getWaitingNum(), store.getAvgWaitingTimeByOne(), store.getId()); //취소한 번호표보다 뒤에있는 사람들에게 - 1
        ticket.changeStatusTicket(TicketStatus.CANCEL); //번호표 상태 변경
        store.changeStoreByCancelOrNext(totalWaitingCount); //Store 갱신

        return ticket;
    }

    /**
     * [회원]번호표 취소
     */
    @Transactional
    public Ticket cancelTicket(Long ticket_id) {

        Ticket ticket = ticketRepository.findById(ticket_id).orElseThrow(() -> new NotFoundTicketException("번호표를 찾을수 없습니다"));
        if (ticket.getStatus().equals(TicketStatus.CANCEL)) {
            throw new IsAlreadyCompleteException("이미 취소처리 되었습니다");
        }

        Store store = storeRepository.findById(ticket.getStore().getId()).orElseThrow(() -> new NotFoundStoreException("해당되는 매장을 찾을수 없습니다"));

        int totalWaitingCount = ticketRepository.countByStore_IdAndStatus(store.getId(), TicketStatus.VALID);

        ticketRepository.updateTicketsMinus1(TicketStatus.VALID, ticket.getWaitingNum(), store.getAvgWaitingTimeByOne(), store.getId()); //취소한 번호표보다 뒤에있는 사람들에게 - 1
        ticket.changeStatusTicket(TicketStatus.CANCEL); //번호표 상태 변경
        store.changeStoreByCancelOrNext(totalWaitingCount); //Store 갱신

        return ticket;
    }

    /**
     *  번호표 넘기기(체크)
     */
    @Transactional
    public Ticket checkTicket(Long store_id, Long ticket_id) {
        //번호표 찾기
        Ticket ticket = ticketRepository.findById(ticket_id).orElseThrow(() -> new NotFoundTicketException("체크할 번호표를 찾을수 없습니다"));
        if (ticket.getStatus().equals(TicketStatus.INVALID)) {
            throw new IsAlreadyCompleteException("이미 체크처리 되었습니다");
        }

        //관리자의 가게 찾기
        Store store = storeRepository.findById(store_id).orElseThrow(() -> new NotFoundStoreException("등록된 매장을 찾을수 없습니다"));

        int totalWaitingCount = ticketRepository.countByStore_IdAndStatus(store.getId(), TicketStatus.VALID);

        ticketRepository.updateTicketsMinus1(TicketStatus.VALID, ticket.getWaitingNum(), store.getAvgWaitingTimeByOne(), store.getId()); //보류한 번호표보다 뒤에있는 사람들에게 - 1
        ticket.changeStatusTicket(TicketStatus.INVALID); //번호표 상태 변경
        store.changeStoreByCancelOrNext(totalWaitingCount); //Store 갱신

        return ticket;
    }


    /**
     * 번호표 보류하기
     */
    @Transactional
    public Ticket holdTicket(Long store_id, Long ticket_id) {
        //번호표 찾기
        Ticket ticket = ticketRepository.findById(ticket_id)
                .orElseThrow(() -> new NotFoundTicketException("보류할 번호표를 찾을수 없습니다"));
        if (ticket.getStatus().equals(TicketStatus.HOLD)) {
            throw new IsAlreadyCompleteException("이미 보류처리 되었습니다");
        }
        //관리자의 매장 찾기
        Store store = storeRepository.findById(store_id).orElseThrow(() -> new NotFoundStoreException("등록된 매장을 찾을수 없습니다"));

        int totalWaitingCount = ticketRepository.countByStore_IdAndStatus(store.getId(), TicketStatus.VALID);

        ticketRepository.updateTicketsMinus1(TicketStatus.VALID, ticket.getWaitingNum(), store.getAvgWaitingTimeByOne(), store.getId()); //보류한 번호표보다 뒤에있는 사람들에게 - 1
        ticket.changeStatusTicket(TicketStatus.HOLD); //번호표 상태 변경
        store.changeStoreByCancelOrNext(totalWaitingCount); //Store 갱신

        return ticket;
    }

    /**
     * 보류한 번호표 체크
     */
    @Transactional
    public Ticket holdCheckTicket(Long store_id, Long ticket_id) {
        Ticket ticket = ticketRepository.findByIdAndStore_Id(ticket_id, store_id).orElseThrow(() -> new NotFoundTicketException("체크할 번호표를 찾을수 없습니다"));
        if (ticket.getStatus().equals(TicketStatus.INVALID)) {
            throw new IsAlreadyCompleteException("이미 체크처리 되었습니다");
        }

        ticket.changeStatusTicket(TicketStatus.INVALID);//번호표 상태 변경
        return ticket;
    }

    /**
     *  보류한 번호표 취소
     */
    @Transactional
    public Ticket holdCancelTicket(Long store_id, Long ticket_id) {

        Ticket ticket = ticketRepository.findByIdAndStore_Id(ticket_id, store_id).orElseThrow(() -> new NotFoundTicketException("취소할 번호표를 찾을수 없습니다"));
        if (ticket.getStatus().equals(TicketStatus.CANCEL)) {
            throw new IsAlreadyCompleteException("이미 취소처리 되었습니다");
        }

        ticket.changeStatusTicket(TicketStatus.CANCEL);//번호표 상태 변경
        return ticket;
    }

    /**
     * 보류한 회원들 + 번호표 보기
     */
    public Page<HoldingMembersDto> findHoldMembers(Long store_id, Pageable pageable) {
        //관리자의 매장 찾기
        Store store = storeRepository.findById(store_id).orElseThrow(() -> new NotFoundStoreException("등록된 매장을 찾을수 없습니다"));

        if (store.getStoreStatus().equals(StoreStatus.INVALID)) {   //매장 승인여부 체크
            throw new NotAuthorizedStoreException("승인되지 않은 매장입니다");
        }

        Page<Ticket> tickets = ticketRepository.findAllByStore_IdAndStatus(store.getId(), TicketStatus.HOLD, pageable);
        return tickets.map(ticket -> HoldingMembersDto.builder()
                .name(ticket.getMember().getName())
                .phoneNum(ticket.getMember().getPhoneNum())
                .ticket_id(ticket.getId())
                .store_id(store.getId())
                .build());
    }

    /**
     * 대기중인 회원들 + 번호표  보기
     */
    public Page<WaitingMembersDto> findWaitingMembers(Long store_id, Pageable pageable) {
        //관리자의 매장 찾기
        Store store = storeRepository.findById(store_id).orElseThrow(() -> new NotFoundStoreException("등록된 매장을 찾을수 없습니다"));

        Page<Ticket> tickets = ticketRepository.findAllByStore_IdAndStatus(store.getId(), TicketStatus.VALID, pageable);
        return tickets.map(ticket -> WaitingMembersDto.builder()
                .waitingNum(ticket.getWaitingNum())
                .name(ticket.getMember().getName())
                .phoneNum(ticket.getMember().getPhoneNum())
                .ticket_id(ticket.getId())
                .store_id(store.getId())
                .build());
    }

    /**
     * 매장 공지사항 수정
     */
    @Transactional
    public void updateStoreNotice(Long store_id, String notice) {
        Store store = storeRepository.findById(store_id).orElseThrow(() -> new NotFoundStoreException("등록된 매장을 찾을수 없습니다"));
        store.changeNotice(notice);
    }

    /**
     * 매장 한사람당 평균 대기시간 수정
     */
    @Transactional
    public void updateAvgTime(Long store_id, int avgTime) {
        Store store = storeRepository.findById(store_id).orElseThrow(() -> new NotFoundStoreException("등록된 매장을 찾을수 없습니다"));
        store.changeAvgWaitingTimeByOne(avgTime);
    }

    /**
     * 매장, 관리자 수정
     */
    @Transactional
    public void updateStoreAdmin(Long store_id, Long member_id, AdminUpdateStoreAdminForm form) {
        Member member = memberRepository.findById(member_id).orElseThrow(() -> new UsernameNotFoundException("해당되는 회원을 찾을수 없습니다"));
        Store store = storeRepository.findById(store_id).orElseThrow(() -> new NotFoundStoreException("등록된 매장을 찾을수 없습니다"));

        if(!member.getUsername().equalsIgnoreCase(form.getMember_username())){//수정할 아이디가 중복이 아닐경우
            validateDuplicateMember(form.getMember_username());
        }
        //포인트는 서비스 미적용
        member.changeMemberByAdmin(form.getMember_username(), form.getMember_name(), form.getMember_phoneNum(), form.getMember_email(), member.getPoint());

        if (!store.getName().equalsIgnoreCase(form.getStore_name())) {//수정할 매장명이 중복이 아닐경우
            validateDuplicateStore(form.getStore_name());
        }
        store.changeStoreByAdmin(form.getStore_name(), form.getStore_phoneNum(), form.getStore_address(), form.getStore_companyNumber());

    }

    /**
     * 매장 가게명 검증
     */
    public void validateDuplicateStore(String name) {
        int findStores = storeRepository.countByName(name);
        if (findStores > 0) {
            throw new DuplicateStoreNameException("매장명이 중복되었습니다");
        }
        //두 유저가 동시에 가입할 경우를 대비해서 DB 에도 유니크 제약조건을 걸어줘야함
    }

    /**
     * 매장 정보 보기(승인되지않은가게도)
     */
    public Store findStore(Long store_id) {
        return storeRepository.findById(store_id).orElseThrow(() -> new NotFoundStoreException("등록된 매장을 찾을수 없습니다"));
    }

//    /**
//     * 매장 정보 보기(승인된 가게만)
//     */
//    public Store findValidStore(Long store_id) {
//        Store store = storeRepository.findById(store_id).orElseThrow(() -> new NotFoundStoreException("등록된 매장을 찾을수 없습니다"));
//        if(store.getStoreStatus().equals(StoreStatus.INVALID) || store.getStoreStatus().equals(StoreStatus.DELETE)){//승인되지 않은 매장 체크
//            throw new NotAuthorizedStoreException("승인 되지 않은 매장입니다");
//        }
//        return store;
//    }

//==========================================================회원 관리===============================================================

    /**
     * 회원 리스트 보기
     */
    public Page<MemberListDto> findMembers(MemberSearchCondition condition, Pageable pageable) {
        return memberRepositoryCustom.search(condition, pageable);
    }

    /**
     * 전체 회원 수
     */
    public int totalMemberCount() {
        return memberRepository.countByStatus(true);
    }

    /**
     * 현재 서비스 이용자 수(번호표 뽑은)
     */
    public int currentUsingServiceCount() {
        return ticketRepository.countByStatus(TicketStatus.VALID);
    }

    /**
     * 회원 정보 보기
     */
    public Member findMember(Long member_id) {
        return memberRepository.findById(member_id).orElseThrow(() -> new UsernameNotFoundException("해당되는 회원을 찾을수 없습니다"));
    }

    /**
     * 회원 수정
     */
    @Transactional
    public void updateMember(Long member_id, AdminUpdateMemberForm form) {
        Member member = memberRepository.findById(member_id).orElseThrow(() -> new UsernameNotFoundException("해당되는 회원을 찾을수 없습니다"));

        if(!member.getUsername().equalsIgnoreCase(form.getUsername())){//수정할 아이디가 중복이 아닐경우
            validateDuplicateMember(form.getUsername());
        }
        member.changeMemberByAdmin(form.getUsername(), form.getName(), form.getPhoneNum(), form.getEmail(), form.getPoint());
    }

    /**
     * 비밀번호 수정
     */
    @Transactional
    public void changePassword(Long member_id, String currentPassword, String newPassword) {

        Member member = memberRepository.findById(member_id).orElseThrow(() -> new UsernameNotFoundException("해당되는 회원을 찾을수 없습니다"));
        if (!passwordEncoder.matches(currentPassword, member.getPassword())) {
            throw new BadCredentialsException("비밀번호가 일치 하지않습니다");
        }
        member.encodingPassword(passwordEncoder.encode(newPassword));
    }

    /**
     * 중복 회원 검증
     */
    public void validateDuplicateMember(String username) {
        int findMembers = memberRepository.countByUsername(username);
        if (findMembers > 0) {
            throw new DuplicateUsernameException("아이디가 중복되었습니다");
        }
        //두 유저가 동시에 가입할 경우를 대비해서 DB 에도 유니크 제약조건을 걸어줘야함
    }


    /**
     * 회원 탈퇴(username 은 null, 회원 상태는 DELETE)
     * (가게 관리자인경우 가게 이름은 null, 가게 상태도 DELETE)
     */
    @Transactional
    public void deleteMember(Long member_id) {
        Member member = memberRepository.findById(member_id).orElseThrow(() -> new UsernameNotFoundException("해당되는 회원을 찾을수 없습니다"));
        if (member.getRole().equals(Role.USER)) {
            member.changeRole(Role.DELETE);
        } else if (member.getRole().equals(Role.STORE_ADMIN)) {
            Store store = storeRepository.findByMember_Id(member_id).orElseThrow(() -> new NotFoundStoreException("등록된 가게를 찾을수 없습니다"));
            member.changeRole(Role.DELETE);
            store.changeStoreStatus(StoreStatus.DELETE);
        }
    }

    /**
     * 탈퇴후 7일지난 회원 영구삭제
     */
    @Transactional
    public void deleteMemberWeekPast() {
        List<Member> memberList = memberRepository.findAllByRole(Role.DELETE);
        if(memberList == null) throw new NoSuchElementException("삭제할 회원이 없습니다");
        memberList.stream().forEach(member ->{
            Period period = Period.between(member.getDeletedDate().toLocalDate(), LocalDateTime.now().toLocalDate());
                if (period.getDays() >= 7) {
                    storeRepository.delete(storeRepository.findByMember_Id(member.getId()).get());
                    memberRepository.delete(member);
                }
        });
    }



//==================================================헤더=========================================================
    /**
     * 가입 승인할 매장 수
     */
    public int totalInvalidStoreCount() {
        return storeRepository.countByStoreStatus(StoreStatus.INVALID);
    }

    /**
     * 시스템 장애 건 수
     */
    public int totalErrorStoreCount() {
        return storeRepository.countByErrorStatus(ErrorStatus.ERROR);
    }

//============================================가입 승인====================================================

    /**
     * 매장 관리자 가입 승인 (시간넣어야됨)
     */
    @Transactional
    public void permitStoreAdmin(Long member_id, Long store_id) {
        Member member = memberRepository.findById(member_id).orElseThrow(() -> new UsernameNotFoundException("해당되는 유저를 찾을수 없습니다"));
        Store store = storeRepository.findById(store_id).orElseThrow(() -> new NotFoundStoreException("등록된 매장을 찾을수 없습니다"));
        member.changeEnabled(true);
        store.changeStoreStatus(StoreStatus.VALID);
        store.changeCreatedDate(LocalDateTime.now());
    }

    /**
     * 매장 관리자 가입 승인 취소
     */
    @Transactional
    public void rejectStoreAdmin(Long member_id, Long store_id) {
        Member member = memberRepository.findById(member_id).orElseThrow(() -> new UsernameNotFoundException("해당되는 유저를 찾을수 없습니다"));
        Store store = storeRepository.findById(store_id).orElseThrow(() -> new NotFoundStoreException("등록된 매장을 찾을수 없습니다"));
        member.changeEnabled(false);
        store.changeStoreStatus(StoreStatus.INVALID);
    }

    /**
     * 가입 대기 이름 검색
     */

    /**
     * 가입 대기 주소 검색
     */

//==============================================시스템 장애================================
    /**
     * 시스템 장애 리스트 보기
     */
    public Page<StoreErrorListDto> findErrorStores(Pageable pageable) {
        Page<Store> stores = storeRepository.findAllByErrorStatus(ErrorStatus.ERROR, pageable);
        return stores.map(store -> StoreErrorListDto.builder()
                .store_id(store.getId())
                .member_id(store.getMember().getId())
                .name(store.getName())
                .phoneNum(store.getPhoneNum())
                .address(store.getAddress())
                .totalWaitingCount(store.getTotalWaitingCount())
                .build());
    }

    /**
     * 시스템 장애 손님수많은 순서대로 보기
     */
    public Page<StoreErrorListDto> findErrorStoresByTotalWaitingCount(Pageable pageable) {
        Page<Store> stores = storeRepository.findAllByErrorStatusOrderByTotalWaitingCountDesc(ErrorStatus.ERROR, pageable);
        return stores.map(store -> StoreErrorListDto.builder()
                .store_id(store.getId())
                .member_id(store.getMember().getId())
                .name(store.getName())
                .phoneNum(store.getPhoneNum())
                .address(store.getAddress())
                .totalWaitingCount(store.getTotalWaitingCount())
                .build());
    }

    /**
     * 시스템 장애 신청
     */
    @Transactional
    public void sendErrorSystem(Long store_id) {
        Store store = storeRepository.findById(store_id).orElseThrow(() -> new NotFoundStoreException("관리자의 이름으로 등록된 매장을 찾을수 없습니다"));
        store.changeErrorStatus(ErrorStatus.ERROR);
    }

    /**
     * 시스템 에러 해결 완료
     */
    @Transactional
    public void completeErrorSystem(Long store_id) {
        Store store = storeRepository.findById(store_id).orElseThrow(() -> new NotFoundStoreException("관리자의 이름으로 등록된 매장을 찾을수 없습니다"));
        store.changeErrorStatus(ErrorStatus.GOOD);
    }

}
