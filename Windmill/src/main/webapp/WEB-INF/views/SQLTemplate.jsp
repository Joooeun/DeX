<%@include file="common/common.jsp" %>

	<!-- Ace Editor CDN -->
	<script src="https://cdnjs.cloudflare.com/ajax/libs/ace/1.23.0/ace.js"></script>
	<script src="https://cdnjs.cloudflare.com/ajax/libs/ace/1.23.0/ext-language_tools.js"></script>

	<!-- SQL Template 전용 스타일 -->
	<link href="/resources/css/sql-template.css" rel="stylesheet" />
	
	<style>
		/* 로딩 오버레이 스타일 */
		.loading-overlay {
			position: fixed;
			top: 0;
			left: 0;
			width: 100%;
			height: 100%;
			background-color: rgba(0, 0, 0, 0.5);
			z-index: 9999;
			display: none;
		}
		
		.loading-spinner {
			position: absolute;
			top: 50%;
			left: 50%;
			transform: translate(-50%, -50%);
			background-color: white;
			padding: 30px;
			border-radius: 8px;
			box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
			text-align: center;
		}
		
		.spinner {
			border: 4px solid #f3f3f3;
			border-top: 4px solid #007bff;
			border-radius: 50%;
			width: 40px;
			height: 40px;
			animation: spin 1s linear infinite;
			margin: 0 auto 15px;
		}
		
		@keyframes spin {
			0% { transform: rotate(0deg); }
			100% { transform: rotate(360deg); }
		}
		
		.loading-text {
			color: #333;
			font-size: 14px;
			font-weight: 500;
		}
	</style>




	<script>
		$(document).ready(function () {
			loadCategories();
			loadDbConnections();
			initSqlEditors();

			// 변경사항 추적 설정
			setupChangeTracking();

			// 브라우저 이탈 시 변경사항 확인
			setupBeforeUnloadWarning();

			// "+" 탭 초기화
			addPlusTab();

			// 부트스트랩 툴팁 초기화
			$('[data-toggle="tooltip"]').tooltip({
				placement: 'top',
				trigger: 'hover'
			});
			
			// 초기 로드 완료 표시 (약간의 지연 후)
			setTimeout(function() {
				window.SqlTemplateState.initialLoadComplete = true;
				window.SqlTemplateState.lastLoadTime = Date.now();
			}, 1000);

		});

		// ===== 유틸리티 함수들 =====
		
		// HTML 이스케이프 함수
		function escapeHtml(text) {
			if (!text) return '';
			return text.toString()
				.replace(/&/g, '&amp;')
				.replace(/</g, '&lt;')
				.replace(/>/g, '&gt;')
				.replace(/"/g, '&quot;')
				.replace(/'/g, '&#39;');
		}

		// 날짜 포맷팅
		function formatDate(timestamp) {
			if (!timestamp)
				return '';
			var date = new Date(timestamp);
			return date.toLocaleDateString() + ' ' + date.toLocaleTimeString();
		}

		// 로딩 오버레이 제어 함수들
		function showLoading(message = '로딩 중...') {
			$('#loadingOverlay .loading-text').text(message);
			$('#loadingOverlay').show();
		}
		
		function hideLoading() {
			$('#loadingOverlay').hide();
		}

		// 토스트 메시지 표시 함수
		function showToast(message, type = 'info', duration = 3000) {
			var toastId = 'toast_' + Date.now();
			var iconClass = {
				'success': 'fa-check-circle',
				'error': 'fa-exclamation-circle',
				'warning': 'fa-exclamation-triangle',
				'info': 'fa-info-circle'
			}[type] || 'fa-info-circle';

			var bgClass = {
				'success': 'alert-success',
				'error': 'alert-danger',
				'warning': 'alert-warning',
				'info': 'alert-info'
			}[type] || 'alert-info';

			var toast = $('<div id="' + toastId + '" class="alert ' + bgClass + ' alert-dismissible" style="margin-bottom: 10px; animation: slideInDown 0.3s ease-out;">' +
				'<button type="button" class="close" data-dismiss="alert">&times;</button>' +
				'<i class="fa ' + iconClass + '"></i> ' + message +
				'</div>');

			$('#toastContainer').append(toast);

			// 자동 제거
			setTimeout(function () {
				$('#' + toastId).fadeOut(300, function () {
					$(this).remove();
				});
			}, duration);
		}

		// 공통 AJAX 요청 함수
		function makeAjaxRequest(config) {
			// 기본 설정
			var defaults = {
				method: 'GET',
				contentType: 'application/x-www-form-urlencoded',
				showLoading: false,
				loadingMessage: '로딩 중...',
				onSuccess: function(result) { /* 기본 성공 처리 */ },
				onError: function(xhr, status, error) { /* 기본 에러 처리 */ }
			};
			
			// 설정 병합
			var options = Object.assign({}, defaults, config);
			
			// 로딩 표시
			if (options.showLoading) {
				showLoading(options.loadingMessage);
			}
			
			// AJAX 요청
			$.ajax({
				type: options.method,
				url: options.url,
				data: options.data,
				contentType: options.contentType,
				success: function(result) {
					if (options.showLoading) {
						hideLoading();
					}
					
					if (result.success) {
						// 성공 콜백 호출
						if (options.onSuccess && typeof options.onSuccess === 'function') {
							options.onSuccess(result);
						}
					} else {
						// 실패 처리
						var errorMessage = result.error || '요청 처리에 실패했습니다.';
						showToast(errorMessage, 'error');
						if (options.onError && typeof options.onError === 'function') {
							options.onError(null, null, errorMessage);
						}
					}
				},
				error: function(xhr, status, error) {
					if (options.showLoading) {
						hideLoading();
					}
					
					// 에러 메시지 추출
					var errorMessage = options.errorMessage || '요청 중 오류가 발생했습니다.';
					if (xhr.responseJSON && xhr.responseJSON.error) {
						errorMessage = xhr.responseJSON.error;
					}
					
					showToast(errorMessage, 'error');
					if (options.onError && typeof options.onError === 'function') {
						options.onError(xhr, status, error);
					}
				}
			});
		}

		// 공통 AJAX 에러 처리 함수 (하위 호환성 유지)
		function handleAjaxError(xhr, status, error, defaultMessage, callback) {
			hideLoading();
			var errorMessage = defaultMessage;
			
			// 서버에서 상세 에러 메시지를 받은 경우
			if (xhr.responseJSON && xhr.responseJSON.error) {
				errorMessage = xhr.responseJSON.error;
			}
			
			showToast(errorMessage, 'error');
			if (callback) callback(false);
		}

		// 공통 목록 렌더링 함수
		function renderList(config) {
			// 기본 설정
			var defaults = {
				useFragment: true,
				emptyMessage: '데이터가 없습니다.',
				emptyMessageClass: 'text-muted text-center',
				emptyMessageStyle: 'padding: 20px;',
				onComplete: null
			};
			
			// 설정 병합
			var options = Object.assign({}, defaults, config);
			
			// 컨테이너 비우기
			options.container.empty();
			
			// 데이터가 있는 경우
			if (options.data && options.data.length > 0) {
				// 컨테이너 보이기
				options.container.show();
				
				if (options.useFragment) {
					// DocumentFragment를 사용한 배치 업데이트
					var fragment = document.createDocumentFragment();
					
					options.data.forEach(function(item, index) {
						var element = options.itemRenderer(item, index);
						if (element) {
							fragment.appendChild(element);
						}
					});
					
					// 한 번의 DOM 조작으로 모든 요소 추가
					options.container.append(fragment);
				} else {
					// 일반적인 방식
					options.data.forEach(function(item, index) {
						var element = options.itemRenderer(item, index);
						if (element) {
							options.container.append(element);
						}
					});
				}
			}
			
			// 완료 콜백 호출
			if (options.onComplete && typeof options.onComplete === 'function') {
				options.onComplete();
			}
		}

		// 공통 Select 옵션 렌더링 함수
		function renderSelectOptions(config) {
			// 기본 설정
			var defaults = {
				valueField: 'id',
				textField: 'name',
				placeholder: '선택하세요',
				allowClear: true,
				width: '100%',
				initSelect2: true
			};
			
			// 설정 병합
			var options = Object.assign({}, defaults, config);
			
			// Select 요소 비우기
			options.select.empty();
			
			// 데이터가 있는 경우 옵션 추가
			if (options.data && options.data.length > 0) {
				options.data.forEach(function(item) {
					var value = item[options.valueField];
					var text;
					
					// textField가 함수인 경우와 문자열인 경우 처리
					if (typeof options.textField === 'function') {
						text = options.textField(item);
					} else {
						text = item[options.textField];
					}
					
					var option = $('<option value="' + value + '">' + text + '</option>');
					options.select.append(option);
				});
			}
			
			// Select2 초기화
			if (options.initSelect2) {
				options.select.select2({
					placeholder: options.placeholder,
					allowClear: options.allowClear,
					width: options.width
				});
			}
		}

		// ===== ID 변환 유틸리티 함수들 =====
		
		// 연결 ID를 탭 ID로 변환 (콤마 → 하이픈)
		function connectionIdToTabId(connectionId) {
			return 'tab-' + connectionId.replace(/,/g, '-');
		}
		
		// 탭 ID를 연결 ID로 변환 (하이픈 → 콤마)
		function tabIdToConnectionId(tabId) {
			return tabId.replace('tab-', '').replace(/-/g, ',');
		}
		
		// 연결 ID를 에디터 ID로 변환 (콤마 → 하이픈)
		function connectionIdToEditorId(connectionId) {
			// default 연결 ID는 특별히 처리 (HTML에서 sqlEditor_default 사용)
			if (connectionId === 'default') {
				return 'sqlEditor_default';
			}
			return 'sqlEditor-' + connectionId.replace(/,/g, '-');
		}

		// ===== SQL 에디터 관련 함수들 =====

		// 모든 SQL 에디터의 자동완성 업데이트
		function updateAllEditorsCompleters() {
			if (window.SqlTemplateState.sqlEditors) {
				Object.keys(window.SqlTemplateState.sqlEditors).forEach(function (dbType) {
					var editor = window.SqlTemplateState.sqlEditors[dbType];
					if (editor && typeof ace !== 'undefined') {
						// 자동완성 업데이트
						var langTools = ace.require("ace/ext/language_tools");
						langTools.setCompleters([]);
						setupCustomCompleter(editor);
					}
				});
			}
		}

		// 커스텀 자동완성 설정 (하이라이팅 제거)
		function setupCustomCompleter(editor) {
			// 기존 자동완성 기능 유지하면서 커스텀 추가
			var langTools = ace.require("ace/ext/language_tools");

			// 커스텀 자동완성 제공자 생성
			var customCompleter = {
				getCompletions: function (editor, session, pos, prefix, callback) {
					var completions = [];

					// 파라미터 목록 가져오기
					var parameters = getParameterNames();

					// 파라미터만 자동완성에 추가
					parameters.forEach(function (paramName) {
						completions.push({
							caption: paramName,
							value: paramName,
							meta: "파라미터",
							docText: "템플릿 파라미터: " + paramName
						});
					});

					callback(null, completions);
				}
			};

			// 기존 자동완성 제공자들에 커스텀 제공자 추가
			langTools.addCompleter(customCompleter);
		}

		// 현재 파라미터 이름 목록 가져오기
		function getParameterNames() {
			var parameters = [];
			$('#parameterTableBody .parameter-name').each(function () {
				var paramName = $(this).val().trim();
				if (paramName) {
					parameters.push(paramName);
				}
			});
			return parameters;
		}

		// ===== 전역 상태 관리 객체 =====

		// 로딩 메시지 상수
		var LOADING_MESSAGES = {
			SAVING: '템플릿을 저장하는 중...',
			LOADING_TEMPLATE: '템플릿 정보를 불러오는 중...',
			LOADING_LIST: '템플릿 목록을 불러오는 중...'
		};

		// 전역 상태 관리 객체 (단순화)
		window.SqlTemplateState = {
			// 상태 관리
			hasUnsavedChanges: false,
			isLoading: false,
			initialLoadComplete: false,
			
			// 에디터 관리
			sqlEditors: {},
			
			// 모달 상태
			editMode: false,
			currentEditingConnectionId: null,
			
			// DB 연결 정보
			dbConnections: [],
			
			// 상태 변경 함수들
			markAsChanged: function() {
				if (!this.isLoading) {
					this.hasUnsavedChanges = true;
					updateSaveButtonState();
				}
			},
			
			resetChanges: function() {
				this.hasUnsavedChanges = false;
				updateSaveButtonState();
			}
		};

		// ===== 카테고리 관련 함수들 =====

		// 카테고리 목록 로드
		function loadCategories() {
			makeAjaxRequest({
				url: '/SQLTemplate/category/list',
				onSuccess: function(result) {
					renderCategoryOptions(result.data);
					renderCategoryList(result.data);
				}
			});
		}

		// 카테고리 목록 렌더링 (DOM 조작 최적화 - 배치 업데이트)
		function renderCategoryList(categories) {
			var container = $('#categoryList');
			container.empty();
			
			// DocumentFragment를 사용한 배치 업데이트
			var fragment = document.createDocumentFragment();
			
			// 미분류 카테고리 추가
			var uncategorizedElement = createUncategorizedItem();
			fragment.appendChild(uncategorizedElement);

			// 카테고리 목록 렌더링
			if (categories && categories.length > 0) {
				categories.forEach(function (category) {
					var itemElement = createCategoryItem(category);
					fragment.appendChild(itemElement);
				});
			}
			
			// 한 번의 DOM 조작으로 모든 카테고리 추가
			container.append(fragment);

			// 각 카테고리의 템플릿 개수 로드
			loadCategoryTemplateCounts();
			selectCategory('UNCATEGORIZED');
		}
		
		// 미분류 카테고리 아이템 생성 함수
		function createUncategorizedItem() {
			var itemHtml = '<div class="category-item" data-id="UNCATEGORIZED" onclick="selectCategory(\'UNCATEGORIZED\')">' +
				'<div class="row align-middle">' +
				'<div class="col-md-8">' +
				'<strong>📁 미분류</strong><br>' +
				'<small>카테고리가 지정되지 않은 템플릿</small>' +
				'</div>' +
				'<div class="col-md-4 text-right" style="display: flex; align-items: center; justify-content: flex-end;">' +
				'<span class="badge bg-gray template-count" id="count-UNCATEGORIZED">0</span>' +
				'</div>' +
				'</div>' +
				'</div>';
			
			return $(itemHtml)[0]; // jQuery 객체를 DOM 요소로 변환
		}
		
		// 카테고리 아이템 생성 함수 (HTML 문자열로 최적화)
		function createCategoryItem(category) {
			var itemHtml = '<div class="category-item" data-id="' + escapeHtml(category.CATEGORY_ID) + 
				'" onclick="selectCategory(\'' + escapeHtml(category.CATEGORY_ID) + '\')">' +
				'<div class="row">' +
				'<div class="col-md-1 col-sm-2" style="display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 0px 0px 0px 15px;">' +
				'<i class="fa fa-chevron-up category-icon order-icon" onclick="event.stopPropagation(); reorderCategory(\'' + 
				escapeHtml(category.CATEGORY_ID) + '\', \'up\')" title="위로 이동" style="margin: 1px 0;"></i>' +
				'<i class="fa fa-chevron-down category-icon order-icon" onclick="event.stopPropagation(); reorderCategory(\'' + 
				escapeHtml(category.CATEGORY_ID) + '\', \'down\')" title="아래로 이동" style="margin: 1px 0;"></i>' +
				'</div>' +
				'<div class="col-md-7 col-sm-7">' +
				'<strong>' + escapeHtml(category.CATEGORY_NAME) + '</strong><br>' +
				'<small>' + escapeHtml(category.CATEGORY_DESCRIPTION || '설명 없음') + '</small>' +
				'</div>' +
				'<div class="col-md-4 col-sm-3 text-right" style="display: flex; align-items: center; justify-content: flex-end;">' +
				'<span class="badge bg-blue template-count" id="count-' + escapeHtml(category.CATEGORY_ID) + '">0</span>&nbsp;' +
				'<i class="fa fa-edit category-icon edit-icon" onclick="event.stopPropagation(); editCategory(\'' + 
				escapeHtml(category.CATEGORY_ID) + '\')" title="수정"></i>&nbsp;' +
				'<i class="fa fa-trash category-icon delete-icon" onclick="event.stopPropagation(); deleteCategory(\'' + 
				escapeHtml(category.CATEGORY_ID) + '\')" title="삭제"></i>' +
				'</div>' +
				'</div>' +
				'</div>';
			
			return $(itemHtml)[0]; // jQuery 객체를 DOM 요소로 변환
		}


		// 카테고리별 템플릿 개수 로드
		function loadCategoryTemplateCounts() {
			// 미분류 템플릿 개수 로드
			makeAjaxRequest({
				url: '/SQLTemplate/category/templates',
				data: { categoryId: 'UNCATEGORIZED' },
				onSuccess: function(result) {
					var count = result.data ? result.data.length : 0;
					$('#count-UNCATEGORIZED').text(count);
				}
			});

			// 각 카테고리별 템플릿 개수 로드
			makeAjaxRequest({
				url: '/SQLTemplate/category/list',
				onSuccess: function(result) {
					if (result.data) {
						result.data.forEach(function (category) {
							makeAjaxRequest({
								url: '/SQLTemplate/category/templates',
								data: { categoryId: category.CATEGORY_ID },
								onSuccess: function(templateResult) {
									var count = templateResult.data ? templateResult.data.length : 0;
									$('#count-' + category.CATEGORY_ID).text(count);
								}
							});
						});
					}
				}
			});
		}

		// 카테고리 선택
		async function selectCategory(categoryId) {
			// 변경사항 확인 (초기 로드 시에는 확인하지 않음)
			if ($('.category-item.selected').length > 0) {
				const canProceed = await confirmUnsavedChanges(function() {
					// 저장 완료 후 실행될 로직
					$('.category-item').removeClass('selected');
					loadTemplatesByCategory(categoryId);
				});
				if (!canProceed) {
					return;
				}
				
				// 사용자가 확인을 선택한 경우 변경사항 초기화
				resetCurrentTemplate();
			} 

				$('.category-item').removeClass('selected');
				$('[data-id="' + categoryId + '"]').addClass('selected');
				loadTemplatesByCategory(categoryId);
		}

		// 카테고리별 템플릿 로드
		function loadTemplatesByCategory(categoryId, preserveSelection = false) {
			// 현재 선택된 템플릿 ID 저장 (선택 유지가 필요한 경우)
			var currentSelectedId = null;
			if (preserveSelection) {
				currentSelectedId = $('.template-item.selected').data('id');
			}
			
			makeAjaxRequest({
				url: '/SQLTemplate/category/templates',
				data: { categoryId: categoryId },
				onSuccess: function(result) {
					renderTemplates(result.data);
					
					// 선택 유지가 필요한 경우 이전 선택 복원
					if (preserveSelection && currentSelectedId) {
						setTimeout(function() {
							$('[data-id="' + currentSelectedId + '"]').addClass('selected');
						}, 100);
					}
				},
				onError: function() {
					showToast('템플릿 목록을 불러오는 중 오류가 발생했습니다.', 'error');
				}
			});
		}

		// 템플릿 렌더링
		function renderTemplates(templates) {
			renderList({
				container: $('#templateList'),
				data: templates,
				emptyMessage: '템플릿이 없습니다.',
				itemRenderer: function(template) {
					var item = $('<div class="template-item" data-id="'
						+ template.TEMPLATE_ID + '" onclick="selectTemplate(\''
						+ template.TEMPLATE_ID + '\')">' + '<div class="row">'
						+ '<div class="col-md-12">' + '<strong>'
						+ template.TEMPLATE_NAME + '</strong>'
						+ '<small style="float:right;">생성일: '
						+ formatDate(template.CREATED_TIMESTAMP) + '</small>'
						+ '</div>' + '</div>' + '</div>');
					return item[0]; // jQuery 객체를 DOM 요소로 변환
				}
			});
		}

		// 템플릿 선택
		async function selectTemplate(templateId) {
			// 변경사항 확인 + 콜백으로 템플릿 선택 로직 전달
			const canProceed = await confirmUnsavedChanges(function() {
				
				loadSqlTemplateDetail(templateId);
			});
		}




		// 카테고리 옵션 렌더링
		function renderCategoryOptions(categories) {
			renderSelectOptions({
				select: $('#sqlTemplateCategories'),
				data: categories,
				valueField: 'CATEGORY_ID',
				textField: 'CATEGORY_NAME',
				placeholder: '카테고리를 선택하세요'
			});
		}


		// 새 카테고리 생성
		function createCategory() {
			$('#categoryModal').modal('show');
			$('#categoryModalTitle').text('새 카테고리 생성');
			$('#categoryForm')[0].reset();
			$('#categoryId').val('');
			$('#categoryModalSaveBtn').text('생성');
		}

		// 카테고리 수정
		function editCategory(categoryId) {
			makeAjaxRequest({
				url: '/SQLTemplate/category/detail',
				data: { categoryId: categoryId },
				onSuccess: function(result) {
					var category = result.data;
					$('#categoryModal').modal('show');
					$('#categoryModalTitle').text('카테고리 수정');
					$('#categoryId').val(category.CATEGORY_ID);
					$('#categoryName').val(category.CATEGORY_NAME);
					$('#categoryDescription').val(category.CATEGORY_DESCRIPTION);
					$('#categoryModalSaveBtn').text('수정');
				},
				onError: function() {
					showToast('카테고리 정보를 불러오는데 실패했습니다.', 'error');
				}
			});
		}

		// 카테고리 저장 (생성/수정)
		function saveCategory() {
			var categoryId = $('#categoryId').val();
			var categoryName = $('#categoryName').val();
			var description = $('#categoryDescription').val();

			if (!categoryName.trim()) {
				showToast('카테고리명을 입력해주세요.', 'warning');
				return;
			}

			var url = categoryId ? '/SQLTemplate/category/update' : '/SQLTemplate/category/create';
			var data = categoryId ? {
				categoryId: categoryId,
				categoryName: categoryName,
				description: description
			} : {
				categoryName: categoryName,
				description: description
			};

			makeAjaxRequest({
				method: 'POST',
				url: url,
				data: data,
				onSuccess: function(result) {
					showToast(result.message, 'success');
					$('#categoryModal').modal('hide');
					loadCategories();
				},
				onError: function() {
					showToast('저장에 실패했습니다.', 'error');
				}
			});
		}

		// 카테고리 삭제
		function deleteCategory(categoryId) {
			if (!confirm('정말로 이 카테고리를 삭제하시겠습니까?')) {
				return;
			}

			makeAjaxRequest({
				method: 'POST',
				url: '/SQLTemplate/category/delete',
				data: { categoryId: categoryId },
				onSuccess: function(result) {
					showToast(result.message, 'success');
					loadCategories();
				},
				onError: function() {
					showToast('삭제에 실패했습니다.', 'error');
				}
			});
		}

		// 카테고리 순서 변경
		function reorderCategory(categoryId, direction) {
			makeAjaxRequest({
				method: 'POST',
				url: '/SQLTemplate/category/reorder',
				data: { categoryId: categoryId, direction: direction },
				onSuccess: function(result) {
					// 카테고리 목록 새로고침
					loadCategories();
				},
				onError: function() {
					showToast('순서 변경 중 오류가 발생했습니다.', 'error');
				}
			});
		}

		// DB 연결 목록 로드
		function loadDbConnections(callback) {
			makeAjaxRequest({
				url: '/SQLTemplate/db-connections',
				onSuccess: function(result) {
					// 전역 변수에 저장
					window.SqlTemplateState.dbConnections = result.data;
					renderDbConnections(result.data);
					
					// 콜백 함수가 있으면 실행
					if (callback && typeof callback === 'function') {
						callback();
					}
				},
				onError: function() {
					showToast('연결 목록을 불러오는데 실패했습니다.', 'error');
				}
			});
		}

		// DB 연결 옵션 렌더링
		function renderDbConnections(connections) {
			renderSelectOptions({
				select: $('#accessibleConnections'),
				data: connections,
				valueField: 'CONNECTION_ID',
				textField: function(connection) {
					return connection.CONNECTION_ID + ' (' + connection.DB_TYPE + ')';
				},
				placeholder: 'DB 연결을 선택하세요'
			});
		}

		// SQL 에디터들 초기화
		function initSqlEditors() {
			// Ace Editor가 로드될 때까지 대기
			var checkAce = setInterval(function () {
				if (typeof ace !== 'undefined') {
					clearInterval(checkAce);

					initSqlEditorForConnection('sqlEditor_default', 'SELECT * FROM (VALUES (\'기본 템플릿\'))');
				}
			}, 100);

			// 5초 후에도 로드되지 않으면 textarea 사용
			setTimeout(function () {
				if (typeof ace === 'undefined') {
					clearInterval(checkAce);
					// Ace Editor 로드 타임아웃, textarea 사용
				}
			}, 5000);
		}



		// 기본 템플릿 탭 활성화
		function activateDefaultTab() {
			$('#sqlContentTabs a:first').tab('show');
		}

		// 에디터에 포커스 설정
		function focusEditor(editorId) {
			if (typeof ace !== 'undefined') {
				try {
					var editorElement = document.getElementById(editorId);
					if (editorElement && editorElement.classList.contains('ace_editor')) {
						var editor = ace.edit(editorId);
						if (editor && typeof editor.focus === 'function') {
							editor.focus();
						}
					} else {
						// textarea인 경우
						var textarea = $('#' + editorId + ' .sql-textarea');
						if (textarea.length > 0) {
							textarea.focus();
						}
					}
				} catch (e) {
					// 에디터 포커스 실패 시 무시
				}
			}
		}


		// 파라미터 추가
		function addParameter() {
			var currentOrder = $('#parameterTableBody tr').length + 1;
			var row = $('<tr class="parameter-row">' +
				'<td><div>' +
				'<button type="button" class="btn btn-xs btn-default move-up" title="위로"><i class="fa fa-chevron-up"></i></button><br> ' +
				'<button type="button" class="btn btn-xs btn-default move-down" title="아래로"><i class="fa fa-chevron-down"></i></button>' +
				'<input type="hidden" class="parameter-order" value="' + currentOrder + '">' +
				'</div></td>' +
				'<td><input type="text" class="form-control parameter-name" placeholder="파라미터명"></td>' +
				'<td><input type="text" class="form-control parameter-description" placeholder="설명"></td>' +
				'<td><select class="form-control parameter-type">' +
				'<option value="STRING">문자열</option>' +
				'<option value="NUMBER">숫자</option>' +
				'<option value="TEXT">텍스트</option>' +
				'<option value="SQL">SQL</option>' +
				'<option value="LOG">로그</option>' +
				'</select></td>' +
				'<td><input type="text" class="form-control parameter-default" placeholder="기본값"></td>' +
				'<td><div><input type="checkbox" class="parameter-required"></div></td>' +
				'<td><div><input type="checkbox" class="parameter-readonly"></div></td>' +
				'<td><div><input type="checkbox" class="parameter-hidden"></div></td>' +
				'<td><div><input type="checkbox" class="parameter-disabled"></div></td>' +
				'<td><button type="button" class="btn btn-danger btn-xs parameter-delete-btn" onclick="removeParameter(this)"><i class="fa fa-minus"></i></button></td>' +
				'</tr>');

			$('#parameterTableBody').append(row);

			// 파라미터 이름 변경 시 자동완성 업데이트
			row.find('.parameter-name').on('input', function () {
				updateAllEditorsCompleters();
			});

			// 새로 추가된 행의 툴팁 초기화
			row.find('[data-toggle="tooltip"]').tooltip({
				placement: 'top',
				trigger: 'hover'
			});

			// 파라미터 속성 변경 이벤트 리스너 추가 (자동 필수 체크 제거)
			// row.find('.parameter-hidden').on('change', function () {
			// 	var isHidden = $(this).is(':checked');
			// 	var requiredCheckbox = $(this).closest('tr').find('.parameter-required');
			// 	// 숨김 필드면 자동으로 필수로 설정
			// 	if (isHidden) {
			// 		requiredCheckbox.prop('checked', true);
			// 	}
			// });

			// 순서 변경 버튼 이벤트 리스너 추가
			row.find('.move-up').on('click', function () {
				moveParameterUp($(this).closest('tr'));
			});

			row.find('.move-down').on('click', function () {
				moveParameterDown($(this).closest('tr'));
			});
		}

		// 파라미터 삭제
		function removeParameter(button) {
			$(button).closest('tr').remove();
			reorderParameters();

			// 파라미터 삭제 시 자동완성 업데이트
			updateAllEditorsCompleters();
		}

		// 파라미터 순서 재정렬
		function reorderParameters() {
			$('#parameterTableBody tr').each(function (index) {
				var newOrder = index + 1;
				$(this).find('.parameter-order').val(newOrder);
			});
		}

		// 파라미터 위로 이동
		function moveParameterUp(row) {
			var prevRow = row.prev();
			if (prevRow.length > 0) {
				row.insertBefore(prevRow);
				reorderParameters();
			}
		}

		// 파라미터 아래로 이동
		function moveParameterDown(row) {
			var nextRow = row.next();
			if (nextRow.length > 0) {
				row.insertAfter(nextRow);
				reorderParameters();
			}
		}


		// 파라미터 렌더링 (DOM 조작 최적화 - 배치 업데이트)
		function renderParameters(parameters) {
			renderList({
				container: $('#parameterTableBody'),
				data: parameters,
				emptyMessage: '',
				itemRenderer: function(param, index) {
					var order = param.PARAMETER_ORDER || (index + 1);
					return createParameterRow(param, order);
				},
				onComplete: function() {
					// 툴팁 초기화 (배치 처리)
					$('#parameterTableBody').find('[data-toggle="tooltip"]').tooltip({
						placement: 'top',
						trigger: 'hover'
					});
				}
			});
		}
		
		// 파라미터 행 생성 함수 (HTML 문자열로 최적화)
		function createParameterRow(param, order) {
			var rowHtml = '<tr class="parameter-row">' +
				'<td><div>' +
				'<button type="button" class="btn btn-xs btn-default move-up" title="위로"><i class="fa fa-chevron-up"></i></button><br> ' +
				'<button type="button" class="btn btn-xs btn-default move-down" title="아래로"><i class="fa fa-chevron-down"></i></button>' +
				'<input type="hidden" class="parameter-order" value="' + escapeHtml(order) + '">' +
				'</div></td>' +
				'<td><input type="text" class="form-control parameter-name" value="' + 
				escapeHtml(param.PARAMETER_NAME || '') + '" placeholder="파라미터명"></td>' +
				'<td><input type="text" class="form-control parameter-description" value="' + 
				escapeHtml(param.DESCRIPTION || '') + '" placeholder="설명"></td>' +
				'<td><select class="form-control parameter-type">' +
				'<option value="STRING"' + (param.PARAMETER_TYPE === 'STRING' ? ' selected' : '') + '>문자열</option>' +
				'<option value="NUMBER"' + (param.PARAMETER_TYPE === 'NUMBER' ? ' selected' : '') + '>숫자</option>' +
				'<option value="TEXT"' + (param.PARAMETER_TYPE === 'TEXT' ? ' selected' : '') + '>텍스트</option>' +
				'<option value="SQL"' + (param.PARAMETER_TYPE === 'SQL' ? ' selected' : '') + '>SQL</option>' +
				'<option value="LOG"' + (param.PARAMETER_TYPE === 'LOG' ? ' selected' : '') + '>로그</option>' +
				'</select></td>' +
				'<td><input type="text" class="form-control parameter-default" value="' + 
				escapeHtml(param.DEFAULT_VALUE || '') + '" placeholder="기본값"></td>' +
				'<td><div><input type="checkbox" class="parameter-required"' + 
				(param.IS_REQUIRED === true || param.IS_REQUIRED === 'true' ? ' checked' : '') + '></div></td>' +
				'<td><div><input type="checkbox" class="parameter-readonly"' + 
				(param.IS_READONLY === true || param.IS_READONLY === 'true' ? ' checked' : '') + '></div></td>' +
				'<td><div><input type="checkbox" class="parameter-hidden"' + 
				(param.IS_HIDDEN === true || param.IS_HIDDEN === 'true' ? ' checked' : '') + '></div></td>' +
				'<td><div><input type="checkbox" class="parameter-disabled"' + 
				(param.IS_DISABLED === true || param.IS_DISABLED === 'true' ? ' checked' : '') + '></div></td>' +
				'<td><button type="button" class="btn btn-danger btn-xs" onclick="removeParameter(this)"><i class="fa fa-minus"></i></button></td>' +
				'</tr>';
			
			return $(rowHtml)[0]; // jQuery 객체를 DOM 요소로 변환
		}
		
		// 파라미터 데이터 수집
		function collectParameters() {
			var parameters = [];
			$('#parameterTableBody tr').each(
				function () {
					var name = $(this).find('.parameter-name').val();
					if (name && name.trim()) {
						parameters.push({
							name: name.trim(),
							type: $(this).find('.parameter-type').val(),
							defaultValue: $(this).find('.parameter-default').val(),
							required: $(this).find('.parameter-required').is(':checked'),
							order: parseInt($(this).find('.parameter-order').val()) || 1,

							description: $(this).find('.parameter-description').val(),
							readonly: $(this).find('.parameter-readonly').is(':checked'),
							hidden: $(this).find('.parameter-hidden').is(':checked'),
							disabled: $(this).find('.parameter-disabled').is(':checked')
						});
					}
				});
			return parameters;
		}

		// SQL 템플릿 벨리데이션
		function validateSqlTemplate() {
			var errors = [];

			// 기본 정보 벨리데이션
			var sqlName = $('#sqlTemplateName').val().trim();
			if (!sqlName) {
				errors.push('SQL 이름을 입력해주세요.');
			} else if (sqlName.length > 100) {
				errors.push('SQL 이름은 100자 이하여야 합니다.');
			}

			// 모든 SQL 탭의 내용 검증
			var hasValidSqlContent = false;

			// 기본 템플릿 검증
			var defaultSqlContent = '';
			if (typeof ace !== 'undefined') {
				try {
					var defaultEditorElement = document.getElementById('sqlEditor_default');
					if (defaultEditorElement && defaultEditorElement.classList.contains('ace_editor')) {
						var defaultEditor = ace.edit('sqlEditor_default');
						defaultSqlContent = defaultEditor.getValue();
					} else {
						defaultSqlContent = $('#sqlEditor_default .sql-textarea').val() || '';
					}
				} catch (e) {
					defaultSqlContent = $('#sqlEditor_default .sql-textarea').val() || '';
				}
			} else {
				defaultSqlContent = $('#sqlEditor_default .sql-textarea').val() || '';
			}

			if (defaultSqlContent.trim()) {
				hasValidSqlContent = true;
			}

			// 추가 SQL 탭들 검증
			$('#sqlContentTabs .nav-item:not(:first)').each(function () {
				var tabLink = $(this).find('.nav-link');
				var href = tabLink.attr('href');
				
				// href가 없거나 예상 형식이 아니면 건너뛰기
				if (!href || !href.startsWith('#tab-')) {
					return;
				}
				
				// 탭 ID에서 연결 ID 추출 (하이픈을 콤마로 복원)
				var connectionId = tabIdToConnectionId(href.replace('#', ''));
				
				// connectionId가 유효하지 않으면 건너뛰기
				if (!connectionId || connectionId === 'default') {
					return;
				}
				
				var editorId = connectionIdToEditorId(connectionId);
				var sqlContent = '';

				if (typeof ace !== 'undefined') {
					try {
						var editorElement = document.getElementById(editorId);
						if (editorElement && editorElement.classList.contains('ace_editor')) {
							var editor = ace.edit(editorId);
							sqlContent = editor.getValue();
						} else {
							sqlContent = $('#' + editorId + ' .sql-textarea').val() || '';
						}
					} catch (e) {
						sqlContent = $('#' + editorId + ' .sql-textarea').val() || '';
					}
				} else {
					sqlContent = $('#' + editorId + ' .sql-textarea').val() || '';
				}

				if (sqlContent.trim()) {
					hasValidSqlContent = true;
				}
			});

			// SQL 내용 검증 결과
			if (!hasValidSqlContent) {
				errors.push('최소 하나의 SQL 내용을 입력해주세요.');
			}


			var executionLimit = parseInt($('#sqlExecutionLimit').val());
			if (isNaN(executionLimit) || executionLimit < 0 || executionLimit > 20000) {
				errors.push('실행 제한은 0~20,000 사이의 숫자여야 합니다.');
			}

			var refreshTimeout = parseInt($('#sqlRefreshTimeout').val());
			if (isNaN(refreshTimeout) || refreshTimeout < 0 || refreshTimeout > 3600) {
				errors.push('새로고침 타임아웃은 0~3600초 사이의 숫자여야 합니다.');
			}


			// 파라미터 벨리데이션
			var parameters = collectParameters();
			var parameterNames = [];
			var duplicateNames = [];

			parameters.forEach(function (param, index) {
				// 파라미터명 체크
				if (!param.name || !param.name.trim()) {
					errors.push('파라미터명을 입력해주세요. (순서: ' + (index + 1) + ')');
				} else if (param.name.length > 100) {
					errors.push('파라미터명은 100자 이하여야 합니다. (' + param.name + ')');
				} else if (!/^[a-zA-Z_][a-zA-Z0-9_]*$/.test(param.name)) {
					errors.push('파라미터명은 영문자, 숫자, 언더스코어만 사용 가능하며 숫자로 시작할 수 없습니다. (' + param.name + ')');
				}

				// 중복 파라미터명 체크
				if (param.name && param.name.trim()) {
					if (parameterNames.indexOf(param.name.toLowerCase()) !== -1) {
						duplicateNames.push(param.name);
					} else {
						parameterNames.push(param.name.toLowerCase());
					}
				}

				// 타입별 기본값 벨리데이션
				if (param.defaultValue && param.defaultValue.trim()) {
					switch (param.type) {
						case 'NUMBER':
							if (isNaN(param.defaultValue)) {
								errors.push('숫자 타입 파라미터의 기본값은 숫자여야 합니다. (' + param.name + ')');
							}
							break;
						case 'DATE':
							if (!(function(dateString) {
								var date = new Date(dateString);
								return date instanceof Date && !isNaN(date);
							})(param.defaultValue)) {
								errors.push('날짜 타입 파라미터의 기본값은 유효한 날짜여야 합니다. (' + param.name + ')');
							}
							break;
						case 'BOOLEAN':
							var boolValue = param.defaultValue.toLowerCase();
							if (boolValue !== 'true' && boolValue !== 'false' && boolValue !== '1' && boolValue !== '0') {
								errors.push('불린 타입 파라미터의 기본값은 true/false/1/0 중 하나여야 합니다. (' + param.name + ')');
							}
							break;
					}
				}
			});

			if (duplicateNames.length > 0) {
				errors.push('중복된 파라미터명이 있습니다: ' + duplicateNames.join(', '));
			}

			// 단축키 벨리데이션
			var shortcuts = getShortcutsFromUI();
			var shortcutKeys = [];
			var duplicateShortcuts = [];

			shortcuts.forEach(function (shortcut, index) {
				if (!shortcut.shortcutKey || !shortcut.shortcutKey.trim()) {
					errors.push('단축키를 입력해주세요. (순서: ' + (index + 1) + ')');
				} else if (!/^F[1-9]|F1[0-2]$/.test(shortcut.shortcutKey)) {
					errors.push('단축키는 F1~F12 중 하나여야 합니다. (' + shortcut.shortcutKey + ')');
				}

				if (!shortcut.shortcutName || !shortcut.shortcutName.trim()) {
					errors.push('단축키명을 입력해주세요. (순서: ' + (index + 1) + ')');
				}

				if (!shortcut.targetTemplateId || !shortcut.targetTemplateId.trim()) {
					errors.push('대상 템플릿을 선택해주세요. (순서: ' + (index + 1) + ')');
				}

				// 소스 컬럼 검증 (대상 템플릿의 파라미터 정보 기반)
				if (shortcut.sourceColumnIndexes && shortcut.sourceColumnIndexes.trim()) {
					var sourceColumns = shortcut.sourceColumnIndexes.split(',').map(function (col) {
						return col.trim();
					});

					// 숫자 형식 검증 (빈 문자열 허용)
					for (var i = 0; i < sourceColumns.length; i++) {
						if (!/^\d*$/.test(sourceColumns[i])) {
							errors.push('소스 컬럼은 숫자 또는 빈 값만 입력 가능합니다. (순서: ' + (index + 1) + ', 값: ' + sourceColumns[i] + ')');
							break;
						}
					}

					// 대상 템플릿의 파라미터 개수와 비교 검증
					if (shortcut.targetTemplateId) {
						// 동기적으로 파라미터 정보 가져오기 (검증을 위해)
						var parameterCount = getParameterCount(shortcut.targetTemplateId);
						if (parameterCount > 0) {
							// 빈 문자열이 아닌 숫자만 필터링하여 최대값 계산
							var numericColumns = sourceColumns.filter(function (col) {
								return col !== '';
							}).map(function (col) {
								return parseInt(col);
							});

							if (numericColumns.length > 0) {
								var maxColumnIndex = Math.max.apply(null, numericColumns);
								if (maxColumnIndex > parameterCount) {
									errors.push('소스 컬럼 인덱스가 대상 템플릿의 파라미터 개수를 초과합니다. (순서: ' + (index + 1) + ', 최대: ' + parameterCount + ', 입력: ' + maxColumnIndex + ')');
								}
							}
						}
					}
				}

				// 중복 단축키 체크
				if (shortcut.shortcutKey && shortcut.shortcutKey.trim()) {
					if (shortcutKeys.indexOf(shortcut.shortcutKey) !== -1) {
						duplicateShortcuts.push(shortcut.shortcutKey);
					} else {
						shortcutKeys.push(shortcut.shortcutKey);
					}
				}
			});

			if (duplicateShortcuts.length > 0) {
				errors.push('중복된 단축키가 있습니다: ' + duplicateShortcuts.join(', '));
			}

		// 에러가 있으면 알림
		if (errors.length > 0) {
			// 첫 번째 에러 메시지를 표시
			showToast(errors[0], 'error');
			return false;
		}

			return true;
		}

		// 날짜 유효성 검사


		// 단축키 추가
		function addShortcut() {
			var row = $('<tr class="shortcut-row">'
				+ '<td><input type="text" class="form-control shortcut-key" placeholder="F1"></td>'
				+ '<td><input type="text" class="form-control shortcut-name" placeholder="단축키명"></td>'
				+ '<td><select class="form-control target-template-select2">'
				+ '<option value="">대상 템플릿 선택</option>'
				+ '</select></td>'
				+ '<td><input type="text" class="form-control shortcut-description" placeholder="단축키 설명"></td>'
				+ '<td><input type="text" class="form-control source-columns" placeholder="1,2,3"></td>'
				+ '<td><div><input type="checkbox" class="auto-execute" checked></div></td>'
				+ '<td><div><input type="checkbox" class="shortcut-status" checked></div></td>'
				+ '<td><button type="button" class="btn btn-danger btn-xs parameter-delete-btn" onclick="removeShortcut(this)"><i class="fa fa-minus"></i></button></td>'
				+ '</tr>');
			$('#shortcutTableBody').append(row);
			
			// 단축키 입력 필드에 키보드 이벤트 리스너 추가
			row.find('.shortcut-key').on('keydown', function(e) {
				// F1~F12 키 감지
				if (e.keyCode >= 112 && e.keyCode <= 123) {
					e.preventDefault();
					var keyName = 'F' + (e.keyCode - 111);
					$(this).val(keyName);
					// 다음 필드로 포커스 이동
					$(this).closest('tr').find('.shortcut-name').focus();
				}
			});

			// 새로 추가된 행의 툴팁 초기화
			row.find('[data-toggle="tooltip"]').tooltip({
				placement: 'top',
				trigger: 'hover'
			});

			// 새로 추가된 행의 대상 템플릿 드롭다운에 옵션 로드 및 Select2 초기화
			loadTemplateOptions(row.find('.target-template-select2'));

			// 변경사항 표시
			markTemplateChanged();
		}

		// 단축키 삭제
		function removeShortcut(button) {
			$(button).closest('tr').remove();

			// 변경사항 표시
			markTemplateChanged();
		}


		// 단축키 렌더링 (DOM 조작 최적화 - 배치 업데이트)
		function renderShortcuts(shortcuts) {
			var tbody = $('#shortcutTableBody');
			tbody.empty();

			if (shortcuts && shortcuts.length > 0) {
				// DocumentFragment를 사용한 배치 업데이트
				var fragment = document.createDocumentFragment();

				shortcuts.forEach(function (shortcut) {
					var rowElement = createShortcutRow(shortcut);
					fragment.appendChild(rowElement);
				});
				
				// 한 번의 DOM 조작으로 모든 행 추가
				tbody.append(fragment);
				
				// 툴팁 초기화 (배치 처리)
				tbody.find('[data-toggle="tooltip"]').tooltip({
						placement: 'top',
						trigger: 'hover'
					});

				// Select2 초기화 (배치 처리)
				tbody.find('.target-template-select2').each(function() {
					var $select = $(this);
					var targetTemplateId = $select.closest('tr').find('.shortcut-key').attr('data-target-template-id');
					loadTemplateOptions($select, targetTemplateId);
				});
				
				// 단축키 입력 필드에 키보드 이벤트 리스너 추가
				tbody.find('.shortcut-key').on('keydown', function(e) {
					// F1~F12 키 감지
					if (e.keyCode >= 112 && e.keyCode <= 123) {
						e.preventDefault();
						var keyName = 'F' + (e.keyCode - 111);
						$(this).val(keyName);
						// 다음 필드로 포커스 이동
						$(this).closest('tr').find('.shortcut-name').focus();
					}
				});
			}
		}
		
		// 단축키 행 생성 함수 (HTML 문자열로 최적화)
		function createShortcutRow(shortcut) {
			var rowHtml = '<tr class="shortcut-row">' +
				'<td><input type="text" class="form-control shortcut-key" value="' + 
				escapeHtml(shortcut.SHORTCUT_KEY || '') + '" placeholder="F1" data-target-template-id="' + 
				escapeHtml(shortcut.TARGET_TEMPLATE_ID || '') + '"></td>' +
				'<td><input type="text" class="form-control shortcut-name" value="' + 
				escapeHtml(shortcut.SHORTCUT_NAME || '') + '" placeholder="단축키명"></td>' +
				'<td><select class="form-control target-template-select2">' +
				'<option value="">대상 템플릿 선택</option>' +
				'</select></td>' +
				'<td><input type="text" class="form-control shortcut-description" value="' + 
				escapeHtml(shortcut.SHORTCUT_DESCRIPTION || '') + '" placeholder="단축키 설명"></td>' +
				'<td><input type="text" class="form-control source-columns" value="' + 
				escapeHtml(shortcut.SOURCE_COLUMN_INDEXES || '') + '" placeholder="1,2,3"></td>' +
				'<td><div><input type="checkbox" class="auto-execute"' + 
				(shortcut.AUTO_EXECUTE ? ' checked' : '') + '></div></td>' +
				'<td><div><input type="checkbox" class="shortcut-status"' + 
				(shortcut.IS_ACTIVE ? ' checked' : '') + '></div></td>' +
				'<td><button type="button" class="btn btn-danger btn-xs parameter-delete-btn" onclick="removeShortcut(this)"><i class="fa fa-minus"></i></button></td>' +
				'</tr>';
			
			return $(rowHtml)[0]; // jQuery 객체를 DOM 요소로 변환
		}

		// 대상 템플릿의 파라미터 개수 가져오기 (DOM 기반)
		function getParameterCount(templateId) {
			// 현재 선택된 템플릿 ID와 비교
			var currentId = $('#sqlTemplateId').val();
			if (currentId === templateId) {
				return $('#parameterTableBody tr').length;
			}
			
			// 현재 템플릿이 아니면 기본값 반환
			return 0;
		}

		// 대상 템플릿의 파라미터 정보로 소스 컬럼 플레이스홀더 업데이트
		function updateSourceColumnsPlaceholder(templateId, sourceColumnsInput) {
			if (!templateId) {
				sourceColumnsInput.attr('placeholder', '1,2,3');
				return;
			}

			// 현재 선택된 템플릿의 파라미터 정보 사용
			var currentId = $('#sqlTemplateId').val();
			if (currentId === templateId) {
				var parameterRows = $('#parameterTableBody tr');
				
				if (parameterRows.length > 0) {
					// 파라미터 순서대로 인덱스 생성
					var parameterIndexes = [];
					var parameterNames = [];
					
					parameterRows.each(function(index) {
						parameterIndexes.push(index + 1);
						var paramName = $(this).find('.parameter-name').val();
						parameterNames.push((index + 1) + ':' + paramName);
					});

					var placeholder = parameterIndexes.join(',');
					sourceColumnsInput.attr('placeholder', placeholder + ' (예: 1,,3)');

					// 툴팁 업데이트
					var tooltipText = '대상 템플릿의 파라미터 순서: ' + placeholder +
						' (예: ' + parameterNames.join(', ') + '). 빈 값(,)으로 특정 파라미터를 건너뛸 수 있습니다.';

					sourceColumnsInput.attr('title', tooltipText);
				} else {
					sourceColumnsInput.attr('placeholder', '1,,3 (빈 값은 해당 파라미터 건너뛰기)');
					sourceColumnsInput.attr('title', '소스 컬럼 인덱스를 입력합니다. 콤마로 구분된 숫자 형태로 입력 (예: 1,,3 - 첫번째와 세번째 파라미터만 전달)');
				}
			} else {
				// 다른 템플릿이면 기본값 사용
				sourceColumnsInput.attr('placeholder', '1,,3 (빈 값은 해당 파라미터 건너뛰기)');
				sourceColumnsInput.attr('title', '소스 컬럼 인덱스를 입력합니다. 콤마로 구분된 숫자 형태로 입력 (예: 1,,3 - 첫번째와 세번째 파라미터만 전달)');
			}
		}

		// 템플릿 옵션 로드 (단축키 대상용)
		function loadTemplateOptions(selectElement, selectedValue) {
			$.ajax({
				type: 'GET',
				url: '/SQLTemplate/list',
				success: function (result) {
					if (result.success) {
						var options = '<option value="">대상 템플릿 선택</option>';
						result.data.forEach(function (template) {
							var selected = (selectedValue && selectedValue === template.TEMPLATE_ID) ? ' selected' : '';
							options += '<option value="' + template.TEMPLATE_ID + '"' + selected + '>' + template.TEMPLATE_NAME + '</option>';
						});

						if (selectElement) {
							selectElement.html(options);
							// Select2 초기화
							selectElement.select2({
								placeholder: '대상 템플릿 선택',
								allowClear: true,
								width: '100%',
								language: {
									noResults: function () {
										return "검색 결과가 없습니다.";
									},
									searching: function () {
										return "검색 중...";
									}
								}
							});

							// 대상 템플릿 변경 이벤트 리스너 추가
							selectElement.on('change', function () {
								var selectedTemplateId = $(this).val();
								var sourceColumnsInput = $(this).closest('tr').find('.source-columns');
								updateSourceColumnsPlaceholder(selectedTemplateId, sourceColumnsInput);
							});

							// 초기 선택된 값이 있으면 파라미터 정보 로드
							if (selectedValue) {
								updateSourceColumnsPlaceholder(selectedValue, selectElement.closest('tr').find('.source-columns'));
							}
						} else {
							// 기존 방식 (하위 호환성)
							$('.target-template').html(options);
						}
					}
				}
			});
		}


		// 현재 템플릿 초기화 (공통 함수)
		function resetCurrentTemplate() {
			// 로딩 상태 설정 (초기화 중에는 변경사항으로 간주하지 않음)
			window.SqlTemplateState.isLoading = true;

			// 폼 초기화
			$('#sqlTemplateId, #sqlTemplateName, #sqlTemplateDesc').val('');
			$('#sqlTemplateStatus').val('ACTIVE');
			$('#sqlExecutionLimit').val('0');
			$('#sqlRefreshTimeout').val('0');
			// 체크박스 설정 (이벤트 트리거 방지)
			$('#sqlNewline').off('change').prop('checked', false);
			$('#sqlInactive').prop('checked', false);
			$('#sqlAudit').prop('checked', false);

			// 해당 메뉴로 이동 버튼 비활성화
			updateGoToTemplateButton();

			// 에디터 초기화 완료 후 이벤트 핸들러 재연결
			setTimeout(function () {
				$('#sqlNewline').on('change', function () {
					// Ace Editor가 초기화되지 않은 상태에서는 아무것도 하지 않음
					if (typeof ace === 'undefined' || !window.SqlTemplateState.sqlEditors) {
						return;
					}

					try {
						// 모든 SQL 에디터에 대해 안전하게 처리
						Object.keys(window.SqlTemplateState.sqlEditors).forEach(function (dbType) {
							var editor = window.SqlTemplateState.sqlEditors[dbType];
							if (editor && typeof editor.resize === 'function') {
								editor.resize();
							}
						});
					} catch (e) {
						// 개행 보기 변경 시 에디터 리사이즈 실패 시 무시
					}
				});
			}, 500);

			// 카테고리 및 연결 설정 초기화
			$('#sqlTemplateCategories').val(null).trigger('change');
			$('#accessibleConnections').val(null).trigger('change');

			// 탭 초기화
			$('#sqlContentTabs .nav-item:not(:first)').remove();
			$('#sqlContentTabContent .tab-pane:not(#tab-default)').remove();
			initSqlEditorForConnection('sqlEditor_default', 'SELECT * FROM (VALUES (\'기본 템플릿\'))');
			
			// "+" 탭 추가
			addPlusTab();

			// 테이블 초기화
			$('#parameterTableBody, #shortcutTableBody').empty();
			$('.template-item').removeClass('selected');
			$('.target-template-select2').select2('destroy');

			// 변경사항 초기화 및 로딩 상태 해제
			window.SqlTemplateState.hasUnsavedChanges = false;
			window.SqlTemplateState.isLoading = false;
			window.SqlTemplateState.lastLoadTime = Date.now();
			updateSaveButtonState();
		}

		// 새 SQL 템플릿 생성
		async function createNewSqlTemplate() {
			// 변경사항 확인
			const canProceed = await confirmUnsavedChanges(function() {
				// 저장 완료 후 실행될 로직
				// 공통 초기화 함수 호출
				resetCurrentTemplate();

				// 새 템플릿 생성 시에만 필요한 추가 작업
				// 현재 선택된 카테고리를 자동으로 설정
				var selectedCategory = $('.category-item.selected').data('id');
				if (selectedCategory && selectedCategory !== 'UNCATEGORIZED') {
					$('#sqlTemplateCategories').val([selectedCategory]).trigger('change');
				}
			});
			if (!canProceed) {
				return;
			}
		}

		// 공통 벨리데이션 함수
		function validateTemplateForSave(callback, showNoChangesMessage = true) {
			// 벨리데이션 체크
			if (!validateSqlTemplate()) {
				if (callback) callback(false);
				return false;
			}

			// 템플릿 이름이 없으면 에러
			if (!$('#sqlTemplateName').val() || !$('#sqlTemplateName').val().trim()) {
				showToast('템플릿 이름을 입력해주세요.', 'warning');
				if (callback) callback(false);
				return false;
			}

			// 변경사항이 없으면 저장하지 않음 (선택사항)
			if (!window.SqlTemplateState.hasUnsavedChanges) {
				if (showNoChangesMessage) {
					showToast('변경된 내용이 없습니다.', 'info');
				}
				if (callback) callback(true); // 저장할 내용이 없어도 성공으로 간주
				return false;
			}

			return true;
		}

		// SQL 템플릿 저장 (UI에서 직접 값을 읽어서 저장) - 저장 버튼용
		function saveSqlTemplate() {
			// 공통 벨리데이션 체크
			if (!validateTemplateForSave(null, true)) {
				return;
			}

			// 로딩 화면 표시
			showLoading(LOADING_MESSAGES.SAVING);

			// UI에서 직접 값을 읽어서 서버로 전송 (새로고침 포함)
			saveTemplateToServer(null);
		}

		// SQL 템플릿 저장 (네비게이션용 - 새로고침 없음)
		function saveSqlTemplateForNavigation(callback) {
			// 로딩 화면 표시
			showLoading(LOADING_MESSAGES.SAVING);

			// 공통 벨리데이션 체크
			if (!validateTemplateForSave(callback, false)) {
				hideLoading();
				return;
			}

			// UI에서 직접 값을 읽어서 서버로 전송 (새로고침 없음)
			saveTemplateToServerForNavigation(callback);
		}


		// ===== ID 변환 유틸리티 함수들 =====
		
		// 연결 ID를 탭 ID로 변환 (콤마 → 하이픈)
		function connectionIdToTabId(connectionId) {
			return 'tab-' + connectionId.replace(/,/g, '-');
		}
		
		// 탭 ID를 연결 ID로 변환 (하이픈 → 콤마)
		function tabIdToConnectionId(tabId) {
			return tabId.replace('tab-', '').replace(/-/g, ',');
		}
		
		// 연결 ID를 에디터 ID로 변환 (콤마 → 하이픈)
		function connectionIdToEditorId(connectionId) {
			// default 연결 ID는 특별히 처리 (HTML에서 sqlEditor_default 사용)
			if (connectionId === 'default') {
				return 'sqlEditor_default';
			}
			return 'sqlEditor-' + connectionId.replace(/,/g, '-');
		}
		

		// SQL 내용을 서버 형식으로 변환
		function convertSqlContentsForServer(sqlContents) {
			var result = [];

			sqlContents.forEach(function (content) {
				// 연결 ID를 그대로 유지 (쉼표로 구분된 경우도 하나의 SQL 내용으로 처리)
				result.push({
					connectionId: content.CONNECTION_ID,
					sqlContent: content.SQL_CONTENT
				});
			});

			return result;
		}

		// 연결 ID로 연결 이름 가져오기
		function getConnectionName(connectionId) {
			// 전역 연결 목록에서 찾기
			if (window.SqlTemplateState.dbConnections) {
				var connection = window.SqlTemplateState.dbConnections.find(function (conn) {
					return conn.CONNECTION_ID === connectionId;
				});
				return connection ? connection.CONNECTION_NAME : connectionId;
			}
			return connectionId;
		}

		// 연결 목록을 축약된 형태로 표시 (탭 제목용)
		function getShortConnectionTitle(connectionId) {
			if (!connectionId) return '';
			
			// 쉼표로 구분된 연결 ID들 분리
			var connectionIds = connectionId.split(',').map(function(id) {
				return id.trim();
			}).filter(function(id) {
				return id.length > 0;
			});
			
			// 연결이 1개인 경우 그대로 반환
			if (connectionIds.length <= 1) {
				return connectionId;
			}
			
			// 연결이 2개 이상인 경우 "첫번째 외 N개" 형식으로 축약
			var firstConnection = connectionIds[0];
			var remainingCount = connectionIds.length - 1;
			
			return firstConnection + ' 외 ' + remainingCount + '개';
		}

		// 전체 연결 목록을 툴팁용으로 포맷팅
		function getFullConnectionTooltip(connectionId) {
			if (!connectionId) return '';
			
			var connectionIds = connectionId.split(',').map(function(id) {
				return id.trim();
			}).filter(function(id) {
				return id.length > 0;
			});
			
			if (connectionIds.length <= 1) {
				return connectionId;
			}
			
			return '전체 연결: ' + connectionIds.join(', ');
		}

		// 통합된 템플릿 저장 함수
		// options.refreshTemplate: true면 템플릿 재선택, false면 목록만 새로고침 (기본값: true)
		function saveTemplateToServer(callback, options = {}) {
			// 기본 옵션 설정
			var refreshTemplate = options.refreshTemplate !== false; // 기본값 true
			
			// UI에서 직접 값을 읽어서 새로운 JSON API 스펙에 맞게 데이터 구성
			var requestData = {
				template: {
					templateId: $('#sqlTemplateId').val() || '',
					templateName: $('#sqlTemplateName').val() || '',
					templateDesc: $('#sqlTemplateDesc').val() || '',
					sqlContent: getSqlContentFromEditor('sqlEditor_default'),
					accessibleConnectionIds: $('#accessibleConnections').val() || [],
					version: 1,
					status: $('#sqlTemplateStatus').val() || 'ACTIVE',
					executionLimit: parseInt($('#sqlExecutionLimit').val()) || 0,
					refreshTimeout: parseInt($('#sqlRefreshTimeout').val()) || 0,
					newline: $('#sqlNewline').is(':checked'),
					audit: $('#sqlAudit').is(':checked')
				},
				categories: $('#sqlTemplateCategories').val() || [],
				parameters: getParametersFromUI(),
				shortcuts: getShortcutsFromUI(),
				sqlContents: getSqlContentsFromUI()
			};

			$.ajax({
				type: 'POST',
				url: '/SQLTemplate/save',
				contentType: 'application/json',
				data: JSON.stringify(requestData),
				success: function (result) {
					if (result.success) {
						showToast('템플릿이 저장되었습니다.', 'success');

						// 변경사항 초기화
						window.SqlTemplateState.resetChanges();

						// 저장된 정보 추출
						var savedTemplateId = result.templateId;
						var savedCategoryId = result.categoryId || $('.category-item.selected').data('id');
						
						// 1단계: 카테고리 선택 (필요한 경우)
						if (savedCategoryId && $('.category-item.selected').data('id') !== savedCategoryId) {
							selectCategory(savedCategoryId);
						}
						
						// 2단계: 템플릿 목록 처리
						var selectedCategory = $('.category-item.selected').data('id');
						if (selectedCategory) {
							if (refreshTemplate) {
								// 템플릿 재선택 모드: 선택 유지하면서 템플릿 목록 새로고침
								loadTemplatesByCategory(selectedCategory, true);
								
								// 목록 로드 완료 후 템플릿 선택
								if (savedTemplateId) {
									setTimeout(function() {
										if ($('[data-id="' + savedTemplateId + '"]').length > 0) {
											selectTemplate(savedTemplateId);
										}
										// 모든 작업 완료 후 로딩 종료
										hideLoading();
									}, 1000);
								} else {
									// 템플릿 선택이 없는 경우 바로 로딩 종료
									setTimeout(function() {
										hideLoading();
									}, 100);
								}
							} else {
								// 네비게이션 모드: 템플릿 목록만 새로고침 (템플릿 재선택 없음)
								loadTemplatesByCategory(selectedCategory);
								hideLoading();
							}
						} else {
							// 카테고리가 없는 경우 바로 로딩 종료
							hideLoading();
						}
						
						// 카테고리별 템플릿 개수 업데이트
						loadCategoryTemplateCounts();
						
						// 콜백 호출 (성공)
						if (callback) callback(true);
					} else {
						hideLoading();
						showToast('저장 실패: ' + result.error, 'error');
						if (callback) callback(false);
					}
				},
				error: function (xhr, status, error) {
					handleAjaxError(xhr, status, error, '저장 중 오류가 발생했습니다.', callback);
				}
			});
		}

		// 네비게이션용 템플릿 저장 함수 (하위 호환성을 위한 래퍼)
		function saveTemplateToServerForNavigation(callback) {
			saveTemplateToServer(callback, { refreshTemplate: false });
		}

		// UI에서 SQL 에디터 내용을 가져오는 함수
		function getSqlContentFromEditor(editorId) {
			// 먼저 window.sqlEditors에서 찾기
			if (window.sqlEditors && window.sqlEditors[editorId]) {
				return window.sqlEditors[editorId].getValue();
			}
			// 그 다음 window.SqlTemplateState.sqlEditors에서 찾기
			else if (window.SqlTemplateState.sqlEditors && window.SqlTemplateState.sqlEditors[editorId]) {
				return window.SqlTemplateState.sqlEditors[editorId].getValue();
			} else {
				// Ace Editor가 없는 경우 textarea에서 가져오기
				return $('#' + editorId + ' .sql-textarea').val() || '';
			}
		}

		// UI에서 파라미터 정보를 가져오는 함수
		function getParametersFromUI() {
			var parameters = [];
			$('#parameterTableBody tr').each(function () {
				var row = $(this);
				var param = {
					parameterName: row.find('.parameter-name').val() || '',
					parameterType: row.find('.parameter-type').val() || 'STRING',
					parameterOrder: parseInt(row.find('.parameter-order').val()) || 1,
					isRequired: row.find('.parameter-required').is(':checked'),
					defaultValue: row.find('.parameter-default').val() || '',
					isReadonly: row.find('.parameter-readonly').is(':checked'),
					isHidden: row.find('.parameter-hidden').is(':checked'),
					isDisabled: row.find('.parameter-disabled').is(':checked'),
					description: row.find('.parameter-description').val() || ''
				};
				
				// 파라미터가 유효한 경우에만 추가
				if (param.parameterName && param.parameterName.trim()) {
					parameters.push(param);
				}
			});
			return parameters;
		}

		// UI에서 단축키 정보를 가져오는 함수
		function getShortcutsFromUI() {
			var shortcuts = [];
			$('#shortcutTableBody tr').each(function () {
				var row = $(this);
				var shortcut = {
					shortcutKey: row.find('.shortcut-key').val() || '',
					shortcutName: row.find('.shortcut-name').val() || '',
					targetTemplateId: row.find('.target-template-select2').val() || '',
					shortcutDescription: row.find('.shortcut-description').val() || '',
					sourceColumnIndexes: row.find('.source-columns').val() || '',
					autoExecute: row.find('.auto-execute').is(':checked'),
					isActive: row.find('.shortcut-status').is(':checked')
				};
				
				// 단축키가 유효한 경우에만 추가
				if (shortcut.shortcutKey && shortcut.shortcutKey.trim()) {
					shortcuts.push(shortcut);
				}
			});
			return shortcuts;
		}

		// UI에서 SQL 내용 정보를 가져오는 함수
		function getSqlContentsFromUI() {
			var sqlContents = [];
			
			// 추가 SQL 내용 탭들에서 가져오기 (첫 번째 sql-editor-container 제외)
			$('#sqlContentTabContent .sql-editor-container:not(:first)').each(function () {
				var container = $(this);
				var connectionId = container.data('connection-id');
				if (connectionId && connectionId !== 'default') {
					var editorId = connectionIdToEditorId(connectionId);
					var sqlContent = getSqlContentFromEditor(editorId);
					
					sqlContents.push({
						connectionId: connectionId,
						sqlContent: sqlContent || '', 
						version: 1
					});
				}
			});
			
			return sqlContents;
		}


		// SQL 템플릿 상세 정보 로드
		function loadSqlTemplateDetail(templateId) {
			showLoading(LOADING_MESSAGES.LOADING_TEMPLATE);
			
			$.ajax({
				type: 'GET',
				url: '/SQLTemplate/detail',
				data: {
					templateId: templateId
				},
				success: function (result) {
					
					if (result.success) {
						var template = result.data;
						$('#sqlTemplateId').val(template.templateId);
						$('#sqlTemplateName').val(template.sqlName);
						$('#sqlTemplateDesc').val(template.sqlDesc || '');
						$('#sqlTemplateStatus').val(template.sqlStatus || 'ACTIVE');
						$('#sqlExecutionLimit').val(template.executionLimit || 0);
						$('#sqlRefreshTimeout').val(template.refreshTimeout || 0);
						// 체크박스 설정 (이벤트 트리거 방지)
						$('#sqlNewline').off('change').prop('checked', template.newline === true);
						$('#sqlInactive').prop('checked', template.sqlStatus === 'INACTIVE');
						$('#sqlAudit').prop('checked', template.audit === true);

						// 해당 메뉴로 이동 버튼 활성화
						updateGoToTemplateButton();


						// 접근 가능한 DB 연결 설정
						if (template.accessibleConnectionIds) {
							var connectionIds = template.accessibleConnectionIds.split(',');
							$('#accessibleConnections').val(connectionIds).trigger('change');
						}

						// 기본 템플릿의 SQL 내용을 에디터에 설정
						initSqlEditorForConnection('sqlEditor_default', template.sqlContent || '');

						// 추가 데이터 로드 (파라미터, 단축키, SQL 내용)
						loadAdditionalTemplateData(templateId);

						// 저장 완료 후 실행될 로직
						$('.template-item').removeClass('selected');
						$('[data-id="' + templateId + '"]').addClass('selected');
					} else {
						hideLoading();
						showToast('템플릿 정보를 불러오는데 실패했습니다.', 'error');
					}
				},
				error: function() {
					hideLoading();
					showToast('템플릿 정보를 불러오는 중 오류가 발생했습니다.', 'error');
				}
			});
		}

		// 추가 템플릿 데이터 로드 (단순화된 로드 로직)
		function loadAdditionalTemplateData(templateId) {
			// 변경사항 추적 일시 중단 및 로드 시간 기록
			window.SqlTemplateState.isLoading = true;
			window.SqlTemplateState.lastLoadTime = Date.now();

			$.ajax({
				type: 'GET',
				url: '/SQLTemplate/full-detail',
				data: {
					templateId: templateId
				},
				success: function (result) {
					if (result.success && result.data) {
						var data = result.data;
						
						// 카테고리 선택 UI 업데이트
						if (data.categories && data.categories.length > 0) {
							$('#sqlTemplateCategories').val(data.categories).trigger('change');
						}
						
						// 데이터를 바로 UI에 렌더링 (로딩 상태 유지)
						if (data.sqlContents) renderSqlContentTabs(data.sqlContents);
						if (data.parameters) renderParameters(data.parameters);
						if (data.shortcuts) renderShortcuts(data.shortcuts);

						// 렌더링 완료 후 변경사항 초기화 및 추적 재개
						setTimeout(function() {
							window.SqlTemplateState.hasUnsavedChanges = false;
							window.SqlTemplateState.isLoading = false;
							updateSaveButtonState();
						}, 100);

						// 커스텀 이벤트 트리거
						$(document).trigger('templateDetailLoaded');
						
						// 로딩 완료
						hideLoading();
					} else {
						window.SqlTemplateState.isLoading = false;
						hideLoading();
					}
				},
				error: function (xhr, status, error) {
					window.SqlTemplateState.isLoading = false;
					handleAjaxError(xhr, status, error, '추가 데이터를 불러오는 중 오류가 발생했습니다.');
				}
			});
		}



		// 단일 SQL 내용 탭 추가 (기존 탭 유지)
		function addSingleSqlContentTab(content, activateTab) {
			if (!content || content.CONNECTION_ID === 'default') {
				return;
			}

			// 탭 ID 생성
			var tabId = connectionIdToTabId(content.CONNECTION_ID);
			
		// 중복 탭 체크
		if ($('#' + tabId).length > 0) {
			showToast('이미 해당 연결의 SQL 내용이 존재합니다.', 'warning');
			return;
		}
			var connectionExists = content.CONNECTION_EXISTS !== false;
			var tabText = getShortConnectionTitle(content.CONNECTION_ID); // 축약된 제목 사용
			var tabTooltip = getFullConnectionTooltip(content.CONNECTION_ID); // 툴팁용 전체 연결 목록
			var tabClass = 'nav-link';

			// 연결이 삭제된 경우 빨간색으로 표시
			if (!connectionExists) {
				tabText += ' <span class="text-danger">(연결 삭제됨)</span>';
				tabClass += ' text-danger';
			}

			// 탭 생성 (편집 버튼 포함)
			var tabElement = createSqlContentTab(content, tabId, tabClass, tabText, tabTooltip);
			// "+" 탭 앞에 새 탭 삽입
			if ($('#sqlContentTabs .add-tab').length > 0) {
				$('#sqlContentTabs .add-tab').before(tabElement);
			} else {
				$('#sqlContentTabs').append(tabElement);
				addPlusTab(); // "+" 탭이 없으면 추가
			}

			// 탭 컨텐츠 생성
			var contentElement = createSqlContentPane(content, tabId, connectionExists);
			$('#sqlContentTabContent').append(contentElement);

			// 에디터 초기화
			var editorId = connectionIdToEditorId(content.CONNECTION_ID);
			setTimeout(function() {
				initSqlEditorForConnection(editorId, content.SQL_CONTENT);
				
				// 탭 활성화 요청이 있으면 해당 탭 활성화
				if (activateTab) {
					$('a[href="#' + tabId + '"]').tab('show');
					focusEditor(editorId);
				}
			}, 50);
		}

		// "+" 탭 추가 (항상 마지막에 위치)
		function addPlusTab() {
			// 기존 "+" 탭이 있으면 제거
			$('#sqlContentTabs .add-tab').remove();
			
			// "+" 탭 생성
			var plusTab = $('<li class="nav-item add-tab">' +
				'<a class="nav-link" href="#" onclick="addSqlContent(); return false;" style="font-size:12px;" title="SQL 내용 추가">' +
				'<i class="fa fa-plus" style=" padding: 4px 0px;"></i>' +
				'</a>' +
				'</li>');
			
			// 탭 목록 마지막에 추가
			$('#sqlContentTabs').append(plusTab);
		}

		// SQL 내용 탭 렌더링 (DOM 조작 최적화 - 배치 업데이트)
		function renderSqlContentTabs(contents, activateLastTab) {
			// contents가 없으면 빈 배열로 처리
			if (!contents) {
				contents = [];
			}

			// 기존 추가 탭들 제거 (기본 탭과 "+" 탭은 유지)
			$('#sqlContentTabs .nav-item:not(:first):not(.add-tab)').remove();
			$('#sqlContentTabContent .tab-pane:not(#tab-default)').remove();

			// 추가 SQL 내용 탭들 추가
			if (contents && contents.length > 0) {
				// DocumentFragment를 사용한 배치 업데이트
				var tabsFragment = document.createDocumentFragment();
				var contentFragment = document.createDocumentFragment();
				
				contents.forEach(function (content, index) {
					// 'default' 연결은 기본 템플릿과 중복되므로 제외
					if (content.CONNECTION_ID === 'default') {
						return;
					}
					
					// 탭 ID 생성 (하이픈 기반)
					var tabId = connectionIdToTabId(content.CONNECTION_ID);
					var connectionExists = content.CONNECTION_EXISTS !== false; // 기본값은 true
					var tabText = getShortConnectionTitle(content.CONNECTION_ID); // 축약된 제목 사용
					var tabTooltip = getFullConnectionTooltip(content.CONNECTION_ID); // 툴팁용 전체 연결 목록
					var tabClass = 'nav-link';

					// 연결이 삭제된 경우 빨간색으로 표시
					if (!connectionExists) {
						tabText += ' <span class="text-danger">(연결 삭제됨)</span>';
						tabClass += ' text-danger';
					}

					// 탭 생성 (편집 버튼 포함)
					var tabElement = createSqlContentTab(content, tabId, tabClass, tabText, tabTooltip);
					tabsFragment.appendChild(tabElement);

					// 탭 컨텐츠 생성
					var contentElement = createSqlContentPane(content, tabId, connectionExists);
					contentFragment.appendChild(contentElement);

					// 마지막 탭이고 활성화 요청이 있으면 해당 탭 활성화
					if (activateLastTab && index === contents.length - 1) {
						setTimeout(function () {
							$('a[href="#' + tabId + '"]').tab('show');
							// 에디터에 포커스
							var editorId = connectionIdToEditorId(content.CONNECTION_ID);
							focusEditor(editorId);
						}, 100);
					}
				});
				
				// 한 번의 DOM 조작으로 모든 탭과 컨텐츠 추가
				$('#sqlContentTabs').append(tabsFragment);
				$('#sqlContentTabContent').append(contentFragment);
				
				// DOM 추가 후 SQL 에디터들 초기화
				contents.forEach(function (content, index) {
					var editorId = connectionIdToEditorId(content.CONNECTION_ID);
					// DOM이 완전히 렌더링된 후 에디터 초기화
					setTimeout(function() {
						initSqlEditorForConnection(editorId, content.SQL_CONTENT);
					}, 50);
				});
			}

			// "+" 탭 추가 (항상 마지막에)
			addPlusTab();

			// 기본 탭 활성화 (새로 추가된 탭이 없는 경우에만)
			if (!activateLastTab) {
				activateDefaultTab();
			}
		}
		
		// SQL 내용 탭 생성 함수 (HTML 문자열로 최적화)
		function createSqlContentTab(content, tabId, tabClass, tabText, tabTooltip) {
			// 툴팁이 있는 경우 title 속성 추가
			var titleAttr = tabTooltip ? ' title="' + escapeHtml(tabTooltip) + '"' : '';
			
			var tabHtml = '<li class="nav-item" style="display: inline-flex; align-items: center;">' +
						'<a class="' + tabClass + '" data-toggle="tab" href="#' + tabId + '" style="display: inline-flex; align-items: center; gap: 10px; "' + titleAttr + '>' +
						// 편집 아이콘 (왼쪽)
						'<button type="button" class="btn btn-sm" ' +
						'onclick="editSqlConnections(\'' + escapeHtml(content.CONNECTION_ID) + '\'); event.stopPropagation();" ' +
						'title="연결 편집" style="padding: 0; border: none; background: rgba(96, 92, 168, 0); color: #605ca8;">' +
						'<i class="fa fa-edit"></i>' +
						'</button>' +
						// 탭 이름 (중앙)
						'<span style="font-weight: 500;">' + tabText + '</span>' +
						// 삭제 x 버튼 (오른쪽)
						'<button type="button" class="btn btn-sm" ' +
						'onclick="deleteSqlContentTab(\'' + escapeHtml(content.CONNECTION_ID) + '\'); event.stopPropagation();" ' +
						'title="탭 삭제" style="padding:0; border: none; background: rgba(220, 53, 69, 0); color: #dc3545;">' +
						'<i class="fa fa-times"></i>' +
						'</button>' +
						'</a>' +
				'</li>';
			
			return $(tabHtml)[0]; // jQuery 객체를 DOM 요소로 변환
		}
		
		// SQL 내용 패널 생성 함수 (HTML 문자열로 최적화)
		function createSqlContentPane(content, tabId, connectionExists) {
			var editorId = connectionIdToEditorId(content.CONNECTION_ID);
			
					var contentHtml = '<div class="tab-pane fade" id="' + tabId + '">' +
				'<div class="sql-editor-container" data-connection-id="' + escapeHtml(content.CONNECTION_ID) + '" data-template-id="' + escapeHtml(content.TEMPLATE_ID) + '">';

					// 연결이 삭제된 경우 경고 메시지 추가
					if (!connectionExists) {
						contentHtml += '<div class="alert alert-warning" role="alert">' +
							'<i class="fa fa-exclamation-triangle"></i> ' +
					'<strong>경고:</strong> 해당 연결(' + escapeHtml(content.CONNECTION_ID) + ')이 삭제되었습니다. ' +
							'다른 연결을 선택하거나 이 SQL 내용을 삭제하세요.' +
							'</div>';
					}

					contentHtml += '<div id="' + editorId + '" class="sql-editor" style="height: 300px; border: 1px solid #ccc;"></div>' +
						'</div></div>';

			return $(contentHtml)[0]; // jQuery 객체를 DOM 요소로 변환
		}

		// 특정 연결용 SQL 에디터 초기화
		function initSqlEditorForConnection(editorId, sqlContent) {
			if (typeof ace !== 'undefined') {
				try {
					var editorElement = document.getElementById(editorId);
					if (editorElement) {
						ace.require("ace/ext/language_tools");
						var editor = ace.edit(editorId);
						editor.setTheme("ace/theme/chrome");
						editor.session.setMode("ace/mode/sql");
						
						// localStorage에서 저장된 폰트 가져오기
						var selectedFont = localStorage.getItem('selectedFont') || 'D2Coding';
						
						editor.setOptions({
							fontFamily: selectedFont,
							enableBasicAutocompletion: true,
							enableSnippets: true,
							enableLiveAutocompletion: true,
							showPrintMargin: false,
							showGutter: true,
							showInvisibles: false
						});

						// 커스텀 자동완성 설정 추가
						updateAllEditorsCompleters()


						editor.setValue(sqlContent || '');

					// 에디터를 전역 변수에 저장 (두 곳에 모두 저장)
					window.sqlEditors = window.sqlEditors || {};
					window.sqlEditors[editorId] = editor;
					
					window.SqlTemplateState.sqlEditors = window.SqlTemplateState.sqlEditors || {};
					window.SqlTemplateState.sqlEditors[editorId] = editor;
					
					// ACE 에디터 변경 이벤트 리스너 설정
					setupAceEditorChangeTracking(editorId);
					} else {
						initTextareaEditorForConnection(editorId, sqlContent);
					}
				} catch (e) {
					initTextareaEditorForConnection(editorId, sqlContent);
				}
			} else {
				initTextareaEditorForConnection(editorId, sqlContent);
			}
		}

		// Textarea 기반 SQL 에디터 초기화 (연결용)
		function initTextareaEditorForConnection(editorId, sqlContent) {
			var editorDiv = document.getElementById(editorId);
			if (!editorDiv) {
				return;
			}
			
			// localStorage에서 저장된 폰트 가져오기
			var selectedFont = localStorage.getItem('selectedFont') || 'D2Coding';
			
			editorDiv.innerHTML = '<textarea class="sql-textarea" style="width: 100%; height: 100%; font-family: ' + selectedFont + '; font-size: 14px; border: none; resize: none; outline: none;">' + (sqlContent || '') + '</textarea>';

			// textarea 변경 이벤트는 setupChangeTracking에서 전역으로 처리됨
		}
		
		// iframe에서 호출할 수 있도록 전역 함수로 노출
		window.changeFont = function(fontFamily) {
			// localStorage에 선택한 글꼴 저장
			localStorage.setItem('selectedFont', fontFamily);
			
			// 현재 페이지의 모든 요소에 글꼴 적용
			document.documentElement.style.setProperty('--selected-font', fontFamily);
			
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
			
			// textarea 에디터들에도 폰트 적용
			$('.sql-textarea').css('font-family', fontFamily);
		};


		// SQL 내용 추가 (기본 템플릿은 이미 존재하므로 추가 SQL만 생성)
		function addSqlContent() {
			// 편집 모드인지 확인
			var isEditMode = window.SqlTemplateState.editMode || false;
			var currentEditingConnectionId = window.SqlTemplateState.currentEditingConnectionId || null;

			// 접근 가능한 연결 목록 가져오기 (multiple select이므로 배열 반환)
			var accessibleConnectionIds = $('#accessibleConnections').val();

			// 편집 모드가 아닌 경우에만 접근 가능한 연결 체크
			if (!isEditMode) {
				// multiple select의 경우 배열이 반환됨
				// 접근 가능한 연결이 설정되지 않았으면 모든 연결 허용
				if (!accessibleConnectionIds || accessibleConnectionIds.length === 0) {
					// 모든 연결 허용 (필터링하지 않음)
					accessibleConnectionIds = null;
				} else {
					// 배열에서 빈 값 제거
					var connectionIds = accessibleConnectionIds.filter(function (id) {
						return id && id.trim && id.trim().length > 0;
					});

					if (connectionIds.length === 0) {
						// 빈 배열이면 모든 연결 허용
						accessibleConnectionIds = null;
					} else {
						accessibleConnectionIds = connectionIds;
					}
				}
			}

			// 전역 캐시에서 연결 목록 가져오기
			if (window.SqlTemplateState.dbConnections && window.SqlTemplateState.dbConnections.length > 0) {
				var connections = window.SqlTemplateState.dbConnections;

				// 편집 모드가 아닌 경우에만 접근 가능한 연결들만 필터링
				if (!isEditMode && accessibleConnectionIds) {
					connections = connections.filter(function (connection) {
						return accessibleConnectionIds.includes(connection.CONNECTION_ID);
					});
				}

			if (connections.length === 0) {
				showToast('접근 가능한 연결이 없습니다.', 'warning');
				return;
			}

				// 연결 선택 모달 표시
				showConnectionSelectionModal(connections, isEditMode, currentEditingConnectionId);
			} else {
				// 캐시된 데이터가 없으면 새로 로드
				loadDbConnections(function() {
					// 로드 완료 후 다시 시도
					addSqlContent();
				});
			}
		}

		// 연결 선택 모달 표시
		function showConnectionSelectionModal(connections, isEditMode, currentEditingConnectionId) {
			// 편집 모드인지 확인
			isEditMode = isEditMode || false;
			currentEditingConnectionId = currentEditingConnectionId || null;

			// 현재 템플릿에 이미 사용 중인 연결들 확인 (기본 템플릿 제외)
			var usedConnections = [];
			$('#sqlContentTabs .nav-item').each(function () {
				var tabLink = $(this).find('.nav-link');
				var href = tabLink.attr('href');
				if (href && href !== '#tab-default') {
					// 탭 ID에서 연결 ID 추출 (하이픈을 콤마로 복원)
					var connectionId = tabIdToConnectionId(href.replace('#', ''));
					// 'default' 연결도 제외 (기본 템플릿과 중복 방지)
					if (connectionId !== 'default') {
					usedConnections.push(connectionId);
					}
				}
			});

			var modalTitle = isEditMode ? 'SQL 연결 편집' : '추가 SQL 내용 생성';
			var modalInfo = isEditMode ?
				'<strong>편집:</strong> 현재 연결(' + currentEditingConnectionId + ')을 다른 연결로 변경하거나 제거할 수 있습니다.' :
				'<strong>참고:</strong> 기본 템플릿은 이미 존재합니다. 특정 연결에 맞는 추가 SQL 내용을 생성합니다.';

			var modalHtml = '<div class="modal fade" id="addSqlContentModal" tabindex="-1">' +
				'<div class="modal-dialog modal-lg">' +
				'<div class="modal-content">' +
				'<div class="modal-header">' +
				'<h5 class="modal-title">' + modalTitle + '</h5>' +
				'<button type="button" class="close" onclick="cancelAddSqlContent()">&times;</button>' +
				'</div>' +
				'<div class="modal-body">' +
				'<div class="alert alert-info">' + modalInfo + '</div>' +
				'<div class="form-group">' +
				'<label><strong>연결 선택</strong></label><br>' +
				'<small class="text-muted">' + (isEditMode ? '변경할 연결을 선택하거나 현재 연결의 체크를 해제하여 제거할 수 있습니다.' : '선택한 연결에 대해 SQL 내용이 생성됩니다.') + '</small>' +
				'<div id="connectionSelection" class="mt-3">' +
				'<label>선택할 연결:</label><br>';

			connections.forEach(function (connection) {
				// 'default' 연결은 기본 템플릿과 중복되므로 제외
				if (connection.CONNECTION_ID === 'default') {
					return;
				}
				
				// 연결 사용 여부 확인 (개별 연결 ID와 복합 연결 ID 모두 체크)
				var isUsed = false;
				for (var i = 0; i < usedConnections.length; i++) {
					var usedConnection = usedConnections[i];
					// 정확히 일치하거나 쉼표로 구분된 연결에 포함되는지 확인
					if (usedConnection === connection.CONNECTION_ID || 
						usedConnection.split(',').includes(connection.CONNECTION_ID)) {
						isUsed = true;
						break;
					}
				}
				
				// 편집 모드에서는 현재 연결도 체크 해제할 수 있도록 disabled 제외
				var disabledAttr = (isUsed && !(isEditMode && currentEditingConnectionId && 
					(currentEditingConnectionId === connection.CONNECTION_ID || 
					 currentEditingConnectionId.split(',').includes(connection.CONNECTION_ID)))) ? 'disabled' : '';
				var usedText = isUsed ? ' <span class="text-danger">(이미 사용 중)</span>' : '';

				// 편집 모드인 경우 현재 연결은 기본으로 체크된 상태로 표시
				var checkedAttr = '';
				if (isEditMode && currentEditingConnectionId) {
					// 현재 연결 ID를 쉼표로 분리하여 각각 체크
					var currentConnectionIds = currentEditingConnectionId.split(',');
					if (currentConnectionIds.includes(connection.CONNECTION_ID)) {
						checkedAttr = 'checked';
					}
				}

				modalHtml += '<div class="form-check form-check-inline">' +
					'<input class="form-check-input" type="checkbox" id="conn_' + connection.CONNECTION_ID + '" value="' + connection.CONNECTION_ID + '" ' + disabledAttr + ' ' + checkedAttr + '>' +
					'<label class="form-check-label" for="conn_' + connection.CONNECTION_ID + '">' +
					connection.CONNECTION_ID + ' (' + connection.DB_TYPE + ')' + usedText + '</label>' +
					'</div>';
			});

			modalHtml += '</div></div></div>' +
				'<div class="modal-footer">' +
				'<button type="button" class="btn btn-default" onclick="cancelAddSqlContent()">취소</button>' +
				'<button type="button" class="btn btn-primary" onclick="confirmAddSqlContent()">' + (isEditMode ? '적용' : '추가') + '</button>' +
				'</div></div></div></div>';

			$('body').append(modalHtml);
			$('#addSqlContentModal').modal('show');
		}

		function confirmAddSqlContent() {
			var templateId = $('#sqlTemplateId').val();
			var isEditMode = window.SqlTemplateState.editMode || false;
			var currentEditingConnectionId = window.SqlTemplateState.currentEditingConnectionId || null;

		if (!templateId) {
			showToast('먼저 템플릿을 선택해주세요.', 'warning');
			return;
		}

			// 선택된 연결들에 대해 SQL 내용 생성
			var selectedConnections = [];
			$('#connectionSelection input[type="checkbox"]:checked').each(function () {
				selectedConnections.push($(this).val());
			});

		// 편집 모드가 아닌 경우에만 연결 선택 체크
		if (!isEditMode && selectedConnections.length === 0) {
			showToast('하나 이상의 연결을 선택해주세요.', 'warning');
			return;
		}

			// 편집 모드에서 아무것도 선택하지 않은 경우 (현재 연결 제거 모드)
			if (isEditMode && selectedConnections.length === 0) {
				// 현재 연결이 체크 해제된 경우 삭제 확인
				var shortTitle = getShortConnectionTitle(currentEditingConnectionId);
				var fullConnections = currentEditingConnectionId;
				
				var confirmMessage = '모든 연결을 해제하면 해당 SQL 탭이 삭제됩니다.\n\n' +
					'탭 제목: ' + shortTitle + '\n' +
					'전체 연결: ' + fullConnections + '\n\n' +
					'정말 삭제하시겠습니까?';
				
				if (!confirm(confirmMessage)) {
					return; // 사용자가 취소를 선택한 경우 함수 종료
				}
			}


			if (isEditMode) {
				// 편집 모드: 현재 탭의 연결을 선택된 연결들로 교체
				if (selectedConnections.length === 0) {
					// 현재 연결 제거 - DOM에서 직접 탭 제거
					var oldTabId = connectionIdToTabId(currentEditingConnectionId);
					var deletedTabTitle = getShortConnectionTitle(currentEditingConnectionId);
					
					$('#' + oldTabId).remove();
					$('a[href="#' + oldTabId + '"]').closest('.nav-item').remove();
					
					// 기본 탭 활성화
					$('a[href="#tab-default"]').tab('show');
					
					// 삭제 완료 메시지
					showToast('SQL 탭이 삭제되었습니다: ' + deletedTabTitle, 'success');
				} else {
					// 현재 연결을 선택된 연결들로 교체
					var newConnectionId = selectedConnections.join(',');
					var newConnectionName = selectedConnections.map(function (connId) {
						return getConnectionName(connId);
					}).join(', ');
					
					// 연결 정보만 업데이트 (에디터는 그대로 유지)
					updateTabConnectionInfo(currentEditingConnectionId, newConnectionId, newConnectionName);
				}
				markTemplateChanged();
			} else {
				// 일반 모드: 선택된 연결들을 하나의 탭으로 통합
				if (selectedConnections.length > 0) {
					// 여러 연결을 하나의 객체로 통합
					var newSqlContent = {
						CONNECTION_ID: selectedConnections.join(','),
						SQL_CONTENT: '',
						CONNECTION_NAME: selectedConnections.map(function (connId) {
							return getConnectionName(connId);
						}).join(', ')
					};

					// 기존 탭들을 유지하면서 새 탭 추가
					addSingleSqlContentTab(newSqlContent, true);
				}
				markTemplateChanged();
			}

			// 편집 모드 변수 초기화
			window.SqlTemplateState.editMode = false;
			window.SqlTemplateState.currentEditingConnectionId = null;

			// 모달 완전히 제거
			$('#addSqlContentModal').modal('hide');
			$('body').removeClass('modal-open');
			$('.modal-backdrop').remove();
			$('#addSqlContentModal').remove();
		}

		// SQL 내용 추가 취소
		function cancelAddSqlContent() {
			// 편집 모드 변수 초기화
			window.SqlTemplateState.editMode = false;
			window.SqlTemplateState.currentEditingConnectionId = null;

			$('#addSqlContentModal').modal('hide');
			$('body').removeClass('modal-open');
			$('.modal-backdrop').remove();
			$('#addSqlContentModal').remove();
		}

		// 템플릿 변경사항 추적
		function markTemplateChanged() {
			// 로딩 중이거나 초기화 중에는 변경사항으로 간주하지 않음
			if (window.SqlTemplateState.isLoading) {
				return;
			}
			
			// 추가 안전장치: 템플릿 로드 후 짧은 시간 내 변경은 무시
			var now = Date.now();
			if (window.SqlTemplateState.lastLoadTime && (now - window.SqlTemplateState.lastLoadTime < 500)) {
				return;
			}
			
			// 초기 로드 완료 후에만 변경사항으로 간주
			if (!window.SqlTemplateState.initialLoadComplete) {
				return;
			}
			
			window.SqlTemplateState.markAsChanged();
		}

		// 변경사항 저장 확인 (노트패드 스타일)
		function confirmUnsavedChanges(callback) {
			if (!window.SqlTemplateState.hasUnsavedChanges) {
				// 변경사항이 없으면 바로 콜백 실행
				if (callback && typeof callback === 'function') {
					callback();
				}
				return Promise.resolve(true);
			}
			
			return new Promise(function(resolve) {
				var templateName = $('#sqlTemplateName').val() || '제목 없음';
				var message = templateName + '의 변경 내용을 저장하시겠습니까?';
				
				showSaveConfirmDialog(message, function(result) {
					if (result === 'save') {
						// 저장 후 진행
						saveSqlTemplateForNavigation(function(success) {
							if (success && callback && typeof callback === 'function') {
								// 저장 성공 시 콜백 실행
								callback();
							}
							resolve(success);
						});
					} else if (result === 'no') {
						// 저장하지 않고 진행
						if (callback && typeof callback === 'function') {
							callback();
						}
						resolve(true);
					} else {
						// 취소
						resolve(false);
					}
				});
			});
		}

		// 저장 확인 다이얼로그 표시 (노트패드 스타일)
		function showSaveConfirmDialog(message, callback) {
			// 기존 다이얼로그가 있으면 제거
			$('#saveConfirmModal').remove();
			
			var modalHtml = '<div class="modal fade" id="saveConfirmModal" tabindex="-1" role="dialog">' +
				'<div class="modal-dialog modal-dialog-centered" role="document" style="max-width: 400px;">' +
				'<div class="modal-content">' +
				'<div class="modal-header" style="border-bottom: 1px solid #dee2e6; padding: 15px 20px;">' +
				'<h5 class="modal-title" style="margin: 0; font-weight: 500;">Windmill</h5>' +
				'</div>' +
				'<div class="modal-body" style="padding: 20px; text-align: center;">' +
				'<p style="margin: 0; font-size: 14px; line-height: 1.5;">' + escapeHtml(message) + '</p>' +
				'</div>' +
				'<div class="modal-footer" style="border-top: 1px solid #dee2e6; padding: 15px 20px; justify-content: center;">' +
				'<button type="button" class="btn btn-primary" id="saveYesBtn" style="min-width: 70px; margin-right: 10px;">예</button>' +
				'<button type="button" class="btn btn-secondary" id="saveNoBtn" style="min-width: 70px; margin-right: 10px;">아니오</button>' +
				'<button type="button" class="btn btn-default" id="saveCancelBtn" style="min-width: 70px;">취소</button>' +
				'</div>' +
				'</div>' +
				'</div>' +
				'</div>';
			
			$('body').append(modalHtml);
			
			// 버튼 이벤트 설정
			$('#saveYesBtn').on('click', function() {
				$('#saveConfirmModal').modal('hide');
				callback('save');
			});
			
			$('#saveNoBtn').on('click', function() {
				$('#saveConfirmModal').modal('hide');
				callback('no');
			});
			
			$('#saveCancelBtn').on('click', function() {
				$('#saveConfirmModal').modal('hide');
				callback('cancel');
			});
			
			// ESC 키로 취소
			$('#saveConfirmModal').on('keydown', function(e) {
				if (e.keyCode === 27) { // ESC
					$('#saveConfirmModal').modal('hide');
					callback('cancel');
				}
			});
			
			// 모달 표시
			$('#saveConfirmModal').modal({
				backdrop: 'static',
				keyboard: false
			});
			
			// 모달이 완전히 숨겨진 후 DOM에서 제거
			$('#saveConfirmModal').on('hidden.bs.modal', function() {
				$(this).remove();
			});
		}


		// 브라우저 이탈 시 변경사항 경고 설정
		function setupBeforeUnloadWarning() {
			window.addEventListener('beforeunload', function(e) {
				if (window.SqlTemplateState.hasUnsavedChanges) {
					var message = '변경된 내용이 저장되지 않았습니다. 페이지를 벗어나시겠습니까?';
					e.preventDefault();
					e.returnValue = message; // Chrome에서 필요
					return message; // 다른 브라우저에서 필요
				}
			});
		}

		// UI 변경사항 추적 이벤트 리스너 설정 (단순화된 DOM 이벤트 기반)
		function setupChangeTracking() {
			// 기존 이벤트 리스너 제거 (중복 방지)
			cleanupEventListeners();
			
			// 폼 전체에 이벤트 위임으로 통합 관리 (단순화)
			$('#templateForm').on('input change', 'input, select, textarea', markTemplateChanged);
			
			// 동적 테이블 요소들 (이벤트 위임으로 자동 처리)
			$('#parameterTableBody, #shortcutTableBody').on('input change', 'input, select', markTemplateChanged);
			
			// 특수 컴포넌트들
			$(document).on('change', '.target-template-select2', markTemplateChanged);
			
			// SQL 에디터 컨테이너 (이벤트 위임)
			$('#sqlContentTabs').on('input change', '.sql-editor textarea, .sql-textarea', markTemplateChanged);
		}
		
		// 기존 이벤트 리스너 정리 함수
		function cleanupEventListeners() {
			// 기존 이벤트 리스너 제거
			$('#templateForm').off('input change');
			$('#parameterTableBody, #shortcutTableBody').off('input change');
			$('#sqlContentTabs').off('input change');
			$(document).off('change', '.target-template-select2');
		}

		// ACE 에디터 변경 이벤트 리스너 설정 (포괄적 이벤트 처리)
		function setupAceEditorChangeTracking(editorId) {
			if (typeof ace !== 'undefined') {
				var editor = null;
				
				// window.sqlEditors에서 먼저 찾기
				if (window.sqlEditors && window.sqlEditors[editorId]) {
					editor = window.sqlEditors[editorId];
				}
				// window.SqlTemplateState.sqlEditors에서도 찾기
				else if (window.SqlTemplateState.sqlEditors && window.SqlTemplateState.sqlEditors[editorId]) {
					editor = window.SqlTemplateState.sqlEditors[editorId];
				}
				
				if (editor && typeof editor.on === 'function') {
					// 기존 이벤트 제거 (중복 방지)
					editor.off('change', markTemplateChanged);
					editor.off('input', markTemplateChanged);
					editor.off('paste', markTemplateChanged);
					
					// 다양한 변경 이벤트 등록
					editor.on('change', markTemplateChanged);
					editor.on('input', markTemplateChanged);
					editor.on('paste', markTemplateChanged);
					
					// 세션 이벤트도 등록 (백스페이스, 삭제 등)
					if (editor.session && typeof editor.session.on === 'function') {
						editor.session.off('change', markTemplateChanged);
						editor.session.on('change', markTemplateChanged);
					}
				}
			}
		}

		// 저장 버튼 상태 업데이트
		function updateSaveButtonState() {
			var saveBtn = $('button[onclick="saveSqlTemplate()"]');
			if (window.SqlTemplateState.hasUnsavedChanges) {
				saveBtn.removeClass('btn-success').addClass('btn-warning');
				saveBtn.html('<i class="fa fa-save"></i> 저장 (변경됨)');
			} else {
				saveBtn.removeClass('btn-warning').addClass('btn-success');
				saveBtn.html('<i class="fa fa-save"></i> 저장');
			}
		}

		// SQL 연결 편집 모달 열기
		function editSqlConnections(currentConnectionId) {
			// 편집 모드로 기존 모달 재활용
			window.SqlTemplateState.editMode = true;
			window.SqlTemplateState.currentEditingConnectionId = currentConnectionId;

			// 기존 addSqlContent 함수 호출 (편집 모드로)
			addSqlContent();
		}


		// 탭의 연결 정보만 업데이트 (에디터는 그대로 유지)
		function updateTabConnectionInfo(oldConnectionId, newConnectionId, newConnectionName) {
			// 탭 ID 생성 (하이픈 기반)
			var oldTabId = connectionIdToTabId(oldConnectionId);
			var newTabId = connectionIdToTabId(newConnectionId);
			
			// 새로운 연결들의 존재 여부 확인 (편집에서는 존재하는 연결들만 선택되므로 모두 존재)
			var newConnectionIds = newConnectionId.split(',');
			var allConnectionsExist = true;
			
			// 편집 모드에서는 이미 존재하는 연결들만 선택할 수 있으므로 
			// 새로 선택된 연결들은 모두 존재한다고 가정
			// (삭제된 연결은 체크박스에서 해제되어 newConnectionId에 포함되지 않음)
			if (window.SqlTemplateState.dbConnections && newConnectionIds.length > 0) {
				for (var i = 0; i < newConnectionIds.length; i++) {
					var connId = newConnectionIds[i].trim();
					var connectionExists = window.SqlTemplateState.dbConnections.some(function(conn) {
						return conn.CONNECTION_ID === connId;
					});
					if (!connectionExists) {
						allConnectionsExist = false;
						break;
					}
				}
			}
			
			// 탭 텍스트와 클래스 설정 (축약된 제목 사용)
			var tabText = getShortConnectionTitle(newConnectionId);
			var tabTooltip = getFullConnectionTooltip(newConnectionId);
			var tabClass = 'nav-link';
			
			if (!allConnectionsExist) {
				tabText += ' <span class="text-danger">(연결 삭제됨)</span>';
				tabClass += ' text-danger';
			}
			
			// 탭 링크 업데이트
			var tabLink = $('a[href="#' + oldTabId + '"]');
			if (tabLink.length > 0) {
				// 탭 ID 변경
				tabLink.attr('href', '#' + newTabId);
				
				// 탭 클래스 업데이트 (색상 반영)
				tabLink.attr('class', tabClass);
				
				// 툴팁 추가/업데이트
				if (tabTooltip) {
					tabLink.attr('title', tabTooltip);
				} else {
					tabLink.removeAttr('title');
				}
				
				// 탭 텍스트 업데이트 - span 요소 찾아서 업데이트
				var spanElement = tabLink.find('span');
				if (spanElement.length > 0) {
					spanElement.html(tabText); // HTML로 변경하여 <span class="text-danger"> 태그 지원
				}
				
				// 편집 버튼의 onclick 속성도 업데이트
				var editButton = tabLink.find('button[title="연결 편집"]');
				if (editButton.length > 0) {
					editButton.attr('onclick', 'editSqlConnections(\'' + newConnectionId.replace(/'/g, "\\'") + '\'); event.stopPropagation();');
				}
				
				// 삭제 버튼의 onclick 속성도 업데이트
				var deleteButton = tabLink.find('button[title="탭 삭제"]');
				if (deleteButton.length > 0) {
					deleteButton.attr('onclick', 'deleteSqlContentTab(\'' + newConnectionId.replace(/'/g, "\\'") + '\'); event.stopPropagation();');
				}
			}
			
			// 탭 패널 ID 변경
			var tabPanel = $('#' + oldTabId);
			if (tabPanel.length > 0) {
				tabPanel.attr('id', newTabId);
				
				// 기존 경고 메시지 제거
				tabPanel.find('.alert-warning').remove();
				
				// 연결이 삭제된 경우 새로운 경고 메시지 추가
				if (!allConnectionsExist) {
					var alertHtml = '<div class="alert alert-warning" role="alert">' +
						'<i class="fa fa-exclamation-triangle"></i> ' +
						'<strong>경고:</strong> 해당 연결(' + newConnectionId + ')이 삭제되었습니다. ' +
						'다른 연결을 선택하거나 이 SQL 내용을 삭제하세요.' +
						'</div>';
					tabPanel.find('.sql-editor-container').prepend(alertHtml);
				}
			}
			
			// 에디터 컨테이너의 data-connection-id 업데이트
			var editorContainer = tabPanel.find('.sql-editor-container');
			if (editorContainer.length > 0) {
				editorContainer.attr('data-connection-id', newConnectionId);
			}
			
			// 에디터 ID도 업데이트 (ACE 에디터 인스턴스 관리)
			var oldEditorId = connectionIdToEditorId(oldConnectionId);
			var newEditorId = connectionIdToEditorId(newConnectionId);
			
			var oldEditorElement = $('#' + oldEditorId);
			if (oldEditorElement.length > 0) {
				oldEditorElement.attr('id', newEditorId);
				
				// ACE 에디터 인스턴스도 업데이트 (여러 위치 확인)
				if (window.sqlEditors && window.sqlEditors[oldEditorId]) {
					window.sqlEditors[newEditorId] = window.sqlEditors[oldEditorId];
					delete window.sqlEditors[oldEditorId];
				}
				if (window.SqlTemplateState.sqlEditors && window.SqlTemplateState.sqlEditors[oldEditorId]) {
					window.SqlTemplateState.sqlEditors[newEditorId] = window.SqlTemplateState.sqlEditors[oldEditorId];
					delete window.SqlTemplateState.sqlEditors[oldEditorId];
				}
			}
		}



		// SQL 내용 탭 삭제 (DOM 기반)
		function deleteSqlContentTab(connectionId, skipConfirm) {
			if (!skipConfirm && !confirm('이 SQL 내용을 삭제하시겠습니까?')) {
				return;
			}

			// DOM에서 직접 탭 제거
			var tabId = connectionIdToTabId(connectionId);
			$('#' + tabId).remove();
			$('a[href="#' + tabId + '"]').closest('.nav-item').remove();
			
			// 기본 탭 활성화
			$('a[href="#tab-default"]').tab('show');

			// 변경사항 표시
			markTemplateChanged();
		}




		// SQL 내용 삭제 (복합 키 방식)
		function deleteSqlContent(templateId, connectionId) {
			if (confirm('이 SQL 내용을 삭제하시겠습니까?\n삭제된 내용은 복구할 수 없습니다.')) {
				$.ajax({
					type: 'POST',
					url: '/SQLTemplate/sql-content/delete',
					data: {
						templateId: templateId,
						connectionId: connectionId
					},
					success: function (result) {
						if (result.success) {
							showToast('SQL 내용이 삭제되었습니다.', 'success');
							// 현재 템플릿의 SQL 내용 다시 로드
							var templateId = $('#sqlTemplateId').val();
							if (templateId) {
								loadSqlContents(templateId);
							}
						} else {
							showToast('삭제 실패: ' + result.error, 'error');
						}
					},
					error: function (xhr, status, error) {
						handleAjaxError(xhr, status, error, '삭제 중 오류가 발생했습니다.');
					}
				});
			}
		}



		// SQL 템플릿 삭제
		function deleteSqlTemplate() {
		var templateId = $('#sqlTemplateId').val();
		if (!templateId) {
			showToast('삭제할 템플릿을 선택해주세요.', 'warning');
			return;
		}

			if (!confirm('정말로 이 SQL 템플릿을 삭제하시겠습니까?')) {
				return;
			}

			$.ajax({
				type: 'POST',
				url: '/SQLTemplate/delete',
				data: {
					templateId: templateId
				},
				success: function (result) {
					if (result.success) {
						showToast('SQL 템플릿이 삭제되었습니다.', 'success');
						createNewSqlTemplate();
						var selectedCategory = $('.category-item.selected').data(
							'id');
						if (selectedCategory) {
							loadTemplatesByCategory(selectedCategory);
						}
						// 카테고리별 템플릿 개수 업데이트
						loadCategoryTemplateCounts();
					} else {
						showToast('삭제에 실패했습니다.', 'error');
					}
				}
			});
		}

		// 해당 메뉴로 이동 버튼 상태 업데이트
		function updateGoToTemplateButton() {
			var templateId = $('#sqlTemplateId').val();
			var button = $('#goToTemplateBtn');
			var buttonBottom = $('#goToTemplateBtnBottom');

			if (templateId && templateId.trim() !== '') {
				button.prop('disabled', false);
				buttonBottom.prop('disabled', false);
			} else {
				button.prop('disabled', true);
				buttonBottom.prop('disabled', true);
			}
		}

		// 해당 메뉴로 이동
		function goToTemplate() {
			var templateId = $('#sqlTemplateId').val();
			if (!templateId) {
				showToast('이동할 템플릿을 선택해주세요.', 'error');
				return;
			}

			// newpage 안의 iframe을 타겟으로 사용
			var parentWindow = window.parent || window;
			var newpageIframe = $(parentWindow.document).find('#newpage iframe');
			var targetName = newpageIframe.attr('name') || 'iframe';

			var url = '/SQL?templateId=' + templateId;

			// 단순한 링크 생성 및 클릭 (사이드바와 동일한 방식)
			var link = document.createElement('a');
			link.href = url;
			link.target = targetName;
			document.body.appendChild(link);
			link.click();
			document.body.removeChild(link);
		}




		// 단축키 입력 필드에 키 이벤트 리스너 추가 (테이블 범위로 최적화)
		$('#shortcutTableBody').on('focus', '.shortcut-key', function () {
			$(this).attr('data-listening', 'true');
			$(this).val('').attr('placeholder', '키를 누르세요...');
		});

		$('#shortcutTableBody').on('blur', '.shortcut-key', function () {
			$(this).removeAttr('data-listening');
			$(this).attr('placeholder', 'F1');
		});

		// 전역 키 이벤트 리스너
		$(document).on('keydown', function (e) {
			var activeShortcutField = $('.shortcut-key[data-listening="true"]');
			if (activeShortcutField.length > 0) {
				e.preventDefault();
				e.stopPropagation();

				// F1~F12 키 감지 (keyCode와 key 모두 확인)
				var keyCode = e.keyCode || e.which;
				var keyName = e.key;

				// F1~F12 키코드 범위: 112~123
				if ((keyCode >= 112 && keyCode <= 123) ||
					(keyName && keyName.match(/^F(1[0-2]|[1-9])$/))) {

					// F키 이름 생성
					var fKeyName = '';
					if (keyCode >= 112 && keyCode <= 123) {
						fKeyName = 'F' + (keyCode - 111);
					} else if (keyName) {
						fKeyName = keyName;
					}

					activeShortcutField.val(fKeyName);
					activeShortcutField.blur(); // 포커스 해제
				} else {
					// F1~F12가 아닌 키를 누른 경우 경고
					showToast('F1~F12 키만 사용 가능합니다.', 'warning');
				}
			}
		});


		// 탭 변경 시 SQL 내용 저장 (탭 컨테이너 범위로 최적화)
		$('#sqlContentTabs').on('shown.bs.tab', '.nav-link', function () {
			// 탭 변경 시 처리 (미리보기 제거됨)
			var href = $(this).attr('href');
			if (href && href !== '#tab-default') {
				// 해당 탭의 에디터에 포커스
				var tabId = href.replace('#', '');
				var connectionId = tabIdToConnectionId(tabId);
				var editorId = connectionIdToEditorId(connectionId);
				setTimeout(function() {
					focusEditor(editorId);
				}, 100);
			}
		});

		// 기본 SQL 설정 변경 시 변경사항 표시 (이벤트 위임으로 처리됨)
		// $('#sqlContentTabs').on('change', 'input[name="defaultSql"]', function () {
		//	markTemplateChanged();
		// });

	</script>

	<!-- Toast 알림 컨테이너 -->
	<div id="toastContainer" style="position: fixed; top: 20px; left: 50%; transform: translateX(-50%); z-index: 9999; width: 350px; font-size: 15px;"></div>


	<!-- Content Wrapper -->
	<div class="content-wrapper" style="margin-left: 0">
		<!-- Content Header -->
		<section class="content-header">
			<h1>SQL 템플릿 관리</h1>
			<ol class="breadcrumb">
				<li><a href="#"><i class="icon ion-ios-home"></i> Home</a></li>
				<li class="active">SQL 템플릿 관리</li>
			</ol>
		</section>

		<!-- Main content -->
		<section class="content">
			<div class="row">
				<!-- 카테고리 목록 패널 -->
				<div class="col-md-4 col-sm-5">
					<div class="box box-primary">
						<div class="box-header with-border">
							<h3 class="box-title">카테고리 목록</h3>
							<div class="box-tools pull-right">
								<button type="button" class="btn btn-box-tool" onclick="createCategory()">
									<i class="fa fa-plus"></i> 새 카테고리
								</button>
							</div>
						</div>
						<div class="box-body">
							<div id="categoryList" class="category-list">
								<!-- 카테고리가 여기에 로드됩니다 -->
							</div>
						</div>
					</div>

					<div class="box box-success">
						<div class="box-header with-border">
							<h3 class="box-title">템플릿 목록</h3>
							<div class="box-tools pull-right">
								<button type="button" class="btn btn-box-tool" onclick="createNewSqlTemplate()">
									<i class="fa fa-plus"></i> 새 템플릿
								</button>
							</div>
						</div>
						<div class="box-body">
							<div id="templateList" class="template-list">
								<!-- 템플릿이 여기에 로드됩니다 -->
							</div>
						</div>
					</div>
				</div>

				<!-- SQL 편집 패널 -->
				<div class="col-md-8 col-sm-7">
					<div class="box box-info">
						<div class="box-header with-border">
							<h3 class="box-title">SQL 템플릿 편집</h3>
							<div class="box-tools pull-right">
								<button type="button" class="btn btn-default btn-sm" id="goToTemplateBtn"
									onclick="goToTemplate()" disabled>
									<i class="fa fa-external-link"></i> 해당 메뉴로 이동
								</button>
								<button type="button" class="btn btn-success btn-sm" onclick="saveSqlTemplate()">
									<i class="fa fa-save"></i> 저장
								</button>
								<button type="button" class="btn btn-danger btn-sm" onclick="deleteSqlTemplate()" style="margin-left: 5px;">
									<i class="fa fa-trash"></i> 삭제
								</button>
							</div>
						</div>
						<div class="box-body">
							<!-- 템플릿 폼 시작 -->
							<form id="templateForm">
							<!-- 숨겨진 ID 필드 -->
							<input type="hidden" id="sqlTemplateId">

							<!-- 기본 정보 및 설정 (통합) -->
							<div class="panel panel-default" style="margin-bottom: 15px;">
								<div class="panel-heading" style="padding: 8px 15px;">
									<h4 class="panel-title" style="font-size: 14px; margin: 0;">
										<i class="fa fa-info-circle"></i> 기본 정보 및 설정
									</h4>
								</div>
								<div class="panel-body" style="padding: 15px;">
									<!-- 첫 번째 행: 이름(1) + 실행제한(1) + 카테고리(2) -->
									<div class="row">
										<div class="col-md-2">
											<div class="form-group" style="margin-bottom: 15px;">
												<label data-toggle="tooltip" data-placement="top"
													title="SQL 템플릿의 고유 이름입니다. 대시보드와 메뉴에서 표시되며, 100자 이하로 입력해주세요."
													style="font-size: 12px; margin-bottom: 5px; font-weight: 500;">
													SQL 이름 <span class="text-danger">*</span>
												</label>
												<input type="text" class="form-control"
													id="sqlTemplateName" placeholder="예: 사용자 활동 조회">
											</div>
										</div>
										<div class="col-md-2">
											<div class="form-group" style="margin-bottom: 15px;">
												<label data-toggle="tooltip" data-placement="top"
													title="SQL 실행 결과의 최대 행 수를 제한합니다. 0으로 설정하면 제한이 없습니다."
													style="font-size: 12px; margin-bottom: 5px; font-weight: 500;">
													실행 제한 (행)
												</label>
												<input type="number" class="form-control"
													id="sqlExecutionLimit" value="0" min="0" max="20000"
													placeholder="0 = 제한 없음">
											</div>
										</div>
										<div class="col-md-2">
											<div class="form-group" style="margin-bottom: 15px;">
												<label data-toggle="tooltip" data-placement="top"
													title="대시보드에서 자동으로 데이터를 새로고침하는 간격을 설정합니다. 0으로 설정하면 자동 새로고침을 사용하지 않습니다."
													style="font-size: 12px; margin-bottom: 5px; font-weight: 500;">
													새로고침 간격 (초)
												</label>
												<input type="number" class="form-control"
													id="sqlRefreshTimeout" value="0" min="0" max="3600"
													placeholder="0 = 자동 새로고침 안함">
											</div>
										</div>
										<div class="col-md-6">
											<div class="form-group" style="margin-bottom: 15px;">
												<label data-toggle="tooltip" data-placement="top"
													title="SQL 템플릿을 분류하여 관리합니다. 카테고리별로 템플릿을 그룹화하여 찾기 쉽게 만들 수 있습니다."
													style="font-size: 12px; margin-bottom: 5px; font-weight: 500;">
													카테고리
												</label>
												<select class="form-control" id="sqlTemplateCategories" multiple>
													<!-- 카테고리 옵션들이 여기에 로드됩니다 -->
												</select>
											</div>
										</div>
									</div>
									
									<!-- 두 번째 행: 새로고침간격(1) + 연결가능DB(3) -->
									<div class="row">
										<div class="col-md-2">
											<div class="form-group" style="margin-bottom: 10px;">
												<label data-toggle="tooltip" data-placement="top"
													title="결과 테이블에서 긴 텍스트를 여러 줄로 표시합니다"
													style="font-size: 11px; margin-bottom: 5px; font-weight: 500; display: block;">
													개행보기
												</label>
												<label class="switch">
													<input type="checkbox" id="sqlNewline">
													<span class="slider round"></span>
												</label>
											</div>
										</div>
										<div class="col-md-2">
											<div class="form-group" style="margin-bottom: 10px;">
												<label data-toggle="tooltip" data-placement="top"
													title="SQL 실행 기록을 감사 로그에 남깁니다"
													style="font-size: 11px; margin-bottom: 5px; font-weight: 500; display: block;">
													감사로그
												</label>
												<label class="switch">
													<input type="checkbox" id="sqlAudit">
													<span class="slider round"></span>
												</label>
											</div>
										</div>
										<div class="col-md-2">
											<div class="form-group" style="margin-bottom: 10px;">
												<label data-toggle="tooltip" data-placement="top"
													title="템플릿을 비활성화하면 메뉴에서 숨겨집니다"
													style="font-size: 11px; margin-bottom: 5px; font-weight: 500; display: block;">
													비활성화
												</label>
												<label class="switch">
													<input type="checkbox" id="sqlInactive">
													<span class="slider round"></span>
												</label>
											</div>
										</div>
										<div class="col-md-6">
											<div class="form-group" style="margin-bottom: 15px;">
												<label data-toggle="tooltip" data-placement="top"
													title="이 SQL 템플릿을 사용할 수 있는 데이터베이스 연결을 선택합니다. 아무것도 선택하지 않으면 모든 DB 연결에서 사용 가능합니다."
													style="font-size: 12px; margin-bottom: 5px; font-weight: 500;">
													접근 가능한 DB 연결
												</label>
												<select class="form-control" id="accessibleConnections" multiple>
													<!-- DB 연결 옵션들이 여기에 로드됩니다 -->
												</select>
											</div>
										</div>
									</div>
									
									<!-- 세 번째 행: 설명(2) + 옵션설정(2) -->
									<div class="row">
										<div class="col-md-6">
											<div class="form-group" style="margin-bottom: 15px;">
												<label data-toggle="tooltip" data-placement="top"
													title="이 템플릿의 용도와 사용법을 설명하세요"
													style="font-size: 12px; margin-bottom: 5px; font-weight: 500;">
													설명
												</label>
												<textarea class="form-control" id="sqlTemplateDesc" rows="3"
													placeholder="이 템플릿의 용도와 사용법을 설명하세요"></textarea>
											</div>
										</div>
										
									</div>
									
									<!-- 숨겨진 상태 필드 (JavaScript에서 체크박스와 동기화) -->
									<select class="form-control" id="sqlTemplateStatus" style="display: none;">
										<option value="ACTIVE">활성</option>
										<option value="INACTIVE">비활성</option>
									</select>
								</div>
							</div>


							<!-- 파라미터 관리 카드 (컴팩트) -->
							<div class="panel panel-default" style="margin-bottom: 15px;">
								<div class="panel-heading" style="padding: 8px 15px;">
									<h4 class="panel-title" style="font-size: 14px; margin: 0;">
										<i class="fa fa-sliders"></i> 파라미터 관리
									</h4>
								</div>
								<div class="panel-body" style="padding: 10px 15px;">
									<div class="table-responsive parameter-table-container">
										<table class="table table-bordered align-middle table-condensed"
											id="parameterTable">
											<thead>
												<tr>
												<th style="width: 50px; font-size: 11px;"><div data-toggle="tooltip" data-placement="top" title="파라미터의 입력 순서를 설정합니다. 숫자가 작을수록 먼저 입력받으며, 사용자 입력 화면에서도 이 순서대로 표시됩니다.">순서</div></th>
												<th style="width: 100px; font-size: 11px;"><div data-toggle="tooltip" data-placement="top" title="SQL 내에서 사용할 파라미터 이름입니다. SQL 문에서 \${파라미터명} 형태로 사용되며, 실행 시 실제 값으로 치환됩니다.">파라미터명</div></th>
												<th style="width: 120px; font-size: 11px;"><div data-toggle="tooltip" data-placement="top" title="파라미터에 대한 설명을 입력합니다. 사용자가 입력할 때 도움말로 표시되며, 올바른 값을 입력할 수 있도록 안내합니다.">설명</div></th>
												<th style="width: 70px; font-size: 11px;"><div data-toggle="tooltip" data-placement="top" title="파라미터의 데이터 타입을 설정합니다. 문자열: 문자열 바인딩, 숫자: 숫자 바인딩, 텍스트: 긴 문자열용, SQL: SQL 코드 조각, 로그: 로깅용(바인딩 안됨)">타입</div></th>
												<th style="width: 80px; font-size: 11px;"><div data-toggle="tooltip" data-placement="top" title="파라미터의 기본값을 설정합니다.">기본값</div></th>
												<th style="width: 40px; font-size: 11px;"><div data-toggle="tooltip" data-placement="top" title="파라미터가 반드시 입력되어야 하는지 설정합니다. 체크하면 사용자가 값을 입력하지 않으면 SQL 실행이 차단됩니다.">필수</div></th>
												<th style="width: 40px; font-size: 11px;"><div data-toggle="tooltip" data-placement="top" title="파라미터를 읽기 전용으로 설정합니다. 체크하면 사용자가 값을 수정할 수 없으며, 기본값이나 시스템에서 설정된 값만 사용됩니다.">읽기전용</div></th>
												<th style="width: 40px; font-size: 11px;"><div data-toggle="tooltip" data-placement="top" title="파라미터 입력 필드를 화면에서 숨깁니다. 체크하면 사용자에게 표시되지 않지만, 기본값이나 시스템 값이 SQL에 전달됩니다.">숨김</div></th>
												<th style="width: 40px; font-size: 11px;"><div data-toggle="tooltip" data-placement="top" title="파라미터를 비활성화합니다. 체크하면 입력 필드가 비활성화되어 사용자가 값을 입력할 수 없으며, SQL 실행에서도 제외됩니다.">비활성화</div></th>
												<th style="width: 40px; font-size: 11px;">삭제</th>
											</tr>
											</thead>
											<tbody id="parameterTableBody">
												<!-- 파라미터들이 여기에 동적으로 추가됩니다 -->
											</tbody>
										</table>
									</div>
									<button type="button" class="btn btn-primary btn-sm" onclick="addParameter()">
										<i class="fa fa-plus"></i> 파라미터 추가
									</button>
								</div>
							</div>

							<!-- 단축키 관리 카드 (컴팩트) -->
							<div class="panel panel-default" style="margin-bottom: 15px;">
								<div class="panel-heading" style="padding: 8px 15px;">
									<h4 class="panel-title" style="font-size: 14px; margin: 0;">
										<i class="fa fa-keyboard-o"></i> 단축키 관리
									</h4>
								</div>
								<div class="panel-body" style="padding: 10px 15px;">
									<div class="table-responsive">
									<table class="table table-bordered table-condensed"
										id="shortcutTable">
										<thead>
											<tr>
											<th style="width: 50px; font-size: 11px;"><div data-toggle="tooltip" data-placement="top" title="키보드 단축키를 설정합니다. F1~F12 키 중에서 선택하여 빠른 SQL 실행이 가능합니다.">단축키</div></th>	
											<th style="width: 120px; font-size: 11px;"><div data-toggle="tooltip" data-placement="top" title="단축키에 대한 설명적인 이름을 입력합니다.">단축키명</div></th>
											<th style="width: 80px; font-size: 11px;"><div data-toggle="tooltip" data-placement="top" title="단축키를 눌렀을 때 실행할 SQL 템플릿을 선택합니다.">대상 템플릿</div></th>
											<th style="width: 150px; font-size: 11px;"><div data-toggle="tooltip" data-placement="top" title="단축키에 대한 상세한 설명을 입력합니다.">설명</div></th>
											<th style="width: 80px; font-size: 11px;"><div data-toggle="tooltip" data-placement="top" title="단축키 실행 시 파라미터로 전달할 컬럼의 인덱스를 설정합니다. 1,2,3 형태로 여러 컬럼을 지정할 수 있습니다.">소스 컬럼</div></th>
											<th style="width: 50px; font-size: 11px;"><div data-toggle="tooltip" data-placement="top" title="단축키를 자동으로 실행할지 설정합니다. 체크하면 조건이 만족될 때 자동으로 SQL이 실행됩니다다.">자동실행</div></th>
											<th style="width: 50px; font-size: 11px;"><div data-toggle="tooltip" data-placement="top" title="단축키의 활성화 상태를 설정합니다. 활성으로 설정하면 단축키가 사용 가능하며, 비활성으로 설정하면 사용할 수 없습니다.">상태</div></th>
											<th style="width: 50px; font-size: 11px;">삭제</th>
										</tr>
										</thead>
										<tbody id="shortcutTableBody">
											<!-- 단축키들이 여기에 동적으로 추가됩니다 -->
										</tbody>
									</table>
								</div>
								<button type="button" class="btn btn-success btn-sm" onclick="addShortcut()">
									<i class="fa fa-plus"></i> 단축키 추가
								</button>
							</div>
						</div>

							<!-- DB별 SQL 내용 관리 카드 (컴팩트) -->
							<div class="panel panel-default" style="margin-bottom: 15px;">
								<div class="panel-heading" style="padding: 8px 15px;">
									<h4 class="panel-title" style="font-size: 14px; margin: 0;">
										<i class="fa fa-database"></i> DB별 SQL 내용
									</h4>
								</div>
								<div class="panel-body" style="padding: 10px 15px;">

								<!-- DB 연결 탭 -->
								<ul class="nav nav-tabs" id="sqlContentTabs">
									<!-- 기본 템플릿 탭 (항상 첫 번째) -->
									<li class="nav-item active"><a class="nav-link" data-toggle="tab"
											href="#tab-default"> 기본 템플릿 </a></li>
									<!-- 추가 DB 연결 탭들이 여기에 동적으로 생성됩니다 -->
								</ul>

								<!-- SQL 내용 탭 컨텐츠 -->
								<div class="tab-content" id="sqlContentTabContent">
									<!-- 기본 템플릿 컨텐츠 (항상 첫 번째) -->
									<div class="tab-pane active" id="tab-default">
										<div class="sql-editor-container" data-connection-id="default"
											data-template-id="${templateId}">
											<div id="sqlEditor_default" class="sql-editor"
												style="height: 300px; border: 1px solid #ccc;"></div>
											<div class="sql-editor-toggle" style="text-align: center; margin-top: 5px;">
											<button type="button" class="btn btn-sm btn-default" id="toggleSqlEditor" style="border-radius: 50%; width: 30px; height: 30px; padding: 0;">
													<i class="fa fa-chevron-down"></i>
												</button>
											</div>
										</div>
									</div>
									<!-- 추가 DB 연결 SQL 에디터가 여기에 동적으로 생성됩니다 -->
								</div>
								</div>
							</div>

							<!-- 하단 액션 버튼들 -->
							<div class="panel-default" style="margin-bottom: 15px;">
								<div class="panel-body" style="padding: 15px; text-align: right;">
									<button type="button" class="btn btn-default" id="goToTemplateBtnBottom"
										onclick="goToTemplate()" disabled>
										<i class="fa fa-external-link"></i> 해당 메뉴로 이동
									</button>
									<button type="button" class="btn btn-success" onclick="saveSqlTemplate()" style="margin-left: 10px;">
										<i class="fa fa-save"></i> 저장
									</button>
									<button type="button" class="btn btn-danger" onclick="deleteSqlTemplate()" style="margin-left: 10px;">
										<i class="fa fa-trash"></i> 삭제
									</button>
								</div>
							</div>

							<!-- 테스트 결과 -->
							<div id="testResult"></div>
							</form>
							<!-- 템플릿 폼 끝 -->
						</div>
					</div>
				</div>
		</section>
	</div>

	<!-- 로딩 오버레이 -->
	<div id="loadingOverlay" class="loading-overlay">
		<div class="loading-spinner">
			<div class="spinner"></div>
			<div class="loading-text">로딩 중...</div>
		</div>
	</div>

	<!-- 카테고리 모달 -->
	<div class="modal fade" id="categoryModal" tabindex="-1" role="dialog" aria-labelledby="categoryModalLabel"
		aria-hidden="true">
		<div class="modal-dialog" role="document">
			<div class="modal-content">
				<div class="modal-header">
					<h5 class="modal-title" id="categoryModalTitle">카테고리 관리</h5>
					<button type="button" class="close" data-dismiss="modal" aria-label="Close">
						<span aria-hidden="true">&times;</span>
					</button>
				</div>
				<div class="modal-body">
					<form id="categoryForm">
						<input type="hidden" id="categoryId">
						<div class="form-group">
							<label for="categoryName" data-toggle="tooltip" data-placement="top"
								title="SQL 템플릿을 분류할 카테고리의 이름을 입력합니다. 고유한 이름이어야 하며, 기존 카테고리와 중복되지 않아야 합니다.">카테고리
								이름</label> <input type="text" class="form-control" id="categoryName" required>
						</div>
						<div class="form-group">
							<label for="categoryDescription" data-toggle="tooltip" data-placement="top"
								title="카테고리에 대한 설명을 입력합니다. 해당 카테고리에 어떤 종류의 SQL 템플릿들이 포함되는지 명확하게 작성해주세요.">설명
								(선택 사항)</label>
							<textarea class="form-control" id="categoryDescription" rows="3"></textarea>
						</div>
					</form>
				</div>
				<div class="modal-footer">
					<button type="button" class="btn btn-default" data-dismiss="modal">취소</button>
					<button type="button" class="btn btn-primary" id="categoryModalSaveBtn"
						onclick="saveCategory()">저장</button>
				</div>
			</div>
		</div>
	</div>


	<!-- SQL 에디터 접기/펼치기 기능 -->
	<script>
		$(document).ready(function () {
			$('#toggleSqlEditor').on('click', function () {
				var editor = $('#sqlEditor_default');
				var button = $(this);
				var icon = button.find('i');

				if (icon.hasClass('fa-chevron-down')) {
					// 펼치기
					editor.css('height', '70vh');
					icon.removeClass('fa-chevron-down').addClass('fa-chevron-up');
				} else {
					// 접기
					editor.css('height', '300px');
					icon.removeClass('fa-chevron-up').addClass('fa-chevron-down');
				}

				// Ace 에디터 리사이즈
				if (typeof ace !== 'undefined') {
					try {
						var aceEditorElement = document.getElementById('sqlEditor_default');
						if (aceEditorElement && aceEditorElement.classList.contains('ace_editor')) {
							var aceEditor = ace.edit('sqlEditor_default');
							if (aceEditor && typeof aceEditor.resize === 'function') {
								aceEditor.resize();
							}
						}
					} catch (e) {
						// Ace editor resize 실패 시 무시
					}
				}
			});
		});
	</script>