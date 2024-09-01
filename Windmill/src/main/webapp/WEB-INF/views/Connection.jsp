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
			});
	
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
			},
			error : function() {
				alert("시스템 에러");
			}
		});
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
				DBTYPE : $('#DBTYPE').val()
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
								<option value="Tibero">Tibero</option>
								<option value="PostgreSQL">PostgreSQL</option>
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
				</div>
				<!-- /.box-body -->
				<div class="box-footer">
					<button type="submit" class="btn btn-primary form-control">저장</button>
				</div>
			</form>
		</div>
	</section>
</div>
No newline at end of file
