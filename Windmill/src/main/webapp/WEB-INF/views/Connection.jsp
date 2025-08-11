<%@include file="common/common.jsp"%>
<style>
.autocomplete-wrapper {
    position: relative;
    z-index:999;
    padding:0;

    #query {
      padding: 5px;
      margin: 0;
      width: 100%;
      border: none;
      font-size: 13px;

      &:focus {
        outline: 0;
      }
    }

    #query-results {
      display: none;
      position: absolute;
      top: 36px;
      left: -1px;
      right: -1px;
      min-height: 50px;
      max-height: 150px;
      overflow: scroll;
      list-style: none;
      padding: 0;
      background-color:rgb(249, 250, 252);

      li {
        padding: 5px;
        margin: 0;
        font-size: 13px;

        &:hover {
          background: #EEE;
          cursor: pointer;
        }
      }
    }
}
</style>
<script>

var arr;
	$(document).ready(
			function() {
				$.ajax({
					type : 'post',
					url : "/Connection/list",
					data : {
						TYPE : ""
					},
					success : function(result) {
						arr=result;
						
						for (var i = 0; i < result.length; i++) {
							$('#connectionlist').append(
									"<option value='" + result[i].split('.')[0]
											+ "'>" + result[i].split('.')[0]
											+ "</option>");
						}
					},
					error : function() {
						alert("시스템 에러");
					}
				});
				
				$('#query').on({
				    "focus": function() {
				    $(this).parent().css('border-color', '#CCCCCC');
				  },
				  "blur": function() {
				    $(this).parent().css('border-color', '#EEEEEE');
				  },
				  "keyup": function() {
				    var results = [];
				        var val = $(this).val();
				    var $queryResults = $('#query-results');
				    var queryResultsMarkup = "";
				    
				    if (val.length > 0) {
				            $queryResults.html("").hide();
				            $.each(arr, function(i) {
				                if (arr[i].match(new RegExp(val,'i'))) {
				                    var $li = $('<li/>')
				                        .html(arr[i])
				                    .attr('data-value', arr[i]);
				                $queryResults.append($li).show();
				            }
				        });

				        $('li').on('click', function() {
				            var selectedVal = $(this).attr('data-value').split(".")[0];
				            $('#query').val(selectedVal);
				            
				            ConnectionDetail(selectedVal);
				            $("#connectionlist").val(selectedVal)

				            $('#query-results').html("").hide();
				        });
				    } else {
				            $queryResults.html("").hide();
				    }
				  }
				});

		// JDBC 드라이버 목록 로드
		loadJdbcDrivers();
	});

	// JDBC 드라이버 목록 로드
	function loadJdbcDrivers() {
		$.ajax({
			type: 'post',
			url: '/Connection/jdbcDrivers',
			success: function(result) {
				var select = $('#JDBC_DRIVER_FILE');
				select.find('option:not(:first)').remove();
				
				result.forEach(function(driver) {
					select.append('<option value="' + driver + '">' + driver + '</option>');
				});
			},
			error: function() {
				console.log('JDBC 드라이버 목록 로드 실패');
			}
		});
	}

	// DB TYPE 변경 시 기본 드라이버 자동 선택
	$(document).on('change', '#DBTYPE', function() {
		var dbType = $(this).val();
		// DB2는 기본 드라이버 사용, 다른 DB는 사용자가 업로드한 드라이버 필요
		if (dbType === 'DB2') {
			$('#JDBC_DRIVER_FILE').val('');
			$('#JDBC_DRIVER_FILE').trigger('change');
		}
	});

	// JDBC 드라이버 파일 선택 시 자동 정보 추출
	$(document).on('change', '#JDBC_DRIVER_FILE', function() {
		var selectedDriver = $(this).val();
		if (selectedDriver) {
			$.ajax({
				type: 'post',
				url: '/Connection/driverInfo',
				data: { driverFile: selectedDriver },
				success: function(result) {
					// 드라이버 정보를 히든 필드에 저장 (나중에 서버에서 사용)
					$('#hidden_driver_info').val(JSON.stringify(result));
					
					// 사용자에게 정보 표시
					var infoText = '드라이버 정보: ' + result.driverClass;
					if (result.version) {
						infoText += ' (버전: ' + result.version + ')';
					}
					$('#driver_info_display').text(infoText).show();
				},
				error: function() {
					$('#driver_info_display').text('드라이버 정보를 가져올 수 없습니다.').show();
				}
			});
		} else {
			$('#driver_info_display').hide();
		}
	});

	/*
	// JDBC 드라이버 목록 새로고침
	function refreshJdbcDrivers() {
		loadJdbcDrivers();
		alert('JDBC 드라이버 목록이 새로고침되었습니다.');
	}

	// JDBC 폴더 열기
	function openJdbcFolder() {
		$.ajax({
			type: 'post',
			url: '/Connection/openJdbcFolder',
			success: function(result) {
				alert('JDBC 폴더가 열렸습니다: ' + result.path);
			},
			error: function() {
				alert('JDBC 폴더 열기 실패');
			}
		});
	}
	*/

	// 연결 테스트 함수
	function testConnection() {
		// 필수 필드 검증
		if (!$('#IP').val() || !$('#PORT').val() || !$('#USER').val() || !$('#PW').val() || !$('#DBTYPE').val()) {
			alert('연결 테스트를 위해 IP, PORT, USER, PW, DB TYPE을 모두 입력해주세요.');
			return;
		}

		// 테스트 버튼 비활성화
		$('button[onclick="testConnection()"]').prop('disabled', true).text('테스트 중...');
		$('#test_result').hide();

		// 테스트 데이터 준비
		var testData = {
			IP: $('#IP').val(),
			PORT: $('#PORT').val(),
			DB: $('#DB').val(),
			USER: $('#USER').val(),
			PW: $('#PW').val(),
			DBTYPE: $('#DBTYPE').val(),
			JDBC_DRIVER_FILE: $('#JDBC_DRIVER_FILE').val()
		};

		$.ajax({
			type: 'post',
			url: '/Connection/testConnection',
			data: testData,
			success: function(result) {
				var resultDiv = $('#test_result');
				if (result.success) {
					resultDiv.html('<div class="alert alert-success"><i class="fa fa-check"></i> 연결 성공!<br>' + 
						'<small>드라이버: ' + (result.driverClass || '기본 드라이버') + '<br>' +
						'버전: ' + (result.version || '알 수 없음') + '<br>' +
						'소요시간: ' + result.duration + 'ms</small></div>');
				} else {
					var errorIcon = '';
					var errorClass = 'alert-danger';
					
					// 오류 유형에 따른 아이콘과 스타일 설정
					if (result.errorType === 'NETWORK_ERROR') {
						errorIcon = '<i class="fa fa-wifi"></i> ';
						errorClass = 'alert-warning';
					} else if (result.errorType === 'AUTHENTICATION_ERROR') {
						errorIcon = '<i class="fa fa-user-times"></i> ';
						errorClass = 'alert-danger';
					} else if (result.errorType === 'DRIVER_NOT_FOUND') {
						errorIcon = '<i class="fa fa-cog"></i> ';
						errorClass = 'alert-info';
					} else if (result.errorType === 'DATABASE_NOT_FOUND') {
						errorIcon = '<i class="fa fa-database"></i> ';
						errorClass = 'alert-warning';
					} else if (result.errorType === 'TIMEOUT_ERROR') {
						errorIcon = '<i class="fa fa-clock-o"></i> ';
						errorClass = 'alert-warning';
					} else if (result.errorType === 'PERMISSION_ERROR') {
						errorIcon = '<i class="fa fa-lock"></i> ';
						errorClass = 'alert-danger';
					} else {
						errorIcon = '<i class="fa fa-exclamation-triangle"></i> ';
					}
					
					var errorHtml = '<div class="alert ' + errorClass + '">' + errorIcon + '연결 실패!<br>' +
						'<small><strong>오류:</strong> ' + result.error + '</small>';
					
					// 원본 오류 메시지가 있으면 추가 표시
					if (result.originalError && result.originalError !== result.error) {
						errorHtml += '<br><small><strong>상세 오류:</strong> ' + result.originalError + '</small>';
					}
					
					errorHtml += '</div>';
					resultDiv.html(errorHtml);
				}
				resultDiv.show();
			},
			error: function(xhr, status, error) {
				var resultDiv = $('#test_result');
				resultDiv.html('<div class="alert alert-danger"><i class="fa fa-times"></i> 연결 테스트 실패!<br>' + 
					'<small>오류: ' + error + '</small></div>');
				resultDiv.show();
			},
			complete: function() {
				// 테스트 버튼 다시 활성화
				$('button[onclick="testConnection()"]').prop('disabled', false).text('연결 테스트');
			}
		});
	}
	
	function ConnectionDetail(value) {
		if (value == 'create') {
			$('#name_input').css("display", "block");
			$("#form_DB").css("display", "block");
			$('#TYPE').val('');
			$('#IP').val('');
			$('#PORT').val('');
			$('#DB').val('');
			$('#USER').val('');
			$('#PW').val('');
			$('#DBTYPE').val('');
			$('#JDBC_DRIVER_FILE').val('');
			return;
		} else {
			$('#name_input').css("display", "none");
		}
		$.ajax({
			type : 'post',
			url : '/Connection/detail',
			data : {
				DB : value
			},
			success : function(result) {
				$('#TYPE').val(result.TYPE);
				$('#IP').val(result.IP);
				$('#PORT').val(result.PORT);
				if (result.TYPE == 'DB') {
					$("#form_DB").css("display", "block");
					$('#DB').val(result.DB);
				} else {
					$("#form_DB").css("display", "none");
				}
				$('#USER').val(result.USER);
				$('#PW').val(result.PW);
				$('#DBTYPE').val(result.DBTYPE);
				$('#DBTYPE').val(result.DBTYPE).prop("selected", true);
				
				// JDBC 드라이버 파일 설정
				if (result.JDBC_DRIVER_FILE) {
					$('#JDBC_DRIVER_FILE').val(result.JDBC_DRIVER_FILE);
					$('#JDBC_DRIVER_FILE').trigger('change');
				} else {
					// DB TYPE에 따른 기본 드라이버 자동 선택
					autoSelectDriver(result.DBTYPE);
				}
			},
			error : function() {
				alert("시스템 에러");
			}
		});
	}
	
	// DB TYPE에 따른 기본 드라이버 자동 선택
	function autoSelectDriver(dbType) {
		// DB2는 기본 드라이버 사용, 다른 DB는 사용자가 업로드한 드라이버 필요
		if (dbType === 'DB2') {
			$('#JDBC_DRIVER_FILE').val('');
			$('#JDBC_DRIVER_FILE').trigger('change');
		}
	}
	function save() {
		var filename = $("#connectionlist").val();
		if (filename == 'create') {
			filename = $('#NAME').val();
		}
		$.ajax({
			type : 'post',
			url : '/Connection/save',
			data : {
				file : filename,
				TYPE : $('#TYPE').val(),
				IP : $('#IP').val(),
				PORT : $('#PORT').val(),
				DB : $('#DB').val(),
				USER : $('#USER').val(),
				PW : $('#PW').val(),
				DBTYPE : $('#DBTYPE').val(),
				JDBC_DRIVER_FILE : $('#JDBC_DRIVER_FILE').val()
			},
			success : function(result) {
				alert("저장 되었습니다.");
			},
			error : function() {
				alert("저장되지 않았습니다.");
			}
		});
	}
</script>
<!-- Content Wrapper. Contains page content -->
<div class="content-wrapper" style="margin-left: 0">
	<!-- Content Header (Page header) -->
	<section class="content-header">
		<h1>Connection관리</h1>
		<ol class="breadcrumb">
			<li>
				<a href="#"><i class="icon ion-ios-home"></i> Home</a>
			</li>
			<li class="active">
				<a href="#">Connection관리</a>
			</li>
		</ol>
	</section>
	<section class="content">
		<div class="row" style="margin: 0">
			<div class="col-md-1 autocomplete-wrapper" style="width: 170px">
				<input class="form-control" type="text" id="query" autocomplete="off" placeholder="아이디 검색">
				<ul id="query-results"></ul>
			</div>
			<div class="col-md-1" style="width: 200px">
				<select id="connectionlist" class="form-control" onchange="ConnectionDetail(this.value)">
					<option value="" selected disabled hidden>==선택하세요==</option>
					<option id="create_option" value="create">새로 만들기</option>
				</select>
			</div>
		</div>
		<div class="box box-default" style="margin-top: 10px;">
			<div class="box-header with-border">
				<h3 class="box-title">Connection Detail</h3>
			</div>
			<!-- /.box-header -->
			<!-- form start -->
			<form role="form-horizontal" onsubmit="save()">
				<div class="box-body">
					<div class="form-group row">
						<div class="col-md-4" style="margin: 2px 0; display: none;" id="name_input">
							<label for="NAME">NAME</label>
							<input type="text" class="form-control" id="NAME" placeholder="NAME" name="NAME">
						</div>
						<div class="col-md-4" style="margin: 2px 0;">
							<label for="TYPE">TYPE</label>
							<select class="form-control" required="required" pattern="\S(.*\S)?" title="공백은 입력할 수 없습니다." id="TYPE" name="TYPE">
								<option value="" selected disabled hidden>TYPE</option>
								<option value="DB">DB</option>
								<option value="HOST">HOST</option>
							</select>
						</div>
					</div>
					<div class="form-group row">
						<div class="col-md-4" style="margin: 2px 0;">
							<label for="IP">IP</label>
							<input type="text" required="required" pattern="\S(.*\S)?" title="공백은 입력할 수 없습니다." class="form-control" id="IP" placeholder="IP" name="IP">
						</div>
						<div class="col-md-4" style="margin: 2px 0;">
							<label for="PORT">PORT</label>
							<input type="text" required="required" pattern="\S(.*\S)?" title="공백은 입력할 수 없습니다." class="form-control" id="PORT" placeholder="PORT" name="PORT">
						</div>
						<div class="col-md-4" style="margin: 2px 0;" id="form_DB">
							<label for="DB">DB</label>
							<input type="text" class="form-control" id="DB" placeholder="DB" name="DB">
						</div>
					</div>
					<div class="form-group row">
						<div class="col-md-4" style="margin: 2px 0;">
							<label for="DBTYPE">DB TYPE</label>
							<select class="form-control" id="DBTYPE" name="DBTYPE">
								<option value="" selected disabled hidden>DB TYPE</option>
								<option value="ORACLE">ORACLE</option>
								<option value="DB2">DB2</option>
								<option value="TIBERO">TIBERO</option>
								<option value="POSTGRESQL">POSTGRESQL</option>
								<option value="MYSQL">MYSQL</option>
							</select>
						</div>
						<div class="col-md-4" style="margin: 2px 0;">
							<label for="USER">USER</label>
							<input type="text" class="form-control" required="required" pattern="\S(.*\S)?" title="공백은 입력할 수 없습니다." id="USER" placeholder="USER" name="USER">
						</div>
						<div class="col-md-4" style="margin: 2px 0;">
							<label for="PW">PW</label>
							<input type="text" class="form-control" required="required" pattern="\S(.*\S)?" title="공백은 입력할 수 없습니다." id="PW" placeholder="PW" name="PW">
						</div>
					</div>


					<div class="form-group row">
						<div class="col-md-4" style="margin: 2px 0;">
							<label for="JDBC_DRIVER_FILE">JDBC Driver File (선택)</label>
							<select class="form-control" id="JDBC_DRIVER_FILE" name="JDBC_DRIVER_FILE">
								<option value="">기본 드라이버 사용</option>
							</select>
							<div id="driver_info_display" style="display:none; margin-top: 5px; font-size: 12px; color: #666;"></div>
							<input type="hidden" id="hidden_driver_info" name="hidden_driver_info" value="">
						</div>
						<!-- 
						<div class="col-md-4" style="margin: 2px 0;">
							<label>&nbsp;</label>
							<button type="button" class="btn btn-info form-control" onclick="refreshJdbcDrivers()">JDBC 드라이버 목록 새로고침</button>
						</div>
						<div class="col-md-4" style="margin: 2px 0;">
							<label>&nbsp;</label>
							<button type="button" class="btn btn-success form-control" onclick="openJdbcFolder()">JDBC 폴더 열기</button>
						</div>
						-->
					</div>
				</div>
				<!-- /.box-body -->
				<div class="box-footer">
					<div class="row">
						<div class="col-md-6">
							<button type="button" class="btn btn-success form-control" onclick="testConnection()">연결 테스트</button>
						</div>
						<div class="col-md-6">
							<button type="submit" class="btn btn-primary form-control">저장</button>
						</div>
					</div>
					<div id="test_result" style="margin-top: 10px; display: none;">
						<!-- 테스트 결과가 여기에 표시됩니다 -->
					</div>
				</div>
			</form>
		</div>
	</section>
</div>
