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
/* 자동완성 드롭다운 스타일 */
.search-autocomplete {
	position: relative;
	display: table;
	width: 100%;
}

.autocomplete-dropdown {
	position: absolute;
	top: 100%;
	left: 0;
	right: 0;
	background: white;
	border: 1px solid #ddd;
	border-top: none;
	max-height: 200px;
	overflow-y: auto;
	z-index: 9999;
	box-shadow: 0 2px 5px rgba(0,0,0,0.2);
	display: none;
}

.autocomplete-item {
	padding: 8px 12px;
	cursor: pointer;
	border-bottom: 1px solid #f0f0f0;
	font-size: 13px;
}

.autocomplete-item:hover {
	background-color: #f5f5f5;
}

.autocomplete-item:last-child {
	border-bottom: none;
}

.autocomplete-item .menu-icon {
	margin-right: 8px;
	color: #666;
}

.autocomplete-item .menu-text {
	font-weight: normal;
}

.autocomplete-item .menu-path {
	font-size: 11px;
	color: #999;
	margin-left: 5px;
}
</style>

<style>
:root {
  --selected-font: D2Coding;
}

* {
  font-family: var(--selected-font);
}

/* Google Fonts fallback 설정 */
.font-noto-sans-mono {
  font-family: 'Noto Sans Mono', 'D2Coding', monospace;
}

.font-roboto-mono {
  font-family: 'Roboto Mono', 'D2Coding', monospace;
}

.font-source-code-pro {
  font-family: 'Source Code Pro', 'D2Coding', monospace;
}

.font-fira-code {
  font-family: 'Fira Code', 'D2Coding', monospace;
}

.font-jetbrains-mono {
  font-family: 'JetBrains Mono', 'D2Coding', monospace;
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
		
		$("#modal-today-close").click(function() {
			$("#NoticeModal").modal("hide");
			try {
				var today = new Date().toDateString();
				localStorage.setItem("noticeClosed", 'popupEnd');
				localStorage.setItem("noticeClosedDate", today);
			} catch (e) {
				console.error('공지사항 닫기 정보 저장 실패:', e);
			}
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
			var templateId = $(this).parents('li').children('a').attr('data-template-id');
			if (templateId) {
				// home 탭은 닫을 수 없음
				if (templateId === 'home') {
					return false;
				}
				// 새로운 탭 관리 시스템 사용
				tabManager.removeTab(templateId);
			} else {
				// 기존 방식 (기본 탭들)
				var tabId = $(this).parents('li').children('a').attr('href');
				$(this).parents('li').remove('li');
				$(tabId).remove();
				$('#pageTab a:first').tab('show');
			}
		});

		/**
		 * Click Tab to show its content 
		 */
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
				collectMenuItems(); // 메뉴 아이템 수집
				var filtered = filterMenus(searchTerm);
				showAutocomplete(filtered);
			} else {
				hideAutocomplete();
			}
		});
		
		// 자동완성 아이템 클릭 이벤트
		$(document).on('click', '.autocomplete-item', function() {
			var menuType = $(this).data('menu-type');
			var onclick = $(this).data('onclick');
			
			if (menuType === 'sql' && onclick) {
				// SQL 메뉴의 경우 onclick 함수 직접 실행
				eval(onclick);
			} else {
				// 일반 메뉴의 경우 기존 방식 사용
				var href = $(this).data('href');
				navigateToMenu(href);
			}
			
			hideAutocomplete();
			$('#search').val('');
		});
		
		// 검색 버튼 클릭 이벤트
		$('#search-btn').on('click', function() {
			Search();
		});
		
		// 검색 입력창에서 Enter 키 이벤트
		$('#search').on('keydown', function(e) {
			if (e.keyCode === 13) { // Enter 키
				e.preventDefault();
				Search();
			} else if (e.keyCode === 27) { // Escape 키
				hideAutocomplete();
			}
		});
		
		// 다른 곳 클릭 시 자동완성 숨기기
		$(document).on('click', function(e) {
			if (!$(e.target).closest('.search-autocomplete').length) {
				hideAutocomplete();
			}
		});

		// 대시보드 탭 초기화
		tabManager.initDashboardTab();

	});

	// 메뉴 데이터를 저장할 변수
	var allMenuItems = [];
	
	// 모든 메뉴 아이템을 수집하는 함수
	function collectMenuItems() {
		allMenuItems = [];
		
		// 고정 메뉴들 추가
		var fixedMenus = [
			{ text: 'Connection', href: '/Connection', icon: 'fa-database', type: 'admin' },
			{ text: 'User', href: '/User', icon: 'fa-user', type: 'admin' },
			{ text: 'SQL 템플릿 관리', href: '/SQLTemplate', icon: 'fa-code', type: 'admin' },
			{ text: '환경설정 관리', href: '/SystemConfig', icon: 'fa-cog', type: 'admin' },
			{ text: 'FileRead', href: '/FileRead', icon: 'fa-file-text-o', type: 'all' },
			{ text: 'FileUpload', href: '/FileUpload', icon: 'fa-file-text-o', type: 'all' }
		];
		
		// 관리자 메뉴만 필터링
		var isAdmin = '${isAdmin}' === 'true';
		fixedMenus.forEach(function(menu) {
			if (menu.type === 'all' || (menu.type === 'admin' && isAdmin)) {
				allMenuItems.push(menu);
			}
		});
		
		// 동적으로 생성된 SQL 메뉴들 추가
		$('#sidemenu a').each(function() {
			var $this = $(this);
			var text = $this.text().trim();
			var onclick = $this.attr('onclick');
			
			if (text && onclick) {
				// onclick에서 templateId와 href 추출
				var templateIdMatch = onclick.match(/addTemplateTab\('([^']+)'/);
				var hrefMatch = onclick.match(/addTemplateTab\('[^']+',\s*'[^']+',\s*'([^']+)'/);
				
				if (templateIdMatch && hrefMatch) {
					var templateId = templateIdMatch[1];
					var href = hrefMatch[1];
					
					allMenuItems.push({
						text: text,
						href: href,
						icon: 'fa-code',
						type: 'sql',
						templateId: templateId,
						onclick: onclick
					});
				}
			}
		});
	}
	
	// 검색어에 따라 메뉴를 필터링하는 함수
	function filterMenus(searchTerm) {
		if (!searchTerm || searchTerm.length < 1) {
			return [];
		}
		
		var filtered = allMenuItems.filter(function(menu) {
			return menu.text.toLowerCase().includes(searchTerm.toLowerCase());
		});
		
		// 최대 10개까지만 표시
		return filtered.slice(0, 10);
	}
	
	// 자동완성 드롭다운을 표시하는 함수
	function showAutocomplete(items) {
		var dropdown = $('#autocompleteDropdown');
		dropdown.empty();
		
		if (items.length === 0) {
			dropdown.hide();
			return;
		}
		
		items.forEach(function(item) {
			var itemHtml = '<div class="autocomplete-item" ' +
				'data-href="' + item.href + '" ' +
				'data-menu-type="' + item.type + '" ' +
				'data-template-id="' + (item.templateId || '') + '" ' +
				'data-onclick="' + (item.onclick || '') + '">' +
				'<i class="fa ' + item.icon + ' menu-icon"></i>' +
				'<span class="menu-text">' + item.text + '</span>' +
				'</div>';
			dropdown.append(itemHtml);
		});
		
		// 검색 입력창의 위치를 기준으로 드롭다운 위치 설정
		var searchInput = $('#search');
		var inputOffset = searchInput.offset();
		var inputHeight = searchInput.outerHeight();
		
		// 스타일을 직접 설정
		dropdown[0].style.display = 'block';
		dropdown[0].style.position = 'fixed';
		dropdown[0].style.zIndex = '9999';
		dropdown[0].style.background = 'white';
		dropdown[0].style.border = '1px solid #ddd';
		dropdown[0].style.borderTop = 'none';
		dropdown[0].style.boxShadow = '0 2px 5px rgba(0,0,0,0.2)';
		dropdown[0].style.top = (inputOffset.top + inputHeight) + 'px';
		dropdown[0].style.left = inputOffset.left + 'px';
		dropdown[0].style.width = searchInput.outerWidth()+40 + 'px';
		dropdown[0].style.maxHeight = '200px';
		dropdown[0].style.overflowY = 'auto';
	}
	
	// 자동완성 드롭다운을 숨기는 함수
	function hideAutocomplete() {
		$('#autocompleteDropdown').hide();
	}
	
	// 메뉴로 이동하는 함수
	function navigateToMenu(href) {
		const iframe = document.querySelector('#pageTabContent > div:last-child > iframe');
		if (iframe && iframe.contentWindow) {
			iframe.contentWindow.location.href = href;
		}
	}
	
	// 기존 Search 함수 (호환성을 위해 유지)
	function Search() {
		var searchTerm = $("#search").val().trim();
		if (!searchTerm) {
			return false;
		}
		
		var filtered = filterMenus(searchTerm);
		if (filtered.length === 0) {
			alert("검색 결과가 없습니다.");
			return false;
		}
		
		// 첫 번째 결과로 이동
		var firstItem = filtered[0];
		if (firstItem.type === 'sql' && firstItem.onclick) {
			// SQL 메뉴의 경우 onclick 함수 직접 실행
			eval(firstItem.onclick);
		} else {
			// 일반 메뉴의 경우 기존 방식 사용
			navigateToMenu(firstItem.href);
		}
		
		hideAutocomplete();
		$('#search').val('');
		return false;
	}

	var pageImages = [];
	var pageNum = 1;
	
	// 새로운 탭 관리 시스템 - 전역 객체로 등록
	window.tabManager = {
		tabs: new Map(), // 탭 정보 저장
		activeTab: null,
		tabOrder: ['home'], // 탭 순서 추적
		
		// 탭 추가
		addTab: function(templateId, title, url) {
			
			// 중복 탭 체크
			if (!this.tabs.has(templateId)) {
				// 새 탭 생성
				var tabId = 'tab_' + templateId;
				var iframeId = 'iframe_' + templateId;
				
				// 탭 정보 저장
				this.tabs.set(templateId, {
					id: tabId,
					iframeId: iframeId,
					title: title,
					url: url
				});
				
				// 탭 순서에 추가
				this.tabOrder.push(templateId);
				
				// 탭 HTML 생성
				var closeButton = '';
				if (templateId !== 'home') {
					closeButton = '<button class="close" type="button" title="Remove this page" style="padding-left:4px; display:inline-flex; align-items:center; justify-content:center; height:18px;"><i class="fa fa-close" style="font-size:16px;"></i></button>';
				}
				var tabHtml = '<li><a href="#' + tabId + '" data-toggle="tab" data-template-id="' + templateId + '">' + 
					title + closeButton + '</a></li>';
				
				var contentHtml = '<div class="tab-pane" id="' + tabId + '">' +
					'<iframe name="' + iframeId + '" id="' + iframeId + '" class="tab_frame" ' +
					'style="margin: 0; width: 100%; height: calc(100vh - 101px); border: none; overflow: auto;" ' +
					'src="' + url + '"></iframe></div>';
				
				// DOM에 추가
				$('#pageTab').append(tabHtml);
				$('#pageTabContent').append(contentHtml);
			}else{
				var tabInfo = this.tabs.get(templateId);
			    if (tabInfo) {
			        // iframe URL 업데이트
			        var iframe = document.getElementById(tabInfo.iframeId);
			        if (iframe) {
			            iframe.src = url;
			        }
			    }
			}
			
			// 탭 활성화
			this.activateTab(templateId);
			
			// 사이드바 링크 타겟 업데이트
			this.updateSidebarTargets(iframeId);
		},
		
		// 탭 활성화
		activateTab: function(templateId) {
			var tabInfo = this.tabs.get(templateId);
			if (tabInfo) {
				$('#pageTab a[data-template-id="' + templateId + '"]').tab('show');
				this.activeTab = templateId;
			}
		},
		
		// 탭 제거
		removeTab: function(templateId) {
			// home 탭은 제거할 수 없음
			if (templateId === 'home') {
				console.log('home 탭은 제거할 수 없습니다.');
				return false;
			}
			
			var tabInfo = this.tabs.get(templateId);
			if (tabInfo) {
				$('#pageTab a[data-template-id="' + templateId + '"]').parent().remove();
				$('#' + tabInfo.id).remove();
				this.tabs.delete(templateId);
				
				// 탭 순서에서 제거
				var index = this.tabOrder.indexOf(templateId);
				if (index > -1) {
					this.tabOrder.splice(index, 1);
				}
				
				// 활성 탭이 제거된 경우 다음 탭 활성화
				if (this.activeTab === templateId) {
					this.activateNextTab();
				}
			}
		},
		
		// 다음 탭 활성화 (LIFO 방식)
		activateNextTab: function() {
			// 현재 활성화된 탭의 인덱스 찾기
			var currentIndex = this.tabOrder.indexOf(this.activeTab);
			var nextTab = null;
			
			// LIFO 방식: 이전 탭(왼쪽) 우선, 없으면 다음 탭(오른쪽)
			if (currentIndex === -1) {
				// 현재 탭이 순서에 없는 경우 마지막 탭 (가장 최근)
				nextTab = this.tabOrder[this.tabOrder.length - 1];
			} else if (currentIndex > 0) {
				// 이전 탭이 있는 경우 (왼쪽 탭)
				nextTab = this.tabOrder[currentIndex - 1];
			} else if (currentIndex < this.tabOrder.length - 1) {
				// 이전 탭이 없고 다음 탭이 있는 경우 (오른쪽 탭)
				nextTab = this.tabOrder[currentIndex + 1];
			}
			
			// 다음 탭이 있으면 활성화
			if (nextTab && this.tabs.has(nextTab)) {
				this.activateTab(nextTab);
			}
		},
		
		// 사이드바 링크 타겟 업데이트
		updateSidebarTargets: function(iframeId) {
			$('.sidebar-menu a:not(\'.addtree\')').attr("target", iframeId);
			$('#iframe_1').contents().find('#menus a').attr("target", iframeId);
			$('#iframe_1').contents().find('#goToTemplateLink').attr("target", iframeId);
		},
		
		// 탭을 마지막 순서로 이동
		moveTabToLast: function(templateId) {
			if (this.tabs.has(templateId)) {
				// 현재 순서에서 해당 탭 제거
				var index = this.tabOrder.indexOf(templateId);
				if (index > -1) {
					this.tabOrder.splice(index, 1);
				}
				// 마지막에 추가
				this.tabOrder.push(templateId);
				console.log('탭 순서 변경:', templateId, '-> 마지막 순서');
			}
		},
		
		// 대시보드 탭 초기화
		initDashboardTab: function() {
			addTemplateTab('home', '<i class="fa fa-home"></i>', '/index2');
			
			// 대시보드 메뉴 접근 권한 확인
			var hasDashboardPermission = '${hasDashboardPermission}' === 'true';
			if (hasDashboardPermission) {
				addTemplateTab('dashboard', '대시보드', '/Dashboard');
			}
		}
	};

	// 새로운 탭 추가 함수 (템플릿용) - 전역 함수로 등록
	window.addTemplateTab = function(templateId, title, url) {
		console.log('addTemplateTab 호출:', templateId, title, url);
		tabManager.addTab(templateId, title, url);
	};

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

<script>
		// 공지사항 관련 함수들
		function loadNotice() {
			$.ajax({
				url: '/SystemConfig/getNotice',
				type: 'GET',
				success: function(response) {
					if (response.success && response.noticeEnabled === 'true' && response.noticeContent) {
						showNotice(response.noticeContent);
					}
				},
				error: function() {
					// 에러 시 아무것도 하지 않음
				}
			});
		}
		
		function showNotice(content) {
			$('#noticeContent').html(content.replace(/\n/g, '<br>'));
			$('#noticeModal').modal('show');
		}
		
		// 페이지 로드 시 공지사항 확인 및 이벤트 설정
		$(document).ready(function() {
			// 오늘 하루 열지 않기 체크
			var closedDate = localStorage.getItem('noticeClosedDate');
			var today = new Date().toDateString();
			if (closedDate !== today) {
				// 오늘 아직 닫지 않았으면 공지사항 로드
				loadNotice();
			}
			
			// 오늘 하루 열지 않기 버튼 이벤트
			$('#modal-today-close').click(function() {
				// 오늘 날짜를 localStorage에 저장
				var today = new Date().toDateString();
				localStorage.setItem('noticeClosedDate', today);
				$('#noticeModal').modal('hide');
			});
		});
		
		// 글꼴 변경 함수
		function changeFont(fontFamily) {
			// 로컬 스토리지에 선택한 글꼴 저장
			localStorage.setItem('selectedFont', fontFamily);
			
			// 현재 페이지의 모든 요소에 글꼴 적용
			document.documentElement.style.setProperty('--selected-font', fontFamily);
			
			// 드롭다운 버튼 텍스트 업데이트
			updateFontDropdownText(fontFamily);
			
			// 현재 페이지의 Ace Editor들에 폰트 적용
			if (typeof ace !== 'undefined') {
				// 전역 에디터들에 폰트 적용
				if (window.sqlEditors) {
					Object.values(window.sqlEditors).forEach(function(editor) {
						if (editor && typeof editor.setOptions === 'function') {
							editor.setOptions({
								fontFamily: fontFamily
							});
						}
					});
				}
				
				// SqlTemplateState 에디터들에 폰트 적용
				if (window.SqlTemplateState && window.SqlTemplateState.sqlEditors) {
					Object.values(window.SqlTemplateState.sqlEditors).forEach(function(editor) {
						if (editor && typeof editor.setOptions === 'function') {
							editor.setOptions({
								fontFamily: fontFamily
							});
						}
					});
				}
			}
			
			// 모든 iframe에도 글꼴 적용
			var iframes = document.querySelectorAll('iframe');
			iframes.forEach(function(iframe) {
				try {
					if (iframe.contentDocument) {
						iframe.contentDocument.documentElement.style.setProperty('--selected-font', fontFamily);
						
						// iframe 내부의 에디터들에 직접 폰트 적용
						if (iframe.contentWindow) {
							// FileUpload.jsp의 에디터들
							if (iframe.contentWindow.contentEditor && typeof iframe.contentWindow.contentEditor.setOptions === 'function') {
								iframe.contentWindow.contentEditor.setOptions({
									fontFamily: fontFamily
								});
							}
							$('#contentTextarea', iframe.contentDocument).css('font-family', fontFamily);
							
							// FileRead.jsp의 에디터들
							if (iframe.contentWindow.resultEditor && typeof iframe.contentWindow.resultEditor.setOptions === 'function') {
								iframe.contentWindow.resultEditor.setOptions({
									fontFamily: fontFamily
								});
							}
							$('#resultTextarea', iframe.contentDocument).css('font-family', fontFamily);
							
							// SQLExecute.jsp의 textarea들
							$('.formtextarea', iframe.contentDocument).css('font-family', fontFamily);
							
							// SQLTemplate.jsp의 에디터들
							if (iframe.contentWindow.sqlEditors) {
								Object.values(iframe.contentWindow.sqlEditors).forEach(function(editor) {
									if (editor && typeof editor.setOptions === 'function') {
										editor.setOptions({
											fontFamily: fontFamily
										});
									}
								});
							}
							if (iframe.contentWindow.SqlTemplateState && iframe.contentWindow.SqlTemplateState.sqlEditors) {
								Object.values(iframe.contentWindow.SqlTemplateState.sqlEditors).forEach(function(editor) {
									if (editor && typeof editor.setOptions === 'function') {
										editor.setOptions({
											fontFamily: fontFamily
										});
									}
								});
							}
						}
					}
				} catch (e) {
					// Cross-origin 에러 무시
				}
			});
			
			// 드롭다운 메뉴 닫기 (안전하게 처리)
			try {
				// 부모 창의 dropdown이 있는 경우 (iframe 내부에서 호출되는 경우)
				if (window.parent && window.parent !== window) {
					var parentDropdown = window.parent.$('.dropdown-toggle');
					if (parentDropdown.length > 0 && typeof parentDropdown.dropdown === 'function') {
						parentDropdown.dropdown('hide');
					}
				} else {
					// 현재 창의 dropdown
					var dropdownToggle = $('.dropdown-toggle');
					if (dropdownToggle.length > 0 && typeof dropdownToggle.dropdown === 'function') {
						dropdownToggle.dropdown('hide');
					}
				}
			} catch (e) {
				// 드롭다운 닫기 실패 시 무시 (iframe 내부에서 호출되는 경우 등)
				console.debug('드롭다운 닫기 실패:', e);
			}
		}
		
		// 드롭다운 버튼 텍스트 업데이트 함수
		function updateFontDropdownText(fontFamily) {
			try {
				// 부모 창의 dropdown이 있는 경우 (iframe 내부에서 호출되는 경우)
				var dropdownToggle;
				if (window.parent && window.parent !== window) {
					dropdownToggle = window.parent.$('.dropdown-toggle');
				} else {
					dropdownToggle = $('.dropdown-toggle');
				}
				
				if (dropdownToggle.length > 0) {
					// 기존 아이콘과 caret은 유지하고 텍스트만 변경
					var newText = '<i class="fa fa-font"></i> ' + fontFamily + ' <span class="caret"></span>';
					dropdownToggle.html(newText);
				}
			} catch (e) {
				// 드롭다운 텍스트 업데이트 실패 시 무시
				console.debug('드롭다운 텍스트 업데이트 실패:', e);
			}
		}
		
		// 페이지 로드 시 저장된 글꼴 복원
		$(document).ready(function() {
			var savedFont = localStorage.getItem('selectedFont');
			if (savedFont) {
				// CSS 변수만 설정하고 드롭다운 텍스트 업데이트
				document.documentElement.style.setProperty('--selected-font', savedFont);
				updateFontDropdownText(savedFont);
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
				<div class="tab-pane active" id="page1">
					<iframe name="iframe_home" id="iframe_home" class="tab_frame" style="margin: 0; width: 100%; height: calc(100vh - 101px); border: none; overflow: auto;" src="/index2"></iframe>
				</div>
				<div class="tab-pane" id="newpage">
					<iframe name="iframe" id="iframe" class="tab_frame" style="margin: 0; width: 100%; height: calc(100vh - 101px); border: none; overflow: auto;"></iframe>
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

		
		