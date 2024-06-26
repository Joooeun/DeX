<meta content='width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no' name='viewport'>
<meta http-equiv="Cache-Control" content="no-cache, no-store, must-revalidate" />
<link href="/resources/bootstrap/css/bootstrap.min.css" rel="stylesheet" type="text/css" />
<link href="/resources/dist/css/AdminLTE.min.css" rel="stylesheet" type="text/css" />
<link href="/resources/dist/css/skins/_all-skins.min.css" rel="stylesheet" type="text/css" />
<link href="/resources/ionicons/2.0.1/css/ionicons.min.css" rel="stylesheet" type="text/css" />
<link href="/resources/font-awesome-4.7.0/css/font-awesome.min.css" rel="stylesheet" type="text/css" />
<script src="/resources/plugins/jQuery/jQuery-2.1.4.min.js"></script>
<script src="/resources/plugins/chartjs/ChartJs.js"></script>
<script src="/resources/bootstrap/js/bootstrap.js"></script>
<link rel="shortcut icon" href="#">

<link href="/resources/plugins/datatables/datatables.min.css" rel="stylesheet">

<script src="/resources/plugins/datatables/datatables.min.js"></script>


<link href="/resources/dist/css/tabulator/tabulator.min.css" rel="stylesheet">
<script type="text/javascript" src="/resources/dist/js/tabulator/tabulator.js"></script>
<script type="text/javascript" src="/resources/dist/js/tabulator/xlsx.full.min.js"></script>

<link href="/resources/dist/css/tabulator/tabulator_bootstrap3.css" rel="stylesheet">
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

			//console.log($(parent.document).find('#pageTab a:eq(' + i + ')').text())
			//console.log("sdfsdf", value.split('&')[0], $(parent.document).find('#pageTab a:eq(' + i + ')').text().replace(/x$/, ''))
			if (value.split('&')[0] == $(parent.document).find('#pageTab a:eq(' + i + ')').text().replace(/x$/, '')) {
				target = $(parent.document).find('#pageTabContent>div:eq(' + i + ')>iframe').attr('id');
				//$(parent.document).find('#pageTab a:eq(' + i + ')').tab('show');
				break;
			}

		}

		var column = value.split('&')[1].split(',');
		var str = '';
		for (var i = 0; i < column.length; i++) {
			if (i > 0) {
				str += '&';
			}
			str += $(".Resultrow.success").children('div').eq(column[i]).text().trim();
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
			//console.log(pathval)
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
			//console.log("[deddbsssssug]",pathval)

			window.open(pathval.replace("?", "?param="), '_blank')
		} else {
			if (value.split('&')[0].includes('.htm')) {

				document.ParamForm.action = "/HTML?Path=" + value.split('&')[0];
			} else {

				document.ParamForm.action = "/SQL?excute=" + value.split('&')[2] + "&Path=" + encodeURI($("#Path").val() + "/" + value.split('&')[0] + ".sql");
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

			if (list.Path.includes('Path')) {
				var folder = $('<li class="treeview">\n' +
					'          <a class="addtree" href="#">\n' +
					'<span>' +
					list.Name +
					'</span><i class="fa fa-angle-left pull-right"></i></a>\n' +
					'        </li>');
				var child = $('<ul class="treeview-menu"></ul>');
				folder.append(setMenu(list.list, child));

				parent.append(folder);
			} else if (list.Name.includes('.htm')) {

				var childItem = $('<li><a href="/HTML?Path=' +
					encodeURI(list.Path) + '" target="iframe" id="' +
					list.Name.split('_')[0] + '">' +
					list.Name.split('.')[0] + '</a></li>');
				parent.append(childItem);

			} else {
				var childItem = $('<li><a href="/SQL?Path=' +
					encodeURI(list.Path) + '" target="iframe" id="' +
					list.Name.split('_')[0] + '">' +
					list.Name.split('.')[0] + '</a></li>');
				parent.append(childItem);
			}
		}

		return parent;

	}
</script>
