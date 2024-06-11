<%@include file="common/common.jsp"%>
<style>
::-webkit-scrollbar {
	width: 10px;
	height: 10px;
	border: 1px solid #fff;
}

::-webkit-scrollbar-track {
	background: #efefef;
	-webkit-border-radius: 10px;
	border-radius: 10px;
	-webkit-box-shadow: inset 0 0 4px rgba(0, 0, 0, .2)
}

::-webkit-scrollbar-thumb {
	background: rgba(0, 0, 0, .2);
	-webkit-border-radius: 8px;
	border-radius: 8px;
	-webkit-box-shadow: inset 0 0 4px rgba(0, 0, 0, .1)
}

#result_head td {
	font-family: "D2Coding" !important;
}

.tableWrapper {
	min-height: calc(( 100vh)*0.2);
	max-height: calc(( 100vh)* 0.6);
	overflow: auto;
}

#result_head th:last-child, #result_head td:last-child {
	border-right: 0px !important;
}

#result_head th {
	position: sticky;
	top: 0px;
	border-left: 1px solid #cccccc;
	background-color: #ffffff;
}

#result_head td {
	border-left: 1px solid #cccccc
}

.newline {
	white-space: pre;
}

.form-group.required .param:after {
	content: "*";
	color: red;
}
</style>
<script>

var ctx;

var myChart;

var graphcolor = ['#FF583A','#FF9032','#FEDD0F','#4B963E','#23439F','#561475','#F2626B','#FEBA4F','#FFEA7F','#89E077','#83C3FF','#C381FD', '#525252' ]
//var graphcolor = [ '#f22613', '#e74c3c', '#f62459', '#663399', '#9a12b3', '#bf55ec', '#19b5fe', '#1e8bc3', '#1f3a93', '#89c4f4', '#03c9a9', '#26c281', '#16a085', '#2eec71', '#f2784b', '#f89406', '#f9bf3b']
//var graphcolor = ['#1a1c2c','#5d275d','#b13e53','#ef7d57','#ffcd75','#a7f070','#38b764','#257179','#29366f','#3b5dc9','#41a6f6','#73eff7','#f4f4f4','#94b0c2','#566c86','#333c57'];

var sql_text = "";

	$(document).ready(function() {
		
		sql_text = $("#sql_org").val();
		$("#sql_org").remove();
		
		
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
				
				if("${DB}"==""){
					for (var i = 0; i < result.length; i++) {

						if (result[i].split('.')[0] == $('#selectedConnection').val()) {
							$('#connectionlist').append("<option value=\"" + result[i].split('.')[0] + "\"  selected=\"selected\">" + result[i].split('.')[0] + "</option>");
						} else {
							$('#connectionlist').append("<option value='" + result[i].split('.')[0] + "'>" + result[i].split('.')[0] + "</option>");
						}
					}
				}else{
					$('#connectionlist').append("<option value=\"${DB}\"  selected=\"selected\">${DB}</option>");
				}
				
				var shortkey = ${Excute};


				if (shortkey && $("#connectionlist option:selected").val() != '') {
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
		
		document.querySelectorAll(".formtextarea").forEach((element) => {
		  element.addEventListener("keydown", function (e){
			    if (e.ctrlKey && e.keyCode == 13) {
					
			    	if(document.ParamForm.checkValidity())
				    	document.ParamForm.submit();
			    	e.preventDefault();
				}
			})
		})
		
		document.querySelectorAll(".paramvalue input").forEach((element) => {
		  element.addEventListener("keydown", function (e){
			    if (e.keyCode == 13) {
					
			    	document.ParamForm.submit();
			    	e.preventDefault();
				}
			})
		})


		$(document).on("click", ".Resultrow", function() {
			$(".Resultrow").removeClass('success');
			$(this).addClass('success');
		});
		
		$(document).on("change", "#newline", function() {
			if($(this).prop('checked')){
				$("#result_head td").addClass('newline');
			}else{
				$("#result_head td").removeClass('newline')
			}
		});

	});

	
	function formsubmit(){
		
	}
	function startexcute() {
		if ($("#connectionlist option:selected").val() == '') {
			alert("Connection을 선택하세요.");
			
		} else{
			excute();
			if ($("#refreshtimeout").val() > 0) {
				setInterval(excute, $("#refreshtimeout").val() * 1000);
			}
		}
		
		return false;
	}
	
	function commit() {
		$.ajax({
			type : 'post',
			url : '/SQL/commit',
			data : {
				Connection : $("#connectionlist").val()
			},
			error : function() {
				alert("시스템 에러");
			}
		});
	}

	function excute() {
		
		var sql = $("#sql_text").val() ?? sql_text;
		var log = "";

		for (var i = 0; i < $(".paramvalue").length; i++) {
			
			if ($(".paramvalue").eq(i).attr('required') == 'required'&&$(".paramvalue").eq(i).val()=="") {
				alert($(".paramvalue").eq(i).attr('paramtitle')+"을 입력하세요.")
				return;
				
			}
			
			
			if ($(".paramvalue").eq(i).attr('paramtype') == 'string') {
				sql = sql.split(':' + $(".paramvalue").eq(i).attr('paramtitle')).join('\'' + $(".paramvalue").eq(i).val() + '\'');
			} else if ($(".paramvalue").eq(i).attr('paramtype') == 'text') {
				sql = sql.split(':' + $(".paramvalue").eq(i).attr('paramtitle')).join( $(".paramvalue").eq(i).val().replace(/'/g, "''"));		
			} else if ($(".paramvalue").eq(i).attr('paramtype') == 'sql') {
				sql = sql.split(':' + $(".paramvalue").eq(i).attr('paramtitle')).join($(".paramvalue").eq(i).val());
			} else if ($(".paramvalue").eq(i).attr('paramtype') == 'number') {
				sql = sql.split(':' + $(".paramvalue").eq(i).attr('paramtitle')).join($(".paramvalue").eq(i).val());
			}else if ($(".paramvalue").eq(i).attr('paramtype') == 'log') {
				log += "\n"+$(".paramvalue").eq(i).attr('paramtitle')+" : "+$(".paramvalue").eq(i).val();
			}
		}
		
		$("#excutebtn").attr('disabled', true);
		if(!$("#refreshtimeout").val() ){
			$("#result_head").html('<tr><td class="text-center"><img alt="loading..." src="/resources/img/loading.gif" style="width:50px; margin : 50px auto;"></tr></td>');
		}
		

		$.ajax({
			type : 'post',
			url : '/SQL/excute',
			data : {
				sql : sql.trim(),
				log : log,
				autocommit : true,
				/* autocommit : $("#autocommit").prop('checked'), */
				Connection : $("#connectionlist").val(),
				limit: $("#limit").val()
			},
			success : function(result, status, jqXHR ) {
				
				if (jqXHR.getResponseHeader("SESSION_EXPIRED") === "true") {
		            alert("세션이 만료되었습니다.");
		            window.parent.location.href = "/Login";
		        }

				$("#Resultbox").css("display", "block");
				
				var newline = $("#newline").prop('checked');
				
				var str = '';
				
				if(result.length > 0){
					str = '<thead>';

					str += '<tr>';
					str += '<th>#</th>';
					for (var title = 0; title < result[0].length; title++) {
						str += '<th>' + result[0][title] + '</th>';
					}
					str += '</tr></thead><tbody>';


					for (var outter = 1; outter < result.length; outter++) {
						
						
						//str += '<tr class="Resultrow" param='+result[outter][0]+'>';
						str += '<tr class="Resultrow sorting">';
						str += '<td>' + outter + '</td>';

						for (var inner = 0; inner < result[outter].length; inner++) {
							var cellstr = result[outter][inner];
							str += `<td><span>\${ConvertSystemSourcetoHtml(cellstr)}</span></td>`;
						}

						str += '</tr>';
					}
					str += '</tbody>';
					
					
				    chart(result);
				    
				}else{
					
					var str = '<thead><tr><th>#</th></tr></thead><tbody></tbody>';
				}
				    
				if ( $.fn.DataTable.isDataTable( '#result_head' ) ) {
			        $('#result_head').DataTable().destroy();
			        $('#result_head').empty(); 	
			    };
			    
			    $("#result_head").html(str);
				$('#result_head').DataTable( {
				    paging: false,
				    searching: false,
				     layout: {
				        topEnd: {
				        	buttons: [
				                {
				                    extend: 'excel',
				                    text: '<i class="fa fa-floppy-o"></i>',
				                    title: `${title}_${memberId}_` + dateFormat()
				                }
				            ]
				        
				        },
				        topStart: {
				            info: {
				                text: 'total : _TOTAL_ records'
				            }
				        },
				          bottomStart: null
				        
				    }
				} );
					
				
				if(newline){
					
					$("#result_head td").addClass('newline');
				}
				
				$("#excutebtn").attr('disabled', false);
				
				

			},
			error : function(error) {
				$("#excutebtn").attr('disabled', false);
				alert('시스템오류');
			}
		});

	}

	function sessionCon(value) {
		
		if("${DB}"==""){

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

	}
	
	function ConvertSystemSourcetoHtml(str){
	 str = str.replace(/</g,"&lt;");
	 str = str.replace(/>/g,"&gt;");
	 str = str.replace(/\"/g,"&quot;");
	 str = str.replace(/\'/g,"&#39;");
	 return str;
	}
	
	function forxmp(str){
		str = str.replace(/\x00/g,"");
	 return str;
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
	
	function dateFormat() {
		
		let date = new Date();
		
        let month = date.getMonth() + 1;
        let day = date.getDate();
        let hour = date.getHours();
        let minute = date.getMinutes();
        let second = date.getSeconds();

        month = month >= 10 ? month : '0' + month;
        day = day >= 10 ? day : '0' + day;
        hour = hour >= 10 ? hour : '0' + hour;
        minute = minute >= 10 ? minute : '0' + minute;
        second = second >= 10 ? second : '0' + second;

        return date.getFullYear() +  month + day + '_' + hour +  minute + second;
}
	
</script>
<!-- Content Wrapper. Contains page content -->
<div class="content-wrapper" style="margin-left: 0">
	<!-- Content Header (Page header) -->
	<section class="content-header">
		<h1>${title}</h1>
		<span>${desc}</span>
		<ol class="breadcrumb">
			<li>
				<a href="#"><i class="icon ion-ios-home"></i> Home</a>
			</li>
			<li class="active">
				<a href="#" onclick="readfile()">SQL</a>
			</li>
		</ol>
	</section>
	<section class="content">
		<div class="row">
			<div class="col-xs-12">
				<div class="box box-default ">
					<div class="box-header with-border">
						<h3 class="box-title">파라미터 입력</h3>
						&nbsp;&nbsp;&nbsp; <input id="selectedConnection" type="hidden" value="${Connection}"> <select id="connectionlist" onchange="sessionCon(this.value)">
							<option value="">====Connection====</option>
						</select>
					</div>
					<c:if test="${sql eq ''}">
						<textarea class="col-sm-12 col-xs-12 formtextarea" id="sql_text" style="margin: 0 0 10px 0" rows="5">${sql}</textarea>
					</c:if>
					<form role="form-horizontal" name="ParamForm" id="ParamForm" action="javascript:startexcute();">

						<!-- onsubmit="startexcute()" -->

						<div class="box-body">

							<c:forEach var="item" items="${Param}" varStatus="status">
								<c:choose>
									<c:when test="${item.type == 'text' || item.type == 'sql'}">
										<div class="col-xs-12">
											<div class="form-group">
												<span class="param" id="param${status.count}" style="padding-top: 7px; font-weight: bold; font-size: 15px">${fn:toUpperCase(item.name)}</span>
												<textarea class="paramvalue col-xs-12 formtextarea" paramtitle="${item.name}" rows="10" paramtype="${item.type}" style="padding: 0 2px;">${item.name=='memberId' ? memberId : item.value}</textarea>
											</div>
										</div>


									</c:when>

									<c:otherwise>
										<div class="col-sm-3">
											<div class="form-group ${item.required}">
												<c:if test="${item.name != 'memberId' && item.hidden !='hidden'}">
													<span class="param" id="param${status.count}" style="padding-top: 7px; font-weight: bold; font-size: 15px">${fn:toUpperCase(item.name)}</span>
												</c:if>
												<div style="margin: 2px 0">
													<input type="${item.name=='memberId' || item.hidden=='hidden' ? 'hidden' : 'text'}" class="form-control paramvalue" paramtitle="${item.name}" paramtype="${item.type}" value="${item.name == 'memberId' ? memberId : item.value}"
														style="padding: 0 2px;" <c:if test="${item.required=='required'}">required="required" pattern="\S(.*\S)?" title="공백은 입력할 수 없습니다."</c:if> <c:if test="${item.disabled=='disabled'}">disabled</c:if>
														<c:if test="${item.name=='memberId' || item.readonly=='readonly'}">readonly</c:if>>
												</div>
											</div>
										</div>

									</c:otherwise>
								</c:choose>
							</c:forEach>

							<div class="col-sm-2 col-md-1 pull-right">
								<input type="hidden" id="sendvalue" name="sendvalue"> <input id="excutebtn" type="submit" class="form-control" value="실행">
								<!-- ="startexcute();" -->

								<!-- <input
										id="excutebtn" type="submit" class="form-control" value="실행">  -->
								<!-- <label><span
										style="font-size: small;">auto commit</span> <input
										id="autocommit" type="checkbox" checked="checked" /> </label> -->

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
							<li role="presentation" class="active">
								<a href="#result" aria-controls="result" role="tab" data-toggle="tab">Result</a>
							</li>
							<li role="presentation">
								<a href="#chart" aria-controls="chart" role="tab" data-toggle="tab">Chart</a>
							</li>
							<!-- <li style="float: right; al"><i class="fa fa-floppy-o"></i></li> -->
							<li style="float: right; margin-right: 5px">
								<label style="margin: 0 3px 0 3px;">
									limit&nbsp; <input type="text" size="3" maxlength="3" id="limit" value="${empty limit ? 0 :  limit}" />
								</label>
								<label style="margin: 0 3px 0 3px;">
									<input type="checkbox" id="newline" <c:if test="${newline=='true'}"> checked </c:if> /> 개행보기
								</label>
								<!-- <label style="margin: 0 3px 0 3px;">
									<a><i class="fa fa-floppy-o"></i> 저장</a>
								</label> -->
							</li>

						</ul>

						<!-- Tab panes -->
						<div class="tab-content">
							<div role="tabpanel" class="tab-pane active" id="result">
								<div class="tableWrapper">
									<table class="table table-striped table-bordered table-hover" id="result_head" style="margin: 0; font-size: 14px">
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
								<button type="button" class="btn btn-default" onclick="sendSql('${item.menu}&${item.column}&${item.autoExecute}')">${item.keytitle}</button>
								<input type="hidden" id="${item.key}" value="${item.menu}&${item.column}&${item.autoExecute}">
							</c:forEach>
						</div>
					</div>
				</div>
			</div>
		</div>
		<c:if test="${sql != ''}">
			<textarea id="sql_org" hidden>${sql}</textarea>
			<input id="Path" name="Path" value="${Path}" type="hidden">
			<input id="refreshtimeout" value="${refreshtimeout}" type="hidden">
		</c:if>
	</section>
</div>