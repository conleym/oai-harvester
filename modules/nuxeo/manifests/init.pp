class nuxeo ($service_enable = $nuxeo::params::service_enable,
             $service_ensure = $nuxeo::params::service_ensure,
             $service_name   = $nuxeo::params::service_name) inherits nuxeo::params
{

  class { '::nuxeo::install': } ->
  class { '::nuxeo::config': } ~>
  class { '::nuxeo::service': }
  
}
