# Apache Geronimo Mail

![Maven Central](https://img.shields.io/maven-central/v/org.apache.geronimo.mail/geronimo-mail_2.1_mail)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Build Status](https://github.com/apache/geronimo-mail/actions/workflows/ci.yml/badge.svg)](https://github.com/apache/geronimo-mail/actions/workflows/ci.yml)
[![Jakarta Mail TCK](https://github.com/apache/geronimo-mail/actions/workflows/tck.yml/badge.svg)](https://github.com/apache/geronimo-mail/actions/workflows/tck.yml)

Apache Geronimo Mail - Apache's implementation of the
[Jakarta Mail 2.1](https://jakarta.ee/specifications/mail/2.1/) specification
(Jakarta EE 10). It passes the Jakarta Mail 2.1 TCK. The artifacts run on
Java 11+ (building requires JDK 21, see below).

Building
========

To build you will need:

 * JDK 21+ (the embedded Apache James 3.9 test servers ship Java 21 class files)
 * Maven 3.9.x

The produced artifacts target Java 11, so they can be used on Java 11+ runtimes.

To build all changes incrementally:

    mvn install

To perform clean builds, which are sometimes needed after some changes to the
source tree:

    mvn clean install

Modules
========

 * `geronimo-mail_2.1_spec` - the Jakarta Mail 2.1 API classes
   (`org.apache.geronimo.specs:geronimo-mail_2.1_spec`)
 * `geronimo-mail_2.1_impl/geronimo-mail_2.1_provider` - the SMTP/IMAP/POP3/NNTP
   protocol providers
 * `geronimo-mail_2.1_impl/geronimo-mail_2.1_mail` - the merged all-in-one
   bundle most consumers want
   (`org.apache.geronimo.mail:geronimo-mail_2.1_mail`)
 * `geronimo-mail_2.1_tck` - runs the Jakarta Mail TCK (see below); inactive
   in normal builds

Jakarta Mail TCK
========

The implementation passes the Jakarta Mail 2.1 TCK (321/321). To run it
locally (JDK 21 required - the TCK harness does not work on JDK 24+):

    mvn clean install
    mvn verify -Ptck -pl geronimo-mail_2.1_tck

The profile downloads the TCK from download.eclipse.org, boots an embedded
Apache James server and fails the build on any test failure. A nightly GitHub
Actions workflow (`tck.yml`) runs the same thing. Known issue: Apache James
3.9 has a response write race that can make exactly one fetch test per run
fail with `Unknown server response: )` - re-run in that case. Details and
manual/debugging instructions: `geronimo-mail_2.1_impl/tck.adoc`.

Releasing
========

See `RELEASE.md`.


Lenient header parameter parsing
========

By default, parameter values in structured headers such as `Content-Type` and
`Content-Disposition` must be quoted when they contain whitespace or special
characters, as the MIME specification requires. Some real-world senders emit
unquoted values (e.g. `Content-Type: multipart/related; type=text/html;
boundary=...`), which then fail to parse. Setting the System property

```
mail.mime.parameters.strict=false
```

makes reading tolerant: an unquoted parameter value then simply ends at the
next semicolon. The default is `true` (strict).

SSL/TLS Protocols used for Mail Connection
========

## Default Behaviour

By default, the implementation checks for the presence of `ssl.protocols`. If this property is not set, the SSL/TLS socket is created with JVM defaults.

## Enable Custom SSL Protocols

The property `ssl.protocols` can be used to specify a list of protocols, which should be enabled for the underlying SSL/TLS socket.
It accepts a list of protocols with a whitespace as delimiter.

### Example for SMTP

To support TLSv1+ the following property can be set:

```
mail.smtp.ssl.protocols=TLSv1 TLSv1.1 TLSv1.2 TLSv1.3
```

## Using a Custom SSL Socket Factory (via Reflection)

The property `ssl.socketFactory.class` can be used to specify a custom SSL socket factory, which is used to create the underlying SSL/TLS socket.
This allows full control of supported cyphers or protocols.

#### Example for SMTP

```
mail.smtp.ssl.socketFactory.class=my.custom.CustomSSLSocketFactory
```

## Using a Custom SSL Socket Factory (as pre-configured instance)

The property `ssl.socketFactory` can be used to specify a pre-configured custom SSL socket factory, which is used to create the underlying SSL/TLS socket.
In this context, the instance has to be passed to the `Properties` of the related `MailSession`. This allows full control of supported cyphers or protocols.

# Cipher suites

## Default Behaviour

By default, the implementation checks for the presence of `ssl.ciphersuites`. If this property is not set, the SSL/TLS socket is created with all supported ciphers of the given SSL Socket.

## Enable Custom Cipher Suites

The property `ssl.ciphersuites` can be used to specify a list of ciphers, which should be enabled for the underlying SSL/TLS socket.
It accepts a list of ciphers with a whitespace as delimiter. You have to ensure, that the listed cipher suites are supported by the given JVM.

### Example for SMTP

To support only `TLS_AES_128_GCM_SHA256` and `TLS_AES_256_GCM_SHA384` the following property can be set:

```
mail.smtp.ssl.ciphersuites=TLS_AES_128_GCM_SHA256 TLS_AES_256_GCM_SHA384
```

## Using a Custom SSL Socket Factory (via Reflection)

The property `ssl.socketFactory.class` can be used to specify a custom SSL socket factory, which is used to create the underlying SSL/TLS socket.
This allows full control of supported cyphers or protocols.

#### Example for SMTP

```
mail.smtp.ssl.socketFactory.class=my.custom.CustomSSLSocketFactory
```

## Using a Custom SSL Socket Factory (as pre-configured instance)

The property `ssl.socketFactory` can be used to specify a pre-configured custom SSL socket factory, which is used to create the underlying SSL/TLS socket.
In this context, the instance has to be passed to the `Properties` of the related `MailSession`. This allows full control of supported cyphers or protocols.
