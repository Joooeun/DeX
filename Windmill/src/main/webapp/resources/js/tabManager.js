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
		previousActiveTab: null, // 이전 활성 탭
		tabOrder: ['home'], // 탭 순서 추적
		
		// templateId를 문자열로 정규화하는 헬퍼 함수
		normalizeTemplateId: function(templateId) {
			return templateId != null ? String(templateId) : templateId;
		},
		
		// 탭 추가
		addTab: function(templateId, title, url, data, forceReload) {

			// templateId를 문자열로 정규화
			templateId = this.normalizeTemplateId(templateId);
			
			// forceReload 기본값 처리
			if (forceReload === undefined) {
				forceReload = false;
			}
			
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
				
				// iframe 로드 완료 시 현재 활성 탭이면 메시지 전송
				// (activateTab에서 보낸 메시지는 iframe이 로드되기 전일 수 있으므로 재전송)
				var iframe = document.getElementById(iframeId);
				if (iframe) {
					iframe.addEventListener('load', function() {
						// iframe이 로드된 후 약간의 지연을 두고 메시지 전송 (내부 스크립트 로드 대기)
						setTimeout(function() {
							if (this.activeTab === templateId) {
								this.notifyTabActivation(templateId);
							}
						}.bind(this), 100);
					}.bind(this));
				}
				
				// data가 있으면 form에 데이터 추가하고 submit
				if (data && Object.keys(data).length > 0) {
					var form = document.getElementById(formId);
					if (form) {
						// form에 데이터 추가
						for (var key in data) {
							if (data.hasOwnProperty(key)) {
								var value = data[key];
				
								if (Array.isArray(value)) {
									for (var i = 0; i < value.length; i++) {
										var input = document.createElement('input');
										input.type = 'hidden';
										input.name = key + '[]';
										input.value = value[i];
										form.appendChild(input);
									}
								} else {
									var input = document.createElement('input');
									input.type = 'hidden';
									input.name = key;
									input.value = value;
									form.appendChild(input);
								}
							}
						}
						// form submit
						form.submit();
					}
				}
			} else {
				// 이미 열린 탭 업데이트
				var tabInfo = this.tabs.get(templateId);
				if (tabInfo) {
					var iframe = document.getElementById(tabInfo.iframeId);
					var formId = 'form_' + templateId;
					var form = document.getElementById(formId);
					
					// URL이 변경되었거나 POST 데이터가 있거나 forceReload가 true인 경우 새로 로드 필요
					var needsReload = (tabInfo.url !== url) || (data && Object.keys(data).length > 0) || forceReload;
					
					if (needsReload) {
						// 탭 정보 업데이트
						tabInfo.url = url;
						
						if (data && Object.keys(data).length > 0) {
							// POST 방식
							if (!form) {
								// form이 없으면 새로 생성
								var tabPane = document.getElementById(tabInfo.id);
								if (tabPane && iframe) {
									form = document.createElement('form');
									form.id = formId;
									form.name = formId;
									form.method = 'POST';
									form.action = url;
									form.target = tabInfo.iframeId;
									form.style.display = 'none';
									tabPane.insertBefore(form, iframe);
								}
							}
							
							if (form) {
								// form action URL 업데이트
								form.action = url;
								
								// 기존 input 제거
								while (form.firstChild) {
									form.removeChild(form.firstChild);
								}
								
								// 새로운 데이터 추가
								for (var key in data) {
									if (data.hasOwnProperty(key)) {
										var value = data[key];
						
										if (Array.isArray(value)) {
											for (var i = 0; i < value.length; i++) {
												var input = document.createElement('input');
												input.type = 'hidden';
												input.name = key + '[]';
												input.value = value[i];
												form.appendChild(input);
											}
										} else {
											var input = document.createElement('input');
											input.type = 'hidden';
											input.name = key;
											input.value = value;
											form.appendChild(input);
										}
									}
								}
								
								// iframe을 빈 페이지로 초기화 후 form submit
								if (iframe) {
									iframe.src = 'about:blank';
									setTimeout(function() {
										form.submit();
									}, 10);
								}
							}
						} else {
							// GET 방식: iframe src 업데이트
							if (iframe) {
								iframe.src = url;
							}
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
			templateId = this.normalizeTemplateId(templateId);
			var tabInfo = this.tabs.get(templateId);
			if (tabInfo) {
				// activeTab도 정규화된 값으로 비교 및 저장
				var normalizedActiveTab = this.normalizeTemplateId(this.activeTab);
				if (normalizedActiveTab && normalizedActiveTab !== templateId) {
					this.notifyTabDeactivation(normalizedActiveTab);
					this.previousActiveTab = normalizedActiveTab;
				}
				$('#pageTab a[data-template-id="' + templateId + '"]').tab('show');
				this.activeTab = templateId; // 정규화된 값 저장
				this.notifyTabActivation(templateId);
			}
		},

		notifyTabActivation: function(templateId) {
			templateId = this.normalizeTemplateId(templateId);
			var tabInfo = this.tabs.get(templateId);
			if (!tabInfo) return;
			var iframe = document.getElementById(tabInfo.iframeId);
			if (iframe && iframe.contentWindow) {
				try {
					iframe.contentWindow.postMessage({ type: 'TAB_ACTIVATION', active: true }, '*');
				} catch (e) {
					// cross origin ignore
				}
			}
		},

		notifyTabDeactivation: function(templateId) {
			templateId = this.normalizeTemplateId(templateId);
			var tabInfo = this.tabs.get(templateId);
			if (!tabInfo) return;
			var iframe = document.getElementById(tabInfo.iframeId);
			if (iframe && iframe.contentWindow) {
				try {
					iframe.contentWindow.postMessage({ type: 'TAB_ACTIVATION', active: false }, '*');
				} catch (e) {
					// cross origin ignore
				}
			}
		},
		
		// 탭 제거
		removeTab: function(templateId) {
			templateId = this.normalizeTemplateId(templateId);
			
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
				
				// 활성 탭이 제거된 경우 다음 탭 활성화 (정규화된 값으로 비교)
				var normalizedActiveTab = this.normalizeTemplateId(this.activeTab);
				if (normalizedActiveTab === templateId) {
					this.activeTab = null; // 제거된 탭이 활성 탭이었으므로 null로 설정
					this.activateNextTab();
				}
			}
		},
		
		// 다음 탭 활성화 (LIFO 방식)
		activateNextTab: function() {
			// activeTab이 null이거나 정규화되지 않은 경우를 대비
			var normalizedActiveTab = this.normalizeTemplateId(this.activeTab);
			var currentIndex = normalizedActiveTab ? this.tabOrder.indexOf(normalizedActiveTab) : -1;
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
			
			// 다음 탭이 있으면 활성화 (nextTab도 정규화된 값이어야 함)
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
			templateId = this.normalizeTemplateId(templateId);
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

		// 탭 전환/닫기 이벤트 리스너 등록
		$(document).ready(function() {
			// Bootstrap 탭 전환 이벤트 감지
			$('#pageTab').on('shown.bs.tab', 'a[data-toggle="tab"]', function(e) {
				var templateId = $(e.target).data('template-id');
				if (templateId) {
					// templateId 정규화 후 활성화
					templateId = window.tabManager.normalizeTemplateId(templateId);
					window.tabManager.activateTab(templateId);
				}
			});

			// 탭 닫기 버튼 이벤트
			$(document).on('click', '#pageTab .close', function(e) {
				e.preventDefault();
				e.stopPropagation();

				var templateId = $(this).closest('a').data('template-id');
				if (templateId) {
					// templateId 정규화 후 제거
					templateId = window.tabManager.normalizeTemplateId(templateId);
					window.tabManager.removeTab(templateId);
				}
			});
		});

})(window);

