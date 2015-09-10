<@extends src="base.ftl">
<@block name="title">Catalog Search Tool</@block>
<@block name="stylesheets">
<link rel="stylesheet" href="${skinPath}/css/main.css" type="text/css">
</@block>
<@block name="content">

<div id="root">
</div>

<script>
    window.lti_data = {
        ext_content_return_url: "${ext_content_return_url}"
    }
</script>
<script src="${jsPath!skinPath}/catalog_search.js"></script>
</@block>
</@extends>
