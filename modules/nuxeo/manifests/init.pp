class nuxeo ($service_enable = $nuxeo::params::service_enable,
             $service_ensure = $nuxeo::params::service_ensure,
             $service_name   = $nuxeo::params::service_name) inherits nuxeo::params
{

  class { 'nuxeo::install': } ->
  class { 'nuxeo::service': } ->
  class { 'nuxeo::config': } ->
  exec { 'restart nuxeo':
    command => "/usr/sbin/service nuxeo restart"
  } # ~> Service['nuxeo'] doesn't work!
}
