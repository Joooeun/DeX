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
		if (isAdmin) {
			// 대시보드 탭 클릭 시 연결 모니터링 시작
			$('#pageTab a[href="#dashboard"]').on('shown.bs.tab', function (e) {
				startConnectionMonitoring();
			});

			// 다른 탭 클릭 시 연결 모니터링 중지
			$('#pageTab a[href!="#dashboard"]').on('shown.bs.tab', function (e) {
				stopConnectionMonitoring();
			});
		}

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

	// 연결 상태 모니터링 관련 변수
	var connectionStatusInterval;

	// 연결 상태 새로고침 함수
	function refreshConnectionStatus() {
		$.ajax({
			type: 'post',
			url: '/Connection/status',
			success: function(result) {
				updateConnectionStatusDisplay(result);
				// 연결 상태 확인 완료 후 다음 타이머 설정
				scheduleNextRefresh();
			},
			error: function() {
				console.error('연결 상태 조회 실패');
				// 에러 발생 시에도 다음 타이머 설정
				scheduleNextRefresh();
			}
		});
	}

	// 연결 상태 표시 업데이트 함수
	function updateConnectionStatusDisplay(connections) {
		console.log(connections)
		var container = $('#connectionStatusContainer');
		
		// 초기 로드인지 확인 (컨테이너가 비어있으면 초기 로드)
		var isInitialLoad = container.children().length === 0;
		
		if (isInitialLoad) {
			// 초기 로드: 전체 카드 생성
			connections.forEach(function(conn) {
				createConnectionCard(conn);
			});
		} else {
			// 업데이트: 기존 카드의 상태만 변경
			connections.forEach(function(conn) {
				updateConnectionCard(conn);
			});
		}
	}
	
	// 연결 카드 생성 함수
	function createConnectionCard(conn) {
		var statusIcon = conn.status === 'connected' ? 'fa-check-circle' : 'fa-times-circle';
		var statusText = conn.status === 'connected' ? '연결됨' : '연결실패';
		var formattedTime = formatDateTime(conn.lastChecked);
		
		var connectionCard = 
			'<div class="col-md-3 col-sm-4 col-xs-6" style="margin-bottom: 15px;" id="card-' + conn.connectionName + '">' +
				'<div class="connection-card" style="' +
					'border: 2px solid ' + conn.color + ';' +
					'border-radius: 10px;' +
					'padding: 15px;' +
					'text-align: center;' +
					'background: white;' +
					'box-shadow: 0 2px 4px rgba(0,0,0,0.1);' +
					'transition: all 0.3s ease;' +
					'cursor: pointer;' +
					'height: 100px;' +
					'display: flex;' +
					'flex-direction: column;' +
					'justify-content: center;' +
				'" onmouseover="this.style.transform=\'scale(1.05)\'" onmouseout="this.style.transform=\'scale(1)\'" onclick="refreshSingleConnection(\'' + conn.connectionName + '\')">' +
					'<div style="font-size: 24px; margin-bottom: 10px;">' +
						'<i class="fa fa-database" style="color: ' + conn.color + ';"></i>' +
					'</div>' +
					'<div style="font-weight: bold; margin-bottom: 5px; color: #333;">' +
						conn.connectionName +
					'</div>' +
					'<div style="font-size: 12px; color: ' + conn.color + ';" id="status-' + conn.connectionName + '">' +
						'<i class="fa ' + statusIcon + '"></i> ' + statusText +
					'</div>' +
					'<div style="font-size: 10px; color: #666; margin-top: 5px;" id="lastChecked-' + conn.connectionName + '">' +
						formattedTime +
					'</div>' +
				'</div>' +
			'</div>';
		
		$('#connectionStatusContainer').append(connectionCard);
	}
	
	// 연결 카드 상태 업데이트 함수
	function updateConnectionCard(conn) {
		var card = $('#card-' + conn.connectionName);
		if (card.length === 0) {
			// 카드가 없으면 새로 생성
			createConnectionCard(conn);
			return;
		}
		
		var statusIcon = conn.status === 'connected' ? 'fa-check-circle' : 'fa-times-circle';
		var statusText = conn.status === 'connected' ? '연결됨' : '연결실패';
		var formattedTime = formatDateTime(conn.lastChecked);
		
		// 상태 텍스트 업데이트
		$('#status-' + conn.connectionName).html('<i class="fa ' + statusIcon + '"></i> ' + statusText);
		
		// 시간 업데이트
		$('#lastChecked-' + conn.connectionName).text(formattedTime);
		
		// 색상과 테두리 업데이트 (부드러운 전환 효과)
		var connectionCard = card.find('.connection-card');
		connectionCard.css({
			'border-color': conn.color,
			'color': conn.color
		});
		
		// 데이터베이스 아이콘 색상 업데이트
		connectionCard.find('.fa-database').css('color', conn.color);
		
		// 상태 아이콘 색상 업데이트
		$('#status-' + conn.connectionName).css('color', conn.color);
	}

	// 단일 연결 상태 수동 새로고침
	function refreshSingleConnection(connectionName) {
		$.ajax({
			type: 'post',
			url: '/Connection/status/refresh',
			data: {
				connectionName: connectionName
			},
			success: function(result) {
				if (result === 'success') {
					// 새로고침 후 상태 다시 조회
					setTimeout(function() {
						refreshConnectionStatus();
					}, 500);
				}
			},
			error: function() {
				console.error('연결 상태 수동 새로고침 실패');
			}
		});
	}

	// 날짜 시간 포맷팅 함수
	function formatDateTime(dateTimeStr) {
		if (!dateTimeStr) return '';
		var date = new Date(dateTimeStr);
		var hours = ('0' + date.getHours()).slice(-2);
		var minutes = ('0' + date.getMinutes()).slice(-2);
		var seconds = ('0' + date.getSeconds()).slice(-2);
		return hours + ':' + minutes + ':' + seconds;
	}

	// 자동 새로고침 시작 함수
	function startConnectionMonitoring() {
		// 초기 로드
		refreshConnectionStatus();
	}

	// 연결 상태 확인 완료 후 다음 타이머 설정
	function scheduleNextRefresh() {
		// 기존 타이머가 있으면 제거
		if (connectionStatusInterval) {
			clearTimeout(connectionStatusInterval);
		}
		
		// 10초 후에 다음 연결 상태 확인 실행
		connectionStatusInterval = setTimeout(function() {
			refreshConnectionStatus();
		}, 10000);
	}

	// 자동 새로고침 중지 함수
	function stopConnectionMonitoring() {
		if (connectionStatusInterval) {
			clearTimeout(connectionStatusInterval);
			connectionStatusInterval = null;
		}
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



					<c:if test="${memberId eq 'admin'}">
						<li class="treeview"><a><i class="fa fa-code-fork"></i><span>5e83eb5 / 46246f0</span></a></li>
						<li class="treeview"><a href="/Connection" target="iframe"> <i class="fa fa-database"></i> <span>Connection</span>

						</a> <!-- <ul class="treeview-menu" id="ConnectionList">
							<li><a href="/Connection?DB=2"><i class="fa fa-circle-o"></i> DB1</a></li>
							<li><a href="/Connection?DB=1"><i class="fa fa-circle-o"></i> DB2</a></li>
						</ul> --></li>

						<li class="treeview"><a href="/User" target="iframe"> <i class="fa fa-user"></i> <span>User</span>

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
						<div class="content-wrapper" style="margin-left: 0; padding: 20px;">
							<div class="row">
								<div class="col-md-12">
									<div class="box box-primary">
										<div class="box-header with-border">
											<h3 class="box-title">
												<i class="fa fa-database"></i> 데이터베이스 연결 상태 모니터링
											</h3>
											<div class="box-tools pull-right">
												<button type="button" class="btn btn-box-tool" onclick="refreshConnectionStatus()">
													<i class="fa fa-refresh"></i> 새로고침
												</button>
											</div>
										</div>
										<div class="box-body">
											<div id="connectionStatusContainer" class="row">
												<!-- 연결 상태가 여기에 동적으로 추가됩니다 -->
											</div>
										</div>
									</div>
								</div>
							</div>
						</div>
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