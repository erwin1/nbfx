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
# Releasing nbfx

nbfx follows the Apache release procedure as closely as a non-ASF project can. The model is the
[Apache Answer release guide](https://answer.apache.org/community/how-to-release/) and the Apache NetBeans
release checklist ([apache/netbeans#9684](https://github.com/apache/netbeans/issues/9684)). Where nbfx
cannot yet do what Apache does, the step says so:

> **Apache:** what an ASF project does · **nbfx now:** what we do instead

Adopting the full procedure later should then be a small diff to this file, not a rewrite.

**nbfx is not an Apache Software Foundation project.** Artefacts, tags and announcements never use the
`apache-` prefix or the Apache name for nbfx itself.

## What a release consists of

| Artefact | Content |
|---|---|
| `nbfx-<version>-src.tar.gz` | **The release.** `git archive` of the release tag. It does not contain the apache/netbeans sources: the build fetches the pinned revision (`nbfx.submodule.commit` in `nbfx.properties`). |
| `nbfx-<version>-bin.zip` | Convenience binary for macOS aarch64/x64, Windows x64 and Linux x64: the nbfx cluster config (`ant build`) with JavaFX for all four and both launchers, and LICENSE, NOTICE, a THIRDPARTY listing exactly its JavaFX files, and the bundled licence texts. Needs JDK 25. |
| `*.asc` | Detached, armored GPG signature of each artefact. |
| `*.sha512` | SHA-512 checksum of each artefact (`sha512sum -c` / `shasum -a 512 -c`). |
| `KEYS` | The public keys of the release managers (repository root). |

Versions are `MAJOR.MINOR.PATCH`. The tags are `<version>-rcN` for candidates and `<version>` for the
release, both on the same commit. Each RC is built under the final file names, so promoting it uploads the
same bytes again.

## One-time setup: the signing key

> **Apache:** the key is added to `https://dist.apache.org/repos/dist/release/<project>/KEYS` (svn) ·
> **nbfx now:** `KEYS` in this repository, and the key on public keyservers.

1. Create an RSA-4096 key (`gpg --full-generate-key`: "RSA and RSA", 4096, expiry `2y`) with your work address.
2. Publish it:
   ```sh
   gpg --keyserver keyserver.ubuntu.com --send-keys <FINGERPRINT>
   gpg --export <FINGERPRINT> | curl -T - https://keys.openpgp.org   # then confirm the e-mail
   ```
3. Append it to `KEYS` and commit:
   ```sh
   (gpg --list-sigs <FINGERPRINT> && gpg --armor --export <FINGERPRINT>) >> KEYS
   ```
4. Back up the secret key and the revocation certificate (`~/.gnupg/openpgp-revocs.d/`).
5. On macOS, use a GUI passphrase prompt: `ant release` runs gpg without a terminal, where the default
   curses prompt fails with *Inappropriate ioctl for device*:
   ```sh
   brew install pinentry-mac
   echo "pinentry-program $(brew --prefix)/bin/pinentry-mac" >> ~/.gnupg/gpg-agent.conf
   gpgconf --kill gpg-agent
   ```

Extend an expiring key with `gpg --quick-set-expire <FINGERPRINT> 2y`, then re-publish it and replace its
`KEYS` entry.

## The procedure

The release manager builds and signs on their own machine, as at Apache. Two GitHub workflows do the
bookkeeping around that; neither builds a release artefact nor holds a key:

| Workflow | Runs on | Does |
|---|---|---|
| *Release draft* (`.github/workflows/release-draft.yml`) | push of a tag `<version>-rcN` or `<version>` | checks the tag (format, matches `nbfx.version`, annotated, signature verifies against `KEYS`; a final tag must be on an RC commit), runs `ant rat`, `ant build` and `ant check-purity` on the tagged commit, creates the **draft** release with the notes from `release/release-notes.md` plus the generated change list, and opens or updates the release checklist issue. For a final tag it copies the files of the RC into the draft. |
| *Release verify* (`.github/workflows/release-verify.yml`) | publishing a release, or by hand (`gh workflow run release-verify.yml -f tag=<tag>`) | downloads the uploaded files, runs `ant release-verify` on them, checks that the source release is exactly `git archive` of the tag, builds the unpacked source release, and posts the result on the checklist issue. |

The checklist issue (template: *Release checklist*) is opened by *Release draft* for the first RC; tick
the steps off there.

### 1. Prepare

- Pick the version and set `nbfx.version` in `nbfx.properties`.
- Make sure `main` is green: `ant bootstrap build`, `ant check-purity`, `ant rat`.
  A file that fails `ant rat` usually just lacks its licence header: `ant headers` adds it.
- Release notes: *Release draft* fills `release/release-notes.md` into the draft and appends GitHub's
  list of merged pull requests since the previous release. Adjust the template beforehand if needed; edit
  the highlights in the draft afterwards.

### 2. Tag the release candidate

> **Apache:** signed RC tag `<version>-rcN` · **nbfx now:** the same.

```sh
git tag -s -a 0.1.0-rc1 -m "nbfx 0.1.0-rc1"
git push origin 0.1.0-rc1
```

Pushing the tag starts *Release draft* (about 10 minutes). When it is green there is a draft pre-release
`0.1.0-rc1` and a checklist issue *Release nbfx 0.1.0*. A red run usually means the tag is wrong: delete it
(`git push --delete origin 0.1.0-rc1 && git tag -d 0.1.0-rc1`), fix, and tag again.

### 3. Build, sign and verify

```sh
ant release -Dnbfx.rc=rc1 -Dnbfx.gpg.key=<FINGERPRINT>
```

This runs, in order:

| Target | Does |
|---|---|
| `release-src` | `git archive` of HEAD → `build/release/nbfx-<v>-src.tar.gz`. Refuses a dirty tree or a HEAD that is not the tag. |
| `release-bin` | `ant build`, the launcher adjustments (`-dist-launcher`), the upstream licence summary (`create-mandatory-files-for-binary`) and the nbfx LICENSE/NOTICE additions, then `zip-cluster-config`. In that zip, `libs.javafx`'s files are replaced by the four pinned SDKs (`libs.javafx/javafx-sdk.properties`): natives per platform, a `javafx.graphics.jar` merged from the four, a regenerated `update_tracking` entry, and THIRDPARTY → `build/release/nbfx-<v>-bin.zip`. |
| `release-sign` | `gpg --detach-sign --armor` → `.asc` and SHA-512 → `.sha512`, for each artefact. |
| `release-verify` | Checks that exactly the 6 files are there, verifies every `.asc` against a keyring that only contains `KEYS`, checks every `.sha512`, and runs Rat on the unpacked source release and checks it holds no compiled code. The binary: the licence files (including the JavaFX licence), no `javafx.web`, and (`release/tools/CheckBinary.java`, against the four pinned SDKs) every native in the right place with its platform's format and CPU, `javafx.graphics.jar` exactly the union of the SDKs' jars, and both launchers. |

`-Dnbfx.release.dryrun=true` skips the tag and clean-tree checks, for a rehearsal.

Then start the binary once on each platform, with an empty userdir (`--userdir`): `bin/netbeans`, or
`bin\netbeans64.exe` on Windows. Whether the application comes up is the one thing no workflow checks.
Look for `var/imported` in the userdir and for a `var/log/messages.log` without "was needed and not
found", `NoClassDefFoundError` or glass/prism errors. macOS x64 can be tried on Apple silicon under
Rosetta 2 with an x64 JDK 25. On macOS, test a zip downloaded through the browser, not the local file:
quarantine and Gatekeeper treat the bundled dylibs differently.

Without `--userdir` the binary uses `nbfx-<version>` under the platform's default roots
(`~/Library/Application Support/NetBeans/nbfx-<version>` and `~/Library/Caches/NetBeans/nbfx-<version>` on
macOS), shared by every build of that version: dry runs, earlier candidates and pre-release tryouts. Their
saved state does not always fit a later build; a window layout naming a view that has since been renamed,
for instance, restores as an empty window. Remove both directories before testing a candidate.

### 4. Publish the candidate

> **Apache:** upload to `https://dist.apache.org/repos/dist/dev/<project>/` (svn) ·
> **nbfx now:** a GitHub **pre-release** on the RC tag.

Upload the six files from `build/release/` (two artefacts, each with `.asc` and `.sha512`) to the draft
and let *Release verify* check them before anyone else sees them:

```sh
gh release upload 0.1.0-rc1 build/release/*
gh workflow run release-verify.yml -f tag=0.1.0-rc1
```

When it reports green on the checklist issue, write the highlights in the draft and **publish** it (it
stays a pre-release). Publishing runs *Release verify* once more, on the public files.

### 5. Review

> **Apache:** `[VOTE]` on `dev@`, open for at least 72 hours, at least three binding +1 votes from the PMC,
> then `[RESULT][VOTE]` · **nbfx now:** a review of at least 72 hours on the release checklist issue; the
> release needs a +1 from different contributors.

Reviewers verify the candidate (see *Verifying a release* below), try it, and comment `+1`, `0` or `-1`
with their reasons. A `-1` that holds means: fix on `main`, then go back to step 2 with `rc2`. When the
window closes, post the result (who voted what) as a comment.

### 6. Promote

> **Apache:** `svn mv` from `dist/dev` to `dist/release`, final tag · **nbfx now:** final tag and a GitHub
> release with the same files.

```sh
git tag -s -a 0.1.0 -m "nbfx 0.1.0" 0.1.0-rc1^{}
git push origin 0.1.0
```

*Release draft* checks the tag, creates the draft release `0.1.0` (not a pre-release) and copies the six
files of `0.1.0-rc1` into it: the same bytes, no rebuild. Check the draft, copy the highlights over from the
RC and publish it. Then edit the RC pre-release to point to the final release.

### 7. Announce

> **Apache:** mail to `announce@apache.org` and `dev@` · **nbfx now:** link to the GitHub release.

### 8. Afterwards

- Bump `nbfx.version` on `main` to the next version.
- Close the checklist issue.

## Verifying a release

```sh
curl -LO https://raw.githubusercontent.com/gluonhq/nbfx/main/KEYS
gpg --import KEYS
for f in nbfx-*.tar.gz nbfx-*.zip; do gpg --verify "$f.asc" "$f"; done
for f in nbfx-*.sha512; do sha512sum -c "$f"; done      # macOS: shasum -a 512 -c "$f"
```

A good signature from a key in `KEYS` and `OK` for every checksum mean that the files are the ones the
release manager built. Then unpack the source release and check that it has `LICENSE`, `NOTICE` and
`THIRDPARTY`, that `ant rat` passes, and that `ant build run` works.

## Release notes

The template is [`release/release-notes.md`](release/release-notes.md); *Release draft* replaces its
`@VERSION@`, `@TAG@`, `@NB_COMMIT@` and `@REPOSITORY@` tokens.
