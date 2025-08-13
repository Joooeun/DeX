<%@include file="common/common.jsp"%>
<c:set var="textlimit" value="1900000" />

<!-- ======================================== -->
<!-- SQL 실행 페이지 - 스타일 정의 -->
<!-- ======================================== -->
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

.form-group.required .param:after {
	content: "*";
	color: red;
}

.textcontainer {
	display: flex;
	border: 1px solid rgb(203, 213, 225);
	border-radius: 0.5rem;
	overflow: hidden;
	max-height: 200px;
}

.formtextarea {
	border: none;
	outline: none;
	width: 100%;
	margin-bottom: 5px;
}

.container__lines {
	border-right: 1px solid rgb(203, 213, 225);
	text-align: right;
	overflow: hidden;
	padding: 0 5px;
}

.expenda .collapsed:after {
	content: '더보기 ▼';
}

#expenda:not(.collapsed):after {
	content: '접기 ▲';
}

.tabulator .tabulator-header .tabulator-col .tabulator-col-content .tabulator-col-title
	{
	white-space: normal;
}

.tabulator-tableholder {
	height: calc(85vh - 31px) !important;
}

#result_table {
	font-family: D2Coding !important;
}
</style>

<!-- ======================================== -->
<!-- SQL 실행 페이지 - JavaScript 변수 및 함수 -->
<!-- ======================================== -->
<script>

// 차트 관련 변수
var ctx;
var myChart;
var graphcolor = ['#FF583A','#4B963E', '#FF9032', '#23439F','#FEDD0F','#561475', '#F2626B','#89E077','#FEBA4F','#83C3FF', '#FFEA7F','#C381FD', '#525252']

// SQL 텍스트 저장 변수
var sql_text = "";

// 자동 새로고침 관련 변수
var timeRemain = null;
var isPaused = false; // 일시정지 상태 변수
var refreshTimer = null; // 타이머 변수

// 테이블 관련 변수
var table;
var column=[];
var data;
var tableoption;
var temp_column;
var tableHeight=0;

	// ========================================
	// 페이지 로드 시 초기화 함수
	// ========================================
	$(document).ready(function() {
		
		// 일시정지 버튼 초기화
		$("#pauseBtn").hide();

		// 차트 초기화
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

		// ========================================
		// 데이터베이스 연결 목록 조회
		// ========================================
		$.ajax({
			type: 'post',
			url: "/Connection/list",
			data: {
				TYPE: "DB"
			},
			success: function(result) {
				

				const list = "${DB}" == "" ? [] : "${DB}".split(",")

				for (var i = 0; i < result.data.length; i++) {

					if (list.length > 0 && !list.includes(result.data[i].split('.')[0])) {
						continue;
					}

					if (result.data[i].split('.')[0] == $('#selectedConnection').val() || list.length == 1) {
						$('#connectionlist').append("<option value=\"" + result.data[i].split('.')[0] + "\"  selected=\"selected\">" + result.data[i].split('.')[0] + "</option>");
					} else {
						$('#connectionlist').append("<option value='" + result.data[i].split('.')[0] + "'>" + result.data[i].split('.')[0] + "</option>");
					}
				}

				var shortkey = ${Excute};

				if (shortkey && $("#connectionlist option:selected").val() != '') {
					excute();
				}
			},
			error: function() {
				alert("시스템 에러");
			}
		});

		// ========================================
		// 키보드 단축키 이벤트 처리 (F1~F12)
		// ========================================
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

		// ========================================
		// 텍스트 영역 이벤트 처리 (Ctrl+Enter, 입력 제한)
		// ========================================
		document.querySelectorAll(".formtextarea").forEach((element) => {
			// Ctrl+Enter로 폼 제출
			element.addEventListener("keydown", function(e) {
				if (e.ctrlKey && e.keyCode == 13) {

					if (document.ParamForm.checkValidity())
						document.ParamForm.submit();
					e.preventDefault();
				}
			})

			// 텍스트 입력 시 라인 번호 업데이트 및 길이 제한
			element.addEventListener('input', function() {
				const lineNumbersEle = document.getElementById('line-numbers');
				const lines = $(this).val().split('\n');

				lineNumbersEle.innerHTML = Array.from({
					length: lines.length
				}, (v, i) => '<div>' + (i + 1) + '</div>').join('');

				if ($(this).val().length > ${textlimit}) {
					alert('입력가능한 범위를 벗어났습니다. 최대 : ${textlimit}');
					$(this).val($(this).val().substring(0, ${textlimit}))
				}
				$("#textcount").text($(this).val().length)
			});

		});

		// ========================================
		// 파라미터 입력 필드 이벤트 처리 (Enter 키)
		// ========================================
		document.querySelectorAll(".paramvalue input").forEach((element) => {
			element.addEventListener("keydown", function(e) {
				if (e.keyCode == 13) {

					document.ParamForm.submit();
					e.preventDefault();
				}
			})
		});


		// ========================================
		// 결과 행 클릭 이벤트 처리
		// ========================================
		$(document).on("click", ".Resultrow", function() {
			$(".Resultrow").removeClass('success');
			$(this).addClass('success');
		});

		// ========================================
		// 개행 보기 체크박스 이벤트 처리
		// ========================================
		$(document).on("change", "#newline", function() {
			
			if(column==null)
				return;
			
			if ($(this).prop('checked')) {
				
				column = column.map((item)=>{
					return {...item, formatter : "textarea", width:undefined}
				});
				table = new Tabulator("#result_table", {
					data: data,
					columns: column,
					...tableoption,
					height: $('#result_table').hasClass( "in" )?"85vh":tableoption.height,
				});
				
				setTimeout(() => {
					table.redraw();
					
				}, 100); 
				
				
			} else {
				
				column = column.map((item)=>{return {...item, formatter : "plaintext"}});
				
				table = new Tabulator("#result_table", {
					data: data,
					columns: column,
					...tableoption,
					height: $('#result_table').hasClass( "in" )?"85vh":tableoption.height,
				});
			}
		});

		// ========================================
		// 텍스트 영역 스크롤 동기화
		// ========================================
		$('.formtextarea').on('scroll', () => {
			$('.container__lines').scrollTop($('.formtextarea').scrollTop());
		});

		// ========================================
		// 파일 다운로드 이벤트 처리
		// ========================================
		$(document).on("click", "#save_excel", function() {
			table.download("xlsx", `${title}_${memberId}_` + dateFormat()+".xlsx");
		});
		
		$(document).on("click", "#save_csv", function() {
			table.download("csv", `${title}_${memberId}_` + dateFormat()+".csv");
		});
		
		
		
		// ========================================
		// 결과 테이블 확장/축소 버튼 이벤트
		// ========================================
		$("#expenda").click(function() {
		    $([document.documentElement, document.body]).animate({
		        scrollTop: $("#result_table").offset().top-50
		    }, 700);
		});

	});
	
	// ========================================
	// SQL 실행 시작 함수
	// ========================================
	function startexcute() {

		if ($("#excutebtn").attr('disabled') == 'disabled') {
			return;
		}

		if ($("#connectionlist option:selected").val() == '') {
			alert("Connection을 선택하세요.");

		} else {
			excute();

		}
		return false;
	}

	// ========================================
	// 자동 새로고침 타이머 함수
	// ========================================
	function refresh() {
		// 일시정지 상태면 타이머 중단
		if (isPaused) {
			return;
		}
		
		timeRemain--;
		$("#excutebtn").val('wait...' + timeRemain + 's')
		if (timeRemain == 0) {
			timeRemain = $("#refreshtimeout").val();
			excute();
		} else {
			refreshTimer = setTimeout(() => {
				refresh();
			}, 1000);
		}
	}

	// ========================================
	// 일시정지/재개 토글 함수
	// ========================================
	function togglePause() {
		if (isPaused) {
			// 재개
			isPaused = false;
			$("#pauseBtn").html('<i class="fa fa-pause"></i> 일시정지');
			$("#pauseBtn").removeClass('btn-warning').addClass('btn-info');
			// 타이머 재시작
			if (timeRemain > 0) {
				refresh();
			}
		} else {
			// 일시정지
			isPaused = true;
			$("#pauseBtn").html('<i class="fa fa-play"></i> 재개');
			$("#pauseBtn").removeClass('btn-info').addClass('btn-warning');
			// 타이머 중단
			if (refreshTimer) {
				clearTimeout(refreshTimer);
				refreshTimer = null;
			}
		}
	}

	// ========================================
	// 자동 새로고침 시작 함수
	// ========================================
	function startAutoRefresh() {
		if ($("#refreshtimeout").val() > 0) {
			timeRemain = $("#refreshtimeout").val();
			isPaused = false;
			$("#pauseBtn").show();
			$("#pauseBtn").html('<i class="fa fa-pause"></i> 일시정지');
			$("#pauseBtn").removeClass('btn-warning').addClass('btn-info');
			// 자동 재실행 중에는 실행 버튼 비활성화 유지
			$("#excutebtn").attr('disabled', true);
			refresh();
		}
	}

	// ========================================
	// 자동 새로고침 중지 함수
	// ========================================
	function stopAutoRefresh() {
		isPaused = true;
		if (refreshTimer) {
			clearTimeout(refreshTimer);
			refreshTimer = null;
		}
		$("#pauseBtn").hide();
		$("#excutebtn").attr('disabled', false);
		$("#excutebtn").val('실행');
	}

	// ========================================
	// 트랜잭션 커밋 함수
	// ========================================
	function commit() {
		$.ajax({
			type: 'post',
			url: '/SQL/commit',
			data: {
				connectionId: $("#connectionlist").val()
			},
			error: function() {
				alert("시스템 에러");
			}
		});
	}
	
	// ========================================
	// SQL 실행 메인 함수
	// ========================================
	async function excute() {

		var log = {};
		var params=[];

		// 파라미터 유효성 검사 및 수집
		for (var i = 0; i < $(".paramvalue").length; i++) {

			if ($(".paramvalue").eq(i).attr('required') == 'required' && $(".paramvalue").eq(i).val() == "") {
				alert($(".paramvalue").eq(i).attr('paramtitle') + "을 입력하세요.")
				return;
			}

		 	if ($(".paramvalue").eq(i).attr('paramtype') == 'log' && $(".paramvalue").eq(i).val().length > 0) {
				log[$(".paramvalue").eq(i).attr('paramtitle')] = $(".paramvalue").eq(i).val()
			} else{
				params.push({title:$(".paramvalue").eq(i).attr('paramtitle'), value:$(".paramvalue").eq(i).val(), type : $(".paramvalue").eq(i).attr('paramtype')});
				
			}
		}

		// 실행 버튼 비활성화 및 로딩 표시
		$("#excutebtn").attr('disabled', true);
		$("#loadingdiv").css('display','block')

		let ondate = new Date();
		
		// 2025-04-29 limit 2만으로 제한
		var limit = ${not empty limit}

		// SQL 실행 AJAX 요청
		await $.ajax({
			type: 'post',
			url: '/SQL/excute',
			data: {
				sql:  $("#sql_text").val(),
				log: JSON.stringify(log),
				Path: $("#Path").val()+"/"+'${title}.sql',
				title: '${title}',
				autocommit: true,
				/* autocommit : $("#autocommit").prop('checked'), */
				audit: ${audit == null ? false : audit},
				connectionId: $("#connectionlist").val(),
				
				// 2025-04-29 limit 2만으로 제한
				// 환경설정에 limit이 설정되어 있는데 0이나 2만 이상으로 조회하는 경우 2만으로 제한, 
				limit: $("#limit").val() == 0 ? (limit ? 20000 : 0) : (limit ?  Math.min(20000,  $("#limit").val()) : $("#limit").val()),
				params : JSON.stringify(params)
			},
			success: function(result, status, jqXHR) {
				
				// 테이블 높이 계산 (최초 1회만)
				if(tableHeight==0){
					tableHeight = Math.max(200,$("#test").height() - $(".content-header").outerHeight(true) - $("#Keybox").outerHeight(true) - $("#top").outerHeight(true) - 200);
				}
				
				// 세션 만료 체크
				if (jqXHR.getResponseHeader("SESSION_EXPIRED") === "true") {
					alert("세션이 만료되었습니다.");
					window.parent.location.href = "/Login";
				}

				$("#Resultbox").css("display", "block");

				var newline = $("#newline").prop('checked');
				

				// 결과 헤더 처리
				if (result.rowhead!=null) {
					
					// 컬럼 정보가 변경된 경우에만 재생성
					if(JSON.stringify(result.rowhead)==JSON.stringify(temp_column)){
						
					}else{
						temp_column = result.rowhead
						column = [];
						
						// 컬럼 정의 생성
						for (var title = 0; title < result.rowhead.length; title++) {
							
							
							if (result.rowlength) {
							
								var culmnitem = {
									title: result.rowhead[title].title,
									field: title+'',
									headerTooltip: result.rowhead[title].desc
								}

								if (result.rowhead[title].rowlength >= 50) {
									culmnitem.width = 4 * result.rowhead[title].rowlength / 10 + 'vw';
								}

								
								if ([-6, 5, 4, 6, 7, 8, 2, -5, 3].includes(result.rowhead[title].type)) {
									culmnitem.hozAlign = "right";
								} else {
									if (newline) {
										culmnitem.formatter = "textarea"
									} else {
										culmnitem.formatter = "plaintext"
									}

								}

								column.push(culmnitem);

							} else {
								column.push({
									title: result.rowhead[title].title,
									field: title+'',
								});
							}
						}
					}

					

					// 차트 데이터가 있는 경우 차트 생성
					if(result.rowbody.length>0)
					chart(result);

				}
				
				// 가상 스크롤 버퍼 설정
				var v_buffer = 40;

				// 결과 데이터 변환
				data = result.rowbody.map((item, index) => {
					
					var obj = {};
					item.map((it, idx) => {
						
						if(it!=null){
							var text = it + "";
							if(text.trim().startsWith('<')){
								var div = document.createElement("div");
								div.innerHTML = it;
								text = div.textContent || div.innerText || "";
							}
							
							if(v_buffer<text.split("\n").length){
								v_buffer = text.split("\n").length
							}
							obj[column[idx].field] = text;
						}else{
							obj[column[idx].field] = undefined
						}
						
					
					})
					return obj
				})

				// 테이블 옵션 설정
				tableoption = {
					maxHeight: "85vh",
					height: tableHeight,
					rowHeader: {
						formatter: "rownum",
						headerSort: false,
						hozAlign: "left",
						resizable: false,
						frozen: true,
					},
					layout: "fitDataFill",
					renderVerticalBuffer: v_buffer*30, // 가상 DOM 버퍼 설정    
					//renderVertical : "basic" ,
					renderHorizontal: "virtual",
					autoResize:false, 
					resizableColumnGuide: true,
					placeholder: "데이터가 없습니다.",
					rowFormatter: function(row) {
						row.getElement().classList.add("Resultrow");
					},
					columnDefaults:{
				        headerTooltip:function(e,cell,onRendered){
				            return result.rowhead.find((item,idx)=> (idx+'') == cell.getField()).desc; 
				        },
				    }
				}
			    
							    // Tabulator 테이블 생성
			    table = new Tabulator("#result_table", {
					data: data,
					columns: newline ? column.map((item)=>{
						return {...item, width:undefined}
					}):column,
					...tableoption,
					height: $('#result_table').hasClass( "in" )?"85vh":tableoption.height,
				});
				
				// 컬럼 크기 조정 이벤트 처리
				table.on("columnResized", function(col){
				    
					column = column.map((it, idx)=>{
						if(it.title == col.getField())
							return {...it, width:col.getWidth()}
						else
							return it
					})
					
				});
				
					
				// 결과 테이블 축소 상태 설정
				if(!$('#result_table').hasClass( "collapse" )){
					$('#result_table').addClass('collapse');
					$('#result_table').attr('aria-expanded', 'false');
					$('#result_table').css('min-height', tableHeight + "px");
//	 				$('#result_table').css('height', tableHeight + "px");
					$('#expenda').parent().addClass('expenda');
				}
					

				// 테이블 다시 그리기
				setTimeout(() => {
					table.redraw();
				}, 100);

				// 결과 정보 표시
				$("#result-text").text('total : ' + data.length + ' records, on ' + dateFormat2(ondate));
				$("#save").css('display', 'block');

				$("#loadingdiv").css('display','none')

				// 자동 새로고침 처리
				if ($("#refreshtimeout").val() > 0) {
					startAutoRefresh();
				} else {
					// 자동 재실행이 아닌 경우에만 버튼 활성화
					$("#excutebtn").attr('disabled', false);
					$("#excutebtn").val('실행');
				}
			},
			error: function(error) {
				// 에러 처리
				$("#excutebtn").attr('disabled', false);
				$("#excutebtn").val('실행');
				$("#loadingdiv").css('display','none')
				// 에러 발생 시 자동 새로고침 중지
				stopAutoRefresh();
				console.log(JSON.stringify(error))
				alert('시스템오류');
			}
		});
	}
	
	// ========================================
	// 연결 세션 설정 함수
	// ========================================
	function sessionCon(value) {

		const list = "${DB}" == "" ? [] : "${DB}".split(",");

		if (list.length != 1) {
			$.ajax({
				type: 'post',
				url: '/Connection/sessionCon',
				data: {
					connectionId: value
				},
				success: function(result) {
					//alert("성공")
				},
				error: function() {
					//alert("시스템 에러");
				}
			});
		}

	}


	// ========================================
	// 유틸리티 함수들
	// ========================================
	function ConvertSystemSourcetoHtml(str) {
		/* str = str.replace(/</g,"&lt;");
		str = str.replace(/>/g,"&gt;");
		str = str.replace(/\"/g,"&quot;");
		str = str.replace(/\'/g,"&#39;");*/
		return str;
	}

	function forxmp(str) {
		str = str.replace(/\x00/g, "");
		return str;
	}


	// ========================================
	// 파일 읽기 함수
	// ========================================
	function readfile() {
		$.ajax({
			type: 'post',
			url: '/SQL/readfile',
			success: function(result) {
				alert(result)
			},
			error: function() {
				alert("시스템 에러");
			}
		});
	}

	// ========================================
	// 차트 생성 함수
	// ========================================
	function chart(result) {
		
		if(myChart.data.labels.length==1){	
			
			myChart.destroy();
		
			var chardata = transpose(result.rowbody)
	
			var labels = result.rowhead.map((item)=>item.title);
			
			var datasets = [];
			var maxdata = 0;	
			
			// 데이터셋 생성
			for (var i = 1; i < labels.length; i++) {
				
				var data = result.rowbody.map((item)=>parseInt(item[i])) 
				
				if (maxdata < Math.max(...data)) {
					maxdata = Math.max(...data);
				}
	
				datasets.push({
					label: labels[i],
					data: data,
					fill: false,
					borderColor: graphcolor[i - 1],
					tension: 0.1,
					hidden: Math.max(...data) < maxdata / 10
				})
			}
	
	
			const datas = {
				labels: result.rowbody.map((item)=>item[0]),
				datasets: datasets
			};
	
			myChart = new Chart(ctx, {
				type: 'line',
				data: datas,
				options: {
					maintainAspectRatio: false,
				}
			});
			
		} else {
			updateChart(result)
			
		}
	}
	
	// ========================================
	// 차트 업데이트 함수
	// ========================================
	function updateChart(result) {
        
        var datasets = [];
		
		var labels = result.rowhead.map((item)=>item.title);
		
		try {
	        myChart.data.labels = result.rowbody.map((item)=>item[0]);
	        for (var i = 1; i < labels.length; i++) {
	        	
	        	var data = result.rowbody.map((item)=>parseInt(item[i]))
				//myChart.data.datasets[i-1].label = labels[i];
	        	myChart.data.datasets[i-1].data = data;
	
			}
	        
		} catch (e) {
			console.error('차트 데이터 업데이트 중 오류 발생:', e);
			sendErrorToServer({
			        type: "debug",
			        data : myChart.data,
			        labels : labels, 
					error:e.stack
			        
			    });
		}
		
		myChart.update('none');
    }

	// ========================================
	// 랜덤 색상 생성 함수
	// ========================================
	function random_rgba() {
		var o = Math.round,
			r = Math.random,
			s = 255;
		//return 'rgba(' + o(r()*s) + ',' + o(r()*s) + ',' + o(r()*s) + ',' + r().toFixed(1) + ')';
		return 'rgb(' + o(r() * s) + ',' + o(r() * s) + ',' + o(r() * s) + ')';
	}


	// ========================================
	// 배열 전치(transpose) 함수
	// ========================================
	function transpose(a) {

		// Calculate the width and height of the Array
		var w = a.length || 0;
		var h = a[0] instanceof Array ? a[0].length : 0;

		// In case it is a zero matrix, no transpose routine needed.
		if (h === 0 || w === 0) {
			return [];
		}

		/**
			* @var {Number} i Counter
			* @var {Number} j Counter
			* @var {Array} t Transposed data is stored in this array.
			*/
		var i, j, t = [];

		// Loop through every item in the outer array (height)
		for (i = 0; i < h; i++) {

			// Insert a new row (array)
			t[i] = [];

			// Loop through every item per item in outer array (width)
			for (j = 0; j < w; j++) {

				// Save transposed data.
				t[i][j] = a[j][i];
			}
		}

		return t;
	}

	// ========================================
	// 날짜 포맷 함수 (YYYYMMDD_HHMMSS)
	// ========================================
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

		return date.getFullYear() + "" + month + "" + day + '_' + hour + minute + second;
	}

	// ========================================
	// 날짜 포맷 함수 (YYYY-MM-DD HH:MM:SS)
	// ========================================
	function dateFormat2(date) {

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

		return date.getFullYear() + "-" + month + "-" + day + " " + hour + ":" + minute + ":" + second;
	}

	// ========================================
	// Limit 값 체크 함수
	// ========================================
	function checkLimit(limit) {
		if (limit.value == '') {
			$(limit).val(0)
		}
	}
	
</script>

<!-- ======================================== -->
<!-- SQL 실행 페이지 - HTML 구조 -->
<!-- ======================================== -->
<!-- Content Wrapper. Contains page content -->
<div class="content-wrapper" style="margin-left: 0; position: relative;" id="test">
	<!-- ======================================== -->
	<!-- 페이지 헤더 영역 -->
	<!-- ======================================== -->
	<section class="content-header">
		<h1>${title}</h1>
		<span>${desc}</span>

		<ol class="breadcrumb">
			<li><a href="#"><i class="icon ion-ios-home"></i> Home</a></li>
			<li class="active"><a href="#" onclick="readfile()">SQL</a></li>

		</ol>
	</section>
	<!-- ======================================== -->
	<!-- 메인 콘텐츠 영역 -->
	<!-- ======================================== -->
	<section class="content">
		<!-- ======================================== -->
		<!-- 파라미터 입력 영역 -->
		<!-- ======================================== -->
		<div class="row" id="top">
			<div class="col-xs-12">
				<div class="box box-default collapse in">
					<div class="box-header with-border">
						<h5 class="box-title">파라미터 입력</h5>
						&nbsp;&nbsp;&nbsp; <input id="selectedConnection" type="hidden" value="${Connection}"> <select id="connectionlist" onchange="sessionCon(this.value)">
							<option value="">====Connection====</option>
						</select>

					</div>

					<form role="form-horizontal" name="ParamForm" id="ParamForm" action="javascript:startexcute();">
						<div class="box-body">
							<div class="row">
								<div class="col-md-10">
									<c:if test="${!sql}">

										<div class="form-group" style="margin-bottom: 0">
											<div id="container" class="textcontainer">
												<div id="line-numbers" class="container__lines"></div>
												<textarea class="col-sm-12 col-xs-12 formtextarea" maxlength="${textlimit}" id="sql_text" style="margin: 0 0 10px 0" rows="5" wrap='off'></textarea>
											</div>
											<span id="textcount">0</span> / <span>${textlimit}</span>
										</div>
									</c:if>

									<c:forEach var="item" items="${Param}" varStatus="status">
										<c:choose>
											<c:when test="${item.type == 'text' || item.type == 'sql'}">
												<div class="col-xs-12">
													<div class="form-group" style="margin-bottom: 0">
														<span class="param" id="param${status.count}" style="padding-top: 7px; font-weight: bold; font-size: 13px">${fn:toUpperCase(item.name)}</span>
														<div id="container" class="textcontainer">

															<div id="line-numbers" class="container__lines"></div>
															<textarea class="paramvalue col-xs-12 formtextarea" maxlength="${textlimit}" paramtitle="${item.name}" rows="5" paramtype="${item.type}" style="padding: 0 2px;" wrap="off">${item.name=='memberId' ? memberId : item.value}</textarea>
														</div>
														<span id="textcount">0</span> / <span>${textlimit}</span>
													</div>
												</div>
											</c:when>

											<c:otherwise>
												<div class="col-lg-2 col-md-3 col-sm-4 col-xs-5">
													<div class="form-group ${item.required}">
														<c:if test="${item.name != 'memberId' && item.hidden !='hidden'}">
															<span class="param" id="param${status.count}" style="padding-top: 7px; font-weight: bold; font-size: 13px">${fn:toUpperCase(item.name)}</span>
														</c:if>
														<div style="margin: 2px 0">
															<input type="${item.name=='memberId' || item.hidden=='hidden' ? 'hidden' : 'text'}" class="form-control paramvalue" paramtitle="${item.name}" paramtype="${item.type}"
																value="${item.name == 'memberId' ? memberId : item.value}" style="padding: 0 2px;"
																<c:if test="${item.required=='required'}">required="required" pattern="\S(.*\S)?" title="공백은 입력할 수 없습니다."</c:if> <c:if test="${item.disabled=='disabled'}">disabled</c:if>
																<c:if test="${item.name=='memberId' || item.readonly=='readonly'}">readonly</c:if>>
														</div>
													</div>
												</div>

											</c:otherwise>
										</c:choose>
									</c:forEach>
								</div>
								<div class="col-md-2">
									<input type="hidden" id="sendvalue" name="sendvalue">
									<div style="display: flex; gap: 5px;">
										<button id="pauseBtn" type="button" class="btn btn-info btn-sm" style="display: none; flex: 1;" onclick="togglePause()">
											<i class="fa fa-pause"></i> 일시정지
										</button>
										<input id="excutebtn" type="submit" class="btn btn-primary" value="실행" style="flex: 1;">
									</div>
									<input id="Path" name="Path" value="${Path}" type="hidden">
									<!-- <input
										id="excutebtn" type="submit" class="form-control" value="실행">  -->
									<!-- <label><span
										style="font-size: small;">auto commit</span> <input
										id="autocommit" type="checkbox" checked="checked" /> </label> -->

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
		<!-- ======================================== -->
		<!-- 결과 표시 영역 -->
		<!-- ======================================== -->
		<div class="row" id="Resultbox">
			<div class="col-xs-12">
				<div class="box">
					<div class="box-header with-border">
						<!-- Nav tabs -->
						<ul class="nav nav-tabs" role="tablist">
							<li role="presentation" class="active"><a href="#result" aria-controls="result" role="tab" data-toggle="tab">Result</a></li>
							<li role="presentation"><a href="#chart" aria-controls="chart" role="tab" data-toggle="tab">Chart</a></li>
							<!-- <li style="float: right; al"><i class="fa fa-floppy-o"></i></li> -->
							<li style="float: right; margin-right: 5px"><label style="margin: 0 3px 0 3px;"> limit&nbsp; <input type="number" min="0" max="500" id="limit" value="${empty limit ? 0 :  limit}"
									onblur="checkLimit(this)" />
							</label> <label style="margin: 0 3px 0 3px;"> <input type="checkbox" id="newline" <c:if test="${newline=='true'}"> checked </c:if> /> 개행보기
							</label> <!-- <label style="margin: 0 3px 0 3px;">
									<a><i class="fa fa-floppy-o"></i> 저장</a>
								</label> --></li>

						</ul>

						<!-- Tab panes -->
						<div class="tab-content">

							<div role="tabpanel" class="tab-pane active" id="result">
								<div style="display: flex; justify-content: space-between;">
									<span id="result-text"></span>
									<c:if test="${DownloadEnable == 'true'}">
										<div id="save" style="display: flex; display: none;">
											<button id="save_excel" class="btn btn-default buttons-excel buttons-html5" type="button">
												<span><i class="fa fa-floppy-o"></i> Excel</span>
											</button>
											<button id="save_csv" class="btn btn-default buttons-csv buttons-html5" type="button">
												<span><i class="fa fa-floppy-o"></i> CSV</span>
											</button>
										</div>
									</c:if>
								</div>
								<div id="result_table" class="tabulator-placeholder table-striped table-bordered" style="display: block"></div>
								<a role="button" id="expenda" class="collapsed" data-toggle="collapse" href="#result_table" aria-expanded="false" aria-controls="result_table"></a>
							</div>

							<div role="tabpanel" class="tab-pane" id="chart">

								<div style="overflow-y: auto; overflow-x: auto; height: calc(100vh * 0.5); width: 100%;">
									<canvas id="myChart" width="100%" height="100%"></canvas>
								</div>
							</div>
						</div>
					</div>
				</div>
			</div>
		</div>
		<!-- ======================================== -->
		<!-- 단축키 영역 -->
		<!-- ======================================== -->
		<div class="row" id="Keybox">
			<div class="col-xs-12">
				<div class="box box-default" style="margin-bottom: 0px">
					<div class="box-header with-border">
						<h3 class="box-title">Short Key</h3>

					</div>
					<div class="box-body">
						<c:forEach var="item" items="${ShortKey}">
							<button type="button" class="btn btn-default btn-sm" onclick="sendSql('${item.menu}&${item.column}&${item.autoExecute}')">${item.keytitle}</button>
							<input type="hidden" id="${item.key}" value="${item.menu}&${item.column}&${item.autoExecute}">
						</c:forEach>
					</div>
				</div>
			</div>
		</div>
		<c:if test="${sql}">
			<input id="refreshtimeout" value="${refreshtimeout}" type="hidden">
		</c:if>
	</section>
	<!-- ======================================== -->
	<!-- 로딩 표시 영역 -->
	<!-- ======================================== -->
	<div id="loadingdiv" style="position: absolute; width: 100%; height: 100%; background-color: #43407520; top: 0; align-content: center; text-align: center; display: none;">
		<img alt="loading..." src="/resources/img/loading.gif" style="width: 50px;">
	</div>
</div>
