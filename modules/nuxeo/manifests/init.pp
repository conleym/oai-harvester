class nuxeo {
  include apt
  
  apt::source { 'nuxeo':
    location => 'http://apt.nuxeo.org/',
    release => 'trusty',
    repos => 'fasttracks',
    key => {
      source => 'http://apt.nuxeo.org/nuxeo.key',
      id => 'DD1BF5E4'
    },
    include => {
      'src' => true,
      'deb' => true,
    },
  }

  package { 'nuxeo':
    ensure  => installed,
    require => [ Apt::Source['nuxeo'],
                 Exec['apt_update'] ]
  }
}
