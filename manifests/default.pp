include nuxeo

apt::source { 'ubuntu':
  location => 'http://archive.ubuntu.com/ubuntu',
  release => 'trusty',
  repos => 'restricted multiverse',
} -> Class['nuxeo']

class { 'apache': }

apache::vhost { 'nuxeo':
  port => '443',
  docroot => '/var/www',
  ssl => true,
  proxy_pass => [
    {
    'path' => '/nuxeo/',
    'url' => 'http://localhost:8080/nuxeo/',
    'reverse_urls' => ['http://localhost:8080/nuxeo/']
    }
  ],
  request_headers => [
    "append nuxeo-virtual-host \"https://${ipaddress_eth1}/\"",
    'append X-Forwarded-Proto "https"'
  ],
  proxy_preserve_host => true
}
