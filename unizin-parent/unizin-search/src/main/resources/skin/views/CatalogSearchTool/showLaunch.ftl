<@extends src="base.ftl">
    <@block name="title">LTI Launch Info</@block>
<@block name="content">
<dl>
    <#list postParams?keys as postParam>
        <dt>${postParam}</dt>
        <dd>${postParams[postParam]}</dd>
    </#list>
</dl>
</@block>
</@extends>
