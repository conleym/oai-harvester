include nuxeo

Exec {
  path      => $::path,
  logoutput => on_failure,
  cwd       => '/',
}

apt::source { 'ubuntu':
  location => 'http://archive.ubuntu.com/ubuntu',
  release  => 'trusty',
  repos    => 'restricted multiverse',
} -> Class['nuxeo']

class { 'apache': }

apache::vhost { 'nuxeo':
  port                => '443',
  docroot             => '/var/www',
  ssl                 => true,
  proxy_pass          => [
    {
    'path'            => '/nuxeo/',
    'url'             => 'http://localhost:8080/nuxeo/',
    'reverse_urls'    => ['http://localhost:8080/nuxeo/']
    }
  ],
  request_headers     => [
    "append nuxeo-virtual-host \"https://${fqdn}/\"",
    'append X-Forwarded-Proto "https"'
  ],
  proxy_preserve_host => true
}

apt::source { 'elasticsearch':
  location => 'http://packages.elastic.co/elasticsearch/1.7/debian',
  release  => 'stable',
  key      => {
    id     => '46095ACC8548582C1A2699A9D27D666CD88E42B4',
    source => 'https://packages.elastic.co/GPG-KEY-elasticsearch'
  }
}

class { 'elasticsearch':
  require => Apt::Source['elasticsearch']
}

elasticsearch::instance { 'es-01':
  require => Class['elasticsearch']
}
