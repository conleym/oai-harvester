#!/bin/sh

if [ ! -f /home/vagrant/upgraded ]
then
   apt-get update
   apt-get upgrade -y
   puppet module install puppetlabs-apt
   touch /home/vagrant/upgraded
fi
