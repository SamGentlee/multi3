package three.team.project;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.log4j.Log4j;
import three.auction.model.AuctionEndVO;
import three.auction.model.AuctionSurveyVO;
import three.chat.model.ChatAlertVO;
import three.chat.model.ChatRoomVO;
import three.chat.model.ChatVO;
import three.chat.service.ChatService;
import three.donation.model.DonateVO;
import three.donation.model.DonationOrgVO;
import three.exchange.model.ExchangeVO;
import three.payment.model.PaymentVO;
import three.product.model.ProductVO;
import three.profile.service.ProfileService;
import three.report.model.ReportVO;
import three.security.SHA256.UserSHA256;
import three.user.model.UserVO;

@Controller
@Log4j
public class ProfileController {
	
	@Inject
	private ProfileService profileServiceImpl;
	
	@Inject
	private ChatService chatServiceImpl;
	
	@GetMapping("/users-profile")
	public String userProfile(Model m, HttpSession ses) { 
		UserVO user=(UserVO)ses.getAttribute("user");
		if (user == null) {
			return "redirect:/";
		}
		String userid=user.getUserId();
		
		int totalDonate =profileServiceImpl.myTotalDonation(userid);
		
		if(totalDonate != 0) {
			int mygoldBg=totalDonate/100000;
			int mysilverBg=totalDonate%100000/50000;
			int mybronzeBg=totalDonate%100000%50000/10000;
			
			UserVO vo=new UserVO();
			vo.setUserId(userid);
			vo.setUserGoldBadge(mygoldBg);
			vo.setUserSilverBadge(mysilverBg);
			vo.setUserBronzeBadge(mybronzeBg);
			
			profileServiceImpl.updateBadge(vo);
			
			log.info("금"+mygoldBg+"은"+mysilverBg+"동"+mybronzeBg);
		}
		
		List<AuctionEndVO> aucvo=profileServiceImpl.myAuction(userid);
		
		//평점 평균
		double avrScore=this.profileServiceImpl.getAverage(userid);
		m.addAttribute("average",avrScore);
		log.info(avrScore);
		
		UserVO vo=profileServiceImpl.findUserByUserId(userid);
		ses.setAttribute("user", vo);
		m.addAttribute("user",vo);
		m.addAttribute("myList",aucvo);
		
		return "users-profile";
	}
	
	//거래 완료
	@GetMapping("/tradeOK")
	public String tradeOK(@RequestParam int aucEndNum,HttpSession ses) {
		
		//경매 확정 정보 가져오기
		AuctionEndVO aucvo=profileServiceImpl.findAuctionEnd(aucEndNum);
		
		//물품 번호로 물품 정보 가져오기 
		int prodNum=aucvo.getProdNum_fk();
		ProductVO pvo=profileServiceImpl.findProductByNum(prodNum);
		
		int donatePer=pvo.getDonatePercent();//기부 비율		
		int totalPrice=aucvo.getEndPrice();	//최종 낙찰 가격
		int donateAmount=totalPrice*donatePer/100;//기부금	
		int restPoint=totalPrice-donateAmount;//기부금 제외한 나머지 
		
		//현재 기부 단체 정보 가져오기
		DonationOrgVO orgvo=profileServiceImpl.findOrgInfo();
		
		//판매자 아이디, 기부금액, 기부단체 ... 객체에 담기
		DonateVO dvo=new DonateVO();
		
		dvo.setDonOrgNum_fk(orgvo.getDonOrgNum());//기부단체 번호
		dvo.setDonOrgName(orgvo.getDonName());//기부단체 이름
		dvo.setUserId_fk(aucvo.getSellId());//판매자 아이디 
		dvo.setDonationType(1);//1: 경매를 통한 기부 , 2: 포인트를 통한 기부
		dvo.setDonAmount(donateAmount);//기부 금액
		
		//기부 DB에 저장
		int a=profileServiceImpl.addDonation(dvo);
		log.info("기부완료 : "+a);
		
		UserVO sellvo=new UserVO();
		sellvo.setUserId(aucvo.getSellId());
		sellvo.setUserPoint(restPoint);
		//나머지 금액 판매자 계정 포인트에 추가
		int r=profileServiceImpl.addPoint(sellvo);
		log.info("포인트 추가 : "+r);
		
		UserVO uvo=(UserVO)ses.getAttribute("user");
		String myId=uvo.getUserId();
		String buyId=aucvo.getBuyId();
		String sellId=aucvo.getSellId();
		if(myId.equals(buyId)) {
			
			//거래완료로 상태 변경
			int n=profileServiceImpl.aucEndupdateStatus(aucEndNum);
			
			//구매자 알림
			ChatRoomVO buyChatRoom=new ChatRoomVO(0,"관리자",buyId);
			int buyRoom=chatServiceImpl.createRoom(buyChatRoom);
			int buyRoomId=chatServiceImpl.findChatRoomIdById(buyChatRoom);
			ChatVO buyChat=new ChatVO(buyRoomId,"관리자",buyId," 물품 거래가 완료되었습니다",null,null);
			chatServiceImpl.insertMessage(buyChat);
			ChatAlertVO buyAlert=new ChatAlertVO(buyRoomId,buyId,1);
			chatServiceImpl.addNoReadCount(buyAlert);
			buyChat.setSendMsg("마이페이지에서 "+sellId+"님과의 거래에 대한 평가를 해주세요~");
			chatServiceImpl.insertMessage(buyChat);
			ChatAlertVO buyAlert2=new ChatAlertVO(buyRoomId,buyId,1);
			chatServiceImpl.addNoReadCount(buyAlert2);
			
			//판매자 알림
			ChatRoomVO sellChatRoom=new ChatRoomVO(0,"관리자",sellId);
			int sellRoom=chatServiceImpl.createRoom(sellChatRoom);
			int sellRoomId=chatServiceImpl.findChatRoomIdById(sellChatRoom);
			ChatVO sellChat=new ChatVO(sellRoomId,"관리자",sellId,"물품 거래가 완료되었습니다",null,null);
			chatServiceImpl.insertMessage(sellChat);
			ChatAlertVO sellAlert=new ChatAlertVO(sellRoomId,sellId,1);
			chatServiceImpl.addNoReadCount(sellAlert);
			sellChat.setSendMsg(donateAmount+"포인트가 기부되었고,");
			chatServiceImpl.insertMessage(sellChat);
			ChatAlertVO sellAlert2=new ChatAlertVO(sellRoomId,sellId,1);
			chatServiceImpl.addNoReadCount(sellAlert2);
			sellChat.setSendMsg(restPoint+"포인트가 들어왔습니다.");
			chatServiceImpl.insertMessage(sellChat);
			ChatAlertVO sellAlert3=new ChatAlertVO(sellRoomId,sellId,1);
			chatServiceImpl.addNoReadCount(sellAlert3);
			sellChat.setSendMsg("마이페이지에서 "+buyId+"님과의 거래에 대한 평가를 해주세요~");
			chatServiceImpl.insertMessage(sellChat);
			ChatAlertVO sellAlert4=new ChatAlertVO(sellRoomId,sellId,1);
			chatServiceImpl.addNoReadCount(sellAlert4);
			log.info("상태변경 :"+n);
		}
		
		
		return "redirect:/users-profile";
	}
	
	//결제요청 & 결제정보저장
	@PostMapping(value="/users-profile/payment")
	@ResponseBody
	public Map<String, Object> requestPayment (@RequestBody Map<String, Object> map, HttpSession ses,
			Model m) {
		String imp_uid=map.get("imp_uid").toString();
		String merchant_uid=map.get("merchant_uid").toString();
		String buyer_email=map.get("buyer_email").toString();
		String buyer_id=map.get("buyer_id").toString();
		int paid_amount=Integer.parseInt(map.get("paid_amount").toString());
		
		PaymentVO vo=new PaymentVO(merchant_uid,imp_uid,buyer_email,buyer_id,paid_amount,null);
		
		int addPayment=this.profileServiceImpl.insertPayment(vo);
		int plusPoint=this.profileServiceImpl.plusPoint(vo);
		
		UserVO user=this.profileServiceImpl.findUserByUserId(buyer_id);
		ses.setAttribute("user", user);
		m.addAttribute("user",user);
		m.addAttribute("payment",vo);
		
		return map;
		
	}
	//환전요청 & 환전정보 저장
	@PostMapping("/users-profile/exchange")
	@ResponseBody
	public Map<String, Object> exchange(@RequestBody Map<String, Object> map, HttpSession ses,
			Model m) {
		String bankName=map.get("bankName").toString();
		String bankAccountNum=map.get("bankAccountNum").toString();
		String userName=map.get("userName").toString();
		String userEmail=map.get("userEmail").toString();
		String userid=map.get("userId").toString();
		int exchangePoint=Integer.parseInt(map.get("exchangePoint").toString());
		
		if(bankName==null || bankAccountNum==null || userName==null || exchangePoint < 1000 ) {
			return null;
		}
		
		ExchangeVO vo=new ExchangeVO(0,bankName,bankAccountNum,userName,userEmail,userid,exchangePoint,null,0);
		int insertExchange=this.profileServiceImpl.addExchange(vo);
		int minusPoint=this.profileServiceImpl.minusPointByExchange(vo);
		
		UserVO user=this.profileServiceImpl.findUserByUserId(userid);
		ses.setAttribute("user", user);
		m.addAttribute("user",user);
		m.addAttribute("exchange",vo);
		return map;
	}
	//기부내역 저장
		@PostMapping("/users-profile/donate")
		@ResponseBody
		public Map<String, Object> donate(@RequestBody Map<String, Object> map, HttpSession ses,
				Model m) {
			String userId=map.get("userId").toString();
			int donAmount=Integer.parseInt(map.get("donAmount").toString());
			int donOrgNum=this.profileServiceImpl.getDonOrgNum();
			String donOrgName=this.profileServiceImpl.getDonOrgName(donOrgNum);
			log.info(userId);
			log.info(donAmount);
			log.info(donOrgNum);
			if(userId==null || donAmount <100 || donOrgNum < 2000) {
				return null;
			}
			
			DonateVO dvo=new DonateVO(0,userId,donOrgNum,donOrgName,donAmount,2,null);
			int insertDonate=this.profileServiceImpl.addDonation(dvo);
			int minusPoint=this.profileServiceImpl.minusPointByDonation(dvo);
			
			UserVO user=this.profileServiceImpl.findUserByUserId(userId);
			ses.setAttribute("user", user);
			m.addAttribute("user",user);
			
			return map;
		}

	//포인트충전리스트
	@GetMapping(value="/users-profile/rechargeList", produces = "application/json")
	@ResponseBody
	public List<PaymentVO> rechargeList(String userId, Model m){
		log.info("userId==="+userId);
		List<PaymentVO> payList=new ArrayList<PaymentVO>();
		payList=this.profileServiceImpl.findPaymentByUserId(userId);
		m.addAttribute("payList",payList);
		return payList;
	}
	//포인트 환전 리스트
	@GetMapping(value="/users-profile/exchangeList", produces = "application/json")
	@ResponseBody
	public List<ExchangeVO> exchangeList(String userId, Model m){
		log.info("userId==="+userId);
		List<ExchangeVO> exList=new ArrayList<ExchangeVO>();
		exList=this.profileServiceImpl.findExchangeByUserId(userId);
		m.addAttribute("exList",exList);
		return exList;
	}
	//포인트 기부 리스트
	@GetMapping(value="/users-profile/donateList", produces = "application/json")
	@ResponseBody
	public List<DonateVO> donateList(String userId, Model m){
		log.info("userId==="+userId);
		List<DonateVO> doList=new ArrayList<DonateVO>();
		doList=this.profileServiceImpl.findDonationByUserId(userId);
		m.addAttribute("doList",doList);
		return doList;
	}
	//프로필이미지등록&수정
	@PostMapping("/users-profile/updateImg")
	public String updateImg(@RequestParam("userImage") MultipartFile file, 
			@RequestParam("userNum") int userNum,
			Model m, HttpSession ses) {
		
		ServletContext app=ses.getServletContext();
		String upDir=app.getRealPath("/resources/User_Image");
		log.info(upDir);
		File dir=new File(upDir);
		if(!dir.exists()){
			dir.mkdirs();
		}
		if(file != null) {
			//기존 공지사항 내용 불러오기
			UserVO vo=this.profileServiceImpl.findUserByuserNum(userNum);
			String oldFileName=vo.getUserImage();//기존 파일이름
			
			//기존 파일이 있을경우 기존 파일 삭제
			if(oldFileName!=null) {
				File delf=new File(upDir,oldFileName);
				if(delf.exists()) {
					delf.delete();
				}
			}
			
			String originFname=file.getOriginalFilename();
			UUID uuid=UUID.randomUUID();
			String newfilename=uuid.toString()+"_"+originFname;
			
			//새로운 vo 객체에 파일 이름 넣기
			vo.setUserImage(newfilename);
			
			//새로운 이미지 업로드
			try {
				file.transferTo(new File(upDir,newfilename));
			}catch(Exception e) {
				log.error(e);
			}
			
			int n=profileServiceImpl.updateUserImage(vo);
			log.info(n);
			
			ses.setAttribute("user", vo);
			m.addAttribute("user",vo);
		}
		return "redirect:/users-profile";
	}
	//비밀번호체크 
//	String encryPwd=UserSHA256.encrypt(user.getUserPassword());
//	user.setUserPassword(encryPwd);
	@PostMapping("/users-profile/updateProfile/loginCheck")
	@ResponseBody
	public String passwordCheck(@RequestParam("userNum") int userNum,
			@RequestParam("password") String password, Model m) {
		String encryPwd=UserSHA256.encrypt(password);
		
		String realPassword=this.profileServiceImpl.getPassword(userNum);
		String code;
		if(realPassword.equals(encryPwd) ) {
			code="success";
		}else {
			code="fail";
		}
		return code;
	}
	//개인정보수정 
	@PostMapping("/users-profile/updateProfile")
	@ResponseBody
	public Map<String, Object> updateProfile(@RequestBody Map<String, Object> map,
	Model m, HttpSession ses) {
		int userNum=Integer.parseInt(map.get("userNum").toString());
		String userInfo=map.get("userInfo").toString();
		String userNick=map.get("userNick").toString();
		String userAddr1=map.get("userAddr1").toString();
		String userAddr2=map.get("userAddr2").toString();
		String userAddr3=map.get("userAddr3").toString();
		String userTel=map.get("userTel").toString();
		String userEmail=map.get("userEmail").toString();
		
		UserVO vo=(UserVO)ses.getAttribute("user");
		vo.setUserAddr1(userAddr1);
		vo.setUserAddr2(userAddr2);
		vo.setUserAddr3(userAddr3);
		vo.setUserEmail(userEmail);
		vo.setUserInfo(userInfo);
		vo.setUserNick(userNick);
		
		int updateProfile=this.profileServiceImpl.updateProfile(vo);
		
		ses.setAttribute("user", vo);
		m.addAttribute("user",vo);
		return map;
	}
	//비밀번호 수정
	@PostMapping("/users-profile/updatePassword")
	@ResponseBody
	public Map<String, String> updatePassword(@RequestParam("newPassword") String newPassword, HttpSession ses) {
		//log.info(newPassword);
		UserVO vo=(UserVO)ses.getAttribute("user");
		String encryPwd=UserSHA256.encrypt(newPassword);
		vo.setUserPassword(encryPwd);
		int updatePassword=this.profileServiceImpl.updatePassword(vo);
		Map<String, String> map=new HashMap<>();
		ses.setAttribute("user", vo);
		map.put("password",newPassword);
		return map;
	}
	//탈퇴회원전환
	@PostMapping("/users-profile/deleteUser")
	@ResponseBody
	public Map<String, String> deleteUser(@RequestParam("userNum") int userNum, HttpSession ses) {
		Map<String, String> map=new HashMap<>();
		UserVO user=(UserVO)ses.getAttribute("user");
		int deleteUser=this.profileServiceImpl.deleteUser(user);
		ses.removeAttribute("user");
		return map;
	}
	
	//옥션 평가
	@PostMapping("/users-profile/insertReview")
	public String insertReview(HttpServletRequest request,Model m) {
		String doUserId=request.getParameter("doUserId");
		String takeUserId=request.getParameter("takeUserId");
		String review=request.getParameter("review");
		int aucEndNum=Integer.parseInt(request.getParameter("aucEndNum"));
		int score=Integer.parseInt(request.getParameter("score"));
		AuctionSurveyVO vo=new AuctionSurveyVO(aucEndNum,doUserId,takeUserId,score,review);
		
		AuctionEndVO endvo=this.profileServiceImpl.findAuctionEnd(aucEndNum);
		
		String buyId=endvo.getBuyId();
		String sellId=endvo.getSellId();
		
		if(endvo.getAucStatus()==1) {
			if(doUserId.equals(sellId)) {
				int n=this.profileServiceImpl.insertSurvey(vo);
				endvo.setAucStatus(3);
				int a=this.profileServiceImpl.aucEndUpdate(endvo);
			}else if(doUserId.equals(buyId)) {
				int n=this.profileServiceImpl.insertSurvey(vo);
				endvo.setAucStatus(2);
				int a=this.profileServiceImpl.aucEndUpdate(endvo);
			}
		}else if(endvo.getAucStatus()==2) {
			int n=this.profileServiceImpl.insertSurvey(vo);
			endvo.setAucStatus(4);
			int a=this.profileServiceImpl.aucEndUpdate(endvo);
		}else if(endvo.getAucStatus()==3) {
			int n=this.profileServiceImpl.insertSurvey(vo);
			endvo.setAucStatus(4);
			int a=this.profileServiceImpl.aucEndUpdate(endvo);
		}
		
		m.addAttribute("survey",vo);
		
		return "redirect:/users-profile";
	}
	
	//신고기능
	@PostMapping("/users-profile/reportUser")
	public String reportUser(HttpServletRequest request,Model m) {
		int aucEndNum=Integer.parseInt(request.getParameter("aucEndNum"));
		String userId=request.getParameter("userId"); //신고자
		String reportedUserId=request.getParameter("reportedUserId"); //신고당하는사람
		String reportContent=request.getParameter("reportedUserId");
		
		ReportVO rvo=new ReportVO(0,aucEndNum,userId,reportedUserId,reportContent,0);
		
		this.profileServiceImpl.insertReport(rvo);
		
		return "redirect:/users-profile";
	}
	
}