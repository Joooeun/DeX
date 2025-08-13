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
						<div class="row">
							<div class="col-sm-8">
								<div class="row">
									<div class="col-sm-6">
										<div class="input-group"
											style="width: 300px; margin-right: 10px;">
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
									<div class="col-sm-6">
										<div class="input-group" style="width: 200px;">
											<span class="input-group-addon">타입</span> <select
												class="form-control" id="typeFilter"
												onchange="filterByType()">
												<option value="">전체</option>
												<option value="DB">DB</option>
												<option value="HOST">HOST</option>
											</select>
										</div>
									</div>
								</div>
							</div>
							<div class="col-sm-4">
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
									<th>마지막 테스트</th>
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
								<label for="connectionId">연결 ID</label> <input type="text"
									class="form-control" id="connectionId" required>
							</div>
							<div class="col-md-6">
								<label for="connectionType">타입</label> <select
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
								<label for="connectionIP">IP</label> <input type="text"
									class="form-control" id="connectionIP" required>
							</div>
							<div class="col-md-6">
								<label for="connectionPort">포트</label> <input type="text"
									class="form-control" id="connectionPort" required>
							</div>
						</div>

						<!-- DB 연결 전용 필드들 -->
						<div id="dbFields" style="display: none;">
							<div class="form-group row">
								<div class="col-md-6">
									<label for="databaseName">데이터베이스명</label> <input type="text"
										class="form-control" id="databaseName">
								</div>
								<div class="col-md-6">
									<label for="dbType">DB 타입</label> <select class="form-control"
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
									<label for="dbUsername">사용자명</label> <input type="text"
										class="form-control" id="dbUsername">
								</div>
								<div class="col-md-6">
									<label for="dbPassword">비밀번호</label> <input type="password"
										class="form-control" id="dbPassword">
								</div>
							</div>
							<div class="form-group row">
								<div class="col-md-12">
									<label for="jdbcDriverFile">JDBC 드라이버 파일 (선택)</label> <select
										class="form-control" id="jdbcDriverFile">
										<option value="">기본 드라이버 사용</option>
									</select>
									<div id="driverInfoDisplay"
										style="display: none; margin-top: 5px; font-size: 12px; color: #666;"></div>
								</div>
							</div>
							<div class="form-group row">
								<div class="col-md-12">
									<label for="testSql">테스트 SQL (선택)</label>
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
									<label for="sftpUsername">사용자명</label> <input type="text"
										class="form-control" id="sftpUsername">
								</div>
								<div class="col-md-6">
									<label for="sftpPassword">비밀번호</label> <input type="password"
										class="form-control" id="sftpPassword">
								</div>
							</div>
							<div class="form-group row">
								<div class="col-md-6">
									<label for="privateKeyPath">개인키 경로</label> <input type="text"
										class="form-control" id="privateKeyPath"
										placeholder="개인키 경로 (선택)">
								</div>
								<div class="col-md-6">
									<label for="remotePath">원격 경로</label> <input type="text"
										class="form-control" id="remotePath" placeholder="원격 경로 (선택)">
								</div>
							</div>
							<div class="form-group row">
								<div class="col-md-6">
									<label for="connectionTimeout">연결 타임아웃</label> <input
										type="number" class="form-control" id="connectionTimeout"
										value="30">
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


<script
	src="<c:url value='/resources/plugins/jQuery/jQuery-2.1.4.min.js'/>"></script>
<script src="<c:url value='/resources/bootstrap/js/bootstrap.min.js'/>"></script>
<script
	src="<c:url value='/resources/plugins/datatables/datatables.js'/>"></script>

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
					alert(response.message);
				}
			},
			error : function() {
				alert('연결 목록 조회 중 오류가 발생했습니다.');
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
							+ (connection.LAST_CONNECTION_TEST || '-')
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
							+ '<button class="btn btn-xs btn-success" onclick="testConnection(\''
							+ connection.CONNECTION_ID
							+ '\')">테스트</button> '
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
		$('#testResultArea').hide(); // 테스트 결과 영역 숨기기
		$('#testSql').val(''); // 테스트 SQL 초기화
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
					$('#connectionType').val(connectionType);
					$('#connectionIP').val(connection.HOST_IP);
					$('#connectionPort').val(connection.PORT);

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
						$('#sftpUsername').val(connection.USERNAME);
						$('#sftpPassword').val(connection.PASSWORD);
						$('#privateKeyPath').val(connection.PRIVATE_KEY_PATH);
						$('#remotePath').val(connection.REMOTE_PATH);
						$('#connectionTimeout').val(
								connection.CONNECTION_TIMEOUT);
						$('#dbFields').hide();
						$('#sftpFields').show();
					}

					$('#testResultArea').hide(); // 테스트 결과 영역 숨기기
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
			CONNECTION_ID : editConnectionId,
			TYPE : connectionType,
			HOST_IP : $('#connectionIP').val(),
			PORT : $('#connectionPort').val()
		};

		if (connectionType === 'DB') {
			connectionData.DATABASE_NAME = $('#databaseName').val();
			connectionData.DB_TYPE = $('#dbType').val();
			connectionData.USERNAME = $('#dbUsername').val();
			connectionData.PASSWORD = $('#dbPassword').val();
			connectionData.JDBC_DRIVER_FILE = $('#jdbcDriverFile').val();
			connectionData.TEST_SQL = $('#testSql').val();
		} else {
			connectionData.USERNAME = $('#sftpUsername').val();
			connectionData.PASSWORD = $('#sftpPassword').val();
			connectionData.PRIVATE_KEY_PATH = $('#privateKeyPath').val();
			connectionData.REMOTE_PATH = $('#remotePath').val();
			connectionData.CONNECTION_TIMEOUT = $('#connectionTimeout').val();
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
		} else if (connectionType === 'HOST') {
			$('#dbFields').hide();
			$('#sftpFields').show();
		} else {
			$('#dbFields').hide();
			$('#sftpFields').hide();
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
			testData.PRIVATE_KEY_PATH = $('#privateKeyPath').val();
			testData.REMOTE_PATH = $('#remotePath').val();
			testData.CONNECTION_TIMEOUT = $('#connectionTimeout').val();
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
</script>

