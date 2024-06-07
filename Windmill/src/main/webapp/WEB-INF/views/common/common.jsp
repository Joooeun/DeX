<meta content='width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no' name='viewport'>
<link href="/resources/bootstrap/css/bootstrap.min.css" rel="stylesheet" type="text/css" />
<link href="/resources/dist/css/AdminLTE.min.css" rel="stylesheet" type="text/css" />
<link href="/resources/dist/css/skins/_all-skins.min.css" rel="stylesheet" type="text/css" />
<link href="/resources/ionicons/2.0.1/css/ionicons.min.css" rel="stylesheet" type="text/css" />
<link href="https://maxcdn.bootstrapcdn.com/font-awesome/4.3.0/css/font-awesome.min.css" rel="stylesheet" type="text/css" />
<script src="/resources/plugins/jQuery/jQuery-2.1.4.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
<script src="/resources/bootstrap/js/bootstrap.js"></script>


<link href="/resources/plugins/datatables/datatables.min.css" rel="stylesheet">

<script src="/resources/plugins/datatables/datatables.min.js"></script>

<script src="/resources/js/common.js"></script>
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
</script>
