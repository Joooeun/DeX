<%@include file="common/common.jsp"%>

<!-- Ace Editor CDN -->
<script src="https://cdnjs.cloudflare.com/ajax/libs/ace/1.23.0/ace.js"></script>
<script
	src="https://cdnjs.cloudflare.com/ajax/libs/ace/1.23.0/ext-language_tools.js"></script>

<!-- Select2 CDN for searchable dropdowns -->
<link href="https://cdnjs.cloudflare.com/ajax/libs/select2/4.0.13/css/select2.min.css" rel="stylesheet" />
<script src="https://cdnjs.cloudflare.com/ajax/libs/select2/4.0.13/js/select2.min.js"></script>

<style>
.category-list {
	max-height: 40vh;
	overflow-y: auto;
	overflow-x: hidden;
}

.category-item {
	cursor: pointer;
	padding: 5px;
	border: 1px solid #ddd;
	margin-bottom: 3px;
	border-radius: 3px;
	background-color: #f9f9f9;
	word-wrap: break-word;
	white-space: normal;
}

.category-item:hover {
	background-color: #e9ecef;
}

.category-item.selected {
	background-color: #007bff;
	color: white;
}

.template-list {
	max-height: 40vh;
	overflow-y: auto;
	overflow-x: hidden;
}

.template-item {
	cursor: pointer;
	padding: 8px;
	border: 1px solid #ddd;
	margin-bottom: 3px;
	border-radius: 3px;
	background-color: #fff;
	word-wrap: break-word;
	white-space: normal;
}

.template-item:hover {
	background-color: #f8f9fa;
}

.template-item.selected {
	background-color: #28a745;
	color: white;
}

.modal-header {
	background-color: #007bff;
	color: white;
}

.sql-preview {
	background-color: #f8f9fa;
	border: 1px solid #dee2e6;
	border-radius: 5px;
	padding: 10px;
	max-height: 300px;
	overflow-y: auto;
	font-family: 'Courier New', monospace;
	font-size: 12px;
	white-space: pre-wrap;
	word-wrap: break-word;
	line-height: 1.4;
}

.template-count {
	font-size: 11px;
	margin-bottom: 5px;
	display: inline-block;
	vertical-align: middle;
}

.category-checkboxes .checkbox {
	margin-bottom: 3px;
}

.category-checkboxes .checkbox label {
	font-size: 12px;
	line-height: 1.2;
	word-wrap: break-word;
}

.bg-gray {
	background-color: #6c757d !important;
	color: white;
}

.bg-blue {
	background-color: #007bff !important;
	color: white;
}

/* 파라미터 순서 변경 버튼 스타일 */
.move-up, .move-down {
	padding: 2px 4px;
	margin: 0 2px;
	font-size: 10px;
}

/* 부트스트랩 툴팁 커스터마이징 */
.tooltip-inner {
	max-width: 300px;
	text-align: left;
	font-size: 12px;
	line-height: 1.4;
}

/* 툴팁이 있는 요소들의 커서 스타일 */
[data-toggle="tooltip"] {
	cursor: help;
}

.category-icon {
	cursor: pointer;
	font-size: 14px;
	padding: 3px;
	border-radius: 3px;
	transition: all 0.2s ease;
	vertical-align: middle;
	display: inline-block;
}

.edit-icon {
	color: #007bff;
}

.delete-icon {
	color: #dc3545;
}
</style>

<script>
	$(document).ready(function() {
		loadCategories();
		initSqlEditor();
		
		// 부트스트랩 툴팁 초기화
		$('[data-toggle="tooltip"]').tooltip({
			placement: 'top',
			trigger: 'hover'
		});
	});

	// 카테고리 목록 로드
	function loadCategories() {
		$.ajax({
			type : 'GET',
			url : '/SQLTemplate/category/list',
			success : function(result) {
				if (result.success) {
					renderCategories(result.data);
					loadCategoryCheckboxes();
				}
			}
		});
	}

	// 카테고리 렌더링
	function renderCategories(categories) {
		var container = $('#categoryList');
		container.empty();

		// 미분류 카테고리 추가
		var uncategorizedItem = $('<div class="category-item" data-id="UNCATEGORIZED" onclick="selectCategory(\'UNCATEGORIZED\')">'
				+ '<div class="row">'
				+ '<div class="col-md-8">'
				+ '<strong>📁 미분류</strong><br>'
				+ '<small>카테고리가 지정되지 않은 템플릿</small>'
				+ '</div>'
				+ '<div class="col-md-4 text-right" style="display: flex; align-items: center; justify-content: flex-end;">'
				+ '<span class="badge bg-gray template-count" id="count-UNCATEGORIZED">0</span>'
				+ '</div>' + '</div>' + '</div>');
		container.append(uncategorizedItem);

		if (categories && categories.length > 0) {
			categories
					.forEach(function(category) {
						var item = $('<div class="category-item" data-id="'
								+ category.CATEGORY_ID
								+ '" onclick="selectCategory(\''
								+ category.CATEGORY_ID
								+ '\')">'
								+ '<div class="row">'
								+ '<div class="col-md-8">'
								+ '<strong>'
								+ category.CATEGORY_NAME
								+ '</strong><br>'
								+ '<small>'
								+ (category.CATEGORY_DESCRIPTION || '설명 없음')
								+ '</small>'
								+ '</div>'
								+ '<div class="col-md-4 text-right" style="display: flex; align-items: center; justify-content: flex-end;">'
								+ '<span class="badge bg-blue template-count" id="count-' + category.CATEGORY_ID + '">0</span>&nbsp;'
								+ '<i class="fa fa-edit category-icon edit-icon" onclick="event.stopPropagation(); editCategory(\''
								+ category.CATEGORY_ID
								+ '\')" title="수정"></i>&nbsp;'
								+ '<i class="fa fa-trash category-icon delete-icon" onclick="event.stopPropagation(); deleteCategory(\''
								+ category.CATEGORY_ID + '\')" title="삭제"></i>'
								+ '</div>' + '</div>' + '</div>');
						container.append(item);
					});
		}

		// 각 카테고리의 템플릿 개수 로드
		loadCategoryTemplateCounts();
		selectCategory('UNCATEGORIZED');
	}

	// 카테고리별 템플릿 개수 로드
	function loadCategoryTemplateCounts() {
		// 미분류 템플릿 개수 로드
		$.ajax({
			type : 'GET',
			url : '/SQLTemplate/category/templates',
			data : {
				categoryId : 'UNCATEGORIZED'
			},
			success : function(result) {
				if (result.success) {
					var count = result.data ? result.data.length : 0;
					$('#count-UNCATEGORIZED').text(count);
				}
			}
		});

		// 각 카테고리별 템플릿 개수 로드
		$
				.ajax({
					type : 'GET',
					url : '/SQLTemplate/category/list',
					success : function(result) {
						if (result.success && result.data) {
							result.data
									.forEach(function(category) {
										$
												.ajax({
													type : 'GET',
													url : '/SQLTemplate/category/templates',
													data : {
														categoryId : category.CATEGORY_ID
													},
													success : function(
															templateResult) {
														if (templateResult.success) {
															var count = templateResult.data ? templateResult.data.length
																	: 0;
															$(
																	'#count-'
																			+ category.CATEGORY_ID)
																	.text(count);
														}
													}
												});
									});
						}
					}
				});
	}

	// 카테고리 선택
	function selectCategory(categoryId) {
		$('.category-item').removeClass('selected');
		$('[data-id="' + categoryId + '"]').addClass('selected');
		loadTemplatesByCategory(categoryId);
	}

	// 카테고리별 템플릿 로드
	function loadTemplatesByCategory(categoryId) {
		$.ajax({
			type : 'GET',
			url : '/SQLTemplate/category/templates',
			data : {
				categoryId : categoryId
			},
			success : function(result) {
				if (result.success) {
					renderTemplates(result.data);
				}
			}
		});
	}

	// 템플릿 렌더링
	function renderTemplates(templates) {
		var container = $('#templateList');
		container.empty();

		if (templates && templates.length > 0) {
			templates.forEach(function(template) {
				var item = $('<div class="template-item" data-id="'
						+ template.TEMPLATE_ID + '" onclick="selectTemplate(\''
						+ template.TEMPLATE_ID + '\')">' + '<div class="row">'
						+ '<div class="col-md-12">' + '<strong>'
						+ template.TEMPLATE_NAME + '</strong>'
						+ '<small style="float:right;">생성일: '
						+ formatDate(template.CREATED_TIMESTAMP) + '</small>'
						+ '</div>' + '</div>' + '</div>');
				container.append(item);
			});
		} else {
			container
					.html('<div class="text-muted text-center" style="padding: 20px;">템플릿이 없습니다.</div>');
		}
	}

	// 템플릿 선택
	function selectTemplate(templateId) {
		$('.template-item').removeClass('selected');
		$('[data-id="' + templateId + '"]').addClass('selected');
		loadSqlTemplateDetail(templateId);
	}

	// 카테고리 옵션 로드 (체크박스용)
	function loadCategoryCheckboxes() {
		$
				.ajax({
					type : 'GET',
					url : '/SQLTemplate/category/list',
					success : function(result) {
						if (result.success) {
							var container = $('#sqlTemplateCategories');
							container.empty();

							// 3열 그리드로 카테고리 배치
							var row = $('<div class="row" style="margin: 0;"></div>');
							container.append(row);

							result.data
									.forEach(function(category, index) {
										var col = $('<div class="col-md-4" style="padding: 2px;"></div>');
										var checkbox = $('<div class="checkbox" style="margin: 0; font-size: 12px;">'
												+ '<label style="margin: 0; cursor: pointer;">'
												+ '<input type="checkbox" name="categoryIds" value="' + category.CATEGORY_ID + '" style="margin-right: 3px;"> '
												+ category.CATEGORY_NAME
												+ '</label>' + '</div>');
										col.append(checkbox);
										row.append(col);
									});
						}
					}
				});
	}

	// 선택된 카테고리 ID들 가져오기
	function getSelectedCategoryIds() {
		var selectedIds = [];
		$('input[name="categoryIds"]:checked').each(function() {
			selectedIds.push($(this).val());
		});
		return selectedIds;
	}

	// 템플릿의 카테고리 정보 로드 (체크박스용)
	function loadTemplateCategories(templateId) {
		$
				.ajax({
					type : 'GET',
					url : '/SQLTemplate/category/list',
					success : function(result) {
						if (result.success) {
							$('input[name="categoryIds"]').prop('checked',
									false);

							result.data
									.forEach(function(category) {
										$
												.ajax({
													type : 'GET',
													url : '/SQLTemplate/category/templates',
													data : {
														categoryId : category.CATEGORY_ID
													},
													async : false,
													success : function(
															templateResult) {
														if (templateResult.success) {
															var hasTemplate = templateResult.data
																	.some(function(
																			template) {
																		return template.TEMPLATE_ID === templateId;
																	});
															if (hasTemplate) {
																$(
																		'input[name="categoryIds"][value="'
																				+ category.CATEGORY_ID
																				+ '"]')
																		.prop(
																				'checked',
																				true);
															}
														}
													}
												});
									});
						}
					}
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
		$.ajax({
			type : 'GET',
			url : '/SQLTemplate/category/detail',
			data : {
				categoryId : categoryId
			},
			success : function(result) {
				if (result.success) {
					var category = result.data;
					$('#categoryModal').modal('show');
					$('#categoryModalTitle').text('카테고리 수정');
					$('#categoryId').val(category.CATEGORY_ID);
					$('#categoryName').val(category.CATEGORY_NAME);
					$('#categoryDescription')
							.val(category.CATEGORY_DESCRIPTION);
					$('#categoryModalSaveBtn').text('수정');
				} else {
					alert('카테고리 정보 로드 실패: ' + result.message);
				}
			}
		});
	}

	// 카테고리 저장 (생성/수정)
	function saveCategory() {
		var categoryId = $('#categoryId').val();
		var categoryName = $('#categoryName').val();
		var description = $('#categoryDescription').val();

		if (!categoryName.trim()) {
			alert('카테고리명을 입력해주세요.');
			return;
		}

		var url = categoryId ? '/SQLTemplate/category/update'
				: '/SQLTemplate/category/create';
		var data = categoryId ? {
			categoryId : categoryId,
			categoryName : categoryName,
			description : description
		} : {
			categoryName : categoryName,
			description : description
		};

		$.ajax({
			type : 'POST',
			url : url,
			data : data,
			success : function(result) {
				if (result.success) {
					alert(result.message);
					$('#categoryModal').modal('hide');
					loadCategories();
				} else {
					alert('저장 실패: ' + result.error);
				}
			}
		});
	}

	// 카테고리 삭제
	function deleteCategory(categoryId) {
		if (!confirm('정말로 이 카테고리를 삭제하시겠습니까?')) {
			return;
		}

		$.ajax({
			type : 'POST',
			url : '/SQLTemplate/category/delete',
			data : {
				categoryId : categoryId
			},
			success : function(result) {
				if (result.success) {
					alert(result.message);
					loadCategories();
				} else {
					alert('삭제 실패: ' + result.error);
				}
			}
		});
	}

	// 날짜 포맷팅
	function formatDate(timestamp) {
		if (!timestamp)
			return '';
		var date = new Date(timestamp);
		return date.toLocaleDateString() + ' ' + date.toLocaleTimeString();
	}

	// SQL 에디터 초기화
	function initSqlEditor() {
		// Ace Editor가 로드될 때까지 대기
		var checkAce = setInterval(function() {
			if (typeof ace !== 'undefined') {
				clearInterval(checkAce);
				try {
					ace.require("ace/ext/language_tools");
					var sqlEditor = ace.edit("sqlEditor");
					sqlEditor.setTheme("ace/theme/chrome");
					sqlEditor.session.setMode("ace/mode/sql");
					sqlEditor.setOptions({
						enableBasicAutocompletion : true,
						enableSnippets : true,
						enableLiveAutocompletion : true
					});
					window.sqlEditor = sqlEditor;
					console.log("Ace Editor 초기화 완료");
				} catch (e) {
					console.log("Ace Editor 초기화 실패:", e);
					initTextareaEditor();
				}
			}
		}, 100);

		// 5초 후에도 로드되지 않으면 textarea 사용
		setTimeout(function() {
			if (typeof ace === 'undefined') {
				clearInterval(checkAce);
				console.log("Ace Editor 로드 타임아웃, textarea 사용");
				initTextareaEditor();
			}
		}, 5000);
	}

	// Textarea 기반 SQL 에디터 초기화
	function initTextareaEditor() {
		var sqlEditorDiv = document.getElementById("sqlEditor");
		sqlEditorDiv.innerHTML = '<textarea id="sqlTextarea" style="width: 100%; height: 100%; font-family: monospace; font-size: 14px; border: none; resize: none; outline: none;"></textarea>';
		window.sqlEditor = {
			getValue : function() {
				return document.getElementById("sqlTextarea").value;
			},
			setValue : function(value) {
				document.getElementById("sqlTextarea").value = value || '';
			}
		};
	}

	// SQL 미리보기 업데이트
	function updateSqlPreview() {
		var sqlContent = '';
		if (window.sqlEditor && window.sqlEditor.getValue) {
			sqlContent = window.sqlEditor.getValue();
		} else {
			sqlContent = $('#sqlTextarea').val() || $('#sqlEditor').val();
		}
		$('#sqlPreview').text(sqlContent);
	}

	// 파라미터 추가
	function addParameter() {
		var currentOrder = $('#parameterTableBody tr').length + 1;
		var row = $('<tr class="parameter-row">'
				+ '<td><div class="text-center">'
				+ '<button type="button" class="btn btn-xs btn-default move-up" title="위로"><i class="fa fa-chevron-up"></i></button><br> '
				+ '<button type="button" class="btn btn-xs btn-default move-down" title="아래로"><i class="fa fa-chevron-down"></i></button>'
				+ '<input type="hidden" class="parameter-order" value="' + currentOrder + '">'
				+ '</div></td>'
				+ '<td><input type="text" class="form-control parameter-name" placeholder="파라미터명"></td>'
				+ '<td><input type="text" class="form-control parameter-description" placeholder="설명"></td>'
				+ '<td><select class="form-control parameter-type">'
				+ '<option value="STRING">문자열</option>'
				+ '<option value="NUMBER">숫자</option>'
				+ '<option value="DATE">날짜</option>'
				+ '<option value="BOOLEAN">불린</option>'
				+ '<option value="TEXT">텍스트</option>'
				+ '<option value="SQL">SQL</option>'
				+ '</select></td>'
				+ '<td><input type="text" class="form-control parameter-default" placeholder="기본값"></td>'
				+ '<td><div class="text-center"><input type="checkbox" class="parameter-required"></div></td>'
				+ '<td><div class="text-center"><input type="checkbox" class="parameter-readonly"></div></td>'
				+ '<td><div class="text-center"><input type="checkbox" class="parameter-hidden"></div></td>'
				+ '<td><div class="text-center"><input type="checkbox" class="parameter-disabled"></div></td>'
				+ '<td><button type="button" class="btn btn-danger btn-sm" onclick="removeParameter(this)">삭제</button></td>'
				+ '</tr>');
		$('#parameterTableBody').append(row);
		
		// 새로 추가된 행의 툴팁 초기화
		row.find('[data-toggle="tooltip"]').tooltip({
			placement: 'top',
			trigger: 'hover'
		});
		
		// 파라미터 속성 변경 이벤트 리스너 추가
		row.find('.parameter-hidden').on('change', function() {
			var isHidden = $(this).is(':checked');
			var requiredCheckbox = $(this).closest('tr').find('.parameter-required');
			
			// 숨김 필드면 자동으로 필수로 설정
			if (isHidden) {
				requiredCheckbox.prop('checked', true);
			}
		});
		
		// 순서 변경 버튼 이벤트 리스너 추가
		row.find('.move-up').on('click', function() {
			moveParameterUp($(this).closest('tr'));
		});
		
		row.find('.move-down').on('click', function() {
			moveParameterDown($(this).closest('tr'));
		});
	}

	// 파라미터 삭제
	function removeParameter(button) {
		$(button).closest('tr').remove();
		reorderParameters();
	}

	// 파라미터 순서 재정렬
	function reorderParameters() {
		$('#parameterTableBody tr').each(function(index) {
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

	// 파라미터 목록 로드
	function loadParameters(templateId) {
		if (!templateId) {
			$('#parameterTableBody').empty();
			return;
		}

		$.ajax({
			type : 'GET',
			url : '/SQLTemplate/parameters',
			data : {
				templateId : templateId
			},
			success : function(result) {
				if (result.success) {
					renderParameters(result.data);
				} else {
					$('#parameterTableBody').empty();
				}
			}
		});
	}

	// 파라미터 렌더링
	function renderParameters(parameters) {
		var tbody = $('#parameterTableBody');
		tbody.empty();

		if (parameters && parameters.length > 0) {
			parameters
					.forEach(function(param, index) {
						var order = param.PARAMETER_ORDER || (index + 1);
						var row = $('<tr class="parameter-row">'
								+ '<td><div class="text-center">'
								+ '<button type="button" class="btn btn-xs btn-default move-up" title="위로"><i class="fa fa-chevron-up"></i></button><br> '
								+ '<button type="button" class="btn btn-xs btn-default move-down" title="아래로"><i class="fa fa-chevron-down"></i></button>'
								+ '<input type="hidden" class="parameter-order" value="' + order + '">'
								+ '</div></td>'
								+ '<td><input type="text" class="form-control parameter-name" value="'
								+ (param.PARAMETER_NAME || '')
								+ '" placeholder="파라미터명"></td>'
								+ '<td><input type="text" class="form-control parameter-description" value="'
								+ (param.DESCRIPTION || '')
								+ '" placeholder="설명"></td>'
								+ '<td><select class="form-control parameter-type">'
								+ '<option value="STRING"'
								+ (param.PARAMETER_TYPE === 'STRING' ? ' selected'
										: '')
								+ '>문자열</option>'
								+ '<option value="NUMBER"'
								+ (param.PARAMETER_TYPE === 'NUMBER' ? ' selected'
										: '')
								+ '>숫자</option>'
								+ '<option value="DATE"'
								+ (param.PARAMETER_TYPE === 'DATE' ? ' selected'
										: '')
								+ '>날짜</option>'
								+ '<option value="BOOLEAN"'
								+ (param.PARAMETER_TYPE === 'BOOLEAN' ? ' selected'
										: '')
								+ '>불린</option>'
								+ '<option value="TEXT"'
								+ (param.PARAMETER_TYPE === 'TEXT' ? ' selected'
										: '')
								+ '>텍스트</option>'
								+ '<option value="SQL"'
								+ (param.PARAMETER_TYPE === 'SQL' ? ' selected'
										: '')
								+ '>SQL</option>'
								+ '</select></td>'
								+ '<td><input type="text" class="form-control parameter-default" value="'
								+ (param.DEFAULT_VALUE || '')
								+ '" placeholder="기본값"></td>'
								+ '<td><div class="text-center"><input type="checkbox" class="parameter-required"'
								+ (param.IS_REQUIRED ? ' checked' : '')
								+ '></div></td>'
								+ '<td><div class="text-center"><input type="checkbox" class="parameter-readonly"'
								+ (param.PARAMETER_READONLY ? ' checked' : '')
								+ '></div></td>'
								+ '<td><div class="text-center"><input type="checkbox" class="parameter-hidden"'
								+ (param.IS_HIDDEN ? ' checked' : '')
								+ '></div></td>'
								+ '<td><div class="text-center"><input type="checkbox" class="parameter-disabled"'
								+ (param.IS_DISABLED ? ' checked' : '')
								+ '></div></td>'
								+ '<td><button type="button" class="btn btn-danger btn-sm" onclick="removeParameter(this)">삭제</button></td>'
								+ '</tr>');
						tbody.append(row);
						
						// 새로 추가된 행의 툴팁 초기화
						row.find('[data-toggle="tooltip"]').tooltip({
							placement: 'top',
							trigger: 'hover'
						});
						
						// 파라미터 속성 변경 이벤트 리스너 추가
						row.find('.parameter-hidden').on('change', function() {
							var isHidden = $(this).is(':checked');
							var requiredCheckbox = $(this).closest('tr').find('.parameter-required');
							
							// 숨김 필드면 자동으로 필수로 설정
							if (isHidden) {
								requiredCheckbox.prop('checked', true);
							}
						});
						
						// 순서 변경 버튼 이벤트 리스너 추가
						row.find('.move-up').on('click', function() {
							moveParameterUp($(this).closest('tr'));
						});
						
						row.find('.move-down').on('click', function() {
							moveParameterDown($(this).closest('tr'));
						});
					});
		}
	}

	// 파라미터 데이터 수집
	function collectParameters() {
		var parameters = [];
		$('#parameterTableBody tr').each(
				function() {
					var name = $(this).find('.parameter-name').val();
					if (name && name.trim()) {
						parameters.push({
							name : name.trim(),
							type : $(this).find('.parameter-type').val(),
							defaultValue : $(this).find('.parameter-default')
									.val(),
							required : $(this).find('.parameter-required').is(':checked'),
							order : parseInt($(this).find('.parameter-order')
									.val()) || 1,

							description : $(this).find('.parameter-description').val(),
							readonly : $(this).find('.parameter-readonly').is(':checked'),
							hidden : $(this).find('.parameter-hidden').is(':checked'),
							disabled : $(this).find('.parameter-disabled').is(':checked')
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
		
		var sqlContent = '';
		if (window.sqlEditor && window.sqlEditor.getValue) {
			sqlContent = window.sqlEditor.getValue();
		} else {
			sqlContent = $('#sqlEditor').val();
		}
		
		if (!sqlContent.trim()) {
			errors.push('SQL 내용을 입력해주세요.');
		}
		
		var executionLimit = parseInt($('#sqlExecutionLimit').val());
		if (isNaN(executionLimit) || executionLimit < 1 || executionLimit > 100000) {
			errors.push('실행 제한은 1~100,000 사이의 숫자여야 합니다.');
		}
		
		var refreshTimeout = parseInt($('#sqlRefreshTimeout').val());
		if (isNaN(refreshTimeout) || refreshTimeout < 1 || refreshTimeout > 3600) {
			errors.push('새로고침 타임아웃은 1~3600초 사이의 숫자여야 합니다.');
		}
		
		// 파라미터 벨리데이션
		var parameters = collectParameters();
		var parameterNames = [];
		var duplicateNames = [];
		
		parameters.forEach(function(param, index) {
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
				switch(param.type) {
					case 'NUMBER':
						if (isNaN(param.defaultValue)) {
							errors.push('숫자 타입 파라미터의 기본값은 숫자여야 합니다. (' + param.name + ')');
						}
						break;
					case 'DATE':
						if (!isValidDate(param.defaultValue)) {
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
		var shortcuts = collectShortcuts();
		var shortcutKeys = [];
		var duplicateShortcuts = [];
		
		shortcuts.forEach(function(shortcut, index) {
			if (!shortcut.key || !shortcut.key.trim()) {
				errors.push('단축키를 입력해주세요. (순서: ' + (index + 1) + ')');
			} else if (!/^F[1-9]|F1[0-2]$/.test(shortcut.key)) {
				errors.push('단축키는 F1~F12 중 하나여야 합니다. (' + shortcut.key + ')');
			}
			
			if (!shortcut.name || !shortcut.name.trim()) {
				errors.push('단축키명을 입력해주세요. (순서: ' + (index + 1) + ')');
			}
			
			if (!shortcut.targetTemplate || !shortcut.targetTemplate.trim()) {
				errors.push('대상 템플릿을 선택해주세요. (순서: ' + (index + 1) + ')');
			}
			
			// 중복 단축키 체크
			if (shortcut.key && shortcut.key.trim()) {
				if (shortcutKeys.indexOf(shortcut.key) !== -1) {
					duplicateShortcuts.push(shortcut.key);
				} else {
					shortcutKeys.push(shortcut.key);
				}
			}
		});
		
		if (duplicateShortcuts.length > 0) {
			errors.push('중복된 단축키가 있습니다: ' + duplicateShortcuts.join(', '));
		}
		
		// 에러가 있으면 알림
		if (errors.length > 0) {
			alert('다음 오류를 수정해주세요:\n\n' + errors.join('\n'));
			return false;
		}
		
		return true;
	}
	
	// 날짜 유효성 검사
	function isValidDate(dateString) {
		var date = new Date(dateString);
		return date instanceof Date && !isNaN(date);
	}

	// 파라미터를 설정 문자열로 변환 (기존 호환성)
	function parametersToConfigString(parameters) {
		var configLines = [];
		parameters.forEach(function(param) {
			configLines.push(param.name + '=' + (param.defaultValue || ''));
		});
		return configLines.join('\n');
	}

	// 단축키 추가
	function addShortcut() {
		var row = $('<tr class="shortcut-row">'
				+ '<td><input type="text" class="form-control shortcut-key" placeholder="F1" readonly></td>'
				+ '<td><input type="text" class="form-control shortcut-name" placeholder="단축키명"></td>'
				+ '<td><select class="form-control target-template-select2">'
				+ '<option value="">대상 템플릿 선택</option>'
				+ '</select></td>'
				+ '<td><input type="text" class="form-control shortcut-description" placeholder="단축키 설명"></td>'
				+ '<td><input type="text" class="form-control source-columns" placeholder="1,2,3"></td>'
				+ '<td><select class="form-control auto-execute">'
				+ '<option value="true">예</option>'
				+ '<option value="false">아니오</option>'
				+ '</select></td>'
				+ '<td><select class="form-control shortcut-status">'
				+ '<option value="true">활성</option>'
				+ '<option value="false">비활성</option>'
				+ '</select></td>'
				+ '<td><button type="button" class="btn btn-danger btn-sm" onclick="removeShortcut(this)">삭제</button></td>'
				+ '</tr>');
		$('#shortcutTableBody').append(row);
		
		// 새로 추가된 행의 툴팁 초기화
		row.find('[data-toggle="tooltip"]').tooltip({
			placement: 'top',
			trigger: 'hover'
		});
		
		// 새로 추가된 행의 대상 템플릿 드롭다운에 옵션 로드 및 Select2 초기화
		loadTemplateOptions(row.find('.target-template-select2'));
	}

	// 단축키 삭제
	function removeShortcut(button) {
		$(button).closest('tr').remove();
	}

	// 단축키 목록 로드
	function loadShortcuts(templateId) {
		if (!templateId) {
			$('#shortcutTableBody').empty();
			return;
		}

		$.ajax({
			type : 'GET',
			url : '/SQLTemplate/shortcuts',
			data : {
				templateId : templateId
			},
			success : function(result) {
				if (result.success) {
					renderShortcuts(result.data);
				} else {
					$('#shortcutTableBody').empty();
				}
			}
		});
	}

	// 단축키 렌더링
	function renderShortcuts(shortcuts) {
		var tbody = $('#shortcutTableBody');
		tbody.empty();

		if (shortcuts && shortcuts.length > 0) {
			shortcuts
					.forEach(function(shortcut) {
						var row = $('<tr class="shortcut-row">'
								+ '<td><input type="text" class="form-control shortcut-key" value="'
								+ (shortcut.SHORTCUT_KEY || '')
								+ '" placeholder="F1" readonly></td>'
								+ '<td><input type="text" class="form-control shortcut-name" value="'
								+ (shortcut.SHORTCUT_NAME || '')
								+ '" placeholder="단축키명"></td>'
								+ '<td><select class="form-control target-template-select2">'
								+ '<option value="">대상 템플릿 선택</option>'
								+ '</select></td>'
								+ '<td><input type="text" class="form-control shortcut-description" value="'
								+ (shortcut.SHORTCUT_DESCRIPTION || '')
								+ '" placeholder="단축키 설명"></td>'
								+ '<td><input type="text" class="form-control source-columns" value="'
								+ (shortcut.SOURCE_COLUMN_INDEXES || '')
								+ '" placeholder="1,2,3"></td>'
								+ '<td><select class="form-control auto-execute">'
								+ '<option value="true"'
								+ (shortcut.AUTO_EXECUTE ? ' selected' : '')
								+ '>예</option>'
								+ '<option value="false"'
								+ (!shortcut.AUTO_EXECUTE ? ' selected' : '')
								+ '>아니오</option>'
								+ '</select></td>'
								+ '<td><select class="form-control shortcut-status">'
								+ '<option value="true"'
								+ (shortcut.IS_ACTIVE ? ' selected' : '')
								+ '>활성</option>'
								+ '<option value="false"'
								+ (!shortcut.IS_ACTIVE ? ' selected' : '')
								+ '>비활성</option>'
								+ '</select></td>'
								+ '<td><button type="button" class="btn btn-danger btn-sm" onclick="removeShortcut(this)">삭제</button></td>'
								+ '</tr>');
						tbody.append(row);
						
						// 새로 추가된 행의 툴팁 초기화
						row.find('[data-toggle="tooltip"]').tooltip({
							placement: 'top',
							trigger: 'hover'
						});
						
						// 새로 추가된 행의 대상 템플릿 드롭다운에 옵션 로드 및 Select2 초기화
						loadTemplateOptions(row.find('.target-template-select2'), shortcut.TARGET_TEMPLATE_ID);
					});
			loadTemplateOptions();
		}
	}

	// 템플릿 옵션 로드 (단축키 대상용)
	function loadTemplateOptions(selectElement, selectedValue) {
		$.ajax({
			type : 'GET',
			url : '/SQLTemplate/list',
			success : function(result) {
				if (result.success) {
					var options = '<option value="">대상 템플릿 선택</option>';
					result.data.forEach(function(template) {
						var selected = (selectedValue && selectedValue === template.TEMPLATE_ID) ? ' selected' : '';
						options += '<option value="' + template.TEMPLATE_ID + '"' + selected + '>'
								+ template.TEMPLATE_NAME
								+ '</option>';
					});
					
					if (selectElement) {
						selectElement.html(options);
						// Select2 초기화
						selectElement.select2({
							placeholder: '대상 템플릿 선택',
							allowClear: true,
							width: '100%',
							language: {
								noResults: function() {
									return "검색 결과가 없습니다.";
								},
								searching: function() {
									return "검색 중...";
								}
							}
						});
					} else {
						// 기존 방식 (하위 호환성)
						$('.target-template').html(options);
					}
				}
			}
		});
	}

	// 단축키 데이터 수집
	function collectShortcuts() {
		var shortcuts = [];
		$('#shortcutTableBody tr')
				.each(
						function() {
							var key = $(this).find('.shortcut-key').val();
							var name = $(this).find('.shortcut-name').val();
							var targetTemplate = $(this).find(
									'.target-template-select2').val();

							if (key && key.trim() && name && name.trim()
									&& targetTemplate) {
								shortcuts.push({
									key : key.trim(),
									name : name.trim(),
									targetTemplateId : targetTemplate,
									description : $(this).find('.shortcut-description').val(),
									sourceColumns : $(this).find('.source-columns').val(),
									autoExecute : $(this).find('.auto-execute')
											.val() === 'true',
									isActive : $(this).find('.shortcut-status')
											.val() === 'true'
								});
							}
						});
		return shortcuts;
	}

	// 새 SQL 템플릿 생성
	function createNewSqlTemplate() {
		$('#sqlTemplateId').val('');
		$('#sqlTemplateName').val('');
		$('#sqlTemplateDesc').val('');
		$('#sqlTemplateStatus').val('ACTIVE');
		$('#sqlExecutionLimit').val('1000');
		$('#sqlRefreshTimeout').val('10');
		$('input[name="categoryIds"]').prop('checked', false);

		if (window.sqlEditor && window.sqlEditor.setValue) {
			window.sqlEditor.setValue('');
		} else {
			$('#sqlEditor').val('');
		}

		$('#parameterTableBody').empty();
		$('#shortcutTableBody').empty();

		updateSqlPreview();
		$('.template-item').removeClass('selected');
		
		// Select2 인스턴스들 정리
		$('.target-template-select2').select2('destroy');
	}

	// SQL 템플릿 저장 (카테고리 포함)
	function saveSqlTemplate() {
		// 벨리데이션 체크
		if (!validateSqlTemplate()) {
			return;
		}
		
		var sqlId = $('#sqlTemplateId').val();
		var sqlName = $('#sqlTemplateName').val();
		var sqlDesc = $('#sqlTemplateDesc').val();
		var sqlStatus = $('#sqlTemplateStatus').val();
		var executionLimit = $('#sqlExecutionLimit').val();
		var refreshTimeout = $('#sqlRefreshTimeout').val();
		var selectedCategoryIds = getSelectedCategoryIds();

		var sqlContent = '';
		if (window.sqlEditor && window.sqlEditor.getValue) {
			sqlContent = window.sqlEditor.getValue();
		} else {
			sqlContent = $('#sqlEditor').val();
		}

		var parameters = collectParameters();
		var configContent = parametersToConfigString(parameters);
		var shortcuts = collectShortcuts();

		var data = {
			sqlId : sqlId,
			sqlName : sqlName,
			sqlDesc : sqlDesc,
			sqlStatus : sqlStatus,
			executionLimit : executionLimit,
			refreshTimeout : refreshTimeout,
			sqlPath : selectedCategoryIds.join(','),
			sqlContent : sqlContent,
			configContent : configContent,
			parameters : JSON.stringify(parameters),
			shortcuts : JSON.stringify(shortcuts)
		};

		$.ajax({
			type : 'post',
			url : '/SQLTemplate/save',
			data : data,
			success : function(result) {
				if (result.success) {
					alert('SQL 템플릿이 저장되었습니다.');
					// 폼 초기화
					createNewSqlTemplate();
					// 템플릿 목록 새로고침
					var selectedCategory = $('.category-item.selected').data(
							'id');
					if (selectedCategory) {
						loadTemplatesByCategory(selectedCategory);
					}
					// 카테고리별 템플릿 개수 업데이트
					loadCategoryTemplateCounts();
				} else {
					alert('저장 실패: ' + result.error);
				}
			}
		});
	}

	// SQL 템플릿 상세 정보 로드
	function loadSqlTemplateDetail(templateId) {
		$
				.ajax({
					type : 'GET',
					url : '/SQLTemplate/detail',
					data : {
						sqlId : templateId
					},
					success : function(result) {
						if (result.success) {
							var template = result.data;
							$('#sqlTemplateId').val(template.sqlId);
							$('#sqlTemplateName').val(template.sqlName);
							$('#sqlTemplateDesc').val(template.sqlDesc || '');
							$('#sqlTemplateStatus').val(
									template.sqlStatus || 'ACTIVE');
							$('#sqlExecutionLimit').val(
									template.executionLimit || 1000);
							$('#sqlRefreshTimeout').val(
									template.refreshTimeout || 10);

							loadTemplateCategories(templateId);
							loadParameters(templateId);
							loadShortcuts(templateId);

							if (window.sqlEditor && window.sqlEditor.setValue) {
								window.sqlEditor.setValue(template.sqlContent
										|| '');
							} else {
								$('#sqlEditor').val(template.sqlContent || '');
							}

							updateSqlPreview();
						} else {
							alert('템플릿 정보 로드 실패: ' + result.error);
						}
					}
				});
	}

	// SQL 템플릿 삭제
	function deleteSqlTemplate() {
		var sqlId = $('#sqlTemplateId').val();
		if (!sqlId) {
			alert('삭제할 템플릿을 선택해주세요.');
			return;
		}

		if (!confirm('정말로 이 SQL 템플릿을 삭제하시겠습니까?')) {
			return;
		}

		$.ajax({
			type : 'POST',
			url : '/SQLTemplate/delete',
			data : {
				sqlId : sqlId
			},
			success : function(result) {
				if (result.success) {
					alert('SQL 템플릿이 삭제되었습니다.');
					createNewSqlTemplate();
					var selectedCategory = $('.category-item.selected').data(
							'id');
					if (selectedCategory) {
						loadTemplatesByCategory(selectedCategory);
					}
					// 카테고리별 템플릿 개수 업데이트
					loadCategoryTemplateCounts();
				} else {
					alert('삭제 실패: ' + result.error);
				}
			}
		});
	}

	// SQL 테스트 실행
	function testSqlTemplate() {
		var sqlContent = '';
		if (window.sqlEditor && window.sqlEditor.getValue) {
			sqlContent = window.sqlEditor.getValue();
		} else {
			sqlContent = $('#sqlEditor').val();
		}

		if (!sqlContent) {
			alert('테스트할 SQL을 입력해주세요.');
			return;
		}

		$('#testResult').html(
				'<div class="alert alert-info">SQL 테스트 중...</div>');

		$
				.ajax({
					type : 'post',
					url : '/SQLTemplate/test',
					data : {
						sqlContent : sqlContent
					},
					success : function(result) {
						if (result.success) {
							$('#testResult')
									.html(
											'<div class="alert alert-success">SQL 문법 검증 성공!</div>');
						} else {
							$('#testResult').html(
									'<div class="alert alert-danger">SQL 오류: '
											+ result.error + '</div>');
						}
					}
				});
	}

	// SQL 에디터 내용 변경 시 미리보기 업데이트
	$(document).on('input', '#sqlEditor, #sqlTextarea', function() {
		updateSqlPreview();
	});

	// Ace Editor 내용 변경 시 미리보기 업데이트
	$(document).on('change', '#sqlEditor', function() {
		updateSqlPreview();
	});
	
	// 파라미터 속성 JSON 파싱 (하위 호환성용)
	function parseParameterAttributes(description) {
		var attributes = {
			readonly: '',
			hidden: '',
			disabled: ''
		};
		
		// 새로운 스키마에서는 개별 필드를 사용하므로 이 함수는 하위 호환성용으로만 사용
		if (!description) return attributes;
		
		try {
			// description에서 JSON 부분 찾기 (기존 데이터용)
			var jsonMatch = description.match(/\{[^}]+\}$/);
			if (jsonMatch) {
				var jsonStr = jsonMatch[0];
				// 간단한 JSON 파싱
				if (jsonStr.includes('"readonly"')) {
					attributes.readonly = 'readonly';
				}
				if (jsonStr.includes('"hidden"')) {
					attributes.hidden = 'hidden';
				}
				if (jsonStr.includes('"disabled"')) {
					attributes.disabled = 'disabled';
				}
			}
		} catch (e) {
			console.log('파라미터 속성 파싱 실패:', e);
		}
		
		return attributes;
	}

	// 단축키 입력 필드에 키 이벤트 리스너 추가
	$(document).on('focus', '.shortcut-key', function() {
		$(this).attr('data-listening', 'true');
		$(this).val('').attr('placeholder', '키를 누르세요...');
	});

	$(document).on('blur', '.shortcut-key', function() {
		$(this).removeAttr('data-listening');
		$(this).attr('placeholder', 'F1');
	});

	// 전역 키 이벤트 리스너
	$(document).on('keydown', function(e) {
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
				alert('F1~F12 키만 사용 가능합니다.');
			}
		}
	});
</script>

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
			<div class="col-md-3">
				<div class="box box-primary">
					<div class="box-header with-border">
						<h3 class="box-title">카테고리 목록</h3>
						<div class="box-tools pull-right">
							<button type="button" class="btn btn-box-tool"
								onclick="createCategory()" data-toggle="tooltip" data-placement="top" title="새로운 카테고리를 생성합니다. 카테고리는 SQL 템플릿을 분류하는 데 사용됩니다.">
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
							<button type="button" class="btn btn-box-tool"
								onclick="createNewSqlTemplate()">
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
			<div class="col-md-9">
				<div class="box box-info">
					<div class="box-header with-border">
						<h3 class="box-title">SQL 템플릿 편집</h3>
						<div class="box-tools pull-right">
							<button type="button" class="btn btn-info btn-sm"
								onclick="testSqlTemplate()" data-toggle="tooltip" data-placement="top" title="SQL 문법을 검증합니다. SQL 실행 없이 문법 오류만 확인할 수 있습니다.">
								<i class="fa fa-play"></i> 테스트
							</button>
							<button type="button" class="btn btn-success btn-sm"
								onclick="saveSqlTemplate()" data-toggle="tooltip" data-placement="top" title="SQL 템플릿을 저장합니다. 모든 필수 항목이 입력되어야 저장할 수 있습니다.">
								<i class="fa fa-save"></i> 저장
							</button>
							<button type="button" class="btn btn-danger btn-sm"
								onclick="deleteSqlTemplate()" data-toggle="tooltip" data-placement="top" title="현재 선택된 SQL 템플릿을 삭제합니다. 삭제된 템플릿은 복구할 수 없습니다.">
								<i class="fa fa-trash"></i> 삭제
							</button>
						</div>
					</div>
					<div class="box-body">
						<!-- 숨겨진 ID 필드 -->
						<input type="hidden" id="sqlTemplateId">

						<!-- 기본 정보 -->
						<div class="row">

							<div class="col-md-7">
								<!-- 설정 정보 -->
								<div class="row">
									<div class="col-md-3">
										<div class="form-group">
											<label data-toggle="tooltip" data-placement="top" title="SQL 템플릿의 고유한 이름을 입력합니다. 템플릿 목록에서 표시되는 이름이며, 100자 이하로 입력해야 합니다.">SQL 이름</label> <input type="text" class="form-control"
												id="sqlTemplateName" placeholder="SQL 이름">
										</div>
									</div>
									<div class="col-md-3">
										<div class="form-group">
											<label data-toggle="tooltip" data-placement="top" title="SQL 템플릿의 상태를 설정합니다. 활성:사용 가능, 비활성:사용 불가, 초안:작성 중">상태</label> <select class="form-control"
												id="sqlTemplateStatus">
												<option value="ACTIVE">활성</option>
												<option value="INACTIVE">비활성</option>
												<option value="DRAFT">초안</option>
											</select>
										</div>
									</div>


									<div class="col-md-3">
										<div class="form-group">
											<label data-toggle="tooltip" data-placement="top" title="SQL 실행 시 최대 반환할 행 수를 설정합니다. 1~100,000 사이의 숫자를 입력하세요. 0은 제한 없음을 의미합니다.">실행 제한 (행)</label> <input type="number"
												class="form-control" id="sqlExecutionLimit" value="1000"
												min="1" placeholder="최대 반환 행 수">
										</div>
									</div>
									<div class="col-md-3">
										<div class="form-group">
											<label data-toggle="tooltip" data-placement="top" title="자동 새로고침 기능 사용 시 대기 시간을 설정합니다. 1~3600초 사이의 숫자를 입력하세요.">새로고침 타임아웃 (초)</label> <input type="number"
												class="form-control" id="sqlRefreshTimeout" value="10"
												min="1" placeholder="새로고침 대기 시간">
										</div>
									</div>
								</div>
								<!-- 추가 정보 -->
								<div class="row">
									<div class="col-md-12">
										<div class="form-group">
											<label data-toggle="tooltip" data-placement="top" title="SQL 템플릿의 용도나 사용법에 대한 설명을 입력합니다. 템플릿 목록에서 표시되는 설명입니다.">설명</label>
											<textarea class="form-control" id="sqlTemplateDesc" rows="2"
												placeholder="SQL 템플릿에 대한 설명을 입력하세요"></textarea>
										</div>
									</div>
								</div>

							</div>

							<div class="col-md-5">
								<div class="form-group">
									<label data-toggle="tooltip" data-placement="top" title="SQL 템플릿을 분류할 카테고리를 선택합니다. 여러 카테고리를 선택할 수 있으며, 선택된 카테고리에서 템플릿을 찾을 수 있습니다.">카테고리</label>
									<div id="sqlTemplateCategories" class="category-checkboxes"
										style="overflow-y: auto; border: 1px solid #ddd; padding: 10px; background-color: #f9f9f9; max-height: 120px;">
										<!-- 카테고리 체크박스들이 여기에 로드됩니다 -->
									</div>
								</div>
							</div>
						</div>

						<!-- 파라미터 관리 패널 -->
						<div class="form-group">
							<label>파라미터 관리</label>
							<div class="row">
								<div class="col-md-12">
									<div class="table-responsive">
										<table class="table table-bordered table-striped"
											id="parameterTable">
											<thead>
												<tr>
													<th class="text-center"><div data-toggle="tooltip" data-placement="top" title="파라미터의 실행 순서를 변경합니다. SQL 실행 시 파라미터가 바인딩되는 순서를 결정합니다.">순서</div></th>
													<th class="text-center"><div data-toggle="tooltip" data-placement="top" title="파라미터의 고유한 이름을 입력합니다. SQL에서 :파라미터명 형태로 사용되며, 영문자/숫자/언더스코어만 허용됩니다.">파라미터명</div></th>
													<th class="text-center"><div data-toggle="tooltip" data-placement="top" title="파라미터의 용도나 설명을 입력합니다. SQL 실행 화면에서 사용자에게 표시되는 설명입니다.">설명</div></th>
													<th class="text-center"><div data-toggle="tooltip" data-placement="top" title="파라미터의 데이터 타입을 선택합니다. STRING:문자열, NUMBER:숫자, DATE:날짜, BOOLEAN:불린, TEXT:긴텍스트, SQL:SQL문">타입</div></th>
													<th class="text-center"><div data-toggle="tooltip" data-placement="top" title="파라미터의 기본값을 설정합니다. 사용자가 값을 입력하지 않았을 때 사용되는 값입니다.">기본값</div></th>
													<th class="text-center"><div data-toggle="tooltip" data-placement="top" title="이 파라미터가 반드시 입력되어야 하는지 설정합니다. 체크 시 SQL 실행 전 필수 입력 검증이 수행됩니다.">필수</div></th>
													<th class="text-center"><div data-toggle="tooltip" data-placement="top" title="사용자가 값을 수정할 수 없도록 설정합니다. 체크 시 입력 필드가 읽기 전용으로 표시됩니다.">읽기전용</div></th>
													<th class="text-center"><div data-toggle="tooltip" data-placement="top" title="파라미터를 화면에 숨깁니다. 체크 시 입력 필드가 숨겨지고 자동으로 필수로 설정됩니다.">숨김</div></th>
													<th class="text-center"><div data-toggle="tooltip" data-placement="top" title="파라미터를 비활성화하여 사용할 수 없게 합니다. 체크 시 입력 필드가 비활성화되어 값을 입력할 수 없습니다.">비활성화</div></th>
													<th class="text-center"><div data-toggle="tooltip" data-placement="top" title="파라미터를 삭제합니다. 삭제된 파라미터는 복구할 수 없습니다.">작업</div></th>
												</tr>
											</thead>
											<tbody id="parameterTableBody">
												<!-- 파라미터들이 여기에 동적으로 추가됩니다 -->
											</tbody>
										</table>
									</div>
									<button type="button" class="btn btn-primary btn-sm"
										onclick="addParameter()" data-toggle="tooltip" data-placement="top" title="새로운 파라미터를 추가합니다. 파라미터는 SQL 실행 시 사용자가 입력할 수 있는 변수입니다.">
										<i class="fa fa-plus"></i> 파라미터 추가
									</button>
								</div>
							</div>
						</div>

						<!-- SQL 에디터 -->
						<div class="form-group">
							<label data-toggle="tooltip" data-placement="top" title="실행할 SQL 문을 입력합니다. 파라미터는 :파라미터명 형태로 사용하며, 문법 하이라이팅과 자동완성 기능을 지원합니다.">SQL 내용</label>
							<div id="sqlEditor"
								style="height: 300px; border: 1px solid #ccc;"></div>
						</div>



						<!-- 단축키 관리 패널 -->
						<div class="form-group">
							<label>단축키 관리</label>
							<div class="row">
								<div class="col-md-12">
									<div class="table-responsive">
										<table class="table table-bordered table-striped"
											id="shortcutTable">
											<thead>
												<tr>
													<th width="12%" class="text-center"><div data-toggle="tooltip" data-placement="top" title="단축키를 입력합니다. F1~F12 중 하나를 선택하여 키보드 단축키로 설정합니다.">단축키</div></th>
													<th width="18%" class="text-center"><div data-toggle="tooltip" data-placement="top" title="단축키의 이름을 입력합니다. 사용자가 단축키를 식별할 수 있는 설명적인 이름을 입력하세요.">단축키명</div></th>
													<th width="20%" class="text-center"><div data-toggle="tooltip" data-placement="top" title="단축키로 실행할 대상 SQL 템플릿을 선택합니다. 검색 기능을 통해 쉽게 찾을 수 있습니다.">대상 템플릿</div></th>
													<th width="15%" class="text-center"><div data-toggle="tooltip" data-placement="top" title="단축키에 대한 설명을 입력합니다. 단축키의 용도나 사용법을 설명하세요.">설명</div></th>
													<th width="10%" class="text-center"><div data-toggle="tooltip" data-placement="top" title="소스 컬럼 인덱스를 입력합니다. 콤마로 구분된 숫자 형태로 입력 (예: 1,2,3)">소스 컬럼</div></th>
													<th width="10%" class="text-center"><div data-toggle="tooltip" data-placement="top" title="단축키 실행 시 자동으로 SQL을 실행할지 설정합니다. 예:자동실행, 아니오:수동실행">자동실행</div></th>
													<th width="10%" class="text-center"><div data-toggle="tooltip" data-placement="top" title="단축키의 활성화 상태를 설정합니다. 활성화된 단축키만 사용할 수 있습니다.">상태</div></th>
													<th width="5%" class="text-center"><div data-toggle="tooltip" data-placement="top" title="단축키를 삭제합니다. 삭제된 단축키는 복구할 수 없습니다.">작업</div></th>
												</tr>
											</thead>
											<tbody id="shortcutTableBody">
												<!-- 단축키들이 여기에 동적으로 추가됩니다 -->
											</tbody>
										</table>
									</div>
									<button type="button" class="btn btn-success btn-sm"
										onclick="addShortcut()" data-toggle="tooltip" data-placement="top" title="새로운 단축키를 추가합니다. 단축키는 F1~F12 키를 사용하여 빠르게 SQL 템플릿을 실행할 수 있게 해줍니다.">
										<i class="fa fa-plus"></i> 단축키 추가
									</button>
								</div>
							</div>
						</div>

						<!-- 미리보기 -->
						<div class="form-group">
							<label>SQL 미리보기</label>
							<div id="sqlPreview" class="sql-preview"></div>
						</div>

						<!-- 테스트 결과 -->
						<div id="testResult"></div>
					</div>
				</div>
			</div>
		</div>
	</section>
</div>

<!-- 카테고리 모달 -->
<div class="modal fade" id="categoryModal" tabindex="-1" role="dialog"
	aria-labelledby="categoryModalLabel" aria-hidden="true">
	<div class="modal-dialog" role="document">
		<div class="modal-content">
			<div class="modal-header">
				<h5 class="modal-title" id="categoryModalTitle">카테고리 관리</h5>
				<button type="button" class="close" data-dismiss="modal"
					aria-label="Close">
					<span aria-hidden="true">&times;</span>
				</button>
			</div>
			<div class="modal-body">
				<form id="categoryForm">
					<input type="hidden" id="categoryId">
					<div class="form-group">
						<label for="categoryName" data-toggle="tooltip" data-placement="top" title="카테고리의 이름을 입력합니다. SQL 템플릿을 분류하는 데 사용되는 이름이며, 중복되지 않는 고유한 이름을 입력하세요.">카테고리 이름</label> <input type="text"
							class="form-control" id="categoryName" required>
					</div>
					<div class="form-group">
						<label for="categoryDescription" data-toggle="tooltip" data-placement="top" title="카테고리에 대한 설명을 입력합니다. 카테고리의 용도나 특징을 설명하여 사용자가 쉽게 이해할 수 있도록 도와줍니다.">설명 (선택 사항)</label>
						<textarea class="form-control" id="categoryDescription" rows="3"></textarea>
					</div>
				</form>
			</div>
			<div class="modal-footer">
				<button type="button" class="btn btn-secondary" data-dismiss="modal" data-toggle="tooltip" data-placement="top" title="카테고리 생성을 취소합니다. 입력한 내용은 저장되지 않습니다.">취소</button>
				<button type="button" class="btn btn-primary"
					id="categoryModalSaveBtn" onclick="saveCategory()" data-toggle="tooltip" data-placement="top" title="카테고리를 저장합니다. 카테고리 이름은 필수 입력 항목입니다.">저장</button>
			</div>
		</div>
	</div>
</div>
