/**
 * 메뉴 검색 및 자동완성 모듈
 * 사이드바 메뉴 검색 기능 제공
 */
(function(window) {
	'use strict';

	// 메뉴 데이터를 저장할 변수 (네임스페이스 내부)
	var allMenuItems = [];
	
	/**
	 * 모든 메뉴 아이템을 수집하는 함수
	 * @param {Object} permissions - 권한 정보 객체
	 *   - isAdmin: 관리자 여부
	 *   - hasDashboardPermission: 대시보드 권한 여부
	 *   - hasFileReadPermission: 파일 읽기 권한 여부
	 *   - hasFileWritePermission: 파일 쓰기 권한 여부
	 */
	function collectMenuItems(permissions) {
		allMenuItems = [];
		
		// 고정 메뉴들 추가 (사이드바 메뉴 이름과 일치하도록 수정)
		var fixedMenus = [
			{ text: '환경설정 관리', href: '/SystemConfig', icon: 'fa-cog', type: 'admin', templateId: 'systemconfig' },
			{ text: '연결 관리', href: '/Connection', icon: 'fa-database', type: 'admin', templateId: 'connection' },
			{ text: '사용자 관리', href: '/User', icon: 'fa-user', type: 'admin', templateId: 'user' },
			{ text: 'SQL 템플릿 관리', href: '/SQLTemplate', icon: 'fa-code', type: 'admin', templateId: 'sqltemplate' },
			{ text: 'ETL 관리', href: '/ETL', icon: 'fa-exchange', type: 'admin', templateId: 'etl' },
			{ text: '대시보드', href: '/Dashboard', icon: 'fa-dashboard', type: 'dashboard', templateId: 'dashboard' },
			{ text: '파일 읽기', href: '/FileRead', icon: 'fa-file-text-o', type: 'all', templateId: 'fileread' },
			{ text: '파일 쓰기', href: '/FileUpload', icon: 'fa-file-text-o', type: 'all', templateId: 'fileupload' }
		];
		
		// 권한에 따라 메뉴 필터링
		var isAdmin = permissions && permissions.isAdmin === true;
		var hasDashboardPermission = permissions && permissions.hasDashboardPermission === true;
		var hasFileReadPermission = permissions && permissions.hasFileReadPermission === true;
		var hasFileWritePermission = permissions && permissions.hasFileWritePermission === true;
		
		fixedMenus.forEach(function(menu) {
			var shouldInclude = false;
			
			// 타입별 권한 체크
			if (menu.type === 'admin') {
				shouldInclude = isAdmin;
			} else if (menu.type === 'dashboard') {
				shouldInclude = hasDashboardPermission;
			} else if (menu.type === 'all') {
				// 'all' 타입은 추가 권한 체크 필요
				if (menu.templateId === 'fileread') {
					shouldInclude = hasFileReadPermission;
				} else if (menu.templateId === 'fileupload') {
					shouldInclude = hasFileWritePermission;
				} else {
					shouldInclude = true;
				}
			}
			
			if (shouldInclude) {
				// 관리자 메뉴와 일반 메뉴 모두 onclick 문자열 생성하여 탭 추가 가능하도록 함
				var menuItem = {
					text: menu.text,
					href: menu.href,
					icon: menu.icon,
					type: menu.type,
					templateId: menu.templateId
				};
				
				// onclick 문자열 생성: addTemplateTab('templateId', 'title', 'url')
				menuItem.onclick = "addTemplateTab('" + menu.templateId + "', '" + menu.text + "', '" + menu.href + "')";
				
				allMenuItems.push(menuItem);
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
	
	/**
	 * 검색어에 따라 메뉴를 필터링하는 함수
	 * @param {string} searchTerm - 검색어
	 * @returns {Array} 필터링된 메뉴 배열
	 */
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
	
	/**
	 * 자동완성 드롭다운을 표시하는 함수
	 * @param {Array} items - 표시할 메뉴 아이템 배열
	 */
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
		dropdown[0].style.width = searchInput.outerWidth() + 40 + 'px';
		dropdown[0].style.maxHeight = '200px';
		dropdown[0].style.overflowY = 'auto';
	}
	
	/**
	 * 자동완성 드롭다운을 숨기는 함수
	 */
	function hideAutocomplete() {
		$('#autocompleteDropdown').hide();
	}
	
	/**
	 * 메뉴로 이동하는 함수 (하위 호환성)
	 * @param {string} href - 이동할 URL
	 */
	function navigateToMenu(href) {
		var iframe = document.querySelector('#pageTabContent > div:last-child > iframe');
		if (iframe && iframe.contentWindow) {
			iframe.contentWindow.location.href = href;
		}
	}
	
	/**
	 * eval() 대체: addTemplateTab 함수 호출을 안전하게 처리
	 * onclick 문자열에서 파라미터를 추출하여 함수를 직접 호출
	 * @param {string} onclickString - 실행할 onclick 문자열
	 */
	function executeTemplateTabFunction(onclickString) {
		if (!onclickString || typeof onclickString !== 'string') {
			console.error('executeTemplateTabFunction: 유효하지 않은 onclick 문자열');
			return;
		}
		
		// addTemplateTab('templateId', 'title', 'url') 형태의 문자열 파싱
		var match = onclickString.match(/addTemplateTab\s*\(\s*'([^']+)'\s*,\s*'([^']+)'\s*,\s*'([^']+)'\s*\)/);
		if (match && match.length === 4) {
			var templateId = match[1];
			var title = match[2];
			var url = match[3];
			
			// 전역 함수 addTemplateTab 직접 호출
			if (typeof window.addTemplateTab === 'function') {
				window.addTemplateTab(templateId, title, url);
			} else {
				console.error('executeTemplateTabFunction: addTemplateTab 함수를 찾을 수 없습니다');
			}
		} else {
			console.error('executeTemplateTabFunction: 올바르지 않은 함수 형식:', onclickString);
		}
	}
	
	/**
	 * 검색 함수
	 * @returns {boolean} 검색 성공 여부
	 */
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
		if (firstItem.onclick) {
			// onclick이 있으면 탭 추가 (SQL 메뉴 및 관리자 메뉴 모두)
			executeTemplateTabFunction(firstItem.onclick);
		} else {
			// onclick이 없는 경우 기존 방식 사용 (하위 호환성)
			navigateToMenu(firstItem.href);
		}
		
		hideAutocomplete();
		$('#search').val('');
		return false;
	}
	
	// 전역 네임스페이스에 등록
	window.MenuSearch = {
		collectMenuItems: collectMenuItems,
		filterMenus: filterMenus,
		showAutocomplete: showAutocomplete,
		hideAutocomplete: hideAutocomplete,
		navigateToMenu: navigateToMenu,
		executeTemplateTabFunction: executeTemplateTabFunction,
		Search: Search
	};

	// 하위 호환성을 위해 전역 함수로도 등록
	window.Search = Search;
	window.filterMenus = filterMenus;
	window.showAutocomplete = showAutocomplete;
	window.hideAutocomplete = hideAutocomplete;
	window.navigateToMenu = navigateToMenu;
	window.executeTemplateTabFunction = executeTemplateTabFunction;

})(window);

