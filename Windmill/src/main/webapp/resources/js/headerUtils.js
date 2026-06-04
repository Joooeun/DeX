/**
 * 헤더 관련 유틸리티 모듈
 * 비밀번호 변경, 공지사항, 글꼴 변경 등의 기능 제공
 */
(function(window, $) {
	'use strict';

	var PASSWORD_MIN = 8;
	var PASSWORD_MAX = 20;
	var PASSWORD_FORBIDDEN = /[<>\"'|\\]/;

	function validatePassword(value) {
		if (!value) {
			return '비밀번호는 필수입니다.';
		}
		if (value.length < PASSWORD_MIN || value.length > PASSWORD_MAX) {
			return '비밀번호는 ' + PASSWORD_MIN + '~' + PASSWORD_MAX + '자여야 합니다.';
		}
		if (PASSWORD_FORBIDDEN.test(value)) {
			return '허용되지 않는 문자: < > " \' \\ |';
		}
		if (!/[A-Za-z]/.test(value) || !/\d/.test(value) || !/[^A-Za-z0-9]/.test(value)) {
			return '영문, 숫자, 특수문자를 각각 1개 이상 포함해야 합니다.';
		}
		return null;
	}

	$(document).ajaxComplete(function(_event, xhr) {
		if (xhr.getResponseHeader('PASSWORD_CHANGE_REQUIRED') !== 'true') {
			return;
		}
		var path = window.location.pathname;
		if (path === '/index' || path === '/index/') {
			return;
		}
		window.location.href = '/index';
	});

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
		var password = $('#PW').val();
		if (password !== $('#newPW').val()) {
			alert("비밀번호가 일치하지 않습니다.");
			return;
		}

		var validationMessage = validatePassword(password);
		if (validationMessage) {
			alert(validationMessage);
			return;
		}

		$.ajax({
			type: 'post',
			url: '/User/changePW',
			data: {
				PW: password
			},
			success: function(result) {
				if (result && result.success) {
					alert("저장 되었습니다.");
					$('#changePWModal').modal('hide');
					$('#PW').val("");
					$('#newPW').val("");
					window.location.reload();
				} else {
					alert((result && result.message) ? result.message : "저장되지 않았습니다.");
				}
			},
			error: function(xhr) {
				if (xhr.getResponseHeader('PASSWORD_CHANGE_REQUIRED') === 'true') {
					window.location.href = '/index';
					return;
				}
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
		try {
			var closedDate = localStorage.getItem('noticeClosedDate');
			var today = new Date().toDateString();
			if (closedDate === today) {
				return;
			}
		} catch (e) {
		}
		
		$.ajax({
			url: '/SystemConfig/getNotice',
			type: 'GET',
			success: function(response) {
				if (response.success && 
				    response.noticeEnabled === 'true' && 
				    response.noticeContent && 
				    response.noticeContent.trim() !== '') {
					showNotice(response.noticeContent);
				}
			},
			error: function() {
			}
		});
	}

	function showNotice(content) {
		if (!content || content.trim() === '') {
			return;
		}
		
		$('#noticeContent').html(content.replace(/\n/g, '<br>'));
		$('#noticeModal').modal('show');
	}

	function changeFont(fontFamily) {
		localStorage.setItem('selectedFont', fontFamily);
		document.documentElement.style.setProperty('--selected-font', fontFamily);
		updateFontDropdownText(fontFamily);
		
		if (typeof ace !== 'undefined') {
			if (window.sqlEditors) {
				Object.values(window.sqlEditors).forEach(function(editor) {
					if (editor && typeof editor.setOptions === 'function') {
						editor.setOptions({
							fontFamily: fontFamily
						});
					}
				});
			}
			
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
		
		var iframes = document.querySelectorAll('iframe');
		iframes.forEach(function(iframe) {
			try {
				if (iframe.contentDocument) {
					var iframeDoc = iframe.contentDocument;
					var iframeWindow = iframe.contentWindow;
					
					iframeDoc.documentElement.style.setProperty('--selected-font', fontFamily);
					
					if (iframeWindow.$) {
						iframeWindow.$('*').css('font-family', fontFamily);
					} else {
						var allElements = iframeDoc.querySelectorAll('*');
						for (var i = 0; i < allElements.length; i++) {
							allElements[i].style.fontFamily = fontFamily;
						}
					}
					
					if (iframeWindow && typeof iframeWindow.changeFont === 'function') {
						iframeWindow.changeFont(fontFamily);
					}
					
					if (iframeWindow) {
						iframeWindow.postMessage({
							type: 'FONT_CHANGE',
							fontFamily: fontFamily
						}, '*');
					}
					
					if (iframeWindow) {
						if (iframeWindow.contentEditor && typeof iframeWindow.contentEditor.setOptions === 'function') {
							iframeWindow.contentEditor.setOptions({
								fontFamily: fontFamily
							});
						}
						if (iframeWindow.$) {
							iframeWindow.$('#contentTextarea').css('font-family', fontFamily);
						}
						
						if (iframeWindow.resultEditor && typeof iframeWindow.resultEditor.setOptions === 'function') {
							iframeWindow.resultEditor.setOptions({
								fontFamily: fontFamily
							});
						}
						if (iframeWindow.$) {
							iframeWindow.$('#resultTextarea').css('font-family', fontFamily);
						}
						
						if (iframeWindow.$) {
							iframeWindow.$('.formtextarea').css('font-family', fontFamily);
						}
						
						if (typeof iframeWindow.ace !== 'undefined') {
							if (iframeWindow.ace.edit) {
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
									}
								}
							}
						}
						
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
			}
		});
		
		try {
			if (window.parent && window.parent !== window) {
				var parentDropdown = window.parent.$('.dropdown-toggle');
				if (parentDropdown.length > 0 && typeof parentDropdown.dropdown === 'function') {
					parentDropdown.dropdown('hide');
				}
			} else {
				var dropdownToggle = $('.dropdown-toggle');
				if (dropdownToggle.length > 0 && typeof dropdownToggle.dropdown === 'function') {
					dropdownToggle.dropdown('hide');
				}
			}
		} catch (e) {
			console.debug('드롭다운 닫기 실패:', e);
		}
	}

	function updateFontDropdownText(fontFamily) {
		try {
			var dropdownToggle;
			if (window.parent && window.parent !== window) {
				dropdownToggle = window.parent.$('.dropdown-toggle');
			} else {
				dropdownToggle = $('.dropdown-toggle');
			}
			
			if (dropdownToggle.length > 0) {
				var newText = '<i class="fa fa-font"></i> ' + fontFamily + ' <span class="caret"></span>';
				dropdownToggle.html(newText);
			}
		} catch (e) {
			console.debug('드롭다운 텍스트 업데이트 실패:', e);
		}
	}

	window.HeaderUtils = {
		checkPWModal: checkPWModal,
		save: save,
		checkPW: checkPW,
		loadNotice: loadNotice,
		showNotice: showNotice,
		changeFont: changeFont,
		updateFontDropdownText: updateFontDropdownText
	};

	window.checkPWModal = checkPWModal;
	window.save = save;
	window.checkPW = checkPW;
	window.changeFont = changeFont;

})(window, jQuery);
