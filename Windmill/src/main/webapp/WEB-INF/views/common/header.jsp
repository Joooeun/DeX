<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Dex</title>
<%@include file="common.jsp"%>
<style type="text/css">
body {
	margin: 0;
}

#sidemenu {
	max-height: calc(100vh - 330px);
	overflow-y: auto;
}

::-webkit-scrollbar {
	width: 3px;
	height: 7px;
	border: 2px solid #fff;
}

::-webkit-scrollbar-track {
	background: #efefef;
	-webkit-border-radius: 10px;
	border-radius: 10px;
	-webkit-box-shadow: inset 0 0 4px rgba(0, 0, 0, .2)
}

::-webkit-scrollbar-thumb {
	height: 50px;
	width: 50px;
	background: rgba(0, 0, 0, .2);
	-webkit-border-radius: 8px;
	border-radius: 8px;
	-webkit-box-shadow: inset 0 0 4px rgba(0, 0, 0, .1)
}
</style>
</head>

<script>

var changePW
			$(document).ready(function() {
		
		changePW = '${changePW}' === 'true';
		
		if(changePW){
			
			$('#changePWModal').modal({backdrop: 'static', keyboard: false});
			$('#changePWModal').modal('show');
		}
		
		getMenu();

		// admin인 경우에만 대시보드 관련 기능 활성화
		var isAdmin = '${isAdmin}' === 'true';

		/* ------------------------------공지사항 start------------------------------ */
		var checkCookie = getCookie("mycookie");
			
		if(checkCookie == 'popupEnd') {
			$("#NoticeModal").modal("hide");
		} else {
			$('#NoticeModal').modal("show");	
		}
		
		$("#modal-today-close").click(function() {
			$("#NoticeModal").modal("hide");
			setCookie("mycookie", 'popupEnd', 1);
		})
		/* ------------------------------공지사항 end------------------------------ */

		$(document).on("click", ".addtree", function() {

			if ($(this).parent().attr('class').includes('active')) {
				$(this).parent().removeClass('active');
			} else {
				$(this).parent().addClass('active');
			}

		});

		$('#pageTab').on('click', ' li a .close', function() {
			var tabId = $(this).parents('li').children('a').attr('href');
			$(this).parents('li').remove('li');
			$(tabId).remove();
			$('#pageTab a:first').tab('show');
		});

		/**
		 * Click Tab to show its content 
		 */
		$("#pageTab").on("click", "a", function(e) {
			e.preventDefault();
			$(this).tab('show');
		});

	});
		
	/* ------------------------------공지사항 start------------------------------ */
	function setCookie(name, value, expiredays){
		var today = new Date();
	
		console.log(today.getDate())
	
		today.setDate(today.getDate() + expiredays); // 현재시간에 하루를 더함 
	
		document.cookie = name + '=' + escape(value) + '; expires=' + today.toGMTString();
	
	}
		
	function getCookie(name) {
	
		var cookie = document.cookie;
		
		if (document.cookie != "") {
			var cookie_array = cookie.split("; ");
			console.log(cookie_array)
			for ( var index in cookie_array) {
				var cookie_name = cookie_array[index].split("=");
				if (cookie_name[0] == "mycookie") {
					return cookie_name[1];
				}
			}
		}
		return;
	}
	/* ------------------------------공지사항 end------------------------------ */

	function Search() {

		if ($('#' + $("#search").val()).length == 0) {
			alert("메뉴가 없습니다.")
			return false;
		}
		
		const iframe = document.querySelector('#pageTabContent > div:last-child > iframe');
		iframe.contentWindow.location.href = $('#' + $("#search").val()).attr('href');

		return false;
	}

	var pageImages = [];
	var pageNum = 1;

	function setFrame(frameid) {

		var text = $('#' + frameid).contents().find('.content-header>h1').text().trim();

		if (text == '') {
			return;
		} else {
			//console.log('text : ', $('#' + frameid).contents().find('.content-header>h1').text(), frameid)
			var newtab = true;
			for (var i = 0; i < $('#pageTab a').length; i++) {
				//console.log('text2 : ', text, $('#pageTab a:eq(' + i + ')').text().replace(/x$/, ''))
				if (text == $('#pageTab a:eq(' + i + ')').text()) {

					newtab = false;
					$('#pageTab a:eq(' + i + ')').tab('show');
					break;
				}

			}
			if (!newtab) {
				return false;
			}

		}
		var pageid = pageNum++;

		$('#pageTab')
				.append(
						'<li><a href="#tab' + pageid+'" data-toggle="tab">'
								+ text
								+ '<button class="close" type="button" title="Remove this page" style="padding-left:3px"><i class="fa fa-close"></button></a></li>')
		$('#pageTabContent>div:last').attr("id", 'tab' + pageid);
		$('#pageTab a:last').tab('show');
		$('#pageTabContent')
				.append(
						'<div class="tab-pane" id="newpage"><iframe name="iframe'
								+ pageid
								+ '" id="iframe'
								+ pageid
								+ '" class="tab_frame" style="margin: 0; width: 100%; height: calc(100vh - 90px); border: none; overflow: auto;" onLoad="setFrame(\'iframe'
								+ pageid + '\')"></iframe></div>')

		$('.sidebar-menu a:not(\'.addtree\')')
				.attr("target", 'iframe' + pageid);

		$('#iframe_1').contents().find('#menus a').attr("target",
				'iframe' + pageid);
		
		// SQLTemplate의 "해당 메뉴로 이동" 링크 타겟도 변경
		$('#iframe_1').contents().find('#goToTemplateLink').attr("target",
				'iframe' + pageid);
		//alert($('#iframe' + (pageNum == 1 ? '' : pageNum - 1)).contents().find('.ParamForm').length)
		//$('.iframe').contents().find('.ParamForm').attr("target", 'iframe' + pageid)
		//console.log($('#iframe').contents().find('.content-header>h1').text())

	}

	function checkPWModal() {
		$('#checkPWModal').modal('show')
	}

	function save() {

		if ($('#PW').val() != $('#newPW').val()) {
			alert("비밀번호가 일치하지 않습니다.")
		} else {
			var lowerCaseLetters = /[a-z]|[A-Z]/g;
			var numbers = /[0-9]/g;

			if ($('#PW').val().match(lowerCaseLetters)
					&& $('#PW').val().match(numbers)) {
			} else {
				alert("비밀번호는 영문, 숫자를 포함해야 합니다.");
				return;
			}

			if ($('#PW').val().length >= 8) {
			} else {
				alert("비밀번호는 최소 8자리 이상입니다.");
				return;
			}

			$.ajax({
				type : 'post',
				url : '/User/changePW',
				data : {
					PW : $('#PW').val(),
				},
				success : function(result) {
					alert("저장 되었습니다.");
					$('#changePWModal').modal('hide')
					$('#PW').val("")
					$('#newPW').val("")
				},
				error : function() {
					alert("저장되지 않았습니다.");
				}
			});

		}
	}

	function checkPW() {
		$.ajax({
			type : 'post',
			url : '/User/checkPW',
			data : {
				PW : $('#curPW').val(),
			},
			success : function(result) {

				if (result) {
					$('#checkPWModal').modal('hide')
					$('#changePWModal').modal('show')
				} else {
					alert("잘못된 비밀번호 입니다.");
				}
				$('#curPW').val("")
			},
			error : function(e) {
				alert("저장되지 않았습니다." + JSON.stringify(e));
			}
		});

	}


	
</script>

<body class="sidebar-mini skin-purple-light">

	<div class="wrapper">
		<header class="main-header">
			<!-- Logo -->
			<a href="/index" class="logo"> <!-- mini logo for sidebar mini 50x50 pixels --> <span class="logo-mini"> <b>D</b>eX
			</span> <!-- logo for regular state and mobile devices --> <span class="logo-lg"> <b>Data</b> Explorer
			</span>
			</a>
			<!-- Header Navbar: style can be found in header.less -->
			<nav class="navbar navbar-static-top" role="navigation">
				<!-- Sidebar toggle button-->
				<a href="#" class="sidebar-toggle" data-toggle="offcanvas" role="button"> <span class="sr-only">Toggle navigation</span> <span class="icon-bar"></span> <span class="icon-bar"></span> <span
					class="icon-bar"></span>
				</a>
				<div class="navbar-custom-menu">
					<ul class="nav navbar-nav">
						<li><a href="javascript:checkPWModal()">${memberId}</a></li>
						<li><a href="/userRemove"><i class="fa fa-sign-out"></i></a></li>
					</ul>
				</div>
			</nav>
		</header>
		<!-- Left side column. contains the logo and sidebar -->
		<aside class="main-sidebar">
			<!-- sidebar: style can be found in sidebar.less -->
			<section class="sidebar" id="sidebar">
				<!-- search form -->
				<form class="sidebar-form" onsubmit="return Search()">
					<div class="input-group">
						<input type="text" name="q" class="form-control" placeholder="Search..." id="search" /> <span class="input-group-btn">
							<button type="button" name='search' id='search-btn' class="btn btn-flat" onclick="Search()">
								<i class="fa fa-search"></i>
							</button>
						</span>
					</div>
				</form>
				<!-- /.search form -->
				<!-- sidebar menu: : style can be found in sidebar.less -->
				<ul class="sidebar-menu" data-widget="tree" id="tree">
					<li class="header">MAIN NAVIGATION</li>



					<c:if test="${isAdmin}">
						<li class="treeview"><a><i class="fa fa-code-fork"></i><span>5e83eb5 / 46246f0</span></a></li>
						<li class="treeview"><a href="/Connection" target="iframe"> <i class="fa fa-database"></i> <span>Connection</span>

						</a> <!-- <ul class="treeview-menu" id="ConnectionList">
							<li><a href="/Connection?DB=2"><i class="fa fa-circle-o"></i> DB1</a></li>
							<li><a href="/Connection?DB=1"><i class="fa fa-circle-o"></i> DB2</a></li>
						</ul> --></li>

						<li class="treeview"><a href="/User" target="iframe"> <i class="fa fa-user"></i> <span>User</span>

						</a></li>
						
						<li class="treeview"><a href="/SQLTemplate" target="iframe"> <i class="fa fa-code"></i> <span>SQL 템플릿 관리</span>
						</a></li>
					</c:if>

					<li class="treeview"><a href="/FileRead" target="iframe"> <i class="fa fa-file-text-o"></i> <span>FileRead</span>
					</a></li>
					<li class="treeview"><a href="/FileUpload" target="iframe"> <i class="fa fa-file-text-o"></i> <span>FileUpload</span>
					</a></li>

					<li id="sqltree" class="active treeview menu-open"></li>
				</ul>
			</section>
			<!-- /.sidebar -->
		</aside>
		<div class="content-wrapper" id="framebox">
			<ul id="pageTab" class="nav nav-tabs">
				<li class="active"><a href="#page1" data-toggle="tab">전체메뉴</a></li>
				<c:if test="${isAdmin}">
					<li><a href="#dashboard" data-toggle="tab">대시보드</a></li>
				</c:if>
			</ul>
			<div id="pageTabContent" class="tab-content">
				<div class="tab-pane active" id="page1">
					<iframe name="iframe_1" id="iframe_1" style="margin: 0; width: 100%; height: calc(100vh - 90px); border: none; overflow: auto;" src="/index2"></iframe>
				</div>
				<c:if test="${isAdmin}">
					<div class="tab-pane" id="dashboard">
						<iframe name="iframe_dashboard" id="iframe_dashboard" style="margin: 0; width: 100%; height: calc(100vh - 90px); border: none; overflow: auto;" src="/Dashboard"></iframe>
					</div>
				</c:if>
				<div class="tab-pane" id="newpage">
					<iframe name="iframe" id="iframe" class="tab_frame" style="margin: 0; width: 100%; height: calc(100vh - 90px); border: none; overflow: auto;" onload="setFrame('iframe')"></iframe>
				</div>

			</div>
		</div>

		<!-- 비번 변경 Modal -->
		<div class="modal fade" id="changePWModal" tabindex="-1" role="dialog" aria-labelledby="myModalLabel">
			<div class="modal-dialog" role="document">
				<div class="modal-content">
					<div class="modal-header">
						<c:if test=" ${changePW==true}">
							<button type="button" class="close" data-dismiss="modal" aria-label="Close">
								<span aria-hidden="true">&times;</span>
							</button>
						</c:if>
						<h4 class="modal-title" id="myModalLabel">비밀번호 변경</h4>
					</div>
					<div class="modal-body">
						<div class="form-group">
							<label for="PW">새 비밀번호</label> <input type="password" class="form-control" id="PW" placeholder="새 비밀번호" maxlength="16">
						</div>

						<div class="form-group">
							<label for="PW">새 비밀번호 확인</label> <input type="password" class="form-control" id="newPW" placeholder="새 비밀번호 확인" maxlength="16">
						</div>
					</div>
					<div class="modal-footer">
						<c:if test=" ${changePW==true}">
							<button type="button" class="btn btn-default" data-dismiss="modal">취소</button>
						</c:if>

						<button type="button" class="btn btn-primary" onclick="save()">저장</button>
					</div>
				</div>
			</div>
		</div>

		<!-- 비번 확인 Modal -->
		<div class="modal fade" id="checkPWModal" tabindex="-1" role="dialog" aria-labelledby="myModalLabel">
			<div class="modal-dialog" role="document">
				<div class="modal-content">
					<div class="modal-header">
						<button type="button" class="close" data-dismiss="modal" aria-label="Close">
							<span aria-hidden="true">&times;</span>
						</button>
						<h4 class="modal-title" id="myModalLabel">비밀번호 확인</h4>
					</div>
					<div class="modal-body">

						<div class="form-group">
							<label for="PW">현재 비밀번호</label> <input type="password" class="form-control" id="curPW" placeholder="현재 비밀번호">
						</div>
					</div>
					<div class="modal-footer">
						<button type="button" class="btn btn-default" data-dismiss="modal">취소</button>
						<button type="button" class="btn btn-primary" onclick="checkPW()">확인</button>
					</div>
				</div>
			</div>
		</div>

        <!-- ------------------------------공지사항 start------------------------------ -->
		<div class="modal" id="NoticeModal" tabindex="-1" role="dialog">
			<div class="modal-dialog" role="document">
				<div class="modal-content">
					<div class="modal-header">
						<h5 class="modal-title">공지</h5>
						<button type="button" class="close" data-dismiss="modal" aria-label="Close">
							<span aria-hidden="true">&times;</span>
						</button>
					</div>
					<div class="modal-body">
						<p>내용</p>
					</div>
					<div class="modal-footer">
						<button type="button" class="btn btn-primary" id="modal-today-close">오늘 하루 열지 않기</button>
						<button type="button" class="btn btn-secondary" data-dismiss="modal">닫기</button>
					</div>
				</div>
			</div>
		</div>
		<!-- ------------------------------공지사항 end------------------------------ -->