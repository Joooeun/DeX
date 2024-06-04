<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Dex</title>
<%@include file="common.jsp"%>
<style type="text/css">
@font-face {
	font-family: "D2Coding"; /* 사용하고 싶은 font-family명을 지정 */
	src: url("/resources/bootstrap/fonts/D2Coding.ttf") format('truetype');
}

body {
	margin: 0;
}

#sidemenu {
	max-height: calc(100vh - 330px);
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

<script>
	$(document)
			.ready(
					function() {
						$
								.ajax({
									type : 'post',
									url : '/SQL/list',
									success : function(result) {

										//alert(JSON.stringify(result))

										var sidebar = $('#tree');
										var parent = $('<li class="active treeview menu-open"><a class="addtree" href="#"> <i class="fa fa-code"></i> <span>SQL</span> <i class="fa fa-angle-left pull-right"></a></i>');
										var child = $('<ul class="treeview-menu" id="sidemenu"></ul>');

										child.append(setMenu(result, child));
										parent.append(child);
										sidebar.append(parent);

									},
									error : function() {
										alert("시스템 에러");
									}
								});

						$(document).on(
								"click",
								".addtree",
								function() {

									if ($(this).parent().attr('class')
											.includes('active')) {
										$(this).parent().removeClass('active');
									} else {
										$(this).parent().addClass('active');
									}

								});

						/**
						 * Remove a Tab
						 */
						$('#pageTab').on(
								'click',
								' li a .close',
								function() {
									var tabId = $(this).parents('li').children(
											'a').attr('href');
									$(this).parents('li').remove('li');
									$(tabId).remove();
									$('#pageTab a:first').tab('show');
								});

						/**
						 * Click Tab to show its content 
						 */
						$("#pageTab").on("click", "a", function(e) {
							e.preventDefault();
							$(this).tab('show');
						});

					});

	function setMenu(result, parent) {

		for (var i = 0; i < result.length; i++) {
			var list = result[i];

			if (list.Path.includes('Path')) {
				var folder = $('<li class="treeview">\n'
						+ '          <a class="addtree" href="#">\n'
						+ '<span>'
						+ list.Name
						+ '</span><i class="fa fa-angle-left pull-right"></i></a>\n'
						+ '        </li>');
				var child = $('<ul class="treeview-menu"></ul>');
				folder.append(setMenu(list.list, child));

				parent.append(folder);
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

	function Search() {

		if ($('#' + $("#search").val()).length == 0) {
			alert("메뉴가 없습니다.")
			return false;
		}

		iframe.location.href = $('#' + $("#search").val()).attr('href');

		return false;
	}

	var pageImages = [];
	var pageNum = 1;

	function setFrame(frameid) {

		var text = $('#' + frameid).contents().find('.content-header>h1')
				.text().trim();

		if (text == '') {
			return;
		} else {
			console.log("frameid", frameid, text)
			//console.log('text : ', $('#' + frameid).contents().find('.content-header>h1').text(), frameid)
			var newtab = true;
			for (var i = 0; i < $('#pageTab a').length; i++) {
				//console.log('text2 : ', text, $('#pageTab a:eq(' + i + ')').text().replace(/x$/, ''))
				if (text == $('#pageTab a:eq(' + i + ')').text().replace(/x$/,
						'')) {

					newtab = false;
					$('#pageTab a:eq(' + i + ')').tab('show');
					break;
				}

			}
			if (!newtab) {
				return false;
			}

		}
		var pageid = pageNum++;
		$('#pageTab')
				.append(
						'<li><a href="#tab' + pageid+'" data-toggle="tab">'
								+ text
								+ '<button class="close" type="button" title="Remove this page" style="padding-left:3px"><i class="fa fa-close"></button></a></li>')
		$('#pageTabContent>div:last').attr("id", 'tab' + pageid);
		$('#pageTab a:last').tab('show');
		$('#pageTabContent')
				.append(
						'<div class="tab-pane active" id="newpage"><iframe name="iframe'
								+ pageid
								+ '" id="iframe'
								+ pageid
								+ '" class="tab_frame" style="margin: 0; width: 100%; height: calc(100vh - 90px); border: none; overflow: auto;" onLoad="setFrame(\'iframe'
								+ pageid + '\')"></iframe></div>')

		$('.sidebar-menu a:not(\'.addtree\')')
				.attr("target", 'iframe' + pageid);

		$('#iframe_1').contents().find('#menus a').attr("target", 'iframe' + pageid);
		//alert($('#iframe' + (pageNum == 1 ? '' : pageNum - 1)).contents().find('.ParamForm').length)
		//$('.iframe').contents().find('.ParamForm').attr("target", 'iframe' + pageid)
		//console.log($('#iframe').contents().find('.content-header>h1').text())

	}
</script>

<body class="sidebar-mini skin-purple-light">
	<script type="text/javascript" src="/resources/js/common.js"></script>

	<div class="wrapper">
		<header class="main-header">
			<!-- Logo -->
			<a href="/index" class="logo"> <!-- mini logo for sidebar mini 50x50 pixels -->
				<span class="logo-mini"> <b>D</b>eX
			</span> <!-- logo for regular state and mobile devices --> <span
				class="logo-lg"> <b>Data</b> Explorer
			</span>
			</a>
			<!-- Header Navbar: style can be found in header.less -->
			<nav class="navbar navbar-static-top" role="navigation">
				<!-- Sidebar toggle button-->
				<a href="#" class="sidebar-toggle" data-toggle="offcanvas"
					role="button"> <span class="sr-only">Toggle navigation</span> <span
					class="icon-bar"></span> <span class="icon-bar"></span> <span
					class="icon-bar"></span>
				</a>
				<div class="navbar-custom-menu">
					<ul class="nav navbar-nav">
						<li><a href="/userRemove"><i class="fa fa-sign-out"></i></a></li>
					</ul>
				</div>
			</nav>
		</header>
		<!-- Left side column. contains the logo and sidebar -->
		<aside class="main-sidebar">
			<!-- sidebar: style can be found in sidebar.less -->
			<section class="sidebar" id="sidebar">
				<!-- search form -->
				<form class="sidebar-form" onsubmit="return Search()">
					<div class="input-group">
						<input type="text" name="q" class="form-control"
							placeholder="Search..." id="search" /> <span
							class="input-group-btn">
							<button type="button" name='search' id='search-btn'
								class="btn btn-flat" onclick="Search()">
								<i class="fa fa-search"></i>
							</button>
						</span>
					</div>
				</form>
				<!-- /.search form -->
				<!-- sidebar menu: : style can be found in sidebar.less -->
				<ul class="sidebar-menu" data-widget="tree" id="tree">
					<li class="header">MAIN NAVIGATION</li>



					<c:if test="${memberId eq 'admin'}">
						<li class="treeview"><a href="/Connection" target="iframe">
								<i class="fa fa-database"></i> <span>Connection</span>

						</a> <!-- <ul class="treeview-menu" id="ConnectionList">
							<li><a href="/Connection?DB=2"><i class="fa fa-circle-o"></i> DB1</a></li>
							<li><a href="/Connection?DB=1"><i class="fa fa-circle-o"></i> DB2</a></li>
						</ul> --></li>

						<li class="treeview"><a href="/User" target="iframe"> <i
								class="fa fa-user"></i> <span>User</span>

						</a></li>
					</c:if>




					<li class="treeview"><a href="/FileRead" target="iframe">
							<i class="fa fa-file-text-o"></i> <span>FileRead</span>
					</a></li>
					<li class="treeview"><a href="/FileUpload" target="iframe">
							<i class="fa fa-file-text-o"></i> <span>FileUpload</span>
					</a></li>
				</ul>
			</section>
			<!-- /.sidebar -->
		</aside>
		<div class="content-wrapper" id="framebox">
			<ul id="pageTab" class="nav nav-tabs">
				<li class="active"><a href="#page1" data-toggle="tab">전체메뉴</a></li>
			</ul>
			<div id="pageTabContent" class="tab-content">
				<div class="tab-pane active" id="page1">
					<iframe name="iframe_1" id="iframe_1"
						style="margin: 0; width: 100%; height: calc(100vh - 90px); border: none; overflow: auto;"
						src="/index2"></iframe>
				</div>
				<div class="tab-pane" id="newpage">
					<iframe name="iframe" id="iframe" class="tab_frame"
						style="margin: 0; width: 100%; height: calc(100vh - 90px); border: none; overflow: auto;"
						onload="setFrame('iframe')"></iframe>
				</div>

			</div>

		</div>