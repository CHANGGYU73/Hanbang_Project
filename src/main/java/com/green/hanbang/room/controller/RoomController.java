package com.green.hanbang.room.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.green.hanbang.member.vo.MemberVO;
import com.green.hanbang.room.service.RoomService;
import com.green.hanbang.room.vo.*;
import com.green.hanbang.util.RoomUtil;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Controller
@RequestMapping("/room")
@RequiredArgsConstructor
public class RoomController {
    private final RoomService roomService;

    @GetMapping("/reg")
    public String regRoom(Model model, HttpSession session){
        //옵션 셀렉트
        List<OptionsVO> optionsList = roomService.selectOptions();
        //매물유형 셀렉트
        List<PropertyTypeVO> propertyList = roomService.selectProperty();
        //전월세 셀렉트
        List<TradeTypeVO> tradeTypeList = roomService.selectTradeType();

        MemberVO loginInfo  = (MemberVO) session.getAttribute("loginInfo");
        model.addAttribute("optionsList", optionsList);
        model.addAttribute("propertyList", propertyList);
        model.addAttribute("tradeTypeList", tradeTypeList);

        return "room/reg_room";
    }
    @PostMapping("/insertRoom")
    public String insertRoom(RoomVO roomVO, MultipartFile mainImg, MultipartFile[] subImg, RoomAddrVO roomAddrVO){
        //상품이미지등록
        //RoomCode를 조회
        String roomCode = roomService.selectNextRoomCode();

        //이미지정보 하나가 들어갈 수 있는 통
        //첨부파일 단일
        RoomIMGVO roomIMGVO = RoomUtil.uploadFile(mainImg);

        //첨부파일 다중
        List<RoomIMGVO> imgList =RoomUtil.multiFileUpload(subImg);
        imgList.add(roomIMGVO);

        //RoomIMGVO에 RoomCode 저장
        for (RoomIMGVO roomIMGVO1 : imgList){
            roomIMGVO1.setRoomCode(roomCode);
        }

        roomAddrVO.setRoomCode(roomCode);

        roomVO.setRoomAddrVO(roomAddrVO);
        roomVO.setImgList(imgList);
        roomService.insertRoom(roomVO);
        return "redirect:/room/roomMain";
    }
    @GetMapping("/roomMain")
    public String roomMain(Model model, RoomSearchVO roomSearchVO){

        model.addAttribute("tradeTypeList", roomService.selectTradeType());
        model.addAttribute("roomList", roomService.selectRoom(roomSearchVO));
        model.addAttribute("propertyTypeList", roomService.selectProperty());
        model.addAttribute("Options", roomService.selectOptions());
        List<RoomVO> room= roomService.selectRoom(roomSearchVO);
        System.out.println(room);
        return "room/room_main";
    }

    @ResponseBody
    @PostMapping("/setMap")
    public List<RoomAddrVO> setMap() {
        // 비동기 통신으로 위도경도 셀렉트
        List<RoomAddrVO> roomAddrs = roomService.selectRoomAddr();
        return roomAddrs;
    }
    @ResponseBody
    @PostMapping("/roomSearch")
    public List<RoomVO> roomSearch(@RequestBody Map<String, Object> searchData){
        System.out.println(searchData);

        ObjectMapper mapper = new ObjectMapper();
        RoomSearchVO roomSearchVO= mapper.convertValue(searchData, RoomSearchVO.class);
        System.out.println(roomSearchVO);
        List<RoomVO> roomList = roomService.selectRoom(roomSearchVO);
    return roomList;

    }

////////////////////////////////

    @GetMapping("/roomDetailInfo")
    public String roomDetailInfo(String roomCode, Model model){
        //방 모든 정보
        RoomVO room = roomService.selectRoomInfo(roomCode);
        System.out.println(room);
        model.addAttribute("roomDetail",room);

        //선택한 옵션
        String options = room.getDetailOptions();
        List<String> optionList = Arrays.asList(options.split(","));
        model.addAttribute("optionList",optionList);

        //모든 옵션
        List<OptionsVO> os = roomService.selectOptions();
        model.addAttribute("allOptionList",roomService.selectOptions());

        //매물번호 (RoomCode 마지막 숫자 4자)
        String number = room.getRoomCode().substring(room.getRoomCode().length()-4);
        model.addAttribute("number", number);

        //방 등록한 사람 login_type 조회
        String loginType = roomService.selectLoginType(room.getUserNo());

        //등록한 사람 정보 조회
        if(Objects.equals(loginType, "USER")){
            model.addAttribute("personInfo",roomService.selectRegUser(room.getUserNo()));
        } else if(Objects.equals(loginType, "REALTOR")){
            model.addAttribute("personInfo",roomService.selectRegRealtor(room.getUserNo()));
        }

        //허위 매물 신고 사유
        model.addAttribute("reasonList",roomService.selectReasonList());

        //매물 문의 제목 조회
        model.addAttribute("inquiryTitleList",roomService.selectInquiryTitle());

        return "room/room_detail";
    }

    //옵션 값 있을 시 '있음' 표시
    @ResponseBody
    @PostMapping("/roomDetailFetch")
    public List<String> roomDetailFetch(@RequestBody Map<String, String> data){
        String options = roomService.selectRoomInfo(data.get("roomCode")).getDetailOptions();
        List<String> optionList = Arrays.asList(options.split(","));
        return optionList;
    }

    //본인인증
    @ResponseBody
    @PostMapping("/elDAS")
    public String elDAS(@RequestBody MemberVO memberVO){
        return roomService.selectElDAS(memberVO);
    }

    //허위매물신고
    @PostMapping("/falseOfferings")
    public String insertFalseOfferings(FalseOfferingsVO falseOfferingsVO){
        roomService.insertFalseOfferings(falseOfferingsVO);
        return "redirect:/room/roomDetailInfo?roomCode=" + falseOfferingsVO.getRoomCode();
    }

    //매물문의
    @ResponseBody
    @PostMapping("/insertInquiry")
    public boolean insertInquiry(@RequestBody InquiryVO inquiryVO, HttpSession session){
        MemberVO loginInfo = (MemberVO) session.getAttribute("loginInfo");
        inquiryVO.setFromUserNo(loginInfo.getUserNo());

        return roomService.insertInquiry(inquiryVO);
    }

}
