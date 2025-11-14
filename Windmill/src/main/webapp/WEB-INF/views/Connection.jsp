<%@include file="common/common.jsp"%>

<div class="content-wrapper" style="margin-left: 0">
	<section class="content-header">
		<h1>연결 관리</h1>
		<ol class="breadcrumb">
			<li><a href="#"><i class="fa fa-dashboard"></i> Home</a></li>
			<li class="active">연결 관리</li>
		</ol>
	</section>

	<section class="content">
		<div class="row">
			<div class="col-md-12">
				<div class="box">
					<div class="box-header with-border">
                        <h3 class="box-title">연결 목록</h3>
                       
                        <div class="row" style="margin-top: 10px;">
							<div class="col-sm-3">
								<div class="input-group input-group-sm">
									<input type="text" class="form-control" id="searchKeyword"
										placeholder="연결명 또는 IP로 검색..."> <span
										class="input-group-btn">
										<button type="button" class="btn btn-default"
											onclick="searchConnections()">
											<i class="fa fa-search"></i>
										</button>
									</span>
								</div>
							</div>
							<div class="col-sm-3">
								<div class="input-group input-group-sm">
									<span class="input-group-addon">타입</span> <select
										class="form-control" id="typeFilter"
										onchange="filterByType()">
										<option value="">전체</option>
										<option value="DB">DB</option>
										<option value="HOST">HOST</option>
									</select>
								</div>
							</div>
							<div class="col-sm-6">
								<button type="button" class="btn pull-right btn-primary btn-sm"
									onclick="showCreateConnectionModal()">
									<i class="fa fa-plus"></i> 새 연결
								</button>
							</div>
						</div>
					</div>
					<div class="box-body">
						<table id="connectionTable"
							class="table table-bordered table-striped">
							<thead>
								<tr>
									<th>연결명</th>
									<th>타입</th>
									<th>IP</th>
									<th>포트</th>
									<th>데이터베이스</th>
									<th>사용자</th>
									<th>상태</th>
									<th>생성일</th>
									<th>관리</th>
								</tr>
							</thead>
							<tbody>
							</tbody>
						</table>

						<!-- 페이징 컨트롤 -->
						<div class="text-center">
							<ul class="pagination" id="pagination">
							</ul>
							<div class="pagination-info">
								<span id="paginationInfo"></span>
							</div>
						</div>
					</div>
				</div>
			</div>
		</div>
	</section>

	<!-- 연결 생성/수정 모달 -->
	<div class="modal fade" id="connectionModal" tabindex="-1"
		role="dialog">
		<div class="modal-dialog modal-lg" role="document">
			<div class="modal-content">
				<div class="modal-header">
					<button type="button" class="close" data-dismiss="modal">&times;</button>
					<h4 class="modal-title" id="connectionModalTitle">연결 생성</h4>
				</div>
				<div class="modal-body">
					<form id="connectionForm">
						<input type="hidden" id="editConnectionId">
						<div class="form-group row">
							<div class="col-md-6">
								<label for="connectionId" data-toggle="tooltip" data-placement="top" title="연결의 고유 식별자입니다. 영문, 숫자, 언더스코어만 사용 가능하며, 중복되지 않아야 합니다.">연결 ID</label> <input type="text"
									class="form-control" id="connectionId" required>
							</div>
							<div class="col-md-6">
								<label for="connectionType" data-toggle="tooltip" data-placement="top" title="연결 유형을 선택합니다. DB: 데이터베이스 연결, HOST: SFTP/SSH 연결">타입</label> <select
									class="form-control" id="connectionType" required
									onchange="updateFormByType(this.value)">
									<option value="">타입 선택</option>
									<option value="DB">DB</option>
									<option value="HOST">HOST</option>
								</select>
							</div>
						</div>
						<div class="form-group row">
							<div class="col-md-6">
								<label for="connectionIP" data-toggle="tooltip" data-placement="top" title="연결할 서버의 IP 주소 또는 호스트명을 입력합니다. 예: 192.168.1.100 또는 db.example.com">IP</label> <input type="text"
									class="form-control" id="connectionIP" required>
							</div>
							<div class="col-md-6">
								<label for="connectionPort" data-toggle="tooltip" data-placement="top" title="연결할 서비스의 포트 번호를 입력합니다. DB: 1521(Oracle), 50000(DB2), 5432(PostgreSQL), HOST: 22(SFTP)">포트</label> <input type="text"
									class="form-control" id="connectionPort" required>
							</div>
						</div>

						<!-- DB 연결 전용 필드들 -->
						<div id="dbFields" style="display: none;">
							<div class="form-group row">
								<div class="col-md-6">
									<label for="databaseName" data-toggle="tooltip" data-placement="top" title="연결할 데이터베이스의 이름을 입력합니다. Oracle: SID 또는 Service Name, DB2: Database Name, PostgreSQL: Database Name">데이터베이스명</label> <input type="text"
										class="form-control" id="databaseName">
								</div>
								<div class="col-md-6">
									<label for="dbType" data-toggle="tooltip" data-placement="top" title="연결할 데이터베이스의 종류를 선택합니다. 각 DB 타입에 맞는 JDBC 드라이버와 연결 문자열이 자동으로 설정됩니다.">DB 타입</label> <select class="form-control"
										id="dbType">
										<option value="">DB 타입 선택</option>
										<option value="ORACLE">ORACLE</option>
										<option value="DB2">DB2</option>
										<option value="TIBERO">TIBERO</option>
										<option value="POSTGRESQL">POSTGRESQL</option>
										<option value="MYSQL">MYSQL</option>
									</select>
								</div>
							</div>
							<div class="form-group row">
								<div class="col-md-6">
									<label for="dbUsername" data-toggle="tooltip" data-placement="top" title="데이터베이스에 접속할 사용자 계정명을 입력합니다. 해당 사용자는 필요한 권한을 가지고 있어야 합니다.">사용자명</label> <input type="text"
										class="form-control" id="dbUsername">
								</div>
								<div class="col-md-6">
									<label for="dbPassword" data-toggle="tooltip" data-placement="top" title="데이터베이스 사용자 계정의 비밀번호를 입력합니다. 보안을 위해 암호화되어 저장됩니다.">비밀번호</label> <input type="password"
										class="form-control" id="dbPassword">
								</div>
							</div>
							<div class="form-group row">
								<div class="col-md-12">
									<label for="jdbcDriverFile" data-toggle="tooltip" data-placement="top" title="사용할 JDBC 드라이버 JAR 파일을 선택합니다. 기본 드라이버가 지원되지 않는 경우 특정 버전의 드라이버를 사용할 수 있습니다.">JDBC 드라이버 파일 (선택)</label> <select
										class="form-control" id="jdbcDriverFile">
										<option value="">기본 드라이버 사용</option>
									</select>
									<div id="driverInfoDisplay"
										style="display: none; margin-top: 5px; font-size: 12px; color: #666;"></div>
								</div>
							</div>
							<div class="form-group row">
								<div class="col-md-12">
									<label for="testSql" data-toggle="tooltip" data-placement="top" title="연결 테스트 시 실행할 SQL을 입력합니다. 비워두면 기본 테스트 쿼리가 사용되며, 각 DB 타입에 맞는 간단한 쿼리를 입력하세요.">테스트 SQL (선택)</label>
									<textarea class="form-control" id="testSql" rows="3"
										placeholder="연결 테스트 시 실행할 SQL을 입력하세요. 비워두면 기본 테스트 쿼리가 사용됩니다."></textarea>
									<small class="form-text text-muted">예: SELECT 1, SELECT
										COUNT(*) FROM DUAL, SELECT CURRENT_TIMESTAMP</small>
								</div>
							</div>
						</div>

						<!-- SFTP 연결 전용 필드들 -->
						<div id="sftpFields" style="display: none;">
							<div class="form-group row">
								<div class="col-md-6">
									<label for="sftpUsername" data-toggle="tooltip" data-placement="top" title="SFTP 서버에 접속할 사용자 계정명을 입력합니다. SSH 키 인증을 사용하는 경우 비밀번호는 비워둘 수 있습니다.">사용자명</label> <input type="text"
										class="form-control" id="sftpUsername">
								</div>
								<div class="col-md-6">
									<label for="sftpPassword" data-toggle="tooltip" data-placement="top" title="SFTP 사용자 계정의 비밀번호를 입력합니다. SSH 키 인증을 사용하는 경우 비워둘 수 있으며, 보안을 위해 암호화되어 저장됩니다.">비밀번호</label> <input type="password"
										class="form-control" id="sftpPassword">
								</div>
							</div>
						</div>

						<!-- 공통 필드들 -->
						<div class="form-group row">
							<div class="col-md-6">
								<label for="connectionStatus">상태</label>
								<select class="form-control" id="connectionStatus">
									<option value="ACTIVE">활성</option>
									<option value="INACTIVE">비활성</option>
									<option value="MAINTENANCE">점검중</option>
								</select>
							</div>
						</div>
						
						<!-- 모니터링 필드들 (DB 연결에서만 표시) -->
						<div id="monitoringFields">
							<div class="form-group row">
								<div class="col-md-6">
									<label for="monitoringEnabled">모니터링 활성화</label>
									<select class="form-control" id="monitoringEnabled">
										<option value="true">활성화</option>
										<option value="false">비활성화</option>
									</select>
								</div>
							</div>
							<div class="form-group row">
								<div class="col-md-6">
									<label for="monitoringInterval">모니터링 간격 (초)</label>
									<input type="number" class="form-control" id="monitoringInterval" 
										value="300" min="60" max="3600" 
										placeholder="60초 ~ 3600초 (기본값: 300초)">
									<small class="form-text text-muted">연결 상태를 확인하는 간격을 설정합니다.</small>
								</div>
							</div>
						</div>
					</form>
				</div>
				<div class="modal-footer">
					<button type="button" class="btn btn-success"
						onclick="testConnection()">연결 테스트</button>
					<button type="button" class="btn btn-default" data-dismiss="modal">취소</button>
					<button type="button" class="btn btn-primary"
						onclick="saveConnection()">저장</button>
				</div>

				<!-- 테스트 결과 표시 영역 -->
				<div id="testResultArea" style="display: none; margin-top: 10px;">
					<div class="alert" id="testResultAlert" role="alert">
						<span id="testResultIcon"></span> <span id="testResultMessage"></span>
					</div>
				</div>
			</div>
		</div>
	</div>
</div>



<script>
	$(document).ready(function() {
		loadConnectionTable();
		loadJdbcDrivers();

		// 검색 필드에서 Enter 키 이벤트 처리
		$('#searchKeyword').on('keypress', function(e) {
			if (e.which === 13) { // Enter 키
				searchConnections();
			}
		});

		// 실시간 검색 (타이핑 후 500ms 대기)
		var searchTimeout;
		$('#searchKeyword').on('input', function() {
			clearTimeout(searchTimeout);
			searchTimeout = setTimeout(function() {
				currentPage = 1; // 검색 시 첫 페이지로 이동
				searchConnections();
			}, 500);
		});
	});

	// 전역 변수로 현재 페이지 관리
	var currentPage = 1;

	// 연결 목록 테이블 로드
	function loadConnectionTable(page) {
		if (page) {
			currentPage = page;
		}

		var searchKeyword = $('#searchKeyword').val();
		var typeFilter = $('#typeFilter').val();

		$.ajax({
			url : '/Connection/list',
			type : 'GET',
			data : {
				searchKeyword : searchKeyword,
				typeFilter : typeFilter,
				page : currentPage,
				pageSize : 10
			},
			success : function(response) {
				if (response.success) {
					displayConnectionTable(response.data);
					displayPagination(response.pagination);
				} else {
					showToast(response.message, 'error');
				}
			},
			error : function() {
				showToast('연결 목록 조회 중 오류가 발생했습니다.', 'error');
			}
		});
	}

	// 연결 테이블 표시
	function displayConnectionTable(connections) {
		var tbody = $('#connectionTable tbody');
		tbody.empty();

		connections
				.forEach(function(connection) {
					var row = '<tr>' + '<td>'
							+ connection.CONNECTION_ID
							+ '</td>'
							+ '<td>'
							+ getTypeBadge(connection.TYPE)
							+ '</td>'
							+ '<td>'
							+ connection.HOST_IP
							+ '</td>'
							+ '<td>'
							+ connection.PORT
							+ '</td>'
							+ '<td>'
							+ (connection.DATABASE_NAME || '-')
							+ '</td>'
							+ '<td>'
							+ connection.USERNAME
							+ '</td>'
							+ '<td>'
							+ getStatusBadge(connection.STATUS)
							+ '</td>'
							+ '<td>'
							+ formatDate(connection.CREATED_TIMESTAMP)
							+ '</td>'
							+ '<td>'
							+ '<button class="btn btn-xs btn-info" onclick="editConnection(\''
							+ connection.CONNECTION_ID
							+ '\', \''
							+ connection.TYPE
							+ '\')">수정</button> '
							+ '<button class="btn btn-xs btn-danger" onclick="deleteConnection(\''
							+ connection.CONNECTION_ID + '\', \''
							+ connection.TYPE + '\')">삭제</button>' + '</td>'
							+ '</tr>';
					tbody.append(row);
				});
	}

	// 타입 배지 생성
	function getTypeBadge(type) {
		if (type === 'DB') {
			return '<span class="label label-primary">DB</span>';
		} else {
			return '<span class="label label-success">HOST</span>';
		}
	}

	// 상태 배지 생성
	function getStatusBadge(status) {
		if (status === 'ACTIVE') {
			return '<span class="label label-success">활성</span>';
		} else if (status === 'INACTIVE') {
			return '<span class="label label-warning">비활성</span>';
		} else {
			return '<span class="label label-danger">삭제됨</span>';
		}
	}

	// 날짜 포맷팅
	function formatDate(dateString) {
		if (!dateString)
			return '-';
		
		// 13자리 숫자(밀리초 타임스탬프)인지 확인
		if (typeof dateString === 'number' || (typeof dateString === 'string' && /^\d{13}$/.test(dateString))) {
			// 13자리 타임스탬프를 Date 객체로 변환
			var date = new Date(parseInt(dateString));
			return date.toLocaleDateString('ko-KR');
		}
		
		// 일반 날짜 문자열 처리
		var date = new Date(dateString);
		return date.toLocaleDateString('ko-KR');
	}

	// 연결 검색
	function searchConnections() {
		currentPage = 1;
		loadConnectionTable();
	}

	// 타입별 필터링
	function filterByType() {
		currentPage = 1;
		loadConnectionTable();
	}

	// 페이징 UI 표시
	function displayPagination(pagination) {
		var paginationContainer = $('#pagination');
		var paginationInfo = $('#paginationInfo');

		paginationContainer.empty();

		var currentPage = pagination.currentPage;
		var totalPages = pagination.totalPages;
		var totalCount = pagination.totalCount;
		var pageSize = pagination.pageSize;

		// 페이징 정보 표시
		var startItem = (currentPage - 1) * pageSize + 1;
		var endItem = Math.min(currentPage * pageSize, totalCount);
		paginationInfo.text('전체 ' + totalCount + '개 중 ' + startItem + '-'
				+ endItem + '개 표시');

		if (totalPages <= 1) {
			return; // 페이지가 1개 이하면 페이징 버튼 숨김
		}

		// 이전 페이지 버튼
		if (currentPage > 1) {
			paginationContainer
					.append('<li><a href="#" onclick="loadConnectionTable('
							+ (currentPage - 1) + ')">&laquo;</a></li>');
		}

		// 페이지 번호 버튼
		var startPage = Math.max(1, currentPage - 2);
		var endPage = Math.min(totalPages, currentPage + 2);

		for (var i = startPage; i <= endPage; i++) {
			var activeClass = i === currentPage ? 'class="active"' : '';
			paginationContainer
					.append('<li ' + activeClass + '><a href="#" onclick="loadConnectionTable('
							+ i + ')">' + i + '</a></li>');
		}

		// 다음 페이지 버튼
		if (currentPage < totalPages) {
			paginationContainer
					.append('<li><a href="#" onclick="loadConnectionTable('
							+ (currentPage + 1) + ')">&raquo;</a></li>');
		}
	}

	// 새 연결 모달 표시
	function showCreateConnectionModal() {
		$('#connectionModalTitle').text('연결 생성');
		$('#connectionForm')[0].reset();
		$('#editConnectionId').val('');
		$('#dbFields').hide();
		$('#sftpFields').hide();
		$('#monitoringFields').hide(); // 모니터링 필드 숨김
		$('#testResultArea').hide(); // 테스트 결과 영역 숨기기
		$('#testSql').val(''); // 테스트 SQL 초기화
		
		// 기본값 설정
		$('#connectionStatus').val('ACTIVE');
		
		$('#connectionModal').modal('show');
	}

	// 연결 수정 모달 표시
	function editConnection(connectionId, connectionType) {
		$.ajax({
			url : '/Connection/detail',
			type : 'GET',
			data : {
				connectionId : connectionId,
				connectionType : connectionType
			},
			success : function(response) {
				if (response.success) {
					var connection = response.data;
					$('#connectionModalTitle').text('연결 수정');
					$('#editConnectionId').val(connectionId);

					// 폼 필드 설정
					$('#connectionId').val(connection.CONNECTION_ID);
					console.log(connection)
					$('#connectionType').val(connectionType);
					$('#connectionIP').val(connection.HOST_IP);
					$('#connectionPort').val(connection.PORT);
					
					// 상태 설정
					$('#connectionStatus').val(connection.STATUS || 'ACTIVE');
					
					// 모니터링 필드는 DB 타입일 때만 설정
					if (connectionType === 'DB') {
						$('#monitoringEnabled').val(connection.MONITORING_ENABLED !== false ? 'true' : 'false');
						$('#monitoringInterval').val(connection.MONITORING_INTERVAL || 300);
					}

					if (connectionType === 'DB') {
						$('#databaseName').val(connection.DATABASE_NAME);
						$('#dbType').val(connection.DB_TYPE);
						$('#dbUsername').val(connection.USERNAME);
						$('#dbPassword').val(connection.PASSWORD);
						$('#jdbcDriverFile').val(connection.JDBC_DRIVER_FILE);
						$('#testSql').val(connection.TEST_SQL);
						$('#dbFields').show();
						$('#sftpFields').hide();
					} else {
						// SFTP 연결의 경우에도 connectionId 설정
						$('#connectionId').val(connection.CONNECTION_ID);
						$('#sftpUsername').val(connection.USERNAME);
						$('#sftpPassword').val(connection.PASSWORD);
						$('#dbFields').hide();
						$('#sftpFields').show();
					}

					$('#testResultArea').hide(); // 테스트 결과 영역 숨기기
					
					// 타입에 따른 폼 업데이트 (모니터링 필드 표시/숨김)
					updateFormByType(connectionType);
					
					$('#connectionModal').modal('show');
				} else {
					alert(response.message);
				}
			}
		});
	}

	// 연결 저장
	function saveConnection() {
		var editConnectionId = $('#editConnectionId').val();
		var connectionType = $('#connectionType').val();

		var connectionData = {
			editConnectionId : editConnectionId,
			TYPE : connectionType,
			HOST_IP : $('#connectionIP').val(),
			PORT : $('#connectionPort').val(),
			STATUS : $('#connectionStatus').val()
		};
		
		// 모니터링 필드는 DB 타입일 때만 추가
		if (connectionType === 'DB') {
			connectionData.MONITORING_ENABLED = $('#monitoringEnabled').val() === 'true';
			connectionData.MONITORING_INTERVAL = parseInt($('#monitoringInterval').val()) || 300;
			connectionData.CONNECTION_ID = $('#connectionId').val();
			connectionData.DATABASE_NAME = $('#databaseName').val();
			connectionData.DB_TYPE = $('#dbType').val();
			connectionData.USERNAME = $('#dbUsername').val();
			connectionData.PASSWORD = $('#dbPassword').val();
			connectionData.JDBC_DRIVER_FILE = $('#jdbcDriverFile').val();
			connectionData.TEST_SQL = $('#testSql').val();
		} else {
			connectionData.CONNECTION_ID = $('#connectionId').val(); // SFTP 연결에서도 ID 설정
			connectionData.USERNAME = $('#sftpUsername').val();
			connectionData.PASSWORD = $('#sftpPassword').val();
		}

		$.ajax({
			url : '/Connection/save',
			type : 'POST',
			data : connectionData,
			success : function(response) {
				if (response.success) {
					alert(response.message);
					$('#connectionModal').modal('hide');
					loadConnectionTable(currentPage);
				} else {
					alert('저장 실패: ' + response.message);
				}
			},
			error : function() {
				alert('저장 중 오류가 발생했습니다.');
			}
		});
	}

	// 연결 삭제
	function deleteConnection(connectionId, connectionType) {
		if (!confirm('정말 삭제하시겠습니까?'))
			return;

		$.ajax({
			url : '/Connection/delete',
			type : 'POST',
			data : {
				connectionId : connectionId,
				connectionType : connectionType
			},
			success : function(response) {
				if (response.success) {
					alert(response.message);
					loadConnectionTable(currentPage);
				} else {
					alert('삭제 실패: ' + response.message);
				}
			},
			error : function() {
				alert('삭제 중 오류가 발생했습니다.');
			}
		});
	}

	// TYPE에 따른 폼 업데이트 (모달용)
	function updateFormByType(connectionType) {
		if (connectionType === 'DB') {
			$('#dbFields').show();
			$('#sftpFields').hide();
			$('#monitoringFields').show(); // DB 연결에서만 모니터링 필드 표시
		} else if (connectionType === 'HOST') {
			$('#dbFields').hide();
			$('#sftpFields').show();
			$('#monitoringFields').hide(); // SFTP 연결에서는 모니터링 필드 숨김
		} else {
			$('#dbFields').hide();
			$('#sftpFields').hide();
			$('#monitoringFields').hide();
		}
	}

	// JDBC 드라이버 목록 로드
	function loadJdbcDrivers() {
		$.ajax({
			url : '/Connection/jdbcDrivers',
			type : 'GET',
			success : function(response) {
				if (response.success) {
					var select = $('#jdbcDriverFile');
					select.empty();
					select.append('<option value="">기본 드라이버 사용</option>');

					response.data.forEach(function(driver) {
						select.append('<option value="' + driver + '">'
								+ driver + '</option>');
					});
				}
			}
		});
	}

	// 연결 테스트
	function testConnection(connectionId) {

		// 모달에서 새 연결 테스트
		var connectionType = $('#connectionType').val();
		if (!connectionType) {
			showTestResult(false, '연결 타입을 선택해주세요.');
			return;
		}

		// 테스트 버튼 비활성화
		$('button[onclick="testConnection()"]').prop('disabled', true).text(
				'테스트 중...');

		// 이전 테스트 결과 숨기기
		$('#testResultArea').hide();

		var testData = {
			TYPE : connectionType,
			HOST_IP : $('#connectionIP').val(),
			PORT : $('#connectionPort').val()
		};

		if (connectionType === 'DB') {
			testData.DATABASE_NAME = $('#databaseName').val();
			testData.DB_TYPE = $('#dbType').val();
			testData.USERNAME = $('#dbUsername').val();
			testData.PASSWORD = $('#dbPassword').val();
			testData.JDBC_DRIVER_FILE = $('#jdbcDriverFile').val();
			testData.TEST_SQL = $('#testSql').val();
		} else {
			testData.USERNAME = $('#sftpUsername').val();
			testData.PASSWORD = $('#sftpPassword').val();
		}

		$.ajax({
			url : '/Connection/test',
			type : 'POST',
			data : testData,
			success : function(response) {
				if (response.success) {
					showTestResult(true, response.message);
				} else {
					showTestResult(false, response.message);
				}
			},
			error : function() {
				showTestResult(false, '연결 테스트 중 오류가 발생했습니다.');
			},
			complete : function() {
				// 테스트 버튼 다시 활성화
				$('button[onclick="testConnection()"]').prop('disabled', false)
						.text('연결 테스트');
			}
		});

	}

	// 테스트 결과를 화면에 표시
	function showTestResult(success, message) {
		var resultArea = $('#testResultArea');
		var resultAlert = $('#testResultAlert');
		var resultIcon = $('#testResultIcon');
		var resultMessage = $('#testResultMessage');

		// 결과 영역 표시
		resultArea.show();

		// 성공/실패에 따른 스타일 설정
		if (success) {
			resultAlert.removeClass('alert-danger alert-warning').addClass(
					'alert-success');
			resultIcon.html('<i class="fa fa-check-circle"></i> ');
			resultMessage.text(message);
		} else {
			resultAlert.removeClass('alert-success alert-warning').addClass(
					'alert-danger');
			resultIcon.html('<i class="fa fa-times-circle"></i> ');
			resultMessage.text(message);
		}

		// 모달 스크롤을 결과 영역으로 이동
		$('#connectionModal').scrollTop($('#connectionModal')[0].scrollHeight);
	}
	
	// ========================================
	// Toast 알림 시스템
	// ========================================
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
		setTimeout(function() {
			$('#' + toastId).fadeOut(300, function() {
				$(this).remove();
			});
		}, duration);
	}
</script>

<!-- Toast 알림 컨테이너 -->
<div id="toastContainer" style="position: fixed; top: 20px; right: 20px; z-index: 9999; width: 350px;"></div>

<style>
@keyframes slideInDown {
    from {
        transform: translateY(-100%);
        opacity: 0;
    }
    to {
        transform: translateY(0);
        opacity: 1;
    }
}
</style>

