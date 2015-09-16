class nuxeo {
  include apt
  include java8
  
  apt::source { 'nuxeo':
    location => 'http://apt.nuxeo.org/',
    release => 'trusty',
    repos => 'fasttracks',
    key => {
      source => 'http://apt.nuxeo.org/nuxeo.key',
      id => '0F3BCCFE175E96EE42FB77DD5E2FAE6BDD1BF5E4'
    },
    include => {
      'src' => true,
      'deb' => true,
    },
    before => Exec['apt_update']
  }

  package { 'nuxeo':
    ensure  => installed,
    install_options => ['--install-suggests', '--ignore-missing'],
    require => [ Apt::Source['nuxeo'],
                 Exec['apt_update'],
                 Package['oracle-java8-installer'] ]
  }
}
