<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Dex</title>
<meta content='width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no' name='viewport'>
<!-- Bootstrap 3.3.4 -->
<link href="/resources/bootstrap/css/bootstrap.min.css" rel="stylesheet" type="text/css" />
<!-- Font Awesome Icons -->
<link href="https://maxcdn.bootstrapcdn.com/font-awesome/4.3.0/css/font-awesome.min.css" rel="stylesheet" type="text/css" />
<!-- Ionicons -->
<link href="/resources/ionicons/2.0.1/css/ionicons.min.css" rel="stylesheet" type="text/css" />
<!-- Theme style -->
<link href="/resources/dist/css/AdminLTE.min.css" rel="stylesheet" type="text/css" />
<!-- AdminLTE Skins. Choose a skin from the css/skins 
         folder instead of downloading all of them to reduce the load. -->
<link href="/resources/dist/css/skins/_all-skins.min.css" rel="stylesheet" type="text/css" />
<style type="text/css">
body {
	margin: 0
}

#sidemenu {
	max-height: calc(100vh - 320px);
	overflow-y: auto;
}

::-webkit-scrollbar {
	width: 3px;
	height: 7px;
	border: 2px solid #fff;
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
</head>
<!-- jQuery 2.1.4 -->
<script src="/resources/plugins/jQuery/jQuery-2.1.4.min.js"></script>
<script>
	$(document).ready(function() {

		$.ajax({
			type : 'post',
			url : '/SQL/list',
			success : function(result) {

				//alert(JSON.stringify(result))

				var sidebar = $('#tree');
				var parent = $('<li class="active treeview menu-open"><a class="addtree" href="#"> <i class="fa fa-bolt"></i> <span>SQL</span> <i class="fa fa-angle-left pull-right"></a></i>');
				var child = $('<ul class="treeview-menu" id="sidemenu"></ul>');

				child.append(setMenu(result, child));
				parent.append(child);
				sidebar.append(parent);

			},
			error : function() {
				alert("시스템 에러");
			}
		});

		$(document).on("click", ".addtree", function() {

			if ($(this).parent().attr('class').includes('active')) {
				$(this).parent().removeClass('active');
			} else {
				$(this).parent().addClass('active');
			}

		});

	});

	function setMenu(result, parent) {
		

		for (var i = 0; i < result.length; i++) {
			var list = result[i];

			if (list.Path == 'Path') {
				var folder = $('<li class="treeview">\n' + '          <a class="addtree" href="#">\n' + '<span>' + list.Name + '</span><i class="fa fa-angle-left pull-right"></i></a>\n' + '        </li>');
				var child = $('<ul class="treeview-menu"></ul>');
				folder.append(setMenu(list.list, child));

				parent.append(folder);
			} else {
				var childItem = $('<li><a href="/SQL?Path=' + encodeURI(list.Path) + '" target="iframe" id="' + list.Name.split('_')[0] + '">' + list.Name.split('.')[0] + '</a></li>');
				parent.append(childItem);
			}
		}

		return parent;

	}

	function Search() {

		if ($('#' + $("#search").val()).length == 0) {
			alert("메뉴가 없습니다.")
			return false;
		}

		iframe.location.href = $('#' + $("#search").val()).attr('href');

		return false;
	}
</script>
<body class="sidebar-mini skin-purple-light">
	<div class="wrapper">
		<header class="main-header">
			<!-- Logo -->
			<a href="/index" class="logo"> <!-- mini logo for sidebar mini 50x50 pixels --> <span class="logo-mini"><b>D</b>eX</span> <!-- logo for regular state and mobile devices -->
				<span class="logo-lg"><b>Data</b> Explorer</span>
			</a>
			<!-- Header Navbar: style can be found in header.less -->
			<nav class="navbar navbar-static-top" role="navigation">
				<!-- Sidebar toggle button-->
				<a href="#" class="sidebar-toggle" data-toggle="offcanvas" role="button"> <span class="sr-only">Toggle navigation</span> <span
					class="icon-bar"></span> <span class="icon-bar"></span> <span class="icon-bar"></span>
				</a>
			</nav>
		</header>
		<!-- Left side column. contains the logo and sidebar -->
		<aside class="main-sidebar">
			<!-- sidebar: style can be found in sidebar.less -->
			<section class="sidebar" id="sidebar">
				<!-- search form -->
				<form class="sidebar-form" onsubmit="return Search()">
					<div class="input-group">
						<input type="text" name="q" class="form-control" placeholder="Search..." id="search" />
						<span class="input-group-btn">
							<button type="button" name='search' id='search-btn' class="btn btn-flat" onclick="Search()">
								<i class="fa fa-search"></i>
							</button>
						</span>
					</div>
				</form>
				<!-- /.search form -->
				<!-- sidebar menu: : style can be found in sidebar.less -->
				<ul class="sidebar-menu" data-widget="tree" id="tree">
					<li class="header">MAIN NAVIGATION</li>
					<li class="treeview"><a href="/Connection" target="iframe"> <i class="fa fa-database"></i> <span>Connection</span>
					</a> <!-- <ul class="treeview-menu" id="ConnectionList">
							<li><a href="/Connection?DB=2"><i class="fa fa-circle-o"></i> DB1</a></li>
							<li><a href="/Connection?DB=1"><i class="fa fa-circle-o"></i> DB2</a></li>
						</ul> --></li>
					<li class="treeview"><a href="/FileRead" target="iframe"> <i class="fa fa-file-text-o"></i> <span>FileRead</span></a></li>
					<li class="treeview"><a href="/FileUpload" target="iframe"> <i class="fa fa-file-text-o"></i> <span>FileUpload</span></a></li>
				</ul>
			</section>
			<!-- /.sidebar -->
		</aside>
		<div class="content-wrapper" id="framebox">
			<iframe name="iframe" id="iframe" style="margin: 0; width: 100%; height: calc(100vh - 110px); border: none; overflow: auto;" src="/index2"></iframe>
		</div>