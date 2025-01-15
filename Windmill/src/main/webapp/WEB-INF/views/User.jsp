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
.required label:after {
	content: "*";
	color: red;
}
</style>
<script>

var arr;

	$(document).ready(
			function() {
				getUserList();

				$.ajax({
					type : 'post',
					url : "/Connection/list",
					data : {
						TYPE : "DB"
					},
					success : function(result) {

						for (var i = 0; i < result.length; i++) {

							if (result[i].split('.')[0] == $(
									'#selectedConnection').val()) {
								$('#CONNECTION').append(
										"<option value=\""
												+ result[i].id.split('.')[0]
												+ "\"  selected=\"selected\">"
												+ result[i].id.split('.')[0]
												+ "</option>");
							} else {
								$('#CONNECTION').append(
										"<option value='"
												+ result[i].split('.')[0]
												+ "'>"
												+ result[i].split('.')[0]
												+ "</option>");
							}
						}

						if ($(".paramvalue").eq(0).val() != ''
								&& $(".paramvalue").length > 0) {
							excute();
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
				                if (arr[i].id.match(new RegExp(val,'i'))) {
				                    var $li = $('<li/>').html(arr[i].id).attr('data-value', arr[i].id);
				                	$queryResults.append($li).show();
				           	 	} else if (arr[i].name.match(new RegExp(val,'i'))) {
				                    var $li = $('<li/>').html(arr[i].name).attr('data-value', arr[i].id);
				                	$queryResults.append($li).show();
				           	 	}
				        });

				        $('li').on('click', function() {
				            var selectedVal = $(this).attr('data-value');
				            $('#query').val(selectedVal);
				            
				            UserDetail(selectedVal);
				            $("#userlist").val(selectedVal)

				            
				        });
				    } else {
				            $queryResults.html("").hide();
				    }
				  }
				});

			});
	
	function getUserList(){
		
		$.ajax({
			type : 'post',
			url : "/User/list",
			data : {
				TYPE : ""
			},
			success : function(result) {
				
				arr=result;
				$('#userlist').empty();
				$('#userlist').append("<option value='' selected disabled hidden>==선택하세요==</option>");
				$('#userlist').append("<option id='create_option' value='create'>새로 만들기</option>");
				
				
				for (var i = 0; i < result.length; i++) {
					$('#userlist').append(
							"<option value='" + result[i].id.split('.')[0]
									+ "'>" + result[i].id.split('.')[0]
									+ "</option>");
				}
			},
			error : function() {
				alert("시스템 에러");
			}
		});
	}
	
	function getSelectValues(select) {
		var result = [];
		var options = select && select.options;
		var opt;

		for (var i = 0, iLen = options.length; i < iLen; i++) {
			opt = options[i];

			if (opt.selected) {
				result.push(opt.value || opt.text);
			}
		}

		return result;
	}

	function UserDetail(value) {
		
		$('#query-results').html("").hide();
		
		$('#query').val("");
		
		$('#ID').val('');
		$('#NAME').val('');
		$('#IP').val('');
		$('#PW').val('');

		
		document.querySelectorAll("#MENU option").forEach((element) => {
			element.removeAttribute("selected");
		});
		
		document.querySelectorAll("#CONNECTION option").forEach((element) => {
			element.removeAttribute("selected");
		});

		if (value == 'create') {
			$('#ID').removeAttr("disabled");
			
			return;
		} else {
			$('#ID').attr("disabled", "disabled");
		}
		
		$.ajax({
			type : 'post',
			url : '/User/detail',
			data : {
				ID : value
			},
			success : function(result) {

				$('#IP').val(result.IP);
				$('#PW').val(result.PW);
				$('#ID').val(result.ID);
				$('#NAME').val(result.NAME);
				
				for ( var it of result.MENU.split(',')) {
					$("#MENU option[value='"+it+"']").attr("selected","selected");
				}
				for ( var it of result.CONNECTION.split(',')) {
					$("#CONNECTION option[value='"+it+"']").attr("selected","selected");
				}

			},
			error : function() {
				alert("시스템 에러");
			}
		});
	}
	
	function save() {
		var filename = $("#userlist").val();
		if (filename == 'create') {
			filename = $('#ID').val();
		}

		$.ajax({
			type : 'post',
			url : '/User/save',
			data : {
				file : filename,
				ID : $('#ID').val(),
				NAME : $('#NAME').val(),
				IP : $('#IP').val(),
				PW : $('#PW').val(),
				MENU : getSelectValues(document.getElementById('MENU'))
						.toString(),
				CONNECTION : getSelectValues(
						document.getElementById('CONNECTION')).toString(),
			},
			success : function(result) {
				alert("저장 되었습니다.");
				location.reload();
			},
			error : function() {
				alert("저장되지 않았습니다.");
			}
		});
		
		
	}
	
	function resetPW() {
		var filename = $("#userlist").val();
		if (filename == 'create') {
			filename = $('#ID').val();
		}

		$.ajax({
			type : 'post',
			url : '/User/resetPW',
			data : {
				file : filename,
				ID : $('#ID').val(),
			},
			success : function(result) {
				alert("비밀번호가 초기화 되었습니다.");
				location.reload();
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
		<h1>User관리</h1>
		<ol class="breadcrumb">
			<li>
				<a href="#"><i class="icon ion-ios-home"></i> Home</a>
			</li>
			<li class="active">
				<a href="#">User관리</a>
			</li>
		</ol>
	</section>
	<section class="content">
		<div class="row" style="margin: 0">
			<div class="col-md-1 autocomplete-wrapper" style="width: 170px">
				<input class="form-control" type="text" id="query" autocomplete="off" placeholder="아이디/이 검색">
				<ul id="query-results"></ul>
			</div>
			<div class="col-md-1" style="width: 200px">
				<select class="form-control" id="userlist" onchange="UserDetail(this.value)">
					<option value="" selected disabled hidden>==선택하세요==</option>
					<option id="create_option" value="create">새로 만들기</option>
				</select>
			</div>

		</div>
		<div class="box box-default" style="margin-top: 10px;">
			<div class="box-header with-border">
				<h3 class="box-title">User Detail</h3>
			</div>
			<!-- /.box-header -->
			<!-- form start -->
			<form role="form" action="javascript:save();">
				<div class="box-body">
					<div class="col-md-6">
						<div class="form-group row">
							<div class="col-md-6 required" style="margin: 2px 0;">
								<label for="ID">ID</label>
								<input type="text" class="form-control" id="ID" placeholder="ID" disabled="disabled" required="required">
							</div>
							<div class="col-md-6 required" style="margin: 2px 0;">
								<label for="NAME">NAME</label>
								<input type="text" class="form-control" id="NAME" placeholder="NAME" required="required">
							</div>

						</div>
						<div class="form-group row">
							<div class="col-md-6" style="margin: 2px 0;">
								<label for="IP">IP</label>
								<input type="text" class="form-control" id="IP" placeholder="IP">
							</div>
							<div class="col-md-6 required" style="margin: 2px 0;">
								<label for="PW">PW</label>
								<input type="password" class="form-control" id="PW" placeholder="PW" required="required">
								
							</div>
						</div>
					</div>
					<div class="col-md-6">
						<div class="col-md-6" style="margin: 2px 0; height: 250px">
							<label for="MENU">MENU</label>
							<select multiple class="form-control" id="MENU" style="height: 85%">
								<c:forEach var="item" items="${MENU}">
									<option value="${item.Name}">${item.Name}</option>
								</c:forEach>
							</select>
						</div>

						<div class="col-md-6" style="margin: 2px 0; height: 250px">
							<label for="MENU">CONNECTION</label>
							<select multiple class="form-control" id="CONNECTION" style="height: 85%">

							</select>
						</div>
					</div>

				</div>
				<!-- /.box-body -->
				<div class="box-footer">
				<div class="col-md-6">
					<button type="button" class="btn btn-primary form-control" onclick="resetPW()">비밀번호 초기화</button>
					</div>
					<div class="col-md-6">
					<button type="submit" class="btn btn-primary form-control">저장</button>
					</div>
				</div>
			</form>
		</div>
	</section>
</div>