#!/bin/sh

# This is a bit clunky for now and will only work within a source build of full jetty

PORT=8081
 [ $# -eq 1 ] && PORT=$1 && shift;

cd $(dirname $0)/..
DEMO_HOME=$(pwd)
JETTY_HOME=../..
JETTY_VERSION=6.1.7

TC_HOME=/java/terracotta-trunk
TC_BOOT_JAR=$TC_HOME/dso-boot-hotspot_linux_150_08.jar
TC_CONFIG_PATH="$DEMO_HOME/etc/tc-config.xml"

CLASSPATH=\
$JETTY_HOME/lib/servlet-api-2.5-$JETTY_VERSION.jar:\
$JETTY_HOME/lib/jetty-util-$JETTY_VERSION.jar:\
$JETTY_HOME/lib/jetty-$JETTY_VERSION.jar:\
$JETTY_HOME/contrib/cometd/target/classes:\
$DEMO_HOME/target/classes

set -x

${JAVA_HOME}/bin/java \
    -Dtc.install-root=${TC_HOME} \
    -Xbootclasspath/p:$TC_BOOT_JAR \
    -Dtc.config=$TC_CONFIG_PATH \
    -cp "$CLASSPATH" \
    org.mortbay.cometd.demo.Main \
    "$PORT"
