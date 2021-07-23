# spring-security-extended-mutual-auth-example
Simple PoC for combining TLS mutual client authentication and basic authentication.

Clients must authenticate first via TLS mutual authentication to establish a TCP connection.
After that individual users need to authenticate via basic auth.
The client certificate is contained in the keystore `etc/certs/client-keystore.p12` with the alias `app1`.

To test the basic auth authentication a test user with the username `tester` and password `test` is defined 
in the [WebSecurityConfig.java](src/main/java/demo/config/WebSecurityConfig.java).

This example his inspired by the [Endava/javaspektrum-securebydesign](https://github.com/Endava/javaspektrum-securebydesign)
example application that was featured in the Java Spektrum Magazin (02/2019).

## Preparation
The example uses the hostname `apps.tdlabs.local` for testing. This setting can be adjusted in the the `generate_certs.sh`
script in the `etc` directory.

Note that if you want to run the tests without modification, you need to configure a host alias for
`127.0.0.1 apps.tdlabs.local` in your hosts file, e.g. `/etc/hosts`.

## Run the test
```
mvn clean test
```

## Misc

The Certificate Authority, keys, certificates and keystores are generated via `etc/generate_certs.sh` into the 
`etc/certs` folder.

To adjust the settings to your own needs, just adapt the `generate_certs.sh` and the `application.yml` file under `src/main/resources` 
and run the following commands. 
```
cd etc
./generate_certs.sh
```

After that you can run the application via `mvn spring-boot:run`. 

## Call endpoint via curl

```
curl \
  -ik \
  --cert-type P12 \
  --cert ./etc/certs/client-keystore.p12:changeit \
  --user tester:test \
  https://apps.tdlabs.local:8443/api/hello
```
