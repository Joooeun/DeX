<%@include file="common/common.jsp" %>

<style>
	.etl-editor-container {
		display: flex;
		gap: 15px;
	}
	
	.etl-source-section,
	.etl-target-section {
		flex: 1;
		min-width: 0;
	}
	
	.column-mapping-table {
		width: 100%;
		font-size: 12px;
	}
	
	.column-mapping-table th,
	.column-mapping-table td {
		padding: 6px;
		text-align: left;
		vertical-align: middle;
	}
	
	.column-mapping-table th {
		background: #f4f4f4;
		font-weight: bold;
	}
	
	.column-mapping-table input,
	.column-mapping-table select {
		width: 100%;
		padding: 4px;
		font-size: 12px;
	}
	
	.preview-table {
		max-height: 300px;
		overflow-y: auto;
		border: 1px solid #ddd;
	}
	
	.preview-table table {
		margin-bottom: 0;
	}
	
	.preview-table th,
	.preview-table td {
		padding: 4px 8px;
		font-size: 11px;
	}
	
	.source-preview-header {
		display: flex;
		justify-content: space-between;
		align-items: center;
		margin-bottom: 10px;
	}
	
	.divider-line {
		border-left: 2px solid #ddd;
		margin: 0 15px;
	}
	
	@media (max-width: 1200px) {
		.etl-editor-container {
			flex-direction: column;
		}
		
		.divider-line {
			border-left: none;
			border-top: 2px solid #ddd;
			margin: 15px 0;
			height: 2px;
		}
	}
</style>

<section class="content-header">
	<h1>
		ETL 작업 편집
		<small>데이터 이관 작업 설정</small>
	</h1>
	<ol class="breadcrumb">
		<li><a href="#"><i class="fa fa-dashboard"></i> Home</a></li>
		<li><a href="/ETL">ETL 관리</a></li>
		<li class="active">작업 편집</li>
	</ol>
</section>

<section class="content">
	<div class="row">
		<div class="col-md-12">
			<div class="box box-primary">
				<div class="box-header with-border">
					<h3 class="box-title">ETL 작업 설정</h3>
					<div class="box-tools pull-right">
						<button type="button" class="btn btn-success btn-sm" onclick="saveETLJob()">
							<i class="fa fa-save"></i> 저장
						</button>
					</div>
				</div>
				<div class="box-body">
					<!-- 기본 정보 (상단) -->
					<div class="row" style="margin-bottom: 20px;">
						<div class="col-md-6">
							<div class="form-group">
								<label>작업명 <span class="text-red">*</span></label>
								<input type="text" class="form-control" id="jobName" placeholder="ETL 작업명을 입력하세요">
							</div>
						</div>
						<div class="col-md-6">
							<div class="form-group">
								<label>작업 상태</label>
								<select class="form-control" id="jobStatus">
									<option value="ACTIVE">활성</option>
									<option value="INACTIVE">비활성</option>
								</select>
							</div>
						</div>
						<div class="col-md-12">
							<div class="form-group">
								<label>설명</label>
								<textarea class="form-control" id="jobDescription" rows="2" placeholder="작업에 대한 설명을 입력하세요"></textarea>
							</div>
						</div>
					</div>
					
					<!-- 소스/타겟 설정 (좌우 분할) -->
					<div class="etl-editor-container">
						<!-- 좌측: 소스 설정 -->
						<div class="etl-source-section">
							<div class="box box-info">
								<div class="box-header with-border">
									<h3 class="box-title">
										<i class="fa fa-database"></i> 소스 설정
									</h3>
								</div>
								<div class="box-body">
									<div class="form-group">
										<label>소스 DB 연결 <span class="text-red">*</span></label>
										<select class="form-control" id="sourceConnectionId">
											<option value="">연결을 선택하세요</option>
											<option value="pg">PostgreSQL (pg)</option>
											<option value="pgmac">PostgreSQL Mac (pgmac)</option>
										</select>
									</div>
									<div class="form-group">
										<label>소스 SQL 템플릿 <span class="text-red">*</span></label>
										<select class="form-control" id="sourceTemplateId">
											<option value="">템플릿을 선택하세요</option>
											<option value="TMP001">사용자 조회</option>
											<option value="TMP002">주문 조회</option>
											<option value="TMP003">상품 조회</option>
										</select>
									</div>
									
									<div class="form-group">
										<label>SQL 파라미터 (JSON 형식)</label>
										<textarea class="form-control" id="sourceParameters" rows="4" placeholder='{"param1": "value1", "param2": "value2"}'></textarea>
										<small class="text-muted">SQL 템플릿에 사용될 파라미터를 JSON 형식으로 입력하세요</small>
									</div>
									
									<div class="form-group">
										<button type="button" class="btn btn-info btn-sm" onclick="previewSourceData()">
											<i class="fa fa-eye"></i> 데이터 미리보기
										</button>
									</div>
									
									<div id="sourcePreviewContainer" style="display: none;">
										<div class="source-preview-header">
											<label><strong>소스 데이터 미리보기</strong></label>
											<small class="text-muted" id="sourcePreviewCount"></small>
										</div>
										<div class="preview-table">
											<table class="table table-bordered table-striped table-condensed" id="sourcePreviewTable">
												<thead></thead>
												<tbody></tbody>
											</table>
										</div>
									</div>
								</div>
							</div>
						</div>
						
						<!-- 구분선 -->
						<div class="divider-line"></div>
						
						<!-- 우측: 타겟 설정 -->
						<div class="etl-target-section">
							<div class="box box-success">
								<div class="box-header with-border">
									<h3 class="box-title">
										<i class="fa fa-arrow-right"></i> 타겟 설정
									</h3>
								</div>
								<div class="box-body">
									<div class="form-group">
										<label>타겟 DB 연결 <span class="text-red">*</span></label>
										<select class="form-control" id="targetConnectionId">
											<option value="">연결을 선택하세요</option>
											<option value="pg">PostgreSQL (pg)</option>
											<option value="pgmac">PostgreSQL Mac (pgmac)</option>
										</select>
									</div>
									
									<div class="form-group">
										<label>타겟 방식 <span class="text-red">*</span></label>
										<select class="form-control" id="targetType" onchange="onTargetTypeChange()">
											<option value="TABLE">직접 테이블</option>
											<option value="TEMPLATE">SQL 템플릿</option>
										</select>
									</div>
									
									<!-- 직접 테이블 방식 -->
									<div id="targetTableSection" class="target-type-section">
										<div class="form-group">
											<label>타겟 테이블명 <span class="text-red">*</span></label>
											<input type="text" class="form-control" id="targetTableName" placeholder="예: USER_DATA">
											<small class="text-muted">테이블이 없으면 자동으로 생성됩니다</small>
										</div>
									</div>
									
									<!-- SQL 템플릿 방식 -->
									<div id="targetTemplateSection" class="target-type-section" style="display: none;">
										<div class="form-group">
											<label>타겟 SQL 템플릿 <span class="text-red">*</span></label>
											<select class="form-control" id="targetTemplateId" onchange="loadTargetTemplateDetail()">
												<option value="">템플릿을 선택하세요</option>
											</select>
											<button type="button" class="btn btn-sm btn-info" onclick="loadTargetTemplates()" style="margin-top: 5px;">
												<i class="fa fa-refresh"></i> 새로고침
											</button>
										</div>
										
										<div id="targetTemplateInfo" style="display: none;">
											<div class="form-group">
												<label>템플릿 파라미터</label>
												<div class="well well-sm" id="targetTemplateParameters" style="max-height: 150px; overflow-y: auto; margin-bottom: 0;">
													템플릿을 선택하면 파라미터 목록이 표시됩니다
												</div>
											</div>
										</div>
									</div>
									
									<div class="form-group">
										<label>컬럼 매핑 및 가공 규칙</label>
										<div class="table-responsive">
											<table class="table table-bordered column-mapping-table" id="columnMappingTable">
												<thead>
													<tr>
														<th style="width: 25%;">소스 컬럼</th>
														<th style="width: 25%;">타겟 컬럼</th>
														<th style="width: 20%;">가공 규칙</th>
														<th style="width: 10%;">기본값</th>
													</tr>
												</thead>
												<tbody id="columnMappingBody">
													<tr>
														<td colspan="4" class="text-center text-muted">
															소스 데이터를 미리보기 해주세요
														</td>
													</tr>
												</tbody>
											</table>
										</div>
										<small class="text-muted" id="targetMappingHint">
											소스 컬럼을 타겟 파라미터(${paramName})에 매핑합니다
										</small>
									</div>
									
									<div class="form-group">
										<label>배치 크기</label>
										<input type="number" class="form-control" id="batchSize" value="1000" min="1" max="10000">
										<small class="text-muted">한 번에 처리할 레코드 수 (기본값: 1000)</small>
									</div>
									
									<div class="form-group">
										<label>에러 처리 방식</label>
										<select class="form-control" id="errorHandlingMode">
											<option value="STOP">중단 (에러 발생 시 중단)</option>
											<option value="CONTINUE">계속 (에러 발생 시 건너뛰고 계속)</option>
											<option value="SKIP">건너뛰기 (에러 행만 건너뛰기)</option>
										</select>
									</div>
								</div>
							</div>
						</div>
					</div>
					
					<!-- 하단 저장 버튼 -->
					<div class="row" style="margin-top: 20px;">
						<div class="col-md-12 text-right">
							<button type="button" class="btn btn-default" onclick="cancelEdit()">
								<i class="fa fa-times"></i> 취소
							</button>
							<button type="button" class="btn btn-success" onclick="saveETLJob()">
								<i class="fa fa-save"></i> 저장
							</button>
						</div>
					</div>
				</div>
			</div>
		</div>
	</div>
</section>

<script>
	var sourceColumns = [];
	var targetTemplateParameters = []; // 타겟 템플릿 파라미터 목록
	
	$(document).ready(function() {
		// 소스 템플릿 변경 시 이벤트
		$('#sourceTemplateId').on('change', function() {
			$('#sourcePreviewContainer').hide();
			$('#columnMappingBody').html(
				'<tr><td colspan="4" class="text-center text-muted">소스 데이터를 미리보기 해주세요</td></tr>'
			);
			sourceColumns = [];
		});
		
		// 소스 연결 변경 시 미리보기 초기화
		$('#sourceConnectionId').on('change', function() {
			$('#sourcePreviewContainer').hide();
		});
		
		// 타겟 템플릿 목록 초기 로드
		loadTargetTemplates();
	});
	
	// 타겟 방식 변경
	function onTargetTypeChange() {
		var targetType = $('#targetType').val();
		
		if (targetType === 'TABLE') {
			$('#targetTableSection').show();
			$('#targetTemplateSection').hide();
			$('#targetTemplateInfo').hide();
			$('#targetMappingHint').text('소스 컬럼을 타겟 테이블 컬럼에 매핑합니다');
		} else if (targetType === 'TEMPLATE') {
			$('#targetTableSection').hide();
			$('#targetTemplateSection').show();
			$('#targetMappingHint').text('소스 컬럼을 타겟 파라미터(${paramName})에 매핑합니다');
		}
		
		// 컬럼 매핑 재초기화
		if (sourceColumns.length > 0) {
			initializeColumnMapping();
		}
	}
	
	// 타겟 템플릿 목록 로드
	function loadTargetTemplates() {
		$.ajax({
			type: 'GET',
			url: '/ETL/target-templates',
			success: function(result) {
				if (result.success) {
					var select = $('#targetTemplateId');
					select.empty().append('<option value="">템플릿을 선택하세요</option>');
					
					if (result.data && result.data.length > 0) {
						result.data.forEach(function(template) {
							var option = $('<option></option>')
								.attr('value', template.TEMPLATE_ID)
								.text(template.TEMPLATE_NAME + (template.TEMPLATE_DESC ? ' - ' + template.TEMPLATE_DESC : ''));
							select.append(option);
						});
					}
				} else {
					alert(result.message || '타겟 템플릿 목록 조회 실패');
				}
			},
			error: function() {
				alert('타겟 템플릿 목록 조회 중 오류가 발생했습니다.');
			}
		});
	}
	
	// 타겟 템플릿 상세 로드
	function loadTargetTemplateDetail() {
		var templateId = $('#targetTemplateId').val();
		
		if (!templateId) {
			$('#targetTemplateInfo').hide();
			targetTemplateParameters = [];
			// 컬럼 매핑 재초기화
			if (sourceColumns.length > 0) {
				initializeColumnMapping();
			}
			return;
		}
		
		$.ajax({
			type: 'GET',
			url: '/ETL/target-template/detail',
			data: { templateId: templateId },
			success: function(result) {
				if (result.success) {
					var data = result.data;
					targetTemplateParameters = data.parameters || [];
					
					// 파라미터 목록 표시
					var paramHtml = '<div><strong>템플릿:</strong> ' + escapeHtml(data.templateName) + '</div>';
					if (targetTemplateParameters.length > 0) {
						paramHtml += '<div style="margin-top: 10px;"><strong>파라미터:</strong></div>';
						paramHtml += '<div style="margin-top: 5px;">';
						targetTemplateParameters.forEach(function(param) {
							paramHtml += '<span class="label label-info" style="margin-right: 5px; margin-bottom: 5px; display: inline-block;">${' + escapeHtml(param) + '}</span>';
						});
						paramHtml += '</div>';
					} else {
						paramHtml += '<div style="margin-top: 10px; color: #999;">파라미터가 없습니다</div>';
					}
					
					$('#targetTemplateParameters').html(paramHtml);
					$('#targetTemplateInfo').show();
					
					// 컬럼 매핑 재초기화
					if (sourceColumns.length > 0) {
						initializeColumnMapping();
					}
				} else {
					alert(result.message || '템플릿 상세 조회 실패');
				}
			},
			error: function() {
				alert('템플릿 상세 조회 중 오류가 발생했습니다.');
			}
		});
	}
	
	// 소스 데이터 미리보기
	function previewSourceData() {
		var sourceTemplateId = $('#sourceTemplateId').val();
		var sourceConnectionId = $('#sourceConnectionId').val();
		
		if (!sourceTemplateId || !sourceConnectionId) {
			alert('소스 템플릿과 연결을 선택해주세요.');
			return;
		}
		
		$.ajax({
			type: 'POST',
			url: '/ETL/preview',
			success: function(result) {
				if (result.success) {
					displaySourcePreview(result.rowhead, result.rowbody);
					sourceColumns = result.rowhead || [];
					initializeColumnMapping();
					$('#sourcePreviewContainer').show();
				} else {
					alert(result.message || '데이터 미리보기 실패');
				}
			},
			error: function() {
				alert('데이터 미리보기 중 오류가 발생했습니다.');
			}
		});
	}
	
	// 소스 데이터 미리보기 표시
	function displaySourcePreview(headers, rows) {
		var thead = $('#sourcePreviewTable thead');
		var tbody = $('#sourcePreviewTable tbody');
		
		// 헤더 생성
		var headerRow = $('<tr></tr>');
		headers.forEach(function(header) {
			headerRow.append($('<th></th>').text(header));
		});
		thead.empty().append(headerRow);
		
		// 데이터 행 생성 (최대 10개만 표시)
		tbody.empty();
		var displayRows = rows.slice(0, 10);
		displayRows.forEach(function(row) {
			var tr = $('<tr></tr>');
			row.forEach(function(cell) {
				tr.append($('<td></td>').text(cell || ''));
			});
			tbody.append(tr);
		});
		
		// 행 개수 표시
		$('#sourcePreviewCount').text('총 ' + rows.length + '건 (최대 10건 미리보기)');
	}
	
	// 컬럼 매핑 테이블 초기화
	function initializeColumnMapping() {
		var tbody = $('#columnMappingBody');
		tbody.empty();
		
		if (sourceColumns.length === 0) {
			tbody.append('<tr><td colspan="4" class="text-center text-muted">소스 데이터를 먼저 미리보기 해주세요.</td></tr>');
			return;
		}
		
		var targetType = $('#targetType').val();
		
		sourceColumns.forEach(function(column, index) {
			var row = $('<tr></tr>');
			row.append($('<td></td>').text(column));
			
			// 타겟 방식에 따라 다른 입력 필드
			if (targetType === 'TEMPLATE') {
				// SQL 템플릿 방식: 드롭다운으로 타겟 파라미터 선택
				var select = $('<select class="form-control"></select>');
				select.append($('<option value=""></option>').text('파라미터 선택'));
				
				if (targetTemplateParameters.length > 0) {
					targetTemplateParameters.forEach(function(param) {
						var option = $('<option></option>')
							.attr('value', param)
							.text('${' + escapeHtml(param) + '}');
						// 기본값으로 동일한 이름이 있으면 선택
						if (param === column) {
							option.attr('selected', 'selected');
						}
						select.append(option);
					});
				}
				
				// 직접 입력도 가능하도록
				var input = $('<input type="text" class="form-control" placeholder="또는 직접 입력" style="margin-top: 5px;">');
				row.append($('<td></td>').append(select).append(input));
			} else {
				// 직접 테이블 방식: 텍스트 입력
				row.append($('<td></td>').html(
					'<input type="text" class="form-control" value="' + escapeHtml(column) + '" placeholder="타겟 컬럼명">'
				));
			}
			
			row.append($('<td></td>').html(
				'<input type="text" class="form-control transformation-rule-input" placeholder="가공 규칙">'
			));
			row.append($('<td></td>').html(
				'<input type="text" class="form-control" placeholder="기본값">'
			));
			tbody.append(row);
		});
	}
	
	// ETL 작업 저장
	function saveETLJob() {
		// 유효성 검사
		if (!$('#jobName').val()) {
			alert('작업명을 입력해주세요.');
			$('#jobName').focus();
			return;
		}
		
		if (!$('#sourceTemplateId').val()) {
			alert('소스 SQL 템플릿을 선택해주세요.');
			$('#sourceTemplateId').focus();
			return;
		}
		
		if (!$('#sourceConnectionId').val()) {
			alert('소스 DB 연결을 선택해주세요.');
			$('#sourceConnectionId').focus();
			return;
		}
		
		if (!$('#targetConnectionId').val()) {
			alert('타겟 DB 연결을 선택해주세요.');
			$('#targetConnectionId').focus();
			return;
		}
		
		var targetType = $('#targetType').val();
		if (targetType === 'TABLE') {
			if (!$('#targetTableName').val()) {
				alert('타겟 테이블명을 입력해주세요.');
				$('#targetTableName').focus();
				return;
			}
		} else if (targetType === 'TEMPLATE') {
			if (!$('#targetTemplateId').val()) {
				alert('타겟 SQL 템플릿을 선택해주세요.');
				$('#targetTemplateId').focus();
				return;
			}
		}
		
		if (sourceColumns.length === 0) {
			alert('소스 데이터를 미리보기 해주세요.');
			return;
		}
		
		// 컬럼 매핑 데이터 수집
		var columnMappings = [];
		$('#columnMappingBody tr').each(function() {
			var $row = $(this);
			var sourceCol = $row.find('td:eq(0)').text().trim();
			
			// 타겟 방식에 따라 다른 방식으로 값 추출
			var targetParam = '';
			if (targetType === 'TEMPLATE') {
				var selectVal = $row.find('td:eq(1) select').val();
				var inputVal = $row.find('td:eq(1) input').val().trim();
				targetParam = selectVal || inputVal;
			} else {
				targetParam = $row.find('td:eq(1) input').val().trim();
			}
			
			var transformRule = $row.find('td:eq(2) input').val().trim();
			var defaultValue = $row.find('td:eq(3) input').val().trim();
			
			if (sourceCol && targetParam) {
				columnMappings.push({
					sourceColumn: sourceCol,
					targetParameter: targetParam,
					transformationRule: transformRule,
					defaultValue: defaultValue
				});
			}
		});
		
		if (columnMappings.length === 0) {
			alert('컬럼 매핑을 설정해주세요.');
			return;
		}
		
		// 저장 데이터 구성
		var jobData = {
			jobName: $('#jobName').val(),
			jobDescription: $('#jobDescription').val(),
			jobStatus: $('#jobStatus').val(),
			sourceTemplateId: $('#sourceTemplateId').val(),
			sourceConnectionId: $('#sourceConnectionId').val(),
			sourceParameters: $('#sourceParameters').val(),
			targetConnectionId: $('#targetConnectionId').val(),
			targetType: targetType,
			targetTableName: targetType === 'TABLE' ? $('#targetTableName').val() : null,
			targetTemplateId: targetType === 'TEMPLATE' ? $('#targetTemplateId').val() : null,
			batchSize: parseInt($('#batchSize').val()) || 1000,
			errorHandlingMode: $('#errorHandlingMode').val(),
			columnMappings: columnMappings
		};
		
		// TODO: 실제 저장 로직 구현
		console.log('ETL 작업 저장 데이터:', jobData);
		alert('ETL 작업 저장 기능은 준비 중입니다.\n\n저장될 데이터:\n' + JSON.stringify(jobData, null, 2));
	}
	
	// 취소
	function cancelEdit() {
		if (confirm('작업을 취소하시겠습니까? 변경사항이 저장되지 않습니다.')) {
			// 부모 창으로 돌아가기
			if (typeof parent !== 'undefined' && parent.tabManager) {
				parent.tabManager.removeTab('etl_editor');
			} else {
				window.location.href = '/ETL';
			}
		}
	}
	
	// HTML 이스케이프
	function escapeHtml(text) {
		if (!text) return '';
		return text.toString()
			.replace(/&/g, '&amp;')
			.replace(/</g, '&lt;')
			.replace(/>/g, '&gt;')
			.replace(/"/g, '&quot;')
			.replace(/'/g, '&#39;');
	}
</script>
