<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@include file="common/common.jsp"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>DeX 사용자 매뉴얼</title>
<style>
	.manual-container {
		display: flex;
		height: 100vh;
		overflow: hidden;
	}
	
	.manual-sidebar {
		width: 280px;
		background-color: #f8f9fa;
		border-right: 1px solid #dee2e6;
		overflow-y: auto;
		padding: 20px;
	}
	
	.manual-sidebar h3 {
		margin-top: 0;
		margin-bottom: 20px;
		color: #2c3e50;
		font-size: 1.3em;
		border-bottom: 2px solid #3498db;
		padding-bottom: 10px;
	}
	
	.manual-menu {
		list-style: none;
		padding: 0;
		margin: 0;
	}
	
	.manual-menu li {
		margin-bottom: 5px;
	}
	
	.manual-menu a {
		display: block;
		padding: 12px 15px;
		color: #495057;
		text-decoration: none;
		border-radius: 4px;
		transition: all 0.2s;
	}
	
	.manual-menu a:hover {
		background-color: #e9ecef;
		color: #2c3e50;
	}
	
	.manual-menu a.active {
		background-color: #3498db;
		color: white;
		font-weight: bold;
	}
	
	.manual-submenu {
		list-style: none;
		padding: 0;
		margin: 0;
		margin-left: 20px;
		margin-top: 5px;
		display: none;
	}
	
	.manual-submenu.active {
		display: block;
	}
	
	.manual-submenu li {
		margin-bottom: 3px;
	}
	
	.manual-submenu a {
		display: block;
		padding: 8px 15px;
		color: #6c757d;
		text-decoration: none;
		border-radius: 4px;
		font-size: 0.9em;
		transition: all 0.2s;
	}
	
	.manual-submenu a:hover {
		background-color: #e9ecef;
		color: #2c3e50;
	}
	
	.manual-submenu a.active {
		background-color: #5dade2;
		color: white;
		font-weight: bold;
	}
	
	.manual-menu-item {
		position: relative;
	}
	
	.manual-menu-item.has-submenu > a::after {
		content: ' ▼';
		font-size: 0.8em;
		float: right;
	}
	
	.manual-menu-item.has-submenu.active > a::after {
		content: ' ▲';
	}
	
	.manual-content {
		flex: 1;
		overflow: hidden;
		position: relative;
	}
	
	.manual-iframe {
		width: 100%;
		height: 100%;
		border: none;
		display: block;
	}
	
	.manual-welcome {
		display: flex;
		align-items: center;
		justify-content: center;
		height: 100%;
		background-color: #f8f9fa;
		color: #6c757d;
		font-size: 1.2em;
		text-align: center;
		padding: 40px;
	}
	
	.manual-welcome-content {
		max-width: 600px;
	}
	
	.manual-welcome h2 {
		color: #2c3e50;
		margin-bottom: 20px;
	}
	
	.manual-welcome p {
		line-height: 1.8;
		margin-bottom: 15px;
	}
</style>
</head>
<body>
	<div class="manual-container">
		<div class="manual-sidebar">
			<h3><i class="fa fa-book"></i> 사용자 매뉴얼</h3>
			<ul class="manual-menu">
				<li class="manual-menu-item has-submenu active">
					<a href="javascript:void(0)" onclick="loadManual('01_환경설정.html', this, null)">1. 환경설정</a>
					<ul class="manual-submenu active">
						<li><a href="javascript:void(0)" onclick="loadManual('01_환경설정.html', this, '1.1')">1.1 화면 구성</a></li>
						<li><a href="javascript:void(0)" onclick="loadManual('01_환경설정.html', this, '1.2')">1.2 기능 설명</a></li>
					</ul>
				</li>
				<li class="manual-menu-item has-submenu">
					<a href="javascript:void(0)" onclick="loadManual('02_연결관리.html', this, null)">2. 연결관리</a>
					<ul class="manual-submenu">
						<li><a href="javascript:void(0)" onclick="loadManual('02_연결관리.html', this, '2.1')">2.1 화면 구성</a></li>
						<li><a href="javascript:void(0)" onclick="loadManual('02_연결관리.html', this, '2.2')">2.2 기능 설명</a></li>
						<li><a href="javascript:void(0)" onclick="loadManual('02_연결관리.html', this, '2.3')">2.3 모달</a></li>
					</ul>
				</li>
				<li class="manual-menu-item has-submenu">
					<a href="javascript:void(0)" onclick="loadManual('03_사용자관리.html', this, null)">3. 사용자관리</a>
					<ul class="manual-submenu">
						<li><a href="javascript:void(0)" onclick="loadManual('03_사용자관리.html', this, '3.1')">3.1 화면 구성</a></li>
						<li><a href="javascript:void(0)" onclick="loadManual('03_사용자관리.html', this, '3.2')">3.2 기능 설명</a></li>
						<li><a href="javascript:void(0)" onclick="loadManual('03_사용자관리.html', this, '3.3')">3.3 모달</a></li>
					</ul>
				</li>
				<li class="manual-menu-item has-submenu">
					<a href="javascript:void(0)" onclick="loadManual('04_템플릿관리.html', this, null)">4. 템플릿 관리</a>
					<ul class="manual-submenu">
						<li><a href="javascript:void(0)" onclick="loadManual('04_템플릿관리.html', this, '4.1')">4.1 화면 구성</a></li>
						<li><a href="javascript:void(0)" onclick="loadManual('04_템플릿관리.html', this, '4.2')">4.2 기능 설명</a></li>
						<li><a href="javascript:void(0)" onclick="loadManual('04_템플릿관리.html', this, '4.3')">4.3 모달</a></li>
					</ul>
				</li>
				<li class="manual-menu-item has-submenu">
					<a href="javascript:void(0)" onclick="loadManual('05_대시보드.html', this, null)">5. 대시보드</a>
					<ul class="manual-submenu">
						<li><a href="javascript:void(0)" onclick="loadManual('05_대시보드.html', this, '5.1')">5.1 화면 구성</a></li>
						<li><a href="javascript:void(0)" onclick="loadManual('05_대시보드.html', this, '5.2')">5.2 기능 설명</a></li>
					</ul>
				</li>
				<li class="manual-menu-item has-submenu">
					<a href="javascript:void(0)" onclick="loadManual('06_파일읽기쓰기.html', this, null)">6. 파일읽기/쓰기</a>
					<ul class="manual-submenu">
						<li><a href="javascript:void(0)" onclick="loadManual('06_파일읽기쓰기.html', this, '6.1')">6.1 파일 읽기 화면</a></li>
						<li><a href="javascript:void(0)" onclick="loadManual('06_파일읽기쓰기.html', this, '6.2')">6.2 파일 쓰기 화면</a></li>
					</ul>
				</li>
				<li class="manual-menu-item has-submenu">
					<a href="javascript:void(0)" onclick="loadManual('07_SQL실행화면.html', this, null)">7. SQL 실행화면</a>
					<ul class="manual-submenu">
						<li><a href="javascript:void(0)" onclick="loadManual('07_SQL실행화면.html', this, '7.1')">7.1 화면 구성</a></li>
						<li><a href="javascript:void(0)" onclick="loadManual('07_SQL실행화면.html', this, '7.2')">7.2 기능 설명</a></li>
					</ul>
				</li>
			</ul>
		</div>
		<div class="manual-content">
			<div class="manual-welcome" id="welcomeMessage">
				<div class="manual-welcome-content">
					<h2><i class="fa fa-book"></i> DeX 사용자 매뉴얼</h2>
					<p>좌측 메뉴에서 원하는 항목을 선택하시면 해당 매뉴얼을 확인하실 수 있습니다.</p>
				</div>
			</div>
			<iframe id="manualFrame" class="manual-iframe" style="display: none;"></iframe>
		</div>
	</div>
	
	<script>
		function loadManual(filename, element, sectionId) {
			// 메인 메뉴 항목 클릭인지 하위 메뉴 클릭인지 확인
			var isSubmenu = $(element).closest('.manual-submenu').length > 0;
			
			if (isSubmenu) {
				// 하위 메뉴 클릭 시
				// 모든 하위 메뉴 항목에서 active 클래스 제거
				$('.manual-submenu a').removeClass('active');
				// 클릭한 하위 메뉴 항목에 active 클래스 추가
				$(element).addClass('active');
				
				// 해당 메인 메뉴 항목도 active 처리
				var parentItem = $(element).closest('.manual-menu-item');
				$('.manual-menu-item').removeClass('active');
				parentItem.addClass('active');
				$('.manual-menu-item > a').removeClass('active');
				parentItem.find('> a').addClass('active');
				
				// 모든 하위 메뉴 닫기
				$('.manual-submenu').removeClass('active');
				$('.manual-menu-item').removeClass('active');
				// 해당 하위 메뉴만 열기
				parentItem.find('.manual-submenu').addClass('active');
				parentItem.addClass('active');
			} else {
				// 메인 메뉴 클릭 시
				// 모든 메뉴 항목에서 active 클래스 제거
				$('.manual-menu a').removeClass('active');
				$('.manual-menu-item').removeClass('active');
				
				// 클릭한 메뉴 항목에 active 클래스 추가
				$(element).addClass('active');
				var menuItem = $(element).closest('.manual-menu-item');
				menuItem.addClass('active');
				
				// 모든 하위 메뉴 닫기
				$('.manual-submenu').removeClass('active');
				// 해당 하위 메뉴만 열기
				menuItem.find('.manual-submenu').addClass('active');
			}
			
			// iframe에 매뉴얼 로드
			var iframe = document.getElementById('manualFrame');
			var welcomeMessage = document.getElementById('welcomeMessage');
			
			// 매뉴얼 파일 경로 설정 (resources/manual 폴더 기준)
			var manualPath = '/resources/manual/' + filename;
			
			// 섹션 ID가 있으면 앵커 추가
			if (sectionId) {
				manualPath += '#' + sectionId;
			}
			
			iframe.src = manualPath;
			iframe.style.display = 'block';
			welcomeMessage.style.display = 'none';
			
			// iframe 로드 완료 후 스크롤 처리
			$(iframe).off('load').on('load', function() {
				if (sectionId) {
					try {
						var iframeDoc = iframe.contentDocument || iframe.contentWindow.document;
						var targetElement = iframeDoc.getElementById(sectionId);
						if (targetElement) {
							targetElement.scrollIntoView({ behavior: 'smooth', block: 'start' });
						}
					} catch (e) {
						// 크로스 오리진 문제로 인한 에러 무시
					}
				}
			});
		}
	</script>
</body>
</html>

