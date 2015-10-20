# Unizin Content Management Platform and Tools

This repository is intended to implement the functionality described in the
[scope statement](https://github.com/unizin/unizin-cmp/wiki/Scope-Statement).

## Development Quick Start

This repository contains a Vagrantfile to configure a virtual machine
for local development work.

1. Install [VirtualBox](https://www.virtualbox.org/wiki/Downloads).

2. Install [Vagrant](https://www.vagrantup.com/downloads.html)
   (version 1.7.3 is earliest that supports SSH autoconfig for
   ansible).

3. Install [Maven](https://maven.apache.org/install.html).

4. Install [Ansible](http://docs.ansible.com/ansible/intro_installation.html).

5. (Optional but highly recommended.) Install
   [vagrant-cachier](https://github.com/fgrehm/vagrant-cachier).  This
   allows your host machine to store local copies of large
   dependencies like Java JDK, Nuxeo, and PostgreSQL locally rather
   than attempt to download them each time a machine is built.

6. Add the line `10.10.20.20 catskateboard.local` to the `/etc/hosts`
   file on your machine.

7. Execute `vagrant up` in your working copy of `unizin-cmp/`

8. Wait quite some time.

9. A Nuxeo instance should be available at
   <https://catskateboard.local/nuxeo>.


## Development Tips

With a working VM, you should be able to build and install the Unizin
packages with the following commands:

    mycomputer:unizin-cmp $ mvn -f unizin-parent/pom.xml package
    mycomputer:unizin-cmp $ ansible-playbook ansible/update_mp.yml

There are a few tricks to avoid this long server restart turnaround
cycle, depending on what you're editing.

* If `use_js_server` is `yes` (the default), then the HTML code is
  pointing to <http://localhost:9595>, which should be the webpack
  server started on the host machine via `npm run` in the web
  application's module directory (for example,
  `unizin-cmp/unizin-parent/unizin-search`.

* If `use_js_server` is `no`, one can build and deploy JavaScript
  from the host machine to the virtual machine with
  `$ ansible-playbook ansible/update_skin.yml`

* If changes to Java code are needed, one can attach to the remote
  server with a debugger (default port is 18787) and use the hot code
  deploy feature of the JVM to avoid server restarts.  This can be
  done with
  [IntelliJ](https://www.jetbrains.com/idea/help/reloading-classes.html),
  [NetBeans](http://stackoverflow.com/questions/10084289/how-to-use-hotswap-in-netbeans),
  [Eclipse](https://wiki.eclipse.org/FAQ_What_is_hot_code_replace%3F),
  or [Ant/Maven](https://code.google.com/p/hotswap/)

* Most changes to the Nuxeo Studio project should not require a server
  restart.
