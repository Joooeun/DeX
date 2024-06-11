
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
			//alert("Asdfasdfasfd")
			break;
		}

	}

	var column = value.split('&')[1].split(',');
	var str = '';
	for (var i = 0; i < column.length; i++) {
		if (i > 0) {
			str += '&';
		}
		str += $(".Resultrow.success").children('td').eq(column[i]).children('span').html();
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
				pathval += $(".Resultrow.success").children('td').html();
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
				pathval += $(".Resultrow.success").children('td').eq(column[i]).children('span').html();
			} else if (column[i].match(/^\d{1,2}A/)) {
				for (var j = 0; j < $(".Resultrow").length; j++) {//$(".Resultrow").length
					pathval += $(".Resultrow").eq(j).children('td').eq(column[i].substr(0, column[i].length - 1)).children('span').html() + "/";
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