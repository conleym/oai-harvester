# -*- mode: ruby -*-
# vi: set ft=ruby :

Vagrant.configure(2) do |config|

  config.vm.box = "ubuntu/trusty64"
  if Vagrant.has_plugin?("vagrant-cachier")
    config.cache.scope = :box
  end

  config.vm.hostname = "catskateboard.local"
  config.vm.network "private_network", ip: "10.10.20.20"
  
  config.vm.provider "virtualbox" do |vb|
    vb.memory = "2048"
  end

  config.vm.provision "shell", path: "bootstrap.sh"
  
  config.vm.provision "puppet" do |puppet|
    puppet.module_path = "modules"
  end
  
end
