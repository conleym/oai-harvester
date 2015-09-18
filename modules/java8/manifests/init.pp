class java8 {
  include apt
  
  file { '/tmp/acceptlicense.txt':
    source => 'puppet:///modules/java8/acceptlicense.txt',
    mode => '0600',
    backup => false,
  }
  
  apt::ppa { 'ppa:webupd8team/java':
    before => Exec['apt_update']
  }

  package { 'oracle-java8-installer':
    responsefile => '/tmp/acceptlicense.txt',
    ensure   => installed,
    require  => [ Apt::Ppa['ppa:webupd8team/java'],
                  File['/tmp/acceptlicense.txt'],
                  Exec['apt_update'] ]
  }

}
