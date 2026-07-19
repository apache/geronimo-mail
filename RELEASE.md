<!--
    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.
-->

# Apache Geronimo Mail — Release Guide

This guide describes how to release Apache Geronimo Mail (the Jakarta Mail 2.1
specification and implementation, `org.apache.geronimo.mail` /
`org.apache.geronimo.specs`). It follows the general ASF release process; the
concrete commands are tailored to this repository.

Throughout this guide `x.y.z` stands for the version being released (e.g.
`1.1.0`) and the release tag is `geronimo-mail_2.1_parent-x.y.z` (the
maven-release-plugin default used by all previous releases).

## 1. Release preparation

- Agree on a release manager (RM) on dev@geronimo.apache.org.
- Review the open [GERONIMO JIRA issues](https://issues.apache.org/jira/browse/GERONIMO)
  for the mail component: everything targeted at `x.y.z` must be resolved;
  move what is not going to make it to the next version.
- Make sure the regular CI build on `main` is green.
- Make sure the nightly *Jakarta Mail TCK* workflow is green (or run it
  manually via the Actions tab). A release must pass the full TCK:

  ```
  mvn clean install
  mvn verify -Ptck -pl geronimo-mail_2.1_tck
  ```

  Expected result: `321 passed / 0 failed / 0 errors`. Note: Apache James 3.9
  (the embedded test server) has a known response write race that can make
  exactly one fetch test fail per run with `Unknown server response: )` — if
  that exact signature appears, re-run; anything else is a real problem.
- Build requirements: **JDK 21** (not newer — the TCK's JavaTest harness does
  not run on JDK 24+; the produced artifacts target Java 11) and Maven 3.9.x.

## 2. One-time setup for the release manager

- Create a PGP code-signing key if you do not have one, upload it to a key
  server and add the fingerprint to your profile at https://id.apache.org/.
- Append your public key to the Geronimo `KEYS` file:

  ```
  svn co --depth files https://dist.apache.org/repos/dist/release/geronimo/ geronimo-dist
  cd geronimo-dist
  (gpg --list-sigs YOUR_KEY_ID && gpg --armor --export YOUR_KEY_ID) >> KEYS
  svn commit -m "Add release key for <apache id>"
  ```

- Add the ASF repository credentials to `~/.m2/settings.xml` (LDAP password,
  or better a [Nexus user token](https://repository.apache.org)):

  ```xml
  <settings>
    <servers>
      <server>
        <id>apache.releases.https</id>
        <username>YOUR_APACHE_ID</username>
        <password>YOUR_PASSWORD_OR_TOKEN</password>
      </server>
      <server>
        <id>apache.snapshots.https</id>
        <username>YOUR_APACHE_ID</username>
        <password>YOUR_PASSWORD_OR_TOKEN</password>
      </server>
    </servers>
  </settings>
  ```

- On a headless machine export `GPG_TTY=$(tty)` so gpg can prompt for the
  passphrase.

## 3. Release steps

### 3.1 Trial build

From a fresh clone of `main`:

```
git clone https://gitbox.apache.org/repos/asf/geronimo-mail.git
cd geronimo-mail
mvn clean install
mvn verify -Ptck -pl geronimo-mail_2.1_tck
mvn clean package -Papache-release
```

The `apache-release` profile (inherited from the `org.apache:apache` parent)
builds the source-release zip, javadoc and source jars, and signs everything
with your PGP key. Fix any problem before continuing.

### 3.2 Prepare and perform the release

`main` is a protected branch that allows normal pushes, so the release is
prepared directly on it:

```
mvn release:prepare -Papache-release
```

- Release version: `x.y.z`, next development version: `x.y.(z+1)-SNAPSHOT`
  (or the next minor if agreed).
- This creates the two `[maven-release-plugin]` commits and the tag
  `geronimo-mail_2.1_parent-x.y.z` and pushes them.

```
mvn release:perform -Papache-release
```

This checks out the tag under `target/checkout`, builds, signs and uploads
all artifacts to a new staging repository at
https://repository.apache.org/#stagingRepositories.

Log into repository.apache.org, find the `orgapachegeronimo-NNNN` staging
repository, inspect its content and **Close** it (do not Release yet). Note
the staging repository URL
(`https://repository.apache.org/content/repositories/orgapachegeronimo-NNNN/`)
for the vote email.

### 3.3 Stage the source release at dist/dev

```
svn co https://dist.apache.org/repos/dist/dev/geronimo/ geronimo-dist-dev
mkdir -p geronimo-dist-dev/javamail
cp target/checkout/target/geronimo-mail_2.1_parent-x.y.z-source-release.zip* geronimo-dist-dev/javamail/
cd geronimo-dist-dev
svn add javamail/geronimo-mail_2.1_parent-x.y.z-source-release.zip*
svn commit -m "Stage Apache Geronimo Mail x.y.z source release for vote"
```

(The zip, its `.asc` signature and its `.sha512` checksum — same layout as
https://dist.apache.org/repos/dist/release/geronimo/javamail/.)

### 3.4 Check the release candidate

On a clean machine/directory:

- Verify the signature against the Geronimo `KEYS` file:

  ```
  curl -O https://downloads.apache.org/geronimo/KEYS
  gpg --import KEYS
  gpg --verify geronimo-mail_2.1_parent-x.y.z-source-release.zip.asc
  sha512sum -c geronimo-mail_2.1_parent-x.y.z-source-release.zip.sha512
  ```

- Unpack the source release and confirm: `LICENSE` and `NOTICE` are present
  and correct, no unexpected binary files, and it builds from source
  (`mvn clean install`, JDK 21).
- Ideally run the TCK against the staged artifacts as well.

### 3.5 Start the vote

Send to dev@geronimo.apache.org, subject:

```
[VOTE] Release Apache Geronimo Mail (Jakarta Mail 2.1) x.y.z
```

Body template:

```
Hi,

I would like to call a vote on releasing Apache Geronimo Mail x.y.z,
our implementation of the Jakarta Mail 2.1 specification.

Notable changes in this release:
  <short list, or link to the JIRA release notes>

JIRA release notes:
  https://issues.apache.org/jira/secure/ReleaseNote.jspa?projectId=...&version=...

Source release (with .asc and .sha512):
  https://dist.apache.org/repos/dist/dev/geronimo/javamail/

Maven staging repository:
  https://repository.apache.org/content/repositories/orgapachegeronimo-NNNN/

Git tag:
  https://github.com/apache/geronimo-mail/releases/tag/geronimo-mail_2.1_parent-x.y.z
  (commit <tag sha>)

Release verification: the build is green on JDK 21 and the full Jakarta
Mail 2.1 TCK passes (321/321) against these artifacts.

KEYS file:
  https://downloads.apache.org/geronimo/KEYS

Please review and vote. The vote is open for at least 72 hours.

[ ] +1 Release these artifacts
[ ] +0 No opinion
[ ] -1 Do not release, because ...

Thanks,
<RM>
```

The vote passes with at least three binding +1 votes (Geronimo PMC) and more
+1 than -1, after a minimum of 72 hours. Close it with a
`[RESULT][VOTE] ...` email summarizing the votes.

## 4. After a successful vote

### 4.1 Release the staging repository

At https://repository.apache.org select the closed `orgapachegeronimo-NNNN`
repository and press **Release**. The artifacts sync to Maven Central within
a few hours.

### 4.2 Move the source release to dist/release

```
svn mv https://dist.apache.org/repos/dist/dev/geronimo/javamail/geronimo-mail_2.1_parent-x.y.z-source-release.zip \
       https://dist.apache.org/repos/dist/release/geronimo/javamail/ -m "Release Apache Geronimo Mail x.y.z"
# repeat for the .asc and .sha512 files
```

Per ASF policy, remove superseded old releases from
`dist/release/geronimo/javamail/` afterwards (they remain available from
archive.apache.org).

### 4.3 Bookkeeping

- Mark the version as released in JIRA and create the next version.
- Record the release at https://reporter.apache.org/addrelease.html?geronimo
  (needed for the board report).
- Create a GitHub release for the tag with a short changelog.
- Send the announcement from your @apache.org address to
  announce@apache.org and dev@geronimo.apache.org, subject
  `[ANNOUNCE] Apache Geronimo Mail x.y.z released`, including a one-paragraph
  project description, the notable changes, the Maven coordinates
  (`org.apache.geronimo.mail:geronimo-mail_2.1_mail:x.y.z`) and the download
  location. Note: announce@ only accepts plain-text mails from apache.org
  addresses.

## 5. After an unsuccessful vote

- Reply to the vote thread with `[CANCEL][VOTE] ...`.
- **Drop** the staging repository at https://repository.apache.org.
- Remove the staged files from `dist/dev/geronimo/javamail/` (`svn rm`).
- Clean up git: `main` cannot be force-pushed, so revert the two
  `[maven-release-plugin]` commits with `git revert` and delete the release
  tag (`git push --delete origin geronimo-mail_2.1_parent-x.y.z`); if tag
  deletion is rejected by infra policy, leave the tag and use the next patch
  version for the new candidate.
- Fix the issues, then restart at step 3.
