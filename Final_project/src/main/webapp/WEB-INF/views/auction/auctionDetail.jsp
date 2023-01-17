<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<c:set var="path" value="${pageContext.request.contextPath}" />
<c:import url="/top" />s
<script src="https://cdnjs.cloudflare.com/ajax/libs/sockjs-client/1.4.0/sockjs.min.js"></script>
<link href="${path}/resources/assets/css/auctionDetail.css" rel="stylesheet">
<SCRIPT type="text/javascript">

//var now=new Date();
var isStop=false
var interval=setInterval(remaindTime,1000); //1초마다 검사를 해주면 실시간으로 시간을 알 수 있다.

//var closeTime = new Date(2023,00,13,11,09,30);

//alert(nowTime);
var nowTime = ${nowTime.getTime()}; // 현재의 시간만 가져온다
var closeTime = ${closeTime.getTime() }; // 종료시간만 가져온다
function remaindTime() {
	if(!isStop){
	   nowTime=nowTime+1000;
	   if(nowTime<closeTime){ //현재시간이 종료시간보다 이르면 종료시간까지의 남은 시간을 구한다.   
	     var sec = parseInt(closeTime - nowTime) / 1000;
	   
	     var hour = parseInt(sec/60/60);
	     
	     sec = (sec - (hour*60*60));
	     
	     var min = parseInt(sec/60);
	     
	     sec = parseInt(sec-(min*60));
	  
	     if(hour<10){hour="0"+hour;}
	     if(min<10){min="0"+min;}
	     if(sec<10){sec="0"+sec;}
	      $("#d-day-hour").html(hour);
	      $("#d-day-min").html(min);
	      $("#d-day-sec").html(sec);
	   } else if(nowTime>=closeTime){ //현재시간이 종료시간보다 크면
			$("#d-day-hour").html('00');
			$("#d-day-min").html('00');
			$("#d-day-sec").html('00');
			closeBid();
			isStop=true;
	   }
	}else{
		clearInterval(interval);
	}
}
	
  function closeBid(){
	  alert('경매가 마감되었습니다');
	  $('#btnBid').prop('disabled',"false");
	  $('#10kBtn').prop('disabled',"false");
	  $('#5kBtn').prop('disabled',"false");
	  $('#1kBtn').prop('disabled',"false");
	  $('#01kBtn').prop('disabled',"false");
	  $('#resetBtn').prop('disabled',"false");
	  location.href="${path}/productList"
  }
  
  
let number=$('#inputPrice').val();
function plus(amount){
	if(number==null){
		number=0;
	}
	if(amount === '10k'){
		number = number+10000;
	}else if(amount === '5k'){
		number = number+5000;
	}else if(amount === '1k'){
		number = number+1000;
	}else if(amount === '0.1k'){
		number = number+100;
	}else if(amount === 'reset'){
		number = 0;
	}
	$('#inputPrice').val(number);
}
</script>
<script>
//전역변수 설정
var socket  = null;
var url = "http://${pageContext.request.serverName}:${pageContext.request.serverPort}${pageContext.request.contextPath}/auction";
sock = new SockJS(url);
socket = sock;

$(document).ready(function(){
	// 웹소켓 연결
	connect();
	$('#btnBid').click(function(){
		bid();
		$('#warning').modal('hide');
		sock.onmessage();
	});
});

function connect(){
	// 데이터를 전달 받았을때 
	//sock.onmessage = onMessage;
	//sock.onclose = onClose;
		console.log('info: connection opened.');
		let str = '<div id="aucPrice" style="font-weight:bold;color:rgb(232,92,0);">';
		str += "<strong>";
		str += "${auction.aucPrice}원";
		str += "</strong>";
		str += '</div>';
		$('#tableAuc').html(str);
	/* sock.onopen = function() {
		alert('시작');
	}; */
}//connect---

function bid(){
	let prodNum=${prod.prodNum};
	let userId='${user.userId}';
	let aucPrice=$('#inputPrice').val();
	
	if(aucPrice > ${user.userPoint}){
		alert('보유하신 포인트가 부족합니다');
		return;
	}
	//전송정보 db에 저장
	var dataObj={
			"prodNum":prodNum,
	        "userId":userId,
	        "aucPrice":aucPrice
	}
	if(aucPrice==0){
		alert('입찰가를 적어주세요');
		$('#inputPrice').focus();
		return;
	}
	if(aucPrice<=${auction.aucPrice}){
		alert('현재입찰가보다 높은금액으로 입찰해야합니다')
		$('#inputPrice').val('');
		$('#inputPrice').focus();
		return;
	}
	
	$.ajax({
	       type: 'post',
	       url: 'auctionDetail/bid',
	       contentType:'application/json',
	       dataType: 'json',
	       data: JSON.stringify(dataObj),
	       success: function(){   
	           // 소켓에 전달되는 메시지
	        socket.send("${prod.prodName},"+aucPrice);
	       	alert('입찰완료되었습니다')
	       }
	   });
	$('#inputPrice').val('');
}//----

sock.onmessage=function(evt){
	if(evt!=null){
		var data = evt.data;
		console.log(evt);
		console.log(data);
		var prodName = data.split(":")[0];
		var aucPrice = data.split(":")[1];
	}
	if (evt==null) {
		var aucPrice = "${auction.aucPrice}";
	}
	let str = '<div id="aucPrice" style="font-weight:bold;color:rgb(232,92,0);text-decoration: underline blue">';
	str += "<strong>";
	str += aucPrice+"원";
	str += "</strong>";
	str += '</div>';
	$('#tableAuc').html(str);
	
}//------
</script>

<main id="main" class="main" style="font-family:KoPubWorldDotum;">
	<!-- Product section-->
	<section class="py-5">

 		<div class="mainSection">		
				<!-- 상품 상단 좌측 전체 div 시작 -->
				<div class="leftDiv">
					<!-- 이미지 slide배너 전체 div 시작 -->
					<div id="carouselExampleIndicators" class="carousel slide" data-bs-ride="carousel">
						<!-- 배너 중앙하단 1, 2 배너이동버튼 시작 -->
						<div class="carousel-indicators">
							<button type="button" data-bs-target="#carouselExampleIndicators"
								data-bs-slide-to="0" class="active" aria-current="true"
								aria-label="Slide 1"></button>
							<button type="button" data-bs-target="#carouselExampleIndicators"
								data-bs-slide-to="1" aria-label="Slide 2"></button>
						</div>
						<!-- 배너 중앙하단 1,2 배너 이동버튼 끝 -->
						
						
						<!-- 이미지 내용 시작 -->
						<div class="carousel-inner" id="imageInner">
							
							<!-- 이미지 1번 내용 -->
							<div class="carousel-item active">
								<c:if test="${prod.prodImage1 ne null}">
									<a href="${path}\resources\Product_Image/${prod.prodImage1}" style="width:100%; display:inline-block;" tabindex="-1" target='_blank'>
                           <img class="card-img-top" src="${path}\resources\Product_Image/${prod.prodImage1}">
                           </a>
								</c:if>
								<c:if test="${prod.prodImage1 eq null}">
									<img src="resources/assets/img/noImage.jpg">
								</c:if>
							</div>
							<!-- 이미지 2번 내용 -->
							<c:if test="${prod.prodImage2 ne null }">							
								<div class="carousel-item ">
									<c:if test="${prod.prodImage2 ne null}">
										<a href="${path}\resources\Product_Image/${prod.prodImage2}" style="width:100%; display:inline-block;" tabindex="0" target='_blank'>
                              <img src="${path}\resources\Product_Image/${prod.prodImage2}" class="card-img-top">
                              </a>

									</c:if>
									<c:if test="${prod.prodImage2 eq null}">
										<img src="resources/assets/img/noImage.jpg">
									</c:if>
								</div>
							</c:if>
						</div>
						<!-- 배너 이미지 내용 끝 -->

						<!-- 배너 좌우 이동 버튼 시작 -->
						<button class="carousel-control-prev" type="button"
							data-bs-target="#carouselExampleIndicators" data-bs-slide="prev">
							<span class="carousel-control-prev-icon" aria-hidden="true"></span>
							<span class="visually-hidden">Previous</span>
						</button>
						<button class="carousel-control-next" type="button"
							data-bs-target="#carouselExampleIndicators" data-bs-slide="next">
							<span class="carousel-control-next-icon" aria-hidden="true"></span>
							<span class="visually-hidden">Next</span>
						</button>
						<!-- 배너 좌우 이동 버튼 끝 -->
					</div>
					<!-- 이미지 slide배너 전체 div 끝-->

				</div>
				<!-- 상품 상단 좌측 전체 div 끝 -->
				
				
				<!-- 상품 상단 우측 전체 div 시작 -->
				<div class="rightDiv">

					<h3 class="fw-bolder">${prod.prodName }</h3>
					<div class="large"><a class="badge border-primary border-2 text-secondary" style="margin-left:10%;text-align:right;font-size:0.9rem;
					background-color:rgb(248,249,255);width:60%">${seller.userNick} [ID : ${seller.userId}]  |   ${average} / 5 점</a></div>
					<br>
					<div class="fs-5">
						<table class="rightTable" style="margin-bottom:7px;">
							<tr>
								<td style="padding-right:3%;width:10rem">시작가</td>
								<td class="d-flex justify-content-end">${prod.aucStartPrice }원</td>
							</tr>
							<tr>
								<td style="padding-right:3%;"><strong>현재입찰가</strong></td>
								<td id="tableAuc" class="d-flex justify-content-end"></td>
							</tr>
							<tr>
								<td style="padding-right:3%;">시작시간</td>
								<td class="d-flex justify-content-end" style="color:grey;font-size:18px;vertical-align:middle;">
									<fmt:formatDate value="${prod.getPIndate() }" pattern="yy.MM.dd HH:mm:ss"/>
								</td>
							</tr>
							<tr>
								<td style="padding-right:3%;">종료시간</td>
								<td class="d-flex justify-content-end" style="font-size:18px;vertical-align:middle;">
									<fmt:formatDate value="${closeTime }" pattern="yy.MM.dd HH:mm:ss"/>
								</td>
							</tr>
							<tr>
								<td style="padding-right:3%;"><strong>남은시간</strong></td>
								<td class="d-flex justify-content-end">
									<div class="time" style="color:rgb(232,92,0);font-weight:bold">
										<span id="d-day-hour"></span> 
										<span class="col">h</span> 
										<span id="d-day-min"></span> 
										<span class="col">m</span> 
										<span id="d-day-sec"></span>
										<span class="col"></span>
									</div>
								</td>
							</tr>
							<c:if test="${user.userId ne prod.userId}">
								<tr>
									<td colspan="2" class="text-center" >
										<input style="margin-top:15px;" class="form-control text-end" 
											id="inputPrice" type="number" 
											placeholder="입찰포인트를 정해주세요" style="width:100%; letter-spacing: 3px"	>
									</td>
								</tr>
								<tr>
									<td colspan="2">
										<div class="text-center" style="margin-top:5px;">
			                              <button type="button" id="10kBtn" onclick=plus("10k") class="btn btn-outline-primary btn-sm">+10000</button>
			                              <button type="button" id="5kBtn" onclick=plus("5k") class="btn btn-outline-primary btn-sm">+5000</button>
			                              <button type="button" id="1kBtn" onclick=plus("1k") class="btn btn-outline-primary btn-sm">+1000</button>
			                              <button type="button" id="01kBtn" onclick=plus("0.1k") class="btn btn-outline-primary btn-sm">+100</button>
			                              <button type="button" id="resetBtn" onclick=plus("reset") class="btn btn-outline-primary btn-sm">Reset</button>
		                             	</div>
									</td>
								</tr>
							</c:if>
						</table>
						<div style="clear:both"></div> 
						<c:if test="${user.userId ne prod.userId}"> 
							<span class="text-end col-3">
								<a class="btn btn-secondary btn-lg" id="btnChat" type="button" href="${path}/addChatRoom?sellerId=${prod.userId}&prodNum=${prod.prodNum}"><strong>판매자와 채팅</strong></a>
							</span>
							<span class="text-start" >
								<button class="btn btn-lg" style="color:white;background-color:rgb(232,92,0);" data-bs-toggle="modal" data-bs-target="#warning" type="button" ><strong>입찰하기</strong></button>
							</span> 
						</c:if>
					</div>
				</div>
			</div>
			
				 <!-- 비밀번호 확인 Modal -->
                     <div class="modal fade" id="warning" tabindex="-1">
                      <div class="modal-dialog modal-dialog-centered">
                        <div class="modal-content">
                          <div class="text-center modal-header">
                            <h5 class="modal-title">주의 사항</h5>
                            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                          </div>
                          <div class="modal-body text-center" style="width:70%;margin:auto"> 
                            	<h6>* 입찰가의 취소나 변경, 교환이 불가능하니 최대한 신중히 입찰해 주세요</h6>
                            	<h5>입찰 하시겠습니까?</h5>
                          </div>
                          <div class="modal-footer" >
                            <button type="button" id="btnBid" class="btn btn-primary">예</button>
                            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">아니오</button>
                          </div>
                        </div>
                      </div>
                    </div>
                  <!-- 비밀번호확인 Modal 끝-->
			
	</section>
	<div style="clear:both"></div>
	<br>
	<section class="section2">
		<div class="text-left lead" style="padding-left:10%;padding-right:10%" id="contents">
			<br><h5 style="margin-left:3%;"><strong>물품소개</strong></h5>
			<hr>
		
		<p style="margin-left: 4%; margin-right: 4%;font-size:17px;">${prod.prodSpec}</p>
		</div>
	</section>
</main>
<!-- End #main -->
<c:import url="/foot" />