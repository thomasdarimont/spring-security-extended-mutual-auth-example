#!/bin/bash

echo Generate Root CA Certificate

ROOT_SUBJECT=/C=DE/ST=SL/L=SB/O=tdlabs
CERTS_DIR=./certs

CA_ALIAS=apps_root_ca
CA_PASSWORD=changeit
CA_SUBJECT="$ROOT_SUBJECT/OU=pki/CN=$CA_ALIAS"

SERVER_ALIAS=apps.tdlabs.local
SERVER_SUBJECT="$ROOT_SUBJECT/OU=servers/CN=$SERVER_ALIAS"
SERVER_KEYSTORE_PASS=changeit
SERVER_TRUSTSTORE_PASS=changeit

CLIENT_ALIAS=app1
CLIENT_SUBJECT="$ROOT_SUBJECT/OU=apps/CN=$CLIENT_ALIAS"
CLIENT_KEYSTORE_PASS=changeit
CLIENT_TRUSTSTORE_PASS=changeit

openssl genrsa \
  -des3 \
  -out $CERTS_DIR/$CA_ALIAS.key.pem \
  -passout pass:"$CA_PASSWORD" \
  4096

openssl req \
  -x509 \
  -new \
  -nodes \
  -key $CERTS_DIR/$CA_ALIAS.key.pem \
  -sha256 \
  -days 356 \
  -subj "$CA_SUBJECT" \
  -out $CERTS_DIR/$CA_ALIAS.crt.pem \
  -passin pass:"$CA_PASSWORD"

# openssl x509 -text -noout -in $CA_ALIAS.crt.pem

echo
echo Generate Server certificate

openssl genrsa \
  -out $CERTS_DIR/$SERVER_ALIAS.key.pem \
  4096

openssl req \
  -new \
  -sha256 \
  -subj "$SERVER_SUBJECT" \
  -key $CERTS_DIR/$SERVER_ALIAS.key.pem \
  -out $CERTS_DIR/$SERVER_ALIAS.csr.pem

# openssl req -in $CERTS_DIR/$SERVER_ALIAS.csr.pem -noout -text

openssl x509 \
  -req \
  -in $CERTS_DIR/$SERVER_ALIAS.csr.pem \
  -CA $CERTS_DIR/$CA_ALIAS.crt.pem \
  -CAkey $CERTS_DIR/$CA_ALIAS.key.pem \
  -CAcreateserial \
  -passin pass:"$CA_PASSWORD" \
  -out $CERTS_DIR/$SERVER_ALIAS.crt.pem \
  -days 120 \
  -sha256 \
  -extfile <(printf "subjectAltName=DNS:$SERVER_ALIAS")

openssl pkcs12 \
  -export \
  -nodes \
  -in $CERTS_DIR/$SERVER_ALIAS.crt.pem \
  -inkey "$CERTS_DIR/$SERVER_ALIAS.key.pem" \
  -name "$SERVER_ALIAS" \
  -out $CERTS_DIR/server-keystore.p12 \
  -passout pass:$SERVER_KEYSTORE_PASS


echo
echo Generate Client certificate

openssl genrsa -out $CERTS_DIR/$CLIENT_ALIAS.key.pem 2048

openssl req \
  -new \
  -sha256 \
  -subj "$CLIENT_SUBJECT" \
  -key $CERTS_DIR/$CLIENT_ALIAS.key.pem \
  -out $CERTS_DIR/$CLIENT_ALIAS.csr.pem

# openssl req -in $CERTS_DIR/$CLIENT_ALIAS.csr.pem -noout -text

openssl x509 \
  -req \
  -in $CERTS_DIR/$CLIENT_ALIAS.csr.pem \
  -CA $CERTS_DIR/$CA_ALIAS.crt.pem \
  -CAkey $CERTS_DIR/$CA_ALIAS.key.pem \
  -CAcreateserial \
  -passin pass:"$CA_PASSWORD" \
  -out $CERTS_DIR/$CLIENT_ALIAS.crt.pem \
  -days 120 \
  -sha256 \
  -extfile <(printf "extendedKeyUsage=clientAuth\nsubjectKeyIdentifier=hash\nauthorityKeyIdentifier=keyid")

openssl pkcs12 \
  -export \
  -nodes \
  -in $CERTS_DIR/$CLIENT_ALIAS.crt.pem \
  -inkey "$CERTS_DIR/$CLIENT_ALIAS.key.pem" \
  -name "$CLIENT_ALIAS" \
  -out $CERTS_DIR/client-keystore.p12 \
  -passout pass:$CLIENT_KEYSTORE_PASS

echo
echo Create client truststore with root-ca and server certificate

keytool -import \
  -trustcacerts \
  -noprompt \
  -alias $CA_ALIAS \
  -file $CERTS_DIR/$CA_ALIAS.crt.pem \
  -keystore "$CERTS_DIR/client-truststore.p12" \
  -storepass "$CLIENT_TRUSTSTORE_PASS" \
  -storetype pkcs12

keytool -import \
  -trustcacerts \
  -noprompt \
  -alias $SERVER_ALIAS \
  -file $CERTS_DIR/$SERVER_ALIAS.crt.pem \
  -keystore "$CERTS_DIR/client-truststore.p12" \
  -storepass "$CLIENT_TRUSTSTORE_PASS" \
  -storetype pkcs12

echo
echo Create server truststore with root-ca and client certificate

keytool -import \
  -trustcacerts \
  -noprompt \
  -alias $CA_ALIAS \
  -file $CERTS_DIR/$CA_ALIAS.crt.pem \
  -keystore "$CERTS_DIR/server-truststore.p12" \
  -storepass "$SERVER_TRUSTSTORE_PASS" \
  -storetype pkcs12

keytool -import \
  -trustcacerts \
  -noprompt \
  -alias $CLIENT_ALIAS \
  -file $CERTS_DIR/$CLIENT_ALIAS.crt.pem \
  -keystore "$CERTS_DIR/server-truststore.p12" \
  -storepass "$SERVER_TRUSTSTORE_PASS" \
  -storetype pkcs12