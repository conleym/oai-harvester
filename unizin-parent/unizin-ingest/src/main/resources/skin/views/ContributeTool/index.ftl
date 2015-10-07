<@extends src="base.ftl">
<@block name="title">Catalog Contribute</@block>
<@block name="stylesheets">
<link rel="stylesheet" href="${jsPath!skinPath}/contribute.css" type="text/css">
</@block>
<@block name="content">

<div id="root">
</div>

<script>
    window.lti_data = {
        ext_content_return_url: "${ext_content_return_url}"
    }
</script>
<script src="${jsPath!skinPath}/contribute.js"></script>
</@block>
</@extends>
