/**
 * 탭 관리 모듈
 * 다중 탭 시스템을 관리하는 기능 제공
 */
(function(window) {
	'use strict';

	// 탭 관리 시스템 - 전역 객체로 등록
	window.tabManager = {
		tabs: new Map(), // 탭 정보 저장
		activeTab: null,
		tabOrder: ['home'], // 탭 순서 추적
		
		// 탭 추가
		addTab: function(templateId, title, url, data) {
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
				
				// data가 있으면 POST form 생성, 없으면 GET 방식으로 iframe src 설정
				var contentHtml = '';
				if (data && Object.keys(data).length > 0) {
					// POST 방식: form 생성
					var formId = 'form_' + templateId;
					contentHtml = '<div class="tab-pane" id="' + tabId + '">' +
						'<form id="' + formId + '" name="' + formId + '" method="POST" action="' + url + '" target="' + iframeId + '" style="display:none;">' +
						'</form>' +
						'<iframe name="' + iframeId + '" id="' + iframeId + '" class="tab_frame" ' +
						'style="margin: 0; width: 100%; height: calc(100vh - 101px); border: none; overflow: auto;"></iframe></div>';
				} else {
					// GET 방식: 기존 방식
					contentHtml = '<div class="tab-pane" id="' + tabId + '">' +
						'<iframe name="' + iframeId + '" id="' + iframeId + '" class="tab_frame" ' +
						'style="margin: 0; width: 100%; height: calc(100vh - 101px); border: none; overflow: auto;" ' +
						'src="' + url + '"></iframe></div>';
				}
				
				// DOM에 추가
				$('#pageTab').append(tabHtml);
				$('#pageTabContent').append(contentHtml);
				
				// data가 있으면 form에 데이터 추가하고 submit
				if (data && Object.keys(data).length > 0) {
					var form = document.getElementById(formId);
					if (form) {
						// form에 데이터 추가
						for (var key in data) {
							if (data.hasOwnProperty(key)) {
								var input = document.createElement('input');
								input.type = 'hidden';
								input.name = key;
								input.value = data[key];
								form.appendChild(input);
							}
						}
						// form submit
						form.submit();
					}
				}
			} else {
				var tabInfo = this.tabs.get(templateId);
				if (tabInfo) {
					// iframe URL 업데이트
					var iframe = document.getElementById(tabInfo.iframeId);
					if (iframe) {
						if (data && Object.keys(data).length > 0) {
							// POST 방식: form 다시 submit
							var formId = 'form_' + templateId;
							var form = document.getElementById(formId);
							if (form) {
								// 기존 input 제거
								while (form.firstChild) {
									form.removeChild(form.firstChild);
								}
								// 새로운 데이터 추가
								for (var key in data) {
									if (data.hasOwnProperty(key)) {
										var input = document.createElement('input');
										input.type = 'hidden';
										input.name = key;
										input.value = data[key];
										form.appendChild(input);
									}
								}
								form.submit();
							}
						} else {
							// GET 방식: iframe src 업데이트
							iframe.src = url;
						}
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
			}
		},
		
		// 대시보드 탭 초기화 (권한 정보를 파라미터로 받음)
		initDashboardTab: function(hasDashboardPermission) {
			if (typeof window.addTemplateTab === 'function') {
				window.addTemplateTab('home', '<i class="fa fa-home"></i>', '/index2');
				
				// 대시보드 메뉴 접근 권한 확인
				if (hasDashboardPermission) {
					window.addTemplateTab('dashboard', '대시보드', '/Dashboard');
				}
			} else {
				console.error('addTemplateTab 함수를 찾을 수 없습니다. tabManager.js가 로드되었는지 확인하세요.');
			}
		}
	};

	// 새로운 탭 추가 함수 (템플릿용) - 전역 함수로 등록
	window.addTemplateTab = function(templateId, title, url) {
		window.tabManager.addTab(templateId, title, url);
	};

})(window);

