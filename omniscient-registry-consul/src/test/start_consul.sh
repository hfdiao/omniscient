#! /bin/bash

killall -9 consul
rm -fr "/tmp/consul"
consul agent -server=true -bootstrap -data-dir=/tmp/consul
