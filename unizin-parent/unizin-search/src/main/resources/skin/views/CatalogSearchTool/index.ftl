<@extends src="base.ftl">
<@block name="title">Unizin Discover Tool</@block>
<@block name="stylesheets">
<link rel="stylesheet" href="${jsPath!skinPath}/catalog_search.css" type="text/css">
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
