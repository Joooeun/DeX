<!-- Ace Editor Local Bundle -->
<script src="/resources/js/ace/ace.js"></script>
<script src="/resources/js/ace/ext-language_tools.js"></script>
<script>
	// 로컬 번들 경로 고정 (offline 환경 대응)
	if (typeof ace !== 'undefined') {
		ace.config.set('basePath', '/resources/js/ace');
		ace.config.set('modePath', '/resources/js/ace');
		ace.config.set('themePath', '/resources/js/ace');
		ace.config.set('workerPath', '/resources/js/ace');
	}
</script>

