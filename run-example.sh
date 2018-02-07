#!/bin/bash
#set -e

CACHING_SERVICE_NAMESPACE="my-example"
EXAMPLE_NAMESPACE="hot-rod-example2"

echo "Changing active project to ${CACHING_SERVICE_NAMESPACE}"
oc project ${CACHING_SERVICE_NAMESPACE}

rm -f caching-service-trust-store.jks

echo "Retrieving secret with certificates via OC from caching service"
oc get secret service-certs -o yaml | grep "tls\.crt" | sed 's/  tls.crt: //g' | base64 -d > caching-service.cer

echo "Generating caching-service-trust-store.jks via keytool"
keytool -import -noprompt -v -trustcacerts -keyalg RSA -alias "Caching Service" -file "caching-service.cer" -keypass "secret" -storepass "secret" -keystore caching-service-trust-store.jks

rm caching-service.cer

echo "Creating project ${EXAMPLE_NAMESPACE}"
oc new-project ${EXAMPLE_NAMESPACE}

echo "Creating new application from source"
oc new-app redhat-openjdk18-openshift~https://github.com/pzapataf/openshift-caching-client-example2 JAVA_MAIN_CLASS=com.redhat.openshift.caching.examples.HotRodOpenshiftExample \
-e HOT_ROD_SERVICE_ENDPOINT=caching-service-app-hotrod.my-example.svc \
-e HOT_ROD_SERVICE_USER=test \
-e HOT_ROD_SERVICE_PASSWORD=test \
-e HR_SERVICE_TRUST_STORE_PATH=/truststore/caching-service-trust-store.jks \
-e HR_SERVICE_TRUST_STORE_PASSWORD=secret

echo "Build logs"
echo "----------------------------------------------------------------------------------------------------------------------"

oc logs -f bc/openshift-caching-client-example2

echo "Creating secret for application"
oc secret new caching-service-truststore caching-service-trust-store.jks=./caching-service-trust-store.jks

