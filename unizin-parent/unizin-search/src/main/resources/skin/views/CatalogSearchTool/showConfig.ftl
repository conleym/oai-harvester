<?xml version="1.0" encoding="UTF-8"?>
<cartridge_basiclti_link xmlns="http://www.imsglobal.org/xsd/imslticc_v1p0"
                         xmlns:blti = "http://www.imsglobal.org/xsd/imsbasiclti_v1p0"
                         xmlns:lticm ="http://www.imsglobal.org/xsd/imslticm_v1p0"
                         xmlns:lticp ="http://www.imsglobal.org/xsd/imslticp_v1p0"
                         xmlns:xsi = "http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation = "http://www.imsglobal.org/xsd/imslticc_v1p0 http://www.imsglobal.org/xsd/lti/ltiv1p0/imslticc_v1p0.xsd
    http://www.imsglobal.org/xsd/imsbasiclti_v1p0 http://www.imsglobal.org/xsd/lti/ltiv1p0/imsbasiclti_v1p0.xsd
    http://www.imsglobal.org/xsd/imslticm_v1p0 http://www.imsglobal.org/xsd/lti/ltiv1p0/imslticm_v1p0.xsd
    http://www.imsglobal.org/xsd/imslticp_v1p0 http://www.imsglobal.org/xsd/lti/ltiv1p0/imslticp_v1p0.xsd">
    <blti:launch_url>${nuxeoURL}${basePath}/catalog</blti:launch_url>
    <blti:title>Asa Catalog Search</blti:title>
    <blti:description>Unizin Catalog Search</blti:description>
    <blti:extensions platform="canvas.instructure.com">
        <lticm:property name="privacy_level">public</lticm:property>
        <lticm:property name="domain">${nuxeoHost}</lticm:property>
        <lticm:property name="text">Unizin Library</lticm:property>
        <lticm:options name="editor_button">
            <lticm:property name="enabled">true</lticm:property>
            <lticm:property name="icon_url">${nuxeoURL}${skinPath}/icon.png</lticm:property>
            <lticm:property name="selection_width">1024</lticm:property>
            <lticm:property name="selection_height">768</lticm:property>
        </lticm:options>
    </blti:extensions>
</cartridge_basiclti_link>
