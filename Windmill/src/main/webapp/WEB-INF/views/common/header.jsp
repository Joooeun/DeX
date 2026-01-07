<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Dex</title>
<%@include file="common.jsp"%>
<!-- 공통 CSS 파일 로드 -->
<link href="/resources/css/common.css" rel="stylesheet" type="text/css" />
<!-- JavaScript 모듈 로드 -->
<script src="/resources/js/tabVisibilityManager.js"></script>
<script src="/resources/js/tabManager.js"></script>
<script src="/resources/js/menuSearch.js"></script>
<script src="/resources/js/headerUtils.js"></script>
</head>

	<script>
	$(document).ready(function() {
		// 비밀번호 변경 모달 처리
		var changePW = '${changePW}' === 'true';
		if (changePW) {
			$('#changePWModal').modal({backdrop: 'static', keyboard: false});
			$('#changePWModal').modal('show');
		}
		
		getMenu();

		// admin인 경우에만 대시보드 관련 기능 활성화
		var isAdmin = '${isAdmin}' === 'true';

		/* ------------------------------공지사항 start------------------------------ */
		try {
			var checkNotice = localStorage.getItem("noticeClosed");
			var closedDate = localStorage.getItem("noticeClosedDate");
			var today = new Date().toDateString();
			
			// 오늘 날짜에 닫았는지 확인
			if(checkNotice == 'popupEnd' && closedDate == today) {
				$("#NoticeModal").modal("hide");
			} else {
				$('#NoticeModal').modal("show");	
			}
		} catch (e) {
			// localStorage 오류 시 모달 표시
			$('#NoticeModal').modal("show");
		}
		
		// 공지사항 닫기 버튼 - 이벤트 위임 사용
		$(document).on("click", "#modal-today-close", function() {
			$("#NoticeModal").modal("hide");
			try {
				var today = new Date().toDateString();
				localStorage.setItem("noticeClosed", 'popupEnd');
				localStorage.setItem("noticeClosedDate", today);
			} catch (e) {
				console.error('공지사항 닫기 정보 저장 실패:', e);
			}
		});
		/* ------------------------------공지사항 end------------------------------ */

		$(document).on("click", ".addtree", function() {
			if ($(this).parent().attr('class').includes('active')) {
				$(this).parent().removeClass('active');
			} else {
				$(this).parent().addClass('active');
			}
		});

		// 탭 닫기 버튼 - 이벤트 위임 사용 (이미 위임 사용 중, tabManager.js에서도 처리)
		// tabManager.js에서 이미 처리하므로 중복 제거 가능하지만 하위 호환성을 위해 유지

		// 탭 클릭 - 이벤트 위임 사용 (이미 위임 사용 중)
		$("#pageTab").on("click", "a", function(e) {
			e.preventDefault();
			$(this).tab('show');
			
			// 탭 클릭 시 해당 탭을 마지막 순서로 이동
			var templateId = $(this).attr('data-template-id');
			if (templateId && window.tabManager) {
				window.tabManager.moveTabToLast(templateId);
			}
		});

		// 자동완성 관련 이벤트 핸들러
		$('#search').on('input', function() {
			var searchTerm = $(this).val().trim();
			if (searchTerm.length >= 1) {
				// 권한 정보 전달하여 메뉴 아이템 수집
				var permissions = {
					isAdmin: '${isAdmin}' === 'true',
					hasDashboardPermission: '${hasDashboardPermission}' === 'true',
					hasFileReadPermission: '${hasFileReadPermission}' === 'true',
					hasFileWritePermission: '${hasFileWritePermission}' === 'true'
				};
				window.MenuSearch.collectMenuItems(permissions);
				var filtered = window.MenuSearch.filterMenus(searchTerm);
				window.MenuSearch.showAutocomplete(filtered);
			} else {
				window.MenuSearch.hideAutocomplete();
			}
		});
		
		// 자동완성 아이템 클릭 이벤트
		$(document).on('click', '.autocomplete-item', function() {
			var menuType = $(this).data('menu-type');
			var onclick = $(this).data('onclick');
			
			if (onclick) {
				// onclick이 있으면 탭 추가 (SQL 메뉴 및 관리자 메뉴 모두)
				window.MenuSearch.executeTemplateTabFunction(onclick);
			} else {
				// onclick이 없는 경우 기존 방식 사용 (하위 호환성)
				var href = $(this).data('href');
				window.MenuSearch.navigateToMenu(href);
			}
			
			window.MenuSearch.hideAutocomplete();
			$('#search').val('');
		});
		
		// 검색 버튼 클릭 이벤트
		$('#search-btn').on('click', function() {
			window.MenuSearch.Search();
		});
		
		// 검색 입력창에서 Enter 키 이벤트
		$('#search').on('keydown', function(e) {
			if (e.keyCode === 13) { // Enter 키
				e.preventDefault();
				window.MenuSearch.Search();
			} else if (e.keyCode === 27) { // Escape 키
				window.MenuSearch.hideAutocomplete();
			}
		});
		
		// 다른 곳 클릭 시 자동완성 숨기기
		$(document).on('click', function(e) {
			if (!$(e.target).closest('.search-autocomplete').length) {
				window.MenuSearch.hideAutocomplete();
			}
		});

		// 대시보드 탭 초기화 (관리자만 기본 탭 설정)
		var hasDashboardPermission = '${isAdmin}' === 'true';
		window.tabManager.initDashboardTab(hasDashboardPermission);
	});
</script>

<script>
	// 공지사항 관련 초기화
	$(document).ready(function() {
		// 오늘 하루 열지 않기 체크
		var closedDate = localStorage.getItem('noticeClosedDate');
		var today = new Date().toDateString();
		if (closedDate !== today) {
			// 오늘 아직 닫지 않았으면 공지사항 로드
			window.HeaderUtils.loadNotice();
		}
		
		// 오늘 하루 열지 않기 버튼 이벤트 - 이벤트 위임 사용 (위에서 이미 처리됨)
		// 중복 제거: 위의 스크립트에서 이미 처리하므로 제거
	});
	
	// 페이지 로드 시 저장된 글꼴 복원
	$(document).ready(function() {
		var savedFont = localStorage.getItem('selectedFont');
		if (savedFont) {
			// CSS 변수만 설정하고 드롭다운 텍스트 업데이트
			document.documentElement.style.setProperty('--selected-font', savedFont);
			window.HeaderUtils.updateFontDropdownText(savedFont);
		}
	});
</script>
<body class="hold-transition skin-purple-light fixed sidebar-mini">

	<div class="wrapper">
		<header class="main-header">
			<!-- Logo -->
			<a href="/index" class="logo"> <!-- mini logo for sidebar mini 50x50 pixels --> <span class="logo-mini"> <b>D</b>eX
			</span> <!-- logo for regular state and mobile devices --> <span class="logo-lg"> <b>Data</b> Explorer
			</span>
			</a>
			<!-- Header Navbar: style can be found in header.less -->
			<nav class="navbar navbar-static-top">
			  <!-- Sidebar toggle button-->
			  <a href="#" class="sidebar-toggle" data-toggle="push-menu" role="button">
				<span class="sr-only">Toggle navigation</span>
				<span class="icon-bar"></span>
				<span class="icon-bar"></span>
				<span class="icon-bar"></span>
			  </a>
				<div class="navbar-custom-menu">
					<ul class="nav navbar-nav">
						<li>
							<a href="/Manual" target="_blank" title="도움말">
								<i class="fa fa-question-circle"></i> 도움말 
							</a>
						</li>
						<li class="dropdown">
							<a href="#" class="dropdown-toggle" data-toggle="dropdown" role="button" aria-haspopup="true" aria-expanded="false">
								<i class="fa fa-font"></i> <span class="caret"></span>
							</a>
							<ul class="dropdown-menu">
								<li><a href="javascript:changeFont('D2Coding')">D2Coding</a></li>
								<li><a href="javascript:changeFont('Courier New')">Courier New</a></li>
								<li><a href="javascript:changeFont('Noto Sans Mono')">Noto Sans Mono</a></li>
								<li><a href="javascript:changeFont('Roboto Mono')">Roboto Mono</a></li>
								<li><a href="javascript:changeFont('Source Code Pro')">Source Code Pro</a></li>
								<li><a href="javascript:changeFont('Fira Code')">Fira Code</a></li>
								<li><a href="javascript:changeFont('JetBrains Mono')">JetBrains Mono</a></li>
							</ul>
						</li>
						<li><a href="javascript:checkPWModal()"><i class="fa fa-user"></i> ${memberId}</a></li>
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
				<form class="sidebar-form" onsubmit="return false;">
					<div class="search-autocomplete">
						<div class="input-group">
							<input type="text" name="q" class="form-control" placeholder="메뉴 검색..." id="search" autocomplete="off" /> 
							<span class="input-group-btn">
								<button type="button" name='search' id='search-btn' class="btn btn-flat">
									<i class="fa fa-search"></i>
								</button>
							</span>
						</div>
						<div class="autocomplete-dropdown" id="autocompleteDropdown" style="display: none;"></div>
					</div>
				</form>
				<!-- /.search form -->
				<!-- sidebar menu: : style can be found in sidebar.less -->
				<ul class="sidebar-menu" data-widget="tree" id="tree">
					<c:if test="${isAdmin}">
						<li><a href="javascript:void(0)" onclick="addTemplateTab('systemconfig', '환경설정 관리', '/SystemConfig')"> <i class="fa fa-cog"></i> <span>환경설정 관리</span></a></li>
						<li><a href="javascript:void(0)" onclick="addTemplateTab('connection', '연결 관리', '/Connection')"> <i class="fa fa-database"></i> <span>연결 관리</span></a></li>
						<li><a href="javascript:void(0)" onclick="addTemplateTab('user', '사용자 관리', '/User')"> <i class="fa fa-user"></i> <span>사용자 관리</span></a></li>
						<li><a href="javascript:void(0)" onclick="addTemplateTab('sqltemplate', 'SQL 템플릿 관리', '/SQLTemplate')"> <i class="fa fa-code"></i> <span>SQL 템플릿 관리</span></a></li>
						<li><a href="javascript:void(0)" onclick="addTemplateTab('etl', 'ETL 관리', '/ETL')"> <i class="fa fa-exchange"></i> <span>ETL 관리</span></a></li>
					</c:if>

					<c:if test="${hasDashboardPermission}">
						<li><a href="javascript:void(0)" onclick="addTemplateTab('dashboard', '대시보드', '/Dashboard')"> <i class="fa fa-dashboard"></i> <span>대시보드</span></a></li>
					</c:if>
					<c:if test="${hasFileReadPermission}">
						<li><a href="javascript:void(0)" onclick="addTemplateTab('fileread', '파일 읽기', '/FileRead')"> <i class="fa fa-file-text-o"></i> <span>파일 읽기</span></a></li>
					</c:if>
					<c:if test="${hasFileWritePermission}">
						<li><a href="javascript:void(0)" onclick="addTemplateTab('fileupload', '파일 쓰기', '/FileUpload')"> <i class="fa fa-file-text-o"></i> <span>파일 쓰기</span></a></li>
					</c:if>

					<li id="sqltree" class="active treeview menu-open"></li>
				</ul>
			</section>
			<!-- /.sidebar -->
		</aside>
		<div class="content-wrapper" id="framebox">
			<ul id="pageTab" class="nav nav-tabs">
			</ul>
			<div id="pageTabContent" class="tab-content">
		
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
		
		

		<!-- 공지사항 모달 -->
		<div class="modal fade" id="noticeModal" tabindex="-1" role="dialog" aria-labelledby="noticeModalLabel">
			<div class="modal-dialog" role="document">
				<div class="modal-content">
					<div class="modal-header">
						<h4 class="modal-title" id="noticeModalLabel">
							<i class="fa fa-bullhorn"></i> 공지사항
						</h4>
						<button type="button" class="close" data-dismiss="modal" aria-label="Close">
							<span aria-hidden="true">&times;</span>
						</button>
					</div>
					<div class="modal-body">
						<div id="noticeContent"></div>
					</div>
					<div class="modal-footer">
						<button type="button" class="btn btn-primary" id="modal-today-close">오늘 하루 열지 않기</button>
						<button type="button" class="btn btn-default" data-dismiss="modal">닫기</button>
					</div>
				</div>
			</div>
		</div>
	</div>
</body>

		
		