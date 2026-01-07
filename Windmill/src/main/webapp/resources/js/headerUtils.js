/**
 * 헤더 관련 유틸리티 모듈
 * 비밀번호 변경, 공지사항, 글꼴 변경 등의 기능 제공
 */
(function(window, $) {
	'use strict';

	/**
	 * 비밀번호 확인 모달 표시
	 */
	function checkPWModal() {
		$('#checkPWModal').modal('show');
	}

	/**
	 * 비밀번호 변경 저장
	 */
	function save() {
		if ($('#PW').val() !== $('#newPW').val()) {
			alert("비밀번호가 일치하지 않습니다.");
			return;
		}

		var lowerCaseLetters = /[a-z]|[A-Z]/g;
		var numbers = /[0-9]/g;

		if (!$('#PW').val().match(lowerCaseLetters) || !$('#PW').val().match(numbers)) {
			alert("비밀번호는 영문, 숫자를 포함해야 합니다.");
			return;
		}

		if ($('#PW').val().length < 8) {
			alert("비밀번호는 최소 8자리 이상입니다.");
			return;
		}

		$.ajax({
			type: 'post',
			url: '/User/changePW',
			data: {
				PW: $('#PW').val()
			},
			success: function(result) {
				alert("저장 되었습니다.");
				$('#changePWModal').modal('hide');
				$('#PW').val("");
				$('#newPW').val("");
			},
			error: function() {
				alert("저장되지 않았습니다.");
			}
		});
	}

	/**
	 * 현재 비밀번호 확인
	 */
	function checkPW() {
		$.ajax({
			type: 'post',
			url: '/User/checkPW',
			data: {
				PW: $('#curPW').val()
			},
			success: function(result) {
				if (result) {
					$('#checkPWModal').modal('hide');
					$('#changePWModal').modal('show');
				} else {
					alert("잘못된 비밀번호 입니다.");
				}
				$('#curPW').val("");
			},
			error: function(e) {
				alert("저장되지 않았습니다." + JSON.stringify(e));
			}
		});
	}

	/**
	 * 공지사항 로드
	 */
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

	/**
	 * 공지사항 표시
	 * @param {string} content - 공지사항 내용
	 */
	function showNotice(content) {
		$('#noticeContent').html(content.replace(/\n/g, '<br>'));
		$('#noticeModal').modal('show');
	}

	/**
	 * 글꼴 변경 함수
	 * @param {string} fontFamily - 변경할 글꼴 이름
	 */
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
					var iframeDoc = iframe.contentDocument;
					var iframeWindow = iframe.contentWindow;
					
					// CSS 변수 설정
					iframeDoc.documentElement.style.setProperty('--selected-font', fontFamily);
					
					// iframe 전체에 폰트 적용 (모든 요소)
					if (iframeWindow.$) {
						iframeWindow.$('*').css('font-family', fontFamily);
					} else {
						// jQuery가 없는 경우 직접 DOM 조작
						var allElements = iframeDoc.querySelectorAll('*');
						for (var i = 0; i < allElements.length; i++) {
							allElements[i].style.fontFamily = fontFamily;
						}
					}
					
					// iframe 내부의 changeFont 함수 호출 (있는 경우)
					if (iframeWindow && typeof iframeWindow.changeFont === 'function') {
						iframeWindow.changeFont(fontFamily);
					}
					
					// postMessage로 폰트 변경 이벤트 전송
					if (iframeWindow) {
						iframeWindow.postMessage({
							type: 'FONT_CHANGE',
							fontFamily: fontFamily
						}, '*');
					}
					
					// iframe 내부의 에디터들에 직접 폰트 적용
					if (iframeWindow) {
						// FileUpload.jsp의 에디터들
						if (iframeWindow.contentEditor && typeof iframeWindow.contentEditor.setOptions === 'function') {
							iframeWindow.contentEditor.setOptions({
								fontFamily: fontFamily
							});
						}
						if (iframeWindow.$) {
							iframeWindow.$('#contentTextarea').css('font-family', fontFamily);
						}
						
						// FileRead.jsp의 에디터들
						if (iframeWindow.resultEditor && typeof iframeWindow.resultEditor.setOptions === 'function') {
							iframeWindow.resultEditor.setOptions({
								fontFamily: fontFamily
							});
						}
						if (iframeWindow.$) {
							iframeWindow.$('#resultTextarea').css('font-family', fontFamily);
						}
						
						// SQLExecute.jsp의 textarea들 및 Ace Editor들
						if (iframeWindow.$) {
							iframeWindow.$('.formtextarea').css('font-family', fontFamily);
						}
						
						// SQLExecute.jsp의 Ace Editor들 (전역에서 찾기)
						if (typeof iframeWindow.ace !== 'undefined') {
							// 모든 Ace Editor 인스턴스 찾기
							if (iframeWindow.ace.edit) {
								// Ace Editor 컨테이너들을 찾아서 폰트 적용
								var aceContainers = iframeDoc.querySelectorAll('[id*="_ace"], #sql_text');
								for (var j = 0; j < aceContainers.length; j++) {
									try {
										var editor = iframeWindow.ace.edit(aceContainers[j].id);
										if (editor && typeof editor.setOptions === 'function') {
											editor.setOptions({
												fontFamily: fontFamily
											});
										}
									} catch (e) {
										// 개별 에디터 적용 실패는 무시
									}
								}
							}
						}
						
						// SQLTemplate.jsp의 에디터들
						if (iframeWindow.sqlEditors) {
							Object.values(iframeWindow.sqlEditors).forEach(function(editor) {
								if (editor && typeof editor.setOptions === 'function') {
									editor.setOptions({
										fontFamily: fontFamily
									});
								}
							});
						}
						if (iframeWindow.SqlTemplateState && iframeWindow.SqlTemplateState.sqlEditors) {
							Object.values(iframeWindow.SqlTemplateState.sqlEditors).forEach(function(editor) {
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

	/**
	 * 드롭다운 버튼 텍스트 업데이트 함수
	 * @param {string} fontFamily - 글꼴 이름
	 */
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

	// 전역 네임스페이스에 등록
	window.HeaderUtils = {
		checkPWModal: checkPWModal,
		save: save,
		checkPW: checkPW,
		loadNotice: loadNotice,
		showNotice: showNotice,
		changeFont: changeFont,
		updateFontDropdownText: updateFontDropdownText
	};

	// 전역 함수로도 등록 (하위 호환성)
	window.checkPWModal = checkPWModal;
	window.save = save;
	window.checkPW = checkPW;
	window.changeFont = changeFont;

})(window, jQuery);

