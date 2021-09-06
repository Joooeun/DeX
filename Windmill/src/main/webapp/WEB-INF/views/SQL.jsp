<%@include file="common/common.jsp"%>
<style>
::-webkit-scrollbar {
	width: 2px;
	height: 8px;
	border: 1px solid #fff;
}

::-webkit-scrollbar-track {
	background: #efefef;
	-webkit-border-radius: 10px;
	border-radius: 10px;
	-webkit-box-shadow: inset 0 0 4px rgba(0, 0, 0, .2)
}

::-webkit-scrollbar-thumb {
	height: 50px;
	width: 50px;
	background: rgba(0, 0, 0, .2);
	-webkit-border-radius: 8px;
	border-radius: 8px;
	-webkit-box-shadow: inset 0 0 4px rgba(0, 0, 0, .1)
}
</style>
<script>

var ctx;

var myChart;

var graphcolor = ['#FF583A','#FF9032','#FEDD0F','#4B963E','#23439F','#561475','#F2626B','#FEBA4F','#FFEA7F','#89E077','#83C3FF','#C381FD', '#525252' ]
//var graphcolor = [ '#f22613', '#e74c3c', '#f62459', '#663399', '#9a12b3', '#bf55ec', '#19b5fe', '#1e8bc3', '#1f3a93', '#89c4f4', '#03c9a9', '#26c281', '#16a085', '#2eec71', '#f2784b', '#f89406', '#f9bf3b']
//var graphcolor = ['#1a1c2c','#5d275d','#b13e53','#ef7d57','#ffcd75','#a7f070','#38b764','#257179','#29366f','#3b5dc9','#41a6f6','#73eff7','#f4f4f4','#94b0c2','#566c86','#333c57'];

	$(document).ready(function() {
		
		ctx = document.getElementById('myChart');
		myChart = myChart = new Chart(ctx, {
		    type: 'line',
		    data: {
		    	  labels: [''],
		    	  datasets: [{
		    	    label: 'My First Dataset',
		    	    data: [65, 59, 80, 81, 56, 55, 40],
		    	    fill: false,
		    	    borderColor: 'rgb(75, 192, 192)',
		    	    tension: 0.1
		    	  }]
		    	},
		    	options: {
		    		maintainAspectRatio: false,
		          }
		});
		
		$.ajax({
			type : 'post',
			url : "/Connection/list",
			data : {
				TYPE : "DB"
			},
			success : function(result) {

				for (var i = 0; i < result.length; i++) {

					if (result[i].split('.')[0] == $('#selectedConnection').val()) {
						$('#connectionlist').append("<option value=\"" + result[i].split('.')[0] + "\"  selected=\"selected\">" + result[i].split('.')[0] + "</option>");
					} else {
						$('#connectionlist').append("<option value='" + result[i].split('.')[0] + "'>" + result[i].split('.')[0] + "</option>");
					}
				}

				if ($(".paramvalue").eq(0).val() != '' && $(".paramvalue").length > 0) {
					excute();
				}
			},
			error : function() {
				alert("시스템 에러");
			}
		});

		$(document).keydown(function(event) {
			if (event.keyCode == '112') {
				sendSql($("#F1").val());
			} else if (event.keyCode == '113') {
				sendSql($("#F2").val());
			} else if (event.keyCode == '114') {
				sendSql($("#F3").val());
			} else if (event.keyCode == '115') {
				sendSql($("#F4").val());
			} else if (event.keyCode == '116') {
				sendSql($("#F5").val());
			} else if (event.keyCode == '117') {
				sendSql($("#F6").val());
			} else if (event.keyCode == '118') {
				sendSql($("#F7").val());
			} else if (event.keyCode == '119') {
				sendSql($("#F8").val());
			} else if (event.keyCode == '120') {
				sendSql($("#F9").val());
			} else if (event.keyCode == '121') {
				sendSql($("#F10").val());
			} else if (event.keyCode == '122') {
				sendSql($("#F11").val());
			} else if (event.keyCode == '가나다라마바사') {
				sendSql($("#F12").val());
			}
		});

		$(document).on("click", ".Resultrow", function() {
			$(".Resultrow").removeClass('success');
			$(this).addClass('success');
		});

	});

	function startexcute() {
		if ($("#connectionlist option:selected").val() == '') {
			alert("Connection을 선택하세요.");
			return;
		}

		$("#excutebtn").attr('disabled', true);
		$("#result_head").html('<tr><td class="text-center"><img alt="loading..." src="/resources/img/loading.gif" style="width:50px; margin : 50px auto;"></tr></td>');

		excute();
		if ($("#refreshtimeout").val() > 0) {
			setInterval(excute, $("#refreshtimeout").val() * 1000);
		}
	}

	function excute() {

		var sql = $("#sql_text").val();

		for (var i = 0; i < $(".param").length; i++) {
			if ($(".paramvalue").eq(i).attr('paramtype') == 'string') {
				sql = sql.split(':' + $(".param").eq(i).attr('paramtitle')).join('\'' + $(".paramvalue").eq(i).val() + '\'');
			} else if ($(".paramvalue").eq(i).attr('paramtype') == 'number') {
				sql = sql.split(':' + $(".param").eq(i).attr('paramtitle')).join($(".paramvalue").eq(i).val());
			}
		}

		$.ajax({
			type : 'post',
			url : '/SQL/excute',
			data : {
				sql : sql,
				Connection : $("#connectionlist").val()
			},
			success : function(result) {

				$("#Resultbox").css("display", "block");

				var str = '';

				str += '<tr>';
				str += '<th>#</th>';
				for (var title = 0; title < result[0].length; title++) {
					str += '<th>' + result[0][title] + '</th>';
				}
				str += '</tr>';
				str += '<tr>' + '<td colspan="100" style="padding: 0; border: none;">' + '<div id="valuediv" style="max-height: calc(100vh - 395px); overflow: auto;">'
						+ '<table class="table table-condensed table-hover table-striped" id="result_body" style="margin: 0; font-size: 14px;">' + '</table>' + '</div>' + '</td>' + '</tr>';

				for (var outter = 1; outter < result.length; outter++) {
					str += '<tr style="visibility: hidden;">';
					str += '<td style="border:none;">' + outter + '</td>';

					for (var inner = 0; inner < result[outter].length; inner++) {
						var cellstr = result[outter][inner];
						// 						if (result[outter].length > 15 && cellstr.length > 10) {
						// 							cellstr = cellstr.substr(0, 10) + '...'
						// 						}
						str += '<td style="border:none;">' + cellstr + '</td>';
					}

					str += '</tr>';
				}

				$("#result_head").html(str);
				str = '';

				for (var outter = 1; outter < result.length; outter++) {
					str += '<tr class="Resultrow" param='+result[outter][0]+'>';
					str += '<td>' + outter + '</td>';

					for (var inner = 0; inner < result[outter].length; inner++) {
						var cellstr = result[outter][inner];
						// 						if (result[outter].length > 15 && cellstr.length > 10) {
						// 							cellstr = cellstr.substr(0, 10) + '...'
						// 						}
						str += '<td style="border-left:1px solid #cccccc">' + cellstr + '</td>';
					}

					str += '</tr>';
				}

				str += '<tr style="visibility: hidden;">';
				str += '<th>#</th>';
				for (var title = 0; title < result[0].length; title++) {
					str += '<th>' + result[0][title] + '</th>';
				}
				str += '</tr>';
				$("#result_body").html(str);
				$("#excutebtn").attr('disabled', false);
				
				chart(result);

			},
			error : function(error) {
				$("#excutebtn").attr('disabled', false);
				alert('시스템오류');
			}
		});

	}

	function sessionCon(value) {

		$.ajax({
			type : 'post',
			url : '/Connection/sessionCon',
			data : {
				Connection : value
			},
			success : function(result) {
				//alert("성공")
			},
			error : function() {
				//alert("시스템 에러");
			}
		});

	}

	

	function readfile() {
		$.ajax({
			type : 'post',
			url : '/SQL/readfile',
			success : function(result) {
				alert(result)
			},
			error : function() {
				alert("시스템 에러");
			}
		});
	}

	function chart(result) {
		
		myChart.destroy();
		
		var chardata = transpose(result)
		
		var labels = chardata[0].slice(1, chardata[0].length);

		var datasets=[];
		var maxdata=0;
		
		for (var i = 1; i < chardata.length; i++) {
			var data = chardata[i].slice(1, chardata[i].length).map((x)=>{return parseInt(x)});
			if(maxdata<Math.max(...data)){
				maxdata=Math.max(...data);
			}
		}
		
		
		for (var i = 1; i < chardata.length; i++) {
			
			var label = chardata[i][0];
			var data = chardata[i].slice(1, chardata[i].length).map((x)=>{return parseInt(x)});
			
			if(maxdata<Math.max(...data)){
				maxdata=Math.max(...data);
			}
			
			datasets.push({label : label,
				data :data,
				fill:false,
				borderColor : graphcolor[i-1],
				/* backgroundColor :  graphcolor[i]+"80", */
				tension : 0.1,
				hidden: Math.max(...data)<maxdata/10
				})
		}
		
		
		const datas = {
			labels : labels,
			datasets : datasets
		};

		myChart = new Chart(ctx, {
		    type: 'line',
		    data: datas,
		    options: {
		    	maintainAspectRatio: false,
	          }
		});
	}
	
	function random_rgba() {
	    var o = Math.round, r = Math.random, s = 255;
	    //return 'rgba(' + o(r()*s) + ',' + o(r()*s) + ',' + o(r()*s) + ',' + r().toFixed(1) + ')';
	    return 'rgb(' + o(r()*s) + ',' + o(r()*s) + ',' + o(r()*s) + ')';
	}

	
	function transpose(a) {

		  // Calculate the width and height of the Array
		  var w = a.length || 0;
		  var h = a[0] instanceof Array ? a[0].length : 0;

		  // In case it is a zero matrix, no transpose routine needed.
		  if(h === 0 || w === 0) { return []; }

		  /**
		   * @var {Number} i Counter
		   * @var {Number} j Counter
		   * @var {Array} t Transposed data is stored in this array.
		   */
		  var i, j, t = [];

		  // Loop through every item in the outer array (height)
		  for(i=0; i<h; i++) {

		    // Insert a new row (array)
		    t[i] = [];

		    // Loop through every item per item in outer array (width)
		    for(j=0; j<w; j++) {

		      // Save transposed data.
		      t[i][j] = a[j][i];
		    }
		  }

		  return t;
		}
</script>
<!-- Content Wrapper. Contains page content -->
<div class="content-wrapper" style="margin-left: 0">
	<!-- Content Header (Page header) -->
	<section class="content-header">
		<h1>${title}</h1>
		<ol class="breadcrumb">
			<li><a href="#"><i class="icon ion-ios-home"></i> Home</a></li>
			<li class="active"><a href="#" onclick="readfile()">SQL</a></li>
		</ol>
	</section>
	<section class="content">
		<div class="row">
			<div class="col-xs-12">
				<div class="box box-default ">
					<div class="box-header with-border">
						<h3 class="box-title">파라미터 입력</h3>
						&nbsp;&nbsp;&nbsp; <input id="selectedConnection" type="hidden" value="${Connection}">
						<select id="connectionlist" onchange="sessionCon(this.value)">
							<option value="">====Connection====</option>
						</select>
					</div>
					<c:if test="${sql eq ''}">
						<textarea class="col-sm-12" id="sql_text">${sql}</textarea>
					</c:if>
					<form role="form-horizontal" name="ParamForm">
						<div class="box-body">
							<div class="form-group">
								<c:forEach var="item" items="${Param}" varStatus="status">
									<span class="col-sm-2 col-md-2 col-lg-1 param text-center" id="param${status.count}" paramtitle="${item.name}" style="padding-top: 7px; font-weight: bold; font-size: 15px">${fn:toUpperCase(item.name)}</span>
									<div class="col-sm-3 col-md-2 col-lg-2" style="margin: 2px 0">
										<input type="text" class="form-control paramvalue" paramtype="${item.type}" value="${item.value}" style="padding: 0 2px;">
									</div>
								</c:forEach>
								<div class="col-sm-2 col-md-1 pull-right">
									<input type="hidden" id="sendvalue" name="sendvalue"> <input id="excutebtn" type="submit" class="form-control" value="실행" onclick="startexcute();">
								</div>
							</div>
						</div>
					</form>
					<form name="popForm">
						<input type="hidden" name="Path" />
					</form>
				</div>
			</div>
		</div>
		<div class="row" id="Resultbox">
			<div class="col-xs-12">
				<div class="box">
					<div class="box-header with-border">
						<!-- Nav tabs -->
						<ul class="nav nav-tabs" role="tablist">
							<li role="presentation" class="active"><a href="#result" aria-controls="result" role="tab" data-toggle="tab">Result</a></li>
							<li role="presentation"><a href="#chart" aria-controls="chart" role="tab" data-toggle="tab">Chart</a></li>
						</ul>
						<!-- Tab panes -->
						<div class="tab-content">
							<div role="tabpanel" class="tab-pane active" id="result">
								<div style="overflow-y: hidden; overflow-x: auto; height: calc(100vh - 395px);">
									<table class="table table-condensed" id="result_head" style="margin: 0; font-size: 14px">
									</table>
								</div>
							</div>
							<div role="tabpanel" class="tab-pane" id="chart">

								<div style="overflow-y: auto; overflow-x: auto; height: calc(100vh - 370px); width: 100%;">
									<canvas id="myChart" width="100%" height="100%"></canvas>
								</div>
							</div>
						</div>
					</div>
				</div>
			</div>
		</div>
		<div class="row" id="Keybox">
			<div class="col-xs-12">
				<div class="box box-default" style="margin-bottom: 0px">
					<div class="box-header with-border">
						<h3 class="box-title">Short Key</h3>
					</div>
					<div class="box-body">
						<div class="form-group">
							<c:forEach var="item" items="${ShortKey}">
								<button type="button" class="btn btn-default" onclick="sendSql('${item.menu}&${item.column}')">${item.keytitle}</button>
								<input type="hidden" id="${item.key}" value="${item.menu}&${item.column}">
							</c:forEach>
						</div>
					</div>
				</div>
			</div>
		</div>
		<c:if test="${sql != ''}">
			<textarea rows="10" cols="200" id="sql_text" hidden="hidden">${sql}</textarea>
			<input id="Path" name="Path" value="${Path}" type="hidden">
			<input id="refreshtimeout" value="${refreshtimeout}" type="hidden">
		</c:if>
	</section>
</div>