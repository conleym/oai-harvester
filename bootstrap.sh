#!/bin/sh

if [ ! -f /home/vagrant/upgraded ]
then
    wget https://apt.puppetlabs.com/puppetlabs-release-trusty.deb
    dpkg -i puppetlabs-release-trusty.deb
    apt-get update
    apt-get upgrade -y
    apt-get install puppet -y
    wget https://s3.amazonaws.com/courseload-public/debs/ffmpeg-nuxeo_2.7.2-1_amd64.deb
    dpkg -i ffmpeg-nuxeo_2.7.2-1_amd64.deb

    PUPPET_MODULES="puppetlabs-apt puppetlabs-apache elasticsearch-elasticsearch"
    
    for module in $PUPPET_MODULES
    do
        puppet module install $module
    done
    
    touch /home/vagrant/upgraded
fi
