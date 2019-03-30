# spring-security-extended-mutual-auth-example
Simple PoC for combining mutual client and basic authentication.

Clients must authenticate first via mutual authentication after that indiviual users authenticate via basic auth.

This example his inspired by the [Endava/javaspektrum-securebydesign](https://github.com/Endava/javaspektrum-securebydesign)
example application that was featured in the Java Spektrum Magazin (02/2019).

## Preparation
The example uses the hostname `apps.tdlabs.local` for testing. This setting can be adjusted in the the `Makefile`
in the `etc/certs` directory.

Note that if you want to run the tests without modification, you need to configure a host alias for
`127.0.0.1 apps.tdlabs.local` in your hosts file, e.g. `/etc/hosts`.

## Run the test
```
mvn clean test
```

## Misc

The Certificate Authority, keys, certificates and keystores are generated via the `Makefile` in
`etc/certs`.

To adjust the settings to your own needs, just adapt the `Makefile` and the `application.yml` file under `src/main/resources` and run the following commands. 
```
cd etc/certs
make clean
make all
```

After that you need to copy the new `srv_keystore.jks` and `srv_truststore.jks` files to `src/main/resources` and 
the new `cln_keystore.jks` and `cln_truststore.jks` files to `src/main/resources`. 


