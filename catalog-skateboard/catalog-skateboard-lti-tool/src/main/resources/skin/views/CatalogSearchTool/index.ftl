<@extends src="base.ftl">
<@block name="title">Catalog Search Tool</@block>
<@block name="stylesheets">
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/normalize/3.0.3/normalize.css" type="text/css">
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
