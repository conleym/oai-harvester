class nuxeo::config inherits nuxeo {
  file_line { 'nuxeo wizard':
    path => '/etc/nuxeo/nuxeo.conf',
    line => 'nuxeo.wizard.done=true',
    match => '^nuxeo.wizard.done'
  }
  
  file_line { 'nuxeo url':
    path => '/etc/nuxeo/nuxeo.conf',
    line => "nuxeo.url=https://${ipaddress_eth1}/nuxeo"
  }
  
  file_line { 'disable elasticsearch':
    path => '/etc/nuxeo/nuxeo.conf',
    line => 'elasticsearch.enabled=false'
  }
  
  file_line { 'enable socket debugging':
    path => '/etc/nuxeo/nuxeo.conf',
    line => 'JAVA_OPTS=$JAVA_OPTS -Xdebug -Xrunjdwp:transport=dt_socket,address=8787,server=y,suspend=n'
  }
  
  file_line { 'enable JMX':
    path => '/etc/nuxeo/nuxeo.conf',
    line => 'JAVA_OPTS=$JAVA_OPTS -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=1089 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false'
  }
}
