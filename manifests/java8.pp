class java8 {
  Apt::Ppa { 'ppa:webupd8team/java': }
  
  package { 'oracle-java8-installer':
    ensure   => installed,
    require  => Apt::Ppa['ppa:webupd8team/java'],
  }
}
