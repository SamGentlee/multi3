<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<link rel="stylesheet"
	href="https://cdn.jsdelivr.net/npm/bootstrap@4.6.2/dist/css/bootstrap.min.css">
<script
	src="https://cdn.jsdelivr.net/npm/jquery@3.6.1/dist/jquery.min.js"></script>
<script
	src="https://cdn.jsdelivr.net/npm/popper.js@1.16.1/dist/umd/popper.min.js"></script>
<script
	src="https://cdn.jsdelivr.net/npm/bootstrap@4.6.2/dist/js/bootstrap.bundle.min.js"></script>
<script type="text/javascript"
	src="https://cdnjs.cloudflare.com/ajax/libs/sockjs-client/1.1.5/sockjs.min.js"></script>
<style>
#wrap {
	margin: 15 15 auto;
}

#chatLog {
	height: 80%;
	background-color: skyblue;
	border: 1px;
	border-radius: 10px;
}

#sendBox {
	height: 13%;
	width: 100%;
	border: 1px;
}

#msg {
	margin: 15 auto;
	width: 80%;
	height:60px;
	border-radius: 10px;
	border: 5px solid skyblue;
	float: left;
	resize: none;
}

button {
	margin: 27 0 0 10;
}

#chatLog{
	overflow: auto;
	list-style: none;
}
div#left{
	text-align:left;
	margin:25px 5px;
}
div#right{
	text-align:right;
	margin:25px 5px;
}
span#left{
	padding:10px;
	background-color:white;
	border-radius: 10px;
}

span#right{
	padding:10px;
	background-color:yellow;
	border-radius: 10px;
}
</style>
<script>
	$(function(){
	
		//전송 버튼 누를때 데이터 전송 
		$("#sendBtn").click(function() {
			if($('#msg').val()!=""){
				sendMessage(${roomid}+":"+${id}+":"+$("#msg").val());
			}
			$('#msg').val('');
		});
		
		// 웹소켓 연결
		var url = "http://${pageContext.request.serverName}:${pageContext.request.serverPort}${pageContext.request.contextPath}/chat";
		let sock = new SockJS(url);
		sock.onmessage = onMessage;
		sock.onopen = onOpen;
		
		//방에 입장 했을때 
		function onOpen(){
			sock.send(${roomid}+":"+${id}+":Enter");
		}
		
		//메시지를 전송 했을때
		function sendMessage(msg) {
			sock.send(msg);
		}
		
		//메시지를 받았을때
		function onMessage(msg) {
			var data = msg.data;
			var userid = data.split(":")[0];
			var position;
			if(userid == ${id} ){
				position="right";
			}else{
				position="left";
			}
			$('#chatLog').append("<div id="+position+"><span id="+position+">" + data + "</span></div>");
		}
	})
</script>
<div id="wrap">
	<div id="chatLog">
		<!-- 채팅 목록이 들어올 장소 -->
	</div>
	<div id="sendBox">
		<input id="msg" />
		<button id="sendBtn" class="btn btn-primary rounded-pill">전송</button>
	</div>
</div>