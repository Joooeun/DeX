<meta content='width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no' name='viewport'>
<meta http-equiv="Cache-Control" content="no-cache, no-store, must-revalidate" />



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


<script src="/resources/dist/js/adminlte.min.js"></script>
<script src='/resources/plugins/fastclick/fastclick.min.js'></script>
<script src="/resources/dist/js/demo.js"></script>

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

	function sendSql(value) {
		if (value == null) {
			return;
		}

		var target = $(parent.document).find('#pageTabContent>div:last>iframe').attr('id');

		for (var i = 0; i < $(parent.document).find('#pageTab a').length; i++) {

			if (value.split('&')[0] == $(parent.document).find('#pageTab a:eq(' + i + ')').text().replace(/x$/, '')) {
				target = $(parent.document).find('#pageTabContent>div:eq(' + i + ')>iframe').attr('id');
				//$(parent.document).find('#pageTab a:eq(' + i + ')').tab('show');
				break;
			}

		}

		var column = value.split('&')[1].split(',');
		var str = '';
		if($(".Resultrow.success").children('div').length>0){			
			for (var i = 0; i < column.length; i++) {
				if (i > 0) {
					str += '&';
				}
				
				if(column[i]==0){
					str += "";
				}else{
					
				str += $(".Resultrow.success").children('div').eq(column[i]).text().trim();
				}
			}
		}else{
			// 단축키로 전달된 파라미터 처리 개선
			if (column.length > 0 && column[0].trim() !== '') {
				str = column.join('&');
			} else {
				str = '';
			}
		}

		$("#sendvalue").val(str);

		if (value.includes("FileRead")) {
			var myForm = document.popForm;
			var url = "/FileRead";
			//var dualScreenLeft = window.screenLeft != undefined ? window.screenLeft : screen.left;
			//var dualScreenTop = window.screenTop != undefined ? window.screenTop : screen.top;
			//var width = window.innerWidth ? window.innerWidth : document.documentElement.clientWidth ? document.documentElement.clientWidth : screen.width;
			//var height = window.innerHeight ? window.innerHeight : document.documentElement.clientHeight ? document.documentElement.clientHeight : screen.height;
			//var left = ((width / 2) - (800 / 2)) + dualScreenLeft;
			//var top = ((height / 2) - (700 / 2)) + dualScreenTop;

			//var w = window.open("", "FileRead",
			//	"width=800, height=700, top=" + top + ", left=" + left + ",  toolbar=no, menubar=no, scrollbars=no, resizable=yes");
			//w.document.title = "FileRead";
			myForm.action = url;
			myForm.method = "post";
			myForm.target = target;

			var pathval = "";
			for (var i = 0; i < column.length; i++) {
				var nCheck = /^\d{1,2}/;
				if (column[i].match(nCheck)) {
					pathval += $(".Resultrow.success").children('div').html();
					
				} else {
					pathval += column[i];
				}
			}
			myForm.Path.value = pathval;

			myForm.submit();

		} else if (value.includes("map")) { // 나중에 external로 바꿀것 



			var pathval = "";
			for (var i = 0; i < column.length; i++) {
				if (column[i].match(/^\d{1,2}$/)) {
					pathval += $(".Resultrow.success").children('div').eq(column[i]).text();
				} else if (column[i].match(/^\d{1,2}A/)) {
					for (var j = 0; j < $(".Resultrow").length; j++) {//$(".Resultrow").length
						pathval += $(".Resultrow").eq(j).children('div').eq(column[i].substr(0, column[i].length - 1)).text() + "/";
					}
				} else {
					pathval += column[i];
				}
			}

			window.open(pathval.replace("?", "?param="), '_blank')
		} else {
			if (value.split('&')[0].includes('.htm')) {

				document.ParamForm.action = "/HTML?Path=" + value.split('&')[0];
			} else {

				document.ParamForm.action = "/SQL?Excute=" + value.split('&')[2] + "&templateId=" + value.split('&')[0];
			}
			
			document.ParamForm.method = "POST";
			document.ParamForm.target = target;
			document.ParamForm.submit();


			document.ParamForm.action = "javascript:startexcute();";
			document.ParamForm.target = "";
		}

	}

	function getMenu() {
		
		$.ajax({
			type: 'post',
			url: '/SQL/list',
			success: function(result) {

				var sidebar = $('#sqltree');
				//var parent = $('<li class="active treeview menu-open"><a class="addtree" href="#"> <i class="fa fa-code"></i> <span>SQL</span> <i class="fa fa-angle-left pull-right"></a></i>');
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
			} else if (list.type === 'sql') {
				// SQL 템플릿인 경우
				var childItem = $('<li><a href="/SQL?templateId=' +
					list.templateId + '" target="iframe" id="' +
					list.Name.split('.')[0] + '">' +
					list.Name.split('.')[0] + '</a></li>');
				parent.append(childItem);
			} else if (list.Name && list.Name.includes('.htm')) {
				// HTML 파일인 경우
				var childItem = $('<li><a href="/HTML?Path=' +
					encodeURI(list.Path) + '" target="iframe" id="' +
					list.Name.split('_')[0] + '">' +
					list.Name.split('.')[0] + '</a></li>');
				parent.append(childItem);
			} else if (list.Path) {
				// 기존 파일 기반 SQL인 경우
				var childItem = $('<li><a href="/SQL?Path=' +
					encodeURI(list.Path) + '" target="iframe" id="' +
					list.Name.split('_')[0] + '">' +
					list.Name.split('.')[0] + '</a></li>');
				parent.append(childItem);
			}
		}

		return parent;

	}
	
	
	// 일반적인 오류 감지
	window.onerror = function(message, source, lineno, colno, error) {
	    sendErrorToServer({
	        type: "error",
	        message: message,
	        source: source,
	        line: lineno,
	        column: colno,
	        stack: error ? error.stack : null
	    });
	};

	// 리소스 로딩 오류 감지 (예: 이미지, 스크립트 로드 실패)
	window.addEventListener("error", function(event) {
	    if (event.target instanceof HTMLElement) {
	        sendErrorToServer({
	            type: "resource",
	            tag: event.target.tagName,
	            src: event.target.src || event.target.href
	        });
	    }
	}, true);

	// Promise 처리 중 발생한 오류 감지
	window.addEventListener("unhandledrejection", function(event) {
	    sendErrorToServer({
	        type: "promise",
	        message: event.reason ? event.reason.message : "Unhandled promise rejection",
	        stack: event.reason ? event.reason.stack : null
	    });
	});

	// 오류를 서버로 전송하는 함수
	function sendErrorToServer(errorData) {
	    fetch("/log-error", {
	        method: "POST",
	        headers: { "Content-Type": "application/json" },
	        body: JSON.stringify(errorData)
	    });
	}

</script>

