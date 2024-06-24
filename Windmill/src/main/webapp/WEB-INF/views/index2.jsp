<%@include file="common/common.jsp"%>
<script>
	$(document).ready(function() {

		$.ajax({
			type : 'post',
			url : '/SQL/list',
			success : function(result) {

				var child = $('#menus');

				setMenu(result, child)

			},
			error : function() {
				alert("시스템 에러");
			}
		});

	});

	function setMenu(result, parent) {

		for (var i = 0; i < result.length; i++) {
			var list = result[i];

			if (list.Path.includes('Path')) {

				var child2 = $('<ul class="nav nav-pills nav-stacked"></ul>');
				var folder;
				if (list.Path.substring(4) == 0) {
					folder = $('<div class="col-lg-3 col-md-4 col-sm-6"><div class="box box-solid"></div></div>');
				} else {

					folder = $('<div class="col-md-12"><div class="box box-solid"></div></div>');
				}
				var folder2 = $('<div class="box-header with-border" style="background-color: #605ca810; border-radius: 10px;"><h3 class="box-title">'
						+ list.Name
						+ '</h3><div class="box-tools"><button type="button" class="btn btn-box-tool" data-widget="collapse"><i class="fa fa-minus"></i></button></div></div>');
				var child1 = $('<div class="box-body no-padding"></div>');

				child1.append(setMenu(list.list, child2));
				folder2.append(child1);
				folder.append(folder2);

				parent.append(folder);
			} else if (list.Name.includes('.htm')) {

				var childItem = $('<li><a href="/HTML?Path='
						+ encodeURI(list.Path) + '" target="iframe" id="'
						+ list.Name.split('_')[0] + '">'
						+ list.Name.split('.')[0] + '</a></li>');
				parent.append(childItem);

			} else {

				var childItem = $('<li><a href="/SQL?Path='
						+ encodeURI(list.Path) + '" target="iframe" id="'
						+ list.Name.split('_')[0] + '">'
						+ list.Name.split('.')[0] + '</a></li>');
				parent.append(childItem);
			}
		}

		return parent;

	}
</script>
<div class="content-wrapper" style="margin-left: 0">
	<section class="content-header">

		<ol class="breadcrumb">
			<li>
				<a href="#"><i class="icon ion-ios-home"></i> Home</a>
			</li>
		</ol>
	</section>
	<section class="content">
		<div class="row" id="menus">
			<p></p>
		</div>
	</section>
</div>
