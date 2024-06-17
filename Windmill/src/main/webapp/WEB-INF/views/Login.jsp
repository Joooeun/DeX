<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Data Explorer</title>
<link href="/resources/bootstrap/css/bootstrap.min.css" rel="stylesheet" type="text/css" />
<!-- Font Awesome Icons -->
<link href="/resources/font-awesome-4.7.0/css/font-awesome.min.css" rel="stylesheet" type="text/css" />
<!-- Ionicons -->
<link href="/resources/ionicons/2.0.1/css/ionicons.min.css" rel="stylesheet" type="text/css" />
<!-- Theme style -->
<link href="/resources/dist/css/AdminLTE.min.css" rel="stylesheet" type="text/css" />
<!-- AdminLTE Skins. Choose a skin from the css/skins 
         folder instead of downloading all of them to reduce the load. -->
<link href="/resources/dist/css/skins/_all-skins.min.css" rel="stylesheet" type="text/css" />
<style type="text/css">
* {
	padding: 0;
	margin: 0;
}

body {
background-color: #f2f4f7; 
	overflow: hidden;
}

span {
	color : #626466;
	font-size: 90px;
	font-weight: bold;
	text-shadow: 1px 1px 2px #626466;
}

div {
	text-align: center;
	align-items: center;
}
img{

}

input {
	background-color: #ffffff; 
	border : 1px solid #aaaaaa;
	border-radius : 25px;
	width: 400px;
	height: 40px;
	border: none;
	padding: 5px 20px;
	font-size: 17px;
	border-radius: 25px;
	box-shadow: 2px 2px 2px #aaaaaa;
	margin-top: 10px;
}

#frame {
	margin-top: 15%;
	animation-duration: 1s;
	animation-name: slidein;
}

@keyframes slidein {
from { 
	margin-top:20%;
	opacity: 0;
}

to {
	margin-top: 15%;
	opacity: 0.8;
}
}
</style>
</head>
<body>
	<div id="frame">
		<form action="/index/login" method="post">
			<div>
				<span>Data Explorer</span>
			</div>
			<div>
				<input type="text" placeholder="id" name="id" required="required" pattern="\S(.*\S)?" title="공백은 입력할 수 없습니다."><br>
				<input type="password" placeholder="pw" name="pw" required="required" pattern="\S(.*\S)?" title="공백은 입력할 수 없습니다."><br>
				<button type="submit" class="btn bg-purple margin">Login</button>
			</div>
		</form>
	</div>
</body>
</html>
