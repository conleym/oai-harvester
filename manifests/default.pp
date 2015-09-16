include nuxeo
apt::source { 'ubuntu':
  location => 'http://archive.ubuntu.com/ubuntu',
  release => 'trusty',
  repos => 'restricted multiverse',
} -> Class['nuxeo']
