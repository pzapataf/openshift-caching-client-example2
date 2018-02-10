#!/bin/bash
set -e

CACHING_SERVICE_NAMESPACE="caching-service-example"


echo "Changing active project to ${CACHING_SERVICE_NAMESPACE}"
oc project ${CACHING_SERVICE_NAMESPACE}

rm -f caching-service-trust-store.jks

echo "Retrieving secret with certificates via OC from caching service"
oc get secret service-certs -o yaml | grep "tls\.crt" | sed 's/  tls.crt: //g' | base64 -d > caching-service.cer

echo "Generating caching-service-trust-store.jks via keytool"
keytool -import -noprompt -v -trustcacerts -keyalg RSA -alias "Caching Service" -file "caching-service.cer" -keypass "secret" -storepass "secret" -keystore caching-service-trust-store.jks

rm caching-service.cer

echo "Creating new application from source"
oc new-app redhat-openjdk18-openshift~https://github.com/pzapataf/openshift-caching-client-example2 JAVA_MAIN_CLASS=com.redhat.openshift.caching.examples.HotRodOpenshiftExample

echo "Build logs"
echo "----------------------------------------------------------------------------------------------------------------------"

oc logs -f bc/openshift-caching-client-example2

echo "Creating secret for application"
oc secret new caching-service-truststore caching-service-trust-store.jks=./caching-service-trust-store.jks

echo "Creating environment"
# Copy variables from caching-service secrets : APPLICATION_USER, APPLICATION_PASSWORD
oc set env dc/openshift-caching-client-example2 --from=secret/caching-service-app
oc set env dc/openshift-caching-client-example2 HOT_ROD_SERVICE_ENDPOINT=caching-service-app-hotrod.${CACHING_SERVICE_NAMESPACE}.svc
oc set env dc/openshift-caching-client-example2 HR_SERVICE_TRUST_STORE_PASSWORD=secret

echo "Mounting secret caching-service-truststore in /trustore in deployment config"
oc volume dc/openshift-caching-client-example2 --add -t secret -m /truststore --secret-name=caching-service-truststore

echo "To build example: "
echo "  oc start-build openshift-caching-client-example2"
echo "  oc logs -f bc/openshift-caching-client-example2"

echo "To clean up example: "
echo "  oc delete pods,services,routes,builds,bc,dc -l app=openshift-caching-client-example2"
echo "  oc delete secret caching-service-truststore"
echo "  oc delete is openshift-caching-client-example2"
