<meta content='width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no' name='viewport'>
<meta http-equiv="Cache-Control" content="no-cache, no-store, must-revalidate" />

<script>
window.sentryOnLoad = function () {
	Sentry.init({
		dsn: "https://8e6f31672463d08b3758a6475c825ae7@o4510201747996672.ingest.us.sentry.io/4510201751470080",
	  	integrations: [Sentry.browserTracingIntegration()],
		sendDefaultPii: true,
		// Performance Monitoring
		tracesSampleRate: 1.0, // 데이터 샘플링 비율 설정
		// Session Replay
		replaysSessionSampleRate: 0.1,
		replaysOnErrorSampleRate: 1.0,
	});
};
</script>
<script>
	// 시스템 에러 알림 함수 (ES5 호환)
	function showSystemError(errorMessage, errorDetails) {
		
		// 사용자에게 상세한 에러 메시지 표시
		var userMessage = errorMessage+"\n\n";
		
		if (errorDetails && errorDetails.error) {
			userMessage += "에러: " + (errorDetails.error || '알 수 없는 에러') + "\n";
		}
		
		if (errorDetails && errorDetails.url) {
			userMessage += "요청 URL: " + errorDetails.url + "\n";
		}
		if (errorDetails && errorDetails.status) {
			userMessage += "상태 코드: " + errorDetails.status + "\n";
		}
		userMessage += "\n자세한 내용은 관리자에게 문의하세요.";
		
		alert(userMessage);
		Sentry.captureException(errorDetails); 
		
	}
</script>
<script
  src="https://js.sentry-cdn.com/8e6f31672463d08b3758a6475c825ae7.min.js"
  crossorigin="anonymous"
></script>

<link href="/resources/bootstrap/css/bootstrap.css" rel="stylesheet" type="text/css" />
<link href="/resources/dist/css/AdminLTE.css" rel="stylesheet" type="text/css" />
<link href="/resources/dist/css/skins/_all-skins.min.css" rel="stylesheet" type="text/css" />

<link href="/resources/ionicons/2.0.1/css/ionicons.min.css" rel="stylesheet" type="text/css" />
<link href="/resources/font-awesome-4.7.0/css/font-awesome.min.css" rel="stylesheet" type="text/css" />
<script src="/resources/plugins/jQuery/jquery.min.js"></script>
<script src="/resources/bootstrap/js/bootstrap.min.js"></script>
<script src="/resources/plugins/chartjs/ChartJs.js"></script>
<script src="https://cdn.jsdelivr.net/npm/chartjs-plugin-datalabels@2.2.0"></script>
<script src="https://cdn.jsdelivr.net/npm/chartjs-plugin-annotation@3.1.0/dist/chartjs-plugin-annotation.min.js"></script>


<link rel="shortcut icon" href="#">

<link href="/resources/plugins/datatables/datatables.min.css" rel="stylesheet">
<script src="/resources/plugins/datatables/datatables.min.js"></script>

<link href="/resources/dist/css/tabulator/tabulator.min.css" rel="stylesheet">
<script type="text/javascript" src="/resources/dist/js/tabulator/tabulator.js"></script>
<script type="text/javascript" src="/resources/dist/js/tabulator/xlsx.full.min.js"></script>


<script src="/resources/jquery-slimscroll/jquery.slimscroll.js"></script>

<link href="/resources/dist/css/tabulator/tabulator_bootstrap3.css" rel="stylesheet">


<script src="/resources/dist/js/adminlte.js"></script>
<script src='/resources/plugins/fastclick/fastclick.min.js'></script>

<!-- Select2 로컬 파일 -->
<link href="/resources/plugins/select2/select2.min.css" rel="stylesheet" />
<script src="/resources/plugins/select2/select2.min.js"></script>

<!-- Google Fonts - 고정폭 폰트 -->
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Noto+Sans+Mono:wght@100;200;300;400;500;600;700;800;900&family=Roboto+Mono:ital,wght@0,100;0,200;0,300;0,400;0,500;0,600;0,700;1,100;1,200;1,300;1,400;1,500;1,600;1,700&family=Source+Code+Pro:ital,wght@0,200;0,300;0,400;0,500;0,600;0,700;0,800;0,900;1,200;1,300;1,400;1,500;1,600;1,700;1,800;1,900&family=Fira+Code:wght@300;400;500;600;700&family=JetBrains+Mono:ital,wght@0,100;0,200;0,300;0,400;0,500;0,600;0,700;0,800;1,100;1,200;1,300;1,400;1,500;1,600;1,700;1,800&display=swap" rel="stylesheet">
<style>

* {
  font-family: var(--selected-font);
}
</style>

<script>

//페이지 로드 시 저장된 글꼴 복원
$(document).ready(function() {
	var savedFont = localStorage.getItem('selectedFont');
	if (savedFont) {
		document.documentElement.style.setProperty('--selected-font', savedFont);
	}
});

</script>

<script type="text/javascript">
	document.onkeydown = function(e) {

		var evtK = (e) ? e.which : window.event.keyCode;
		var isCtrl = ((typeof isCtrl != 'undefiend' && isCtrl) || ((e && evtK == 17) || (!e && event.ctrlKey))) ? true
				: false;

		if ((isCtrl && evtK == 82) || evtK == 116) {
			if (e) {
				evtK = 505;
			} else {
				event.keyCode = evtK = 505;
			}
		}
		if (evtK == 505) {
			// 자바스크립트에서 현재 경로는 받아내는 메소드로 대치.
			location.reload(location.href);
			return false;
		}
	}

	function sendSql(value, connectionId) {
		if (value == null) {
			return;
		}

		
		var column = value.split('&')[1].split(',');
		var str = '';
		if($(".Resultrow.success").children('div').length>0){			
			for (var i = 0; i < column.length; i++) {
				if (i > 0) {
					str += '&';
				}
				
				var colValue = column[i].trim();
				
				// 작은따옴표로 감싼 값은 상수값으로 처리
				if (colValue.startsWith("'") && colValue.endsWith("'") && colValue.length > 1) {
					// 작은따옴표 제거하고 상수값으로 사용
					str += colValue.substring(1, colValue.length - 1);
				} else if(colValue == 0){
					str += "";
				} else {
					// 숫자인 경우 컬럼 인덱스로 처리
					str += $(".Resultrow.success").children('div').eq(colValue).text().trim();
				}
			}
		}else{
			// 단축키로 전달된 파라미터 처리 개선
			for (var i = 0; i < column.length; i++) {
				if (i > 0) {
					str += '&';
				}
				
				var colValue = column[i].trim();
				
				// 작은따옴표로 감싼 값은 상수값으로 처리
				if (colValue.startsWith("'") && colValue.endsWith("'") && colValue.length > 1) {
					// 작은따옴표 제거하고 상수값으로 사용
					str += colValue.substring(1, colValue.length - 1);
				} else {
					// 작은따옴표로 감싸지 않은 값은 그대로 사용 (이미 처리된 값)
					str += colValue;
				}
			}
		}

		$("#sendvalue").val(str);

		if (value.includes("FileRead")) {
			var myForm = document.popForm;
			var url = "/FileRead";
			myForm.action = url;
			myForm.method = "post";
			myForm.target = target;

			var pathval = "";
			for (var i = 0; i < column.length; i++) {
				var colValue = column[i].trim();
				
				// 작은따옴표로 감싼 값은 상수값으로 처리
				if (colValue.startsWith("'") && colValue.endsWith("'") && colValue.length > 1) {
					// 작은따옴표 제거하고 상수값으로 사용
					pathval += colValue.substring(1, colValue.length - 1);
				} else {
					var nCheck = /^\d{1,2}/;
					if (colValue.match(nCheck)) {
						pathval += $(".Resultrow.success").children('div').html();
					} else {
						pathval += colValue;
					}
				}
			}
			myForm.Path.value = pathval;

			myForm.submit();

		} else if (value.includes("map")) { // 나중에 external로 바꿀것 

			var pathval = "";
			for (var i = 0; i < column.length; i++) {
				var colValue = column[i].trim();
				
				// 작은따옴표로 감싼 값은 상수값으로 처리
				if (colValue.startsWith("'") && colValue.endsWith("'") && colValue.length > 1) {
					// 작은따옴표 제거하고 상수값으로 사용
					pathval += colValue.substring(1, colValue.length - 1);
				} else if (colValue.match(/^\d{1,2}$/)) {
					pathval += $(".Resultrow.success").children('div').eq(colValue).text();
				} else if (colValue.match(/^\d{1,2}A/)) {
					for (var j = 0; j < $(".Resultrow").length; j++) {//$(".Resultrow").length
						pathval += $(".Resultrow").eq(j).children('div').eq(colValue.substr(0, colValue.length - 1)).text() + "/";
					}
				} else {
					pathval += colValue;
				}
			}

			window.open(pathval.replace("?", "?param="), '_blank')
		} else {
			// 템플릿 실행 - 부모 창의 tabManager 사용
			var templateId = value.split('&')[0];
			var excuteParam = value.split('&')[2] || false; // excuteParam이 없으면 false 기본값
			
			// POST로 전송할 데이터를 JSON 객체로 구성
			var postData = {};
			
			// sendvalue는 POST 데이터로 전송
			var sendvalue = $("#sendvalue").val();
			if (sendvalue && sendvalue.trim() !== '') {
				postData.sendvalue = sendvalue;
			}
			
			// 연결 ID는 POST 데이터로 전송
			if (!connectionId) {
				// SQLExecute.jsp에서 호출된 경우 현재 선택된 연결 ID 가져오기
				if (typeof $("#connectionlist") !== 'undefined' && $("#connectionlist").length > 0) {
					connectionId = $("#connectionlist").val();
				}
			}
			if (connectionId && connectionId.trim() !== '' && connectionId !== '====Connection====' && connectionId !== '====SFTP Connection====') {
				postData.connectionId = connectionId;
			}
			
			// 템플릿 정보를 가져와서 탭 추가
			$.ajax({
				type: 'GET',
				url: '/SQLTemplate/detail',
				data: { templateId: templateId },
				success: function(templateResult) {
					var finalUrl;
					var title = templateId;
					
					if (templateResult.success && templateResult.data) {
						title = templateResult.data.sqlName || templateId;
						var templateType = templateResult.data.templateType || 'SQL';
						
						// HTML 타입이면 /Template/execute로, 아니면 /SQLExecute로
						if (templateType.toUpperCase() === 'HTML') {
							finalUrl = "/Template/execute?templateId=" + templateId;
							if (excuteParam !== false) {
								finalUrl += "&Excute=" + excuteParam;
							}
						} else {
							finalUrl = "/SQLExecute?templateId=" + templateId + "&templateType=" + templateType;
							if (excuteParam !== false) {
								finalUrl += "&Excute=" + excuteParam;
							}
						}
					} else {
						// 템플릿 정보 조회 실패 시 기본 URL 사용
						finalUrl = "/SQLExecute?templateId=" + templateId;
						if (excuteParam !== false) {
							finalUrl += "&Excute=" + excuteParam;
						}
					}
					
					parent.tabManager.addTab(templateId, title, finalUrl, Object.keys(postData).length > 0 ? postData : null);
				},
				error: function() {
					// 에러 시 기본 탭 추가 (SQLExecute 사용)
					var title = templateId;
					var url = "/SQLExecute?templateId=" + templateId;
					if (excuteParam !== false) {
						url += "&Excute=" + excuteParam;
					}
					// POST 데이터가 있으면 data 파라미터로 전달, 없으면 GET 방식
					parent.tabManager.addTab(templateId, title, url, Object.keys(postData).length > 0 ? postData : null);
				}
			});
			
		}

	}

	function getMenu() {
		
		$.ajax({
			type: 'post',
			url: '/SQL/list',
			success: function(result) {

				var sidebar = $('#sqltree');
				var child = $('<ul class="treeview-menu" id="sidemenu"></ul>');
				child.append(setMenu(result, child));
				sidebar.empty();
				sidebar.append('<a class="addtree" href="#"> <i class="fa fa-code"></i> <span>SQL</span> <i  class="fa fa-angle-left pull-right"></i></a>');
				sidebar.append(child);

			},
			error: function() {
				alert("시스템 에러");
			}
		});
	}


	function setMenu(result, parent) {

		for (var i = 0; i < result.length; i++) {
			var list = result[i];

			// 안전한 속성 접근을 위한 검증
			if (!list || !list.Name) {
				continue;
			}

			if (list.type === 'folder') {
				var folder = $('<li class="treeview">\n' +
					'          <a class="addtree" href="#">\n' +
					'<span>' +
					list.Name +
					'</span><i class="fa fa-angle-left pull-right"></i></a>\n' +
					'        </li>');
				var child = $('<ul class="treeview-menu"></ul>');
				folder.append(setMenu(list.list, child));

				parent.append(folder);
			} else {
				// 템플릿 아이템 처리 (새로운 탭 시스템 사용)
				var href = "/Template/execute?templateId=" + list.templateId;
				
				// 메뉴 아이템 생성 (새로운 탭 시스템 사용)
				var childItem = $('<li><a href="javascript:void(0)" onclick="window.addTemplateTab(\'' + list.templateId + '\', \'' + list.Name + '\', \'' + href + '\')" id="' +
					list.Name + '">' + list.Name + '</a></li>');
				parent.append(childItem);
			}
		}

		return parent;

	}

</script>

