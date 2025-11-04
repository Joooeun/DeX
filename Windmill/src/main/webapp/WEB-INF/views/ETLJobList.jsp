<%@include file="common/common.jsp" %>

<style>
	.etl-job-accordion {
		margin-bottom: 10px;
	}
	
	.etl-job-accordion .panel {
		margin-bottom: 0;
		border: 1px solid #ddd;
		border-radius: 4px;
		box-shadow: 0 1px 1px rgba(0,0,0,0.05);
	}
	
	.etl-job-accordion .panel-heading {
		padding: 10px 15px;
		background-color: #f5f5f5;
		border-bottom: 1px solid #ddd;
		cursor: pointer;
		user-select: none;
	}
	
	.etl-job-accordion .panel-heading:hover {
		background-color: #e8e8e8;
	}
	
	.etl-job-accordion .panel-heading.active {
		background-color: #3c8dbc;
		color: white;
	}
	
	.etl-job-accordion .panel-heading.active .job-title,
	.etl-job-accordion .panel-heading.active .job-status {
		color: white;
	}
	
	.etl-job-accordion .job-header-row {
		display: flex;
		justify-content: space-between;
		align-items: center;
		flex-wrap: wrap;
		gap: 10px;
	}
	
	.etl-job-accordion .job-header-left {
		display: flex;
		align-items: center;
		gap: 10px;
		flex: 1;
		min-width: 0;
	}
	
	.etl-job-accordion .job-header-right {
		display: flex;
		align-items: center;
		gap: 5px;
		flex-shrink: 0;
	}
	
	.etl-job-accordion .job-title {
		font-size: 16px;
		font-weight: bold;
		color: #333;
		margin: 0;
		flex: 1;
		min-width: 0;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}
	
	.etl-job-accordion .panel-heading.active .job-title {
		color: white;
	}
	
	.etl-job-accordion .job-status {
		padding: 3px 10px;
		border-radius: 12px;
		font-size: 11px;
		font-weight: bold;
		white-space: nowrap;
	}
	
	.etl-job-accordion .status-active {
		background: #5cb85c;
		color: white;
	}
	
	.etl-job-accordion .status-inactive {
		background: #d9534f;
		color: white;
	}
	
	.etl-job-accordion .status-running {
		background: #f0ad4e;
		color: white;
	}
	
	.etl-job-accordion .toggle-icon {
		margin-right: 5px;
		transition: transform 0.3s;
	}
	
	.etl-job-accordion .panel-heading.active .toggle-icon {
		transform: rotate(90deg);
	}
	
	.etl-job-accordion .panel-body {
		padding: 15px;
		display: none;
	}
	
	.etl-job-accordion .panel-body.show {
		display: block;
	}
	
	.etl-job-accordion .job-info {
		color: #666;
		font-size: 14px;
		margin-bottom: 10px;
	}
	
	.etl-job-accordion .job-info p {
		margin-bottom: 8px;
	}
	
	.etl-job-accordion .job-info span {
		margin-right: 15px;
		display: inline-block;
	}
	
	.etl-job-accordion .job-actions {
		margin-top: 10px;
		text-align: right;
	}
	
	.etl-job-accordion .job-actions .btn {
		margin-left: 5px;
	}
	
	.empty-state {
		text-align: center;
		padding: 60px 20px;
		color: #999;
	}
	
	.empty-state i {
		font-size: 64px;
		margin-bottom: 20px;
		opacity: 0.3;
	}
</style>

<section class="content-header">
	<h1>
		ETL 관리
		<small>데이터 이관 작업 관리</small>
	</h1>
	<ol class="breadcrumb">
		<li><a href="#"><i class="fa fa-dashboard"></i> Home</a></li>
		<li class="active">ETL 관리</li>
	</ol>
</section>

<section class="content">
	<div class="row">
		<!-- 좌측: ETL 작업 목록 -->
		<div class="col-md-8">
			<div class="box box-primary">
				<div class="box-header with-border">
					<h3 class="box-title">ETL 작업 목록</h3>
					<div class="box-tools pull-right">
						<button type="button" class="btn btn-primary btn-sm" onclick="createNewJob()">
							<i class="fa fa-plus"></i> 새 작업 생성
						</button>
					</div>
				</div>
				<div class="box-body">
					<div id="jobListContainer">
						<!-- ETL 작업 카드들이 여기에 동적으로 추가됩니다 -->
					</div>
				</div>
			</div>
		</div>
		
		<!-- 우측: 실행 이력 -->
		<div class="col-md-4">
			<div class="box box-info">
				<div class="box-header with-border">
					<h3 class="box-title">실행 이력</h3>
				</div>
				<div class="box-body">
					<div id="executionHistoryContainer">
						<div class="empty-state">
							<i class="fa fa-history"></i>
							<p>작업을 선택하면 실행 이력이 표시됩니다</p>
						</div>
					</div>
				</div>
			</div>
		</div>
	</div>
</section>

<script>
	$(document).ready(function() {
		loadETLJobs();
	});
	
	// ETL 작업 목록 로드
	function loadETLJobs() {
		$.ajax({
			type: 'GET',
			url: '/ETL/jobs',
			success: function(result) {
				if (result.success) {
					renderJobList(result.data);
				} else {
					showToast(result.message || '작업 목록을 불러오는데 실패했습니다.', 'error');
				}
			},
			error: function(xhr, status, error) {
				showToast('작업 목록 조회 중 오류가 발생했습니다.', 'error');
			}
		});
	}
	
	// 작업 목록 렌더링
	function renderJobList(jobs) {
		var container = $('#jobListContainer');
		container.empty();
		
		if (!jobs || jobs.length === 0) {
			container.html(
				'<div class="empty-state">' +
				'<i class="fa fa-exchange"></i>' +
				'<p>등록된 ETL 작업이 없습니다.</p>' +
				'<button type="button" class="btn btn-primary" onclick="createNewJob()">' +
				'<i class="fa fa-plus"></i> 새 작업 생성' +
				'</button>' +
				'</div>'
			);
			return;
		}
		
		jobs.forEach(function(job) {
			var card = createJobCard(job);
			container.append(card);
		});
	}
	
	// 작업 아코디언 생성
	function createJobCard(job) {
		var statusClass = 'status-' + (job.status || 'inactive').toLowerCase();
		var statusText = job.status === 'ACTIVE' ? '활성' : 
		                job.status === 'RUNNING' ? '실행중' : '비활성';
		
		var lastExecutionInfo = '';
		if (job.lastExecutionTime) {
			lastExecutionInfo = '<span><i class="fa fa-clock-o"></i> ' + escapeHtml(job.lastExecutionTime) + '</span>';
			if (job.lastExecutionStatus) {
				var statusIcon = job.lastExecutionStatus === 'SUCCESS' ? 'fa-check-circle text-success' : 'fa-times-circle text-danger';
				lastExecutionInfo += '<span><i class="fa ' + statusIcon + '"></i> ' + escapeHtml(job.lastExecutionStatus) + '</span>';
			}
		}
		
		var accordionId = 'job-accordion-' + job.jobId;
		
		// 아코디언 패널 생성
		var panel = $('<div class="panel panel-default etl-job-accordion" data-job-id="' + job.jobId + '"></div>');
		
		// 헤더 (한 줄)
		var header = $('<div class="panel-heading" onclick="toggleJobAccordion(\'' + accordionId + '\')"></div>');
		var headerRow = $('<div class="job-header-row"></div>');
		
		var headerLeft = $('<div class="job-header-left"></div>');
		headerLeft.append('<i class="fa fa-chevron-right toggle-icon"></i>');
		headerLeft.append('<span class="job-title">' + escapeHtml(job.jobName) + '</span>');
		headerLeft.append('<span class="job-status ' + statusClass + '">' + statusText + '</span>');
		
		var headerRight = $('<div class="job-header-right"></div>');
		headerRight.append(
			'<button type="button" class="btn btn-xs btn-info" onclick="event.stopPropagation(); editJob(\'' + job.jobId + '\')" title="수정">' +
			'<i class="fa fa-edit"></i>' +
			'</button>'
		);
		headerRight.append(
			'<button type="button" class="btn btn-xs btn-success" onclick="event.stopPropagation(); executeJob(\'' + job.jobId + '\')" title="실행">' +
			'<i class="fa fa-play"></i>' +
			'</button>'
		);
		headerRight.append(
			'<button type="button" class="btn btn-xs btn-danger" onclick="event.stopPropagation(); deleteJob(\'' + job.jobId + '\')" title="삭제">' +
			'<i class="fa fa-trash"></i>' +
			'</button>'
		);
		
		headerRow.append(headerLeft);
		headerRow.append(headerRight);
		header.append(headerRow);
		
		// 본문 (접혀있음)
		var body = $('<div class="panel-body" id="' + accordionId + '"></div>');
		body.append(
			'<div class="job-info">' +
			'<p><strong>설명:</strong> ' + escapeHtml(job.jobDescription || '설명 없음') + '</p>' +
			'<div>' +
			'<span><i class="fa fa-database"></i> <strong>소스:</strong> ' + escapeHtml(job.sourceTemplateName || job.sourceTemplateId) + '</span>' +
			'<span><i class="fa fa-arrow-right"></i> <strong>타겟:</strong> ' + escapeHtml(job.targetTableName || 'N/A') + '</span>' +
			'</div>' +
			lastExecutionInfo +
			'</div>'
		);
		
		panel.append(header);
		panel.append(body);
		
		return panel;
	}
	
	// 아코디언 토글
	function toggleJobAccordion(accordionId) {
		var body = $('#' + accordionId);
		var header = body.prev('.panel-heading');
		
		if (body.hasClass('show')) {
			body.removeClass('show').slideUp(300);
			header.removeClass('active');
		} else {
			body.addClass('show').slideDown(300);
			header.addClass('active');
		}
	}
	
	// 새 작업 생성
	function createNewJob() {
		var templateId = 'etl_editor';
		var title = 'ETL 작업 생성';
		var url = '/ETL/editor';
		
		// iframe 안에서 실행되는 경우 부모 창의 tabManager 사용
		if (typeof parent !== 'undefined' && parent.tabManager) {
			parent.tabManager.addTab(templateId, title, url);
		} else if (typeof parent !== 'undefined' && typeof parent.addTemplateTab === 'function') {
			parent.addTemplateTab(templateId, title, url);
		} else if (typeof window.tabManager !== 'undefined') {
			window.tabManager.addTab(templateId, title, url);
		} else if (typeof window.addTemplateTab === 'function') {
			window.addTemplateTab(templateId, title, url);
		} else {
			// 폴백: 직접 페이지 이동
			window.location.href = url;
		}
	}
	
	// 작업 편집
	function editJob(jobId) {
		var templateId = 'etl_editor_' + jobId;
		var title = 'ETL 작업 편집';
		var url = '/ETL/editor?jobId=' + jobId;
		
		// iframe 안에서 실행되는 경우 부모 창의 tabManager 사용
		if (typeof parent !== 'undefined' && parent.tabManager) {
			parent.tabManager.addTab(templateId, title, url);
		} else if (typeof parent !== 'undefined' && typeof parent.addTemplateTab === 'function') {
			parent.addTemplateTab(templateId, title, url);
		} else if (typeof window.tabManager !== 'undefined') {
			window.tabManager.addTab(templateId, title, url);
		} else if (typeof window.addTemplateTab === 'function') {
			window.addTemplateTab(templateId, title, url);
		} else {
			// 폴백: 직접 페이지 이동
			window.location.href = url;
		}
	}
	
	// 작업 실행
	function executeJob(jobId) {
		if (!confirm('ETL 작업을 실행하시겠습니까?')) {
			return;
		}
		
		showToast('ETL 작업 실행은 준비 중입니다.', 'info');
		// TODO: 실제 실행 로직 구현
	}
	
	// 작업 삭제
	function deleteJob(jobId) {
		if (!confirm('ETL 작업을 삭제하시겠습니까?')) {
			return;
		}
		
		showToast('ETL 작업 삭제는 준비 중입니다.', 'info');
		// TODO: 실제 삭제 로직 구현
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
	
	// 토스트 메시지 표시
	function showToast(message, type) {
		type = type || 'info';
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
		
		var toast = $('<div class="alert ' + bgClass + ' alert-dismissible" style="position: fixed; top: 70px; right: 20px; z-index: 9999; min-width: 300px; animation: slideInRight 0.3s ease-out;">' +
			'<button type="button" class="close" data-dismiss="alert">&times;</button>' +
			'<i class="fa ' + iconClass + '"></i> ' + message +
			'</div>');
		
		$('body').append(toast);
		
		setTimeout(function() {
			toast.fadeOut(300, function() {
				$(this).remove();
			});
		}, 3000);
	}
</script>

